package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.VaultViewModel
import com.example.ui.screens.LockScreen
import com.example.ui.screens.NoteDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VaultListScreen
import com.example.ui.theme.VaultNotesTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: VaultViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            VaultNotesTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    VaultApp(activity = this@MainActivity, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun VaultApp(
    activity: FragmentActivity,
    viewModel: VaultViewModel = viewModel()
) {
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()

    Crossfade(targetState = isUnlocked, label = "VaultLockCrossfade") { unlocked ->
        if (!unlocked) {
            LockScreen(viewModel = viewModel, activity = activity)
        } else {
            Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                when (screen) {
                    is VaultViewModel.Screen.VaultList -> {
                        VaultListScreen(viewModel = viewModel)
                    }
                    is VaultViewModel.Screen.NoteDetail -> {
                        NoteDetailScreen(viewModel = viewModel)
                    }
                    is VaultViewModel.Screen.Settings -> {
                        SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
