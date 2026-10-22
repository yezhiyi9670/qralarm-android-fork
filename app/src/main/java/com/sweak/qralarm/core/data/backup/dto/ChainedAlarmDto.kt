package com.sweak.qralarm.core.data.backup.dto

import kotlinx.serialization.Serializable

/** One alarm in an alarm's chain, see [AlarmDto.chainedAlarms]. */
@Serializable
data class ChainedAlarmDto(
    val delayMinutes: Int = 5,
    val assignedCodeId: Long? = null,
    val label: String? = null,
    val order: Int = 0
)
