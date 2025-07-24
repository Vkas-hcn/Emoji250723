package com.mobile.emoticon.fast.emoji

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import kotlin.math.sqrt
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.min

object EmojiUtils {


    fun createCompositeBitmap(context: Context, drawableIds: IntArray, size: Int = 200): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawableIds.forEach { drawableId ->
            val drawable = ContextCompat.getDrawable(context, drawableId)
            drawable?.let {
                it.setBounds(0, 0, size, size)
                it.draw(canvas)
            }
        }

        return bitmap
    }


    fun createEmojiBitmap(context: Context, compositeEmoji: EmojiItem.CompositeEmoji, size: Int = 200): Bitmap {
        val drawableIds = intArrayOf(
            compositeEmoji.faceId,
            compositeEmoji.eyeId,
            compositeEmoji.mouthId,
            compositeEmoji.handId
        )
        return createCompositeBitmap(context, drawableIds, size)
    }


    fun createSingleEmojiBitmap(context: Context, singleEmoji: EmojiItem.SingleEmoji, size: Int = 200): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val drawable = ContextCompat.getDrawable(context, singleEmoji.resourceId)
        drawable?.let {
            it.setBounds(0, 0, size, size)
            it.draw(canvas)
        }

        return bitmap
    }


    fun advancedBlendBitmaps(first: Bitmap, second: Bitmap): Bitmap {
        val width = maxOf(first.width, second.width)
        val height = maxOf(first.height, second.height)
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val firstPixels = IntArray(width * height)
        val secondPixels = IntArray(width * height)
        val resultPixels = IntArray(width * height)

        val firstScaled = Bitmap.createScaledBitmap(first, width, height, true)
        val secondScaled = Bitmap.createScaledBitmap(second, width, height, true)

        firstScaled.getPixels(firstPixels, 0, width, 0, 0, width, height)
        secondScaled.getPixels(secondPixels, 0, width, 0, 0, width, height)

        val blendModes = listOf(
            BlendMode.OVERLAY,
            BlendMode.MULTIPLY,
            BlendMode.SCREEN,
            BlendMode.SOFT_LIGHT,
            BlendMode.COLOR_DODGE
        )
        val selectedMode = blendModes.random()

        for (i in resultPixels.indices) {
            val pixel1 = firstPixels[i]
            val pixel2 = secondPixels[i]

            val a1 = (pixel1 shr 24) and 0xFF
            val r1 = (pixel1 shr 16) and 0xFF
            val g1 = (pixel1 shr 8) and 0xFF
            val b1 = pixel1 and 0xFF

            val a2 = (pixel2 shr 24) and 0xFF
            val r2 = (pixel2 shr 16) and 0xFF
            val g2 = (pixel2 shr 8) and 0xFF
            val b2 = pixel2 and 0xFF

            if (a1 == 0 && a2 == 0) {
                resultPixels[i] = 0
                continue
            }

            if (a1 == 0) {
                resultPixels[i] = pixel2
                continue
            }

            if (a2 == 0) {
                resultPixels[i] = pixel1
                continue
            }

            val (blendR, blendG, blendB) = when (selectedMode) {
                BlendMode.OVERLAY -> overlayBlend(r1, g1, b1, r2, g2, b2)
                BlendMode.MULTIPLY -> multiplyBlend(r1, g1, b1, r2, g2, b2)
                BlendMode.SCREEN -> screenBlend(r1, g1, b1, r2, g2, b2)
                BlendMode.SOFT_LIGHT -> softLightBlend(r1, g1, b1, r2, g2, b2)
                BlendMode.COLOR_DODGE -> colorDodgeBlend(r1, g1, b1, r2, g2, b2)
            }

            val finalAlpha = maxOf(a1, a2)

            resultPixels[i] = (finalAlpha shl 24) or (blendR shl 16) or (blendG shl 8) or blendB
        }

        resultBitmap.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return resultBitmap
    }

    private enum class BlendMode {
        OVERLAY, MULTIPLY, SCREEN, SOFT_LIGHT, COLOR_DODGE
    }

    private fun overlayBlend(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Triple<Int, Int, Int> {
        fun overlay(base: Int, blend: Int): Int {
            return if (base < 128) {
                (2 * base * blend / 255).coerceIn(0, 255)
            } else {
                (255 - 2 * (255 - base) * (255 - blend) / 255).coerceIn(0, 255)
            }
        }
        return Triple(overlay(r1, r2), overlay(g1, g2), overlay(b1, b2))
    }

    private fun multiplyBlend(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Triple<Int, Int, Int> {
        return Triple(
            (r1 * r2 / 255).coerceIn(0, 255),
            (g1 * g2 / 255).coerceIn(0, 255),
            (b1 * b2 / 255).coerceIn(0, 255)
        )
    }

    private fun screenBlend(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Triple<Int, Int, Int> {
        return Triple(
            (255 - (255 - r1) * (255 - r2) / 255).coerceIn(0, 255),
            (255 - (255 - g1) * (255 - g2) / 255).coerceIn(0, 255),
            (255 - (255 - b1) * (255 - b2) / 255).coerceIn(0, 255)
        )
    }

    private fun softLightBlend(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Triple<Int, Int, Int> {
        fun softLight(base: Int, blend: Int): Int {
            val baseNorm = base / 255.0
            val blendNorm = blend / 255.0

            val result = if (blendNorm < 0.5) {
                baseNorm - (1 - 2 * blendNorm) * baseNorm * (1 - baseNorm)
            } else {
                val d = if (baseNorm < 0.25) {
                    ((16 * baseNorm - 12) * baseNorm + 4) * baseNorm
                } else {
                    sqrt(baseNorm)
                }
                baseNorm + (2 * blendNorm - 1) * (d - baseNorm)
            }

            return (result * 255).toInt().coerceIn(0, 255)
        }

        return Triple(softLight(r1, r2), softLight(g1, g2), softLight(b1, b2))
    }

    private fun colorDodgeBlend(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Triple<Int, Int, Int> {
        fun colorDodge(base: Int, blend: Int): Int {
            return if (blend == 255) {
                255
            } else {
                (base * 255 / (255 - blend)).coerceIn(0, 255)
            }
        }
        return Triple(colorDodge(r1, r2), colorDodge(g1, g2), colorDodge(b1, b2))
    }

    fun creativeBlendBitmaps(first: Bitmap, second: Bitmap): Bitmap {
        val width = maxOf(first.width, second.width)
        val height = maxOf(first.height, second.height)
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        val firstScaled = Bitmap.createScaledBitmap(first, width, height, true)
        val secondScaled = Bitmap.createScaledBitmap(second, width, height, true)

        val effects = listOf(
            CreativeEffect.GRADIENT_MIX,
            CreativeEffect.MOSAIC_MIX,
            CreativeEffect.RADIAL_MIX,
            CreativeEffect.WAVE_MIX
        )
        val selectedEffect = effects.random()

        when (selectedEffect) {
            CreativeEffect.GRADIENT_MIX -> gradientMix(canvas, firstScaled, secondScaled)
            CreativeEffect.MOSAIC_MIX -> mosaicMix(canvas, firstScaled, secondScaled)
            CreativeEffect.RADIAL_MIX -> radialMix(canvas, firstScaled, secondScaled)
            CreativeEffect.WAVE_MIX -> waveMix(canvas, firstScaled, secondScaled)
        }

        return resultBitmap
    }

    private enum class CreativeEffect {
        GRADIENT_MIX, MOSAIC_MIX, RADIAL_MIX, WAVE_MIX
    }

    private fun gradientMix(canvas: Canvas, first: Bitmap, second: Bitmap) {
        val width = first.width
        val height = first.height

        for (y in 0 until height) {
            val alpha = (255 * y / height).toInt()
            val paint = android.graphics.Paint().apply {
                this.alpha = 255 - alpha
            }
            val rect = android.graphics.Rect(0, y, width, y + 1)
            canvas.drawBitmap(first, rect, rect, paint)

            paint.alpha = alpha
            canvas.drawBitmap(second, rect, rect, paint)
        }
    }

    private fun mosaicMix(canvas: Canvas, first: Bitmap, second: Bitmap) {
        val width = first.width
        val height = first.height
        val blockSize = 20

        for (y in 0 until height step blockSize) {
            for (x in 0 until width step blockSize) {
                val useFirst = (x / blockSize + y / blockSize) % 2 == 0
                val bitmap = if (useFirst) first else second
                val rect = android.graphics.Rect(x, y,
                    min(x + blockSize, width),
                    min(y + blockSize, height))
                canvas.drawBitmap(bitmap, rect, rect, null)
            }
        }
    }

    private fun radialMix(canvas: Canvas, first: Bitmap, second: Bitmap) {
        val width = first.width
        val height = first.height
        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadius = sqrt((centerX * centerX + centerY * centerY).toDouble()).toFloat()

        canvas.drawBitmap(first, 0f, 0f, null)

        val pixels = IntArray(width * height)
        second.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val distance = sqrt(((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY)).toDouble()).toFloat()
                val alpha = (255 * (1 - distance / maxRadius)).toInt().coerceIn(0, 255)

                val pixel = pixels[y * width + x]
                val newPixel = (pixel and 0x00FFFFFF) or (alpha shl 24)
                pixels[y * width + x] = newPixel
            }
        }

        val maskedSecond = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        maskedSecond.setPixels(pixels, 0, width, 0, 0, width, height)
        canvas.drawBitmap(maskedSecond, 0f, 0f, null)
    }

    private fun waveMix(canvas: Canvas, first: Bitmap, second: Bitmap) {
        val width = first.width
        val height = first.height

        canvas.drawBitmap(first, 0f, 0f, null)

        val waveHeight = 20
        val waveLength = 40

        for (y in 0 until height) {
            val waveOffset = (waveHeight * sin(2 * PI * y / waveLength)).toInt()
            val alpha = (128 + 64 * sin(2 * PI * y / waveLength)).toInt().coerceIn(0, 255)

            val paint = android.graphics.Paint().apply {
                this.alpha = alpha
            }

            val srcRect = android.graphics.Rect(0, y, width, y + 1)
            val dstRect = android.graphics.Rect(waveOffset, y, width + waveOffset, y + 1)
            canvas.drawBitmap(second, srcRect, dstRect, paint)
        }
    }

}