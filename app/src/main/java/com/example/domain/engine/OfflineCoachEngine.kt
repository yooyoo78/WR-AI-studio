package com.example.domain.engine

import com.example.data.model.AppLanguage
import com.example.data.model.ChampionData
import com.example.data.model.ChampionRole
import com.example.data.model.Lane

data class CoachPlaybookSection(
  val titleEn: String,
  val titleTh: String,
  val bodyEn: String,
  val bodyTh: String,
) {
  fun title(lang: AppLanguage) = if (lang == AppLanguage.TH) titleTh else titleEn

  fun body(lang: AppLanguage) = if (lang == AppLanguage.TH) bodyTh else bodyEn
}

class OfflineCoachEngine {
  fun buildPlaybook(
    champion: ChampionData,
    lane: Lane,
    enemies: List<ChampionData> = emptyList(),
  ): List<CoachPlaybookSection> {
    val waveTipEn =
      when (lane) {
        Lane.BARON ->
          "Wave Management: Pull wave toward your tower against stronger lvl 1–3 duelists; slow-push 2 waves before Herald spawns at 5:00 to gain roam priority."
        Lane.MID ->
          "Wave Management: Shove cannon waves quickly before rotating to Dragon/Scuttle; hold freeze only when enemy jungler lacks dive threat."
        Lane.DRAGON ->
          "Wave Management: Hit level 2 off the first 2 waves (first wave + 1 melee minion) to establish brush control and early poke priority."
        Lane.SUPPORT ->
          "Lane & Vision: Secure lane brush control early, roam mid on cannon resets, and drop wards 45s before Dragon spawns."
        Lane.JUNGLE ->
          "Pathing & Tempo: Plan your first clear toward the lane with the best CC setup; contest Scuttle at 1:25 only with lane priority."
      }
    val waveTipTh =
      when (lane) {
        Lane.BARON ->
          "การคุมเวฟ: ดึงเวฟค้างหน้าป้อมเมื่อเจอตัวดวลต้นเกมแรง และดันเวฟใหญ่ 2 ชุดก่อน Herald เกิดนาที 5:00 เพื่อชิงจังหวะเดินเกม"
        Lane.MID ->
          "การคุมเวฟ: เคลียร์เวฟรถถังให้ไวก่อนเดินช่วยป่าชิงปูหรือมังกร คุมเลนกลางให้ฝั่งตรงข้ามขยับตามไม่ทัน"
        Lane.DRAGON ->
          "การคุมเวฟ: แย่งเลเวล 2 จากเวฟแรก + ครีปประชิด 1 ตัวของเวฟสอง เพื่อคุมพุ่มและเปิดจังหวะกดดันก่อน"
        Lane.SUPPORT ->
          "คุมเลนและวิชั่น: ยึดพุ่มข้างให้ได้ เดินแวะเลนกลางจังหวะแครี่รีคอล และปักวาร์ดรอบมังกรล่วงหน้า 45 วินาที"
        Lane.JUNGLE ->
          "เส้นทางฟาร์ม: เลือกฝั่งเคลียร์ป่าเข้าหาเลนที่มี CC ช่วยแก๊ง และชิงปูแม่น้ำนาที 1:25 เฉพาะตอนเลนข้างดันนำ"
      }

    val positioningEn =
      when {
        ChampionRole.MARKSMAN in champion.roles || ChampionRole.MAGE in champion.roles ->
          "Teamfight Positioning: Attack the closest safe target behind your frontline; save Flash/mobility purely for enemy dive ultimates."
        ChampionRole.ASSASSIN in champion.roles ->
          "Teamfight Positioning: Sweep flanks from fog of war; wait until enemy hard CC & Stasis are spent before committing."
        ChampionRole.TANK in champion.roles ->
          "Teamfight Positioning: Decide whether to dive enemy carries or peel for your own fed ADC based on who is stronger."
        else ->
          "Teamfight Positioning: Hover side angles or skirmish alongside your engage tank; track enemy Grievous Wounds before diving deep."
      }
    val positioningTh =
      when {
        ChampionRole.MARKSMAN in champion.roles || ChampionRole.MAGE in champion.roles ->
          "ตำแหน่งในทีมไฟต์: ยืนหลังแทงค์แล้วยิงตัวที่ใกล้และปลอดภัยที่สุด เก็บ Flash/สกิลหนีไว้หลบอัลติตัวล้วงเท่านั้น"
        ChampionRole.ASSASSIN in champion.roles ->
          "ตำแหน่งในทีมไฟต์: ซุ่มอ้อมข้างในจุดอับสายตา รอให้ศัตรูใช้สกิล CC และ Stasis ไปก่อนค่อยพุ่งปิดตัวบาง"
        ChampionRole.TANK in champion.roles ->
          "ตำแหน่งในทีมไฟต์: ประเมินว่าทีมต้องการ 'เปิดวง' หรือ 'ยืนกันแครี่' หากแครี่ทีมเราเกิดให้เก็บ CC กันตัวล้วงเป็นหลัก"
        else ->
          "ตำแหน่งในทีมไฟต์: เล่นมุมข้างหรือลุยพร้อมตัวเปิดไฟต์ ระวังระยะสกิลตัดฮีลและอย่าหลุดระยะซัพพอร์ต"
      }

    val matchupNoteEn =
      if (enemies.isEmpty()) {
        "Strengths: ${champion.strengthsEn.joinToString(" • ")}"
      } else {
        "Active Matchup Focus vs ${enemies.joinToString(", ") { it.nameEn }}: Track key CC and burst windows before overextending."
      }
    val matchupNoteTh =
      if (enemies.isEmpty()) {
        "จุดแข็งหลัก: ${champion.strengthsTh.joinToString(" • ")}"
      } else {
        "โฟกัสการเจอ ${enemies.joinToString(", ") { it.nameTh }}: นับคูลดาวน์สกิล CC และเบิร์สของศัตรูก่อนดันสูงหรือเปิดไฟต์"
      }

    val laneLoadout = champion.resolveForLane(lane)
    val laneSetupEn =
      "${laneLoadout.conditionNoteEn} (Skill Order: ${laneLoadout.skillOrder} • Spells: ${laneLoadout.spellIds.joinToString(" + ")} • Core: ${laneLoadout.coreItemIds.take(3).joinToString(" -> ")})"
    val laneSetupTh =
      "${laneLoadout.conditionNoteTh} (อัปสกิล ${laneLoadout.skillOrder} • Spells: ${laneLoadout.spellIds.joinToString(" + ")})"

    return listOf(
      CoachPlaybookSection(
        titleEn = "Early Game & Laning",
        titleTh = "แผนการเล่นช่วงต้นเกม (Early)",
        bodyEn = "${champion.earlyPlanEn}\n\n$laneSetupEn\n\n$waveTipEn",
        bodyTh = "${champion.earlyPlanTh}\n\n$laneSetupTh\n\n$waveTipTh",
      ),
      CoachPlaybookSection(
        titleEn = "Mid Game & Objectives",
        titleTh = "แผนกลางเกมและคุมอ็อบเจกต์ (Mid)",
        bodyEn = "${champion.midPlanEn}\n\n$matchupNoteEn",
        bodyTh = "${champion.midPlanTh}\n\n$matchupNoteTh",
      ),
      CoachPlaybookSection(
        titleEn = "Late Game & Teamfights",
        titleTh = "แผนท้ายเกมและทีมไฟต์ (Late)",
        bodyEn = "${champion.latePlanEn}\n\n$positioningEn",
        bodyTh = "${champion.latePlanTh}\n\n$positioningTh",
      ),
    )
  }
}
