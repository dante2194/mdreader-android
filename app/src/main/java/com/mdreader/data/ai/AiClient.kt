package com.mdreader.data.ai

import android.content.Context
import androidx.annotation.WorkerThread
import com.mdreader.data.repository.PrefsRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.io.IOException

/**
 * Client for OpenRouter API (OpenAI-compatible).
 */
class AiClient(
    private val context: Context,
    private val prefs: PrefsRepository
) {

    private val httpClient = OkHttpClient()
    private val apiEndpoint = "https://openrouter.ai/api/v1/chat/completions"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Fetches the list of models from OpenRouter (no auth required).
     * @return list of model IDs and names, or empty list on failure
     */
    @WorkerThread
    fun fetchModels(): List<ModelInfo> {
        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/models")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            return try {
                val json = JSONObject(body)
                val dataArray = json.getJSONArray("data")
                val list = mutableListOf<ModelInfo>()
                for (i in 0 until dataArray.length()) {
                    val obj = dataArray.getJSONObject(i)
                    val id = obj.getString("id")
                    val name = obj.optString("name", id)
                    val contextLength = obj.optInt("context_length", 0)
                    val pricing = obj.optJSONObject("pricing")
                    val promptPrice = pricing?.optString("prompt") ?: "0"
                    val isFree = promptPrice == "0" || promptPrice == "0.0"
                    list.add(ModelInfo(id, name, contextLength, isFree))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Sends a chat completion request to OpenRouter.
     * @param prompt the user prompt (with {copied text} replaced)
     * @return the AI response text, or empty string on failure
     */
    @WorkerThread
    fun getCompletion(prompt: String): String {
        val apiKey = prefs.apiKey.first() ?: return "Error: API key not set"
        val modelId = prefs.selectedModel.first() ?: return "Error: No model selected"

        val requestBody = """
            {
                "model": "$modelId",
                "messages": [
                    {
                        "role": "user",
                        "content": "$prompt"
                    }
                ],
                "temperature": 0.7
            }
        """.trimIndent()

        val body = RequestBody.create(requestBody, jsonMediaType)
        val request = Request.Builder()
            .url(apiEndpoint)
            .post(body)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", "https://mdreader.app") // Optional, but recommended
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return "Error: ${response.code}"
            }
            val body = response.body?.string() ?: return "Error: Empty response"
            return try {
                val json = JSONObject(body)
                val choices = json.getJSONArray("choices")
                val message = choices.getJSONObject(0).getJSONObject("message")
                message.getString("content")
            } catch (e: Exception) {
                "Error: Failed to parse response"
            }
        }
    }

    data class ModelInfo(
        val id: String,
        val name: String,
        val contextLength: Int,
        val isFree: Boolean
    )
}
