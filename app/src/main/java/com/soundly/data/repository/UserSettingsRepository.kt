package com.soundly.data.repository

import android.content.Context
import androidx.annotation.StringRes
import com.soundly.R
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userDataStore by preferencesDataStore(name = "user_settings")

enum class ThemeMode {
    SYSTEM, DARK, LIGHT
}

enum class PlayerExpansionMode {
    EXPANSION, ELEVATION
}

enum class PlayerType {
    CLASSIC, MODERN
}

enum class AnimationSpeed(val label: String, val duration: Int) {
    NORMAL("Normal", 1100),
    SLOW("Lenta", 2200)
}

enum class MiniPlayerStyle(val label: String) {
    SOLID("Sólido"),
    TINTED("Tintado"),
    BLUR("Blur")
}

enum class ProgressBarType(val label: String) {
    DEFAULT("Material 3 (Sin opciones)"),
    WAVE("Wave"),
    PLANE("Plane")
}

enum class MiniProgressBarType(val label: String) {
    WAVE("Wave (Predeterminado)"),
    PLANE("Plane"),
    NONE("Nulo")
}

enum class MiniProgressBarThickness(val label: String, val value: Float) {
    NORMAL("Normal", 2.5f),
    MEDIANO("Mediano", 4.5f),
    GORDO("Gordo", 7f)
}

enum class ArtworkShape(val label: String) {
    DEFAULT("Predeterminado"),
    CIRCLE("Circle"),
    SQUARE("Square"),
    ARCH("Arch"),
    PILL("Pill"),
    ARROW("Arrow"),
    PENTAGON("Pentagon"),
    COOKIE_4("4-sided cookie"),
    COOKIE_6("6-sided cookie"),
    COOKIE_7("7-sided cookie"),
    CLOVER_4("4-leaf clover")
}

enum class HomeSectionType(@StringRes val titleRes: Int) {
    USER_PLAYLISTS(R.string.home_section_user_playlists),
    DISCOVER_ALBUMS(R.string.home_section_discover_albums),
    RECENTLY_PLAYED(R.string.home_section_recently_played),
    MONTHLY_RECAP(R.string.home_section_monthly_recap),
    TOP_ARTISTS(R.string.home_section_top_artists),
    RECENTLY_ADDED(R.string.home_section_recently_added),
    RECOMMENDED(R.string.home_section_recommended),
    CLOUD_RECOMMENDATIONS(R.string.home_section_cloud_recommendations)
}

@Singleton
class UserSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HOME_SECTIONS_ORDER = stringPreferencesKey("home_sections_order")
        val PLAYER_EXPANSION_MODE = stringPreferencesKey("player_expansion_mode")
        val EXPANSION_SPEED = stringPreferencesKey("expansion_speed")
        val ELEVATION_SPEED = stringPreferencesKey("elevation_speed")
        val MINI_PLAYER_STYLE = stringPreferencesKey("mini_player_style")
        val PROGRESS_BAR_TYPE = stringPreferencesKey("progress_bar_type")
        val SHOW_THUMB = booleanPreferencesKey("show_thumb")
        val PROGRESS_BAR_THICKNESS = floatPreferencesKey("progress_bar_thickness")
        val ARTWORK_SHAPE = stringPreferencesKey("artwork_shape")
        val PLAYER_TYPE = stringPreferencesKey("player_type")
        val MINI_ARTWORK_SHAPE = stringPreferencesKey("mini_artwork_shape")
        val MINI_PROGRESS_BAR_TYPE = stringPreferencesKey("mini_progress_bar_type")
        val MINI_PROGRESS_BAR_THICKNESS = stringPreferencesKey("mini_progress_bar_thickness")
        val SHOW_MINI_PREVIOUS = booleanPreferencesKey("show_mini_previous")
        val SWIPE_TO_DISMISS = booleanPreferencesKey("swipe_to_dismiss")
        val VIVID_COLORS = booleanPreferencesKey("vivid_colors")
        val DYNAMIC_COLORS_ENABLED = booleanPreferencesKey("dynamic_colors_enabled")
        val LYRICS_EXPANSION_SPEED = stringPreferencesKey("lyrics_expansion_speed")
        val USE_LYRICS_AGSL_ANIMATION = booleanPreferencesKey("use_lyrics_agsl_animation")
        val SHOW_HOME_SECTION_SUBTITLES = booleanPreferencesKey("show_home_section_subtitles")
        val SHOW_HOME_PAGE = booleanPreferencesKey("show_home_page")
        val TEXT_ALIGN_CENTERED = booleanPreferencesKey("text_align_centered")
        val MARQUEE_TEXT_ENABLED = booleanPreferencesKey("marquee_text_enabled")
        val CAROUSEL_ENABLED = booleanPreferencesKey("carousel_enabled")
        val CLOUD_ENABLED = booleanPreferencesKey("cloud_enabled")
    }

    val themeModeFlow: Flow<ThemeMode> = context.userDataStore.data.map { preferences ->
        val themeName = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(themeName)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    val homeSectionsOrderFlow: Flow<List<HomeSectionType>> = context.userDataStore.data.map { preferences ->
        val orderString = preferences[PreferencesKeys.HOME_SECTIONS_ORDER]
        val allSections = HomeSectionType.entries.toList()
        
        if (orderString.isNullOrEmpty()) {
            allSections
        } else {
            val savedOrder = orderString.split(",").mapNotNull {
                try {
                    HomeSectionType.valueOf(it)
                } catch (e: Exception) {
                    null
                }
            }
            
            // Si hay secciones nuevas que no están en el orden guardado, las añadimos al final
            val missingSections = allSections.filter { it !in savedOrder }
            if (missingSections.isNotEmpty()) {
                savedOrder + missingSections
            } else {
                savedOrder
            }
        }
    }

    val playerExpansionModeFlow: Flow<PlayerExpansionMode> = context.userDataStore.data.map { preferences ->
        val modeName = preferences[PreferencesKeys.PLAYER_EXPANSION_MODE] ?: PlayerExpansionMode.EXPANSION.name
        try {
            PlayerExpansionMode.valueOf(modeName)
        } catch (e: Exception) {
            PlayerExpansionMode.EXPANSION
        }
    }

    val expansionSpeedFlow: Flow<AnimationSpeed> = context.userDataStore.data.map { preferences ->
        val speedName = preferences[PreferencesKeys.EXPANSION_SPEED] ?: AnimationSpeed.NORMAL.name
        try {
            AnimationSpeed.valueOf(speedName)
        } catch (e: Exception) {
            AnimationSpeed.NORMAL
        }
    }

    val elevationSpeedFlow: Flow<AnimationSpeed> = context.userDataStore.data.map { preferences ->
        val speedName = preferences[PreferencesKeys.ELEVATION_SPEED] ?: AnimationSpeed.NORMAL.name
        try {
            AnimationSpeed.valueOf(speedName)
        } catch (e: Exception) {
            AnimationSpeed.NORMAL
        }
    }

    val miniPlayerStyleFlow: Flow<MiniPlayerStyle> = context.userDataStore.data.map { preferences ->
        val styleName = preferences[PreferencesKeys.MINI_PLAYER_STYLE] ?: MiniPlayerStyle.TINTED.name
        try {
            MiniPlayerStyle.valueOf(styleName)
        } catch (e: Exception) {
            MiniPlayerStyle.TINTED
        }
    }

    val progressBarTypeFlow: Flow<ProgressBarType> = context.userDataStore.data.map { preferences ->
        val typeName = preferences[PreferencesKeys.PROGRESS_BAR_TYPE] ?: ProgressBarType.WAVE.name
        try {
            ProgressBarType.valueOf(typeName)
        } catch (e: Exception) {
            ProgressBarType.WAVE
        }
    }

    val showThumbFlow: Flow<Boolean> = context.userDataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_THUMB] ?: true
    }

    val progressBarThicknessFlow: Flow<Float> = context.userDataStore.data.map { preferences ->
        preferences[PreferencesKeys.PROGRESS_BAR_THICKNESS] ?: 7f
    }

    val artworkShapeFlow: Flow<ArtworkShape> = context.userDataStore.data.map { preferences ->
        val shapeName = preferences[PreferencesKeys.ARTWORK_SHAPE] ?: ArtworkShape.DEFAULT.name
        try {
            ArtworkShape.valueOf(shapeName)
        } catch (e: Exception) {
            ArtworkShape.DEFAULT
        }
    }

    val playerTypeFlow: Flow<PlayerType> = context.userDataStore.data.map { preferences ->
        val typeName = preferences[PreferencesKeys.PLAYER_TYPE] ?: PlayerType.CLASSIC.name
        try {
            PlayerType.valueOf(typeName)
        } catch (e: Exception) {
            PlayerType.CLASSIC
        }
    }

    val miniArtworkShapeFlow: Flow<ArtworkShape> = context.userDataStore.data.map { preferences ->
        val shapeName = preferences[PreferencesKeys.MINI_ARTWORK_SHAPE] ?: ArtworkShape.DEFAULT.name
        try {
            ArtworkShape.valueOf(shapeName)
        } catch (e: Exception) {
            ArtworkShape.DEFAULT
        }
    }

    val miniProgressBarTypeFlow: Flow<MiniProgressBarType> = context.userDataStore.data.map { preferences ->
        val typeName = preferences[PreferencesKeys.MINI_PROGRESS_BAR_TYPE] ?: MiniProgressBarType.WAVE.name
        try {
            MiniProgressBarType.valueOf(typeName)
        } catch (e: Exception) {
            MiniProgressBarType.WAVE
        }
    }

    val miniProgressBarThicknessFlow: Flow<MiniProgressBarThickness> = context.userDataStore.data.map { preferences ->
        val thicknessName = preferences[PreferencesKeys.MINI_PROGRESS_BAR_THICKNESS] ?: MiniProgressBarThickness.NORMAL.name
        try {
            MiniProgressBarThickness.valueOf(thicknessName)
        } catch (e: Exception) {
            MiniProgressBarThickness.NORMAL
        }
    }

    val showMiniPreviousFlow: Flow<Boolean> = context.userDataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_MINI_PREVIOUS] ?: false
    }

    val swipeToDismissFlow: Flow<Boolean> = context.userDataStore.data.map { preferences ->
        preferences[PreferencesKeys.SWIPE_TO_DISMISS] ?: true
    }

    val vividColorsFlow: Flow<Boolean> = context.userDataStore.data.map { preferences ->
        preferences[PreferencesKeys.VIVID_COLORS] ?: true
    }

    val dynamicColorsEnabledFlow: Flow<Boolean> = context.userDataStore.data.map { preferences ->
        preferences[PreferencesKeys.DYNAMIC_COLORS_ENABLED] ?: false
    }

    val lyricsExpansionSpeedFlow: Flow<AnimationSpeed> = context.userDataStore.data.map { preferences ->
        val speedName = preferences[PreferencesKeys.LYRICS_EXPANSION_SPEED] ?: AnimationSpeed.NORMAL.name
        try {
            AnimationSpeed.valueOf(speedName)
        } catch (e: Exception) {
            AnimationSpeed.NORMAL
        }
    }

    val useLyricsAgslAnimationFlow: Flow<Boolean> = context.userDataStore.data.map { preferences ->
        preferences[PreferencesKeys.USE_LYRICS_AGSL_ANIMATION] ?: false
    }

    val showHomeSectionSubtitlesFlow: Flow<Boolean> = context.userDataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_HOME_SECTION_SUBTITLES] ?: false
    }

    val showHomePageFlow: Flow<Boolean> = context.userDataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_HOME_PAGE] ?: true
    }

    val textAlignCenteredFlow: Flow<Boolean> = context.userDataStore.data.map { preferences ->
        preferences[PreferencesKeys.TEXT_ALIGN_CENTERED] ?: false
    }

    val marqueeTextEnabledFlow: Flow<Boolean> = context.userDataStore.data.map { preferences ->
        preferences[PreferencesKeys.MARQUEE_TEXT_ENABLED] ?: false
    }

    val carouselEnabledFlow: Flow<Boolean> = context.userDataStore.data.map { preferences ->
        preferences[PreferencesKeys.CAROUSEL_ENABLED] ?: false
    }

    val cloudEnabledFlow: Flow<Boolean> = context.userDataStore.data.map { preferences ->
        preferences[PreferencesKeys.CLOUD_ENABLED] ?: true
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun updatePlayerExpansionMode(mode: PlayerExpansionMode) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.PLAYER_EXPANSION_MODE] = mode.name
        }
    }

    suspend fun updateExpansionSpeed(speed: AnimationSpeed) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.EXPANSION_SPEED] = speed.name
        }
    }

    suspend fun updateElevationSpeed(speed: AnimationSpeed) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.ELEVATION_SPEED] = speed.name
        }
    }

    suspend fun updateMiniPlayerStyle(style: MiniPlayerStyle) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.MINI_PLAYER_STYLE] = style.name
        }
    }

    suspend fun updateProgressBarType(type: ProgressBarType) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.PROGRESS_BAR_TYPE] = type.name
        }
    }

    suspend fun updateShowThumb(show: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_THUMB] = show
        }
    }

    suspend fun updateProgressBarThickness(thickness: Float) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.PROGRESS_BAR_THICKNESS] = thickness
        }
    }

    suspend fun updateArtworkShape(shape: ArtworkShape) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.ARTWORK_SHAPE] = shape.name
        }
    }

    suspend fun updatePlayerType(type: PlayerType) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.PLAYER_TYPE] = type.name
        }
    }

    suspend fun updateMiniArtworkShape(shape: ArtworkShape) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.MINI_ARTWORK_SHAPE] = shape.name
        }
    }

    suspend fun updateMiniProgressBarType(type: MiniProgressBarType) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.MINI_PROGRESS_BAR_TYPE] = type.name
        }
    }

    suspend fun updateMiniProgressBarThickness(thickness: MiniProgressBarThickness) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.MINI_PROGRESS_BAR_THICKNESS] = thickness.name
        }
    }

    suspend fun updateShowMiniPrevious(show: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_MINI_PREVIOUS] = show
        }
    }

    suspend fun updateSwipeToDismiss(enabled: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.SWIPE_TO_DISMISS] = enabled
        }
    }

    suspend fun updateVividColors(enabled: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.VIVID_COLORS] = enabled
        }
    }

    suspend fun updateDynamicColorsEnabled(enabled: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_COLORS_ENABLED] = enabled
        }
    }

    suspend fun updateLyricsExpansionSpeed(speed: AnimationSpeed) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.LYRICS_EXPANSION_SPEED] = speed.name
        }
    }

    suspend fun updateUseLyricsAgslAnimation(enabled: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_LYRICS_AGSL_ANIMATION] = enabled
        }
    }

    suspend fun updateShowHomeSectionSubtitles(show: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_HOME_SECTION_SUBTITLES] = show
        }
    }

    suspend fun updateShowHomePage(show: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_HOME_PAGE] = show
        }
    }

    suspend fun updateTextAlignCentered(centered: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.TEXT_ALIGN_CENTERED] = centered
        }
    }

    suspend fun updateMarqueeTextEnabled(enabled: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.MARQUEE_TEXT_ENABLED] = enabled
        }
    }

    suspend fun updateCarouselEnabled(enabled: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.CAROUSEL_ENABLED] = enabled
        }
    }

    suspend fun updateCloudEnabled(enabled: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.CLOUD_ENABLED] = enabled
        }
    }

    suspend fun updateHomeSectionsOrder(order: List<HomeSectionType>) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.HOME_SECTIONS_ORDER] = order.joinToString(",") { it.name }
        }
    }
}
