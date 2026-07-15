package com.zobaze.parkspot.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.zobaze.parkspot.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val user: FirebaseUser? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState(user = repo.currentUser))
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun signIn(email: String, password: String) = authenticate { repo.signIn(email, password) }
    fun signUp(email: String, password: String) = authenticate { repo.signUp(email, password) }

    private fun authenticate(block: suspend () -> Result<FirebaseUser>) {
        if (_state.value.loading) return
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val result = block()
            _state.value = result.fold(
                onSuccess = { AuthUiState(user = it, loading = false) },
                onFailure = { AuthUiState(user = null, loading = false, error = it.friendlyMessage()) },
            )
        }
    }

    fun signOut() {
        repo.signOut()
        _state.value = AuthUiState(user = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun Throwable.friendlyMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
}
