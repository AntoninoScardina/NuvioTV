package com.nuvio.tv.ui.screens.watchparty

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import com.nuvio.tv.ui.navigation.Screen
import com.nuvio.tv.watchparty.rememberWatchPartySession

/** Guest: quando l'host avvia (o cambia) uno stream, apre il player sullo stesso flusso. */
@Composable
fun WatchPartyNavigationEffect(navController: NavHostController) {
    val session = rememberWatchPartySession()
    val request by session.mediaRequest.collectAsState()

    LaunchedEffect(request) {
        val media = request ?: return@LaunchedEffect
        session.consumeMediaRequest()
        val route = Screen.Player.createRoute(
            streamUrl = media.url,
            title = media.title ?: "Watch Party",
            streamName = media.streamName,
            headers = media.headers.takeIf { it.isNotEmpty() },
            contentId = media.contentId,
            contentType = media.contentType,
            contentName = media.title,
            poster = media.poster,
            backdrop = media.backdrop,
            logo = media.logo,
            videoId = media.videoId,
            season = media.season,
            episode = media.episode,
            episodeTitle = media.subtitle,
            startFromBeginning = true,
        )
        val playerOpen = navController.currentDestination?.route == Screen.Player.route
        navController.navigate(route) {
            if (playerOpen) popUpTo(Screen.Player.route) { inclusive = true }
        }
    }
}
