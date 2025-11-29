package com.company.employer

import android.app.Application
import com.company.employer.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber



class EmployerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize Koin
        startKoin {
            androidContext(this@EmployerApp)
            modules(appModule)
        }

        Timber.d("EmployerApp initialized")
    }
}
