package com.example.easysport.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.easysport.data.model.User
import com.example.easysport.data.repository.AuthRepository
import com.example.easysport.data.repository.AuthResult
import com.example.easysport.data.repository.NetworkAuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
    private val phoneRegex = Regex("^0[0-9]{9,10}\$")

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onLoggedIn(user: User) {
        _uiState.value = LoginUiState(loggedInUser = user)
    }

    fun login(email: String, password: String) {
        if (_uiState.value.isLoading) return

        val currentEmail = email.trim()
        val isEmail = emailRegex.matches(currentEmail)
        val isPhone = phoneRegex.matches(currentEmail)

        val error = when {
            currentEmail.isBlank() -> "Vui lòng nhập địa chỉ email"
            !isEmail && !isPhone -> "Định dạng email không hợp lệ (ví dụ: user@example.com)"
            password.isBlank() -> "Vui lòng nhập mật khẩu"
            password.length < 6 -> "Mật khẩu phải có ít nhất 6 ký tự"
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                when (val result = authRepository.login(currentEmail, password)) {
                    is AuthResult.Success -> {
                        _uiState.update {
                            it.copy(isLoading = false, loggedInUser = result.user)
                        }
                    }
                    is AuthResult.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = result.message)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Đăng nhập thất bại: ${e.localizedMessage ?: "Lỗi không xác định"}"
                    )
                }
            }
        }
    }
}

class LoginViewModelFactory(
    private val authRepository: AuthRepository = NetworkAuthRepository.instance
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
