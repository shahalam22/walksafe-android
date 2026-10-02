package io.github.shahalam22.walksafe.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.shahalam22.walksafe.data.auth.AuthRepository
import io.github.shahalam22.walksafe.data.auth.AuthState
import io.github.shahalam22.walksafe.data.model.SignedInUser
import io.github.shahalam22.walksafe.data.prefs.UserPrefsRepository
import io.github.shahalam22.walksafe.guidance.GuidanceController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RootState {
    data object Loading : RootState
    data object SignedOut : RootState
    data class Guide(val user: SignedInUser) : RootState
    data class Admin(val user: SignedInUser) : RootState
}

/** Which screen to show: sign-in, the guidance screen, or (for admins) the dashboard. */
@HiltViewModel
class RootViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val prefs: UserPrefsRepository,
    private val guidance: GuidanceController,
) : ViewModel() {

    val state: StateFlow<RootState> = combine(auth.state, prefs.prefs) { a, p ->
        when (a) {
            AuthState.Loading -> RootState.Loading
            AuthState.SignedOut -> RootState.SignedOut
            is AuthState.SignedIn ->
                if (a.user.isAdmin && !p.adminOnGuide) RootState.Admin(a.user) else RootState.Guide(a.user)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RootState.Loading)

    fun showGuide() {
        viewModelScope.launch { prefs.setAdminOnGuide(true) }
    }

    fun showAdmin() {
        guidance.stop()
        viewModelScope.launch { prefs.setAdminOnGuide(false) }
    }

    fun signOut() {
        guidance.stop()
        viewModelScope.launch { auth.signOut() }
    }
}
