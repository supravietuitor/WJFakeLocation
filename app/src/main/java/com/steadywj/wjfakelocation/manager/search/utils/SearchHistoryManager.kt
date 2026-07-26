// SearchHistoryManager.kt
package com.steadywj.wjfakelocation.manager.search.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * æœç´¢åŽ†å²ç®¡ç†å™?
 * 
 * åŠŸèƒ½:
 * - åŽ†å²è®°å½•å­˜å‚¨
 * - çƒ­é—¨æœç´¢æŽ¨è
 * - æ™ºèƒ½æŽ’åºï¼ˆé¢‘çŽ?+ æ—¶é—´ï¼?
 */
@Singleton
class SearchHistoryManager @Inject constructor(
    private val context: Context
) {
    
    /** æœç´¢åŽ†å²åˆ—è¡¨ */
    private val _searchHistory = MutableStateFlow<List<SearchRecord>>(emptyList())
    val searchHistory: Flow<List<SearchRecord>> = _searchHistory.asStateFlow()
    
    /** çƒ­é—¨æœç´¢åˆ—è¡¨ */
    private val _hotSearches = MutableStateFlow<List<String>>(emptyList())
    val hotSearches: Flow<List<String>> = _hotSearches.asStateFlow()
    
    /** åŽ†å²è®°å½•æ–‡ä»¶ */
    private val historyFile: File by lazy {
        File(context.filesDir, "search_history.json")
    }
    
    /** æœ€å¤§åŽ†å²è®°å½•æ•° */
    private val MAX_HISTORY_SIZE = 50
    
    init {
        loadHistory()
        updateHotSearches()
    }
    
    /**
     * æ·»åŠ æœç´¢è®°å½•
     */
    suspend fun addSearchRecord(query: String, category: String = "default") {
        return withContext(Dispatchers.IO) {
            val currentList = _searchHistory.value.toMutableList()
            
            // æŸ¥æ‰¾æ˜¯å¦å·²å­˜åœ¨ç›¸åŒæŸ¥è¯?
            val existingIndex = currentList.indexOfFirst { it.query == query }
            
            if (existingIndex >= 0) {
                // å·²å­˜åœ¨ï¼Œæ›´æ–°è®¡æ•°å’Œæ—¶é—?
                val existing = currentList[existingIndex]
                currentList[existingIndex] = existing.copy(
                    count = existing.count + 1,
                    lastSearchedAt = System.currentTimeMillis()
                )
                // ç§»åˆ°æœ€å‰é¢
                val record = currentList.removeAt(existingIndex)
                currentList.add(0, record)
            } else {
                // æ–°å¢žè®°å½•
                val newRecord = SearchRecord(
                    query = query,
                    category = category,
                    count = 1,
                    lastSearchedAt = System.currentTimeMillis()
                )
                currentList.add(0, newRecord)
                
                // é™åˆ¶å¤§å°
                while (currentList.size > MAX_HISTORY_SIZE) {
                    currentList.removeAt(currentList.size - 1)
                }
            }
            
            _searchHistory.value = currentList
            
            // å¼‚æ­¥ä¿å­˜åˆ°æ–‡ä»?
            saveHistoryAsync()
            
            // æ›´æ–°çƒ­é—¨æœç´¢
            updateHotSearches()
        }
    }
    
    /**
     * åˆ é™¤å•æ¡è®°å½•
     */
    suspend fun deleteRecord(record: SearchRecord) {
        return withContext(Dispatchers.IO) {
            _searchHistory.value = _searchHistory.value.filter { it != record }
            saveHistoryAsync()
            updateHotSearches()
        }
    }
    
    /**
     * æ¸…é™¤æ‰€æœ‰åŽ†å²è®°å½?
     */
    suspend fun clearAllHistory() {
        return withContext(Dispatchers.IO) {
            _searchHistory.value = emptyList()
            _hotSearches.value = emptyList()
            
            withContext(Dispatchers.IO) {
                historyFile.delete()
            }
        }
    }
    
    /**
     * èŽ·å–æŽ¨èæœç´¢ï¼ˆåŸºäºŽæ—¶é—´å’Œé¢‘çŽ‡ï¼?
     */
    fun getRecommendations(limit: Int = 5): List<SearchRecord> {
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        
        return _searchHistory.value
            .map { record ->
                val recencyScore = calculateRecencyScore(record.lastSearchedAt, now, oneDayMillis)
                val frequencyScore = Math.log(record.count.toDouble() + 1)
                val score = recencyScore * 0.6 + frequencyScore * 0.4 // 60% æ—¶æ•ˆæ€?+ 40% é¢‘çŽ‡
                
                record to score
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }
    
    /**
     * æŒ‰åˆ†ç±»ç­›é€‰åŽ†å²è®°å½?
     */
    fun filterByCategory(category: String): List<SearchRecord> {
        return if (category == "all") {
            _searchHistory.value
        } else {
            _searchHistory.value.filter { it.category == category }
        }
    }
    
    /**
     * å¯¼å‡ºåŽ†å²è®°å½•
     */
    suspend fun exportHistory(): String {
        return withContext(Dispatchers.IO) {
            // ç®€å?JSON æ ¼å¼å¯¼å‡º
            buildString {
                appendLine("[")
                _searchHistory.value.forEachIndexed { index, record ->
                    appendLine("  {")
                    appendLine("    \"query\": \"${record.query}\",")
                    appendLine("    \"category\": \"${record.category}\",")
                    appendLine("    \"count\": ${record.count},")
                    appendLine("    \"lastSearchedAt\": ${record.lastSearchedAt}")
                    appendLine("  }${if (index < _searchHistory.value.size - 1) "," else ""}")
                }
                appendLine("]")
            }
        }
    }
    
    // ==================== å†…éƒ¨æ–¹æ³• ====================
    
    /**
     * åŠ è½½åŽ†å²è®°å½•
     */
    private fun loadHistory() {
        try {
            if (historyFile.exists()) {
                val content = historyFile.readText()
                // ä½¿ç”¨ JSON è§£æžåº“ï¼ˆå¦?Kotlinx Serializationï¼?
                // TODO: æ·»åŠ  kotlinx-serialization ä¾èµ–å¹¶å®žçŽ°å®Œæ•´è§£æž?
                // è¿™é‡Œç®€åŒ–å¤„ç†ï¼Œå®žé™…åº”è¯¥è§£æž JSON
                _searchHistory.value = emptyList() // å ä½å®žçŽ°
            }
        } catch (e: Exception) {
            _searchHistory.value = emptyList()
        }
    }
    
    /**
     * å¼‚æ­¥ä¿å­˜åŽ†å²è®°å½•
     */
    private fun saveHistoryAsync() {
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                // ä½¿ç”¨ JSON åºåˆ—åŒ?
                // TODO: æ·»åŠ  kotlinx-serialization ä¾èµ–å¹¶å®žçŽ°å®Œæ•´åºåˆ—åŒ–
                val json = buildString {
                    appendLine("[")
                    _searchHistory.value.forEachIndexed { index, record ->
                        appendLine("  {")
                        appendLine("    \"query\": \"${record.query}\",")
                        appendLine("    \"category\": \"${record.category}\",")
                        appendLine("    \"count\": ${record.count},")
                        appendLine("    \"lastSearchedAt\": ${record.lastSearchedAt}")
                        appendLine("  }${if (index < _searchHistory.value.size - 1) "," else ""}")
                    }
                    appendLine("]")
                }
                historyFile.writeText(json)
            } catch (e: Exception) {
                // å¿½ç•¥ä¿å­˜å¤±è´¥
            }
        }
    }
    
    /**
     * æ›´æ–°çƒ­é—¨æœç´¢åˆ—è¡¨
     */
    private fun updateHotSearches() {
        val hotList = _searchHistory.value
            .filter { it.count >= 3 } // è‡³å°‘æœç´¢ 3 æ¬?
            .sortedByDescending { it.count }
            .take(10)
            .map { it.query }
        
        _hotSearches.value = hotList
    }
    
    /**
     * è®¡ç®—æ—¶æ•ˆæ€§åˆ†æ•?
     */
    private fun calculateRecencyScore(lastSearchedAt: Long, now: Long, oneDayMillis: Long): Double {
        val hoursAgo = (now - lastSearchedAt).toDouble() / oneDayMillis * 24
        
        return when {
            hoursAgo < 1 -> 1.0 // 1 å°æ—¶å†?
            hoursAgo < 24 -> 0.8 // 1 å¤©å†…
            hoursAgo < 168 -> 0.6 // 1 å‘¨å†…
            hoursAgo < 720 -> 0.4 // 1 æœˆå†…
            else -> 0.2 // æ›´æ—©
        }
    }
}

// ==================== æ•°æ®æ¨¡åž‹ ====================

/**
 * æœç´¢è®°å½•
 */
data class SearchRecord(
    val query: String,
    val category: String = "default",
    val count: Int = 1,
    val lastSearchedAt: Long = System.currentTimeMillis()
)

/**
 * POI åˆ†ç±»
 */
enum class POICategory(val displayName: String) {
    FOOD("Food"),
    HOTEL("Hotel"),
    SHOPPING("Shopping"),
    TRANSPORT("äº¤é€?),
    EDUCATION("Education"),
    MEDICAL("Medical"),
    ENTERTAINMENT("Entertainment"),
    DEFAULT("Default")
}

/**
 * çƒ­é—¨æœç´¢é¡?
 */
data class HotSearchItem(
    val query: String,
    val trend: SearchTrend, // ä¸Šå‡ã€ä¸‹é™ã€ç¨³å®?
    val count: Int
)

/**
 * æœç´¢è¶‹åŠ¿
 */
enum class SearchTrend {
    RISING,    // ä¸Šå‡
    FALLING,   // ä¸‹é™
    STABLE     // ç¨³å®š
}
