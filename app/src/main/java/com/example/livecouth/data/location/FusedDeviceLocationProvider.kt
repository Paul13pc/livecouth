package com.example.livecouth.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.livecouth.domain.model.LocationCoordinates
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FusedDeviceLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    fun isEnabled(): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return try {
            LocationManagerCompat.isLocationEnabled(manager)
        } catch (_: RuntimeException) {
            false
        }
    }

    suspend fun getCurrentLocation(): LocationCoordinates? {
        if (!hasPermission() || !isEnabled()) return null
        val client = try {
            LocationServices.getFusedLocationProviderClient(context)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return null
        }
        val current = try {
            client.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token,
            ).await()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        current?.toCoordinates()?.let { return it }

        return getLastKnownLocation(client)
    }

    suspend fun getLastKnownLocation(): LocationCoordinates? {
        if (!hasPermission() || !isEnabled()) return null
        val client = try {
            LocationServices.getFusedLocationProviderClient(context)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return null
        }
        return getLastKnownLocation(client)
    }

    private fun Location.toCoordinates(): LocationCoordinates? =
        LocationCoordinates(latitude.toFloat(), longitude.toFloat())
            .takeIf { it.areValid() }

    private suspend fun getLastKnownLocation(
        client: com.google.android.gms.location.FusedLocationProviderClient,
    ): LocationCoordinates? {
        val lastKnown = try {
            client.lastLocation.await()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        return lastKnown?.toCoordinates()
    }
}
