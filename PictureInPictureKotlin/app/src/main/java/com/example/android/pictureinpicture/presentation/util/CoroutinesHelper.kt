package com.example.android.pictureinpicture.presentation.util

import kotlinx.coroutines.android.awaitFrame as awaitNextFrame

class CoroutinesHelper {

    suspend fun awaitFrame() = awaitNextFrame()
}