package com.lilapause.warumup.ui.main

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lilapause.warumup.data.AppLanguage
import com.lilapause.warumup.data.GooglePlayBillingManager
import com.lilapause.warumup.data.MonetizationConfig

@Composable
fun HotDrinkSupportDialog(
  lang: AppLanguage,
  isAdFree: Boolean,
  onAdFreeUnlocked: () -> Unit,
  onDismiss: () -> Unit,
) {
  val isEn = lang == AppLanguage.EN
  val context = LocalContext.current
  val activity = context as? Activity
  val scrollState = rememberScrollState()

  val billingManager = remember {
    GooglePlayBillingManager(context) { _ ->
      onAdFreeUnlocked()
      Toast.makeText(
        context,
        if (isEn) "❤️ Thank you so much for your warm support!" else "❤️ Herzlichen Dank für deine wärmende Unterstützung!",
        Toast.LENGTH_LONG,
      ).show()
    }
  }

  var customAmountStr by remember { mutableStateOf("") }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .heightIn(max = 560.dp),
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
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
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
            }
            Text(
              text = if (isEn) "Support WarmUp" else "WarmUp unterstützen",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
          }

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
            text = if (isEn) {
              "WarmUp is developed independently with love for the cold season. Support our servers and continuous development with a cozy hot drink!"
            } else {
              "WarmUp wird unabhängig mit viel Liebe für die kalte Jahreszeit gepflegt. Unterstütze die Server und Weiterentwicklung gerne mit einem heißen Getränk!"
            },
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Text(
            text = if (isEn) "Choose a hot drink:" else "Wähle dein Heißgetränk:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
          )

          // 1. Heißer Espresso (0,99 €)
          DrinkOptionCard(
            title = if (isEn) "☕ Hot Espresso" else "☕ Heißer Espresso",
            price = "0,99 €",
            subtitle = if (isEn) "A warm little boost" else "Eine kleine wärmende Aufmerksamkeit",
            onClick = {
              if (activity != null) {
                billingManager.launchPurchase(activity, MonetizationConfig.SKU_DONATION_099) {
                  onAdFreeUnlocked()
                }
              }
            },
          )

          // 2. Heiße Schokolade (2,50 €)
          DrinkOptionCard(
            title = if (isEn) "🍫 Hot Chocolate" else "🍫 Heiße Schokolade",
            price = "2,50 €",
            subtitle = if (isEn) "Sweet warmth against winter chills" else "Süße Wärme gegen die Kälte",
            isPopular = true,
            onClick = {
              if (activity != null) {
                billingManager.launchPurchase(activity, MonetizationConfig.SKU_HOT_CHOCOLATE) {
                  onAdFreeUnlocked()
                }
              }
            },
          )

          HorizontalDivider()

          Text(
            text = if (isEn) "Custom amount (€):" else "Freier Wunschbetrag (€):",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
              value = customAmountStr,
              onValueChange = { customAmountStr = it },
              placeholder = { Text("z. B. 3,50") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              modifier = Modifier.weight(1f),
              singleLine = true,
            )

            Button(
              onClick = {
                val parsed = customAmountStr.replace(",", ".").toDoubleOrNull() ?: 1.0
                if (activity != null) {
                  billingManager.launchCustomAmountPurchase(activity, parsed) {
                    onAdFreeUnlocked()
                  }
                }
              },
            ) {
              Text(if (isEn) "Send" else "Senden")
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = if (isEn) "🛡️ Processed via Google Play" else "🛡️ Google Play In-App Zahlung",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            androidx.compose.material3.TextButton(
              onClick = {
                billingManager.restorePurchases { restored ->
                  Toast.makeText(
                    context,
                    if (restored) {
                      if (isEn) "Purchases restored!" else "Käufe wiederhergestellt!"
                    } else {
                      if (isEn) "No previous purchases found." else "Keine früheren Käufe gefunden."
                    },
                    Toast.LENGTH_SHORT,
                  ).show()
                }
              },
            ) {
              Text(
                text = if (isEn) "Restore" else "Wiederherstellen",
                fontSize = 11.5.sp,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
          onClick = onDismiss,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(if (isEn) "Close" else "Schließen")
        }
      }
    }
  }
}

@Composable
private fun DrinkOptionCard(
  title: String,
  price: String,
  subtitle: String,
  isPopular: Boolean = false,
  onClick: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() },
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isPopular) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant,
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          if (isPopular) {
            Text(
              "★ TIPP",
              fontSize = 9.sp,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
        Text(
          subtitle,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
      ) {
        Text(price, fontWeight = FontWeight.Bold, fontSize = 13.sp)
      }
    }
  }
}
