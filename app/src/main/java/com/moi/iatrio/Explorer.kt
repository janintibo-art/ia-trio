package com.moi.iatrio

import android.app.Activity
import android.graphics.BitmapFactory
import org.json.JSONArray
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * EXPLORATEUR AUTONOME 🧭 : l'IA part seule sur le web.
 *
 * - Donne-lui un sujet de départ, ou laisse vide : elle choisit selon sa
 *   CURIOSITÉ (les étiquettes qu'elle connaît et les mots de sa mémoire).
 * - Elle lit une page, apprend le texte, repère les liens qui l'intriguent,
 *   et continue de page en page — journal de bord en direct.
 * - Sujet simple ("guitare") -> elle explore Wikipédia français.
 *   URL complète -> elle explore ce site-là (liens du même domaine).
 * - Limites : nombre de pages choisi, bouton Stop, pause polie de 400 ms
 *   entre les pages, délais réseau courts. Tout ce qu'elle apprend reste local.
 */
class Explorer(
    private val activity: Activity,
    private val web: WebLearner,
    private val image: ImageBrain,
    private val audio: AudioBrain,
    private val code: CodeBrain
) {
    @Volatile var cancel = false

    private val visited = HashSet<String>()
    private val frontier = ArrayList<String>()   // sujets wiki OU urls complètes

    fun explore(
        start: String,
        maxPages: Int,
        learnImages: Boolean,
        onLog: (String) -> Unit,
        onDone: (String) -> Unit
    ) {
        cancel = false
        visited.clear(); frontier.clear()
        Thread {
            var pages = 0; var chars = 0; var imgs = 0
            val log = ArrayDeque<String>()
            fun say(msg: String) {
                log.addLast(msg)
                while (log.size > 12) log.removeFirst()
                val t = log.joinToString("\n")
                activity.runOnUiThread { onLog(t) }
            }
            try {
                val s = start.trim()
                when {
                    s.startsWith("http") || s.contains("/") -> frontier.add(if (s.startsWith("http")) s else "https://$s")
                    s.isNotBlank() -> frontier.add(s)
                    else -> {
                        // Curiosité pure : elle pioche dans ce qu'elle connaît déjà
                        val known = (image.labels() + audio.labels()).shuffled()
                        if (known.isNotEmpty()) {
                            say("\uD83E\uDD14 Personne ne me guide... « ${known.first()} » m'intrigue, allons voir !")
                            frontier.addAll(known.take(3))
                        } else {
                            say("\uD83E\uDD14 Je ne connais encore rien... je pars au hasard de Wikipédia !")
                            frontier.add("!random")
                        }
                    }
                }

                while (pages < maxPages && frontier.isNotEmpty() && !cancel) {
                    val item = frontier.removeAt(0)
                    val url = resolve(item) ?: continue
                    if (url in visited) continue
                    visited.add(url)

                    val title = URLDecoder.decode(url.substringAfterLast("/"), "UTF-8").replace('_', ' ')
                    say("\uD83D\uDCD6 Je lis « ${title.take(40)} »...")
                    val res = web.httpGet(url) ?: run { say("   (page injoignable, tant pis)"); null } ?: continue
                    val (type, bytes) = res
                    if (!type.contains("html") && !type.contains("text")) continue
                    val html = String(bytes, Charsets.UTF_8)
                    val text = web.htmlToText(html).take(15_000)
                    if (text.length > 200) {
                        code.learn(text)
                        chars += text.length
                        pages++
                        say("   \u2714 ${text.length} caractères appris (page $pages/$maxPages)")
                    }

                    // Images de la page, étiquetées avec le titre
                    if (learnImages && !cancel) {
                        imgs += grabImages(html, url, title.take(24))
                    }

                    // Elle repère les liens qui l'attirent
                    val next = pickLinks(html, url)
                    if (next.isNotEmpty()) {
                        val fav = next.first()
                        say("\u2728 « ${URLDecoder.decode(fav.substringAfterLast("/"), "UTF-8").replace('_',' ').take(35)} » me fait de l'œil...")
                        frontier.addAll(0, next)
                        while (frontier.size > 40) frontier.removeAt(frontier.size - 1)
                    }
                    Thread.sleep(400)   // politesse envers les serveurs
                }
            } catch (e: Exception) {
                say("\u26A0 Oups : ${e.message}")
            }
            val end = "\uD83C\uDFC1 Exploration terminée : $pages pages lues, $chars caractères" +
                    (if (imgs > 0) ", $imgs images" else "") + " appris." +
                    (if (cancel) " (interrompue)" else "")
            activity.runOnUiThread { onDone(end) }
        }.start()
    }

    /** Sujet -> URL Wikipédia (via l'API de recherche) ; URL -> telle quelle. */
    private fun resolve(item: String): String? {
        if (item.startsWith("http")) return item
        if (item == "!random") return "https://fr.wikipedia.org/wiki/Sp%C3%A9cial:Page_au_hasard"
        return try {
            val enc = URLEncoder.encode(item, "UTF-8")
            val api = "https://fr.wikipedia.org/w/api.php?action=opensearch&search=$enc&limit=1&format=json"
            val res = web.httpGet(api) ?: return direct(item)
            val arr = JSONArray(String(res.second, Charsets.UTF_8))
            val urls = arr.getJSONArray(3)
            if (urls.length() > 0) urls.getString(0) else direct(item)
        } catch (e: Exception) { direct(item) }
    }

    private fun direct(topic: String): String =
        "https://fr.wikipedia.org/wiki/" + URLEncoder.encode(topic.replace(' ', '_'), "UTF-8")

    /** Choisit les liens qui l'intriguent sur la page. */
    private fun pickLinks(html: String, baseUrl: String): List<String> {
        val base = try { URL(baseUrl) } catch (e: Exception) { return emptyList() }
        val isWiki = base.host.contains("wikipedia.org")
        val out = ArrayList<String>()
        if (isWiki) {
            // Liens internes d'articles (pas les pages techniques avec ':')
            val links = Regex("href=\"/wiki/([^\"#:]+)\"").findAll(html)
                .map { it.groupValues[1] }.distinct()
                .filter { it.length in 3..60 }.toMutableList()
            links.shuffle()
            val known = (image.labels() + audio.labels()).map { it.lowercase() }
            // 1 lien "familier" (contient un mot connu) + 2 découvertes pures
            val familiar = links.firstOrNull { l -> known.any { k -> k.length > 2 && l.lowercase().contains(k) } }
            familiar?.let { out.add("https://fr.wikipedia.org/wiki/$it"); links.remove(it) }
            links.take(2).forEach { out.add("https://fr.wikipedia.org/wiki/$it") }
        } else {
            // Site quelconque : liens du même domaine
            val links = Regex("href=\"([^\"#]+)\"").findAll(html)
                .map { it.groupValues[1] }.distinct().toMutableList()
            links.shuffle()
            var n = 0
            for (l in links) {
                val abs = try { URL(base, l).toString() } catch (e: Exception) { continue }
                if (URL(abs).host == base.host && abs !in visited &&
                    !abs.endsWith(".png") && !abs.endsWith(".jpg") && !abs.endsWith(".pdf")) {
                    out.add(abs); n++
                    if (n >= 3) break
                }
            }
        }
        return out.filter { it !in visited }
    }

    /** Apprend jusqu'à 3 images de la page, étiquetées avec le titre. */
    private fun grabImages(html: String, baseUrl: String, label: String): Int {
        var learned = 0
        try {
            val base = URL(baseUrl)
            val srcs = Regex("(?i)<img[^>]+src=\"([^\"]+)\"").findAll(html)
                .map { it.groupValues[1] }
                .filter { !it.startsWith("data:") && !it.contains("Wikipedia-logo") && !it.contains(".svg") }
                .distinct().take(3)
            for (s in srcs) {
                if (cancel) break
                val abs = try { URL(base, s).toString() } catch (e: Exception) { continue }
                val r = web.httpGet(abs) ?: continue
                if (r.first.startsWith("image/") && web.learnImageBytes(r.second, label)) learned++
            }
        } catch (e: Exception) { }
        return learned
    }
}
