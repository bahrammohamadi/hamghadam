package ir.hamghadam.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.hamghadam.app.ui.AppRoot

class MainActivity : ComponentActivity() {

    private var activeViewModel: AppViewModel? = null

    /** Widget / notification entry points, consumed once by the UI. */
    private val pendingRoute = androidx.compose.runtime.mutableStateOf<String?>(null)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* answered */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        pendingRoute.value = routeFrom(intent)
        setContent {
            val viewModel: AppViewModel = viewModel()
            activeViewModel = viewModel
            AppRoot(
                viewModel = viewModel,
                pendingRoute = pendingRoute.value,
                onRouteHandled = { pendingRoute.value = null },
            )
        }
    }

    /**
     * Notification permission is asked once, on first launch, and only on
     * Android 13+. Refusing keeps every feature working; only reminders and the
     * focus timer lose their system notification.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onStop() {
        super.onStop()
        activeViewModel?.onAppPaused()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute.value = routeFrom(intent)
    }

    private fun routeFrom(intent: Intent?): String? = when (intent?.getStringExtra(EXTRA_ROUTE)) {
        ROUTE_QUICK_ADD -> ROUTE_QUICK_ADD
        ROUTE_START_FOCUS -> ROUTE_START_FOCUS
        else -> null
    }

    override fun onStart() {
        super.onStart()
        activeViewModel?.onAppResumed()
    }

    companion object {
        const val EXTRA_ROUTE = "ir.hamghadam.app.extra.ROUTE"
        const val ROUTE_QUICK_ADD = "quick_add"
        const val ROUTE_START_FOCUS = "start_focus"
    }
}
