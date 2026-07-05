package com.moi.iatrio

/**
 * BASE DE CONNAISSANCES MUSICALES 🥁 : 12 genres avec leurs vrais codes,
 * hérités de décennies de production.
 *
 * Chaque pattern est une grille de 16 doubles-croches (une mesure 4/4) :
 * '1' = frappe. Le tempo, le swing et la densité sont ceux du genre réel.
 */
data class MusicStyle(
    val name: String,
    val keys: List<String>,          // mots-clés reconnus dans le texte
    val bpmLo: Int, val bpmHi: Int,  // plage de tempo authentique
    val kick: String,                // grosse caisse (16 pas)
    val snare: String,               // caisse claire / clap
    val hat: String,                 // charley
    val swing: Double,               // retard des doubles-croches impaires
    val chops: List<String>,         // motifs mélodiques du genre (calme -> dense)
    val bassSlots: List<Int>,        // où la basse se pose
    val drumBoost: Double = 1.0,     // énergie de la batterie (0 = pas de batterie)
    val steps: Int = 16              // pas par mesure (18 = mesure impaire 9/8 !)
)

object MusicStyles {
    val all = listOf(
        MusicStyle("Pop", listOf("pop", "chanson", "variete", "variété"), 100, 120,
            "1000000010000000", "0000100000001000", "1010101010101010", 0.0,
            listOf("1000100010001000", "1010001010100010"), listOf(0, 8)),
        MusicStyle("Rock", listOf("rock", "metal", "punk", "grunge", "garage"), 110, 140,
            "1000001010000000", "0000100000001000", "1010101010101010", 0.0,
            listOf("1010101010101010", "1011001010110010"), listOf(0, 8), 1.15),
        MusicStyle("Hip-hop", listOf("hiphop", "hip-hop", "rap", "boombap", "boom-bap"), 85, 95,
            "1000000100100000", "0000100000001000", "1010101010101010", 0.12,
            listOf("1000001010000010", "1001000010010000"), listOf(0, 8)),
        MusicStyle("Trap", listOf("trap", "drill"), 130, 150,
            "1000000000100100", "0000000010000000", "1111111111111111", 0.0,
            listOf("1000000010000000", "1000100000001000"), listOf(0, 10), 1.1),
        MusicStyle("House", listOf("house", "dance", "club", "edm"), 120, 128,
            "1000100010001000", "0000100000001000", "0010001000100010", 0.0,
            listOf("1000100010001000", "1010100010101000"), listOf(0, 8)),
        MusicStyle("Techno", listOf("techno", "rave", "electro", "acid"), 125, 135,
            "1000100010001000", "0000000000001000", "1111111111111111", 0.0,
            listOf("1000000010000000", "1000100010001000"), listOf(0, 8), 1.1),
        MusicStyle("Reggae", listOf("reggae", "dub", "ska", "roots"), 70, 80,
            "0000000010000000", "0000000010000000", "1010101010101010", 0.08,
            listOf("0010001000100010", "0010000000100000"), listOf(0, 6, 10)),
        MusicStyle("Reggaeton", listOf("reggaeton", "latino", "dembow", "latina"), 90, 100,
            "1000100010001000", "0010010000100100", "1010101010101010", 0.0,
            listOf("1001001000100100", "1000100100010010"), listOf(0, 8)),
        MusicStyle("Funk", listOf("funk", "groove", "disco", "soul"), 95, 112,
            "1000001001000000", "0000100000001001", "1111111111111111", 0.08,
            listOf("1010010010100100", "1101001010010010"), listOf(0, 6, 8)),
        MusicStyle("Drum'n'bass", listOf("dnb", "jungle", "drum'n'bass", "breakbeat"), 165, 178,
            "1000000000100000", "0000100000001000", "1010101010101010", 0.0,
            listOf("1000100010001000", "1010001000101000"), listOf(0, 8), 1.1),
        MusicStyle("Lo-fi", listOf("lofi", "lo-fi", "chill", "calme", "detente", "détente", "etude", "étude"), 75, 88,
            "1000000010010000", "0000100000001000", "1010101010101010", 0.16,
            listOf("1000001000001000", "1000000010000010"), listOf(0, 8), 0.85),
        MusicStyle("Afrobeat", listOf("afro", "afrobeat", "afrobeats", "amapiano"), 100, 115,
            "1000100000101000", "0010000100100001", "1010101010101010", 0.05,
            listOf("1001001010010010", "0010100100101000"), listOf(0, 8)),
        // ===== LES MARGES 🤘 =====
        MusicStyle("Breakcore", listOf("breakcore", "break", "amen", "idm"), 190, 220,
            "1001001010010010", "0100100101001001", "1111111111111111", 0.0,
            listOf("1011011010110110", "1111011011110110"), listOf(0, 6, 10), 1.25),
        MusicStyle("Hardtek", listOf("hardtek", "hardtech", "tribe", "teuf", "freeparty", "tekno"), 175, 190,
            "1000100010001000", "0000000000001000", "0010001000100010", 0.0,
            listOf("0010001000100010", "0011001100110011"), listOf(2, 6, 10, 14), 1.3),
        MusicStyle("Punk", listOf("punk", "hardcore", "dbeat", "d-beat", "crust"), 160, 190,
            "1010001010100010", "0000100000001000", "1010101010101010", 0.0,
            listOf("1010101010101010", "1110101011101010"), listOf(0, 8), 1.35),
        MusicStyle("Oi!", listOf("oi", "streetpunk", "skinhead"), 115, 135,
            "1000001010000000", "0000100000001000", "1010101010101010", 0.0,
            listOf("1000100010001000", "1010100010101000"), listOf(0, 8), 1.2),
        MusicStyle("Balkan", listOf("balkan", "fanfare", "tzigane", "gipsy", "cocek", "čoček", "klezmer"), 105, 130,
            "100010001000100000", "000000000000001000", "101010101010101010", 0.0,
            listOf("001000100010001010", "011001100110011010"), listOf(0, 12), 1.1, 18),
        MusicStyle("Classique", listOf("classique", "classical", "orchestre", "symphonie", "piano"), 72, 100,
            "0000000000000000", "0000000000000000", "0000000000000000", 0.0,
            listOf("1000100010001000", "1010101010101010"), listOf(0, 8), 0.0),
        MusicStyle("Baroque", listOf("baroque", "bach", "clavecin", "vivaldi", "fugue"), 92, 115,
            "0000000000000000", "0000000000000000", "0000000000000000", 0.0,
            listOf("1111111111111111", "1111111111111111"), listOf(0, 4, 8, 12), 0.0)
    )

    /** Choix : genre forcé > mot-clé dans le texte > auto (selon la graine). */
    fun pick(prompt: String, forced: String, seed: Long): MusicStyle {
        if (forced != "auto") all.firstOrNull { it.name == forced }?.let { return it }
        val p = prompt.lowercase()
        all.firstOrNull { st -> st.keys.any { p.contains(it) } }?.let { return it }
        return all[((seed % all.size + all.size) % all.size).toInt()]
    }
}
