package info.hkdevstudio.gom.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val radiusKey = intPreferencesKey("radius")
    private val excludedCategoriesKey = stringSetPreferencesKey("excluded_categories")

    val radius: Flow<Int> = context.dataStore.data.map { it[radiusKey] ?: DEFAULT_RADIUS }

    val excludedCategories: Flow<Set<String>> =
        context.dataStore.data.map { it[excludedCategoriesKey] ?: emptySet() }

    suspend fun setRadius(value: Int) {
        context.dataStore.edit { it[radiusKey] = value.coerceIn(MIN_RADIUS, MAX_RADIUS) }
    }

    suspend fun toggleExcludedCategory(category: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[excludedCategoriesKey] ?: emptySet()
            prefs[excludedCategoriesKey] =
                if (category in current) current - category else current + category
        }
    }

    companion object {
        const val DEFAULT_RADIUS = 500
        const val MIN_RADIUS = 100
        const val MAX_RADIUS = 1000
    }
}
