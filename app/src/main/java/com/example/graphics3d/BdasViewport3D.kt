package com.example.graphics3d

import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.aircraft.AircraftLivery
import com.example.aircraft.AircraftSpec
import com.example.flightphysics.CameraMode
import com.example.flightphysics.FlightState
import com.example.weather.WeatherState

@Composable
fun BdasViewport3D(
    modifier: Modifier = Modifier,
    sceneMode: Scene3DMode,
    flightState: FlightState,
    aircraftSpec: AircraftSpec,
    livery: AircraftLivery,
    weatherState: WeatherState,
    cameraMode: CameraMode,
    graphicsPreset: GraphicsPreset,
    runwayHeadingDeg: Float,
    runwayLengthM: Int,
    groundElevMeters: Float,
    peerAircraft: List<PeerAircraftRender> = emptyList(),
    passengerState: Passenger3DRenderState = Passenger3DRenderState()
) {
    val context = LocalContext.current
    val renderer = remember { BdasGlRenderer() }

    renderer.sceneMode = sceneMode
    renderer.flightState = flightState
    renderer.aircraftSpec = aircraftSpec
    renderer.livery = livery
    renderer.weatherState = weatherState
    renderer.cameraMode = cameraMode
    renderer.graphicsPreset = graphicsPreset
    renderer.runwayHeadingDeg = runwayHeadingDeg
    renderer.runwayLengthM = runwayLengthM
    renderer.groundElevMeters = groundElevMeters
    renderer.peerAircraft = peerAircraft
    renderer.passengerState = passengerState

    val glView = remember {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    DisposableEffect(glView) {
        glView.onResume()
        onDispose {
            glView.onPause()
        }
    }

    AndroidView(
        factory = { glView },
        modifier = modifier
    )
}
