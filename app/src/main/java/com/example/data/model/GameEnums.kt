package com.example.data.model

import com.squareup.moshi.JsonClass

enum class AppLanguage(val code: String, val displayLabel: String) {
  TH("th", "ไทย"),
  EN("en", "English"),
}

enum class Lane(val labelEn: String, val labelTh: String) {
  BARON("Baron Lane", "เลนบารอน"),
  JUNGLE("Jungle", "ป่า (Jungle)"),
  MID("Mid Lane", "เลนกลาง"),
  DRAGON("Dragon Lane", "เลนมังกร (ADC)"),
  SUPPORT("Support", "ซัพพอร์ต"),
}

enum class ChampionRole(val labelEn: String, val labelTh: String) {
  FIGHTER("Fighter", "ไฟต์เตอร์"),
  ASSASSIN("Assassin", "แอสซาซิน"),
  MAGE("Mage", "เมจ"),
  MARKSMAN("Marksman", "แครี่"),
  TANK("Tank", "แทงค์"),
  SUPPORT("Support", "ซัพพอร์ต"),
}

enum class DamageType(val labelEn: String, val labelTh: String) {
  AD("Physical (AD)", "กายภาพ (AD)"),
  AP("Magic (AP)", "เวท (AP)"),
  HYBRID("Hybrid", "ผสม (AD/AP)"),
  TRUE("True Damage", "ทรูดาเมจ"),
}

enum class TierRank(val label: String, val sortOrder: Int) {
  S_PLUS("S+", 0),
  S("S", 1),
  A("A", 2),
  B("B", 3),
}

enum class ItemCategory(val labelEn: String, val labelTh: String) {
  STARTER("Starter", "ไอเทมเริ่มต้น"),
  CORE("Core", "ไอเทมหลัก"),
  BOOTS("Boots", "รองเท้า"),
  ENCHANTMENT("Active Item", "ไอเทมกดใช้ (Active)"),
  DEFENSIVE("Defensive", "ป้องกัน"),
  OFFENSIVE("Offensive", "โจมตี / เจาะเกราะ"),
}

enum class ThreatTag(val labelEn: String, val labelTh: String) {
  HIGH_AP_BURST("High AP Burst", "เบิร์สเวทแรง"),
  HIGH_AD_DPS("High AD DPS", "ดาเมจกายภาพต่อเนื่อง"),
  HEAVY_HEALING("Heavy Healing", "ฮีล / ดูดเลือดหนัก"),
  HIGH_HP_TANK("High-HP Tank", "แทงค์เลือดและเกราะหนา"),
  HIGH_ATTACK_SPEED("High Attack Speed", "โจมตีไวต่อเนื่อง"),
  HEAVY_CC("Heavy CC", "CC ล็อกหนัก"),
  CRIT_HYPERCARRY("Crit Hypercarry", "แครี่คริท้ายเกม"),
  ASSASSIN_DIVE("Assassin Dive", "ตัวพุ่งล้วงแนวหลัง"),
  POKE_SIEGE("Long-Range Poke", "ตอดระยะไกล"),
}

enum class ItemCounterTag {
  ANTI_AP_BURST,
  ANTI_AD_ARMOR,
  ANTI_HEAL,
  ANTI_TANK_PCT_HP,
  ARMOR_PEN,
  MAGIC_PEN,
  ANTI_ATTACK_SPEED,
  ANTI_CRIT,
  ANTI_CC_QSS,
  STASIS_SURVIVAL,
  TENACITY,
  BURST_SHIELD,
  ENGAGE_MOBILITY,
}

enum class DataSourceType(val labelEn: String, val labelTh: String) {
  OFFICIAL_PATCH_NOTES("Official Patch Notes", "อ้างอิงแพตช์โน้ตทางการ"),
  CURATED_META_ESTIMATE("Curated Tactical Estimate", "การวิเคราะห์แทคติคเชิงระบบ"),
  CUSTOM_USER_PACK("Imported Patch Pack", "ข้อมูลจากแพ็กแพตช์ที่นำเข้า"),
}

enum class LaneDifficulty(val labelEn: String, val labelTh: String) {
  EASY("Favorable", "ได้เปรียบ"),
  EVEN("Even", "สูสี"),
  SKILL("Skill Matchup", "วัดฝีมือ"),
  HARD("Disadvantage", "เสียเปรียบ"),
}

enum class PatchChangeType(val labelEn: String, val labelTh: String) {
  NEW("NEW", "มาใหม่"),
  BUFF("BUFF", "บัฟ"),
  NERF("NERF", "เนิร์ฟ"),
  ADJUST("ADJUST", "ปรับสมดุล"),
}

@JsonClass(generateAdapter = true)
data class TextScalePreset(
  val id: String,
  val multiplier: Float,
  val labelEn: String,
  val labelTh: String,
) {
  companion object {
    val PRESETS =
      listOf(
        TextScalePreset("compact", 0.92f, "Compact", "กะทัดรัด"),
        TextScalePreset("standard", 1.0f, "Standard", "มาตรฐาน"),
        TextScalePreset("large", 1.12f, "Large", "ใหญ่ชัดเจน"),
        TextScalePreset("xlarge", 1.24f, "Extra Large", "ใหญ่พิเศษ"),
      )
  }
}

enum class AppDarkThemePreset(
  val id: String,
  val labelEn: String,
  val labelTh: String,
  val shortLabelEn: String,
  val shortLabelTh: String,
) {
  HEXTECH_OBSIDIAN(
    id = "hextech_obsidian",
    labelEn = "Hextech Obsidian",
    labelTh = "Hextech น้ำเงินทอง",
    shortLabelEn = "Hextech",
    shortLabelTh = "Hextech",
  ),
  VOID_ABYSS(
    id = "void_abyss",
    labelEn = "Void Abyss",
    labelTh = "Void ม่วงอวกาศ",
    shortLabelEn = "Void",
    shortLabelTh = "Void",
  ),
  SHADOW_EMERALD(
    id = "shadow_emerald",
    labelEn = "Shadow Emerald",
    labelTh = "Shadow เขียวมรกต",
    shortLabelEn = "Emerald",
    shortLabelTh = "มรกต",
  ),
  TACTICAL_CARBON(
    id = "tactical_carbon",
    labelEn = "Tactical Carbon",
    labelTh = "Carbon กราไฟต์คม",
    shortLabelEn = "Carbon",
    shortLabelTh = "คาร์บอน",
  );

  fun displayLabel(lang: AppLanguage): String = if (lang == AppLanguage.TH) labelTh else labelEn

  fun shortLabel(lang: AppLanguage): String =
    if (lang == AppLanguage.TH) shortLabelTh else shortLabelEn

  fun next(): AppDarkThemePreset = entries[(ordinal + 1) % entries.size]

  companion object {
    fun fromId(id: String?): AppDarkThemePreset =
      entries.firstOrNull { it.id == id } ?: HEXTECH_OBSIDIAN
  }
}

enum class AppFontPreset(
  val id: String,
  val labelEn: String,
  val labelTh: String,
) {
  NOTO_SANS_THAI(
    id = "noto_sans_thai",
    labelEn = "Noto Sans Thai",
    labelTh = "Noto Sans Thai",
  ),
  KANIT(
    id = "kanit",
    labelEn = "Kanit",
    labelTh = "Kanit (คมชัดทันสมัย)",
  ),
  SARABUN(
    id = "sarabun",
    labelEn = "Sarabun",
    labelTh = "Sarabun (อ่านสบายตา)",
  ),
  SYSTEM(
    id = "system",
    labelEn = "System Default",
    labelTh = "ฟอนต์ระบบ",
  );

  fun displayLabel(lang: AppLanguage): String = if (lang == AppLanguage.TH) labelTh else labelEn

  companion object {
    fun fromId(id: String?): AppFontPreset =
      entries.firstOrNull { it.id == id } ?: NOTO_SANS_THAI
  }
}

enum class IconSizePreset(
  val id: String,
  val scale: Float,
  val labelEn: String,
  val labelTh: String,
) {
  COMPACT(
    id = "compact",
    scale = 0.78f,
    labelEn = "Compact (Recommended)",
    labelTh = "กะทัดรัด (แนะนำ)",
  ),
  STANDARD(
    id = "standard",
    scale = 1.0f,
    labelEn = "Standard",
    labelTh = "ปกติ",
  );

  fun displayLabel(lang: AppLanguage): String = if (lang == AppLanguage.TH) labelTh else labelEn

  companion object {
    fun fromId(id: String?): IconSizePreset =
      entries.firstOrNull { it.id == id } ?: COMPACT
  }
}

