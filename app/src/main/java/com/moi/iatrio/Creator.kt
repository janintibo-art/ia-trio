package com.moi.iatrio

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
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

    // ==================== MUSIQUE ====================
    private var track: AudioTrack? = null
    private val rate = 22050

    /**
     * Composition algorithmique : le texte choisit la gamme, le tempo, la
     * racine et la mélodie (marche aléatoire sur la gamme) + une basse.
     */
    fun makeMusic(prompt: String, thought: String = "", timbres: List<Pair<String, DoubleArray>> = emptyList()): ShortArray {
        val seed = prompt.trim().lowercase().hashCode().toLong()
        val rnd = Random(seed)
        val scales = listOf(
            listOf(0, 2, 4, 5, 7, 9, 11),   // majeure : joyeux
            listOf(0, 2, 3, 5, 7, 8, 10),   // mineure : mélancolique
            listOf(0, 3, 5, 7, 10),         // pentatonique : planant
            listOf(0, 2, 3, 6, 7, 8, 11)    // orientale : mystérieux
        )
        val scale = scales[rnd.nextInt(scales.size)]

        // ===== LA SONORITÉ DE TA MUSIQUE =====
        // Empreinte spectrale moyenne des morceaux appris (32 bandes) :
        // elle façonne les harmoniques, la hauteur, le tempo et l'attaque.
        val tb: DoubleArray? = if (timbres.isEmpty()) null else {
            val avg = DoubleArray(32)
            for ((_, t) in timbres) for (i in 0 until 32) avg[i] += t[i] / timbres.size
            val mx = avg.maxOrNull() ?: 1.0
            if (mx > 0) for (i in avg.indices) avg[i] /= mx
            avg
        }
        // Centre spectral : où se concentre l'énergie (grave 0 ... aigu 31)
        val centroid = tb?.let { t ->
            var num = 0.0; var den = 1e-9
            for (i in t.indices) { num += i * t[i]; den += t[i] }
            num / den
        } ?: 10.0
        // Harmoniques des notes tirées de l'empreinte (repli : timbre doux)
        val harm = DoubleArray(8) { k ->
            val v = tb?.get(((k + 1) * 3).coerceAtMost(31)) ?: when (k) {
                0 -> 0.6; 1 -> 0.25; 2 -> 0.1; else -> 0.0
            }
            v.coerceAtLeast(if (k == 0) 0.35 else 0.0)
        }
        run { val s = harm.sum(); if (s > 0) for (i in harm.indices) harm[i] = harm[i] / s * 0.95 }

        val rootMul = (0.7 + centroid / 16.0).coerceIn(0.7, 2.0)      // musique brillante -> plus haut
        val root = 220.0 * 2.0.pow(rnd.nextInt(8) / 12.0) * rootMul
        val bpm = (84 + rnd.nextInt(56) + ((centroid - 10) * 3).toInt()).coerceIn(70, 160)
        val decayRate = (2.0 + centroid * 0.15)                        // brillant = plus percussif
        val noteDur = 60.0 / bpm / 2.0            // croches
        val nNotes = 48
        val total = (rate * noteDur * nNotes).toInt() + rate / 2
        val out = DoubleArray(total)

        fun addNote(freq: Double, start: Int, durS: Double, vol: Double) {
            val n = (rate * durS).toInt()
            for (i in 0 until n) {
                val idx = start + i
                if (idx >= total) break
                val t = i.toDouble() / rate
                val env = (1 - exp(-t * 60)) * exp(-t * decayRate)   // attaque + déclin façonné
                var s = 0.0
                for (k in harm.indices) {
                    if (harm[k] < 0.01) continue
                    s += sin(2 * PI * freq * (k + 1) * t) * harm[k]
                }
                out[idx] += s * env * vol
            }
        }

        var degree = rnd.nextInt(scale.size)
        var octave = 1
        val song = thought.filter { it.code in 32..1000 }
        for (k in 0 until nNotes) {
            val start = (k * noteDur * rate).toInt()
            if (song.length > 8) {
                // L'IA CHANTE SA PENSÉE : chaque caractère devient une note
                val ch = song[k % song.length]
                if (ch == ' ' || ch == '.') {
                    // respiration
                } else {
                    degree = ch.code % scale.size
                    octave = when {
                        ch.isUpperCase() -> 2
                        ch in "aeiouyàéèêô" -> 1
                        else -> 1 + (ch.code / 7) % 2
                    }.coerceIn(0, 2)
                    val freq = root * 2.0.pow(octave + scale[degree] / 12.0)
                    val long = ch in "aeiouyàéèêô"   // les voyelles durent plus longtemps
                    addNote(freq, start, noteDur * (if (long) 1.8 else 0.95), 0.5)
                }
            } else {
                // marche aléatoire classique si la mémoire est vide
                degree += rnd.nextInt(5) - 2
                if (degree < 0) { degree += scale.size; octave = maxOf(0, octave - 1) }
                if (degree >= scale.size) { degree -= scale.size; octave = minOf(2, octave + 1) }
                val freq = root * 2.0.pow(octave + scale[degree] / 12.0)
                if (rnd.nextInt(100) < 85) addNote(freq, start, noteDur * 0.95, 0.5)
            }
            // basse sur les temps, dosée par les graves de ta musique
            val bassVol = tb?.let { 0.15 + (it[0] + it[1] + it[2]) / 3.0 * 0.35 } ?: 0.3
            if (k % 4 == 0) addNote(root / 2, start, noteDur * 3.0, bassVol)
        }

        // normalisation
        var mx = 1e-9
        for (v in out) if (kotlin.math.abs(v) > mx) mx = kotlin.math.abs(v)
        return ShortArray(total) { ((out[it] / mx) * 30000).toInt().toShort() }
    }

    fun play(pcm: ShortArray) {
        stop()
        try {
            @Suppress("DEPRECATION")
            val t = AudioTrack(AudioManager.STREAM_MUSIC, rate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
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

    /** Écrit un fichier WAV standard (16 bits mono). */
    fun saveWav(pcm: ShortArray, file: File) {
        val dataLen = pcm.size * 2
        file.outputStream().use { o ->
            fun le32(v: Int) = byteArrayOf((v and 0xFF).toByte(), (v shr 8 and 0xFF).toByte(),
                (v shr 16 and 0xFF).toByte(), (v shr 24 and 0xFF).toByte())
            fun le16(v: Int) = byteArrayOf((v and 0xFF).toByte(), (v shr 8 and 0xFF).toByte())
            o.write("RIFF".toByteArray()); o.write(le32(36 + dataLen)); o.write("WAVE".toByteArray())
            o.write("fmt ".toByteArray()); o.write(le32(16)); o.write(le16(1)); o.write(le16(1))
            o.write(le32(rate)); o.write(le32(rate * 2)); o.write(le16(2)); o.write(le16(16))
            o.write("data".toByteArray()); o.write(le32(dataLen))
            val bytes = ByteArray(dataLen)
            for (i in pcm.indices) {
                bytes[i * 2] = (pcm[i].toInt() and 0xFF).toByte()
                bytes[i * 2 + 1] = (pcm[i].toInt() shr 8 and 0xFF).toByte()
            }
            o.write(bytes)
        }
    }
}
