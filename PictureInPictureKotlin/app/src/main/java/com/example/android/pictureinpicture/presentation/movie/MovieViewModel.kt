package com.example.android.pictureinpicture.presentation.movie

import androidx.lifecycle.ViewModel
import com.example.android.pictureinpicture.presentation.timer.TimerViewModelDelegate

class MovieViewModel(
    timerViewModelDelegate: TimerViewModelDelegate
): ViewModel(), TimerViewModelDelegate by timerViewModelDelegate