package com.example.quarterproject

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.vertexai.FirebaseVertexAI
import com.google.firebase.vertexai.type.content
import com.google.firebase.Firebase
import com.google.firebase.app
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

// Define the UI states to hold the parsed data
sealed interface UiState {
    object Initial : UiState
    object Loading : UiState
    // Success state now holds a list of DetectedObject
    data class Success(val detectedObjects: List<DetectedObject>) : UiState
    data class Error(val errorMessage: String) : UiState
}

class SpatialViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState

    // A Json parser instance that ignores unknown keys in the response
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true // Helpful for minor syntax issues
    }

    // Helper to convert URI to Bitmap
    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun analyzeImage(prompt: String, bitmap: Bitmap) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val generativeModel = FirebaseVertexAI.getInstance(Firebase.app)
                    .generativeModel("gemini-pro-vision")

                // IMPORTANT: Your prompt must ask for a JSON response
                val structuredPrompt = """
                    $prompt
                    
                    Provide the response in a valid JSON array format like this:
                    [
                      {
                        "label": "object_label",
                        "boundingBox": {
                          "x_min": 0.1, "y_min": 0.2, "x_max": 0.3, "y_max": 0.4
                        }
                      }
                    ]
                    Only output the JSON array. Do not include any other text or code fences.
                """.trimIndent()

                val inputContent = content {
                    image(bitmap)
                    text(structuredPrompt)
                }

                val response = generativeModel.generateContent(inputContent)
                val responseText = response.text ?: ""

                // Find the start and end of the JSON array
                val jsonStartIndex = responseText.indexOfFirst { it == '[' }
                val jsonEndIndex = responseText.indexOfLast { it == ']' }

                if (jsonStartIndex != -1 && jsonEndIndex != -1) {
                    val jsonString = responseText.substring(jsonStartIndex, jsonEndIndex + 1)

                    // Parse the JSON string into a list of DetectedObject
                    val detectedObjects = jsonParser.decodeFromString<List<DetectedObject>>(jsonString)
                    _uiState.value = UiState.Success(detectedObjects)
                } else {
                    _uiState.value = UiState.Error("No valid JSON array found in the response.")
                }

            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "An error occurred")
            }
        }
    }
}