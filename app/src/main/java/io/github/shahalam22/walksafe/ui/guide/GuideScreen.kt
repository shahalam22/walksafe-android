package io.github.shahalam22.walksafe.ui.guide

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.shahalam22.walksafe.data.model.SignedInUser
import io.github.shahalam22.walksafe.ui.theme.GuideColors

/**
 * The blind user's screen: black, and one full-screen button. Everything is
 * spoken, so nothing here has to be seen. Guidance keeps going with the
 * screen off.
 */
@Composable
fun GuideScreen(
    user: SignedInUser,
    onOpenAdmin: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: GuideViewModel = hiltViewModel(),
) {
    val g by viewModel.guidance.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val helper by viewModel.helper.collectAsStateWithLifecycle()
    var helperOpen by rememberSaveable { mutableStateOf(false) }

    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.onPermissionsResult(viewModel.hasCamera())
    }

    val on = g.active
    val showView = on && prefs.showView
    val dim = on && !prefs.showView            // nothing to read while guiding: keep it dark
    val label = if (on) "Stop WalkSafe" else "Start WalkSafe"

    Box(Modifier.fillMaxSize().background(GuideColors.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .clickable { viewModel.onTap() }
                .clearAndSetSemantics {
                    contentDescription = label
                    role = Role.Button
                    onClick { viewModel.onTap(); true }
                }
                .safeDrawingPadding()
                .padding(start = 24.dp, end = 24.dp, top = 72.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (showView) Arrangement.Top else Arrangement.Center,
        ) {
            if (showView) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(g.camera, g.panel).forEach { bmp ->
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(GuideColors.dim)) {
                            bmp?.let {
                                Image(it.asImageBitmap(), null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
            }
            Text(
                if (on) "Tap anywhere to stop" else "Tap anywhere to start",
                color = when {
                    dim -> GuideColors.dim
                    showView -> GuideColors.muted
                    else -> GuideColors.text
                },
                fontSize = if (on) 22.sp else 40.sp,
                lineHeight = if (on) 26.sp else 46.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                if (on) g.phrase else "",
                color = if (dim) GuideColors.dim else GuideColors.command,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                if (on && g.detail.isNotBlank()) g.detail else g.statusText,
                color = if (dim) GuideColors.dim else GuideColors.muted,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )
        }

        OutlinedButton(
            onClick = {
                viewModel.stop()
                helperOpen = true
            },
            modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(12.dp),
        ) {
            Text("Helper menu", color = if (dim) GuideColors.dim else GuideColors.muted)
        }
    }

    if (helperOpen) {
        HelperDialog(
            user = user,
            prefs = prefs,
            helper = helper,
            viewModel = viewModel,
            onAllowCamera = {
                val list = buildList {
                    add(Manifest.permission.CAMERA)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
                }
                permissions.launch(list.toTypedArray())
            },
            onOpenAdmin = {
                helperOpen = false
                onOpenAdmin()
            },
            onSignOut = {
                helperOpen = false
                onSignOut()
            },
            onDismiss = { helperOpen = false },
        )
    }
}
