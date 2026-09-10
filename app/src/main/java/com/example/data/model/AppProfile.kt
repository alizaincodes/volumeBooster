package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_profiles")
data class AppProfile(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val boostPercent: Int = 100,
    val isEnabled: Boolean = true,
    val lastUsedTimestamp: Long = System.currentTimeMillis(),
    val iconCachePath: String? = null
)
