package com.example.gemma4camera

import android.app.Application
import android.graphics.Bitmap
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gemma4camera.inference.GemmaInference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class UiState(
    val description: String = "",
    val isAnalyzing: Boolean = false,
    val isModelLoading: Boolean = true,
    val isSpeaking: Boolean = false,
    val error: String? = null,
    val autoMode: Boolean = false,
    val modelReady: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val gemmaInference = GemmaInference()
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    @Volatile
    var latestBitmap: Bitmap? = null

    init {
        initTts(application)
        initModel(application)
    }

    private fun initTts(application: Application) {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.JAPANESE
                ttsReady = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        _uiState.value = _uiState.value.copy(isSpeaking = false)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _uiState.value = _uiState.value.copy(isSpeaking = false)
                    }
                })
            }
        }
    }

    private fun initModel(application: Application) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isModelLoading = true)
                gemmaInference.initialize(application)
                _uiState.value = _uiState.value.copy(
                    isModelLoading = false,
                    modelReady = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isModelLoading = false,
                    error = "モデル読み込みエラー: ${e.message}"
                )
            }
        }
    }

    fun analyzeCurrentFrame() {
        val bitmap = latestBitmap ?: return
        if (_uiState.value.isAnalyzing || !_uiState.value.modelReady) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true, error = null)
            try {
                val scaled = bitmap.scaleToMax(512)
                val description = gemmaInference.analyzeImage(
                    scaled,
                    "この画像に映っているものを日本語で簡潔に説明してください。"
                )
                _uiState.value = _uiState.value.copy(
                    description = description,
                    isAnalyzing = false
                )
                speak(description)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    error = e.message ?: "解析エラー"
                )
            }
        }
    }

    fun toggleAutoMode() {
        _uiState.value = _uiState.value.copy(autoMode = !_uiState.value.autoMode)
    }

    private fun speak(text: String) {
        if (!ttsReady || text.isBlank()) return
        _uiState.value = _uiState.value.copy(isSpeaking = true)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "gemma_desc")
    }

    fun stopSpeaking() {
        tts?.stop()
        _uiState.value = _uiState.value.copy(isSpeaking = false)
    }

    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        gemmaInference.close()
        super.onCleared()
    }
}

private fun Bitmap.scaleToMax(maxDimension: Int): Bitmap {
    val ratio = maxDimension.toFloat() / maxOf(width, height)
    if (ratio >= 1f) return this
    return Bitmap.createScaledBitmap(
        this,
        (width * ratio).toInt(),
        (height * ratio).toInt(),
        true
    )
}
