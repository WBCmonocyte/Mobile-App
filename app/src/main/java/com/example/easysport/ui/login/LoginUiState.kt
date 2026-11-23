package com.example.easysport.ui.login

import com.example.easysport.data.model.User

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loggedInUser: User? = null,
)
