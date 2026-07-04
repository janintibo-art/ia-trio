package com.moi.iatrio

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel

/**
 * YAMNET 🧠 : l'équivalent de MobileNet pour les SONS.
 * Pré-entraîné par Google sur AudioSet : reconnaît ~520 sons du monde réel
 * (aboiement, guitare, sirène, pluie, rire, klaxon...) dès l'installation.
 *
 * Rôle double, comme MobileNet pour les images :
 * 1. classify() : ce que YAMNet reconnaît tout seul.
 * 2. scores()   : 521 caractéristiques de haut niveau pour le transfer
 *    learning du petit réseau maison — précision audio incomparable.
 *
 * Le modèle (~4 Mo) est téléchargé par GitHub Actions et embarqué dans l'APK.
 * S'il est absent, l'app retombe automatiquement sur le mode FFT maison.
 */
class TFAudio private constructor(
    private val interpreter: Interpreter,
    private val labels: List<String>
) {
    companion object {
        fun tryLoad(context: Context): TFAudio? = try {
            val fd = context.assets.openFd("yamnet.tflite")
            val channel = FileInputStream(fd.fileDescriptor).channel
            val buf = channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            val interp = Interpreter(buf)
            // yamnet_class_map.csv : index,mid,display_name
            val labels = context.assets.open("yamnet_class_map.csv").bufferedReader()
                .readLines().drop(1).map { line ->
                    line.split(",", limit = 3).getOrElse(2) { "?" }.trim('"')
                }
            val tfa = TFAudio(interp, labels)
            tfa.scores(ShortArray(16000))   // test sur du silence : lève si incompatible
            tfa
        } catch (e: Exception) {
            null   // modèle absent ou incompatible -> repli FFT maison
        }
    }

    private val input = FloatArray(15600)          // ~0,975 s à 16 kHz
    private val output = Array(1) { FloatArray(521) }

    /** 521 scores (0..1) pour le transfer learning. pcm attendu à 16 kHz. */
    @Synchronized
    fun scores(pcm: ShortArray): DoubleArray {
        for (i in input.indices) input[i] = if (i < pcm.size) pcm[i] / 32768f else 0f
        interpreter.run(input, output)
        val out = output[0]
        return DoubleArray(521) { out[it].toDouble().coerceIn(0.0, 1.0) }
    }

    /** Ce que YAMNet entend tout seul (~520 sons, en anglais). */
    fun classify(pcm: ShortArray): Pair<String, Int> {
        val s = scores(pcm)
        var best = 0
        for (i in s.indices) if (s[i] > s[best]) best = i
        return Pair(FrenchLabels.translate(labels.getOrElse(best) { "?" }), (s[best] * 100).toInt())
    }
}
