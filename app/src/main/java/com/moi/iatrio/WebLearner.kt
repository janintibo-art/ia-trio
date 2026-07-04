package com.moi.iatrio

import android.app.Activity
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * APPRENTISSAGE DEPUIS INTERNET :
 * - texte d'une page web -> IA code/texte
 * - images d'une page    -> IA images (avec l'étiquette donnée)
 * - raccourci Wikipédia  -> article français
 * Aucune bibliothèque externe : HttpURLConnection natif.
 */
class WebLearner(
    private val activity: Activity,
    private val image: ImageBrain,
    private val code: CodeBrain
) {
    private val maxBytes = 3_000_000      // 3 Mo max par fichier téléchargé
    private val maxTextChars = 20_000     // texte appris par page
    private val maxImagesPerPage = 8

    private fun httpGet(rawUrl: String): Pair<String, ByteArray>? {
        var url = rawUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"
        var conn: HttpURLConnection? = null
        try {
            var target = URL(url)
            // suivre jusqu'à 3 redirections (y compris http->https)
            repeat(4) {
                conn = (target.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 10000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android) IATrio/1.0")
                }
                val codeHttp = conn!!.responseCode
                if (codeHttp in 300..399) {
                    val loc = conn!!.getHeaderField("Location") ?: return null
                    conn!!.disconnect()
                    target = if (loc.startsWith("http")) URL(loc) else URL(target, loc)
                } else if (codeHttp in 200..299) {
                    val type = conn!!.contentType ?: ""
                    val out = ByteArrayOutputStream()
                    conn!!.inputStream.use { input ->
                        val buf = ByteArray(8192)
                        var total = 0
                        while (true) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            total += n
                            if (total > maxBytes) break
                            out.write(buf, 0, n)
                        }
                    }
                    return Pair(type, out.toByteArray())
                } else return null
            }
            return null
        } catch (e: Exception) {
            return null
        } finally {
            try { conn?.disconnect() } catch (e: Exception) { }
        }
    }

    /** Retire les balises HTML et garde le texte lisible. */
    private fun htmlToText(html: String): String {
        var t = html
        // supprimer scripts et styles
        t = t.replace(Regex("(?is)<script.*?</script>"), " ")
        t = t.replace(Regex("(?is)<style.*?</style>"), " ")
        // les balises deviennent des espaces / retours à la ligne
        t = t.replace(Regex("(?i)<br\\s*/?>|</p>|</div>|</li>|</h[1-6]>"), "\n")
        t = t.replace(Regex("<[^>]+>"), " ")
        // entités courantes
        t = t.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&#39;", "'").replace("&nbsp;", " ")
        // compacter
        t = t.replace(Regex("[ \\t]+"), " ").replace(Regex("\\n{3,}"), "\n\n").trim()
        return t
    }

    /** Apprend le texte d'une page web dans l'IA code/texte. */
    fun learnPageText(url: String, onDone: (String) -> Unit) {
        Thread {
            val res = httpGet(url)
            val msg = if (res == null) "Impossible de charger la page (connexion ?)"
            else {
                val (type, bytes) = res
                if (type.contains("text") || type.contains("html") || type.contains("json")) {
                    val raw = String(bytes, Charsets.UTF_8)
                    val text = if (type.contains("html")) htmlToText(raw) else raw
                    val cut = text.take(maxTextChars)
                    if (cut.length > 100) {
                        code.learn(cut)
                        "Texte appris ! ${cut.length} caractères → ${code.size()} motifs en mémoire."
                    } else "Page trop courte ou vide."
                } else "Ce n'est pas une page texte (type: ${type.take(40)})."
            }
            activity.runOnUiThread { onDone(msg) }
        }.start()
    }

    /** Télécharge les images d'une page et les apprend avec l'étiquette. */
    fun learnPageImages(url: String, label: String, onProgress: (String) -> Unit, onDone: (String) -> Unit) {
        Thread {
            val res = httpGet(url)
            if (res == null) { activity.runOnUiThread { onDone("Impossible de charger la page.") }; return@Thread }
            val (type, bytes) = res

            // Cas 1 : l'URL est directement une image
            if (type.startsWith("image/")) {
                val n = learnImageBytes(bytes, label)
                activity.runOnUiThread { onDone(if (n) "Image apprise avec l'étiquette « $label » !" else "Image illisible.") }
                return@Thread
            }

            // Cas 2 : page HTML -> extraire les <img src>
            val html = String(bytes, Charsets.UTF_8)
            val srcs = Regex("(?i)<img[^>]+src=[\"']([^\"']+)[\"']").findAll(html)
                .map { it.groupValues[1] }
                .filter { !it.startsWith("data:") }
                .distinct().take(maxImagesPerPage).toList()
            if (srcs.isEmpty()) { activity.runOnUiThread { onDone("Aucune image trouvée sur la page.") }; return@Thread }

            var learned = 0
            val base = URL(if (url.startsWith("http")) url else "https://$url")
            for ((i, src) in srcs.withIndex()) {
                val abs = try { URL(base, src).toString() } catch (e: Exception) { continue }
                activity.runOnUiThread { onProgress("Image ${i + 1}/${srcs.size}...") }
                val imgRes = httpGet(abs) ?: continue
                if (imgRes.first.startsWith("image/") && learnImageBytes(imgRes.second, label)) learned++
            }
            activity.runOnUiThread {
                onDone("$learned image(s) apprise(s) avec l'étiquette « $label » ! ${image.summary()}")
            }
        }.start()
    }

    private fun learnImageBytes(bytes: ByteArray, label: String): Boolean = try {
        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        if (bmp != null && bmp.width > 8 && bmp.height > 8) {
            image.learn(bmp, label); bmp.recycle(); true
        } else false
    } catch (e: Exception) { false }

    /** Raccourci : apprend l'article Wikipédia français d'un sujet. */
    fun learnWikipedia(topic: String, onDone: (String) -> Unit) {
        val t = topic.trim().replace(' ', '_')
        val enc = URLEncoder.encode(t, "UTF-8")
        learnPageText("https://fr.wikipedia.org/wiki/$enc") { msg ->
            onDone(if (msg.startsWith("Texte appris")) "Wikipédia « $topic » : $msg" else msg)
        }
    }
}
