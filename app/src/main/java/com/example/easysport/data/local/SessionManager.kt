package com.example.easysport.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.easysport.data.model.User
import com.example.easysport.data.remote.PaymentApiClient

object SessionManager {

    private const val PREF_NAME = "easysport_session"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_PHONE = "user_phone"
    private const val KEY_USER_ADDRESS = "user_address"
    private const val KEY_USER_DOB = "user_dob"
    private const val KEY_USER_ROLE = "user_role"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val savedToken = getToken()
            if (!savedToken.isNullOrBlank()) {
                PaymentApiClient.authToken = savedToken
            }
        }
    }

    fun saveSession(token: String, user: User) {
        PaymentApiClient.authToken = token
        prefs?.edit()?.apply {
            putString(KEY_TOKEN, token)
            putString(KEY_USER_ID, user.id)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_NAME, user.name)
            putString(KEY_USER_PHONE, user.phone)
            putString(KEY_USER_ADDRESS, user.address)
            putString(KEY_USER_DOB, user.dateOfBirth)
            putString(KEY_USER_ROLE, user.role)
            apply()
        }
    }

    fun saveUser(user: User) {
        prefs?.edit()?.apply {
            putString(KEY_USER_ID, user.id)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_NAME, user.name)
            putString(KEY_USER_PHONE, user.phone)
            putString(KEY_USER_ADDRESS, user.address)
            putString(KEY_USER_DOB, user.dateOfBirth)
            putString(KEY_USER_ROLE, user.role)
            apply()
        }
    }

    fun getToken(): String? {
        return prefs?.getString(KEY_TOKEN, null)
    }

    fun getUser(): User? {
        val id = prefs?.getString(KEY_USER_ID, null) ?: return null
        val email = prefs?.getString(KEY_USER_EMAIL, "") ?: ""
        val name = prefs?.getString(KEY_USER_NAME, "") ?: ""
        val phone = prefs?.getString(KEY_USER_PHONE, "") ?: ""
        val address = prefs?.getString(KEY_USER_ADDRESS, "") ?: ""
        val dob = prefs?.getString(KEY_USER_DOB, "") ?: ""
        val role = prefs?.getString(KEY_USER_ROLE, "USER") ?: "USER"
        return User(id = id, email = email, name = name, phone = phone, address = address, dateOfBirth = dob, role = role)
    }

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    fun clearSession() {
        PaymentApiClient.authToken = null
        prefs?.edit()?.clear()?.apply()
    }
}

