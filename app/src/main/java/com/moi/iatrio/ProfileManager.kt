package com.moi.iatrio

import java.io.File

/**
 * Un profil = une mémoire complète (images + sons + code) + un comportement.
 * Comme passer d'un modèle d'IA à un autre : chaque profil apprend et se
 * comporte différemment.
 */
class Profile(
    val name: String,
    var creativity: Int = 50,  // 0 = complétion très sage, 100 = très audacieuse
    var caution: Int = 30,     // seuil de confiance sous lequel l'IA avoue son doute
    var style: String = "Équilibré"
)

class ProfileManager(private val base: File) {
    private val root = File(base, "profiles").apply { mkdirs() }
    private val currentFile = File(base, "current_profile.txt")

    fun list(): List<String> =
        root.listFiles()?.filter { it.isDirectory }?.map { it.name }?.sorted() ?: emptyList()

    fun currentName(): String =
        if (currentFile.exists()) currentFile.readText().trim().ifBlank { "standard" } else "standard"

    fun setCurrent(name: String) { currentFile.writeText(name) }

    fun dir(name: String): File = File(root, name).apply { mkdirs() }

    fun loadBehavior(name: String): Profile {
        val f = File(dir(name), "behavior.txt")
        val p = Profile(name)
        if (f.exists()) {
            for (line in f.readLines()) {
                val kv = line.split("=", limit = 2)
                if (kv.size == 2) when (kv[0]) {
                    "creativity" -> p.creativity = kv[1].toIntOrNull() ?: 50
                    "caution" -> p.caution = kv[1].toIntOrNull() ?: 30
                    "style" -> p.style = kv[1]
                }
            }
        }
        return p
    }

    fun saveBehavior(p: Profile) {
        File(dir(p.name), "behavior.txt")
            .writeText("creativity=${p.creativity}\ncaution=${p.caution}\nstyle=${p.style}")
    }

    /** Migre les mémoires de l'ancienne version (v1-v5) vers le profil standard. */
    fun migrateLegacy() {
        val std = dir("standard")
        for (fn in listOf("image.net", "audio.net", "code.txt")) {
            val old = File(base, fn)
            val neu = File(std, fn)
            if (old.exists() && !neu.exists()) {
                try { old.copyTo(neu); old.delete() } catch (e: Exception) { }
            }
        }
    }

    fun delete(name: String) { dir(name).deleteRecursively() }
}
