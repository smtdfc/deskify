package com.smtdfc.deskify.feature.diagnostics

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smtdfc.deskify.core.detection.CapabilityDetector
import com.smtdfc.deskify.core.detection.CapabilityReport
import com.smtdfc.deskify.core.detection.CheckResult
import com.smtdfc.deskify.core.detection.CheckStatus
import com.smtdfc.deskify.core.detection.SupportLevel
import com.smtdfc.deskify.core.detection.verdictLabel

/**
 * The diagnostics + setup + phone-as-touchpad entry point.
 *
 * This is the *primary-display* face of the app. It is declared as a
 * HOME launcher in the manifest so the user can set Deskify as their
 * default launcher; when they do, this screen is what they see on
 * the phone. The external display gets its own
 * [com.smtdfc.deskify.desktop.DesktopActivity].
 *
 * Layout: a scrollable column of cards.
 *   1. Verdict banner (SUPPORTED / PARTIAL / UNSUPPORTED).
 *   2. One card per [CheckResult] from the [CapabilityReport].
 *   3. A "Recheck" button that re-runs the detection pass.
 *   4. A "Grant WRITE_SECURE_SETTINGS" card, shown only when the
 *      permission check failed.
 *   5. A "Phone as touchpad" button, shown when the launch intent
 *      carried the [EXTRA_SHOW_TOUCHPAD] flag (stage 4 will fill in
 *      the body of that screen).
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val showTouchpad = intent.getBooleanExtra(EXTRA_SHOW_TOUCHPAD, false)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    DiagnosticsScreen(
                        context = this@MainActivity,
                        showTouchpadButton = showTouchpad,
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_SHOW_TOUCHPAD = "show_touchpad"

        /** Starts the diagnostics screen from anywhere in the app. */
        fun start(context: Context, showTouchpad: Boolean = false) {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(EXTRA_SHOW_TOUCHPAD, showTouchpad)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)
        }
    }
}

/**
 * The actual diagnostics UI. Decoupled from [MainActivity] so it can
 * be hosted in any activity (e.g. a settings activity in a future
 * stage).
 */
@Composable
fun DiagnosticsScreen(
    context: Context,
    showTouchpadButton: Boolean,
) {
    var report by remember { mutableStateOf<CapabilityReport?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    // Run the detection pass when the screen first appears.
    LaunchedEffect(Unit) {
        isChecking = true
        try {
            report = CapabilityDetector.detect(context)
        } finally {
            isChecking = false
        }
    }

    // Re-run on demand via the "Recheck" button.
    fun recheck() {
        isChecking = true
        report = CapabilityDetector.detect(context)
        isChecking = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = "Deskify Diagnostics",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))

        when (val r = report) {
            null -> {
                if (isChecking) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator()
                        Spacer(Modifier.width(16.dp))
                        Text("Running capability checks…")
                    }
                }
            }
            else -> {
                // Verdict banner.
                Card(modifier = Modifier.fillMaxWidth().testTag("verdict_banner")) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = r.verdictLabel(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = when (r.verdict) {
                                SupportLevel.SUPPORTED -> MaterialTheme.colorScheme.primary
                                SupportLevel.PARTIALLY_SUPPORTED ->
                                    MaterialTheme.colorScheme.tertiary
                                SupportLevel.UNSUPPORTED ->
                                    MaterialTheme.colorScheme.error
                            },
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Android ${r.androidSdk} / OEM: ${r.oem}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Individual checks.
                r.checks.forEach { check ->
                    CheckCard(check = check)
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { recheck() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Recheck")
                }

                // Setup card, shown when the permission is missing.
                val permCheck = r.checks.firstOrNull {
                    it.checkId == "permission.write_secure_settings"
                }
                if (permCheck != null && !permCheck.isPassing()) {
                    Spacer(Modifier.height(24.dp))
                    SetupCard(packageName = context.packageName)
                }

                // Phone-as-touchpad launcher (when the flag was set).
                if (showTouchpadButton) {
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { /* stage 4 */ },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Phone as touchpad (stage 4)")
                    }
                }
            }
        }
    }
}

/**
 * A single check card: label + status + detail.
 */
@Composable
fun CheckCard(check: CheckResult) {
    Card(modifier = Modifier.fillMaxWidth().testTag("check_${check.checkId}")) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = check.label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = when (check.status) {
                        CheckStatus.PASS -> "PASS"
                        CheckStatus.FAIL -> "FAIL"
                        CheckStatus.UNAVAILABLE -> "N/A"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = when (check.status) {
                        CheckStatus.PASS -> MaterialTheme.colorScheme.primary
                        CheckStatus.FAIL -> MaterialTheme.colorScheme.error
                        CheckStatus.UNAVAILABLE -> MaterialTheme.colorScheme.secondary
                    },
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = check.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

/**
 * The "grant WRITE_SECURE_SETTINGS" card. Shown only when the app
 * does not hold the permission. The user copies the command and runs
 * it on their PC (or grants it via Shizuku in a later stage).
 */
@Composable
fun SetupCard(packageName: String) {
    val command = "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
    Card(modifier = Modifier.fillMaxWidth().testTag("setup_card")) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Grant WRITE_SECURE_SETTINGS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "The app needs this permission to toggle the freeform / " +
                    "desktop-mode settings that make the external display " +
                    "behave like a desktop. Run this command on your PC:",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = command,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}
