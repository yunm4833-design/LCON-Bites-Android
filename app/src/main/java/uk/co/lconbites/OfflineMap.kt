package uk.co.lconbites

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import kotlin.math.*

private data class GeoPoint(val lat: Double, val lon: Double)
private data class Road(val points: List<GeoPoint>, val kind: String)
private data class MapPin(val x: Float, val y: Float, val places: List<Restaurant>)

@Composable
fun OfflineMap(onSelect: (Restaurant) -> Unit) {
    val context = LocalContext.current
    val roads = remember {
        try {
            val data = context.assets.open("leeds_roads.json").bufferedReader().use { it.readText() }
            val array = JSONObject(data).getJSONArray("roads")
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                val coords = obj.getJSONArray("points")
                Road((0 until coords.length()).map { j ->
                    val p = coords.getJSONArray(j)
                    GeoPoint(p.getDouble(1), p.getDouble(0))
                }, obj.optString("kind"))
            }
        } catch (_: Exception) { emptyList() }
    }
    val homeLat = 53.7986
    val homeLon = -1.5354
    // The default view covers approximately 700 m across, centered on campus.
    var zoom by remember { mutableFloatStateOf(3.6f) }
    var centerLat by remember { mutableDoubleStateOf(homeLat) }
    var centerLon by remember { mutableDoubleStateOf(homeLon) }
    val baseLat = 0.031
    val baseLon = 0.050
    val spanLat = baseLat / zoom
    val spanLon = baseLon / zoom

    fun project(lat: Double, lon: Double, width: Float, height: Float): Offset {
        return Offset(
            (((lon - centerLon) / spanLon) + 0.5).toFloat() * width,
            (0.5 - ((lat - centerLat) / spanLat)).toFloat() * height
        )
    }

    BoxWithConstraints(
        Modifier.fillMaxWidth().height(360.dp).padding(12.dp)
            .background(Color(0xFFFFF9F1), RoundedCornerShape(20.dp))
    ) {
        val mapWidth = maxWidth
        val mapHeight = maxHeight
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        // One finger pans; two fingers pinch to zoom. Keep the location under
        // the gesture centroid stable during zooming.
        val gestureModifier = Modifier.pointerInput(widthPx, heightPx) {
            detectTransformGestures { centroid, pan, gestureZoom, _ ->
                val beforeLon = centerLon + ((centroid.x / widthPx) - .5) * baseLon / zoom
                val beforeLat = centerLat - ((centroid.y / heightPx) - .5) * baseLat / zoom
                val nextZoom = (zoom * gestureZoom).coerceIn(1f, 20f)
                val nextSpanLon = baseLon / nextZoom
                val nextSpanLat = baseLat / nextZoom
                zoom = nextZoom
                centerLon = (beforeLon - ((centroid.x + pan.x) / widthPx - .5) * nextSpanLon)
                    .coerceIn(homeLon - .025, homeLon + .025)
                centerLat = (beforeLat + ((centroid.y + pan.y) / heightPx - .5) * nextSpanLat)
                    .coerceIn(homeLat - .016, homeLat + .016)
            }
        }

        Canvas(Modifier.fillMaxSize().then(gestureModifier)) {
            roads.forEach { road ->
                if (road.points.size > 1) {
                    val projected = road.points.map { project(it.lat, it.lon, size.width, size.height) }
                    if (projected.none { it.x in -100f..(size.width + 100f) && it.y in -100f..(size.height + 100f) }) return@forEach
                    val path = Path()
                    projected.forEachIndexed { i, point ->
                        if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                    }
                    val major = road.kind == "primary" || road.kind == "secondary"
                    drawPath(path, Color.White, style = Stroke(width = if (major) 8f else 3.5f))
                    drawPath(path, Color(0xFFB9AFC0), style = Stroke(width = if (major) 2.8f else 1.3f))
                }
            }
            val campus = project(homeLat, homeLon, size.width, size.height)
            if (campus.x in 0f..size.width && campus.y in 0f..size.height) {
                drawCircle(Color(0xFFFFD67E), radius = 16f, center = campus)
                drawCircle(Color(0xFF43386C), radius = 7f, center = campus)
            }
        }

        // Group visible restaurants by screen-space grid. Only a small number
        // of clickable composables are rendered even with hundreds of places.
        val pins = remember(Places.all, centerLat, centerLon, zoom, widthPx, heightPx) {
            val cells = linkedMapOf<Pair<Int, Int>, MutableList<Pair<Restaurant, Offset>>>()
            val cellSize = 58f * (widthPx / mapWidth.value.coerceAtLeast(1f)).coerceIn(1f, 4f)
            Places.all.forEach { place ->
                val p = project(place.latitude, place.longitude, widthPx, heightPx)
                if (p.x in 22f..(widthPx - 22f) && p.y in 30f..(heightPx - 30f)) {
                    val key = Pair(floor(p.x / cellSize).toInt(), floor(p.y / cellSize).toInt())
                    cells.getOrPut(key) { mutableListOf() }.add(place to p)
                }
            }
            cells.values.map { group ->
                MapPin(group.map { it.second.x }.average().toFloat(),
                    group.map { it.second.y }.average().toFloat(), group.map { it.first })
            }
        }
        pins.forEach { pin ->
            val diameter = if (pin.places.size > 1) 36.dp else 27.dp
            Box(
                Modifier.offset(x = mapWidth * (pin.x / widthPx) - diameter / 2,
                    y = mapHeight * (pin.y / heightPx) - diameter / 2)
                    .size(diameter)
                    .background(if (pin.places.size > 1) Color(0xFF51417C) else Color(0xFF8D4BA2), CircleShape)
                    .border(1.5.dp, Color.White, CircleShape)
                    .clickable {
                        if (pin.places.size == 1) onSelect(pin.places.first())
                        else {
                            val avgLat = pin.places.map { it.latitude }.average()
                            val avgLon = pin.places.map { it.longitude }.average()
                            centerLat = avgLat
                            centerLon = avgLon
                            zoom = (zoom * 1.8f).coerceAtMost(20f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(if (pin.places.size == 1) "•" else pin.places.size.toString(),
                    color = Color.White, fontSize = if (pin.places.size > 99) 10.sp else 12.sp,
                    fontWeight = FontWeight.Bold)
            }
        }

        Column(Modifier.align(Alignment.TopStart).padding(12.dp)) {
            Text("LEEDS CONSERVATOIRE", color = Color(0xFF201728), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Offline map · Pinch to zoom · Drag to move", fontSize = 10.sp, color = Color.DarkGray)
        }
        Column(Modifier.align(Alignment.TopEnd).padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("+" to 1.6f, "−" to (1f / 1.6f)).forEach { (label, factor) ->
                Box(Modifier.size(36.dp).background(Color.White, RoundedCornerShape(10.dp))
                    .clickable { zoom = (zoom * factor).coerceIn(1f, 20f) },
                    contentAlignment = Alignment.Center) {
                    Text(label, color = Color(0xFF51417C), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(Modifier.size(36.dp).background(Color.White, RoundedCornerShape(10.dp))
                .clickable { centerLat = homeLat; centerLon = homeLon; zoom = 3.6f },
                contentAlignment = Alignment.Center) {
                Text("⌂", color = Color(0xFF51417C), fontSize = 22.sp)
            }
        }
        Text("© OpenStreetMap contributors", Modifier.align(Alignment.BottomEnd).padding(10.dp),
            color = Color.DarkGray, fontSize = 9.sp)
    }
}
