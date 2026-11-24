package com.example.easysport.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.easysport.data.model.User
import com.example.easysport.data.repository.AuthRepository
import com.example.easysport.data.repository.AuthResult
import com.example.easysport.data.repository.NetworkAuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class SignUpForm(
    val fullName: String = "",
    val dob: LocalDate? = null,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val password: String = "",
    val confirmPassword: String = "",
)

data class SignUpErrors(
    val fullName: String? = null,
    val dob: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val password: String? = null,
    val confirmPassword: String? = null,
) {
    val hasDetailsErrors get() = listOf(fullName, dob, phone, email, address).any { it != null }
    val hasPasswordErrors get() = password != null || confirmPassword != null
}

data class SignUpUiState(
    val step: Int = 1,
    val form: SignUpForm = SignUpForm(),
    val errors: SignUpErrors = SignUpErrors(),
    val isLoading: Boolean = false,
    val submitError: String? = null,
    val challengeId: String? = null,
)

private val EMAIL_RE = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
private val PHONE_RE = Regex("^(\\+84|0)\\d{9,10}$")

class SignUpViewModel(
    private val authRepository: AuthRepository = NetworkAuthRepository.instance,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
) : ViewModel() {

    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    /** Cập nhật form và chỉ xóa lỗi của những trường vừa được sửa. */
    fun onFormChange(new: SignUpForm) = _state.update { s ->
        val o = s.form
        val e = s.errors
        s.copy(
            form = new,
            submitError = null,
            errors = e.copy(
                fullName = e.fullName.takeIf { new.fullName == o.fullName },
                dob = e.dob.takeIf { new.dob == o.dob },
                phone = e.phone.takeIf { new.phone == o.phone },
                email = e.email.takeIf { new.email == o.email },
                address = e.address.takeIf { new.address == o.address },
                password = e.password.takeIf { new.password == o.password },
                confirmPassword = e.confirmPassword.takeIf {
                    new.confirmPassword == o.confirmPassword && new.password == o.password
                },
            ),
        )
    }

    fun next() = _state.update { s ->
        val f = s.form
        val errors = SignUpErrors(
            fullName = if (f.fullName.isBlank()) "Vui lòng nhập họ tên." else null,
            dob = when {
                f.dob == null -> "Vui lòng chọn ngày sinh."
                f.dob.isAfter(LocalDate.now()) -> "Ngày sinh không hợp lệ."
                else -> null
            },
            phone = if (!PHONE_RE.matches(f.phone.filterNot { it == ' ' }))
                "Số điện thoại không hợp lệ." else null,
            email = if (!EMAIL_RE.matches(f.email.trim())) "Email không hợp lệ." else null,
            address = if (f.address.isBlank()) "Vui lòng nhập địa chỉ." else null,
        )
        if (errors.hasDetailsErrors) s.copy(errors = errors) else s.copy(step = 2, errors = SignUpErrors())
    }

    fun back() = _state.update {
        if (it.isLoading) it else it.copy(
            step = if (it.step == 3) 2 else 1, errors = SignUpErrors(), submitError = null,
        )
    }

    fun reset() {
        _state.value = SignUpUiState()
    }

    fun complete(onSuccess: (User) -> Unit) {
        val current = _state.value
        if (current.isLoading) return

        val f = current.form
        val errors = SignUpErrors(
            password = if (f.password.length !in 8..128 || f.password.none { it in '0'..'9' })
                "Mật khẩu cần từ 8 đến 128 ký tự và có ít nhất một chữ số." else null,
            confirmPassword = if (f.confirmPassword != f.password) "Mật khẩu xác nhận không khớp." else null,
        )
        if (errors.hasPasswordErrors) {
            _state.update { it.copy(errors = errors, submitError = null) }
            return
        }

        val dob = f.dob ?: run {
            _state.update { it.copy(submitError = "Vui lòng chọn ngày sinh.") }
            return
        }

        submit(onSuccess) {
            authRepository.register(f.fullName, dob, f.phone, f.email, f.address, f.password)
        }
    }

    fun verify(code: String, onSuccess: (User) -> Unit) {
        val challenge = _state.value.challengeId ?: return
        if (code.length != 6 || code.any { it !in '0'..'9' }) return
        submit(onSuccess) { authRepository.verifyRegistration(challenge, code) }
    }

    private fun submit(onSuccess: (User) -> Unit, action: suspend () -> AuthResult) {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, errors = SignUpErrors(), submitError = null) }
        viewModelScope.launch(dispatcher) {
            try {
                when (val result = action()) {
                    is AuthResult.VerificationRequired -> _state.update {
                        it.copy(step = 3, challengeId = result.challengeId, isLoading = false)
                    }
                    is AuthResult.Success -> {
                        reset()
                        onSuccess(result.user)
                    }
                    is AuthResult.Error -> _state.update {
                        it.copy(isLoading = false, submitError = result.message)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, submitError = e.message ?: "Đăng ký thất bại. Vui lòng thử lại.")
                }
            }
        }
    }
}

class SignUpViewModelFactory(
    private val authRepository: AuthRepository = NetworkAuthRepository.instance,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignUpViewModel::class.java)) {
            return SignUpViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
