package com.songshawn.nutritrack.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewModelStateFlowTurbineTest {

    @Test
    fun `WelcomeViewModel emits navigation state transitions via StateFlow`() = runTest {
        val viewModel = WelcomeViewModel()

        viewModel.navigateToLogin.test {
            assertFalse(awaitItem())

            viewModel.onLoginClicked()
            assertTrue(awaitItem())

            viewModel.onNavigationComplete()
            assertFalse(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
