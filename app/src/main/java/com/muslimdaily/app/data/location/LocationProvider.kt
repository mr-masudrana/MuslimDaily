package com.muslimdaily.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class LocationProvider @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(ctx)

    private fun hasPermission() = ContextCompat.checkSelfPermission(
        ctx, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    /** Returns (lat, lng) or null if permission is missing / location unavailable. */
    @SuppressLint("MissingPermission")
    suspend fun current(): Pair<Double, Double>? {
        if (!hasPermission()) return null
        return try {
            val loc = client.lastLocation.await()
                ?: client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
            loc?.let { it.latitude to it.longitude }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
}
