package com.example.android.pictureinpicture.di

import com.example.android.pictureinpicture.presentation.util.CoroutinesHelper
import com.example.android.pictureinpicture.presentation.main.MainViewModel
import com.example.android.pictureinpicture.presentation.util.SystemClockHelper
import com.example.android.pictureinpicture.presentation.movie.MovieViewModel
import com.example.android.pictureinpicture.presentation.timer.TimerViewModelDelegate
import com.example.android.pictureinpicture.presentation.timer.TimerViewModelDelegateImpl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel {
        MainViewModel(
            timerViewModelDelegate = get()
        )
    }

    viewModel {
        MovieViewModel(
            timerViewModelDelegate = get()
        )
    }

    factory {
        SystemClockHelper()
    }

    factory {
        CoroutinesHelper()
    }

    single<TimerViewModelDelegate> {
        TimerViewModelDelegateImpl(
            coroutinesHelper = get(),
            systemClockHelper = get()
        )
    }


}