package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

private const val TAG = "HandwritingRecognizer"

sealed class RecognitionResult {
    data class Success(val text: String) : RecognitionResult()
    data object NoInternet : RecognitionResult()
    data object EmptyOutput : RecognitionResult()
    data object TimedOut : RecognitionResult()
    data class Failure(val message: String) : RecognitionResult()
}

class HandwritingRecognizer(private val appContext: Context) {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.1f
            },
            systemInstruction = content {
                text(
                    "You are an exact handwriting transcription engine for a to-do app. " +
                        "The user writes tasks by hand in Arabic, English, or both mixed in the same line. " +
                        "Read the handwritten image carefully and return ONLY the plain text transcription. " +
                        "Do not add quotes, labels, bullet points, markdown, translations, or any extra words. " +
                        "If the writing is completely illegible or blank, return an empty string."
                )
            }
        )
    }

    suspend fun recognize(bitmap: Bitmap): RecognitionResult {
        if (!isNetworkAvailable()) {
            return RecognitionResult.NoInternet
        }

        return try {
            val response = withTimeout(45_000L) {
                model.generateContent(
                    content {
                        image(bitmap)
                        text("Transcribe the handwritten task text from this image. Return only the plain text.")
                    }
                )
            }
            val rawText = response.text?.trim().orEmpty()
            val cleanedText = rawText
                .removePrefix("\"")
                .removeSuffix("\"")
                .trim()

            if (cleanedText.isBlank()) {
                RecognitionResult.EmptyOutput
            } else {
                RecognitionResult.Success(cleanedText)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini handwriting recognition timed out", e)
            RecognitionResult.TimedOut
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini handwriting recognition failed", e)
            if (isLikelyNetworkException(e) || !isNetworkAvailable()) {
                RecognitionResult.NoInternet
            } else {
                RecognitionResult.Failure(
                    e.localizedMessage ?: "Failed to recognize handwriting."
                )
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun isLikelyNetworkException(throwable: Throwable): Boolean {
        var current: Throwable? = throwable
        while (current != null) {
            when (current) {
                is UnknownHostException,
                is ConnectException,
                is SocketTimeoutException,
                is IOException -> return true
            }
            val msg = current.message?.lowercase().orEmpty()
            if (
                msg.contains("unable to resolve host") ||
                msg.contains("network") ||
                msg.contains("failed to connect") ||
                msg.contains("offline")
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }
}
