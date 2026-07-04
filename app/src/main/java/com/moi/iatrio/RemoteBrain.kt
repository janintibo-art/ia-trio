package com.moi.iatrio

import android.app.Activity
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * CERVEAU DISTANT (optionnel, désactivable) :
 * utilise l'API Gemini de Google (niveau gratuit disponible) pour épauler
 * les 3 IA locales avec un gros modèle en ligne.
 *
 * IMPORTANT vie privée : quand il est ACTIVÉ, les questions que tu poses
 * (et le résumé vision/ouïe de l'orchestrateur) sont envoyées aux serveurs
 * de Google. Quand il est DÉSACTIVÉ, rien ne quitte le téléphone.
 * Les mémoires de tes 3 IA locales ne sont jamais envoyées.
 */
class RemoteBrain(private val activity: Activity) {

    private val prefs = activity.getSharedPreferences("iatrio", 0)

    var enabled: Boolean
        get() = prefs.getBoolean("remote_enabled", false)
        set(v) { prefs.edit().putBoolean("remote_enabled", v).apply() }

    var apiKey: String
        get() = prefs.getString("remote_key", "") ?: ""
        set(v) { prefs.edit().putString("remote_key", v.trim()).apply() }

    fun ready(): Boolean = enabled && apiKey.isNotBlank()

    // On essaie plusieurs modèles : si le premier n'existe plus, on bascule.
    private val models = listOf("gemini-2.0-flash", "gemini-1.5-flash")

    fun ask(prompt: String, onDone: (String) -> Unit) {
        if (!enabled) { onDone("(Cerveau distant désactivé — active-le dans l'onglet Profils)"); return }
        if (apiKey.isBlank()) { onDone("(Aucune clé API — colle ta clé dans l'onglet Profils)"); return }
        Thread {
            var result: String? = null
            var lastError = "aucune réponse"
            for (m in models) {
                val r = call(m, prompt)
                if (r.first != null) { result = r.first; break } else lastError = r.second
            }
            val msg = result ?: "Erreur cerveau distant : $lastError"
            activity.runOnUiThread { onDone(msg) }
        }.start()
    }

    /** @return Pair(réponse ou null, message d'erreur) */
    private fun call(model: String, prompt: String): Pair<String?, String> {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 25_000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            val body = JSONObject().put(
                "contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                )
            )
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val codeHttp = conn.responseCode
            if (codeHttp != 200) {
                val err = try { conn.errorStream?.bufferedReader()?.readText()?.take(200) } catch (e: Exception) { null }
                return Pair(null, "HTTP $codeHttp ${err ?: ""}".trim())
            }
            val resp = conn.inputStream.bufferedReader().readText()
            val text = JSONObject(resp)
                .getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts")
                .getJSONObject(0).getString("text")
            Pair(text.trim(), "")
        } catch (e: Exception) {
            Pair(null, e.message ?: "exception")
        } finally {
            try { conn?.disconnect() } catch (e: Exception) { }
        }
    }
}
