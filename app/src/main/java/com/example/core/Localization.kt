package com.example.core

enum class AppLanguage(val code: String, val label: String) {
    ENGLISH("en", "EN"),
    BANGLA("bn", "বাংলা");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            if (code == BANGLA.code) BANGLA else ENGLISH
    }
}

data class BiText(val en: String, val bn: String) {
    fun get(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) bn else en
}

object BdasStrings {
    fun title(lang: AppLanguage) = BiText(
        "Bangladesh Airline Simulator",
        "বাংলাদেশ এয়ারলাইন সিমুলেটর"
    ).get(lang)

    fun subtitle(lang: AppLanguage) = BiText(
        "64 Districts • Dynamic Weather • Pilot & Passenger 3D",
        "৬৪ জেলা • আবহাওয়া সিমুলেশন • পাইলট ও যাত্রী ৩ডি"
    ).get(lang)

    fun signInHeading(lang: AppLanguage) = BiText(
        "Flight Deck & Cloud Sync Access",
        "ফ্লাইট ডেক ও ক্লাউড সিঙ্ক প্রবেশ"
    ).get(lang)

    fun signInDesc(lang: AppLanguage) = BiText(
        "Sign in with Google to sync your Pilot Career, Airline Empire, and Online Multiplayer across devices.",
        "পাইলট ক্যারিয়ার, এয়ারলাইন ও মাল্টিপ্লেয়ার সংরক্ষণ করতে গুগল দিয়ে সাইন ইন করুন।"
    ).get(lang)

    fun newFlight(lang: AppLanguage) = BiText("New Flight", "নতুন ফ্লাইট").get(lang)
    fun pilotCareer(lang: AppLanguage) = BiText("Pilot Career", "পাইলট ক্যারিয়ার").get(lang)
    fun passengerJourney(lang: AppLanguage) = BiText("Passenger Mode", "যাত্রী সিমুলেটর").get(lang)
    fun freeFlight(lang: AppLanguage) = BiText("Free Flight", "ফ্রি ফ্লাইট").get(lang)
    fun airlineMgmt(lang: AppLanguage) = BiText("Airline HQ", "এয়ারলাইন পরিচালনা").get(lang)
    fun multiplayer(lang: AppLanguage) = BiText("Multiplayer", "মাল্টিপ্লেয়ার").get(lang)
    fun training(lang: AppLanguage) = BiText("Training", "প্রশিক্ষণ").get(lang)
    fun hangar(lang: AppLanguage) = BiText("Hangar", "বিমান হ্যাঙ্গার").get(lang)
    fun worldMap(lang: AppLanguage) = BiText("Bangladesh Map", "বাংলাদেশ মানচিত্র").get(lang)
    fun replay(lang: AppLanguage) = BiText("Flight Replay", "ফ্লাইট রিপ্লে").get(lang)
    fun achievements(lang: AppLanguage) = BiText("Achievements", "অর্জনসমূহ").get(lang)
    fun settings(lang: AppLanguage) = BiText("Settings", "সেটিংস").get(lang)
    fun signOut(lang: AppLanguage) = BiText("Sign Out", "সাইন আউট").get(lang)
    fun launchFlight(lang: AppLanguage) = BiText("LAUNCH FLIGHT", "ফ্লাইট শুরু করুন").get(lang)
    fun emergencyMissions(lang: AppLanguage) = BiText("Emergency Missions", "জরুরি মিশন").get(lang)
    fun welcomeAnnouncement(lang: AppLanguage, origin: String, dest: String) = if (lang == AppLanguage.BANGLA) {
        "শুভ যাত্রা! বাংলাদেশ এয়ারলাইন সিমুলেটরে স্বাগতম। $origin থেকে $dest গন্তব্যের ফ্লাইট বোর্ডিং চলছে।"
    } else {
        "Ladies and gentlemen, welcome aboard. Boarding is now open for our flight from $origin to $dest."
    }
    fun takeoffAnnouncement(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) {
        "কেবিন ক্রু, টেকঅফের জন্য প্রস্তুত হোন। অনুগ্রহ করে সিটবেল্ট বেঁধে নিন।"
    } else {
        "Cabin crew, prepare for takeoff. Please fasten your seatbelts and enjoy your flight over Bangladesh."
    }
    fun landingAnnouncement(lang: AppLanguage, dest: String) = if (lang == AppLanguage.BANGLA) {
        "আমরা $dest বিমানবন্দরে অবতরণ করছি। নিরাপদ ভ্রমণের জন্য ধন্যবাদ।"
    } else {
        "We are now descending into $dest Airport. Thank you for flying with us."
    }
}
