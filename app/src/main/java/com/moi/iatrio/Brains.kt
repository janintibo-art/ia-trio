package com.moi.iatrio

import android.graphics.Bitmap
import android.graphics.Color
import java.io.File
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

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

    // Cache des souvenirs en mémoire vive (pour le replay et le ré-entraînement)
    private val cache = ArrayList<Pair<String, DoubleArray>>()
    init {
        try {
            if (samples.exists()) for (line in samples.readLines()) {
                val i = line.indexOf('|'); if (i <= 0) continue
                val x = line.substring(i + 1).split(",").mapNotNull { it.toDoubleOrNull() }.toDoubleArray()
                if (x.size == nIn) cache.add(Pair(line.substring(0, i), x))
            }
        } catch (e: Exception) { }
    }

    private fun remember(x: DoubleArray, label: String) {
        cache.add(Pair(label, x))
        while (cache.size > 400) cache.removeAt(0)
        try {
            if (samples.exists() && samples.readLines().size > 400)
                samples.writeText(samples.readLines().takeLast(300).joinToString("\n") + "\n")
            samples.appendText(label + "|" + x.joinToString(",") + "\n")
        } catch (e: Exception) { }
    }

    /** REPLAY anti-oubli : après chaque nouveauté, on révise 24 vieux souvenirs. */
    private fun replay() {
        if (cache.size < 6) return
        repeat(24) {
            val (l, x) = cache[Random.nextInt(cache.size)]
            net.train(x, l, 0.03, 3)
        }
    }

    /** Consolidation complète : ré-entraîne le réseau sur TOUTE la mémoire. */
    fun retrainAll(): String {
        if (cache.isEmpty()) return "rien en mémoire"
        net.trainBatch(cache.map { Pair(it.second, it.first) }, 8)
        net.save(file)
        return "${cache.size} souvenirs consolidés (8 passes)"
    }

    fun guessTop(bmp: Bitmap): List<Pair<String, Double>> = net.predictTop(feat(bmp), 3)

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
            val f = tf.logits(bmp)
            remember(f, label)
            net.train(f, label, 0.05, 25)
        } else {
            remember(features(bmp), label)
            net.train(features(bmp), label, 0.05, 15)
            net.train(features(bmp, mirror = true), label, 0.05, 10)
            net.train(features(bmp, bright = 1.25), label, 0.05, 8)
            net.train(features(bmp, bright = 0.8), label, 0.05, 8)
        }
        replay()   // anti-oubli : on révise les anciens souvenirs
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

    private val cache = ArrayList<Pair<String, DoubleArray>>()
    init {
        try {
            if (samples.exists()) for (line in samples.readLines()) {
                val i = line.indexOf('|'); if (i <= 0) continue
                val x = line.substring(i + 1).split(",").mapNotNull { it.toDoubleOrNull() }.toDoubleArray()
                if (x.size == nIn) cache.add(Pair(line.substring(0, i), x))
            }
        } catch (e: Exception) { }
    }

    private fun remember(x: DoubleArray, label: String) {
        cache.add(Pair(label, x))
        while (cache.size > 400) cache.removeAt(0)
        try {
            if (samples.exists() && samples.readLines().size > 400)
                samples.writeText(samples.readLines().takeLast(300).joinToString("\n") + "\n")
            samples.appendText(label + "|" + x.joinToString(",") + "\n")
        } catch (e: Exception) { }
    }

    private fun replay() {
        if (cache.size < 6) return
        repeat(24) {
            val (l, x) = cache[Random.nextInt(cache.size)]
            net.train(x, l, 0.03, 3)
        }
    }

    fun retrainAll(): String {
        if (cache.isEmpty()) return "rien en mémoire"
        net.trainBatch(cache.map { Pair(it.second, it.first) }, 8)
        net.save(file)
        return "${cache.size} souvenirs consolidés (8 passes)"
    }

    fun guessTop(pcm: ShortArray): List<Pair<String, Double>> = net.predictTop(feat(pcm), 3)

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

    // BANQUE D'EXTRAITS RÉELS : vrais morceaux de TES musiques (PCM 16 kHz)
    private val clipDir = File(dir, "clips").apply { mkdirs() }
    private var clipIdx = (clipDir.listFiles()?.size ?: 0)

    /** Garde les 3 tranches les plus énergiques (0,5 s) du son appris. */
    private fun rememberClip(pcm: ShortArray, label: String) {
        try {
            val win = 8000   // 0,5 s à 16 kHz
            if (pcm.size < win) return
            val slices = ArrayList<Pair<Double, Int>>()
            var off = 0
            while (off + win <= pcm.size) {
                var e = 0.0
                for (i in off until off + win) { val v = pcm[i].toDouble() / 32768.0; e += v * v }
                slices.add(Pair(e, off)); off += win
            }
            val safe = label.lowercase().replace(Regex("[^a-z0-9_-]"), "").ifBlank { "son" }
            for ((_, o) in slices.sortedByDescending { it.first }.take(3)) {
                val f = File(clipDir, "${safe}__${clipIdx++}.pcm")
                val bytes = ByteArray(win * 2)
                for (i in 0 until win) {
                    bytes[i * 2] = (pcm[o + i].toInt() and 0xFF).toByte()
                    bytes[i * 2 + 1] = (pcm[o + i].toInt() shr 8 and 0xFF).toByte()
                }
                f.writeBytes(bytes)
            }
            // plafond : 400 extraits, on supprime les plus vieux
            val all = clipDir.listFiles()?.sortedBy { it.lastModified() } ?: return
            if (all.size > 400) all.take(all.size - 400).forEach { it.delete() }
        } catch (e: Exception) { }
    }

    /** Extraits correspondant au texte (par étiquette), sinon vide. */
    fun matchClips(prompt: String): List<ShortArray> {
        val words = prompt.lowercase().split(Regex("[^\\p{L}0-9]+")).filter { it.length > 2 }
        val files = clipDir.listFiles()?.filter { f ->
            val l = f.name.substringBefore("__").lowercase()
            words.any { w -> l.contains(w) || w.contains(l) }
        } ?: emptyList()
        return files.shuffled().take(24).mapNotNull { loadClip(it) }
    }

    /** N'importe quels extraits de ta bibliothèque (mélange). */
    fun anyClips(max: Int = 24): List<ShortArray> =
        (clipDir.listFiles()?.toList() ?: emptyList()).shuffled().take(max).mapNotNull { loadClip(it) }

    fun clipCount(): Int = clipDir.listFiles()?.size ?: 0

    private fun loadClip(f: File): ShortArray? = try {
        val b = f.readBytes()
        ShortArray(b.size / 2) { i -> (((b[i * 2 + 1].toInt() shl 8) or (b[i * 2].toInt() and 0xFF)).toShort()) }
    } catch (e: Exception) { null }

    // Mémoire des TIMBRES : empreinte spectrale moyenne (32 bandes) par étiquette
    private val timbreFile = File(dir, "timbre_aud.txt")
    private val timbres = HashMap<String, Pair<Int, DoubleArray>>()
    init {
        try {
            if (timbreFile.exists()) for (line in timbreFile.readLines()) {
                val parts = line.split("|")
                if (parts.size == 3) {
                    val arr = parts[2].split(",").mapNotNull { it.toDoubleOrNull() }.toDoubleArray()
                    if (arr.size == bands) timbres[parts[0]] = Pair(parts[1].toIntOrNull() ?: 1, arr)
                }
            }
        } catch (e: Exception) { }
    }

    private fun rememberTimbre(spec: DoubleArray, label: String) {
        val old = timbres[label]
        val merged = if (old == null) Pair(1, spec.copyOf())
        else {
            val (n, avg) = old
            Pair(n + 1, DoubleArray(bands) { (avg[it] * n + spec[it]) / (n + 1) })
        }
        if (timbres.size < 120 || timbres.containsKey(label)) timbres[label] = merged
        try {
            timbreFile.bufferedWriter().use { w ->
                for ((l, pn) in timbres) w.appendLine(l + "|" + pn.first + "|" + pn.second.joinToString(","))
            }
        } catch (e: Exception) { }
    }

    /** Timbres dont l'étiquette apparaît dans le texte (ou l'inverse). */
    fun matchTimbres(prompt: String): List<Pair<String, DoubleArray>> {
        val words = prompt.lowercase().split(Regex("[^\\p{L}0-9]+")).filter { it.length > 2 }
        return timbres.entries.filter { (label, _) ->
            val l = label.lowercase()
            words.any { w -> l.contains(w) || w.contains(l) }
        }.map { Pair(it.key, it.value.second) }
    }

    /** Toute la sonorité générale apprise (pour le mélange par défaut). */
    fun allTimbres(): List<Pair<String, DoubleArray>> =
        timbres.entries.map { Pair(it.key, it.value.second) }

    /** Caractéristiques : YAMNet (521) si dispo, sinon FFT maison (32). */
    private fun feat(pcm: ShortArray): DoubleArray = tfa?.scores(pcm) ?: features(pcm)

    fun learn(pcm: ShortArray, label: String) {
        rememberClip(pcm, label)               // extrait RÉEL pour le remix
        rememberTimbre(features(pcm), label)   // empreinte spectrale, toujours en FFT
        val x = feat(pcm)
        remember(x, label)
        net.train(x, label, 0.05, 25)
        replay()
        net.save(file)
    }
    fun guess(pcm: ShortArray) = net.predict(feat(pcm))

    /** Ce que la base YAMNet reconnaît toute seule (~520 sons). */
    fun preKnowledge(pcm: ShortArray): String? =
        tfa?.classify(pcm)?.let { "${it.first} (${it.second}%)" }

    fun usingTF(): Boolean = tfa != null
    fun summary() = net.summary()
    fun labels(): List<String> = net.labels.toList()
    fun forget() { net.reset(); timbres.clear(); if (file.exists()) file.delete(); if (samples.exists()) samples.delete(); if (timbreFile.exists()) timbreFile.delete(); clipDir.listFiles()?.forEach { it.delete() } }
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
    // Modèle à REPLI : on essaie d'abord un contexte de 5 caractères (précis),
    // puis 4, puis 3 (plus général) — complétions bien plus cohérentes.
    private val orders = intArrayOf(5, 4, 3)
    private val models = Array(orders.size) { HashMap<String, HashMap<Char, Int>>() }
    private val corpus = File(dir, "code.txt")
    init { if (corpus.exists()) learnInternal(corpus.readText()) }

    private fun learnInternal(text: String) {
        for ((oi, order) in orders.withIndex()) {
            val m = models[oi]
            for (i in 0 until text.length - order) {
                val ctx = text.substring(i, i + order)
                val next = text[i + order]
                val mm = m.getOrPut(ctx) { HashMap() }
                mm[next] = (mm[next] ?: 0) + 1
            }
        }
    }

    fun learn(text: String) {
        learnInternal(text)
        corpus.appendText(text + "\n")
        // Plafond : au-delà de 500 Ko on garde les 350 derniers Ko et on
        // reconstruit — la mémoire reste fraîche et rapide.
        try {
            if (corpus.length() > 500_000) {
                val keep = corpus.readText().takeLast(350_000)
                corpus.writeText(keep)
                for (m in models) m.clear()
                learnInternal(keep)
            }
        } catch (e: Exception) { }
    }

    /**
     * creativity 0..100 : à 0 on choisit toujours la suite la plus probable,
     * à 100 on tire au sort selon les probabilités.
     */
    fun complete(prompt: String, length: Int = 200, creativity: Int = 50): String {
        if (models[0].isEmpty() && models[1].isEmpty()) return "(rien appris : colle-moi du code d'abord)"
        val sb = StringBuilder(prompt)
        repeat(length) {
            var m: HashMap<Char, Int>? = null
            for ((oi, order) in orders.withIndex()) {
                if (sb.length < order) continue
                m = models[oi][sb.takeLast(order).toString()]
                if (m != null) break
            }
            if (m == null) return sb.toString()
            val pick: Char = if (Math.random() * 100 >= creativity) {
                m.maxByOrNull { it.value }!!.key
            } else {
                val total = m.values.sum()
                var r = (Math.random() * total).toInt()
                var p = m.keys.first()
                for ((c, nn) in m) { r -= nn; if (r < 0) { p = c; break } }
                p
            }
            sb.append(pick)
        }
        return sb.toString()
    }

    fun size() = models.sumOf { it.size }
    fun corpusExcerpt(maxChars: Int = 3000): String =
        if (corpus.exists()) corpus.readText().take(maxChars) else ""
    fun forget() { for (m in models) m.clear(); if (corpus.exists()) corpus.delete() }
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
