package com.moi.iatrio

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.view.Gravity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageBrain = ImageBrain(filesDir)
        audioBrain = AudioBrain(filesDir)
        codeBrain = CodeBrain(filesDir)
        orchestrator = Orchestrator(imageBrain, audioBrain, codeBrain)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        fun title(t: String) = TextView(this).apply {
            text = t; textSize = 20f; setPadding(0, 40, 0, 8); gravity = Gravity.START
        }
        fun button(t: String, onClick: () -> Unit) = Button(this).apply {
            text = t; setOnClickListener { onClick() }
        }

        // ---------- IA IMAGES ----------
        root.addView(title("🖼 IA Images"))
        imgLabel = EditText(this).apply { hint = "Nom de ce que montre l'image (ex: chat)" }
        imgStatus = TextView(this).apply { text = "Connaît : ${imageBrain.knowledge()}" }
        root.addView(button("Choisir une image") {
            val i = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            startActivityForResult(i, 1)
        })
        root.addView(imgLabel)
        root.addView(button("Apprendre cette image") {
            val b = currentBitmap; val l = imgLabel.text.toString().trim()
            if (b == null || l.isEmpty()) { toast("Choisis une image ET donne un nom") ; return@button }
            imageBrain.learn(b, l)
            imgStatus.text = "Appris ! Connaît : ${imageBrain.knowledge()}"
        })
        root.addView(button("Deviner cette image") {
            val b = currentBitmap ?: return@button toast("Choisis une image d'abord")
            val r = imageBrain.guess(b)
            orchestrator.lastImage = r
            imgStatus.text = "Je pense : « ${r.first} » (${(r.second * 100).toInt()}%)"
        })
        root.addView(imgStatus)

        // ---------- IA MUSIQUE / SONS ----------
        root.addView(title("🎵 IA Musique & Sons"))
        audLabel = EditText(this).apply { hint = "Nom de ce son (ex: sifflement, guitare)" }
        audStatus = TextView(this).apply { text = "Connaît : ${audioBrain.knowledge()}" }
        root.addView(button("Enregistrer 2 secondes") { record() })
        root.addView(audLabel)
        root.addView(button("Apprendre ce son") {
            val a = currentAudio; val l = audLabel.text.toString().trim()
            if (a == null || l.isEmpty()) { toast("Enregistre un son ET donne un nom"); return@button }
            audioBrain.learn(a, l)
            audStatus.text = "Appris ! Connaît : ${audioBrain.knowledge()}"
        })
        root.addView(button("Deviner ce son") {
            val a = currentAudio ?: return@button toast("Enregistre un son d'abord")
            val r = audioBrain.guess(a)
            orchestrator.lastAudio = r
            audStatus.text = "J'entends : « ${r.first} » (${(r.second * 100).toInt()}%)"
        })
        root.addView(audStatus)

        // ---------- IA CODE ----------
        root.addView(title("💻 IA Code & Texte"))
        codeInput = EditText(this).apply { hint = "Colle ici du code ou du texte à apprendre"; minLines = 3 }
        codePrompt = EditText(this).apply { hint = "Début de code à compléter" }
        codeStatus = TextView(this).apply { text = "Mémoire : ${codeBrain.size()} motifs" }
        root.addView(codeInput)
        root.addView(button("Apprendre ce code") {
            val t = codeInput.text.toString()
            if (t.length < 10) return@button toast("Colle au moins quelques lignes")
            codeBrain.learn(t)
            codeStatus.text = "Appris ! Mémoire : ${codeBrain.size()} motifs"
            codeInput.setText("")
        })
        root.addView(codePrompt)
        root.addView(button("Compléter") {
            codeStatus.text = codeBrain.complete(codePrompt.text.toString())
        })
        root.addView(codeStatus)

        // ---------- ORCHESTRATEUR ----------
        root.addView(title("🧠 Les 3 IA réfléchissent ensemble"))
        fusionOut = TextView(this)
        root.addView(button("Penser ensemble") { fusionOut.text = orchestrator.thinkTogether() })
        root.addView(fusionOut)

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun record() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 42)
            return
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
                while (read < buf.size) {
                    val n = rec.read(buf, read, buf.size - read)
                    if (n <= 0) break
                    read += n
                }
                rec.stop(); rec.release()
                currentAudio = buf
                runOnUiThread { audStatus.text = "Son capturé ✔ ($read échantillons)" }
            } catch (e: Exception) {
                runOnUiThread { audStatus.text = "Erreur micro : ${e.message}" }
            }
        }.start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK && data?.data != null) {
            try {
                contentResolver.openInputStream(data.data!!).use {
                    currentBitmap = BitmapFactory.decodeStream(it)
                }
                imgStatus.text = "Image chargée ✔"
            } catch (e: Exception) { toast("Impossible de lire l'image") }
        }
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
