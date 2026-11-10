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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
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
                        NavigationBar(containerColor = Color(0xFFFFF4E6)) {
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
                            Text("MELES’ NEARBY PICKS · WITHIN 1.5 KM", modifier = Modifier.padding(start = 18.dp, top = 14.dp), color = Purple, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
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
                            FoodIllustration(place, 108)
                            Text("Illustrative food icon · not a restaurant photo", fontSize = 10.sp, color = Color.Gray)
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
    Column(Modifier.fillMaxWidth().background(Color(0xFF302455)).padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text("LEEDS CONSERVATOIRE", color = Color(0xFFE7B4DB), fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Text("✦ LCON BITES", color = Color(0xFFFFF8E7), fontSize = 29.sp, fontWeight = FontWeight.Black)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MelesMascot(86)
            Column { Text("♛ Meles’ Food Quest", color = Color(0xFFFFE7A5), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Your knight of nearby bites!", color = Color(0xFFF2E6F3), fontSize = 12.sp)
                Text("Offline guide · 1.5 km radius", color = Color(0xFFD9C9E5), fontSize = 10.sp) }
        }
        Spacer(Modifier.height(5.dp))
        Box(Modifier.width(58.dp).height(4.dp).background(Magenta, CircleShape))
    }
}

@Composable private fun PlaceCard(place: Restaurant, saved: Boolean, onOpen: () -> Unit, onSave: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFFFFFDF8), RoundedCornerShape(16.dp)).border(1.dp, Color(0xFFE7C887), RoundedCornerShape(16.dp)).clickable { onOpen() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        FoodIllustration(place, 58)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(place.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("${place.category} · ${place.price}", color = Purple, fontSize = 11.sp)
            Text(place.address, color = Color.Gray, fontSize = 11.sp, maxLines = 1)
        }
        IconButton(onClick = onSave) { Icon(if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (saved) Magenta else Color.Gray) }
    }
}

@Composable private fun MelesMascot(size: Int) {
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

/**
 * Offline food-category illustrations. A stable restaurant-specific seed chooses
 * between related food symbols; it never misrepresents these as venue photographs.
 */
private fun foodArtwork(place: Restaurant): Pair<String, Color> {
    val text = (place.category + " " + place.name).lowercase()
    val variants: List<String>
    val tint: Color
    when {
        listOf("burger", "hamburger", "five guys", "mcdonald", "wendy").any { it in text } -> {
            variants = listOf("🍔", "🍟", "🍔"); tint = Color(0xFFFFE2B8)
        }
        listOf("pizza", "pizzeria").any { it in text } -> {
            variants = listOf("🍕", "🍕", "🧀"); tint = Color(0xFFFFD8C7)
        }
        listOf("sushi", "japanese", "ramen", "noodle", "udon").any { it in text } -> {
            variants = listOf("🍣", "🍜", "🍱"); tint = Color(0xFFE5E0FF)
        }
        listOf("chinese", "dim sum", "dumpling", "wok").any { it in text } -> {
            variants = listOf("🥟", "🍜", "🥡"); tint = Color(0xFFFFE1CE)
        }
        listOf("indian", "curry", "biryani", "pakistani").any { it in text } -> {
            variants = listOf("🍛", "🍚", "🥘"); tint = Color(0xFFFFE9B8)
        }
        listOf("thai", "vietnam", "pho").any { it in text } -> {
            variants = listOf("🍜", "🥢", "🍲"); tint = Color(0xFFE2F1D8)
        }
        listOf("kebab", "shawarma", "turkish", "grill", "bbq", "steak").any { it in text } -> {
            variants = listOf("🥙", "🍖", "🥩"); tint = Color(0xFFFFD9C8)
        }
        listOf("coffee", "cafe", "café", "espresso").any { it in text } -> {
            variants = listOf("☕", "🥐", "🧁"); tint = Color(0xFFE9DCCB)
        }
        listOf("bakery", "pastry", "bread", "sandwich", "bagel").any { it in text } -> {
            variants = listOf("🥐", "🥪", "🥯"); tint = Color(0xFFFFE8CC)
        }
        listOf("ice cream", "gelato", "dessert", "sweet", "cake", "donut").any { it in text } -> {
            variants = listOf("🍦", "🍰", "🍩"); tint = Color(0xFFF7DDF1)
        }
        listOf("pub", "bar", "beer").any { it in text } -> {
            variants = listOf("🍺", "🍟", "🥨"); tint = Color(0xFFFFE8B4)
        }
        listOf("vegan", "vegetarian", "salad", "healthy").any { it in text } -> {
            variants = listOf("🥗", "🥑", "🥬"); tint = Color(0xFFDDF2D9)
        }
        listOf("chicken", "fried").any { it in text } -> {
            variants = listOf("🍗", "🍟", "🍗"); tint = Color(0xFFFFE2BA)
        }
        listOf("fish", "seafood", "chips").any { it in text } -> {
            variants = listOf("🐟", "🍤", "🍟"); tint = Color(0xFFD9EAF7)
        }
        else -> {
            variants = listOf("🍽️", "🥘", "🍲", "🥪"); tint = Color(0xFFF0E5F4)
        }
    }
    val index = (place.name.hashCode().toLong().and(0x7fffffffL) % variants.size).toInt()
    return variants[index] to tint
}

// Original pastel food drawings made from vector shapes, bundled offline.
private fun DrawScope.drawFoodDish(kind: String) {
    val w = size.width; val h = size.height
    fun ellipse(c: Color, x: Float, y: Float, rw: Float, rh: Float) {
        drawOval(c, Offset(x*w,y*h), Size(rw*w,rh*h))
    }
    fun rect(c: Color, x: Float, y: Float, rw: Float, rh: Float, r: Float=.05f) {
        drawRoundRect(c, Offset(x*w,y*h), Size(rw*w,rh*h), CornerRadius(r*w))
    }
    fun dot(c: Color, x: Float, y: Float, r: Float) {
        drawCircle(c,r*w,Offset(x*w,y*h))
    }
    // Framed miniature illustration with a plate and sparkle.
    ellipse(Color(0x55FFFFFF),.08f,.16f,.84f,.72f)
    ellipse(Color(0xFFEAD6BA),.15f,.65f,.70f,.19f)
    ellipse(Color(0xFFFFFAF2),.20f,.67f,.60f,.13f)
    when(kind) {
        "burger" -> {
            rect(Color(0xFFB56C37),.20f,.60f,.60f,.12f)
            rect(Color(0xFF6A3B30),.20f,.52f,.60f,.11f)
            rect(Color(0xFF78A45D),.18f,.48f,.64f,.07f)
            rect(Color(0xFFFFD568),.24f,.44f,.52f,.08f)
            ellipse(Color(0xFFE9A44D),.19f,.27f,.62f,.23f)
            listOf(.35f,.51f,.66f).forEach { dot(Color(0xFFFFF0BF),it,.34f,.018f) }
        }
        "pizza" -> {
            dot(Color(0xFFE3A14D),.5f,.49f,.31f)
            dot(Color(0xFFC85B48),.5f,.49f,.26f)
            dot(Color(0xFFFFD881),.5f,.49f,.22f)
            listOf(.37f to .42f,.58f to .37f,.62f to .57f,.42f to .60f).forEach { (x,y)->dot(Color(0xFFBA4C50),x,y,.055f) }
            dot(Color(0xFF4C885D),.52f,.52f,.035f)
        }
        "coffee" -> {
            rect(Color(0xFFFFFCF2),.26f,.30f,.44f,.38f,.08f)
            drawArc(Color(0xFFFFFCF2),-80f,260f,false,Offset(.63f*w,.37f*h),Size(.22f*w,.23f*h),style=Stroke(width=w*.07f))
            ellipse(Color(0xFF966343),.28f,.31f,.40f,.11f)
            ellipse(Color(0xFFFFE5B7),.38f,.33f,.20f,.05f)
            rect(Color(0xFFE4BD84),.23f,.68f,.52f,.05f)
        }
        "noodles" -> {
            ellipse(Color(0xFFFFF6E8),.18f,.35f,.64f,.43f)
            ellipse(Color(0xFFAD624D),.23f,.38f,.54f,.26f)
            listOf(.34f,.45f,.57f,.67f).forEach { x->
                drawArc(Color(0xFFFFD57B),180f,180f,false,Offset(x*w,.39f*h),Size(.14f*w,.18f*h),style=Stroke(width=w*.035f))
            }
            dot(Color(0xFF6C9A58),.44f,.45f,.055f)
            rect(Color(0xFF8B5B43),.17f,.23f,.65f,.025f,.01f)
        }
        "curry" -> {
            ellipse(Color(0xFFFDF0DC),.16f,.32f,.68f,.45f)
            ellipse(Color(0xFFD38A3B),.22f,.36f,.56f,.30f)
            listOf(.35f to .47f,.56f to .44f,.62f to .55f).forEach { (x,y)->dot(Color(0xFFF2B857),x,y,.075f) }
            dot(Color(0xFF5E935A),.48f,.49f,.035f)
            ellipse(Color(0xFFF8EBD0),.16f,.69f,.68f,.10f)
        }
        "dessert" -> {
            rect(Color(0xFFE6A1B7),.30f,.44f,.42f,.24f)
            rect(Color(0xFFFFF0D7),.28f,.39f,.46f,.10f)
            rect(Color(0xFFE6A1B7),.30f,.32f,.42f,.09f)
            dot(Color(0xFFCB5368),.50f,.28f,.07f)
            rect(Color(0xFFF8E0C8),.26f,.68f,.50f,.05f)
        }
        "greens" -> {
            ellipse(Color(0xFFF7F0E1),.16f,.35f,.68f,.43f)
            listOf(.33f to .48f,.48f to .42f,.62f to .50f,.45f to .59f).forEach { (x,y)->dot(Color(0xFF78A768),x,y,.12f) }
            dot(Color(0xFFE9B55E),.57f,.47f,.06f)
            dot(Color(0xFFD96D55),.40f,.53f,.06f)
        }
        else -> {
            ellipse(Color(0xFFF7F0E1),.16f,.35f,.68f,.43f)
            ellipse(Color(0xFFE3A36C),.24f,.39f,.52f,.28f)
            dot(Color(0xFF7D9D63),.41f,.46f,.065f)
            dot(Color(0xFFFFD47C),.60f,.52f,.08f)
        }
    }
    dot(Color(0xFFFFE9A4),.84f,.20f,.032f)
    dot(Color(0xFFFFE9A4),.15f,.30f,.018f)
}

private fun foodDrawingKind(place: Restaurant): String {
    val t = (place.category + " " + place.name).lowercase()
    return when {
        listOf("burger","hamburger","five guys","mcdonald","wendy").any { it in t } -> "burger"
        listOf("pizza","pizzeria","italian").any { it in t } -> "pizza"
        listOf("coffee","cafe","café","espresso","tea").any { it in t } -> "coffee"
        listOf("ramen","noodle","sushi","japanese","chinese","thai","vietnam","pho","wok").any { it in t } -> "noodles"
        listOf("indian","curry","biryani","pakistani").any { it in t } -> "curry"
        listOf("dessert","cake","ice cream","gelato","bakery","pastry","donut").any { it in t } -> "dessert"
        listOf("vegan","vegetarian","salad","healthy").any { it in t } -> "greens"
        else -> "general"
    }
}

@Composable private fun FoodIllustration(place: Restaurant, size: Int) {
    val (_, tint) = remember(place.name, place.category) { foodArtwork(place) }
    val kind = remember(place.name, place.category) { foodDrawingKind(place) }
    Box(Modifier.size(size.dp).background(tint, RoundedCornerShape((size / 5).dp))
        .border(1.dp, Color(0xFFE2BD81), RoundedCornerShape((size / 5).dp)),
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(3.dp)) { drawFoodDish(kind) }
    }
}
