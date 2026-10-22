package com.sweak.qralarm.features.add_edit_alarm.model

import android.os.Parcelable
import com.sweak.qralarm.core.ui.model.Code
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChainedAlarmWrapper(
    val id: Long = 0,
    val uiId: String,
    val alarmHourOfDay: Int,
    val alarmMinute: Int,
    val selectedDelayMinutesIndex: Int,
    val assignedCode: Code?,
    val label: String?
) : Parcelable
