package com.gwl.vitalandroid.ui.main

import android.content.res.Resources
import android.os.RemoteException
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.gwl.vitalandroid.base.BaseViewModel
import com.gwl.vitalandroid.health_connect.ExerciseSession
import com.gwl.vitalandroid.health_connect.HealthConnectManager
import com.gwl.vitalandroid.health_connect.dateTimeWithOffsetOrDefault
import com.gwl.vitalandroid.health_connect.getActivityName
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    val healthConnectManager: HealthConnectManager,
) : BaseViewModel(), OnItemClickListener {

    private var uiState = MutableLiveData<UiState>(UiState.Uninitialized)

    private val _isPermissionGranted = MutableLiveData(false)
    val isPermissionGranted: LiveData<Boolean> = _isPermissionGranted

    private val _healthActivities = MutableLiveData<List<ExerciseSession>>()
    val healthActivities: LiveData<List<ExerciseSession>> = _healthActivities

    private val _onActivityClick = MutableLiveData<ExerciseSession>()
    val onActivityClick: LiveData<ExerciseSession> = _onActivityClick

    private suspend fun tryWithPermissionsCheck(block: suspend () -> Unit) {
        _isPermissionGranted.value = healthConnectManager.hasAllPermissions()
        uiState.value = try {
            if (_isPermissionGranted.value == true) {
                block()
            }
            UiState.Done
        } catch (remoteException: RemoteException) {
            UiState.Error(remoteException)
        } catch (securityException: SecurityException) {
            UiState.Error(securityException)
        } catch (ioException: IOException) {
            UiState.Error(ioException)
        } catch (illegalStateException: IllegalStateException) {
            UiState.Error(illegalStateException)
        }
    }

    fun startToReadHealthConnectData(resources: Resources) {
        viewModelScope.launch {
            try {
                tryWithPermissionsCheck {
                    readHealthConnectData(resources)
                }
            } catch (e: Exception) {
                Log.e("TAG", " ${e.message}")
            }
        }
    }

    private suspend fun readHealthConnectData(resources: Resources) {
        val startMillis: Long = Calendar.getInstance(TimeZone.getDefault()).run {
            set(2023, 0, 1, 0, 0)
            timeInMillis
        }

        val endMillis: Long = Calendar.getInstance(TimeZone.getDefault()).run { timeInMillis }

        // region - Activity record
        val list = arrayListOf<ExerciseSession>()
        //---- Get activity
        val exerciseSessions = healthConnectManager.readExerciseSessions(
            start = Instant.ofEpochMilli(startMillis), end = Instant.ofEpochMilli(endMillis)
        )
        exerciseSessions.forEach { sessionRecord ->
            val exerciseSessionData =
                healthConnectManager.readAssociatedSessionData(uid = sessionRecord.metadata.id)
            list.add(
                ExerciseSession(
                    startTime = dateTimeWithOffsetOrDefault(
                        sessionRecord.startTime,
                        sessionRecord.startZoneOffset
                    ).toLocalTime().format(DateTimeFormatter.ofPattern("hh:mm")),
                    endTime = dateTimeWithOffsetOrDefault(
                        sessionRecord.endTime,
                        sessionRecord.startZoneOffset
                    ).toLocalTime().format(DateTimeFormatter.ofPattern("hh:mm")),
                    date = dateTimeWithOffsetOrDefault(
                        sessionRecord.endTime,
                        sessionRecord.startZoneOffset
                    ).toLocalDate().toString(),
                    id = sessionRecord.metadata.id,
                    exerciseType = sessionRecord.exerciseType,
                    exerciseName = sessionRecord.exerciseType.getActivityName(provider = resources),
                    sourceAppInfo = healthConnectManager
                        .healthConnectCompatibleApps[sessionRecord.metadata.dataOrigin.packageName],
                    title = sessionRecord.title,
                    exerciseSessionData = exerciseSessionData
                )
            )
        }
        // endregion

        _healthActivities.value = list
    }

    override fun onItemClick(item: ExerciseSession) {
        _onActivityClick.value = item
    }
}

sealed class UiState {
    object Uninitialized : UiState()

    object Done : UiState()

    data class Error(val exception: Throwable, val uuid: UUID = UUID.randomUUID()) : UiState()
}