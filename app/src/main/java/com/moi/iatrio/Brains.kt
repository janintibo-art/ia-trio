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
class ImageBrain(dir: File, private val tf: TFVision? = null) {
    private val nIn = if (tf != null) 1001 else 192
    private val net = NeuralNet(nIn, if (tf != null) 64 else 48)
    private val file = File(dir, if (tf != null) "image_tf.net" else "image.net")
    private val samples = File(dir, if (tf != null) "samples_img_tf.txt" else "samples_img.txt")
    private val paletteFile = File(dir, "palette_img.txt")
    // Mémoire des couleurs : moyenne 8x8 RGB par étiquette (label -> (nb, 192 valeurs))
    private val palettes = HashMap<String, Pair<Int, DoubleArray>>()
    init {
        if (file.exists()) net.load(file)
        try {
            if (paletteFile.exists()) for (line in paletteFile.readLines()) {
                val parts = line.split("|")
                if (parts.size == 3) {
                    val arr = parts[2].split(",").mapNotNull { it.toDoubleOrNull() }.toDoubleArray()
                    if (arr.size == 192) palettes[parts[0]] = Pair(parts[1].toIntOrNull() ?: 1, arr)
                }
            }
        } catch (e: Exception) { }
    }

    private fun rememberPalette(x: DoubleArray, label: String) {
        val old = palettes[label]
        val merged = if (old == null) Pair(1, x.copyOf())
        else {
            val (n, avg) = old
            val out = DoubleArray(192) { (avg[it] * n + x[it]) / (n + 1) }
            Pair(n + 1, out)
        }
        if (palettes.size < 120 || palettes.containsKey(label)) palettes[label] = merged
        try {
            paletteFile.bufferedWriter().use { w ->
                for ((l, pn) in palettes) w.appendLine(l + "|" + pn.first + "|" + pn.second.joinToString(","))
            }
        } catch (e: Exception) { }
    }

    /** Palettes dont l'étiquette apparaît dans le texte (ou l'inverse). */
    fun matchPalettes(prompt: String): List<Pair<String, DoubleArray>> {
        val words = prompt.lowercase().split(Regex("[^\\p{L}0-9]+")).filter { it.length > 2 }
        return palettes.entries.filter { (label, _) ->
            val l = label.lowercase()
            words.any { w -> l.contains(w) || w.contains(l) }
        }.map { Pair(it.key, it.value.second) }
    }

    private fun remember(x: DoubleArray, label: String) {
        try {
            if (samples.exists() && samples.readLines().size > 400)
                samples.writeText(samples.readLines().takeLast(300).joinToString("\n") + "\n")
            samples.appendText(label + "|" + x.joinToString(",") + "\n")
        } catch (e: Exception) { }
    }

    /** Examen : l'IA repasse sur tous les échantillons mémorisés. */
    fun exam(): String = examOn(samples, nIn) { net.predict(it).first }

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

    /** Caractéristiques : MobileNet (1001) si dispo, sinon pixels (192). */
    private fun feat(bmp: Bitmap): DoubleArray = tf?.logits(bmp) ?: features(bmp)

    fun learn(bmp: Bitmap, label: String) {
        rememberPalette(features(bmp), label)
        if (tf != null) {
            // Transfer learning : les caractéristiques MobileNet sont si riches
            // que la data augmentation n'est plus nécessaire.
            val f = tf.logits(bmp)
            remember(f, label)
            net.train(f, label)
        } else {
            remember(features(bmp), label)
            net.train(features(bmp), label)
            net.train(features(bmp, mirror = true), label)
            net.train(features(bmp, bright = 1.25), label)
            net.train(features(bmp, bright = 0.8), label)
        }
        net.save(file)
    }

    fun guess(bmp: Bitmap) = net.predict(feat(bmp))

    /** Ce que la base MobileNet reconnaît toute seule (~1000 objets). */
    fun preKnowledge(bmp: Bitmap): String? =
        tf?.classify(bmp)?.let { "${it.first} (${it.second}%)" }

    fun usingTF(): Boolean = tf != null
    fun summary() = net.summary()
    fun labels(): List<String> = net.labels.toList()
    fun forget() { net.reset(); palettes.clear(); if (file.exists()) file.delete(); if (samples.exists()) samples.delete(); if (paletteFile.exists()) paletteFile.delete() }
}

/**
 * IA n°2 : SONS (v2) — analyse par DFT (Fourier), 32 bandes de fréquences.
 */
class AudioBrain(dir: File, private val tfa: TFAudio? = null) {
    private val bands = 32
    private val nIn = if (tfa != null) 521 else bands
    private val net = NeuralNet(nIn, if (tfa != null) 64 else 32)
    private val file = File(dir, if (tfa != null) "audio_tf.net" else "audio.net")
    private val samples = File(dir, if (tfa != null) "samples_aud_tf.txt" else "samples_aud.txt")
    init { if (file.exists()) net.load(file) }

    private fun remember(x: DoubleArray, label: String) {
        try {
            if (samples.exists() && samples.readLines().size > 400)
                samples.writeText(samples.readLines().takeLast(300).joinToString("\n") + "\n")
            samples.appendText(label + "|" + x.joinToString(",") + "\n")
        } catch (e: Exception) { }
    }

    /** Examen : l'IA repasse sur tous les échantillons mémorisés. */
    fun exam(): String = examOn(samples, nIn) { net.predict(it).first }

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

    /** Caractéristiques : YAMNet (521) si dispo, sinon FFT maison (32). */
    private fun feat(pcm: ShortArray): DoubleArray = tfa?.scores(pcm) ?: features(pcm)

    fun learn(pcm: ShortArray, label: String) {
        val x = feat(pcm)
        remember(x, label)
        net.train(x, label)
        net.save(file)
    }
    fun guess(pcm: ShortArray) = net.predict(feat(pcm))

    /** Ce que la base YAMNet reconnaît toute seule (~520 sons). */
    fun preKnowledge(pcm: ShortArray): String? =
        tfa?.classify(pcm)?.let { "${it.first} (${it.second}%)" }

    fun usingTF(): Boolean = tfa != null
    fun summary() = net.summary()
    fun labels(): List<String> = net.labels.toList()
    fun forget() { net.reset(); if (file.exists()) file.delete(); if (samples.exists()) samples.delete() }
}

/** Fonction d'examen commune : relit les échantillons et calcule les scores. */
internal fun examOn(samples: File, nFeatures: Int, predict: (DoubleArray) -> String): String {
    if (!samples.exists()) return "Aucun échantillon (apprends d'abord quelque chose)."
    var ok = 0; var tot = 0
    val perOk = HashMap<String, Int>(); val perTot = HashMap<String, Int>()
    for (line in samples.readLines()) {
        val i = line.indexOf('|'); if (i <= 0) continue
        val label = line.substring(0, i)
        val x = line.substring(i + 1).split(",").mapNotNull { it.toDoubleOrNull() }.toDoubleArray()
        if (x.size != nFeatures) continue
        tot++; perTot[label] = (perTot[label] ?: 0) + 1
        if (predict(x) == label) { ok++; perOk[label] = (perOk[label] ?: 0) + 1 }
    }
    if (tot == 0) return "Aucun échantillon valide."
    val per = perTot.keys.sorted().joinToString("\n") { l ->
        val o = perOk[l] ?: 0; val t = perTot[l]!!
        val star = if (o * 100 / t < 60) "  \u26A0 à entraîner !" else ""
        "  \u2022 $l : $o/$t (${o * 100 / t}%)$star"
    }
    return "Score global : $ok/$tot (${ok * 100 / tot}%)\n$per"
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
