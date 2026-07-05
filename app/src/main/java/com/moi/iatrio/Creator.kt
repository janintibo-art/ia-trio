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
    fun makeImage(prompt: String, size: Int = 768): Bitmap {
        val seed = prompt.trim().lowercase().hashCode().toLong()
        val rnd = Random(seed)
        val hueBase = ((seed % 360 + 360) % 360).toFloat()

        fun col(hueOff: Float, s: Float, v: Float, a: Int): Int {
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
    fun makeMusic(prompt: String): ShortArray {
        val seed = prompt.trim().lowercase().hashCode().toLong()
        val rnd = Random(seed)
        val scales = listOf(
            listOf(0, 2, 4, 5, 7, 9, 11),   // majeure : joyeux
            listOf(0, 2, 3, 5, 7, 8, 10),   // mineure : mélancolique
            listOf(0, 3, 5, 7, 10),         // pentatonique : planant
            listOf(0, 2, 3, 6, 7, 8, 11)    // orientale : mystérieux
        )
        val scale = scales[rnd.nextInt(scales.size)]
        val root = 220.0 * 2.0.pow(rnd.nextInt(8) / 12.0)
        val bpm = 84 + rnd.nextInt(56)
        val noteDur = 60.0 / bpm / 2.0            // croches
        val nNotes = 32
        val total = (rate * noteDur * nNotes).toInt() + rate / 2
        val out = DoubleArray(total)

        fun addNote(freq: Double, start: Int, durS: Double, vol: Double) {
            val n = (rate * durS).toInt()
            for (i in 0 until n) {
                val idx = start + i
                if (idx >= total) break
                val t = i.toDouble() / rate
                val env = (1 - exp(-t * 60)) * exp(-t * 3.2)   // attaque + déclin
                val s = sin(2 * PI * freq * t) * 0.6 +
                        sin(2 * PI * freq * 2 * t) * 0.25 +
                        sin(2 * PI * freq * 3 * t) * 0.1
                out[idx] += s * env * vol
            }
        }

        var degree = rnd.nextInt(scale.size)
        var octave = 1
        for (k in 0 until nNotes) {
            // marche aléatoire mélodique
            degree += rnd.nextInt(5) - 2
            if (degree < 0) { degree += scale.size; octave = maxOf(0, octave - 1) }
            if (degree >= scale.size) { degree -= scale.size; octave = minOf(2, octave + 1) }
            val freq = root * 2.0.pow(octave + scale[degree] / 12.0)
            val start = (k * noteDur * rate).toInt()
            if (rnd.nextInt(100) < 85)   // parfois un silence, ça respire
                addNote(freq, start, noteDur * 0.95, 0.5)
            // basse sur les temps
            if (k % 4 == 0) addNote(root / 2, start, noteDur * 3.0, 0.3)
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
