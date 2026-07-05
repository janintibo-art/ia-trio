package com.moi.iatrio

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * LE CRÉATEUR ✨ : transforme un texte en image, musique ou code — 100% local.
 *
 * Le texte devient une GRAINE : le même texte redonne toujours la même œuvre,
 * un texte différent donne une œuvre différente. Palette, formes, gamme,
 * tempo, mélodie... tout découle des mots.
 */
class Creator {

    // ==================== IMAGE ====================
    /**
     * Art génératif : palette harmonique dérivée du texte, formes en couches
     * (anneaux, arcs, triangles, traits), symétrie éventuelle, grain final.
     */
    fun makeImage(
        prompt: String,
        size: Int = 768,
        learned: List<Pair<String, DoubleArray>> = emptyList()   // palettes apprises (label, 192 RGB)
    ): Bitmap {
        val seed = prompt.trim().lowercase().hashCode().toLong()
        val rnd = Random(seed)
        val hueBase = ((seed % 360 + 360) % 360).toFloat()

        // Couleurs réelles apprises : on extrait les teintes dominantes des souvenirs
        val learnedColors = ArrayList<Int>()
        for ((_, avg) in learned) {
            for (i in 0 until 64) {
                val r = (avg[i * 3] * 255).toInt().coerceIn(0, 255)
                val g = (avg[i * 3 + 1] * 255).toInt().coerceIn(0, 255)
                val b = (avg[i * 3 + 2] * 255).toInt().coerceIn(0, 255)
                learnedColors.add(Color.rgb(r, g, b))
            }
        }

        fun col(hueOff: Float, s: Float, v: Float, a: Int): Int {
            // Si l'IA connaît le sujet, elle peint avec SES couleurs
            if (learnedColors.isNotEmpty() && rnd.nextInt(100) < 70) {
                val c = learnedColors[rnd.nextInt(learnedColors.size)]
                return Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))
            }
            val hsv = floatArrayOf((hueBase + hueOff + 360f) % 360f, s, v)
            val c = Color.HSVToColor(hsv)
            return Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))
        }

        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Fond : dégradé sombre de la teinte de base
        paint.shader = LinearGradient(0f, 0f, 0f, size.toFloat(),
            col(0f, 0.55f, 0.16f, 255), col(30f, 0.65f, 0.05f, 255), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        paint.shader = null

        // Mosaïque fantôme : l'IA peint en fond le souvenir 8x8 de ce qu'elle connaît
        if (learned.isNotEmpty()) {
            val (_, avg) = learned[rnd.nextInt(learned.size)]
            val cell = size / 8f
            paint.style = Paint.Style.FILL
            for (gy in 0 until 8) for (gx in 0 until 8) {
                val i = (gy * 8 + gx) * 3
                val r = (avg[i] * 255).toInt().coerceIn(0, 255)
                val g = (avg[i + 1] * 255).toInt().coerceIn(0, 255)
                val b = (avg[i + 2] * 255).toInt().coerceIn(0, 255)
                paint.color = Color.argb(70, r, g, b)
                canvas.drawRoundRect(gx * cell + 4, gy * cell + 4,
                    (gx + 1) * cell - 4, (gy + 1) * cell - 4, cell / 4, cell / 4, paint)
            }
        }

        val mirror = rnd.nextBoolean()
        val palette = listOf(0f, 25f, 160f, 190f, 330f)   // harmonies autour de la base
        val nShapes = 45 + rnd.nextInt(40)

        fun drawShape(x: Float, y: Float) {
            val hue = palette[rnd.nextInt(palette.size)]
            val a = 40 + rnd.nextInt(140)
            paint.color = col(hue, 0.55f + rnd.nextFloat() * 0.4f, 0.6f + rnd.nextFloat() * 0.4f, a)
            when (rnd.nextInt(5)) {
                0 -> { paint.style = Paint.Style.FILL
                    canvas.drawCircle(x, y, 8f + rnd.nextFloat() * size / 7f, paint) }
                1 -> { paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f + rnd.nextFloat() * 14f
                    canvas.drawCircle(x, y, 20f + rnd.nextFloat() * size / 4f, paint) }
                2 -> { paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 3f + rnd.nextFloat() * 10f
                    val r = 30f + rnd.nextFloat() * size / 3f
                    canvas.drawArc(x - r, y - r, x + r, y + r,
                        rnd.nextFloat() * 360f, 60f + rnd.nextFloat() * 200f, false, paint) }
                3 -> { paint.style = Paint.Style.FILL
                    val r = 15f + rnd.nextFloat() * size / 8f
                    val p = Path()
                    p.moveTo(x, y - r); p.lineTo(x - r, y + r); p.lineTo(x + r, y + r); p.close()
                    canvas.save(); canvas.rotate(rnd.nextFloat() * 360f, x, y)
                    canvas.drawPath(p, paint); canvas.restore() }
                else -> { paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1.5f + rnd.nextFloat() * 6f
                    canvas.drawLine(x, y, x + (rnd.nextFloat() - 0.5f) * size * 0.7f,
                        y + (rnd.nextFloat() - 0.5f) * size * 0.7f, paint) }
            }
        }

        repeat(nShapes) {
            val x = rnd.nextFloat() * size
            val y = rnd.nextFloat() * size
            drawShape(x, y)
            if (mirror) {
                // redessiner la même forme en miroir avec le même hasard :
                // on triche en re-piochant, l'effet reste harmonieux
                drawShape(size - x, y)
            }
        }

        // Halo central : un "soleil" dont la taille dépend du nombre de mots
        val words = prompt.trim().split(Regex("\\s+")).size
        paint.style = Paint.Style.FILL
        for (i in 6 downTo 1) {
            paint.color = col(15f, 0.5f, 1.0f, 10 + i * 6)
            canvas.drawCircle(size / 2f, size / 2f, (40f + words * 8f) * i / 2f, paint)
        }

        // Grain léger
        paint.style = Paint.Style.FILL
        repeat(size) {
            paint.color = Color.argb(rnd.nextInt(28), 255, 255, 255)
            canvas.drawCircle(rnd.nextFloat() * size, rnd.nextFloat() * size, 1f, paint)
        }
        return bmp
    }

    // ==================== COLLAGE : TES vraies photos, structurées ====================
    /**
     * Le collagiste discipliné — même recette que le remix musical :
     * 1. RÔLES : la photo la plus douce = FOND, la plus colorée = POINT FOCAL,
     *    les autres = TUILES de la mosaïque.
     * 2. MOTIF : un agencement de grille composé une fois puis RÉPÉTÉ
     *    (rangées décalées façon briques, respirations).
     * 3. HARMONIE : toutes les tuiles sont teintées vers la palette d'une
     *    image « tonique » — l'équivalent visuel de la transposition :
     *    c'est ce qui unifie le collage au lieu d'un patchwork qui jure.
     */
    fun makeCollage(
        prompt: String,
        images: List<Bitmap>,
        creativity: Int = 50,
        thought: String = "",
        size: Int = 1024
    ): Bitmap {
        val seed = prompt.trim().lowercase().hashCode().toLong()
        val rnd = Random(seed)

        // ===== ANALYSE : rôles par douceur / couleur =====
        fun stats(b: Bitmap): Pair<Double, Double> {   // (saturation moyenne, détail)
            var sat = 0.0; var det = 0.0; var prev = 0.0; var n = 0
            val hsv = FloatArray(3)
            val step = (b.width / 24).coerceAtLeast(1)
            var y = 0
            while (y < b.height) {
                var x = 0
                while (x < b.width) {
                    val c = b.getPixel(x, y)
                    Color.colorToHSV(c, hsv)
                    sat += hsv[1]
                    val lum = 0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)
                    det += kotlin.math.abs(lum - prev); prev = lum
                    n++; x += step
                }
                y += step
            }
            return Pair(sat / n, det / n)
        }
        val analyzed = images.map { Pair(it, stats(it)) }
        val background = analyzed.minByOrNull { it.second.second }!!.first     // la plus douce
        val focal = analyzed.maxByOrNull { it.second.first }!!.first           // la plus colorée
        val tiles = analyzed.map { it.first }.filter { it !== background }.ifEmpty { images }

        // ===== LA TONIQUE : palette de l'image focale =====
        fun keyColors(b: Bitmap): List<Int> {
            val out = ArrayList<Int>()
            for (qy in 0 until 2) for (qx in 0 until 2) {
                var r = 0L; var g = 0L; var bl = 0L; var n = 0
                var y = qy * b.height / 2
                while (y < (qy + 1) * b.height / 2) {
                    var x = qx * b.width / 2
                    while (x < (qx + 1) * b.width / 2) {
                        val c = b.getPixel(x, y)
                        r += Color.red(c); g += Color.green(c); bl += Color.blue(c); n++
                        x += 8
                    }
                    y += 8
                }
                out.add(Color.rgb((r / n).toInt(), (g / n).toInt(), (bl / n).toInt()))
            }
            return out
        }
        val keys = keyColors(focal)
        fun key(i: Int) = keys[i % keys.size]
        fun withAlpha(c: Int, a: Int) = Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))

        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun drawCover(b: Bitmap, dst: RectF) {   // recadrage centré qui remplit
            val side = minOf(b.width, b.height)
            val sx = (b.width - side) / 2; val sy = (b.height - side) / 2
            canvas.drawBitmap(b, Rect(sx, sy, sx + side, sy + side), dst, paint)
        }

        // ===== 1. LE FOND : ta photo la plus douce, assombrie et teintée =====
        drawCover(background, RectF(0f, 0f, size.toFloat(), size.toFloat()))
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(120, 10, 10, 18)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        paint.color = withAlpha(key(0), 40)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        // ===== 2. LE MOTIF : une rangée composée, puis répétée (briques) =====
        val gs = 5 + (creativity / 40)                 // 5 à 7 colonnes
        val cell = size.toFloat() / gs
        val song = thought.filter { it.isLetter() }
        var ci = 0
        // motif d'une rangée : (colonne, index de tuile, largeur en cellules)
        val motif = ArrayList<Triple<Int, Int, Int>>()
        var col = 0
        while (col < gs) {
            if (rnd.nextInt(100) < 78) {               // respirations : cases vides
                val ch = if (song.length > 4) song[ci++ % song.length].code else rnd.nextInt(97, 123)
                val span = if (creativity > 55 && rnd.nextInt(100) < 22) 2 else 1
                motif.add(Triple(col, ch % tiles.size, span.coerceAtMost(gs - col)))
                col += span
            } else col++
        }
        val rows = gs
        val margin = cell * 0.045f
        for (row in 1 until rows - 1) {                // on laisse respirer haut et bas
            val brick = if (row % 2 == 1) cell / 2 else 0f    // décalage façon briques
            val variation = row % 3 == 2
            for ((c0, tIdx0, span) in motif) {
                val tIdx = if (variation) (tIdx0 + 1) % tiles.size else tIdx0
                val x = c0 * cell + brick
                if (x + span * cell > size + cell / 2) continue
                val dst = RectF(x + margin, row * cell + margin,
                    x + span * cell - margin, (row + 1) * cell - margin)
                drawCover(tiles[tIdx % tiles.size], dst)
                // HARMONISATION : voile de la couleur tonique sur chaque tuile
                paint.color = withAlpha(key(row + c0), 46)
                canvas.drawRect(dst, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = size / 340f
                paint.color = withAlpha(key(0), 130)
                canvas.drawRect(dst, paint)
                paint.style = Paint.Style.FILL
            }
        }

        // ===== 3. LE POINT FOCAL : ta photo la plus vivante, en médaillon =====
        val fx = size * (if (rnd.nextBoolean()) 0.62f else 0.38f)
        val fy = size * 0.42f
        val fr = size * (0.16f + creativity / 900f)
        val clip = Path().apply { addCircle(fx, fy, fr, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(clip)
        drawCover(focal, RectF(fx - fr, fy - fr, fx + fr, fy + fr))
        canvas.restore()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size / 90f
        paint.color = withAlpha(key(0), 235)
        canvas.drawCircle(fx, fy, fr, paint)
        paint.strokeWidth = size / 240f
        paint.color = Color.argb(160, 255, 255, 255)
        canvas.drawCircle(fx, fy, fr + size / 60f, paint)
        paint.style = Paint.Style.FILL

        // ===== 4. FINITIONS : vignettage + grain =====
        for (i in 5 downTo 1) {
            paint.color = Color.argb(6 + i * 4, 0, 0, 0)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = size * 0.02f * i
            canvas.drawRect(paint.strokeWidth / 2, paint.strokeWidth / 2,
                size - paint.strokeWidth / 2, size - paint.strokeWidth / 2, paint)
        }
        paint.style = Paint.Style.FILL
        repeat(size / 2) {
            paint.color = Color.argb(rnd.nextInt(22), 255, 255, 255)
            canvas.drawCircle(rnd.nextFloat() * size, rnd.nextFloat() * size, 1f, paint)
        }
        return bmp
    }

    // ==================== MUSIQUE ====================
    data class NoteEvent(val midi: Int, val startSec: Double, val durSec: Double, val vel: Int, val ch: Int)
    var lastEvents: List<NoteEvent> = emptyList()   // la partition de la dernière composition
    var lastBpm = 100
    var lastRate = 44100   // taux de la dernière création (remix = 16000)

    private var track: AudioTrack? = null
    private val rate = 44100

    /**
     * Composition algorithmique : le texte choisit la gamme, le tempo, la
     * racine et la mélodie (marche aléatoire sur la gamme) + une basse.
     */
    fun makeMusic(
        prompt: String,
        thought: String = "",
        timbres: List<Pair<String, DoubleArray>> = emptyList(),
        creativity: Int = 50,
        bpmOverride: Int = 0,      // 0 = automatique
        barsCount: Int = 8
    ): ShortArray {
        val seed = prompt.trim().lowercase().hashCode().toLong()
        val rnd = Random(seed)
        val scales = listOf(
            listOf(0, 2, 4, 5, 7, 9, 11), listOf(0, 2, 3, 5, 7, 8, 10),
            listOf(0, 3, 5, 7, 10), listOf(0, 2, 3, 6, 7, 8, 11)
        )
        val scale = scales[rnd.nextInt(scales.size)]
        val ns = scale.size

        // ===== SONORITÉ DE TA MUSIQUE =====
        val tb: DoubleArray? = if (timbres.isEmpty()) null else {
            val avg = DoubleArray(32)
            for ((_, t) in timbres) for (i in 0 until 32) avg[i] += t[i] / timbres.size
            val mx = avg.maxOrNull() ?: 1.0
            if (mx > 0) for (i in avg.indices) avg[i] /= mx
            avg
        }
        val centroid = tb?.let { t ->
            var num = 0.0; var den = 1e-9
            for (i in t.indices) { num += i * t[i]; den += t[i] }
            num / den
        } ?: 10.0
        val harm = DoubleArray(6) { k ->
            val v = tb?.get(((k + 1) * 3).coerceAtMost(31)) ?: when (k) {
                0 -> 0.6; 1 -> 0.25; 2 -> 0.1; else -> 0.0
            }
            v.coerceAtLeast(if (k == 0) 0.35 else 0.0)
        }
        run { val s = harm.sum(); if (s > 0) for (i in harm.indices) harm[i] = harm[i] / s }

        val rootMul = (0.7 + centroid / 16.0).coerceIn(0.7, 2.0)
        val root = 220.0 * 2.0.pow(rnd.nextInt(8) / 12.0) * rootMul
        val bpm = if (bpmOverride > 0) bpmOverride
                  else (84 + rnd.nextInt(56) + ((centroid - 10) * 3).toInt()).coerceIn(70, 160)
        val drumVol = tb?.let { 0.25 + (it[20] + it[25] + it[30]) / 3.0 * 0.5 } ?: 0.4
        val bright = (0.6 + centroid / 20.0)

        val bars = barsCount.coerceIn(2, 64)
        val slotsPerBar = 8
        val slotDur = 60.0 / bpm / 2.0
        val n = (rate * slotDur * bars * slotsPerBar).toInt() + (rate * 2.5).toInt()
        val outL = DoubleArray(n); val outR = DoubleArray(n)
        val revL = DoubleArray(n); val revR = DoubleArray(n)   // bus de réverbération

        val events = ArrayList<NoteEvent>()
        fun jitter(): Int = ((rnd.nextDouble() - 0.5) * rate * 0.006).toInt()   // humanisation

        fun mixAt(idx: Int, s: Double, pan: Double, rev: Double) {
            if (idx < 0 || idx >= n) return
            outL[idx] += s * (1.0 - pan)
            outR[idx] += s * pan
            if (rev > 0) { revL[idx] += s * rev * (1.0 - pan); revR[idx] += s * rev * pan }
        }

        // ===== SYNTHÉ mélodie : double oscillo désaccordé + filtre passe-bas + vibrato =====
        fun addLead(freq: Double, start: Int, durS: Double, vol: Double, pan: Double = 0.58, ch: Int = 0) {
            val midi = (69 + 12 * ln(freq / 440.0) / ln(2.0)).roundToInt().coerceIn(0, 127)
            events.add(NoteEvent(midi, start.toDouble().coerceAtLeast(0.0) / rate, durS, (vol * 220).toInt().coerceIn(30, 120), ch))
            val len = (rate * durS).toInt()
            var lp = 0.0
            for (i in 0 until len) {
                val idx = start + i; if (idx >= n) break
                val t = i.toDouble() / rate
                val vib = 1.0 + 0.005 * sin(2 * PI * 5.3 * t) * kotlin.math.min(1.0, t * 3)
                val fA = freq * vib * 1.004; val fB = freq * vib * 0.996
                var raw = 0.0
                for (k in harm.indices) {
                    if (harm[k] < 0.02) continue
                    raw += (sin(2 * PI * fA * (k + 1) * t) + sin(2 * PI * fB * (k + 1) * t)) * 0.5 * harm[k]
                }
                val cut = (700 + 5200 * exp(-t * 5.0)) * bright
                val alpha = 1 - exp(-2 * PI * cut / rate)
                lp += alpha * (raw - lp)
                val env = (1 - exp(-t * 90)) * exp(-t * 2.4)
                mixAt(idx, lp * env * vol, pan, 0.30)
            }
        }

        // ===== NAPPE d'accords : attaque lente, large, très réverbérée =====
        fun addPad(freq: Double, start: Int, durS: Double, vol: Double, pan: Double) {
            val midi = (69 + 12 * ln(freq / 440.0) / ln(2.0)).roundToInt().coerceIn(0, 127)
            events.add(NoteEvent(midi, start.toDouble() / rate, durS, (vol * 400).toInt().coerceIn(20, 90), 2))
            val len = (rate * durS).toInt()
            for (i in 0 until len) {
                val idx = start + i; if (idx >= n) break
                val t = i.toDouble() / rate
                val s = sin(2 * PI * freq * t) * 0.5 + sin(2 * PI * freq * 2.0 * t) * 0.2 +
                        sin(2 * PI * freq * 0.5 * t) * 0.2
                val env = (1 - exp(-t * 3.0)) * exp(-t * 0.35)
                mixAt(idx, s * env * vol, pan, 0.5)
            }
        }

        // ===== BASSE : sinus + sous-harmonique, légère saturation =====
        fun addBass(freq: Double, start: Int, durS: Double, vol: Double) {
            val midi = (69 + 12 * ln(freq / 440.0) / ln(2.0)).roundToInt().coerceIn(0, 127)
            events.add(NoteEvent(midi, start.toDouble() / rate, durS, (vol * 260).toInt().coerceIn(30, 120), 1))
            val len = (rate * durS).toInt()
            for (i in 0 until len) {
                val idx = start + i; if (idx >= n) break
                val t = i.toDouble() / rate
                val s = kotlin.math.tanh((sin(2 * PI * freq * t) + 0.35 * sin(2 * PI * freq * 2 * t)) * 1.6)
                val env = (1 - exp(-t * 70)) * exp(-t * 1.9)
                mixAt(idx, s * env * vol, 0.5, 0.0)
            }
        }

        // ===== BATTERIE =====
        fun addKick(start: Int, vol: Double) {
            events.add(NoteEvent(36, start.toDouble() / rate, 0.12, (vol * 220).toInt().coerceIn(30, 127), 9))
            val len = (rate * 0.14).toInt()
            for (i in 0 until len) {
                val idx = start + i; if (idx >= n) break
                val t = i.toDouble() / rate
                val f = 115.0 * exp(-t * 24) + 44.0
                var s = sin(2 * PI * f * t) * exp(-t * 22)
                if (t < 0.004) s += (rnd.nextDouble() * 2 - 1) * exp(-t * 500) * 0.5   // clic d'attaque
                mixAt(idx, s * vol, 0.5, 0.0)
            }
        }
        fun addSnare(start: Int, vol: Double) {
            events.add(NoteEvent(38, start.toDouble() / rate, 0.1, (vol * 220).toInt().coerceIn(30, 127), 9))
            val len = (rate * 0.13).toInt()
            var noise = 0.0
            for (i in 0 until len) {
                val idx = start + i; if (idx >= n) break
                val t = i.toDouble() / rate
                noise = noise * 0.35 + (rnd.nextDouble() * 2 - 1) * 0.65
                val tone = sin(2 * PI * (210 * exp(-t * 16) + 150) * t) * 0.35
                mixAt(idx, (noise * 0.8 + tone) * exp(-t * 26) * vol, 0.45, 0.35)
            }
        }
        fun addHat(start: Int, vol: Double, pan: Double) {
            events.add(NoteEvent(42, start.toDouble() / rate, 0.03, (vol * 220).toInt().coerceIn(20, 110), 9))
            val len = (rate * 0.04).toInt()
            var prev = 0.0
            for (i in 0 until len) {
                val idx = start + i; if (idx >= n) break
                val w = rnd.nextDouble() * 2 - 1
                mixAt(idx, (w - prev) * exp(-i.toDouble() / rate * 85) * vol, pan, 0.12)
                prev = w
            }
        }

        // ===== HARMONIE & STRUCTURE =====
        val progressions = listOf(
            listOf(0, 4, 5, 3), listOf(0, 5, 3, 4), listOf(5, 3, 0, 4), listOf(0, 3, 0, 4)
        )
        val prog = progressions[rnd.nextInt(progressions.size)]
        val calm = listOf("10101010", "10100010", "10001000", "10100100")
        val spicy = listOf("10110100", "01101010", "10010110", "11010010")

        val song = thought.filter { it.code in 32..1000 }
        var degree = rnd.nextInt(ns)
        var charIdx = 0
        var slotGlobal = 0

        for (bar in 0 until bars) {
            val sectionB = (bar * 4 / bars) == 2
            val chordDeg = prog[bar % prog.size]
            val chordTones = setOf(chordDeg % ns, (chordDeg + 2) % ns, (chordDeg + 4) % ns)
            val spice = creativity + (if (sectionB) 25 else 0)
            val pattern = if (rnd.nextInt(100) < spice) spicy[rnd.nextInt(spicy.size)]
                          else calm[rnd.nextInt(calm.size)]
            val barStart = (slotGlobal * slotDur * rate).toInt()
            val barDur = slotDur * slotsPerBar

            // Nappe : triade de l'accord, notes réparties dans le champ stéréo
            val pans = listOf(0.25, 0.5, 0.75)
            chordTones.toList().sorted().forEachIndexed { i, tone ->
                val f = root * 2.0.pow(1 + scale[tone % ns] / 12.0)
                addPad(f, barStart, barDur * 1.05, 0.085, pans[i % 3])
            }

            // Basse : fondamentale, temps 1 et 3
            val bassFreq = root / 2 * 2.0.pow(scale[chordDeg % ns] / 12.0)
            val bassVol = tb?.let { 0.16 + (it[0] + it[1] + it[2]) / 3.0 * 0.3 } ?: 0.26
            addBass(bassFreq, barStart, slotDur * 3.5, bassVol)
            addBass(bassFreq, barStart + (4 * slotDur * rate).toInt(), slotDur * 3.5, bassVol * 0.9)

            for (s in 0 until slotsPerBar) {
                val start = (slotGlobal * slotDur * rate).toInt()
                if (s == 0 || s == 4) addKick(start, drumVol)
                if (s == 2 || s == 6) addSnare(start + jitter(), drumVol * 0.9)
                addHat(start + jitter(), drumVol * (if (s % 2 == 0) 0.42 else 0.26), if (s % 2 == 0) 0.62 else 0.38)
                if (bar % 4 == 3 && s >= 6 && creativity > 30) addSnare(start + jitter(), drumVol * 0.55)

                if (pattern[s] == '1') {
                    val ch = if (song.length > 8) song[charIdx++ % song.length] else ('a' + rnd.nextInt(26))
                    if (ch == ' ' || ch == '.') { slotGlobal++; continue }
                    degree += ((ch.code % 5) - 2)
                    while (degree < 0) degree += ns
                    degree %= ns
                    if (s % 4 == 0 && degree !in chordTones) {
                        degree = chordTones.minByOrNull { d -> minOf((d - degree + ns) % ns, (degree - d + ns) % ns) } ?: degree
                    }
                    val octave = (if (sectionB) 2 else 1) + (if (ch.isUpperCase()) 1 else 0)
                    val freq = root * 2.0.pow(octave.coerceIn(0, 3) + scale[degree] / 12.0)
                    var len = 1
                    var q = s + 1
                    while (q < slotsPerBar && pattern[q] == '0') { len++; q++ }
                    val phraseEnd = (bar % 2 == 1 && q >= slotsPerBar)
                    val dur = slotDur * (if (phraseEnd) len + 2.0 else len * 0.92)
                    val accent = if (s % 4 == 0) 1.15 else 1.0            // accents de musicien
                    addLead(freq, start + jitter(), dur, 0.42 * accent)
                    if (rnd.nextInt(200) < creativity) {
                        val gDeg = (degree + 1) % ns
                        val gFreq = root * 2.0.pow(octave.coerceIn(0, 3) + scale[gDeg] / 12.0)
                        addLead(gFreq, (start - rate * slotDur * 0.12).toInt().coerceAtLeast(0), slotDur * 0.12, 0.22)
                    }
                }
                slotGlobal++
            }
        }

        // ===== FIN MUSICALE : accord final tenu =====
        val endStart = (slotGlobal * slotDur * rate).toInt()
        val endDeg = prog[0]
        addKick(endStart, drumVol)
        addBass(root / 2 * 2.0.pow(scale[endDeg % ns] / 12.0), endStart, 2.0, 0.3)
        listOf(0, 2, 4).forEachIndexed { i, off ->
            val f = root * 2.0.pow(1 + scale[(endDeg + off) % ns] / 12.0)
            addPad(f, endStart, 2.2, 0.10, listOf(0.3, 0.5, 0.7)[i])
        }
        addLead(root * 2.0.pow(1 + scale[endDeg % ns] / 12.0), endStart, 1.8, 0.4)

        // ===== ÉCHO dotted-eighth (croisé en stéréo) =====
        val delay = (slotDur * 1.5 * rate).toInt()
        for (i in delay until n) {
            outL[i] += outR[i - delay] * 0.22
            outR[i] += outL[i - delay] * 0.22
        }

        // ===== RÉVERBÉRATION : filtres en peigne amortis sur le bus =====
        fun comb(buf: DoubleArray, d: Int, fb: Double, damp: Double) {
            var lp = 0.0
            for (i in d until n) {
                lp = lp * damp + buf[i - d] * (1 - damp)
                buf[i] += lp * fb
            }
        }
        comb(revL, (rate * 0.029).toInt(), 0.72, 0.35)
        comb(revL, (rate * 0.043).toInt(), 0.68, 0.4)
        comb(revR, (rate * 0.031).toInt(), 0.72, 0.35)
        comb(revR, (rate * 0.047).toInt(), 0.68, 0.4)
        for (i in 0 until n) { outL[i] += revL[i] * 0.5; outR[i] += revR[i] * 0.5 }

        // ===== MASTERING : anti-DC + saturation douce + normalisation =====
        var pL = 0.0; var pR = 0.0; var yL = 0.0; var yR = 0.0
        for (i in 0 until n) {
            val xl = outL[i]; val xr = outR[i]
            yL = xl - pL + 0.995 * yL; pL = xl
            yR = xr - pR + 0.995 * yR; pR = xr
            outL[i] = kotlin.math.tanh(yL * 0.9)
            outR[i] = kotlin.math.tanh(yR * 0.9)
        }
        var mx = 1e-9
        for (i in 0 until n) { if (kotlin.math.abs(outL[i]) > mx) mx = kotlin.math.abs(outL[i]); if (kotlin.math.abs(outR[i]) > mx) mx = kotlin.math.abs(outR[i]) }

        lastEvents = events
        lastBpm = bpm
        lastRate = rate
        // Sortie STÉRÉO entrelacée (G, D, G, D, ...)
        val pcm = ShortArray(n * 2)
        for (i in 0 until n) {
            pcm[i * 2] = ((outL[i] / mx) * 29000).toInt().toShort()
            pcm[i * 2 + 1] = ((outR[i] / mx) * 29000).toInt().toShort()
        }
        return pcm
    }

    // ==================== REMIX HARMONIQUE : TES sons, structurés ====================
    /**
     * Le sampler discipliné : comme un vrai producteur.
     * 1. RÔLES : chaque extrait reçoit un rôle fixe pour tout le morceau
     *    (basse = le plus grave + filtre, kick/charley = les plus percussifs,
     *    3 chops mélodiques). Un seul "kit" = cohérence.
     * 2. MOTIF : UNE mesure est composée, puis RÉPÉTÉE avec variations
     *    (répétition = musique). Le pont (section B) a son propre motif.
     * 3. HARMONIE : tout le motif est transposé chaque mesure selon la
     *    progression d'accords (I-V-vi-IV...) — c'est là que ça sonne juste.
     */
    fun makeRemix(
        prompt: String,
        clips: List<ShortArray>,
        creativity: Int = 50,
        thought: String = "",
        bpmOverride: Int = 0,
        barsCount: Int = 8
    ): ShortArray {
        val srate = 16000
        val seed = prompt.trim().lowercase().hashCode().toLong()
        val rnd = Random(seed)
        val bpm = if (bpmOverride > 0) bpmOverride else 88 + rnd.nextInt(40)
        val slotDur = 60.0 / bpm / 2.0
        val bars = barsCount.coerceIn(4, 64)
        val slotsPerBar = 8
        val n = (srate * slotDur * bars * slotsPerBar).toInt() + srate * 2
        // Deux bus : MAIN (basse + percus, intouchable) et SIDE (nappe + chops,
        // qui s'écarte à chaque kick = ducking sidechain, la glue du mix)
        val mainL = DoubleArray(n); val mainR = DoubleArray(n)
        val sideL = DoubleArray(n); val sideR = DoubleArray(n)
        val duck = DoubleArray(n) { 1.0 }
        val events = ArrayList<NoteEvent>()

        // ===== RÔLES =====
        fun zcr(c: ShortArray): Double {
            var z = 0
            for (i in 1 until c.size) if ((c[i] >= 0) != (c[i - 1] >= 0)) z++
            return z.toDouble() / c.size
        }
        val byBright = clips.sortedBy { zcr(it) }
        val bassClip = byBright.first()
        val hatClip = byBright.last()
        val kickClip = byBright[byBright.size / 4]
        val mid = byBright.drop(1).dropLast(1).ifEmpty { clips }
        val chops = List(3) { mid[(it * mid.size / 3).coerceIn(0, mid.size - 1)] }
        val bedClip = mid[mid.size / 2]

        fun resample(clip: ShortArray, factor: Double): DoubleArray {
            val outN = (clip.size / factor).toInt().coerceAtLeast(16)
            return DoubleArray(outN) { i ->
                val pp = i * factor
                val i0 = pp.toInt().coerceAtMost(clip.size - 2)
                val f = pp - i0
                (clip[i0] * (1 - f) + clip[i0 + 1] * f) / 32768.0
            }
        }

        fun addClip(clip: ShortArray, start: Int, semitones: Int, vol: Double, pan: Double,
                    reverse: Boolean, maxDurS: Double, ch: Int, lpCut: Double = 0.0, side: Boolean = false) {
            val factor = 2.0.pow(semitones / 12.0)
            var d = resample(clip, factor)
            if (reverse) d = DoubleArray(d.size) { d[d.size - 1 - it] }
            val len = minOf(d.size, (srate * maxDurS).toInt())
            val fade = (srate * 0.005).toInt().coerceAtLeast(1)
            events.add(NoteEvent((60 + semitones).coerceIn(0, 127), start.toDouble() / srate,
                len.toDouble() / srate, (vol * 220).toInt().coerceIn(30, 120), ch))
            var lp = 0.0
            val alpha = if (lpCut > 0) 1 - exp(-2 * PI * lpCut / srate) else 1.0
            val dl = if (side) sideL else mainL
            val dr = if (side) sideR else mainR
            for (i in 0 until len) {
                val idx = start + i; if (idx >= n) break
                var s = d[i]
                if (lpCut > 0) { lp += alpha * (s - lp); s = lp }
                s *= vol
                if (i < fade) s *= i.toDouble() / fade
                if (i > len - fade) s *= (len - i).toDouble() / fade
                dl[idx] += s * (1.0 - pan)
                dr[idx] += s * pan
            }
        }

        /** Le kick creuse le bus SIDE pendant 0,22 s : le mix respire au tempo. */
        fun duckAt(start: Int) {
            val len = (srate * 0.22).toInt()
            for (i in 0 until len) {
                val idx = start + i; if (idx >= n) break
                val g = 0.35 + 0.65 * (i.toDouble() / len)
                if (g < duck[idx]) duck[idx] = g
            }
        }

        // ===== HARMONIE : un accord toutes les DEUX mesures (stabilité) =====
        val majorSemi = listOf(0, 2, 4, 5, 7, 9, 11)
        val progressions = listOf(
            listOf(0, 4, 5, 3), listOf(0, 5, 3, 4), listOf(5, 3, 0, 4), listOf(0, 3, 0, 4)
        )
        val prog = progressions[rnd.nextInt(progressions.size)]
        fun fold(s: Int): Int { var v = s; while (v > 6) v -= 12; while (v < -6) v += 12; return v }
        fun shiftFor(bar: Int): Int = fold(majorSemi[prog[(bar / 2) % prog.size] % 7])

        // ===== LE MOTIF (composé une fois, répété) =====
        fun composePattern(density: Int): List<Triple<Int, Int, Int>> {
            val base = if (density < 50) listOf("10100010", "10001010", "10100100")
                       else listOf("10110100", "10010110", "01101010")
            val pat = base[rnd.nextInt(base.size)]
            val song = thought.filter { it.isLetter() }
            val out = ArrayList<Triple<Int, Int, Int>>()
            var ci = 0
            for (s in 0 until slotsPerBar) if (pat[s] == '1') {
                val c = if (song.length > 4) song[ci++ % song.length].code else rnd.nextInt(97, 123)
                var len = 1; var q = s + 1
                while (q < slotsPerBar && pat[q] == '0') { len++; q++ }
                out.add(Triple(s, c % 3, len.coerceAtMost(2)))   // chops courts = net
            }
            return out
        }
        val patternA = composePattern(creativity)
        val patternB = composePattern(creativity + 30)

        // ===== ARRANGEMENT : intro -> A -> pont -> A -> outro =====
        for (bar in 0 until bars) {
            val q = bar * 4 / bars
            val intro = bar == 0 || (bars >= 16 && bar == 1)
            val outro = bar == bars - 1
            val sectionB = q == 2 && !outro && !intro
            val shift = shiftFor(bar)
            val barStart = (bar * slotsPerBar * slotDur * srate).toInt()
            val variation = bar % 4 == 3

            // NAPPE : présente sauf au pont, filtrée pour laisser la place
            if (!sectionB) addClip(bedClip, barStart, -12 + shift, 0.14, 0.5, false,
                slotDur * slotsPerBar * 1.02, 2, lpCut = 500.0, side = true)

            // BASSE : absente à l'intro (elle ARRIVE, c'est la montée)
            if (!intro) {
                addClip(bassClip, barStart, -12 + shift, 0.44, 0.5, false, slotDur * 3.6, 1, lpCut = 200.0)
                if (!outro) addClip(bassClip, barStart + (4 * slotDur * srate).toInt(),
                    -12 + shift, 0.4, 0.5, false, slotDur * 3.6, 1, lpCut = 200.0)
            }

            // GROOVE
            for (s in 0 until slotsPerBar) {
                val st = barStart + (s * slotDur * srate).toInt()
                val kickHere = if (intro || outro) s == 0 else (s == 0 || s == 4)
                if (kickHere) { addClip(kickClip, st, -7, 0.5, 0.5, false, 0.09, 9); duckAt(st) }
                if (!intro && !outro && (s == 2 || s == 6))
                    addClip(kickClip, st, 3, 0.32, 0.45, false, 0.11, 9)
                if (!outro && s % 2 == 0)
                    addClip(hatClip, st, 12, 0.15, if (s % 4 == 0) 0.62 else 0.38, false, 0.045, 9)
            }
            if (variation && creativity > 30 && !outro)
                addClip(hatClip, barStart + (7 * slotDur * srate).toInt(), 12, 0.2, 0.5, false, 0.09, 9)

            // CHOPS : pas à l'intro (une annonce), pas à l'outro (un adieu)
            when {
                intro -> addClip(chops[0], barStart + (6 * slotDur * srate).toInt(),
                    shift, 0.4, 0.5, false, slotDur * 1.9, 0, side = true)
                outro -> addClip(chops[0], barStart, shift, 0.42, 0.5, false, slotDur * 3.9, 0, side = true)
                else -> {
                    val pattern = if (sectionB) patternB else patternA
                    for ((s, chopIdx, len) in pattern) {
                        val st = barStart + (s * slotDur * srate).toInt()
                        val rev = variation && creativity > 55 && s == pattern.last().first
                        val accent = if (s % 4 == 0) 1.1 else 0.9
                        addClip(chops[chopIdx], st, shift + (if (sectionB) 12 else 0),
                            0.5 * accent, if (s % 2 == 0) 0.4 else 0.6, rev,
                            slotDur * len * 0.98, 0, side = true)
                    }
                }
            }
        }

        // Fin : basse tonique tenue
        val endStart = (bars * slotsPerBar * slotDur * srate).toInt()
        addClip(bassClip, endStart, -12, 0.4, 0.5, false, 1.6, 1, lpCut = 200.0)

        // ===== MIX : side ducké + écho discret sur le side + fondu final =====
        val delay = (slotDur * 1.5 * srate).toInt()
        for (i in delay until n) {
            sideL[i] += sideR[i - delay] * 0.13
            sideR[i] += sideL[i - delay] * 0.13
        }
        val fadeStart = endStart
        val outL = DoubleArray(n); val outR = DoubleArray(n)
        var mx = 1e-9
        for (i in 0 until n) {
            var g = 1.0
            if (i > fadeStart) g = (1.0 - (i - fadeStart).toDouble() / (n - fadeStart)).coerceAtLeast(0.0)
            outL[i] = kotlin.math.tanh((mainL[i] + sideL[i] * duck[i]) * 1.1) * g
            outR[i] = kotlin.math.tanh((mainR[i] + sideR[i] * duck[i]) * 1.1) * g
            if (kotlin.math.abs(outL[i]) > mx) mx = kotlin.math.abs(outL[i])
            if (kotlin.math.abs(outR[i]) > mx) mx = kotlin.math.abs(outR[i])
        }
        lastEvents = events
        lastBpm = bpm
        lastRate = srate
        val pcm = ShortArray(n * 2)
        for (i in 0 until n) {
            pcm[i * 2] = ((outL[i] / mx) * 29000).toInt().toShort()
            pcm[i * 2 + 1] = ((outR[i] / mx) * 29000).toInt().toShort()
        }
        return pcm
    }

    // ==================== EXPORT MIDI ====================
    /**
     * Écrit un fichier MIDI standard (format 0) de la dernière composition :
     * mélodie sur le canal 1 (piano), basse sur le canal 2, tempo inclus.
     * Ouvrable dans FL Studio, Ableton, GarageBand, MuseScore...
     */
    fun saveMidi(file: File) {
        if (lastEvents.isEmpty()) return
        val tpq = 480
        val bpm = lastBpm

        fun varint(value: Int): ByteArray {
            var v = value.coerceAtLeast(0)
            val stack = ArrayList<Int>()
            stack.add(v and 0x7F)
            v = v shr 7
            while (v > 0) { stack.add((v and 0x7F) or 0x80); v = v shr 7 }
            return ByteArray(stack.size) { stack[stack.size - 1 - it].toByte() }
        }

        class Ev(val tick: Int, val order: Int, val bytes: ByteArray)
        val evs = ArrayList<Ev>()
        evs.add(Ev(0, 0, byteArrayOf(0xC0.toByte(), 0)))              // canal 1 : piano
        evs.add(Ev(0, 0, byteArrayOf(0xC1.toByte(), 33)))             // canal 2 : basse
        for (e in lastEvents) {
            val on = (e.startSec * bpm / 60.0 * tpq).toInt()
            val off = on + max(1, (e.durSec * bpm / 60.0 * tpq).toInt())
            evs.add(Ev(on, 1, byteArrayOf((0x90 or e.ch).toByte(), e.midi.toByte(), e.vel.toByte())))
            evs.add(Ev(off, 2, byteArrayOf((0x80 or e.ch).toByte(), e.midi.toByte(), 0)))
        }
        evs.sortWith(compareBy({ it.tick }, { it.order }))

        val body = ByteArrayOutputStream()
        // tempo (microsecondes par noire)
        val mpq = 60_000_000 / bpm
        body.write(varint(0))
        body.write(byteArrayOf(0xFF.toByte(), 0x51, 3,
            (mpq shr 16 and 0xFF).toByte(), (mpq shr 8 and 0xFF).toByte(), (mpq and 0xFF).toByte()))
        var last = 0
        for (e in evs) {
            body.write(varint(e.tick - last)); last = e.tick
            body.write(e.bytes)
        }
        body.write(varint(0))
        body.write(byteArrayOf(0xFF.toByte(), 0x2F, 0))               // fin de piste

        file.outputStream().use { o ->
            fun be16(v: Int) = o.write(byteArrayOf((v shr 8).toByte(), (v and 0xFF).toByte()))
            fun be32(v: Int) = o.write(byteArrayOf((v shr 24).toByte(), (v shr 16 and 0xFF).toByte(),
                (v shr 8 and 0xFF).toByte(), (v and 0xFF).toByte()))
            o.write("MThd".toByteArray()); be32(6); be16(0); be16(1); be16(tpq)
            val b = body.toByteArray()
            o.write("MTrk".toByteArray()); be32(b.size); o.write(b)
        }
    }

    fun play(pcm: ShortArray) {
        stop()
        try {
            @Suppress("DEPRECATION")
            val t = AudioTrack(AudioManager.STREAM_MUSIC, lastRate,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                pcm.size * 2, AudioTrack.MODE_STATIC)
            t.write(pcm, 0, pcm.size)
            t.play()
            track = t
        } catch (e: Exception) { }
    }

    fun stop() {
        try { track?.stop(); track?.release() } catch (e: Exception) { }
        track = null
    }

    /** Écrit un fichier WAV standard (16 bits STÉRÉO, au taux de la création). */
    fun saveWav(pcm: ShortArray, file: File) {
        val dataLen = pcm.size * 2
        val ch = 2
        file.outputStream().use { o ->
            fun le32(v: Int) = o.write(byteArrayOf((v and 0xFF).toByte(), (v shr 8 and 0xFF).toByte(),
                (v shr 16 and 0xFF).toByte(), (v shr 24 and 0xFF).toByte()))
            fun le16(v: Int) = o.write(byteArrayOf((v and 0xFF).toByte(), (v shr 8 and 0xFF).toByte()))
            o.write("RIFF".toByteArray()); le32(36 + dataLen); o.write("WAVE".toByteArray())
            o.write("fmt ".toByteArray()); le32(16); le16(1); le16(ch)
            le32(lastRate); le32(lastRate * 2 * ch); le16(2 * ch); le16(16)
            o.write("data".toByteArray()); le32(dataLen)
            val bytes = ByteArray(dataLen)
            for (i in pcm.indices) {
                bytes[i * 2] = (pcm[i].toInt() and 0xFF).toByte()
                bytes[i * 2 + 1] = (pcm[i].toInt() shr 8 and 0xFF).toByte()
            }
            o.write(bytes)
        }
    }
}
