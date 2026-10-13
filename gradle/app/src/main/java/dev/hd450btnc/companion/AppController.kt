package dev.hd450btnc.companion

import android.app.Application
import dev.hd450btnc.companion.ambient.AmbientEngine
import dev.hd450btnc.companion.ambient.AmbientInput
import dev.hd450btnc.companion.audio.AudioEngine
import dev.hd450btnc.companion.bt.HeadsetMonitor
import dev.hd450btnc.companion.data.PrefsStore
import dev.hd450btnc.companion.eq.EqController
import dev.hd450btnc.companion.eq.EqRepository
import dev.hd450btnc.companion.eq.EqState
import dev.hd450btnc.companion.settings.AppSettings
import dev.hd450btnc.companion.settings.SettingsRepository
import dev.hd450btnc.companion.systemeq.SystemEqController
import dev.hd450btnc.companion.systemeq.SystemEqService
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
 * playback/system-EQ services even when no activity exists, and survives configuration changes.
 * Main thread only.
 */
class AppController(private val app: Application) {
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
    val ambient = AmbientEngine(app)
    val systemEq = SystemEqController(app)

    /** Session handed to us by a player that opened our effect control panel. */
    private var pendingPanelSession: Pair<Int, String?>? = null

    private val _panelRequest = MutableStateFlow<String?>(null)
    /** Package (or "a player") that opened CapTune as its equalizer, for an on-screen notice. */
    val panelRequest: StateFlow<String?> = _panelRequest.asStateFlow()

    private var saveJob: Job? = null

    init {
        audio.setEqParams(_eq.value.toParams())
        systemEq.apply(_eq.value.toParams())
        systemEq.setRequested(_settings.value.systemEqEnabled)
        applySettings(_settings.value)
    }

    fun updateEq(transform: (EqState) -> EqState) {
        val next = transform(_eq.value)
        if (next == _eq.value) return
        _eq.value = next
        val params = next.toParams()
        audio.setEqParams(params)
        systemEq.apply(params)
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

    // ---- System-wide EQ ----

    fun setSystemEqEnabled(on: Boolean) {
        updateSettings { it.copy(systemEqEnabled = on) }
        systemEq.setRequested(on)
        if (on) startSystemEqService() else SystemEqService.stop(app)
    }

    fun setGlobalMixExperimental(on: Boolean) {
        updateSettings { it.copy(globalMixExperimental = on) }
        if (systemEq.state.value.serviceRunning) systemEq.setGlobalExperimental(on)
    }

    /** Called from the visible activity, where starting a foreground service is allowed. */
    fun ensureSystemEqService() {
        if (_settings.value.systemEqEnabled && !systemEq.state.value.serviceRunning) startSystemEqService()
    }

    private fun startSystemEqService() {
        try {
            SystemEqService.start(app)
        } catch (e: Exception) {
            systemEq.setServiceRunning(false)
        }
    }

    fun onSystemEqServiceStarted() {
        systemEq.setServiceRunning(true)
        systemEq.apply(_eq.value.toParams())
        systemEq.setGlobalExperimental(_settings.value.globalMixExperimental)
        pendingPanelSession?.let { (id, pkg) -> systemEq.attach(id, pkg) }
        pendingPanelSession = null
    }

    /** A player opened CapTune via ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL with its session. */
    fun onEffectPanelRequest(sessionId: Int, packageName: String?) {
        _panelRequest.value = packageName ?: "a player"
        if (sessionId <= 0) return
        if (systemEq.state.value.serviceRunning) systemEq.attach(sessionId, packageName)
        else pendingPanelSession = sessionId to packageName
    }

    fun dismissPanelRequest() {
        _panelRequest.value = null
    }

    // ---- Ambient ----

    fun setAmbientInput(input: AmbientInput) = updateSettings { it.copy(ambientInput = input) }

    /** Writes pending changes immediately (called when the UI goes to the background). */
    fun flush() {
        if (saveJob?.isActive == true) {
            saveJob?.cancel()
            eqRepo.save(_eq.value)
        }
    }

    /** Restores defaults: stops ambient and system EQ, removes profiles, custom curve, settings and the queue. */
    fun resetAll() {
        ambient.stop("Stopped: app data was reset.")
        setSystemEqEnabled(false)
        saveJob?.cancel()
        eqRepo.clear()
        settingsRepo.clear()
        audio.clearQueue()
        _eq.value = EqState()
        _settings.value = AppSettings()
        audio.setEqParams(_eq.value.toParams())
        systemEq.apply(_eq.value.toParams())
        applySettings(_settings.value)
    }

    private fun applySettings(s: AppSettings) {
        audio.setRepeat(s.repeatQueue)
        audio.setPauseOnDisconnect(s.pauseOnDisconnect)
    }
}
