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
import java.io.InputStreamReader
import java.nio.ByteOrder

/**
 * ENTRAÎNEMENT MASSIF (v5) : parcourt un dossier choisi par l'utilisateur et
 * entraîne automatiquement les 3 IA. Tout reste 100% local.
 * - Images  : jpg jpeg png webp bmp gif        -> étiquette = dossier parent
 * - 3D      : obj stl -> projections 3 vues (R=dessus G=face B=côté) -> IA images
 * - Audio   : wav (lecture directe) + mp3 flac ogg m4a aac opus (décodeur Android)
 * - Textes  : ~30 formats de code et texte -> IA code
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
    private val maxVisited = 4000

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

    fun scan(treeUri: Uri, onProgress: (String) -> Unit, onDone: (String) -> Unit) {
        cancel = false
        nImg = 0; nTxt = 0; nAud = 0; n3d = 0; visited = 0
        Thread {
            try {
                val rootId = DocumentsContract.getTreeDocumentId(treeUri)
                val rootName = rootId.substringAfterLast(':').substringAfterLast('/')
                    .ifBlank { "racine" }
                walk(treeUri, rootId, rootName, onProgress)
                val msg = "Terminé ! $nImg images, $n3d modèles 3D, $nAud sons, $nTxt textes appris." +
                        if (cancel) " (interrompu)" else ""
                activity.runOnUiThread { onDone(msg) }
            } catch (e: Exception) {
                activity.runOnUiThread { onDone("Erreur : ${e.message}") }
            }
        }.start()
    }

    private fun full(): Boolean =
        nImg >= maxImages && nTxt >= maxTexts && nAud >= maxAudio && n3d >= max3d

    private fun walk(treeUri: Uri, docId: String, folderName: String, onProgress: (String) -> Unit) {
        if (cancel || full() || visited > maxVisited) return
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
                if (cancel || full() || visited > maxVisited) return
                visited++
                val id = c.getString(0)
                val name = c.getString(1) ?: continue
                val mime = c.getString(2) ?: ""
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    walk(treeUri, id, name, onProgress)
                } else {
                    val ext = name.substringAfterLast('.', "").lowercase()
                    val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                    when {
                        ext in imgExt && nImg < maxImages ->
                            if (learnImage(uri, folderName)) { nImg++; progress(onProgress) }
                        ext in d3Ext && n3d < max3d ->
                            if (learn3d(uri, ext, folderName)) { n3d++; progress(onProgress) }
                        ext == "wav" && nAud < maxAudio ->
                            if (learnWav(uri, folderName)) { nAud++; progress(onProgress) }
                        ext in audioExt && nAud < maxAudio ->
                            if (learnCompressedAudio(uri, folderName)) { nAud++; progress(onProgress) }
                        ext in txtExt && nTxt < maxTexts ->
                            if (learnText(uri)) { nTxt++; progress(onProgress) }
                    }
                }
            }
        }
    }

    private fun progress(onProgress: (String) -> Unit) {
        val t = "Scan... $nImg img, $n3d 3D, $nAud sons, $nTxt txt ($visited fichiers vus)"
        activity.runOnUiThread { onProgress(t) }
    }

    // ---------- IMAGES ----------
    private fun learnImage(uri: Uri, label: String): Boolean = try {
        activity.contentResolver.openInputStream(uri)?.use { input ->
            val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
            val bmp = BitmapFactory.decodeStream(input, null, opts)
            if (bmp != null) { image.learn(bmp, label); bmp.recycle(); true } else false
        } ?: false
    } catch (e: Exception) { false }

    // ---------- TEXTES / CODE ----------
    private fun learnText(uri: Uri): Boolean = try {
        activity.contentResolver.openInputStream(uri)?.use { input ->
            val bytes = ByteArray(10_000)
            val n = input.read(bytes)
            if (n > 50) {
                val text = String(bytes, 0, n, Charsets.UTF_8)
                val weird = text.count { it.code < 9 }
                if (weird < n / 20) { code.learn(text); true } else false
            } else false
        } ?: false
    } catch (e: Exception) { false }

    // ---------- AUDIO WAV ----------
    private fun learnWav(uri: Uri, label: String): Boolean = try {
        activity.contentResolver.openInputStream(uri)?.use { input ->
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

    // ---------- AUDIO MP3/FLAC/OGG/M4A/AAC/OPUS via le décodeur Android ----------
    private fun learnCompressedAudio(uri: Uri, label: String): Boolean {
        val pcm = decodeCompressed(uri) ?: return false
        return try { audio.learn(pcm, label); true } catch (e: Exception) { false }
    }

    private fun decodeCompressed(uri: Uri): ShortArray? {
        var extractor: MediaExtractor? = null
        var codec: MediaCodec? = null
        try {
            extractor = MediaExtractor()
            extractor.setDataSource(activity, uri, null)
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

            val wanted = sampleRate * 2   // 2 secondes, mono
            val out = ShortArray(wanted)
            var filled = 0
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            val deadline = System.currentTimeMillis() + 8000  // garde-fou 8s par fichier

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
                        out[filled++] = sb.get(i)  // canal gauche si stéréo
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

    // ---------- FICHIERS 3D (OBJ / STL) ----------
    // On lit les sommets du modèle, puis on fabrique une mini-image 8x8 où
    // R = vue de dessus (XY), G = vue de face (XZ), B = vue de côté (YZ).
    // L'IA images apprend ainsi la FORME 3D avec le dossier comme étiquette.
    private fun learn3d(uri: Uri, ext: String, label: String): Boolean {
        val verts = try {
            when (ext) {
                "obj" -> readObj(uri)
                else -> readStl(uri)
            }
        } catch (e: Exception) { null } ?: return false
        if (verts.size < 30) return false
        val bmp = projectTo3Views(verts) ?: return false
        return try { image.learn(bmp, label); bmp.recycle(); true } catch (e: Exception) { false }
    }

    private fun readObj(uri: Uri): List<FloatArray>? =
        activity.contentResolver.openInputStream(uri)?.use { input ->
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
            verts
        }

    private fun readStl(uri: Uri): List<FloatArray>? =
        activity.contentResolver.openInputStream(uri)?.use { input ->
            val head = ByteArray(84)
            val hn = input.read(head)
            if (hn < 84) return null
            val headText = String(head, 0, 5, Charsets.US_ASCII).lowercase()
            val verts = ArrayList<FloatArray>()
            if (headText == "solid") {
                // STL ASCII : lignes "vertex x y z"
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
                // STL binaire : nb triangles (uint32 LE) puis 50 octets/triangle
                val nTri = ((head[83].toInt() and 0xFF) shl 24) or ((head[82].toInt() and 0xFF) shl 16) or
                        ((head[81].toInt() and 0xFF) shl 8) or (head[80].toInt() and 0xFF)
                val limit = minOf(nTri, 6000)
                val tri = ByteArray(50)
                val din = DataInputStream(input)
                for (t in 0 until limit) {
                    try { din.readFully(tri) } catch (e: Exception) { break }
                    // 12 octets de normale, puis 3 sommets de 12 octets
                    for (v in 0 until 3) {
                        val off = 12 + v * 12
                        val x = leFloat(tri, off); val y = leFloat(tri, off + 4); val z = leFloat(tri, off + 8)
                        verts.add(floatArrayOf(x, y, z))
                    }
                }
            }
            verts
        }

    private fun leFloat(b: ByteArray, o: Int): Float {
        val bits = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8) or
                ((b[o + 2].toInt() and 0xFF) shl 16) or ((b[o + 3].toInt() and 0xFF) shl 24)
        return Float.fromBits(bits)
    }

    private fun projectTo3Views(verts: List<FloatArray>): Bitmap? {
        // normaliser dans [0,1]
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
        val xy = Array(g) { IntArray(g) }  // vue de dessus
        val xz = Array(g) { IntArray(g) }  // vue de face
        val yz = Array(g) { IntArray(g) }  // vue de côté
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
