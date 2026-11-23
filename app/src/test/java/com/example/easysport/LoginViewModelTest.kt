package com.example.easysport

import com.example.easysport.data.model.User
import com.example.easysport.data.repository.AuthRepository
import com.example.easysport.data.repository.AuthResult
import com.example.easysport.ui.login.LoginViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FakeTestAuthRepository : AuthRepository {
    override suspend fun login(emailOrPhone: String, password: String): AuthResult {
        return if (emailOrPhone == "error@easysport.com") {
            AuthResult.Error("Tài khoản đã bị khóa hoặc không tồn tại.")
        } else {
            AuthResult.Success(User(id = "1", email = emailOrPhone, name = "Test User"))
        }
    }

    override suspend fun register(
        fullName: String,
        dateOfBirth: LocalDate,
        phone: String,
        email: String,
        address: String,
        password: String,
    ): AuthResult {
        return AuthResult.Success(
            User(id = "2", email = email, name = fullName, phone = phone, address = address)
        )
    }
}

class LoginViewModelTest {

    @Test
    fun login_emptyEmail_showsEmailError() {
        val viewModel = LoginViewModel(FakeTestAuthRepository())
        viewModel.login("", "123456")

        val state = viewModel.uiState.value
        assertEquals("Vui lòng nhập địa chỉ email", state.errorMessage)
        assertNull(state.loggedInUser)
    }

    @Test
    fun login_invalidEmailFormat_showsEmailError() {
        val viewModel = LoginViewModel(FakeTestAuthRepository())
        viewModel.login("invalid-email", "123456")

        val state = viewModel.uiState.value
        assertEquals("Định dạng email không hợp lệ (ví dụ: user@example.com)", state.errorMessage)
        assertNull(state.loggedInUser)
    }

    @Test
    fun login_shortPassword_showsPasswordError() {
        val viewModel = LoginViewModel(FakeTestAuthRepository())
        viewModel.login("user@example.com", "123")

        val state = viewModel.uiState.value
        assertEquals("Mật khẩu phải có ít nhất 6 ký tự", state.errorMessage)
        assertNull(state.loggedInUser)
    }
}
