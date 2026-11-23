package com.example.easysport

import com.example.easysport.ui.signup.SignUpForm
import com.example.easysport.ui.signup.SignUpViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SignUpViewModelTest {

    private fun viewModel() = SignUpViewModel(FakeTestAuthRepository())

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
            "Mật khẩu cần ít nhất 8 ký tự và có một chữ số.",
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
}
