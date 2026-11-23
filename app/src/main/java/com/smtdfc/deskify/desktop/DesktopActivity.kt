package com.smtdfc.deskify.desktop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The desktop launcher that runs on the *external* display.
 *
 * Declared in the manifest with `HOME` + `SECONDARY_HOME` so the
 * system routes the secondary display's HOME intent here. When the
 * user connects a monitor (or enables "Simulate a secondary display"),
 * this activity is created on the secondary display and is the
 * launcher for that display.
 *
 * ## Stage 3 status
 *
 * In this stage the activity is a *shell*: it renders a placeholder
 * desktop background and a bottom taskbar so the user can verify the
 * SECONDARY_HOME routing works. The full desktop UI (icons, start
 * menu, window management) lands in stages 3–4.
 *
 * ## Display routing
 *
 * The activity receives the secondary display as its "default display"
 * automatically, because the manifest declares SECONDARY_HOME. No
 * `Display` extra is required in the intent - the system handles it.
 *
 * ## Graceful disconnect
 *
 * When the external display is disconnected, the system destroys this
 * activity. `onDestroy` is called and the activity is removed from
 * recents. No special handling is needed in this stage; the
 * launcher-service module (stage 5) will re-launch the launcher on
 * the *primary* display if the user wants to keep using it.
 */
class DesktopActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force the desktop theme (dark, no status bar, no nav bar).
        setTheme(com.smtdfc.deskify.R.style.Theme_Deskify_Desktop)

        // Hide the system bars. On API 29+ the immersive-sticky flags
        // are deprecated; the theme's `windowLayoutInDisplayCutoutMode`
        // + `windowFullscreen` already handles the cutout. The legacy
        // flags below still work on API 28-30 and are a no-op on
        // newer versions.
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
            )

        setContent {
            MaterialTheme {
                DesktopShell(context = this@DesktopActivity)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        // The SECONDARY_HOME intent can be re-delivered when the
        // display changes. We do not need to handle it in this stage.
        super.onNewIntent(intent)
    }
}

/**
 * The placeholder desktop UI: a dark background with a title and a
 * bottom taskbar. The real components (icons, start menu, windows)
 * are added in stages 3–4.
 */
@Composable
fun DesktopShell(context: android.content.Context) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F1A)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Deskify Desktop",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE8EAF0),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Stage 3 shell – full desktop UI lands in the next stage",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8A92A6),
            )
        }
    }

    // Bottom taskbar (placeholder – the real taskbar with start menu,
    // pinned apps, and system tray lands in stage 3).
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF11151F))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Taskbar (placeholder)",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF8A92A6),
        )
    }
}
