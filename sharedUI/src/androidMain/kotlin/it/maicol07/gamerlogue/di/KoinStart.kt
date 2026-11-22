package it.maicol07.gamerlogue.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.mp.KoinPlatform
import org.koin.plugin.module.dsl.startKoin

/**
 * Starts Koin for this process, unless it already runs. Called by the Application, so a process started for a
 * background sync (no activity, no composition) has its dependencies too; the composition's `KoinApplication` then
 * attaches to this instance instead of starting its own.
 */
fun startKoinIfNeeded(context: Context) {
    if (KoinPlatform.getKoinOrNull() != null) return
    startKoin<KoinApp> {
        androidContext(context.applicationContext)
        androidLogger()
    }
}
