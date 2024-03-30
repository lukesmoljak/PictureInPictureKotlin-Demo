package com.example.android.pictureinpicture.presentation.timer

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.example.android.TestDispatcherRule
import com.example.android.pictureinpicture.presentation.util.CoroutinesHelper
import com.example.android.pictureinpicture.presentation.util.SystemClockHelper
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelDelegateImplTest {

    private lateinit var sut: TimerViewModelDelegateImpl

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = TestDispatcherRule()

    @MockK
    private lateinit var systemClockHelper: SystemClockHelper

    @MockK
    private lateinit var coroutinesHelper: CoroutinesHelper

    @RelaxedMockK
    private lateinit var timeObserver: Observer<String>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { systemClockHelper.uptimeMillis() } returns MAR_22_2024_MIDNIGHT_UTC_MILLIS
        sut = TimerViewModelDelegateImpl(
            systemClockHelper = systemClockHelper,
            coroutinesHelper = coroutinesHelper
        )

        sut.time.observeForever(timeObserver)
    }

    @Test
    fun `Given timer has passed one second, When invoke clear, Then timer should be cleared`() = runTest {
        // GIVEN
        givenIsActiveLoopRunsOnce()
        coEvery { systemClockHelper.uptimeMillis() } answers {
            MAR_22_2024_MIDNIGHT_UTC_MILLIS
        } andThenAnswer {
            MAR_22_2024_MIDNIGHT_UTC_MILLIS.plus(ONE_SECOND_MILLIS)
        }

        // start timer
        sut.startOrPause()
        advanceUntilIdle()

        // WHEN
        sut.clear()

        // THEN
        val slotIds = mutableListOf<String>()
        verify { timeObserver.onChanged(capture(slotIds)) }
        // Verify that timer was set and then reset correctly
        assertThat(slotIds[0]).isEqualTo("00:00:00")
        assertThat(slotIds[1]).isEqualTo("00:01:00")
        assertThat(slotIds[2]).isEqualTo("00:00:00")
    }

    @Test
    fun `When invoke startOrPause, Then timer state should be correct`() = runTest {
        givenIsActiveLoopRunsOnce()

        // start timer
        sut.startOrPause()
        advanceUntilIdle()

        assertThat(sut.started.value).isEqualTo(true)

        // pause timer
        sut.startOrPause()
        advanceUntilIdle()

        assertThat(sut.started.value).isEqualTo(false)
    }

    @Test
    fun `Given 12 hours and 34 minutes and 3 and a half hundredths have passed, When started timer, Then display correct time format`() = runTest {
        // GIVEN
        givenIsActiveLoopRunsOnce()
        coEvery { systemClockHelper.uptimeMillis() } answers {
            MAR_22_2024_MIDNIGHT_UTC_MILLIS
        } andThenAnswer {
            MAR_22_2024_MIDNIGHT_UTC_MILLIS
                .plus(TWELVE_MINUTES_MILLIS)
                .plus(THIRTY_FOUR_SECONDS)
                .plus(THREE_AND_A_HALF_HUNDREDTHS_MILLIS)
        }

        // WHEN
        sut.startOrPause()
        advanceUntilIdle()

        // THEN
        val slotIds = mutableListOf<String>()
        verify { timeObserver.onChanged(capture(slotIds)) }
        // Verify that timer was set and then reset correctly
        assertThat(slotIds[0]).isEqualTo("00:00:00")
        assertThat(slotIds[1]).isEqualTo("12:34:35")
    }

    private fun givenIsActiveLoopRunsOnce() {
        coEvery { coroutinesHelper.awaitFrame() } throws CancellationException()
    }

    companion object {
        const val MAR_22_2024_MIDNIGHT_UTC_MILLIS = 1711065600000L
        const val ONE_SECOND_MILLIS = 1000L
        const val THREE_AND_A_HALF_HUNDREDTHS_MILLIS = 350L
        const val THIRTY_FOUR_SECONDS = 34000L
        const val TWELVE_MINUTES_MILLIS = 720000L
    }

}