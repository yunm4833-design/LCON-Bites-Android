package uk.co.lconbites

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF17131D)
private val Purple = Color(0xFF51417C)
private val Magenta = Color(0xFFB97C9F)
private val Paper = Color(0xFFFFF9F1)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Places.load(this)
        val prefs = getSharedPreferences("lcon_bites", MODE_PRIVATE)
        setContent {
            var tab by remember { mutableStateOf(0) }
            var query by remember { mutableStateOf("") }
            var category by remember { mutableStateOf("All") }
            var selected by remember { mutableStateOf<Restaurant?>(null) }
            var favourites by remember { mutableStateOf(prefs.getStringSet("favourites", emptySet())?.toSet() ?: emptySet()) }
            val categories = listOf("All") + Places.all.map { it.category }.distinct().sorted()
            val visible = Places.all.filter {
                (tab != 2 || favourites.contains(it.id.toString())) &&
                (category == "All" || it.category.equals(category, true)) &&
                (it.name.contains(query, true) || it.category.contains(query, true) || it.address.contains(query, true))
            }
            MaterialTheme(colorScheme = lightColorScheme(primary = Purple, secondary = Magenta, background = Paper)) {
                Scaffold(
                    containerColor = Paper,
                    bottomBar = {
                        NavigationBar(containerColor = Color.White) {
                            NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.LocationOn, null) }, label = { Text("Map") })
                            NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.Search, null) }, label = { Text("Explore") })
                            NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.Favorite, null) }, label = { Text("Saved") })
                        }
                    }
                ) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        Header()
                        if (tab == 0) {
                            OfflineMap(onSelect = { selected = it })
                            Text("LIA’S NEARBY PICKS · WITHIN 1.5 KM", modifier = Modifier.padding(start = 18.dp, top = 14.dp), color = Purple, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(Places.all.take(5)) { place -> PlaceCard(place, favourites.contains(place.id.toString()), { selected = place }, {
                                    favourites = favourites.toMutableSet().apply { if (contains(place.id.toString())) remove(place.id.toString()) else add(place.id.toString()) }
                                    prefs.edit().putStringSet("favourites", favourites).apply()
                                }) }
                            }
                        } else {
                            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), placeholder = { Text("Search places, food or address") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = RoundedCornerShape(16.dp))
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                categories.take(4).forEach { c -> FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) }) }
                            }
                            LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(visible) { place -> PlaceCard(place, favourites.contains(place.id.toString()), { selected = place }, {
                                    favourites = favourites.toMutableSet().apply { if (contains(place.id.toString())) remove(place.id.toString()) else add(place.id.toString()) }
                                    prefs.edit().putStringSet("favourites", favourites).apply()
                                }) }
                            }
                        }
                    }
                }
                selected?.let { place ->
                    AlertDialog(
                        onDismissRequest = { selected = null },
                        title = { Text(place.name, fontWeight = FontWeight.Bold) },
                        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(place.category.uppercase(), color = Purple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(place.description)
                            Text(place.address, color = Color.DarkGray)
                            Text("Ratings and photos are available in Google Maps (internet required)", color = Purple)
                            TextButton(onClick = { val q = Uri.encode(place.name + ", " + (if (place.address == "Leeds city centre") "Leeds, UK" else place.address + ", Leeds, UK")); startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$q"))) }) { Text("Open Google Maps · Ratings & photos") }
                            Text("Coordinates: ${place.latitude}, ${place.longitude}", fontSize = 11.sp, color = Color.Gray)
                        } },
                        confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } }
                    )
                }
            }
        }
    }
}

@Composable private fun Header() {
    Column(Modifier.fillMaxWidth().background(Ink).padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text("LEEDS CONSERVATOIRE", color = Color(0xFFE7B4DB), fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Text("LCON BITES", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LiaMascot(86)
            Column { Text("Lia’s food quest", color = Color(0xFFFFE7A5), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Your knight of nearby bites!", color = Color(0xFFF2E6F3), fontSize = 12.sp)
                Text("Offline guide · 1.5 km radius", color = Color(0xFFD9C9E5), fontSize = 10.sp) }
        }
        Spacer(Modifier.height(5.dp))
        Box(Modifier.width(58.dp).height(4.dp).background(Magenta, CircleShape))
    }
}

@Composable private fun PlaceCard(place: Restaurant, saved: Boolean, onOpen: () -> Unit, onSave: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).clickable { onOpen() }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(58.dp).background(Color(0xFFF0E5F4), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Text("✦", color = Purple, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(place.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("${place.category} · ${place.price}", color = Purple, fontSize = 11.sp)
            Text(place.address, color = Color.Gray, fontSize = 11.sp, maxLines = 1)
        }
        IconButton(onClick = onSave) { Icon(if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (saved) Magenta else Color.Gray) }
    }
}

@Composable private fun LiaMascot(size: Int) {
    // Original lightweight vector mascot: entirely bundled inside the APK.
    // Inspired by fantasy knight aesthetics; no external copyrighted artwork.
    Box(Modifier.size(size.dp).background(Color(0xFFFFF0D7), CircleShape).border(2.dp, Color(0xFFE7C887), CircleShape), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width; val h = this.size.height
            fun circle(color: Color, x: Float, y: Float, radius: Float) {
                drawCircle(color, radius*w, Offset(x*w,y*h))
            }
            // Navy ribbon and shoulders
            circle(Color(0xFF3C3D75), .29f,.35f,.17f)
            circle(Color(0xFF3C3D75), .72f,.35f,.17f)
            drawOval(Color(0xFF3C3D75), Offset(.19f*w,.68f*h), androidx.compose.ui.geometry.Size(.62f*w,.30f*h))
            drawOval(Color(0xFFF7F2EA), Offset(.29f*w,.68f*h), androidx.compose.ui.geometry.Size(.42f*w,.24f*h))
            // Golden hair silhouette and braided side buns
            circle(Color(0xFFE8B85F),.5f,.43f,.32f)
            circle(Color(0xFFDCA64F),.21f,.49f,.12f)
            circle(Color(0xFFDCA64F),.79f,.49f,.12f)
            circle(Color(0xFFFFE1B9),.5f,.51f,.245f)
            // Bangs
            val fringe=Path().apply { moveTo(.27f*w,.39f*h); quadraticTo(.31f*w,.12f*h,.62f*w,.23f*h); quadraticTo(.80f*w,.31f*h,.72f*w,.45f*h); quadraticTo(.55f*w,.35f*h,.48f*w,.40f*h); quadraticTo(.40f*w,.35f*h,.27f*w,.39f*h); close() }
            drawPath(fringe,Color(0xFFF2C777))
            // Eyes
            circle(Color.White,.40f,.52f,.059f);circle(Color.White,.60f,.52f,.059f)
            circle(Color(0xFF4797A3),.40f,.52f,.039f);circle(Color(0xFF4797A3),.60f,.52f,.039f)
            circle(Color(0xFF26365E),.40f,.52f,.018f);circle(Color(0xFF26365E),.60f,.52f,.018f)
            circle(Color(0xFFE99E9C),.31f,.61f,.038f);circle(Color(0xFFE99E9C),.69f,.61f,.038f)
            drawArc(Color(0xFF8C5261), 5f, 170f, false, Offset(.46f*w,.59f*h), androidx.compose.ui.geometry.Size(.08f*w,.07f*h), style=Stroke(width=2f))
            // Apron brooch
            circle(Color(0xFFE6BD68),.5f,.83f,.05f)
        }
    }
}
