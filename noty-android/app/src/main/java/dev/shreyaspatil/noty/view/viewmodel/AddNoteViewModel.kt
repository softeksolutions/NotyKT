/*
 * Copyright 2020 Shreyas Patil
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.shreyaspatil.noty.view.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.shreyaspatil.noty.core.model.NotyTask
import dev.shreyaspatil.noty.core.repository.NotyNoteRepository
import dev.shreyaspatil.noty.core.repository.Either
import dev.shreyaspatil.noty.core.task.NotyTaskManager
import dev.shreyaspatil.noty.di.LocalRepository
import dev.shreyaspatil.noty.utils.validator.NoteValidator
import dev.shreyaspatil.noty.view.state.AddNoteState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalCoroutinesApi
@HiltViewModel
class AddNoteViewModel @Inject constructor(
    @LocalRepository private val noteRepository: NotyNoteRepository,
    private val notyTaskManager: NotyTaskManager
) : BaseViewModel<AddNoteState>(initialState = AddNoteState()) {

    private var job: Job? = null

    fun addNote() {
        job?.cancel()
        job = viewModelScope.launch {
            val title = state.value.title.trim()
            val note = state.value.note.trim()

            setState { state -> state.copy(isAdding = true) }

            val result = noteRepository.addNote(title, note)
            setState { state -> state.copy(isAdding = false) }

            when (result) {
                is Either.Success -> {
                    val noteId = result.data
                    scheduleNoteCreate(noteId)
                    setState { state -> state.copy(added = true) }
                }
                is Either.Error -> {
                    setState { state -> state.copy(errorMessage = result.message) }
                }
            }
        }
    }

    private fun scheduleNoteCreate(noteId: String) =
        notyTaskManager.scheduleTask(NotyTask.create(noteId))

    fun setTitle(title: String) {
        setState { state -> state.copy(title = title) }
        validateNote()
    }

    fun setNote(note: String) {
        setState { state -> state.copy(note = note) }
        validateNote()
    }

    private fun validateNote() {
        val isValid = NoteValidator.isValidNote(currentState.title, currentState.note)
        setState { state -> state.copy(showSave = isValid) }
    }

    fun resetState() {
        setState { AddNoteState() }
    }
}
