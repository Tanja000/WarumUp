package com.findwarmplaceswithwarmup.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.HotTub
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.findwarmplaceswithwarmup.data.AppLanguage

@Composable
fun OnboardingIntroDialog(
  lang: AppLanguage,
  onDismiss: () -> Unit,
) {
  val isEn = lang == AppLanguage.EN
  var step by remember { mutableIntStateOf(0) }
  val maxSteps = 3

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.90f)
        .padding(16.dp),
      shape = RoundedCornerShape(28.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 10.dp,
      shadowElevation = 12.dp,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        AnimatedContent(targetState = step, label = "fancy_step") { currentStep ->
          when (currentStep) {
            0 -> FancyStepContent(
              icon = Icons.Default.LocalFireDepartment,
              gradient = listOf(Color(0xFFEA580C), Color(0xFFD97706)),
              badge = if (isEn) "WINTER EDITION" else "WINTER EDITION",
              headline = if (isEn) "Raus aus der Kälte! 🔥" else "Raus aus der Kälte! 🔥",
              subheadline = if (isEn) "Dein persönlicher Wärmefinder" else "Dein persönlicher Wärmefinder",
              features = listOf(
                "☕" to if (isEn) "Cozy cafés & hot drinks" else "Gemütliche Cafés & Heißgetränke",
                "🧖" to if (isEn) "Steaming saunas & thermal baths" else "Dampfende Saunen & Thermen",
                "🏛️" to if (isEn) "Warm museums & libraries" else "Geheizte Museen & Bibliotheken",
              ),
            )
            1 -> FancyStepContent(
              icon = Icons.Default.HotTub,
              gradient = listOf(Color(0xFFD97706), Color(0xFFC05621)),
              badge = if (isEn) "13 KATEGORIEN" else "13 KATEGORIEN",
              headline = if (isEn) "1-Klick Filter ⚡" else "1-Klick Filter ⚡",
              subheadline = if (isEn) "Finde sofort, was dir guttut" else "Finde sofort, was dir guttut",
              features = listOf(
                "🍷" to if (isEn) "Christmas markets & mulled wine" else "Weihnachtsmärkte & Glühwein",
                "🎬" to if (isEn) "Cinemas, theaters & culture" else "Kinos, Theater & Kulturabende",
                "🪵" to if (isEn) "Restaurants with cozy fireplace" else "Lokale mit offenem Kamin",
              ),
            )
            else -> FancyStepContent(
              icon = Icons.Default.Navigation,
              gradient = listOf(Color(0xFFC05621), Color(0xFF9C4146)),
              badge = if (isEn) "OFFLINE & ROUTING" else "OFFLINE & ROUTING",
              headline = if (isEn) "Schnell ins Warme 🚶" else "Schnell ins Warme 🚶",
              subheadline = if (isEn) "Navigation & Multi-Stopps" else "Navigation & Multi-Stopps",
              features = listOf(
                "🗺️" to if (isEn) "Turn-by-turn route calculation" else "Routenberechnung zu Fuß & Rad",
                "📍" to if (isEn) "Multi-stop winter walks" else "Mehrere warme Zwischenstopps",
                "❄️" to if (isEn) "Works offline in snow flurries" else "Funktioniert auch ohne Internet",
              ),
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Progress Dots
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          repeat(maxSteps) { idx ->
            val isSelected = idx == step
            Box(
              modifier = Modifier
                .height(8.dp)
                .width(if (isSelected) 24.dp else 8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                  if (isSelected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                ),
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (step > 0) {
            OutlinedButton(
              onClick = { step-- },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(14.dp),
            ) {
              Text(if (isEn) "Zurück" else "Zurück")
            }
          }

          Button(
            onClick = {
              if (step < maxSteps - 1) step++
              else onDismiss()
            },
            modifier = Modifier.weight(1.5f),
            shape = RoundedCornerShape(14.dp),
          ) {
            Text(
              text = if (step == maxSteps - 1) {
                if (isEn) "Aufwärmen! 🔥" else "Aufwärmen! 🔥"
              } else {
                if (isEn) "Weiter" else "Weiter"
              },
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FancyStepContent(
  icon: ImageVector,
  gradient: List<Color>,
  badge: String,
  headline: String,
  subheadline: String,
  features: List<Pair<String, String>>,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Glowing gradient icon box
    Box(
      modifier = Modifier
        .size(72.dp)
        .clip(CircleShape)
        .background(Brush.linearGradient(gradient)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(38.dp),
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Surface(
      shape = RoundedCornerShape(6.dp),
      color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
    ) {
      Text(
        text = badge,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = headline,
      fontSize = 22.sp,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center,
    )

    Text(
      text = subheadline,
      fontSize = 13.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(18.dp))

    // 3 Crisp Feature rows
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        features.forEach { (emoji, text) ->
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(emoji, fontSize = 18.sp)
            Text(
              text = text,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }
  }
}
