package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.ui.theme.AdvantageEmerald
import com.example.ui.theme.LocalRiftPalette

private data class MiniTutorialStep(
  val icon: ImageVector,
  val titleEn: String,
  val titleTh: String,
  val subtitleEn: String,
  val subtitleTh: String,
  val bulletsEn: List<String>,
  val bulletsTh: List<String>,
  val tipEn: String,
  val tipTh: String,
)

private val MINI_TUTORIAL_STEPS =
  listOf(
    MiniTutorialStep(
      icon = Icons.Default.PictureInPictureAlt,
      titleEn = "1. Launch Mini-Window Beside Wild Rift",
      titleTh = "1. เปิดโหมดหน้าต่างเล็กคู่กับเกม Wild Rift",
      subtitleEn = "Keep counter builds, 1v5 defense, and combos visible without tabbing away.",
      subtitleTh = "ดูของแก้ทาง คอมโบ และแผนรับมือได้ทันทีโดยไม่ต้องสลับหน้าจอจนหลุดโฟกัส",
      bulletsEn =
        listOf(
          "Toggle Mini-Window via the top-right PiP icon or in Settings.",
          "Open Android Recents → tap WR-AI icon → choose Pop-up View (Floating) or Split-Screen.",
          "Resize the floating corner above the river / scoreboard area so the minimap stays clear.",
        ),
      bulletsTh =
        listOf(
          "กดไอคอนหน้าต่างเล็ก (PiP) ที่มุมขวาบนของแอพ หรือสลับเปิดได้ในหน้า Settings",
          "เปิดหน้า Recents ของมือถือ → แตะไอคอนแอพ → เลือก Pop-up View (หน้าต่างลอย) หรือ Split-Screen",
          "ลากมุมหน้าต่างลอยวางบริเวณมุมขวาบนหรือกึ่งกลางบน เพื่อไม่ให้บัง Minimap และปุ่มสกิล",
        ),
      tipEn = "Tip: Works during Champ Select, Loading Screen, or when recalling to base.",
      tipTh = "ทริค: เหมาะมากช่วง Draft เลือกตัว โหลดเข้าเกม และช่วงวาร์ปกลับบ้านซื้อไอเทม",
    ),
    MiniTutorialStep(
      icon = Icons.Default.CameraAlt,
      titleEn = "2. Fast Live-Match Scanning & Compact Icons",
      titleTh = "2. อ่านไวตอนเล่นจริงด้วย Quick Scan & ไอคอนกะทัดรัด",
      subtitleEn = "Mini-Window automatically condenses padding and keeps key actions 1 tap away.",
      subtitleTh = "โหมดหน้าต่างเล็กจะย่อระยะขอบอัตโนมัติ ให้เห็นไอเทมแก้ทางและสถิติสำคัญในหน้าจอเดียว",
      bulletsEn =
        listOf(
          "Tap Quick Scan (Camera icon) to scan the loading screen or scoreboard for instant counter items.",
          "Use Compact Icon Size in Settings so skill orders, builds, and rune slots fit cleanly.",
          "Check the 1v5 Analyzer Threat Meter to pick Anti-Heal, QSS, or Stasis before Dragon spawns.",
        ),
      bulletsTh =
        listOf(
          "กดปุ่ม Quick Scan (รูปกล้องด้านบน) เพื่อสแกนหน้าจอโหลดเกมหรือ Tab คะแนนแล้วคำนวณของแก้ทางทันที",
          "แนะนำให้ใช้ขนาดไอคอน 'กะทัดรัด (Compact)' ใน Settings เพื่ออ่านชื่อไอเทมและลำดับสกิลได้ครบในหน้าต่างเล็ก",
          "เปิด 1v5 Analyzer ดูเกจอันตราย (Anti-Heal / กัน CC / Stasis) ให้พร้อมก่อนมังกรเกิด",
        ),
      tipEn = "Tip: Tap any item or skill icon to pop open a concise stat summary box.",
      tipTh = "ทริค: แตะที่ไอคอนไอเทมหรือสกิลตัวไหนก็ได้ เพื่อเปิดกล่องสรุปค่าพลังแบบย่อทันที",
    ),
    MiniTutorialStep(
      icon = Icons.AutoMirrored.Filled.HelpOutline,
      titleEn = "3. Exit Anytime or Replay This Guide",
      titleTh = "3. สลับกลับจอปกติ หรือเปิดดูคู่มือซ้ำได้ตลอด",
      subtitleEn = "You stay in full control between pre-game theorycrafting and live matches.",
      subtitleTh = "สลับโหมดกลับไป-มาได้ในคลิกเดียว ไม่ต้องกลัวค้างในขนาดเล็ก",
      bulletsEn =
        listOf(
          "Tap the Exit Mini-Window icon (top bar) to restore full-width layout right after your match.",
          "Whenever Mini-Window is active, a '?' help button appears in the top bar to replay these steps.",
          "You can also re-open this tutorial anytime inside Settings → In-Game Mini-Window Mode.",
        ),
      bulletsTh =
        listOf(
          "หลังจบเกม กดไอคอนย่อ-ขยายมุมขวาบนอีกครั้งเพื่อคืนค่าขนาดเต็มจอทันที",
          "เมื่อเปิดโหมดหน้าต่างเล็ก ปุ่ม '?' จะแสดงบนแถบด้านบนให้กดดูคู่มือนี้ซ้ำได้ทุกเวลา",
          "หรือกดปุ่ม 'ดูคู่มือวิธีใช้' ในหน้า Settings ตรงหัวข้อ In-Game Mini-Window Mode ได้เช่นกัน",
        ),
      tipEn = "Ready! Switch on High-Contrast Card Borders if you play under bright daylight.",
      tipTh = "พร้อมลุย! แนะนำเปิด 'เส้นขอบการ์ดคอนทราสต์สูง' เพิ่มเติมหากเล่นในที่แสงจ้า",
    ),
  )

@Composable
fun MiniWindowTutorialOverlay(
  language: AppLanguage,
  onComplete: () -> Unit,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)

  val palette = LocalRiftPalette.current
  var stepIndex by remember { mutableIntStateOf(0) }
  val steps = MINI_TUTORIAL_STEPS
  val step = steps[stepIndex]
  val isLastStep = stepIndex == steps.lastIndex

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color.Black.copy(alpha = 0.76f))
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 12.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onDismiss,
          )
    )
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = palette.surfaceNavy,
      border = BorderStroke(1.6.dp, palette.primaryBright.copy(alpha = 0.85f)),
      tonalElevation = 8.dp,
      modifier =
        Modifier.fillMaxWidth()
          .widthIn(max = 520.dp)
          .pointerInput(Unit) { detectTapGestures {} }
          .testTag("mini_tutorial_overlay"),
    ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
        ) {
          // Pinned Header Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f),
            ) {
              Surface(
                shape = CircleShape,
                color = palette.primary.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.7f)),
                modifier = Modifier.size(36.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = palette.primaryBright,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
              Column {
                Text(
                  text =
                    if (language == AppLanguage.TH) "คู่มือ Mini-Window Mode"
                    else "Mini-Window Mode Guide",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = palette.secondary,
                )
                Text(
                  text = "${stepIndex + 1} / ${steps.size}",
                  style = MaterialTheme.typography.labelMedium,
                  color = palette.primaryBright,
                  modifier = Modifier.testTag("mini_tutorial_step_indicator"),
                )
              }
            }
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("mini_tutorial_close"),
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = if (language == AppLanguage.TH) "ปิดคู่มือ" else "Close tutorial",
                tint = palette.textSecondary,
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          LinearProgressIndicator(
            progress = { (stepIndex + 1) / steps.size.toFloat() },
            color = palette.primary,
            trackColor = palette.surfaceHigh,
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(4.dp)),
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Scrollable Step Content
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
          ) {
            Text(
              text = if (language == AppLanguage.TH) step.titleTh else step.titleEn,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = palette.textPrimary,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = if (language == AppLanguage.TH) step.subtitleTh else step.subtitleEn,
              style = MaterialTheme.typography.bodySmall,
              color = palette.textSecondary,
            )

            if (stepIndex == 0) {
              Spacer(modifier = Modifier.height(8.dp))
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = palette.surface,
                border = BorderStroke(1.dp, palette.outlineHighContrast.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(8.dp),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.surfaceHigh,
                    border = BorderStroke(1.dp, palette.outline),
                    modifier = Modifier.weight(1.35f).height(46.dp),
                  ) {
                    Column(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      verticalArrangement = Arrangement.Center,
                    ) {
                      Text(
                        text = "Wild Rift (Live)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                      )
                      Text(
                        text =
                          if (language == AppLanguage.TH) "Minimap & ปุ่มสกิลโล่ง"
                          else "Minimap & controls clear",
                        style = MaterialTheme.typography.labelSmall,
                        color = AdvantageEmerald,
                      )
                    }
                  }
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.primaryContainer,
                    border = BorderStroke(1.25.dp, palette.primaryBright),
                    modifier = Modifier.weight(1f).height(46.dp),
                  ) {
                    Column(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      verticalArrangement = Arrangement.Center,
                    ) {
                      Text(
                        text = "WR-AI Mini",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryBright,
                      )
                      Text(
                        text =
                          if (language == AppLanguage.TH) "ลอยมุมจอ / Split"
                          else "Floating / Split",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondary,
                      )
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val bullets = if (language == AppLanguage.TH) step.bulletsTh else step.bulletsEn
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              bullets.forEachIndexed { idx, line ->
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = palette.surface,
                  border = BorderStroke(1.dp, palette.outline.copy(alpha = 0.85f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Box(
                      modifier =
                        Modifier.size(18.dp)
                          .clip(CircleShape)
                          .background(palette.secondary.copy(alpha = 0.2f)),
                      contentAlignment = Alignment.Center,
                    ) {
                      Text(
                        text = "${idx + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.secondary,
                      )
                    }
                    Text(
                      text = line,
                      style = MaterialTheme.typography.bodySmall,
                      color = palette.textPrimary,
                      modifier = Modifier.weight(1f),
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
              shape = RoundedCornerShape(10.dp),
              color = palette.secondaryContainer.copy(alpha = 0.55f),
              border = BorderStroke(1.dp, palette.secondary.copy(alpha = 0.7f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = if (language == AppLanguage.TH) step.tipTh else step.tipEn,
                style = MaterialTheme.typography.labelMedium,
                color = palette.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Pinned Footer Navigation Controls
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            TextButton(
              onClick = onDismiss,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("mini_tutorial_skip"),
            ) {
              Text(
                text = if (language == AppLanguage.TH) "ข้าม" else "Skip",
                color = palette.textMuted,
              )
            }

            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              if (stepIndex > 0) {
                OutlinedButton(
                  onClick = { stepIndex -= 1 },
                  border = BorderStroke(1.dp, palette.outlineHighContrast),
                  modifier =
                    Modifier.minimumInteractiveComponentSize().testTag("mini_tutorial_prev"),
                ) {
                  Text(
                    text = if (language == AppLanguage.TH) "ย้อนกลับ" else "Back",
                    color = palette.textPrimary,
                  )
                }
              }

              if (!isLastStep) {
                Button(
                  onClick = { stepIndex += 1 },
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = palette.primary,
                      contentColor = palette.onPrimary,
                    ),
                  modifier =
                    Modifier.minimumInteractiveComponentSize().testTag("mini_tutorial_next"),
                ) {
                  Text(
                    text = if (language == AppLanguage.TH) "ถัดไป" else "Next",
                    fontWeight = FontWeight.Bold,
                  )
                }
              } else {
                Button(
                  onClick = onComplete,
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = AdvantageEmerald,
                      contentColor = palette.onPrimary,
                    ),
                  modifier =
                    Modifier.minimumInteractiveComponentSize().testTag("mini_tutorial_done"),
                ) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (language == AppLanguage.TH) "เริ่มใช้งานเลย" else "Got it",
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
          }
        }
      }
    }
  }

