package com.example.android.pictureinpicture

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private lateinit var sut: MainViewModel

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @MockK
    private lateinit var systemClockHelper: SystemClockHelper

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { systemClockHelper.uptimeMillis() } returns MAR_22_2024_MIDNIGHT_UTC_MILLIS
        sut = MainViewModel(
            systemClockHelper = systemClockHelper
        )
    }

    @Test
    fun `Given paused timer after 1 second, When invoke clear, Then timer should be cleared`() = runTest {
        val expectedStartValue = MAR_22_2024_MIDNIGHT_UTC_MILLIS.plus(ONE_SECOND_MILLIS)
        coEvery { systemClockHelper.uptimeMillis() } returns expectedStartValue
        sut.startOrPause()
        assertThat(sut.time.value).isNotEqualTo("00:00:00")

        sut.clear()

        assertThat(sut.time.value).isEqualTo("00:00:00")
    }

    @Test
    fun `Given timer is started, When invoke clear, Then timer should reset and continue counting`() {
        assertTrue(false)
    }

    @Test
    fun `Given timer is paused, When invoke startOrPause, Then timer should start`() {
        assertTrue(false)
    }

    @Test
    fun `Given timer is started, When invoke startOrPause, Then timer should pause`() {
        assertTrue(false)
    }

    @Test
    fun `Given timer is started, When invoke startOrPause, Then coroutine should gracefully cancel`() {
        assertTrue(false)
    }

    companion object {
        const val MAR_22_2024_MIDNIGHT_UTC_MILLIS = 1711065600000L
        const val ONE_SECOND_MILLIS = 1000L
    }

}