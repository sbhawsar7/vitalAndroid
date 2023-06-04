package com.gwl.vitalandroid.health_connect

import androidx.health.connect.client.records.SleepStageRecord
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.random.Random

fun dateTimeWithOffsetOrDefault(time: Instant, offset: ZoneOffset?): ZonedDateTime =
    if (offset != null) {
        ZonedDateTime.ofInstant(time, offset)
    } else {
        ZonedDateTime.ofInstant(time, ZoneId.systemDefault())
    }

fun randomSleepStage() = listOf(

    SleepStageRecord.STAGE_TYPE_AWAKE,
    SleepStageRecord.STAGE_TYPE_DEEP,
    SleepStageRecord.STAGE_TYPE_LIGHT,
    SleepStageRecord.STAGE_TYPE_OUT_OF_BED,
    SleepStageRecord.STAGE_TYPE_REM,
    SleepStageRecord.STAGE_TYPE_SLEEPING
).let { stages ->
    stages[Random.nextInt(stages.size)]
}
