package com.lilapause.warumup.ui.main

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource

enum class FreeMapStyle(
  val titleDe: String,
  val subtitleDe: String,
  val tileSource: OnlineTileSourceBase,
) {
  OSM_DE(
    titleDe = "OSM Deutschland",
    subtitleDe = "Schneller deutscher Kartenserver (kostenlos, ohne API-Key)",
    tileSource = XYTileSource(
      "OSM_DE_Fast",
      0,
      19,
      256,
      ".png",
      arrayOf(
        "https://a.tile.openstreetmap.de/",
        "https://b.tile.openstreetmap.de/",
        "https://c.tile.openstreetmap.de/",
      ),
      "© OpenStreetMap-Mitwirkende (tile.openstreetmap.de)",
    ),
  ),
  OSM_HOT(
    titleDe = "OSM Humanitarian (Hell & Klar)",
    subtitleDe = "Kontrastreiche Karte mit klarer Gebäude- und Wegausprägung",
    tileSource = XYTileSource(
      "OSM_HOT_Free",
      0,
      19,
      256,
      ".png",
      arrayOf(
        "https://a.tile.openstreetmap.fr/hot/",
        "https://b.tile.openstreetmap.fr/hot/",
        "https://c.tile.openstreetmap.fr/hot/",
      ),
      "© OpenStreetMap-Mitwirkende, Tiles: HOT / OSM France",
    ),
  ),
  OPEN_TOPO(
    titleDe = "OpenTopoMap (Topografisch)",
    subtitleDe = "Geländekarte mit Höhenlinien, Hügeln und Tälern",
    tileSource = XYTileSource(
      "OpenTopoMap_Free",
      1,
      17,
      256,
      ".png",
      arrayOf(
        "https://a.tile.opentopomap.org/",
        "https://b.tile.opentopomap.org/",
        "https://c.tile.opentopomap.org/",
      ),
      "© OpenStreetMap-Mitwirkende, SRTM | OpenTopoMap",
    ),
  ),
  OSM_STANDARD(
    titleDe = "OpenStreetMap Standard",
    subtitleDe = "Weltweiter Standard-Kartenstil",
    tileSource = XYTileSource(
      "OSM_Standard_Configured",
      0,
      19,
      256,
      ".png",
      arrayOf(
        "https://tile.openstreetmap.org/",
      ),
      "© OpenStreetMap-Mitwirkende",
    ),
  ),
}
