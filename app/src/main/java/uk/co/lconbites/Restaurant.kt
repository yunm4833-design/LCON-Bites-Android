package uk.co.lconbites

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
    // Curated starter records. Opening hours and live ratings are deliberately not
    // embedded because they change; the app works fully offline with these records.
    val all = listOf(
        Restaurant(1, "Leeds Conservatoire Café:Bar", "Cafe", "3 Quarry Hill, Leeds LS2 7PD", "On-campus café serving food and drinks. Check campus notices for current service.", 53.7986, -1.5354, "£"),
        Restaurant(2, "Trinity Kitchen", "Food court", "27 Albion St, Leeds LS1 5ER", "A choice of rotating street-food traders under one roof.", 53.7972, -1.5432, "££"),
        Restaurant(3, "The Empire Cafe", "British", "6 Fish St, Leeds LS1 6DB", "Leeds café-bar known for rotisserie cooking and local character.", 53.7958, -1.5401, "££"),
        Restaurant(4, "Dishoom Leeds", "Indian", "68 Vicar Ln, Leeds LS1 7JH", "Bombay-inspired food, breakfast, lunch and dinner.", 53.8000, -1.5414, "££"),
        Restaurant(5, "Moose Coffee", "Brunch", "Bond Court, Wine St, Leeds LS1 2JZ", "Brunch restaurant serving breakfast and daytime dishes.", 53.7972, -1.5454, "££"),
        Restaurant(6, "Flamingos Coffee House", "Cafe", "10 Central Arcade, Leeds LS1 6DX", "Independent city-centre coffee house and community space.", 53.7988, -1.5420, "£"),
        Restaurant(7, "200 Degrees Coffee", "Cafe", "31 Bond St, Leeds LS1 5BQ", "Speciality coffee shop with pastries and a barista school.", 53.7965, -1.5436, "£"),
        Restaurant(8, "Farmhouse", "Brunch", "20-22 Lands Ln, Leeds LS1 6LB", "All-day breakfast and brunch in the city centre.", 53.7979, -1.5426, "££"),
        Restaurant(9, "Laynes", "Cafe", "16 New Station St, Leeds LS1 5DL", "Independent coffee shop and brunch spot near the station.", 53.7955, -1.5457, "££"),
        Restaurant(10, "Olive & Rye", "Cafe", "Queens Arcade, 18-20 Briggate, Leeds LS1 6LF", "Cafe and deli serving breakfast, sandwiches and coffee.", 53.7985, -1.5427, "££"),
        Restaurant(11, "Noodle House", "Chinese", "20 Merrion St, Leeds LS1 6PQ", "Casual Chinese noodle and roast-meat restaurant.", 53.8015, -1.5433, "£"),
        Restaurant(12, "Sweet Saeeda", "Street food", "Leeds Kirkgate Market, Leeds LS2 7HJ", "Market stall serving Tunisian-inspired wraps and hot food.", 53.7961, -1.5359, "£")
    )
}
