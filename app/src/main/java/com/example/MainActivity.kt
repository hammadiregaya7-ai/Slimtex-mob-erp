package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ErpAppShell
import com.example.ui.theme.TexERPTheme
import com.example.ui.viewmodel.ErpViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TexERPTheme {
                val erpViewModel: ErpViewModel = viewModel()
                Surface(modifier = Modifier.fillMaxSize()) {
                    ErpAppShell(viewModel = erpViewModel)
                }
            }
        }
    }
}
