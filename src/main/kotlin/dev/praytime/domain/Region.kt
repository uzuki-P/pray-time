package dev.praytime.domain

data class Region(
    val name: String,
    val adminName: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneId: String,
) {
    val displayName: String
        get() = listOfNotNull(name, adminName.ifBlank { null }, country).joinToString(", ")
}

val medanJohor = Region(
    name = "Medan Johor",
    adminName = "Medan",
    country = "Indonesia",
    latitude = 3.535617,
    longitude = 98.676772,
    timezoneId = "Asia/Jakarta",
)

private fun medan(name: String, latitude: Double, longitude: Double) =
    Region(name, "Medan", "Indonesia", latitude, longitude, "Asia/Jakarta")

val builtInRegions = listOf(
    // Kecamatan and kelurahan in Medan, roughly north to south.
    medan("Medan Belawan", 3.7540, 98.6830),
    medan("Medan Labuhan", 3.7130, 98.6940),
    medan("Medan Marelan", 3.6430, 98.6960),
    medan("Medan Deli", 3.6250, 98.7190),
    medan("Medan Tembung", 3.6030, 98.7090),
    medan("Medan Denai", 3.5980, 98.6930),
    medan("Medan Kota", 3.5940, 98.6720),
    medan("Medan Perjuangan", 3.5850, 98.6800),
    medan("Medan Timur", 3.5850, 98.7000),
    medan("Medan Petisah", 3.5830, 98.6820),
    medan("Medan Barat", 3.5720, 98.6780),
    medan("Medan Polonia", 3.5760, 98.6650),
    medan("Medan Baru", 3.5640, 98.6620),
    medan("Medan Maimun", 3.5600, 98.6870),
    medan("Medan Helvetia", 3.5880, 98.6360),
    medan("Medan Sunggal", 3.5560, 98.6240),
    medan("Medan Area", 3.5520, 98.6980),
    medan("Medan Selayang", 3.5350, 98.6450),
    medanJohor,
    medan("Gedung Johor", 3.5430, 98.6820),
    medan("Medan Amplas", 3.5230, 98.6970),
    medan("Medan Tuntungan", 3.5170, 98.6300),
    // Nearby cities and regencies.
    Region("Binjai", "Binjai", "Indonesia", 3.6042, 98.4892, "Asia/Jakarta"),
    Region("Lubuk Pakam", "Deli Serdang", "Indonesia", 3.4260, 98.8760, "Asia/Jakarta"),
    Region("Percut Sei Tuan", "Deli Serdang", "Indonesia", 3.4830, 98.8610, "Asia/Jakarta"),
    Region("Stabat", "Langkat", "Indonesia", 3.7760, 98.4960, "Asia/Jakarta"),
    Region("Tanjung Balai", "North Sumatra", "Indonesia", 2.9650, 99.8020, "Asia/Jakarta"),
    Region("Pekanbaru", "Riau", "Indonesia", 0.5071, 101.4478, "Asia/Jakarta"),
    Region("Padang", "West Sumatra", "Indonesia", -0.9471, 100.4172, "Asia/Jakarta"),
    Region("Jakarta", "Jakarta", "Indonesia", -6.2088, 106.8456, "Asia/Jakarta"),
    Region("Bandung", "West Java", "Indonesia", -6.9175, 107.6191, "Asia/Jakarta"),
    Region("Yogyakarta", "Yogyakarta", "Indonesia", -7.7956, 110.3695, "Asia/Jakarta"),
    Region("Surabaya", "East Java", "Indonesia", -7.2575, 112.7521, "Asia/Jakarta"),
    Region("Makassar", "South Sulawesi", "Indonesia", -5.1477, 119.4327, "Asia/Makassar"),
    Region("Kuala Lumpur", "Selangor", "Malaysia", 3.1390, 101.6869, "Asia/Kuala_Lumpur"),
    Region("Singapore", "Central", "Singapore", 1.3521, 103.8198, "Asia/Singapore"),
)
