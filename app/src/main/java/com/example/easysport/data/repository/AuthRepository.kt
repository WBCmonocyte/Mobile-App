package com.example.easysport.data.repository

import com.example.easysport.data.model.User
import java.time.LocalDate

sealed interface AuthResult {
    data class Success(val user: User) : AuthResult
    data class Error(val message: String) : AuthResult
}

interface AuthRepository {
    suspend fun login(emailOrPhone: String, password: String): AuthResult
    suspend fun register(
        fullName: String,
        dateOfBirth: LocalDate,
        phone: String,
        email: String,
        address: String,
        password: String,
    ): AuthResult
}
