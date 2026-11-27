package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.AppLanguage
import com.example.ui.theme.*
import com.example.world.BangladeshGeoData
import com.example.world.BdAirport
import com.example.world.BdDivision
import kotlin.math.hypot
import kotlin.math.roundToInt

private data class DivisionVisualMeta(
    val division: BdDivision,
    val color: Color,
    val centerLat: Double,
    val centerLon: Double,
    val primaryIcao: String
)

private val DIVISION_META: List<DivisionVisualMeta> by lazy {
    val palette = mapOf(
        BdDivision.DHAKA to BangladeshEmerald,
        BdDivision.CHATTOGRAM to HudCyan,
        BdDivision.RAJSHAHI to WarningAmber,
        BdDivision.KHULNA to Color(0xFF4CAF50),
        BdDivision.BARISHAL to Color(0xFF29B6F6),
        BdDivision.SYLHET to Color(0xFF66BB6A),
        BdDivision.RANGPUR to Color(0xFFFFCA28),
        BdDivision.MYMENSINGH to Color(0xFFAB47BC)
    )
    val hubIcao = mapOf(
        BdDivision.DHAKA to "VGHS",
        BdDivision.CHATTOGRAM to "VGEG",
        BdDivision.RAJSHAHI to "VGRJ",
        BdDivision.KHULNA to "VGJR",
        BdDivision.BARISHAL to "VGBR",
        BdDivision.SYLHET to "VGSY",
        BdDivision.RANGPUR to "VGSD",
        BdDivision.MYMENSINGH to "VGHS"
    )
    BdDivision.entries.map { div ->
        val dists = BangladeshGeoData.districts.filter { it.division == div }
        val avgLat = dists.map { it.lat }.average()
        val avgLon = dists.map { it.lon }.average()
        DivisionVisualMeta(
            division = div,
            color = palette[div] ?: BangladeshEmerald,
            centerLat = avgLat,
            centerLon = avgLon,
            primaryIcao = hubIcao[div] ?: "VGHS"
        )
    }
}

enum class MapRoutePickTarget { ORIGIN, DESTINATION }

@Composable
fun BangladeshDivisionRouteMap(
    language: AppLanguage,
    selectedOriginIcao: String,
    selectedDestinationIcao: String,
    selectedDivision: BdDivision?,
    unlockedAirports: List<String>,
    onDivisionSelected: (BdDivision?) -> Unit,
    onOriginSelected: (String) -> Unit,
    onDestinationSelected: (String) -> Unit,
    onAirportFocused: (BdAirport) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isBn = language == AppLanguage.BANGLA
    var pickTarget by remember { mutableStateOf(MapRoutePickTarget.DESTINATION) }
    val textMeasurer = rememberTextMeasurer()

    val infiniteTransition = rememberInfiniteTransition(label = "route_pulse")
    val flightDotProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flight_dot"
    )

    val origAirport = remember(selectedOriginIcao) { BangladeshGeoData.findAirport(selectedOriginIcao) }
    val destAirport = remember(selectedDestinationIcao) { BangladeshGeoData.findAirport(selectedDestinationIcao) }
    val distanceNm = remember(origAirport, destAirport) {
        BangladeshGeoData.distanceNm(origAirport, destAirport)
    }
    val distanceKm = remember(distanceNm) { (distanceNm * 1.852).roundToInt() }
    val bearing = remember(origAirport, destAirport) {
        BangladeshGeoData.bearingDeg(origAirport.lat, origAirport.lon, destAirport.lat, destAirport.lon).roundToInt()
    }
    val etaMin = remember(distanceNm) { ((distanceNm / 275.0) * 60.0).roundToInt().coerceAtLeast(8) }

    Surface(
        color = CockpitDeepNavy,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, InstrumentBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Controls: Pick Mode (Origin vs Destination) & Division Quick Filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(InstrumentSlate.copy(alpha = 0.85f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = pickTarget == MapRoutePickTarget.ORIGIN,
                    onClick = { pickTarget = MapRoutePickTarget.ORIGIN },
                    label = { Text(if (isBn) "প্রস্থান: ${origAirport.icao}" else "Origin: ${origAirport.icao}") },
                    modifier = Modifier.defaultMinSize(minHeight = 40.dp).testTag("map_pick_origin_chip")
                )
                IconButton(
                    onClick = {
                        val prevOrig = selectedOriginIcao
                        onOriginSelected(selectedDestinationIcao)
                        onDestinationSelected(prevOrig)
                    },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("map_swap_route_button")
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = "Swap Origin and Destination", tint = HudCyan)
                }
                FilterChip(
                    selected = pickTarget == MapRoutePickTarget.DESTINATION,
                    onClick = { pickTarget = MapRoutePickTarget.DESTINATION },
                    label = { Text(if (isBn) "গন্তব্য: ${destAirport.icao}" else "Dest: ${destAirport.icao}") },
                    modifier = Modifier.defaultMinSize(minHeight = 40.dp).testTag("map_pick_dest_chip")
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    color = CockpitVoid,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BangladeshEmerald)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.FlightTakeoff, contentDescription = null, tint = BangladeshEmerald, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${distanceNm.roundToInt()} NM ($distanceKm km) • ${bearing}° • ~${etaMin}m",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextInstrumentWhite
                        )
                    }
                }
            }

            // 8 Divisions Quick Selector Ribbon
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedDivision == null,
                        onClick = { onDivisionSelected(null) },
                        label = { Text(if (isBn) "৮ বিভাগ" else "All 8 Divisions") },
                        modifier = Modifier.testTag("map_div_all")
                    )
                }
                items(DIVISION_META) { meta ->
                    val isSel = selectedDivision == meta.division
                    FilterChip(
                        selected = isSel,
                        onClick = {
                            onDivisionSelected(if (isSel) null else meta.division)
                            val ap = BangladeshGeoData.findAirport(meta.primaryIcao)
                            onAirportFocused(ap)
                        },
                        label = { Text(if (isBn) meta.division.bn else meta.division.en) },
                        modifier = Modifier.testTag("map_div_${meta.division.name.lowercase()}")
                    )
                }
            }

            // Interactive 8-Division & Flight Route Map Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("bangladesh_gis_canvas")
                        .pointerInput(pickTarget, selectedOriginIcao, selectedDestinationIcao, unlockedAirports) {
                            detectTapGestures { tapOffset ->
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                if (w <= 0f || h <= 0f) return@detectTapGestures
                                val minLat = 20.6
                                val maxLat = 26.8
                                val minLon = 88.0
                                val maxLon = 92.8
                                fun project(lat: Double, lon: Double): Offset {
                                    val x = ((lon - minLon) / (maxLon - minLon)).toFloat() * w
                                    val y = (1f - ((lat - minLat) / (maxLat - minLat)).toFloat()) * h
                                    return Offset(x, y)
                                }

                                // 1. Check nearest airport within 56px touch radius
                                val nearestAirport = BangladeshGeoData.airports
                                    .map { it to hypot((project(it.lat, it.lon).x - tapOffset.x).toDouble(), (project(it.lat, it.lon).y - tapOffset.y).toDouble()) }
                                    .minByOrNull { it.second }

                                if (nearestAirport != null && nearestAirport.second <= 68.0) {
                                    val ap = nearestAirport.first
                                    onAirportFocused(ap)
                                    val unlocked = ap.icao in unlockedAirports || ap.unlockCostCoins == 0L
                                    if (unlocked) {
                                        if (pickTarget == MapRoutePickTarget.ORIGIN) {
                                            if (ap.icao != selectedDestinationIcao) onOriginSelected(ap.icao)
                                        } else {
                                            if (ap.icao != selectedOriginIcao) onDestinationSelected(ap.icao)
                                        }
                                    }
                                    return@detectTapGestures
                                }

                                // 2. Otherwise select nearest of the 8 Divisions and highlight its region
                                val nearestDiv = DIVISION_META
                                    .map { it to hypot((project(it.centerLat, it.centerLon).x - tapOffset.x).toDouble(), (project(it.centerLat, it.centerLon).y - tapOffset.y).toDouble()) }
                                    .minByOrNull { it.second }
                                    ?.first
                                if (nearestDiv != null) {
                                    onDivisionSelected(nearestDiv.division)
                                    val hub = BangladeshGeoData.findAirport(nearestDiv.primaryIcao)
                                    onAirportFocused(hub)
                                }
                            }
                        }
                ) {
                    val minLat = 20.6
                    val maxLat = 26.8
                    val minLon = 88.0
                    val maxLon = 92.8
                    fun project(lat: Double, lon: Double): Offset {
                        val x = ((lon - minLon) / (maxLon - minLon)).toFloat() * size.width
                        val y = (1f - ((lat - minLat) / (maxLat - minLat)).toFloat()) * size.height
                        return Offset(x, y)
                    }

                    // National Outline
                    val outlinePath = Path()
                    BangladeshGeoData.nationalOutline.forEachIndexed { idx, pt ->
                        val p = project(pt.lat, pt.lon)
                        if (idx == 0) outlinePath.moveTo(p.x, p.y) else outlinePath.lineTo(p.x, p.y)
                    }
                    drawPath(outlinePath, color = BangladeshEmeraldDeep.copy(alpha = 0.42f))
                    drawPath(outlinePath, color = BangladeshEmerald.copy(alpha = 0.85f), style = Stroke(width = 2.8f))

                    // 8 Division Regions (soft zone halos + division name labels)
                    DIVISION_META.forEach { meta ->
                        val center = project(meta.centerLat, meta.centerLon)
                        val isDivActive = selectedDivision == null || selectedDivision == meta.division
                        val zoneAlpha = if (selectedDivision == meta.division) 0.34f else if (isDivActive) 0.14f else 0.05f
                        drawCircle(
                            color = meta.color.copy(alpha = zoneAlpha),
                            radius = size.minDimension * if (selectedDivision == meta.division) 0.15f else 0.11f,
                            center = center
                        )
                        drawCircle(
                            color = meta.color.copy(alpha = if (selectedDivision == meta.division) 0.85f else 0.35f),
                            radius = size.minDimension * if (selectedDivision == meta.division) 0.15f else 0.11f,
                            center = center,
                            style = Stroke(width = if (selectedDivision == meta.division) 2.5f else 1.2f)
                        )
                    }

                    // Major Rivers (Padma, Jamuna, Meghna)
                    BangladeshGeoData.rivers.forEach { river ->
                        val rPath = Path()
                        river.points.forEachIndexed { idx, pt ->
                            val p = project(pt.lat, pt.lon)
                            if (idx == 0) rPath.moveTo(p.x, p.y) else rPath.lineTo(p.x, p.y)
                        }
                        drawPath(rPath, color = HudCyan.copy(alpha = 0.65f), style = Stroke(width = 4f, cap = StrokeCap.Round))
                    }

                    // Bridges
                    BangladeshGeoData.bridges.forEach { br ->
                        drawCircle(color = WarningAmber, radius = 5f, center = project(br.lat, br.lon))
                    }

                    // All 64 Districts colored by Division
                    BangladeshGeoData.districts.forEach { dist ->
                        val divColor = DIVISION_META.firstOrNull { it.division == dist.division }?.color ?: TextMutedSlate
                        val dimmed = selectedDivision != null && selectedDivision != dist.division
                        drawCircle(
                            color = divColor.copy(alpha = if (dimmed) 0.25f else 0.85f),
                            radius = if (dimmed) 2.5f else 4f,
                            center = project(dist.lat, dist.lon)
                        )
                    }

                    // Active Flight Route Arc (Origin -> Destination)
                    val pOrig = project(origAirport.lat, origAirport.lon)
                    val pDest = project(destAirport.lat, destAirport.lon)
                    if (origAirport.icao != destAirport.icao) {
                        val mid = Offset((pOrig.x + pDest.x) * 0.5f, (pOrig.y + pDest.y) * 0.5f)
                        val dx = pDest.x - pOrig.x
                        val dy = pDest.y - pOrig.y
                        val ctrl = Offset(mid.x - dy * 0.18f, mid.y + dx * 0.18f)

                        val routePath = Path().apply {
                            moveTo(pOrig.x, pOrig.y)
                            quadraticTo(ctrl.x, ctrl.y, pDest.x, pDest.y)
                        }
                        drawPath(
                            path = routePath,
                            brush = Brush.linearGradient(listOf(BangladeshEmerald, HudCyan, WarningAmber), start = pOrig, end = pDest),
                            style = Stroke(width = 5f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f))
                        )

                        // Animated plane dot along quadratic Bezier
                        val t = flightDotProgress
                        val inv = 1f - t
                        val bx = inv * inv * pOrig.x + 2f * inv * t * ctrl.x + t * t * pDest.x
                        val by = inv * inv * pOrig.y + 2f * inv * t * ctrl.y + t * t * pDest.y
                        drawCircle(color = TextInstrumentWhite, radius = 6.5f, center = Offset(bx, by))
                        drawCircle(color = HudCyan, radius = 11f, center = Offset(bx, by), style = Stroke(width = 2f))
                    }

                    // Division Name Labels
                    DIVISION_META.forEach { meta ->
                        val center = project(meta.centerLat, meta.centerLon)
                        val label = if (isBn) meta.division.bn else meta.division.en
                        val measured = textMeasurer.measure(
                            text = label,
                            style = TextStyle(
                                color = if (selectedDivision == meta.division) WarningAmber else TextInstrumentWhite.copy(alpha = 0.88f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(
                                (center.x - measured.size.width / 2f).coerceIn(4f, (size.width - measured.size.width - 4f).coerceAtLeast(4f)),
                                (center.y - measured.size.height / 2f).coerceIn(4f, (size.height - measured.size.height - 4f).coerceAtLeast(4f))
                            )
                        )
                    }

                    // 8 Airport Pins & ICAO Tags
                    BangladeshGeoData.airports.forEach { ap ->
                        val p = project(ap.lat, ap.lon)
                        val unlocked = ap.icao in unlockedAirports || ap.unlockCostCoins == 0L
                        val isOrig = ap.icao == selectedOriginIcao
                        val isDest = ap.icao == selectedDestinationIcao
                        val pinColor = when {
                            isOrig -> BangladeshEmerald
                            isDest -> CautionCrimson
                            unlocked -> HudCyan
                            else -> WarningAmber
                        }
                        if (isOrig || isDest) {
                            drawCircle(color = pinColor.copy(alpha = 0.28f), radius = 17f, center = p)
                        }
                        drawCircle(color = pinColor, radius = if (isOrig || isDest) 9.5f else 6.5f, center = p)
                        drawCircle(color = CockpitVoid, radius = 3f, center = p)

                        val tagText = when {
                            isOrig -> "${ap.icao} (DEP)"
                            isDest -> "${ap.icao} (ARR)"
                            else -> ap.icao
                        }
                        val measuredIcao = textMeasurer.measure(
                            text = tagText,
                            style = TextStyle(color = pinColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                        drawText(
                            textLayoutResult = measuredIcao,
                            topLeft = Offset(
                                (p.x + 10f).coerceAtMost((size.width - measuredIcao.size.width - 2f).coerceAtLeast(2f)),
                                (p.y - measuredIcao.size.height / 2f).coerceIn(2f, (size.height - measuredIcao.size.height - 2f).coerceAtLeast(2f))
                            )
                        )
                    }
                }
            }
        }
    }
}
