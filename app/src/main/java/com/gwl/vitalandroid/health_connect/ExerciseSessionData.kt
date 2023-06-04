package com.gwl.vitalandroid.health_connect

import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Velocity
import java.time.Duration

data class ExerciseSessionData(
    val uid: String,
    val totalActiveTime: Duration? = null,
    var totalSteps: Long? = null,
    var totalDistance: Length? = null,
    val totalEnergyBurned: Energy? = null,
    var minHeartRate: Long? = null,
    var maxHeartRate: Long? = null,
    var avgHeartRate: Long? = null,
    val heartRateSeries: List<HeartRateRecord> = listOf(),
    val minSpeed: Velocity? = null,
    val maxSpeed: Velocity? = null,
    val avgSpeed: Velocity? = null,
    val speedRecord: List<SpeedRecord> = listOf()
)
