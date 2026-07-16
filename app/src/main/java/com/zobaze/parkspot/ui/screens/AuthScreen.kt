package com.zobaze.parkspot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zobaze.parkspot.ui.AuthViewModel

import androidx.compose.ui.tooling.preview.Preview
import com.zobaze.parkspot.ui.AuthUiState

@Composable
fun AuthScreen(vm: AuthViewModel) {
    val state by vm.state.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }

    AuthScreenContent(
        state = state,
        email = email,
        password = password,
        isSignUp = isSignUp,
        onEmailChange = { email = it },
        onPasswordChange = { password = it },
        onToggleMode = {
            isSignUp = !isSignUp
            vm.clearError()
        },
        onAction = {
            if (isSignUp) {
                vm.signUp(email, password)
            } else {
                vm.signIn(email, password)
            }
        }
    )
}

@Composable
fun AuthScreenContent(
    state: AuthUiState,
    email: String,
    password: String,
    isSignUp: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9)), // Cool slate 100 background
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Cool ParkSpot Branding Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color(0xFF2563EB), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "P",
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ParkSpot", 
                    fontSize = 28.sp, 
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Shared parking, no double-booking", 
                    color = Color(0xFF64748B),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // Input Fields with Custom Styling
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email Address") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedLabelColor = Color(0xFF2563EB),
                        unfocusedLabelColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password (min 6 chars)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedLabelColor = Color(0xFF2563EB),
                        unfocusedLabelColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                state.error?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = it,
                        color = Color(0xFFDC2626), // Cool red
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Action Button (Sign In / Sign Up)
                Button(
                    onClick = onAction,
                    enabled = !state.loading && email.isNotBlank() && password.length >= 6,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isSignUp) "Create Account" else "Sign In",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Toggle Mode Button
                TextButton(onClick = onToggleMode) {
                    Text(
                        text = if (isSignUp) "Already have an account? Sign In" else "New to ParkSpot? Sign Up",
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthScreenPreview() {
    androidx.compose.material3.MaterialTheme {
        AuthScreenContent(
            state = AuthUiState(loading = false, error = null),
            email = "employee@company.com",
            password = "password",
            isSignUp = false,
            onEmailChange = {},
            onPasswordChange = {},
            onToggleMode = {},
            onAction = {}
        )
    }
}
