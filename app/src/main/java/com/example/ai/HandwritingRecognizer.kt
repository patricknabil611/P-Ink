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

    private val primaryModel by lazy {
        createModel("gemini-3.8-flash")
    }

    private val fallbackModel by lazy {
        createModel("gemini-3.5-flash-lite")
    }

    private fun createModel(modelName: String) = Firebase.ai.generativeModel(
        modelName = modelName,
        generationConfig = generationConfig {
            temperature = 0.0f
        },
        systemInstruction = content {
            text(
                "You are a high-precision handwriting OCR engine for a bilingual to-do app. " +
                    "The image shows dark stylus handwriting on a white background in Arabic, English, or both mixed inline. " +
                    "Pay close attention to Arabic dots (nuqat: ب ت ث ن ي ق ف ض ظ ذ ز خ غ ش), hamzas (أ إ آ ء ؤ ئ), " +
                    "ta marbuta vs ha (ة / ه), alif maqsura vs ya (ى / ي), Arabic/Western digits, and English casing. " +
                    "Preserve the exact words, numbers, and natural inline order across Arabic and English without translating, correcting grammar into a different word, or paraphrasing. " +
                    "Return ONLY the exact plain text transcription—no quotes, no labels, no bullet points, no markdown, and no extra commentary. " +
                    "If the image is blank or completely illegible, return an empty string."
            )
        }
    )

    suspend fun recognize(bitmap: Bitmap): RecognitionResult {
        if (!isNetworkAvailable()) {
            return RecognitionResult.NoInternet
        }

        return try {
            val response = withTimeout(60_000L) {
                try {
                    generateWith(primaryModel, bitmap)
                } catch (e: Exception) {
                    if (isModelUnavailableError(e)) {
                        Log.w(TAG, "Primary model unavailable, retrying with gemini-3.5-flash-lite", e)
                        generateWith(fallbackModel, bitmap)
                    } else {
                        throw e
                    }
                }
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
                RecognitionResult.Failure(extractReadableError(e))
            }
        }
    }

    private suspend fun generateWith(
        generativeModel: com.google.firebase.ai.GenerativeModel,
        bitmap: Bitmap,
    ) = generativeModel.generateContent(
        content {
            image(bitmap)
            text(
                "Transcribe the exact handwritten task text (Arabic, English, or mixed) from this image. " +
                    "Output only the plain text."
            )
        }
    )

    private fun isModelUnavailableError(throwable: Throwable): Boolean {
        var current: Throwable? = throwable
        while (current != null) {
            val msg = current.message?.lowercase().orEmpty()
            if (msg.contains("not found") ||
                msg.contains("404") ||
                msg.contains("is not supported for generatecontent")
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun extractReadableError(throwable: Throwable): String {
        val primary = throwable.localizedMessage?.takeIf { it.isNotBlank() }
        val causeMsg = throwable.cause?.localizedMessage?.takeIf { it.isNotBlank() && it != primary }
        return when {
            primary != null && causeMsg != null -> "$primary ($causeMsg)"
            primary != null -> primary
            causeMsg != null -> causeMsg
            else -> "Handwriting conversion failed (${throwable::class.java.simpleName})."
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
                msg.contains("failed to connect") ||
                msg.contains("offline") ||
                msg.contains("network is unreachable")
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }
}
