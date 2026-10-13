package dev.hd450btnc.companion

import android.app.Application
import dev.hd450btnc.companion.audio.AudioEngine
import dev.hd450btnc.companion.bt.HeadsetMonitor
import dev.hd450btnc.companion.data.PrefsStore
import dev.hd450btnc.companion.eq.EqController
import dev.hd450btnc.companion.eq.EqRepository
import dev.hd450btnc.companion.eq.EqState
import dev.hd450btnc.companion.settings.AppSettings
import dev.hd450btnc.companion.settings.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Process-wide owner of app state. Lives in [CompanionApp] so the EQ keeps working for the
 * playback service even when no activity exists, and survives configuration changes.
 * Main thread only.
 */
class AppController(app: Application) {
    private val scope = MainScope()
    private val store = PrefsStore(app)
    private val eqRepo = EqRepository(store)
    private val settingsRepo = SettingsRepository(store)

    private val _eq = MutableStateFlow(eqRepo.load())
    val eq: StateFlow<EqState> = _eq.asStateFlow()

    private val _settings = MutableStateFlow(settingsRepo.load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    val audio = AudioEngine(app, store, scope)
    val headset = HeadsetMonitor(app)

    private var saveJob: Job? = null

    init {
        audio.setEqParams(_eq.value.toParams())
        applySettings(_settings.value)
    }

    fun updateEq(transform: (EqState) -> EqState) {
        val next = transform(_eq.value)
        if (next == _eq.value) return
        _eq.value = next
        audio.setEqParams(next.toParams())
        // Slider drags emit many updates; persist once they settle.
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(400)
            eqRepo.save(_eq.value)
        }
    }

    fun selectPreset(id: String) = updateEq { EqController.selectPreset(it, id) }
    fun setBand(index: Int, gainDb: Float) = updateEq { EqController.setBand(it, index, gainDb) }
    fun resetCurve() = updateEq(EqController::reset)
    fun setEnabled(on: Boolean) = updateEq { EqController.setEnabled(it, on) }
    fun setAutoHeadroom(on: Boolean) = updateEq { EqController.setAutoHeadroom(it, on) }
    fun saveProfile(name: String) = updateEq { EqController.saveProfile(it, name, UUID.randomUUID().toString()) }
    fun renameProfile(id: String, name: String) = updateEq { EqController.renameProfile(it, id, name) }
    fun deleteProfile(id: String) = updateEq { EqController.deleteProfile(it, id) }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        if (next == _settings.value) return
        _settings.value = next
        settingsRepo.save(next)
        applySettings(next)
    }

    /** Writes pending changes immediately (called when the UI goes to the background). */
    fun flush() {
        if (saveJob?.isActive == true) {
            saveJob?.cancel()
            eqRepo.save(_eq.value)
        }
    }

    /** Restores defaults: removes saved profiles, custom curve, settings and the playback queue. */
    fun resetAll() {
        saveJob?.cancel()
        eqRepo.clear()
        settingsRepo.clear()
        audio.clearQueue()
        _eq.value = EqState()
        _settings.value = AppSettings()
        audio.setEqParams(_eq.value.toParams())
        applySettings(_settings.value)
    }

    private fun applySettings(s: AppSettings) {
        audio.setRepeat(s.repeatQueue)
        audio.setPauseOnDisconnect(s.pauseOnDisconnect)
    }
}
