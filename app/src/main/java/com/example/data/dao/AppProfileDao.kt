package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AppProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface AppProfileDao {

    @Query("SELECT * FROM app_profiles ORDER BY lastUsedTimestamp DESC")
    fun getAllProfilesFlow(): Flow<List<AppProfile>>

    @Query("SELECT * FROM app_profiles ORDER BY lastUsedTimestamp DESC")
    suspend fun getAllProfiles(): List<AppProfile>

    @Query("SELECT * FROM app_profiles WHERE packageName = :packageName LIMIT 1")
    suspend fun getProfile(packageName: String): AppProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: AppProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfiles(profiles: List<AppProfile>)

    @Query("UPDATE app_profiles SET boostPercent = :boostPercent, lastUsedTimestamp = :timestamp WHERE packageName = :packageName")
    suspend fun updateBoost(packageName: String, boostPercent: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE app_profiles SET isEnabled = :isEnabled WHERE packageName = :packageName")
    suspend fun updateEnabled(packageName: String, isEnabled: Boolean)

    @Query("UPDATE app_profiles SET boostPercent = 100")
    suspend fun resetAllBoostsToNormal()

    @Query("DELETE FROM app_profiles WHERE packageName = :packageName")
    suspend fun deleteProfile(packageName: String)
}
