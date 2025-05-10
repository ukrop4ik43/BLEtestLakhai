package com.test.bletestlakhai

import android.app.Application
import com.test.bletestlakhai.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class BleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@BleApp)
            modules(appModule)
        }
    }
}