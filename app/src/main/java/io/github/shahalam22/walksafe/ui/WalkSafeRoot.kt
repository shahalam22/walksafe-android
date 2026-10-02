package io.github.shahalam22.walksafe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.shahalam22.walksafe.ui.admin.AdminScreen
import io.github.shahalam22.walksafe.ui.guide.GuideScreen
import io.github.shahalam22.walksafe.ui.login.LoginScreen

@Composable
fun WalkSafeRoot(viewModel: RootViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        RootState.Loading -> Box(Modifier.fillMaxSize().background(Color.Black))
        RootState.SignedOut -> LoginScreen()
        is RootState.Guide -> GuideScreen(
            user = s.user,
            onOpenAdmin = viewModel::showAdmin,
            onSignOut = viewModel::signOut,
        )
        is RootState.Admin -> AdminScreen(
            user = s.user,
            onOpenGuide = viewModel::showGuide,
            onSignOut = viewModel::signOut,
        )
    }
}
