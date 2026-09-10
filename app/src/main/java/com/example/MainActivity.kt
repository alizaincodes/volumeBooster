package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.screens.MainScreen
import com.example.ui.theme.VolumeBoostTheme
import com.example.ui.viewmodel.VolumeBoostViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: VolumeBoostViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val dynamicColor by viewModel.dynamicColor.collectAsState()

            VolumeBoostTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor
            ) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

