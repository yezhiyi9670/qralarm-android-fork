package com.sweak.qralarm.core.storage.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chainedAlarm",
    foreignKeys = [
        ForeignKey(
            entity = AlarmEntity::class,
            parentColumns = ["alarmId"],
            childColumns = ["parentAlarmId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CodeEntity::class,
            parentColumns = ["codeId"],
            childColumns = ["assignedCodeId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("parentAlarmId"), Index("assignedCodeId")]
)
data class ChainedAlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val chainedAlarmId: Long = 0,
    val parentAlarmId: Long,
    val delayMinutes: Int,
    val assignedCodeId: Long?,
    val label: String?,
    val order: Int,
    val nextChainedAlarmTimeInMillis: Long?
)
