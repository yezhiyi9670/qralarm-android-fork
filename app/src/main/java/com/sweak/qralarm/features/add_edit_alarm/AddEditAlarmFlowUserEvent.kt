package com.sweak.qralarm.features.add_edit_alarm

import android.net.Uri
import com.sweak.qralarm.core.domain.alarm.Alarm.Ringtone
import com.sweak.qralarm.core.ui.model.AlarmRepeatingScheduleWrapper
import com.sweak.qralarm.core.ui.model.Code

sealed class AddEditAlarmFlowUserEvent {

    sealed class AddEditAlarmScreenUserEvent : AddEditAlarmFlowUserEvent() {
        data object OnCancelClicked : AddEditAlarmScreenUserEvent()
        data class DiscardAlarmChangesDialogVisible(
            val isVisible: Boolean
        ) : AddEditAlarmScreenUserEvent()

        data object ConfirmDiscardAlarmChanges : AddEditAlarmScreenUserEvent()
        data object SaveAlarmClicked : AddEditAlarmScreenUserEvent()
        data class TrySaveAlarm(
            val cameraPermissionStatus: Boolean,
            val notificationsPermissionStatus: Boolean
        ) : AddEditAlarmScreenUserEvent()

        data object HideMissingPermissionsDialog : AddEditAlarmScreenUserEvent()
        data object RequestCameraPermission : AddEditAlarmScreenUserEvent()
        data object RequestNotificationsPermission : AddEditAlarmScreenUserEvent()
        data object RequestAlarmsPermission : AddEditAlarmScreenUserEvent()
        data object RequestFullScreenIntentPermission : AddEditAlarmScreenUserEvent()
        data class NotificationsPermissionDeniedDialogVisible(
            val isVisible: Boolean
        ) : AddEditAlarmScreenUserEvent()

        data class DialerPickerDialogVisible(val isVisible: Boolean) : AddEditAlarmScreenUserEvent()
        data class AlarmTimeChanged(
            val newAlarmHourOfDay: Int,
            val newAlarmMinute: Int,
            val source: Source
        ) : AddEditAlarmScreenUserEvent() {
            enum class Source {
                Spinner, Dialog
            }
        }

        data class DatePickerDialogVisible(val isVisible: Boolean) : AddEditAlarmScreenUserEvent()
        data class AlarmDateSelected(val selectedDateInMillis: Long) : AddEditAlarmScreenUserEvent()
        data class ChooseAlarmRepeatingScheduleDialogVisible(
            val isVisible: Boolean
        ) : AddEditAlarmScreenUserEvent()

        data class AlarmRepeatingScheduleSelected(
            val newAlarmRepeatingScheduleWrapper: AlarmRepeatingScheduleWrapper
        ) : AddEditAlarmScreenUserEvent()

        data class ChooseAlarmSnoozeConfigurationDialogVisible(
            val isVisible: Boolean
        ) : AddEditAlarmScreenUserEvent()

        data class AlarmSnoozeConfigurationSelected(
            val newSnoozeNumberToDurationPair: Pair<Int, Int>
        ) : AddEditAlarmScreenUserEvent()

        data class ChooseAlarmRingtoneDialogVisible(
            val isVisible: Boolean
        ) : AddEditAlarmScreenUserEvent()

        data class AlarmRingtoneConfigSelected(
            val newRingtone: Ringtone,
            val newAlarmVolumePercentage: Int?
        ) : AddEditAlarmScreenUserEvent()

        data class ToggleAlarmRingtonePlayback(
            val ringtone: Ringtone
        ) : AddEditAlarmScreenUserEvent()

        data object PickCustomRingtone : AddEditAlarmScreenUserEvent()
        data class CustomRingtoneUriRetrieved(
            val customRingtoneUri: Uri?
        ) : AddEditAlarmScreenUserEvent()

        data class VibrationsEnabledChanged(val areEnabled: Boolean) : AddEditAlarmScreenUserEvent()
        data class CodeEnabledChanged(val isEnabled: Boolean) : AddEditAlarmScreenUserEvent()
        data class AssignCodeDialogVisible(val isVisible: Boolean) : AddEditAlarmScreenUserEvent()
        data class EditCodeNameDialogVisible(val isVisible: Boolean) : AddEditAlarmScreenUserEvent()
        data object TryScanSpecificCode : AddEditAlarmScreenUserEvent()
        data class CameraPermissionDeniedDialogVisible(
            val isVisible: Boolean
        ) : AddEditAlarmScreenUserEvent()

        data class CodeChosenFromList(val code: Code) : AddEditAlarmScreenUserEvent()
        data class CodeNameEdited(val newName: String?) : AddEditAlarmScreenUserEvent()
        data object ClearAssignedCode : AddEditAlarmScreenUserEvent()
        data object GoToApplicationSettingsClicked : AddEditAlarmScreenUserEvent()
        data class AlarmLabelChanged(val newAlarmLabel: String) : AddEditAlarmScreenUserEvent()
        data object AdvancedSettingsClicked : AddEditAlarmScreenUserEvent()
        data object SpecialSettingsClicked : AddEditAlarmScreenUserEvent()
        data class DeleteAlarmDialogVisible(val isVisible: Boolean) : AddEditAlarmScreenUserEvent()
        data object DeleteAlarm : AddEditAlarmScreenUserEvent()
        data class DownloadCodeDialogVisible(val isVisible: Boolean) : AddEditAlarmScreenUserEvent()
        data object DownloadCode : AddEditAlarmScreenUserEvent()
        data object AlarmsChainSettingsClicked : AddEditAlarmScreenUserEvent()
        data object RefreshAlarmCountdown : AddEditAlarmScreenUserEvent()
    }

    sealed class AdvancedAlarmSettingsScreenUserEvent : AddEditAlarmFlowUserEvent() {
        data object OnCancelClicked : AdvancedAlarmSettingsScreenUserEvent()
        data class ChooseGentleWakeUpDurationDialogVisible(
            val isVisible: Boolean
        ) : AdvancedAlarmSettingsScreenUserEvent()

        data class GentleWakeUpDurationSelected(
            val newGentleWakeUpDurationInSeconds: Int
        ) : AdvancedAlarmSettingsScreenUserEvent()

        data class ChooseTemporaryMuteDurationDialogVisible(
            val isVisible: Boolean
        ) : AdvancedAlarmSettingsScreenUserEvent()

        data class TemporaryMuteDurationSelected(
            val newTemporaryMuteDurationInSeconds: Int
        ) : AdvancedAlarmSettingsScreenUserEvent()

        data class OpenCodeLinkEnabledChanged(
            val isEnabled: Boolean
        ) : AdvancedAlarmSettingsScreenUserEvent()

        data class ChooseCancelLockDurationDialogVisible(
            val isVisible: Boolean
        ) : AdvancedAlarmSettingsScreenUserEvent()

        data class CancelLockDurationSelected(
            val newCancelLockDurationInMinutes: Int
        ) : AdvancedAlarmSettingsScreenUserEvent()

        data class EmergencyTaskEnabledChanged(
            val isEnabled: Boolean
        ) : AdvancedAlarmSettingsScreenUserEvent()
    }

    sealed class SpecialAlarmSettingsScreenUserEvent : AddEditAlarmFlowUserEvent() {
        data object OnCancelClicked : SpecialAlarmSettingsScreenUserEvent()
        data object TryUseSpecialAlarmSettings : SpecialAlarmSettingsScreenUserEvent()
    }

    sealed class AlarmsChainSettingsScreenUserEvent : AddEditAlarmFlowUserEvent() {
        data object OnCancelClicked : AlarmsChainSettingsScreenUserEvent()
        data object AddNewChainedAlarmClicked : AlarmsChainSettingsScreenUserEvent()

        data class ChainedAlarmDelayMinutesChanged(
            val chainedAlarmUiId: String,
            val delayMinutesIndex: Int
        ) : AlarmsChainSettingsScreenUserEvent()

        data class AssignCodeForChainedAlarmDialogVisible(
            val chainedAlarmUiId: String,
            val isVisible: Boolean
        ) : AlarmsChainSettingsScreenUserEvent()

        data class EditCodeNameForChainedAlarmDialogVisible(
            val chainedAlarmUiId: String,
            val isVisible: Boolean
        ) : AlarmsChainSettingsScreenUserEvent()

        data class CodeNameForChainedAlarmEdited(
            val chainedAlarmUiId: String,
            val newName: String?
        ) : AlarmsChainSettingsScreenUserEvent()

        data class CodeForChainedAlarmUpdated(
            val chainedAlarmUiId: String,
            val code: Code
        ) : AlarmsChainSettingsScreenUserEvent()

        data class TryScanSpecificCodeForChainedAlarm(
            val chainedAlarmUiId: String
        ) : AlarmsChainSettingsScreenUserEvent()

        data class DeleteChainedAlarmDialogVisible(
            val chainedAlarmUiId: String,
            val isVisible: Boolean
        ) : AlarmsChainSettingsScreenUserEvent()

        data class DeleteChainedAlarm(
            val chainedAlarmUiId: String
        ) : AlarmsChainSettingsScreenUserEvent()

        data class AlarmsChainDisclaimerDialogVisible(
            val isVisible: Boolean
        ) : AlarmsChainSettingsScreenUserEvent()

        data class ChainedAlarmLabelChanged(
            val chainedAlarmUiId: String,
            val newLabel: String?
        ) : AlarmsChainSettingsScreenUserEvent()
    }
}
