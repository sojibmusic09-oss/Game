package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class PilotProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Captain",
    val callsign: String = "BD-101",
    val pilotRank: String = "STUDENT_PILOT",
    val xp: Long = 0L,
    val flightHours: Double = 0.0,
    val bdasCoins: Long = 25000L,
    val airlineCredits: Long = 50L,
    val totalFlights: Long = 0L,
    val unlockedAircraftIds: List<String> = listOf("atr72"),
    val unlockedAirportIds: List<String> = listOf("VGHS", "VGEG"),
    val unlockedAchievementIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AirlineDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val iataCode: String = "",
    val logoId: String = "emerald_sun",
    val primaryColorHex: String = "#00A86B",
    val secondaryColorHex: String = "#EF5350",
    val reputation: Long = 65L,
    val pilotsHired: Long = 1L,
    val cabinCrewHired: Long = 2L,
    val dailyRevenue: Long = 0L,
    val dailyExpenses: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FleetAircraftDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val airlineId: String = "",
    val aircraftModelId: String = "atr72",
    val registration: String = "S2-BDA",
    val liveryName: String = "Emerald Sun",
    val conditionPercent: Long = 100L,
    val assignedRouteId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AirlineRouteDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val airlineId: String = "",
    val originIcao: String = "VGHS",
    val destinationIcao: String = "VGEG",
    val ticketPriceCoins: Long = 4200L,
    val demandScore: Long = 80L,
    val assignedFleetId: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class EconomyTransactionDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val txType: String = "INCOME",
    val category: String = "FLIGHT_REWARD",
    val amountCoins: Long = 0L,
    val amountCredits: Long = 0L,
    val summary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FlightLogDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mode: String = "FREE_FLIGHT",
    val aircraftId: String = "atr72",
    val originIcao: String = "VGHS",
    val destinationIcao: String = "VGEG",
    val missionId: String = "",
    val landingFpm: Long = 0L,
    val score: Long = 0L,
    val xpEarned: Long = 0L,
    val coinsEarned: Long = 0L,
    val weatherPreset: String = "CLEAR",
    val durationSec: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FriendEntryDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val friendUid: String = "",
    val friendCallsign: String = "",
    val friendRank: String = "STUDENT_PILOT",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MultiplayerSessionDoc(
    @DocumentId val id: String = "",
    val hostUserId: String = "",
    val hostCallsign: String = "",
    val title: String = "",
    val airportIcao: String = "VGHS",
    val weatherPreset: String = "CLEAR",
    val missionType: String = "FREE_FLIGHT",
    val status: String = "WAITING",
    val maxPlayers: Long = 8L,
    val playerCount: Long = 1L,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MultiplayerAircraftStateDoc(
    @DocumentId val id: String = "",
    val sessionId: String = "",
    val playerUid: String = "",
    val callsign: String = "",
    val aircraftId: String = "atr72",
    val latitude: Double = 23.8433,
    val longitude: Double = 90.3978,
    val altitudeFt: Double = 30.0,
    val headingDeg: Double = 140.0,
    val pitchDeg: Double = 0.0,
    val rollDeg: Double = 0.0,
    val speedKts: Double = 0.0,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val updatedAt: Timestamp? = null
)
