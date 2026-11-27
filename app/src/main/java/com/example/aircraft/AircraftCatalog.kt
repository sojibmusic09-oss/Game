package com.example.aircraft

import com.example.core.AppLanguage
import com.example.core.BiText

enum class AircraftCategory(val en: String, val bn: String) {
    REGIONAL_TURBOPROP("Regional Turboprop", "আঞ্চলিক টার্বোপ্রপ"),
    NARROW_BODY("Narrow-body Jet", "ন্যারো-বডি জেট"),
    WIDE_BODY("Wide-body Long Haul", "ওয়াইড-বডি জেট")
}

data class AircraftSpec(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val category: AircraftCategory,
    val engines: Int,
    val isTurboprop: Boolean,
    val emptyMassKg: Float,
    val maxFuelKg: Float,
    val wingAreaM2: Float,
    val maxThrustN: Float,
    val cruiseSpeedKts: Float,
    val stallSpeedKts: Float,
    val maxSpeedKts: Float,
    val pitchAgility: Float,
    val rollAgility: Float,
    val yawAgility: Float,
    val fuelBurnKgPerSecAtMax: Float,
    val passengerCapacity: Int,
    val minRunwayMeters: Int,
    val purchasePriceCoins: Long,
    val requiredRankIndex: Int,
    val primaryHex: Long,
    val accentHex: Long,
    val description: BiText
) {
    fun displayName(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) nameBn else nameEn
}

data class AircraftLivery(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val fuselageColor: FloatArray,
    val stripeColor: FloatArray,
    val tailColor: FloatArray
)

object AircraftCatalog {
    val aircraft: List<AircraftSpec> = listOf(
        AircraftSpec(
            id = "atr72",
            nameEn = "BD-72 Regional Turboprop",
            nameBn = "বিডি-৭২ টার্বোপ্রপ",
            category = AircraftCategory.REGIONAL_TURBOPROP,
            engines = 2,
            isTurboprop = true,
            emptyMassKg = 13500f,
            maxFuelKg = 5000f,
            wingAreaM2 = 61.0f,
            maxThrustN = 74000f,
            cruiseSpeedKts = 275f,
            stallSpeedKts = 82f,
            maxSpeedKts = 315f,
            pitchAgility = 1.25f,
            rollAgility = 1.30f,
            yawAgility = 1.15f,
            fuelBurnKgPerSecAtMax = 0.35f,
            passengerCapacity = 72,
            minRunwayMeters = 1200,
            purchasePriceCoins = 0L,
            requiredRankIndex = 0,
            primaryHex = 0xFF00A86BL,
            accentHex = 0xFFEF5350L,
            description = BiText(
                "Twin-turboprop workhorse built for short regional Bangladeshi strips and agile crosswind landings.",
                "দেশের আঞ্চলিক ছোট রানওয়ে এবং ক্রসউইন্ড অবতরণের জন্য উপযোগী টার্বোপ্রপ বিমান।"
            )
        ),
        AircraftSpec(
            id = "a320",
            nameEn = "BD-320 Neo Twinjet",
            nameBn = "বিডি-৩২০ নিও জেট",
            category = AircraftCategory.NARROW_BODY,
            engines = 2,
            isTurboprop = false,
            emptyMassKg = 44300f,
            maxFuelKg = 19000f,
            wingAreaM2 = 124.0f,
            maxThrustN = 220000f,
            cruiseSpeedKts = 445f,
            stallSpeedKts = 112f,
            maxSpeedKts = 490f,
            pitchAgility = 1.05f,
            rollAgility = 1.10f,
            yawAgility = 0.95f,
            fuelBurnKgPerSecAtMax = 0.95f,
            passengerCapacity = 180,
            minRunwayMeters = 2000,
            purchasePriceCoins = 18000L,
            requiredRankIndex = 1,
            primaryHex = 0xFF00A86BL,
            accentHex = 0xFF26C6DAL,
            description = BiText(
                "Modern fly-by-wire narrow-body jet with balanced climb performance for busy Dhaka-Chattogram shuttles.",
                "ঢাকা-চট্টগ্রাম ব্যস্ত রুটের জন্য আধুনিক ফ্লাই-বাই-ওয়্যার ন্যারো-বডি যাত্রীবাহী জেট।"
            )
        ),
        AircraftSpec(
            id = "b737",
            nameEn = "BD-738 NextGen Jet",
            nameBn = "বিডি-৭৩৮ নেক্সটজেন জেট",
            category = AircraftCategory.NARROW_BODY,
            engines = 2,
            isTurboprop = false,
            emptyMassKg = 41400f,
            maxFuelKg = 20800f,
            wingAreaM2 = 124.6f,
            maxThrustN = 235000f,
            cruiseSpeedKts = 455f,
            stallSpeedKts = 118f,
            maxSpeedKts = 505f,
            pitchAgility = 1.12f,
            rollAgility = 1.20f,
            yawAgility = 1.0f,
            fuelBurnKgPerSecAtMax = 1.02f,
            passengerCapacity = 174,
            minRunwayMeters = 2150,
            purchasePriceCoins = 32000L,
            requiredRankIndex = 2,
            primaryHex = 0xFF1565C0L,
            accentHex = 0xFFFFB300L,
            description = BiText(
                "Crisp manual roll authority and high-climb thrust tailored for challenging monsoon approaches.",
                "বর্ষা মৌসুমের চ্যালেঞ্জিং অ্যাপ্রোচের জন্য শক্তিশালী থ্রাস্ট ও নিখুঁত কন্ট্রোল।"
            )
        ),
        AircraftSpec(
            id = "b777",
            nameEn = "BD-777 Heavy Wide-Body",
            nameBn = "বিডি-৭৭৭ ওয়াইড-বডি",
            category = AircraftCategory.WIDE_BODY,
            engines = 2,
            isTurboprop = false,
            emptyMassKg = 167800f,
            maxFuelKg = 145000f,
            wingAreaM2 = 436.8f,
            maxThrustN = 860000f,
            cruiseSpeedKts = 490f,
            stallSpeedKts = 132f,
            maxSpeedKts = 535f,
            pitchAgility = 0.72f,
            rollAgility = 0.74f,
            yawAgility = 0.70f,
            fuelBurnKgPerSecAtMax = 2.65f,
            passengerCapacity = 368,
            minRunwayMeters = 2850,
            purchasePriceCoins = 75000L,
            requiredRankIndex = 4,
            primaryHex = 0xFF006D44L,
            accentHex = 0xFFEF5350L,
            description = BiText(
                "Flagship twin-aisle wide-body with heavy aerodynamic inertia, long takeoff roll, and 368-seat cabin.",
                "৩৬৮ আসনের ফ্ল্যাগশিপ ওয়াইড-বডি বিমান—দীর্ঘ রানওয়ে এবং সুদূর ফ্লাইটের জন্য নির্মিত।"
            )
        )
    )

    val liveries: List<AircraftLivery> = listOf(
        AircraftLivery(
            "emerald_sun", "Emerald Sun (National)", "সবুজ ও লাল সূর্য",
            floatArrayOf(0.95f, 0.96f, 0.98f, 1f),
            floatArrayOf(0.00f, 0.66f, 0.42f, 1f),
            floatArrayOf(0.88f, 0.22f, 0.21f, 1f)
        ),
        AircraftLivery(
            "padma_cyan", "Padma Express", "পদ্মা এক্সপ্রেস",
            floatArrayOf(0.92f, 0.95f, 0.98f, 1f),
            floatArrayOf(0.08f, 0.45f, 0.78f, 1f),
            floatArrayOf(0.15f, 0.78f, 0.85f, 1f)
        ),
        AircraftLivery(
            "sundarban_gold", "Sundarban Royal", "সুন্দরবন রয়্যাল",
            floatArrayOf(0.14f, 0.20f, 0.26f, 1f),
            floatArrayOf(1.00f, 0.70f, 0.00f, 1f),
            floatArrayOf(0.00f, 0.52f, 0.33f, 1f)
        )
    )

    fun findById(id: String): AircraftSpec =
        aircraft.firstOrNull { it.id == id } ?: aircraft.first()

    fun findLivery(id: String): AircraftLivery =
        liveries.firstOrNull { it.id == id } ?: liveries.first()
}
