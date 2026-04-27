package com.moravian.comictracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.moravian.comictracker.network.ComicVineApi
import com.moravian.comictracker.network.ComicVineIssue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

sealed class IssueDetailUiState {
    data object Loading : IssueDetailUiState()
    data class Success(val issue: ComicVineIssue) : IssueDetailUiState()
    data class Error(val message: String) : IssueDetailUiState()
}

class IssueDetailViewModel(private val issueId: Int) : ViewModel() {
    private val api = ComicVineApi()

    private val _uiState = MutableStateFlow<IssueDetailUiState>(IssueDetailUiState.Loading)
    val uiState: StateFlow<IssueDetailUiState> = _uiState.asStateFlow()

    init {
        loadIssue()
    }

    private fun loadIssue() {
        viewModelScope.launch {
            _uiState.value = IssueDetailUiState.Loading
            try {
                val response = api.getIssue(issueId)
                _uiState.value = IssueDetailUiState.Success(response.results)
            } catch (e: Exception) {
                _uiState.value = IssueDetailUiState.Error(e.message ?: "Failed to load issue")
            }
        }
    }

    companion object {
        fun factory(issueId: Int): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T =
                IssueDetailViewModel(issueId) as T
        }
    }
}
