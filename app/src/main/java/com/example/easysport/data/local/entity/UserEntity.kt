package com.example.easysport.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.easysport.data.model.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val dateOfBirth: String = "",
    val passwordHash: String = "",
    val locked: Boolean = false
)

fun UserEntity.toDomainModel(): User {
    return User(
        id = id,
        email = email,
        name = name,
        phone = phone,
        address = address,
        dateOfBirth = dateOfBirth
    )
}

fun User.toEntity(passwordHash: String = "", locked: Boolean = false): UserEntity {
    return UserEntity(
        id = id,
        email = email,
        name = name,
        phone = phone,
        address = address,
        dateOfBirth = dateOfBirth,
        passwordHash = passwordHash,
        locked = locked
    )
}
