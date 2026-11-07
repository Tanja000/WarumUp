package com.findwarmplaceswithwarmup.ui.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.findwarmplaceswithwarmup.data.SpotCategory

object MarkerIconFactory {
  private val cache = mutableMapOf<Pair<SpotCategory, Boolean>, Drawable>()
  private val clusterCache = mutableMapOf<Pair<SpotCategory, Int>, Drawable>()

  fun getClusterDrawable(
    context: Context,
    category: SpotCategory,
    count: Int,
  ): Drawable {
    val bucketCount = count.coerceAtMost(99)
    val key = category to bucketCount
    clusterCache[key]?.let { return it }

    val density = context.resources.displayMetrics.density
    val sizeDp = when {
      count >= 20 -> 44f
      count >= 5 -> 40f
      else -> 36f
    }
    val sizePx = (sizeDp * density).toInt().coerceAtLeast(48)

    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = sizePx / 2f
    val cy = sizePx / 2f
    val outerRadius = (sizePx / 2f) - (2f * density)
    val innerRadius = outerRadius - (4f * density)

    // Warm glow halo in category color
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.argb(
        110,
        Color.red(category.colorHex),
        Color.green(category.colorHex),
        Color.blue(category.colorHex),
      )
      style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, outerRadius, haloPaint)

    // Inner solid circle
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = category.colorHex
      style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, innerRadius, fillPaint)

    // Crisp white border
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.WHITE
      style = Paint.Style.STROKE
      strokeWidth = 2.2f * density
    }
    canvas.drawCircle(cx, cy, innerRadius, borderPaint)

    // Count label
    val label = if (count > 99) "99+" else count.toString()
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.WHITE
      textAlign = Paint.Align.CENTER
      isFakeBoldText = true
      textSize = (if (label.length >= 3) 11.5f else 13.5f) * density
    }
    val textOffsetY = (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(label, cx, cy - textOffsetY, textPaint)

    val drawable = BitmapDrawable(context.resources, bitmap)
    clusterCache[key] = drawable
    return drawable
  }

  fun getMarkerDrawable(
    context: Context,
    category: SpotCategory,
    isSelected: Boolean = false,
  ): Drawable {
    val key = category to isSelected
    cache[key]?.let { return it }

    val density = context.resources.displayMetrics.density
    val sizeDp = if (isSelected) 46f else 36f
    val width = (sizeDp * density).toInt().coerceAtLeast(48)
    val height = (sizeDp * 1.25f * density).toInt().coerceAtLeast(60)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = width / 2f
    val headRadius = width * 0.44f
    val headCy = headRadius + (2f * density)
    val tipY = height - (2f * density)

    // Drop shadow
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.argb(60, 0, 0, 0)
      style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, tipY, 4f * density, shadowPaint)

    // Pin shape
    val pinPath = Path().apply {
      val angle = 35.0
      val rad = Math.toRadians(angle)
      val xTangent = (headRadius * kotlin.math.cos(rad)).toFloat()
      val yTangent = (headRadius * kotlin.math.sin(rad)).toFloat()

      moveTo(cx - xTangent, headCy + yTangent)
      arcTo(
        cx - headRadius,
        headCy - headRadius,
        cx + headRadius,
        headCy + headRadius,
        180f - angle.toFloat(),
        180f + 2 * angle.toFloat(),
        false,
      )
      lineTo(cx, tipY)
      close()
    }

    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = category.colorHex
      style = Paint.Style.FILL
    }
    canvas.drawPath(pinPath, pinPaint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.WHITE
      style = Paint.Style.STROKE
      strokeWidth = if (isSelected) 3.0f * density else 2.2f * density
    }
    canvas.drawPath(pinPath, strokePaint)

    // Inner white circle
    val innerRadius = headRadius * 0.60f
    val innerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.WHITE
      style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, headCy, innerRadius, innerBgPaint)

    // Category glyph inside inner circle
    drawCategoryGlyph(canvas, cx, headCy, innerRadius * 0.70f, category.colorHex, category)

    val drawable = BitmapDrawable(context.resources, bitmap)
    cache[key] = drawable
    return drawable
  }

  private fun drawCategoryGlyph(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    radius: Float,
    colorHex: Int,
    category: SpotCategory,
  ) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = colorHex
      style = Paint.Style.FILL
      strokeWidth = radius * 0.35f
      strokeCap = Paint.Cap.ROUND
    }

    when (category) {
      SpotCategory.CAFE_HOT_DRINKS -> {
        // Coffee Cup: body + handle + steam
        val cupW = radius * 0.8f
        val cupH = radius * 0.6f
        canvas.drawRoundRect(
          cx - cupW, cy - cupH * 0.2f,
          cx + cupW * 0.6f, cy + cupH,
          radius * 0.2f, radius * 0.2f, paint
        )
        // Steam dots/lines
        canvas.drawCircle(cx - cupW * 0.4f, cy - cupH * 0.6f, radius * 0.15f, paint)
        canvas.drawCircle(cx + cupW * 0.1f, cy - cupH * 0.7f, radius * 0.15f, paint)
      }
      SpotCategory.THERMAL_BATH_SAUNA, SpotCategory.FIREPLACE_RESTAURANT -> {
        // Flame shape
        val flamePath = Path().apply {
          moveTo(cx, cy - radius)
          quadTo(cx + radius * 0.8f, cy, cx, cy + radius * 0.9f)
          quadTo(cx - radius * 0.8f, cy, cx, cy - radius)
          close()
        }
        canvas.drawPath(flamePath, paint)
      }
      SpotCategory.INDOOR_POOL -> {
        // Water waves
        paint.style = Paint.Style.STROKE
        canvas.drawLine(cx - radius * 0.8f, cy - radius * 0.2f, cx + radius * 0.8f, cy - radius * 0.2f, paint)
        canvas.drawLine(cx - radius * 0.6f, cy + radius * 0.4f, cx + radius * 0.6f, cy + radius * 0.4f, paint)
      }
      SpotCategory.CHRISTMAS_MARKET -> {
        // Festive star / tree
        val treePath = Path().apply {
          moveTo(cx, cy - radius)
          lineTo(cx + radius * 0.8f, cy + radius * 0.8f)
          lineTo(cx - radius * 0.8f, cy + radius * 0.8f)
          close()
        }
        canvas.drawPath(treePath, paint)
      }
      SpotCategory.LIBRARY_READING -> {
        // Open book
        paint.style = Paint.Style.STROKE
        canvas.drawArc(cx - radius, cy - radius * 0.4f, cx, cy + radius * 0.6f, 180f, 180f, false, paint)
        canvas.drawArc(cx, cy - radius * 0.4f, cx + radius, cy + radius * 0.6f, 180f, 180f, false, paint)
      }
      SpotCategory.MUSEUM_EXHIBITION -> {
        // Temple / museum pillars
        paint.style = Paint.Style.STROKE
        val roof = Path().apply {
          moveTo(cx - radius, cy - radius * 0.2f)
          lineTo(cx, cy - radius)
          lineTo(cx + radius, cy - radius * 0.2f)
          close()
        }
        canvas.drawPath(roof, paint)
        canvas.drawLine(cx - radius * 0.6f, cy, cx - radius * 0.6f, cy + radius * 0.8f, paint)
        canvas.drawLine(cx, cy, cx, cy + radius * 0.8f, paint)
        canvas.drawLine(cx + radius * 0.6f, cy, cx + radius * 0.6f, cy + radius * 0.8f, paint)
      }
      SpotCategory.CINEMA_THEATER -> {
        // Film clapper / ticket icon
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(
          cx - radius * 0.75f, cy - radius * 0.55f,
          cx + radius * 0.75f, cy + radius * 0.65f,
          radius * 0.15f, radius * 0.15f, paint
        )
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy + radius * 0.05f, radius * 0.22f, paint)
      }
      SpotCategory.INDOOR_SHOPPING_PASSAGE -> {
        // Shopping bag
        paint.style = Paint.Style.STROKE
        canvas.drawRect(cx - radius * 0.7f, cy - radius * 0.2f, cx + radius * 0.7f, cy + radius * 0.8f, paint)
        canvas.drawArc(cx - radius * 0.4f, cy - radius * 0.8f, cx + radius * 0.4f, cy, 180f, 180f, false, paint)
      }
      SpotCategory.ICE_RINK -> {
        // Skate / snowflake dot
        paint.style = Paint.Style.STROKE
        canvas.drawLine(cx - radius * 0.8f, cy, cx + radius * 0.8f, cy, paint)
        canvas.drawLine(cx, cy - radius * 0.8f, cx, cy + radius * 0.8f, paint)
        canvas.drawLine(cx - radius * 0.6f, cy - radius * 0.6f, cx + radius * 0.6f, cy + radius * 0.6f, paint)
        canvas.drawLine(cx + radius * 0.6f, cy - radius * 0.6f, cx - radius * 0.6f, cy + radius * 0.6f, paint)
      }
      SpotCategory.INDOOR_PLAYGROUND -> {
        // Joy / play circle & dot
        canvas.drawCircle(cx, cy - radius * 0.4f, radius * 0.35f, paint)
        val body = Path().apply {
          moveTo(cx - radius * 0.6f, cy + radius * 0.8f)
          lineTo(cx, cy)
          lineTo(cx + radius * 0.6f, cy + radius * 0.8f)
        }
        paint.style = Paint.Style.STROKE
        canvas.drawPath(body, paint)
      }
      SpotCategory.WARM_CHURCH -> {
        // Cross
        paint.style = Paint.Style.STROKE
        canvas.drawLine(cx, cy - radius * 0.9f, cx, cy + radius * 0.9f, paint)
        canvas.drawLine(cx - radius * 0.6f, cy - radius * 0.3f, cx + radius * 0.6f, cy - radius * 0.3f, paint)
      }
      SpotCategory.WARMING_SHELTER -> {
        // Roof / home
        val home = Path().apply {
          moveTo(cx, cy - radius * 0.9f)
          lineTo(cx + radius * 0.8f, cy - radius * 0.1f)
          lineTo(cx + radius * 0.6f, cy + radius * 0.8f)
          lineTo(cx - radius * 0.6f, cy + radius * 0.8f)
          lineTo(cx - radius * 0.8f, cy - radius * 0.1f)
          close()
        }
        canvas.drawPath(home, paint)
      }
    }
  }
}
