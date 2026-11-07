package com.findwarmplaceswithwarmup.data

/**
 * Zentrale Konfiguration für Werbung (Google AdMob) und Unterstützung (Google Play Billing).
 */
object MonetizationConfig {
  // Adaptive Banner Ad Unit ID (Google Test ID - Universal Android Test Banner)
  const val ADMOB_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

  // Interstitial (Vollbild) Ad Unit ID
  const val ADMOB_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

  // Mindestabstand zwischen zwei Vollbild-Werbeanzeigen (3 Minuten)
  const val INTERSTITIAL_MIN_INTERVAL_MS = 180_000L

  const val INTERSTITIAL_ACTION_THRESHOLD = 3

  // In-App-Produkt-IDs für Google Play Billing (Consumables für Spenden / Kaffee)
  const val SKU_DONATION_099 = "donation_espresso_099"
  const val SKU_HOT_CHOCOLATE = "donation_chocolate_250"
  const val SKU_GLUEHWEIN = "donation_gluehwein_500"
  const val SKU_DONATION_1000 = "donation_kaminfeuer_1000"
  const val SKU_DONATION_UNIT_1EUR = "donation_unit_1eur"

  // Legacy-IDs zur Abwärtskompatibilität
  val ALL_DONATION_SKUS = listOf(
    SKU_DONATION_099,
    SKU_HOT_CHOCOLATE,
    SKU_GLUEHWEIN,
    SKU_DONATION_1000,
    SKU_DONATION_UNIT_1EUR,
    "donation_099",
    "hot_drink_small",
    "hot_drink_medium",
  )
}
