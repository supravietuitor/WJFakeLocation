// DatabaseModule.kt
package com.steadywj.wjfakelocation.di

import android.content.Context
import androidx.room.Room
import com.steadywj.wjfakelocation.data.local.AppDatabase
import com.steadywj.wjfakelocation.data.local.FavoriteLocationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "wjfakelocation_database"
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideFavoriteLocationDao(database: AppDatabase): FavoriteLocationDao {
        return database.favoriteLocationDao()
    }
}
