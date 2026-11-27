package com.example.world

import kotlin.math.*

enum class BdDivision(val en: String, val bn: String) {
    DHAKA("Dhaka", "ঢাকা"),
    CHATTOGRAM("Chattogram", "চট্টগ্রাম"),
    RAJSHAHI("Rajshahi", "রাজশাহী"),
    KHULNA("Khulna", "খুলনা"),
    BARISHAL("Barishal", "বরিশাল"),
    SYLHET("Sylhet", "সিলেট"),
    RANGPUR("Rangpur", "রংপুর"),
    MYMENSINGH("Mymensingh", "ময়মনসিংহ")
}

enum class BdBiome(val en: String, val bn: String) {
    URBAN_HUB("Urban Hub", "নগর কেন্দ্র"),
    DELTA_RIVERINE("Delta & Rivers", "নদীমাতৃক বদ্বীপ"),
    HILL_TRACTS("Chattogram Hills", "পার্বত্য অঞ্চল"),
    TEA_VALLEYS("Sylhet Tea Valleys", "সিলেট চা বাগান"),
    SUNDARBANS_COAST("Sundarbans & Coast", "সুন্দরবন ও উপকূল"),
    NORTHERN_PLAINS("Northern Plains", "উত্তরাঞ্চল সমভূমি")
}

data class BdDistrict(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val division: BdDivision,
    val lat: Double,
    val lon: Double,
    val biome: BdBiome
)

data class BdAirport(
    val icao: String,
    val nameEn: String,
    val nameBn: String,
    val cityEn: String,
    val division: BdDivision,
    val lat: Double,
    val lon: Double,
    val elevationFt: Int,
    val runwayHeadingDeg: Float,
    val runwayLengthM: Int,
    val isInternational: Boolean,
    val isFull3DTerminal: Boolean,
    val unlockCostCoins: Long
)

data class GeoPoint(val lat: Double, val lon: Double)

data class BdRiver(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val widthMeters: Float,
    val points: List<GeoPoint>
)

data class BdBridge(
    val nameEn: String,
    val nameBn: String,
    val lat: Double,
    val lon: Double,
    val headingDeg: Float,
    val lengthMeters: Float
)

object BangladeshGeoData {
    // Authentic coordinates for all 8 Bangladesh Divisions & 64 Districts
    val districts: List<BdDistrict> = listOf(
        // Dhaka Division (13)
        BdDistrict("dhaka", "Dhaka", "ঢাকা", BdDivision.DHAKA, 23.8103, 90.4125, BdBiome.URBAN_HUB),
        BdDistrict("gazipur", "Gazipur", "গাজীপুর", BdDivision.DHAKA, 24.0023, 90.4264, BdBiome.URBAN_HUB),
        BdDistrict("narayanganj", "Narayanganj", "নারায়ণগঞ্জ", BdDivision.DHAKA, 23.6238, 90.5000, BdBiome.DELTA_RIVERINE),
        BdDistrict("narsingdi", "Narsingdi", "নরসিংদী", BdDivision.DHAKA, 23.9322, 90.7154, BdBiome.DELTA_RIVERINE),
        BdDistrict("munshiganj", "Munshiganj", "মুন্সীগঞ্জ", BdDivision.DHAKA, 23.5422, 90.5305, BdBiome.DELTA_RIVERINE),
        BdDistrict("manikganj", "Manikganj", "মানিকগঞ্জ", BdDivision.DHAKA, 23.8644, 90.0047, BdBiome.DELTA_RIVERINE),
        BdDistrict("tangail", "Tangail", "টাঙ্গাইল", BdDivision.DHAKA, 24.2513, 89.9167, BdBiome.DELTA_RIVERINE),
        BdDistrict("kishoreganj", "Kishoreganj", "কিশোরগঞ্জ", BdDivision.DHAKA, 24.4449, 90.7766, BdBiome.TEA_VALLEYS),
        BdDistrict("faridpur", "Faridpur", "ফরিদপুর", BdDivision.DHAKA, 23.6071, 89.8429, BdBiome.DELTA_RIVERINE),
        BdDistrict("gopalganj", "Gopalganj", "গোপালগঞ্জ", BdDivision.DHAKA, 23.0051, 89.8266, BdBiome.DELTA_RIVERINE),
        BdDistrict("madaripur", "Madaripur", "মাদারীপুর", BdDivision.DHAKA, 23.1641, 90.1897, BdBiome.DELTA_RIVERINE),
        BdDistrict("rajbari", "Rajbari", "রাজবাড়ী", BdDivision.DHAKA, 23.7574, 89.6445, BdBiome.DELTA_RIVERINE),
        BdDistrict("shariatpur", "Shariatpur", "শরীয়তপুর", BdDivision.DHAKA, 23.2423, 90.4348, BdBiome.DELTA_RIVERINE),

        // Chattogram Division (11)
        BdDistrict("chattogram", "Chattogram", "চট্টগ্রাম", BdDivision.CHATTOGRAM, 22.3569, 91.7832, BdBiome.URBAN_HUB),
        BdDistrict("coxsbazar", "Cox's Bazar", "কক্সবাজার", BdDivision.CHATTOGRAM, 21.4272, 92.0058, BdBiome.SUNDARBANS_COAST),
        BdDistrict("rangamati", "Rangamati", "রাঙ্গামাটি", BdDivision.CHATTOGRAM, 22.7324, 92.2985, BdBiome.HILL_TRACTS),
        BdDistrict("bandarban", "Bandarban", "বান্দরবান", BdDivision.CHATTOGRAM, 22.1953, 92.2184, BdBiome.HILL_TRACTS),
        BdDistrict("khagrachhari", "Khagrachhari", "খাগড়াছড়ি", BdDivision.CHATTOGRAM, 23.1193, 91.9847, BdBiome.HILL_TRACTS),
        BdDistrict("cumilla", "Cumilla", "কুমিল্লা", BdDivision.CHATTOGRAM, 23.4607, 91.1809, BdBiome.DELTA_RIVERINE),
        BdDistrict("feni", "Feni", "ফেনী", BdDivision.CHATTOGRAM, 23.0159, 91.3976, BdBiome.DELTA_RIVERINE),
        BdDistrict("brahmanbaria", "Brahmanbaria", "ব্রাহ্মণবাড়িয়া", BdDivision.CHATTOGRAM, 23.9571, 91.1119, BdBiome.DELTA_RIVERINE),
        BdDistrict("chandpur", "Chandpur", "চাঁদপুর", BdDivision.CHATTOGRAM, 23.2333, 90.6713, BdBiome.DELTA_RIVERINE),
        BdDistrict("lakshmipur", "Lakshmipur", "লক্ষ্মীপুর", BdDivision.CHATTOGRAM, 22.9425, 90.8412, BdBiome.DELTA_RIVERINE),
        BdDistrict("noakhali", "Noakhali", "নোয়াখালী", BdDivision.CHATTOGRAM, 22.8696, 91.0994, BdBiome.SUNDARBANS_COAST),

        // Rajshahi Division (8)
        BdDistrict("rajshahi", "Rajshahi", "রাজশাহী", BdDivision.RAJSHAHI, 24.3745, 88.6042, BdBiome.URBAN_HUB),
        BdDistrict("bogura", "Bogura", "বগুড়া", BdDivision.RAJSHAHI, 24.8465, 89.3773, BdBiome.NORTHERN_PLAINS),
        BdDistrict("pabna", "Pabna", "পাবনা", BdDivision.RAJSHAHI, 24.0064, 89.2372, BdBiome.DELTA_RIVERINE),
        BdDistrict("sirajganj", "Sirajganj", "সিরাজগঞ্জ", BdDivision.RAJSHAHI, 24.4534, 89.7007, BdBiome.DELTA_RIVERINE),
        BdDistrict("natore", "Natore", "নাটোর", BdDivision.RAJSHAHI, 24.4206, 89.0003, BdBiome.NORTHERN_PLAINS),
        BdDistrict("naogaon", "Naogaon", "নওগাঁ", BdDivision.RAJSHAHI, 24.7936, 88.9318, BdBiome.NORTHERN_PLAINS),
        BdDistrict("chapainawabganj", "Chapainawabganj", "চাঁপাইনবাবগঞ্জ", BdDivision.RAJSHAHI, 24.5965, 88.2775, BdBiome.NORTHERN_PLAINS),
        BdDistrict("joypurhat", "Joypurhat", "জয়পুরহাট", BdDivision.RAJSHAHI, 25.0968, 89.0227, BdBiome.NORTHERN_PLAINS),

        // Khulna Division (10)
        BdDistrict("khulna", "Khulna", "খুলনা", BdDivision.KHULNA, 22.8456, 89.5403, BdBiome.SUNDARBANS_COAST),
        BdDistrict("jashore", "Jashore", "যশোর", BdDivision.KHULNA, 23.1634, 89.2182, BdBiome.DELTA_RIVERINE),
        BdDistrict("satkhira", "Satkhira", "সাতক্ষীরা", BdDivision.KHULNA, 22.7185, 89.0705, BdBiome.SUNDARBANS_COAST),
        BdDistrict("bagerhat", "Bagerhat", "বাগেরহাট", BdDivision.KHULNA, 22.6516, 89.7859, BdBiome.SUNDARBANS_COAST),
        BdDistrict("kushtia", "Kushtia", "কুষ্টিয়া", BdDivision.KHULNA, 23.9013, 89.1205, BdBiome.DELTA_RIVERINE),
        BdDistrict("jhenaidah", "Jhenaidah", "ঝিনাইদহ", BdDivision.KHULNA, 23.5448, 89.1539, BdBiome.DELTA_RIVERINE),
        BdDistrict("magura", "Magura", "মাগুরা", BdDivision.KHULNA, 23.4873, 89.4199, BdBiome.DELTA_RIVERINE),
        BdDistrict("narail", "Narail", "নড়াইল", BdDivision.KHULNA, 23.1725, 89.5127, BdBiome.DELTA_RIVERINE),
        BdDistrict("chuadanga", "Chuadanga", "চুয়াডাঙ্গা", BdDivision.KHULNA, 23.6402, 88.8418, BdBiome.NORTHERN_PLAINS),
        BdDistrict("meherpur", "Meherpur", "মেহেরপুর", BdDivision.KHULNA, 23.7622, 88.6318, BdBiome.NORTHERN_PLAINS),

        // Barishal Division (6)
        BdDistrict("barishal", "Barishal", "বরিশাল", BdDivision.BARISHAL, 22.7010, 90.3535, BdBiome.DELTA_RIVERINE),
        BdDistrict("patuakhali", "Patuakhali", "পটুয়াখালী", BdDivision.BARISHAL, 22.3596, 90.3299, BdBiome.SUNDARBANS_COAST),
        BdDistrict("bhola", "Bhola", "ভোলা", BdDivision.BARISHAL, 22.6859, 90.6482, BdBiome.SUNDARBANS_COAST),
        BdDistrict("pirojpur", "Pirojpur", "পিরোজপুর", BdDivision.BARISHAL, 22.5841, 89.9720, BdBiome.DELTA_RIVERINE),
        BdDistrict("jhalokati", "Jhalokati", "ঝালকাঠি", BdDivision.BARISHAL, 22.6406, 90.1987, BdBiome.DELTA_RIVERINE),
        BdDistrict("barguna", "Barguna", "বরগুনা", BdDivision.BARISHAL, 22.1592, 90.1255, BdBiome.SUNDARBANS_COAST),

        // Sylhet Division (4)
        BdDistrict("sylhet", "Sylhet", "সিলেট", BdDivision.SYLHET, 24.8949, 91.8687, BdBiome.TEA_VALLEYS),
        BdDistrict("moulvibazar", "Moulvibazar", "মৌলভীবাজার", BdDivision.SYLHET, 24.4829, 91.7774, BdBiome.TEA_VALLEYS),
        BdDistrict("habiganj", "Habiganj", "হবিগঞ্জ", BdDivision.SYLHET, 24.3749, 91.4155, BdBiome.TEA_VALLEYS),
        BdDistrict("sunamganj", "Sunamganj", "সুনামগঞ্জ", BdDivision.SYLHET, 25.0658, 91.3950, BdBiome.TEA_VALLEYS),

        // Rangpur Division (8)
        BdDistrict("rangpur", "Rangpur", "রংপুর", BdDivision.RANGPUR, 25.7439, 89.2752, BdBiome.NORTHERN_PLAINS),
        BdDistrict("dinajpur", "Dinajpur", "দিনাজপুর", BdDivision.RANGPUR, 25.6217, 88.6355, BdBiome.NORTHERN_PLAINS),
        BdDistrict("nilphamari", "Nilphamari", "নীলফামারী", BdDivision.RANGPUR, 25.9318, 88.8560, BdBiome.NORTHERN_PLAINS),
        BdDistrict("kurigram", "Kurigram", "কুড়িগ্রাম", BdDivision.RANGPUR, 25.8054, 89.6362, BdBiome.DELTA_RIVERINE),
        BdDistrict("gaibandha", "Gaibandha", "গাইবান্ধা", BdDivision.RANGPUR, 25.3288, 89.5281, BdBiome.DELTA_RIVERINE),
        BdDistrict("thakurgaon", "Thakurgaon", "ঠাকুরগাঁও", BdDivision.RANGPUR, 26.0337, 88.4617, BdBiome.NORTHERN_PLAINS),
        BdDistrict("panchagarh", "Panchagarh", "পঞ্চগড়", BdDivision.RANGPUR, 26.3411, 88.5542, BdBiome.NORTHERN_PLAINS),
        BdDistrict("lalmonirhat", "Lalmonirhat", "লালমনিরহাট", BdDivision.RANGPUR, 25.9923, 89.2847, BdBiome.NORTHERN_PLAINS),

        // Mymensingh Division (4)
        BdDistrict("mymensingh", "Mymensingh", "ময়মনসিংহ", BdDivision.MYMENSINGH, 24.7471, 90.4203, BdBiome.DELTA_RIVERINE),
        BdDistrict("jamalpur", "Jamalpur", "জামালপুর", BdDivision.MYMENSINGH, 24.9375, 89.9378, BdBiome.DELTA_RIVERINE),
        BdDistrict("netrokona", "Netrokona", "নেত্রকোণা", BdDivision.MYMENSINGH, 24.8709, 90.7279, BdBiome.TEA_VALLEYS),
        BdDistrict("sherpur", "Sherpur", "শেরপুর", BdDivision.MYMENSINGH, 25.0205, 90.0153, BdBiome.NORTHERN_PLAINS)
    )

    val airports: List<BdAirport> = listOf(
        BdAirport("VGHS", "Hazrat Shahjalal Intl", "হযরত শাহজালাল আন্তর্জাতিক", "Dhaka", BdDivision.DHAKA, 23.8433, 90.3978, 30, 140f, 3200, true, true, 0L),
        BdAirport("VGEG", "Shah Amanat Intl", "শাহ আমানত আন্তর্জাতিক", "Chattogram", BdDivision.CHATTOGRAM, 22.2496, 91.8133, 12, 230f, 2940, true, true, 0L),
        BdAirport("VGSY", "Osmani Intl", "ওসমানী আন্তর্জাতিক", "Sylhet", BdDivision.SYLHET, 24.9633, 91.8669, 50, 110f, 2895, true, false, 2000L),
        BdAirport("VGCB", "Cox's Bazar Airport", "কক্সবাজার বিমানবন্দর", "Cox's Bazar", BdDivision.CHATTOGRAM, 21.4522, 91.9639, 12, 170f, 2743, false, false, 2500L),
        BdAirport("VGRJ", "Shah Makhdum Airport", "শাহ মখদুম বিমানবন্দর", "Rajshahi", BdDivision.RAJSHAHI, 24.4372, 88.6165, 64, 170f, 1829, false, false, 3000L),
        BdAirport("VGJR", "Jashore Airport", "যশোর বিমানবন্দর", "Jashore", BdDivision.KHULNA, 23.1838, 89.1608, 20, 160f, 2438, false, false, 3000L),
        BdAirport("VGSD", "Saidpur Airport", "সৈয়দপুর বিমানবন্দর", "Saidpur", BdDivision.RANGPUR, 25.7592, 88.9089, 125, 160f, 1829, false, false, 3500L),
        BdAirport("VGBR", "Barishal Airport", "বরিশাল বিমানবন্দর", "Barishal", BdDivision.BARISHAL, 22.8010, 90.3012, 23, 170f, 1829, false, false, 3500L)
    )

    val rivers: List<BdRiver> = listOf(
        BdRiver(
            "padma", "Padma River", "পদ্মা নদী", 2400f,
            listOf(GeoPoint(24.47, 88.25), GeoPoint(24.10, 89.05), GeoPoint(23.82, 89.78), GeoPoint(23.44, 90.26), GeoPoint(23.22, 90.63))
        ),
        BdRiver(
            "jamuna", "Jamuna River", "যমুনা নদী", 2200f,
            listOf(GeoPoint(25.85, 89.73), GeoPoint(25.15, 89.65), GeoPoint(24.39, 89.78), GeoPoint(23.82, 89.78))
        ),
        BdRiver(
            "meghna", "Meghna River", "মেঘনা নদী", 2600f,
            listOf(GeoPoint(24.25, 90.98), GeoPoint(23.62, 90.65), GeoPoint(23.22, 90.63), GeoPoint(22.55, 90.78), GeoPoint(22.10, 91.10))
        )
    )

    val bridges: List<BdBridge> = listOf(
        BdBridge("Padma Multipurpose Bridge", "পদ্মা বহুমুখী সেতু", 23.4460, 90.2620, 15f, 6150f),
        BdBridge("Bangabandhu Jamuna Bridge", "বঙ্গবন্ধু যমুনা সেতু", 24.3995, 89.7765, 90f, 4800f)
    )

    // Simplified national boundary polygon for Bangladesh map & boundary telemetry
    val nationalOutline: List<GeoPoint> = listOf(
        GeoPoint(26.63, 88.45), GeoPoint(26.35, 89.50), GeoPoint(25.70, 89.85),
        GeoPoint(25.18, 90.50), GeoPoint(25.18, 92.15), GeoPoint(24.50, 92.25),
        GeoPoint(23.65, 91.50), GeoPoint(22.85, 91.55), GeoPoint(22.90, 92.35),
        GeoPoint(21.40, 92.45), GeoPoint(20.75, 92.30), GeoPoint(21.45, 91.95),
        GeoPoint(22.25, 91.78), GeoPoint(22.80, 91.10), GeoPoint(22.15, 90.10),
        GeoPoint(21.70, 89.55), GeoPoint(22.05, 88.95), GeoPoint(23.15, 88.85),
        GeoPoint(24.25, 88.35), GeoPoint(25.25, 88.55), GeoPoint(26.63, 88.45)
    )

    fun findAirport(icao: String): BdAirport =
        airports.firstOrNull { it.icao == icao } ?: airports.first()

    fun distanceNm(a: BdAirport, b: BdAirport): Double {
        val r = 3440.065 // Earth radius in nautical miles
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val sinLat = sin(dLat / 2)
        val sinLon = sin(dLon / 2)
        val h = sinLat * sinLat + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sinLon * sinLon
        return 2 * r * asin(min(1.0, sqrt(h)))
    }

    fun bearingDeg(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val y = sin(Math.toRadians(lon2 - lon1)) * cos(Math.toRadians(lat2))
        val x = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) -
            sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(Math.toRadians(lon2 - lon1))
        val deg = Math.toDegrees(atan2(y, x))
        return ((deg + 360.0) % 360.0).toFloat()
    }

    fun nearestDistrict(lat: Double, lon: Double): BdDistrict =
        districts.minByOrNull { (it.lat - lat) * (it.lat - lat) + (it.lon - lon) * (it.lon - lon) }
            ?: districts.first()
}
