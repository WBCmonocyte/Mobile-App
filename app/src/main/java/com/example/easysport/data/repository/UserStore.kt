package com.example.easysport.data.repository

import android.content.Context
import com.example.easysport.data.local.SessionManager
import com.example.easysport.data.model.User
import com.example.easysport.data.remote.PaymentApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Quản lý trạng thái người dùng hiện tại (reactive singleton).
 * Đồng bộ với SessionManager và Backend API.
 */
object UserStore {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun init(context: Context) {
        SessionManager.init(context)
        val savedUser = SessionManager.getUser()
        if (savedUser != null) {
            _currentUser.value = savedUser
            // Tải thông tin hồ sơ mới nhất từ backend trong nền
            CoroutineScope(Dispatchers.IO).launch {
                PaymentApiClient.getProfile().onSuccess { freshUser ->
                    _currentUser.value = freshUser
                    SessionManager.saveUser(freshUser)
                }
            }
        }
    }

    fun getUser(): User? = _currentUser.value

    fun update(user: User) {
        _currentUser.value = user
        SessionManager.saveUser(user)
    }

    fun updateProfile(
        name: String,
        phone: String,
        dateOfBirth: String = "",
        email: String = "",
        address: String = "",
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val res = PaymentApiClient.updateProfile(
                fullName = name.trim(),
                phone = phone.trim(),
                address = address.trim(),
                dob = dateOfBirth.trim()
            )
            res.fold(
                onSuccess = { backendUser ->
                    _currentUser.value = backendUser
                    SessionManager.saveUser(backendUser)
                    onComplete?.invoke(true, null)
                },
                onFailure = { err ->
                    onComplete?.invoke(false, err.message)
                }
            )
        }
    }

    fun isLoggedIn(): Boolean = SessionManager.isLoggedIn() || _currentUser.value != null

    fun reset() {
        SessionManager.clearSession()
        _currentUser.value = null
    }

    fun logout() {
        val token = PaymentApiClient.authToken
        CoroutineScope(Dispatchers.IO).launch {
            PaymentApiClient.logout(token)
        }
        reset()
        BookingStore.clear()
    }
}
