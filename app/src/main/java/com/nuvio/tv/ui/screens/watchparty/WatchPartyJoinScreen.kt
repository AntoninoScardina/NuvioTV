@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.watchparty

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.ui.screens.account.InputField
import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.watchparty.WatchPartyProtocol
import com.nuvio.tv.watchparty.WatchPartyRole
import com.nuvio.tv.watchparty.WatchPartyStatus
import com.nuvio.tv.watchparty.rememberWatchPartySession

@Composable
fun WatchPartyJoinScreen(onBackPress: () -> Unit = {}) {
    BackHandler { onBackPress() }

    val session = rememberWatchPartySession()
    val state by session.state.collectAsState()
    var code by remember { mutableStateOf("") }
    var invalidCode by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun join() {
        keyboardController?.hide()
        invalidCode = !session.joinRoom(code)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .background(color = NuvioTheme.colors.BackgroundElevated, shape = RoundedCornerShape(20.dp))
                .padding(NuvioTheme.spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.watch_party_join_title),
                style = MaterialTheme.typography.headlineSmall,
                color = NuvioTheme.colors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(NuvioTheme.spacing.sm))

            val inGuestRoom = state.isActive && state.role == WatchPartyRole.GUEST
            if (inGuestRoom || (state.status == WatchPartyStatus.ERROR && state.role == WatchPartyRole.GUEST)) {
                Text(
                    text = state.code.orEmpty().chunked(3).joinToString(" "),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    letterSpacing = 4.sp,
                    color = NuvioTheme.colors.Primary,
                )
                Spacer(modifier = Modifier.height(NuvioTheme.spacing.md))
                Text(
                    text = when {
                        state.status == WatchPartyStatus.ERROR -> stringResource(R.string.watch_party_error, state.error.orEmpty())
                        state.status == WatchPartyStatus.CONNECTING -> stringResource(R.string.watch_party_connecting)
                        state.participants.isEmpty() -> stringResource(R.string.watch_party_waiting)
                        else -> stringResource(R.string.watch_party_waiting_host)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = NuvioTheme.colors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(NuvioTheme.spacing.xl))
                PrimaryButton(text = stringResource(R.string.watch_party_leave), onClick = { session.leaveRoom() })
                return@Column
            }

            Text(
                text = stringResource(R.string.watch_party_join_description),
                style = MaterialTheme.typography.bodyMedium,
                color = NuvioTheme.colors.TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(NuvioTheme.spacing.xl))
            Text(
                text = stringResource(R.string.watch_party_join_code_label),
                style = MaterialTheme.typography.labelMedium,
                color = NuvioTheme.colors.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NuvioTheme.spacing.xs))
            InputField(
                value = code,
                onValueChange = {
                    code = it.uppercase().filter(Char::isLetterOrDigit).take(WatchPartyProtocol.CODE_LENGTH)
                    invalidCode = false
                },
                placeholder = "ABC123",
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
                onImeAction = ::join,
            )
            if (invalidCode) {
                Spacer(modifier = Modifier.height(NuvioTheme.spacing.sm))
                Text(
                    text = stringResource(R.string.watch_party_invalid_code),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF44336),
                )
            }
            Spacer(modifier = Modifier.height(NuvioTheme.spacing.xl))
            PrimaryButton(
                text = stringResource(R.string.watch_party_join),
                enabled = code.length == WatchPartyProtocol.CODE_LENGTH,
                onClick = ::join,
            )
        }
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.colors(
            containerColor = NuvioTheme.colors.Secondary,
            focusedContainerColor = NuvioTheme.colors.SecondaryVariant,
            contentColor = NuvioTheme.colors.OnSecondary,
            focusedContentColor = NuvioTheme.colors.OnSecondaryVariant,
        ),
        shape = ButtonDefaults.shape(RoundedCornerShape(50)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = NuvioTheme.spacing.xs),
            fontWeight = FontWeight.Medium,
        )
    }
}
