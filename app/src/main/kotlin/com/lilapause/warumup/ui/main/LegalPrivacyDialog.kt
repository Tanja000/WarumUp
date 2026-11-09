package com.lilapause.warumup.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lilapause.warumup.data.AppLanguage

@Composable
fun GdprConsentBanner(
  lang: AppLanguage,
  onToggleLanguage: () -> Unit = {},
  onAcceptPersonalized: () -> Unit,
  onAcceptNonPersonalized: () -> Unit,
  onOpenPrivacyDetails: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isEn = lang == AppLanguage.EN

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(12.dp),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.onSurface,
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Gavel,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = if (isEn) "Privacy & Advertising" else "Datenschutz & Werbung",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
        TextButton(onClick = onToggleLanguage) {
          Text(if (isEn) "🇩🇪 DE" else "🇬🇧 EN", fontWeight = FontWeight.Bold)
        }
      }

      Text(
        text = if (isEn) {
          "WarmUp uses OpenStreetMap to display cozy winter spots, saunas, cafés, cinemas and museums. To keep the app free, we display Google AdMob banners. You can decide whether you want personalized or non-personalized ads."
        } else {
          "WarmUp nutzt OpenStreetMap für die Anzeige gemütlicher Cafés, Saunen, Thermen, Kinos und Museen. Zur kostenlosen Bereitstellung zeigen wir Banner von Google AdMob. Wähle deine Werbepräferenz:"
        },
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onAcceptNonPersonalized,
          modifier = Modifier.weight(1f),
        ) {
          Text(if (isEn) "Basic Ads" else "Basis-Werbung", fontSize = 12.sp)
        }

        Button(
          onClick = onAcceptPersonalized,
          modifier = Modifier.weight(1f),
        ) {
          Text(if (isEn) "Accept All" else "Zustimmen", fontSize = 12.sp)
        }
      }

      TextButton(
        onClick = onOpenPrivacyDetails,
        modifier = Modifier.align(Alignment.CenterHorizontally),
      ) {
        Text(if (isEn) "Privacy Details & Imprint" else "Datenschutzerklärung & Impressum", fontSize = 12.sp)
      }
    }
  }
}

@Composable
fun LegalPrivacyDialog(
  lang: AppLanguage,
  personalizedConsent: Boolean,
  onConsentChanged: (Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  val isEn = lang == AppLanguage.EN
  val scrollState = rememberScrollState()

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .heightIn(max = 620.dp),
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = if (isEn) "Legal & Privacy" else "Rechtliches & Datenschutz",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Schließen")
          }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(scrollState),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Text(
            text = if (isEn) "Advertising Preference" else "Werbeeinstellungen",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (isEn) "Personalized Ads (Google AdMob)" else "Personalisierte Werbung (Google AdMob)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
              )
              Text(
                text = if (isEn) "Uses advertising ID for relevant ads" else "Nutzt Werbe-ID für passendere Anzeigen",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = personalizedConsent,
              onCheckedChange = onConsentChanged,
            )
          }

          HorizontalDivider()

          Text(
            text = if (isEn) "Location & Data Processing" else "Standort & Datennutzung",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
          )
          Text(
            text = if (isEn) {
              "WarmUp requests GPS location exclusively to center the map on your position and find nearby warm places. Location data is never stored on servers or linked to identities."
            } else {
              "WarmUp verarbeitet den Gerätestandort ausschließlich lokal, um dir warme Orte und Saunen in deiner Nähe anzuzeigen. Der Standort wird nicht auf externen Servern gespeichert."
            },
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          HorizontalDivider()

          Text(
            text = if (isEn) "OpenStreetMap & Map Attribution" else "Kartendaten & Urheberrecht",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
          )
          Text(
            text = if (isEn) {
              "Map data and points of interest are powered by OpenStreetMap contributors (ODbL). Routing is calculated via OpenStreetMap Routing Service (OSRM). POI search & geocoding via Photon (Komoot) & Nominatim. Map tiles by OpenStreetMap & CartoDB."
            } else {
              "Kartendaten und POIs stammen von OpenStreetMap (© OpenStreetMap-Mitwirkende, ODbL). Routenberechnung via OpenStreetMap Routing Service (OSRM). POI-Suche & Geocoding via Photon (Komoot) & Nominatim. Kartenkacheln von OpenStreetMap & CartoDB."
            },
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          HorizontalDivider()

          Text(
            text = if (isEn) "Impressum & Controller (§ 5 DDG / GDPR)" else "Impressum & Verantwortliche Stelle (§ 5 DDG)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
          )
          Text(
            text = if (isEn) {
              "Tanja Reize\nWilhelm-Busch Str. 41\n81477 München\nGermany\n\nEmail: tanjareize@web.de\nPackage ID: com.lilapause.warumup\nApp Version 1.0 (Winter Edition)"
            } else {
              "Tanja Reize\nWilhelm-Busch Str. 41\n81477 München\nDeutschland\n\nE-Mail: tanjareize@web.de\nPaketname: com.lilapause.warumup\nApp Version 1.0 (Winter Edition)"
            },
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
          onClick = onDismiss,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(if (isEn) "Done" else "Fertig")
        }
      }
    }
  }
}
