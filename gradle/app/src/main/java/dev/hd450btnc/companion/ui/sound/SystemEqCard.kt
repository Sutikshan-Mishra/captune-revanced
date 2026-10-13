package dev.hd450btnc.companion.ui.sound

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.hd450btnc.companion.systemeq.AttachedSession
import dev.hd450btnc.companion.systemeq.GlobalMixState
import dev.hd450btnc.companion.systemeq.SystemEqState
import dev.hd450btnc.companion.ui.components.Badge
import dev.hd450btnc.companion.ui.components.ButtonStyle
import dev.hd450btnc.companion.ui.components.GlyphKind
import dev.hd450btnc.companion.ui.components.InfoRow
import dev.hd450btnc.companion.ui.components.Notice
import dev.hd450btnc.companion.ui.components.Overline
import dev.hd450btnc.companion.ui.components.StudioButton
import dev.hd450btnc.companion.ui.components.StudioCard
import dev.hd450btnc.companion.ui.components.StudioDivider
import dev.hd450btnc.companion.ui.components.ToggleRow
import dev.hd450btnc.companion.ui.components.Tone
import dev.hd450btnc.companion.ui.theme.Space
import dev.hd450btnc.companion.ui.theme.Studio
import dev.hd450btnc.companion.ui.theme.StudioType

data class SystemEqActions(
    val setEnabled: (Boolean) -> Unit,
    val setGlobalMix: (Boolean) -> Unit,
    val dismissPanelRequest: () -> Unit,
)

/**
 * Scope of the EQ, shown truthfully: always this app's player; optionally other apps that announce
 * their audio session. Status lines come only from what the backend actually observed.
 */
@Composable
fun SystemEqCard(state: SystemEqState, globalMix: Boolean, panelRequest: String?, actions: SystemEqActions) {
    val c = Studio.colors
    StudioCard(Modifier.animateContentSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Overline("Where the EQ applies", Modifier.weight(1f))
            Badge("Experimental", Tone.WARNING)
        }
        Spacer(Modifier.height(Space.s))
        Text(
            "Always: music played in CapTune ReVanced. Other apps: only when you switch this on and the app announces its audio session.",
            style = StudioType.bodySmall,
            color = c.textMuted,
        )

        if (panelRequest != null) {
            Spacer(Modifier.height(Space.m))
            Notice(
                if (state.requested) "Opened as the equalizer for $panelRequest. Its session is attached when available."
                else "Opened as the equalizer for $panelRequest. Switch on “Apply to other apps” to equalize it.",
                tone = Tone.NEUTRAL,
            )
            StudioButton("Dismiss", actions.dismissPanelRequest, style = ButtonStyle.GHOST)
        }

        ToggleRow(
            "Apply to other apps",
            "Keeps a notification while on. Works with players that announce their session; many do, some don't, and some apps' audio can't be reached at all.",
            state.requested,
            actions.setEnabled,
        )
        StudioDivider()
        Text(
            state.summary,
            Modifier.padding(vertical = Space.m),
            style = StudioType.label,
            color = when {
                state.workingCount > 0 -> c.accent
                state.requested -> c.text
                else -> c.textMuted
            },
        )

        AnimatedVisibility(state.requested) {
            Column {
                state.sessions.sortedBy { it.packageName }.forEach { SessionRow(it) }
                StudioDivider()
                ToggleRow(
                    "Global output mix (legacy)",
                    "Deprecated Android path. Some phones honour it for all apps; most ignore it. CapTune can only tell whether the phone accepted it, not whether you hear it.",
                    globalMix,
                    actions.setGlobalMix,
                )
                when (state.global) {
                    GlobalMixState.OFF -> Unit
                    GlobalMixState.CREATED_UNVERIFIED -> Notice(state.globalMessage ?: "", tone = Tone.WARNING, glyph = GlyphKind.WARNING)
                    GlobalMixState.REJECTED -> Notice(state.globalMessage ?: "Rejected by this phone.", tone = Tone.NEUTRAL)
                }
                Spacer(Modifier.height(Space.m))
                Notice(
                    "To check it works: play a song in the other app, set the 62 Hz fader to +12 and back to 0, and listen. " +
                        "If nothing changes, that app isn't reachable this way. Not yet verified with Spotify on a real phone.",
                )
            }
        }
    }
}

@Composable
private fun SessionRow(s: AttachedSession) {
    val c = Studio.colors
    val value = when {
        s.error != null -> "Failed: ${s.error}"
        s.mechanism == null -> "Not attached"
        !s.hasControl -> "No control (another effect owns it)"
        else -> "Attached · ${s.mechanism.label}"
    }
    InfoRow(
        label = "${s.packageName} · #${s.sessionId}",
        value = value,
        valueColor = if (s.working) c.accent else c.warning,
    )
}

