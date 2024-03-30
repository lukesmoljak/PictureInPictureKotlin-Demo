package com.example.android.pictureinpicture

import kotlinx.coroutines.android.awaitFrame as awaitNextFrame

class CoroutinesHelper {

    suspend fun awaitFrame() = awaitNextFrame()
}