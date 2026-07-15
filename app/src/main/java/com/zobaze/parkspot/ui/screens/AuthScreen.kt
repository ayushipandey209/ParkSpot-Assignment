package com.zobaze.parkspot.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zobaze.parkspot.ui.AuthViewModel

@Composable
fun AuthScreen(vm: AuthViewModel) {
    val state by vm.state.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("ParkSpot", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("Shared parking, no double-booking", modifier = Modifier.padding(top = 4.dp, bottom = 32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password (min 6 chars)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )

        state.error?.let {
            Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp))
        }

        Button(
            onClick = { if (isSignUp) vm.signUp(email, password) else vm.signIn(email, password) },
            enabled = !state.loading && email.isNotBlank() && password.length >= 6,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(48.dp),
        ) {
            if (state.loading) CircularProgressIndicator(modifier = Modifier.height(20.dp))
            else Text(if (isSignUp) "Create account" else "Sign in")
        }

        TextButton(onClick = { isSignUp = !isSignUp; vm.clearError() }) {
            Text(if (isSignUp) "Have an account? Sign in" else "New here? Create an account")
        }
    }
}
