package com.unkwn2.yandexhud

import android.app.Application
import android.content.Context
import org.conscrypt.Conscrypt
import java.security.Security

class HudApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
        FileLogger.write("HudApp", "Conscrypt provider installed")
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }
}