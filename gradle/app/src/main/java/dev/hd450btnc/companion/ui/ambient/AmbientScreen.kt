package dev.hd450btnc.companion.ui.ambient

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.hd450btnc.companion.ambient.AmbientGain
import dev.hd450btnc.companion.ambient.AmbientInput
import dev.hd450btnc.companion.ambient.AmbientPhase
import dev.hd450btnc.companion.ambient.AmbientProcessor
import dev.hd450btnc.companion.ambient.AmbientStatus
import dev.hd450btnc.companion.ambient.Availability
import dev.hd450btnc.companion.ambient.RoutePlan
import dev.hd450btnc.companion.ui.components.ButtonStyle
import dev.hd450btnc.companion.ui.components.Glyph
import dev.hd450btnc.companion.ui.components.GlyphKind
import dev.hd450btnc.companion.ui.components.InfoRow
import dev.hd450btnc.companion.ui.components.Notice
import dev.hd450btnc.companion.ui.components.Overline
import dev.hd450btnc.companion.ui.components.ScreenHeader
import dev.hd450btnc.companion.ui.components.SegmentedChoice
import dev.hd450btnc.companion.ui.components.StatusPill
import dev.hd450btnc.companion.ui.components.StudioButton
import dev.hd450btnc.companion.ui.components.StudioCard
import dev.hd450btnc.companion.ui.components.StudioDivider
import dev.hd450btnc.companion.ui.components.Tone
import dev.hd450btnc.companion.ui.theme.Shapes
import dev.hd450btnc.companion.ui.theme.Space
import dev.hd450btnc.companion.ui.theme.Studio
import dev.hd450btnc.companion.ui.theme.StudioType

enum class MicPermission(val label: String) { GRANTED("Granted"), NOT_ASKED("Asked when you press Start"), DENIED("Denied"), BLOCKED("Denied — enable in app settings") }

data class AmbientActions(
    val start: () -> Unit,
    val stop: () -> Unit,
    val selectInput: (AmbientInput) -> Unit,
    val setGain: (Float) -> Unit,
    val resetGain: () -> Unit,
    val setMuted: (Boolean) -> Unit,
    val openAppSettings: () -> Unit,
)

@Composable
fun AmbientScreen(
    status: AmbientStatus,
    plan: RoutePlan,
    selectedInput: AmbientInput,
    permission: MicPermission,
    actions: AmbientActions,
    modifier: Modifier = Modifier,
) {
    val c = Studio.colors
    val active = status.isActive
    val shownInput = if (active) status.input else selectedInput
    val availability = if (shownInput == AmbientInput.PHONE_MIC) plan.phoneMic else plan.headsetMic

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.screen)
            .padding(bottom = Space.xl),
    ) {
        ScreenHeader(overline = "Ambient", title = "Ambient listening")

        Notice(
            "Not hearing protection, and not a substitute for hearing traffic or alarms. Sound is passed through live and never recorded or saved.",
            tone = Tone.WARNING,
            glyph = GlyphKind.WARNING,
        )
        Spacer(Modifier.height(Space.l))

        StudioCard(Modifier.animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Overline("Passthrough", Modifier.weight(1f))
                val (label, tone) = when (status.phase) {
                    AmbientPhase.IDLE -> "Off" to Tone.NEUTRAL
                    AmbientPhase.STARTING -> "Starting" to Tone.NEUTRAL
                    AmbientPhase.RUNNING -> (if (status.muted) "Running · muted" else "Listening") to Tone.POSITIVE
                    AmbientPhase.ERROR -> "Error" to Tone.ERROR
                }
                StatusPill(label, tone, pulsing = status.phase == AmbientPhase.STARTING)
            }
            status.message?.let {
                Spacer(Modifier.height(Space.s))
                Text(it, style = StudioType.bodySmall, color = if (status.phase == AmbientPhase.ERROR) c.error else c.textMuted)
            }

            Spacer(Modifier.height(Space.l))
            Overline("Microphone")
            Spacer(Modifier.height(Space.s))
            SegmentedChoice(
                options = AmbientInput.entries.toList(),
                selected = shownInput,
                label = { it.label },
                onSelect = { if (!active) actions.selectInput(it) },
            )
            Spacer(Modifier.height(Space.s))
            Text(
                when (shownInput) {
                    AmbientInput.PHONE_MIC -> "Hears what is around the phone, not at your ears. Music keeps playing and is mixed in. Over Bluetooth the sound arrives late (the headset link adds delay)."
                    AmbientInput.HEADSET_MIC -> "Uses the HD 4.50 BTNC microphone over its hands-free (call) link. This switches the headset to mono, call-grade audio; other music usually pauses or degrades until you stop. Not verified on this headset."
                },
                style = StudioType.bodySmall,
                color = c.textMuted,
            )
            if (availability is Availability.Unavailable && !active) {
                Spacer(Modifier.height(Space.s))
                Notice(availability.reason, tone = Tone.WARNING, glyph = GlyphKind.INFO)
            }

            Spacer(Modifier.height(Space.l))
            if (active) {
                StudioButton("Stop", actions.stop, style = ButtonStyle.OUTLINE, glyph = GlyphKind.CLOSE, destructive = true, modifier = Modifier.fillMaxWidth())
            } else {
                StudioButton(
                    "Start ambient listening",
                    actions.start,
                    style = ButtonStyle.FILLED,
                    glyph = GlyphKind.MIC,
                    enabled = availability is Availability.Available && permission != MicPermission.BLOCKED,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (permission == MicPermission.BLOCKED) {
                    StudioButton("Open app settings", actions.openAppSettings, style = ButtonStyle.GHOST, modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(Modifier.height(Space.m))
            StudioDivider()
            InfoRow("Microphone permission", permission.label, valueColor = if (permission == MicPermission.GRANTED) c.text else c.textMuted)
            InfoRow("Input", status.inputDevice ?: "—")
            InfoRow("Output", status.outputDevice ?: plan.mediaOutput?.name ?: "No headphones detected")
            if (status.phase == AmbientPhase.RUNNING) {
                InfoRow("Sample rate", "${status.sampleRate} Hz · ${status.chunkFrames}-frame blocks", mono = true)
                InfoRow(
                    "Delay inside the phone",
                    status.inPhoneLatencyMs?.let { "≈ $it ms" } ?: "measuring…",
                    mono = true,
                )
                Text(
                    "Measured from the audio buffers. The Bluetooth link adds its own delay, which the app cannot measure — see “Measure the total delay” below.",
                    style = StudioType.bodySmall,
                    color = c.textFaint,
                )
            }
        }

        Spacer(Modifier.height(Space.l))
        GainCard(status, actions)

        Spacer(Modifier.height(Space.l))
        StudioCard {
            Overline("Ambient and NoiseGard")
            Spacer(Modifier.height(Space.s))
            Text(
                "These are separate. Ambient listening does not switch NoiseGard. For the most natural result, switch NoiseGard on or off on the headphones: hold Volume + and Volume − together.",
                style = StudioType.bodySmall,
                color = c.textMuted,
            )
        }

        Spacer(Modifier.height(Space.l))
        LatencyGuide()
    }
}

@Composable
private fun GainCard(status: AmbientStatus, actions: AmbientActions) {
    val c = Studio.colors
    StudioCard {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Overline("Ambient gain")
                Spacer(Modifier.height(2.dp))
                Text("Digital gain on the microphone signal", style = StudioType.bodySmall, color = c.textFaint)
            }
            Text(
                AmbientGain.label(status.gainDb, status.muted),
                style = StudioType.monoLarge,
                color = if (status.muted) c.textMuted else c.text,
            )
        }
        Spacer(Modifier.height(Space.m))
        Slider(
            value = AmbientGain.dbToSlider(status.gainDb),
            onValueChange = { actions.setGain(AmbientGain.sliderToDb(it)) },
            colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent, inactiveTrackColor = c.surfaceHigh),
            modifier = Modifier.semantics {
                contentDescription = "Ambient gain"
                stateDescription = AmbientGain.label(status.gainDb, status.muted)
            },
        )
        Row {
            Text(AmbientGain.label(AmbientGain.MIN_DB, false), style = StudioType.monoSmall, color = c.textFaint)
            Spacer(Modifier.weight(1f))
            Text("0 dB = unchanged", style = StudioType.monoSmall, color = c.textFaint)
            Spacer(Modifier.weight(1f))
            Text(AmbientGain.label(AmbientGain.MAX_DB, false), style = StudioType.monoSmall, color = c.textFaint)
        }
        Spacer(Modifier.height(Space.m))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            StudioButton(
                if (status.muted) "Unmute" else "Mute",
                { actions.setMuted(!status.muted) },
                glyph = GlyphKind.MUTE,
                modifier = Modifier.weight(1f),
            )
            StudioButton(
                "Reset to ${AmbientGain.label(AmbientGain.DEFAULT_DB, false)}",
                actions.resetGain,
                style = ButtonStyle.GHOST,
                glyph = GlyphKind.RESET,
                enabled = status.gainDb != AmbientGain.DEFAULT_DB,
            )
        }
        if (status.phase == AmbientPhase.RUNNING) {
            Spacer(Modifier.height(Space.l))
            LevelMeter("Mic level", status.inputPeakDbfs)
            Spacer(Modifier.height(Space.s))
            LevelMeter("To headphones", status.outputPeakDbfs)
        }
        Spacer(Modifier.height(Space.m))
        Text(
            "Gain starts at ${AmbientGain.label(AmbientGain.DEFAULT_DB, false)} every time. Changes are ramped, a limiter caps peaks at −2 dBFS, and your phone volume still applies.",
            style = StudioType.bodySmall,
            color = c.textFaint,
        )
    }
}

/** Peak level from the running engine, -60..0 dBFS. */
@Composable
private fun LevelMeter(label: String, dbfs: Float) {
    val c = Studio.colors
    val fraction by animateFloatAsState(((dbfs + 60f) / 60f).coerceIn(0f, 1f), label = "meter")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(104.dp), style = StudioType.bodySmall, color = c.textMuted)
        Box(Modifier.weight(1f).height(6.dp).clip(Shapes.small).background(c.surfaceHigh)) {
            Box(Modifier.fillMaxWidth(fraction).height(6.dp).background(if (dbfs > -3f) c.warning else c.accent))
        }
        Spacer(Modifier.width(Space.s))
        Text(
            if (dbfs <= AmbientProcessor.SILENCE_DB) "—" else "%.0f dB".format(java.util.Locale.ROOT, dbfs),
            Modifier.width(52.dp),
            style = StudioType.monoSmall,
            color = c.textFaint,
        )
    }
}

@Composable
private fun LatencyGuide() {
    val c = Studio.colors
    var open by rememberSaveable { mutableStateOf(false) }
    StudioCard(Modifier.animateContentSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { open = !open }
                .padding(vertical = Space.xs)
                .semantics { stateDescription = if (open) "Expanded" else "Collapsed" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Measure the total delay", Modifier.weight(1f), style = StudioType.label, color = c.text)
            val rot by animateFloatAsState(if (open) 180f else 0f, label = "lat")
            Glyph(GlyphKind.CHEVRON_DOWN, c.textMuted, Modifier.rotate(rot), size = 18.dp)
        }
        AnimatedVisibility(open) {
            Column(Modifier.padding(top = Space.s), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                listOf(
                    "Start ambient listening with a second phone or a laptop recording sound nearby.",
                    "Hold one ear cup against the recorder's microphone and keep the other device close to the mic in use.",
                    "Clap once, sharply. The recording shows two clicks: the direct clap and the clap replayed by the headphones.",
                    "The time between the two clicks in an audio editor (e.g. Audacity) is the total delay.",
                    "Repeat 3 times and note the median, the phone model, Android version and Phone mic / Headset mic mode.",
                ).forEachIndexed { i, s ->
                    Row {
                        Text("${i + 1}", Modifier.width(18.dp), style = StudioType.monoSmall, color = c.accent)
                        Text(s, style = StudioType.bodySmall, color = c.textMuted)
                    }
                }
            }
        }
    }
}
