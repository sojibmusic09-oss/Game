package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.replay.BdasLocalDatabase
import com.example.replay.ReplayFrameEntity
import com.example.replay.ReplayRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BdasRoomRobolectricTest {

    @Test
    fun `room replay database saves and loads frames`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, BdasLocalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = ReplayRepository(db.replayDao())
            val id = repo.saveRecordedReplay(
                title = "VGHS -> VGEG",
                aircraftId = "atr72",
                originIcao = "VGHS",
                destinationIcao = "VGEG",
                weatherPreset = "CLEAR",
                durationSec = 12f,
                landingFpm = -150,
                frames = listOf(
                    ReplayFrameEntity(replayId = 0L, timestampSec = 0f, xMeters = 0f, zMeters = 0f, altitudeMeters = 9f, pitchDeg = 0f, rollDeg = 0f, headingDeg = 140f, speedKts = 0f, throttle = 0f, flapsLevel = 1, gearDown = true, eventTag = ""),
                    ReplayFrameEntity(replayId = 0L, timestampSec = 5f, xMeters = 100f, zMeters = 120f, altitudeMeters = 120f, pitchDeg = 8f, rollDeg = 0f, headingDeg = 140f, speedKts = 115f, throttle = 1f, flapsLevel = 1, gearDown = false, eventTag = "TAKEOFF")
                )
            )
            val loaded = repo.loadFrames(id)
            assertEquals(2, loaded.size)
            assertEquals("TAKEOFF", loaded[1].eventTag)
        } finally {
            db.close()
        }
    }
}
