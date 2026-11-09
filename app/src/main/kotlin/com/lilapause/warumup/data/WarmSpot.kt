package com.lilapause.warumup.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.serialization.Serializable

enum class AppLanguage(val code: String, val displayName: String) {
  DE("de", "Deutsch"),
  EN("en", "English"),
}

@Serializable
enum class SpotCategory(
  val titleDe: String,
  val titleEn: String,
  val subtitleDe: String,
  val subtitleEn: String,
  val osmRuleDescription: String,
  val colorHex: Int,
  val isIndoor: Boolean,
) {
  CAFE_HOT_DRINKS(
    titleDe = "Café & Bäckerei",
    titleEn = "Café & Bakery",
    subtitleDe = "Kaffee, heißer Kakao, Tee & ofenfrisches Gebäck",
    subtitleEn = "Coffee, hot chocolate, tea & fresh pastries",
    osmRuleDescription = "amenity=cafe / amenity=tea_room / shop=bakery",
    colorHex = 0xFFD97706.toInt(),
    isIndoor = true,
  ),
  THERMAL_BATH_SAUNA(
    titleDe = "Therme, Sauna & Spa",
    titleEn = "Thermal Bath & Sauna",
    subtitleDe = "Wohltuende Saunahitze, heiße Bäder & Dampfbad",
    subtitleEn = "Hot sauna, thermal springs & steam room",
    osmRuleDescription = "leisure=sauna / amenity=public_bath / amenity=spa",
    colorHex = 0xFFEA580C.toInt(),
    isIndoor = true,
  ),
  INDOOR_POOL(
    titleDe = "Hallenbad & Erlebnisbad",
    titleEn = "Indoor Pool & Aquapark",
    subtitleDe = "Beheizte Schwimmhallen, Whirlpools & Erlebnisbäder",
    subtitleEn = "Heated indoor pools, whirlpools & aquatic leisure",
    osmRuleDescription = "leisure=swimming_pool / leisure=water_park",
    colorHex = 0xFF0284C7.toInt(),
    isIndoor = true,
  ),
  FIREPLACE_RESTAURANT(
    titleDe = "Kaminfeuer & Gemütliches Lokal",
    titleEn = "Fireplace Inn & Cozy Eatery",
    subtitleDe = "Gaststube mit Kamin, Ofen & herzhafter Küche",
    subtitleEn = "Rustic tavern or eatery with open fireplace",
    osmRuleDescription = "amenity=restaurant / pub (fireplace=yes)",
    colorHex = 0xFFB45309.toInt(),
    isIndoor = true,
  ),
  CINEMA_THEATER(
    titleDe = "Kino, Theater & Kultur",
    titleEn = "Cinema, Theater & Culture",
    subtitleDe = "Warme Kinosäle, Theaterbühnen & Kulturabende",
    subtitleEn = "Heated movie theaters, stage halls & live shows",
    osmRuleDescription = "amenity=cinema / amenity=theatre",
    colorHex = 0xFF9333EA.toInt(),
    isIndoor = true,
  ),
  MUSEUM_EXHIBITION(
    titleDe = "Museum & Ausstellung",
    titleEn = "Museum & Art Gallery",
    subtitleDe = "Kultur, Kunst & wohlige Wärme an kalten Tagen",
    subtitleEn = "Art, culture & warmth on freezing days",
    osmRuleDescription = "tourism=museum / amenity=arts_centre",
    colorHex = 0xFF7C3AED.toInt(),
    isIndoor = true,
  ),
  LIBRARY_READING(
    titleDe = "Bibliothek & Buchhandlung",
    titleEn = "Library & Bookstore",
    subtitleDe = "Warme Lesesäle, Leseecken & Bücherwelten",
    subtitleEn = "Quiet warm reading halls, bookshops & armchairs",
    osmRuleDescription = "amenity=library / shop=books",
    colorHex = 0xFF4F46E5.toInt(),
    isIndoor = true,
  ),
  INDOOR_SHOPPING_PASSAGE(
    titleDe = "Einkaufspassage & Markthalle",
    titleEn = "Shopping Arcade & Indoor Market",
    subtitleDe = "Geheizte Flaniermeile, Boutiquen & Markthallen",
    subtitleEn = "Heated indoor promenade, malls & covered hall",
    osmRuleDescription = "shop=mall / amenity=marketplace (covered)",
    colorHex = 0xFFDB2777.toInt(),
    isIndoor = true,
  );

  fun title(lang: AppLanguage): String = if (lang == AppLanguage.EN) titleEn else titleDe
  fun subtitle(lang: AppLanguage): String = if (lang == AppLanguage.EN) subtitleEn else subtitleDe
}

@Serializable
data class WarmSpot(
  val id: Long,
  val lat: Double,
  val lon: Double,
  val category: SpotCategory,
  val name: String,
  val description: String? = null,
  val openingHours: String? = null,
  val operator: String? = null,
  val wheelchair: String? = null,
  val fee: String? = null,
  val address: String? = null,
  val website: String? = null,
  val phone: String? = null,
  val hasFireplace: Boolean = false,
  val tags: Map<String, String> = emptyMap(),
) {
  fun distanceMetersTo(otherLat: Double, otherLon: Double): Int {
    val earthRadius = 6371000.0
    val dLat = Math.toRadians(otherLat - lat)
    val dLon = Math.toRadians(otherLon - lon)
    val a =
      sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat)) * cos(Math.toRadians(otherLat)) * sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return (earthRadius * c).roundToInt()
  }

  fun formattedDistanceTo(otherLat: Double, otherLon: Double): String {
    val meters = distanceMetersTo(otherLat, otherLon)
    return if (meters < 1000) {
      "$meters m"
    } else {
      val km = (meters / 100.0).roundToInt() / 10.0
      "$km km"
    }
  }
}

@Serializable
data class BoundingBoxCoords(
  val south: Double,
  val west: Double,
  val north: Double,
  val east: Double,
)

enum class TravelMode(
  val titleDe: String,
  val titleEn: String,
  val googleMapsMode: String,
) {
  WALKING("Zu Fuß", "Walk", "walking"),
  BICYCLING("Fahrrad", "Bike", "bicycling"),
  DRIVING("Auto", "Car", "driving");

  fun title(lang: AppLanguage): String = if (lang == AppLanguage.EN) titleEn else titleDe
}

data class RouteInfo(
  val targetSpots: List<WarmSpot>,
  val originLat: Double,
  val originLon: Double,
  val travelMode: TravelMode,
  val distanceMeters: Int,
  val durationSeconds: Int,
  val polylinePoints: List<Pair<Double, Double>>,
) {
  constructor(
    targetSpot: WarmSpot,
    originLat: Double,
    originLon: Double,
    travelMode: TravelMode,
    distanceMeters: Int,
    durationSeconds: Int,
    polylinePoints: List<Pair<Double, Double>>,
  ) : this(
    targetSpots = listOf(targetSpot),
    originLat = originLat,
    originLon = originLon,
    travelMode = travelMode,
    distanceMeters = distanceMeters,
    durationSeconds = durationSeconds,
    polylinePoints = polylinePoints,
  )

  val targetSpot: WarmSpot
    get() = targetSpots.lastOrNull() ?: WarmSpot(
      id = 0L,
      lat = originLat,
      lon = originLon,
      category = SpotCategory.CAFE_HOT_DRINKS,
      name = "Ziel",
    )

  val stopCount: Int
    get() = targetSpots.size

  val isMultiStop: Boolean
    get() = targetSpots.size > 1

  fun containsSpot(spotId: Long): Boolean = targetSpots.any { it.id == spotId }

  val formattedDistance: String
    get() = if (distanceMeters < 1000) {
      "$distanceMeters m"
    } else {
      val km = (distanceMeters / 100.0).roundToInt() / 10.0
      "$km km"
    }

  fun formattedDuration(lang: AppLanguage = AppLanguage.DE): String {
    val mins = (durationSeconds / 60.0).roundToInt().coerceAtLeast(1)
    val minUnit = if (lang == AppLanguage.EN) "min" else "Min."
    val hrUnit = if (lang == AppLanguage.EN) "hr" else "Std."
    return if (mins < 60) {
      "$mins $minUnit"
    } else {
      val h = mins / 60
      val rem = mins % 60
      "$h $hrUnit $rem $minUnit"
    }
  }
}
