package com.moi.iatrio

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {

    private lateinit var imageBrain: ImageBrain
    private lateinit var audioBrain: AudioBrain
    private lateinit var codeBrain: CodeBrain
    private lateinit var orchestrator: Orchestrator

    private var currentBitmap: Bitmap? = null
    private var currentAudio: ShortArray? = null

    private lateinit var imgStatus: TextView
    private lateinit var audStatus: TextView
    private lateinit var codeStatus: TextView
    private lateinit var fusionOut: TextView
    private lateinit var imgLabel: EditText
    private lateinit var audLabel: EditText
    private lateinit var codeInput: EditText
    private lateinit var codePrompt: EditText

    // Palette (accordée au logo)
    private val cBlue = Color.parseColor("#22B8F0")
    private val cBlueDark = Color.parseColor("#1E88E5")
    private val cPurple = Color.parseColor("#7C3AED")
    private val cPurpleDark = Color.parseColor("#6D28D9")
    private val cOrange = Color.parseColor("#FB8C00")
    private val cOrangeDark = Color.parseColor("#F4511E")
    private val cInk = Color.parseColor("#1E293B")
    private val cMuted = Color.parseColor("#64748B")
    private val cField = Color.parseColor("#F1F5F9")
    private val cCard = Color.parseColor("#FFFFFF")

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    // Bouton pilule coloré (dégradé)
    private fun pill(text: String, c1: Int, c2: Int, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        isAllCaps = false
        typeface = Typeface.DEFAULT_BOLD
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        stateListAnimator = null
        background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(c1, c2)).apply {
            cornerRadius = dp(26).toFloat()
        }
        setPadding(dp(8), dp(14), dp(8), dp(14))
        setOnClickListener { onClick() }
    }

    // Petit bouton discret (oublier)
    private fun ghost(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        setTextColor(Color.parseColor("#EF4444"))
        isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        stateListAnimator = null
        background = GradientDrawable().apply {
            cornerRadius = dp(22).toFloat()
            setColor(Color.parseColor("#FEE2E2"))
        }
        setPadding(dp(8), dp(11), dp(8), dp(11))
        setOnClickListener { onClick() }
    }

    private fun field(hintText: String, lines: Int = 1) = EditText(this).apply {
        hint = hintText
        setHintTextColor(cMuted)
        setTextColor(cInk)
        minLines = lines
        setPadding(dp(16), dp(12), dp(16), dp(12))
        background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(cField)
            setStroke(dp(1), Color.parseColor("#E2E8F0"))
        }
    }

    private fun lp(topMargin: Int = 0) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { setMargins(0, dp(topMargin), 0, 0) }

    // Carte blanche arrondie avec liseré coloré à gauche
    private fun card(accent: Int): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(18), dp(18), dp(18), dp(18))
        c.background = GradientDrawable().apply {
            cornerRadius = dp(22).toFloat()
            setColor(cCard)
            setStroke(dp(2), accent)
        }
        c.elevation = dp(6).toFloat()
        return c
    }

    private fun sectionTitle(t: String, accent: Int) = TextView(this).apply {
        text = t
        setTextColor(accent)
        typeface = Typeface.DEFAULT_BOLD
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
        setPadding(0, 0, 0, dp(6))
    }

    private fun status() = TextView(this).apply {
        setTextColor(cMuted)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        setPadding(0, dp(10), 0, 0)
    }

    private fun rowEqual(vararg views: View) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        views.forEachIndexed { i, v ->
            val p = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            if (i > 0) p.setMargins(dp(8), 0, 0, 0)
            addView(v, p)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageBrain = ImageBrain(filesDir)
        audioBrain = AudioBrain(filesDir)
        codeBrain = CodeBrain(filesDir)
        orchestrator = Orchestrator(imageBrain, audioBrain, codeBrain)

        // Fond dégradé
        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#EEF2FF"), Color.parseColor("#E0F2FE"))
            )
            setPadding(dp(18), dp(24), dp(18), dp(24))
        }

        // En-tête : logo
        screen.addView(ImageView(this).apply {
            setImageResource(R.drawable.logo)
            adjustViewBounds = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(70)
            ).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(4) }
        })
        screen.addView(TextView(this).apply {
            text = "3 IA locales que tu entraînes toi-même"
            setTextColor(cMuted)
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, 0, 0, dp(18))
        })

        // ---------- IMAGES ----------
        val ci = card(cBlue)
        ci.addView(sectionTitle("\uD83D\uDDBC  Images (couleur)", cBlue))
        imgLabel = field("Nom de l'image (ex: chat)")
        imgStatus = status().also { it.text = "Appris : ${imageBrain.summary()}" }
        ci.addView(pill("Choisir une image", cBlue, cBlueDark) {
            startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }, 1)
        })
        ci.addView(imgLabel, lp(10))
        ci.addView(rowEqual(
            pill("Apprendre", cBlue, cBlueDark) {
                val b = currentBitmap; val l = imgLabel.text.toString().trim()
                if (b == null || l.isEmpty()) { toast("Choisis une image ET un nom"); return@pill }
                imageBrain.learn(b, l); imgStatus.text = "Appris : ${imageBrain.summary()}"
            },
            pill("Deviner", cBlue, cBlueDark) {
                val b = currentBitmap ?: return@pill toast("Choisis une image")
                val r = imageBrain.guess(b); orchestrator.lastImage = r
                imgStatus.text = "Je pense : « ${r.first} » (${(r.second * 100).toInt()}%)"
            }
        ), lp(10))
        ci.addView(ghost("\uD83D\uDDD1 Oublier les images") { imageBrain.forget(); imgStatus.text = "Mémoire images effacée." }, lp(10))
        ci.addView(imgStatus)
        screen.addView(ci, lp(0))

        // ---------- SONS ----------
        val cs = card(cPurple)
        cs.addView(sectionTitle("\uD83C\uDFB5  Musique & Sons (FFT)", cPurple))
        audLabel = field("Nom du son (ex: guitare)")
        audStatus = status().also { it.text = "Appris : ${audioBrain.summary()}" }
        cs.addView(pill("Enregistrer 2 secondes", cPurple, cPurpleDark) { record() })
        cs.addView(audLabel, lp(10))
        cs.addView(rowEqual(
            pill("Apprendre", cPurple, cPurpleDark) {
                val a = currentAudio; val l = audLabel.text.toString().trim()
                if (a == null || l.isEmpty()) { toast("Enregistre un son ET un nom"); return@pill }
                audioBrain.learn(a, l); audStatus.text = "Appris : ${audioBrain.summary()}"
            },
            pill("Deviner", cPurple, cPurpleDark) {
                val a = currentAudio ?: return@pill toast("Enregistre un son")
                val r = audioBrain.guess(a); orchestrator.lastAudio = r
                audStatus.text = "J'entends : « ${r.first} » (${(r.second * 100).toInt()}%)"
            }
        ), lp(10))
        cs.addView(ghost("\uD83D\uDDD1 Oublier les sons") { audioBrain.forget(); audStatus.text = "Mémoire sons effacée." }, lp(10))
        cs.addView(audStatus)
        screen.addView(cs, lp(16))

        // ---------- CODE ----------
        val cc = card(cOrange)
        cc.addView(sectionTitle("\uD83D\uDCBB  Code & Texte", cOrange))
        codeInput = field("Colle du code à apprendre", 3)
        codePrompt = field("Début de code à compléter")
        codeStatus = status().also { it.text = "Mémoire : ${codeBrain.size()} motifs" }
        cc.addView(codeInput)
        cc.addView(pill("Apprendre ce code", cOrange, cOrangeDark) {
            val t = codeInput.text.toString()
            if (t.length < 10) return@pill toast("Colle au moins quelques lignes")
            codeBrain.learn(t); codeStatus.text = "Appris ! Mémoire : ${codeBrain.size()} motifs"; codeInput.setText("")
        }, lp(10))
        cc.addView(codePrompt, lp(10))
        cc.addView(rowEqual(
            pill("Compléter", cOrange, cOrangeDark) { codeStatus.text = codeBrain.complete(codePrompt.text.toString()) },
            ghost("\uD83D\uDDD1 Oublier") { codeBrain.forget(); codeStatus.text = "Mémoire code effacée." }
        ), lp(10))
        cc.addView(codeStatus)
        screen.addView(cc, lp(16))

        // ---------- ORCHESTRATEUR ----------
        val cf = card(cInk)
        cf.addView(sectionTitle("\uD83E\uDDE0  Réflexion commune", cInk))
        fusionOut = TextView(this).apply {
            setTextColor(cInk); setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); setPadding(0, dp(8), 0, 0)
        }
        cf.addView(pill("Penser ensemble", Color.parseColor("#334155"), cInk) { fusionOut.text = orchestrator.thinkTogether() })
        cf.addView(fusionOut)
        screen.addView(cf, lp(16))

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(screen)
        })
    }

    private fun record() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 42); return
        }
        audStatus.text = "Enregistrement..."
        Thread {
            try {
                val rate = 16000
                val buf = ShortArray(rate * 2)
                val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                val rec = AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(min, buf.size * 2))
                rec.startRecording()
                var read = 0
                while (read < buf.size) { val n = rec.read(buf, read, buf.size - read); if (n <= 0) break; read += n }
                rec.stop(); rec.release()
                currentAudio = buf
                runOnUiThread { audStatus.text = "Son capturé \u2714 ($read échantillons)" }
            } catch (e: Exception) {
                runOnUiThread { audStatus.text = "Erreur micro : ${e.message}" }
            }
        }.start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK && data?.data != null) {
            try {
                contentResolver.openInputStream(data.data!!).use { currentBitmap = BitmapFactory.decodeStream(it) }
                imgStatus.text = "Image chargée \u2714"
            } catch (e: Exception) { toast("Impossible de lire l'image") }
        }
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
