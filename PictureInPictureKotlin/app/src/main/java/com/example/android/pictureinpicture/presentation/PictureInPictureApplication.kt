package com.example.android.pictureinpicture.presentation

import android.app.Application
import com.example.android.pictureinpicture.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin

class PictureInPictureApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@PictureInPictureApplication)
            loadKoinModules(
                appModule
            )
        }
    }
}