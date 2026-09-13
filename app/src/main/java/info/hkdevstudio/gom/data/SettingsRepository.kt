package info.hkdevstudio.gom.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
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
    private val onboardingDoneKey = booleanPreferencesKey("onboarding_done")

    /** 반경(m). 저장된 값이 스텝에 없으면 가장 가까운 스텝으로 보정. */
    val radius: Flow<Int> = context.dataStore.data.map { snapToStep(it[radiusKey] ?: DEFAULT_RADIUS) }

    val excludedCategories: Flow<Set<String>> =
        context.dataStore.data.map { it[excludedCategoriesKey] ?: emptySet() }

    /** 첫 실행(위치 권한) 화면을 이미 지나쳤는지. */
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[onboardingDoneKey] ?: false }

    suspend fun setRadius(value: Int) {
        context.dataStore.edit { it[radiusKey] = snapToStep(value) }
    }

    suspend fun toggleExcludedCategory(category: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[excludedCategoriesKey] ?: emptySet()
            prefs[excludedCategoriesKey] =
                if (category in current) current - category else current + category
        }
    }

    suspend fun clearExcludedCategories() {
        context.dataStore.edit { it[excludedCategoriesKey] = emptySet() }
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[onboardingDoneKey] = true }
    }

    companion object {
        const val DEFAULT_RADIUS = 500
        val RADIUS_STEPS = listOf(200, 500, 1000, 2000)

        fun snapToStep(value: Int): Int = RADIUS_STEPS.minByOrNull { kotlin.math.abs(it - value) } ?: DEFAULT_RADIUS
    }
}
