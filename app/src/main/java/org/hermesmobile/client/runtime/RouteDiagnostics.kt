package org.hermesmobile.client.runtime

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RouteSnapshot(
    val deviceLabel: String,
    val androidLabel: String,
    val activeTransports: List<String>,
    val vpnActive: Boolean,
    val networkValidated: Boolean,
    val networkMetered: Boolean,
    val checkedAt: String
)

fun collectRouteSnapshot(context: Context): RouteSnapshot {
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    val activeNetwork = connectivityManager?.activeNetwork
    val capabilities = activeNetwork?.let(connectivityManager::getNetworkCapabilities)

    return RouteSnapshot(
        deviceLabel = deviceLabel(),
        androidLabel = "Android ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}",
        activeTransports = transportLabels(capabilities),
        vpnActive = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true,
        networkValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
        networkMetered = connectivityManager?.isActiveNetworkMetered ?: true,
        checkedAt = timestamp()
    )
}

private fun deviceLabel(): String {
    val maker = Build.MANUFACTURER.trim().replaceFirstChar { it.titlecase(Locale.US) }
    val model = Build.MODEL.trim()
    return listOf(maker, model)
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(" ")
        .ifBlank { "Android device" }
}

private fun transportLabels(capabilities: NetworkCapabilities?): List<String> {
    if (capabilities == null) return emptyList()

    return buildList {
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("wifi")
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("cellular")
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add("ethernet")
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("vpn")
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_USB)) add("usb")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)
        ) {
            add("bluetooth")
        }
    }
}

private fun timestamp(): String {
    return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
}
