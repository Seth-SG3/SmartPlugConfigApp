package com.example.smartplugconfig

import android.app.Application
import android.content.Context

class ContextInitialisation  : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContext.initialize(this)
    }
}
object AppContext {
    private lateinit var appContext: Context

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun getContext(): Context {
        if (!::appContext.isInitialized) {
            throw IllegalStateException("AppContext not initialized")
        }
        return appContext
    }
}
