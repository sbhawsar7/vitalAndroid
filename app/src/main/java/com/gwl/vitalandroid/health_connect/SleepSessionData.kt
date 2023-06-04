package com.gwl.vitalandroid.health_connect

import androidx.health.connect.client.records.SleepStageRecord
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

data class SleepSessionData(
    val uid: String,
    val title: String?,
    val notes: String?,
    val startTime: Instant,
    val startZoneOffset: ZoneOffset?,
    val endTime: Instant,
    val endZoneOffset: ZoneOffset?,
    val duration: Duration?,
    val stages: List<SleepStageRecord> = listOf()
)
