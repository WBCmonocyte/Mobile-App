package com.example.easysport.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.easysport.data.model.Court

@Entity(tableName = "courts")
data class CourtEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val rating: Double,
    val reviewCount: Int,
    val distanceKm: Double,
    val pricePerHour: Int,
    val imageRes: Int,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val sportType: String = "Bóng đá",
    val indoor: Boolean = false,
)

fun CourtEntity.toDomainModel(
    sessionPrices: Map<com.example.easysport.data.model.Session, Int> = emptyMap(),
    amenities: List<com.example.easysport.data.model.Amenity> = emptyList(),
    fields: List<com.example.easysport.data.model.CourtField> = emptyList()
): Court {
    return Court(
        id = id,
        name = name,
        address = address,
        rating = rating,
        reviewCount = reviewCount,
        distanceKm = distanceKm,
        pricePerHour = pricePerHour,
        sessionPrices = sessionPrices,
        amenities = amenities,
        imageRes = imageRes,
        fields = fields,
        sportType = sportType,
        indoor = indoor,
        latitude = latitude,
        longitude = longitude,
    )
}

fun Court.toEntity(): CourtEntity {
    return CourtEntity(
        id = id,
        name = name,
        address = address,
        rating = rating,
        reviewCount = reviewCount,
        distanceKm = distanceKm,
        pricePerHour = pricePerHour,
        imageRes = imageRes,
        latitude = latitude,
        longitude = longitude,
        sportType = sportType,
        indoor = indoor,
    )
}
