package com.jsmammen.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.jsmammen.OutpatientApp
import com.jsmammen.data.repository.OutpatientRepository

@Composable
fun rememberRepository(): OutpatientRepository {
    val context = LocalContext.current
    return remember(context) {
        (context.applicationContext as OutpatientApp).repository
    }
}
