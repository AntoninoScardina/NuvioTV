package com.nuvio.tv.watchparty

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WatchPartyModule {
    @Provides
    @Singleton
    fun provideWatchPartySession(@ApplicationContext context: Context): WatchPartySession =
        WatchPartySession(
            transportFactory = { WebViewWatchPartyTransport(context) },
            deviceName = { deviceName(context) },
        )

    private fun deviceName(context: Context): String =
        runCatching { Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: Build.MODEL
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WatchPartyEntryPoint {
    fun watchPartySession(): WatchPartySession
}

@Composable
fun rememberWatchPartySession(): WatchPartySession {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        EntryPointAccessors.fromApplication(context, WatchPartyEntryPoint::class.java).watchPartySession()
    }
}
