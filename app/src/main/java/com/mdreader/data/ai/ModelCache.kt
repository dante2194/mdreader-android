package com.mdreader.data.ai

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mdreader.data.repository.PrefsRepository
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * Simple cache for the model list, with live data for UI.
 */
class ModelCache(
    private val context: Context,
    private val prefs: PrefsRepository,
    private val aiClient: AiClient
) : ViewModel() {

    private val _models = MutableLiveData<List<AiClient.ModelInfo>>()
    val models: LiveData<List<AiClient.ModelInfo>> = _models

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    // User preference: show only free models?
    private val _showFreeOnly = MutableLiveData<true>
    val showFreeOnly: LiveData<Boolean> = _showFreeOnly

    init {
        loadModels()
    }

    fun setShowFreeOnly(showFree: Boolean) {
        _showFreeOnly.value = showFree
        // Re-filter the current list
        val current = _models.value ?: emptyList()
        _models.value = current.filter { it.isFree == showFree || !showFree }
    }

    fun loadModels() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val models = aiClient.fetchModels()
                _models.value = models
                // Apply initial filter (free only by default)
                setShowFreeOnly(true)
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
