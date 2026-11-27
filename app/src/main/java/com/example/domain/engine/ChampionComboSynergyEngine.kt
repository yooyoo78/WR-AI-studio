package com.example.domain.engine

import com.example.data.model.ChampionData
import com.example.data.model.ChampionRole
import com.example.data.model.DamageType
import com.example.data.model.Lane
import kotlin.math.roundToInt

enum class ComboDifficulty(val labelEn: String, val labelTh: String) {
  BASIC("Basic", "Basic • พื้นฐาน"),
  MEDIUM("Medium", "Medium • ระดับกลาง"),
  HARD("Hard", "Hard • ขั้นสูง"),
}

enum class ComboSituation(val labelEn: String, val labelTh: String) {
  WAVE_CLEAR("Wave Clear", "เคลียร์เวฟ"),
  LANE_TRADE("Lane Trade", "เทรดเลน"),
  SOLO_KILL("Solo Kill", "โซโล่คิล"),
  TEAMFIGHT("Teamfight", "ทีมไฟต์"),
  ESCAPE_SURVIVAL("Escape / Survival", "เอาตัวรอด / หนีตาย"),
}

data class ChampionComboEntry(
  val id: String,
  val difficulty: ComboDifficulty,
  val situation: ComboSituation,
  val titleEn: String,
  val titleTh: String,
  val sequence: List<String>,
  val summaryEn: String,
  val summaryTh: String,
  val timingTipEn: String,
  val timingTipTh: String,
)

data class TeamSynergyPartner(
  val champion: ChampionData,
  val synergyScore: Int,
  val tagEn: String,
  val tagTh: String,
  val comboSequenceLabel: String,
  val reasonEn: String,
  val reasonTh: String,
)

object ChampionComboSynergyEngine {

  private fun combo(
    id: String,
    diff: ComboDifficulty,
    sit: ComboSituation,
    titleEn: String,
    titleTh: String,
    seq: List<String>,
    summaryEn: String,
    summaryTh: String,
    tipEn: String,
    tipTh: String,
  ) =
    ChampionComboEntry(
      id = id,
      difficulty = diff,
      situation = sit,
      titleEn = titleEn,
      titleTh = titleTh,
      sequence = seq,
      summaryEn = summaryEn,
      summaryTh = summaryTh,
      timingTipEn = tipEn,
      timingTipTh = tipTh,
    )

  private val CURATED_COMBOS: Map<String, List<ChampionComboEntry>> =
    mapOf(
      "ahri" to
        listOf(
          combo(
            "ahri_b_wc",
            ComboDifficulty.BASIC,
            ComboSituation.WAVE_CLEAR,
            "Fast Caster Clear",
            "เคลียร์เวฟเร็ว",
            listOf("Q", "W"),
            "Aim Q through both melee and caster minions so the return True Damage finishes casters, then tap W to clean up.",
            "ปา Q ทะลุมิเนียนแถวหน้า-หลังให้ขากลับโดนทรูดาเมจเต็มแถว แล้วกด W เก็บตกมิเนียนใกล้ตาย",
            "Wait until enemy mid steps beside the wave so Q pokes and clears together.",
            "รอจังหวะมิดฝั่งตรงข้ามยืนข้างเวฟ จะได้ทั้งเคลียร์และตอดเลือดพร้อมกัน",
          ),
          combo(
            "ahri_b_trade",
            ComboDifficulty.BASIC,
            ComboSituation.LANE_TRADE,
            "Electrocute Charm Trade",
            "เทรดล็อก Charm + Electrocute",
            listOf("E", "Q", "W", "AA"),
            "Land Charm first to guarantee both hits of Orb of Deception, then pop W and weave an Auto Attack.",
            "เปิด E ล็อกให้ติดก่อนเพื่อการันตี Q โดนทั้งไป-กลับ ตามด้วย W และตบธรรมดาปิด Electrocute",
            "Step forward after Q cast so the return orb hits straight back through the target.",
            "ขยับมุมตามศัตรูเล็กน้อยหลังปา Q เพื่อให้ลูกแก้วขากลับลากโดนแม่นยำขึ้น",
          ),
          combo(
            "ahri_m_solo",
            ComboDifficulty.MEDIUM,
            ComboSituation.SOLO_KILL,
            "Triple-Dash Assassin All-In",
            "สปีริตรัชปิดคิลเดี่ยว",
            listOf("R", "E", "Q", "W", "AA", "R"),
            "Dash diagonally with R1 to bypass minions, fire E point-blank into Q-W, then reposition with R2 to secure return Q.",
            "ใช้ R1 พุ่งเฉียงหลบมิเนียนแล้วยัด E ระยะประชิด ต่อด้วย Q-W-AA แล้วใช้ R2 ปรับมุมดึง Q ขากลับเข้าเป้า",
            "Hold your 3rd R charge for dodging key skillshots or chasing after Flash.",
            "เก็บชาร์จ R สุดท้ายไว้หลบสกิลสวนหรือตาม Flash ศัตรู อย่ารีบกดหมดรวดเดียว",
          ),
          combo(
            "ahri_m_escape",
            ComboDifficulty.MEDIUM,
            ComboSituation.ESCAPE_SURVIVAL,
            "Kite & Wall Hop Escape",
            "Charm สกัด + พุ่งข้ามกำแพงหนี",
            listOf("E", "W", "R", "R"),
            "Throw E backward into the chaser's dash path, activate W for burst speed, and R over thick walls.",
            "ปา E สวนใส่นักล้วงที่กำลังพุ่งเข้ามา กด W เอาความเร็ววิ่ง แล้วใช้ R พุ่งข้ามกำแพงหนี",
            "Hold E until the diver commits their dash; W movespeed alone often baits cooldowns.",
            "รอตัวล้วงใช้สกิลพุ่งเข้ามาก่อนค่อยสวน E จะโดนชัวร์ 100% ไม่เสียฟรี",
          ),
          combo(
            "ahri_h_tf",
            ComboDifficulty.HARD,
            ComboSituation.TEAMFIGHT,
            "E-Flash Catch & Reset",
            "Charm-Flash จับแครี่ในทีมไฟต์",
            listOf("E", "Flash", "Q", "W", "R", "R"),
            "Buffer E then immediately Flash to erase reaction time, burst the carry, and chain R resets on takedown.",
            "กด E แล้วต่อ Flash ทันทีเพื่อตัดจังหวะหลบของแครี่ ยัด Q-W ปิดแล้วใช้ R รีเซ็ตพุ่งคุมไฟต์ต่อ",
            "Practice E -> Flash direction in Practice Tool; Flash changes cast origin instantaneously.",
            "กด E ก่อนแล้วค่อย Flash ภายในเสี้ยววินาที หัวสกิลจะออกทันทีจากตำแหน่งปลายทางโดยไร้แอนิเมชันเตือน",
          ),
        ),
      "yasuo" to
        listOf(
          combo(
            "yas_b_wc",
            ComboDifficulty.BASIC,
            ComboSituation.WAVE_CLEAR,
            "Stack & Sweep Wave",
            "สแต็กพายุเคลียร์เวฟ",
            listOf("Q", "E", "Q", "Q3"),
            "Poke melee creeps with Q, dash through casters with E-Q for AoE circular slash, then throw Tornado across the line.",
            "แทง Q ใส่มิเนียนแถวหน้าแล้วใช้ E-Q หมุนรอบตัวใส่แถวหลัง ก่อนปล่อยพายุ Q3 กวาดทั้งเวฟ",
            "Leave one front minion un-dashed so you keep an escape dash back toward tower.",
            "เว้นมิเนียนหน้าไว้ 1 ตัวเสมอเผื่อใช้ E ถอยกลับหลังโดนป่ามาแก๊ง",
          ),
          combo(
            "yas_b_trade",
            ComboDifficulty.BASIC,
            ComboSituation.LANE_TRADE,
            "In-and-Out Minion Trade",
            "พุ่งเข้าตีแล้ว E กลับหนี",
            listOf("E", "Q", "AA", "E"),
            "Dash onto a forward minion to gap-close, Q-AA your opponent, then E through a back minion before they trade back.",
            "ใช้ E ใส่มิเนียนหน้าเข้าไปตี Q+AA แล้ว E มิเนียนหลังถอยออกก่อนอีกฝั่งได้สวน",
            "Trigger your Passive Flow Shield before dashing in so the trade is nearly free.",
            "รอเกจโล่ Passive เต็มก่อนพุ่งเข้าเทรด จะกินเลือดฟรีแบบอีกฝั่งแลกไม่คุ้ม",
          ),
          combo(
            "yas_m_solo",
            ComboDifficulty.MEDIUM,
            ComboSituation.SOLO_KILL,
            "Knockup Air-Slash All-In",
            "งัดลอยคิลพร้อมบัฟเจาะเกราะ",
            listOf("Q3", "AA", "R", "E", "Q", "AA"),
            "Land Q3 Tornado, sneak 1 Auto Attack during knockup hangtime before pressing R, then follow up with E-Q.",
            "เป่า Q3 ให้ลอยแล้วตีธรรมดา 1 ทีกลางอากาศก่อนกด R ลงมาฟันต่อด้วย E-Q",
            "Slipping an AA before R maximizes DPS and resets turret-aggro timing cleanly.",
            "จังหวะงัดลอยจะมีเวลาตีธรรมดาฟรี 1 ทีเสมอ ก่อนกด R เพื่อเร่งดาเมจปิดคิล",
          ),
          combo(
            "yas_m_escape",
            ComboDifficulty.MEDIUM,
            ComboSituation.ESCAPE_SURVIVAL,
            "Windwall & Jungle-Camp Dash",
            "กางม่านลม + พุ่งข้ามแคมป์ป่าหนี",
            listOf("W", "Q", "E"),
            "Drop Windwall against ranged CC, peek Krugs/Raptors/Gromp for vision, and dash across the wall with E.",
            "กาง W บังโปรเจกไทล์ CC สำคัญ แล้วเดินชิดกำแพงแคมป์ป่าหรือมิเนียนใช้ E พุ่งข้ามกำแพงหนี",
            "Use Raptors or Krugs by hugging the camp lip before pressing E to cleanly cross the wall.",
            "เดินชิดตัวแคมป์ป่าระยะประชิดที่สุดก่อนกด E จะทะลุกำแพงหนาได้เนียนไม่ติดมุม",
          ),
          combo(
            "yas_h_tf",
            ComboDifficulty.HARD,
            ComboSituation.TEAMFIGHT,
            "Beyblade & Airblade Combo",
            "Keyblade / Airblade เบิร์สทีมไฟต์",
            listOf("E", "Q3", "Flash", "AA", "E", "Q", "R"),
            "Cast E-Q3 and Flash onto the carry mid-spin (Beyblade), then quickly E-Q a nearby unit before pressing R (Airblade) for an extra Q stack.",
            "กด E-Q3 แล้ว Flash ทับแครี่กลางวงหมุน (Beyblade) ตามด้วยรีบกด E-Q ใส่เป้าหมายใกล้ตัวแล้ว R ทันที (Airblade)",
            "Needs ~1.3s Q cooldown; Airblade starts your next Tornado stack instantly after landing.",
            "ต้องมีคูลดาวน์ Q สั้นพอ หลังกด R ลงพื้นจะมีสแต็ก Q ติดตัวพร้อมงัดต่อทันทีในไฟต์",
          ),
        ),
      "yone" to
        listOf(
          combo(
            "yone_b_wc",
            ComboDifficulty.BASIC,
            ComboSituation.WAVE_CLEAR,
            "Wave Slash & Shield",
            "สับเวฟพร้อมฟันเกราะ",
            listOf("Q", "W", "Q", "AA"),
            "Line up Q thrusts across minions and sweep Spirit Cleave (W) for AoE waveclear + shield.",
            "แทง Q เรียงมิเนียนแล้วฟันกวาด W ทั้งเวฟเพื่อเอาโล่และเคลียร์ไว",
            "Cast W across both the wave and enemy laner for maximum shield value.",
            "ฟัน W ให้โดนทั้งมิเนียนและฮีโร่ฝั่งตรงข้าม จะได้โล่หนาขึ้นอีกเท่าตัว",
          ),
          combo(
            "yone_b_trade",
            ComboDifficulty.BASIC,
            ComboSituation.LANE_TRADE,
            "Zero-Risk E Snapback Trade",
            "ถอดวิญญาณเทรดฟรี",
            listOf("Q", "Q", "E", "Q3", "W", "AA", "E2"),
            "Pre-stack Q3 on minions, dash in with E-Q3 + W + AA, then snap back with E2 before enemy CC lands.",
            "เก็บ Q3 บนมิเนียนให้ครบ กด E พุ่งงัด Q3 + W + ตีธรรมดา แล้วดีด E2 กลับร่างทันที",
            "Save E2 snapback to cancel incoming CC or burst from tower shots.",
            "กด E2 ดีดกลับก่อนสกิลสวนจะถึงตัว เพื่อทิ้งรอยดาเมจจริงไว้ปิดท้ายฟรีๆ",
          ),
          combo(
            "yone_m_solo",
            ComboDifficulty.MEDIUM,
            ComboSituation.SOLO_KILL,
            "Chain Knockup Execution",
            "ล็อกคอมโบงัด 2 ต่อปิดคิล",
            listOf("E", "Q3", "AA", "R", "AA", "W", "Q", "E2"),
            "Knock up with Q3 so Ult cannot be dodged, weave AAs + W + Q, then let E mark pop the execution.",
            "เข้าด้วย Q3 ให้ลอยก่อนกด R (หลบไม่ได้) สลับตบ W-Q เก็บดาเมจ แล้วดีด E2 ระเบิดทรูดาเมจปิด",
            "Weaving 1 AA between Q casts maximizes Lethal Tempo + E bonus true damage.",
            "แทรกตีธรรมดาระหว่าง Q ทุกจังหวะ เพื่อเร่งดาเมจรวมที่ E จะคูณทบตอนดีดกลับ",
          ),
          combo(
            "yone_m_escape",
            ComboDifficulty.MEDIUM,
            ComboSituation.ESCAPE_SURVIVAL,
            "Unstoppable Ult Wall Escape",
            "พุ่ง R หนี / E ล่อสกิล",
            listOf("E", "W", "Q3", "R"),
            "Note E2 ALWAYS pulls back to body; use Q3 or Unstoppable R away from gankers over terrain instead.",
            "จำไว้ว่า E2 ดึงกลับจุดเดิมเสมอ ถ้าโดนแก๊งให้ใช้ Q3 หรือกด R (กัน CC ระหว่างร่าย) พุ่งข้ามกำแพงหนีแทน",
            "Press R cleanly away when outnumbered — Fate Sealed makes you Unstoppable during the windup.",
            "ท่า R กัน CC ช่วงร่าย กดสวนตอนศัตรูปล่อยสตั๊นจะพุ่งหนีข้ามกำแพงได้ชัวร์",
          ),
          combo(
            "yone_h_tf",
            ComboDifficulty.HARD,
            ComboSituation.TEAMFIGHT,
            "Backline E-Flash-R Wombo",
            "ทะลุแนวหลัง E-Flash-R รวบทีม",
            listOf("E", "Flash", "R", "W", "Q", "AA", "Q3", "E2"),
            "Snap from fog with E -> Flash -> R across the entire enemy backline, cleave multi-target W shield, then snap safe with E2.",
            "เปิดจากมุมอับด้วย E -> Flash -> R รวบแครี่-เมจหลายคน ฟัน W เอาโล่ก้อนใหญ่ ไล่เก็บแล้วดีด E2 ถอยปลอดภัย",
            "Check allied follow-up distance so you don't snap back before your team arrives.",
            "ดูระยะเพื่อนร่วมทีมให้พร้อมตาม เพราะหลังดีด E2 เราจะถอยออกจากแนวหน้าอัตโนมัติ",
          ),
        ),
      "zed" to
        listOf(
          combo(
            "zed_b_wc",
            ComboDifficulty.BASIC,
            ComboSituation.WAVE_CLEAR,
            "Shadow Shuriken Clear",
            "ปาดาวกระจายคู่เคลียร์เวฟ",
            listOf("W", "E", "Q"),
            "Drop Shadow alongside the wave, E to slow/slice all 4 minions, and throw double Q down the line.",
            "วางเงา W ข้างเวฟ กด E หมุนเก็บแล้วปา Q คู่ลากผ่านทั้งแถว",
            "Only spend W to clear when safe from ganks or right before roaming.",
            "ระวังอย่าใช้ W เคลียร์เวฟถ้าไม่เห็นตัวป่าฝั่งตรงข้าม เพราะจะไม่มีสกิลหนี",
          ),
          combo(
            "zed_b_trade",
            ComboDifficulty.BASIC,
            ComboSituation.LANE_TRADE,
            "Standard W-E-Q Poke",
            "ชุดตอดมาตรฐาน W-E-Q",
            listOf("W", "E", "Q", "W2"),
            "Place W slightly behind the target, immediately E to slow them, then aim double Q for Electrocute; only W2 in for passive AA if lethal.",
            "วาง W ข้างหลังเป้าหมาย กด E สโลว์ทันทีเพื่อให้ปา Q คู่ติด Electrocute แม่นขึ้น",
            "Casting E right after W hits while the target is still slowed into double Q.",
            "กด E ทันทีที่เงาโผล่ ศัตรูจะติดสโลว์ทำให้ปา Q สองแฉกโดนแทบ 100%",
          ),
          combo(
            "zed_m_solo",
            ComboDifficulty.MEDIUM,
            ComboSituation.SOLO_KILL,
            "Death Mark Shadow Kite",
            "อันติวางเงาปิดคิลแล้วสลับหลบ",
            listOf("W", "R", "E", "Q", "AA", "W2", "R2"),
            "Pre-place W behind for an exit, Ult onto the carry, land triple-Q/E + Passive AA, then swap to W2/R2 to dodge Stasis/CC.",
            "วาง W ทิ้งไว้ข้างหลังก่อนกด R เข้าไปยัด E + Q + Passive แล้วสลับ W2 หรือ R2 หลบสวน",
            "Watch the shuriken icon above the enemy's head — it means Death Mark is lethal.",
            "สังเกตสัญลักษณ์ดาวกระจายหมุนบนหัวศัตรู หากขึ้นแสดงว่าดาเมจ R ระเบิดตายแน่นอน",
          ),
          combo(
            "zed_m_escape",
            ComboDifficulty.MEDIUM,
            ComboSituation.ESCAPE_SURVIVAL,
            "Shadow Fake-Out Escape",
            "หลอกสลับเงาข้ามกำแพงหนี",
            listOf("W", "R", "E", "W2", "R2"),
            "Throw W over a wall, R an enemy diver to dodge burst untargetably, then instantly swap to W2 far away.",
            "ปา W ข้ามกำแพง กด R จิ้มตัวศัตรูเพื่อหลบสกิลแบบอมตะ แล้วสลับตัวกลับไปหา W2 ข้ามกำแพงทันที",
            "Remember Ult places you directly BEHIND the target — use it to jump past skillshots.",
            "R จะพาเราไปโผล่หลังตัวศัตรูเสมอ ใช้หลบสกิล CC ที่พุ่งมาข้างหน้าได้ดีมาก",
          ),
          combo(
            "zed_h_tf",
            ComboDifficulty.HARD,
            ComboSituation.TEAMFIGHT,
            "Triple Shuriken Flash Burst",
            "ดาวกระจาย 3 แฉก Teamfight",
            listOf("R", "W", "Flash", "Q", "E", "AA", "W2"),
            "R the priority carry, snap W to the flank angle + Flash sideways to dodge retaliation, then converge all 3 Qs.",
            "กด R ใส่แครี่ วาง W อีกมุมแล้ว Flash หลบ CC ไปด้านข้าง จากนั้นปา Q ให้ 3 แฉกพุ่งบรรจบจุดเดียว",
            "Keep W and R shadows forming a triangle around the target so all 3 Qs converge.",
            "จัดวางเงา R กับ W ให้เป็นรูปสามเหลี่ยมล้อมเป้าหมาย ดาวกระจาย 3 เส้นจะพุ่งตัดเข้าเป้าพร้อมกัน",
          ),
        ),
      "lee_sin" to
        listOf(
          combo(
            "lee_b_wc",
            ComboDifficulty.BASIC,
            ComboSituation.WAVE_CLEAR,
            "Flurry Camp Clear",
            "สลับสกิลตีเร็วเคลียร์ป่า",
            listOf("Q", "AA", "AA", "E", "AA", "AA", "W"),
            "Space every ability cast with 2 Passive AAs to sustain Energy and maximize Jungle clear DPS.",
            "แทรกตีธรรมดา 2 ครั้งหลังกดแต่ละสกิล เพื่อรี Energy และได้ความเร็วตีสูงสุด",
            "Never spam Q1-Q2 back-to-back on jungle camps; hold Q2 slightly as an execute.",
            "รอเลือดมอนป่ายุบก่อนค่อยกด Q2 ปิด เพราะ Q2 แรงขึ้นตามเลือดที่หายไป",
          ),
          combo(
            "lee_b_trade",
            ComboDifficulty.BASIC,
            ComboSituation.LANE_TRADE,
            "Free-Dash Gank Engage",
            "พุ่ง W เปิดสโลว์ E ก่อนเตะ Q",
            listOf("W", "E", "AA", "Q1", "Q2"),
            "Dash in with Wild Rift free-target W, slow the enemy with E first so Q1 becomes undodgeable, then finish with Q2.",
            "พุ่ง W เข้าประชิดแล้วกด E สโลว์ก่อน เพื่อปา Q1 ให้โดนง่ายๆ แล้วตาม Q2 ปิด",
            "In Wild Rift, W dashes anywhere without needing a ward — use it to angle E slow first.",
            "W ใน Wild Rift พุ่งได้อิสระไม่ต้องปักหวอด ใช้พุ่งดักหน้าแล้วยัด E สโลว์ก่อนปา Q1",
          ),
          combo(
            "lee_m_solo",
            ComboDifficulty.MEDIUM,
            ComboSituation.SOLO_KILL,
            "Guaranteed Kick-Q Burst",
            "เตะ R แล้วการันตี Q2 ปิดคิล",
            listOf("Q1", "AA", "E", "R", "Q2", "AA"),
            "Tag enemy with Q1, weave E + AA, kick them away with R, and chase with Q2 while they are mid-air.",
            "ปา Q1 โดนแล้วเข้าไปยัด E+AA จากนั้นเตะ R กระเด็นแล้วรีบกด Q2 พุ่งตามปิดขณะลอยอยู่",
            "Wait ~0.2s after R kick before pressing Q2 so you don't miss melee follow-up.",
            "รอจังหวะตัวศัตรูกระเด็นออกไปนิดนึงค่อยกด Q2 จะได้ทั้งดาเมจเตะและดาเมจปิดครบถ้วน",
          ),
          combo(
            "lee_m_escape",
            ComboDifficulty.MEDIUM,
            ComboSituation.ESCAPE_SURVIVAL,
            "W Wall-Hop & Kick Peel",
            "พุ่งข้ามกำแพง + เตะกันตัวล้วง",
            listOf("R", "W", "Q1", "Q2"),
            "Kick the closest threat into their teammates to knock them all up, then W dash across terrain.",
            "เตะตัวที่เข้ามาใกล้ที่สุดชนเพื่อนหลังให้ลอยยกแผง แล้วกด W พุ่งข้ามกำแพงหนี",
            "If an enemy jungler is near a camp, Q1 onto the far camp and Q2 out across the wall.",
            "ใช้ Q1 จิ้มครีปป่าฝั่งตรงข้ามแล้วกด Q2 พุ่งหนีข้ามกำแพงได้อีกต่อ",
          ),
          combo(
            "lee_h_tf",
            ComboDifficulty.HARD,
            ComboSituation.TEAMFIGHT,
            "Insec R-Flash Carry Delivery",
            "Insec R-Flash เตะแครี่เข้าวง",
            listOf("Q1", "Q2", "R", "Flash", "W", "E"),
            "Fly in with Q1-Q2, press R then immediately Flash behind the enemy carry during R windup to kick them straight into your team.",
            "พุ่ง Q1-Q2 เข้าใส่แล้วกด R ตามด้วย Flash ข้ามหลังเป้าหมายกลางแอนิเมชัน เตะแครี่ปลิวกลับมาหาเพื่อนทันที",
            "R -> Flash redirects the kick angle completely with zero time for the carry to Flash away.",
            "กด R ก่อนแล้วค่อย Flash ภายในเสี้ยววินาที เป้าหมายจะไม่มีเวลา Flash หนีเด็ดขาด",
          ),
        ),
      "lux" to
        listOf(
          combo(
            "lux_b_wc",
            ComboDifficulty.BASIC,
            ComboSituation.WAVE_CLEAR,
            "Double-Pop Wave Clear",
            "E-Q เคลียร์เวฟไว",
            listOf("E", "E2", "Q"),
            "Drop E on the caster minions and detonate immediately; use Q only if cannon/melees remain and jungle is spotted.",
            "วาง E กลางแถวมิเนียนเวทแล้วกดระเบิด เก็บ Q ไว้ป้องกันตัวหากไม่เห็นตัวป่าฝั่งตรงข้าม",
            "One E + Luden's Echo clears caster minions cleanly after level 5.",
            "พอมีไอเทมแรก E เพียงลูกเดียวก็เก็บแถวหลังตายเรียบ ประหยัดมานา Q ไว้ป้องกันตัว",
          ),
          combo(
            "lux_b_trade",
            ComboDifficulty.BASIC,
            ComboSituation.LANE_TRADE,
            "Illumination Poke Loop",
            "คอมโบจุดแสงตอดเลน",
            listOf("E", "AA", "Q", "AA"),
            "Slow with E first to make Light Binding easy, weave an Auto Attack after each spell to trigger Passive.",
            "โยน E สโลว์ก่อนแล้วปา Q จะโดนง่ายขึ้น พร้อมยิงธรรมดากระตุก Passive ทุกดอก",
            "Don't detonate E until your Q root hits or they step to the edge of the circle.",
            "อย่าเพิ่งกดระเบิด E ทันที ปล่อยวงสโลว์ค้างไว้ช่วยให้ปา Q โดนง่ายขึ้นมาก",
          ),
          combo(
            "lux_m_solo",
            ComboDifficulty.MEDIUM,
            ComboSituation.SOLO_KILL,
            "Full 100-0 Lock & Laser",
            "ล็อกเบิร์ส 100-0 ด้วย Final Spark",
            listOf("Q", "E", "R", "E2", "AA"),
            "Root with Q, immediately toss E underneath and channel Final Spark (R) before detonating E2 simultaneously.",
            "ยิง Q จับขาได้ปุ๊บโยน E รอใต้เท้าแล้วกด R ทันที จากนั้นกดระเบิด E2 จังหวะเดียวกับแสงเลเซอร์",
            "Final Spark detonates and refreshes Lux Passive automatically across the map.",
            "แสง R จะจุดระเบิด Passive บนตัวศัตรูให้เองโดยไม่ต้องเดินเข้าไปยิง",
          ),
          combo(
            "lux_m_escape",
            ComboDifficulty.MEDIUM,
            ComboSituation.ESCAPE_SURVIVAL,
            "Double-Root & Shield Kite",
            "จับขา 2 ตัว + กางโล่หนี",
            listOf("Q", "E", "W", "Flash"),
            "Wait for enemies to line up so Q hits 2 targets, drop E at your feet to slow pursuers, and throw W shield.",
            "รอจังหวะตัวไล่เดินซ้อนแนวแล้วยิง Q จับ 2 ตัวพร้อมกัน วาง E ใต้เท้าตัวเองสโลว์ แล้วปา W กางโล่",
            "Lux Q pierces through 1 target and roots up to 2 — line it through a minion to surprise chasers.",
            "Q ทะลุตัวแรกได้ 2 เป้าหมาย ยิงทะลุมิเนียนใส่นักล้วงที่ตามหลังได้",
          ),
          combo(
            "lux_h_tf",
            ComboDifficulty.HARD,
            ComboSituation.TEAMFIGHT,
            "Q-Flash Fog Snipe Pick",
            "Q-Flash จับแครี่จากมุมมืด",
            listOf("Q", "Flash", "E", "R", "W", "E2"),
            "Cast Q from brush then Flash forward to extend binding range past frontliners, bursting the backline with E-R while shielding allies with W.",
            "ร่าย Q จากพุ่มแล้ว Flash ข้ามแนวหน้าจับแครี่ทันที ต่อด้วย E-R เบิร์สและปา W ผ่านเพื่อนทั้งทีม",
            "Throw W straight through your clump of frontline allies for maximum multi-man shielding.",
            "เล็งปา W ให้ตัดผ่านแนวหน้าหลายๆ คน โล่ขากลับจะซ้อนหนาขึ้นอีกเท่าตัว",
          ),
        ),
      "katarina" to
        listOf(
          combo(
            "kat_b_wc",
            ComboDifficulty.BASIC,
            ComboSituation.WAVE_CLEAR,
            "Double Dagger Wave Spin",
            "ปามีดคู่ปั่นล้างเวฟ",
            listOf("Q", "E", "W"),
            "Q the front minion so the dagger lands on the casters, Shunpo (E) onto it and drop W to clear everything.",
            "ปา Q ตัวหน้าให้มีดตกกลางเวฟ แล้ว E ลงมีดพร้อมวาง W ปั่นอีกรอบเก็บเรียบ",
            "Picking up both daggers slashes E cooldown so you can jump back safely.",
            "เก็บมีด W เพื่อรีเซ็ตคูลดาวน์ E เอาไว้พุ่งถอยออกทันที",
          ),
          combo(
            "kat_b_trade",
            ComboDifficulty.BASIC,
            ComboSituation.LANE_TRADE,
            "Quick Dagger Poke & Retreat",
            "พุ่งปั่นแล้วกระโดดหนี",
            listOf("Q", "E", "W", "AA", "E"),
            "Bounce Q onto target, E behind them onto the dagger, pop W for movespeed + 2nd spin, and E back to an ally minion.",
            "ปา Q โดนเป้าหมายแล้ว E ลงหลังศัตรู กด W ปั่นรอบสองพร้อมความเร็ววิ่ง แล้วใช้ E ที่รีเซ็ตโดดกลับมิเนียน",
            "Never stay to brawl against bruisers; take the short Electrocute/Conqueror burst and leave.",
            "ไม่ต้องยืนแลกกับไฟต์เตอร์ ใช้สปีด W + E ที่รีเซ็ตหนีออกทันทีหลังเก็บมีด",
          ),
          combo(
            "kat_m_solo",
            ComboDifficulty.MEDIUM,
            ComboSituation.SOLO_KILL,
            "E-W-Q Trap & Death Lotus",
            "ดักมีด 2 จุดแล้วปั่นอัลติ",
            listOf("E", "W", "Q", "R", "E"),
            "Engage instantly with E -> W so the enemy must walk over your Q dagger path, hold R while spinning, then E to finish.",
            "พุ่ง E->W ประชิดทันที แล้วปา Q ดักทางหนี เปิด R ปั่น (ใน Wild Rift เดินขยับช้าๆ ระหว่าง R ได้) แล้ว E ปิด",
            "Unlike PC LoL, Katarina can slowly drift during R in Wild Rift — drift toward their escape path.",
            "ใน Wild Rift กด R แล้วขยับลากตามศัตรูช้าๆ ได้ พยายามขยับดักทาง Flash ออก",
          ),
          combo(
            "kat_m_escape",
            ComboDifficulty.MEDIUM,
            ComboSituation.ESCAPE_SURVIVAL,
            "W-Speed & Ally/Camp Shunpo",
            "เร่งสปีด W + โดด E ข้ามกำแพง",
            listOf("W", "Q", "E"),
            "Drop W immediately for instant movement speed, throw Q toward a jungle camp/ally over the wall, and Shunpo out.",
            "วาง W เอาบัฟวิ่งเร็ว ปา Q หรือหาจังหวะให้เห็นยูนิต/เพื่อนอีกฝั่งของกำแพง แล้วกด E หนีข้ามไป",
            "Shunpo (E) can target friendly champions, enemy minions, jungle plants, and camps.",
            "E กระโดดใส่เพื่อน พืชป่า หรือมิเนียนได้ทุกตัว ใช้หาทางข้ามกำแพงหนีได้ไวมาก",
          ),
          combo(
            "kat_h_tf",
            ComboDifficulty.HARD,
            ComboSituation.TEAMFIGHT,
            "Voracity Reset Chain",
            "คอมโบริเซ็ตต่อเนื่อง Teamfight",
            listOf("E", "W", "Q", "R", "E", "W", "Q", "E"),
            "Wait until enemy hard CC is spent, jump onto the weakest target, cancel R the instant you get a reset to E-W-Q chain the next carry.",
            "รอสกิลล็อกฝั่งตรงข้ามออกหมดก่อน ค่อยโดด E-W-Q-R ใส่ตัวบาง พอฆ่าได้ให้ตัด R ทันทีเพื่อ E-W-Q ไล่เก็บตัวต่อไป",
            "Do NOT channel full R if a low-HP reset target tries to walk out — cancel R early with E.",
            "อย่าฝืนยืนหมุน R จนจบหากเห็นตัวเลือดน้อยกำลังหนี ให้กด E ตัด R พุ่งตามเก็บรีเซ็ตเลย",
          ),
        ),
      "xayah" to
        listOf(
          combo(
            "xay_b_wc",
            ComboDifficulty.BASIC,
            ComboSituation.WAVE_CLEAR,
            "Feather Piercing Clear",
            "ขนนกปักแถวแล้วดึง E เคลียร์",
            listOf("Q", "AA", "E"),
            "Send Q down the wave, fire 1–2 piercing Passive AAs, and recall feathers with E to nuke all minions.",
            "ยิง Q ทะลุแถวมิเนียน ตบ Passive ทะลุ 1–2 นัด แล้วดึง E เก็บทั้งเวฟในพริบตา",
            "Angle yourself straight down the lane so every feather passes through all 4 minions.",
            "ยืนจัดมุมตรงกับแถวมิเนียน ขนนกจะได้ปักทะลุทั้งตัวหน้าและตัวหลัง",
          ),
          combo(
            "xay_b_trade",
            ComboDifficulty.BASIC,
            ComboSituation.LANE_TRADE,
            "Instant 3-Feather Snare",
            "คอมโบปักขนนกจับขาเร็ว",
            listOf("Q", "AA", "E", "W"),
            "Fire Q then immediately weave 1 AA so 3 feathers sit behind the enemy, then press E to root cleanly.",
            "กด Q พร้อมยิงธรรมดา 1 นัดทันที จะได้ขนนก 3 อันพร้อมดึง E จับขาทันที แล้วเปิด W ไล่ยิง",
            "You only need 3 feathers passing through a champion to trigger the Bladecaller root.",
            "ขอแค่ขนนก 3 อันพุ่งกลับโดนตัวศัตรู จะติด Root ทันที",
          ),
          combo(
            "xay_m_solo",
            ComboDifficulty.MEDIUM,
            ComboSituation.SOLO_KILL,
            "W-Speed Kite & E Burst",
            "เร่งสปีด W + E ปิดคิล",
            listOf("W", "Q", "AA", "AA", "E"),
            "Pop W for Attack Speed and 20% bonus damage, kite sideways laying 5+ feathers behind them, then rip E.",
            "เปิด W เอาความเร็วตีและดาเมจเสริม ยิงสไลด์ขวางวางขนนก 5+ อันแล้วกระชาก E ปิดดาเมจหนัก",
            "W grants bonus movespeed whenever you hit an enemy champion — use it to strafe skillshots.",
            "W ให้โบนัสวิ่งเร็วทุกครั้งที่ยิงโดนฮีโร่ ใช้โยกหลบสกิลและวางมุมขนนก",
          ),
          combo(
            "xay_m_escape",
            ComboDifficulty.MEDIUM,
            ComboSituation.ESCAPE_SURVIVAL,
            "Untargetable Featherstorm Escape",
            "อัลติหลบเบิร์ส + สวนจับขาหนี",
            listOf("R", "W", "E", "Flash"),
            "Use R the split-second a Zed/Malphite engages to become untargetable, drop 5 feathers, and pull E to root and kite out.",
            "กด R สวนเสี้ยววินาทีที่โดนล้วง (อมตะชั่วคราว) โปรยขนนก 5 อัน แล้วดึง E จับขาวิ่งสวนหนี",
            "Move backward while hovering during R so your landing spot creates max distance.",
            "ระหว่างลอย R ให้ลากจอยเดินถอยหลังตลอด เพื่อสร้างระยะห่างก่อนร่วงลงพื้น",
          ),
          combo(
            "xay_h_tf",
            ComboDifficulty.HARD,
            ComboSituation.TEAMFIGHT,
            "E-Flash Angle Redirection",
            "E-Flash หักมุมขนนกจับหมู่",
            listOf("Q", "R", "AA", "E", "Flash"),
            "Flood the fight with feathers using Q + R, press E and instantly Flash sideways to drag every feather across the backline.",
            "วางขนนกให้เต็มสนามด้วย Q+R กด E แล้วรีบ Flash เฉียงเปลี่ยนจุดรวมขนนก ให้พุ่งปักรวบแครี่-เมจทุกคน",
            "Feathers always recall to Xayah's NEW post-Flash location — surprise flashes catch whole teams.",
            "ขนนกจะพุ่งกลับมาที่ตัว Xayah ณ ตำแหน่งปลายทางหลัง Flash เสมอ ใช้หักมุมจับหมู่ได้เฉียบขาด",
          ),
        ),
      "rakan" to
        listOf(
          combo(
            "rak_b_wc",
            ComboDifficulty.BASIC,
            ComboSituation.WAVE_CLEAR,
            "Support Poke & Heal Setup",
            "ช่วยดันเวฟ + ฮีล Q",
            listOf("Q", "W", "E"),
            "Poke champions/cannon with Q to charge heal, help clear with W only if pushing fast, and keep E ready to shield your ADC.",
            "ปา Q เก็บสแต็กฮีลให้แครี่ ใช้ W เฉพาะตอนรีบดันเวฟเข้าป้อม และเก็บ E ไว้กางโล่ให้แครี่",
            "Deliver Q's healing circle directly to your carry by pressing E to dash onto them.",
            "หลังปา Q โดน กด E พุ่งกลับหาแครี่วงฮีลจะไปติดที่ตัวแครี่ทันที",
          ),
          combo(
            "rak_b_trade",
            ComboDifficulty.BASIC,
            ComboSituation.LANE_TRADE,
            "Safe E-W-Q-E2 Engage",
            "เข้าเร็วออกไว E-W-Q-E2",
            listOf("E", "W", "Q", "AA", "E2"),
            "Dash to frontline/ADC with E1 for extra engage range, knock up with W, hit Q, then E2 right back to safety.",
            "กด E1 พุ่งเกาะเพื่อนเพิ่มระยะ เข้า W งัดลอย ยิง Q-AA แล้วดีด E2 กลับหาแครี่ทันที",
            "Watch your ally's position; don't let them walk out of your E2 return circle.",
            "สังเกตวงสีเขียวรอบตัวแครี่ อย่าเข้าลึกจนแครี่เดินหนีพ้นระยะดีด E2 กลับ",
          ),
          combo(
            "rak_m_solo",
            ComboDifficulty.MEDIUM,
            ComboSituation.SOLO_KILL,
            "Charm Into Knockup Chain",
            "R เสน่ห์ลากมารวมแล้ว W งัดซ้ำ",
            listOf("R", "W", "Q", "AA", "E"),
            "Activate R for huge movespeed, tag targets to group them with Charm, then immediately W underneath so they cannot Flash.",
            "เปิด R เร่งสปีดเดินกวาดชนให้ติดเสน่ห์ลากเข้าหากัน แล้ววาง W งัดซ้ำกลางวงไม่ให้หลบ",
            "W right as R Charm is about to expire to maximize total CC duration (2.25s+).",
            "รออีกฝ่ายติดเสน่ห์ R สักเสี้ยววินาทีค่อยกด W จะต่อเวลา CC ได้นานสุด",
          ),
          combo(
            "rak_m_escape",
            ComboDifficulty.MEDIUM,
            ComboSituation.ESCAPE_SURVIVAL,
            "Double Wall-Hop & Charm Peel",
            "W ข้ามกำแพง + E เกาะเพื่อนหนี",
            listOf("R", "W", "E", "E2"),
            "Pop R to stop pursuers chasing, W across a wall, then E-E2 to the furthest teammate (bonus range with Xayah).",
            "กด R ชนตัวไล่ให้ติดเสน่ห์ โดด W ข้ามกำแพง แล้วใช้ E1-E2 พุ่งเกาะเพื่อนหนีไกลๆ",
            "With Xayah on your team, Rakan's E dash range is dramatically extended.",
            "ถ้ามี Xayah ในทีม ระยะกระโดด E จะไกลขึ้นมหาศาล หนีพ้นแทบทุกสถานการณ์",
          ),
          combo(
            "rak_h_tf",
            ComboDifficulty.HARD,
            ComboSituation.TEAMFIGHT,
            "R-Flash-W Unreactable Engage",
            "R-Flash-W เปิดไฟต์ไร้ดีเลย์",
            listOf("R", "Flash", "W", "Q", "E", "E2"),
            "Press R first, Flash straight into the enemy carry cluster for instant Charm, then W everyone together and E2 out to shield your carry.",
            "เปิด R ก่อนแล้ว Flash ผ่ากลางวงแครี่เพื่อติดเสน่ห์ทันที จากนั้น W งัดรวบทั้งทีมแล้วยิง Q ก่อน E2 กลับมากางโล่ให้เพื่อน",
            "R -> Flash gives opponents zero reaction window compared to W alone.",
            "การเปิด R ก่อนแล้ว Flash ไม่มีแอนิเมชันเตือนเหมือน W ทำให้ฝั่งตรงข้ามกดหนีไม่ทัน",
          ),
        ),
      "samira" to
        listOf(
          combo(
            "sam_b_wc",
            ComboDifficulty.BASIC,
            ComboSituation.WAVE_CLEAR,
            "Q-E Sword Wave Clear",
            "ฟัน Q-E ล้างเวฟ",
            listOf("Q", "AA", "E"),
            "Fire ranged Q then dash through low minions with E-Q (draws a blazing line across the wave).",
            "ยิง Q แล้วกด E พุ่งผ่านเวฟพร้อมกด Q กลางทางเพื่อฟันคลื่นดาบกวาดทั้งแถว",
            "Use E through minions only when enemy support cooldowns are down.",
            "ใช้ E พุ่งเข้าเวฟเฉพาะตอนซัพพอร์ตอีกฝั่งใช้สกิลจับไปแล้ว",
          ),
          combo(
            "sam_b_trade",
            ComboDifficulty.BASIC,
            ComboSituation.LANE_TRADE,
            "Fast Style Rank Weaving",
            "สลับสกิล-ตีธรรมดาปั้นเกรด",
            listOf("AA", "Q", "AA", "W"),
            "Always alternate unique hits (AA -> Q -> AA) to climb Style Grade rapidly and block key projectiles with W.",
            "สลับ AA -> Q -> AA ห้ามใช้ท่าซ้ำติดกันเพื่อให้เกรด Style ขึ้นเร็ว แล้วเก็บ W ไว้ปัดสกิลสำคัญ",
            "Consecutive identical attacks don't add Style — alternating AA and abilities is key.",
            "ตีซ้ำหรือใช้สกิลเดิมติดกันจะไม่ได้สแต็กเกรด ต้องสลับตีธรรมดากับสกิลเสมอ",
          ),
          combo(
            "sam_m_solo",
            ComboDifficulty.MEDIUM,
            ComboSituation.SOLO_KILL,
            "Instant S-Rank Fast Combo",
            "คอมโบปั้นเกรด S ภายใน 1.5 วิ",
            listOf("AA", "W1", "E", "Q", "W2", "AA", "R"),
            "AA (E), cast W1 (D), dash through with E+Q during W (C -> B), let W2 slash (A), weave 1 AA (S), and unleash Inferno Trigger!",
            "AA (E) -> กด W1 (D) -> พุ่ง E+Q กลางวงหมุน (C -> B) -> ปล่อยฟัน W2 (A) -> ตบ AA (S) -> กด R ทันที!",
            "Both halves of W (start & end spin) grant separate Style stacks if you E-Q in between.",
            "หมุนดาบ W นับเป็น 2 ฮิต (เริ่มหมุนกับจบหมุน) ถ้าแทรก E+Q ตรงกลางจะขึ้นเกรด S ทันที",
          ),
          combo(
            "sam_m_escape",
            ComboDifficulty.MEDIUM,
            ComboSituation.ESCAPE_SURVIVAL,
            "W Projectile Block & E Reset Out",
            "W ปัด CC + E รีเซ็ตโดดออก",
            listOf("W", "E", "Flash"),
            "Hold W for hooks/stuns/Ashe Arrow, then dash through an angled enemy or backline minion after any kill reset.",
            "เก็บ W ไว้ปัดสกิลจับอย่าง Blitz/Thresh/Lux แล้วรอ E ที่รีเซ็ตหลังคิลพุ่งหนีข้ามจุดอับ",
            "E resets immediately on champion takedowns — look for a back-angle dash out of danger.",
            "ทุกครั้งที่เก็บคิลหรือแอสซิสต์ E จะรีเซ็ตทันที ใช้พุ่งฉีกออกจากวงไฟต์ได้",
          ),
          combo(
            "sam_h_tf",
            ComboDifficulty.HARD,
            ComboSituation.TEAMFIGHT,
            "Double Inferno Reset Sweep",
            "รำอัลติ 2 รอบ Teamfight",
            listOf("AA", "Q", "W1", "E", "W2", "AA", "R", "E", "Q", "R"),
            "Pre-stack C/B safely on frontline, dive backline with W-E-AA into R1, then use the E reset + quick Q-AA to fire R2.",
            "เก็บเกรด C-B จากแทงค์แนวหน้าก่อน ค่อยพุ่ง W-E-AA เข้าวงแครี่เปิด R1 พอได้คิลรีบ E-Q-AA ยิง R2 ซ้ำ",
            "Watch enemy hard CC (Nautilus R, Leona Q) before diving — CC instantly cancels Samira R.",
            "เช็คสกิลสตั๊นอีกฝั่งก่อนพุ่งเข้า เพราะถ้าโดน Hard CC กลางวง R จะถูกตัดทันที",
          ),
        ),
      "lux" to (emptyList()), // replaced below cleanly? Wait - avoid duplicate key in mapOf!
    )

  // Clean separate map without duplicate keys (remove duplicate "lux" above and define extended curated map)
  private val SIGNATURE_COMBOS: Map<String, List<ChampionComboEntry>> =
    mapOf(
      "ahri" to CURATED_COMBOS.getValue("ahri"),
      "yasuo" to CURATED_COMBOS.getValue("yasuo"),
      "yone" to CURATED_COMBOS.getValue("yone"),
      "zed" to CURATED_COMBOS.getValue("zed"),
      "lee_sin" to CURATED_COMBOS.getValue("lee_sin"),
      "katarina" to CURATED_COMBOS.getValue("katarina"),
      "xayah" to CURATED_COMBOS.getValue("xayah"),
      "rakan" to CURATED_COMBOS.getValue("rakan"),
      "samira" to CURATED_COMBOS.getValue("samira"),
      "lux" to
        listOf(
          combo(
            "lux_b_wc",
            ComboDifficulty.BASIC,
            ComboSituation.WAVE_CLEAR,
            "Double-Pop Wave Clear",
            "E-Q เคลียร์เวฟไว",
            listOf("E", "E2", "Q"),
            "Drop E on the caster minions and detonate immediately; keep Q if enemy jungler is missing.",
            "วาง E กลางแถวมิเนียนเวทแล้วกดระเบิด เก็บ Q ไว้ป้องกันตัวหากไม่เห็นตัวป่าฝั่งตรงข้าม",
            "One E + Luden's Echo clears caster minions cleanly after level 5.",
            "พอมีไอเทมแรก E เพียงลูกเดียวก็เก็บแถวหลังตายเรียบ ประหยัดมานา Q ไว้ป้องกันตัว",
          ),
          combo(
            "lux_b_trade",
            ComboDifficulty.BASIC,
            ComboSituation.LANE_TRADE,
            "Illumination Poke Loop",
            "คอมโบจุดแสงตอดเลน",
            listOf("E", "AA", "Q", "AA"),
            "Slow with E first to make Q easy, weave AAs to pop Lux Passive.",
            "โยน E สโลว์ก่อนแล้วปา Q จะโดนง่ายขึ้น พร้อมยิงธรรมดากระตุก Passive",
            "Don't pop E too early — let the zone slow them into your Q.",
            "อย่าเพิ่งกดระเบิด E ทันที ปล่อยวงสโลว์ค้างไว้ช่วยให้ปา Q โดนง่ายขึ้นมาก",
          ),
          combo(
            "lux_m_solo",
            ComboDifficulty.MEDIUM,
            ComboSituation.SOLO_KILL,
            "Full 100-0 Lock & Laser",
            "ล็อกเบิร์ส 100-0 ด้วย Final Spark",
            listOf("Q", "E", "R", "E2"),
            "Root with Q, toss E underneath and channel Final Spark (R), then detonate E2 with the beam.",
            "ยิง Q จับขาได้ปุ๊บโยน E รอใต้เท้าแล้วกด R ทันที จากนั้นกดระเบิด E2 จังหวะเดียวกับแสงเลเซอร์",
            "Final Spark detonates and refreshes Lux Passive automatically across the map.",
            "แสง R จะจุดระเบิด Passive บนตัวศัตรูให้เองโดยไม่ต้องเดินเข้าไปยิง",
          ),
          combo(
            "lux_m_escape",
            ComboDifficulty.MEDIUM,
            ComboSituation.ESCAPE_SURVIVAL,
            "Double-Root & Shield Kite",
            "จับขา 2 ตัว + กางโล่หนี",
            listOf("Q", "E", "W", "Flash"),
            "Line up Q to root 2 chasers, drop E at your feet to slow, and throw W shield.",
            "รอจังหวะตัวไล่เดินซ้อนแนวแล้วยิง Q จับ 2 ตัวพร้อมกัน วาง E ใต้เท้าสโลว์ แล้วปา W กางโล่",
            "Lux Q pierces 1 target and roots up to 2 — shoot through a minion to surprise divers.",
            "Q ทะลุตัวแรกได้ 2 เป้าหมาย ยิงทะลุมิเนียนใส่นักล้วงที่ตามหลังได้",
          ),
          combo(
            "lux_h_tf",
            ComboDifficulty.HARD,
            ComboSituation.TEAMFIGHT,
            "Q-Flash Fog Snipe Pick",
            "Q-Flash จับแครี่จากมุมมืด",
            listOf("Q", "Flash", "E", "R", "W", "E2"),
            "Cast Q from brush then Flash forward to extend binding past frontliners, bursting with E-R while shielding allies with W.",
            "ร่าย Q จากพุ่มแล้ว Flash ข้ามแนวหน้าจับแครี่ทันที ต่อด้วย E-R เบิร์สและปา W ผ่านเพื่อนทั้งทีม",
            "Aim W through clumped allies for maximum multi-target shield value.",
            "เล็งปา W ให้ตัดผ่านแนวหน้าหลายๆ คน โล่ขากลับจะซ้อนหนาขึ้นอีกเท่าตัว",
          ),
        ),
      "malphite" to
        listOf(
          combo(
            "mal_b_wc",
            ComboDifficulty.BASIC,
            ComboSituation.WAVE_CLEAR,
            "E-W Granite Wave Slam",
            "ตบพื้น E-W ล้างเวฟ",
            listOf("W", "E", "AA"),
            "Activate Thunderclap (W) for cleave autos and slam Ground Slam (E) in the center of the wave.",
            "เปิด W เพิ่มเกราะและตีกระจาย แล้วกด E ตบพื้นกลางวงมิเนียน",
            "Save Q poke for champions; W+E clears minions efficiently on low mana.",
            "เก็บ Q ไว้ตอดแชมเปี้ยน ประหยัดมานาด้วยการเคลียร์เวฟด้วย W+E",
          ),
          combo(
            "mal_b_trade",
            ComboDifficulty.BASIC,
            ComboSituation.LANE_TRADE,
            "Passive-Shield Short Trade",
            "รอโล่ Passive แล้วเข้าเทรด",
            listOf("Q", "W", "AA", "E"),
            "Steal movespeed with Q, walk up with Granite Shield full, slam E to cripple enemy Attack Speed and walk out.",
            "ปา Q ขโมยสปีดแล้วเดินเข้าไปฟาด W+AA+E ลดความเร็วโจมตีอีกฝั่ง ก่อนถอยออกตอนโล่แตก",
            "Don't trade without Passive Shield; Malphite wins every short trade with it up.",
            "เทรดเฉพาะตอนโล่หิน Passive เต็มหลอดเท่านั้น แล้วถอยรอคูลดาวน์โล่รอบต่อไป",
          ),
          combo(
            "mal_m_solo",
            ComboDifficulty.MEDIUM,
            ComboSituation.SOLO_KILL,
            "Unstoppable Force Burst",
            "อัลติชนปิดคิลเดี่ยว",
            listOf("Q", "R", "W", "AA", "E"),
            "Poke enemy below 60% with Q, smash R on top of them, immediately W-AA-E to delete squishies before they land.",
            "ตอดด้วย Q จนเลือดต่ำกว่า 60% แล้วพุ่ง R กระแทก ต่อด้วย W-AA-E ปิดก่อนอีกฝั่งตกถึงพื้น",
            "Q first forces their dashes/Flash OR guarantees they stay slowed inside R radius.",
            "ปา Q นำก่อนช่วยให้กะระยะพุ่ง R แม่นขึ้นมาก",
          ),
          combo(
            "mal_m_escape",
            ComboDifficulty.MEDIUM,
            ComboSituation.ESCAPE_SURVIVAL,
            "Steal-Speed & Terrain Ult Escape",
            "ปา Q ดูดสปีด / พุ่ง R หนีข้ามกำแพง",
            listOf("Q", "E", "R"),
            "Q the fastest chaser to steal movespeed, E to slow melee bruisers, and use Unstoppable R over thick walls only if lethal.",
            "ปา Q ใส่ตัวที่ไล่ใกล้สุดเพื่อขโมยสปีดวิ่ง กด E ตัดขา และใช้ R พุ่งข้ามกำแพงหนีเมื่อจวนตัวจริงๆ",
            "Remember R makes you Unstoppable — use it through incoming Leona/Morgana CC.",
            "R กัน CC ระหว่างพุ่ง 100% ใช้พุ่งทะลุสกิลจับหนีออกจากวงล้อมได้",
          ),
          combo(
            "mal_h_tf",
            ComboDifficulty.HARD,
            ComboSituation.TEAMFIGHT,
            "Flash-R Fog Backline Knockup",
            "Flash-R จับหมู่แนวหลัง",
            listOf("Flash", "R", "E", "W", "Q"),
            "From fog, Flash into range and instant-R 3+ carries, instantly E to cripple ADC DPS, then focus the carry with W-Q.",
            "ดักในพุ่มมืดแล้ว Flash+R ทันทีใส่กลุ่มแครี่-เมจ 3 ตัวขึ้นไป ตามด้วย E ลดความเร็วตีแครี่แล้วไล่ Q-W",
            "Never walk slowly in vision before pressing R — enemies will Stasis or Flash out.",
            "ห้ามเดินดุ่มๆ เข้าไปตรงหน้าเพราะแครี่จะรอ Flash สวน ให้ใช้ระยะจากพุ่มมืดหรือ Flash+R",
          ),
        ),
      "darius" to
        listOf(
          combo(
            "dar_b_wc",
            ComboDifficulty.BASIC,
            ComboSituation.WAVE_CLEAR,
            "Outer-Blade Wave Spin",
            "เหวี่ยง Q ปลายขวานล้างเวฟ",
            listOf("Q", "W", "AA"),
            "Position so the outer ring of Decimate (Q) slices all 4 creeps, then reset an auto with W on the cannon.",
            "จัดระยะให้วงนอกของ Q โดนมิเนียนครบทั้ง 4 ตัว แล้วกด W รีเซ็ตจังหวะตีเก็บรถถัง",
            "Auto-cancelling into W refunds mana & halves W cooldown if it kills a minion.",
            "ใช้ W ปิดลาสช็อตมิเนียนจะคืนมานาและลดคูลดาวน์ W ครึ่งหนึ่ง",
          ),
          combo(
            "dar_b_trade",
            ComboDifficulty.BASIC,
            ComboSituation.LANE_TRADE,
            "AA-W-Q Animation Cancel",
            "สับ AA-W-Q ตัดแอนิเมชัน",
            listOf("AA", "W", "Q", "E"),
            "Auto, instantly reset with W (90% slow), and immediately press Q while backing slightly so the outer blade always lands.",
            "ตีธรรมดาแล้วกด W ตัดจังหวะให้ติดสโลว์ 90% จากนั้นกด Q พร้อมก้าวถอยครึ่งก้าวให้ปลายขวานฟันโดนเป๊ะ",
            "E pulls them back in only if they try to run before stack 5.",
            "เก็บ E ไว้ดึงตอนศัตรูกำลังจะหนี อย่าเพิ่งเปิดด้วย E ถ้าเดินถึงตัวอยู่แล้ว",
          ),
          combo(
            "dar_m_solo",
            ComboDifficulty.MEDIUM,
            ComboSituation.SOLO_KILL,
            "Guaranteed E-Q Stack 5 Dunk",
            "ดึง E ต่อ Q ไม่มีพลาด + สับ R",
            listOf("E", "AA", "W", "Q", "AA", "R"),
            "Hook with E -> AA -> W -> immediate Q (while slowed/airborne, outer blade cannot miss) -> reach 5 Bleed stacks -> Dunk R.",
            "เกี่ยว E เข้ามาตบ AA -> W -> กด Q ทันที (ติดสโลว์หนีวงนอกไม่ทัน) เก็บเลือดไหลครบ 5 ชั้นแล้วสับ R",
            "Wait for 5 Bleed stacks before R unless target is below ~20% HP for reset.",
            "พยายามให้ครบ 5 สแต็กเลือดไหลก่อนสับ R เพื่อให้ได้บัฟ Noxian Might ล้างไฟต์ต่อ",
          ),
          combo(
            "dar_m_escape",
            ComboDifficulty.MEDIUM,
            ComboSituation.ESCAPE_SURVIVAL,
            "Q-Heal Sustain & E Peel",
            "ฮีลปลายขวาน Q + ดึงเบรกเอาตัวรอด",
            listOf("E", "Q", "W", "Ghost"),
            "Wait until 2+ gankers hug you, walk slightly out and hit multi-champ outer Q for huge missing-HP heal, then Ghost/W away.",
            "ล่อให้ตัวแก๊งยืนติดกัน แล้วถอยนิดนึงเหวี่ยง Q วงนอกให้โดน 2–3 คนจะฮีลเลือดเด้งมหาศาล ตามด้วย W+Ghost ถอย",
            "Outer Q heals up to 45% missing HP if it clips 3 enemies — turn 1v2s around.",
            "Q วงนอกโดน 3 ฮีโร่ฮีลเลือดแรงมาก ใช้พลิกชนะไฟต์ 1v2 หรือซื้อเวลาหนีได้สบาย",
          ),
          combo(
            "dar_h_tf",
            ComboDifficulty.HARD,
            ComboSituation.TEAMFIGHT,
            "Q-Flash Reset Chain Dunk",
            "Q-Flash สับ R รีเซ็ตล้างทีมไฟต์",
            listOf("Ghost", "Q", "Flash", "E", "AA", "W", "R", "R"),
            "Channel Q on frontline then Flash into backline so outer blade clips low-HP carries, chain 5-stack dunk R, then spread instant 5 stacks onto everyone.",
            "ง้าง Q แล้ว Flash เข้าแนวหลังให้ปลายขวานเฉือนตัวบาง ตบปิดด้วย R พอได้ Noxian Might แล้วสับต่อทุกตัวที่เข้าใกล้",
            "Once Noxian Might activates from a 5-stack kill, a single AA/Q puts full 5 stacks on new targets.",
            "เมื่อเข้าสู่โหมด Noxian Might แล้ว ตีหรือสกิลทีเดียวจะติด 5 สแต็กทันที เตรียมกด R ตัวต่อไปได้เลย",
          ),
        ),
      "nautilus" to
        listOf(
          combo(
            "nau_b_wc",
            ComboDifficulty.BASIC,
            ComboSituation.WAVE_CLEAR,
            "W-E Anchor Slam",
            "กางโล่ W + ตบคลื่น E",
            listOf("W", "E", "AA"),
            "Pop Titan's Wrath (W) to shield and empower AoE autos, then press Riptide (E) over the wave.",
            "กดโล่ W ตีกระจายแล้วกระทืบพื้น E รอบตัวเพื่อช่วยดันเวฟไว",
            "Keep Q open to hook terrain and retreat quickly after shoving.",
            "เก็บ Q ไว้ยิงติดกำแพงถอยกลับเสมอเวลาช่วยแครี่ดันเวฟลึก",
          ),
          combo(
            "nau_b_trade",
            ComboDifficulty.BASIC,
            ComboSituation.LANE_TRADE,
            "Hook-Root-Shield Lock",
            "ดึง Q + ตบแช่แข็ง Passive",
            listOf("Q", "AA", "W", "E"),
            "Land Dredge Line (Q), let the automatic AA root with Staggering Blow, immediately W-reset AA, then E slow as they flee.",
            "ยิง Q ดึงเข้ามา ปล่อยให้ตีธรรมดาปัก Passive จับขา แล้วกด W รีเซ็ตจังหวะตีต่อด้วย E สโลว์",
            "Your W is an auto-attack reset — always press W right after Passive AA lands.",
            "W ช่วยรีเซ็ตการตีธรรมดา กด W ทันทีหลังตบ Passive ดอกแรกจะได้ตีซ้ำไวขึ้น",
          ),
          combo(
            "nau_m_solo",
            ComboDifficulty.MEDIUM,
            ComboSituation.SOLO_KILL,
            "Point-Blank Ult Lock Chain",
            "ล็อก CC ต่อเนื่อง 3 จังหวะ",
            listOf("R", "Q", "AA", "W", "E"),
            "Lead with Depth Charge (R) when enemies try to juke behind minions, wait for the knockup, then land guaranteed Q-AA-W-E.",
            "เปิด R ล็อกเป้าก่อนเวลาอีกฝั่งยืนหลบหลังมิเนียน พอลอยขึ้นค่อยปา Q-AA-W-E การันตีไม่พลาด",
            "Point-and-click R knocks up everyone along its travel path too.",
            "R วิ่งล็อกเป้าหลบไม่ได้และงัดทุกคนที่ขวางทางลอยไปด้วย",
          ),
          combo(
            "nau_m_escape",
            ComboDifficulty.MEDIUM,
            ComboSituation.ESCAPE_SURVIVAL,
            "Terrain Anchor Escape",
            "ยิงสมอเกาะกำแพงหนี + ลดคูลดาวน์",
            listOf("E", "W", "Q"),
            "Trigger E to slow chasers, pop W shield, and aim Q at thick walls or turrets to pull away and refund 50% Q cooldown.",
            "กด E สโลว์คนไล่ กางโล่ W แล้วเล็ง Q ยิงติดกำแพงหรือป้อมฝั่งเรา จะดึงตัวหนีเร็วและคืนคูลดาวน์ Q 50%",
            "Terrain hooks refund half Q cooldown and mana — faster than walking!",
            "ยิง Q โดนกำแพงคืนคูลดาวน์ครึ่งหนึ่ง ทำให้เคลื่อนที่หนีตามแผนที่ได้เร็วมาก",
          ),
          combo(
            "nau_h_tf",
            ComboDifficulty.HARD,
            ComboSituation.TEAMFIGHT,
            "Split-CC Carry & Diver Peel",
            "แยก CC: R แครี่ / Passive หยุดตัวล้วง",
            listOf("R", "Flash", "Q", "AA", "E", "W"),
            "Cast R onto the enemy backline carry to force Flash/Stasis, then Q-Flash or Passive-AA different divers to root every threat.",
            "ปล่อย R ใส่แครี่แนวหลังให้กระเด็น แล้วหันมา Passive-AA ตบตัวล้วงฝั่งเราคนละทีเพื่อแช่แข็งแยกกันทั้งหมด",
            "Track Passive circle cooldown per enemy: spreading 1 AA per diver roots multiple assassins!",
            "Passive จับขาคิดแยกตัวละ 1 รอบ ในไฟต์ให้สลับตบตัวละทีจะหยุดศัตรูได้หลายคนพร้อมกัน",
          ),
        ),
      "jinx" to
        listOf(
          combo(
            "jnx_b_wc",
            ComboDifficulty.BASIC,
            ComboSituation.WAVE_CLEAR,
            "Fishbones Rocket Shove",
            "เปิดปืนใหญ่ Q เคลียร์เวฟ",
            listOf("Q (Rocket)", "AA", "Q (Minigun)"),
            "Switch to Fishbones Rockets for splash clear on grouped minions, then swap back to Pow-Pow Minigun to save mana.",
            "สลับเป็นปืนใหญ่ Q ยิงกระจายใส่กลุ่มมิเนียน แล้วสลับกลับปืนกลประหยัดมานา",
            "One rocket splashes all 3 caster minions once you have Runaan's or IE.",
            "สลับยิงปืนใหญ่แค่ 2–3 นัดแล้วสลับกลับปืนกล จะดันไวโดยไม่เสียมานาฟรี",
          ),
          combo(
            "jnx_b_trade",
            ComboDifficulty.BASIC,
            ComboSituation.LANE_TRADE,
            "Range Poke & Zap",
            "ตอดระยะไกลด้วยปืนใหญ่ + W",
            listOf("Q (Rocket)", "AA", "W", "Q (Minigun)"),
            "Abuse rocket bonus range when enemy ADC steps up to last-hit, follow with Zap (W) only if minion line clears.",
            "แอบยิงปืนใหญ่ใส่จังหวะแครี่อีกฝั่งเดินมาลาสช็อต ถ้าทางโล่งปา W ซ้ำ",
            "Rockets get 110% AD scaling and longer range — harass freely out of short-range ADC reach.",
            "ปืนใหญ่ระยะไกลกว่าแครี่ส่วนใหญ่ ใช้ยิงฟรี 1 นัดทุกครั้งที่ศัตรูเดินเก็บเงิน",
          ),
          combo(
            "jnx_m_solo",
            ComboDifficulty.MEDIUM,
            ComboSituation.SOLO_KILL,
            "Chompers Snare & Mega Rocket",
            "ดัก E จับขา + ยิงอัลติปิด",
            listOf("E", "W", "AA", "R"),
            "Drop Flame Chompers (E) slightly behind the enemy's retreat path, hit W slow into rocket autos, and snipe with Super Mega Death Rocket (R).",
            "วางกับดัก E ดักทางถอย ยิง W สโลว์ต้อนเข้ากับดัก แล้วปิดด้วยจรวดใหญ่ R",
            "Layer E directly under CC'd targets from your support so Chompers arm in time.",
            "รอซัพพอร์ตเปิด CC ก่อนค่อยวาง E ใต้เท้าศัตรู กับดักจะงับทันพอดี",
          ),
          combo(
            "jnx_m_escape",
            ComboDifficulty.MEDIUM,
            ComboSituation.ESCAPE_SURVIVAL,
            "Self-Chompers & Zap Kite",
            "วาง E กันหน้าตัวเอง + หนี",
            listOf("E", "W", "Flash"),
            "Drop Chompers directly at your own feet between you and the assassin, shoot W behind to slow, and kite back.",
            "วาง E ลงตรงเท้าตัวเองขวางทางตัวล้วงที่วิ่งเข้ามา แล้วยิง W สโลว์พร้อมถอยออก",
            "Jinx E stops dashes like Lee Sin Q2, Leona E, and Vi Q mid-flight!",
            "กับดัก E หยุดสกิลพุ่งกลางอากาศได้หลายตัว วางดักหน้าตัวเองไว้เสมอ",
          ),
          combo(
            "jnx_h_tf",
            ComboDifficulty.HARD,
            ComboSituation.TEAMFIGHT,
            "Get Excited Reset Snowball",
            "ปั้น Passive วิ่งเร่งกวาดไฟต์",
            listOf("E", "W", "R", "AA", "Q (Rocket)"),
            "Stay max range behind tanks, fire R onto any sub-35% target or assist to trigger Get Excited!, then rocket-splash the entire team at warp speed.",
            "ยืนระยะไกลสุดหลังแทงค์ ปล่อย R เก็บตัวเลือดต่ำหรือแอสซิสต์เพื่อจุดติด Get Excited! แล้วสาดปืนใหญ่สปีดสูงกวาดทั้งทีม",
            "Get Excited! triggers on Epic Monster kills & Towers too — use Drake/Tower resets to chase!",
            "Passive ติดแม้ช่วยตีป้อมหรือมังกรแตก ใช้จังหวะเร่งสปีดนี้สลับไล่ยิงเก็บทั้งไฟต์",
          ),
        ),
    )

  fun getCombosForChampion(champion: ChampionData): List<ChampionComboEntry> {
    SIGNATURE_COMBOS[champion.id]?.let { curated ->
      if (curated.isNotEmpty()) return curated
    }
    return synthesizeCombosFromKit(champion)
  }

  private fun synthesizeCombosFromKit(champion: ChampionData): List<ChampionComboEntry> {
    val qNameEn = champion.abilities.firstOrNull { it.slot.equals("Q", true) }?.nameEn ?: "Skill 1 (Q)"
    val qNameTh = champion.abilities.firstOrNull { it.slot.equals("Q", true) }?.nameTh ?: "สกิล 1 (Q)"
    val wNameEn = champion.abilities.firstOrNull { it.slot.equals("W", true) }?.nameEn ?: "Skill 2 (W)"
    val wNameTh = champion.abilities.firstOrNull { it.slot.equals("W", true) }?.nameTh ?: "สกิล 2 (W)"
    val eNameEn = champion.abilities.firstOrNull { it.slot.equals("E", true) }?.nameEn ?: "Skill 3 (E)"
    val eNameTh = champion.abilities.firstOrNull { it.slot.equals("E", true) }?.nameTh ?: "สกิล 3 (E)"
    val rNameEn = champion.abilities.firstOrNull { it.slot.equals("R", true) }?.nameEn ?: "Ultimate (R)"
    val rNameTh = champion.abilities.firstOrNull { it.slot.equals("R", true) }?.nameTh ?: "อัลติเมท (R)"

    val isMarksman = ChampionRole.MARKSMAN in champion.roles
    val isSupport = ChampionRole.SUPPORT in champion.roles
    val isTank = ChampionRole.TANK in champion.roles || champion.tankinessRating >= 8
    val isAssassin = ChampionRole.ASSASSIN in champion.roles
    val isMage = ChampionRole.MAGE in champion.roles
    val hasMobility = champion.mobilityRating >= 6
    val hasHighCc = champion.ccRating >= 6

    val basicWaveClear =
      when {
        isMarksman ->
          combo(
            "${champion.id}_b_wc",
            ComboDifficulty.BASIC,
            ComboSituation.WAVE_CLEAR,
            "Wave Shove & Last-Hit Loop",
            "ดันเวฟและลาสช็อตเร็ว",
            listOf("Q", "AA", "W", "AA"),
            "Sweep the wave with $qNameEn, weave AAs to trim melees first, and drop $wNameEn on casters for fast lane priority.",
            "ใช้ $qNameTh กวาดเวฟแล้วสลับตีธรรมดามิเนียนแถวหน้า ตามด้วย $wNameTh เก็บแถวหลังเพื่อชิงจังหวะดัน",
            "Track enemy support threat before stepping past river mid-line.",
            "เช็คตำแหน่งซัพพอร์ตอีกฝั่งก่อนเดินข้ามกลางเลนไปดันเวฟ",
          )
        isSupport && !isMage ->
          combo(
            "${champion.id}_b_wc",
            ComboDifficulty.BASIC,
            ComboSituation.WAVE_CLEAR,
            "Support Quest & Wave Help",
            "ช่วยเคลียร์เวฟ + เก็บเควสซัพพอร์ต",
            listOf("Q", "W", "AA"),
            "Help trim caster minions to low HP with $qNameEn/$wNameEn without stealing last-hits, freeing your ADC to recall or rotate.",
            "ช่วยตอดเลือดมิเนียนด้วย $qNameTh / $wNameTh ให้ง่ายต่อการลาสช็อตของแครี่ แล้วเดินคุมวิชั่นแม่น้ำ",
            "Use Support Quest stacks on cannon minions for maximum shared gold.",
            "เก็บสแต็กไอเทมซัพพอร์ตไว้ตีรถถังเสมอเพื่อแบ่งทองคุ้มที่สุด",
          )
        else ->
          combo(
            "${champion.id}_b_wc",
            ComboDifficulty.BASIC,
            ComboSituation.WAVE_CLEAR,
            "Standard AoE Wave Clear",
            "คอมโบเคลียร์เวฟมาตรฐาน",
            listOf("Q", "W", "AA"),
            "Line up $qNameEn through both front and back creeps, then follow with $wNameEn to finish the wave quickly before roaming.",
            "วาง $qNameTh ให้โดนมิเนียนทั้งแถวหน้า-หลัง ตามด้วย $wNameTh เพื่อดันเวฟเข้าป้อมไวก่อนเดินแก๊ง",
            "Save $eNameEn whenever possible so you still have safety or counter-engage if ganked.",
            "พยายามเก็บ $eNameTh ไว้เสมอเผื่อป่าฝั่งตรงข้ามโผล่มาแก๊งระหว่างดันเวฟ",
          )
      }

    val basicTrade =
      when {
        isMarksman ->
          combo(
            "${champion.id}_b_trade",
            ComboDifficulty.BASIC,
            ComboSituation.LANE_TRADE,
            "Space & Burst Poke",
            "เทรดรักษาระยะในเลน",
            listOf("AA", "Q", "AA", "W"),
            "Punish last-hits with AA -> $qNameEn -> AA animation weave, layering $wNameEn before backing out of counter range.",
            "ดักยิงตอนอีกฝั่งลาสช็อตด้วย AA -> $qNameTh -> AA แล้วกด $wNameTh ทิ้งท้ายก่อนถอยรักษาระยะ",
            "Keep maximum auto range; step back during ability recovery animations.",
            "รักษาระยะตีไกลสุดเสมอ อย่าเดินชิดเกินจังหวะสกิลกำลังร่าย",
          )
        isTank || hasHighCc ->
          combo(
            "${champion.id}_b_trade",
            ComboDifficulty.BASIC,
            ComboSituation.LANE_TRADE,
            "CC Lock & Short Trade",
            "เปิด CC เทรดสั้นแล้วถอย",
            listOf("E", "Q", "W", "AA"),
            "Trigger engage/CC with $eNameEn into guaranteed $qNameEn + $wNameEn + AA, then disengage before extended DPS kicks in.",
            "เปิดด้วย $eNameTh จับจังหวะ แล้วยัด $qNameTh + $wNameTh + AA เสร็จแล้วถอยออกก่อนอีกฝั่งได้ยิงสวนยาว",
            "Trade around Bone Plating / Passive shield cooldowns for zero-loss skirmishes.",
            "เข้าเทรดเฉพาะตอนรูน Bone Plating หรือโล่พร้อมใช้ จะได้เปรียบแทบทุกรอบ",
          )
        else ->
          combo(
            "${champion.id}_b_trade",
            ComboDifficulty.BASIC,
            ComboSituation.LANE_TRADE,
            "Core 3-Hit Keystone Trade",
            "คอมโบเทรด 3 ฮิตปิดรูนหลัก",
            listOf("Q", "W", "AA"),
            "Poke with $qNameEn, confirm follow-up with $wNameEn, and weave 1 Auto Attack to trigger Electrocute/Conqueror stacks.",
            "เปิดด้วย $qNameTh ต่อด้วย $wNameTh และตบธรรมดา 1 ทีเพื่อปิดดาเมจรูนหลักหรือสะสมสแต็ก",
            "Clip the wave simultaneously to build lane priority while harassing.",
            "เล็งให้สกิลโดนทั้งมิเนียนและศัตรูพร้อมกัน จะคุมเลนได้ง่ายขึ้น",
          )
      }

    val mediumSoloKill =
      when {
        isAssassin || champion.burstRating >= 7 ->
          combo(
            "${champion.id}_m_solo",
            ComboDifficulty.MEDIUM,
            ComboSituation.SOLO_KILL,
            "Full All-In Burst Execution",
            "คอมโบเบิร์สปิดคิลเดี่ยว 100-0",
            listOf("E", "Q", "R", "W", "AA"),
            "Close distance with $eNameEn, weave $qNameEn into $rNameEn burst, and finish with $wNameEn + AA before they reach tower.",
            "เข้าประชิดด้วย $eNameTh ต่อด้วย $qNameTh + $rNameTh เบิร์สชุดใหญ่ แล้วย้ำด้วย $wNameTh + AA ปิดคิล",
            "Confirm enemy Stasis / Flash cooldown first so your $rNameEn isn't negated.",
            "เช็ค Stasis หรือ Flash ศัตรูก่อนทุ่ม $rNameTh จะได้ปิดคิลแบบชัวร์ไม่เสียอัลติฟรี",
          )
        isTank ->
          combo(
            "${champion.id}_m_solo",
            ComboDifficulty.MEDIUM,
            ComboSituation.SOLO_KILL,
            "CC Chain Lockdown Kill",
            "คอมโบจับล็อกต่อเนื่องให้เพื่อนปิด",
            listOf("R", "AA", "Q", "E", "W"),
            "Initiate with $rNameEn, weave AA + $qNameEn during knockup/stun window, then chain $eNameEn + $wNameEn to prevent any escape.",
            "เปิดฉากด้วย $rNameTh ต่อด้วยตีธรรมดาและ $qNameTh ก่อนหมดสถานะ CC แล้วต่อ $eNameTh + $wNameTh ล็อกยาว",
            "Wait 0.2s between CCs so durations don't overlap needlessly.",
            "เว้นจังหวะต่อ CC ประมาณ 0.2 วินาทีไม่ให้ทับกัน จะจับศัตรูนิ่งอยู่กับที่ได้นานสุด",
          )
        else ->
          combo(
            "${champion.id}_m_solo",
            ComboDifficulty.MEDIUM,
            ComboSituation.SOLO_KILL,
            "Ult Spike Duel Combo",
            "คอมโบปิดคิลจังหวะอัลติ",
            listOf("Q", "W", "R", "E", "AA"),
            "Whittle enemy to ~65% HP, lock in with $qNameEn -> $wNameEn -> $rNameEn, then use $eNameEn + AA to chase their retreat.",
            "ตอดเลือดลงราว 65% แล้วจัดชุด $qNameTh -> $wNameTh -> $rNameTh ก่อนใช้ $eNameTh + AA ไล่ปิด",
            "Hold $eNameEn slightly after $rNameEn to track enemy Flash or dash direction.",
            "เก็บ $eNameTh ไว้ตามจังหวะที่ศัตรู Flash หรือพุ่งหนีหลังโดน $rNameTh",
          )
      }

    val mediumEscape =
      when {
        hasMobility ->
          combo(
            "${champion.id}_m_escape",
            ComboDifficulty.MEDIUM,
            ComboSituation.ESCAPE_SURVIVAL,
            "Terrain Kite & Escape",
            "สโลว์ตัดจังหวะ + พุ่งหนีข้ามมุม",
            listOf("Q", "W", "E", "Flash"),
            "Slow or zone pursuers with $qNameEn/$wNameEn, then angle $eNameEn across walls or toward your turret.",
            "ปล่อย $qNameTh / $wNameTh สกัดจังหวะคนไล่ แล้วหันมุมใช้ $eNameTh พุ่งข้ามกำแพงหรือถอยเข้าป้อม",
            "Wait for enemy gap-closers to be spent before burning Flash.",
            "อดทนให้ตัวไล่ใช้สกิลพุ่งมาก่อนค่อยสวน Flash จะรอดได้แน่นอนกว่า",
          )
        else ->
          combo(
            "${champion.id}_m_escape",
            ComboDifficulty.MEDIUM,
            ComboSituation.ESCAPE_SURVIVAL,
            "Defensive Self-Peel Kite",
            "สกัดตัวล้วงเอาตัวรอดในไฟต์",
            listOf("E", "W", "Q", "Flash"),
            "Drop $eNameEn/$wNameEn directly between yourself and divers, counter with $qNameEn, and Flash laterally over terrain if trapped.",
            "วาง $eNameTh / $wNameTh กันหน้าตัวเองจากตัวล้วง ต่อด้วย $qNameTh แล้ว Flash ออกด้านข้างหรือข้ามกำแพงหากจวนตัว",
            "Buy Stasis or Quicksilver Enchant against heavy dive/pick comps.",
            "ออก Stasis หรือ QSS ไว้กดรับจังหวะล้วง จะช่วยให้รอดและปั้นดาเมจสวนได้",
          )
      }

    val hardTeamfight =
      combo(
        "${champion.id}_h_tf",
        ComboDifficulty.HARD,
        ComboSituation.TEAMFIGHT,
        "Flash-Engage Teamfight Peak",
        "คอมโบเปิด/ปิดเกม Teamfight",
        if (isMarksman) listOf("W", "Q", "AA", "R", "E", "Flash")
        else listOf("Flash", "R", "Q", "W", "E", "AA"),
        if (isMarksman) {
          "Hold $eNameEn/Flash purely to dodge primary engage, melt front-to-back with $qNameEn/$wNameEn + AA, and snipe low carries with $rNameEn."
        } else {
          "Wait for grouped targets in objective fights, chain Flash into $rNameEn -> $qNameEn -> $wNameEn across multiple threats, and reposition with $eNameEn."
        },
        if (isMarksman) {
          "เก็บ $eNameTh และ Flash ไว้หลบตัวล้วงเท่านั้น ยิงละลายแนวหน้าด้วย $qNameTh/$wNameTh + AA แล้วใช้ $rNameTh เก็บตัวหลัง"
        } else {
          "ดักจังหวะศัตรูยืนรวมกันหน้ามังกร/บารอน กด Flash ต่อ $rNameTh -> $qNameTh -> $wNameTh รวบหลายเป้าหมายแล้วใช้ $eNameTh จัดระยะ"
        },
        "Coordinate with your frontline CC timer so your burst/CC hits without overlap.",
        "ซิงค์จังหวะกับ CC ของแนวหน้าในทีมให้ดี เพื่อไม่ให้สกิลทับกันและปิดแครี่ให้ตายในชุดเดียว",
      )

    return listOf(basicWaveClear, basicTrade, mediumSoloKill, mediumEscape, hardTeamfight)
  }

  private data class CuratedSynergyRule(
    val partnerId: String,
    val score: Int,
    val tagEn: String,
    val tagTh: String,
    val comboLabel: String,
    val reasonEn: String,
    val reasonTh: String,
  )

  private val CURATED_SYNERGIES: Map<String, List<CuratedSynergyRule>> =
    mapOf(
      "yasuo" to
        listOf(
          CuratedSynergyRule("malphite", 99, "Instant Knockup", "งัดหมู่การันตี R", "Malphite R → Yasuo R", "Malphite's Unstoppable Force guarantees multi-target R for Yasuo from fog.", "อัลติ Malphite งัดลอยหมู่ทันที เปิดโอกาสให้ Yasuo ต่อ R เข้าแนวหลังแบบหลบไม่ได้"),
          CuratedSynergyRule("yone", 96, "Wind Brothers Chain", "พี่น้องสายลมต่อ CC", "Yone R/Q3 → Yasuo R", "Double AoE knockups + mixed crit/true burst melts front-to-back.", "งัดลอยต่อกันได้ 2 ต่อทั้ง Yone Q3/R และ Yasuo R ละลายทีมไฟต์ไวสุด"),
          CuratedSynergyRule("alistar", 95, "Frontline Knockup", "ชนงัด Q+W ต่อ R", "Alistar W-Q → Yasuo R", "Alistar W-Q engages reliably every 12s to feed Yasuo Ult resets.", "Alistar คอมโบ W-Q งัดเปิดได้บ่อยมาก ช่วยป้อนอัลติให้ Yasuo ทุกไฟต์"),
          CuratedSynergyRule("wukong", 95, "Double Cyclone", "หมุนงัดหมู่ 2 รอบ", "Wukong R → Yasuo R", "Wukong clone + Cyclone spins lift entire backlines for easy Ult chains.", "Wukong แยกร่างปั่น R ยกลอย 2 รอบให้ Yasuo เลือกต่อ R ได้ง่าย"),
          CuratedSynergyRule("nautilus", 93, "Point-Click Knockup", "ล็อก R งัดการันตี", "Nautilus R → Yasuo R", "Point-and-click Depth Charge removes all juking against carry targets.", "อัลติ Nautilus ล็อกเป้าวิ่งงัดแครี่แน่นอน ทำให้ Yasuo กด R ถึงตัวชัวร์"),
        ),
      "xayah" to
        listOf(
          CuratedSynergyRule("rakan", 99, "Lovers Duo Bonus", "คู่รักบัฟสกิลเฉพาะ", "Rakan R-W → Xayah W+E", "Shared recall, extended E dash range, and dual W attack-speed buffs in Dragon Lane.", "ได้บัฟพิเศษคู่รัก: Rakan พุ่ง E ไกลขึ้น, W Xayah บัฟสปีดให้ทั้งคู่ และวาร์ปกลับพร้อมกันได้"),
          CuratedSynergyRule("nautilus", 94, "Lineup Snare Setup", "ดึงหยุดให้วางขนนก", "Nautilus Q/R → Xayah Q-E", "Long CC locks targets directly inside Xayah's feather recall line.", "Nautilus ดึงแล้วจับขาอยู่กับที่ ทำให้ Xayah ปักขนนกแล้วกระชาก E รวบแม่น 100%"),
          CuratedSynergyRule("thresh", 93, "Lantern + Hook", "โคมไฟช่วยหนี + ดึง", "Thresh Q/E → Xayah Q-E", "Lantern saves Xayah between R cooldowns while Flay sets up 3-feather roots.", "โคมไฟ Thresh ช่วยดึง Xayah หนีช่วงไม่มีอัลติ และ E กวาดช่วยให้จับขาต่อได้ง่าย"),
          CuratedSynergyRule("braum", 92, "Wall + Feather Lockdown", "โล่ตั้งรับ + สตั๊น 2 ต่อ", "Xayah W → Braum P → E", "Fast W autos stack Braum Passive instantaneously to trap divers.", "สปีดตีจาก W ของ Xayah ปิดสแต็ก Passive Braum ได้ในพริบตาแล้วตาม E จับซ้ำ"),
        ),
      "rakan" to
        listOf(
          CuratedSynergyRule("xayah", 99, "Lovers Duo Bonus", "คู่รักบัฟสกิลเฉพาะ", "Rakan E-W → Xayah W+E", "Massive E range extension to Xayah plus shared W Attack Speed & recall.", "E พุ่งหา Xayah ได้ไกลขึ้นเกือบเท่าตัว พร้อมแชร์บัฟ W ตีไวและวาร์ปกลับบ้านคู่กัน"),
          CuratedSynergyRule("samira", 95, "All-In Dive Buddy", "พุ่งเปิดคู่สายโถม", "Rakan R-W → Samira E-R", "Rakan clusters enemies with R-W so Samira hits 5-man Inferno Trigger safely.", "Rakan ลากรวบศัตรูเป็นก้อนด้วย R-W ให้ Samira พุ่งเข้าไปปั่น R เก็บเรียบ"),
          CuratedSynergyRule("kaisa", 94, "Instant Plasma Dive", "ซ้อนสแต็กพลาสมาให้ R", "Rakan W → Kai'Sa R", "Rakan's W+Q adds Plasma stacks immediately so Kai'Sa Ults straight onto the carry.", "Rakan เปิด W ใส่แครี่เพิ่มสแต็กให้ Kai'Sa กดอัลติตามมาล้วงพร้อมกันได้ทันที"),
          CuratedSynergyRule("yasuo", 94, "W Knockup → Ult", "W งัดหมู่ต่อ Yasuo R", "Rakan R-W → Yasuo R", "Grand Entrance sets up grouped knockups for Yasuo Ult & Windwall cover.", "Rakan ใช้ R-W รวบงัดให้ Yasuo ต่ออัลติทันทีกลางวงไฟต์"),
        ),
      "lucian" to
        listOf(
          CuratedSynergyRule("nami", 98, "Iconic Electrocute Lane", "คู่หูบัฟน้ำเบิร์สโหด", "Nami E → Lucian E-AA-Q", "Nami Tidecaller's Blessing (E) procs instantly on Lucian's passive double-shot for burst & slow.", "Nami ใส่บัฟ E ให้ Lucian ยิงเบิ้ล 2 นัดกระตุกดาเมจ+สโลว์ในเสี้ยววินาที ชนะเลนขาด"),
          CuratedSynergyRule("braum", 95, "Instant Passive Stun", "ยิงเบิ้ลปิดสตั๊นทันที", "Braum Q → Lucian E-AA-Q-AA", "Lucian double-shots stack Braum's Concussive Blows faster than any ADC.", "ยิงเบิ้ล Passive ของ Lucian ปั่นสแต็กสตั๊น Braum ครบ 4 ชั้นเร็วที่สุดในเกม"),
          CuratedSynergyRule("lulu", 93, "Hyper Skirmisher", "เร่งสปีด-โล่โถมเลน", "Lulu W/E → Lucian E-R", "Lulu shields & speeds Lucian during Culling so he kites and melts cleanly.", "Lulu กางโล่และเร่งสปีดช่วยให้ Lucian พุ่งไล่ยิงและสาด R ได้เต็มแม็กซ์"),
          CuratedSynergyRule("thresh", 91, "Aggressive 2v2 Pick", "ดึงเปิด + โคมไฟ", "Thresh Q → Lucian E-Q-R", "Early hook catches let Lucian unleash full passive burst at level 2–3.", "Thresh เกี่ยวเปิดเลเวล 2–3 ป้อนให้ Lucian ยิงคอมโบเบิร์สเก็บคิลต้นเกม"),
        ),
      "samira" to
        listOf(
          CuratedSynergyRule("nautilus", 98, "Zero-Escape CC Chain", "ล็อก CC ให้ปั่นเกรด S", "Nautilus Q-R → Samira P-R", "Every Nautilus CC extends via Samira's knockup Passive, feeding free Style & Ult.", "CC ของ Nautilus ทุกสกิลเชื่อม Passive ยิงต่อของ Samira ทำให้ขึ้นเกรด S แบบไม่ต้องเสี่ยง"),
          CuratedSynergyRule("leona", 97, "Sunlight Dive Duo", "สายพุ่ง 2 ต่อปั่นอัลติ", "Leona E-Q-R → Samira E-R", "Leona tanks turret and chains 3 stuns while Samira dives in with Inferno Trigger.", "Leona ชนป้อมล็อกยาว 3 วินาที เปิดทางให้ Samira พุ่งเข้าไปหมุน R ฟรีๆ"),
          CuratedSynergyRule("alistar", 96, "Tower-Dive Tank", "งัดลอย+แทงค์ป้อมสบาย", "Alistar W-Q → Samira P-E-R", "Knockup triggers Samira Passive and Alistar's Ult soaks focus during R.", "งัด W-Q กระตุก Passive Samira ได้ทันที และ Alistar อัลติรับตีนในวงไฟต์ให้"),
          CuratedSynergyRule("rakan", 94, "Grouped Backline R", "รวบเป็นก้อนให้ R เต็มวง", "Rakan R-W → Samira E-R", "Rakan's fast Charm pulls carries tight together for maximum Samira Ult hits.", "Rakan ลากรวบแครี่ให้ยืนชิดกัน ช่วยให้ Samira ปั่น R โดนครบทุกคน"),
        ),
      "jinx" to
        listOf(
          CuratedSynergyRule("lulu", 98, "Hypercarry Peel & Polymorph", "ซัพพอร์ตปั้นไฮเปอร์แครี่", "Lulu R/W → Jinx W-Q", "Lulu Polymorph stops assassins while Wild Growth + Pix let Jinx trigger Get Excited!.", "Lulu เสกแอดซาซินเป็นกระต่าย + R ขยายร่าง ให้ Jinx ยืนสาดปืนใหญ่เก็บรีเซ็ตสบาย"),
          CuratedSynergyRule("thresh", 96, "Lantern Safety Net", "โคมไฟชุบชีวิตตัวไม่มีพุ่ง", "Thresh W/E → Jinx E-Q", "Lantern solves Jinx's lack of dashes, and Flay/Hook hold targets inside Flame Chompers.", "โคมไฟช่วยเติมช่องว่างที่ Jinx ไม่มีสกิลหนี และ Q/E ดึงศัตรูเข้ากับดัก E"),
          CuratedSynergyRule("braum", 94, "Shield Wall + AoE Rockets", "กำแพงโล่+ยิงจรวดหลังป้อม", "Braum E/R → Jinx Q-E", "Braum blocks burst projectiles and slows whole teams for Jinx's splash rockets.", "Braum กางโล่บังสกิลให้ Jinx ยืนยิงปืนใหญ่สาด Splash ใส่แนวหน้า-หลังพร้อมกัน"),
          CuratedSynergyRule("milio", 94, "Extreme Rocket Range", "เพิ่มระยะปืนใหญ่สุดขอบจอ", "Milio W/R → Jinx Q+AA", "Extra attack range + AoE cleanse let Jinx shred from off-screen safety.", "Milio เพิ่มระยะโจมตีให้ปืนใหญ่ Jinx ยิงไกลสุดจอ พร้อม R ล้าง CC ให้ทั้งทีม"),
        ),
      "miss_fortune" to
        listOf(
          CuratedSynergyRule("amumu", 98, "Curse + Bullet Time", "พันธนาการหมู่ป้อน Bullet Time", "Amumu R → MF E-R", "Amumu's huge AoE stun traps 4–5 enemies inside the entire Bullet Time cone.", "อัลติ Amumu มัดทั้งทีมให้อยู่เฉยๆ รับกระสุน Bullet Time ของ MF เต็มๆ 3 วินาที"),
          CuratedSynergyRule("leona", 96, "Solar Flare Lock", "R แสงอาทิตย์หยุดหมู่", "Leona R → MF E-R", "Long-range Solar Flare sets up instant E slow + R wave-melt.", "Leona ปา R สตั๊นระยะไกลเปิดให้ MF วาง E-R สาดกระสุนละลายจากแนวหลังทันที"),
          CuratedSynergyRule("jarvan_iv", 95, "Cataclysm Cage Combo", "ขังกรงดินสาดกระสุน", "J4 R → MF E-R", "Jarvan's Cataclysm traps dashless carries inside the exact width of Bullet Time.", "กรงอัลติของ Jarvan ขังศัตรูให้อยู่ในกรวยกระสุน R ของ MF พอดีเป๊ะ"),
          CuratedSynergyRule("seraphine", 94, "Encore + Double Slow", "สโลว์ต่อ R Charm เต็มหลอด", "MF E → Seraphine R → MF R", "MF's Make It Rain guarantees Seraphine E root, chaining Encore into Bullet Time.", "MF ปา E สโลว์ช่วยให้ Seraphine จับ+R เสน่ห์แม่น แล้วปิดด้วย Bullet Time"),
        ),
      "katarina" to
        listOf(
          CuratedSynergyRule("amumu", 97, "CC Absorb + Reset", "รับสกิลล็อก+มัดให้หมุน R", "Amumu R → Katarina E-W-Q-R", "Amumu forces enemy stuns on himself and locks the backline so Katarina spins undisturbed.", "Amumu ล่อสกิลสตั๊นและมัดทีมตรงข้ามให้นิ่ง ให้ Katarina โดดเข้าไปปั่น R เก็บรีเซ็ตสบาย"),
          CuratedSynergyRule("malphite", 95, "Engage Distraction", "ชนงัดเปิดให้โดดตามเก็บ", "Malphite R → Katarina E-R", "Malphite bursts squishies to reset threshold immediately for Katarina.", "Malphite อัลติตัดเลือดแครี่ให้ต่ำพอ ทำให้ Katarina โดด E ปิดคิลรีเซ็ตแรกได้ทันที"),
          CuratedSynergyRule("galio", 95, "Hero's Entrance Dive Cover", "กระโดด R กันดาเมจ+ยั่วยุ", "Katarina E-W → Galio R → Kat R", "Galio Ults onto Katarina mid-dive for magic shield + AoE Taunt.", "Galio กระโดด R ลงหัว Katarina กางโล่กันตายพร้อม Taunt ให้ Katarina ปั่นอัลติฟรี"),
          CuratedSynergyRule("yuumi", 93, "Speed + Heal Reset", "เกาะติดฮีล+เร่งสปีดรีเซ็ต", "Yuumi R/E → Katarina E-R", "Yuumi anchors onto Katarina with movespeed, heal, and root waves during resets.", "Yuumi เกาะหัวเร่งสปีด+ฮีล+ปล่อยคลื่นล็อก ทำให้ Katarina รำรีเซ็ตไม่ตายกลางวง"),
        ),
      "ori" to emptyList(),
    )

  private fun curatedPartnersFor(
    champion: ChampionData,
    byId: Map<String, ChampionData>,
  ): List<TeamSynergyPartner> {
    val rules = CURATED_SYNERGIES[champion.id].orEmpty()
    if (rules.isNotEmpty()) {
      return rules.mapNotNull { rule ->
        val partner = byId[rule.partnerId] ?: return@mapNotNull null
        TeamSynergyPartner(
          champion = partner,
          synergyScore = rule.score,
          tagEn = rule.tagEn,
          tagTh = rule.tagTh,
          comboSequenceLabel = rule.comboLabel,
          reasonEn = rule.reasonEn,
          reasonTh = rule.reasonTh,
        )
      }
    }
    return emptyList()
  }

  fun getTeamSynergies(
    champion: ChampionData,
    allChampions: List<ChampionData>,
    limit: Int = 5,
  ): List<TeamSynergyPartner> {
    if (allChampions.isEmpty()) return emptyList()
    val byId = allChampions.associateBy { it.id }

    val curated = curatedPartnersFor(champion, byId)
    val curatedIds = curated.map { it.champion.id }.toSet()

    val myPrimaryLane = champion.lanes.firstOrNull()
    val isAdc = ChampionRole.MARKSMAN in champion.roles || myPrimaryLane == Lane.DRAGON
    val isSupport = ChampionRole.SUPPORT in champion.roles || myPrimaryLane == Lane.SUPPORT
    val isEngageTank =
      (ChampionRole.TANK in champion.roles || champion.tankinessRating >= 7) && champion.ccRating >= 7
    val isAssassin = ChampionRole.ASSASSIN in champion.roles
    val isMage = ChampionRole.MAGE in champion.roles

    val dynamicCandidates =
      allChampions
        .asSequence()
        .filter { it.id != champion.id && it.id !in curatedIds }
        .mapNotNull { cand ->
          val candPrimaryLane = cand.lanes.firstOrNull()
          // Prefer distinct roles / non-overlapping main lane unless Jungler/Support flex
          if (candPrimaryLane == myPrimaryLane && candPrimaryLane != Lane.JUNGLE) {
            return@mapNotNull null
          }

          var score = 70
          var tagEn = "Balanced Comp"
          var tagTh = "คอมโบสมดุลทีม"
          var seqLabel = "${cand.nameEn} CC → ${champion.nameEn} Burst"
          var reasonEn = ""
          var reasonTh = ""

          val candIsTankCc = (ChampionRole.TANK in cand.roles || cand.tankinessRating >= 7) && cand.ccRating >= 7
          val candIsEnchanter =
            ChampionRole.SUPPORT in cand.roles && cand.ccRating <= 6 && cand.tankinessRating <= 6
          val candIsAdc = ChampionRole.MARKSMAN in cand.roles
          val candIsDive = ChampionRole.ASSASSIN in cand.roles || (ChampionRole.FIGHTER in cand.roles && cand.mobilityRating >= 7)

          // Damage split bonus (AD + AP)
          if (champion.damageType != cand.damageType &&
            champion.damageType != DamageType.HYBRID &&
            cand.damageType != DamageType.HYBRID
          ) {
            score += 6
          }

          when {
            isAdc && (cand.id in listOf("lulu", "nami", "thresh", "nautilus", "leona", "braum", "milio", "yuumi", "rakan")) -> {
              score += 18
              if (cand.ccRating >= 7) {
                tagEn = "CC Peel & Setup"
                tagTh = "แทงค์ล็อกให้ยิงฟรี"
                seqLabel = "${cand.nameEn} CC → ${champion.nameEn} DPS"
                reasonEn = "${cand.nameEn} absorbs frontline threat and locks targets in place so ${champion.nameEn} deals safe sustained DPS."
                reasonTh = "${cand.nameTh} รับดาเมจและล็อกเป้าให้อยู่กับที่ เปิดทางให้ ${champion.nameTh} ยืนยิงต่อเนื่องสบาย"
              } else {
                tagEn = "Enchanter Peel & Buff"
                tagTh = "ซัพพอร์ตบัฟ+กันล้วง"
                seqLabel = "${cand.nameEn} Shield/Buff → ${champion.nameEn}"
                reasonEn = "${cand.nameEn} boosts attack speed/shields and disengages assassins trying to dive ${champion.nameEn}."
                reasonTh = "${cand.nameTh} ช่วยเสริมโล่ เร่งความเร็ว และปัดตัวล้วงให้ ${champion.nameTh} รอดพ้นทีมไฟต์"
              }
            }
            isAdc && candIsTankCc -> {
              score += 14
              tagEn = "Frontline Lock"
              tagTh = "แนวหน้าเปิด+กันให้"
              seqLabel = "${cand.nameEn} Engage → ${champion.nameEn}"
              reasonEn = "Strong frontline CC absorbs engage cooldowns while ${champion.nameEn} melts enemies front-to-back."
              reasonTh = "${cand.nameTh} ดึงความสนใจและจับล็อกแนวหน้า ให้ ${champion.nameTh} ละลายทีละตัว"
            }
            isSupport && candIsAdc -> {
              score += 15
              tagEn = "Dragon Lane Partner"
              tagTh = "คู่เลนดราก้อนเข้าขา"
              seqLabel = "${champion.nameEn} Setup → ${cand.nameEn} DPS"
              reasonEn = "${champion.nameEn}'s utility/CC directly covers ${cand.nameEn}'s positioning and amplifies 2v2 lane kills."
              reasonTh = "สกิลของ ${champion.nameTh} ช่วยปิดจุดอ่อนการยืนตำแหน่งและเร่งดาเมจ 2v2 คู่กับ ${cand.nameTh} ในเลนมังกร"
            }
            (isAssassin || isMage) && candIsTankCc -> {
              score += 16
              tagEn = "Wombo CC Setup"
              tagTh = "เปิดไฟต์ป้อนเบิร์ส"
              seqLabel = "${cand.nameEn} R/CC → ${champion.nameEn} Full Combo"
              reasonEn = "${cand.nameEn}'s crowd control eats enemy QSS/Stasis and holds carries inside ${champion.nameEn}'s full combo."
              reasonTh = "CC ของ ${cand.nameTh} บังคับให้อีกฝั่งเสีย QSS/Stasis ก่อน แล้วเปิดช่องให้ ${champion.nameTh} ยัดคอมโบเต็มชุด"
            }
            isEngageTank && (cand.burstRating >= 8 || cand.dpsRating >= 8) -> {
              score += 16
              tagEn = "AoE Follow-Up Carry"
              tagTh = "ตัวดาเมจตามไฟต์สุดแรง"
              seqLabel = "${champion.nameEn} Engage → ${cand.nameEn} R"
              reasonEn = "Your CC engage groups enemies tightly so ${champion.nameEn} lands massive AoE damage before targets recover."
              reasonTh = "จังหวะเปิด CC ของ ${champion.nameTh} ช่วยรวบศัตรูเป็นก้อนให้ ${champion.nameTh} ตามซ้ำปิดไฟต์ทันที"
            }
            isAssassin && (cand.id in listOf("shen", "galio", "tf", "yuumi", "ori", "orianna", "lulu")) -> {
              score += 17
              tagEn = "Global Dive Cover"
              tagTh = "อัลติซ้อนจังหวะล้วง"
              seqLabel = "${champion.nameEn} Dive → ${cand.nameEn} R"
              reasonEn = "${cand.nameEn} shields and follows ${champion.nameEn}'s deep backline dive for unstoppable 2-man assassinations."
              reasonTh = "${cand.nameTh} ซ้อนโล่หรือกระโดดอัลติตามจังหวะพุ่งล้วงของ ${champion.nameTh} ปิดแครี่แบบ 2v1 ไร้ทางรอด"
            }
            cand.id == "yasuo" && champion.ccRating >= 7 -> {
              score += 18
              tagEn = "Knockup Ult Chain"
              tagTh = "งัดลอยเชื่อม Yasuo R"
              seqLabel = "${champion.nameEn} CC → Yasuo R"
              reasonEn = "${champion.nameEn}'s airborne/CC opens Last Breath instantly for double backline pressure."
              reasonTh = "สกิลงัดหรือเปิดของ ${champion.nameTh} ช่วยป้อนอัลติ Last Breath ให้ Yasuo ต่อคอมโบได้ทันที"
            }
            isMage && cand.ccRating >= 7 && cand.id in listOf("jarvan_iv", "amumu", "sejuani", "maokai", "vi", "maokai") -> {
              score += 15
              tagEn = "Lockdown Burst Chain"
              tagTh = "ล็อกเป้าต่อเบิร์สเวท"
              seqLabel = "${cand.nameEn} Lock → ${champion.nameEn} Combo"
              reasonEn = "Guaranteed Jungle/Frontline CC ensures ${champion.nameEn}'s skillshots hit cleanly."
              reasonTh = "ตัวเปิดอย่าง ${cand.nameTh} ช่วยหยุดศัตรูให้นิ่ง ทำให้สกิลช็อตของ ${champion.nameTh} ยัดโดน 100%"
            }
            isSupport && candIsDive && cand.burstRating >= 7 -> {
              score += 13
              tagEn = "Skirmish Roam Duo"
              tagTh = "เดินแก๊งโรมคู่ป่า/มิด"
              seqLabel = "${champion.nameEn} + ${cand.nameEn} Roam"
              reasonEn = "Strong river/objective skirmish duo; roam mid or jungle together on timing."
              reasonTh = "เดินแก๊งแม่น้ำและคุมมังกรคู่กับ ${cand.nameTh} ได้ดุดัน ปิดเกมเร็วได้ตั้งแต่ต้นเกม"
            }
            else -> {
              if (cand.ccRating >= 7 && champion.burstRating >= 7) {
                score += 8
                tagEn = "CC + Burst Pairing"
                tagTh = "คอมโบ CC + เบิร์ส"
                seqLabel = "${cand.nameEn} CC → ${champion.nameEn}"
                reasonEn = "${cand.nameEn} provides reliable setup to convert ${champion.nameEn}'s burst reliably."
                reasonTh = "${cand.nameTh} ช่วยเปิดจังหวะ CC ให้ ${champion.nameTh} ปิดคิลในทีมไฟต์ง่ายขึ้น"
              } else {
                return@mapNotNull null
              }
            }
          }

          // Bonus for strong meta tier synergy
          score += when (cand.tierRank.sortOrder) {
            0 -> 4
            1 -> 2
            else -> 0
          }
          val boundedScore = score.coerceIn(72, 97)
          TeamSynergyPartner(
            champion = cand,
            synergyScore = boundedScore,
            tagEn = tagEn,
            tagTh = tagTh,
            comboSequenceLabel = seqLabel,
            reasonEn = reasonEn,
            reasonTh = reasonTh,
          )
        }
        .sortedWith(
          compareByDescending<TeamSynergyPartner> { it.synergyScore }
            .thenBy { it.champion.tierRank.sortOrder }
        )
        .toList()

    val combined = (curated + dynamicCandidates).distinctBy { it.champion.id }.take(limit)
    if (combined.size >= 3) return combined

    // Safety fallback if pool is small (e.g. partial unit test list)
    val fallback =
      allChampions
        .filter { it.id != champion.id && it.id !in combined.map { c -> c.champion.id } }
        .sortedBy { it.tierRank.sortOrder }
        .take(limit - combined.size)
        .map { cand ->
          val dmgBalance =
            if (champion.damageType != cand.damageType) 86 else 80
          TeamSynergyPartner(
            champion = cand,
            synergyScore = dmgBalance.roundToIntOrZero(),
            tagEn = "Team Balance",
            tagTh = "เสริมสมดุลทีม",
            comboSequenceLabel = "${champion.nameEn} + ${cand.nameEn}",
            reasonEn = "Complements ${champion.nameEn}'s ${champion.damageType.labelEn} output in skirmishes and objective fights.",
            reasonTh = "ช่วยเสริมจังหวะทีมไฟต์และสมดุลดาเมจคู่กับ ${champion.nameTh} ในช่วงชิงมังกร-บารอน",
          )
        }
    return combined + fallback
  }

  private fun Int.roundToIntOrZero(): Int = this
}
