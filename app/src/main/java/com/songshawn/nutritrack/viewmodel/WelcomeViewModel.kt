package com.songshawn.nutritrack.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for the Welcome screen exposing navigation state via [StateFlow].
 */
@HiltViewModel
class WelcomeViewModel @Inject constructor() : ViewModel() {
    private val _navigateToLogin = MutableStateFlow(false)
    val navigateToLogin: StateFlow<Boolean> = _navigateToLogin.asStateFlow()

    fun onLoginClicked() {
        _navigateToLogin.value = true
    }

    fun onNavigationComplete() {
        _navigateToLogin.value = false
    }
}
