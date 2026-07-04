package com.moi.iatrio

import java.io.File
import kotlin.math.exp
import kotlin.random.Random

/**
 * Mini réseau de neurones (1 couche cachée) 100% local.
 * v2 : compte le nombre d'exemples appris par classe + remise à zéro.
 */
class NeuralNet(private val nIn: Int, private val nHid: Int) {

    val labels = mutableListOf<String>()
    val counts = mutableListOf<Int>()
    private var w1 = Array(nHid) { DoubleArray(nIn) { (Random.nextDouble() - 0.5) * 0.1 } }
    private var b1 = DoubleArray(nHid)
    private val w2 = mutableListOf<DoubleArray>()
    private val b2 = mutableListOf<Double>()

    private fun ensureLabel(label: String): Int {
        var i = labels.indexOf(label)
        if (i < 0) {
            labels.add(label); counts.add(0)
            w2.add(DoubleArray(nHid) { (Random.nextDouble() - 0.5) * 0.1 }); b2.add(0.0)
            i = labels.size - 1
        }
        return i
    }

    private fun forward(x: DoubleArray): Pair<DoubleArray, DoubleArray> {
        val h = DoubleArray(nHid)
        for (j in 0 until nHid) {
            var s = b1[j]
            for (i in 0 until nIn) s += w1[j][i] * x[i]
            h[j] = if (s > 0) s else 0.01 * s
        }
        val o = DoubleArray(labels.size)
        for (k in labels.indices) {
            var s = b2[k]
            for (j in 0 until nHid) s += w2[k][j] * h[j]
            o[k] = s
        }
        val m = o.maxOrNull() ?: 0.0
        var sum = 0.0
        for (k in o.indices) { o[k] = exp(o[k] - m); sum += o[k] }
        if (sum > 0) for (k in o.indices) o[k] /= sum
        return Pair(h, o)
    }

    fun train(x: DoubleArray, label: String, lr: Double = 0.05, epochs: Int = 40) {
        val t = ensureLabel(label)
        counts[t] = counts[t] + 1
        repeat(epochs) {
            val (h, o) = forward(x)
            val dOut = DoubleArray(labels.size) { k -> o[k] - if (k == t) 1.0 else 0.0 }
            val dH = DoubleArray(nHid)
            for (k in labels.indices) for (j in 0 until nHid) dH[j] += dOut[k] * w2[k][j]
            for (k in labels.indices) {
                for (j in 0 until nHid) w2[k][j] -= lr * dOut[k] * h[j]
                b2[k] = b2[k] - lr * dOut[k]
            }
            for (j in 0 until nHid) {
                val g = if (h[j] > 0) dH[j] else 0.01 * dH[j]
                for (i in 0 until nIn) w1[j][i] -= lr * g * x[i]
                b1[j] -= lr * g
            }
        }
    }

    fun predict(x: DoubleArray): Pair<String, Double> {
        if (labels.isEmpty()) return Pair("(rien appris)", 0.0)
        val (_, o) = forward(x)
        var best = 0
        for (k in o.indices) if (o[k] > o[best]) best = k
        return Pair(labels[best], o[best])
    }

    fun summary(): String =
        if (labels.isEmpty()) "rien appris"
        else labels.indices.joinToString(", ") { "${labels[it]}:${counts[it]}" }

    fun reset() {
        labels.clear(); counts.clear(); w2.clear(); b2.clear()
        w1 = Array(nHid) { DoubleArray(nIn) { (Random.nextDouble() - 0.5) * 0.1 } }
        b1 = DoubleArray(nHid)
    }

    fun save(f: File) {
        f.bufferedWriter().use { w ->
            w.appendLine(labels.joinToString("|"))
            w.appendLine(counts.joinToString(","))
            w.appendLine(b1.joinToString(","))
            for (row in w1) w.appendLine(row.joinToString(","))
            w.appendLine(b2.joinToString(","))
            for (row in w2) w.appendLine(row.joinToString(","))
        }
    }

    fun load(f: File) {
        try {
            val lines = f.readLines()
            if (lines.size < 3) return
            labels.clear(); counts.clear(); w2.clear(); b2.clear()
            if (lines[0].isNotBlank()) labels.addAll(lines[0].split("|"))
            if (lines[1].isNotBlank()) counts.addAll(lines[1].split(",").map { it.toInt() })
            b1 = lines[2].split(",").map { it.toDouble() }.toDoubleArray()
            var idx = 3
            for (j in 0 until nHid) w1[j] = lines[idx++].split(",").map { it.toDouble() }.toDoubleArray()
            if (labels.isNotEmpty()) {
                b2.addAll(lines[idx++].split(",").map { it.toDouble() })
                repeat(labels.size) { w2.add(lines[idx++].split(",").map { it.toDouble() }.toDoubleArray()) }
            }
            while (counts.size < labels.size) counts.add(0)
        } catch (e: Exception) { reset() }
    }
}
