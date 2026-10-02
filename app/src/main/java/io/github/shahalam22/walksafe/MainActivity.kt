package io.github.shahalam22.walksafe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.shahalam22.walksafe.guidance.GuidanceStateHolder
import io.github.shahalam22.walksafe.ui.WalkSafeRoot
import io.github.shahalam22.walksafe.ui.theme.WalkSafeTheme
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var guidance: GuidanceStateHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // While guiding, pressing the power button shows WalkSafe over the lock
        // screen, so the user can tap anywhere to stop.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                guidance.state.collect { setShowWhenLocked(it.active) }
            }
        }

        setContent {
            WalkSafeTheme {
                WalkSafeRoot()
            }
        }
    }
}
