package com.example.model

data class PackItem(
  val id: String,
  val name: String,
  val personId: String,
  val isPacked: Boolean = false,
  val qty: Int = 1,
  val icon: String = "📦",
  val category: String = "Allgemein"
)

data class FamilyMember(
  val id: String,
  val name: String,
  val colorHex: Long,
  val badgeBgHex: Long
)

data class CatalogItem(
  val name: String,
  val icon: String,
  val defaultPersonId: String = "haushalt",
  val category: String
)

data class CatalogCategory(
  val id: String,
  val title: String,
  val icon: String,
  val items: List<CatalogItem>
)

object FamilyConstants {
  val MEMBERS = listOf(
    FamilyMember("diverse", "Diverse", 0xFF64748B, 0xFFE2E8F0),
    FamilyMember("be", "Be", 0xFFE11D48, 0xFFFFE4E6),
    FamilyMember("to", "To", 0xFF6366F1, 0xFFEEF2FF),
    FamilyMember("li", "Li", 0xFF0284C7, 0xFFE0F2FE),
    FamilyMember("lu", "Lu", 0xFFD97706, 0xFFFEF3C7)
  )

  fun normalizePersonId(id: String): String {
    val norm = id.lowercase().trim()
    return when (norm) {
      "diverse", "haushalt" -> "diverse"
      "be", "bettina" -> "be"
      "to", "tomi" -> "to"
      "li", "lionel" -> "li"
      "lu", "luke" -> "lu"
      else -> "diverse"
    }
  }

  fun getMember(id: String): FamilyMember {
    val normalized = normalizePersonId(id)
    return MEMBERS.find { it.id == normalized } ?: MEMBERS[0]
  }
}

object CatalogData {
  val CATEGORIES = listOf(
    CatalogCategory(
      id = "textilien",
      title = "Haushalt / Textilien & Bettwäsche",
      icon = "🛏️",
      items = listOf(
        CatalogItem("Frottetuch Hand", "🧖", "haushalt", "Textilien"),
        CatalogItem("Frottetuch Duschen", "🛁", "haushalt", "Textilien"),
        CatalogItem("Waschlappen", "🧼", "haushalt", "Textilien"),
        CatalogItem("Fusstuch Dusche", "👣", "haushalt", "Textilien"),
        CatalogItem("Fixleintücher 90", "🛏️", "haushalt", "Textilien"),
        CatalogItem("Fixleintücher 160", "🛌", "haushalt", "Textilien"),
        CatalogItem("Bettzeug Kids", "🧸", "haushalt", "Textilien"),
        CatalogItem("Bettzeug Eltern", "✨", "haushalt", "Textilien"),
        CatalogItem("Bettzeug Gäste", "🏡", "haushalt", "Textilien")
      )
    ),
    CatalogCategory(
      id = "kueche",
      title = "Haushalt / Küche & Verbrauch",
      icon = "🍳",
      items = listOf(
        CatalogItem("Küche Lumpen", "🧽", "haushalt", "Küche"),
        CatalogItem("Küche Tüechli", "🍽️", "haushalt", "Küche"),
        CatalogItem("Kleenex", "🧻", "haushalt", "Küche"),
        CatalogItem("Haushaltspapier", "📜", "haushalt", "Küche"),
        CatalogItem("Bodenmopp", "🧹", "haushalt", "Küche"),
        CatalogItem("Holz", "🪵", "haushalt", "Küche"),
        CatalogItem("Sonnencreme", "☀️", "haushalt", "Küche"),
        CatalogItem("neue Packliste", "📋", "haushalt", "Küche")
      )
    ),
    CatalogCategory(
      id = "bekleidung",
      title = "Bekleidung & Wäsche (Allgemein)",
      icon = "👕",
      items = listOf(
        CatalogItem("Pijama", "🌙", "haushalt", "Bekleidung"),
        CatalogItem("Unterhosen", "🩲", "haushalt", "Bekleidung"),
        CatalogItem("Singlet", "🎽", "haushalt", "Bekleidung"),
        CatalogItem("Socken", "🧦", "haushalt", "Bekleidung"),
        CatalogItem("Jeans", "👖", "haushalt", "Bekleidung"),
        CatalogItem("Hosen", "👖", "haushalt", "Bekleidung"),
        CatalogItem("Pullover", "🧶", "haushalt", "Bekleidung"),
        CatalogItem("T-Shirt kurz", "👕", "haushalt", "Bekleidung"),
        CatalogItem("T-shirt lang", "👔", "haushalt", "Bekleidung"),
        CatalogItem("Trainerhosen", "🏃", "haushalt", "Bekleidung"),
        CatalogItem("Hoodie", "🧥", "haushalt", "Bekleidung"),
        CatalogItem("Shorts", "🩳", "haushalt", "Bekleidung"),
        CatalogItem("Fleece", "🏔️", "haushalt", "Bekleidung")
      )
    ),
    CatalogCategory(
      id = "sport",
      title = "Sport & Outdoor (Sommer & Winter)",
      icon = "🎿",
      items = listOf(
        CatalogItem("Badehose", "🩱", "haushalt", "Sport & Outdoor"),
        CatalogItem("Bikini", "👙", "bettina", "Sport & Outdoor"),
        CatalogItem("Bikehose", "🚴", "haushalt", "Sport & Outdoor"),
        CatalogItem("Rashy", "🏄", "haushalt", "Sport & Outdoor"),
        CatalogItem("Hut", "👒", "haushalt", "Sport & Outdoor"),
        CatalogItem("Wanderhose", "🥾", "haushalt", "Sport & Outdoor"),
        CatalogItem("Wandersocken", "🧦", "haushalt", "Sport & Outdoor"),
        CatalogItem("Skisocken", "⛷️", "haushalt", "Sport & Outdoor"),
        CatalogItem("Skijacke", "🧥", "haushalt", "Sport & Outdoor"),
        CatalogItem("Skihosen", "🏂", "haushalt", "Sport & Outdoor"),
        CatalogItem("Helmstrumpf", "🪖", "haushalt", "Sport & Outdoor"),
        CatalogItem("Thermo Oberteil", "❄️", "haushalt", "Sport & Outdoor"),
        CatalogItem("Thermo Hosen", "⛄", "haushalt", "Sport & Outdoor"),
        CatalogItem("Kappe", "🧢", "haushalt", "Sport & Outdoor"),
        CatalogItem("Merino kurz", "🐑", "haushalt", "Sport & Outdoor"),
        CatalogItem("Merino lang", "🐏", "haushalt", "Sport & Outdoor"),
        CatalogItem("Lange Unterhosen", "🩲", "haushalt", "Sport & Outdoor"),
        CatalogItem("BH", "👚", "bettina", "Sport & Outdoor")
      )
    ),
    CatalogCategory(
      id = "retour",
      title = "Feste Retour-Artikel (für \"Nach Hause\")",
      icon = "🚗",
      items = listOf(
        CatalogItem("Kompost", "🌱", "haushalt", "Retour"),
        CatalogItem("Brille", "👓", "tomi", "Retour"),
        CatalogItem("Kuscheltierli", "🧸", "luke", "Retour"),
        CatalogItem("Medis", "💊", "haushalt", "Retour"),
        CatalogItem("Wanderschuhe", "🥾", "haushalt", "Retour"),
        CatalogItem("Windjacken", "🧥", "haushalt", "Retour"),
        CatalogItem("Keens", "👟", "haushalt", "Retour"),
        CatalogItem("Sonnencreme", "☀️", "haushalt", "Retour"),
        CatalogItem("Winterschuhe", "🥾", "haushalt", "Retour"),
        CatalogItem("Schlittschuhe", "⛸️", "haushalt", "Retour"),
        CatalogItem("Spange Lionel", "🦷", "lionel", "Retour")
      )
    )
  )

  fun findIcon(name: String): String {
    val lower = name.lowercase().trim()
    for (cat in CATEGORIES) {
      for (item in cat.items) {
        if (item.name.equals(name, ignoreCase = true)) return item.icon
      }
    }
    return when {
      lower.contains("ski") || lower.contains("schnee") -> "⛷️"
      lower.contains("hose") || lower.contains("jeans") -> "👖"
      lower.contains("schuh") || lower.contains("keen") -> "🥾"
      lower.contains("bad") || lower.contains("bikini") -> "🩱"
      lower.contains("brille") -> "👓"
      lower.contains("bett") || lower.contains("kissen") -> "🛏️"
      lower.contains("socke") -> "🧦"
      lower.contains("bike") || lower.contains("velo") -> "🚴"
      lower.contains("jacke") || lower.contains("fleece") -> "🧥"
      lower.contains("apfel") || lower.contains("brot") -> "🍎"
      else -> "📦"
    }
  }
}
