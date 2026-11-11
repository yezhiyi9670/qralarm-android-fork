package com.sweak.qralarm.features.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sweak.qralarm.R
import com.sweak.qralarm.core.domain.alarm.Alarm
import com.sweak.qralarm.core.domain.alarm.AlarmsRepository
import com.sweak.qralarm.core.domain.alarm.DisableAlarm
import com.sweak.qralarm.core.domain.alarm.SetAlarm
import com.sweak.qralarm.core.domain.alarm.ScheduleChainedAlarm
import com.sweak.qralarm.core.domain.alarm.SnoozeAlarm
import com.sweak.qralarm.core.domain.alarm.isSnoozeAvailable
import com.sweak.qralarm.core.ui.compose_util.UiText
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = AlarmViewModel.Factory::class)
class AlarmViewModel @AssistedInject constructor(
    @Assisted private val idOfAlarm: Long,
    @Assisted private val isTransient: Boolean,
    private val alarmsRepository: AlarmsRepository,
    private val snoozeAlarm: SnoozeAlarm,
    private val setAlarm: SetAlarm,
    private val disableAlarm: DisableAlarm,
    private val scheduleChainedAlarm: ScheduleChainedAlarm
) : ViewModel() {

    private lateinit var alarm: Alarm
    private var isAlarmBeingStopped: Boolean = false
    private var activeChainedAlarmId: Long? = null
    private var currentAlarmId: Long = idOfAlarm
    private var isTransientMode: Boolean = isTransient
    private var alarmObservationJob: Job? = null

    @AssistedFactory
    interface Factory {
        fun create(idOfAlarm: Long, isTransient: Boolean): AlarmViewModel
    }

    private var _state = MutableStateFlow(AlarmScreenState())
    val state = _state.asStateFlow()

    private val backendEventsChannel = Channel<AlarmScreenBackendEvent>()
    val backendEvents = backendEventsChannel.receiveAsFlow()

    init {
        observeAlarm(alarmId = currentAlarmId)
    }

    /**
     * Called when the screen is handed over to another alarm that started ringing while this
     * [ViewModel] was already alive.
     */
    fun onAlarmChanged(alarmId: Long, isTransientScreen: Boolean) {
        isTransientMode = isTransientScreen

        if (currentAlarmId == alarmId) return

        currentAlarmId = alarmId
        isAlarmBeingStopped = false
        activeChainedAlarmId = null
        _state.value = AlarmScreenState()

        observeAlarm(alarmId = alarmId)
    }

    private fun observeAlarm(alarmId: Long) {
        alarmObservationJob?.cancel()
        alarmObservationJob = viewModelScope.launch {
            alarmsRepository.getAlarmFlow(alarmId = alarmId).collect {
                alarm = it
                it.chainedAlarms.firstOrNull { chainedAlarm ->
                    chainedAlarm.nextChainedAlarmTimeInMillis != null
                }?.let { chainedAlarm ->
                    activeChainedAlarmId = chainedAlarm.chainedAlarmId
                }

                if (!isAlarmBeingStopped) {
                    val isAlarmSnoozed = it.snoozeConfig.isAlarmSnoozed
                    val isSnoozeAvailable = it.isSnoozeAvailable()
                    val isAlarmRunning = it.isAlarmRunning
                    val isInteractionEnabled = isAlarmRunning || (isAlarmSnoozed && !isTransientMode)
                    val activeChainedAlarm = it.chainedAlarms.firstOrNull { chainedAlarm ->
                        chainedAlarm.chainedAlarmId == activeChainedAlarmId
                    }
                    val isUsingCode = it.isUsingCode ||
                        activeChainedAlarm?.assignedCode != null
                    val alarmLabel = if (isAlarmRunning) {
                        if (activeChainedAlarm != null) {
                            if (activeChainedAlarm.label != null) {
                                UiText.DynamicString(activeChainedAlarm.label)
                            } else {
                                UiText.StringResource(
                                    R.string.alarm_in_chain,
                                    it.chainedAlarms.indexOf(activeChainedAlarm) + 1
                                )
                            }
                        } else if (it.alarmLabel != null) {
                            UiText.DynamicString(it.alarmLabel)
                        } else {
                            UiText.StringResource(resId = R.string.alarm_wake_up)
                        }
                    } else if (isAlarmSnoozed) {
                        UiText.StringResource(resId = R.string.alarm_snoozed_until)
                    } else null
                    val codeName = if (isAlarmRunning || isAlarmSnoozed) {
                        activeChainedAlarm?.assignedCode?.name
                            ?: if (it.isUsingCode) it.assignedCode?.name else null
                    } else null
                    val timeToShow =
                        if (isAlarmSnoozed && it.snoozeConfig.nextSnoozedAlarmTimeInMillis != null) {
                            it.snoozeConfig.nextSnoozedAlarmTimeInMillis
                        } else {
                            System.currentTimeMillis()
                        }

                    _state.update { currentState ->
                        currentState.copy(
                            alarmLabel = alarmLabel,
                            codeName = codeName,
                            timeToShow = timeToShow,
                            isAlarmSnoozed = isAlarmSnoozed,
                            isSnoozeAvailable = isSnoozeAvailable,
                            isInteractionEnabled = isInteractionEnabled,
                            isEmergencyAvailable =
                                isUsingCode && it.isEmergencyTaskEnabled
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: AlarmScreenUserEvent) {
        when (event) {
            is AlarmScreenUserEvent.TryStopAlarm -> {
                val isUsingCode = ::alarm.isInitialized &&
                    (alarm.isUsingCode || alarm.chainedAlarms.any { chainedAlarm ->
                        chainedAlarm.chainedAlarmId == activeChainedAlarmId &&
                            chainedAlarm.assignedCode != null
                    })

                if (!isUsingCode) {
                    viewModelScope.launch {
                        isAlarmBeingStopped = true
                        alarmsRepository.setAlarmSnoozed(
                            alarmId = currentAlarmId,
                            snoozed = false
                        )
                        handleAlarmRescheduling()

                        backendEventsChannel.send(AlarmScreenBackendEvent.StopAlarm)
                    }
                    return
                }

                _state.update { currentState ->
                    if (currentState.permissionsDialogState.isVisible) {
                        if (currentState.permissionsDialogState.cameraPermissionState == true) {
                            viewModelScope.launch {
                                backendEventsChannel.send(
                                    AlarmScreenBackendEvent.RequestCodeScanToStopAlarm
                                )
                            }

                            return@update currentState.copy(
                                permissionsDialogState =
                                    AlarmScreenState.PermissionsDialogState(
                                        isVisible = false
                                    )
                            )
                        } else {
                            return@update currentState.copy(
                                permissionsDialogState = currentState.permissionsDialogState.copy(
                                    cameraPermissionState =
                                        currentState.permissionsDialogState.cameraPermissionState?.let {
                                            event.cameraPermissionStatus
                                        }
                                )
                            )
                        }
                    }

                    if (!event.cameraPermissionStatus) {
                        if (alarm.temporaryMuteDurationInSeconds > 0) {
                            viewModelScope.launch {
                                backendEventsChannel.send(
                                    AlarmScreenBackendEvent.TryTemporarilyMuteAlarm(
                                        muteDurationInSeconds = alarm.temporaryMuteDurationInSeconds
                                    )
                                )
                            }
                        }

                        return@update currentState.copy(
                            permissionsDialogState =
                                AlarmScreenState.PermissionsDialogState(
                                    isVisible = true,
                                    cameraPermissionState = false
                                )
                        )
                    }

                    viewModelScope.launch {
                        if (alarm.temporaryMuteDurationInSeconds > 0) {
                            backendEventsChannel.send(
                                AlarmScreenBackendEvent.TryTemporarilyMuteAlarm(
                                    muteDurationInSeconds = alarm.temporaryMuteDurationInSeconds
                                )
                            )
                        }

                        backendEventsChannel.send(
                            AlarmScreenBackendEvent.RequestCodeScanToStopAlarm
                        )
                    }

                    return@update currentState
                }
            }

            is AlarmScreenUserEvent.SnoozeAlarmClicked -> {
                viewModelScope.launch {
                    _state.update { currentState ->
                        currentState.copy(isInteractionEnabled = !isTransientMode)
                    }

                    val result = snoozeAlarm(
                        alarmId = currentAlarmId,
                        isReschedulingCurrentOrMissedSnooze = false
                    )

                    if (result is SnoozeAlarm.Result.NotAvailable) {
                        _state.update { currentState ->
                            currentState.copy(isInteractionEnabled = true)
                        }
                        return@launch
                    }

                    backendEventsChannel.send(AlarmScreenBackendEvent.SnoozeAlarm)
                }
            }

            is AlarmScreenUserEvent.HideMissingPermissionsDialog -> {
                _state.update { currentState ->
                    currentState.copy(
                        permissionsDialogState = AlarmScreenState.PermissionsDialogState(
                            isVisible = false
                        )
                    )
                }
            }

            is AlarmScreenUserEvent.CameraPermissionDeniedDialogVisible -> {
                _state.update { currentState ->
                    currentState.copy(isCameraPermissionDeniedDialogVisible = event.isVisible)
                }
            }

            is AlarmScreenUserEvent.UpdateCurrentTime -> {
                if (!_state.value.isAlarmSnoozed) {
                    _state.update { currentState ->
                        currentState.copy(timeToShow = System.currentTimeMillis())
                    }
                }
            }

            else -> { /* no-op */
            }
        }
    }

    private suspend fun handleAlarmRescheduling() {
        if (::alarm.isInitialized) {
            if (alarm.chainedAlarms.isNotEmpty()) {
                scheduleChainedAlarm(alarmId = alarm.alarmId)
            } else if (alarm.repeatingMode is Alarm.RepeatingMode.Once) {
                disableAlarm(alarmId = alarm.alarmId)
            } else if (alarm.repeatingMode is Alarm.RepeatingMode.Days) {
                setAlarm(
                    alarmId = alarm.alarmId,
                    isReschedulingMissedAlarm = false
                )
            }
        }
    }
}