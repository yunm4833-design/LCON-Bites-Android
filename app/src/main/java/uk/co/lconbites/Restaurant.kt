package uk.co.lconbites

import android.content.Context
import org.json.JSONObject

data class Restaurant(
    val id: Int,
    val name: String,
    val category: String,
    val address: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val price: String
)

object Places {
    var all: List<Restaurant> = emptyList()
        private set

    fun load(context: Context) {
        val data = context.assets.open("leeds_food.json").bufferedReader().use { it.readText() }
        val array = JSONObject(data).getJSONArray("places")
        all = (0 until array.length()).map { i ->
            val p = array.getJSONObject(i)
            Restaurant(
                i + 1,
                p.getString("name"),
                p.optString("category", "Food").replace("_", " "),
                p.optString("address", "Leeds city centre"),
                "OpenStreetMap listing. Approx. ${p.optInt("distance_m")} m straight-line from Leeds Conservatoire; walking time not verified. Check current hours before visiting.",
                p.getDouble("lat"), p.getDouble("lon"), "—"
            )
        }
    }
}
