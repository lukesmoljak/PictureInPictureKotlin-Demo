package com.example.android.pictureinpicture.presentation.timer

import androidx.lifecycle.LiveData

interface TimerViewModelDelegate {
    val started: LiveData<Boolean>
    val time: LiveData<String>
    fun startOrPause()
    fun clear()
}