package com.kenkho.nutritracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kenkho.nutritracker.ui.NutriApp
import com.kenkho.nutritracker.ui.NutriTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NutriTheme {
                NutriApp()
            }
        }
    }
}
