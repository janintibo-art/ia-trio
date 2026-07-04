package com.moi.iatrio

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * TENSORFLOW LITE 🧠 : MobileNet pré-entraîné sur 1,2 million d'images.
 *
 * Rôle double :
 * 1. classify() : reconnaît ~1000 objets du monde réel DÈS L'INSTALLATION
 *    (chien, chat, guitare, tasse, voiture...) sans aucun entraînement.
 * 2. logits()   : fournit 1001 caractéristiques de haut niveau que le petit
 *    réseau maison utilise comme base (transfer learning) — ton entraînement
 *    personnalisé devient beaucoup plus précis qu'avec les pixels bruts.
 *
 * Le modèle (mobilenet_v1_quant, ~4 Mo) est téléchargé par GitHub Actions
 * pendant la construction de l'APK et embarqué dans les assets.
 * Si le modèle est absent, l'app retombe automatiquement sur le mode pixels.
 */
class TFVision private constructor(
    private val interpreter: Interpreter,
    private val labels: List<String>
) {
    companion object {
        fun tryLoad(context: Context): TFVision? = try {
            val fd = context.assets.openFd("mobilenet_v1_1.0_224_quant.tflite")
            val channel = FileInputStream(fd.fileDescriptor).channel
            val buf = channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            val interp = Interpreter(buf)
            val labels = context.assets.open("labels_mobilenet_quant_v1_224.txt")
                .bufferedReader().readLines()
            TFVision(interp, labels)
        } catch (e: Exception) {
            null   // modèle absent -> l'app utilise le mode pixels classique
        }
    }

    private val input: ByteBuffer =
        ByteBuffer.allocateDirect(224 * 224 * 3).order(ByteOrder.nativeOrder())
    private val output = Array(1) { ByteArray(1001) }
    private val pixels = IntArray(224 * 224)

    /** 1001 caractéristiques de haut niveau (0..1) pour le transfer learning. */
    @Synchronized
    fun logits(bmp: Bitmap): DoubleArray {
        val s = Bitmap.createScaledBitmap(bmp, 224, 224, true)
        input.rewind()
        s.getPixels(pixels, 0, 224, 0, 0, 224, 224)
        for (p in pixels) {
            input.put((p shr 16 and 0xFF).toByte())
            input.put((p shr 8 and 0xFF).toByte())
            input.put((p and 0xFF).toByte())
        }
        interpreter.run(input, output)
        val out = output[0]
        return DoubleArray(1001) { (out[it].toInt() and 0xFF) / 255.0 }
    }

    /** Ce que MobileNet reconnaît tout seul (parmi ~1000 objets, en anglais). */
    fun classify(bmp: Bitmap): Pair<String, Int> {
        val l = logits(bmp)
        var best = 0
        for (i in l.indices) if (l[i] > l[best]) best = i
        return Pair(labels.getOrElse(best) { "?" }, (l[best] * 100).toInt())
    }
}
