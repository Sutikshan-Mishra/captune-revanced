package dev.hd450btnc.companion.settings

import dev.hd450btnc.companion.eq.KeyValueStore

enum class ThemeMode(val label: String) { SYSTEM("System"), DARK("Dark"), LIGHT("Light") }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val repeatQueue: Boolean = true,
    val pauseOnDisconnect: Boolean = true,
)

class SettingsRepository(private val store: KeyValueStore) {
    fun load(): AppSettings = AppSettings(
        theme = store.getString(THEME)?.let { v -> ThemeMode.entries.firstOrNull { it.name == v } } ?: ThemeMode.SYSTEM,
        repeatQueue = store.getString(REPEAT)?.let { it == "1" } ?: true,
        pauseOnDisconnect = store.getString(NOISY)?.let { it == "1" } ?: true,
    )

    fun save(s: AppSettings) {
        store.putString(THEME, s.theme.name)
        store.putString(REPEAT, if (s.repeatQueue) "1" else "0")
        store.putString(NOISY, if (s.pauseOnDisconnect) "1" else "0")
    }

    fun clear() {
        listOf(THEME, REPEAT, NOISY).forEach { store.putString(it, null) }
    }

    private companion object {
        const val THEME = "settings_theme"
        const val REPEAT = "settings_repeat"
        const val NOISY = "settings_pause_on_disconnect"
    }
}
