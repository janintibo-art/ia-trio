package com.moi.iatrio

import android.graphics.Bitmap
import android.graphics.Color
import java.io.File
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * IA n°1 : IMAGES (v2) — couleur RGB + data augmentation.
 */
class ImageBrain(dir: File) {
    private val net = NeuralNet(192, 48)
    private val file = File(dir, "image.net")
    init { if (file.exists()) net.load(file) }

    private fun features(bmp: Bitmap, mirror: Boolean = false, bright: Double = 1.0): DoubleArray {
        val s = Bitmap.createScaledBitmap(bmp, 8, 8, true)
        val x = DoubleArray(192)
        var i = 0
        for (y in 0 until 8) for (xx in 0 until 8) {
            val px = if (mirror) 7 - xx else xx
            val p = s.getPixel(px, y)
            x[i++] = (Color.red(p) / 255.0 * bright).coerceIn(0.0, 1.0)
            x[i++] = (Color.green(p) / 255.0 * bright).coerceIn(0.0, 1.0)
            x[i++] = (Color.blue(p) / 255.0 * bright).coerceIn(0.0, 1.0)
        }
        return x
    }

    fun learn(bmp: Bitmap, label: String) {
        net.train(features(bmp), label)
        net.train(features(bmp, mirror = true), label)
        net.train(features(bmp, bright = 1.25), label)
        net.train(features(bmp, bright = 0.8), label)
        net.save(file)
    }

    fun guess(bmp: Bitmap) = net.predict(features(bmp))
    fun summary() = net.summary()
    fun labels(): List<String> = net.labels.toList()
    fun forget() { net.reset(); if (file.exists()) file.delete() }
}

/**
 * IA n°2 : SONS (v2) — analyse par DFT (Fourier), 32 bandes de fréquences.
 */
class AudioBrain(dir: File) {
    private val bands = 32
    private val net = NeuralNet(bands, 32)
    private val file = File(dir, "audio.net")
    init { if (file.exists()) net.load(file) }

    private fun features(pcm: ShortArray): DoubleArray {
        val n = 512
        val step = maxOf(1, pcm.size / n)
        val sig = DoubleArray(n)
        for (i in 0 until n) {
            val idx = i * step
            sig[i] = if (idx < pcm.size) pcm[idx].toDouble() / 32768.0 else 0.0
        }
        for (i in 0 until n) sig[i] *= 0.5 - 0.5 * cos(2 * Math.PI * i / (n - 1))
        val x = DoubleArray(bands)
        for (b in 0 until bands) {
            val f = b + 1
            var re = 0.0; var im = 0.0
            for (i in 0 until n) {
                val ang = 2 * Math.PI * f * i / n
                re += sig[i] * cos(ang)
                im -= sig[i] * sin(ang)
            }
            x[b] = ln(1.0 + sqrt(re * re + im * im))
        }
        val mx = x.maxOrNull() ?: 1.0
        if (mx > 0) for (i in x.indices) x[i] /= mx
        return x
    }

    fun learn(pcm: ShortArray, label: String) { net.train(features(pcm), label); net.save(file) }
    fun guess(pcm: ShortArray) = net.predict(features(pcm))
    fun summary() = net.summary()
    fun labels(): List<String> = net.labels.toList()
    fun forget() { net.reset(); if (file.exists()) file.delete() }
}

/**
 * IA n°3 : CODE / TEXTE (v2) — n-gramme + oublier.
 */
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

    fun learn(text: String) { learnInternal(text); corpus.appendText(text + "\n") }

    /**
     * creativity 0..100 : à 0 on choisit toujours la suite la plus probable
     * (texte sage et répétitif), à 100 on tire au sort selon les probabilités
     * (texte varié et audacieux).
     */
    fun complete(prompt: String, length: Int = 200, creativity: Int = 50): String {
        if (model.isEmpty()) return "(rien appris : colle-moi du code d'abord)"
        var ctx = if (prompt.length >= order) prompt.takeLast(order)
                  else model.keys.firstOrNull() ?: return ""
        val sb = StringBuilder(prompt)
        repeat(length) {
            val m = model[ctx] ?: return sb.toString()
            val pick: Char = if (Math.random() * 100 >= creativity) {
                m.maxByOrNull { it.value }!!.key          // choix le plus probable
            } else {
                val total = m.values.sum()                 // tirage pondéré
                var r = (Math.random() * total).toInt()
                var p = m.keys.first()
                for ((c, nn) in m) { r -= nn; if (r < 0) { p = c; break } }
                p
            }
            sb.append(pick)
            ctx = sb.takeLast(order).toString()
        }
        return sb.toString()
    }

    fun size() = model.size
    fun corpusExcerpt(maxChars: Int = 3000): String =
        if (corpus.exists()) corpus.readText().take(maxChars) else ""
    fun forget() { model.clear(); if (corpus.exists()) corpus.delete() }
}

/**
 * ORCHESTRATEUR (v2) — raisonne sur l'accord vision/ouïe.
 */
class Orchestrator(val image: ImageBrain, val audio: AudioBrain, val code: CodeBrain) {
    var lastImage: Pair<String, Double>? = null
    var lastAudio: Pair<String, Double>? = null

    fun thinkTogether(): String {
        val sb = StringBuilder("=== Réflexion commune ===\n")
        val img = lastImage
        val aud = lastAudio
        sb.append("Vision : ").append(img?.let { "« ${it.first} » (${(it.second * 100).toInt()}%)" } ?: "rien vu").append("\n")
        sb.append("Ouïe : ").append(aud?.let { "« ${it.first} » (${(it.second * 100).toInt()}%)" } ?: "rien entendu").append("\n")
        if (img != null && aud != null) {
            val conf = (img.second + aud.second) / 2.0
            sb.append("\nSynthèse : ")
            when {
                img.first == aud.first ->
                    sb.append("les deux sens sont d'accord sur « ${img.first} » → confiance TRÈS élevée.\n")
                conf > 0.7 ->
                    sb.append("je vois « ${img.first} » et j'entends « ${aud.first} » avec assez de certitude.\n")
                else ->
                    sb.append("signaux faibles, entraîne-moi avec plus d'exemples.\n")
            }
        }
        val seed = listOfNotNull(img?.first, aud?.first).joinToString(" ")
        if (seed.isNotBlank() && code.size() > 0) {
            sb.append("\nLe cerveau code s'inspire des deux autres :\n")
            sb.append(code.complete("// $seed\n", 250))
        } else if (code.size() == 0) {
            sb.append("\n(Apprends aussi du code pour compléter la collaboration.)")
        }
        return sb.toString()
    }
}
