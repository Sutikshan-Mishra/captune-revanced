package dev.hd450btnc.companion.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hd450btnc.companion.AppController
import dev.hd450btnc.companion.ui.components.Glyph
import dev.hd450btnc.companion.ui.components.GlyphKind
import dev.hd450btnc.companion.ui.headphones.HeadphonesActions
import dev.hd450btnc.companion.ui.headphones.HeadphonesScreen
import dev.hd450btnc.companion.ui.player.MiniPlayer
import dev.hd450btnc.companion.ui.player.PlayerActions
import dev.hd450btnc.companion.ui.settings.SettingsActions
import dev.hd450btnc.companion.ui.settings.SettingsScreen
import dev.hd450btnc.companion.ui.sound.SoundActions
import dev.hd450btnc.companion.ui.sound.SoundScreen
import dev.hd450btnc.companion.ui.theme.Shapes
import dev.hd450btnc.companion.ui.theme.Space
import dev.hd450btnc.companion.ui.theme.Studio
import dev.hd450btnc.companion.ui.theme.StudioType

enum class Destination(val label: String, val glyph: GlyphKind) {
    SOUND("Sound", GlyphKind.CURVE),
    HEADPHONES("Headphones", GlyphKind.HEADPHONES),
    SETTINGS("Settings", GlyphKind.SLIDERS),
}

@Composable
fun AppRoot(controller: AppController) {
    val c = Studio.colors
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val eq by controller.eq.collectAsStateWithLifecycle()
    val settings by controller.settings.collectAsStateWithLifecycle()
    val player by controller.audio.state.collectAsStateWithLifecycle()
    val headset by controller.headset.state.collectAsStateWithLifecycle()
    var dest by rememberSaveable { mutableStateOf(Destination.SOUND) }

    BackHandler(enabled = dest != Destination.SOUND) { dest = Destination.SOUND }

    // --- Bluetooth permission (Android 12+), requested only from the Headphones screen. ---
    val prefs = remember(context) { context.getSharedPreferences("companion", Context.MODE_PRIVATE) }
    var asked by remember { mutableStateOf(prefs.getBoolean(KEY_ASKED, false)) }
    var lastDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        asked = true
        prefs.edit().putBoolean(KEY_ASKED, true).apply()
        lastDenied = !granted
        controller.headset.onPermissionResult()
    }
    val permissionBlocked = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && asked && activity != null &&
        !activity.shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT) &&
        (lastDenied || !controller.headset.hasConnectPermission())

    // --- Audio files via the system picker (no storage permission). ---
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        controller.audio.addUris(uris)
    }

    fun launch(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Nothing to open on this device; the screen already explains the state.
        }
    }

    val playerActions = PlayerActions(
        togglePlay = controller.audio::togglePlay,
        next = controller.audio::next,
        previous = controller.audio::previous,
        seekTo = controller.audio::seekTo,
        playAt = controller.audio::playAt,
        removeAt = controller.audio::removeAt,
        clear = controller.audio::clearQueue,
        addMusic = { pickAudio.launch(arrayOf("audio/*")) },
        playReference = controller.audio::playReference,
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
    ) {
        Column(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = dest,
                modifier = Modifier.weight(1f).statusBarsPadding(),
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                label = "destination",
            ) { d ->
                when (d) {
                    Destination.SOUND -> SoundScreen(
                        eq = eq,
                        headset = headset,
                        actions = SoundActions(
                            selectPreset = controller::selectPreset,
                            setBand = controller::setBand,
                            reset = controller::resetCurve,
                            setEnabled = controller::setEnabled,
                            saveProfile = controller::saveProfile,
                            openHeadphones = { dest = Destination.HEADPHONES },
                        ),
                    )
                    Destination.HEADPHONES -> HeadphonesScreen(
                        state = headset,
                        permissionBlocked = permissionBlocked,
                        actions = HeadphonesActions(
                            requestPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                }
                            },
                            openAppSettings = {
                                launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                            },
                            openBluetoothSettings = { launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                            refresh = controller.headset::refresh,
                        ),
                    )
                    Destination.SETTINGS -> SettingsScreen(
                        eq = eq,
                        settings = settings,
                        actions = SettingsActions(
                            selectProfile = controller::selectPreset,
                            renameProfile = controller::renameProfile,
                            deleteProfile = controller::deleteProfile,
                            setAutoHeadroom = controller::setAutoHeadroom,
                            updateSettings = controller::updateSettings,
                            resetAll = controller::resetAll,
                        ),
                    )
                }
            }
            MiniPlayer(player, eq.enabled, playerActions)
            Spacer(Modifier.height(Space.s))
            BottomBar(dest) { dest = it }
        }
    }
}

@Composable
private fun BottomBar(selected: Destination, onSelect: (Destination) -> Unit) {
    val c = Studio.colors
    Column(Modifier.fillMaxWidth().background(c.surface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
        Row(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Destination.entries.forEach { d ->
                val isSel = d == selected
                val tint by animateColorAsState(if (isSel) c.accent else c.textMuted, label = "navTint")
                val indicator by animateFloatAsState(if (isSel) 1f else 0f, label = "navIndicator")
                Column(
                    Modifier
                        .weight(1f)
                        .height(64.dp)
                        .selectable(selected = isSel, role = Role.Tab) { onSelect(d) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(width = 28.dp, height = 2.dp)
                            .alpha(indicator)
                            .clip(Shapes.small)
                            .background(c.accent),
                    )
                    Spacer(Modifier.height(10.dp))
                    Glyph(d.glyph, tint, size = 22.dp)
                    Spacer(Modifier.height(4.dp))
                    Text(d.label, style = StudioType.monoSmall, color = tint)
                }
            }
        }
    }
}

private const val KEY_ASKED = "bt_permission_asked"

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
