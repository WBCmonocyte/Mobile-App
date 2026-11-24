package com.example.easysport.data.repository

import com.example.easysport.data.local.SessionManager
import com.example.easysport.data.remote.PaymentApiClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NetworkAuthRepository : AuthRepository {

    private val dobFormat: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    override suspend fun login(emailOrPhone: String, password: String): AuthResult {
        val result = PaymentApiClient.login(emailOrPhone, password)
        return result.fold(
            onSuccess = { authData ->
                SessionManager.saveSession(authData.token, authData.user)
                UserStore.update(authData.user)
                AuthResult.Success(authData.user)
            },
            onFailure = { error ->
                AuthResult.Error(error.localizedMessage ?: error.message ?: "Đăng nhập thất bại")
            }
        )
    }

    override suspend fun register(
        fullName: String,
        dateOfBirth: LocalDate,
        phone: String,
        email: String,
        address: String,
        password: String
    ): AuthResult {
        val dobStr = dateOfBirth.format(dobFormat)
        val result = PaymentApiClient.register(
            fullName = fullName,
            dob = dobStr,
            phone = phone,
            email = email,
            address = address,
            password = password
        )
        return result.fold(
            onSuccess = { AuthResult.VerificationRequired(it) },
            onFailure = { AuthResult.Error(it.localizedMessage ?: "Không thể gửi mã xác nhận.") }
        )
    }

    override suspend fun verifyRegistration(challengeId: String, code: String): AuthResult {
        val result = PaymentApiClient.verifyRegistration(challengeId, code)
        return result.fold(
            onSuccess = { authData ->
                SessionManager.saveSession(authData.token, authData.user)
                UserStore.update(authData.user)
                AuthResult.Success(authData.user)
            },
            onFailure = { error ->
                AuthResult.Error(error.localizedMessage ?: error.message ?: "Đăng ký thất bại")
            }
        )
    }

    companion object {
        val instance: NetworkAuthRepository by lazy { NetworkAuthRepository() }
    }
}
