package dev.praytime.domain

data class Region(
    val name: String,
    val adminName: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneId: String,
) {
    val displayName: String get() = "$name, $adminName, $country"
}

val medanJohor = Region(
    name = "Medan Johor",
    adminName = "Medan",
    country = "Indonesia",
    latitude = 3.535617,
    longitude = 98.676772,
    timezoneId = "Asia/Jakarta",
)
