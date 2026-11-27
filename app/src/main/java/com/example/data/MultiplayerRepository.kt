package com.example.data

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MultiplayerRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(FirestoreInstanceProvider.get(context))

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Google Sign-In required for multiplayer")

    // Aligned with firestore.rules: allow list requires status in ['WAITING', 'ACTIVE']
    fun observeActiveSessions(): Flow<List<MultiplayerSessionDoc>> =
        db.collection("multiplayerSessions")
            .whereIn("status", listOf("WAITING", "ACTIVE"))
            .snapshots()
            .map { snap -> snap.toObjects(MultiplayerSessionDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

    fun observeSessionAircraftStates(sessionId: String, currentUserId: String): Flow<List<MultiplayerAircraftStateDoc>> =
        db.collection("multiplayerSessions")
            .document(sessionId)
            .collection("aircraftStates")
            .whereArrayContains("memberUids", currentUserId)
            .snapshots()
            .map { snap -> snap.toObjects(MultiplayerAircraftStateDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

    fun observeFriends(userId: String): Flow<List<FriendEntryDoc>> =
        db.collection("users").document(userId).collection("friends")
            .snapshots()
            .map { snap -> snap.toObjects(FriendEntryDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

    suspend fun createSession(
        hostCallsign: String,
        title: String,
        airportIcao: String,
        weatherPreset: String,
        missionType: String
    ): Result<String> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("multiplayerSessions").document()
        return try {
            docRef.set(
                mapOf(
                    "hostUserId" to uid,
                    "hostCallsign" to hostCallsign.take(32),
                    "title" to title.trim().ifBlank { "BDAS Flight Lobby" }.take(60),
                    "airportIcao" to airportIcao.take(8),
                    "weatherPreset" to weatherPreset.take(32),
                    "missionType" to missionType.take(32),
                    "status" to "WAITING",
                    "maxPlayers" to 8L,
                    "playerCount" to 1L,
                    "memberUids" to listOf(uid),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun joinSession(session: MultiplayerSessionDoc): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        if (uid in session.memberUids) return Result.success(Unit)
        if (session.playerCount >= session.maxPlayers) {
            return Result.failure(IllegalStateException("Session is full"))
        }
        val docRef = db.collection("multiplayerSessions").document(session.id)
        return try {
            val updatedMembers = session.memberUids + uid
            docRef.update(
                mapOf(
                    "memberUids" to updatedMembers,
                    "playerCount" to updatedMembers.size.toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun leaveSession(session: MultiplayerSessionDoc): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("multiplayerSessions").document(session.id)
        return try {
            if (session.hostUserId == uid) {
                docRef.update(
                    mapOf(
                        "status" to "FINISHED",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val updatedMembers = session.memberUids.filter { it != uid }
                docRef.update(
                    mapOf(
                        "memberUids" to updatedMembers,
                        "playerCount" to updatedMembers.size.toLong(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun startSession(sessionId: String): Result<Unit> {
        val docRef = db.collection("multiplayerSessions").document(sessionId)
        return try {
            requireUserId()
            docRef.update(
                mapOf(
                    "status" to "ACTIVE",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun publishAircraftState(
        sessionId: String,
        memberUids: List<String>,
        callsign: String,
        aircraftId: String,
        latitude: Double,
        longitude: Double,
        altitudeFt: Double,
        headingDeg: Double,
        pitchDeg: Double,
        rollDeg: Double,
        speedKts: Double
    ): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("multiplayerSessions")
            .document(sessionId)
            .collection("aircraftStates")
            .document(uid)
        val safeMembers = if (uid in memberUids) memberUids else memberUids + uid
        return try {
            docRef.set(
                mapOf(
                    "sessionId" to sessionId,
                    "playerUid" to uid,
                    "callsign" to callsign.take(32),
                    "aircraftId" to aircraftId.take(32),
                    "latitude" to latitude.coerceIn(18.0, 28.0),
                    "longitude" to longitude.coerceIn(86.0, 95.0),
                    "altitudeFt" to altitudeFt.coerceIn(-500.0, 60000.0),
                    "headingDeg" to ((headingDeg % 360.0) + 360.0) % 360.0,
                    "pitchDeg" to pitchDeg.coerceIn(-90.0, 90.0),
                    "rollDeg" to rollDeg.coerceIn(-180.0, 180.0),
                    "speedKts" to speedKts.coerceIn(0.0, 700.0),
                    "memberUids" to safeMembers.take(16),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addFriend(friendUid: String, friendCallsign: String, friendRank: String): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val cleanFriendUid = friendUid.trim().take(128)
        if (cleanFriendUid.isBlank() || cleanFriendUid == uid) {
            return Result.failure(IllegalArgumentException("Enter a valid friend pilot ID"))
        }
        val docRef = db.collection("users").document(uid).collection("friends").document(cleanFriendUid)
        return try {
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "friendUid" to cleanFriendUid,
                    "friendCallsign" to friendCallsign.trim().ifBlank { "BD-FRIEND" }.take(32),
                    "friendRank" to friendRank.take(32),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }
}
