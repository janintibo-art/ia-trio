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
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {

    private lateinit var profiles: ProfileManager
    private lateinit var profile: Profile
    private lateinit var imageBrain: ImageBrain
    private lateinit var audioBrain: AudioBrain
    private lateinit var codeBrain: CodeBrain
    private lateinit var orchestrator: Orchestrator
    private lateinit var scanner: ScanTrainer
    private lateinit var web: WebLearner

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
        val tabNames = listOf("Entraîner", "Profils", "Tutoriel")
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
        tabTuto = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        buildTrainTab(tabTrain)
        buildProfilesTab(tabProfiles)
        buildTutoTab(tabTuto)
        screen.addView(tabTrain, lp(14))
        screen.addView(tabProfiles, lp(14))
        screen.addView(tabTuto, lp(14))

        setContentView(ScrollView(this).apply { isFillViewport = true; addView(screen) })
        showTab(0)
        refreshAllStatuses()
    }

    private fun showTab(i: Int) {
        tabTrain.visibility = if (i == 0) View.VISIBLE else View.GONE
        tabProfiles.visibility = if (i == 1) View.VISIBLE else View.GONE
        tabTuto.visibility = if (i == 2) View.VISIBLE else View.GONE
        tabButtons.forEachIndexed { j, b ->
            b.setTextColor(if (i == j) Color.WHITE else cInk)
            b.typeface = if (i == j) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            b.background = GradientDrawable().apply {
                cornerRadius = dp(22).toFloat()
                setColor(if (i == j) cInk else Color.parseColor("#E2E8F0"))
            }
        }
        if (i == 1) refreshProfilesTab()
    }

    // ============================================================
    // PROFILS : charger / basculer
    private fun loadProfile(name: String, refreshUi: Boolean = true) {
        profiles.setCurrent(name)
        profile = profiles.loadBehavior(name)
        profiles.saveBehavior(profile) // crée behavior.txt si absent
        val d = profiles.dir(name)
        imageBrain = ImageBrain(d)
        audioBrain = AudioBrain(d)
        codeBrain = CodeBrain(d)
        orchestrator = Orchestrator(imageBrain, audioBrain, codeBrain)
        scanner = ScanTrainer(this, imageBrain, audioBrain, codeBrain)
        web = WebLearner(this, imageBrain, codeBrain)
        if (refreshUi) { refreshAllStatuses(); refreshProfilesTab(); toast("Profil « $name » activé") }
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

    // ============================================================
    // ONGLET 1 : ENTRAÎNER
    private fun buildTrainTab(box: LinearLayout) {
        // Images
        val ci = card(cBlue)
        ci.addView(sectionTitle("\uD83D\uDDBC  Images (couleur)", cBlue))
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
                imgStatus.text = verdict(r, "Je pense :")
            }
        ), lp(10))
        ci.addView(ghost("\uD83D\uDDD1 Oublier les images") { imageBrain.forget(); imgStatus.text = "Mémoire images effacée." }, lp(10))
        ci.addView(imgStatus)
        box.addView(ci, lp(0))

        // Sons
        val cs = card(cPurple)
        cs.addView(sectionTitle("\uD83C\uDFB5  Musique & Sons (FFT)", cPurple))
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
                audStatus.text = verdict(r, "J'entends :")
            }
        ), lp(10))
        cs.addView(ghost("\uD83D\uDDD1 Oublier les sons") { audioBrain.forget(); audStatus.text = "Mémoire sons effacée." }, lp(10))
        cs.addView(audStatus)
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

        // Orchestrateur
        val cf = card(cInk)
        cf.addView(sectionTitle("\uD83E\uDDE0  Réflexion commune", cInk))
        fusionOut = TextView(this).apply {
            setTextColor(cInk); setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); setPadding(0, dp(8), 0, 0)
        }
        cf.addView(pill("Penser ensemble", Color.parseColor("#334155"), cInk) { fusionOut.text = orchestrator.thinkTogether() })
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
        box.addView(cl, lp(16))
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
            "Le nom de chaque sous-dossier devient l'étiquette. Tu peux relancer plusieurs scans, les IA cumulent tout.")
        tutoCard(Color.parseColor("#0EA5E9"), "\uD83C\uDF10  Bien utiliser Internet",
            "\u2022 Texte \u2192 IA code : colle l'URL d'un article, d'une doc, d'un blog. Le texte nourrit les complétions.\n" +
            "\u2022 Images \u2192 IA images : colle l'URL d'une page pleine de photos du même sujet (ex: une recherche d'images de chats), donne l'étiquette « chat », et hop : jusqu'à 8 images apprises d'un coup.\n" +
            "\u2022 Wikipédia : le plus simple ! Tape juste un sujet (« guitare », « python ») et l'article français entier est appris.\n" +
            "\u2022 Astuce : combine avec les profils. Un profil « cuisine » nourri d'articles de recettes complétera tes phrases comme un chef !")
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
        if (requestCode == 1 && resultCode == RESULT_OK && data?.data != null) {
            try {
                contentResolver.openInputStream(data.data!!).use { currentBitmap = BitmapFactory.decodeStream(it) }
                imgStatus.text = "Image chargée \u2714"
            } catch (e: Exception) { toast("Impossible de lire l'image") }
        }
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
