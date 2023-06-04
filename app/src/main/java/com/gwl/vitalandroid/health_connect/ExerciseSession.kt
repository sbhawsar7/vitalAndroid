package com.gwl.vitalandroid.health_connect

data class ExerciseSession(
    val startTime: String = "",
    val endTime: String = "",
    val date: String = "",
    var id: String = "",
    val exerciseType: Int = 0,
    var exerciseName: String = "",
    var title: String? = "",
    val sourceAppInfo: HealthConnectAppInfo? = null,
    var exerciseSessionData: ExerciseSessionData? = null
)