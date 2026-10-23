package com.sweak.qralarm.features.add_edit_alarm.destinations.alarms_chain.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sweak.qralarm.R
import com.sweak.qralarm.core.designsystem.icon.QRAlarmIcons
import com.sweak.qralarm.core.designsystem.theme.QRAlarmTheme
import com.sweak.qralarm.core.designsystem.theme.space
import com.sweak.qralarm.core.ui.getTimeString
import com.sweak.qralarm.features.add_edit_alarm.model.ChainedAlarmWrapper

@Composable
fun ChainedAlarmCard(
    chainedAlarm: ChainedAlarmWrapper,
    number: Int,
    availableDelays: List<Int>,
    onDelayMinutesSelected: (delayMinutesIndex: Int) -> Unit,
    onAssignCodeClicked: () -> Unit,
    onEditCodeNameClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onLabelChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(all = MaterialTheme.space.medium)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = QRAlarmIcons.Chain,
                    contentDescription = stringResource(R.string.content_description_chain_icon)
                )

                Column(
                    modifier = Modifier
                        .weight(weight = 1f)
                        .padding(horizontal = MaterialTheme.space.smallMedium)
                ) {
                    BasicTextField(
                        value = chainedAlarm.label ?: "",
                        onValueChange = onLabelChanged,
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            color = LocalContentColor.current
                        ),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (chainedAlarm.label.isNullOrBlank()) {
                                Text(
                                    text = stringResource(
                                        R.string.alarm_in_chain,
                                        number
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                Text(
                    text = "~" + getTimeString(
                        hourOfDay = chainedAlarm.alarmHourOfDay,
                        minute = chainedAlarm.alarmMinute,
                        is24HourFormat = DateFormat.is24HourFormat(LocalContext.current)
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton(onClick = onDeleteClicked) {
                    Icon(
                        imageVector = QRAlarmIcons.Delete,
                        contentDescription = stringResource(
                            R.string.content_description_delete_icon
                        ),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = LocalContentColor.current,
                modifier = Modifier.padding(vertical = MaterialTheme.space.small)
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.after_colon),
                    style = MaterialTheme.typography.titleMedium
                )

                DelayMinutesPicker(
                    selectedDelayMinutes = availableDelays.getOrElse(
                        chainedAlarm.selectedDelayMinutesIndex
                    ) { availableDelays.first() },
                    availableDelays = availableDelays,
                    onDelayMinutesSelected = onDelayMinutesSelected
                )
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = LocalContentColor.current,
                modifier = Modifier.padding(vertical = MaterialTheme.space.small)
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val assignedCode = chainedAlarm.assignedCode

                if (assignedCode != null) {
                    Column(modifier = Modifier.weight(weight = 1f)) {
                        Text(
                            text = stringResource(R.string.code_assigned),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = assignedCode.name ?: assignedCode.value,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = onEditCodeNameClicked) {
                        Icon(
                            imageVector = QRAlarmIcons.Edit,
                            contentDescription = stringResource(R.string.edit_code_name)
                        )
                    }
                    TextButton(
                        onClick = onAssignCodeClicked,
                        colors = chipTextButtonColors()
                    ) {
                        Text(text = stringResource(R.string.assign_new_code))
                    }
                } else {
                    TextButton(
                        onClick = onAssignCodeClicked,
                        colors = chipTextButtonColors(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = QRAlarmIcons.QrCode,
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(R.string.assign_new_code),
                            modifier = Modifier.padding(start = MaterialTheme.space.small)
                        )
                    }
                }
            }
        }
    }
}

/**
 * A [TextButton] inside the card. The QRAlarm theme maps primary to the same purple as the card
 * container, which made the default text button colors invisible, so the button follows the
 * card content color instead.
 */
@Composable
private fun chipTextButtonColors() = ButtonDefaults.textButtonColors(
    contentColor = LocalContentColor.current
)

@Composable
private fun DelayMinutesPicker(
    selectedDelayMinutes: Int,
    availableDelays: List<Int>,
    onDelayMinutesSelected: (Int) -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Box {
        TextButton(
            onClick = { isMenuExpanded = true },
            colors = chipTextButtonColors()
        ) {
            Text(
                text = pluralStringResource(
                    R.plurals.minutes,
                    selectedDelayMinutes,
                    selectedDelayMinutes
                ),
                style = MaterialTheme.typography.titleMedium
            )
        }

        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false }
        ) {
            availableDelays.forEachIndexed { index, delayMinutes ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = pluralStringResource(
                                R.plurals.minutes,
                                delayMinutes,
                                delayMinutes
                            )
                        )
                    },
                    onClick = {
                        isMenuExpanded = false
                        onDelayMinutesSelected(index)
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun ChainedAlarmCardPreview() {
    QRAlarmTheme {
        ChainedAlarmCard(
            chainedAlarm = ChainedAlarmWrapper(
                uiId = "1",
                alarmHourOfDay = 9,
                alarmMinute = 35,
                selectedDelayMinutesIndex = 0,
                assignedCode = null,
                label = null
            ),
            number = 1,
            availableDelays = listOf(5, 10, 15, 20, 30),
            onDelayMinutesSelected = {},
            onAssignCodeClicked = {},
            onEditCodeNameClicked = {},
            onDeleteClicked = {},
            onLabelChanged = {},
            modifier = Modifier.width(400.dp)
        )
    }
}
