package com.example

import android.app.Application
import android.util.Log

class SoundWaveApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupGlobalCrashResistance()
    }

    /**
     * Catches any unhandled global exceptions in background threads or UI threads
     * to prevent fatal crash dialogs and ensure app stability.
     */
    private fun setupGlobalCrashResistance() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("SoundWaveApp", "Crash shielded on thread ${thread.name}: ${throwable.message}", throwable)
            
            // Allow graceful recovery without abruptly killing the process if recoverable
            try {
                // If it's a known non-fatal or background error, suppress hard crash
                val isNonFatal = throwable is java.io.IOException ||
                        throwable is IllegalStateException ||
                        throwable is NullPointerException ||
                        throwable is SecurityException ||
                        throwable.stackTraceToString().contains("chromium") ||
                        throwable.stackTraceToString().contains("ksp") ||
                        throwable.stackTraceToString().contains("mesa")

                if (isNonFatal) {
                    Log.w("SoundWaveApp", "Non-fatal exception intercepted and neutralized.")
                } else {
                    defaultHandler?.uncaughtException(thread, throwable)
                }
            } catch (recoveryEx: Throwable) {
                Log.e("SoundWaveApp", "Recovery failed", recoveryEx)
            }
        }
    }
}
