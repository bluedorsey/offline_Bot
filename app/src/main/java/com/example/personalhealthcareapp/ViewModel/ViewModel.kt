package com.example.personalhealthcareapp.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalhealthcareapp.LLMinference.LLMInferenceManager
import com.example.personalhealthcareapp.LLMinference.ModelState
import com.example.personalhealthcareapp.chat_managment.Chat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ViewModel(application: Application) : AndroidViewModel(application) {

    val modelState: StateFlow<ModelState> = LLMInferenceManager.modelState

    private val _chatHistory = MutableStateFlow<List<Chat>>(emptyList())
    val chathistory: StateFlow<List<Chat>> = _chatHistory.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    init {
        // Load model on IO thread
        viewModelScope.launch(Dispatchers.IO) {
            LLMInferenceManager.initModel(application)
        }

        // Collect partial tokens on Default dispatcher for faster processing
        viewModelScope.launch(Dispatchers.Default) {
            LLMInferenceManager.partialResult.collect { token ->
                appendToken(token)
            }
        }

        // Collect completion signals
        viewModelScope.launch(Dispatchers.Default) {
            LLMInferenceManager.responseDone.collect {
                finalizeResponse()
            }
        }
    }

    fun sendMessage(message: String) {
        if (_isGenerating.value) return // Prevent overlapping requests

        val trimmed = message.trim()
        if (trimmed.isEmpty()) return

        _isGenerating.value = true

        // Add user message + a placeholder AI loading bubble
        val updated = _chatHistory.value + listOf(
            Chat(text = trimmed, isUser = true),
            Chat(text = "", isUser = false, isLoading = true)
        )
        _chatHistory.value = updated

        // Fire the async generation on IO
        viewModelScope.launch(Dispatchers.IO) {
            val success = LLMInferenceManager.generateResponse(trimmed)
            if (!success) {
                markError("Model is not ready. Please wait.")
            }
        }
    }

    private fun appendToken(token: String) {
        val current = _chatHistory.value.toMutableList()
        if (current.isEmpty()) return

        val lastIndex = current.lastIndex
        val last = current[lastIndex]

        if (!last.isUser) {
            current[lastIndex] = last.copy(
                text = last.text + token,
                isLoading = false
            )
            _chatHistory.value = current
        }
    }

    private fun finalizeResponse() {
        _isGenerating.value = false
        // Ensure loading flag is off on the last bubble
        val current = _chatHistory.value.toMutableList()
        if (current.isEmpty()) return

        val lastIndex = current.lastIndex
        val last = current[lastIndex]
        if (!last.isUser && last.isLoading) {
            current[lastIndex] = last.copy(isLoading = false)
            _chatHistory.value = current
        }
    }

    private fun markError(errorMsg: String) {
        _isGenerating.value = false
        val current = _chatHistory.value.toMutableList()
        if (current.isEmpty()) return

        val lastIndex = current.lastIndex
        val last = current[lastIndex]
        if (!last.isUser) {
            current[lastIndex] = last.copy(
                text = errorMsg,
                isLoading = false,
                isError = true
            )
            _chatHistory.value = current
        }
    }
}