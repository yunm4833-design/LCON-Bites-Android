package uk.co.lconbites

import android.os.Bundle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF17131D)
private val Purple = Color(0xFF6E2B91)
private val Magenta = Color(0xFFD72D8A)
private val Paper = Color(0xFFF7F5F8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("lcon_bites", MODE_PRIVATE)
        setContent {
            var tab by remember { mutableStateOf(0) }
            var query by remember { mutableStateOf("") }
            var category by remember { mutableStateOf("All") }
            var selected by remember { mutableStateOf<Restaurant?>(null) }
            var favourites by remember { mutableStateOf(prefs.getStringSet("favourites", emptySet())?.toSet() ?: emptySet()) }
            val categories = listOf("All", "Cafe", "Brunch", "Indian", "Chinese", "Street food", "Food court", "British")
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
                            OfflineMap(onSelect = { selected = it }, favourites = favourites)
                            Text("NEAR LEEDS CONSERVATOIRE", modifier = Modifier.padding(start = 18.dp, top = 14.dp), color = Purple, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
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
                            Text("Indicative price: ${place.price}")
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
        Text("Good food. Close to campus.", color = Color(0xFFD8D1DC), fontSize = 13.sp)
        Spacer(Modifier.height(5.dp))
        Box(Modifier.width(58.dp).height(4.dp).background(Magenta, CircleShape))
    }
}

@Composable private fun OfflineMap(onSelect: (Restaurant) -> Unit, favourites: Set<String>) {
    Box(Modifier.fillMaxWidth().height(235.dp).padding(14.dp).background(Color(0xFFEAE5ED), RoundedCornerShape(20.dp))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val road = Color(0xFFD2C9D9)
            for (i in 1..5) {
                drawLine(road, Offset(0f, h * i / 6f), Offset(w, h * i / 6f), 7f)
                drawLine(road, Offset(w * i / 6f, 0f), Offset(w * i / 6f, h), 7f)
            }
            drawLine(Color(0xFFB9D8C4), Offset(0f, h * .82f), Offset(w, h * .62f), 18f)
        }
        Column(Modifier.align(Alignment.TopStart).padding(12.dp)) {
            Text("LEEDS CITY CENTRE", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Offline preview map", color = Color.DarkGray, fontSize = 10.sp)
        }
        Places.all.take(8).forEachIndexed { index, place ->
            val x = .13f + (index % 4) * .23f
            val y = .34f + (index / 4) * .34f
            Box(Modifier.fillMaxWidth().fillMaxHeight().padding(start = 0.dp)) {
                Box(Modifier.offset(x = (x * 290).dp, y = (y * 165).dp).size(28.dp).background(Purple, CircleShape).clickable { onSelect(place) }, contentAlignment = Alignment.Center) {
                    Text("•", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Text("Map data pack: planned", Modifier.align(Alignment.BottomEnd).padding(10.dp), color = Color.DarkGray, fontSize = 9.sp)
    }
}

@Composable private fun PlaceCard(place: Restaurant, saved: Boolean, onOpen: () -> Unit, onSave: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).clickable { onOpen() }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).background(Color(0xFFF0E5F4), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Text(place.category.take(1).uppercase(), color = Purple, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(place.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("${place.category} · ${place.price}", color = Purple, fontSize = 11.sp)
            Text(place.address, color = Color.Gray, fontSize = 11.sp, maxLines = 1)
        }
        IconButton(onClick = onSave) { Icon(if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (saved) Magenta else Color.Gray) }
    }
}
