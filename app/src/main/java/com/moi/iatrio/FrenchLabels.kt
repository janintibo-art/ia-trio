package com.moi.iatrio

/**
 * Traduction française des étiquettes les plus courantes de MobileNet
 * (objets) et YAMNet (sons). Les étiquettes non listées restent en anglais.
 */
object FrenchLabels {
    private val map = mapOf(
        // Animaux
        "tabby" to "chat tigré", "tabby cat" to "chat tigré", "egyptian cat" to "chat",
        "tiger cat" to "chat tigré", "persian cat" to "chat persan", "siamese cat" to "chat siamois",
        "golden retriever" to "chien golden retriever", "labrador retriever" to "chien labrador",
        "german shepherd" to "berger allemand", "chihuahua" to "chihuahua", "pug" to "carlin",
        "beagle" to "beagle", "poodle" to "caniche", "boxer" to "boxer",
        "hamster" to "hamster", "rabbit" to "lapin", "fox" to "renard", "wolf" to "loup",
        "horse" to "cheval", "cow" to "vache", "pig" to "cochon", "sheep" to "mouton",
        "goldfish" to "poisson rouge", "parrot" to "perroquet", "eagle" to "aigle",
        "owl" to "hibou", "duck" to "canard", "goose" to "oie", "hen" to "poule", "cock" to "coq",
        "bee" to "abeille", "butterfly" to "papillon", "spider" to "araignée", "snake" to "serpent",
        // Objets du quotidien
        "coffee mug" to "tasse à café", "cup" to "tasse", "water bottle" to "bouteille d'eau",
        "beer bottle" to "bouteille de bière", "wine bottle" to "bouteille de vin",
        "plate" to "assiette", "fork" to "fourchette", "spoon" to "cuillère",
        "laptop" to "ordinateur portable", "notebook" to "ordinateur portable",
        "cellular telephone" to "téléphone", "remote control" to "télécommande",
        "keyboard" to "clavier", "computer keyboard" to "clavier", "mouse" to "souris",
        "television" to "télévision", "desk" to "bureau", "chair" to "chaise", "table" to "table",
        "couch" to "canapé", "pillow" to "coussin", "lamp" to "lampe", "table lamp" to "lampe",
        "backpack" to "sac à dos", "wallet" to "portefeuille", "umbrella" to "parapluie",
        "sunglasses" to "lunettes de soleil", "wristwatch" to "montre",
        "running shoe" to "chaussure de sport", "sandal" to "sandale", "boot" to "botte",
        "t-shirt" to "t-shirt", "jean" to "jean", "jersey" to "maillot",
        "banana" to "banane", "orange" to "orange", "lemon" to "citron", "apple" to "pomme",
        "granny smith" to "pomme verte", "pizza" to "pizza", "hotdog" to "hot-dog",
        "guitar" to "guitare", "acoustic guitar" to "guitare acoustique",
        "electric guitar" to "guitare électrique", "piano" to "piano", "grand piano" to "piano",
        "violin" to "violon", "drum" to "batterie", "trumpet" to "trompette",
        "sports car" to "voiture de sport", "convertible" to "cabriolet", "minivan" to "monospace",
        "pickup" to "pick-up", "motorcycle" to "moto", "mountain bike" to "VTT",
        "bicycle" to "vélo", "school bus" to "bus scolaire",
        // Sons (YAMNet)
        "speech" to "parole", "conversation" to "conversation", "shout" to "cri",
        "laughter" to "rire", "baby cry, infant cry" to "pleurs de bébé", "crying, sobbing" to "pleurs",
        "singing" to "chant", "whistling" to "sifflement", "clapping" to "applaudissements",
        "dog" to "chien", "bark" to "aboiement", "cat" to "chat", "meow" to "miaulement",
        "bird" to "oiseau", "bird vocalization, bird call, bird song" to "chant d'oiseau",
        "music" to "musique", "musical instrument" to "instrument de musique",
        "guitar" to "guitare", "piano" to "piano", "drum kit" to "batterie",
        "violin, fiddle" to "violon", "flute" to "flûte", "saxophone" to "saxophone",
        "rain" to "pluie", "thunderstorm" to "orage", "thunder" to "tonnerre",
        "wind" to "vent", "water" to "eau", "stream" to "ruisseau", "ocean" to "océan",
        "fire" to "feu", "car" to "voiture", "vehicle horn, car horn, honking" to "klaxon",
        "siren" to "sirène", "train" to "train", "aircraft" to "avion",
        "telephone bell ringing" to "sonnerie de téléphone", "alarm clock" to "réveil",
        "doorbell" to "sonnette", "knock" to "toc-toc", "typing" to "frappe au clavier",
        "silence" to "silence", "snoring" to "ronflement", "cough" to "toux", "sneeze" to "éternuement",
        "footsteps" to "bruits de pas", "door" to "porte", "applause" to "applaudissements"
    )

    fun translate(label: String): String {
        val key = label.lowercase().trim()
        map[key]?.let { return it }
        // essai sur la première partie ("tabby, tabby cat" -> "tabby")
        val first = key.substringBefore(",").trim()
        map[first]?.let { return it }
        return label
    }
}
