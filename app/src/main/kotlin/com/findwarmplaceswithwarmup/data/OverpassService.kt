package com.findwarmplaceswithwarmup.data

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Collections
import java.util.zip.GZIPInputStream
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class PhotonResponse(
  val features: List<PhotonFeatureItem> = emptyList(),
)

@Serializable
data class PhotonFeatureItem(
  val geometry: PhotonPointGeometry? = null,
  val properties: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
)

@Serializable
data class PhotonPointGeometry(
  val coordinates: List<Double> = emptyList(),
)

@Serializable
data class OverpassResponse(
  val elements: List<OverpassElement> = emptyList(),
)

@Serializable
data class OverpassElement(
  val type: String,
  val id: Long,
  val lat: Double? = null,
  val lon: Double? = null,
  val center: OverpassCenter? = null,
  val nodes: List<Long>? = null,
  val tags: Map<String, String> = emptyMap(),
)

@Serializable
data class OverpassCenter(
  val lat: Double,
  val lon: Double,
)

@Serializable
data class NominatimResult(
  val place_id: Long? = null,
  val osm_id: Long? = null,
  val display_name: String = "",
  val name: String? = null,
  val lat: String = "0.0",
  val lon: String = "0.0",
  val extratags: Map<String, String>? = null,
)

@Serializable
data class OsrmRouteResponse(
  val code: String = "",
  val routes: List<OsrmRoute> = emptyList(),
)

@Serializable
data class OsrmTripResponse(
  val code: String = "",
  val trips: List<OsrmRoute> = emptyList(),
  val waypoints: List<OsrmWaypoint> = emptyList(),
)

@Serializable
data class OsrmWaypoint(
  val waypoint_index: Int = 0,
  val trips_index: Int = 0,
  val location: List<Double> = emptyList(),
)

@Serializable
data class OsrmRoute(
  val distance: Double = 0.0,
  val duration: Double = 0.0,
  val geometry: OsrmGeometry? = null,
)

@Serializable
data class OsrmGeometry(
  val coordinates: List<List<Double>> = emptyList(),
)

class OverpassService {
  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
  }

  suspend fun fetchWarmSpots(
    bbox: BoundingBoxCoords,
    onPartialSpots: ((List<WarmSpot>) -> Unit)? = null,
  ): Result<List<WarmSpot>> = withContext(Dispatchers.IO) {
    val combinedSpots = Collections.synchronizedList(mutableListOf<WarmSpot>())
    var anySuccess = false
    var lastError: Throwable? = null

    fun emitProgress() {
      if (onPartialSpots != null && combinedSpots.isNotEmpty()) {
        val snapshot = synchronized(combinedSpots) { combinedSpots.toList() }
        val deduped = deduplicateSpots(snapshot)
        if (deduped.isNotEmpty()) {
          onPartialSpots(deduped)
        }
      }
    }

    supervisorScope {
      // 1. High-speed Photon POI Search (Komoot OpenStreetMap index)
      val photonDeferred = async {
        runCatching {
          fetchFromPhotonMultiSource(bbox) { batch ->
            if (batch.isNotEmpty()) {
              anySuccess = true
              combinedSpots.addAll(batch)
              emitProgress()
            }
          }
        }
      }

      // 2. Official OpenStreetMap API v0.6 tiled (fast & direct raw nodes)
      val osmMapDeferred = async {
        runCatching {
          fetchFromOfficialOsmMapApiTiled(bbox) { tileSpots ->
            if (tileSpots.isNotEmpty()) {
              anySuccess = true
              combinedSpots.addAll(tileSpots)
              emitProgress()
            }
          }
        }
      }

      val photonRes = photonDeferred.await()
      photonRes.onSuccess { spots ->
        if (spots.isNotEmpty()) anySuccess = true
        combinedSpots.addAll(spots)
        emitProgress()
      }.onFailure { lastError = it }

      val osmRes = osmMapDeferred.await()
      osmRes.onSuccess { spots ->
        if (spots.isNotEmpty()) anySuccess = true
        combinedSpots.addAll(spots)
        emitProgress()
      }.onFailure { if (lastError == null) lastError = it }

      // 3. Fallback to Overpass servers if fewer than 25 spots found
      val currentCount = synchronized(combinedSpots) { combinedSpots.size }
      if (currentCount < 25) {
        val overpassDeferred = async {
          runCatching { fetchFromOverpassServers(bbox) }
        }
        val expandedDeferred = async {
          runCatching { fetchExpandedRadiusFallback(bbox) }
        }

        overpassDeferred.await().onSuccess { spots ->
          if (spots.isNotEmpty()) {
            anySuccess = true
            combinedSpots.addAll(spots)
            emitProgress()
          }
        }.onFailure { if (lastError == null) lastError = it }

        expandedDeferred.await().onSuccess { spots ->
          if (spots.isNotEmpty()) {
            anySuccess = true
            combinedSpots.addAll(spots)
            emitProgress()
          }
        }.onFailure { if (lastError == null) lastError = it }
      }
    }

    val snapshot = synchronized(combinedSpots) { combinedSpots.toList() }
    val deduplicated = deduplicateSpots(snapshot)

    if (deduplicated.isNotEmpty() || anySuccess) {
      Result.success(deduplicated)
    } else {
      Result.failure(lastError ?: RuntimeException("No places found"))
    }
  }

  private suspend fun fetchFromOfficialOsmMapApiTiled(
    bbox: BoundingBoxCoords,
    onTileLoaded: (List<WarmSpot>) -> Unit = {},
  ): List<WarmSpot> = coroutineScope {
    val centerLat = (bbox.south + bbox.north) / 2.0
    val centerLon = (bbox.west + bbox.east) / 2.0

    val tileMaxSpan = 0.0075
    val totalLatSpan = (bbox.north - bbox.south).coerceIn(0.005, 0.016)
    val totalLonSpan = (bbox.east - bbox.west).coerceIn(0.005, 0.018)

    val tiles = mutableListOf<BoundingBoxCoords>()
    val cLatHalf = (totalLatSpan.coerceAtMost(tileMaxSpan)) / 2.0
    val cLonHalf = (totalLonSpan.coerceAtMost(tileMaxSpan)) / 2.0
    tiles.add(
      BoundingBoxCoords(
        south = centerLat - cLatHalf,
        west = centerLon - cLonHalf,
        north = centerLat + cLatHalf,
        east = centerLon + cLonHalf,
      )
    )

    if (totalLatSpan > tileMaxSpan * 1.2 || totalLonSpan > tileMaxSpan * 1.2) {
      val qLatHalf = (totalLatSpan / 2.0).coerceAtMost(tileMaxSpan)
      val qLonHalf = (totalLonSpan / 2.0).coerceAtMost(tileMaxSpan)
      tiles.add(BoundingBoxCoords(centerLat, centerLon - qLonHalf, centerLat + qLatHalf, centerLon))
      tiles.add(BoundingBoxCoords(centerLat, centerLon, centerLat + qLatHalf, centerLon + qLonHalf))
      tiles.add(BoundingBoxCoords(centerLat - qLatHalf, centerLon - qLonHalf, centerLat, centerLon))
      tiles.add(BoundingBoxCoords(centerLat - qLatHalf, centerLon, centerLat, centerLon + qLonHalf))
    }

    val allTileSpots = Collections.synchronizedList(mutableListOf<WarmSpot>())
    val sem = Semaphore(3)

    tiles.map { tile ->
      async {
        sem.withPermit {
          val spots = runCatching {
            fetchSingleOsmMapTile(tile)
          }.getOrDefault(emptyList())
          if (spots.isNotEmpty()) {
            allTileSpots.addAll(spots)
            onTileLoaded(spots)
          }
          spots
        }
      }
    }.awaitAll()

    allTileSpots.toList()
  }

  private fun fetchSingleOsmMapTile(tile: BoundingBoxCoords): List<WarmSpot> {
    val urlStr =
      "https://api.openstreetmap.org/api/0.6/map.json?bbox=${tile.west},${tile.south},${tile.east},${tile.north}"
    val body = executeGetWithGzip(urlStr, connectTimeoutMs = 20000, readTimeoutMs = 45000)
    val parsed = json.decodeFromString<OverpassResponse>(body)

    val nodeCoords = HashMap<Long, Pair<Double, Double>>(parsed.elements.size)
    for (el in parsed.elements) {
      if (el.type == "node" && el.lat != null && el.lon != null) {
        nodeCoords[el.id] = el.lat to el.lon
      }
    }

    val spots = mutableListOf<WarmSpot>()
    for (el in parsed.elements) {
      if (el.tags.isEmpty()) continue
      val resolvedEl = if (el.lat == null && el.center == null && !el.nodes.isNullOrEmpty()) {
        var sumLat = 0.0
        var sumLon = 0.0
        var count = 0
        for (nid in el.nodes) {
          val pt = nodeCoords[nid]
          if (pt != null) {
            sumLat += pt.first
            sumLon += pt.second
            count++
          }
        }
        if (count > 0) {
          el.copy(center = OverpassCenter(sumLat / count, sumLon / count))
        } else {
          el
        }
      } else {
        el
      }

      val spot = parseOverpassElement(resolvedEl) ?: continue
      spots.add(spot)
    }
    return spots
  }

  private data class PhotonTask(
    val url: String,
    val fallbackCategory: SpotCategory,
  )

  private suspend fun fetchFromPhotonMultiSource(
    bbox: BoundingBoxCoords,
    onBatchLoaded: (List<WarmSpot>) -> Unit = {},
  ): List<WarmSpot> = coroutineScope {
    val centerLat = (bbox.south + bbox.north) / 2.0
    val centerLon = (bbox.west + bbox.east) / 2.0
    val latSpan = (bbox.north - bbox.south).coerceAtLeast(0.010)
    val lonSpan = (bbox.east - bbox.west).coerceAtLeast(0.012)

    val minLat = bbox.south - latSpan * 0.15
    val maxLat = bbox.north + latSpan * 0.15
    val minLon = bbox.west - lonSpan * 0.15
    val maxLon = bbox.east + lonSpan * 0.15
    val photonBbox = "$minLon,$minLat,$maxLon,$maxLat"

    val latKm = latSpan * 111.0
    val lonKm = lonSpan * 111.0 * cos(Math.toRadians(centerLat)).coerceAtLeast(0.2)
    val radiusKm = max(latKm, lonKm).coerceIn(3.0, 30.0).roundToInt()
    val subRadiusKm = (radiusKm * 0.65).coerceIn(2.0, 20.0).roundToInt()

    // Tiling: Center + 4 quadrant points to cover entire viewport without gaps
    val qLat = latSpan * 0.30
    val qLon = lonSpan * 0.30
    val tileCenters = listOf(
      centerLat to centerLon,
      (centerLat + qLat) to (centerLon - qLon), // North-West
      (centerLat + qLat) to (centerLon + qLon), // North-East
      (centerLat - qLat) to (centerLon - qLon), // South-West
      (centerLat - qLat) to (centerLon + qLon), // South-East
    )

    val tasks = mutableListOf<PhotonTask>()

    // 1. CAFES & TEA HOUSES & BAKERIES WITH CAFE (Multi-point reverse tiling)
    tileCenters.forEach { (tLat, tLon) ->
      tasks.add(
        PhotonTask(
          url = "https://photon.komoot.io/reverse?lon=$tLon&lat=$tLat&radius=$subRadiusKm&limit=60" +
            "&osm_tag=amenity:cafe&osm_tag=amenity:tea_room&osm_tag=shop:coffee&osm_tag=shop:tea&osm_tag=shop:bakery&osm_tag=amenity:bistro",
          fallbackCategory = SpotCategory.CAFE_HOT_DRINKS,
        )
      )
    }
    listOf("Cafe", "Kaffee", "Teehaus", "Kaffeerösterei", "Bäckerei", "Konditorei", "Bistro", "Espresso").forEach { kw ->
      val enc = URLEncoder.encode(kw, "UTF-8")
      tasks.add(
        PhotonTask(
          url = "https://photon.komoot.io/api/?q=$enc&bbox=$photonBbox&limit=40&lang=de",
          fallbackCategory = SpotCategory.CAFE_HOT_DRINKS,
        )
      )
    }

    // 2. THERMAL BATHS & SAUNAS (Multi-point reverse tiling)
    tileCenters.take(3).forEach { (tLat, tLon) ->
      tasks.add(
        PhotonTask(
          url = "https://photon.komoot.io/reverse?lon=$tLon&lat=$tLat&radius=$radiusKm&limit=50" +
            "&osm_tag=leisure:sauna&osm_tag=amenity:public_bath&osm_tag=leisure:water_park&osm_tag=amenity:spa",
          fallbackCategory = SpotCategory.THERMAL_BATH_SAUNA,
        )
      )
    }
    listOf("Therme", "Sauna", "Thermalbad", "Dampfbad", "Wellness", "Spa").forEach { kw ->
      val enc = URLEncoder.encode(kw, "UTF-8")
      tasks.add(
        PhotonTask(
          url = "https://photon.komoot.io/api/?q=$enc&bbox=$photonBbox&limit=30&lang=de",
          fallbackCategory = SpotCategory.THERMAL_BATH_SAUNA,
        )
      )
    }

    // 3. INDOOR POOL (real indoor pools only, strict osm_tag to avoid bus stops / streets)
    tileCenters.take(3).forEach { (tLat, tLon) ->
      tasks.add(
        PhotonTask(
          url = "https://photon.komoot.io/reverse?lon=$tLon&lat=$tLat&radius=$radiusKm&limit=30" +
            "&osm_tag=leisure:swimming_pool&osm_tag=leisure:water_park&osm_tag=amenity:public_bath",
          fallbackCategory = SpotCategory.INDOOR_POOL,
        )
      )
    }
    listOf("Hallenbad", "Schwimmhalle", "Erlebnisbad", "Freizeitbad").forEach { kw ->
      val enc = URLEncoder.encode(kw, "UTF-8")
      tasks.add(
        PhotonTask(
          url = "https://photon.komoot.io/api/?q=$enc&bbox=$photonBbox&limit=25&lang=de" +
            "&osm_tag=leisure:swimming_pool&osm_tag=leisure:water_park&osm_tag=amenity:public_bath",
          fallbackCategory = SpotCategory.INDOOR_POOL,
        )
      )
    }

    // 4. CHRISTMAS MARKETS
    listOf("Weihnachtsmarkt", "Christkindlmarkt", "Wintermarkt", "Adventmarkt", "Glühwein").forEach { kw ->
      val enc = URLEncoder.encode(kw, "UTF-8")
      tasks.add(
        PhotonTask(
          url = "https://photon.komoot.io/api/?q=$enc&bbox=$photonBbox&limit=30&lang=de",
          fallbackCategory = SpotCategory.CHRISTMAS_MARKET,
        )
      )
    }

    // 5. FIREPLACE / COZY RESTAURANTS
    tileCenters.take(3).forEach { (tLat, tLon) ->
      tasks.add(
        PhotonTask(
          url = "https://photon.komoot.io/reverse?lon=$tLon&lat=$tLat&radius=$subRadiusKm&limit=40" +
            "&osm_tag=amenity:restaurant&osm_tag=amenity:pub&osm_tag=tourism:alpine_hut",
          fallbackCategory = SpotCategory.FIREPLACE_RESTAURANT,
        )
      )
    }
    listOf("Kamin", "Almhütte", "Gasthaus", "Wirtshaus").forEach { kw ->
      val enc = URLEncoder.encode(kw, "UTF-8")
      tasks.add(
        PhotonTask(
          url = "https://photon.komoot.io/api/?q=$enc&bbox=$photonBbox&limit=30&lang=de" +
            "&osm_tag=amenity:restaurant&osm_tag=amenity:pub&osm_tag=tourism:alpine_hut",
          fallbackCategory = SpotCategory.FIREPLACE_RESTAURANT,
        )
      )
    }

    // 6. LIBRARIES & BOOKSHOPS
    tasks.add(
      PhotonTask(
        url = "https://photon.komoot.io/reverse?lon=$centerLon&lat=$centerLat&radius=$radiusKm&limit=40" +
          "&osm_tag=amenity:library&osm_tag=shop:books",
        fallbackCategory = SpotCategory.LIBRARY_READING,
      )
    )
    listOf("Bibliothek", "Bücherei", "Buchhandlung", "Stadtbibliothek", "Thalia", "Hugendubel").forEach { kw ->
      val enc = URLEncoder.encode(kw, "UTF-8")
      tasks.add(
        PhotonTask(
          url = "https://photon.komoot.io/api/?q=$enc&bbox=$photonBbox&limit=30&lang=de&osm_tag=amenity:library&osm_tag=shop:books",
          fallbackCategory = SpotCategory.LIBRARY_READING,
        )
      )
    }

    // 7. MUSEUMS & EXHIBITIONS
    tasks.add(
      PhotonTask(
        url = "https://photon.komoot.io/reverse?lon=$centerLon&lat=$centerLat&radius=$radiusKm&limit=50" +
          "&osm_tag=tourism:museum&osm_tag=amenity:arts_centre&osm_tag=tourism:gallery",
        fallbackCategory = SpotCategory.MUSEUM_EXHIBITION,
      )
    )
    listOf("Museum", "Kunsthalle", "Galerie", "Ausstellung").forEach { kw ->
      val enc = URLEncoder.encode(kw, "UTF-8")
      tasks.add(
        PhotonTask(
          url = "https://photon.komoot.io/api/?q=$enc&bbox=$photonBbox&limit=30&lang=de&osm_tag=tourism:museum",
          fallbackCategory = SpotCategory.MUSEUM_EXHIBITION,
        )
      )
    }

    // 8. CINEMA & THEATER (Warm cultural retreats)
    tileCenters.take(3).forEach { (tLat, tLon) ->
      tasks.add(
        PhotonTask(
          url = "https://photon.komoot.io/reverse?lon=$tLon&lat=$tLat&radius=$radiusKm&limit=30" +
            "&osm_tag=amenity:cinema&osm_tag=amenity:theatre",
          fallbackCategory = SpotCategory.CINEMA_THEATER,
        )
      )
    }
    listOf("Kino", "Theater", "Filmpalast", "Schauspielhaus").forEach { kw ->
      val enc = URLEncoder.encode(kw, "UTF-8")
      tasks.add(
        PhotonTask(
          url = "https://photon.komoot.io/api/?q=$enc&bbox=$photonBbox&limit=25&lang=de" +
            "&osm_tag=amenity:cinema&osm_tag=amenity:theatre",
          fallbackCategory = SpotCategory.CINEMA_THEATER,
        )
      )
    }

    // 9. SHOPPING MALLS / PASSAGES
    tasks.add(
      PhotonTask(
        url = "https://photon.komoot.io/reverse?lon=$centerLon&lat=$centerLat&radius=$radiusKm&limit=30" +
          "&osm_tag=shop:mall",
        fallbackCategory = SpotCategory.INDOOR_SHOPPING_PASSAGE,
      )
    )
    listOf("Einkaufscenter", "Passage", "Mall", "Galerie", "Arkaden").forEach { kw ->
      val enc = URLEncoder.encode(kw, "UTF-8")
      tasks.add(
        PhotonTask(
          url = "https://photon.komoot.io/api/?q=$enc&bbox=$photonBbox&limit=25&lang=de&osm_tag=shop:mall",
          fallbackCategory = SpotCategory.INDOOR_SHOPPING_PASSAGE,
        )
      )
    }

    // 10. ICE RINKS
    listOf("Eislaufbahn", "Eissporthalle", "Schlittschuh", "Eishalle").forEach { kw ->
      val enc = URLEncoder.encode(kw, "UTF-8")
      tasks.add(
        PhotonTask(
          url = "https://photon.komoot.io/api/?q=$enc&bbox=$photonBbox&limit=25&lang=de&osm_tag=leisure:ice_rink",
          fallbackCategory = SpotCategory.ICE_RINK,
        )
      )
    }

    // 11. INDOOR PLAYGROUND / CLIMBING / BOWLING
    listOf("Indoorspielplatz", "Kletterhalle", "Boulderhalle", "Bowling").forEach { kw ->
      val enc = URLEncoder.encode(kw, "UTF-8")
      tasks.add(
        PhotonTask(
          url = "https://photon.komoot.io/api/?q=$enc&bbox=$photonBbox&limit=25&lang=de" +
            "&osm_tag=leisure:indoor_play&osm_tag=sport:climbing&osm_tag=leisure:bowling_alley",
          fallbackCategory = SpotCategory.INDOOR_PLAYGROUND,
        )
      )
    }

    // 12. CHURCHES
    tasks.add(
      PhotonTask(
        url = "https://photon.komoot.io/reverse?lon=$centerLon&lat=$centerLat&radius=$radiusKm&limit=30" +
          "&osm_tag=amenity:place_of_worship",
        fallbackCategory = SpotCategory.WARM_CHURCH,
      )
    )

    // 13. WARMING SHELTERS
    listOf("Wärmestube", "Warteraum").forEach { kw ->
      val enc = URLEncoder.encode(kw, "UTF-8")
      tasks.add(
        PhotonTask(
          url = "https://photon.komoot.io/api/?q=$enc&bbox=$photonBbox&limit=20&lang=de",
          fallbackCategory = SpotCategory.WARMING_SHELTER,
        )
      )
    }

    val semaphore = Semaphore(5)
    val allSpots = Collections.synchronizedList(mutableListOf<WarmSpot>())

    tasks.map { task ->
      async {
        semaphore.withPermit {
          val batch = runCatching {
            fetchPhotonUrl(task.url, task.fallbackCategory)
          }.getOrDefault(emptyList())
          if (batch.isNotEmpty()) {
            allSpots.addAll(batch)
            onBatchLoaded(batch)
          }
        }
      }
    }.awaitAll()

    allSpots.toList()
  }

  private fun fetchPhotonUrl(urlStr: String, fallbackCat: SpotCategory): List<WarmSpot> {
    val body = executeGetWithGzip(urlStr, connectTimeoutMs = 15000, readTimeoutMs = 30000)
    val response = json.decodeFromString<PhotonResponse>(body)

    return response.features.mapNotNull { feat ->
      val coords = feat.geometry?.coordinates ?: return@mapNotNull null
      if (coords.size < 2) return@mapNotNull null
      val lon = coords[0]
      val lat = coords[1]

      val rawName = feat.properties["name"]?.jsonPrimitive?.contentOrNull?.trim()
      // Real venues MUST have an explicit, non-empty name (never fallback to street name or category)
      if (rawName.isNullOrBlank()) return@mapNotNull null

      val osmKey = feat.properties["osm_key"]?.jsonPrimitive?.contentOrNull.orEmpty()
      val osmValue = feat.properties["osm_value"]?.jsonPrimitive?.contentOrNull.orEmpty()

      // Reject non-venues: bus stops, railway platforms, residential streets, boundaries, landuses
      val rejectedKeys = setOf("highway", "place", "boundary", "landuse", "natural", "railway", "waterway", "barrier", "power")
      val rejectedValues = setOf(
        "bus_stop", "stop_position", "platform", "station", "halt", "residential", "track",
        "footway", "cycleway", "path", "service", "motorway", "trunk", "primary", "secondary",
        "tertiary", "unclassified", "suburb", "quarter", "neighbourhood", "village", "town", "city"
      )
      if (osmKey in rejectedKeys || osmValue in rejectedValues) {
        return@mapNotNull null
      }

      // Reject street names disguised as venues (e.g. "Hallenbadweg", "Badstraße", "Poststraße") unless tagged as actual POI
      if (osmKey !in setOf("amenity", "leisure", "shop", "tourism", "craft", "historic")) {
        val lower = rawName.lowercase()
        if (lower.endsWith("straße") || lower.endsWith("str.") || lower.endsWith("weg") ||
            lower.endsWith("gasse") || lower.endsWith("platz") || lower.endsWith("allee") ||
            lower.endsWith("damm") || lower.contains("haltestelle") || lower.contains("bushaltestelle")) {
          return@mapNotNull null
        }
      }

      // Special validation for indoor pools: must NOT be an outdoor-only freibad/lake/summer bath
      if (fallbackCat == SpotCategory.INDOOR_POOL) {
        val lower = rawName.lowercase()
        if (lower.contains("freibad") || lower.contains("strandbad") || lower.contains("badesee") || lower.contains("naturbad")) {
          if (!lower.contains("hallenbad") && !lower.contains("therme") && !lower.contains("erlebnisbad")) {
            return@mapNotNull null
          }
        }
        if (osmKey == "leisure" && osmValue !in listOf("swimming_pool", "water_park", "sports_centre") && osmKey != "amenity") {
          return@mapNotNull null
        }
      }

      val osmId = feat.properties["osm_id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
        ?: ((lat.hashCode() * 31) + lon.hashCode()).toLong()

      val street = feat.properties["street"]?.jsonPrimitive?.contentOrNull
      val housenumber = feat.properties["housenumber"]?.jsonPrimitive?.contentOrNull
      val city = feat.properties["city"]?.jsonPrimitive?.contentOrNull
      val streetLine = if (street != null && housenumber != null) "$street $housenumber" else street
      val address = listOfNotNull(streetLine, city).joinToString(", ").ifBlank { null }

      WarmSpot(
        id = osmId,
        lat = lat,
        lon = lon,
        category = fallbackCat,
        name = rawName,
        address = address,
        hasFireplace = fallbackCat == SpotCategory.FIREPLACE_RESTAURANT,
      )
    }
  }

  private suspend fun fetchExpandedRadiusFallback(bbox: BoundingBoxCoords): List<WarmSpot> {
    val centerLat = (bbox.south + bbox.north) / 2.0
    val centerLon = (bbox.west + bbox.east) / 2.0

    return listOf(
      "cafe" to SpotCategory.CAFE_HOT_DRINKS,
      "sauna" to SpotCategory.THERMAL_BATH_SAUNA,
      "museum" to SpotCategory.MUSEUM_EXHIBITION,
      "weihnachtsmarkt" to SpotCategory.CHRISTMAS_MARKET,
      "bibliothek" to SpotCategory.LIBRARY_READING,
    ).flatMap { (q, cat) ->
      try {
        val url = "https://photon.komoot.io/api/?lat=$centerLat&lon=$centerLon&q=$q&limit=25"
        fetchPhotonUrl(url, cat)
      } catch (_: Exception) {
        emptyList()
      }
    }
  }

  private fun fetchFromOverpassServers(bbox: BoundingBoxCoords): List<WarmSpot> {
    val servers = listOf(
      "https://overpass-api.de/api/interpreter",
      "https://overpass.kumi.systems/api/interpreter",
      "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
    )

    val query = buildOverpassQuery(bbox)
    for (server in servers) {
      try {
        val spots = executeOverpassQuery(server, query)
        if (spots.isNotEmpty()) return spots
      } catch (_: Exception) {
      }
    }
    return emptyList()
  }

  private fun buildOverpassQuery(bbox: BoundingBoxCoords): String {
    val s = bbox.south
    val w = bbox.west
    val n = bbox.north
    val e = bbox.east
    val box = "$s,$w,$n,$e"

    return """
      [out:json][timeout:25];
      (
        node["amenity"="cafe"]($box);
        way["amenity"="cafe"]($box);
        node["leisure"="sauna"]($box);
        way["leisure"="sauna"]($box);
        node["amenity"="public_bath"]($box);
        way["amenity"="public_bath"]($box);
        node["leisure"="swimming_pool"]($box);
        way["leisure"="swimming_pool"]($box);
        node["amenity"="library"]($box);
        way["amenity"="library"]($box);
        node["tourism"="museum"]($box);
        way["tourism"="museum"]($box);
        node["amenity"="cinema"]($box);
        way["amenity"="cinema"]($box);
        node["amenity"="theatre"]($box);
        way["amenity"="theatre"]($box);
        node["shop"="books"]($box);
        way["shop"="books"]($box);
        node["shop"="mall"]($box);
        way["shop"="mall"]($box);
        node["leisure"="ice_rink"]($box);
        way["leisure"="ice_rink"]($box);
        node["leisure"="indoor_play"]($box);
        way["leisure"="indoor_play"]($box);
        node["amenity"="place_of_worship"]($box);
        way["amenity"="place_of_worship"]($box);
      );
      out center 150;
    """.trimIndent()
  }

  private fun executeOverpassQuery(serverUrl: String, qlQuery: String): List<WarmSpot> {
    val formBody = "data=" + URLEncoder.encode(qlQuery, "UTF-8")
    val body = executePost(serverUrl, formBody)
    val response = json.decodeFromString<OverpassResponse>(body)
    return response.elements.mapNotNull { parseOverpassElement(it) }
  }

  private fun executeGetWithGzip(
    urlStr: String,
    connectTimeoutMs: Int = 20000,
    readTimeoutMs: Int = 45000,
  ): String {
    val url = URL(urlStr)
    val conn = (url.openConnection() as HttpURLConnection).apply {
      requestMethod = "GET"
      connectTimeout = connectTimeoutMs
      readTimeout = readTimeoutMs
      setRequestProperty("User-Agent", "WarmUp-App/1.0 (warmup@app.de)")
      setRequestProperty("Accept", "application/json")
      setRequestProperty("Accept-Encoding", "gzip")
    }
    val code = conn.responseCode
    if (code !in 200..299) {
      throw RuntimeException("HTTP $code on ${url.host}")
    }
    val stream = if ("gzip".equals(conn.contentEncoding, ignoreCase = true)) {
      GZIPInputStream(conn.inputStream)
    } else {
      conn.inputStream
    }
    return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
  }

  private fun executePost(endpointUrl: String, formBody: String): String {
    val url = URL(endpointUrl)
    val conn = (url.openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      doOutput = true
      connectTimeout = 20000
      readTimeout = 45000
      setRequestProperty("User-Agent", "WarmUp-App/1.0 (warmup@app.de)")
      setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
      setRequestProperty("Accept-Encoding", "gzip")
    }
    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
      writer.write(formBody)
      writer.flush()
    }
    val responseCode = conn.responseCode
    if (responseCode !in 200..299) {
      throw RuntimeException("Overpass HTTP $responseCode")
    }
    val stream = if ("gzip".equals(conn.contentEncoding, ignoreCase = true)) {
      GZIPInputStream(conn.inputStream)
    } else {
      conn.inputStream
    }
    return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
  }

  private fun deduplicateSpots(spots: List<WarmSpot>): List<WarmSpot> {
    val result = mutableListOf<WarmSpot>()
    for (spot in spots) {
      val duplicate = result.any { existing ->
        (existing.id == spot.id && existing.category == spot.category) ||
          (existing.category == spot.category && existing.distanceMetersTo(spot.lat, spot.lon) < 15)
      }
      if (!duplicate) {
        result.add(spot)
      }
    }
    return result
  }

  fun parseOverpassElement(el: OverpassElement): WarmSpot? {
    val lat = el.lat ?: el.center?.lat ?: return null
    val lon = el.lon ?: el.center?.lon ?: return null
    val tags = el.tags
    if (tags.isEmpty()) return null

    val category = classifyTags(tags) ?: return null
    val rawName = tags["name:de"] ?: tags["name:en"] ?: tags["name"] ?: tags["brand"] ?: defaultNameForCategory(category, tags)
    val hasFireplace = tags["fireplace"] == "yes" || tags["stove"] == "yes"

    return WarmSpot(
      id = el.id,
      lat = lat,
      lon = lon,
      category = category,
      name = rawName,
      description = buildSpotDescription(category, tags),
      openingHours = tags["opening_hours"],
      operator = tags["operator"],
      wheelchair = tags["wheelchair"],
      fee = tags["fee"],
      address = buildAddress(tags),
      website = tags["website"] ?: tags["contact:website"],
      phone = tags["phone"] ?: tags["contact:phone"],
      hasFireplace = hasFireplace,
      tags = tags,
    )
  }

  private fun classifyTags(tags: Map<String, String>): SpotCategory? {
    val amenity = tags["amenity"]
    val leisure = tags["leisure"]
    val tourism = tags["tourism"]
    val shop = tags["shop"]
    val craft = tags["craft"]
    val building = tags["building"]
    val sport = tags["sport"]
    val marketType = tags["market:type"]
    val temporary = tags["temporary"]
    val name = (tags["name"] ?: "").lowercase()

    if (leisure == "sauna" || amenity == "public_bath" || amenity == "spa" || tags["sauna"] == "yes" || tags["spa"] == "yes" ||
        tags["baths:type"]?.contains("thermal") == true || name.contains("sauna") || name.contains("therme") ||
        name.contains("thermalbad") || name.contains("dampfbad") || name.contains("hamam") || name.contains("wellness")) {
      return SpotCategory.THERMAL_BATH_SAUNA
    }

    if (marketType == "christmas" || temporary == "christmas_market" || tags["xmas:feature"] == "market" ||
        name.contains("weihnachtsmarkt") || name.contains("christkindlmarkt") || name.contains("wintermarkt") ||
        name.contains("adventmarkt") || name.contains("glühwein")) {
      return SpotCategory.CHRISTMAS_MARKET
    }

    val isFreibad = (tags["outdoor"] == "yes" || name.contains("freibad") || name.contains("strandbad") || name.contains("badesee")) &&
      tags["indoor"] != "yes" && tags["covered"] != "yes" && !name.contains("hallenbad") && !name.contains("therme") && !name.contains("erlebnisbad")
    if (isFreibad) return null

    if (leisure == "water_park" || (leisure == "swimming_pool" && (tags["indoor"] == "yes" || tags["covered"] == "yes" || !tags.containsKey("outdoor"))) ||
        (leisure == "sports_centre" && sport == "swimming") ||
        name.contains("hallenbad") || name.contains("hallen-bad") || name.contains("wellnessbad") || name.contains("thermenwelt") ||
        name.contains("schwimmhalle") || name.contains("erlebnisbad") || name.contains("freizeitbad")) {
      return SpotCategory.INDOOR_POOL
    }

    if (tags["fireplace"] == "yes" || tags["stove"] == "yes" || tourism == "alpine_hut" || name.contains("kamin") ||
        name.contains("ofen") || name.contains("almhütte") ||
        (amenity in listOf("restaurant", "pub", "bar") && (tags["fireplace"] == "yes" || name.contains("kamin")))) {
      return SpotCategory.FIREPLACE_RESTAURANT
    }

    val isBakeryCafe = (shop == "bakery" || craft == "bakery" || shop == "pastry" || craft == "confectionery") &&
      (tags["cafe"] == "yes" || tags["coffee"] == "yes" || tags["seating"] == "yes" || tags["indoor_seating"] == "yes" ||
        name.contains("café") || name.contains("cafe") || name.contains("konditorei") || name.contains("backstube") ||
        name.contains("bäckerei") || name.contains("croissant") || name.contains("patisserie") || name.contains("espresso"))

    if (amenity == "cafe" || amenity == "tea_room" || amenity == "bistro" || amenity == "ice_cream" ||
        shop == "coffee" || shop == "tea" || isBakeryCafe ||
        name.contains("café") || name.contains("cafe") || name.contains("teehaus") || name.contains("rösterei") ||
        name.contains("chocolat") || name.contains("schokolade") || name.contains("kaffeehaus") || name.contains("espresso")) {
      return SpotCategory.CAFE_HOT_DRINKS
    }

    if (amenity == "cinema" || amenity == "theatre" || name.contains("kino") || name.contains("lichtspiel") ||
        name.contains("filmpalast") || name.contains("cinestar") || name.contains("cinemaxx") || name.contains("theater") ||
        name.contains("schauspielhaus") || name.contains("oper")) {
      return SpotCategory.CINEMA_THEATER
    }

    if (leisure == "ice_rink" || name.contains("eislauf") || name.contains("schlittschuh") || name.contains("eissporthalle") || name.contains("eishalle")) {
      return SpotCategory.ICE_RINK
    }

    if (leisure == "indoor_play" || tags["playground"] == "indoor" || sport == "climbing" || leisure == "bowling_alley" ||
        sport == "bowling" || sport == "billiards" || name.contains("indoorspielplatz") || name.contains("kletterhalle") ||
        name.contains("boulderhalle") || name.contains("bowling") || name.contains("billard")) {
      return SpotCategory.INDOOR_PLAYGROUND
    }

    if (amenity == "library" || shop == "books" || name.contains("bibliothek") || name.contains("bücherei") ||
        name.contains("buchhandlung") || name.contains("thalia") || name.contains("hugendubel") || name.contains("lesesaal")) {
      return SpotCategory.LIBRARY_READING
    }

    if (tourism == "museum" || tourism == "gallery" || amenity == "arts_centre" || building == "greenhouse" ||
        name.contains("museum") || name.contains("galerie") || name.contains("kunsthalle") || name.contains("ausstellung") ||
        name.contains("tropenhaus") || name.contains("palmenhaus") || name.contains("botanischer garten")) {
      return SpotCategory.MUSEUM_EXHIBITION
    }

    if (shop == "mall" || (amenity == "marketplace" && (tags["covered"] == "yes" || tags["indoor"] == "yes")) ||
        name.contains("passage") || name.contains("mall") || name.contains("arkaden") || name.contains("center") || name.contains("markthalle")) {
      return SpotCategory.INDOOR_SHOPPING_PASSAGE
    }

    if (amenity == "waiting_room" || (amenity == "shelter" && (tags["heating"] == "yes" || tags["fireplace"] == "yes")) ||
        amenity == "social_facility" || name.contains("wärmestube") || name.contains("warteraum") || name.contains("kältebus")) {
      return SpotCategory.WARMING_SHELTER
    }

    if (amenity == "place_of_worship" || building == "church" || building == "cathedral") {
      return SpotCategory.WARM_CHURCH
    }

    return null
  }

  private fun defaultNameForCategory(category: SpotCategory, tags: Map<String, String>): String = when (category) {
    SpotCategory.CAFE_HOT_DRINKS -> "Café / Bäckerei"
    SpotCategory.THERMAL_BATH_SAUNA -> "Therme / Sauna"
    SpotCategory.INDOOR_POOL -> "Hallenbad & Erlebnisbad"
    SpotCategory.CHRISTMAS_MARKET -> "Weihnachtsmarkt"
    SpotCategory.FIREPLACE_RESTAURANT -> "Lokal mit Kaminfeuer"
    SpotCategory.CINEMA_THEATER -> "Kino / Theater"
    SpotCategory.LIBRARY_READING -> "Bibliothek / Buchhandlung"
    SpotCategory.MUSEUM_EXHIBITION -> "Museum / Ausstellung"
    SpotCategory.INDOOR_SHOPPING_PASSAGE -> "Einkaufspassage"
    SpotCategory.ICE_RINK -> "Eislaufbahn"
    SpotCategory.INDOOR_PLAYGROUND -> "Indoor-Freizeit / Klettern"
    SpotCategory.WARM_CHURCH -> "Kirche / Andachtsort"
    SpotCategory.WARMING_SHELTER -> "Wärmestube / Warteraum"
  }

  private fun buildSpotDescription(category: SpotCategory, tags: Map<String, String>): String? {
    val parts = mutableListOf<String>()
    if (tags["fireplace"] == "yes") parts.add("Kaminfeuer vorhanden")
    if (tags["heating"] == "yes") parts.add("Beheizter Innenraum")
    tags["cuisine"]?.let { parts.add("Küche: $it") }
    tags["coffee"]?.let { parts.add("Kaffeespezialitäten") }
    tags["tea"]?.let { parts.add("Teeauswahl") }
    tags["indoor"]?.let { if (it == "yes") parts.add("Überdacht & beheizt") }
    tags["sauna"]?.let { parts.add("Saunabereich") }
    tags["admission"]?.let { parts.add("Eintritt: $it") }
    return if (parts.isNotEmpty()) parts.joinToString(" • ") else null
  }

  private fun buildAddress(tags: Map<String, String>): String? {
    val street = tags["addr:street"]
    val house = tags["addr:housenumber"]
    val city = tags["addr:city"]
    val post = tags["addr:postcode"]

    val streetLine = if (street != null && house != null) "$street $house" else street
    val cityLine = if (post != null && city != null) "$post $city" else city

    return listOfNotNull(streetLine, cityLine).joinToString(", ").ifBlank { null }
  }

  suspend fun searchLocations(query: String): List<NominatimResult> = withContext(Dispatchers.IO) {
    val trimmed = query.trim()
    if (trimmed.length < 2) return@withContext emptyList()

    // 1. Primary fast location search via Photon (Komoot OSM Geocoder)
    try {
      val encoded = URLEncoder.encode(trimmed, "UTF-8")
      val photonUrl = "https://photon.komoot.io/api/?q=$encoded&limit=6&lang=de"
      val body = executeGetWithGzip(photonUrl, connectTimeoutMs = 12000, readTimeoutMs = 20000)
      val photonParsed = json.decodeFromString<PhotonResponse>(body)
      if (photonParsed.features.isNotEmpty()) {
        val results = photonParsed.features.mapNotNull { feat ->
          val coords = feat.geometry?.coordinates ?: return@mapNotNull null
          if (coords.size < 2) return@mapNotNull null
          val lon = coords[0]
          val lat = coords[1]
          val props = feat.properties

          val name = props["name"]?.jsonPrimitive?.contentOrNull
          val street = props["street"]?.jsonPrimitive?.contentOrNull
          val housenumber = props["housenumber"]?.jsonPrimitive?.contentOrNull
          val city = props["city"]?.jsonPrimitive?.contentOrNull
            ?: props["town"]?.jsonPrimitive?.contentOrNull
            ?: props["village"]?.jsonPrimitive?.contentOrNull
            ?: props["county"]?.jsonPrimitive?.contentOrNull
          val state = props["state"]?.jsonPrimitive?.contentOrNull
          val country = props["country"]?.jsonPrimitive?.contentOrNull

          val streetPart = if (street != null) {
            if (housenumber != null) "$street $housenumber" else street
          } else null

          val displayParts = listOfNotNull(name, streetPart, city, state, country).distinct()
          val displayName = if (displayParts.isNotEmpty()) {
            displayParts.joinToString(", ")
          } else {
            trimmed
          }

          NominatimResult(
            display_name = displayName,
            name = name ?: city ?: displayName,
            lat = lat.toString(),
            lon = lon.toString(),
          )
        }
        if (results.isNotEmpty()) {
          return@withContext results
        }
      }
    } catch (_: Exception) {
    }

    // 2. Fallback to Nominatim
    try {
      val encoded = URLEncoder.encode(trimmed, "UTF-8")
      val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=6&accept-language=de"
      val body = executeGetWithGzip(url, connectTimeoutMs = 15000, readTimeoutMs = 25000)
      json.decodeFromString<List<NominatimResult>>(body)
    } catch (_: Exception) {
      emptyList()
    }
  }

  private fun routedServicePrefix(mode: TravelMode): String = when (mode) {
    TravelMode.WALKING -> "routed-foot"
    TravelMode.BICYCLING -> "routed-bike"
    TravelMode.DRIVING -> "routed-car"
  }

  private fun computeModeDurationSeconds(distanceMeters: Double, rawDurationSeconds: Double, mode: TravelMode, fromDedicatedServer: Boolean): Int {
    if (fromDedicatedServer && rawDurationSeconds > 0.0) {
      return rawDurationSeconds.roundToInt().coerceAtLeast(30)
    }
    return when (mode) {
      TravelMode.WALKING -> (distanceMeters / 1.33).roundToInt().coerceAtLeast(60)   // ~4.8 km/h walking
      TravelMode.BICYCLING -> (distanceMeters / 4.20).roundToInt().coerceAtLeast(45) // ~15.1 km/h cycling
      TravelMode.DRIVING -> (distanceMeters / 9.50).roundToInt().coerceAtLeast(30)   // ~34 km/h urban driving
    }
  }

  suspend fun calculateRoute(
    originLat: Double,
    originLon: Double,
    destination: WarmSpot,
    mode: TravelMode = TravelMode.WALKING,
  ): Result<RouteInfo> = withContext(Dispatchers.IO) {
    val servicePrefix = routedServicePrefix(mode)
    val coords = "$originLon,$originLat;${destination.lon},${destination.lat}"

    // 1. Primary: Official OpenStreetMap.de dedicated routing engines (routed-foot, routed-bike, routed-car)
    try {
      val urlStr = "https://routing.openstreetmap.de/$servicePrefix/route/v1/driving/$coords?overview=full&geometries=geojson&alternatives=true&continue_straight=false"
      val body = executeGetWithGzip(urlStr, connectTimeoutMs = 7000, readTimeoutMs = 10000)
      val resp = json.decodeFromString<OsrmRouteResponse>(body)
      val best = resp.routes.minByOrNull { it.distance } ?: resp.routes.firstOrNull()
      if (best != null && best.distance > 0) {
        val points = best.geometry?.coordinates?.map { it[1] to it[0] } ?: emptyList()
        return@withContext Result.success(
          RouteInfo(
            targetSpot = destination,
            originLat = originLat,
            originLon = originLon,
            travelMode = mode,
            distanceMeters = best.distance.roundToInt(),
            durationSeconds = computeModeDurationSeconds(best.distance, best.duration, mode, fromDedicatedServer = true),
            polylinePoints = points,
          )
        )
      }
    } catch (_: Exception) {
    }

    // 2. Fallback: Project OSRM with mode-specific duration calculation
    try {
      val fallbackUrl = "https://router.project-osrm.org/route/v1/driving/$coords?overview=full&geometries=geojson&alternatives=true"
      val body = executeGetWithGzip(fallbackUrl, connectTimeoutMs = 8000, readTimeoutMs = 12000)
      val resp = json.decodeFromString<OsrmRouteResponse>(body)
      val best = resp.routes.minByOrNull { it.distance } ?: resp.routes.firstOrNull()
        ?: return@withContext Result.failure(RuntimeException("No route found"))
      val points = best.geometry?.coordinates?.map { it[1] to it[0] } ?: emptyList()
      Result.success(
        RouteInfo(
          targetSpot = destination,
          originLat = originLat,
          originLon = originLon,
          travelMode = mode,
          distanceMeters = best.distance.roundToInt(),
          durationSeconds = computeModeDurationSeconds(best.distance, best.duration, mode, fromDedicatedServer = false),
          polylinePoints = points,
        )
      )
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Calculates the optimal sequence of stops to minimize total travel distance
   * starting from origin (exact optimal permutation for up to 8 stops, Nearest-Neighbor + 2-opt beyond).
   */
  fun optimizeStopOrder(
    originLat: Double,
    originLon: Double,
    stops: List<WarmSpot>,
  ): List<WarmSpot> {
    if (stops.size <= 1) return stops

    fun distMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
      val dLat = (lat2 - lat1) * 110540.0
      val dLon = (lon2 - lon1) * 111320.0 * cos(Math.toRadians((lat1 + lat2) / 2.0))
      return kotlin.math.hypot(dLat, dLon)
    }

    if (stops.size <= 8) {
      var bestPermutation = stops
      var minTotalDist = Double.MAX_VALUE

      fun permute(list: MutableList<WarmSpot>, k: Int) {
        if (k == list.size) {
          var total = distMeters(originLat, originLon, list[0].lat, list[0].lon)
          for (i in 0 until list.size - 1) {
            total += distMeters(list[i].lat, list[i].lon, list[i + 1].lat, list[i + 1].lon)
            if (total >= minTotalDist) return
          }
          if (total < minTotalDist) {
            minTotalDist = total
            bestPermutation = ArrayList(list)
          }
          return
        }
        for (i in k until list.size) {
          val temp = list[k]
          list[k] = list[i]
          list[i] = temp
          permute(list, k + 1)
          val back = list[k]
          list[k] = list[i]
          list[i] = back
        }
      }

      permute(stops.toMutableList(), 0)
      return bestPermutation
    } else {
      // Nearest neighbor initialization
      val remaining = stops.toMutableList()
      val ordered = mutableListOf<WarmSpot>()
      var currentLat = originLat
      var currentLon = originLon

      while (remaining.isNotEmpty()) {
        val next = remaining.minByOrNull { distMeters(currentLat, currentLon, it.lat, it.lon) } ?: remaining.first()
        remaining.remove(next)
        ordered.add(next)
        currentLat = next.lat
        currentLon = next.lon
      }

      // 2-opt local search optimization to uncross any sub-routes
      var improved = true
      var iterations = 0
      while (improved && iterations < 30) {
        improved = false
        iterations++
        for (i in 0 until ordered.size - 1) {
          for (j in i + 1 until ordered.size) {
            val pBefore = if (i == 0) (originLat to originLon) else (ordered[i - 1].lat to ordered[i - 1].lon)
            val pI = ordered[i].lat to ordered[i].lon
            val pJ = ordered[j].lat to ordered[j].lon
            val pAfter = if (j + 1 < ordered.size) (ordered[j + 1].lat to ordered[j + 1].lon) else null

            val currentD = distMeters(pBefore.first, pBefore.second, pI.first, pI.second) +
              (if (pAfter != null) distMeters(pJ.first, pJ.second, pAfter.first, pAfter.second) else 0.0)

            val swappedD = distMeters(pBefore.first, pBefore.second, pJ.first, pJ.second) +
              (if (pAfter != null) distMeters(pI.first, pI.second, pAfter.first, pAfter.second) else 0.0)

            if (swappedD < currentD - 1.0) {
              ordered.subList(i, j + 1).reverse()
              improved = true
            }
          }
        }
      }

      return ordered
    }
  }

  suspend fun calculateMultiStopRoute(
    originLat: Double,
    originLon: Double,
    stops: List<WarmSpot>,
    mode: TravelMode = TravelMode.WALKING,
  ): Result<RouteInfo> = withContext(Dispatchers.IO) {
    if (stops.isEmpty()) return@withContext Result.failure(IllegalArgumentException("No stops provided"))
    if (stops.size == 1) return@withContext calculateRoute(originLat, originLon, stops.first(), mode)

    val orderedStops = optimizeStopOrder(originLat, originLon, stops)
    val servicePrefix = routedServicePrefix(mode)

    val coordList = mutableListOf("$originLon,$originLat")
    for (s in orderedStops) {
      coordList.add("${s.lon},${s.lat}")
    }
    val coords = coordList.joinToString(";")

    // 1. Primary: Query Official OpenStreetMap.de Trip API & Route API concurrently for the specific travel mode
    // and pick the strictly shortest route in meters.
    try {
      val bestDedicatedRoute = coroutineScope {
        val tripDeferred = async {
          runCatching {
            val tripUrl = "https://routing.openstreetmap.de/$servicePrefix/trip/v1/driving/$coords?source=first&roundtrip=false&overview=full&geometries=geojson"
            val tripBody = executeGetWithGzip(tripUrl, connectTimeoutMs = 7000, readTimeoutMs = 10000)
            val tripResp = json.decodeFromString<OsrmTripResponse>(tripBody)
            val bestTrip = tripResp.trips.minByOrNull { it.distance } ?: tripResp.trips.firstOrNull()
            if (bestTrip != null && bestTrip.distance > 0) {
              val networkOrderedStops = if (tripResp.waypoints.size == orderedStops.size + 1) {
                orderedStops.indices
                  .sortedBy { idx -> tripResp.waypoints[idx + 1].waypoint_index }
                  .map { idx -> orderedStops[idx] }
              } else {
                orderedStops
              }
              val points = bestTrip.geometry?.coordinates?.map { it[1] to it[0] } ?: emptyList()
              RouteInfo(
                targetSpots = networkOrderedStops,
                originLat = originLat,
                originLon = originLon,
                travelMode = mode,
                distanceMeters = bestTrip.distance.roundToInt(),
                durationSeconds = computeModeDurationSeconds(bestTrip.distance, bestTrip.duration, mode, fromDedicatedServer = true),
                polylinePoints = points,
              )
            } else null
          }.getOrNull()
        }

        val routeDeferred = async {
          runCatching {
            val routeUrl = "https://routing.openstreetmap.de/$servicePrefix/route/v1/driving/$coords?overview=full&geometries=geojson&alternatives=true&continue_straight=false"
            val body = executeGetWithGzip(routeUrl, connectTimeoutMs = 7000, readTimeoutMs = 10000)
            val resp = json.decodeFromString<OsrmRouteResponse>(body)
            val best = resp.routes.minByOrNull { it.distance } ?: resp.routes.firstOrNull()
            if (best != null && best.distance > 0) {
              val points = best.geometry?.coordinates?.map { it[1] to it[0] } ?: emptyList()
              RouteInfo(
                targetSpots = orderedStops,
                originLat = originLat,
                originLon = originLon,
                travelMode = mode,
                distanceMeters = best.distance.roundToInt(),
                durationSeconds = computeModeDurationSeconds(best.distance, best.duration, mode, fromDedicatedServer = true),
                polylinePoints = points,
              )
            } else null
          }.getOrNull()
        }

        val r1 = tripDeferred.await()
        val r2 = routeDeferred.await()
        listOfNotNull(r1, r2).minByOrNull { it.distanceMeters }
      }

      if (bestDedicatedRoute != null) {
        return@withContext Result.success(bestDedicatedRoute)
      }
    } catch (_: Exception) {
    }

    // 3. Fallback: Project OSRM with mode-specific duration calculation
    try {
      val fallbackUrl = "https://router.project-osrm.org/route/v1/driving/$coords?overview=full&geometries=geojson&alternatives=true"
      val body = executeGetWithGzip(fallbackUrl, connectTimeoutMs = 8000, readTimeoutMs = 12000)
      val resp = json.decodeFromString<OsrmRouteResponse>(body)
      val best = resp.routes.minByOrNull { it.distance } ?: resp.routes.firstOrNull()
        ?: return@withContext Result.failure(RuntimeException("No multi-stop route found"))

      val points = best.geometry?.coordinates?.map { it[1] to it[0] } ?: emptyList()
      Result.success(
        RouteInfo(
          targetSpots = orderedStops,
          originLat = originLat,
          originLon = originLon,
          travelMode = mode,
          distanceMeters = best.distance.roundToInt(),
          durationSeconds = computeModeDurationSeconds(best.distance, best.duration, mode, fromDedicatedServer = false),
          polylinePoints = points,
        )
      )
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
