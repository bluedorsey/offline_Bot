package com.example.personalhealthcareapp.LLMinference

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

sealed class ModelState {
    data object Idle : ModelState()
    data object Loading : ModelState()
    data object Ready : ModelState()
    data class Error(val message: String) : ModelState()
}

object LLMInferenceManager {

    private var llmInference: LlmInference? = null
    private const val MODEL_NAME = "gemma-1.1-2b-it-cpu-int4.bin"
    private const val TAG = "NeuroPocket"

    private val _modelState = MutableStateFlow<ModelState>(ModelState.Idle)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    // Use SUSPEND strategy so we never drop tokens mid-stream
    private val _partialResult = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    val partialResult: SharedFlow<String> = _partialResult.asSharedFlow()

    // Signal when a full response is done
    private val _responseDone = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val responseDone: SharedFlow<Unit> = _responseDone.asSharedFlow()

    fun initModel(context: Context) {
        if (llmInference != null) return
        _modelState.value = ModelState.Loading
        Log.d(TAG, "Starting model load...")

        try {
            val modelFile = File(context.cacheDir, MODEL_NAME)
            if (!modelFile.exists()) {
                Log.d(TAG, "Copying model from assets to cache...")
                context.assets.open(MODEL_NAME).use { input ->
                    modelFile.outputStream().buffered(8192).use { output ->
                        input.copyTo(output, bufferSize = 8192)
                    }
                }
            }

            Log.d(TAG, "Configuring inference engine...")
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(1024)
                .setResultListener { partialResult, done ->
                    _partialResult.tryEmit(partialResult)
                    if (done) {
                        _responseDone.tryEmit(Unit)
                    }
                }
                .build()

            Log.d(TAG, "Loading model into memory...")
            llmInference = LlmInference.createFromOptions(context, options)
            _modelState.value = ModelState.Ready
            Log.d(TAG, "Model loaded successfully.")

        } catch (e: Exception) {
            val msg = e.message ?: "Unknown error"
            Log.e(TAG, "Failed to load model: $msg")
            _modelState.value = ModelState.Error(msg)
        }
    }

    fun generateResponse(prompt: String): Boolean {
        val inference = llmInference
        if (inference == null) {
            Log.e(TAG, "Cannot generate: model not loaded.")
            return false
        }

        Log.d(TAG, "Generating response for: ${prompt.take(50)}...")
        val formattedPrompt =
            "<start_of_turn>user\n$prompt<end_of_turn>\n<start_of_turn>model\n"

        return try {
            inference.generateResponseAsync(formattedPrompt)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Generation error: ${e.message}")
            false
        }
    }
}