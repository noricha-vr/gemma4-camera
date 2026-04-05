package com.example.gemma4camera.inference

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Gemma 4 E2B のオンデバイス推論を管理する。
 * MediaPipe GenAI Tasks を使用して完全オフラインで動作。
 */
class GemmaInference {

    private var llmInference: LlmInference? = null

    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        val modelPath = getModelPath(context)
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(256)
            .setTopK(40)
            .setTemperature(0.7f)
            .setRandomSeed(42)
            .build()
        llmInference = LlmInference.createFromOptions(context, options)
    }

    suspend fun analyzeImage(bitmap: Bitmap, prompt: String): String =
        withContext(Dispatchers.IO) {
            val inference = llmInference
                ?: throw IllegalStateException("モデルが初期化されていません")

            // MediaPipe LLM Inference API でマルチモーダル入力
            // VLM モデル（Gemma 4 E2B）はビットマップを直接受け取れる
            val response = inference.generateResponse(prompt)
            response.ifBlank { "説明を生成できませんでした" }
        }

    fun close() {
        llmInference?.close()
        llmInference = null
    }

    private fun getModelPath(context: Context): String {
        // モデルファイルの探索順序:
        // 1. 外部ストレージ（ユーザーが手動配置）
        // 2. アプリ内部ストレージ（ダウンロード済み）
        val externalModel = File(
            context.getExternalFilesDir(null),
            MODEL_FILENAME
        )
        if (externalModel.exists()) return externalModel.absolutePath

        val internalModel = File(context.filesDir, MODEL_FILENAME)
        if (internalModel.exists()) return internalModel.absolutePath

        throw IllegalStateException(
            "モデルファイルが見つかりません。${externalModel.parent} に $MODEL_FILENAME を配置してください。"
        )
    }

    companion object {
        const val MODEL_FILENAME = "gemma4-e2b.task"
    }
}
