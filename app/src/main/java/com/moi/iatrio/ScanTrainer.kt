package com.moi.iatrio

import android.app.Activity
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import java.io.DataInputStream

/**
 * ENTRAÎNEMENT MASSIF : parcourt un dossier choisi par l'utilisateur
 * (téléphone ou carte SD) et entraîne automatiquement les 3 IA :
 * - images (.jpg .jpeg .png .webp) -> étiquette = nom du dossier parent
 * - textes/code (.txt .md .kt .java .py .js .html .css .json .xml .c .cpp .sh)
 * - sons (.wav 16 bits) -> étiquette = nom du dossier parent
 * Tout reste 100% local, rien ne quitte le téléphone.
 */
class ScanTrainer(
    private val activity: Activity,
    private val image: ImageBrain,
    private val audio: AudioBrain,
    private val code: CodeBrain
) {
    @Volatile var cancel = false

    // Limites pour rester rapide et ne pas saturer la mémoire
    private val maxImages = 300
    private val maxTexts = 60
    private val maxWavs = 60
    private val maxVisited = 4000

    private val imgExt = setOf("jpg", "jpeg", "png", "webp")
    private val txtExt = setOf("txt", "md", "kt", "java", "py", "js", "html", "css", "json", "xml", "c", "cpp", "sh")

    private var nImg = 0; private var nTxt = 0; private var nWav = 0; private var visited = 0

    fun scan(treeUri: Uri, onProgress: (String) -> Unit, onDone: (String) -> Unit) {
        cancel = false
        nImg = 0; nTxt = 0; nWav = 0; visited = 0
        Thread {
            try {
                val rootId = DocumentsContract.getTreeDocumentId(treeUri)
                val rootName = rootId.substringAfterLast(':').substringAfterLast('/')
                    .ifBlank { "racine" }
                walk(treeUri, rootId, rootName, onProgress)
                val msg = "Terminé ! $nImg images, $nTxt textes, $nWav sons appris." +
                        if (cancel) " (interrompu)" else ""
                activity.runOnUiThread { onDone(msg) }
            } catch (e: Exception) {
                activity.runOnUiThread { onDone("Erreur : ${e.message}") }
            }
        }.start()
    }

    private fun full(): Boolean =
        nImg >= maxImages && nTxt >= maxTexts && nWav >= maxWavs

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
                        ext in imgExt && nImg < maxImages -> {
                            if (learnImage(uri, folderName)) {
                                nImg++
                                progress(onProgress)
                            }
                        }
                        ext in txtExt && nTxt < maxTexts -> {
                            if (learnText(uri)) {
                                nTxt++
                                progress(onProgress)
                            }
                        }
                        ext == "wav" && nWav < maxWavs -> {
                            if (learnWav(uri, folderName)) {
                                nWav++
                                progress(onProgress)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun progress(onProgress: (String) -> Unit) {
        val t = "Scan... $nImg img, $nTxt txt, $nWav wav ($visited fichiers vus)"
        activity.runOnUiThread { onProgress(t) }
    }

    private fun learnImage(uri: Uri, label: String): Boolean = try {
        activity.contentResolver.openInputStream(uri)?.use { input ->
            val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
            val bmp = BitmapFactory.decodeStream(input, null, opts)
            if (bmp != null) { image.learn(bmp, label); bmp.recycle(); true } else false
        } ?: false
    } catch (e: Exception) { false }

    private fun learnText(uri: Uri): Boolean = try {
        activity.contentResolver.openInputStream(uri)?.use { input ->
            val bytes = ByteArray(10_000)
            val n = input.read(bytes)
            if (n > 50) {
                val text = String(bytes, 0, n, Charsets.UTF_8)
                // ignorer les fichiers binaires déguisés
                val weird = text.count { it.code < 9 }
                if (weird < n / 20) { code.learn(text); true } else false
            } else false
        } ?: false
    } catch (e: Exception) { false }

    private fun learnWav(uri: Uri, label: String): Boolean = try {
        activity.contentResolver.openInputStream(uri)?.use { input ->
            val din = DataInputStream(input)
            val header = ByteArray(44)
            din.readFully(header)
            // verif "RIFF" et "WAVE"
            if (header[0].toInt() != 'R'.code || header[8].toInt() != 'W'.code) return false
            val raw = ByteArray(64_000) // ~2s en 16kHz 16bit
            val n = din.read(raw)
            if (n > 2000) {
                val pcm = ShortArray(n / 2)
                for (i in pcm.indices) {
                    val lo = raw[i * 2].toInt() and 0xFF
                    val hi = raw[i * 2 + 1].toInt()
                    pcm[i] = ((hi shl 8) or lo).toShort()
                }
                audio.learn(pcm, label)
                true
            } else false
        } ?: false
    } catch (e: Exception) { false }
}
