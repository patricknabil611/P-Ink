package com.example.ai

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import java.util.UUID

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        if (BuildConfig.DEBUG) {
            val persistenceKey = FirebaseApp.getInstance().persistenceKey
            val debugStorePrefs = context.getSharedPreferences(
                "$DEBUG_STORE_PREFIX$persistenceKey",
                Context.MODE_PRIVATE
            )

            val intentToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)?.trim()
            if (!intentToken.isNullOrEmpty()) {
                // Synchronous commit() ensures DebugAppCheckProviderFactory reads this secret immediately.
                debugStorePrefs.edit()
                    .putString(DEBUG_SECRET_KEY, intentToken)
                    .commit()
                Log.d(TAG, "App Check debug secret configured from launch intent.")
            } else if (debugStorePrefs.getString(DEBUG_SECRET_KEY, null).isNullOrBlank()) {
                val generatedSecret = UUID.randomUUID().toString()
                debugStorePrefs.edit()
                    .putString(DEBUG_SECRET_KEY, generatedSecret)
                    .commit()
                Log.d(TAG, "Generated persistent App Check debug secret for sideloaded debug APK.")
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            val resId = context.resources.getIdentifier(
                RECAPTCHA_SITE_KEY_RES,
                "string",
                context.packageName
            )
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
                } else {
                    Log.e(TAG, "recaptcha_site_key resource string is blank.")
                }
            } else {
                Log.e(TAG, "recaptcha_site_key string resource not found in package.")
            }
        }
    } catch (t: Throwable) {
        Log.e(TAG, "Failed to initialize Firebase App Check", t)
    }
}

fun getAppCheckDebugToken(context: Context): String {
    if (!BuildConfig.DEBUG) return ""
    return try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        val persistenceKey = FirebaseApp.getInstance().persistenceKey
        val prefs = context.getSharedPreferences(
            "$DEBUG_STORE_PREFIX$persistenceKey",
            Context.MODE_PRIVATE
        )
        val existing = prefs.getString(DEBUG_SECRET_KEY, null)
        if (!existing.isNullOrBlank()) {
            existing
        } else {
            val generated = UUID.randomUUID().toString()
            prefs.edit().putString(DEBUG_SECRET_KEY, generated).commit()
            generated
        }
    } catch (t: Throwable) {
        Log.e(TAG, "Unable to read App Check debug token", t)
        ""
    }
}

fun setCustomAppCheckDebugToken(context: Context, token: String): Boolean {
    if (!BuildConfig.DEBUG) return false
    val trimmed = token.trim()
    if (trimmed.isEmpty()) return false
    return try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        val persistenceKey = FirebaseApp.getInstance().persistenceKey
        val prefs = context.getSharedPreferences(
            "$DEBUG_STORE_PREFIX$persistenceKey",
            Context.MODE_PRIVATE
        )
        prefs.edit().putString(DEBUG_SECRET_KEY, trimmed).commit()
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
        true
    } catch (t: Throwable) {
        Log.e(TAG, "Failed to update custom App Check debug token", t)
        false
    }
}
