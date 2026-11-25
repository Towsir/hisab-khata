package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "hishab_khata_preferences"
)

class ThemePreferences(private val context: Context) {

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val themeModeFlow: Flow<ThemeMode> = context.applicationContext.themeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            when (prefs[KEY_THEME_MODE]) {
                ThemeMode.LIGHT.name -> ThemeMode.LIGHT
                ThemeMode.DARK.name -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.applicationContext.themeDataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }
}
