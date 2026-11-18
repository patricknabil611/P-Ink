package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "p_ink_settings"
)

class AppSettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val bgId = prefs[KEY_PAD_BACKGROUND] ?: "obsidian"
            val penId = prefs[KEY_PEN_COLOR] ?: "ice_white"
            val padSizeName = prefs[KEY_PAD_SIZE] ?: WritingPadSize.STANDARD.name
            val padSize = enumValues<WritingPadSize>().firstOrNull { it.name == padSizeName }
                ?: WritingPadSize.STANDARD

            AppSettings(
                padBackgroundId = bgId,
                penColorId = penId,
                showGuideLines = prefs[KEY_SHOW_GUIDES] ?: true,
                autoConvertEnabled = prefs[KEY_AUTO_CONVERT] ?: true,
                writingPadSize = padSize,
                compactLists = prefs[KEY_COMPACT_LISTS] ?: false,
                cloudSyncEnabled = prefs[KEY_CLOUD_SYNC] ?: true,
            )
        }

    suspend fun setPadBackground(optionId: String) {
        val bg = InkColorPalettes.findBackground(optionId)
        dataStore.edit { prefs ->
            prefs[KEY_PAD_BACKGROUND] = bg.id
            // Ensure high contrast automatically when switching between light paper and dark ink pads
            val currentPen = InkColorPalettes.findPen(prefs[KEY_PEN_COLOR] ?: "ice_white")
            if (bg.isLight && currentPen.id == "ice_white") {
                prefs[KEY_PEN_COLOR] = "charcoal"
            } else if (!bg.isLight && currentPen.id == "charcoal") {
                prefs[KEY_PEN_COLOR] = "ice_white"
            }
        }
    }

    suspend fun setPenColor(optionId: String) {
        val pen = InkColorPalettes.findPen(optionId)
        dataStore.edit { prefs ->
            prefs[KEY_PEN_COLOR] = pen.id
        }
    }

    suspend fun setShowGuideLines(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_SHOW_GUIDES] = enabled
        }
    }

    suspend fun setAutoConvertEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_CONVERT] = enabled
        }
    }

    suspend fun setWritingPadSize(size: WritingPadSize) {
        dataStore.edit { prefs ->
            prefs[KEY_PAD_SIZE] = size.name
        }
    }

    suspend fun setCompactLists(compact: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_COMPACT_LISTS] = compact
        }
    }

    suspend fun setCloudSyncEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_CLOUD_SYNC] = enabled
        }
    }

    private companion object {
        val KEY_PAD_BACKGROUND = stringPreferencesKey("pad_background_id")
        val KEY_PEN_COLOR = stringPreferencesKey("pen_color_id")
        val KEY_SHOW_GUIDES = booleanPreferencesKey("show_guide_lines")
        val KEY_AUTO_CONVERT = booleanPreferencesKey("auto_convert_enabled")
        val KEY_PAD_SIZE = stringPreferencesKey("writing_pad_size")
        val KEY_COMPACT_LISTS = booleanPreferencesKey("compact_lists")
        val KEY_CLOUD_SYNC = booleanPreferencesKey("cloud_sync_enabled")
    }
}
