package kr.ai.redbridgedev.bobmap.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

object LocationProvider {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** 위치 권한이 있을 때 현재 위치를 반환. 실패 시 null. */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(context: Context): Pair<Double, Double>? {
        if (!hasPermission(context)) return null
        return runCatching {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val location = client
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .await()
                ?: client.lastLocation.await()
            location?.let { it.latitude to it.longitude }
        }.getOrNull()
    }
}
