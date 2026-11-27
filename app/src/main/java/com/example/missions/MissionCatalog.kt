package com.example.missions

import com.example.core.AppLanguage
import com.example.core.BiText
import com.example.weather.WeatherPreset

enum class PilotRank(
    val id: String,
    val titleEn: String,
    val titleBn: String,
    val minXp: Long
) {
    STUDENT_PILOT("STUDENT_PILOT", "Student Pilot", "শিক্ষানবিশ পাইলট", 0),
    JUNIOR_PILOT("JUNIOR_PILOT", "Junior Pilot", "জুনিয়র পাইলট", 500),
    FIRST_OFFICER("FIRST_OFFICER", "First Officer", "ফার্স্ট অফিসার", 1500),
    SENIOR_FIRST_OFFICER("SENIOR_FIRST_OFFICER", "Senior First Officer", "সিনিয়র ফার্স্ট অফিসার", 3500),
    CAPTAIN("CAPTAIN", "Captain", "ক্যাপ্টেন", 7000),
    SENIOR_CAPTAIN("SENIOR_CAPTAIN", "Senior Captain", "সিনিয়র ক্যাপ্টেন", 12000);

    fun title(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) titleBn else titleEn

    companion object {
        fun forXp(xp: Long): PilotRank =
            entries.lastOrNull { xp >= it.minXp } ?: STUDENT_PILOT
    }
}

enum class EmergencyType {
    NONE,
    ENGINE_FAILURE,
    LOW_FUEL,
    SEVERE_TURBULENCE,
    BAD_WEATHER_LANDING,
    GEAR_FAILURE,
    EMERGENCY_DIVERSION,
    RUNWAY_APPROACH,
    EMERGENCY_LANDING
}

enum class FlightModeType {
    FREE_FLIGHT,
    TRAINING,
    CAREER,
    EMERGENCY,
    PASSENGER
}

data class MissionDefinition(
    val id: String,
    val mode: FlightModeType,
    val emergencyType: EmergencyType,
    val title: BiText,
    val briefing: BiText,
    val originIcao: String,
    val destinationIcao: String,
    val recommendedAircraftId: String,
    val weatherPreset: WeatherPreset,
    val startAirborne: Boolean,
    val startAltitudeFt: Float,
    val startSpeedKts: Float,
    val startFuelFraction: Float,
    val rewardCoins: Long,
    val rewardXp: Long,
    val targetLandingFpmMax: Int = -550
)

data class AchievementDef(
    val id: String,
    val title: BiText,
    val description: BiText,
    val rewardCoins: Long
)

object MissionCatalog {
    val trainingAndCareerMissions: List<MissionDefinition> = listOf(
        MissionDefinition(
            id = "train_takeoff_vghs",
            mode = FlightModeType.TRAINING,
            emergencyType = EmergencyType.NONE,
            title = BiText("Training 1: Dhaka Takeoff & Climb", "প্রশিক্ষণ ১: ঢাকা উড্ডয়ন"),
            briefing = BiText(
                "Advance throttle to 100%, rotate smoothly at 95 KTS, retract landing gear, and climb above 1,500 ft.",
                "থ্রটল ১০০% করুন, ৯৫ নটে টেকঅফ করুন, ল্যান্ডিং গিয়ার তুলুন এবং ১,৫০০ ফুটের উপরে উঠুন।"
            ),
            originIcao = "VGHS",
            destinationIcao = "VGEG",
            recommendedAircraftId = "atr72",
            weatherPreset = WeatherPreset.CLEAR,
            startAirborne = false,
            startAltitudeFt = 30f,
            startSpeedKts = 0f,
            startFuelFraction = 0.85f,
            rewardCoins = 1200L,
            rewardXp = 250L
        ),
        MissionDefinition(
            id = "career_dhaka_ctg",
            mode = FlightModeType.CAREER,
            emergencyType = EmergencyType.NONE,
            title = BiText("Career Exam: Dhaka to Chattogram", "ক্যারিয়ার পরীক্ষা: ঢাকা-চট্টগ্রাম"),
            briefing = BiText(
                "Complete a full commercial sector or stabilized ILS runway landing at Shah Amanat Intl with passenger comfort.",
                "শাহ আমানত আন্তর্জাতিক বিমানবন্দরে স্থিতিশীল অ্যাপ্রোচ ও নিরাপদ অবতরণ সম্পন্ন করুন।"
            ),
            originIcao = "VGHS",
            destinationIcao = "VGEG",
            recommendedAircraftId = "a320",
            weatherPreset = WeatherPreset.CLOUDY,
            startAirborne = false,
            startAltitudeFt = 30f,
            startSpeedKts = 0f,
            startFuelFraction = 0.75f,
            rewardCoins = 3000L,
            rewardXp = 450L
        )
    )

    val emergencyMissions: List<MissionDefinition> = listOf(
        MissionDefinition(
            id = "em_engine_failure",
            mode = FlightModeType.EMERGENCY,
            emergencyType = EmergencyType.ENGINE_FAILURE,
            title = BiText("Engine Failure After Takeoff", "উড্ডয়নের পর ইঞ্জিন বিকল"),
            briefing = BiText(
                "Left engine failed at 1,800 ft over Dhaka. Counter asymmetric yaw, keep airspeed above stall, and land safely.",
                "ঢাকার আকাশে বাম ইঞ্জিন বিকল হয়েছে। ইয়-নিয়ন্ত্রণ ঠিক রেখে নিরাপদে রানওয়েতে নামুন।"
            ),
            originIcao = "VGHS",
            destinationIcao = "VGHS",
            recommendedAircraftId = "a320",
            weatherPreset = WeatherPreset.CLOUDY,
            startAirborne = true,
            startAltitudeFt = 1800f,
            startSpeedKts = 165f,
            startFuelFraction = 0.60f,
            rewardCoins = 4200L,
            rewardXp = 600L
        ),
        MissionDefinition(
            id = "em_low_fuel",
            mode = FlightModeType.EMERGENCY,
            emergencyType = EmergencyType.LOW_FUEL,
            title = BiText("Low Fuel Emergency", "জ্বালানি সংকট জরুরি অবস্থা"),
            briefing = BiText(
                "Fuel reserves critical at 10%. Reduce throttle to conserve fuel, configure flaps early, and touch down cleanly.",
                "জ্বালানি মাত্র ১০% বাকি। থ্রটল কমিয়ে সাশ্রয়ী গ্লাইড করুন এবং দ্রুত অবতরণ করুন।"
            ),
            originIcao = "VGEG",
            destinationIcao = "VGEG",
            recommendedAircraftId = "b737",
            weatherPreset = WeatherPreset.CLEAR,
            startAirborne = true,
            startAltitudeFt = 2200f,
            startSpeedKts = 175f,
            startFuelFraction = 0.08f,
            rewardCoins = 4500L,
            rewardXp = 650L
        ),
        MissionDefinition(
            id = "em_severe_turbulence",
            mode = FlightModeType.EMERGENCY,
            emergencyType = EmergencyType.SEVERE_TURBULENCE,
            title = BiText("Severe Monsoon Turbulence", "তীব্র কালবৈশাখী ঝাঁকুনি"),
            briefing = BiText(
                "Nor'wester squall lines over Padma basin. Maintain level wings and safe speed through severe wind shear.",
                "পদ্মা অববাহিকায় তীব্র ঝোড়ো হাওয়া। উইন্ড শিয়ার সামলে বিমানের ভারসাম্য বজায় রাখুন।"
            ),
            originIcao = "VGHS",
            destinationIcao = "VGBR",
            recommendedAircraftId = "atr72",
            weatherPreset = WeatherPreset.THUNDERSTORM,
            startAirborne = true,
            startAltitudeFt = 3200f,
            startSpeedKts = 180f,
            startFuelFraction = 0.55f,
            rewardCoins = 4800L,
            rewardXp = 700L
        ),
        MissionDefinition(
            id = "em_bad_weather_landing",
            mode = FlightModeType.EMERGENCY,
            emergencyType = EmergencyType.BAD_WEATHER_LANDING,
            title = BiText("Bad Weather Storm Landing", "দুর্যোগপূর্ণ আবহাওয়ায় অবতরণ"),
            briefing = BiText(
                "Heavy monsoon rain and crosswinds at Chattogram. Track the runway centerline and flare smoothly.",
                "চট্টগ্রামে ভারী বৃষ্টি ও ক্রসউইন্ড। রানওয়ের সেন্টারলাইন ধরে রেখে ফ্লেয়ার করুন।"
            ),
            originIcao = "VGEG",
            destinationIcao = "VGEG",
            recommendedAircraftId = "a320",
            weatherPreset = WeatherPreset.HEAVY_RAIN,
            startAirborne = true,
            startAltitudeFt = 1600f,
            startSpeedKts = 155f,
            startFuelFraction = 0.40f,
            rewardCoins = 5200L,
            rewardXp = 750L
        ),
        MissionDefinition(
            id = "em_gear_failure",
            mode = FlightModeType.EMERGENCY,
            emergencyType = EmergencyType.GEAR_FAILURE,
            title = BiText("Landing Gear Failure", "ল্যান্ডিং গিয়ার বিকল"),
            briefing = BiText(
                "Hydraulic gear lock jammed. Execute a low-descent-rate emergency belly touchdown below -250 FPM.",
                "হাইড্রোলিক গিয়ার আটকে গেছে। -২৫০ এফপিএমের নিচে ধীরে রানওয়েতে জরুরি অবতরণ করুন।"
            ),
            originIcao = "VGHS",
            destinationIcao = "VGHS",
            recommendedAircraftId = "b737",
            weatherPreset = WeatherPreset.LIGHT_RAIN,
            startAirborne = true,
            startAltitudeFt = 1500f,
            startSpeedKts = 160f,
            startFuelFraction = 0.30f,
            rewardCoins = 5600L,
            rewardXp = 850L,
            targetLandingFpmMax = -320
        ),
        MissionDefinition(
            id = "em_diversion",
            mode = FlightModeType.EMERGENCY,
            emergencyType = EmergencyType.EMERGENCY_DIVERSION,
            title = BiText("Emergency Diversion to Cox's Bazar", "কক্সবাজারে জরুরি ডাইভার্সন"),
            briefing = BiText(
                "Weather closed origin airfield. Divert toward coastal runway VGCB under dense fog and headwinds.",
                "ঘন কুয়াশা ও হেডউইন্ডের মধ্যে কক্সবাজার উপকূলীয় রানওয়েতে নিরাপদে ডাইভার্ট করুন।"
            ),
            originIcao = "VGEG",
            destinationIcao = "VGCB",
            recommendedAircraftId = "atr72",
            weatherPreset = WeatherPreset.FOG,
            startAirborne = true,
            startAltitudeFt = 2600f,
            startSpeedKts = 160f,
            startFuelFraction = 0.45f,
            rewardCoins = 5000L,
            rewardXp = 720L
        ),
        MissionDefinition(
            id = "em_runway_approach",
            mode = FlightModeType.EMERGENCY,
            emergencyType = EmergencyType.RUNWAY_APPROACH,
            title = BiText("Runway Approach Challenge", "রানওয়ে অ্যাপ্রোচ চ্যালেঞ্জ"),
            briefing = BiText(
                "High-altitude steep approach with strong crosswind gusts. Stabilize flaps FULL and touch down softly.",
                "তীব্র ক্রসউইন্ডে স্টিপ অ্যাপ্রোচ। ফ্ল্যাপস ফুল করে নরমভাবে রানওয়ে স্পর্শ করুন।"
            ),
            originIcao = "VGSY",
            destinationIcao = "VGSY",
            recommendedAircraftId = "b777",
            weatherPreset = WeatherPreset.STRONG_WIND,
            startAirborne = true,
            startAltitudeFt = 2100f,
            startSpeedKts = 185f,
            startFuelFraction = 0.35f,
            rewardCoins = 6000L,
            rewardXp = 900L,
            targetLandingFpmMax = -400
        ),
        MissionDefinition(
            id = "em_emergency_landing",
            mode = FlightModeType.EMERGENCY,
            emergencyType = EmergencyType.EMERGENCY_LANDING,
            title = BiText("Dual-Fault Emergency Landing", "দ্বৈত ত্রুটি জরুরি অবতরণ"),
            briefing = BiText(
                "Combined engine thrust limit and storm squall. Manage pitch energy and brake hard after touchdown.",
                "ইঞ্জিন থ্রাস্ট সীমিত ও ঝোড়ো আবহাওয়া। পিচ নিয়ন্ত্রণ করে রানওয়েতে ব্রেক কষুন।"
            ),
            originIcao = "VGHS",
            destinationIcao = "VGHS",
            recommendedAircraftId = "a320",
            weatherPreset = WeatherPreset.THUNDERSTORM,
            startAirborne = true,
            startAltitudeFt = 1900f,
            startSpeedKts = 165f,
            startFuelFraction = 0.25f,
            rewardCoins = 7000L,
            rewardXp = 1000L
        )
    )

    val achievements: List<AchievementDef> = listOf(
        AchievementDef("first_takeoff", BiText("First Takeoff", "প্রথম উড্ডয়ন"), BiText("Lift off from a Bangladeshi runway.", "বাংলাদেশের রানওয়ে থেকে প্রথম উড্ডয়ন সম্পন্ন করুন।"), 500L),
        AchievementDef("first_landing", BiText("First Landing", "প্রথম অবতরণ"), BiText("Complete your first safe touchdown.", "আপনার প্রথম নিরাপদ অবতরণ সম্পন্ন করুন।"), 800L),
        AchievementDef("perfect_landing", BiText("Butter Touchdown", "নিখুঁত অবতরণ"), BiText("Land smoothly softer than -180 FPM.", "-১৮০ এফপিএমের নিচে নরম অবতরণ করুন।"), 1500L),
        AchievementDef("emergency_landing", BiText("Crisis Commander", "জরুরি অবস্থা জয়ী"), BiText("Survive and complete an emergency mission.", "যেকোনো একটি জরুরি মিশন সফলভাবে সম্পন্ন করুন।"), 2000L),
        AchievementDef("long_distance", BiText("Cross-Country Wing", "দূরপাল্লার উড্ডয়ন"), BiText("Log over 15 minutes of total flight time.", "মোট ১৫ মিনিটের বেশি ফ্লাইট সময় অর্জন করুন।"), 1200L),
        AchievementDef("night_flight", BiText("Night Owl Captain", "রাতের ফ্লাইট"), BiText("Complete a flight during Night conditions.", "রাতের অন্ধকারে ফ্লাইট সম্পন্ন করুন।"), 1200L),
        AchievementDef("storm_flight", BiText("Monsoon Navigator", "কালবৈশাখী নাবিক"), BiText("Fly and land during Heavy Rain or Thunderstorm.", "ভারী বৃষ্টি বা বজ্রঝড়ের মধ্যে ফ্লাইট সম্পন্ন করুন।"), 1800L),
        AchievementDef("passenger_transport", BiText("Sky Traveler", "যাত্রী অভিজ্ঞতা"), BiText("Complete the full Passenger Journey from check-in to baggage.", "চেক-ইন থেকে ব্যাগেজ সংগ্রহ পর্যন্ত যাত্রী যাত্রা সম্পন্ন করুন।"), 1000L),
        AchievementDef("career_milestone", BiText("Captain's Stripes", "ক্যারিয়ার মাইলফলক"), BiText("Reach Junior Pilot rank or higher.", "জুনিয়র পাইলট বা তার উপরের পদমর্যাদা অর্জন করুন।"), 1500L),
        AchievementDef("airline_growth", BiText("Bengal Carrier CEO", "এয়ারলাইন প্রতিষ্ঠাতা"), BiText("Found your own Bangladeshi airline and open a route.", "নিজস্ব বাংলাদেশি এয়ারলাইন তৈরি করে একটি রুট চালু করুন।"), 2000L)
    )

    fun findById(id: String): MissionDefinition? =
        (trainingAndCareerMissions + emergencyMissions).firstOrNull { it.id == id }
}
