package com.zobaze.parkspot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zobaze.parkspot.ui.AuthViewModel
import com.zobaze.parkspot.ui.screens.AuthScreen
import com.zobaze.parkspot.ui.screens.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    App()
                }
            }
        }
    }
}

@Composable
private fun App(authVm: AuthViewModel = viewModel()) {
    val authState by authVm.state.collectAsState()
    val user = authState.user
    if (user == null) {
        AuthScreen(authVm)
    } else {
        HomeScreen(userEmail = user.email ?: "", onSignOut = authVm::signOut)
    }
}
