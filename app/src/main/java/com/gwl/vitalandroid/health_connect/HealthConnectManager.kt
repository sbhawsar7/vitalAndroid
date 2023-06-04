package com.gwl.vitalandroid.health_connect

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.Resources.NotFoundException
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.changes.Change
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseEventRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.SleepStageRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Velocity
import androidx.lifecycle.MutableLiveData
import com.gwl.vitalandroid.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.text.Typography.dagger

const val MIN_SUPPORTED_SDK = Build.VERSION_CODES.O_MR1

@Singleton
class HealthConnectManager @Inject constructor(@ApplicationContext private val context: Context) {

    val permissions = setOf(
        HealthPermission.createReadPermission(ExerciseSessionRecord::class),
        HealthPermission.createWritePermission(ExerciseSessionRecord::class),

        HealthPermission.createReadPermission(ExerciseEventRecord::class),
        HealthPermission.createWritePermission(ExerciseEventRecord::class),

        HealthPermission.createReadPermission(StepsRecord::class),
        HealthPermission.createWritePermission(StepsRecord::class),

        HealthPermission.createReadPermission(SpeedRecord::class),
        HealthPermission.createWritePermission(SpeedRecord::class),

        HealthPermission.createReadPermission(DistanceRecord::class),
        HealthPermission.createWritePermission(DistanceRecord::class),

        HealthPermission.createReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.createWritePermission(TotalCaloriesBurnedRecord::class),

        HealthPermission.createReadPermission(HeartRateRecord::class),
        HealthPermission.createWritePermission(HeartRateRecord::class),

        HealthPermission.createReadPermission(SleepSessionRecord::class),
        HealthPermission.createWritePermission(SleepSessionRecord::class),

        HealthPermission.createReadPermission(WeightRecord::class),
        HealthPermission.createWritePermission(WeightRecord::class),

        HealthPermission.createReadPermission(BodyFatRecord::class),
        HealthPermission.createWritePermission(BodyFatRecord::class),

        HealthPermission.createReadPermission(BodyTemperatureRecord::class),
        HealthPermission.createWritePermission(BodyTemperatureRecord::class),

        HealthPermission.createReadPermission(HeightRecord::class),
        HealthPermission.createWritePermission(HeightRecord::class),

        HealthPermission.createReadPermission(HydrationRecord::class),
        HealthPermission.createWritePermission(HydrationRecord::class),

        HealthPermission.createReadPermission(NutritionRecord::class),
        HealthPermission.createWritePermission(NutritionRecord::class),

        HealthPermission.createReadPermission(SleepSessionRecord::class),
        HealthPermission.createWritePermission(SleepSessionRecord::class),

        HealthPermission.createReadPermission(SleepStageRecord::class),
        HealthPermission.createWritePermission(SleepStageRecord::class)
    )

    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val healthConnectCompatibleApps by lazy {
        val intent = Intent("androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE")

        // This call is deprecated in API level 33, however, this app targets a lower level.
        @Suppress("DEPRECATION")
        val packages = context.packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_ALL
        )
        packages.associate {
            val icon = try {
                context.packageManager.getApplicationIcon(it.activityInfo.packageName)
            } catch (e: NotFoundException) {
                null
            }
            val label = context.packageManager.getApplicationLabel(it.activityInfo.applicationInfo)
                .toString()
            it.activityInfo.packageName to
                    HealthConnectAppInfo(
                        packageName = it.activityInfo.packageName,
                        icon = icon,
                        appLabel = label
                    )
        }
    }

    var availability = MutableLiveData(HealthConnectAvailability.NOT_SUPPORTED)
        private set

    init {
        checkAvailability()
    }

    private fun checkAvailability() {
        availability.value = when {
            HealthConnectClient.isProviderAvailable(context) -> HealthConnectAvailability.INSTALLED
            isSupported() -> HealthConnectAvailability.NOT_INSTALLED
            else -> HealthConnectAvailability.NOT_SUPPORTED
        }
    }

    suspend fun hasAllPermissions(): Boolean {
        return permissions == healthConnectClient.permissionController.getGrantedPermissions(
            permissions
        )
    }

    fun requestPermissionsActivityContract(): ActivityResultContract<Set<HealthPermission>, Set<HealthPermission>> {
        return PermissionController.createRequestPermissionResultContract()
    }

    // region - Exercise Session Records
    suspend fun readExerciseSessions(start: Instant, end: Instant): List<ExerciseSessionRecord> {
        val request = ReadRecordsRequest(
            recordType = ExerciseSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun readAssociatedSessionData(
        uid: String
    ): ExerciseSessionData {
        val exerciseSession = healthConnectClient.readRecord(ExerciseSessionRecord::class, uid)
        // Use the start time and end time from the session, for reading raw and aggregate data.
        val timeRangeFilter = TimeRangeFilter.between(
            startTime = exerciseSession.record.startTime,
            endTime = exerciseSession.record.endTime
        )
        val aggregateDataTypes = setOf(
            ExerciseSessionRecord.EXERCISE_DURATION_TOTAL,
            StepsRecord.COUNT_TOTAL,
            DistanceRecord.DISTANCE_TOTAL,
            TotalCaloriesBurnedRecord.ENERGY_TOTAL,
            HeartRateRecord.BPM_AVG,
            HeartRateRecord.BPM_MAX,
            HeartRateRecord.BPM_MIN,
            SpeedRecord.SPEED_AVG,
            SpeedRecord.SPEED_MAX,
            SpeedRecord.SPEED_MIN
        )
        val dataOriginFilter = setOf(exerciseSession.record.metadata.dataOrigin)
        val aggregateRequest = AggregateRequest(
            metrics = aggregateDataTypes,
            timeRangeFilter = timeRangeFilter,
            dataOriginFilter = dataOriginFilter
        )
        val aggregateData = healthConnectClient.aggregate(aggregateRequest)
        val speedData = readData<SpeedRecord>(timeRangeFilter, dataOriginFilter)
        val heartRateData = readData<HeartRateRecord>(timeRangeFilter, dataOriginFilter)

        return ExerciseSessionData(
            uid = uid,
            totalActiveTime = aggregateData[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL],
            totalSteps = aggregateData[StepsRecord.COUNT_TOTAL],
            totalDistance = aggregateData[DistanceRecord.DISTANCE_TOTAL],
            totalEnergyBurned = aggregateData[TotalCaloriesBurnedRecord.ENERGY_TOTAL],
            minHeartRate = aggregateData[HeartRateRecord.BPM_MIN],
            maxHeartRate = aggregateData[HeartRateRecord.BPM_MAX],
            avgHeartRate = aggregateData[HeartRateRecord.BPM_AVG],
            heartRateSeries = heartRateData,
            speedRecord = speedData,
            minSpeed = aggregateData[SpeedRecord.SPEED_MIN],
            maxSpeed = aggregateData[SpeedRecord.SPEED_MAX],
            avgSpeed = aggregateData[SpeedRecord.SPEED_AVG],
        )
    }

    suspend fun readDistanceRecord(start: Instant, end: Instant): List<DistanceRecord> {
        val request = ReadRecordsRequest(
            recordType = DistanceRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun readSpeedRecord(start: Instant, end: Instant): List<SpeedRecord> {
        val request = ReadRecordsRequest(
            recordType = SpeedRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun readStepRecord(start: Instant, end: Instant): List<StepsRecord> {
        val request = ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun readTotalCaloriesBurnedRecord(
        start: Instant,
        end: Instant
    ): List<TotalCaloriesBurnedRecord> {
        val request = ReadRecordsRequest(
            recordType = TotalCaloriesBurnedRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }
    // endregion

    // region - Body Measurements
    suspend fun readBodyFatRecord(start: Instant, end: Instant): List<BodyFatRecord> {
        val request = ReadRecordsRequest(
            recordType = BodyFatRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun readBodyTemperatureRecord(
        start: Instant,
        end: Instant
    ): List<BodyTemperatureRecord> {
        val request = ReadRecordsRequest(
            recordType = BodyTemperatureRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun readHeightRecord(start: Instant, end: Instant): List<HeightRecord> {
        val request = ReadRecordsRequest(
            recordType = HeightRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun readHeartRateRecord(start: Instant, end: Instant): List<HeartRateRecord> {
        val request = ReadRecordsRequest(
            recordType = HeartRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun readWeightRecord(start: Instant, end: Instant): List<WeightRecord> {
        val request = ReadRecordsRequest(
            recordType = WeightRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }
    // endregion

    // region - Nutrition Records
    suspend fun readHydrationRecord(start: Instant, end: Instant): List<HydrationRecord> {
        val request = ReadRecordsRequest(
            recordType = HydrationRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun readNutritionRecord(start: Instant, end: Instant): List<NutritionRecord> {
        val request = ReadRecordsRequest(
            recordType = NutritionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun readSleepSessions(start: Instant, end: Instant): List<SleepSessionData> {
        val sessions = mutableListOf<SleepSessionData>()
        val sleepSessionRequest = ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end),
            ascendingOrder = false
        )
        val sleepSessions = healthConnectClient.readRecords(sleepSessionRequest)
        sleepSessions.records.forEach { session ->
            val sessionTimeFilter = TimeRangeFilter.between(session.startTime, session.endTime)
            val durationAggregateRequest = AggregateRequest(
                metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                timeRangeFilter = sessionTimeFilter
            )
            val aggregateResponse = healthConnectClient.aggregate(durationAggregateRequest)
            val stagesRequest = ReadRecordsRequest(
                recordType = SleepStageRecord::class,
                timeRangeFilter = sessionTimeFilter
            )
            val stagesResponse = healthConnectClient.readRecords(stagesRequest)
            sessions.add(
                SleepSessionData(
                    uid = session.metadata.id,
                    title = session.title,
                    notes = session.notes,
                    startTime = session.startTime,
                    startZoneOffset = session.startZoneOffset,
                    endTime = session.endTime,
                    endZoneOffset = session.endZoneOffset,
                    duration = aggregateResponse[SleepSessionRecord.SLEEP_DURATION_TOTAL],
                    stages = stagesResponse.records
                )
            )
        }
        return sessions
    }
    // endregion

    suspend fun writeExerciseSession(start: ZonedDateTime, end: ZonedDateTime) {
        healthConnectClient.insertRecords(
            listOf(
                ExerciseSessionRecord(
                    startTime = start.toInstant(),
                    startZoneOffset = start.offset,
                    endTime = end.toInstant(),
                    endZoneOffset = end.offset,
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                    title = "My Run #${Random.nextInt(0, 60)}"
                ),
                StepsRecord(
                    startTime = start.toInstant(),
                    startZoneOffset = start.offset,
                    endTime = end.toInstant(),
                    endZoneOffset = end.offset,
                    count = (1000 + 1000 * Random.nextInt(3)).toLong()
                ),
                // Mark a 5 minute pause during the workout
                ExerciseEventRecord(
                    startTime = start.toInstant().plus(10, ChronoUnit.MINUTES),
                    startZoneOffset = start.offset,
                    endTime = start.toInstant().plus(15, ChronoUnit.MINUTES),
                    endZoneOffset = end.offset,
                    eventType = ExerciseEventRecord.EVENT_TYPE_PAUSE
                ),
                DistanceRecord(
                    startTime = start.toInstant(),
                    startZoneOffset = start.offset,
                    endTime = end.toInstant(),
                    endZoneOffset = end.offset,
                    distance = Length.meters((1000 + 100 * Random.nextInt(20)).toDouble())
                ),
                TotalCaloriesBurnedRecord(
                    startTime = start.toInstant(),
                    startZoneOffset = start.offset,
                    endTime = end.toInstant(),
                    endZoneOffset = end.offset,
                    energy = Energy.calories((140 + Random.nextInt(20)) * 0.01)
                )
            ) + buildHeartRateSeries(start, end) + buildSpeedSeries(start, end)
        )
    }

    suspend fun deleteExerciseSession(uid: String) {
        val exerciseSession = healthConnectClient.readRecord(ExerciseSessionRecord::class, uid)
        healthConnectClient.deleteRecords(
            ExerciseSessionRecord::class,
            recordIdsList = listOf(uid),
            clientRecordIdsList = emptyList()
        )
        val timeRangeFilter = TimeRangeFilter.between(
            exerciseSession.record.startTime,
            exerciseSession.record.endTime
        )
        val rawDataTypes: Set<KClass<out Record>> = setOf(
            HeartRateRecord::class,
            SpeedRecord::class,
            DistanceRecord::class,
            StepsRecord::class,
            TotalCaloriesBurnedRecord::class,
            ExerciseEventRecord::class
        )
        rawDataTypes.forEach { rawType ->
            healthConnectClient.deleteRecords(rawType, timeRangeFilter)
        }
    }

    suspend fun deleteAllSleepData() {
        val now = Instant.now()
        healthConnectClient.deleteRecords(SleepStageRecord::class, TimeRangeFilter.before(now))
        healthConnectClient.deleteRecords(SleepSessionRecord::class, TimeRangeFilter.before(now))
    }

    suspend fun generateSleepData() {
        val records = mutableListOf<Record>()
        // Make yesterday the last day of the sleep data
        val lastDay = ZonedDateTime.now().minusDays(1).truncatedTo(ChronoUnit.DAYS)
        val notes = context.resources.getStringArray(R.array.sleep_notes_array)
        // Create 7 days-worth of sleep data
        for (i in 0..7) {
            val wakeUp = lastDay.minusDays(i.toLong())
                .withHour(Random.nextInt(7, 10))
                .withMinute(Random.nextInt(0, 60))
            val bedtime = wakeUp.minusDays(1)
                .withHour(Random.nextInt(19, 22))
                .withMinute(Random.nextInt(0, 60))
            val sleepSession = SleepSessionRecord(
                notes = notes[Random.nextInt(0, notes.size)],
                startTime = bedtime.toInstant(),
                startZoneOffset = bedtime.offset,
                endTime = wakeUp.toInstant(),
                endZoneOffset = wakeUp.offset
            )
            val sleepStages = generateSleepStages(bedtime, wakeUp)
            records.add(sleepSession)
            records.addAll(sleepStages)
        }
        healthConnectClient.insertRecords(records)
    }

    suspend fun writeWeightInput(weight: WeightRecord) {
        val records = listOf(weight)
        healthConnectClient.insertRecords(records)
    }

    suspend fun readWeightInputs(start: Instant, end: Instant): List<WeightRecord> {
        val request = ReadRecordsRequest(
            recordType = WeightRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    suspend fun computeWeeklyAverage(start: Instant, end: Instant): Mass? {
        val request = AggregateRequest(
            metrics = setOf(WeightRecord.WEIGHT_AVG),
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.aggregate(request)
        return response[WeightRecord.WEIGHT_AVG]
    }

    suspend fun deleteWeightInput(uid: String) {
        healthConnectClient.deleteRecords(
            WeightRecord::class,
            recordIdsList = listOf(uid),
            clientRecordIdsList = emptyList()
        )
    }

    suspend fun getChangesToken(dataTypes: Set<KClass<out Record>>): String {
        val request = ChangesTokenRequest(dataTypes)
        return healthConnectClient.getChangesToken(request)
    }

    suspend fun getChanges(token: String): Flow<ChangesMessage> = flow {
        var nextChangesToken = token
        do {
            val response = healthConnectClient.getChanges(nextChangesToken)
            if (response.changesTokenExpired) {
                throw IOException("Changes token has expired")
            }
            emit(ChangesMessage.ChangeList(response.changes))
            nextChangesToken = response.nextChangesToken
        } while (response.hasMore)
        emit(ChangesMessage.NoMoreChanges(nextChangesToken))
    }

    private fun generateSleepStages(
        start: ZonedDateTime,
        end: ZonedDateTime
    ): List<SleepStageRecord> {
        val sleepStages = mutableListOf<SleepStageRecord>()
        var stageStart = start
        while (stageStart < end) {
            val stageEnd = stageStart.plusMinutes(Random.nextLong(30, 120))
            val checkedEnd = if (stageEnd > end) end else stageEnd
            sleepStages.add(
                SleepStageRecord(
                    stage = randomSleepStage(),
                    startTime = stageStart.toInstant(),
                    startZoneOffset = stageStart.offset,
                    endTime = checkedEnd.toInstant(),
                    endZoneOffset = checkedEnd.offset
                )
            )
            stageStart = checkedEnd
        }
        return sleepStages
    }

    private suspend inline fun <reified T : Record> readData(
        timeRangeFilter: TimeRangeFilter,
        dataOriginFilter: Set<DataOrigin> = setOf()
    ): List<T> {
        val request = ReadRecordsRequest(
            recordType = T::class,
            dataOriginFilter = dataOriginFilter,
            timeRangeFilter = timeRangeFilter
        )
        return healthConnectClient.readRecords(request).records
    }

    private fun buildHeartRateSeries(
        sessionStartTime: ZonedDateTime,
        sessionEndTime: ZonedDateTime
    ): HeartRateRecord {
        val samples = mutableListOf<HeartRateRecord.Sample>()
        var time = sessionStartTime
        while (time.isBefore(sessionEndTime)) {
            samples.add(
                HeartRateRecord.Sample(
                    time = time.toInstant(),
                    beatsPerMinute = (80 + Random.nextInt(80)).toLong()
                )
            )
            time = time.plusSeconds(30)
        }
        return HeartRateRecord(
            startTime = sessionStartTime.toInstant(),
            startZoneOffset = sessionStartTime.offset,
            endTime = sessionEndTime.toInstant(),
            endZoneOffset = sessionEndTime.offset,
            samples = samples
        )
    }

    private fun buildSpeedSeries(
        sessionStartTime: ZonedDateTime,
        sessionEndTime: ZonedDateTime
    ) = SpeedRecord(
        startTime = sessionStartTime.toInstant(),
        startZoneOffset = sessionStartTime.offset,
        endTime = sessionEndTime.toInstant(),
        endZoneOffset = sessionEndTime.offset,
        samples = listOf(
            SpeedRecord.Sample(
                time = sessionStartTime.toInstant(),
                speed = Velocity.metersPerSecond(2.5)
            ),
            SpeedRecord.Sample(
                time = sessionStartTime.toInstant().plus(5, ChronoUnit.MINUTES),
                speed = Velocity.metersPerSecond(2.7)
            ),
            SpeedRecord.Sample(
                time = sessionStartTime.toInstant().plus(10, ChronoUnit.MINUTES),
                speed = Velocity.metersPerSecond(2.9)
            )
        )
    )

    private fun isSupported() = Build.VERSION.SDK_INT >= MIN_SUPPORTED_SDK

    sealed class ChangesMessage {
        data class NoMoreChanges(val nextChangesToken: String) : ChangesMessage()
        data class ChangeList(val changes: List<Change>) : ChangesMessage()
    }
}

enum class HealthConnectAvailability {
    INSTALLED,
    NOT_INSTALLED,
    NOT_SUPPORTED
}

fun Int.getActivityName(provider: Resources): String {
    return when (this) {
        ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT -> provider.getString(R.string.swimming)
        ExerciseSessionRecord.EXERCISE_TYPE_BACK_EXTENSION -> provider.getString(R.string.back_extension)
        ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON -> provider.getString(R.string.badminton)
        ExerciseSessionRecord.EXERCISE_TYPE_BARBELL_SHOULDER_PRESS -> provider.getString(R.string.shoulder_press)
        ExerciseSessionRecord.EXERCISE_TYPE_BASEBALL -> provider.getString(R.string.baseball)
        ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL -> provider.getString(R.string.basketball)
        ExerciseSessionRecord.EXERCISE_TYPE_BENCH_PRESS -> provider.getString(R.string.bench_press)
        ExerciseSessionRecord.EXERCISE_TYPE_BENCH_SIT_UP -> provider.getString(R.string.bench_sit_up)
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> provider.getString(R.string.biking)
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> provider.getString(R.string.biking_stationary)
        ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP -> provider.getString(R.string.boot_camp)
        ExerciseSessionRecord.EXERCISE_TYPE_BOXING -> provider.getString(R.string.boxing)
        ExerciseSessionRecord.EXERCISE_TYPE_BURPEE -> provider.getString(R.string.burpee)
        ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS -> provider.getString(R.string.calesthenics)
        ExerciseSessionRecord.EXERCISE_TYPE_CRICKET -> provider.getString(R.string.cricket)
        ExerciseSessionRecord.EXERCISE_TYPE_CRUNCH -> provider.getString(R.string.crunh)
        ExerciseSessionRecord.EXERCISE_TYPE_DANCING -> provider.getString(R.string.dancing)
        ExerciseSessionRecord.EXERCISE_TYPE_DEADLIFT -> provider.getString(R.string.dead_lift)
        ExerciseSessionRecord.EXERCISE_TYPE_DUMBBELL_CURL_LEFT_ARM -> provider.getString(R.string.dumbell_curl_left_arm)
        ExerciseSessionRecord.EXERCISE_TYPE_DUMBBELL_CURL_RIGHT_ARM -> provider.getString(R.string.dumbell_curl_right_arm)
        ExerciseSessionRecord.EXERCISE_TYPE_DUMBBELL_FRONT_RAISE -> provider.getString(R.string.front_raise)
        ExerciseSessionRecord.EXERCISE_TYPE_DUMBBELL_LATERAL_RAISE -> provider.getString(R.string.lateral_raise)
        ExerciseSessionRecord.EXERCISE_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM -> provider.getString(
            R.string.triceps_extension_left_arm
        )
        ExerciseSessionRecord.EXERCISE_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM -> provider.getString(
            R.string.triceps_extension_right_arm
        )
        ExerciseSessionRecord.EXERCISE_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM -> provider.getString(
            R.string.triceps_extension_two_arm
        )
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> provider.getString(R.string.elliptical)
        ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS -> provider.getString(R.string.elliptical_class)
        ExerciseSessionRecord.EXERCISE_TYPE_FENCING -> provider.getString(R.string.type_fencing)
        ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN -> provider.getString(R.string.american_football)
        ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AUSTRALIAN -> provider.getString(R.string.australian_football)
        ExerciseSessionRecord.EXERCISE_TYPE_FORWARD_TWIST -> provider.getString(R.string.forward_twist)
        ExerciseSessionRecord.EXERCISE_TYPE_FRISBEE_DISC -> provider.getString(R.string.frisbee_disc)
        ExerciseSessionRecord.EXERCISE_TYPE_GOLF -> provider.getString(R.string.golf)
        ExerciseSessionRecord.EXERCISE_TYPE_GUIDED_BREATHING -> provider.getString(R.string.guided_breathing)
        ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS -> provider.getString(R.string.gymnastic)
        ExerciseSessionRecord.EXERCISE_TYPE_HANDBALL -> provider.getString(R.string.handball)
        ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> provider.getString(
            R.string.high_intensity_internal_training
        )
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> provider.getString(R.string.hiking)
        ExerciseSessionRecord.EXERCISE_TYPE_ICE_HOCKEY -> provider.getString(R.string.ice_hockey)
        ExerciseSessionRecord.EXERCISE_TYPE_ICE_SKATING -> provider.getString(R.string.ice_skating)
        ExerciseSessionRecord.EXERCISE_TYPE_JUMPING_JACK -> provider.getString(R.string.jumping_jack)
        ExerciseSessionRecord.EXERCISE_TYPE_JUMP_ROPE -> provider.getString(R.string.jump_rope)
        ExerciseSessionRecord.EXERCISE_TYPE_LAT_PULL_DOWN -> provider.getString(R.string.lat_pull_down)
        ExerciseSessionRecord.EXERCISE_TYPE_LUNGE -> provider.getString(R.string.lunge)
        ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS -> provider.getString(R.string.martial_arts)
        ExerciseSessionRecord.EXERCISE_TYPE_PADDLING -> provider.getString(R.string.paddling)
        ExerciseSessionRecord.EXERCISE_TYPE_PARAGLIDING -> provider.getString(R.string.paragliding)
        ExerciseSessionRecord.EXERCISE_TYPE_PILATES -> provider.getString(R.string.pilates)
        ExerciseSessionRecord.EXERCISE_TYPE_PLANK -> provider.getString(R.string.plank)
        ExerciseSessionRecord.EXERCISE_TYPE_RACQUETBALL -> provider.getString(R.string.racquetball)
        ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING -> provider.getString(R.string.rock_climbing)
        ExerciseSessionRecord.EXERCISE_TYPE_ROLLER_HOCKEY -> provider.getString(R.string.roller_hockey)
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING -> provider.getString(R.string.rowing)
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> provider.getString(R.string.rowing_machine)
        ExerciseSessionRecord.EXERCISE_TYPE_RUGBY -> provider.getString(R.string.rugby)
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> provider.getString(R.string.running)
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> provider.getString(R.string.running_tradmill)
        ExerciseSessionRecord.EXERCISE_TYPE_SAILING -> provider.getString(R.string.sailing)
        ExerciseSessionRecord.EXERCISE_TYPE_SCUBA_DIVING -> provider.getString(R.string.scuba_diving)
        ExerciseSessionRecord.EXERCISE_TYPE_SKATING -> provider.getString(R.string.skating)
        ExerciseSessionRecord.EXERCISE_TYPE_SKIING -> provider.getString(R.string.skiting)
        ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING -> provider.getString(R.string.snowboarding)
        ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING -> provider.getString(R.string.snowshoeing)
        ExerciseSessionRecord.EXERCISE_TYPE_SOCCER -> provider.getString(R.string.soccer)
        ExerciseSessionRecord.EXERCISE_TYPE_SOFTBALL -> provider.getString(R.string.softball)
        ExerciseSessionRecord.EXERCISE_TYPE_SQUASH -> provider.getString(R.string.squash)
        ExerciseSessionRecord.EXERCISE_TYPE_SQUAT -> provider.getString(R.string.squat)
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING -> provider.getString(R.string.stair_climbing)
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE -> provider.getString(R.string.stair_climbing_machine)
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> provider.getString(R.string.strength_training)
        ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING -> provider.getString(R.string.streching)
        ExerciseSessionRecord.EXERCISE_TYPE_SURFING -> provider.getString(R.string.surfing)
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> provider.getString(R.string.swimming)
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> provider.getString(R.string.swimming)
        ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS -> provider.getString(R.string.table_tennis)
        ExerciseSessionRecord.EXERCISE_TYPE_TENNIS -> provider.getString(R.string.tennis)
        ExerciseSessionRecord.EXERCISE_TYPE_UPPER_TWIST -> provider.getString(R.string.upper_twist)
        ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL -> provider.getString(R.string.volleyball)
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> provider.getString(R.string.walking)
        ExerciseSessionRecord.EXERCISE_TYPE_WATER_POLO -> provider.getString(R.string.water_polo)
        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> provider.getString(R.string.weightlifting)
        ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR -> provider.getString(R.string.wheel_chair)
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> provider.getString(R.string.yoga)
        else -> ""
    }
}