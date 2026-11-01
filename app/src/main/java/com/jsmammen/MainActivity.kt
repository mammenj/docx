package com.jsmammen

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.jsmammen.ui.disclaimer.DisclaimerScreen
import com.jsmammen.ui.navigation.OutpatientNavHost
import com.jsmammen.ui.theme.OutpatientTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OutpatientTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val context = LocalContext.current
                    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
                    var accepted by remember { mutableStateOf(prefs.getBoolean("disclaimer_accepted", false)) }

                    if (!accepted) {
                        DisclaimerScreen(
                            onAccepted = {
                                prefs.edit().putBoolean("disclaimer_accepted", true).apply()
                                accepted = true
                            },
                        )
                    } else {
                        OutpatientNavHost()
                    }
                }
            }
        }
    }
}
