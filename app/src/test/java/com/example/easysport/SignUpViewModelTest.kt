package com.example.easysport

import com.example.easysport.data.repository.AuthRepository
import com.example.easysport.data.repository.AuthResult
import com.example.easysport.ui.signup.SignUpForm
import com.example.easysport.ui.signup.SignUpViewModel
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SignUpViewModelTest {

    private fun viewModel() = SignUpViewModel(FakeTestAuthRepository(), Dispatchers.Unconfined)

    @Test
    fun next_blankFields_showsDetailsErrors() {
        val vm = viewModel()
        vm.next()

        val errors = vm.state.value.errors
        assertEquals(1, vm.state.value.step)
        assertEquals("Vui lòng nhập họ tên.", errors.fullName)
        assertEquals("Vui lòng chọn ngày sinh.", errors.dob)
        assertEquals("Số điện thoại không hợp lệ.", errors.phone)
        assertEquals("Email không hợp lệ.", errors.email)
        assertEquals("Vui lòng nhập địa chỉ.", errors.address)
    }

    @Test
    fun next_validDetails_goesToStep2() {
        val vm = viewModel()
        vm.onFormChange(
            SignUpForm(
                fullName = "Nguyen Van A",
                dob = LocalDate.of(2000, 1, 15),
                phone = "0901234567",
                email = "a@example.com",
                address = "Q1, HCM",
            )
        )
        vm.next()

        assertEquals(2, vm.state.value.step)
        assertNull(vm.state.value.errors.fullName)
    }

    @Test
    fun complete_weakPassword_staysOnStep2() {
        val vm = viewModel()
        vm.onFormChange(
            SignUpForm(
                fullName = "Nguyen Van A",
                dob = LocalDate.of(2000, 1, 15),
                phone = "0901234567",
                email = "a@example.com",
                address = "Q1, HCM",
                password = "short",
                confirmPassword = "short",
            )
        )
        vm.next()
        vm.complete { }

        assertEquals(2, vm.state.value.step)
        assertEquals(
            "Mật khẩu cần từ 8 đến 128 ký tự và có ít nhất một chữ số.",
            vm.state.value.errors.password
        )
    }

    @Test
    fun complete_passwordMismatch_showsConfirmError() {
        val vm = viewModel()
        vm.onFormChange(
            SignUpForm(
                fullName = "Nguyen Van A",
                dob = LocalDate.of(2000, 1, 15),
                phone = "0901234567",
                email = "a@example.com",
                address = "Q1, HCM",
                password = "Password1",
                confirmPassword = "Password2",
            )
        )
        vm.next()
        vm.complete { }

        assertEquals("Mật khẩu xác nhận không khớp.", vm.state.value.errors.confirmPassword)
    }

    @Test
    fun complete_errorFromBackend_staysOnStep2WithErrorMessage() {
        val errorRepo = object : FakeTestAuthRepo() {
            override suspend fun register(
                fullName: String,
                dateOfBirth: LocalDate,
                phone: String,
                email: String,
                address: String,
                password: String
            ): AuthResult = AuthResult.Error("Hệ thống máy chủ chưa cập nhật phiên bản xác thực OTP (thiếu mã phiên challengeId). Vui lòng cập nhật backend trên Railway.")
        }
        val vm = SignUpViewModel(errorRepo, Dispatchers.Unconfined)
        fillValidForm(vm)
        vm.next()
        vm.complete { }

        assertEquals(2, vm.state.value.step)
        assertEquals(
            "Hệ thống máy chủ chưa cập nhật phiên bản xác thực OTP (thiếu mã phiên challengeId). Vui lòng cập nhật backend trên Railway.",
            vm.state.value.submitError
        )
    }

    @Test
    fun complete_successVerificationRequired_movesToStep3() {
        val otpRepo = object : FakeTestAuthRepo() {
            override suspend fun register(
                fullName: String,
                dateOfBirth: LocalDate,
                phone: String,
                email: String,
                address: String,
                password: String
            ): AuthResult = AuthResult.VerificationRequired("test-challenge-123")
        }
        val vm = SignUpViewModel(otpRepo, Dispatchers.Unconfined)
        fillValidForm(vm)
        vm.next()
        vm.complete { }

        assertEquals(3, vm.state.value.step)
        assertEquals("test-challenge-123", vm.state.value.challengeId)
        assertNull(vm.state.value.submitError)
    }

    @Test
    fun verify_invalidCode_staysOnStep3WithErrorMessage() {
        val otpRepo = object : FakeTestAuthRepo() {
            override suspend fun register(
                fullName: String,
                dateOfBirth: LocalDate,
                phone: String,
                email: String,
                address: String,
                password: String
            ): AuthResult = AuthResult.VerificationRequired("test-challenge-123")

            override suspend fun verifyRegistration(challengeId: String, code: String): AuthResult {
                return AuthResult.Error("Mã xác nhận không chính xác.")
            }
        }
        val vm = SignUpViewModel(otpRepo, Dispatchers.Unconfined)
        fillValidForm(vm)
        vm.next()
        vm.complete { }

        assertEquals(3, vm.state.value.step)
        vm.verify("999999") { }

        assertEquals(3, vm.state.value.step)
        assertEquals("Mã xác nhận không chính xác.", vm.state.value.submitError)
    }

    @Test
    fun verify_correctCode_callsOnSuccess() {
        val testUser = com.example.easysport.data.model.User(id = "user100", email = "a@example.com", name = "Nguyen Van A")
        val otpRepo = object : FakeTestAuthRepo() {
            override suspend fun register(
                fullName: String,
                dateOfBirth: LocalDate,
                phone: String,
                email: String,
                address: String,
                password: String
            ): AuthResult = AuthResult.VerificationRequired("test-challenge-123")

            override suspend fun verifyRegistration(challengeId: String, code: String): AuthResult {
                return AuthResult.Success(testUser)
            }
        }
        val vm = SignUpViewModel(otpRepo, Dispatchers.Unconfined)
        fillValidForm(vm)
        vm.next()
        vm.complete { }

        var successCalledWith: com.example.easysport.data.model.User? = null
        vm.verify("123456") { user ->
            successCalledWith = user
        }

        assertEquals(testUser, successCalledWith)
        assertEquals(1, vm.state.value.step)
    }

    private fun fillValidForm(vm: SignUpViewModel) {
        vm.onFormChange(
            SignUpForm(
                fullName = "Nguyen Van A",
                dob = LocalDate.of(2000, 1, 15),
                phone = "0901234567",
                email = "a@example.com",
                address = "Q1, HCM",
                password = "Password123",
                confirmPassword = "Password123",
            )
        )
    }
}

open class FakeTestAuthRepo : AuthRepository {
    override suspend fun login(emailOrPhone: String, password: String): AuthResult =
        AuthResult.Success(com.example.easysport.data.model.User(id = "1", email = emailOrPhone, name = "Test"))

    override suspend fun register(
        fullName: String,
        dateOfBirth: LocalDate,
        phone: String,
        email: String,
        address: String,
        password: String
    ): AuthResult = AuthResult.Success(com.example.easysport.data.model.User(id = "2", email = email, name = fullName))

    override suspend fun verifyRegistration(challengeId: String, code: String): AuthResult =
        AuthResult.Error("Mã xác nhận không chính xác.")
}
