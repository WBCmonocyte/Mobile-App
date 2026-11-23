package com.example.easysport.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.CancellationException
import kotlin.coroutines.resume

sealed class LocationFetchResult {
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val accuracyMeters: Float? = null,
        val isFineAccuracy: Boolean = true,
    ) : LocationFetchResult()

    object PermissionDenied : LocationFetchResult()
    object GpsDisabled : LocationFetchResult()
    data class Error(val message: String) : LocationFetchResult()
}

object LocationHelper {

    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasFineLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isGpsEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Lấy vị trí hiện tại của thiết bị qua Fused Location Provider.
     * Sử dụng getCurrentLocation với timeout an toàn, fallback về lastLocation nếu vị trí tức thời chưa kịp nạp.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): LocationFetchResult {
        if (!hasLocationPermission(context)) {
            return LocationFetchResult.PermissionDenied
        }

        if (!isGpsEnabled(context)) {
            return LocationFetchResult.GpsDisabled
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val isFine = hasFineLocationPermission(context)
        val priority = if (isFine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY

        val cts = CancellationTokenSource()

        return try {
            val location: Location? = withTimeoutOrNull(15_000) { suspendCancellableCoroutine { continuation ->
                fusedClient.getCurrentLocation(priority, cts.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            if (continuation.isActive) continuation.resume(loc)
                        } else {
                            // Fallback về lastLocation nếu getCurrentLocation null
                            fusedClient.lastLocation
                                .addOnSuccessListener { lastLoc -> if (continuation.isActive) continuation.resume(lastLoc) }
                                .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
                        }
                    }
                    .addOnFailureListener {
                        // Thử fallback lastLocation
                        fusedClient.lastLocation
                            .addOnSuccessListener { lastLoc -> if (continuation.isActive) continuation.resume(lastLoc) }
                            .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
                    }

                continuation.invokeOnCancellation {
                    cts.cancel()
                }
            } }

            if (location != null && (android.os.SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) in 0..120_000_000_000L) {
                LocationFetchResult.Success(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = location.accuracy,
                    isFineAccuracy = isFine
                )
            } else {
                LocationFetchResult.Error("Không thể xác định vị trí hiện tại của thiết bị.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LocationFetchResult.Error(e.message ?: "Lỗi không xác định khi lấy vị trí.")
        }
    }
}

