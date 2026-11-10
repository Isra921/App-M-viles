package com.escom.escomorienta.ui.institutional

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.escom.escomorienta.data.institutional.InstitutionalLocalDataSource
import com.escom.escomorienta.data.institutional.LocalInstitutionalRepository
import com.escom.escomorienta.domain.institutional.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface InstitutionalUiState {
    data object Loading : InstitutionalUiState
    data class Content(val resources: List<InstitutionalResource>, val categories: List<String>) : InstitutionalUiState
    data class Empty(val searchActive: Boolean) : InstitutionalUiState
    data class Error(val message: String) : InstitutionalUiState
}

class InstitutionalViewModel(private val repository: InstitutionalRepository) : ViewModel() {
    private val getAll = GetInstitutionalResourcesUseCase(repository)
    private val search = SearchInstitutionalResourcesUseCase(repository)
    private val _state = MutableStateFlow<InstitutionalUiState>(InstitutionalUiState.Loading)
    val state: StateFlow<InstitutionalUiState> = _state.asStateFlow()
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()
    private val _category = MutableStateFlow<String?>(null)
    val category = _category.asStateFlow()
    private val _archetype = MutableStateFlow<InstitutionalArchetype?>(null)
    val archetype = _archetype.asStateFlow()

    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _state.value = InstitutionalUiState.Loading
        runCatching { withContext(Dispatchers.IO) { getAll() } }
            .onSuccess { publish(it) }
            .onFailure { _state.value = InstitutionalUiState.Error(it.message ?: "No se pudo cargar la información") }
    }
    fun setQuery(value: String) { _query.value = value; filter() }
    fun setCategory(value: String?) { _category.value = value; filter() }
    fun setArchetype(value: InstitutionalArchetype?) {
        _archetype.value = value
        _category.value = null
        filter()
    }
    private fun filter() = viewModelScope.launch {
        runCatching { withContext(Dispatchers.Default) { search(_query.value) } }
            .onSuccess { publish(it) }
            .onFailure { _state.value = InstitutionalUiState.Error(it.message ?: "No se pudo realizar la búsqueda") }
    }
    private suspend fun publish(matches: List<InstitutionalResource>) {
        val all = withContext(Dispatchers.IO) { getAll() }
        val filtered = matches
            .filter { _archetype.value == null || it.archetype == _archetype.value }
            .filter { _category.value == null || it.category == _category.value }
        val relevantCategories = all
            .filter { _archetype.value == null || it.archetype == _archetype.value }
            .map { it.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        _state.value = when {
            all.isEmpty() -> InstitutionalUiState.Empty(false)
            filtered.isEmpty() -> InstitutionalUiState.Empty(_query.value.isNotBlank() || _category.value != null || _archetype.value != null)
            else -> InstitutionalUiState.Content(filtered, relevantCategories)
        }
    }
}

class InstitutionalViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InstitutionalViewModel::class.java)) {
            return InstitutionalViewModel(LocalInstitutionalRepository(InstitutionalLocalDataSource.from(appContext))) as T
        }
        error("Unknown ViewModel class: ${modelClass.name}")
    }
}
