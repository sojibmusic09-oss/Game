package com.example.flightphysics

import com.example.aircraft.AircraftSpec
import com.example.missions.EmergencyType
import com.example.weather.WeatherState
import kotlin.math.*

enum class CameraMode(val en: String, val bn: String) {
    COCKPIT("Cockpit", "ককপিট"),
    CHASE("External Chase", "বাহ্যিক"),
    WING("Wing View", "উইং ভিউ"),
    TOWER("ATC Tower", "টাওয়ার ভিউ");

    fun title(isBangla: Boolean): String = if (isBangla) bn else en
}

data class FlightControlInput(
    val pitchInput: Float = 0f,      // -1 (nose down) .. +1 (nose up)
    val rollInput: Float = 0f,       // -1 (left) .. +1 (right)
    val yawInput: Float = 0f,        // -1 (left) .. +1 (right)
    val throttle: Float = 0f,        // 0..1
    val flapsLevel: Int = 1,         // 0=UP, 1=10deg, 2=20deg, 3=FULL
    val gearDown: Boolean = true,
    val brakesActive: Boolean = false
)

enum class FlightOutcomeStatus {
    IN_PROGRESS,
    LANDED_SAFELY,
    CRASHED
}

data class FlightState(
    val xMeters: Float = 0f,          // East (+X) from origin airport center
    val zMeters: Float = 0f,          // North (-Z in OpenGL, stored as North+ meters for clean math)
    val altitudeMeters: Float = 9.1f, // MSL meters
    val groundElevationMeters: Float = 9.1f,
    val latitude: Double = 23.8433,
    val longitude: Double = 90.3978,
    val headingDeg: Float = 140f,
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f,
    val airspeedMps: Float = 0f,
    val verticalSpeedMps: Float = 0f,
    val fuelKg: Float = 4000f,
    val throttleActual: Float = 0f,
    val flapsLevel: Int = 1,
    val gearDown: Boolean = true,
    val gearJammed: Boolean = false,
    val engineLeftFailed: Boolean = false,
    val engineRightFailed: Boolean = false,
    val isOnGround: Boolean = true,
    val hasTakenOff: Boolean = false,
    val stallWarning: Boolean = false,
    val gearWarning: Boolean = false,
    val lowFuelWarning: Boolean = false,
    val propAngleDeg: Float = 0f,
    val elapsedSec: Float = 0f,
    val lastTouchdownFpm: Int = 0,
    val outcome: FlightOutcomeStatus = FlightOutcomeStatus.IN_PROGRESS,
    val outcomeReason: String = ""
) {
    val airspeedKts: Float get() = airspeedMps * 1.94384f
    val altitudeFt: Float get() = altitudeMeters * 3.28084f
    val aglFeet: Float get() = ((altitudeMeters - groundElevationMeters).coerceAtLeast(0f)) * 3.28084f
    val verticalSpeedFpm: Int get() = (verticalSpeedMps * 196.85f).roundToInt()
}

class FlightPhysicsEngine {

    companion object {
        private const val GRAVITY = 9.81f
        private const val METERS_PER_DEG_LAT = 111320.0
    }

    fun createInitialState(
        originLat: Double,
        originLon: Double,
        groundElevFt: Int,
        runwayHeadingDeg: Float,
        spec: AircraftSpec,
        startAirborne: Boolean,
        startAltFt: Float,
        startSpeedKts: Float,
        fuelFraction: Float,
        emergencyType: EmergencyType
    ): FlightState {
        val groundM = groundElevFt * 0.3048f
        val altM = if (startAirborne) max(groundM + 50f, startAltFt * 0.3048f) else groundM
        val speedMps = if (startAirborne) startSpeedKts / 1.94384f else 0f
        val radHeading = Math.toRadians(runwayHeadingDeg.toDouble())
        // If airborne, place 4.2 km behind runway threshold aligned on final approach!
        val offsetBackM = if (startAirborne) -4200f else 0f
        val xM = (sin(radHeading) * offsetBackM).toFloat()
        val zM = (cos(radHeading) * offsetBackM).toFloat()
        val lat = originLat + (zM / METERS_PER_DEG_LAT)
        val lon = originLon + (xM / (METERS_PER_DEG_LAT * cos(Math.toRadians(originLat))))

        return FlightState(
            xMeters = xM,
            zMeters = zM,
            altitudeMeters = altM,
            groundElevationMeters = groundM,
            latitude = lat,
            longitude = lon,
            headingDeg = runwayHeadingDeg,
            pitchDeg = if (startAirborne) 2.5f else 0f,
            rollDeg = 0f,
            airspeedMps = speedMps,
            verticalSpeedMps = 0f,
            fuelKg = (spec.maxFuelKg * fuelFraction.coerceIn(0.03f, 1f)),
            throttleActual = if (startAirborne) 0.58f else 0f,
            flapsLevel = if (startAirborne) 1 else 1,
            gearDown = emergencyType != EmergencyType.GEAR_FAILURE && !startAirborne,
            gearJammed = emergencyType == EmergencyType.GEAR_FAILURE,
            engineLeftFailed = emergencyType == EmergencyType.ENGINE_FAILURE || emergencyType == EmergencyType.EMERGENCY_LANDING,
            engineRightFailed = false,
            isOnGround = !startAirborne,
            hasTakenOff = startAirborne
        )
    }

    fun step(
        current: FlightState,
        input: FlightControlInput,
        spec: AircraftSpec,
        weather: WeatherState,
        originLat: Double,
        originLon: Double,
        beginnerAssist: Boolean,
        sensitivity: Float,
        dtRaw: Float
    ): FlightState {
        if (current.outcome != FlightOutcomeStatus.IN_PROGRESS) return current

        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        val effectiveGearDown = if (current.gearJammed) false else input.gearDown
        val effectiveFlaps = input.flapsLevel.coerceIn(0, 3)

        // Smooth spool-up / spool-down of throttle
        val targetThrottle = if (current.fuelKg <= 0.1f) 0f else input.throttle.coerceIn(0f, 1f)
        val throttle = current.throttleActual + (targetThrottle - current.throttleActual) * (dt * 2.4f).coerceAtMost(1f)

        // Engine availability factor (supports single-engine failure!)
        val activeEngines = (if (!current.engineLeftFailed) 1 else 0) + (if (!current.engineRightFailed) 1 else 0)
        val engineFraction = activeEngines.toFloat() / spec.engines.coerceAtLeast(1)

        // Fuel burn & instantaneous aircraft mass
        val burnRate = spec.fuelBurnKgPerSecAtMax * throttle * engineFraction
        val nextFuel = (current.fuelKg - burnRate * dt).coerceAtLeast(0f)
        val payloadKg = spec.passengerCapacity * 72f * 0.7f
        val totalMassKg = spec.emptyMassKg + nextFuel + payloadKg

        // Air density decreases with altitude
        val altKm = (current.altitudeMeters / 1000f).coerceAtLeast(0f)
        val airDensity = (1.225f * exp(-altKm / 8.5f)).coerceIn(0.25f, 1.25f)

        // Thrust force (N)
        val thrustN = if (nextFuel > 0.1f) spec.maxThrustN * throttle * engineFraction else 0f

        // Aerodynamic Lift & Drag
        val stallSpeedMps = spec.stallSpeedKts / 1.94384f
        val speedSq = current.airspeedMps * current.airspeedMps
        val q = 0.5f * airDensity * speedSq

        val flapsLiftBonus = effectiveFlaps * 0.34f
        val flapsDrag = effectiveFlaps * 0.022f
        val gearDrag = if (effectiveGearDown) 0.026f else 0f

        // Angle of Attack approximated from pitch minus flight path angle
        val flightPathRad = if (current.airspeedMps > 5f) {
            atan2(current.verticalSpeedMps.toDouble(), current.airspeedMps.toDouble()).toFloat()
        } else 0f
        val pitchRad = Math.toRadians(current.pitchDeg.toDouble()).toFloat()
        val aoaRad = (pitchRad - flightPathRad).coerceIn(-0.35f, 0.45f)
        val aoaDeg = Math.toDegrees(aoaRad.toDouble()).toFloat()

        // Lift coefficient peaks near 16 deg AoA, drops sharply on stall
        val clBase = 0.40f + flapsLiftBonus + (aoaDeg * 0.095f)
        val cl = if (aoaDeg > 16.5f) (clBase * 0.45f).coerceAtLeast(0.15f) else clBase.coerceIn(-0.4f, 2.35f)
        val liftN = q * spec.wingAreaM2 * cl * cos(Math.toRadians(current.rollDeg.toDouble())).toFloat()

        val cd0 = 0.022f + flapsDrag + gearDrag
        val inducedCd = 0.048f * cl * cl
        val dragN = q * spec.wingAreaM2 * (cd0 + inducedCd)

        // Asymmetric yaw torque if one engine failed
        val asymmetricYaw = if (current.engineLeftFailed != current.engineRightFailed) {
            (if (current.engineLeftFailed) -1f else 1f) * throttle * 7.5f
        } else 0f

        val sens = sensitivity.coerceIn(0.4f, 1.8f)

        // Attitude dynamics
        var nextPitch = current.pitchDeg
        var nextRoll = current.rollDeg
        var nextHeading = current.headingDeg

        if (current.isOnGround) {
            // Ground roll: pitch rotates above 62% stall speed; nosewheel steering via yaw/roll
            val canRotate = current.airspeedMps > stallSpeedMps * 0.62f
            val pitchCmd = if (canRotate) input.pitchInput * 18f * spec.pitchAgility * sens else 0f
            nextPitch = (current.pitchDeg + (pitchCmd - current.pitchDeg * 1.1f) * dt).coerceIn(-2f, 16f)
            nextRoll *= (1f - dt * 5f).coerceAtLeast(0f)
            val steerRate = (input.yawInput + input.rollInput * 0.4f) * 26f * (current.airspeedMps / 40f).coerceIn(0.1f, 1f)
            nextHeading = ((nextHeading + (steerRate + asymmetricYaw * 0.4f) * dt) % 360f + 360f) % 360f
        } else {
            val pitchRate = input.pitchInput * 24f * spec.pitchAgility * sens
            val rollRate = input.rollInput * 42f * spec.rollAgility * sens
            val yawRate = input.yawInput * 16f * spec.yawAgility * sens

            nextPitch += (pitchRate + weather.gustPitchOffset * 0.35f) * dt
            nextRoll += (rollRate + weather.gustRollOffset * 0.45f) * dt

            // Beginner assist gently levels roll & dampens excessive pitch when stick is neutral
            if (beginnerAssist) {
                if (abs(input.rollInput) < 0.08f) {
                    nextRoll -= nextRoll * (dt * 1.6f).coerceAtMost(1f)
                }
                if (abs(input.pitchInput) < 0.08f) {
                    nextPitch -= (nextPitch - 1.5f) * (dt * 0.8f).coerceAtMost(1f)
                }
            }
            nextPitch = nextPitch.coerceIn(-45f, 45f)
            nextRoll = nextRoll.coerceIn(-68f, 68f)

            // Banked turn coordination + rudder + asymmetric engine + weather yaw
            val turnRate = sin(Math.toRadians(nextRoll.toDouble())).toFloat() * 14.5f
            nextHeading = ((nextHeading + (turnRate + yawRate + asymmetricYaw + weather.gustYawOffset) * dt) % 360f + 360f) % 360f
        }

        // Longitudinal & vertical acceleration
        val sinPitch = sin(Math.toRadians(nextPitch.toDouble())).toFloat()
        val cosPitch = cos(Math.toRadians(nextPitch.toDouble())).toFloat()
        val weightN = totalMassKg * GRAVITY

        val groundFrictionN = if (current.isOnGround) {
            val baseRollCoeff = if (effectiveGearDown) 0.022f else 0.38f // belly friction if gear failed
            val brakeCoeff = if (input.brakesActive && effectiveGearDown) 0.36f else 0f
            (weightN - liftN).coerceAtLeast(0f) * (baseRollCoeff + brakeCoeff)
        } else 0f

        val netForwardN = thrustN - dragN - groundFrictionN - (if (!current.isOnGround) weightN * sinPitch else 0f)
        val accelForward = netForwardN / totalMassKg
        val maxSpeedMps = (spec.maxSpeedKts / 1.94384f) * 1.15f
        val nextAirspeed = (current.airspeedMps + accelForward * dt).coerceIn(0f, maxSpeedMps)

        var nextVSpeed = if (current.isOnGround) {
            if (liftN > weightN * 1.01f && nextPitch > 2.5f) {
                ((liftN - weightN) / totalMassKg * 2.2f).coerceIn(0.8f, 8f)
            } else {
                0f
            }
        } else {
            val verticalForceN = liftN * cosPitch - weightN + thrustN * sinPitch * 0.35f
            val vAccel = (verticalForceN / totalMassKg) + (weather.gustPitchOffset * 0.12f)
            // Natural pitch-path alignment
            val targetVSpeed = nextAirspeed * sinPitch
            val blended = current.verticalSpeedMps + vAccel * dt
            (blended * 0.85f + targetVSpeed * 0.15f).coerceIn(-65f, 55f)
        }

        var nextAlt = current.altitudeMeters + nextVSpeed * dt

        // Wind drift vector (world meters/sec)
        val windMps = weather.windSpeedKts / 1.94384f
        val windDirRad = Math.toRadians(weather.windDirectionDeg.toDouble())
        // Wind pushes aircraft opposite to where it blows from
        val windVx = if (current.isOnGround) 0f else (-sin(windDirRad) * windMps).toFloat()
        val windVz = if (current.isOnGround) 0f else (-cos(windDirRad) * windMps).toFloat()

        val headingRad = Math.toRadians(nextHeading.toDouble())
        val groundSpeedHoriz = nextAirspeed * cosPitch
        val vx = (sin(headingRad) * groundSpeedHoriz).toFloat() + windVx
        val vz = (cos(headingRad) * groundSpeedHoriz).toFloat() + windVz

        val nextX = current.xMeters + vx * dt
        val nextZ = current.zMeters + vz * dt

        val nextLat = originLat + (nextZ / METERS_PER_DEG_LAT)
        val nextLon = originLon + (nextX / (METERS_PER_DEG_LAT * cos(Math.toRadians(originLat))))

        var nextOnGround = current.isOnGround
        var nextTakenOff = current.hasTakenOff
        var touchdownFpm = current.lastTouchdownFpm
        var outcome = current.outcome
        var reason = current.outcomeReason

        val aglM = nextAlt - current.groundElevationMeters
        if (current.isOnGround && aglM > 1.8f && nextVSpeed > 0.4f) {
            nextOnGround = false
            nextTakenOff = true
        } else if (!current.isOnGround && nextAlt <= current.groundElevationMeters) {
            // Ground contact / touchdown!
            val fpm = (nextVSpeed * 196.85f).roundToInt()
            touchdownFpm = fpm
            nextAlt = current.groundElevationMeters
            nextVSpeed = 0f
            nextOnGround = true
            nextRoll = 0f
            nextPitch = 0f

            val absRoll = abs(current.rollDeg)
            val maxSafeFpm = if (current.gearJammed) -380 else -720
            if (fpm < maxSafeFpm || absRoll > 22f) {
                outcome = FlightOutcomeStatus.CRASHED
                reason = if (absRoll > 22f) "Excessive bank angle on touchdown" else "Hard impact ($fpm FPM)"
            } else if (!effectiveGearDown && !current.gearJammed) {
                outcome = FlightOutcomeStatus.CRASHED
                reason = "Landing gear was retracted on touchdown"
            }
        }

        // Stall & warnings
        val stallThresholdMps = stallSpeedMps * (1f - effectiveFlaps * 0.06f)
        val isStalling = !nextOnGround && nextAirspeed < stallThresholdMps
        val isGearWarn = !nextOnGround && aglM < 180f && nextVSpeed < -1.5f && !effectiveGearDown && !current.gearJammed
        val isLowFuel = (nextFuel / spec.maxFuelKg) < 0.12f

        return current.copy(
            xMeters = nextX,
            zMeters = nextZ,
            altitudeMeters = nextAlt,
            latitude = nextLat,
            longitude = nextLon,
            headingDeg = nextHeading,
            pitchDeg = nextPitch,
            rollDeg = nextRoll,
            airspeedMps = nextAirspeed,
            verticalSpeedMps = nextVSpeed,
            fuelKg = nextFuel,
            throttleActual = throttle,
            flapsLevel = effectiveFlaps,
            gearDown = effectiveGearDown,
            isOnGround = nextOnGround,
            hasTakenOff = nextTakenOff,
            stallWarning = isStalling,
            gearWarning = isGearWarn,
            lowFuelWarning = isLowFuel,
            propAngleDeg = (current.propAngleDeg + (if (spec.isTurboprop) 720f else 1080f) * (0.25f + throttle) * engineFraction * dt) % 360f,
            elapsedSec = current.elapsedSec + dt,
            lastTouchdownFpm = touchdownFpm,
            outcome = outcome,
            outcomeReason = reason
        )
    }
}
