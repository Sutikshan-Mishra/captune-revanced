package dev.hd450btnc.companion.ui.headphones

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.hd450btnc.companion.bt.AudioDevice
import dev.hd450btnc.companion.bt.ConnectionStatus
import dev.hd450btnc.companion.bt.HeadsetState
import dev.hd450btnc.companion.ui.components.Badge
import dev.hd450btnc.companion.ui.components.ButtonStyle
import dev.hd450btnc.companion.ui.components.Glyph
import dev.hd450btnc.companion.ui.components.GlyphKind
import dev.hd450btnc.companion.ui.components.InfoRow
import dev.hd450btnc.companion.ui.components.Notice
import dev.hd450btnc.companion.ui.components.Overline
import dev.hd450btnc.companion.ui.components.ScreenHeader
import dev.hd450btnc.companion.ui.components.StudioButton
import dev.hd450btnc.companion.ui.components.StudioCard
import dev.hd450btnc.companion.ui.components.StudioDivider
import dev.hd450btnc.companion.ui.components.Tone
import dev.hd450btnc.companion.ui.components.toneColor
import dev.hd450btnc.companion.ui.theme.Shapes
import dev.hd450btnc.companion.ui.theme.Space
import dev.hd450btnc.companion.ui.theme.Studio
import dev.hd450btnc.companion.ui.theme.StudioType

data class HeadphonesActions(
    val requestPermission: () -> Unit,
    val openAppSettings: () -> Unit,
    val openBluetoothSettings: () -> Unit,
    val refresh: () -> Unit,
)

@Composable
fun HeadphonesScreen(
    state: HeadsetState,
    permissionBlocked: Boolean,
    actions: HeadphonesActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.screen)
            .padding(bottom = Space.xl),
    ) {
        ScreenHeader(overline = "Headphones", title = "Connection & NoiseGard")
        ConnectionCard(state, permissionBlocked, actions)
        Spacer(Modifier.height(Space.l))
        NoiseGardCard()
        Spacer(Modifier.height(Space.l))
        TroubleshootingCard()
    }
}

@Composable
private fun ConnectionCard(state: HeadsetState, permissionBlocked: Boolean, actions: HeadphonesActions) {
    val c = Studio.colors
    val status = state.status
    val tone = when (status) {
        ConnectionStatus.CONNECTED -> Tone.POSITIVE
        ConnectionStatus.PERMISSION_REQUIRED, ConnectionStatus.BLUETOOTH_OFF -> Tone.WARNING
        ConnectionStatus.UNSUPPORTED -> Tone.ERROR
        else -> Tone.NEUTRAL
    }
    val target = state.target

    StudioCard(Modifier.animateContentSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(Shapes.control).background(c.surfaceHigh),
                contentAlignment = Alignment.Center,
            ) { Glyph(GlyphKind.HEADPHONES, toneColor(tone), size = 22.dp) }
            Spacer(Modifier.width(Space.m))
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                Text(
                    if (!state.ready) "Checking…" else status.label,
                    style = StudioType.title,
                    color = if (tone == Tone.NEUTRAL) c.text else toneColor(tone),
                )
                Text(
                    target?.name ?: "Sennheiser HD 4.50 BTNC",
                    style = StudioType.bodySmall,
                    color = c.textMuted,
                )
            }
        }

        Spacer(Modifier.height(Space.m))
        when (status) {
            ConnectionStatus.PERMISSION_REQUIRED -> {
                Text(
                    "To show whether your headphones are connected, Android needs the Nearby devices permission. " +
                        "The app only reads paired and connected devices; it never scans or uses location.",
                    style = StudioType.bodySmall,
                    color = c.textMuted,
                )
                Spacer(Modifier.height(Space.m))
                if (permissionBlocked) {
                    StudioButton("Open app settings", actions.openAppSettings, style = ButtonStyle.FILLED, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(Space.xs))
                    Text("Permission was declined. Enable “Nearby devices” in app settings.", style = StudioType.bodySmall, color = c.textFaint)
                } else {
                    StudioButton("Allow access", actions.requestPermission, style = ButtonStyle.FILLED, modifier = Modifier.fillMaxWidth())
                }
            }
            ConnectionStatus.UNSUPPORTED -> Text("This device has no Bluetooth adapter.", style = StudioType.bodySmall, color = c.textMuted)
            else -> {
                StudioDivider()
                if (target != null) {
                    DeviceDetails(target)
                } else {
                    InfoRow("Paired on this phone", if (state.targetPaired) "Yes" else "No")
                    state.others.forEach { other ->
                        InfoRow("Also connected", other.name)
                    }
                    if (state.others.isNotEmpty()) {
                        Spacer(Modifier.height(Space.xs))
                        Text(
                            "The connected device isn't reporting an HD 4.50 BTNC name. If you renamed your headphones, they still work with the EQ; only detection is affected.",
                            style = StudioType.bodySmall,
                            color = c.textFaint,
                        )
                    }
                }
                Spacer(Modifier.height(Space.m))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    StudioButton("Bluetooth settings", actions.openBluetoothSettings, glyph = GlyphKind.BLUETOOTH, modifier = Modifier.weight(1f))
                    StudioButton("Refresh", actions.refresh, glyph = GlyphKind.RESET, style = ButtonStyle.GHOST)
                }
            }
        }
    }
}

@Composable
private fun DeviceDetails(d: AudioDevice) {
    val c = Studio.colors
    InfoRow("Media audio (A2DP)", if (d.mediaAudio) "Active" else "Not active", valueColor = if (d.mediaAudio) c.accent else c.textMuted)
    InfoRow("Calls (Hands-free)", if (d.calls) "Active" else "Not active", valueColor = if (d.calls) c.text else c.textMuted)
    InfoRow(
        "Battery",
        d.batteryPercent?.let { "$it %" } ?: "Not reported",
        valueColor = if (d.batteryPercent != null) c.text else c.textMuted,
        mono = d.batteryPercent != null,
    )
    InfoRow("Address", "…${d.shortAddress}", mono = true, valueColor = c.textMuted)
    if (d.batteryPercent != null) {
        Text("Battery level as reported by the headset to Android over the hands-free link.", style = StudioType.bodySmall, color = c.textFaint)
    }
}

@Composable
private fun NoiseGardCard() {
    val c = Studio.colors
    var whyOpen by rememberSaveable { mutableStateOf(false) }
    StudioCard(Modifier.animateContentSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Overline("NoiseGard · active noise cancelling")
                Spacer(Modifier.height(Space.xs))
                Text("Switch it on the headphones", style = StudioType.heading, color = c.text)
            }
        }
        Spacer(Modifier.height(Space.s))
        Badge("App control unavailable", Tone.WARNING)
        Spacer(Modifier.height(Space.l))

        HoldShortcut()

        Spacer(Modifier.height(Space.l))
        Text(
            "Over Bluetooth: press and hold Volume + and Volume − together to switch NoiseGard on or off. The LED flashes violet three times to confirm the change.",
            style = StudioType.body,
            color = c.text,
        )
        Spacer(Modifier.height(Space.s))
        Text(
            "With the audio cable: press the On/Off button once.",
            style = StudioType.bodySmall,
            color = c.textMuted,
        )
        Spacer(Modifier.height(Space.m))
        StudioDivider()
        InfoRow("Current state", "Not readable by the phone", valueColor = c.textMuted)
        StudioDivider()

        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { whyOpen = !whyOpen }
                .padding(vertical = Space.m)
                .semantics { stateDescription = if (whyOpen) "Expanded" else "Collapsed" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Why can't the app switch it?", Modifier.weight(1f), style = StudioType.label, color = c.text)
            val rot by animateFloatAsState(if (whyOpen) 180f else 0f, label = "chev")
            Glyph(GlyphKind.CHEVRON_DOWN, c.textMuted, Modifier.rotate(rot), size = 18.dp)
        }
        AnimatedVisibility(whyOpen) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                Bullet("The HD 4.50 BTNC exposes standard Bluetooth audio and call profiles (A2DP, AVRCP, HFP, HSP). None of them carries a noise-cancelling command.")
                Bullet("Sennheiser has not published a control protocol for this model, and its Smart Control app does not support it.")
                Bullet("No verified community protocol for this exact model was found. Sending guessed commands could misbehave, so the app does not try.")
                Bullet("For the same reason the app never shows an ANC on/off state it cannot read.")
            }
        }
    }
}

/** Two keycaps with a looping hold indicator: shows "press both, keep holding" without claiming a duration. */
@Composable
private fun HoldShortcut() {
    val c = Studio.colors
    val t = rememberInfiniteTransition(label = "hold")
    val fill by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 2600
                0f at 0 using LinearEasing
                0f at 400 using LinearEasing
                1f at 2000 using LinearEasing
                1f at 2600
            },
            RepeatMode.Restart,
        ),
        label = "holdFill",
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(Shapes.control)
            .background(c.surfaceRaised)
            .padding(Space.l)
            .clearAndSetSemantics { contentDescription = "Shortcut: hold Volume plus and Volume minus together" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Keycap("+", fill > 0f)
            Text("+", Modifier.padding(horizontal = Space.m), style = StudioType.label, color = c.textFaint)
            Keycap("−", fill > 0f)
        }
        Spacer(Modifier.height(Space.m))
        Box(Modifier.width(150.dp).height(3.dp).clip(Shapes.small).background(c.surfaceHigh)) {
            Box(Modifier.fillMaxWidth(fill).height(3.dp).background(c.accent))
        }
        Spacer(Modifier.height(Space.s))
        Text("HOLD BOTH", style = StudioType.overline, color = c.textMuted)
    }
}

@Composable
private fun Keycap(symbol: String, pressed: Boolean) {
    val c = Studio.colors
    val border = if (pressed) c.accent else c.borderStrong
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(width = 64.dp, height = 52.dp)
                .clip(Shapes.control)
                .background(if (pressed) c.accentSoft else c.surfaceHigh)
                .border(1.5.dp, border, Shapes.control),
            contentAlignment = Alignment.Center,
        ) {
            Text(symbol, style = StudioType.monoLarge, color = if (pressed) c.accent else c.text)
        }
        Spacer(Modifier.height(Space.xs))
        Text("VOL", style = StudioType.overline, color = c.textFaint)
    }
}

@Composable
private fun Bullet(text: String) {
    val c = Studio.colors
    Row {
        Box(Modifier.padding(top = 8.dp).size(4.dp).clip(Shapes.small).background(c.textFaint))
        Spacer(Modifier.width(Space.m))
        Text(text, style = StudioType.bodySmall, color = c.textMuted)
    }
}

private data class Tip(val title: String, val steps: List<String>)

private val TIPS = listOf(
    Tip(
        "Pair the headphones",
        listOf(
            "Switch the headphones off, then press and hold the On/Off button for about 4 seconds until you hear “power on”.",
            "The LED flashing blue and red means pairing mode (it also starts automatically the very first time).",
            "On the phone, open Bluetooth settings and choose the HD 4.50 BTNC.",
        ),
    ),
    Tip(
        "Clear old pairings",
        listOf(
            "Enter pairing mode first (LED flashing blue and red).",
            "Press the multifunction button and the On/Off button together for about 4 seconds.",
            "The LED flashes violet three times; all pairings are removed and the headphones return to pairing mode. Then remove them from the phone's list and pair again.",
        ),
    ),
    Tip(
        "Disconnects or drop-outs",
        listOf(
            "The headphones remember up to 8 devices and reconnect to the last one used. Turn off Bluetooth on other nearby phones, tablets or laptops that might grab the connection.",
            "Keep the phone within a few metres and avoid pockets on the opposite side of the body.",
            "If the problem persists, clear pairings (above) and pair again.",
        ),
    ),
    Tip(
        "Battery isn't shown",
        listOf(
            "The app shows battery only when the headphones report it to Android over the hands-free (calls) link.",
            "Make sure “Phone calls” is enabled for the headphones in Android's Bluetooth device settings.",
            "Otherwise use the headset's own LED and voice prompts.",
        ),
    ),
    Tip(
        "Other apps don't sound different",
        listOf(
            "By default the EQ only processes music played in CapTune ReVanced.",
            "On the Sound tab, switch on \u201cApply to other apps\u201d. It works only with players that announce their audio session; restart playback in the other app after switching it on.",
            "If a player has an \u201cEqualizer\u201d setting that opens the phone's equalizer, choose CapTune ReVanced there.",
            "Some apps can't be reached this way at all. That is an Android limitation, not a fault in the headphones.",
        ),
    ),
    Tip(
        "No sound from the in-app player",
        listOf(
            "Check media volume on the phone and on the headphones.",
            "If a file shows an error, it may have been moved or deleted: remove it from the queue and add it again.",
            "With the audio cable connected the headphones switch Bluetooth off, so playback comes through the cable.",
        ),
    ),
)

@Composable
private fun TroubleshootingCard() {
    val c = Studio.colors
    var open by rememberSaveable { mutableStateOf(-1) }
    StudioCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Space.l, vertical = Space.s)) {
        Overline("Troubleshooting", Modifier.padding(top = Space.s, bottom = Space.xs))
        TIPS.forEachIndexed { i, tip ->
            if (i > 0) StudioDivider()
            val expanded = open == i
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { open = if (expanded) -1 else i }
                    .padding(vertical = Space.m)
                    .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(tip.title, Modifier.weight(1f), style = StudioType.label, color = c.text)
                val rot by animateFloatAsState(if (expanded) 180f else 0f, label = "tipChev")
                Glyph(GlyphKind.CHEVRON_DOWN, c.textMuted, Modifier.rotate(rot), size = 18.dp)
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(bottom = Space.m), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    tip.steps.forEachIndexed { n, s ->
                        Row {
                            Text("${n + 1}", Modifier.width(18.dp), style = StudioType.monoSmall, color = c.accent)
                            Text(s, style = StudioType.bodySmall, color = c.textMuted)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(Space.xs))
        Notice("Steps above follow Sennheiser's HD 4.50 BTNC user manual.", modifier = Modifier.padding(bottom = Space.s))
    }
}
