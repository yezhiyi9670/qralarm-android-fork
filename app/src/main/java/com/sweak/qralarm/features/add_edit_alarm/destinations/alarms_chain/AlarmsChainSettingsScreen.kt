package com.sweak.qralarm.features.add_edit_alarm.destinations.alarms_chain

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.sweak.qralarm.R
import com.sweak.qralarm.core.designsystem.component.QRAlarmDialog
import com.sweak.qralarm.core.designsystem.icon.QRAlarmIcons
import com.sweak.qralarm.core.designsystem.theme.BlueZodiac
import com.sweak.qralarm.core.designsystem.theme.Jacarta
import com.sweak.qralarm.core.designsystem.theme.QRAlarmTheme
import com.sweak.qralarm.core.designsystem.theme.isQRAlarmTheme
import com.sweak.qralarm.core.designsystem.theme.space
import com.sweak.qralarm.core.ui.components.EditCodeNameBottomSheet
import com.sweak.qralarm.features.add_edit_alarm.AddEditAlarmFlowState
import com.sweak.qralarm.features.add_edit_alarm.AddEditAlarmFlowUserEvent.AlarmsChainSettingsScreenUserEvent
import com.sweak.qralarm.features.add_edit_alarm.AddEditAlarmViewModel
import com.sweak.qralarm.features.add_edit_alarm.destinations.add_edit.components.AssignCodeBottomSheet
import com.sweak.qralarm.features.add_edit_alarm.destinations.alarms_chain.components.AddNewChainedAlarmCard
import com.sweak.qralarm.features.add_edit_alarm.destinations.alarms_chain.components.ChainedAlarmCard
import com.sweak.qralarm.features.add_edit_alarm.destinations.alarms_chain.components.OriginalAlarmCard

@Composable
fun AlarmsChainSettingsScreen(
    addEditAlarmViewModel: AddEditAlarmViewModel,
    onCancelClicked: () -> Unit,
    onScanCustomCodeClicked: () -> Unit
) {
    val addEditAlarmScreenState by addEditAlarmViewModel.state.collectAsStateWithLifecycle()

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var pendingScanUiId by rememberSaveable { mutableStateOf<String?>(null) }
    var isCameraPermissionDeniedDialogVisible by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted && pendingScanUiId != null) {
            pendingScanUiId = null
            onScanCustomCodeClicked()
        }
    }

    AlarmsChainSettingsScreenContent(
        state = addEditAlarmScreenState,
        onEvent = { event ->
            when (event) {
                is AlarmsChainSettingsScreenUserEvent.OnCancelClicked -> {
                    onCancelClicked()
                }

                is AlarmsChainSettingsScreenUserEvent.TryScanSpecificCodeForChainedAlarm -> {
                    addEditAlarmViewModel.onEvent(event)
                    pendingScanUiId = event.chainedAlarmUiId

                    if (cameraPermissionState.status.isGranted) {
                        pendingScanUiId = null
                        onScanCustomCodeClicked()
                    } else if (cameraPermissionState.status.shouldShowRationale) {
                        isCameraPermissionDeniedDialogVisible = true
                    } else {
                        cameraPermissionState.launchPermissionRequest()
                    }
                }

                else -> addEditAlarmViewModel.onEvent(event)
            }
        }
    )

    if (isCameraPermissionDeniedDialogVisible) {
        QRAlarmDialog(
            title = stringResource(R.string.camera_permission_required),
            message = stringResource(R.string.camera_permission_required_description),
            onDismissRequest = { isCameraPermissionDeniedDialogVisible = false },
            onPositiveClick = {
                isCameraPermissionDeniedDialogVisible = false
            },
            positiveButtonText = stringResource(R.string.ok),
            onNegativeClick = {
                isCameraPermissionDeniedDialogVisible = false
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
            },
            negativeButtonText = stringResource(R.string.settings)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsChainSettingsScreenContent(
    state: AddEditAlarmFlowState,
    onEvent: (AlarmsChainSettingsScreenUserEvent) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.alarms_chain),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(AlarmsChainSettingsScreenUserEvent.OnCancelClicked) }
                    ) {
                        Icon(
                            imageVector = QRAlarmIcons.BackArrow,
                            contentDescription =
                                stringResource(R.string.content_description_back_arrow_icon)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onEvent(
                                AlarmsChainSettingsScreenUserEvent.AlarmsChainDisclaimerDialogVisible(
                                    isVisible = true
                                )
                            )
                        }
                    ) {
                        Icon(
                            imageVector = QRAlarmIcons.Info,
                            contentDescription = stringResource(R.string.alarms_chain_disclaimer)
                        )
                    }
                }
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (MaterialTheme.isQRAlarmTheme)
                        Modifier.background(
                            brush = Brush.verticalGradient(listOf(Jacarta, BlueZodiac))
                        )
                    else Modifier
                )
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.space.small),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = MaterialTheme.space.medium)
            ) {
                item(key = "alarms_chain_description") {
                    Text(
                        text = stringResource(R.string.alarms_chain_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = MaterialTheme.space.small)
                    )
                }

                item(key = "original_alarm_card") {
                    OriginalAlarmCard(
                        alarmHourOfDay = state.alarmHourOfDay ?: 0,
                        alarmMinute = state.alarmMinute ?: 0,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                itemsIndexed(
                    items = state.chainedAlarms,
                    key = { _, chainedAlarm -> chainedAlarm.uiId }
                ) { index, chainedAlarm ->
                    ChainArrow()

                    ChainedAlarmCard(
                        chainedAlarm = chainedAlarm,
                        number = index + 1,
                        availableDelays = state.availableChainedAlarmDelaysMinutes,
                        onDelayMinutesSelected = { delayMinutesIndex ->
                            onEvent(
                                AlarmsChainSettingsScreenUserEvent.ChainedAlarmDelayMinutesChanged(
                                    chainedAlarmUiId = chainedAlarm.uiId,
                                    delayMinutesIndex = delayMinutesIndex
                                )
                            )
                        },
                        onAssignCodeClicked = {
                            onEvent(
                                AlarmsChainSettingsScreenUserEvent.AssignCodeForChainedAlarmDialogVisible(
                                    chainedAlarmUiId = chainedAlarm.uiId,
                                    isVisible = true
                                )
                            )
                        },
                        onEditCodeNameClicked = {
                            onEvent(
                                AlarmsChainSettingsScreenUserEvent.EditCodeNameForChainedAlarmDialogVisible(
                                    chainedAlarmUiId = chainedAlarm.uiId,
                                    isVisible = true
                                )
                            )
                        },
                        onDeleteClicked = {
                            onEvent(
                                AlarmsChainSettingsScreenUserEvent.DeleteChainedAlarmDialogVisible(
                                    chainedAlarmUiId = chainedAlarm.uiId,
                                    isVisible = true
                                )
                            )
                        },
                        onLabelChanged = { newLabel ->
                            onEvent(
                                AlarmsChainSettingsScreenUserEvent.ChainedAlarmLabelChanged(
                                    chainedAlarmUiId = chainedAlarm.uiId,
                                    newLabel = newLabel.ifBlank { null }
                                )
                            )
                        }
                    )
                }

                if (state.canAddChainedAlarms) {
                    item(key = "add_new_chained_alarm_card") {
                        ChainArrow()

                        AddNewChainedAlarmCard(
                            onAddNewChainedAlarmClicked = {
                                onEvent(
                                    AlarmsChainSettingsScreenUserEvent.AddNewChainedAlarmClicked
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item(key = "bottom_spacer") {
                    Box(modifier = Modifier.height(MaterialTheme.space.xLarge))
                }
            }
        }
    }

    val assignCodeDialogState = state.assignCodeForChainedAlarmDialogState
    val assignCodeForChainedAlarm = state.chainedAlarms.find {
        it.uiId == assignCodeDialogState.chainedAlarmUiId
    }

    if (assignCodeDialogState.isVisible && assignCodeForChainedAlarm != null) {
        AssignCodeBottomSheet(
            onScanCodeClicked = {
                onEvent(
                    AlarmsChainSettingsScreenUserEvent.TryScanSpecificCodeForChainedAlarm(
                        chainedAlarmUiId = assignCodeForChainedAlarm.uiId
                    )
                )
            },
            availableCodes = state.previouslySavedCodes,
            onChooseCodeFromList = { code ->
                onEvent(
                    AlarmsChainSettingsScreenUserEvent.CodeForChainedAlarmUpdated(
                        chainedAlarmUiId = assignCodeForChainedAlarm.uiId,
                        code = code
                    )
                )
            },
            onDismissRequest = {
                onEvent(
                    AlarmsChainSettingsScreenUserEvent.AssignCodeForChainedAlarmDialogVisible(
                        chainedAlarmUiId = assignCodeForChainedAlarm.uiId,
                        isVisible = false
                    )
                )
            }
        )
    }

    val editCodeNameDialogState = state.editCodeNameForChainedAlarmDialogState
    val editCodeNameForChainedAlarm = state.chainedAlarms.find {
        it.uiId == editCodeNameDialogState.chainedAlarmUiId
    }

    if (editCodeNameDialogState.isVisible && editCodeNameForChainedAlarm != null) {
        EditCodeNameBottomSheet(
            initialCodeName = editCodeNameDialogState.initialCodeName,
            onConfirmClicked = { newName ->
                onEvent(
                    AlarmsChainSettingsScreenUserEvent.CodeNameForChainedAlarmEdited(
                        chainedAlarmUiId = editCodeNameForChainedAlarm.uiId,
                        newName = newName
                    )
                )
            },
            onDismissRequest = {
                onEvent(
                    AlarmsChainSettingsScreenUserEvent.EditCodeNameForChainedAlarmDialogVisible(
                        chainedAlarmUiId = editCodeNameForChainedAlarm.uiId,
                        isVisible = false
                    )
                )
            }
        )
    }

    if (state.deleteChainedAlarmDialogState.isVisible) {
        state.deleteChainedAlarmDialogState.chainedAlarmUiId?.let { uiId ->
            QRAlarmDialog(
                title = stringResource(R.string.delete_this_alarm_in_chain),
                onDismissRequest = {
                    onEvent(
                        AlarmsChainSettingsScreenUserEvent.DeleteChainedAlarmDialogVisible(
                            chainedAlarmUiId = uiId,
                            isVisible = false
                        )
                    )
                },
                onPositiveClick = {
                    onEvent(
                        AlarmsChainSettingsScreenUserEvent.DeleteChainedAlarm(
                            chainedAlarmUiId = uiId
                        )
                    )
                },
                positiveButtonText = stringResource(R.string.delete),
                positiveButtonColor = MaterialTheme.colorScheme.error,
                negativeButtonText = stringResource(R.string.cancel)
            )
        }
    }

    if (state.isAlarmsChainDisclaimerDialogVisible) {
        QRAlarmDialog(
            title = stringResource(R.string.alarms_chain_disclaimer),
            message = stringResource(R.string.alarms_chain_disclaimer_description),
            onDismissRequest = {
                onEvent(
                    AlarmsChainSettingsScreenUserEvent.AlarmsChainDisclaimerDialogVisible(
                        isVisible = false
                    )
                )
            },
            onPositiveClick = {
                onEvent(
                    AlarmsChainSettingsScreenUserEvent.AlarmsChainDisclaimerDialogVisible(
                        isVisible = false
                    )
                )
            },
            positiveButtonText = stringResource(R.string.ok)
        )
    }
}

@Composable
private fun ChainArrow() {
    Image(
        painter = painterResource(R.drawable.ic_arrow_down),
        contentDescription = null,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .height(MaterialTheme.space.xLarge)
    )
}

@Preview
@Composable
private fun AlarmsChainSettingsScreenContentPreview() {
    QRAlarmTheme {
        AlarmsChainSettingsScreenContent(
            state = AddEditAlarmFlowState(
                isLoading = false,
                alarmHourOfDay = 9,
                alarmMinute = 30
            ),
            onEvent = {}
        )
    }
}
