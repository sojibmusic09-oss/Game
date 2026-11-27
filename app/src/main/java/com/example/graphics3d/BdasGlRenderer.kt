package com.example.graphics3d

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.aircraft.AircraftCatalog
import com.example.aircraft.AircraftLivery
import com.example.aircraft.AircraftSpec
import com.example.flightphysics.CameraMode
import com.example.flightphysics.FlightState
import com.example.weather.WeatherState
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

enum class GraphicsPreset(val en: String, val bn: String, val gridRadius: Int, val cloudCount: Int, val rainCount: Int) {
    LOW("Low", "নিম্ন", 5, 10, 24),
    MEDIUM("Medium", "মাঝারি", 7, 18, 48),
    HIGH("High", "উচ্চ", 9, 26, 72),
    ULTRA("Ultra", "আল্ট্রা", 11, 36, 110);

    fun title(isBangla: Boolean): String = if (isBangla) bn else en
}

data class PeerAircraftRender(
    val callsign: String,
    val aircraftId: String,
    val xMeters: Float,
    val zMeters: Float,
    val altitudeMeters: Float,
    val headingDeg: Float,
    val pitchDeg: Float,
    val rollDeg: Float
)

enum class Scene3DMode {
    FLIGHT,
    PASSENGER_TERMINAL,
    PASSENGER_CABIN
}

data class Passenger3DRenderState(
    val playerX: Float = 0f,
    val playerZ: Float = 12f,
    val yawDeg: Float = 180f,
    val pitchDeg: Float = 0f,
    val thirdPerson: Boolean = false,
    val flightAltitudeM: Float = 9.1f,
    val flightPitchDeg: Float = 0f,
    val flightRollDeg: Float = 0f,
    val flightSpeedKts: Float = 0f,
    val seatRow: Int = 6,
    val seatIsRightSide: Boolean = true,
    val seated: Boolean = false
)

class BdasGlRenderer : GLSurfaceView.Renderer {
    @Volatile var sceneMode: Scene3DMode = Scene3DMode.FLIGHT
    @Volatile var flightState: FlightState = FlightState()
    @Volatile var aircraftSpec: AircraftSpec = AircraftCatalog.aircraft.first()
    @Volatile var livery: AircraftLivery = AircraftCatalog.liveries.first()
    @Volatile var weatherState: WeatherState = WeatherState()
    @Volatile var cameraMode: CameraMode = CameraMode.CHASE
    @Volatile var graphicsPreset: GraphicsPreset = GraphicsPreset.HIGH
    @Volatile var runwayHeadingDeg: Float = 140f
    @Volatile var runwayLengthM: Int = 3200
    @Volatile var groundElevMeters: Float = 9.1f
    @Volatile var peerAircraft: List<PeerAircraftRender> = emptyList()
    @Volatile var passengerState: Passenger3DRenderState = Passenger3DRenderState()

    private var program = 0
    private var uMvpLoc = 0
    private var uModelLoc = 0
    private var uColorLoc = 0
    private var uSunDirLoc = 0
    private var uAmbientLoc = 0
    private var uFogColorLoc = 0
    private var uFogDensityLoc = 0
    private var uCameraPosLoc = 0
    private var uEmissiveLoc = 0

    private lateinit var boxMesh: GlMesh
    private lateinit var cylMesh: GlMesh

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var camX = 0f
    private var camY = 15f
    private var camZ = 0f
    private var animClock = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        program = ShaderUtils.compileProgram()
        uMvpLoc = GLES30.glGetUniformLocation(program, "uMvpMatrix")
        uModelLoc = GLES30.glGetUniformLocation(program, "uModelMatrix")
        uColorLoc = GLES30.glGetUniformLocation(program, "uColor")
        uSunDirLoc = GLES30.glGetUniformLocation(program, "uSunDir")
        uAmbientLoc = GLES30.glGetUniformLocation(program, "uAmbient")
        uFogColorLoc = GLES30.glGetUniformLocation(program, "uFogColor")
        uFogDensityLoc = GLES30.glGetUniformLocation(program, "uFogDensity")
        uCameraPosLoc = GLES30.glGetUniformLocation(program, "uCameraPos")
        uEmissiveLoc = GLES30.glGetUniformLocation(program, "uEmissive")
        boxMesh = MeshBuilders.buildBox()
        cylMesh = MeshBuilders.buildCylinderZ(12)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.coerceAtLeast(1)
        Matrix.perspectiveM(projMatrix, 0, 58f, aspect, 0.4f, 18000f)
    }

    override fun onDrawFrame(gl: GL10?) {
        animClock += 0.016f
        val weather = weatherState
        val tod = weather.timeOfDay
        val flash = weather.lightningFlash * 0.45f
        val skyR = (tod.skyHorizonRgb[0] + flash).coerceIn(0f, 1f)
        val skyG = (tod.skyHorizonRgb[1] + flash).coerceIn(0f, 1f)
        val skyB = (tod.skyHorizonRgb[2] + flash).coerceIn(0f, 1f)

        GLES30.glClearColor(skyR, skyG, skyB, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        GLES30.glUseProgram(program)

        val sunElevRad = Math.toRadians(tod.sunElevationDeg.toDouble())
        val sunX = 0.45f
        val sunY = sin(sunElevRad).toFloat().coerceAtLeast(0.15f)
        val sunZ = -cos(sunElevRad).toFloat()
        GLES30.glUniform3f(uSunDirLoc, sunX, sunY, sunZ)
        GLES30.glUniform1f(uAmbientLoc, (tod.ambientLight * (1f - weather.preset.cloudCoverage * 0.22f) + flash).coerceIn(0.22f, 1.1f))
        GLES30.glUniform3f(uFogColorLoc, skyR, skyG, skyB)
        val fogDens = if (sceneMode == Scene3DMode.PASSENGER_TERMINAL) 0.002f else (weather.preset.fogDensity + weather.season.fogBoost) * 0.065f
        GLES30.glUniform1f(uFogDensityLoc, fogDens)

        when (sceneMode) {
            Scene3DMode.FLIGHT -> renderFlightScene(weather)
            Scene3DMode.PASSENGER_TERMINAL -> renderTerminalScene()
            Scene3DMode.PASSENGER_CABIN -> renderCabinScene(weather)
        }
    }

    private fun renderFlightScene(weather: WeatherState) {
        val fs = flightState
        // Convert FlightState (xMeters=East, zMeters=North) -> OpenGL (X=East, Y=Up, Z=-North)
        val acX = fs.xMeters
        val acY = fs.altitudeMeters
        val acZ = -fs.zMeters

        setupFlightCamera(fs, acX, acY, acZ, weather)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)
        GLES30.glUniform3f(uCameraPosLoc, camX, camY, camZ)

        // 1. Bangladesh Terrain Grid & River Ribbon
        renderTerrainChunks(acX, acZ, groundElevMeters)

        // 2. Airport Runway, Markings, Lights, Terminal & ATC Tower
        renderAirportComplex(groundElevMeters, weather.timeOfDay.ambientLight < 0.6f)

        // 3. Clouds & Rain Streaks
        renderCloudsAndRain(acX, acY, acZ, weather)

        // 4. Player Aircraft (skip external fuselage in cockpit view; draw cockpit frame instead)
        if (cameraMode == CameraMode.COCKPIT) {
            renderCockpitGlareShield(acX, acY, acZ, fs)
        } else {
            renderAircraftModel(
                x = acX, y = acY, z = acZ,
                headingDeg = fs.headingDeg, pitchDeg = fs.pitchDeg, rollDeg = fs.rollDeg,
                spec = aircraftSpec, livery = livery,
                gearDown = fs.gearDown, flapsLevel = fs.flapsLevel,
                propAngle = fs.propAngleDeg
            )
        }

        // 5. Multiplayer Peer Aircraft
        for (peer in peerAircraft) {
            renderAircraftModel(
                x = peer.xMeters, y = peer.altitudeMeters, z = -peer.zMeters,
                headingDeg = peer.headingDeg, pitchDeg = peer.pitchDeg, rollDeg = peer.rollDeg,
                spec = AircraftCatalog.findById(peer.aircraftId),
                livery = AircraftCatalog.liveries[1],
                gearDown = peer.altitudeMeters < groundElevMeters + 120f,
                flapsLevel = 0,
                propAngle = animClock * 700f
            )
        }
    }

    private fun setupFlightCamera(fs: FlightState, acX: Float, acY: Float, acZ: Float, weather: WeatherState) {
        val hRad = Math.toRadians(fs.headingDeg.toDouble())
        val pRad = Math.toRadians(fs.pitchDeg.toDouble())
        val fwdX = (sin(hRad) * cos(pRad)).toFloat()
        val fwdY = sin(pRad).toFloat()
        val fwdZ = (-cos(hRad) * cos(pRad)).toFloat()
        val rightX = cos(hRad).toFloat()
        val rightZ = sin(hRad).toFloat()

        val shakeX = sin(animClock * 41f) * weather.cameraShakeAmount * 0.25f
        val shakeY = cos(animClock * 53f) * weather.cameraShakeAmount * 0.25f

        when (cameraMode) {
            CameraMode.COCKPIT -> {
                camX = acX + fwdX * 8.5f + shakeX * 0.25f
                camY = acY + 2.4f + fwdY * 8.5f + shakeY * 0.25f
                camZ = acZ + fwdZ * 8.5f
                val rollRad = Math.toRadians(fs.rollDeg.toDouble())
                val upX = (-sin(rollRad) * rightX).toFloat()
                val upY = cos(rollRad).toFloat()
                val upZ = (-sin(rollRad) * rightZ).toFloat()
                Matrix.setLookAtM(
                    viewMatrix, 0,
                    camX, camY, camZ,
                    camX + fwdX * 120f, camY + fwdY * 120f - 4f, camZ + fwdZ * 120f,
                    upX, upY, upZ
                )
            }
            CameraMode.CHASE -> {
                val dist = if (aircraftSpec.id == "b777") 58f else 38f
                camX = acX - fwdX * dist + shakeX
                camY = (acY + 10f - fwdY * dist * 0.4f + shakeY).coerceAtLeast(groundElevMeters + 2.2f)
                camZ = acZ - fwdZ * dist
                Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, acX + fwdX * 15f, acY + 1.5f, acZ + fwdZ * 15f, 0f, 1f, 0f)
            }
            CameraMode.WING -> {
                camX = acX + rightX * 11f - fwdX * 4f + shakeX * 0.4f
                camY = acY + 3.2f + shakeY * 0.4f
                camZ = acZ + rightZ * 11f - fwdZ * 4f
                Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, acX + fwdX * 40f, acY - 2f, acZ + fwdZ * 40f, 0f, 1f, 0f)
            }
            CameraMode.TOWER -> {
                val towerOffsetX = rightX * 260f
                val towerOffsetZ = rightZ * 260f
                camX = 240f + towerOffsetX * 0.1f
                camY = groundElevMeters + 46f
                camZ = -220f + towerOffsetZ * 0.1f
                Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, acX, acY, acZ, 0f, 1f, 0f)
            }
        }
    }

    private fun renderTerrainChunks(acX: Float, acZ: Float, baseY: Float) {
        val tileSize = 1400f
        val centerIx = (acX / tileSize).roundToInt()
        val centerIz = (acZ / tileSize).roundToInt()
        val r = graphicsPreset.gridRadius

        for (ix in (centerIx - r)..(centerIx + r)) {
            for (iz in (centerIz - r)..(centerIz + r)) {
                val wx = ix * tileSize
                val wz = iz * tileSize
                // Procedural Bangladesh delta river channel winding across the plains
                val riverCenterX = sin(wz * 0.00035f) * 2600f + 2200f
                val isRiver = abs(wx - riverCenterX) < 750f
                val isAirportZone = abs(wx) < 1800f && abs(wz) < 2600f

                val tileHeight = if (isRiver || isAirportZone) 0f else {
                    val hillNoise = (sin(ix * 0.7f) * cos(iz * 0.6f) + 1f) * 0.5f
                    if (abs(wx) > 5500f) hillNoise * 42f else hillNoise * 8f
                }

                val color = when {
                    isAirportZone -> floatArrayOf(0.18f, 0.29f, 0.19f, 1f)
                    isRiver -> floatArrayOf(0.08f, 0.28f, 0.42f, 1f)
                    (ix + iz) % 2 == 0 -> floatArrayOf(0.14f, 0.34f, 0.18f, 1f)
                    else -> floatArrayOf(0.17f, 0.38f, 0.20f, 1f)
                }

                drawTransformedBox(
                    x = wx, y = baseY - 1.5f + tileHeight * 0.5f, z = wz,
                    sx = tileSize, sy = 3f + tileHeight, sz = tileSize,
                    rotY = 0f, rotX = 0f, rotZ = 0f,
                    color = color, emissive = 0f
                )
            }
        }
    }

    private fun renderAirportComplex(baseY: Float, lightsOn: Boolean) {
        val rwyLen = runwayLengthM.toFloat()
        val hDeg = runwayHeadingDeg

        // Main Runway asphalt aligned along runwayHeadingDeg
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, baseY + 0.05f, 0f)
        Matrix.rotateM(modelMatrix, 0, -hDeg, 0f, 1f, 0f)

        // Asphalt surface
        drawRelativeBox(modelMatrix, 0f, 0f, -rwyLen * 0.35f, 48f, 0.35f, rwyLen, floatArrayOf(0.14f, 0.15f, 0.17f, 1f), 0f)

        // Dashed Centerline Stripes
        val step = 120f
        var zPos = rwyLen * 0.12f
        while (zPos > -rwyLen * 0.82f) {
            drawRelativeBox(modelMatrix, 0f, 0.22f, zPos, 1.4f, 0.05f, 42f, floatArrayOf(0.95f, 0.95f, 0.95f, 1f), 0.1f)
            if (lightsOn || graphicsPreset.ordinal >= 1) {
                // Edge lights
                drawRelativeBox(modelMatrix, -23.5f, 0.5f, zPos, 1.2f, 0.9f, 1.2f, floatArrayOf(1f, 0.92f, 0.55f, 1f), if (lightsOn) 0.85f else 0.25f)
                drawRelativeBox(modelMatrix, 23.5f, 0.5f, zPos, 1.2f, 0.9f, 1.2f, floatArrayOf(1f, 0.92f, 0.55f, 1f), if (lightsOn) 0.85f else 0.25f)
            }
            zPos -= step
        }

        // Parallel Taxiway & Apron
        drawRelativeBox(modelMatrix, 110f, 0.02f, -150f, 26f, 0.3f, rwyLen * 0.7f, floatArrayOf(0.18f, 0.19f, 0.21f, 1f), 0f)
        drawRelativeBox(modelMatrix, 185f, 0.04f, -120f, 120f, 0.32f, 340f, floatArrayOf(0.22f, 0.23f, 0.25f, 1f), 0f)

        // Airport Terminal Building (Bangladesh Emerald + Glass Facade)
        drawRelativeBox(modelMatrix, 275f, 11f, -120f, 46f, 22f, 220f, floatArrayOf(0.84f, 0.88f, 0.92f, 1f), 0f)
        drawRelativeBox(modelMatrix, 251f, 11f, -120f, 3f, 14f, 200f, floatArrayOf(0.12f, 0.48f, 0.58f, 1f), if (lightsOn) 0.4f else 0.05f)
        drawRelativeBox(modelMatrix, 275f, 23f, -120f, 52f, 2.5f, 228f, floatArrayOf(0.0f, 0.55f, 0.35f, 1f), 0f)

        // ATC Tower
        drawRelativeBox(modelMatrix, 235f, 22f, -290f, 12f, 44f, 12f, floatArrayOf(0.80f, 0.82f, 0.86f, 1f), 0f)
        drawRelativeBox(modelMatrix, 235f, 46f, -290f, 20f, 8f, 20f, floatArrayOf(0.10f, 0.55f, 0.65f, 1f), if (lightsOn) 0.65f else 0.1f)
    }

    private fun renderAircraftModel(
        x: Float, y: Float, z: Float,
        headingDeg: Float, pitchDeg: Float, rollDeg: Float,
        spec: AircraftSpec, livery: AircraftLivery,
        gearDown: Boolean, flapsLevel: Int, propAngle: Float
    ) {
        val scale = when (spec.id) {
            "atr72" -> 0.85f
            "b777" -> 1.55f
            else -> 1.05f
        }
        val root = FloatArray(16)
        Matrix.setIdentityM(root, 0)
        Matrix.translateM(root, 0, x, y + 2.2f * scale, z)
        Matrix.rotateM(root, 0, -headingDeg, 0f, 1f, 0f)
        Matrix.rotateM(root, 0, pitchDeg, 1f, 0f, 0f)
        Matrix.rotateM(root, 0, -rollDeg, 0f, 0f, 1f)
        Matrix.scaleM(root, 0, scale, scale, scale)

        // Fuselage (cylinder along -Z forward)
        val fuseMat = FloatArray(16)
        Matrix.translateM(fuseMat, 0, root, 0, 0f, 0f, 0f)
        Matrix.rotateM(fuseMat, 0, 180f, 0f, 1f, 0f)
        Matrix.scaleM(fuseMat, 0, 3.2f, 3.4f, 24f)
        drawMeshWithMatrix(cylMesh, fuseMat, livery.fuselageColor, 0f)

        // Cockpit Windshield
        drawRelativeBox(root, 0f, 0.9f, -10.2f, 2.2f, 0.9f, 2.0f, floatArrayOf(0.08f, 0.16f, 0.25f, 1f), 0.1f)

        // Airline Livery Stripe
        drawRelativeBox(root, 0f, 0.1f, 0f, 3.35f, 0.55f, 20f, livery.stripeColor, 0f)

        // Main Wings (high wing for ATR72, low swept wing for jets)
        val wingY = if (spec.isTurboprop) 1.5f else -0.6f
        val sweepAngle = if (spec.isTurboprop) 4f else 22f
        val wingMatL = FloatArray(16)
        Matrix.translateM(wingMatL, 0, root, 0, -8.5f, wingY, 1.2f)
        Matrix.rotateM(wingMatL, 0, sweepAngle, 0f, 1f, 0f)
        Matrix.scaleM(wingMatL, 0, 16.5f, 0.35f, 3.8f)
        drawMeshWithMatrix(boxMesh, wingMatL, livery.fuselageColor, 0f)

        val wingMatR = FloatArray(16)
        Matrix.translateM(wingMatR, 0, root, 0, 8.5f, wingY, 1.2f)
        Matrix.rotateM(wingMatR, 0, -sweepAngle, 0f, 1f, 0f)
        Matrix.scaleM(wingMatR, 0, 16.5f, 0.35f, 3.8f)
        drawMeshWithMatrix(boxMesh, wingMatR, livery.fuselageColor, 0f)

        // Animated Flaps
        val flapDropDeg = flapsLevel * 12f
        if (flapsLevel > 0) {
            val flapMatL = FloatArray(16)
            Matrix.translateM(flapMatL, 0, root, 0, -5.5f, wingY - 0.15f, 3.1f)
            Matrix.rotateM(flapMatL, 0, -flapDropDeg, 1f, 0f, 0f)
            Matrix.scaleM(flapMatL, 0, 6.2f, 0.18f, 1.1f)
            drawMeshWithMatrix(boxMesh, flapMatL, livery.stripeColor, 0f)

            val flapMatR = FloatArray(16)
            Matrix.translateM(flapMatR, 0, root, 0, 5.5f, wingY - 0.15f, 3.1f)
            Matrix.rotateM(flapMatR, 0, -flapDropDeg, 1f, 0f, 0f)
            Matrix.scaleM(flapMatR, 0, 6.2f, 0.18f, 1.1f)
            drawMeshWithMatrix(boxMesh, flapMatR, livery.stripeColor, 0f)
        }

        // Tailfin + Horizontal Stabilizer
        drawRelativeBox(root, 0f, 3.8f, 10.6f, 0.45f, 5.4f, 3.6f, livery.tailColor, 0f)
        drawRelativeBox(root, 0f, if (spec.isTurboprop) 6.3f else 0.7f, 11.2f, 9.5f, 0.3f, 2.2f, livery.fuselageColor, 0f)

        // Twin Engines + Propellers / Fans
        for (side in floatArrayOf(-1f, 1f)) {
            val engX = side * 5.2f
            val engY = wingY - 0.85f
            val engZ = -1.2f
            drawRelativeBox(root, engX, engY, engZ, 1.6f, 1.6f, 3.8f, livery.stripeColor, 0f)
            if (spec.isTurboprop) {
                val propMat = FloatArray(16)
                Matrix.translateM(propMat, 0, root, 0, engX, engY, engZ - 2.1f)
                Matrix.rotateM(propMat, 0, propAngle, 0f, 0f, 1f)
                Matrix.scaleM(propMat, 0, 3.6f, 0.24f, 0.12f)
                drawMeshWithMatrix(boxMesh, propMat, floatArrayOf(0.18f, 0.18f, 0.2f, 1f), 0f)
            }
        }

        // Retractable Landing Gear
        if (gearDown) {
            val gearColor = floatArrayOf(0.15f, 0.15f, 0.17f, 1f)
            drawRelativeBox(root, 0f, -2.1f, -8.5f, 0.4f, 1.8f, 0.4f, gearColor, 0f)
            drawRelativeBox(root, -3.2f, -2.1f, 0.8f, 0.45f, 1.8f, 0.45f, gearColor, 0f)
            drawRelativeBox(root, 3.2f, -2.1f, 0.8f, 0.45f, 1.8f, 0.45f, gearColor, 0f)
        }
    }

    private fun renderCockpitGlareShield(acX: Float, acY: Float, acZ: Float, fs: FlightState) {
        val root = FloatArray(16)
        Matrix.setIdentityM(root, 0)
        Matrix.translateM(root, 0, acX, acY + 2.2f, acZ)
        Matrix.rotateM(root, 0, -fs.headingDeg, 0f, 1f, 0f)
        Matrix.rotateM(root, 0, fs.pitchDeg, 1f, 0f, 0f)
        Matrix.rotateM(root, 0, -fs.rollDeg, 0f, 0f, 1f)

        // Cockpit glare shield & side pillars
        drawRelativeBox(root, 0f, -0.55f, -9.8f, 2.4f, 0.55f, 1.0f, floatArrayOf(0.08f, 0.10f, 0.13f, 1f), 0f)
        drawRelativeBox(root, -1.25f, 0.35f, -9.8f, 0.14f, 1.5f, 0.25f, floatArrayOf(0.10f, 0.12f, 0.16f, 1f), 0f)
        drawRelativeBox(root, 1.25f, 0.35f, -9.8f, 0.14f, 1.5f, 0.25f, floatArrayOf(0.10f, 0.12f, 0.16f, 1f), 0f)
        drawRelativeBox(root, 0f, 1.1f, -9.8f, 2.6f, 0.15f, 0.3f, floatArrayOf(0.10f, 0.12f, 0.16f, 1f), 0f)
    }

    private fun renderCloudsAndRain(acX: Float, acY: Float, acZ: Float, weather: WeatherState) {
        val cloudColor = floatArrayOf(0.92f, 0.95f, 0.99f, 0.9f)
        val count = (graphicsPreset.cloudCount * weather.preset.cloudCoverage).roundToInt()
        for (i in 0 until count) {
            val angle = i * 2.39996f
            val radius = 650f + (i % 7) * 520f
            val cx = (acX / 2000f).roundToInt() * 2000f + cos(angle) * radius
            val cz = (acZ / 2000f).roundToInt() * 2000f + sin(angle) * radius
            val cy = 950f + (i % 4) * 180f
            drawTransformedBox(cx, cy, cz, 260f + (i % 3) * 110f, 48f, 180f, 0f, 0f, 0f, cloudColor, 0f)
        }

        if (weather.preset.rainIntensity > 0.05f) {
            val rainColor = floatArrayOf(0.68f, 0.80f, 0.92f, 0.85f)
            val rainCount = (graphicsPreset.rainCount * weather.preset.rainIntensity).roundToInt()
            for (i in 0 until rainCount) {
                val rx = acX + ((i * 73) % 80 - 40).toFloat()
                val rz = acZ + ((i * 37) % 80 - 40).toFloat()
                val ry = acY + 12f - ((animClock * 42f + i * 3.1f) % 24f)
                drawTransformedBox(rx, ry, rz, 0.08f, 2.2f, 0.08f, 0f, 0f, 0f, rainColor, 0.2f)
            }
        }
    }

    private fun renderTerminalScene() {
        val ps = passengerState
        val yawRad = Math.toRadians(ps.yawDeg.toDouble())
        val lookX = sin(yawRad).toFloat()
        val lookZ = -cos(yawRad).toFloat()

        if (ps.thirdPerson) {
            camX = ps.playerX - lookX * 4.5f
            camY = 2.6f
            camZ = ps.playerZ - lookZ * 4.5f
            Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, ps.playerX, 1.3f, ps.playerZ, 0f, 1f, 0f)
        } else {
            camX = ps.playerX
            camY = 1.65f
            camZ = ps.playerZ
            Matrix.setLookAtM(
                viewMatrix, 0,
                camX, camY, camZ,
                camX + lookX * 10f, camY + sin(Math.toRadians(ps.pitchDeg.toDouble())).toFloat() * 6f, camZ + lookZ * 10f,
                0f, 1f, 0f
            )
        }
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)
        GLES30.glUniform3f(uCameraPosLoc, camX, camY, camZ)

        // Terminal Floor & Ceiling
        drawTransformedBox(0f, -0.25f, 0f, 36f, 0.5f, 44f, 0f, 0f, 0f, floatArrayOf(0.20f, 0.24f, 0.30f, 1f), 0f)
        drawTransformedBox(0f, 6.2f, 0f, 36f, 0.5f, 44f, 0f, 0f, 0f, floatArrayOf(0.85f, 0.88f, 0.92f, 1f), 0.15f)
        // Side Walls
        drawTransformedBox(-18f, 3f, 0f, 0.6f, 6f, 44f, 0f, 0f, 0f, floatArrayOf(0.14f, 0.20f, 0.28f, 1f), 0f)
        drawTransformedBox(18f, 3f, 0f, 0.6f, 6f, 44f, 0f, 0f, 0f, floatArrayOf(0.14f, 0.20f, 0.28f, 1f), 0f)
        // Check-in Counters (Green Bangladesh Theme)
        drawTransformedBox(-8f, 0.6f, 5f, 5f, 1.2f, 1.4f, 0f, 0f, 0f, floatArrayOf(0.0f, 0.62f, 0.40f, 1f), 0.1f)
        // Security Scanner Arch
        drawTransformedBox(0f, 1.3f, -2f, 2.2f, 2.6f, 0.6f, 0f, 0f, 0f, floatArrayOf(0.15f, 0.72f, 0.80f, 1f), 0.2f)
        // Tea & Duty-Free Stall
        drawTransformedBox(9f, 1.1f, -6f, 4.5f, 2.2f, 2.5f, 0f, 0f, 0f, floatArrayOf(0.95f, 0.68f, 0.10f, 1f), 0.15f)
        // Boarding Gate Desk (-14Z)
        drawTransformedBox(0f, 0.7f, -15f, 6f, 1.4f, 1.5f, 0f, 0f, 0f, floatArrayOf(0.88f, 0.24f, 0.22f, 1f), 0.15f)

        // NPC Passengers & Staff in Bangladeshi attire colors
        val npcPositions = listOf(
            Triple(-6f, 6.8f, floatArrayOf(0.85f, 0.2f, 0.3f, 1f)),
            Triple(3f, 1.0f, floatArrayOf(0.15f, 0.65f, 0.45f, 1f)),
            Triple(-3f, -8f, floatArrayOf(0.92f, 0.75f, 0.2f, 1f)),
            Triple(6f, -11f, floatArrayOf(0.2f, 0.5f, 0.85f, 1f))
        )
        for ((idx, npc) in npcPositions.withIndex()) {
            val bob = sin(animClock * 3f + idx) * 0.05f
            drawTransformedBox(npc.first, 0.9f + bob, npc.second, 0.55f, 1.7f, 0.35f, 0f, 0f, 0f, npc.third, 0f)
        }

        if (ps.thirdPerson) {
            drawTransformedBox(ps.playerX, 0.95f, ps.playerZ, 0.6f, 1.75f, 0.4f, ps.yawDeg, 0f, 0f, floatArrayOf(0.1f, 0.8f, 0.6f, 1f), 0.1f)
        }
    }

    private fun renderCabinScene(weather: WeatherState) {
        val ps = passengerState
        val seatZ = -ps.seatRow * 1.1f
        val seatX = if (ps.seated) (if (ps.seatIsRightSide) 1.45f else -1.45f) else ps.playerX.coerceIn(-0.45f, 0.45f)
        val camZPos = if (ps.seated) seatZ else ps.playerZ.coerceIn(-12f, 2f)

        val yawRad = Math.toRadians(ps.yawDeg.toDouble())
        camX = seatX
        camY = if (ps.seated) 1.15f else 1.55f
        camZ = camZPos
        Matrix.setLookAtM(
            viewMatrix, 0,
            camX, camY, camZ,
            camX + sin(yawRad).toFloat() * 8f,
            camY + sin(Math.toRadians(ps.pitchDeg.toDouble())).toFloat() * 5f,
            camZ - cos(yawRad).toFloat() * 8f,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)
        GLES30.glUniform3f(uCameraPosLoc, camX, camY, camZ)

        // Cabin Floor, Ceiling & Side Walls with Window Gaps
        drawTransformedBox(0f, 0f, -5f, 4.2f, 0.2f, 22f, 0f, 0f, 0f, floatArrayOf(0.12f, 0.26f, 0.24f, 1f), 0f)
        drawTransformedBox(0f, 2.5f, -5f, 4.2f, 0.2f, 22f, 0f, 0f, 0f, floatArrayOf(0.92f, 0.94f, 0.96f, 1f), 0.25f)
        // Lower walls
        drawTransformedBox(-2.0f, 0.5f, -5f, 0.18f, 1.0f, 22f, 0f, 0f, 0f, floatArrayOf(0.86f, 0.88f, 0.91f, 1f), 0f)
        drawTransformedBox(2.0f, 0.5f, -5f, 0.18f, 1.0f, 22f, 0f, 0f, 0f, floatArrayOf(0.86f, 0.88f, 0.91f, 1f), 0f)
        // Overhead bins
        drawTransformedBox(-1.4f, 2.0f, -5f, 1.0f, 0.5f, 22f, 0f, 0f, 0f, floatArrayOf(0.82f, 0.85f, 0.90f, 1f), 0.1f)
        drawTransformedBox(1.4f, 2.0f, -5f, 1.0f, 0.5f, 22f, 0f, 0f, 0f, floatArrayOf(0.82f, 0.85f, 0.90f, 1f), 0.1f)

        // Passenger Seat Rows (1..10)
        for (row in 1..10) {
            val rz = -row * 1.1f
            drawTransformedBox(-1.2f, 0.55f, rz, 1.2f, 0.95f, 0.45f, 0f, 0f, 0f, floatArrayOf(0.02f, 0.44f, 0.34f, 1f), 0f)
            drawTransformedBox(1.2f, 0.55f, rz, 1.2f, 0.95f, 0.45f, 0f, 0f, 0f, floatArrayOf(0.02f, 0.44f, 0.34f, 1f), 0f)
        }

        // Exterior Wing & Moving Ground visible through the cabin window!
        val rollRad = ps.flightRollDeg
        drawTransformedBox(7.5f, 0.7f, -6f, 11f, 0.25f, 3.2f, 0f, 0f, -rollRad, floatArrayOf(0.92f, 0.94f, 0.96f, 1f), 0f)
        val groundDrop = -(ps.flightAltitudeM * 0.25f).coerceIn(4f, 140f)
        val scrollZ = -((animClock * (ps.flightSpeedKts * 0.3f)) % 120f)
        drawTransformedBox(22f, groundDrop, scrollZ, 90f, 1f, 260f, 0f, 0f, 0f, floatArrayOf(0.14f, 0.36f, 0.20f, 1f), 0f)

        if (weather.preset.cloudCoverage > 0.2f) {
            drawTransformedBox(26f, groundDrop * 0.4f + 8f, scrollZ * 0.8f, 18f, 4f, 28f, 0f, 0f, 0f, floatArrayOf(0.95f, 0.96f, 0.99f, 1f), 0.1f)
        }
    }

    private fun drawTransformedBox(
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        rotY: Float, rotX: Float, rotZ: Float,
        color: FloatArray, emissive: Float
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        if (rotY != 0f) Matrix.rotateM(modelMatrix, 0, rotY, 0f, 1f, 0f)
        if (rotX != 0f) Matrix.rotateM(modelMatrix, 0, rotX, 1f, 0f, 0f)
        if (rotZ != 0f) Matrix.rotateM(modelMatrix, 0, rotZ, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        drawMeshWithMatrix(boxMesh, modelMatrix, color, emissive)
    }

    private fun drawRelativeBox(
        parent: FloatArray,
        lx: Float, ly: Float, lz: Float,
        sx: Float, sy: Float, sz: Float,
        color: FloatArray, emissive: Float
    ) {
        val m = FloatArray(16)
        Matrix.translateM(m, 0, parent, 0, lx, ly, lz)
        Matrix.scaleM(m, 0, sx, sy, sz)
        drawMeshWithMatrix(boxMesh, m, color, emissive)
    }

    private fun drawMeshWithMatrix(mesh: GlMesh, model: FloatArray, color: FloatArray, emissive: Float) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, model, 0)
        GLES30.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(uModelLoc, 1, false, model, 0)
        GLES30.glUniform4f(uColorLoc, color[0], color[1], color[2], color[3])
        GLES30.glUniform1f(uEmissiveLoc, emissive)
        mesh.draw()
    }
}
