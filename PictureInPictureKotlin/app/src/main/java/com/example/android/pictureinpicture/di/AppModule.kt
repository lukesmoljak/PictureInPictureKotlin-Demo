package com.example.android.pictureinpicture.di

import com.example.android.pictureinpicture.CoroutinesHelper
import com.example.android.pictureinpicture.MainViewModel
import com.example.android.pictureinpicture.SystemClockHelper
import org.koin.dsl.module

val appModule = module {
    factory {
        MainViewModel(
            systemClockHelper = get(),
            coroutinesHelper = get()
        )
    }

    factory {
        SystemClockHelper()
    }

    factory {
        CoroutinesHelper()
    }
}