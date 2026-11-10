package uk.co.lconbites

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import kotlin.math.cos

private data class GeoPoint(val lat: Double, val lon: Double)
private data class Road(val points: List<GeoPoint>, val kind: String)

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
    // All positions are projected from their actual longitude/latitude, not arranged
    // into a decorative grid. Map is centered on Leeds Conservatoire.
    val centerLat = 53.7986
    val centerLon = -1.5354
    val spanLat = 0.060
    val spanLon = 0.108
    fun position(lat: Double, lon: Double, width: Float, height: Float): Offset {
        val x = ((lon - centerLon) / spanLon + 0.5).toFloat() * width
        val y = (0.5 - (lat - centerLat) / spanLat).toFloat() * height
        return Offset(x, y)
    }

    Box(Modifier.fillMaxWidth().height(300.dp).padding(12.dp).background(Color(0xFFF1EDF2), RoundedCornerShape(20.dp))) {
        Canvas(Modifier.fillMaxSize()) {
            roads.forEach { road ->
                if (road.points.size > 1) {
                    val path = Path()
                    road.points.forEachIndexed { i, p ->
                        val at = position(p.lat, p.lon, size.width, size.height)
                        if (i == 0) path.moveTo(at.x, at.y) else path.lineTo(at.x, at.y)
                    }
                    drawPath(path, Color.White, style = Stroke(width = if (road.kind == "primary" || road.kind == "secondary") 7f else 3f))
                    drawPath(path, Color(0xFFB9AFC0), style = Stroke(width = if (road.kind == "primary" || road.kind == "secondary") 2.8f else 1.5f))
                }
            }
        }
        // BoxWithConstraints converts the map's actual on-screen size to dp for pin offsets.
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = maxWidth
            val h = maxHeight
            Places.all.forEach { place ->
                val x = ((place.longitude - centerLon) / spanLon + 0.5).toFloat()
                val y = (0.5 - (place.latitude - centerLat) / spanLat).toFloat()
                if (x in 0.04f..0.94f && y in 0.10f..0.90f) {
                    Box(
                        Modifier.offset(x = w * x - 12.dp, y = h * y - 12.dp)
                            .size(24.dp)
                            .background(Color(0xFF722C92), CircleShape)
                            .clickable { onSelect(place) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("•", color = Color.White, fontSize = 18.sp)
                    }
                }
            }
        }
        Column(Modifier.align(Alignment.TopStart).padding(12.dp)) {
            Text("LEEDS CITY CENTRE", color = Color(0xFF201728), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(if (roads.isEmpty()) "Offline coordinates • road data unavailable" else "Offline OSM roads • restaurant coordinates", fontSize = 10.sp, color = Color.DarkGray)
        }
        Text("© OpenStreetMap contributors", Modifier.align(Alignment.BottomEnd).padding(10.dp), color = Color.DarkGray, fontSize = 9.sp)
    }
}
