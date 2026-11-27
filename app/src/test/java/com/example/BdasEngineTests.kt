package com.example

import com.example.aircraft.AircraftCatalog
import com.example.airline.AirlineEconomyEngine
import com.example.core.AppLanguage
import com.example.data.AirlineDoc
import com.example.data.AirlineRouteDoc
import com.example.data.FleetAircraftDoc
import com.example.flightphysics.FlightControlInput
import com.example.flightphysics.FlightOutcomeStatus
import com.example.flightphysics.FlightPhysicsEngine
import com.example.missions.EmergencyType
import com.example.missions.MissionCatalog
import com.example.passenger.PassengerController
import com.example.passenger.PassengerSessionState
import com.example.passenger.PassengerStage
import com.example.weather.*
import com.example.world.BangladeshGeoData
import com.example.world.BdDivision
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class BdasEngineTests {

    @Test
    fun `bangladesh geo data covers all 8 divisions 64 districts and 8 airports`() {
        assertEquals(8, BdDivision.entries.size)
        assertEquals(64, BangladeshGeoData.districts.size)
        assertEquals(64, BangladeshGeoData.districts.map { it.id }.distinct().size)
        assertEquals(8, BangladeshGeoData.airports.size)
        assertEquals(3, BangladeshGeoData.rivers.size)
        assertEquals(2, BangladeshGeoData.bridges.size)

        val vghs = BangladeshGeoData.findAirport("VGHS")
        val vgeg = BangladeshGeoData.findAirport("VGEG")
        val distNm = BangladeshGeoData.distanceNm(vghs, vgeg)
        assertTrue("Dhaka to Chattogram should be ~115-135 NM, got $distNm", distNm in 110.0..145.0)
        val bearing = BangladeshGeoData.bearingDeg(vghs.lat, vghs.lon, vgeg.lat, vgeg.lon)
        assertTrue("Bearing VGHS->VGEG should be southeast (~135 deg), got $bearing", bearing in 110f..165f)
    }

    @Test
    fun `flight physics achieves takeoff lift climb and fuel burn`() {
        val engine = FlightPhysicsEngine()
        val spec = AircraftCatalog.findById("atr72")
        val weather = WeatherState(windSpeedKts = 0f, turbulenceIntensity = 0f)
        var state = engine.createInitialState(
            originLat = 23.8433,
            originLon = 90.3978,
            groundElevFt = 30,
            runwayHeadingDeg = 140f,
            spec = spec,
            startAirborne = false,
            startAltFt = 30f,
            startSpeedKts = 0f,
            fuelFraction = 0.8f,
            emergencyType = EmergencyType.NONE
        )
        val initialFuel = state.fuelKg
        for (i in 0 until 900) {
            val rotate = if (state.airspeedKts > 85f) 0.55f else 0f
            state = engine.step(
                current = state,
                input = FlightControlInput(throttle = 1.0f, pitchInput = rotate, flapsLevel = 2, gearDown = true),
                spec = spec,
                weather = weather,
                originLat = 23.8433,
                originLon = 90.3978,
                beginnerAssist = true,
                sensitivity = 1.0f,
                dtRaw = 0.03f
            )
        }
        assertTrue("Aircraft should become airborne", state.hasTakenOff)
        assertFalse("Aircraft should no longer be on ground", state.isOnGround)
        assertTrue("Altitude should climb above 150 ft, got ${state.altitudeFt}", state.altitudeFt > 150f)
        assertTrue("Fuel should be consumed", state.fuelKg < initialFuel)
    }

    @Test
    fun `single engine failure induces asymmetric yaw and lower thrust`() {
        val engine = FlightPhysicsEngine()
        val spec = AircraftCatalog.findById("a320")
        val weather = WeatherState(windSpeedKts = 0f, turbulenceIntensity = 0f)
        val start = engine.createInitialState(
            originLat = 23.8433,
            originLon = 90.3978,
            groundElevFt = 30,
            runwayHeadingDeg = 180f,
            spec = spec,
            startAirborne = true,
            startAltFt = 2000f,
            startSpeedKts = 170f,
            fuelFraction = 0.6f,
            emergencyType = EmergencyType.ENGINE_FAILURE
        )
        assertTrue(start.engineLeftFailed)
        var cur = start
        repeat(60) {
            cur = engine.step(
                current = cur,
                input = FlightControlInput(throttle = 0.9f, gearDown = false),
                spec = spec,
                weather = weather,
                originLat = 23.8433,
                originLon = 90.3978,
                beginnerAssist = false,
                sensitivity = 1.0f,
                dtRaw = 0.03f
            )
        }
        assertNotEquals("Asymmetric thrust should yaw heading away from 180", 180f, cur.headingDeg, 0.5f)
    }

    @Test
    fun `gear up touchdown crashes normal aircraft but gear jammed soft touchdown survives`() {
        val engine = FlightPhysicsEngine()
        val spec = AircraftCatalog.findById("b737")
        val weather = WeatherState(windSpeedKts = 0f)

        val normalAirborne = engine.createInitialState(
            23.84, 90.39, 30, 140f, spec,
            startAirborne = true, startAltFt = 105f, startSpeedKts = 108f,
            fuelFraction = 0.5f, emergencyType = EmergencyType.NONE
        ).copy(altitudeMeters = 9.18f, pitchDeg = -3.0f, verticalSpeedMps = -2.2f, gearDown = false)

        val crashed = engine.step(
            normalAirborne,
            FlightControlInput(throttle = 0.1f, pitchInput = -0.5f, gearDown = false),
            spec, weather, 23.84, 90.39, false, 1.0f, 0.05f
        )
        assertEquals(FlightOutcomeStatus.CRASHED, crashed.outcome)

        val jammedAirborne = engine.createInitialState(
            23.84, 90.39, 30, 140f, spec,
            startAirborne = true, startAltFt = 105f, startSpeedKts = 108f,
            fuelFraction = 0.5f, emergencyType = EmergencyType.GEAR_FAILURE
        ).copy(altitudeMeters = 9.18f, pitchDeg = -1.5f, verticalSpeedMps = -1.5f)

        val bellyLanded = engine.step(
            jammedAirborne,
            FlightControlInput(throttle = 0.1f, gearDown = false),
            spec, weather, 23.84, 90.39, false, 1.0f, 0.05f
        )
        assertEquals(FlightOutcomeStatus.IN_PROGRESS, bellyLanded.outcome)
        assertTrue(bellyLanded.isOnGround)
    }

    @Test
    fun `dynamic weather scales turbulence by storm and season`() {
        val controller = DynamicWeatherController()
        val clear = controller.createInitialState(
            WeatherPreset.CLEAR, TimeOfDay.DAY, SeasonType.AUTUMN_CLEAR, WindDirectionMode.HEADWIND, 140f
        )
        val storm = controller.createInitialState(
            WeatherPreset.THUNDERSTORM, TimeOfDay.NIGHT, SeasonType.SUMMER_MONSOON, WindDirectionMode.CROSSWIND, 140f
        )
        val steppedStorm = controller.step(storm, 0.05f, 3000f, 180f)
        assertTrue(steppedStorm.turbulenceIntensity > clear.turbulenceIntensity)
        assertEquals(TurbulenceLevel.SEVERE, steppedStorm.turbulenceLevel)
        assertEquals(1.0f, steppedStorm.lightningFlash, 0.2f)
        assertTrue(steppedStorm.passengerAnxiety > 0.6f)
    }

    @Test
    fun `airline economy calculates demand elasticity and cycle profit`() {
        val lowPriceDemand = AirlineEconomyEngine.computeDemandScore("VGHS", "VGEG", 2000L, 80L)
        val highPriceDemand = AirlineEconomyEngine.computeDemandScore("VGHS", "VGEG", 9500L, 80L)
        assertTrue("Lower price should drive higher demand ($lowPriceDemand vs $highPriceDemand)", lowPriceDemand > highPriceDemand)

        val airline = AirlineDoc(id = "a1", name = "Bengal Sky", iataCode = "BS", pilotsHired = 2L, cabinCrewHired = 4L)
        val fleet = listOf(FleetAircraftDoc(id = "f1", aircraftModelId = "a320"))
        val routes = listOf(AirlineRouteDoc(id = "r1", originIcao = "VGHS", destinationIcao = "VGEG", ticketPriceCoins = 4200L, demandScore = 85L, assignedFleetId = "f1", active = true))
        val report = AirlineEconomyEngine.simulateOperatingCycle(airline, fleet, routes)
        assertTrue(report.passengersCarried > 50)
        assertTrue(report.grossRevenueCoins > 0L)
    }

    @Test
    fun `passenger controller clamps cabin boundaries and covers all 15 stages`() {
        assertEquals(15, PassengerStage.entries.size)
        assertEquals(8, MissionCatalog.emergencyMissions.size)
        assertEquals(10, MissionCatalog.achievements.size)

        val controller = PassengerController()
        var state = PassengerSessionState(stage = PassengerStage.BOARD_AIRCRAFT)
        repeat(50) {
            state = controller.move(state, forward = 0f, strafe = 1f, dt = 0.2f)
        }
        assertTrue("Cabin X must stay within aisle bounds", abs(state.renderState.playerX) <= 0.451f)
        val looked = controller.look(state, deltaYaw = 400f, deltaPitch = 80f)
        assertTrue(looked.renderState.yawDeg in 0f..360f)
        assertEquals(45f, looked.renderState.pitchDeg, 0.01f)
        assertTrue(controller.crewMessage(AppLanguage.BANGLA, "চা").contains("ধন্যবাদ"))
    }
}
