package com.moi.iatrio

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.provider.DocumentsContract
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteOrder

/**
 * ENTRAÎNEMENT MASSIF (v10) : deux modes.
 * 1. scan(treeUri)  : un dossier choisi (sélecteur Android, marche pour la SD)
 * 2. scanAll()      : TOUT le téléphone + carte(s) SD (permission "Tous les
 *    fichiers" requise). Ignore les dossiers cachés et Android/ (système).
 * Tout reste 100% local.
 */
class ScanTrainer(
    private val activity: Activity,
    private val image: ImageBrain,
    private val audio: AudioBrain,
    private val code: CodeBrain
) {
    @Volatile var cancel = false

    private val maxImages = 300
    private val maxTexts = 60
    private val maxAudio = 60
    private val max3d = 40

    private val imgExt = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif")
    private val audioExt = setOf("mp3", "flac", "ogg", "m4a", "aac", "opus")
    private val d3Ext = setOf("obj", "stl")
    private val txtExt = setOf(
        "txt", "md", "kt", "java", "py", "js", "ts", "jsx", "tsx", "html", "css",
        "json", "xml", "c", "cpp", "h", "hpp", "sh", "php", "rb", "go", "rs",
        "swift", "sql", "yml", "yaml", "csv", "ini", "log", "gradle", "properties", "bat"
    )

    private var nImg = 0; private var nTxt = 0; private var nAud = 0; private var n3d = 0
    private var visited = 0
    private var visitLimit = 4000

    // ==================== MODE 1 : DOSSIER CHOISI (SAF) ====================
    fun scan(treeUri: Uri, onProgress: (String) -> Unit, onDone: (String) -> Unit) {
        cancel = false
        nImg = 0; nTxt = 0; nAud = 0; n3d = 0; visited = 0
        visitLimit = 4000
        Thread {
            try {
                val rootId = DocumentsContract.getTreeDocumentId(treeUri)
                val rootName = rootId.substringAfterLast(':').substringAfterLast('/')
                    .ifBlank { "racine" }
                walkTree(treeUri, rootId, rootName, onProgress)
                activity.runOnUiThread { onDone(doneMsg()) }
            } catch (e: Exception) {
                activity.runOnUiThread { onDone("Erreur : ${e.message}") }
            }
        }.start()
    }

    private fun walkTree(treeUri: Uri, docId: String, folderName: String, onProgress: (String) -> Unit) {
        if (cancel || full() || visited > visitLimit) return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val cursor = activity.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null, null, null
        ) ?: return
        cursor.use { c ->
            while (c.moveToNext()) {
                if (cancel || full() || visited > visitLimit) return
                visited++
                val id = c.getString(0)
                val name = c.getString(1) ?: continue
                val mime = c.getString(2) ?: ""
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    walkTree(treeUri, id, name, onProgress)
                } else {
                    val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                    dispatch(name, folderName, onProgress,
                        streamProvider = { activity.contentResolver.openInputStream(uri) },
                        audioSource = { decodeCompressedUri(uri) })
                }
            }
        }
    }

    // ==================== MODE 2 : TOUT LE TÉLÉPHONE + SD ====================
    fun scanAll(onProgress: (String) -> Unit, onDone: (String) -> Unit) {
        cancel = false
        nImg = 0; nTxt = 0; nAud = 0; n3d = 0; visited = 0
        visitLimit = 25_000   // scan complet : on visite beaucoup plus de fichiers
        Thread {
            try {
                val roots = mutableListOf<File>()
                val main = File("/storage/emulated/0")
                if (main.canRead()) roots.add(main)
                // Cartes SD / USB : /storage/XXXX-XXXX
                File("/storage").listFiles()?.forEach {
                    if (it.isDirectory && it.name != "emulated" && it.name != "self" && it.canRead())
                        roots.add(it)
                }
                if (roots.isEmpty()) {
                    activity.runOnUiThread { onDone("Aucun stockage lisible — as-tu accordé « Accès à tous les fichiers » ?") }
                    return@Thread
                }
                for (r in roots) walkFile(r, if (r.name == "0") "telephone" else "carte-sd", onProgress)
                activity.runOnUiThread { onDone(doneMsg() + " (${roots.size} stockage(s) parcourus)") }
            } catch (e: Exception) {
                activity.runOnUiThread { onDone("Erreur : ${e.message}") }
            }
        }.start()
    }

    private fun walkFile(dir: File, label: String, onProgress: (String) -> Unit) {
        if (cancel || full() || visited > visitLimit) return
        val kids = dir.listFiles() ?: return
        for (f in kids) {
            if (cancel || full() || visited > visitLimit) return
            val name = f.name
            if (name.startsWith(".")) continue                       // dossiers/fichiers cachés
            if (f.isDirectory) {
                if (name == "Android" || name == "LOST.DIR") continue // système : inaccessible/inutile
                walkFile(f, name, onProgress)
            } else {
                visited++
                dispatch(name, label, onProgress,
                    streamProvider = { FileInputStream(f) },
                    audioSource = { decodeCompressedPath(f.absolutePath) })
            }
        }
    }

    // ==================== DISPATCH COMMUN ====================
    private fun dispatch(
        fileName: String,
        label: String,
        onProgress: (String) -> Unit,
        streamProvider: () -> InputStream?,
        audioSource: () -> ShortArray?
    ) {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        when {
            ext in imgExt && nImg < maxImages ->
                if (learnImageStream(streamProvider, label)) { nImg++; progress(onProgress) }
            ext in d3Ext && n3d < max3d ->
                if (learn3dStream(streamProvider, ext, label)) { n3d++; progress(onProgress) }
            ext == "wav" && nAud < maxAudio ->
                if (learnWavStream(streamProvider, label)) { nAud++; progress(onProgress) }
            ext in audioExt && nAud < maxAudio -> {
                val pcm = audioSource()
                if (pcm != null) {
                    try { audio.learn(pcm, label); nAud++; progress(onProgress) } catch (e: Exception) { }
                }
            }
            ext in txtExt && nTxt < maxTexts ->
                if (learnTextStream(streamProvider)) { nTxt++; progress(onProgress) }
        }
    }

    private fun full(): Boolean =
        nImg >= maxImages && nTxt >= maxTexts && nAud >= maxAudio && n3d >= max3d

    private fun doneMsg(): String =
        "Terminé ! $nImg images, $n3d modèles 3D, $nAud sons, $nTxt textes appris." +
                if (cancel) " (interrompu)" else ""

    private fun progress(onProgress: (String) -> Unit) {
        val t = "Scan... $nImg img, $n3d 3D, $nAud sons, $nTxt txt ($visited fichiers vus)"
        activity.runOnUiThread { onProgress(t) }
    }

    // ==================== APPRENTISSAGES (flux génériques) ====================
    private fun learnImageStream(provider: () -> InputStream?, label: String): Boolean = try {
        provider()?.use { input ->
            val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
            val bmp = BitmapFactory.decodeStream(input, null, opts)
            if (bmp != null) { image.learn(bmp, label); bmp.recycle(); true } else false
        } ?: false
    } catch (e: Exception) { false }

    private fun learnTextStream(provider: () -> InputStream?): Boolean = try {
        provider()?.use { input ->
            val bytes = ByteArray(10_000)
            val n = input.read(bytes)
            if (n > 50) {
                val text = String(bytes, 0, n, Charsets.UTF_8)
                val weird = text.count { it.code < 9 }
                if (weird < n / 20) { code.learn(text); true } else false
            } else false
        } ?: false
    } catch (e: Exception) { false }

    private fun learnWavStream(provider: () -> InputStream?, label: String): Boolean = try {
        provider()?.use { input ->
            val din = DataInputStream(input)
            val header = ByteArray(44)
            din.readFully(header)
            if (header[0].toInt() != 'R'.code || header[8].toInt() != 'W'.code) return false
            val raw = ByteArray(64_000)
            val n = din.read(raw)
            if (n > 2000) {
                val pcm = ShortArray(n / 2)
                for (i in pcm.indices) {
                    val lo = raw[i * 2].toInt() and 0xFF
                    val hi = raw[i * 2 + 1].toInt()
                    pcm[i] = ((hi shl 8) or lo).toShort()
                }
                audio.learn(pcm, label); true
            } else false
        } ?: false
    } catch (e: Exception) { false }

    // ==================== AUDIO COMPRESSÉ ====================
    private fun decodeCompressedUri(uri: Uri): ShortArray? =
        decodeWith { it.setDataSource(activity, uri, null) }

    private fun decodeCompressedPath(path: String): ShortArray? =
        decodeWith { it.setDataSource(path) }

    private fun decodeWith(setSource: (MediaExtractor) -> Unit): ShortArray? {
        var extractor: MediaExtractor? = null
        var codec: MediaCodec? = null
        try {
            extractor = MediaExtractor()
            setSource(extractor)
            var track = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                if ((f.getString(MediaFormat.KEY_MIME) ?: "").startsWith("audio/")) {
                    track = i; format = f; break
                }
            }
            if (track < 0 || format == null) return null
            val mime = format.getString(MediaFormat.KEY_MIME)!!
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = try { format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) } catch (e: Exception) { 1 }
            extractor.selectTrack(track)
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val wanted = sampleRate * 2
            val out = ShortArray(wanted)
            var filled = 0
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            val deadline = System.currentTimeMillis() + 8000

            while (filled < wanted && System.currentTimeMillis() < deadline) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(10_000)
                    if (inIdx >= 0) {
                        val buf = codec.getInputBuffer(inIdx)!!
                        val size = extractor.readSampleData(buf, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIdx = codec.dequeueOutputBuffer(info, 10_000)
                if (outIdx >= 0) {
                    val buf = codec.getOutputBuffer(outIdx)!!
                    buf.order(ByteOrder.LITTLE_ENDIAN)
                    val sb = buf.asShortBuffer()
                    val n = sb.remaining()
                    var i = 0
                    while (i < n && filled < wanted) {
                        out[filled++] = sb.get(i)
                        i += channels
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
            }
            return if (filled > 2000) out.copyOf(filled) else null
        } catch (e: Exception) {
            return null
        } finally {
            try { codec?.stop(); codec?.release() } catch (e: Exception) { }
            try { extractor?.release() } catch (e: Exception) { }
        }
    }

    // ==================== FICHIERS 3D (OBJ / STL) ====================
    private fun learn3dStream(provider: () -> InputStream?, ext: String, label: String): Boolean {
        val verts = try {
            provider()?.use { if (ext == "obj") readObj(it) else readStl(it) }
        } catch (e: Exception) { null } ?: return false
        if (verts.size < 30) return false
        val bmp = projectTo3Views(verts) ?: return false
        return try { image.learn(bmp, label); bmp.recycle(); true } catch (e: Exception) { false }
    }

    private fun readObj(input: InputStream): List<FloatArray> {
        val verts = ArrayList<FloatArray>()
        val br = BufferedReader(InputStreamReader(input))
        var line: String?
        var count = 0
        while (br.readLine().also { line = it } != null && count < 20_000) {
            val l = line!!
            if (l.startsWith("v ")) {
                val p = l.split(" ", "\t").filter { it.isNotBlank() }
                if (p.size >= 4) {
                    val x = p[1].toFloatOrNull(); val y = p[2].toFloatOrNull(); val z = p[3].toFloatOrNull()
                    if (x != null && y != null && z != null) { verts.add(floatArrayOf(x, y, z)); count++ }
                }
            }
        }
        return verts
    }

    private fun readStl(input: InputStream): List<FloatArray>? {
        val head = ByteArray(84)
        val hn = input.read(head)
        if (hn < 84) return null
        val headText = String(head, 0, 5, Charsets.US_ASCII).lowercase()
        val verts = ArrayList<FloatArray>()
        if (headText == "solid") {
            val rest = BufferedReader(InputStreamReader(input))
            var line: String?
            var count = 0
            while (rest.readLine().also { line = it } != null && count < 20_000) {
                val l = line!!.trim()
                if (l.startsWith("vertex")) {
                    val p = l.split(" ", "\t").filter { it.isNotBlank() }
                    if (p.size >= 4) {
                        val x = p[1].toFloatOrNull(); val y = p[2].toFloatOrNull(); val z = p[3].toFloatOrNull()
                        if (x != null && y != null && z != null) { verts.add(floatArrayOf(x, y, z)); count++ }
                    }
                }
            }
        } else {
            val nTri = ((head[83].toInt() and 0xFF) shl 24) or ((head[82].toInt() and 0xFF) shl 16) or
                    ((head[81].toInt() and 0xFF) shl 8) or (head[80].toInt() and 0xFF)
            val limit = minOf(nTri, 6000)
            val tri = ByteArray(50)
            val din = DataInputStream(input)
            for (t in 0 until limit) {
                try { din.readFully(tri) } catch (e: Exception) { break }
                for (v in 0 until 3) {
                    val off = 12 + v * 12
                    verts.add(floatArrayOf(leFloat(tri, off), leFloat(tri, off + 4), leFloat(tri, off + 8)))
                }
            }
        }
        return verts
    }

    private fun leFloat(b: ByteArray, o: Int): Float {
        val bits = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8) or
                ((b[o + 2].toInt() and 0xFF) shl 16) or ((b[o + 3].toInt() and 0xFF) shl 24)
        return Float.fromBits(bits)
    }

    private fun projectTo3Views(verts: List<FloatArray>): Bitmap? {
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
        for (v in verts) {
            if (v[0] < minX) minX = v[0]; if (v[0] > maxX) maxX = v[0]
            if (v[1] < minY) minY = v[1]; if (v[1] > maxY) maxY = v[1]
            if (v[2] < minZ) minZ = v[2]; if (v[2] > maxZ) maxZ = v[2]
        }
        val dx = (maxX - minX).takeIf { it > 0 } ?: return null
        val dy = (maxY - minY).takeIf { it > 0 } ?: 1f
        val dz = (maxZ - minZ).takeIf { it > 0 } ?: 1f
        val g = 8
        val xy = Array(g) { IntArray(g) }
        val xz = Array(g) { IntArray(g) }
        val yz = Array(g) { IntArray(g) }
        for (v in verts) {
            val ix = (((v[0] - minX) / dx) * (g - 1)).toInt().coerceIn(0, g - 1)
            val iy = (((v[1] - minY) / dy) * (g - 1)).toInt().coerceIn(0, g - 1)
            val iz = (((v[2] - minZ) / dz) * (g - 1)).toInt().coerceIn(0, g - 1)
            xy[iy][ix]++; xz[iz][ix]++; yz[iz][iy]++
        }
        fun norm(m: Array<IntArray>): Array<IntArray> {
            var mx = 1
            for (r in m) for (c in r) if (c > mx) mx = c
            return Array(g) { y -> IntArray(g) { x -> (m[y][x] * 255) / mx } }
        }
        val r = norm(xy); val gr = norm(xz); val b = norm(yz)
        val bmp = Bitmap.createBitmap(g, g, Bitmap.Config.ARGB_8888)
        for (y in 0 until g) for (x in 0 until g)
            bmp.setPixel(x, y, Color.rgb(r[y][x], gr[y][x], b[y][x]))
        return bmp
    }
}
