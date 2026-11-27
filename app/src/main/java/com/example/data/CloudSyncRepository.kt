package com.example.data

import android.content.Context
import com.example.R
import com.example.missions.PilotRank
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

object FirestoreInstanceProvider {
    @Volatile
    private var configuredDb: FirebaseFirestore? = null

    fun get(context: Context): FirebaseFirestore {
        return configuredDb ?: synchronized(this) {
            configuredDb ?: run {
                val dbId = context.applicationContext.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                runCatching {
                    db.firestoreSettings = firestoreSettings {
                        setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    }
                }
                configuredDb = db
                db
            }
        }
    }
}

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(FirestoreInstanceProvider.get(context))

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeProfile(userId: String): Flow<PilotProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snap ->
                snap.toObject(PilotProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeAirlines(userId: String): Flow<List<AirlineDoc>> =
        db.collection("users").document(userId).collection("airlines")
            .snapshots()
            .map { snap -> snap.toObjects(AirlineDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

    fun observeFleet(userId: String): Flow<List<FleetAircraftDoc>> =
        db.collection("users").document(userId).collection("fleet")
            .snapshots()
            .map { snap -> snap.toObjects(FleetAircraftDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

    fun observeRoutes(userId: String): Flow<List<AirlineRouteDoc>> =
        db.collection("users").document(userId).collection("routes")
            .snapshots()
            .map { snap -> snap.toObjects(AirlineRouteDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

    fun observeTransactions(userId: String): Flow<List<EconomyTransactionDoc>> =
        db.collection("users").document(userId).collection("transactions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(40)
            .snapshots()
            .map { snap -> snap.toObjects(EconomyTransactionDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

    fun observeFlightLogs(userId: String): Flow<List<FlightLogDoc>> =
        db.collection("users").document(userId).collection("flightLogs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .snapshots()
            .map { snap -> snap.toObjects(FlightLogDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

    suspend fun ensureInitialProfile(displayName: String?): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = (displayName?.takeIf { it.isNotBlank() } ?: "Bangladesh Pilot").take(60)
                val suffix = uid.take(4).uppercase()
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "callsign" to "BD-$suffix",
                    "pilotRank" to PilotRank.STUDENT_PILOT.id,
                    "xp" to 0L,
                    "flightHours" to 0.0,
                    "bdasCoins" to 25000L,
                    "airlineCredits" to 50L,
                    "totalFlights" to 0L,
                    "unlockedAircraftIds" to listOf("atr72"),
                    "unlockedAirportIds" to listOf("VGHS", "VGEG"),
                    "unlockedAchievementIds" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordCompletedFlight(
        currentProfile: PilotProfile,
        mode: String,
        aircraftId: String,
        originIcao: String,
        destinationIcao: String,
        missionId: String,
        landingFpm: Int,
        score: Int,
        xpEarned: Long,
        coinsEarned: Long,
        creditsEarned: Long,
        weatherPreset: String,
        durationSec: Long,
        newlyUnlockedAchievements: List<String>
    ): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val userRef = db.collection("users").document(uid)
        val logRef = userRef.collection("flightLogs").document()
        val txRef = userRef.collection("transactions").document()
        return try {
            val nextXp = (currentProfile.xp + xpEarned).coerceAtLeast(0L)
            val nextRank = PilotRank.forXp(nextXp).id
            val nextCoins = (currentProfile.bdasCoins + coinsEarned).coerceAtLeast(0L)
            val nextCredits = (currentProfile.airlineCredits + creditsEarned).coerceAtLeast(0L)
            val nextHours = currentProfile.flightHours + (durationSec / 3600.0)
            val nextAchievements = (currentProfile.unlockedAchievementIds + newlyUnlockedAchievements).distinct().take(50)

            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "xp" to nextXp,
                    "pilotRank" to nextRank,
                    "flightHours" to nextHours,
                    "bdasCoins" to nextCoins,
                    "airlineCredits" to nextCredits,
                    "totalFlights" to (currentProfile.totalFlights + 1L),
                    "unlockedAchievementIds" to nextAchievements,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                logRef,
                mapOf(
                    "userId" to uid,
                    "mode" to mode,
                    "aircraftId" to aircraftId,
                    "originIcao" to originIcao,
                    "destinationIcao" to destinationIcao,
                    "missionId" to missionId.take(64),
                    "landingFpm" to landingFpm.toLong().coerceIn(-10000L, 10000L),
                    "score" to score.toLong().coerceIn(0L, 10000L),
                    "xpEarned" to xpEarned.coerceIn(0L, 50000L),
                    "coinsEarned" to coinsEarned.coerceIn(0L, 500000L),
                    "weatherPreset" to weatherPreset.take(32),
                    "durationSec" to durationSec.coerceIn(0L, 86400L),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            if (coinsEarned > 0L || creditsEarned > 0L) {
                batch.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "txType" to "INCOME",
                        "category" to "FLIGHT_REWARD",
                        "amountCoins" to coinsEarned,
                        "amountCredits" to creditsEarned,
                        "summary" to "$originIcao -> $destinationIcao ($mode)".take(140),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userRef.path)
            Result.failure(e)
        }
    }

    suspend fun unlockAircraft(currentProfile: PilotProfile, aircraftId: String, priceCoins: Long): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        if (currentProfile.bdasCoins < priceCoins) {
            return Result.failure(IllegalArgumentException("Insufficient BDAS Coins"))
        }
        val userRef = db.collection("users").document(uid)
        val txRef = userRef.collection("transactions").document()
        return try {
            val nextUnlocked = (currentProfile.unlockedAircraftIds + aircraftId).distinct().take(20)
            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "bdasCoins" to (currentProfile.bdasCoins - priceCoins),
                    "unlockedAircraftIds" to nextUnlocked,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                txRef,
                mapOf(
                    "userId" to uid,
                    "txType" to "EXPENSE",
                    "category" to "AIRCRAFT_PURCHASE",
                    "amountCoins" to priceCoins,
                    "amountCredits" to 0L,
                    "summary" to "Unlocked $aircraftId".take(140),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            Result.failure(e)
        }
    }

    suspend fun unlockAirport(currentProfile: PilotProfile, icao: String, costCoins: Long): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        if (currentProfile.bdasCoins < costCoins) {
            return Result.failure(IllegalArgumentException("Insufficient BDAS Coins"))
        }
        val userRef = db.collection("users").document(uid)
        val txRef = userRef.collection("transactions").document()
        return try {
            val nextAirports = (currentProfile.unlockedAirportIds + icao).distinct().take(64)
            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "bdasCoins" to (currentProfile.bdasCoins - costCoins),
                    "unlockedAirportIds" to nextAirports,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                txRef,
                mapOf(
                    "userId" to uid,
                    "txType" to "EXPENSE",
                    "category" to "AIRLINE_OPS",
                    "amountCoins" to costCoins,
                    "amountCredits" to 0L,
                    "summary" to "Airport landing rights: $icao".take(140),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            Result.failure(e)
        }
    }

    suspend fun createOrUpdateAirline(
        existingId: String?,
        name: String,
        iataCode: String,
        logoId: String,
        primaryHex: String,
        secondaryHex: String,
        reputation: Long,
        pilotsHired: Long,
        cabinCrewHired: Long,
        dailyRevenue: Long,
        dailyExpenses: Long
    ): Result<String> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val cleanName = name.trim().take(40)
        val cleanIata = iataCode.trim().uppercase().take(4)
        if (cleanName.length < 2 || cleanIata.length < 2) {
            return Result.failure(IllegalArgumentException("Airline name and 2-4 char IATA code required"))
        }
        val col = db.collection("users").document(uid).collection("airlines")
        val docRef = if (existingId.isNullOrBlank()) col.document() else col.document(existingId)
        return try {
            if (existingId.isNullOrBlank()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "name" to cleanName,
                        "iataCode" to cleanIata,
                        "logoId" to logoId.take(32),
                        "primaryColorHex" to primaryHex.take(7),
                        "secondaryColorHex" to secondaryHex.take(7),
                        "reputation" to reputation.coerceIn(0L, 100L),
                        "pilotsHired" to pilotsHired.coerceIn(0L, 500L),
                        "cabinCrewHired" to cabinCrewHired.coerceIn(0L, 1000L),
                        "dailyRevenue" to dailyRevenue.coerceAtLeast(0L),
                        "dailyExpenses" to dailyExpenses.coerceAtLeast(0L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "iataCode" to cleanIata,
                        "logoId" to logoId.take(32),
                        "primaryColorHex" to primaryHex.take(7),
                        "secondaryColorHex" to secondaryHex.take(7),
                        "reputation" to reputation.coerceIn(0L, 100L),
                        "pilotsHired" to pilotsHired.coerceIn(0L, 500L),
                        "cabinCrewHired" to cabinCrewHired.coerceIn(0L, 1000L),
                        "dailyRevenue" to dailyRevenue.coerceAtLeast(0L),
                        "dailyExpenses" to dailyExpenses.coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun purchaseFleetAircraft(
        currentProfile: PilotProfile,
        airlineId: String,
        aircraftModelId: String,
        registration: String,
        liveryName: String,
        costCoins: Long
    ): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        if (currentProfile.bdasCoins < costCoins) {
            return Result.failure(IllegalArgumentException("Insufficient BDAS Coins"))
        }
        val userRef = db.collection("users").document(uid)
        val fleetRef = userRef.collection("fleet").document()
        val txRef = userRef.collection("transactions").document()
        return try {
            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "bdasCoins" to (currentProfile.bdasCoins - costCoins),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                fleetRef,
                mapOf(
                    "userId" to uid,
                    "airlineId" to airlineId,
                    "aircraftModelId" to aircraftModelId,
                    "registration" to registration.take(16),
                    "liveryName" to liveryName.take(40),
                    "conditionPercent" to 100L,
                    "assignedRouteId" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                txRef,
                mapOf(
                    "userId" to uid,
                    "txType" to "EXPENSE",
                    "category" to "AIRCRAFT_PURCHASE",
                    "amountCoins" to costCoins,
                    "amountCredits" to 0L,
                    "summary" to "Purchased $aircraftModelId ($registration)".take(140),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, fleetRef.path)
            Result.failure(e)
        }
    }

    suspend fun createRoute(
        airlineId: String,
        originIcao: String,
        destinationIcao: String,
        ticketPriceCoins: Long,
        demandScore: Long,
        assignedFleetId: String
    ): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        if (originIcao == destinationIcao) {
            return Result.failure(IllegalArgumentException("Origin and destination must differ"))
        }
        val routeRef = db.collection("users").document(uid).collection("routes").document()
        return try {
            routeRef.set(
                mapOf(
                    "userId" to uid,
                    "airlineId" to airlineId,
                    "originIcao" to originIcao,
                    "destinationIcao" to destinationIcao,
                    "ticketPriceCoins" to ticketPriceCoins.coerceIn(50L, 50000L),
                    "demandScore" to demandScore.coerceIn(0L, 100L),
                    "assignedFleetId" to assignedFleetId,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, routeRef.path)
            Result.failure(e)
        }
    }

    suspend fun settleAirlineCycle(
        currentProfile: PilotProfile,
        airlineId: String,
        netCoins: Long,
        grossIncome: Long,
        totalExpenses: Long,
        summary: String
    ): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val userRef = db.collection("users").document(uid)
        val airlineRef = userRef.collection("airlines").document(airlineId)
        val txRef = userRef.collection("transactions").document()
        return try {
            val nextCoins = (currentProfile.bdasCoins + netCoins).coerceAtLeast(0L)
            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "bdasCoins" to nextCoins,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                airlineRef,
                mapOf(
                    "dailyRevenue" to grossIncome.coerceAtLeast(0L),
                    "dailyExpenses" to totalExpenses.coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                txRef,
                mapOf(
                    "userId" to uid,
                    "txType" to if (netCoins >= 0) "INCOME" else "EXPENSE",
                    "category" to "AIRLINE_OPS",
                    "amountCoins" to kotlin.math.abs(netCoins),
                    "amountCredits" to 0L,
                    "summary" to summary.take(140),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, airlineRef.path)
            Result.failure(e)
        }
    }
}
