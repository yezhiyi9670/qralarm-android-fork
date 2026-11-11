package com.sweak.qralarm.core.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sweak.qralarm.core.storage.database.model.CodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CodesDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCode(code: CodeEntity): Long

    @Query("SELECT * FROM code WHERE value = :value LIMIT 1")
    suspend fun getCodeByValue(value: String): CodeEntity?

    @Query("SELECT * FROM code WHERE codeId = :codeId")
    suspend fun getCode(codeId: Long): CodeEntity?

    @Query("SELECT * FROM code WHERE codeId = :codeId")
    fun getCodeFlow(codeId: Long): Flow<CodeEntity?>

    @Query("SELECT * FROM code ORDER BY codeId")
    fun getAllCodes(): Flow<List<CodeEntity>>

    @Update
    suspend fun updateCode(code: CodeEntity)

    @Query(
        """
        SELECT assignedCodeId FROM alarm WHERE assignedCodeId IS NOT NULL
        UNION
        SELECT assignedCodeId FROM chainedAlarm WHERE assignedCodeId IS NOT NULL
        """
    )
    fun getAssignedCodeIds(): Flow<List<Long>>

    @Query("DELETE FROM code WHERE codeId = :codeId")
    suspend fun deleteCode(codeId: Long)

    @Query("DELETE FROM code")
    suspend fun deleteAllCodes()
}
