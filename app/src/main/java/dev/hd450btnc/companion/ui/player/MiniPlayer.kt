package dev.hd450btnc.companion.ui.player

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.hd450btnc.companion.audio.PlayerState
import dev.hd450btnc.companion.ui.components.ButtonStyle
import dev.hd450btnc.companion.ui.components.Glyph
import dev.hd450btnc.companion.ui.components.GlyphKind
import dev.hd450btnc.companion.ui.components.IconAction
import dev.hd450btnc.companion.ui.components.Notice
import dev.hd450btnc.companion.ui.components.Overline
import dev.hd450btnc.companion.ui.components.StudioButton
import dev.hd450btnc.companion.ui.components.StudioDivider
import dev.hd450btnc.companion.ui.components.Tone
import dev.hd450btnc.companion.ui.theme.Shapes
import dev.hd450btnc.companion.ui.theme.Space
import dev.hd450btnc.companion.ui.theme.Studio
import dev.hd450btnc.companion.ui.theme.StudioType

data class PlayerActions(
    val togglePlay: () -> Unit,
    val next: () -> Unit,
    val previous: () -> Unit,
    val seekTo: (Long) -> Unit,
    val playAt: (Int) -> Unit,
    val removeAt: (Int) -> Unit,
    val clear: () -> Unit,
    val addMusic: () -> Unit,
    val playReference: () -> Unit,
)

/** Compact transport docked above the navigation bar on every screen. Tap to open the queue. */
@Composable
fun MiniPlayer(state: PlayerState, eqEnabled: Boolean, actions: PlayerActions, modifier: Modifier = Modifier) {
    val c = Studio.colors
    var sheetOpen by remember { mutableStateOf(false) }
    val progress = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f

    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Space.m)
            .clip(Shapes.card)
            .background(c.surfaceRaised)
            .border(1.dp, c.border, Shapes.card),
    ) {
        // Thin progress line along the top edge.
        Box(Modifier.fillMaxWidth().height(2.dp).background(c.surfaceHigh)) {
            Box(Modifier.fillMaxWidth(progress).height(2.dp).background(if (eqEnabled) c.accent else c.textMuted))
        }
        if (!state.hasMedia) {
            Row(
                Modifier.fillMaxWidth().padding(start = Space.l, end = Space.s, top = Space.s, bottom = Space.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Nothing playing", style = StudioType.label, color = c.text)
                    Text("EQ applies to playback here", style = StudioType.bodySmall, color = c.textFaint)
                }
                StudioButton(if (state.preparingReference) "Preparing" else "Pink noise", actions.playReference, style = ButtonStyle.GHOST, glyph = GlyphKind.WAVE, enabled = !state.preparingReference)
                StudioButton("Add", actions.addMusic, style = ButtonStyle.OUTLINE, glyph = GlyphKind.PLUS)
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClickLabel = "Open queue") { sheetOpen = true }
                    .padding(start = Space.l, end = Space.xs, top = Space.xs, bottom = Space.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(state.current?.title ?: "Ready", style = StudioType.label, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        state.error ?: when {
                            state.isBuffering -> "Loading…"
                            eqEnabled -> "In-app playback · EQ on"
                            else -> "In-app playback · EQ off"
                        },
                        style = StudioType.bodySmall,
                        color = if (state.error != null) c.error else c.textFaint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconAction(GlyphKind.PREVIOUS, "Previous", actions.previous, tint = c.textMuted)
                IconAction(if (state.isPlaying) GlyphKind.PAUSE else GlyphKind.PLAY, if (state.isPlaying) "Pause" else "Play", actions.togglePlay, filled = true)
                IconAction(GlyphKind.NEXT, "Next", actions.next, tint = c.textMuted, enabled = state.queue.size > 1)
            }
        }
    }

    if (sheetOpen) QueueSheet(state, actions) { sheetOpen = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(state: PlayerState, actions: PlayerActions, onDismiss: () -> Unit) {
    val c = Studio.colors
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var scrubbing by remember { mutableStateOf(false) }
    var scrub by remember { mutableFloatStateOf(0f) }
    val duration = state.durationMs.coerceAtLeast(1L)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = c.surface,
        shape = Shapes.card,
        dragHandle = { Box(Modifier.padding(top = Space.m).size(width = 36.dp, height = 4.dp).clip(Shapes.small).background(c.borderStrong)) },
    ) {
        Column(Modifier.padding(horizontal = Space.screen)) {
            Spacer(Modifier.height(Space.l))
            Overline("Now playing")
            Spacer(Modifier.height(Space.xs))
            Text(state.current?.title ?: "—", style = StudioType.title, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (state.error != null) {
                Spacer(Modifier.height(Space.s))
                Notice(state.error, tone = Tone.WARNING, glyph = GlyphKind.WARNING)
            }
            Spacer(Modifier.height(Space.m))
            Slider(
                value = (if (scrubbing) scrub else state.positionMs.toFloat() / duration).coerceIn(0f, 1f),
                onValueChange = { scrubbing = true; scrub = it },
                onValueChangeFinished = { actions.seekTo((scrub * duration).toLong()); scrubbing = false },
                enabled = state.durationMs > 0,
                colors = SliderDefaults.colors(
                    thumbColor = c.accent,
                    activeTrackColor = c.accent,
                    inactiveTrackColor = c.surfaceHigh,
                ),
                modifier = Modifier.semantics { contentDescription = "Playback position" },
            )
            Row {
                Text(formatTime(if (scrubbing) (scrub * duration).toLong() else state.positionMs), style = StudioType.monoSmall, color = c.textMuted)
                Spacer(Modifier.weight(1f))
                Text(formatTime(state.durationMs), style = StudioType.monoSmall, color = c.textMuted)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = Space.s), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                IconAction(GlyphKind.PREVIOUS, "Previous", actions.previous)
                Spacer(Modifier.width(Space.l))
                IconAction(if (state.isPlaying) GlyphKind.PAUSE else GlyphKind.PLAY, if (state.isPlaying) "Pause" else "Play", actions.togglePlay, filled = true)
                Spacer(Modifier.width(Space.l))
                IconAction(GlyphKind.NEXT, "Next", actions.next)
            }
            Spacer(Modifier.height(Space.m))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Overline("Queue · ${state.queue.size}", Modifier.weight(1f))
                StudioButton("Clear", { actions.clear(); onDismiss() }, style = ButtonStyle.GHOST, destructive = true)
            }
            StudioDivider()
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
                itemsIndexed(state.queue, key = { i, item -> "$i:${item.uri}" }) { index, item ->
                    val current = index == state.currentIndex
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button, onClickLabel = "Play") { actions.playAt(index) }
                            .padding(start = Space.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Glyph(if (item.isReference) GlyphKind.WAVE else GlyphKind.CURVE, if (current) c.accent else c.textFaint, size = 18.dp)
                        Spacer(Modifier.width(Space.m))
                        Text(
                            item.title,
                            Modifier.weight(1f),
                            style = StudioType.label,
                            color = if (current) c.accent else c.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        IconAction(GlyphKind.CLOSE, "Remove ${item.title}", { actions.removeAt(index) }, tint = c.textFaint)
                    }
                }
            }
            Spacer(Modifier.height(Space.m))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                StudioButton("Add music", actions.addMusic, glyph = GlyphKind.PLUS, modifier = Modifier.weight(1f))
                StudioButton("Pink noise", actions.playReference, glyph = GlyphKind.WAVE, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(Space.l))
            Notice("Files are opened through Android's file picker; the app gets access only to what you choose. Headset play/pause and skip buttons control this player.")
            Spacer(Modifier.height(Space.xl))
        }
    }
}

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
