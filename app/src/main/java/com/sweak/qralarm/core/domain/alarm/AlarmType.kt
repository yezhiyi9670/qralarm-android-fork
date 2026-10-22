package com.sweak.qralarm.core.domain.alarm

/** Which occurrence of an alarm is being scheduled or fired. */
enum class AlarmType {
    REGULAR,
    SNOOZED,
    CHAINED
}
