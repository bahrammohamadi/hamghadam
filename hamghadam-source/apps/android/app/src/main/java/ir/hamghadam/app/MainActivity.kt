package ir.hamghadam.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.hamghadam.app.ui.AppRoot

class MainActivity : ComponentActivity() {

    private var activeViewModel: AppViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AppViewModel = viewModel()
            activeViewModel = viewModel
            AppRoot(viewModel)
        }
    }

    override fun onStop() {
        super.onStop()
        activeViewModel?.onAppPaused()
    }

    override fun onStart() {
        super.onStart()
        activeViewModel?.onAppResumed()
    }
}
