package com.gwl.vitalandroid.ui.detail

import android.os.RemoteException
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.gwl.vitalandroid.base.BaseViewModel
import com.gwl.vitalandroid.health_connect.ExerciseSessionData
import com.gwl.vitalandroid.health_connect.HealthConnectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class DetailActivityViewModel @Inject constructor(
    private val healthConnectManager: HealthConnectManager,
) : BaseViewModel() {


    private var uiState = MutableLiveData<UiState>(UiState.Uninitialized)

    private val _isPermissionGranted = MutableLiveData(false)

    private val _healthSessionData = MutableLiveData<ExerciseSessionData>()
    val healthSessionData: LiveData<ExerciseSessionData> = _healthSessionData

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

    fun startToReadHealthConnectData(id: String) {
        viewModelScope.launch {
            try {
                tryWithPermissionsCheck {
                    readHealthConnectData(id)
                }
            } catch (e: Exception) {
                Log.e("TAG", " ${e.message}")
            }
        }
    }

    private suspend fun readHealthConnectData(id: String) {
        val result = healthConnectManager.readAssociatedSessionData(uid = id)
        result.totalSteps = result.totalSteps ?: 0
        result.totalDistance = result.totalDistance
        result.minHeartRate = result.minHeartRate ?: 0
        result.maxHeartRate = result.maxHeartRate ?: 0
        result.avgHeartRate = result.avgHeartRate ?: 0
        _healthSessionData.value = result
    }
}

sealed class UiState {
    object Uninitialized : UiState()

    object Done : UiState()

    data class Error(val exception: Throwable, val uuid: UUID = UUID.randomUUID()) : UiState()
}