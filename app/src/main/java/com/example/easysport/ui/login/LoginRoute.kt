package com.example.easysport.ui.login

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

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
            Toast.makeText(context, "Tính năng Quên mật khẩu đang được phát triển", Toast.LENGTH_SHORT).show()
        },
        onSignUp = onSignUp,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onInputChange = viewModel::clearError,
    )
}
