package com.moi.iatrio

import android.graphics.Bitmap
import android.graphics.Color
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

/** IA n°1 : les IMAGES. Tu lui montres une photo + un nom, elle apprend. */
class ImageBrain(dir: File) {
    private val net = NeuralNet(256, 32)
    private val file = File(dir, "image.net")
    init { if (file.exists()) net.load(file) }

    private fun features(bmp: Bitmap): DoubleArray {
        val s = Bitmap.createScaledBitmap(bmp, 16, 16, true)
        val x = DoubleArray(256)
        var i = 0
        for (y in 0 until 16) for (xx in 0 until 16) {
            val p = s.getPixel(xx, y)
            x[i++] = ((Color.red(p) + Color.green(p) + Color.blue(p)) / 3.0) / 255.0
        }
        return x
    }

    fun learn(bmp: Bitmap, label: String) { net.train(features(bmp), label); net.save(file) }
    fun guess(bmp: Bitmap) = net.predict(features(bmp))
    fun knowledge() = net.labels.toList()
}

/** IA n°2 : la MUSIQUE / les SONS. Elle apprend à reconnaître des sons enregistrés au micro. */
class AudioBrain(dir: File) {
    private val net = NeuralNet(64, 32)
    private val file = File(dir, "audio.net")
    init { if (file.exists()) net.load(file) }

    /** Transforme 2 s de son en 64 caractéristiques (énergie + fréquence par fenêtre). */
    private fun features(pcm: ShortArray): DoubleArray {
        val x = DoubleArray(64)
        val win = pcm.size / 32
        if (win == 0) return x
        for (w in 0 until 32) {
            var energy = 0.0
            var crossings = 0
            for (i in 1 until win) {
                val v = pcm[w * win + i].toDouble() / 32768.0
                energy += v * v
                if ((pcm[w * win + i] >= 0) != (pcm[w * win + i - 1] >= 0)) crossings++
            }
            x[w] = sqrt(energy / win)          // volume de la fenêtre
            x[32 + w] = crossings.toDouble() / win // hauteur approximative
        }
        return x
    }

    fun learn(pcm: ShortArray, label: String) { net.train(features(pcm), label); net.save(file) }
    fun guess(pcm: ShortArray) = net.predict(features(pcm))
    fun knowledge() = net.labels.toList()
}

/** IA n°3 : le CODE / TEXTE. Modèle n-gramme : colle-lui du code, elle apprend ton style et complète. */
class CodeBrain(dir: File) {
    private val order = 4
    private val model = HashMap<String, HashMap<Char, Int>>()
    private val corpus = File(dir, "code.txt")
    init { if (corpus.exists()) learnInternal(corpus.readText()) }

    private fun learnInternal(text: String) {
        for (i in 0 until text.length - order) {
            val ctx = text.substring(i, i + order)
            val next = text[i + order]
            val m = model.getOrPut(ctx) { HashMap() }
            m[next] = (m[next] ?: 0) + 1
        }
    }

    fun learn(text: String) {
        learnInternal(text)
        corpus.appendText(text + "\n")
    }

    fun complete(prompt: String, length: Int = 200): String {
        if (model.isEmpty()) return "(rien appris : colle-moi du code d'abord)"
        var ctx = if (prompt.length >= order) prompt.takeLast(order)
                  else model.keys.firstOrNull() ?: return ""
        val sb = StringBuilder(prompt)
        repeat(length) {
            val m = model[ctx] ?: return sb.toString()
            val total = m.values.sum()
            var r = (Math.random() * total).toInt()
            var pick = m.keys.first()
            for ((c, n) in m) { r -= n; if (r < 0) { pick = c; break } }
            sb.append(pick)
            ctx = sb.takeLast(order).toString()
        }
        return sb.toString()
    }

    fun size() = model.size
}

/**
 * L'ORCHESTRATEUR : le tableau commun où les 3 IA déposent ce qu'elles savent,
 * puis réfléchissent ensemble.
 */
class Orchestrator(val image: ImageBrain, val audio: AudioBrain, val code: CodeBrain) {
    var lastImage: Pair<String, Double>? = null
    var lastAudio: Pair<String, Double>? = null

    fun thinkTogether(): String {
        val sb = StringBuilder("=== Réflexion commune ===\n")
        val img = lastImage
        val aud = lastAudio
        sb.append("Vision : ").append(img?.let { "je vois « ${it.first} » (${(it.second * 100).toInt()}%)" } ?: "rien vu récemment").append("\n")
        sb.append("Ouïe : ").append(aud?.let { "j'entends « ${it.first} » (${(it.second * 100).toInt()}%)" } ?: "rien entendu récemment").append("\n")
        val seed = listOfNotNull(img?.first, aud?.first).joinToString(" ")
        if (seed.isNotBlank() && code.size() > 0) {
            sb.append("Le cerveau code s'inspire des deux autres :\n")
            sb.append(code.complete("// $seed\n", 250))
        } else {
            sb.append("(Entraîne les 3 cerveaux pour qu'ils puissent collaborer !)")
        }
        return sb.toString()
    }
}
