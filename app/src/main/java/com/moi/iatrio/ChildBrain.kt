package com.moi.iatrio

import java.io.File
import kotlin.random.Random

/**
 * L'IA ENFANT 👶 : une nouvelle IA qui NAÎT des 3 IA parents.
 *
 * À la naissance elle hérite :
 * - d'un extrait de la mémoire texte des parents (son "bagage")
 * - du vocabulaire des parents (les étiquettes images + sons connues)
 * - de GÈNES avec mutations aléatoires : curiosité, calme, créativité,
 *   et un mot préféré pioché chez les parents.
 * Chaque enfant est donc UNIQUE.
 *
 * Ensuite elle grandit en discutant avec toi : nouveau-né qui gazouille,
 * puis bébé, enfant, ado, adulte. Plus elle apprend, mieux elle parle.
 */
class ChildBrain(val dir: File) {
    val name: String = dir.name
    private val genesFile = File(dir, "genes.txt")
    val brain = CodeBrain(dir)   // sa propre mémoire de langage, séparée

    var xp = 0
    var curiosity = 50
    var calm = 50
    var creativity = 50
    var favoriteWord = ""

    init { load() }

    fun stage(): String = when {
        xp < 10 -> "nouveau-né \uD83C\uDF7C"
        xp < 30 -> "bébé \uD83D\uDC76"
        xp < 80 -> "enfant \uD83E\uDDD2"
        xp < 200 -> "ado \uD83C\uDFA7"
        else -> "adulte \uD83E\uDDE0"
    }

    fun mood(): String {
        val moods = listOf("joyeux \uD83D\uDE04", "curieux \uD83E\uDD14", "rêveur \uD83D\uDCAD",
            "espiègle \uD83D\uDE1C", "câlin \uD83E\uDD17", "somnolent \uD83D\uDE34")
        // humeur stable mais qui évolue avec l'expérience et le caractère
        return moods[(xp / 3 + calm + curiosity) % moods.size]
    }

    /** L'enfant écoute et retient ce que tu lui dis. */
    fun listen(text: String) {
        if (text.length < 2) return
        brain.learn(text)
        xp += (1 + text.length / 40).coerceAtMost(5)
        save()
    }

    /** L'enfant répond, selon son âge et ses gènes. */
    fun speak(prompt: String): String {
        val babbles = listOf("areuh", "ba", "bou", "gaga", "dada", "mmmh", "prrr", "hihi")
        fun babble(n: Int): String =
            (1..n).joinToString(" ") { babbles[Random.nextInt(babbles.size)] } + " !"
        return when {
            xp < 10 -> {
                // nouveau-né : gazouillis, parfois son mot préféré déformé
                val extra = if (favoriteWord.isNotBlank() && Random.nextInt(100) < curiosity)
                    " " + favoriteWord.take(3) + "..." else ""
                babble(2 + Random.nextInt(3)) + extra
            }
            xp < 30 -> {
                // bébé : gazouillis + un petit bout de phrase appris
                val bit = brain.complete(prompt.take(4), 20, 90).takeLast(20)
                babble(1 + Random.nextInt(2)) + " " + bit
            }
            xp < 80 -> brain.complete(prompt, 60, (creativity + 20).coerceAtMost(100))
            xp < 200 -> {
                val attitude = if (calm < 40 && Random.nextInt(100) < 30) "Pff... " else ""
                attitude + brain.complete(prompt, 120, creativity)
            }
            else -> brain.complete(prompt, 200, (creativity - 10).coerceAtLeast(0))
        }
    }

    fun describe(): String =
        "$name — ${stage()}\n" +
        "Humeur : ${mood()}\n" +
        "Expérience : $xp points\n" +
        "Gènes : curiosité $curiosity, calme $calm, créativité $creativity\n" +
        (if (favoriteWord.isNotBlank()) "Mot préféré : « $favoriteWord »\n" else "") +
        "Mémoire : ${brain.size()} motifs de langage"

    fun save() {
        genesFile.writeText(
            "xp=$xp\ncuriosity=$curiosity\ncalm=$calm\ncreativity=$creativity\nfavorite=$favoriteWord"
        )
    }

    private fun load() {
        if (!genesFile.exists()) return
        for (line in genesFile.readLines()) {
            val kv = line.split("=", limit = 2)
            if (kv.size == 2) when (kv[0]) {
                "xp" -> xp = kv[1].toIntOrNull() ?: 0
                "curiosity" -> curiosity = kv[1].toIntOrNull() ?: 50
                "calm" -> calm = kv[1].toIntOrNull() ?: 50
                "creativity" -> creativity = kv[1].toIntOrNull() ?: 50
                "favorite" -> favoriteWord = kv[1]
            }
        }
    }
}

/** La pouponnière : donne naissance, liste et retrouve les enfants. */
class ChildManager(base: File) {
    private val root = File(base, "children").apply { mkdirs() }
    private val currentFile = File(base, "current_child.txt")

    fun list(): List<String> =
        root.listFiles()?.filter { it.isDirectory }?.map { it.name }?.sorted() ?: emptyList()

    fun currentName(): String? =
        if (currentFile.exists()) currentFile.readText().trim().ifBlank { null } else null

    fun setCurrent(name: String) { currentFile.writeText(name) }

    fun get(name: String): ChildBrain = ChildBrain(File(root, name).apply { mkdirs() })

    fun delete(name: String) {
        File(root, name).deleteRecursively()
        if (currentName() == name) currentFile.delete()
    }

    /**
     * NAISSANCE 👶 : mélange l'héritage des parents avec des mutations aléatoires.
     * @param parentCorpus  extrait de la mémoire texte du profil parent
     * @param parentLabels  étiquettes connues des IA images + sons
     * @param parentCreativity  créativité du profil parent
     */
    fun birth(
        name: String,
        parentCorpus: String,
        parentLabels: List<String>,
        parentCreativity: Int
    ): ChildBrain {
        val clean = name.trim().lowercase().replace(Regex("[^a-z0-9_-]"), "").ifBlank { "bebe" }
        val child = get(clean)

        // Gènes : hérités du parent + mutation aléatoire (comme dans la vraie vie)
        child.curiosity = (30 + Random.nextInt(70))
        child.calm = (20 + Random.nextInt(80))
        child.creativity = (parentCreativity + Random.nextInt(-30, 31)).coerceIn(5, 100)
        child.favoriteWord = if (parentLabels.isNotEmpty()) parentLabels[Random.nextInt(parentLabels.size)] else ""

        // Bagage de naissance : un extrait de la mémoire des parents + son vocabulaire
        if (parentCorpus.isNotBlank()) child.brain.learn(parentCorpus.take(3000))
        if (parentLabels.isNotEmpty())
            child.brain.learn("Je connais " + parentLabels.joinToString(", ") + ". ")

        child.xp = 0
        child.save()
        setCurrent(clean)
        return child
    }

    /**
     * CROISEMENT 💞 : deux enfants ont un bébé qui mélange leurs gènes
     * (moyenne + mutation) et hérite d'un morceau du savoir de chacun.
     */
    fun cross(name: String, p1: ChildBrain, p2: ChildBrain): ChildBrain {
        val clean = name.trim().lowercase().replace(Regex("[^a-z0-9_-]"), "").ifBlank { "bebe2" }
        val c = get(clean)
        c.curiosity = ((p1.curiosity + p2.curiosity) / 2 + Random.nextInt(-20, 21)).coerceIn(0, 100)
        c.calm = ((p1.calm + p2.calm) / 2 + Random.nextInt(-20, 21)).coerceIn(0, 100)
        c.creativity = ((p1.creativity + p2.creativity) / 2 + Random.nextInt(-20, 21)).coerceIn(5, 100)
        c.favoriteWord = if (Random.nextBoolean()) p1.favoriteWord else p2.favoriteWord
        val h1 = p1.brain.corpusExcerpt(1500)
        val h2 = p2.brain.corpusExcerpt(1500)
        if (h1.isNotBlank()) c.brain.learn(h1)
        if (h2.isNotBlank()) c.brain.learn(h2)
        c.xp = 0
        c.save()
        setCurrent(clean)
        return c
    }
}
