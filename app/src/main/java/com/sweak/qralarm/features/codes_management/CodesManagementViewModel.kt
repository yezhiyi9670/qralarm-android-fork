package com.sweak.qralarm.features.codes_management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sweak.qralarm.core.domain.alarm.CodesRepository
import com.sweak.qralarm.core.ui.model.Code
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CodesManagementViewModel @Inject constructor(
    private val codesRepository: CodesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CodesManagementScreenState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                codesRepository.getDefaultAlarmCodeFlow(),
                codesRepository.getCodesFlow(),
                codesRepository.getAssignedCodeIdsFlow()
            ) { defaultCode, allCodes, assignedCodeIds ->
                val defaultUi = defaultCode?.let {
                    Code(id = it.codeId, value = it.value, name = it.name)
                }
                val othersUi = allCodes
                    .filter { it.codeId != defaultCode?.codeId }
                    .map { Code(id = it.codeId, value = it.value, name = it.name) }
                Triple(defaultUi, othersUi, assignedCodeIds.toSet())
            }.collect { (defaultUi, othersUi, usedCodeIds) ->
                _state.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        defaultAlarmCode = defaultUi,
                        otherCodes = othersUi,
                        usedCodeIds = usedCodeIds
                    )
                }
            }
        }
    }

    fun onEvent(event: CodesManagementScreenUserEvent) {
        when (event) {
            is CodesManagementScreenUserEvent.PickExistingCodeAsDefaultClicked -> {
                if (_state.value.otherCodes.isNotEmpty()) {
                    _state.update { it.copy(isPickingDefault = true) }
                }
            }

            is CodesManagementScreenUserEvent.CancelPickingCodeClicked -> {
                _state.update { it.copy(isPickingDefault = false) }
            }

            is CodesManagementScreenUserEvent.CodePickedAsDefault -> viewModelScope.launch {
                codesRepository.setDefaultAlarmCodeById(event.code.id)
                _state.update { it.copy(isPickingDefault = false) }
            }

            is CodesManagementScreenUserEvent.ClearDefaultCodeClicked -> viewModelScope.launch {
                codesRepository.setDefaultAlarmCodeById(null)
            }

            is CodesManagementScreenUserEvent.EditCodeNameClicked -> {
                _state.update { it.copy(codeBeingEdited = event.code) }
            }

            is CodesManagementScreenUserEvent.CodeNameEditConfirmed -> viewModelScope.launch {
                _state.value.codeBeingEdited?.let { code ->
                    codesRepository.updateCodeName(code.id, event.newName)
                }
                _state.update { it.copy(codeBeingEdited = null) }
            }

            is CodesManagementScreenUserEvent.CodeNameEditDismissed -> {
                _state.update { it.copy(codeBeingEdited = null) }
            }

            is CodesManagementScreenUserEvent.DeleteCodeClicked -> {
                _state.update { it.copy(codeBeingDeleted = event.code) }
            }

            is CodesManagementScreenUserEvent.DeleteCodeConfirmed -> {
                val code = _state.value.codeBeingDeleted

                _state.update { it.copy(codeBeingDeleted = null) }

                if (code != null) {
                    viewModelScope.launch {
                        codesRepository.deleteCode(code.id)
                    }
                }
            }

            is CodesManagementScreenUserEvent.DeleteCodeDismissed -> {
                _state.update { it.copy(codeBeingDeleted = null) }
            }

            is CodesManagementScreenUserEvent.CameraPermissionDeniedDialogVisible -> {
                _state.update { it.copy(isCameraPermissionDeniedDialogVisible = event.isVisible) }
            }

            else -> { /* no-op */
            }
        }
    }
}
