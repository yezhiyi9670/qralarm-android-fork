package com.sweak.qralarm.core.domain.alarm

const val MAX_CHAINED_ALARMS = 3
const val DEFAULT_CHAINED_ALARM_DELAY_MINUTES = 5
val AVAILABLE_CHAINED_ALARM_DELAYS_MINUTES = listOf(5, 10, 15, 20, 30)

data class ChainedAlarm(
    val chainedAlarmId: Long,
    val delayMinutes: Int,
    val assignedCode: Code?,
    val label: String?,
    val order: Int,
    val nextChainedAlarmTimeInMillis: Long?
) {
    /** Only the chained alarm that is going to ring next carries its time. */
    val isArmed: Boolean
        get() = nextChainedAlarmTimeInMillis != null
}
