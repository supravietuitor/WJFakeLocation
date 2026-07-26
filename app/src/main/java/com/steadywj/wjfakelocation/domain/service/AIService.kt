// AIService.kt
package com.steadywj.wjfakelocation.domain.service

import android.content.Context
import android.location.Location
import com.steadywj.wjfakelocation.data.model.FavoriteLocation
import com.steadywj.wjfakelocation.data.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI intelligent service
 */
@Singleton
class AIService @Inject constructor(
    private val context: Context,
    private val favoritesRepository: FavoritesRepository
) {

    private val _userPatterns = MutableStateFlow(UserBehaviorPatterns())
    val userPatterns: Flow<UserBehaviorPatterns> = _userPatterns.asStateFlow()

    private val _predictions = MutableStateFlow<List<LocationPrediction>>(emptyList())
    val predictions: Flow<List<LocationPrediction>> = _predictions.asStateFlow()

    suspend fun recordLocationVisit(location: FavoriteLocation, timestamp: Long) {
        val patterns = _userPatterns.value.copy(
            visitedLocations = _userPatterns.value.visitedLocations + LocationVisit(
                latitude = location.latitude,
                longitude = location.longitude,
                name = location.name,
                category = location.category,
                timestamp = timestamp,
                visitCount = 1
            )
        )

        val mergedLocations = mergeDuplicateVisits(patterns.visitedLocations)
        _userPatterns.value = patterns.copy(visitedLocations = mergedLocations)

        updatePredictions()
    }

    suspend fun learnUserHabits() {
        val patterns = _userPatterns.value

        val timePatterns = analyzeTimePatterns(patterns.visitedLocations)

        val locationPreferences = analyzeLocationPreferences(patterns.visitedLocations)

        _userPatterns.value = patterns.copy(
            timePatterns = timePatterns,
            locationPreferences = locationPreferences
        )
    }

    fun predictDestination(currentLocation: Location, currentTime: Long): List<LocationPrediction> {
        val patterns = _userPatterns.value

        return patterns.visitedLocations
            .map { visit ->
                val probability = calculateProbability(visit, currentTime, currentLocation)
                LocationPrediction(
                    name = visit.name,
                    latitude = visit.latitude,
                    longitude = visit.longitude,
                    probability = probability,
                    reason = generateReason(visit, currentTime)
                )
            }
            .filter { it.probability > 0.3 }
            .sortedByDescending { it.probability }
    }

    suspend fun checkAutoTrigger(currentLocation: Location): AutoTriggerResult? {
        val patterns = _userPatterns.value

        val nearbyLocation = patterns.visitedLocations.find { visit ->
            val distance = calculateDistance(
                currentLocation.latitude,
                currentLocation.longitude,
                visit.latitude,
                visit.longitude
            )
            distance < 100.0
        } ?: return null

        val autoRule = patterns.autoRules.find { rule ->
            rule.locationName == nearbyLocation.name
        } ?: return null

        return AutoTriggerResult(
            action = autoRule.action,
            locationName = nearbyLocation.name,
            confidence = 0.9
        )
    }

    suspend fun addAutoRule(rule: AutomationRule) {
        val patterns = _userPatterns.value
        _userPatterns.value = patterns.copy(
            autoRules = patterns.autoRules + rule
        )
    }

    suspend fun clearLearningData() {
        _userPatterns.value = UserBehaviorPatterns()
        _predictions.value = emptyList()
    }

    private fun updatePredictions() {
        val patterns = _userPatterns.value
        val preds = patterns.visitedLocations
            .groupBy { "${it.latitude},${it.longitude}" }
            .map { (_, group) ->
                val first = group.first()
                val totalVisits = group.sumOf { it.visitCount }
                val probability = kotlin.math.min(totalVisits.toDouble() / 10.0, 1.0)
                LocationPrediction(
                    name = first.name,
                    latitude = first.latitude,
                    longitude = first.longitude,
                    probability = probability,
                    reason = "Based on ${totalVisits} visit(s)"
                )
            }
            .filter { it.probability > 0.3 }
            .sortedByDescending { it.probability }
        _predictions.value = preds
    }

    private fun mergeDuplicateVisits(visits: List<LocationVisit>): List<LocationVisit> {
        return visits.groupBy { "${it.latitude},${it.longitude}" }
            .map { (_, group) ->
                val first = group.first()
                LocationVisit(
                    latitude = first.latitude,
                    longitude = first.longitude,
                    name = first.name,
                    category = first.category,
                    timestamp = group.maxOf { it.timestamp },
                    visitCount = group.sumOf { it.visitCount }
                )
            }
    }

    private fun analyzeTimePatterns(visits: List<LocationVisit>): TimePatterns {
        val hourDistribution = IntArray(24)
        val dayDistribution = IntArray(7)

        visits.forEach { visit ->
            val hour = java.util.Calendar.getInstance().apply {
                timeInMillis = visit.timestamp
            }.get(java.util.Calendar.HOUR_OF_DAY)

            val dayOfWeek = java.util.Calendar.getInstance().apply {
                timeInMillis = visit.timestamp
            }.get(java.util.Calendar.DAY_OF_WEEK) - 1

            hourDistribution[hour]++
            dayDistribution[dayOfWeek]++
        }

        return TimePatterns(
            peakHours = hourDistribution.indices.maxByOrNull { hourDistribution[it] } ?: 9,
            weekendActivityRatio = (dayDistribution[6] + dayDistribution[0]).toFloat() /
                (dayDistribution.slice(1..5).sum() + 1)
        )
    }

    private fun analyzeLocationPreferences(visits: List<LocationVisit>): LocationPreferences {
        val categoryCount = visits.groupingBy { it.category }.eachCount()
        val topCategory = categoryCount.maxByOrNull { it.value }?.key ?: "default"

        return LocationPreferences(
            favoriteCategories = categoryCount.keys.sortedByDescending { categoryCount[it] },
            mostVisitedLocation = visits.maxByOrNull { it.visitCount }?.name ?: ""
        )
    }

    private fun calculateProbability(
        visit: LocationVisit,
        currentTime: Long,
        currentLocation: Location
    ): Double {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = currentTime
        }
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentDay = calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1

        val baseProbability = kotlin.math.min(visit.visitCount.toDouble() / 10.0, 1.0)

        val timeMatch = if (kotlin.math.abs(currentHour - visit.timestamp % 86400000 / 3600000) < 2) 1.0 else 0.5

        val dayMatch = if (currentDay == 0 || currentDay == 6) 0.8 else 0.6

        val distance = calculateDistance(
            currentLocation.latitude,
            currentLocation.longitude,
            visit.latitude,
            visit.longitude
        )
        val distanceDecay = kotlin.math.exp(-distance / 5000.0)

        return baseProbability * timeMatch * dayMatch * distanceDecay
    }

    private fun generateReason(visit: LocationVisit, currentTime: Long): String {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = currentTime
        }
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)

        return when {
            visit.visitCount > 5 -> "Frequently visited location (${visit.visitCount} times)"
            hour in 7..9 -> "Likely destination during commute to work"
            hour in 17..19 -> "Likely destination during commute from work"
            else -> "Recommended based on your habits"
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c
    }
}

data class UserBehaviorPatterns(
    val visitedLocations: List<LocationVisit> = emptyList(),
    val timePatterns: TimePatterns = TimePatterns(),
    val locationPreferences: LocationPreferences = LocationPreferences(),
    val autoRules: List<AutomationRule> = emptyList()
)

data class LocationVisit(
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val category: String,
    val timestamp: Long,
    val visitCount: Int
)

data class TimePatterns(
    val peakHours: Int = 9,
    val weekendActivityRatio: Float = 0.5f
)

data class LocationPreferences(
    val favoriteCategories: List<String> = emptyList(),
    val mostVisitedLocation: String = ""
)

data class LocationPrediction(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val probability: Double,
    val reason: String
)

data class AutomationRule(
    val locationName: String,
    val action: AutomationAction,
    val enabled: Boolean = true
)

enum class AutomationAction {
    ENABLE_FAKE_LOCATION,
    DISABLE_FAKE_LOCATION,
    SWITCH_PROFILE,
    NOTIFY_USER
}

data class AutoTriggerResult(
    val action: AutomationAction,
    val locationName: String,
    val confidence: Double
)
