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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import java.util.Locale
import android.util.TypedValue
import android.view.Gravity
import android.view.TextureView
import android.view.View
import android.widget.*
import java.io.File

class MainActivity : Activity() {

    private lateinit var profiles: ProfileManager
    private lateinit var profile: Profile
    private lateinit var imageBrain: ImageBrain
    private lateinit var audioBrain: AudioBrain
    private lateinit var codeBrain: CodeBrain
    private lateinit var orchestrator: Orchestrator
    private lateinit var scanner: ScanTrainer
    private lateinit var web: WebLearner
    private lateinit var explorer: Explorer
    private lateinit var expTopic: EditText
    private lateinit var expLog: TextView
    private lateinit var expPages: Spinner
    private lateinit var expImages: Switch
    private lateinit var remote: RemoteBrain
    private lateinit var childManager: ChildManager
    private var child: ChildBrain? = null
    private lateinit var childStatus: TextView
    private lateinit var childTalkOut: TextView
    private lateinit var childInput: EditText
    private lateinit var childNameField: EditText
    private lateinit var childListBox: LinearLayout
    private lateinit var crossA: Spinner
    private lateinit var crossB: Spinner
    private lateinit var babyNameField: EditText
    private lateinit var liveTexture: TextureView
    private lateinit var liveOut: TextView
    private lateinit var liveLabel: EditText
    private var liveCamera: android.hardware.Camera? = null
    private var liveRunning = false
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private lateinit var examOut: TextView
    private lateinit var remoteStatus: TextView
    private lateinit var remoteKeyField: EditText
    private lateinit var remoteAsk: EditText
    private lateinit var remoteSwitch: Switch
    private lateinit var providerSpinner: Spinner
    private var tfVision: TFVision? = null
    private var tfAudio: TFAudio? = null
    private lateinit var imgConf: ProgressBar
    private lateinit var audConf: ProgressBar
    private lateinit var liveConf: ProgressBar
    private lateinit var tabPunk: LinearLayout
    private lateinit var tabChat: LinearLayout
    private lateinit var tabCreate: LinearLayout
    private val creator = Creator()
    private lateinit var createPrompt: EditText
    private lateinit var createImg: ImageView
    private lateinit var createOut: TextView
    private var lastMusic: ShortArray? = null
    private lateinit var chatBox: LinearLayout
    private lateinit var chatInput: EditText
    private lateinit var chatSource: Spinner
    private lateinit var punkArena: FrameLayout
    private lateinit var punkBox: LinearLayout
    private lateinit var punkImg: ImageView
    private lateinit var punkBubble: TextView
    private lateinit var punkSource: Spinner
    private var punkRunning = false
    private var punkX = 0f
    private var punkDir = 1
    private var punkTicks = 0
    private var punkPause = 0
    private var lastRemoteThought = 0L
    private var punkShowingFace = true
    private var punkFrame = -1
    private val punkWalk = listOf(R.drawable.punk_walk_0, R.drawable.punk_walk_1, R.drawable.punk_walk_2,
        R.drawable.punk_walk_3, R.drawable.punk_walk_4, R.drawable.punk_walk_5)

    private var currentBitmap: Bitmap? = null
    private var currentAudio: ShortArray? = null

    private lateinit var imgStatus: TextView
    private lateinit var audStatus: TextView
    private lateinit var codeStatus: TextView
    private lateinit var scanStatus: TextView
    private lateinit var fusionOut: TextView
    private lateinit var webStatus: TextView
    private lateinit var webUrl: EditText
    private lateinit var webLabel: EditText
    private lateinit var imgLabel: EditText
    private lateinit var audLabel: EditText
    private lateinit var codeInput: EditText
    private lateinit var codePrompt: EditText

    // Onglets
    private lateinit var tabTrain: LinearLayout
    private lateinit var tabProfiles: LinearLayout
    private lateinit var tabChild: LinearLayout
    private lateinit var tabTuto: LinearLayout
    private lateinit var tabButtons: List<Button>

    // Vues de l'onglet Profils
    private lateinit var profileTitle: TextView
    private lateinit var behaviorLabel: TextView
    private lateinit var creaBar: SeekBar
    private lateinit var cautBar: SeekBar
    private lateinit var profileListBox: LinearLayout
    private lateinit var newProfileName: EditText

    // Palette
    private val cBlue = Color.parseColor("#22B8F0")
    private val cBlueDark = Color.parseColor("#1E88E5")
    private val cPurple = Color.parseColor("#7C3AED")
    private val cPurpleDark = Color.parseColor("#6D28D9")
    private val cOrange = Color.parseColor("#FB8C00")
    private val cOrangeDark = Color.parseColor("#F4511E")
    private val cGreen = Color.parseColor("#10B981")
    private val cGreenDark = Color.parseColor("#059669")
    private val cInk = Color.parseColor("#1E293B")
    private val cMuted = Color.parseColor("#64748B")
    private val cField = Color.parseColor("#F1F5F9")
    private val cCard = Color.parseColor("#FFFFFF")

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun pill(text: String, c1: Int, c2: Int, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        setTextColor(Color.WHITE); isAllCaps = false; typeface = Typeface.DEFAULT_BOLD
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f); stateListAnimator = null
        background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(c1, c2)).apply {
            cornerRadius = dp(26).toFloat()
        }
        setPadding(dp(8), dp(14), dp(8), dp(14))
        setOnClickListener { onClick() }
    }

    private fun ghost(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        setTextColor(Color.parseColor("#EF4444")); isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f); stateListAnimator = null
        background = GradientDrawable().apply { cornerRadius = dp(22).toFloat(); setColor(Color.parseColor("#FEE2E2")) }
        setPadding(dp(8), dp(11), dp(8), dp(11))
        setOnClickListener { onClick() }
    }

    private fun field(hintText: String, lines: Int = 1) = EditText(this).apply {
        hint = hintText; setHintTextColor(cMuted); setTextColor(cInk); minLines = lines
        setPadding(dp(16), dp(12), dp(16), dp(12))
        background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat(); setColor(cField); setStroke(dp(1), Color.parseColor("#E2E8F0"))
        }
    }

    private fun lp(topMargin: Int = 0) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { setMargins(0, dp(topMargin), 0, 0) }

    private fun card(accent: Int): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = GradientDrawable().apply {
            cornerRadius = dp(22).toFloat(); setColor(cCard); setStroke(dp(2), accent)
        }
        elevation = dp(6).toFloat()
    }

    private fun sectionTitle(t: String, accent: Int) = TextView(this).apply {
        text = t; setTextColor(accent); typeface = Typeface.DEFAULT_BOLD
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f); setPadding(0, 0, 0, dp(6))
    }

    private fun body(t: String) = TextView(this).apply {
        text = t; setTextColor(cInk); setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); setPadding(0, dp(4), 0, dp(4))
    }

    private fun status() = TextView(this).apply {
        setTextColor(cMuted); setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f); setPadding(0, dp(10), 0, 0)
    }

    private fun rowEqual(vararg views: View) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        views.forEachIndexed { i, v ->
            val p = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            if (i > 0) p.setMargins(dp(8), 0, 0, 0)
            addView(v, p)
        }
    }

    // ============================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tfVision = TFVision.tryLoad(this)
        tfAudio = TFAudio.tryLoad(this)
        remote = RemoteBrain(this)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.FRENCH
                ttsReady = true
            }
        }
        childManager = ChildManager(filesDir)
        childManager.currentName()?.let { child = childManager.get(it) }
        profiles = ProfileManager(filesDir)
        profiles.migrateLegacy()
        loadProfile(profiles.currentName(), refreshUi = false)

        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#EEF2FF"), Color.parseColor("#E0F2FE"))
            )
            setPadding(dp(18), dp(24), dp(18), dp(24))
        }

        screen.addView(ImageView(this).apply {
            setImageResource(R.drawable.logo)
            adjustViewBounds = true
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(64))
                .apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(10) }
        })

        // Barre d'onglets
        val tabNames = listOf("IA", "Profils", "\uD83D\uDC76", "\uD83E\uDD18", "\uD83D\uDCAC", "\u2728", "Tuto")
        tabButtons = tabNames.mapIndexed { i, n ->
            Button(this).apply {
                text = n; isAllCaps = false; stateListAnimator = null
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setOnClickListener { showTab(i) }
            }
        }
        screen.addView(rowEqual(*tabButtons.toTypedArray()), lp(0))

        // Conteneurs des onglets
        tabTrain = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        tabProfiles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        tabChild = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        tabPunk = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        tabChat = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        tabCreate = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        tabTuto = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        buildTrainTab(tabTrain)
        buildProfilesTab(tabProfiles)
        buildChildTab(tabChild)
        buildPunkTab(tabPunk)
        buildChatTab(tabChat)
        buildCreateTab(tabCreate)
        buildTutoTab(tabTuto)
        screen.addView(tabTrain, lp(14))
        screen.addView(tabProfiles, lp(14))
        screen.addView(tabChild, lp(14))
        screen.addView(tabPunk, lp(14))
        screen.addView(tabChat, lp(14))
        screen.addView(tabCreate, lp(14))
        screen.addView(tabTuto, lp(14))

        setContentView(ScrollView(this).apply { isFillViewport = true; addView(screen) })
        showTab(0)
        refreshAllStatuses()
    }

    private fun showTab(i: Int) {
        tabTrain.visibility = if (i == 0) View.VISIBLE else View.GONE
        tabProfiles.visibility = if (i == 1) View.VISIBLE else View.GONE
        tabChild.visibility = if (i == 2) View.VISIBLE else View.GONE
        tabPunk.visibility = if (i == 3) View.VISIBLE else View.GONE
        tabChat.visibility = if (i == 4) View.VISIBLE else View.GONE
        tabCreate.visibility = if (i == 5) View.VISIBLE else View.GONE
        tabTuto.visibility = if (i == 6) View.VISIBLE else View.GONE
        tabButtons.forEachIndexed { j, b ->
            b.setTextColor(if (i == j) Color.WHITE else cInk)
            b.typeface = if (i == j) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            b.background = GradientDrawable().apply {
                cornerRadius = dp(22).toFloat()
                setColor(if (i == j) cInk else Color.parseColor("#E2E8F0"))
            }
        }
        if (i == 1) refreshProfilesTab()
        if (i == 2) refreshChildTab()
        if (i == 3) refreshPunkTab()
        if (i != 3) punkRunning = false
        if (i == 4) refreshChatSources()
    }

    // ============================================================
    // PROFILS : charger / basculer
    private fun loadProfile(name: String, refreshUi: Boolean = true) {
        profiles.setCurrent(name)
        profile = profiles.loadBehavior(name)
        profiles.saveBehavior(profile) // crée behavior.txt si absent
        val d = profiles.dir(name)
        imageBrain = ImageBrain(d, tfVision)
        audioBrain = AudioBrain(d, tfAudio)
        codeBrain = CodeBrain(d)
        orchestrator = Orchestrator(imageBrain, audioBrain, codeBrain)
        scanner = ScanTrainer(this, imageBrain, audioBrain, codeBrain)
        web = WebLearner(this, imageBrain, codeBrain)
        explorer = Explorer(this, web, imageBrain, audioBrain, codeBrain)
        if (refreshUi) { refreshAllStatuses(); refreshProfilesTab(); loadChatHistory(); toast("Profil « $name » activé") }
    }

    private fun refreshAllStatuses() {
        if (!::imgStatus.isInitialized) return
        imgStatus.text = "Appris : ${imageBrain.summary()}"
        audStatus.text = "Appris : ${audioBrain.summary()}"
        codeStatus.text = "Mémoire : ${codeBrain.size()} motifs"
        scanStatus.text = "Prêt."
        fusionOut.text = ""
    }

    private fun verdict(r: Pair<String, Double>, verb: String): String {
        val pct = (r.second * 100).toInt()
        return if (pct < profile.caution) "\uD83E\uDD14 Pas sûr... peut-être « ${r.first} » ($pct%)"
        else "$verb « ${r.first} » ($pct%)"
    }

    /** Barre de confiance : rouge si doute, orange si moyen, verte si sûre. */
    private fun confBar(): ProgressBar =
        ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; progress = 0 }

    private fun setConf(bar: ProgressBar, pct: Int) {
        bar.progress = pct
        val color = when {
            pct < profile.caution -> Color.parseColor("#EF4444")
            pct < 70 -> Color.parseColor("#F59E0B")
            else -> Color.parseColor("#10B981")
        }
        bar.progressTintList = android.content.res.ColorStateList.valueOf(color)
    }

    /** Mini-graphique en caractères : ▁▂▃▄▅▆▇█ */
    private fun sparkline(values: List<Int>): String {
        val blocks = "\u2581\u2582\u2583\u2584\u2585\u2586\u2587\u2588"
        return values.joinToString("") { v ->
            blocks[(v.coerceIn(0, 100) * (blocks.length - 1) / 100)].toString()
        }
    }

    // ============================================================
    // ONGLET 1 : ENTRAÎNER
    private fun buildTrainTab(box: LinearLayout) {
        // Images
        val ci = card(cBlue)
        ci.addView(sectionTitle(if (tfVision != null) "\uD83D\uDDBC  Images (TensorFlow \uD83E\uDDE0)" else "\uD83D\uDDBC  Images (couleur)", cBlue))
        imgLabel = field("Nom de l'image (ex: chat)")
        imgStatus = status()
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
                val pre = imageBrain.preKnowledge(b)?.let { "\nBase MobileNet : $it" } ?: ""
                imgStatus.text = verdict(r, "Je pense :") + pre
                setConf(imgConf, (r.second * 100).toInt())
            }
        ), lp(10))
        ci.addView(ghost("\uD83D\uDDD1 Oublier les images") { imageBrain.forget(); imgStatus.text = "Mémoire images effacée." }, lp(10))
        ci.addView(imgStatus)
        imgConf = confBar()
        ci.addView(imgConf, lp(6))
        box.addView(ci, lp(0))

        // Vision en direct (caméra)
        val cv = card(Color.parseColor("#14B8A6"))
        cv.addView(sectionTitle("\uD83D\uDC41  Vision en direct", Color.parseColor("#14B8A6")))
        cv.addView(TextView(this).apply {
            text = "L'IA images regarde par la caméra et devine en temps réel. Pointe un objet, et si elle se trompe, apprends-lui sur le champ !"
            setTextColor(cMuted); setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); setPadding(0, 0, 0, dp(8))
        })
        liveTexture = TextureView(this)
        cv.addView(liveTexture, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(240)))
        liveOut = status()
        cv.addView(rowEqual(
            pill("\u25B6 Démarrer", Color.parseColor("#14B8A6"), Color.parseColor("#0D9488")) { startLive() },
            ghost("\u23F9 Stop") { stopLive() }
        ), lp(10))
        liveLabel = field("Nom de ce que voit la caméra")
        cv.addView(liveLabel, lp(10))
        cv.addView(pill("Apprendre cette vue", Color.parseColor("#14B8A6"), Color.parseColor("#0D9488")) {
            if (!liveRunning) return@pill toast("Démarre d'abord la caméra")
            val l = liveLabel.text.toString().trim()
            if (l.isEmpty()) return@pill toast("Donne un nom à ce que voit la caméra")
            val bmp = liveTexture.getBitmap(160, 160) ?: return@pill toast("Image indisponible")
            imageBrain.learn(bmp, l)
            imgStatus.text = "Appris : ${imageBrain.summary()}"
            liveOut.text = "\u2714 Vue apprise comme « $l » !"
        }, lp(10))
        liveConf = confBar()
        cv.addView(liveConf, lp(6))
        cv.addView(liveOut)
        box.addView(cv, lp(16))

        // Sons
        val cs = card(cPurple)
        cs.addView(sectionTitle(if (tfAudio != null) "\uD83C\uDFB5  Musique & Sons (YAMNet \uD83E\uDDE0)" else "\uD83C\uDFB5  Musique & Sons (FFT)", cPurple))
        audLabel = field("Nom du son (ex: guitare)")
        audStatus = status()
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
                val preA = audioBrain.preKnowledge(a)?.let { "\nBase YAMNet : $it" } ?: ""
                audStatus.text = verdict(r, "J'entends :") + preA
                setConf(audConf, (r.second * 100).toInt())
            }
        ), lp(10))
        cs.addView(ghost("\uD83D\uDDD1 Oublier les sons") { audioBrain.forget(); audStatus.text = "Mémoire sons effacée." }, lp(10))
        cs.addView(audStatus)
        audConf = confBar()
        cs.addView(audConf, lp(6))
        box.addView(cs, lp(16))

        // Code
        val cc = card(cOrange)
        cc.addView(sectionTitle("\uD83D\uDCBB  Code & Texte", cOrange))
        codeInput = field("Colle du code à apprendre", 3)
        codePrompt = field("Début de code à compléter")
        codeStatus = status()
        cc.addView(codeInput)
        cc.addView(pill("Apprendre ce code", cOrange, cOrangeDark) {
            val t = codeInput.text.toString()
            if (t.length < 10) return@pill toast("Colle au moins quelques lignes")
            codeBrain.learn(t); codeStatus.text = "Appris ! Mémoire : ${codeBrain.size()} motifs"; codeInput.setText("")
        }, lp(10))
        cc.addView(codePrompt, lp(10))
        cc.addView(rowEqual(
            pill("Compléter", cOrange, cOrangeDark) {
                codeStatus.text = codeBrain.complete(codePrompt.text.toString(), 200, profile.creativity)
            },
            ghost("\uD83D\uDDD1 Oublier") { codeBrain.forget(); codeStatus.text = "Mémoire code effacée." }
        ), lp(10))
        cc.addView(codeStatus)
        box.addView(cc, lp(16))

        // Entraînement massif
        val cm = card(cGreen)
        cm.addView(sectionTitle("\uD83D\uDCDA  Entraînement massif", cGreen))
        cm.addView(TextView(this).apply {
            text = "Choisis un dossier (téléphone ou carte SD) : images ET modèles 3D (obj/stl) apprennent avec le nom du dossier comme étiquette, musiques (mp3, flac, ogg, wav...) nourrissent l'IA sons, textes/code l'IA code. 100% local."
            setTextColor(cMuted); setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); setPadding(0, 0, 0, dp(8))
        })
        scanStatus = status()
        cm.addView(rowEqual(
            pill("Choisir un dossier", cGreen, cGreenDark) {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 2)
            },
            ghost("Stop") { scanner.cancel = true }
        ))
        cm.addView(pill("\uD83D\uDCF1 TOUT scanner (téléphone + SD)", cGreenDark, Color.parseColor("#047857")) {
            startFullScan()
        }, lp(10))
        cm.addView(scanStatus)
        box.addView(cm, lp(16))

        // Internet
        val cw = card(Color.parseColor("#0EA5E9"))
        cw.addView(sectionTitle("\uD83C\uDF10  Internet", Color.parseColor("#0EA5E9")))
        cw.addView(TextView(this).apply {
            text = "Nourris tes IA depuis le web : le texte d'une page va dans l'IA code, les images d'une page dans l'IA images (avec ton étiquette). Ou tape juste un sujet et utilise Wikipédia !"
            setTextColor(cMuted); setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); setPadding(0, 0, 0, dp(8))
        })
        webUrl = field("URL ou sujet (ex: wikipedia.org/... ou chat)")
        webLabel = field("Étiquette pour les images (ex: chat)")
        webStatus = status()
        cw.addView(webUrl)
        cw.addView(webLabel, lp(10))
        cw.addView(rowEqual(
            pill("Texte \u2192 IA code", Color.parseColor("#0EA5E9"), Color.parseColor("#0284C7")) {
                val u = webUrl.text.toString().trim()
                if (u.isEmpty()) return@pill toast("Entre une URL")
                webStatus.text = "Chargement..."
                web.learnPageText(u) { webStatus.text = it; codeStatus.text = "Mémoire : ${codeBrain.size()} motifs" }
            },
            pill("Images \u2192 IA images", Color.parseColor("#0EA5E9"), Color.parseColor("#0284C7")) {
                val u = webUrl.text.toString().trim()
                val l = webLabel.text.toString().trim()
                if (u.isEmpty() || l.isEmpty()) return@pill toast("URL ET étiquette nécessaires")
                webStatus.text = "Chargement..."
                web.learnPageImages(u, l,
                    onProgress = { webStatus.text = it },
                    onDone = { webStatus.text = it; imgStatus.text = "Appris : ${imageBrain.summary()}" })
            }
        ), lp(10))
        cw.addView(pill("\uD83D\uDCD6 Apprendre depuis Wikipédia", Color.parseColor("#0EA5E9"), Color.parseColor("#0284C7")) {
            val u = webUrl.text.toString().trim()
            if (u.isEmpty()) return@pill toast("Tape un sujet (ex: chat)")
            webStatus.text = "Chargement de Wikipédia..."
            web.learnWikipedia(u) { webStatus.text = it; codeStatus.text = "Mémoire : ${codeBrain.size()} motifs" }
        }, lp(10))
        cw.addView(webStatus)
        box.addView(cw, lp(16))

        // Exploration autonome
        val cx = card(Color.parseColor("#6366F1"))
        cx.addView(sectionTitle("\uD83E\uDDED  Exploration autonome", Color.parseColor("#6366F1")))
        cx.addView(TextView(this).apply {
            text = "Lâche l'IA sur le web : elle lit, choisit elle-même les liens qui l'intriguent, et apprend de page en page. Sujet simple = Wikipédia, URL complète = ce site-là, VIDE = elle suit sa pure curiosité !"
            setTextColor(cMuted); setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); setPadding(0, 0, 0, dp(8))
        })
        expTopic = field("Sujet, URL... ou rien du tout")
        cx.addView(expTopic)
        expPages = Spinner(this)
        expPages.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            listOf("5 pages", "10 pages", "20 pages", "30 pages"))
        expPages.setSelection(1)
        cx.addView(expPages, lp(8))
        expImages = Switch(this).apply {
            text = "\uD83D\uDDBC Apprendre aussi les images des pages"
            setTextColor(cInk)
        }
        cx.addView(expImages, lp(6))
        expLog = TextView(this).apply {
            setTextColor(cInk); setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            typeface = Typeface.MONOSPACE
            background = GradientDrawable().apply { cornerRadius = dp(12).toFloat(); setColor(cField) }
        }
        cx.addView(rowEqual(
            pill("\uD83E\uDDED Explorer", Color.parseColor("#6366F1"), Color.parseColor("#4F46E5")) {
                val pages = listOf(5, 10, 20, 30)[expPages.selectedItemPosition.coerceIn(0, 3)]
                expLog.text = "\uD83E\uDDED Départ de l'exploration..."
                explorer.explore(expTopic.text.toString(), pages, expImages.isChecked,
                    onLog = { expLog.text = it },
                    onDone = {
                        expLog.text = "${expLog.text}\n$it"
                        codeStatus.text = "Mémoire : ${codeBrain.size()} motifs"
                        imgStatus.text = "Appris : ${imageBrain.summary()}"
                    })
            },
            ghost("\u23F9 Stop") { explorer.cancel = true }
        ), lp(10))
        cx.addView(expLog, lp(10))
        box.addView(cx, lp(16))

        // Bilan & Examen
        val ce = card(Color.parseColor("#8B5CF6"))
        ce.addView(sectionTitle("\uD83D\uDCCA  Bilan & Examen", Color.parseColor("#8B5CF6")))
        ce.addView(TextView(this).apply {
            text = "L'app mémorise un échantillon de tout ce que tes IA apprennent. L'examen les fait replancher dessus : score global, note par étiquette, et \u26A0 sur ce qu'il faut retravailler."
            setTextColor(cMuted); setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); setPadding(0, 0, 0, dp(8))
        })
        examOut = TextView(this).apply {
            setTextColor(cInk); setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f); setPadding(0, dp(8), 0, 0)
        }
        ce.addView(pill("\uD83C\uDFC6 Passer l'examen", Color.parseColor("#8B5CF6"), Color.parseColor("#7C3AED")) {
            examOut.text = "Examen en cours..."
            Thread {
                val rImg = imageBrain.exam()
                val rAud = audioBrain.exam()
                // Extraire les scores globaux et les archiver
                val rx = Regex("\\((\\d+)%\\)")
                val pImg = rx.find(rImg)?.groupValues?.get(1)?.toIntOrNull() ?: -1
                val pAud = rx.find(rAud)?.groupValues?.get(1)?.toIntOrNull() ?: -1
                val histFile = java.io.File(profiles.dir(profile.name), "history.txt")
                if (pImg >= 0 || pAud >= 0) {
                    val date = java.text.SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(java.util.Date())
                    try { histFile.appendText("$date|$pImg|$pAud\n") } catch (e: Exception) { }
                }
                // Historique : 12 derniers examens
                val hist = try {
                    if (histFile.exists()) histFile.readLines().takeLast(12) else emptyList()
                } catch (e: Exception) { emptyList() }
                val imgSeries = hist.mapNotNull { it.split("|").getOrNull(1)?.toIntOrNull() }.filter { it >= 0 }
                val audSeries = hist.mapNotNull { it.split("|").getOrNull(2)?.toIntOrNull() }.filter { it >= 0 }
                val prog = StringBuilder()
                if (imgSeries.size >= 2) prog.append("\n\n\uD83D\uDCC8 PROGRESSION (${imgSeries.size} examens)\nImages : ${sparkline(imgSeries)} (dernier ${imgSeries.last()}%)")
                if (audSeries.size >= 2) prog.append("\nSons : ${sparkline(audSeries)} (dernier ${audSeries.last()}%)")
                runOnUiThread {
                    examOut.text = "\uD83D\uDDBC IMAGES\n$rImg\n\n\uD83C\uDFB5 SONS\n$rAud\n\n\uD83D\uDCBB CODE : ${codeBrain.size()} motifs en mémoire" + prog.toString()
                }
            }.start()
        })
        ce.addView(examOut)
        box.addView(ce, lp(16))

        // Orchestrateur
        val cf = card(cInk)
        cf.addView(sectionTitle("\uD83E\uDDE0  Réflexion commune", cInk))
        fusionOut = TextView(this).apply {
            setTextColor(cInk); setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); setPadding(0, dp(8), 0, 0)
        }
        cf.addView(pill("Penser ensemble", Color.parseColor("#334155"), cInk) {
            val local = orchestrator.thinkTogether()
            fusionOut.text = local
            if (remote.ready()) {
                fusionOut.text = local + "\n\n\u2601\uFE0F Le cerveau distant réfléchit..."
                val img = orchestrator.lastImage?.let { "je vois « ${it.first} » (confiance ${(it.second * 100).toInt()}%)" } ?: "rien vu"
                val aud = orchestrator.lastAudio?.let { "j'entends « ${it.first} » (confiance ${(it.second * 100).toInt()}%)" } ?: "rien entendu"
                remote.ask("Mes petites IA locales sur téléphone disent : $img et $aud. En 2-3 phrases en français : commente ce qu'elles perçoivent et donne UN conseil concret pour mieux les entraîner.") {
                    fusionOut.text = local + "\n\n\u2601\uFE0F Cerveau distant :\n" + it
                }
            }
        })
        cf.addView(fusionOut)
        box.addView(cf, lp(16))
    }

    // ============================================================
    // ONGLET 2 : PROFILS & COMPORTEMENTS
    private fun buildProfilesTab(box: LinearLayout) {
        val cp = card(cInk)
        cp.addView(sectionTitle("\uD83E\uDDEC  Profil actif", cInk))
        profileTitle = TextView(this).apply {
            setTextColor(cInk); typeface = Typeface.DEFAULT_BOLD
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
        }
        behaviorLabel = status()
        cp.addView(profileTitle)
        cp.addView(behaviorLabel)
        box.addView(cp, lp(0))

        // Préréglages de comportement
        val cb = card(cPurple)
        cb.addView(sectionTitle("\uD83C\uDFAD  Comportements prédéfinis", cPurple))
        cb.addView(TextView(this).apply {
            text = "Applique un style au profil actif (sauvegardé automatiquement) :"
            setTextColor(cMuted); setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        })
        fun preset(label: String, crea: Int, caut: Int) = pill(label, cPurple, cPurpleDark) {
            profile.creativity = crea; profile.caution = caut; profile.style = label
            profiles.saveBehavior(profile)
            refreshProfilesTab()
            toast("$label appliqué")
        }
        cb.addView(rowEqual(preset("\uD83C\uDFAF Précis", 10, 60), preset("\u2696\uFE0F Équilibré", 50, 30)), lp(10))
        cb.addView(rowEqual(preset("\uD83C\uDFA8 Créatif", 90, 10), preset("\uD83D\uDEE1 Prudent", 30, 70)), lp(10))

        // Réglages fins
        cb.addView(body("Créativité (complétion de code) :"), lp(14))
        creaBar = SeekBar(this).apply {
            max = 100
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, v: Int, fromUser: Boolean) {
                    if (fromUser) { profile.creativity = v; profile.style = "Personnalisé"; profiles.saveBehavior(profile); updateBehaviorLabel() }
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        cb.addView(creaBar)
        cb.addView(body("Prudence (l'IA avoue son doute sous ce seuil de confiance) :"))
        cautBar = SeekBar(this).apply {
            max = 100
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, v: Int, fromUser: Boolean) {
                    if (fromUser) { profile.caution = v; profile.style = "Personnalisé"; profiles.saveBehavior(profile); updateBehaviorLabel() }
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        cb.addView(cautBar)
        box.addView(cb, lp(16))

        // Liste + création de profils
        val cl = card(cGreen)
        cl.addView(sectionTitle("\uD83D\uDCC2  Mes profils", cGreen))
        cl.addView(TextView(this).apply {
            text = "Chaque profil a sa propre mémoire (images, sons, code). Bascule de l'un à l'autre comme on change de modèle d'IA."
            setTextColor(cMuted); setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); setPadding(0, 0, 0, dp(8))
        })
        profileListBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cl.addView(profileListBox)
        newProfileName = field("Nom du nouveau profil (ex: musique)")
        cl.addView(newProfileName, lp(10))
        cl.addView(pill("Créer et basculer", cGreen, cGreenDark) {
            val n = newProfileName.text.toString().trim().lowercase().replace(Regex("[^a-z0-9_-]"), "")
            if (n.isEmpty()) { toast("Donne un nom (lettres/chiffres)"); return@pill }
            newProfileName.setText("")
            loadProfile(n)
        }, lp(10))
        cl.addView(rowEqual(
            pill("\uD83D\uDCBE Sauvegarder", Color.parseColor("#64748B"), Color.parseColor("#475569")) { backupProfile() },
            pill("\u267B\uFE0F Restaurer", Color.parseColor("#64748B"), Color.parseColor("#475569")) { restoreProfile() }
        ), lp(10))
        box.addView(cl, lp(16))

        // Cerveau distant (Gemini)
        val cr = card(Color.parseColor("#F59E0B"))
        cr.addView(sectionTitle("\u2601\uFE0F  Cerveau distant (Gemini)", Color.parseColor("#F59E0B")))
        cr.addView(TextView(this).apply {
            text = "Un gros modèle en ligne pour épauler tes 3 IA locales. Choisis ton fournisseur : Gemini et Groq ont des niveaux GRATUITS ; Claude, OpenAI et Mistral sont payants à l'usage. Chaque fournisseur garde sa propre clé. ATTENTION : activé, tes questions partent chez le fournisseur choisi. Désactivé, tout reste 100% local. Les mémoires de tes IA ne sont jamais envoyées."
            setTextColor(cMuted); setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); setPadding(0, 0, 0, dp(8))
        })
        remoteSwitch = Switch(this).apply {
            text = "Activer le cerveau distant"
            setTextColor(cInk)
            isChecked = remote.enabled
            setOnCheckedChangeListener { _, checked ->
                remote.enabled = checked
                remoteStatus.text = if (checked) "Activé \u2601\uFE0F" else "Désactivé — 100% local \uD83D\uDD12"
            }
        }
        cr.addView(remoteSwitch)
        providerSpinner = Spinner(this)
        providerSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, remote.providerNames())
        providerSpinner.setSelection(remote.providerIndex)
        providerSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                remote.providerIndex = position
                remoteKeyField.setText(remote.apiKey)
                remoteStatus.text = "Fournisseur : ${remote.provider.name}"
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        cr.addView(providerSpinner, lp(8))
        remoteKeyField = field("Colle ta clé API Gemini ici")
        if (remote.apiKey.isNotBlank()) remoteKeyField.setText(remote.apiKey)
        cr.addView(remoteKeyField, lp(10))
        remoteStatus = status().also {
            it.text = if (remote.enabled) "Activé \u2601\uFE0F" else "Désactivé — 100% local \uD83D\uDD12"
        }
        cr.addView(rowEqual(
            pill("Enregistrer la clé", Color.parseColor("#F59E0B"), Color.parseColor("#D97706")) {
                remote.apiKey = remoteKeyField.text.toString()
                remoteStatus.text = if (remote.apiKey.isBlank()) "Clé effacée." else "Clé enregistrée \u2714"
            },
            pill("Tester", Color.parseColor("#F59E0B"), Color.parseColor("#D97706")) {
                remoteStatus.text = "Test en cours..."
                remote.ask("Réponds juste : OK, je suis là !") { remoteStatus.text = it }
            }
        ), lp(10))
        remoteAsk = field("Pose une question au cerveau distant")
        cr.addView(remoteAsk, lp(10))
        cr.addView(pill("Demander", Color.parseColor("#F59E0B"), Color.parseColor("#D97706")) {
            val q = remoteAsk.text.toString().trim()
            if (q.isEmpty()) return@pill toast("Écris une question")
            remoteStatus.text = "Réflexion..."
            remote.ask(q) { remoteStatus.text = it }
        }, lp(10))
        cr.addView(remoteStatus)
        box.addView(cr, lp(16))
    }

    private fun updateBehaviorLabel() {
        behaviorLabel.text = "Comportement : ${profile.style} — créativité ${profile.creativity}/100, prudence ${profile.caution}/100"
    }

    private fun refreshProfilesTab() {
        if (!::profileTitle.isInitialized) return
        profileTitle.text = "\u25B6 ${profile.name}"
        updateBehaviorLabel()
        creaBar.progress = profile.creativity
        cautBar.progress = profile.caution
        profileListBox.removeAllViews()
        val all = (profiles.list() + profile.name).distinct().sorted()
        for (name in all) {
            val isCurrent = name == profile.name
            val label = if (isCurrent) "\u2705 $name (actif)" else name
            val btnSwitch = pill(label, if (isCurrent) cGreen else Color.parseColor("#94A3B8"),
                if (isCurrent) cGreenDark else Color.parseColor("#64748B")) {
                if (!isCurrent) loadProfile(name)
            }
            val row = if (isCurrent) rowEqual(btnSwitch)
            else rowEqual(btnSwitch, ghost("\uD83D\uDDD1") {
                profiles.delete(name); refreshProfilesTab(); toast("Profil « $name » supprimé")
            })
            profileListBox.addView(row, lp(8))
        }
    }

    // ============================================================
    // ONGLET 3 : TUTORIEL
    private fun buildTutoTab(box: LinearLayout) {
        fun tutoCard(accent: Int, title: String, text: String) {
            val c = card(accent)
            c.addView(sectionTitle(title, accent))
            c.addView(body(text))
            box.addView(c, lp(if (box.childCount == 0) 0 else 16))
        }
        tutoCard(cInk, "\uD83C\uDF93  Les 3 règles d'or",
            "1. QUANTITÉ : donne au moins 5 à 10 exemples par étiquette. Avec 1 seul exemple, l'IA apprend par cœur au lieu de comprendre.\n\n" +
            "2. VARIÉTÉ : varie les exemples ! Pour « chat » : chats de différentes couleurs, angles, arrière-plans. L'IA apprendra le concept, pas une photo précise.\n\n" +
            "3. ÉQUILIBRE : donne à peu près autant d'exemples à chaque étiquette. Si « chat » a 20 photos et « chien » 2, l'IA répondra presque toujours « chat ».")
        tutoCard(cBlue, "\uD83E\uDDE0  TensorFlow : la super-vision",
            "Si le titre de la carte Images affiche « TensorFlow \uD83E\uDDE0 », ton IA images utilise MobileNet, un réseau pré-entraîné sur 1,2 MILLION d'images.\n\n" +
            "\u2022 Elle reconnaît ~1000 objets dès l'installation (ligne « Base MobileNet » sous Deviner).\n" +
            "\u2022 Ton entraînement personnalisé se construit PAR-DESSUS cette base (transfer learning) : 3-4 exemples suffisent souvent là où il en fallait 10.\n" +
            "\u2022 Ancien et nouveau cerveau images sont séparés : tes apprentissages pixels d'avant restent sauvegardés.\n" +
            "\u2022 Si le modèle n'a pas pu être embarqué dans l'APK, l'app retombe automatiquement sur le mode pixels (titre « couleur »).")
        tutoCard(cBlue, "\uD83D\uDDBC  Bien entraîner les images",
            "• Choisis des photos où le sujet remplit bien l'image.\n" +
            "• Évite les arrière-plans trop chargés au début.\n" +
            "• L'app génère automatiquement des variantes (miroir, clair, sombre) : chaque photo compte donc x4.\n" +
            "• Teste avec « Deviner » sur une photo JAMAIS apprise : c'est le vrai test !\n" +
            "• Les modèles 3D (obj/stl) scannés arrivent aussi ici : classe-les en dossiers par forme.")
        tutoCard(cPurple, "\uD83C\uDFB5  Bien entraîner les sons",
            "• Enregistre dans un endroit calme, le son doit être net.\n" +
            "• Fais durer le son sur les 2 secondes (une note tenue vaut mieux qu'un « bip » perdu dans le silence).\n" +
            "• 5 à 10 enregistrements par son, en variant légèrement.\n" +
            "• Via l'entraînement massif, tes mp3/flac/ogg sont appris par dossier : range ta musique en dossiers par genre (rock/, jazz/...) et l'IA apprendra à reconnaître les genres !")
        tutoCard(cOrange, "\uD83D\uDCBB  Bien entraîner le code",
            "• Colle ton propre code : l'IA apprend TON style.\n" +
            "• Plus tu donnes de code du même langage, plus les complétions sont cohérentes.\n" +
            "• Mélanger les langages rend les complétions exotiques (parfois drôle, rarement utile).\n" +
            "• La créativité (onglet Profils) change tout : basse = complétions sages et répétitives, haute = surprenantes.")
        tutoCard(cGreen, "\uD83D\uDCDA  Bien utiliser l'entraînement massif",
            "LA méthode la plus puissante :\n\n" +
            "1. Crée un dossier Entrainement/ sur ton téléphone ou ta carte SD.\n" +
            "2. Dedans, crée un sous-dossier par catégorie : chats/, chiens/, rock/, jazz/...\n" +
            "3. Mets photos, musiques ou modèles 3D dans le bon dossier.\n" +
            "4. Onglet Entraîner → Choisir un dossier → sélectionne Entrainement/.\n\n" +
            "Le nom de chaque sous-dossier devient l'étiquette. Tu peux relancer plusieurs scans, les IA cumulent tout.\n\n" +
            "TOUT SCANNER : le bouton « TOUT scanner » parcourt l'intégralité du téléphone ET de la carte SD. Android demandera « Accès à tous les fichiers » : active-le pour IA Trio puis relance. Parfait pour un gros bagage général ; des dossiers bien rangés restent plus précis.")
        tutoCard(Color.parseColor("#6366F1"), "\uD83E\uDDED  L'exploration autonome",
            "Le mode le plus libre : l'IA vagabonde seule sur le web.\n\n" +
            "\u2022 SUJET (« volcans ») : elle part de l'article Wikipédia et suit les liens qui l'intriguent — un mélange de familier (mots qu'elle connaît) et de pure découverte.\n" +
            "\u2022 URL complète : elle explore ce site-là, de lien en lien (même domaine).\n" +
            "\u2022 RIEN : la curiosité pure ! Elle pioche dans ses propres étiquettes, ou part d'une page Wikipédia au hasard si elle ne connaît rien.\n" +
            "\u2022 Le journal de bord montre ses lectures et ses coups de cœur en direct.\n" +
            "\u2022 Active « Apprendre aussi les images » pour qu'elle étiquette les photos des pages avec le titre de l'article.\n" +
            "\u2022 Garde-fous : nombre de pages limité, Stop à tout moment, pause polie entre les pages. Tout reste 100% local.\n\n" +
            "Astuce : lance une exploration « vide » après un gros scan de photos — elle ira se documenter sur ce qu'elle a vu !")
        tutoCard(Color.parseColor("#0EA5E9"), "\uD83C\uDF10  Bien utiliser Internet",
            "\u2022 Texte \u2192 IA code : colle l'URL d'un article, d'une doc, d'un blog. Le texte nourrit les complétions.\n" +
            "\u2022 Images \u2192 IA images : colle l'URL d'une page pleine de photos du même sujet (ex: une recherche d'images de chats), donne l'étiquette « chat », et hop : jusqu'à 8 images apprises d'un coup.\n" +
            "\u2022 Wikipédia : le plus simple ! Tape juste un sujet (« guitare », « python ») et l'article français entier est appris.\n" +
            "\u2022 Astuce : combine avec les profils. Un profil « cuisine » nourri d'articles de recettes complétera tes phrases comme un chef !")
        tutoCard(Color.parseColor("#F59E0B"), "\u2601\uFE0F  Le cerveau distant (optionnel)",
            "Ton téléphone est limité ? Un gros modèle en ligne peut épauler tes IA.\n\n" +
            "OBTENIR UNE CLÉ GRATUITE :\n" +
            "1. Va sur aistudio.google.com (compte Google requis).\n" +
            "2. Clique sur « Get API key » \u2192 « Create API key ».\n" +
            "3. Copie la clé, colle-la dans l'onglet Profils, Enregistrer, puis active l'interrupteur.\n\n" +
            "AUTRES FOURNISSEURS : Groq gratuit (console.groq.com), Claude (console.anthropic.com), OpenAI (platform.openai.com), Mistral (console.mistral.ai). Choisis dans la liste déroulante : chaque fournisseur garde sa propre clé.\n\n" +
            "CE QUE ÇA CHANGE :\n" +
            "\u2022 Tu peux poser des questions libres au cerveau distant.\n" +
            "\u2022 « Penser ensemble » : il commente ce que tes IA perçoivent et te conseille.\n\n" +
            "VIE PRIVÉE : activé, tes questions partent chez le fournisseur choisi. Désactivé (interrupteur sur off), TOUT reste local. Les mémoires de tes 3 IA ne sont jamais envoyées.")
        tutoCard(Color.parseColor("#8B5CF6"), "\uD83D\uDCCA  Le bilan et l'examen",
            "Comment savoir si tes IA sont bien entraînées ? Fais-les passer l'examen !\n\n" +
            "\u2022 L'app garde un échantillon de tout ce qui est appris (jusqu'à 400 par IA).\n" +
            "\u2022 Passer l'examen : chaque IA repasse sur ses échantillons, score global + note par étiquette.\n" +
            "\u2022 Les étiquettes marquées \u26A0 (moins de 60%) ont besoin de plus d'exemples, ou de plus variés.\n" +
            "\u2022 Repasse l'examen après chaque grosse session : un mini-graphique \uD83D\uDCC8 de tes 12 derniers scores apparaît, tu VOIS tes IA progresser.\n" +
            "\u2022 Les barres colorées sous chaque IA montrent la confiance en direct : rouge = doute, orange = moyen, vert = sûre.")
        tutoCard(Color.parseColor("#14B8A6"), "\uD83D\uDC41  La vision en direct",
            "L'IA regarde le monde par ta caméra et devine en continu !\n\n" +
            "\u2022 Démarrer \u2192 pointe un objet \u2192 elle affiche ce qu'elle croit voir.\n" +
            "\u2022 Elle se trompe ? Écris le bon nom et « Apprendre cette vue » : elle apprend sur le champ. C'est la façon la plus rapide et amusante de l'entraîner.\n" +
            "\u2022 Fais le tour de la maison : tasse, plante, télécommande... 5-6 vues par objet sous différents angles et elle devient bluffante.\n" +
            "\u2022 La vision en direct alimente aussi « Penser ensemble » (l'orchestrateur).")
        tutoCard(Color.parseColor("#D946EF"), "\u2728  Créer : texte \u2192 image, musique, code",
            "Tes mots deviennent des œuvres, 100% sur ton téléphone.\n\n" +
            "\u2022 \uD83D\uDDBC IMAGE : art génératif — le texte choisit la palette, les formes, la symétrie. Même texte = même image, à jamais. Change UN mot et tout change.\n" +
            "\u2022 \uD83C\uDFB5 MUSIQUE : vraie synthèse — le texte choisit la gamme (majeure=joyeux, mineure=mélancolique, pentatonique=planant, orientale=mystérieux), le tempo et la mélodie. Sauvée en WAV.\n" +
            "\u2022 \uD83D\uDCBB CODE : ton IA code locale génère dans TON style — ou le cerveau distant s'il est activé (qualité pro).\n" +
            "\u2022 Tout est enregistré dans Download/IATrio/creations/ (avec la permission fichiers).\n\n" +
            "Idée : crée la musique ET l'image du même texte — elles partagent la même âme. Puis fais-la écouter à ton IA sons pour qu'elle apprenne tes propres créations !")
        tutoCard(Color.parseColor("#0891B2"), "\uD83D\uDCAC  Le Chat",
            "Un seul endroit pour parler à tous tes cerveaux.\n\n" +
            "\u2022 \uD83E\uDD16 AUTO : le plus malin disponible répond — cerveau distant s'il est activé, sinon ton enfant, sinon le cerveau code local.\n" +
            "\u2022 Le cerveau distant reçoit le CONTEXTE de tes IA : ce qu'elles connaissent, ont vu et entendu — ses réponses parlent vraiment de TON app.\n" +
            "\u2022 Chaque message que tu écris nourrit l'IA code : discuter, c'est entraîner.\n" +
            "\u2022 Parler à l'enfant ici le fait grandir, comme dans son onglet.\n" +
            "\u2022 \uD83C\uDFA4 micro, \uD83D\uDD0A voix, historique sauvegardé par profil, \uD83D\uDDD1 pour repartir de zéro.")
        tutoCard(Color.parseColor("#84CC16"), "\uD83E\uDD18  Le Punk",
            "Un punk se balade sur l'écran et pense à voix haute !\n\n" +
            "\u2022 Choisis quel cerveau l'habite : le cerveau code du profil actif, n'importe quel enfant de la fratrie, ou le cerveau distant (s'il est activé).\n" +
            "\u2022 « Libérer le punk » : il marche, fait demi-tour, s'arrête pour rêver, et une pensée lui traverse la tête toutes les quelques secondes.\n" +
            "\u2022 Touche-le pour le faire réagir immédiatement.\n" +
            "\u2022 Avec un enfant aux commandes, le punk parle avec SA voix (aiguë s'il est bébé !). Avec le cerveau distant, il devient carrément philosophe.\n" +
            "\u2022 Plus le cerveau choisi a appris, plus ses pensées sont intéressantes. Un punk vide dira n'importe quoi — un punk cultivé aussi, mais avec style.")
        tutoCard(Color.parseColor("#EC4899"), "\uD83D\uDC76  L'IA Enfant",
            "Ta création la plus personnelle : une IA qui NAÎT de tes 3 IA.\n\n" +
            "\u2022 À la naissance, elle hérite du savoir des parents du profil actif + des gènes ALÉATOIRES : deux enfants ne seront jamais identiques.\n" +
            "\u2022 Nouveau-né, elle gazouille (« areuh gaga ! »). Parle-lui souvent : chaque message lui donne de l'expérience.\n" +
            "\u2022 Étapes : nouveau-né \uD83C\uDF7C \u2192 bébé \uD83D\uDC76 \u2192 enfant \uD83E\uDDD2 \u2192 ado \uD83C\uDFA7 (attention au caractère !) \u2192 adulte \uD83E\uDDE0.\n" +
            "\u2022 « Hériter des parents » : offre-lui d'un coup un morceau du savoir des 3 IA.\n" +
            "\u2022 Tu peux élever toute une fratrie et comparer leurs personnalités !\n" +
            "\u2022 CROISEMENT \uD83D\uDC9E : deux enfants peuvent avoir un bébé qui mélange leurs gènes (moyenne + mutation) et hérite du savoir des deux. Fais des lignées !\n\n" +
            "LA VOIX \uD83C\uDFA4\uD83D\uDD0A : parle-lui au micro (reconnaissance vocale française) et il répond à voix haute, avec une voix aiguë de bébé qui devient grave en grandissant ! Interrupteur pour couper le son.\n\n" +
            "Astuce : entraîne d'abord bien tes 3 IA (surtout le cerveau code), puis donne naissance — l'enfant naîtra avec un meilleur bagage.")
        tutoCard(cInk, "\uD83E\uDDEC  Profils : comme changer de modèle",
            "Chaque profil = une mémoire totalement séparée + un comportement.\n\n" +
            "Exemples d'usage :\n" +
            "• Un profil « famille » entraîné sur tes photos perso.\n" +
            "• Un profil « musique » entraîné sur ta bibliothèque.\n" +
            "• Un profil « dev » nourri de ton code, réglé sur Précis pour des complétions sages, et un profil « poète » sur Créatif !\n\n" +
            "Comportements :\n" +
            "\uD83C\uDFAF Précis : complétions sages, avoue vite son doute.\n" +
            "\u2696\uFE0F Équilibré : le réglage par défaut.\n" +
            "\uD83C\uDFA8 Créatif : complétions audacieuses, répond toujours.\n" +
            "\uD83D\uDEE1 Prudent : n'affirme que s'il est très sûr.")
    }

    // ============================================================
    // ONGLET 3 : L'IA ENFANT 👶
    private fun buildChildTab(box: LinearLayout) {
        // Naissance
        val cn = card(Color.parseColor("#EC4899"))
        cn.addView(sectionTitle("\uD83C\uDF7C  Donner naissance", Color.parseColor("#EC4899")))
        cn.addView(TextView(this).apply {
            text = "Une nouvelle IA naît des 3 IA du profil actif : elle hérite d'un extrait de leur mémoire, de leur vocabulaire, et reçoit des GÈNES aléatoires (curiosité, calme, créativité, mot préféré). Chaque enfant est unique. Elle grandit en discutant avec toi !"
            setTextColor(cMuted); setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); setPadding(0, 0, 0, dp(8))
        })
        childNameField = field("Prénom de l'enfant (ex: pixel)")
        cn.addView(childNameField)
        cn.addView(pill("\uD83D\uDC76 Donner naissance", Color.parseColor("#EC4899"), Color.parseColor("#DB2777")) {
            val n = childNameField.text.toString().trim()
            if (n.isEmpty()) return@pill toast("Donne-lui un prénom !")
            val labels = imageBrain.labels() + audioBrain.labels()
            child = childManager.birth(n, codeBrain.corpusExcerpt(), labels, profile.creativity)
            childNameField.setText("")
            refreshChildTab()
            childTalkOut.text = "\uD83C\uDF89 ${child!!.name} est né(e) ! " + child!!.speak("bonjour")
        }, lp(10))
        box.addView(cn, lp(0))

        // Discussion
        val cd = card(Color.parseColor("#EC4899"))
        cd.addView(sectionTitle("\uD83D\uDCAC  Parler avec l'enfant", Color.parseColor("#EC4899")))
        childStatus = status()
        childInput = field("Dis-lui quelque chose...")
        childTalkOut = TextView(this).apply {
            setTextColor(cInk); setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); setPadding(0, dp(8), 0, 0)
        }
        cd.addView(childStatus)
        cd.addView(childInput, lp(10))
        cd.addView(rowEqual(
            pill("Parler", Color.parseColor("#EC4899"), Color.parseColor("#DB2777")) {
                val c = child ?: return@pill toast("Donne d'abord naissance à un enfant !")
                val t = childInput.text.toString().trim()
                if (t.isEmpty()) return@pill toast("Écris quelque chose")
                c.listen(t)
                val resp = c.speak(t)
                childTalkOut.text = "${c.name} : " + resp
                speakAloud(resp, c)
                childInput.setText("")
                refreshChildTab()
            },
            pill("\uD83C\uDF81 Hériter des parents", Color.parseColor("#EC4899"), Color.parseColor("#DB2777")) {
                val c = child ?: return@pill toast("Donne d'abord naissance à un enfant !")
                val excerpt = codeBrain.corpusExcerpt(2000)
                val labels = imageBrain.labels() + audioBrain.labels()
                if (excerpt.isBlank() && labels.isEmpty()) return@pill toast("Les parents n'ont encore rien à transmettre")
                if (excerpt.isNotBlank()) c.listen(excerpt)
                if (labels.isNotEmpty()) c.listen("Les parents connaissent " + labels.joinToString(", ") + ". ")
                childTalkOut.text = "${c.name} a reçu un cadeau de savoir des parents ! \uD83C\uDF81"
                refreshChildTab()
            }
        ), lp(10))
        cd.addView(pill("\uD83C\uDFA4 Parler au micro", Color.parseColor("#EC4899"), Color.parseColor("#DB2777")) {
            if (child == null) return@pill toast("Donne d'abord naissance à un enfant !")
            try {
                val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Parle à ${child!!.name}...")
                }
                startActivityForResult(i, 45)
            } catch (e: Exception) {
                toast("Reconnaissance vocale indisponible sur cet appareil")
            }
        }, lp(10))
        cd.addView(Switch(this).apply {
            text = "\uD83D\uDD14 Rappel quotidien (18h) : il réclame de l'attention"
            setTextColor(cInk)
            isChecked = getSharedPreferences("iatrio", 0).getBoolean("daily_notif", false)
            setOnCheckedChangeListener { _, checked ->
                getSharedPreferences("iatrio", 0).edit().putBoolean("daily_notif", checked).apply()
                if (checked) enableDailyReminder() else disableDailyReminder()
            }
        }, lp(6))
        cd.addView(Switch(this).apply {
            text = "\uD83D\uDD0A L'enfant répond à voix haute"
            setTextColor(cInk)
            isChecked = getSharedPreferences("iatrio", 0).getBoolean("child_voice", true)
            setOnCheckedChangeListener { _, checked ->
                getSharedPreferences("iatrio", 0).edit().putBoolean("child_voice", checked).apply()
            }
        }, lp(6))
        cd.addView(childTalkOut)
        box.addView(cd, lp(16))

        // Fratrie
        val cf2 = card(Color.parseColor("#EC4899"))
        cf2.addView(sectionTitle("\uD83D\uDC6A  La fratrie", Color.parseColor("#EC4899")))
        childListBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cf2.addView(childListBox)
        cf2.addView(body("\uD83D\uDC9E Croisement : deux enfants peuvent avoir un bébé qui mélange leurs gènes !"), lp(12))
        crossA = Spinner(this)
        crossB = Spinner(this)
        cf2.addView(rowEqual(crossA, crossB), lp(6))
        babyNameField = field("Prénom du bébé")
        cf2.addView(babyNameField, lp(8))
        cf2.addView(pill("\uD83D\uDC9E Croiser", Color.parseColor("#EC4899"), Color.parseColor("#DB2777")) {
            val names = childManager.list()
            if (names.size < 2) return@pill toast("Il faut au moins 2 enfants pour un croisement !")
            val a = crossA.selectedItem as? String ?: return@pill toast("Choisis le 1er parent")
            val b = crossB.selectedItem as? String ?: return@pill toast("Choisis le 2e parent")
            if (a == b) return@pill toast("Choisis deux enfants DIFFÉRENTS")
            val n = babyNameField.text.toString().trim()
            if (n.isEmpty()) return@pill toast("Donne un prénom au bébé !")
            child = childManager.cross(n, childManager.get(a), childManager.get(b))
            babyNameField.setText("")
            refreshChildTab()
            childTalkOut.text = "\uD83C\uDF89 ${child!!.name} est né(e) du croisement de $a et $b ! " + child!!.speak("bonjour")
        }, lp(8))
        cf2.addView(rowEqual(
            pill("\uD83D\uDCE4 Exporter l'enfant", Color.parseColor("#94A3B8"), Color.parseColor("#64748B")) { exportChild() },
            pill("\uD83D\uDCE5 Importer", Color.parseColor("#94A3B8"), Color.parseColor("#64748B")) { importChildren() }
        ), lp(12))
        box.addView(cf2, lp(16))
    }

    private fun refreshChildTab() {
        if (!::childStatus.isInitialized) return
        childStatus.text = child?.describe() ?: "Aucun enfant pour le moment. Donne naissance ci-dessus !"
        childListBox.removeAllViews()
        val all = childManager.list()
        if (all.isEmpty()) {
            childListBox.addView(body("(personne pour l'instant)"))
            return
        }
        for (name in all) {
            val isCurrent = name == child?.name
            val c = childManager.get(name)
            val label = (if (isCurrent) "\u2705 " else "") + "$name — ${c.stage()}"
            val btn = pill(label, if (isCurrent) Color.parseColor("#EC4899") else Color.parseColor("#94A3B8"),
                if (isCurrent) Color.parseColor("#DB2777") else Color.parseColor("#64748B")) {
                if (!isCurrent) {
                    child = childManager.get(name)
                    childManager.setCurrent(name)
                    refreshChildTab()
                    toast("$name te fait coucou \uD83D\uDC4B")
                }
            }
            val row = if (isCurrent) rowEqual(btn)
            else rowEqual(btn, ghost("\uD83D\uDDD1") {
                childManager.delete(name)
                if (child?.name == name) child = null
                refreshChildTab()
            })
            childListBox.addView(row, lp(8))
        }
        if (::crossA.isInitialized) {
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, all)
            crossA.adapter = adapter
            crossB.adapter = adapter
            if (all.size >= 2) crossB.setSelection(1)
        }
    }

    // ============================================================
    // Vision en direct : caméra + boucle de devinette
    @Suppress("DEPRECATION")
    private fun startLive() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 44)
            toast("Autorise la caméra puis rappuie sur Démarrer")
            return
        }
        if (liveRunning) return
        val open = {
            try {
                liveCamera = android.hardware.Camera.open()
                liveCamera!!.setDisplayOrientation(90)
                liveCamera!!.setPreviewTexture(liveTexture.surfaceTexture)
                liveCamera!!.startPreview()
                liveRunning = true
                liveOut.text = "\uD83D\uDC41 Je regarde..."
                tickLive()
            } catch (e: Exception) {
                liveOut.text = "Erreur caméra : ${e.message}"
            }
        }
        if (liveTexture.isAvailable) open()
        else liveTexture.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(s: android.graphics.SurfaceTexture, w: Int, h: Int) { open() }
            override fun onSurfaceTextureSizeChanged(s: android.graphics.SurfaceTexture, w: Int, h: Int) {}
            override fun onSurfaceTextureDestroyed(s: android.graphics.SurfaceTexture): Boolean { stopLive(); return true }
            override fun onSurfaceTextureUpdated(s: android.graphics.SurfaceTexture) {}
        }
    }

    @Suppress("DEPRECATION")
    private fun stopLive() {
        liveRunning = false
        try { liveCamera?.stopPreview(); liveCamera?.release() } catch (e: Exception) { }
        liveCamera = null
        if (::liveOut.isInitialized) liveOut.text = "Caméra arrêtée."
    }

    private fun tickLive() {
        if (!liveRunning) return
        try {
            val bmp = liveTexture.getBitmap(160, 160)
            if (bmp != null) {
                val r = imageBrain.guess(bmp)
                orchestrator.lastImage = r
                liveOut.text = verdict(r, "\uD83D\uDC41 Je vois :")
                setConf(liveConf, (r.second * 100).toInt())
            }
        } catch (e: Exception) { }
        liveTexture.postDelayed({ tickLive() }, 900)
    }

    override fun onPause() {
        super.onPause()
        stopLive()
        punkRunning = false
        explorer.cancel = true
        creator.stop()
        // Sauvegarde automatique silencieuse du profil actif
        try {
            if (Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager()) {
                val dest = backupDir().apply { mkdirs() }
                profiles.dir(profile.name).listFiles()?.forEach { f ->
                    if (f.isFile) f.copyTo(File(dest, f.name), overwrite = true)
                }
            }
        } catch (e: Exception) { }
    }

    // ============================================================
    // Sauvegarde / restauration du profil actif dans Download/IATrio/
    private fun backupDir(): File =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "IATrio/${profile.name}")

    private fun backupProfile() {
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            toast("Autorise d'abord « Accès à tous les fichiers » (bouton TOUT scanner)"); return
        }
        try {
            val dest = backupDir().apply { mkdirs() }
            var n = 0
            profiles.dir(profile.name).listFiles()?.forEach { f ->
                if (f.isFile) { f.copyTo(File(dest, f.name), overwrite = true); n++ }
            }
            toast("Profil « ${profile.name} » sauvegardé ($n fichiers) dans Download/IATrio/")
        } catch (e: Exception) { toast("Erreur sauvegarde : ${e.message}") }
    }

    private fun restoreProfile() {
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            toast("Autorise d'abord « Accès à tous les fichiers » (bouton TOUT scanner)"); return
        }
        try {
            val srcDir = backupDir()
            if (!srcDir.exists()) { toast("Aucune sauvegarde trouvée dans Download/IATrio/${profile.name}"); return }
            var n = 0
            srcDir.listFiles()?.forEach { f ->
                if (f.isFile) { f.copyTo(File(profiles.dir(profile.name), f.name), overwrite = true); n++ }
            }
            loadProfile(profile.name)   // recharge les cerveaux restaurés
            toast("Profil restauré ($n fichiers) \u2714")
        } catch (e: Exception) { toast("Erreur restauration : ${e.message}") }
    }

    // ============================================================
    // Scan complet : demande la permission "Tous les fichiers" si besoin
    private fun startFullScan() {
        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                toast("Autorise « Accès à tous les fichiers » puis reviens et relance")
                try {
                    startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName")))
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
                return
            }
        } else {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 43)
                return
            }
        }
        scanStatus.text = "Scan complet en cours... (peut prendre plusieurs minutes)"
        scanner.scanAll(
            onProgress = { scanStatus.text = it },
            onDone = {
                scanStatus.text = it
                imgStatus.text = "Appris : ${imageBrain.summary()}"
                audStatus.text = "Appris : ${audioBrain.summary()}"
                codeStatus.text = "Mémoire : ${codeBrain.size()} motifs"
            })
    }

    // ============================================================
    // ONGLET ✨ : CRÉER — un texte devient image, musique ou code
    private fun buildCreateTab(box: LinearLayout) {
        val cc = card(Color.parseColor("#D946EF"))
        cc.addView(sectionTitle("\u2728  Créer à partir d'un texte", Color.parseColor("#D946EF")))
        cc.addView(TextView(this).apply {
            text = "Écris quelques mots : ils deviennent la GRAINE d'une création. Le même texte redonne toujours la même œuvre — change un mot et tout change ! Image et musique sont générées 100% en local ; le code utilise ton IA locale, ou le cerveau distant s'il est activé."
            setTextColor(cMuted); setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); setPadding(0, 0, 0, dp(8))
        })
        createPrompt = field("Ex : coucher de soleil punk sur l'océan")
        cc.addView(createPrompt)
        cc.addView(rowEqual(
            pill("\uD83D\uDDBC Image", Color.parseColor("#D946EF"), Color.parseColor("#A21CAF")) { doCreateImage() },
            pill("\uD83C\uDFB5 Musique", Color.parseColor("#D946EF"), Color.parseColor("#A21CAF")) { doCreateMusic() },
            pill("\uD83D\uDCBB Code", Color.parseColor("#D946EF"), Color.parseColor("#A21CAF")) { doCreateCode() }
        ), lp(10))
        cc.addView(rowEqual(
            ghost("\u23F9 Stop musique") { creator.stop() }
        ), lp(8))
        createImg = ImageView(this).apply {
            adjustViewBounds = true
            visibility = View.GONE
        }
        cc.addView(createImg, lp(10))
        createOut = TextView(this).apply {
            setTextColor(cInk); setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = GradientDrawable().apply { cornerRadius = dp(12).toFloat(); setColor(cField) }
            visibility = View.GONE
        }
        cc.addView(createOut, lp(10))
        box.addView(cc, lp(0))
    }

    private fun creationsDir(): File? = try {
        if (Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager())
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "IATrio/creations").apply { mkdirs() }
        else null
    } catch (e: Exception) { null }

    private fun stamp(): String =
        java.text.SimpleDateFormat("MMdd_HHmmss", Locale.FRANCE).format(java.util.Date())

    private fun doCreateImage() {
        val t = createPrompt.text.toString().trim()
        if (t.isEmpty()) return toast("Écris quelques mots d'abord")
        createOut.visibility = View.GONE
        Thread {
            val bmp = creator.makeImage(t)
            var saved = ""
            try {
                creationsDir()?.let { d ->
                    val f = File(d, "image_${stamp()}.png")
                    f.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, it) }
                    saved = " Enregistrée dans Download/IATrio/creations/"
                }
            } catch (e: Exception) { }
            runOnUiThread {
                createImg.setImageBitmap(bmp)
                createImg.visibility = View.VISIBLE
                toast("Image créée !$saved")
            }
        }.start()
    }

    private fun doCreateMusic() {
        val t = createPrompt.text.toString().trim()
        if (t.isEmpty()) return toast("Écris quelques mots d'abord")
        toast("Composition en cours...")
        Thread {
            val pcm = creator.makeMusic(t)
            lastMusic = pcm
            var saved = ""
            try {
                creationsDir()?.let { d ->
                    creator.saveWav(pcm, File(d, "musique_${stamp()}.wav"))
                    saved = " WAV dans Download/IATrio/creations/"
                }
            } catch (e: Exception) { }
            runOnUiThread {
                creator.play(pcm)
                toast("\uD83C\uDFB5 Ta musique !$saved")
            }
        }.start()
    }

    private fun doCreateCode() {
        val t = createPrompt.text.toString().trim()
        if (t.isEmpty()) return toast("Écris quelques mots d'abord")
        createImg.visibility = View.GONE
        createOut.visibility = View.VISIBLE
        if (remote.ready()) {
            createOut.text = "\u2601\uFE0F Le cerveau distant écrit ton code..."
            remote.ask("Écris du code (langage le plus adapté) pour : $t. Réponds UNIQUEMENT avec le code et de courts commentaires en français.") {
                createOut.text = it
            }
        } else {
            createOut.text = if (codeBrain.size() > 0)
                codeBrain.complete("// $t\n", 400, profile.creativity)
            else
                "(Mon IA code est vide : apprends-lui du code d'abord, ou active le cerveau distant dans Profils pour du code de qualité pro.)"
        }
    }

    // ============================================================
    // ONGLET 💬 : LE CHAT — discuter avec tous les cerveaux au même endroit
    private fun buildChatTab(box: LinearLayout) {
        val cc = card(Color.parseColor("#0891B2"))
        cc.addView(sectionTitle("\uD83D\uDCAC  Discuter avec IA Trio", Color.parseColor("#0891B2")))
        cc.addView(TextView(this).apply {
            text = "Une seule conversation, tous tes cerveaux. En mode Auto, le plus malin disponible répond — et le cerveau distant reçoit le CONTEXTE de tes IA (ce qu'elles ont vu, entendu, appris) pour des réponses personnalisées. Tout ce que tu écris nourrit aussi ton IA code."
            setTextColor(cMuted); setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); setPadding(0, 0, 0, dp(8))
        })
        chatSource = Spinner(this)
        cc.addView(chatSource)
        cc.addView(Switch(this).apply {
            text = "\uD83D\uDD0A Réponses à voix haute"
            setTextColor(cInk)
            isChecked = getSharedPreferences("iatrio", 0).getBoolean("chat_voice", false)
            setOnCheckedChangeListener { _, checked ->
                getSharedPreferences("iatrio", 0).edit().putBoolean("chat_voice", checked).apply()
            }
        }, lp(6))
        box.addView(cc, lp(0))

        val cm2 = card(Color.parseColor("#0891B2"))
        chatBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        cm2.addView(chatBox)
        chatInput = field("Écris ton message...")
        cm2.addView(chatInput, lp(10))
        cm2.addView(rowEqual(
            pill("Envoyer", Color.parseColor("#0891B2"), Color.parseColor("#0E7490")) {
                val t = chatInput.text.toString().trim()
                if (t.isEmpty()) return@pill toast("Écris quelque chose")
                chatInput.setText("")
                chatSend(t)
            },
            pill("\uD83C\uDFA4", Color.parseColor("#0891B2"), Color.parseColor("#0E7490")) {
                try {
                    val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                    }
                    startActivityForResult(i, 47)
                } catch (e: Exception) { toast("Reconnaissance vocale indisponible") }
            },
            ghost("\uD83D\uDDD1") {
                chatFile().delete(); chatBox.removeAllViews(); toast("Conversation effacée")
            }
        ), lp(10))
        box.addView(cm2, lp(16))
        loadChatHistory()
    }

    private fun refreshChatSources() {
        if (!::chatSource.isInitialized) return
        val opts = mutableListOf("\uD83E\uDD16 Auto (le plus malin dispo)", "\uD83D\uDCBB Cerveau code local")
        childManager.currentName()?.let { opts.add("\uD83D\uDC76 Enfant : $it") }
        if (remote.ready()) opts.add("\u2601\uFE0F Cerveau distant (${remote.provider.name})")
        val keep = chatSource.selectedItemPosition
        chatSource.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opts)
        if (keep in opts.indices) chatSource.setSelection(keep)
    }

    private fun chatFile(): File = File(profiles.dir(profile.name), "chat.txt")

    private fun addChatBubble(text: String, mine: Boolean): TextView {
        val b = TextView(this).apply {
            this.text = text
            setTextColor(if (mine) Color.WHITE else cInk)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dp(14), dp(9), dp(14), dp(9))
            maxWidth = dp(280)
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(if (mine) Color.parseColor("#0891B2") else Color.parseColor("#E2E8F0"))
            }
        }
        val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        p.setMargins(0, dp(6), 0, 0)
        val row = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            gravity = if (mine) Gravity.END else Gravity.START
            addView(b, p)
        }
        chatBox.addView(row)
        while (chatBox.childCount > 30) chatBox.removeViewAt(0)
        return b
    }

    private fun loadChatHistory() {
        if (!::chatBox.isInitialized) return
        chatBox.removeAllViews()
        try {
            if (chatFile().exists()) {
                chatFile().readLines().takeLast(20).forEach { line ->
                    val i = line.indexOf('|')
                    if (i > 0) addChatBubble(line.substring(i + 1).replace("\\n", "\n"), line.startsWith("U"))
                }
            }
        } catch (e: Exception) { }
    }

    private fun chatPersist(prefix: String, text: String) {
        try { chatFile().appendText(prefix + "|" + text.replace("\n", "\\n") + "\n") } catch (e: Exception) { }
    }

    private fun chatSend(msg: String) {
        addChatBubble(msg, true)
        chatPersist("U", msg)
        codeBrain.learn(msg)   // la conversation nourrit aussi l'IA code
        val waiting = addChatBubble("...", false)
        val label = chatSource.selectedItem as? String ?: ""
        val sel = chatSource.selectedItemPosition

        fun reply(text: String, pitch: Float = 1.0f) {
            val t = text.trim().ifBlank { "..." }
            waiting.text = t
            chatPersist("A", t)
            if (ttsReady && getSharedPreferences("iatrio", 0).getBoolean("chat_voice", false)) {
                try { tts?.setPitch(pitch); tts?.setSpeechRate(1.0f)
                    tts?.speak(t.take(300), TextToSpeech.QUEUE_FLUSH, null, "chat") } catch (e: Exception) { }
            }
        }

        val useRemote = label.contains("distant") || (sel == 0 && remote.ready())
        val useChild = !useRemote && (label.contains("Enfant") || (sel == 0 && childManager.currentName() != null))
        when {
            useRemote -> {
                val img = orchestrator.lastImage?.let { "vu récemment : ${it.first}." } ?: ""
                val aud = orchestrator.lastAudio?.let { " Entendu : ${it.first}." } ?: ""
                val known = (imageBrain.labels() + audioBrain.labels()).take(15).joinToString(", ")
                val ctx = "Tu es IA Trio, l'assistant d'une app d'IA locale entraînée par l'utilisateur sur son téléphone. " +
                        "Profil actif : « ${profile.name} ». L'IA locale connaît : $known. $img$aud " +
                        "Réponds en français, bref (2-4 phrases), chaleureux. Message de l'utilisateur : $msg"
                remote.ask(ctx) { reply(it) }
            }
            useChild -> {
                val c = childManager.currentName()?.let { childManager.get(it) }
                if (c != null) {
                    c.listen(msg)
                    reply("${c.name} : " + c.speak(msg), when {
                        c.xp < 30 -> 1.6f; c.xp < 80 -> 1.35f; else -> 1.0f
                    })
                } else reply(codeBrain.complete(msg.takeLast(20), 150, profile.creativity))
            }
            else -> {
                if (codeBrain.size() > 0)
                    reply(codeBrain.complete(msg.takeLast(20), 150, profile.creativity))
                else
                    reply("Ma mémoire est vide ! Apprends-moi des choses (Wikipédia, exploration, scans...) et je te répondrai avec ce que je sais.")
            }
        }
    }

    // ============================================================
    // ONGLET 🤘 : LE PUNK — choisis un cerveau, libère-le sur l'écran
    private fun buildPunkTab(box: LinearLayout) {
        val cp = card(Color.parseColor("#84CC16"))
        cp.addView(sectionTitle("\uD83E\uDD18  Le Punk", Color.parseColor("#84CC16")))
        cp.addView(TextView(this).apply {
            text = "Choisis quel cerveau habite le punk, libère-le, et il se balade en racontant ce qui lui passe par la tête. Touche-le pour le faire réagir !"
            setTextColor(cMuted); setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); setPadding(0, 0, 0, dp(8))
        })
        punkSource = Spinner(this)
        cp.addView(punkSource)
        cp.addView(rowEqual(
            pill("\uD83E\uDD18 Libérer le punk", Color.parseColor("#84CC16"), Color.parseColor("#65A30D")) {
                if (!punkRunning) { punkRunning = true; punkTicks = 0; punkTick() }
                punkThought()
            },
            ghost("\u23F9 Rappeler") { punkRunning = false; punkBubble.text = "..." }
        ), lp(10))
        cp.addView(Switch(this).apply {
            text = "\uD83D\uDD0A Le punk parle à voix haute"
            setTextColor(cInk)
            isChecked = getSharedPreferences("iatrio", 0).getBoolean("punk_voice", false)
            setOnCheckedChangeListener { _, checked ->
                getSharedPreferences("iatrio", 0).edit().putBoolean("punk_voice", checked).apply()
            }
        }, lp(6))
        box.addView(cp, lp(0))

        // L'arène où il se balade
        val ca = card(Color.parseColor("#84CC16"))
        punkArena = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#0F172A"), Color.parseColor("#1E293B"))
            ).apply { cornerRadius = dp(16).toFloat() }
        }
        // Le sol
        punkArena.addView(View(this).apply {
            setBackgroundColor(Color.parseColor("#334155"))
        }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(6)).apply {
            gravity = android.view.Gravity.BOTTOM
        })
        // Bulle + punk, déplacés ensemble
        punkBubble = TextView(this).apply {
            text = "..."
            setTextColor(cInk); setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            maxWidth = dp(190)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat(); setColor(Color.WHITE)
            }
        }
        punkImg = ImageView(this).apply {
            setImageResource(R.drawable.punk_face)
            adjustViewBounds = true
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(120))
            setOnClickListener { punkThought() }
        }
        punkBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            addView(punkBubble)
            addView(punkImg)
        }
        punkArena.addView(punkBox, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = android.view.Gravity.BOTTOM; bottomMargin = dp(6) })
        ca.addView(punkArena, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(380)))
        box.addView(ca, lp(16))
    }

    private fun refreshPunkTab() {
        if (!::punkSource.isInitialized) return
        val sources = mutableListOf("\uD83D\uDCBB Cerveau code du profil « ${profile.name} »")
        childManager.list().forEach { sources.add("\uD83D\uDC76 Enfant : $it") }
        if (remote.ready()) sources.add("\u2601\uFE0F Cerveau distant (${remote.provider.name})")
        val keep = punkSource.selectedItemPosition
        punkSource.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sources)
        if (keep in sources.indices) punkSource.setSelection(keep)
    }

    /** La balade : avance, fait demi-tour aux bords, s'arrête parfois pour rêver. */
    private fun punkTick() {
        if (!punkRunning) return
        val w = punkArena.width.toFloat()
        val pw = punkBox.width.toFloat().coerceAtLeast(dp(64).toFloat())
        if (w > 0) {
            if (punkPause > 0) {
                punkPause--
            } else {
                punkX += punkDir * dp(3)
                if (punkX > w - pw) { punkX = w - pw; punkDir = -1 }
                if (punkX < 0f) { punkX = 0f; punkDir = 1 }
                if (Math.random() < 0.008) punkPause = 20 + (Math.random() * 40).toInt()
                if (Math.random() < 0.005) punkDir = -punkDir
            }
            punkBox.translationX = punkX
            // cycle de marche 3D quand il avance, face vers nous quand il pense
            val paused = punkPause > 0
            if (paused) {
                if (!punkShowingFace) {
                    punkImg.setImageResource(R.drawable.punk_face)
                    punkShowingFace = true; punkFrame = -1
                }
                punkImg.scaleX = 1f
                punkImg.translationY = 0f
            } else {
                val f = (punkTicks / 3) % punkWalk.size
                if (f != punkFrame || punkShowingFace) {
                    punkImg.setImageResource(punkWalk[f])
                    punkFrame = f; punkShowingFace = false
                }
                punkImg.scaleX = punkDir.toFloat()
                punkImg.translationY = 0f
            }
        }
        punkTicks++
        if (punkTicks % 90 == 0) punkThought()
        punkArena.postDelayed({ punkTick() }, 50)
    }

    /** Une pensée lui traverse la tête, selon le cerveau choisi. */
    private fun punkThought() {
        val starters = listOf("Hé, ", "Tu sais quoi, ", "Je pense que ", "La vie c'est ", "Punk un jour, ", "Franchement ")
        val labels = imageBrain.labels() + audioBrain.labels()
        val seed = if (labels.isNotEmpty() && Math.random() < 0.4) labels[(Math.random() * labels.size).toInt()] + " "
                   else starters[(Math.random() * starters.size).toInt()]
        val sel = punkSource.selectedItemPosition.coerceAtLeast(0)
        val kids = childManager.list()
        when {
            sel == 0 -> {
                val t = if (codeBrain.size() > 0) codeBrain.complete(seed, 70, 85)
                        else "Apprends-moi des trucs dans l'onglet Entraîner et je te raconterai ma vie !"
                showPunkThought(t, 0.8f)
            }
            sel - 1 < kids.size -> {
                val c = childManager.get(kids[sel - 1])
                showPunkThought(c.speak(seed), when {
                    c.xp < 30 -> 1.6f
                    c.xp < 80 -> 1.35f
                    else -> 1.0f
                })
            }
            else -> {
                val now = System.currentTimeMillis()
                if (remote.ready() && now - lastRemoteThought > 20_000) {
                    lastRemoteThought = now
                    remote.ask("Tu es un punk à crête verte qui se balade sur l'écran d'un téléphone. Dis UNE pensée courte (max 20 mots), drôle et punk, en français.") {
                        showPunkThought(it, 0.8f)
                    }
                } else if (codeBrain.size() > 0) {
                    showPunkThought(codeBrain.complete(seed, 70, 85), 0.8f)
                }
            }
        }
    }

    private fun showPunkThought(text: String, pitch: Float) {
        val t = text.take(120).replace("\n", " ")
        punkBubble.text = t
        punkPause = maxOf(punkPause, 30)   // il s'arrête et nous regarde pour parler
        if (ttsReady && getSharedPreferences("iatrio", 0).getBoolean("punk_voice", false)) {
            try {
                tts?.setPitch(pitch)
                tts?.setSpeechRate(1.0f)
                tts?.speak(t, TextToSpeech.QUEUE_FLUSH, null, "punk")
            } catch (e: Exception) { }
        }
    }

    // ============================================================
    // La voix de l'enfant : plus il est jeune, plus elle est aiguë !
    private fun speakAloud(text: String, c: ChildBrain) {
        if (!ttsReady) return
        if (!getSharedPreferences("iatrio", 0).getBoolean("child_voice", true)) return
        val pitch = when {
            c.xp < 10 -> 1.9f   // nouveau-né
            c.xp < 30 -> 1.6f   // bébé
            c.xp < 80 -> 1.35f  // enfant
            c.xp < 200 -> 1.1f  // ado
            else -> 1.0f        // adulte
        }
        try {
            tts?.setPitch(pitch)
            tts?.setSpeechRate(if (c.xp < 30) 0.85f else 1.0f)
            tts?.speak(text.take(300), TextToSpeech.QUEUE_FLUSH, null, "child")
        } catch (e: Exception) { }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { tts?.shutdown() } catch (e: Exception) { }
    }

    // ============================================================
    // Rappel quotidien 🔔
    private fun enableDailyReminder() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 46)
        }
        val am = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
        val pi = android.app.PendingIntent.getBroadcast(
            this, 7, Intent(this, DailyReceiver::class.java),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 18); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        am.setInexactRepeating(android.app.AlarmManager.RTC_WAKEUP, cal.timeInMillis,
            android.app.AlarmManager.INTERVAL_DAY, pi)
        toast("Rappel quotidien activé (vers 18h)")
    }

    private fun disableDailyReminder() {
        val am = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
        val pi = android.app.PendingIntent.getBroadcast(
            this, 7, Intent(this, DailyReceiver::class.java),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        am.cancel(pi)
        toast("Rappel désactivé")
    }

    // ============================================================
    // Partage d'enfants 📤📥 via Download/IATrio/enfants/
    private fun childrenShareDir(): File =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "IATrio/enfants")

    private fun exportChild() {
        val c = child ?: return toast("Aucun enfant actif à exporter")
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager())
            return toast("Autorise « Accès à tous les fichiers » (bouton TOUT scanner)")
        try {
            val dest = File(childrenShareDir(), c.name).apply { mkdirs() }
            var n = 0
            c.dir.listFiles()?.forEach { f -> if (f.isFile) { f.copyTo(File(dest, f.name), overwrite = true); n++ } }
            toast("« ${c.name} » exporté ($n fichiers) dans Download/IATrio/enfants/ — envoie ce dossier à un ami !")
        } catch (e: Exception) { toast("Erreur export : ${e.message}") }
    }

    private fun importChildren() {
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager())
            return toast("Autorise « Accès à tous les fichiers » (bouton TOUT scanner)")
        try {
            val srcRoot = childrenShareDir()
            if (!srcRoot.exists()) return toast("Rien à importer dans Download/IATrio/enfants/")
            var n = 0
            srcRoot.listFiles()?.forEach { dir ->
                if (dir.isDirectory) {
                    val target = childManager.get(dir.name).dir
                    dir.listFiles()?.forEach { f -> if (f.isFile) f.copyTo(File(target, f.name), overwrite = true) }
                    n++
                }
            }
            refreshChildTab()
            toast("$n enfant(s) importé(s) ! Bienvenue dans la fratrie \uD83D\uDC6A")
        } catch (e: Exception) { toast("Erreur import : ${e.message}") }
    }

    // ============================================================
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
        if (requestCode == 2 && resultCode == RESULT_OK && data?.data != null) {
            val tree: Uri = data.data!!
            try {
                contentResolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) { }
            scanStatus.text = "Scan en cours..."
            scanner.scan(tree,
                onProgress = { scanStatus.text = it },
                onDone = {
                    scanStatus.text = it
                    imgStatus.text = "Appris : ${imageBrain.summary()}"
                    audStatus.text = "Appris : ${audioBrain.summary()}"
                    codeStatus.text = "Mémoire : ${codeBrain.size()} motifs"
                })
            return
        }
        if (requestCode == 47 && resultCode == RESULT_OK) {
            val heard = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (heard != null) chatSend(heard)
            return
        }
        if (requestCode == 45 && resultCode == RESULT_OK) {
            val heard = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            val c = child
            if (heard != null && c != null) {
                c.listen(heard)
                val resp = c.speak(heard)
                childTalkOut.text = "Toi \uD83C\uDFA4 : $heard\n${c.name} : $resp"
                speakAloud(resp, c)
                refreshChildTab()
            }
            return
        }
        if (requestCode == 1 && resultCode == RESULT_OK && data?.data != null) {
            try {
                contentResolver.openInputStream(data.data!!).use { currentBitmap = BitmapFactory.decodeStream(it) }
                imgStatus.text = "Image chargée \u2714"
            } catch (e: Exception) { toast("Impossible de lire l'image") }
        }
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
