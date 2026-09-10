package com.example.data.repository

import com.example.data.dao.AppProfileDao
import com.example.data.model.AppProfile
import kotlinx.coroutines.flow.Flow

class AppProfileRepository(private val dao: AppProfileDao) {

    val allProfiles: Flow<List<AppProfile>> = dao.getAllProfilesFlow()

    suspend fun getAllProfiles(): List<AppProfile> = dao.getAllProfiles()

    suspend fun getProfile(packageName: String): AppProfile? = dao.getProfile(packageName)

    suspend fun saveProfile(profile: AppProfile) {
        dao.upsertProfile(profile)
    }

    suspend fun updateBoost(packageName: String, boostPercent: Int) {
        val clamped = boostPercent.coerceIn(0, 300)
        dao.updateBoost(packageName, clamped)
    }

    suspend fun updateEnabled(packageName: String, isEnabled: Boolean) {
        dao.updateEnabled(packageName, isEnabled)
    }

    suspend fun resetAllBoosts() {
        dao.resetAllBoostsToNormal()
    }

    suspend fun deleteProfile(packageName: String) {
        dao.deleteProfile(packageName)
    }
}
