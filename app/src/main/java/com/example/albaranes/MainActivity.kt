package com.example.albaranes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.albaranes.ui.capture.CaptureScreen
import com.example.albaranes.ui.theme.AlbaranesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlbaranesTheme {
                CaptureScreen()
            }
        }
    }
}
