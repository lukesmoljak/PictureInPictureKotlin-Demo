package com.example.android.pictureinpicture.presentation.timer

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.example.android.pictureinpicture.presentation.util.CoroutinesHelper
import com.example.android.pictureinpicture.presentation.util.SystemClockHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.Exception

class TimerViewModelDelegateImpl(
    private val systemClockHelper: SystemClockHelper,
    private val coroutinesHelper: CoroutinesHelper,
) : TimerViewModelDelegate {

    init {
        Log.d("TimerViewModelDelegateImpl", this.hashCode().toString())
    }

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private var startUptimeMillis = systemClockHelper.uptimeMillis()
    private val timeMillis = MutableLiveData(0L)

    private val _started = MutableLiveData(false)

    override val started: LiveData<Boolean> = _started

    override val time = timeMillis.map { millis ->
        val minutes = millis / 1000 / 60
        val m = minutes.toString().padStart(2, '0')
        val seconds = (millis / 1000) % 60
        val s = seconds.toString().padStart(2, '0')
        val hundredths = (millis % 1000) / 10
        val h = hundredths.toString().padStart(2, '0')
        "$m:$s:$h"
    }

    /**
     * Starts the stopwatch if it is not yet started, or pauses it if it is already started.
     */
    override fun startOrPause() {
        if (_started.value == true) {
            _started.value = false
            job?.cancel()
        } else {
            _started.value = true
            job = scope.launch {
                try {
                    start()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private suspend fun CoroutineScope.start() {
        startUptimeMillis = systemClockHelper.uptimeMillis() - (timeMillis.value ?: 0L)
        while (isActive) {
            timeMillis.value = systemClockHelper.uptimeMillis() - startUptimeMillis
            // Updates on every render frame.
            coroutinesHelper.awaitFrame()
        }
    }

    /**
     * Clears the stopwatch to 00:00:00.
     */
    override fun clear() {
        startUptimeMillis = systemClockHelper.uptimeMillis()
        timeMillis.value = 0L
    }
}