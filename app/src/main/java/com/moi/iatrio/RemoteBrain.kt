package com.moi.iatrio

import android.app.Activity
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * CERVEAU DISTANT multi-fournisseurs (optionnel, désactivable) :
 * Gemini (Google), Claude (Anthropic), OpenAI, Groq (gratuit), Mistral.
 * Chaque fournisseur garde SA propre clé API.
 *
 * Vie privée : ACTIVÉ, tes questions partent chez le fournisseur choisi.
 * DÉSACTIVÉ, rien ne quitte le téléphone. Les mémoires locales ne sont
 * jamais envoyées.
 */
class RemoteBrain(private val activity: Activity) {

    data class Provider(
        val name: String,
        val kind: String,           // gemini | openai | anthropic
        val url: String,
        val models: List<String>,
        val keyPref: String
    )

    val providers = listOf(
        Provider("Gemini (Google, gratuit)", "gemini",
            "https://generativelanguage.googleapis.com/v1beta/models",
            listOf("gemini-2.0-flash", "gemini-1.5-flash"), "key_gemini"),
        Provider("Groq (gratuit)", "openai",
            "https://api.groq.com/openai/v1/chat/completions",
            listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant"), "key_groq"),
        Provider("Claude (Anthropic)", "anthropic",
            "https://api.anthropic.com/v1/messages",
            listOf("claude-haiku-4-5", "claude-3-5-haiku-latest"), "key_claude"),
        Provider("OpenAI", "openai",
            "https://api.openai.com/v1/chat/completions",
            listOf("gpt-4o-mini"), "key_openai"),
        Provider("Mistral", "openai",
            "https://api.mistral.ai/v1/chat/completions",
            listOf("mistral-small-latest"), "key_mistral")
    )

    private val prefs = activity.getSharedPreferences("iatrio", 0)

    var enabled: Boolean
        get() = prefs.getBoolean("remote_enabled", false)
        set(v) { prefs.edit().putBoolean("remote_enabled", v).apply() }

    var providerIndex: Int
        get() = prefs.getInt("remote_provider", 0).coerceIn(0, providers.size - 1)
        set(v) { prefs.edit().putInt("remote_provider", v.coerceIn(0, providers.size - 1)).apply() }

    val provider: Provider get() = providers[providerIndex]

    var apiKey: String
        get() = prefs.getString("remote_" + provider.keyPref, "") ?: ""
        set(v) { prefs.edit().putString("remote_" + provider.keyPref, v.trim()).apply() }

    fun providerNames(): List<String> = providers.map { it.name }

    fun ready(): Boolean = enabled && apiKey.isNotBlank()

    fun ask(prompt: String, onDone: (String) -> Unit) {
        if (!enabled) { onDone("(Cerveau distant désactivé — active-le dans l'onglet Profils)"); return }
        if (apiKey.isBlank()) { onDone("(Aucune clé API pour ${provider.name} — colle ta clé dans l'onglet Profils)"); return }
        val prov = provider
        val key = apiKey
        Thread {
            var result: String? = null
            var lastError = "aucune réponse"
            for (m in prov.models) {
                val r = when (prov.kind) {
                    "gemini" -> callGemini(prov.url, m, key, prompt)
                    "anthropic" -> callAnthropic(prov.url, m, key, prompt)
                    else -> callOpenAI(prov.url, m, key, prompt)
                }
                if (r.first != null) { result = r.first; break } else lastError = r.second
            }
            val msg = result ?: "Erreur ${prov.name} : $lastError"
            activity.runOnUiThread { onDone(msg) }
        }.start()
    }

    // ---------- Gemini ----------
    private fun callGemini(base: String, model: String, key: String, prompt: String): Pair<String?, String> =
        post("$base/$model:generateContent?key=$key", emptyMap(),
            JSONObject().put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
        ) { resp ->
            JSONObject(resp).getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts")
                .getJSONObject(0).getString("text")
        }

    // ---------- OpenAI / Groq / Mistral (format commun) ----------
    private fun callOpenAI(url: String, model: String, key: String, prompt: String): Pair<String?, String> =
        post(url, mapOf("Authorization" to "Bearer $key"),
            JSONObject().put("model", model).put("messages", JSONArray().put(
                JSONObject().put("role", "user").put("content", prompt)))
        ) { resp ->
            JSONObject(resp).getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
        }

    // ---------- Claude (Anthropic) ----------
    private fun callAnthropic(url: String, model: String, key: String, prompt: String): Pair<String?, String> =
        post(url, mapOf("x-api-key" to key, "anthropic-version" to "2023-06-01"),
            JSONObject().put("model", model).put("max_tokens", 600)
                .put("messages", JSONArray().put(
                    JSONObject().put("role", "user").put("content", prompt)))
        ) { resp ->
            JSONObject(resp).getJSONArray("content").getJSONObject(0).getString("text")
        }

    // ---------- Requête HTTP commune ----------
    private fun post(
        urlStr: String,
        headers: Map<String, String>,
        body: JSONObject,
        parse: (String) -> String
    ): Pair<String?, String> {
        var conn: HttpURLConnection? = null
        return try {
            conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Content-Type", "application/json")
            for ((k, v) in headers) conn.setRequestProperty(k, v)
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val codeHttp = conn.responseCode
            if (codeHttp !in 200..299) {
                val err = try { conn.errorStream?.bufferedReader()?.readText()?.take(200) } catch (e: Exception) { null }
                return Pair(null, "HTTP $codeHttp ${err ?: ""}".trim())
            }
            Pair(parse(conn.inputStream.bufferedReader().readText()).trim(), "")
        } catch (e: Exception) {
            Pair(null, e.message ?: "exception")
        } finally {
            try { conn?.disconnect() } catch (e: Exception) { }
        }
    }
}
