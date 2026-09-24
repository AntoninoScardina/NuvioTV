package com.nuvio.tv.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.watchparty.WatchPartyMedia
import com.nuvio.tv.watchparty.WatchPartyPlayer
import com.nuvio.tv.watchparty.WatchPartyRole
import com.nuvio.tv.watchparty.WatchPartySession
import com.nuvio.tv.watchparty.WatchPartyState
import com.nuvio.tv.watchparty.WatchPartyStatus
import com.nuvio.tv.watchparty.rememberWatchPartySession
import kotlinx.coroutines.delay

/** Il player di NuvioTV (ExoPlayer o mpv) visto dal Watch Party. */
internal class ControllerWatchPartyPlayer(
    private val controller: PlayerRuntimeController,
) : WatchPartyPlayer {
    override val positionMs: Long
        get() = controller.currentPlaybackPositionMs() ?: 0L

    override val isPlaying: Boolean
        get() = controller.hasActivePlayIntent()

    override val isBuffering: Boolean
        get() = controller.uiState.value.let { it.isBuffering || it.showLoadingOverlay }

    override fun play() = setPaused(false)

    override fun pause() = setPaused(true)

    override fun seekTo(positionMs: Long) {
        controller.seekPlaybackTo(positionMs.coerceAtLeast(0L))
    }

    override fun setPlaybackSpeed(speed: Float) {
        controller.setPlaybackSpeedInternal(speed)
    }

    private fun setPaused(paused: Boolean) {
        if (controller.hasActivePlayIntent() == !paused) return
        controller.userPausedManually = paused
        if (paused) {
            controller.setPlaybackPaused(true)
            if (controller.isUsingMpvEngine()) {
                controller.stopProgressUpdates()
                controller.stopWatchProgressSaving()
            }
            controller.schedulePauseOverlay()
        } else {
            controller.cancelPauseOverlay()
            controller.setPlaybackPaused(false)
            if (controller.isUsingMpvEngine()) {
                controller.startProgressUpdates()
                controller.startWatchProgressSaving()
            }
        }
    }
}

/** Stream corrente condivisibile con gli altri, o null (torrent, URL locali, player non pronto). */
internal fun PlayerRuntimeController.watchPartyMedia(): WatchPartyMedia? {
    val url = currentStreamUrl
    if (url.isBlank() || isTorrentStream || !url.startsWith("http", ignoreCase = true)) return null
    if (url.contains("://127.0.0.1") || url.contains("://localhost")) return null
    return WatchPartyMedia(
        url = url,
        headers = currentHeaders,
        title = contentName ?: title,
        subtitle = currentEpisodeTitle,
        contentId = contentId,
        contentType = contentType,
        videoId = currentVideoId,
        season = currentSeason,
        episode = currentEpisode,
        poster = poster,
        backdrop = backdrop,
        logo = logo,
        streamName = streamName,
    )
}

/** Collega il player aperto alla stanza Watch Party (se attiva). Da chiamare una volta in PlayerScreen. */
@Composable
internal fun WatchPartyPlayerBinding(viewModel: PlayerViewModel) {
    val session = rememberWatchPartySession()
    val controller = viewModel.controller
    val adapter = remember(controller) { ControllerWatchPartyPlayer(controller) }

    LaunchedEffect(adapter) {
        var attachedUrl: String? = null
        while (true) {
            val ui = controller.uiState.value
            val ready = !ui.showLoadingOverlay && ui.error == null && controller.currentPlaybackDurationMs() > 0
            val media = if (ready) controller.watchPartyMedia() else null
            if (media != null && media.url != attachedUrl) {
                session.attachPlayer(adapter, media)
                attachedUrl = media.url
            }
            delay(1_000)
        }
    }
    DisposableEffect(adapter) {
        onDispose { session.detachPlayer(adapter) }
    }
}

@Composable
internal fun WatchPartyPlayerPanel(
    visible: Boolean,
    canShare: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = rememberWatchPartySession()
    val state by session.state.collectAsState()
    val firstButton = remember { FocusRequester() }

    LaunchedEffect(visible, state.status) {
        if (visible) {
            delay(120)
            runCatching { firstButton.requestFocus() }
        }
    }

    PlayerOverlayScaffold(
        visible = visible,
        onDismiss = onDismiss,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = NuvioTheme.spacing.xxxl, vertical = 36.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(460.dp)
                .clip(RoundedCornerShape(NuvioTheme.radii.xl))
                .background(NuvioTheme.colors.BackgroundElevated)
                .padding(NuvioTheme.spacing.xl),
        ) {
            WatchPartyPanelContent(
                state = state,
                session = session,
                canShare = canShare,
                firstButton = firstButton,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun WatchPartyPanelContent(
    state: WatchPartyState,
    session: WatchPartySession,
    canShare: Boolean,
    firstButton: FocusRequester,
    onDismiss: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md)) {
        Text(
            text = stringResource(R.string.watch_party_title),
            style = MaterialTheme.typography.headlineSmall,
            color = NuvioTheme.colors.TextPrimary,
        )

        if (!state.isActive && state.status != WatchPartyStatus.ERROR) {
            Text(
                text = stringResource(
                    if (canShare) R.string.watch_party_create_description else R.string.watch_party_not_shareable
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = NuvioTheme.colors.TextSecondary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)) {
                DialogButton(
                    text = stringResource(R.string.watch_party_create),
                    onClick = { session.createRoom() },
                    isPrimary = true,
                    enabled = canShare,
                    modifier = Modifier.focusRequester(firstButton),
                )
                DialogButton(
                    text = stringResource(R.string.watch_party_close),
                    onClick = onDismiss,
                    isPrimary = false,
                )
            }
            return@Column
        }

        if (state.status == WatchPartyStatus.ERROR) {
            Text(
                text = stringResource(R.string.watch_party_error, state.error.orEmpty()),
                style = MaterialTheme.typography.bodyMedium,
                color = NuvioTheme.colors.TextSecondary,
            )
        } else {
            Text(
                text = stringResource(
                    if (state.role == WatchPartyRole.HOST) R.string.watch_party_share_code else R.string.watch_party_joined_code
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = NuvioTheme.colors.TextSecondary,
            )
            Text(
                text = state.code.orEmpty().chunked(3).joinToString(" "),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                letterSpacing = 4.sp,
                color = NuvioTheme.colors.Primary,
            )
            Text(
                text = when {
                    state.status == WatchPartyStatus.CONNECTING -> stringResource(R.string.watch_party_connecting)
                    state.participants.isEmpty() -> stringResource(R.string.watch_party_waiting)
                    else -> stringResource(R.string.watch_party_participants, state.participants.joinToString(", "))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = NuvioTheme.colors.TextPrimary,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)) {
            DialogButton(
                text = stringResource(R.string.watch_party_close),
                onClick = onDismiss,
                isPrimary = true,
                modifier = Modifier.focusRequester(firstButton),
            )
            DialogButton(
                text = stringResource(R.string.watch_party_leave),
                onClick = { session.leaveRoom() },
                isPrimary = false,
            )
        }
    }
}

/** Piccola etichetta sempre visibile mentre si è in una stanza. */
@Composable
internal fun WatchPartyBadge(modifier: Modifier = Modifier) {
    val session = rememberWatchPartySession()
    val state by session.state.collectAsState()
    if (!state.isActive) return
    Text(
        text = stringResource(R.string.watch_party_badge, state.code.orEmpty(), state.participants.size + 1),
        style = MaterialTheme.typography.labelMedium,
        color = NuvioTheme.colors.TextPrimary,
        modifier = modifier
            .clip(RoundedCornerShape(NuvioTheme.radii.md))
            .background(NuvioTheme.colors.BackgroundElevated.copy(alpha = 0.75f))
            .padding(horizontal = NuvioTheme.spacing.md, vertical = NuvioTheme.spacing.xs),
    )
}
