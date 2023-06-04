package com.gwl.vitalandroid.health_connect

import android.content.res.Resources
import com.gwl.vitalandroid.R

data class HealthKitActivity(
    val sessionRecord: ExerciseSession,
    val sessionData: ExerciseSessionData
)

data class HealthKitActivityModel(
    val id: Int,
    val name: String,
    val date: String,
    var distance: Double,
    val distanceUnit: String,
    val activityType: String,
    var energyBurntValue: Double,
    val energyBurntType: String,
    var steps: Long,
)

fun Map<Int, ArrayList<HealthKitActivity>>.getHealthDataSubCategoryOfActivities(provider: Resources): List<HealthKitActivityModel> {
    val resultList: ArrayList<HealthKitActivityModel> = arrayListOf()
    this.forEach { (_, healthKitActivities) ->
        val aggregateObj = HealthKitActivityModel(
            id = healthKitActivities.first().sessionRecord.exerciseType,
            name = healthKitActivities.first().sessionRecord.exerciseType.getActivityName(provider = provider),
            date = "",
            distance = 0.0,
            distanceUnit = provider.getString(R.string.km),
            activityType = "",
            energyBurntValue = 0.0,
            energyBurntType = provider.getString(R.string.kc),
            steps = 0,
        )
        healthKitActivities.forEach { hkActivity ->
            aggregateObj.apply {
                this.distance += hkActivity.sessionData.totalDistance?.inKilometers?.roundTo(1)
                    ?: 0.0
                this.energyBurntValue += hkActivity.sessionData.totalEnergyBurned?.inKilocalories?.roundTo(
                    1
                ) ?: 0.0
                this.steps += hkActivity.sessionData.totalSteps ?: 0
            }
        }
        resultList.add(aggregateObj)
    }
    return resultList
}

fun Double.roundTo(n: Int): Double {
    return "%.${n}f".format(this).toDouble()
}