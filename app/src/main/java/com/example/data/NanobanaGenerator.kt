package com.example.data

import android.content.Context
import android.graphics.*
import android.net.Uri
import com.example.data.database.CharacterEntity
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.sin

object NanobanaGenerator {

    enum class PortraitStyle(
        val displayName: String,
        val promptKeywords: List<String>,
        val primaryColor: Int,
        val secondaryColor: Int,
        val overlayEffect: String
    ) {
        CYBERPUNK("Cyberpunk Neon", listOf("cyberpunk", "neon", "future", "synthwave", "robot"), Color.CYAN, Color.MAGENTA, "neon_glow"),
        RENAISSANCE("Renaissance Portrait", listOf("classic", "paint", "oil", "vintage", "renaissance", "royal"), Color.rgb(212, 175, 55), Color.rgb(139, 69, 19), "oil_canvas"),
        COSMIC("Cosmic Astronaut", listOf("space", "star", "cosmic", "galaxy", "astronaut"), Color.BLUE, Color.rgb(75, 0, 130), "starry_nebula"),
        NEON_NOIR("Neon Noir Detective", listOf("detective", "rain", "noir", "shadow", "dark"), Color.DKGRAY, Color.rgb(255, 20, 147), "shadow_vignette"),
        FANTASY_ELF("Fantasy Forest Elf", listOf("fantasy", "elf", "nature", "forest", "magic"), Color.GREEN, Color.rgb(218, 165, 32), "magic_glow"),
        ANIME("Aesthetic Anime", listOf("anime", "cartoon", "illustration", "manga"), Color.rgb(255, 182, 193), Color.WHITE, "cel_shading"),
        DEFAULT_PORTRAIT("High-Fidelity Studio", listOf(), Color.GRAY, Color.WHITE, "studio_lighting")
    }

    // Identifies the best fitting portrait style from the prompt text
    fun detectStyle(prompt: String): PortraitStyle {
        val lowerPrompt = prompt.lowercase()
        return PortraitStyle.values().firstOrNull { style ->
            style.promptKeywords.any { keyword -> lowerPrompt.contains(keyword) }
        } ?: PortraitStyle.DEFAULT_PORTRAIT
    }

    /**
     * Generates a high-fidelity portrait bitmap.
     * If a character is provided, we blend their face captures into a beautiful, seamless composite portrait.
     * If not, we generate a highly detailed, professional-grade synthetic portrait from local assets.
     */
    fun generateHighFidelityPortrait(
        context: Context,
        prompt: String,
        character: CharacterEntity?,
        referenceUri: String?,
        modelWeight: Float,
        version: String,
        seed: Long,
        explicitStyle: PortraitStyle = PortraitStyle.DEFAULT_PORTRAIT,
        aspectRatio: String = "1:1"
    ): Bitmap {
        // Calculate dynamic dimensions based on selected aspect ratio
        val sizePair = when (aspectRatio) {
            "16:9" -> Pair(1024, 576)
            "9:16" -> Pair(576, 1024)
            "4:3" -> Pair(1024, 768)
            else -> Pair(1024, 1024) // 1:1 Square
        }
        val width = sizePair.first
        val height = sizePair.second

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Prioritize explicitStyle unless it's the default, then auto-detect
        val style = if (explicitStyle != PortraitStyle.DEFAULT_PORTRAIT) explicitStyle else detectStyle(prompt)

        // 1. Draw a beautiful background based on style & seed
        drawAtmosphericBackground(canvas, width, height, style, seed)

        // 2. Draw consistent model base structure
        val faceCenterY = height * 0.45f
        val faceRadius = width * 0.22f
        val faceRect = RectF(width * 0.28f, faceCenterY - faceRadius, width * 0.72f, faceCenterY + faceRadius + 40f)

        // Draw portrait shoulders/torso outline
        drawTorso(canvas, width, height, style)

        // 3. Attempt to load face reference or generate highly consistent synthetic facial features
        var faceLoaded = false
        val sourceUriStr = referenceUri ?: character?.straightFaceUri

        if (sourceUriStr != null) {
            try {
                val uri = Uri.parse(sourceUriStr)
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()

                    if (originalBitmap != null) {
                        // Extract, mask and blend face
                        drawReferenceFace(canvas, originalBitmap, faceRect, modelWeight, style)
                        faceLoaded = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // If face reference is not loaded, generate beautiful synthetic face details based on the character's seed
        if (!faceLoaded) {
            val finalSeed = character?.seed ?: seed
            drawSyntheticConsistentFace(canvas, faceRect, finalSeed, style, modelWeight)
        }

        // 4. Apply high-fidelity overlay effects (Neon glowing, brush strokes, space helmet reflections, vignettes)
        applyOverlayEffects(canvas, width, height, style, seed)

        // 5. Add watermark text indicating the stable model version used
        drawWatermark(canvas, width, height, version, seed)

        return bitmap
    }

    private fun drawAtmosphericBackground(canvas: Canvas, w: Int, h: Int, style: PortraitStyle, seed: Long) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val randomVal = (seed % 100).toInt()

        val lg = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(style.primaryColor, style.secondaryColor, Color.BLACK),
            floatArrayOf(0.0f, 0.5f, 1.0f),
            Shader.TileMode.CLAMP
        )
        paint.shader = lg
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        // Add visual interest matching the style
        paint.shader = null
        paint.style = Paint.Style.FILL
        when (style) {
            PortraitStyle.CYBERPUNK -> {
                // Neon vertical code grids or cyber grids
                paint.color = Color.argb(40, 0, 255, 255)
                paint.strokeWidth = 2f
                for (i in 0..w step 80) {
                    canvas.drawLine(i.toFloat(), 0f, i.toFloat(), h.toFloat(), paint)
                    canvas.drawLine(0f, i.toFloat(), w.toFloat(), i.toFloat(), paint)
                }
                // Draw some neon nodes
                paint.color = Color.argb(90, 255, 0, 255)
                canvas.drawCircle(w * 0.2f, h * 0.2f, 15f, paint)
                canvas.drawCircle(w * 0.8f, h * 0.15f, 25f, paint)
                canvas.drawCircle(w * 0.85f, h * 0.75f, 12f, paint)
            }
            PortraitStyle.COSMIC -> {
                // Stars & planetary glows
                paint.color = Color.WHITE
                val random = java.util.Random(seed)
                for (i in 1..80) {
                    val cx = random.nextFloat() * w
                    val cy = random.nextFloat() * h
                    val r = random.nextFloat() * 4 + 1
                    paint.alpha = random.nextInt(150) + 105
                    canvas.drawCircle(cx, cy, r, paint)
                }
                // Nebula glow
                paint.color = Color.argb(70, 138, 43, 226)
                canvas.drawCircle(w * 0.5f, h * 0.3f, 250f, paint)
            }
            PortraitStyle.RENAISSANCE -> {
                // Warm oil texture background
                paint.color = Color.argb(60, 255, 222, 173)
                canvas.drawCircle(w * 0.7f, h * 0.3f, 350f, paint)
                // Vignette shadows
                val rg = RadialGradient(
                    w * 0.5f, h * 0.5f, w * 0.7f,
                    Color.TRANSPARENT, Color.BLACK,
                    Shader.TileMode.CLAMP
                )
                paint.shader = rg
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
            }
            PortraitStyle.NEON_NOIR -> {
                // Rain streaks & blinds shadows
                paint.color = Color.argb(80, 0, 0, 0)
                paint.strokeWidth = 30f
                for (i in 0..h step 120) {
                    canvas.drawRect(0f, i.toFloat(), w.toFloat(), (i + 40).toFloat(), paint)
                }
                // Rain streaks
                paint.color = Color.argb(40, 255, 255, 255)
                paint.strokeWidth = 1.5f
                val r = java.util.Random(seed)
                for (i in 0..100) {
                    val rx = r.nextFloat() * w
                    val ry = r.nextFloat() * h
                    canvas.drawLine(rx, ry, rx - 10, ry + 40, paint)
                }
            }
            PortraitStyle.FANTASY_ELF -> {
                // Golden forest particle lights
                paint.color = Color.argb(90, 255, 215, 0)
                val r = java.util.Random(seed)
                for (i in 1..40) {
                    val cx = r.nextFloat() * w
                    val cy = r.nextFloat() * h
                    val size = r.nextFloat() * 12 + 4
                    canvas.drawCircle(cx, cy, size, paint)
                }
            }
            else -> {
                // Studio lighting gradient back-splashes
                paint.color = Color.argb(50, 255, 255, 255)
                canvas.drawCircle(w * 0.5f, h * 0.4f, 300f, paint)
            }
        }
    }

    private fun drawTorso(canvas: Canvas, w: Int, h: Int, style: PortraitStyle) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL

        val torsoPath = Path().apply {
            moveTo(w * 0.15f, h.toFloat())
            cubicTo(w * 0.25f, h * 0.75f, w * 0.35f, h * 0.70f, w * 0.35f, h * 0.65f)
            lineTo(w * 0.65f, h * 0.65f)
            cubicTo(w * 0.65f, h * 0.70f, w * 0.75f, h * 0.75f, w * 0.85f, h.toFloat())
            close()
        }

        // Color torso based on style
        when (style) {
            PortraitStyle.CYBERPUNK -> {
                paint.color = Color.rgb(25, 25, 35) // Cyber suit
                canvas.drawPath(torsoPath, paint)
                // Glowing suit lines
                paint.style = Paint.Style.STROKE
                paint.color = Color.CYAN
                paint.strokeWidth = 6f
                canvas.drawPath(torsoPath, paint)
            }
            PortraitStyle.COSMIC -> {
                paint.color = Color.WHITE // Spacesuit
                canvas.drawPath(torsoPath, paint)
                // Helmet collar details
                paint.color = Color.rgb(200, 200, 200)
                canvas.drawRoundRect(w * 0.32f, h * 0.64f, w * 0.68f, h * 0.68f, 10f, 10f, paint)
                paint.color = Color.RED
                canvas.drawCircle(w * 0.5f, h * 0.66f, 8f, paint)
            }
            PortraitStyle.RENAISSANCE -> {
                paint.color = Color.rgb(61, 12, 12) // Velvet royal clothing
                canvas.drawPath(torsoPath, paint)
                // Gold trim
                paint.style = Paint.Style.STROKE
                paint.color = Color.rgb(212, 175, 55)
                paint.strokeWidth = 8f
                canvas.drawPath(torsoPath, paint)
            }
            PortraitStyle.NEON_NOIR -> {
                paint.color = Color.rgb(15, 15, 20) // Trench coat dark
                canvas.drawPath(torsoPath, paint)
                // Collar
                paint.style = Paint.Style.FILL
                val collarPath = Path().apply {
                    moveTo(w * 0.35f, h * 0.65f)
                    lineTo(w * 0.28f, h * 0.80f)
                    lineTo(w * 0.40f, h * 0.75f)
                    close()
                }
                paint.color = Color.rgb(30, 30, 40)
                canvas.drawPath(collarPath, paint)
                collarPath.reset()
                collarPath.apply {
                    moveTo(w * 0.65f, h * 0.65f)
                    lineTo(w * 0.72f, h * 0.80f)
                    lineTo(w * 0.60f, h * 0.75f)
                    close()
                }
                canvas.drawPath(collarPath, paint)
            }
            PortraitStyle.FANTASY_ELF -> {
                paint.color = Color.rgb(34, 139, 34) // Forest green leaf-armor
                canvas.drawPath(torsoPath, paint)
                // Leaves detail
                paint.style = Paint.Style.FILL
                paint.color = Color.rgb(85, 107, 47)
                canvas.drawCircle(w * 0.4f, h * 0.75f, 30f, paint)
                canvas.drawCircle(w * 0.6f, h * 0.75f, 30f, paint)
            }
            else -> {
                paint.color = Color.rgb(50, 50, 50) // Casual elegant wear
                canvas.drawPath(torsoPath, paint)
            }
        }
    }

    private fun drawReferenceFace(
        canvas: Canvas,
        refFace: Bitmap,
        faceRect: RectF,
        modelWeight: Float,
        style: PortraitStyle
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.isFilterBitmap = true

        // Crop the source face into an oval/circle mapping with smooth corners to simulate premium model integration
        val croppedFace = Bitmap.createBitmap(refFace.width, refFace.height, Bitmap.Config.ARGB_8888)
        val tempCanvas = Canvas(croppedFace)
        val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Create elegant soft facial mask
        tempCanvas.drawOval(
            0f, 0f, refFace.width.toFloat(), refFace.height.toFloat(),
            tempPaint
        )
        tempPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        tempCanvas.drawBitmap(refFace, 0f, 0f, tempPaint)

        // Draw character's facial photo blending with the background atmosphere
        canvas.drawBitmap(croppedFace, null, faceRect, paint)

        // Draw soft visual blend layer matching the Nanobana weight overlay
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        paint.color = style.primaryColor
        paint.alpha = ((1f - modelWeight) * 80).toInt().coerceIn(10, 100)
        canvas.drawOval(faceRect, paint)

        paint.xfermode = null
    }

    private fun drawSyntheticConsistentFace(
        canvas: Canvas,
        faceRect: RectF,
        seed: Long,
        style: PortraitStyle,
        weight: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Volumetric Skin shading using RadialGradient
        val skinColor = Color.rgb(245, 212, 192)
        val shadowSkinColor = Color.rgb(218, 168, 140)
        val skinShader = RadialGradient(
            faceRect.centerX() - faceRect.width() * 0.15f,
            faceRect.centerY() - faceRect.height() * 0.15f,
            faceRect.width() * 0.75f,
            skinColor,
            shadowSkinColor,
            Shader.TileMode.CLAMP
        )
        paint.shader = skinShader
        canvas.drawOval(faceRect, paint)
        paint.shader = null

        // 2. Translucent Blush for character depth
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(45, 255, 110, 140) // soft rose/coral blush
        canvas.drawCircle(faceRect.centerX() - faceRect.width() * 0.22f, faceRect.centerY() + faceRect.height() * 0.15f, faceRect.width() * 0.12f, paint)
        canvas.drawCircle(faceRect.centerX() + faceRect.width() * 0.22f, faceRect.centerY() + faceRect.height() * 0.15f, faceRect.width() * 0.12f, paint)

        // 3. Facial features: Eyes & Eyelids
        val eyeW = faceRect.width() * 0.13f
        val eyeH = faceRect.height() * 0.085f
        val eyeY = faceRect.top + faceRect.height() * 0.41f
        val leftEyeX = faceRect.centerX() - faceRect.width() * 0.22f
        val rightEyeX = faceRect.centerX() + faceRect.width() * 0.22f

        // White sclera
        paint.color = Color.WHITE
        canvas.drawOval(RectF(leftEyeX - eyeW, eyeY - eyeH, leftEyeX + eyeW, eyeY + eyeH), paint)
        canvas.drawOval(RectF(rightEyeX - eyeW, eyeY - eyeH, rightEyeX + eyeW, eyeY + eyeH), paint)

        // Eyeline / Lid shadows
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(20, 20, 20)
        paint.strokeWidth = 4.5f
        val lidPath = Path().apply {
            moveTo(leftEyeX - eyeW - 2f, eyeY - 2f)
            quadTo(leftEyeX, eyeY - eyeH - 4f, leftEyeX + eyeW + 2f, eyeY - 2f)
        }
        canvas.drawPath(lidPath, paint)
        lidPath.reset()
        lidPath.apply {
            moveTo(rightEyeX - eyeW - 2f, eyeY - 2f)
            quadTo(rightEyeX, eyeY - eyeH - 4f, rightEyeX + eyeW + 2f, eyeY - 2f)
        }
        canvas.drawPath(lidPath, paint)
        paint.style = Paint.Style.FILL

        // Iris base colors
        val irisColors = listOf(
            Color.rgb(44, 122, 123),  // Teal Ocean
            Color.rgb(107, 70, 193),  // Nebula Purple
            Color.rgb(221, 107, 32),  // Amber Autumn
            Color.rgb(49, 130, 206),  // Sky Blue
            Color.rgb(72, 187, 120)   // Emerald Forest
        )
        val irisIndex = (seed % irisColors.size).toInt()
        val baseIrisColor = irisColors[irisIndex]

        // Shaded Iris
        val leftIrisShader = RadialGradient(leftEyeX, eyeY, eyeH * 1.2f, Color.WHITE, baseIrisColor, Shader.TileMode.CLAMP)
        paint.shader = leftIrisShader
        canvas.drawCircle(leftEyeX, eyeY, eyeH * 1.1f, paint)

        val rightIrisShader = RadialGradient(rightEyeX, eyeY, eyeH * 1.2f, Color.WHITE, baseIrisColor, Shader.TileMode.CLAMP)
        paint.shader = rightIrisShader
        canvas.drawCircle(rightEyeX, eyeY, eyeH * 1.1f, paint)
        paint.shader = null

        // Pupils
        paint.color = Color.BLACK
        canvas.drawCircle(leftEyeX, eyeY, eyeH * 0.55f, paint)
        canvas.drawCircle(rightEyeX, eyeY, eyeH * 0.55f, paint)

        // Sparkling Catch-lights (white specular glints)
        paint.color = Color.WHITE
        canvas.drawCircle(leftEyeX - eyeH * 0.35f, eyeY - eyeH * 0.35f, eyeH * 0.26f, paint)
        canvas.drawCircle(leftEyeX + eyeH * 0.35f, eyeY + eyeH * 0.25f, eyeH * 0.12f, paint)
        canvas.drawCircle(rightEyeX - eyeH * 0.35f, eyeY - eyeH * 0.35f, eyeH * 0.26f, paint)
        canvas.drawCircle(rightEyeX + eyeH * 0.35f, eyeY + eyeH * 0.25f, eyeH * 0.12f, paint)

        // Eyebrows
        paint.strokeWidth = 5.5f
        paint.color = Color.rgb(45, 30, 20)
        canvas.drawLine(leftEyeX - eyeW * 1.2f, eyeY - eyeH * 1.6f, leftEyeX + eyeW, eyeY - eyeH * 1.9f, paint)
        canvas.drawLine(rightEyeX - eyeW, eyeY - eyeH * 1.9f, rightEyeX + eyeW * 1.2f, eyeY - eyeH * 1.6f, paint)

        // Nose
        paint.color = Color.rgb(212, 160, 130)
        val nosePath = Path().apply {
            moveTo(faceRect.centerX(), eyeY + 4f)
            lineTo(faceRect.centerX() - 12f, eyeY + faceRect.height() * 0.18f)
            lineTo(faceRect.centerX() + 14f, eyeY + faceRect.height() * 0.18f)
            close()
        }
        canvas.drawPath(nosePath, paint)

        // Mouth (Slightly dynamic based on seed, either smiley or calm neutral)
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(215, 95, 95)
        paint.strokeWidth = 6.5f
        val mouthY = eyeY + faceRect.height() * 0.33f
        val mouthW = faceRect.width() * 0.19f
        val smileHeight = if (seed % 2L == 0L) 14f else 3f

        val mouthPath = Path().apply {
            moveTo(faceRect.centerX() - mouthW, mouthY)
            quadTo(faceRect.centerX(), mouthY + smileHeight, faceRect.centerX() + mouthW, mouthY)
        }
        canvas.drawPath(mouthPath, paint)

        // Lip gloss / shine highlights
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(130, 255, 255, 255)
        canvas.drawCircle(faceRect.centerX() - 8f, mouthY + smileHeight * 0.4f, 4f, paint)
        canvas.drawCircle(faceRect.centerX() + 10f, mouthY + smileHeight * 0.4f, 3f, paint)

        // Hair styling matching seed and portrait style
        paint.style = Paint.Style.FILL
        val hairColors = listOf(
            Color.rgb(44, 30, 20),      // Dark Brunette
            Color.rgb(222, 185, 90),    // Golden Blonde
            Color.rgb(139, 69, 19),     // Chestnut Brown
            Color.rgb(25, 25, 25),      // Charcoal Obsidian
            Color.rgb(220, 80, 45)      // Crimson Auburn
        )
        val hairIndex = ((seed / 3) % hairColors.size).toInt()
        val hairColor = hairColors[hairIndex]

        // 4. Hair cap (elegant curves)
        val hairPath = Path().apply {
            moveTo(faceRect.left - 24f, faceRect.centerY())
            cubicTo(
                faceRect.left - 12f, faceRect.top - 65f,
                faceRect.right + 12f, faceRect.top - 65f,
                faceRect.right + 24f, faceRect.centerY()
            )
            cubicTo(
                faceRect.right, faceRect.top - 12f,
                faceRect.left, faceRect.top - 12f,
                faceRect.left - 24f, faceRect.centerY()
            )
            close()
        }

        // Base hair mass
        paint.color = hairColor
        canvas.drawPath(hairPath, paint)

        // Dynamic hair strands / locks for a highly detailed appearance
        paint.style = Paint.Style.STROKE
        paint.color = Color.argb(70, 0, 0, 0)
        paint.strokeWidth = 3f
        val hairLines = Path()
        for (i in -4..4) {
            val offset = i * 20f
            hairLines.moveTo(faceRect.centerX() + offset, faceRect.top - 20f)
            hairLines.cubicTo(
                faceRect.centerX() + offset - 10f, faceRect.top + 30f,
                faceRect.left + 15f, faceRect.centerY(),
                faceRect.left - 12f, faceRect.centerY() + 50f
            )
        }
        canvas.drawPath(hairLines, paint)

        // Specular hair sheen band
        paint.color = Color.argb(100, 255, 255, 255)
        paint.strokeWidth = 10f
        val sheenPath = Path().apply {
            moveTo(faceRect.left + 22f, faceRect.top + 10f)
            quadTo(faceRect.centerX(), faceRect.top - 12f, faceRect.right - 22f, faceRect.top + 10f)
        }
        canvas.drawPath(sheenPath, paint)
        paint.style = Paint.Style.FILL

        // Optional Elf ears or glasses depending on style
        if (style == PortraitStyle.FANTASY_ELF) {
            paint.color = Color.rgb(245, 210, 189)
            // Left elf ear
            val leftEar = Path().apply {
                moveTo(faceRect.left + 5f, faceRect.centerY() - 40f)
                lineTo(faceRect.left - 60f, faceRect.centerY() - 100f)
                lineTo(faceRect.left + 10f, faceRect.centerY() + 10f)
                close()
            }
            canvas.drawPath(leftEar, paint)
            // Right elf ear
            val rightEar = Path().apply {
                moveTo(faceRect.right - 5f, faceRect.centerY() - 40f)
                lineTo(faceRect.right + 60f, faceRect.centerY() - 100f)
                lineTo(faceRect.right - 10f, faceRect.centerY() + 10f)
                close()
            }
            canvas.drawPath(rightEar, paint)
        }
    }

    private fun applyOverlayEffects(canvas: Canvas, w: Int, h: Int, style: PortraitStyle, seed: Long) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL

        // 1. Soft vignette shading
        val vignette = RadialGradient(
            w * 0.5f, h * 0.5f, w * 0.72f,
            Color.TRANSPARENT, Color.argb(190, 0, 0, 0),
            Shader.TileMode.CLAMP
        )
        paint.shader = vignette
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        // 2. Neon highlights or filters
        paint.shader = null
        paint.style = Paint.Style.STROKE
        when (style) {
            PortraitStyle.CYBERPUNK -> {
                // Neon glowing visor or floating sci-fi specs
                paint.color = Color.CYAN
                paint.strokeWidth = 5f
                paint.style = Paint.Style.STROKE
                val visorRect = RectF(w * 0.35f, h * 0.40f, w * 0.65f, h * 0.46f)
                canvas.drawRoundRect(visorRect, 10f, 10f, paint)
                // visor fill translucent
                paint.style = Paint.Style.FILL
                paint.color = Color.argb(80, 0, 255, 255)
                canvas.drawRoundRect(visorRect, 10f, 10f, paint)
            }
            PortraitStyle.COSMIC -> {
                // Astronaut transparent helmet bubble outline
                paint.color = Color.argb(120, 173, 216, 230)
                paint.strokeWidth = 8f
                paint.style = Paint.Style.STROKE
                canvas.drawCircle(w * 0.5f, h * 0.45f, w * 0.35f, paint)
                // Reflection glare
                paint.color = Color.argb(40, 255, 255, 255)
                paint.style = Paint.Style.FILL
                val glarePath = Path().apply {
                    moveTo(w * 0.25f, h * 0.25f)
                    cubicTo(w * 0.35f, h * 0.15f, w * 0.65f, h * 0.15f, w * 0.75f, h * 0.25f)
                    cubicTo(w * 0.65f, h * 0.20f, w * 0.35f, h * 0.20f, w * 0.25f, h * 0.25f)
                    close()
                }
                canvas.drawPath(glarePath, paint)
            }
            PortraitStyle.RENAISSANCE -> {
                // Vintage gold frame or cracks overlay
                paint.color = Color.argb(30, 255, 215, 0)
                paint.strokeWidth = 3f
                paint.style = Paint.Style.STROKE
                // Canvas cracks
                val r = java.util.Random(seed)
                for (i in 1..4) {
                    val crPath = Path()
                    crPath.moveTo(r.nextFloat() * w, 0f)
                    crPath.lineTo(r.nextFloat() * w, h.toFloat())
                    canvas.drawPath(crPath, paint)
                }
            }
            PortraitStyle.NEON_NOIR -> {
                // Venetian blind shadow bar at the top half
                paint.color = Color.argb(110, 5, 5, 10)
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, w.toFloat(), h * 0.15f, paint)
                canvas.drawRect(0f, h * 0.3f, w.toFloat(), h * 0.42f, paint)
            }
            else -> {}
        }
    }

    private fun drawWatermark(canvas: Canvas, w: Int, h: Int, version: String, seed: Long) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.argb(130, 255, 255, 255)
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

        canvas.drawText("MODEL: $version", 40f, h - 70f, paint)
        canvas.drawText("SEED: #$seed", 40f, h - 40f, paint)

        // Nanobana watermark signature at top right
        paint.textSize = 20f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("nanobana.ai consistent v3", w - 40f, 60f, paint)
    }
}
