package com.example.easysport.ui.login

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginRoute(
    onLoggedIn: () -> Unit,
    onSignUp: () -> Unit,
    onForgotPassword: () -> Unit = {},
    viewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory()),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var resetting by remember { mutableStateOf(false) }
    if (resetting) {
        PasswordResetDialog(onClose = { resetting = false })
    }

    LaunchedEffect(uiState.loggedInUser) {
        val user = uiState.loggedInUser
        if (user != null) {
            com.example.easysport.data.repository.UserStore.update(user)
            onLoggedIn()
        }
    }

    LoginScreen(
        onSignIn = viewModel::login,
        onForgotPassword = {
            onForgotPassword()
            resetting = true
        },
        onSignUp = onSignUp,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onInputChange = viewModel::clearError,
    )
}
