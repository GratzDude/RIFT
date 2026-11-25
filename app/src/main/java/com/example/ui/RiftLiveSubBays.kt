package com.example.ui

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ArmadaMemberSlot
import com.example.data.CloudSyncHealth
import com.example.data.LeaderboardEntry
import com.example.data.LocalPilotPrefs
import com.example.data.SyndicateInfo
import com.example.data.SyndicateWarRecord
import com.example.data.formatSurvivalDurationSec
import com.example.data.snapCustomFivePercentScale
import com.example.engine.CustomGameConfig
import com.example.engine.RankedGameMode
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftCodex
import com.example.engine.RiftGameEngine
import com.example.engine.RiftSpriteBaker
import com.example.engine.RiftVisualForgeState
import com.example.engine.SyndicateCrestRecipe
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt
import org.json.JSONObject

private fun cleanCodexShipName(rawName: String): String =
  rawName.replace(Regex("^#?[0-9]+[-\\s]*"), "").trim().uppercase(Locale.US)

private data class LiveControllerActionSlotInfo(
  val slotId: String,
  val title: String,
  val subtitle: String,
  val accentColor: Color,
)

private val LIVE_CONTROLLER_ACTION_SLOTS =
  listOf(
    LiveControllerActionSlotInfo(
      "PRIMARY_FIRE",
      "MAIN ATTACK",
      "Primary weapon trigger or fire stick",
      Color(0xFFEF4444),
    ),
    LiveControllerActionSlotInfo(
      "SPECIAL_BUFF",
      "BUFF ATTACK",
      "Fires equipped Special Crate Buff",
      Color(0xFF34D399),
    ),
    LiveControllerActionSlotInfo(
      "RETRO_BRAKE",
      "RETRO-BRAKE",
      "Hold to damp Newtonian drift",
      Color(0xFF38BDF8),
    ),
    LiveControllerActionSlotInfo(
      "SECONDARY_ABILITY",
      "SECONDARY ABILITY",
      "Phase Dash, Cloak, Stasis, or Warhead",
      Color(0xFFF59E0B),
    ),
    LiveControllerActionSlotInfo(
      "REPEL",
      "REPEL SHOCKWAVE",
      "360 degree radial repulsion pulse",
      Color(0xFF60A5FA),
    ),
    LiveControllerActionSlotInfo(
      "MULTI_TOGGLE",
      "MULTI-SHOT TOGGLE",
      "Toggle multi-stream on compatible hulls",
      Color(0xFFFACC15),
    ),
  )

private val LIVE_HARDWARE_BIND_CHIPS =
  listOf(
    RiftCalibrationState.BIND_RT to "RT / R2",
    KeyEvent.KEYCODE_BUTTON_R1 to "R1 / RB",
    RiftCalibrationState.BIND_LT to "LT / L2",
    KeyEvent.KEYCODE_BUTTON_L1 to "L1 / LB",
    KeyEvent.KEYCODE_BUTTON_X to "X BTN",
    KeyEvent.KEYCODE_BUTTON_A to "A BTN",
    KeyEvent.KEYCODE_BUTTON_B to "B BTN",
    KeyEvent.KEYCODE_BUTTON_Y to "Y BTN",
    KeyEvent.KEYCODE_BUTTON_THUMBL to "L3",
    KeyEvent.KEYCODE_BUTTON_THUMBR to "R3",
  )

@Composable
internal fun DossierLeftIdentityBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  callsign: String,
  syndicateTag: String,
  liveResonance: Int,
  peakResonance: Int,
  localPrefs: LocalPilotPrefs,
  currentUserId: String,
  onBackToHub: () -> Unit,
  onOpenRenamePrompt: () -> Unit,
  onOpenResetPrompt: () -> Unit,
  onOpenSwitchAccountPrompt: () -> Unit,
  onOpenSurvivalSpreadsheet: () -> Unit,
) {
  val uidOrNull = currentUserId.ifBlank { null }
  val career = remember(currentUserId, liveResonance) { localPrefs.getPilotCareerStatsSummary(uidOrNull) }
  val callsignChangeCount = remember(currentUserId, callsign) { localPrefs.getCallsignChangeCount(uidOrNull) }
  val isFirstRenameFree = callsignChangeCount == 0
  val maxXpTrack =
    maxOf(
      1L,
      career.combatXp,
      career.survivalXp,
      career.objectiveXp,
      career.supportCratesXp,
      career.dailyDirectivesXp,
    )

  Column(
    modifier = Modifier.fillMaxSize().testTag("dossier_left_identity_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = false,
      isTabletDeck = isTabletDeck,
      sectionTitle = "PILOT DOSSIER",
      accent = Color(0xFF00F5D4),
      onBackToHub = onBackToHub,
    )

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      // Pilot Identity + Level 1..100 Header Pod
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = false,
              primaryBorder = Color(0xFF00F5D4),
              secondaryBorder = Color(0xFFFFD700),
            )
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = if (syndicateTag.isNotBlank()) "[$syndicateTag] $callsign" else callsign,
            color = Color.White,
            fontSize = if (isTabletDeck) 10.6.sp else 9.4.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 6.dp),
          )
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(5.dp))
                .background(Color(0x2800F5D4))
                .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(5.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .testTag("dossier_pilot_level_badge"),
          ) {
            Text(
              text = "LVL ${career.pilotLevel.coerceIn(1, 100)}",
              color = Color(0xFF00F5D4),
              fontSize = 8.0.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }
        }

        // Level Progress Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Box(
            modifier =
              Modifier.weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF02050B))
                .border(0.6.dp, Color(0x4400F5D4), RoundedCornerShape(3.dp)),
          ) {
            Box(
              modifier =
                Modifier.fillMaxWidth(career.levelProgressFraction.coerceIn(0.04f, 1f))
                  .fillMaxHeight()
                  .background(Brush.horizontalGradient(listOf(Color(0xFF00F5D4), Color(0xFFFFD700)))),
            )
          }
          Text(
            text = "${career.xpIntoLevel}/${career.xpSpanForLevel} XP",
            color = Color(0xFF94A3B8),
            fontSize = 7.2.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          ResonanceCrestIcon(sizeDp = 12.dp)
          Text(
            text = "LIVE ${String.format(Locale.US, "%,d", liveResonance)} • PEAK ${String.format(Locale.US, "%,d", peakResonance)}",
            color = Color(0xFFFFD700),
            fontSize = 8.2.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      // 5 Dedicated XP Bars (Combat, Survival, Objective, Support & Crates, Daily Directives)
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = false,
              primaryBorder = Color(0xFF38BDF8),
              secondaryBorder = Color(0xFF091528),
            )
            .padding(horizontal = 9.dp, vertical = 7.dp)
            .testTag("dossier_five_xp_tracks"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = "5 XP PROGRESSION TRACKS (${String.format(Locale.US, "%,d", career.totalXp)} TOTAL)",
          color = Color(0xFF38BDF8),
          fontSize = 7.6.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        listOf(
          Triple("COMBAT XP", career.combatXp, Color(0xFFEF4444)),
          Triple("SURVIVAL XP", career.survivalXp, Color(0xFF10B981)),
          Triple("OBJECTIVE XP", career.objectiveXp, Color(0xFFFACC15)),
          Triple("SUPPORT & CRATES XP", career.supportCratesXp, Color(0xFF38BDF8)),
          Triple("DAILY DIRECTIVES XP", career.dailyDirectivesXp, Color(0xFFC084FC)),
        ).forEach { (label, xpVal, barCol) ->
          val frac = (xpVal.toFloat() / maxXpTrack.toFloat()).coerceIn(0.05f, 1f)
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = label,
                color = Color(0xFFCBD5E1),
                fontSize = 7.0.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
              )
              Text(
                text = String.format(Locale.US, "%,d", xpVal),
                color = barCol,
                fontSize = 7.2.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .height(4.dp)
                  .clip(RoundedCornerShape(2.dp))
                  .background(Color(0xFF02050B)),
            ) {
              Box(
                modifier =
                  Modifier.fillMaxWidth(frac)
                    .fillMaxHeight()
                    .background(barCol),
              )
            }
          }
        }
      }

      // Rename Callsign (1ST FREE vs 250 Shards with Crystal Icon)
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .heightIn(min = 48.dp)
            .chamferedSciFiPodSurface(
              isRightSide = false,
              primaryBorder = Color(0xFF00F5D4),
              secondaryBorder = Color(0xFF091528),
            )
            .clickable { onOpenRenamePrompt() }
            .padding(horizontal = 9.dp, vertical = 6.dp)
            .testTag("dossier_btn_rename_callsign"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "RENAME CALLSIGN",
          color = Color.White,
          fontSize = if (isTabletDeck) 8.8.sp else 8.0.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        if (isFirstRenameFree) {
          Text(
            text = "1ST FREE >",
            color = Color(0xFF00F5D4),
            fontSize = 8.0.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        } else {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            RiftShardGemIcon(sizeDp = 12.dp)
            Text(
              text = "250 >",
              color = Color(0xFFFACC15),
              fontSize = 8.0.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }

      // Survival Sheet + Overwatch-Guarded Switch Account & Reset Record
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .heightIn(min = 48.dp)
            .chamferedSciFiPodSurface(
              isRightSide = false,
              primaryBorder = Color(0xFFFFD700),
              secondaryBorder = Color(0xFF091528),
            )
            .clickable { onOpenSurvivalSpreadsheet() }
            .padding(horizontal = 9.dp, vertical = 6.dp)
            .testTag("dossier_btn_survival_sheet"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "SURVIVAL SHEET",
          color = Color(0xFFFFD700),
          fontSize = 8.0.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        Text(
          text = "OPEN TABLE >",
          color = Color(0xFF00F5D4),
          fontSize = 7.6.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Box(
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .heightIn(min = 48.dp)
              .chamferedSciFiPodSurface(
                isRightSide = false,
                primaryBorder = Color(0xFF38BDF8),
                secondaryBorder = Color(0xFF091528),
              )
              .clickable { onOpenSwitchAccountPrompt() }
              .padding(horizontal = 6.dp)
              .testTag("dossier_btn_switch_account"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "SWITCH ACCOUNT",
            color = Color(0xFF38BDF8),
            fontSize = 7.4.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
          )
        }
        Box(
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .heightIn(min = 48.dp)
              .chamferedSciFiPodSurface(
                isRightSide = false,
                primaryBorder = Color(0xFFFB7185),
                secondaryBorder = Color(0xFF091528),
              )
              .clickable { onOpenResetPrompt() }
              .padding(horizontal = 6.dp)
              .testTag("dossier_btn_reset_record"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "RESET RECORD",
            color = Color(0xFFFB7185),
            fontSize = 7.4.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
internal fun DossierRightCareerBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  bestShipId: Int,
  shipSpinDeg: Float,
  fluxOrbitRad: Float,
  shipResonanceFor: (Int) -> Int,
  localPrefs: LocalPilotPrefs,
  currentUserId: String,
  cloudLeaderboardEntries: List<LeaderboardEntry>,
  onInspectShip: (Int) -> Unit,
) {
  val context = LocalContext.current
  val uidOrNull = currentUserId.ifBlank { null }
  val career = remember(currentUserId) { localPrefs.getPilotCareerStatsSummary(uidOrNull) }
  val survival = remember(currentUserId) { localPrefs.getPilotSurvivalTelemetrySummary(uidOrNull) }
  val hubTapCount = remember(currentUserId) { localPrefs.getHubTapTelemetryHistory().size }
  val allShips = remember(RiftSpriteBaker.customShip9Version) { RiftCodex.getAllTenShips() }
  val hallOfFame = remember(cloudLeaderboardEntries) { cloudLeaderboardEntries.take(5) }
  var exportBanner by remember { mutableStateOf<String?>(null) }

  Column(
    modifier = Modifier.fillMaxSize().testTag("dossier_right_career_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = true,
      isTabletDeck = isTabletDeck,
      sectionTitle = "CAREER & TELEMETRY",
      accent = Color(0xFFFFD700),
    )

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      // Full Career Stats Grid (Matches, Wins, Losses, Win %, Elims, Deaths, Assists, Accuracy, Crates, Rifts)
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = true,
              primaryBorder = Color(0xFF38BDF8),
              secondaryBorder = Color(0xFF091528),
            )
            .padding(horizontal = 9.dp, vertical = 7.dp)
            .testTag("dossier_career_stats_grid"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = "MATCH RECORD",
          color = Color(0xFF38BDF8),
          fontSize = 7.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        val statRows =
          listOf(
            "MATCHES" to "${career.matchesPlayed}",
            "WINS / LOSSES" to "${career.wins} / ${career.losses}",
            "WIN RATE" to "${career.winPercent}%",
            "ELIMINATIONS" to "${career.eliminations}",
            "DEATHS / ASSISTS" to "${career.deaths} / ${career.assists}",
            "ACCURACY" to "${career.accuracyPercent}%",
            "CRATES / RIFTS" to "${career.cratePickups} / ${career.riftCaptures}",
          )
        statRows.forEach { (k, v) ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = k,
              color = Color(0xFF94A3B8),
              fontSize = 7.2.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
            )
            Text(
              text = v,
              color = Color.White,
              fontSize = 7.6.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }

      // Ship Survival Duration Readout
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = true,
              primaryBorder = Color(0xFF00F5D4),
              secondaryBorder = Color(0xFF091528),
            )
            .padding(horizontal = 9.dp, vertical = 7.dp)
            .testTag("dossier_survival_summary_card"),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Text(
          text = "SHIP SURVIVAL DURATION (${survival.pilotCompletedLives} LIVES)",
          color = Color(0xFF00F5D4),
          fontSize = 7.6.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "AVG ${formatSurvivalDurationSec(survival.pilotAvgSurvivalSec)}",
            color = Color.White,
            fontSize = 7.6.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          Text(
            text = "LAST ${formatSurvivalDurationSec(survival.pilotLastSurvivalSec)}",
            color = Color(0xFF38BDF8),
            fontSize = 7.6.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          Text(
            text = "MAX ${formatSurvivalDurationSec(survival.pilotMaxSurvivalSec)}",
            color = Color(0xFFFFD700),
            fontSize = 7.6.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }

      // Numberless Per-Ship Resonance Mastery Rows
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = true,
              primaryBorder = Color(0xFFFFD700),
              secondaryBorder = Color(0xFF091528),
            )
            .padding(horizontal = 8.dp, vertical = 7.dp)
            .testTag("dossier_per_ship_mastery"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = "PER-SHIP RESONANCE (10 HULLS)",
          color = Color(0xFFFFD700),
          fontSize = 7.6.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        allShips.forEach { ship ->
          val cleanShip = cleanCodexShipName(ship.name)
          val shipRes = shipResonanceFor(ship.id)
          val isBest = ship.id == bestShipId
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isBest) Color(0x22FFD700) else Color(0x6602050B))
                .border(
                  0.8.dp,
                  if (isBest) Color(0xFFFFD700) else Color(0x3338BDF8),
                  RoundedCornerShape(6.dp),
                )
                .clickable { onInspectShip(ship.id) }
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .testTag("dossier_ship_mastery_${ship.id}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.weight(1f),
            ) {
              Spinning25dShipWithOrbitingFlux(
                shipId = ship.id,
                shipSpinDeg = shipSpinDeg,
                fluxOrbitRad = fluxOrbitRad,
                isCharged = false,
                isBestShip = isBest,
                sizeDp = 28.dp,
              )
              Text(
                text = cleanShip,
                color = if (isBest) Color(0xFFFFD700) else Color.White,
                fontSize = 7.8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              ResonanceCrestIcon(sizeDp = 11.dp)
              Text(
                text = String.format(Locale.US, "%,d", shipRes),
                color = Color(0xFFFFD700),
                fontSize = 7.8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
        }
      }

      // Hall of Fame Top Pilots
      if (hallOfFame.isNotEmpty()) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .chamferedSciFiPodSurface(
                isRightSide = true,
                primaryBorder = Color(0xFF38BDF8),
                secondaryBorder = Color(0xFF091528),
              )
              .padding(horizontal = 8.dp, vertical = 7.dp),
          verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          Text(
            text = "SECTOR HALL OF FAME",
            color = Color(0xFF38BDF8),
            fontSize = 7.6.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          hallOfFame.forEachIndexed { idx, entry ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "${idx + 1}. ${entry.callsign}",
                color = if (idx == 0) Color(0xFFFFD700) else Color.White,
                fontSize = 7.4.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 6.dp),
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
              ) {
                ResonanceCrestIcon(sizeDp = 10.dp)
                Text(
                  text = String.format(Locale.US, "%,d", entry.allTimeBounty.coerceAtLeast(0L)),
                  color = Color(0xFF00F5D4),
                  fontSize = 7.4.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
          }
        }
      }

      // Filterable Export Row (HTML Table, CSV, JSON incl. Hub Tap Telemetry)
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = true,
              primaryBorder = Color(0xFF00F5D4),
              secondaryBorder = Color(0xFF091528),
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = "TELEMETRY EXPORT ($hubTapCount HUB TAPS LOGGED)",
          color = Color(0xFF00F5D4),
          fontSize = 7.2.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          HubTelemetryMiniExportBtn(
            modifier = Modifier.weight(1f),
            label = "HTML TABLE",
            accent = Color(0xFF00F5D4),
            testTag = "dossier_export_html",
            onClick = {
              val payload =
                localPrefs.exportSurvivalTelemetryHtmlSpreadsheet(
                  localPrefs.getCallsign(),
                  localPrefs.getLiveResonance(uidOrNull),
                  uidOrNull,
                )
              exportBanner =
                shareOrOpenSurvivalExport(
                  context = context,
                  fileName = "rift_ship_survival_telemetry.html",
                  mimeType = "text/html",
                  payload = payload,
                  preferBrowserView = true,
                )
            },
          )
          HubTelemetryMiniExportBtn(
            modifier = Modifier.weight(1f),
            label = "CSV",
            accent = Color(0xFF38BDF8),
            testTag = "dossier_export_csv",
            onClick = {
              val payload =
                localPrefs.exportSurvivalTelemetryCsv(
                  localPrefs.getCallsign(),
                  localPrefs.getLiveResonance(uidOrNull),
                  uidOrNull,
                )
              exportBanner =
                shareOrOpenSurvivalExport(
                  context = context,
                  fileName = "rift_ship_survival_telemetry.csv",
                  mimeType = "text/csv",
                  payload = payload,
                )
            },
          )
          HubTelemetryMiniExportBtn(
            modifier = Modifier.weight(1f),
            label = "JSON",
            accent = Color(0xFFFFD700),
            testTag = "dossier_export_json",
            onClick = {
              val payload =
                localPrefs.exportSurvivalTelemetryJson(
                  localPrefs.getCallsign(),
                  localPrefs.getLiveResonance(uidOrNull),
                  uidOrNull,
                )
              exportBanner =
                shareOrOpenSurvivalExport(
                  context = context,
                  fileName = "rift_ship_survival_telemetry.json",
                  mimeType = "application/json",
                  payload = payload,
                )
            },
          )
        }
        if (!exportBanner.isNullOrBlank()) {
          Text(
            text = exportBanner!!,
            color = Color(0xFF00F5D4),
            fontSize = 7.0.sp,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }
  }
}

@Composable
internal fun HubTelemetryMiniExportBtn(
  modifier: Modifier = Modifier,
  label: String,
  accent: Color,
  testTag: String,
  onClick: () -> Unit,
) {
  Box(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .height(48.dp)
        .clickable { onClick() }
        .padding(vertical = 6.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .clip(RoundedCornerShape(6.dp))
          .background(accent.copy(alpha = 0.16f))
          .border(1.dp, accent, RoundedCornerShape(6.dp)),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label,
        color = Color.White,
        fontSize = 7.2.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
      )
    }
  }
}

@Composable
internal fun SyndicateLeftDirectoryBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  currentTag: String,
  syndicateDirectory: List<SyndicateInfo>,
  syndicateWarRecord: SyndicateWarRecord,
  statusMessage: String?,
  onBackToHub: () -> Unit,
  onInspectSyndicate: (SyndicateInfo) -> Unit,
) {
  var searchQuery by remember { mutableStateOf("") }
  val realSyndicates =
    remember(syndicateDirectory, searchQuery) {
      val q = searchQuery.trim().uppercase(Locale.US)
      syndicateDirectory
        .filter { it.isRealPlayerFoundedSyndicate() }
        .filter { syn ->
          q.isEmpty() ||
            syn.tag.uppercase(Locale.US).contains(q) ||
            syn.name.uppercase(Locale.US).contains(q) ||
            syn.motto.uppercase(Locale.US).contains(q)
        }
    }

  Column(
    modifier = Modifier.fillMaxSize().testTag("syndicate_left_directory_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = false,
      isTabletDeck = isTabletDeck,
      sectionTitle = "SYNDICATES",
      accent = Color(0xFFFFD700),
      onBackToHub = onBackToHub,
    )

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      // War Standings Readout Pod
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = false,
              primaryBorder = Color(0xFFFFD700),
              secondaryBorder = Color(0xFF091528),
            )
            .padding(horizontal = 9.dp, vertical = 6.dp)
            .testTag("syndicate_war_standings_pod"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "WAR STANDINGS",
          color = Color(0xFFFFD700),
          fontSize = 7.6.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
        Text(
          text = "${syndicateWarRecord.warTrophies} TROPHIES • ${syndicateWarRecord.warWins}-${syndicateWarRecord.warLosses}",
          color = Color(0xFF00F5D4),
          fontSize = 7.6.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
      }

      // Live Search Bar
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .heightIn(min = 48.dp)
            .chamferedSciFiPodSurface(
              isRightSide = false,
              primaryBorder = Color(0xFF38BDF8),
              secondaryBorder = Color(0xFF091528),
            )
            .padding(horizontal = 9.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart,
      ) {
        if (searchQuery.isEmpty()) {
          Text(
            text = "Search chartered Syndicates by tag or name...",
            color = Color(0xFF64748B),
            fontSize = 8.0.sp,
            fontFamily = FontFamily.Monospace,
          )
        }
        BasicTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it.take(24) },
          singleLine = true,
          cursorBrush = SolidColor(Color(0xFF00F5D4)),
          textStyle =
            TextStyle(
              color = Color.White,
              fontSize = 8.4.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
            ),
          modifier = Modifier.fillMaxWidth().testTag("syndicate_search_input"),
        )
      }

      if (!statusMessage.isNullOrBlank()) {
        Text(
          text = statusMessage,
          color = Color(0xFF00F5D4),
          fontSize = 7.6.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.padding(horizontal = 4.dp),
        )
      }

      if (realSyndicates.isEmpty()) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .chamferedSciFiPodSurface(
                isRightSide = false,
                primaryBorder = Color(0xFF38BDF8),
                secondaryBorder = Color(0xFF091528),
              )
              .padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text =
              if (searchQuery.isBlank()) "NO CHARTERED SYNDICATES YET"
              else "NO SYNDICATES MATCH \"$searchQuery\"",
            color = Color(0xFFFFD700),
            fontSize = 8.2.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          Text(
            text = "Use the Citadel bay on the right to found a real-player Syndicate.",
            color = Color(0xFFCBD5E1),
            fontSize = 7.6.sp,
            fontFamily = FontFamily.SansSerif,
          )
        }
      } else {
        realSyndicates.forEach { syn ->
          val isCurrent = currentTag.isNotBlank() && syn.tag.equals(currentTag, ignoreCase = true)
          RiftVerticalCard(
            modifier = Modifier.fillMaxWidth(),
            title = "[${syn.tag.uppercase(Locale.US)}] ${syn.name.uppercase(Locale.US)}",
            subtitle = "${syn.memberCount.coerceAtLeast(1)} pilots • ${String.format(Locale.US, "%,d", syn.combinedBounty.coerceAtLeast(0L))}",
            accent = if (isCurrent) Color(0xFF00F5D4) else Color(0xFFFFD700),
            secondaryAccent = Color(0xFF38BDF8),
            isRightColumn = false,
            isHighlighted = isCurrent,
            badgeText = if (isCurrent) "MEMBER" else "CHARTERED",
            badgeColor = if (isCurrent) Color(0xFF00F5D4) else Color(0xFFFFD700),
            isTabletDeck = isTabletDeck,
            minHeightDp = if (isTabletDeck) 92.dp else 80.dp,
            testTag = "syndicate_dir_${syn.tag}",
            visualContent = { sizeDp ->
              ResonanceCrestIcon(sizeDp = (sizeDp * 0.65f).coerceAtLeast(22.dp))
            },
            onClick = { onInspectSyndicate(syn) },
          )
        }
      }
    }
  }
}

@Composable
internal fun SyndicateRightCitadelBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  syndicateTag: String,
  syndicateName: String,
  syndicateOnlineCount: Int,
  syndicateWarRecord: SyndicateWarRecord,
  currentUserId: String,
  callsign: String,
  localShipId: Int,
  liveResonance: Long,
  kills: Long,
  localPrefs: LocalPilotPrefs,
  onLeaveCurrentSyndicate: () -> Unit,
  onCharterCustomSyndicate: (tag: String, name: String, motto: String) -> Unit,
  onOpenCrestBuilder: (String) -> Unit,
) {
  var tagDraft by remember { mutableStateOf("") }
  var nameDraft by remember { mutableStateOf("") }
  var mottoDraft by remember { mutableStateOf("") }
  var rosterRefreshTick by remember { mutableIntStateOf(0) }
  var inviteOnly by remember(syndicateTag, rosterRefreshTick) {
    mutableStateOf(
      if (syndicateTag.isNotBlank()) localPrefs.isSyndicateInviteOnly(syndicateTag) else false
    )
  }

  val activeCrestRecipe =
    remember(syndicateTag, RiftVisualForgeState.visualRevision) {
      val savedJson = if (syndicateTag.isNotBlank()) localPrefs.getSyndicateCrestJson(syndicateTag) else null
      if (!savedJson.isNullOrBlank()) {
        runCatching { SyndicateCrestRecipe.fromJson(JSONObject(savedJson)) }
          .getOrDefault(RiftVisualForgeState.activeSyndicateCrestRecipe)
      } else {
        RiftVisualForgeState.activeSyndicateCrestRecipe
      }
    }

  val rosterMembers =
    remember(syndicateTag, callsign, localShipId, liveResonance, rosterRefreshTick) {
      if (syndicateTag.isBlank()) emptyList()
      else localPrefs.getMySyndicateRoster(currentUserId, callsign, localShipId, liveResonance, kills)
    }
  val myRole =
    rosterMembers.firstOrNull { it.isLocalPlayer }?.role?.uppercase(Locale.US) ?: "PILOT"
  val isLeaderOrOfficer =
    myRole.contains("LEADER") || myRole.contains("FOUNDER") || myRole.contains("COMMANDER") || myRole.contains("OFFICER")

  Column(
    modifier = Modifier.fillMaxSize().testTag("syndicate_right_citadel_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = true,
      isTabletDeck = isTabletDeck,
      sectionTitle = if (syndicateTag.isNotBlank()) "[$syndicateTag] CITADEL" else "CHARTER SYNDICATE",
      accent = Color(0xFF00F5D4),
      trailingLabel = if (syndicateTag.isNotBlank()) "LEAVE" else null,
      trailingTag = if (syndicateTag.isNotBlank()) "syndicate_btn_leave_header" else null,
      onTrailingClick = if (syndicateTag.isNotBlank()) onLeaveCurrentSyndicate else null,
    )

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      if (syndicateTag.isNotBlank()) {
        // Citadel Summary + Live Crest Preview + Customize Crest Trigger
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .chamferedSciFiPodSurface(
                isRightSide = true,
                primaryBorder = Color(0xFFFFD700),
                secondaryBorder = Color(0xFF091528),
              )
              .padding(horizontal = 9.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Canvas(modifier = Modifier.size(if (isTabletDeck) 52.dp else 44.dp)) {
            drawIntoCanvas { canvas ->
              RiftSpriteBaker.drawSyndicateCrestOnCanvas(
                c = canvas.nativeCanvas,
                cx = size.width * 0.5f,
                cy = size.height * 0.5f,
                r = min(size.width, size.height) * 0.44f,
                recipe = activeCrestRecipe,
                tagText = syndicateTag,
              )
            }
          }
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
              text = "[$syndicateTag] $syndicateName",
              color = Color.White,
              fontSize = if (isTabletDeck) 9.8.sp else 8.8.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "$syndicateOnlineCount ONLINE • ${syndicateWarRecord.warWins}W-${syndicateWarRecord.warLosses}L",
              color = Color(0xFF00F5D4),
              fontSize = 7.4.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
              Box(
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .heightIn(min = 48.dp)
                    .clickable { onOpenCrestBuilder(syndicateTag) }
                    .padding(vertical = 4.dp)
                    .testTag("syndicate_btn_crest_builder"),
                contentAlignment = Alignment.Center,
              ) {
                Box(
                  modifier =
                    Modifier.clip(RoundedCornerShape(6.dp))
                      .background(Color(0xFF0D253B))
                      .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(6.dp))
                      .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                  Text(
                    text = "CUSTOMIZE CREST",
                    color = Color(0xFF00F5D4),
                    fontSize = 7.2.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                  )
                }
              }

              if (isLeaderOrOfficer) {
                Box(
                  modifier =
                    Modifier.minimumInteractiveComponentSize()
                      .heightIn(min = 48.dp)
                      .clickable {
                        val next = !inviteOnly
                        inviteOnly = next
                        localPrefs.setSyndicateInviteOnly(syndicateTag, next)
                      }
                      .padding(vertical = 4.dp)
                      .testTag("syndicate_toggle_invite_only"),
                  contentAlignment = Alignment.Center,
                ) {
                  Box(
                    modifier =
                      Modifier.clip(RoundedCornerShape(6.dp))
                        .background(if (inviteOnly) Color(0xFF3B1A2A) else Color(0xFF072B24))
                        .border(
                          1.dp,
                          if (inviteOnly) Color(0xFFFACC15) else Color(0xFF10B981),
                          RoundedCornerShape(6.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                  ) {
                    Text(
                      text = if (inviteOnly) "INVITE-ONLY" else "OPEN JOIN",
                      color = Color.White,
                      fontSize = 7.2.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace,
                    )
                  }
                }
              }
            }
          }
        }

        // Syndicate Member Roster (Online green dot, Role, Resonance, Promote/Kick)
        Text(
          text = "MEMBER ROSTER (${rosterMembers.size})",
          color = Color(0xFF38BDF8),
          fontSize = 7.8.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.padding(horizontal = 4.dp),
        )
        rosterMembers.forEach { member ->
          val memberRoleUp = member.role.uppercase(Locale.US)
          val isMemberOfficer = memberRoleUp.contains("OFFICER")
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .chamferedSciFiPodSurface(
                  isRightSide = true,
                  primaryBorder = if (member.isOnline) Color(0xFF10B981) else Color(0xFF334155),
                  secondaryBorder = Color(0xFF091528),
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .testTag("syndicate_member_${member.callsign}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Box(
              modifier =
                Modifier.size(8.dp)
                  .clip(CircleShape)
                  .background(if (member.isOnline) Color(0xFF10B981) else Color(0xFF475569)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
              Text(
                text = if (member.isLocalPlayer) "${member.callsign} (YOU)" else member.callsign,
                color = Color.White,
                fontSize = 8.0.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(
                  text = member.role.uppercase(Locale.US),
                  color = Color(0xFF38BDF8),
                  fontSize = 6.8.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                )
                ResonanceCrestIcon(sizeDp = 10.dp)
                Text(
                  text = String.format(Locale.US, "%,d", member.bountyContribution.coerceAtLeast(0L)),
                  color = Color(0xFFFFD700),
                  fontSize = 7.0.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
            if (isLeaderOrOfficer && !member.isLocalPlayer && !memberRoleUp.contains("FOUNDER")) {
              Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                  modifier =
                    Modifier.minimumInteractiveComponentSize()
                      .height(48.dp)
                      .clickable {
                        localPrefs.toggleSyndicateOfficer(syndicateTag, member.callsign)
                        rosterRefreshTick++
                      }
                      .padding(vertical = 8.dp)
                      .testTag("syndicate_toggle_officer_${member.callsign}"),
                  contentAlignment = Alignment.Center,
                ) {
                  Box(
                    modifier =
                      Modifier.clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF0D253B))
                        .border(1.dp, Color(0xFFFACC15), RoundedCornerShape(5.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                  ) {
                    Text(
                      text = if (isMemberOfficer) "DEMOTE" else "PROMOTE",
                      color = Color(0xFFFACC15),
                      fontSize = 6.6.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace,
                    )
                  }
                }
                Box(
                  modifier =
                    Modifier.minimumInteractiveComponentSize()
                      .height(48.dp)
                      .clickable {
                        localPrefs.kickSyndicateMember(syndicateTag, member.callsign)
                        rosterRefreshTick++
                      }
                      .padding(vertical = 8.dp)
                      .testTag("syndicate_kick_${member.callsign}"),
                  contentAlignment = Alignment.Center,
                ) {
                  Box(
                    modifier =
                      Modifier.clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF3B0A18))
                        .border(1.dp, Color(0xFFFB7185), RoundedCornerShape(5.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                  ) {
                    Text(
                      text = "REMOVE",
                      color = Color(0xFFFB7185),
                      fontSize = 6.6.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace,
                    )
                  }
                }
              }
            }
          }
        }
      } else {
        // Unaffiliated Charter New Player-Founded Syndicate Form
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .chamferedSciFiPodSurface(
                isRightSide = true,
                primaryBorder = Color(0xFF00F5D4),
                secondaryBorder = Color(0xFF091528),
              )
              .padding(horizontal = 9.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "FOUND A NEW SYNDICATE",
            color = Color(0xFF00F5D4),
            fontSize = 8.6.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF02050B))
                .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 7.dp),
          ) {
            if (tagDraft.isEmpty()) {
              Text(
                text = "TAG (2-4 LETTERS, e.g. APEX)",
                color = Color(0xFF64748B),
                fontSize = 7.6.sp,
                fontFamily = FontFamily.Monospace,
              )
            }
            BasicTextField(
              value = tagDraft,
              onValueChange = { tagDraft = it.uppercase(Locale.US).replace(Regex("[^A-Z0-9]"), "").take(4) },
              singleLine = true,
              cursorBrush = SolidColor(Color(0xFF00F5D4)),
              textStyle =
                TextStyle(
                  color = Color.White,
                  fontSize = 8.4.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                ),
              modifier = Modifier.fillMaxWidth().testTag("syndicate_charter_tag_input"),
            )
          }
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF02050B))
                .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 7.dp),
          ) {
            if (nameDraft.isEmpty()) {
              Text(
                text = "SYNDICATE NAME",
                color = Color(0xFF64748B),
                fontSize = 7.6.sp,
                fontFamily = FontFamily.Monospace,
              )
            }
            BasicTextField(
              value = nameDraft,
              onValueChange = { nameDraft = it.take(26) },
              singleLine = true,
              cursorBrush = SolidColor(Color(0xFF00F5D4)),
              textStyle =
                TextStyle(
                  color = Color.White,
                  fontSize = 8.2.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                ),
              modifier = Modifier.fillMaxWidth().testTag("syndicate_charter_name_input"),
            )
          }
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF02050B))
                .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 7.dp),
          ) {
            if (mottoDraft.isEmpty()) {
              Text(
                text = "MOTTO (OPTIONAL)",
                color = Color(0xFF64748B),
                fontSize = 7.6.sp,
                fontFamily = FontFamily.Monospace,
              )
            }
            BasicTextField(
              value = mottoDraft,
              onValueChange = { mottoDraft = it.take(48) },
              singleLine = true,
              cursorBrush = SolidColor(Color(0xFF00F5D4)),
              textStyle =
                TextStyle(
                  color = Color(0xFFCBD5E1),
                  fontSize = 7.8.sp,
                  fontFamily = FontFamily.Monospace,
                ),
              modifier = Modifier.fillMaxWidth().testTag("syndicate_charter_motto_input"),
            )
          }

          val canCharter = tagDraft.length >= 2 && nameDraft.trim().length >= 3
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .heightIn(min = 48.dp)
                .clickable(enabled = canCharter) {
                  onCharterCustomSyndicate(tagDraft, nameDraft.trim(), mottoDraft.trim())
                }
                .padding(vertical = 4.dp)
                .testTag("syndicate_btn_charter"),
            contentAlignment = Alignment.Center,
          ) {
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(7.dp))
                  .background(if (canCharter) Color(0xFF0D2D46) else Color(0xFF0B1322))
                  .border(1.dp, if (canCharter) Color(0xFF00F5D4) else Color(0xFF334155), RoundedCornerShape(7.dp))
                  .padding(vertical = 8.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "CHARTER SYNDICATE >",
                color = if (canCharter) Color.White else Color(0xFF64748B),
                fontSize = 8.2.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun SquadronLeftPilotListBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  filter: SquadronPilotFilter,
  onSelectFilter: (SquadronPilotFilter) -> Unit,
  sortMode: SquadronPilotSort,
  onToggleSort: () -> Unit,
  pilots: List<SquadronOnlinePilotItem>,
  selectedPilotUid: String,
  squadronStatusBanner: String?,
  onBackToHub: () -> Unit,
  onBroadcastLfg: () -> Unit,
  onAddFriendCallsign: (String) -> Unit,
  onTapPilot: (SquadronOnlinePilotItem) -> Unit,
) {
  var friendDraft by remember { mutableStateOf("") }
  var friendSearchQuery by remember { mutableStateOf("") }
  val filteredList =
    remember(pilots, friendSearchQuery) {
      val q = friendSearchQuery.trim().uppercase(Locale.US)
      if (q.isEmpty()) pilots
      else pilots.filter { it.callsign.uppercase(Locale.US).contains(q) || it.syndicateTag.uppercase(Locale.US).contains(q) }
    }

  Column(
    modifier = Modifier.fillMaxSize().testTag("squadron_left_pilot_list_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = false,
      isTabletDeck = isTabletDeck,
      sectionTitle = "SQUADRON",
      accent = Color(0xFF00F5D4),
      trailingLabel = "LFG PING",
      trailingTag = "squadron_btn_lfg",
      onTrailingClick = onBroadcastLfg,
      onBackToHub = onBackToHub,
    )

    // Untruncated Filter Row: ALL | SYNDICATE | FRIENDS
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(48.dp)
          .chamferedSciFiPodSurface(
            isRightSide = false,
            primaryBorder = Color(0xFF38BDF8),
            secondaryBorder = Color(0xFF091528),
          )
          .padding(horizontal = 3.dp),
      horizontalArrangement = Arrangement.spacedBy(2.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      listOf(
        SquadronPilotFilter.ALL to "ALL",
        SquadronPilotFilter.SYNDICATE to "SYNDICATE",
        SquadronPilotFilter.FRIENDS to "FRIENDS",
      ).forEach { (tab, label) ->
        val sel = filter == tab
        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxHeight()
              .minimumInteractiveComponentSize()
              .clickable { onSelectFilter(tab) }
              .padding(vertical = 5.dp)
              .testTag("squadron_filter_${tab.name}"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.fillMaxSize()
                .clip(RoundedCornerShape(5.dp))
                .background(if (sel) Color(0x3000F5D4) else Color(0x6602050B))
                .border(1.dp, if (sel) Color(0xFF00F5D4) else Color(0x3338BDF8), RoundedCornerShape(5.dp)),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = label,
              color = if (sel) Color.White else Color(0xFF94A3B8),
              fontSize = if (isTabletDeck) 7.8.sp else 6.9.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
          }
        }
      }
      Box(
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .fillMaxHeight()
            .clickable { onToggleSort() }
            .padding(vertical = 5.dp)
            .testTag("squadron_toggle_sort"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(5.dp))
              .background(Color(0xFF081426))
              .border(1.dp, Color(0xFFFACC15), RoundedCornerShape(5.dp))
              .padding(horizontal = 6.dp, vertical = 4.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (sortMode == SquadronPilotSort.RESONANCE) "SORT: RES" else "SORT: ACT",
            color = Color(0xFFFACC15),
            fontSize = 6.8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
          )
        }
      }
    }

    // Add Friend by Callsign + Quick Search Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.weight(1f)
            .minimumInteractiveComponentSize()
            .heightIn(min = 48.dp)
            .chamferedSciFiPodSurface(
              isRightSide = false,
              primaryBorder = Color(0xFF38BDF8),
              secondaryBorder = Color(0xFF091528),
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart,
      ) {
        if (friendDraft.isEmpty()) {
          Text(
            text = "Callsign to add friend or filter...",
            color = Color(0xFF64748B),
            fontSize = 7.4.sp,
            fontFamily = FontFamily.Monospace,
          )
        }
        BasicTextField(
          value = friendDraft,
          onValueChange = { raw ->
            val clean = raw.uppercase(Locale.US).replace(Regex("[^A-Z0-9_\\-]"), "").take(14)
            friendDraft = clean
            friendSearchQuery = clean
          },
          singleLine = true,
          cursorBrush = SolidColor(Color(0xFF00F5D4)),
          textStyle =
            TextStyle(
              color = Color.White,
              fontSize = 8.0.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            ),
          modifier = Modifier.fillMaxWidth().testTag("squadron_friend_search_input"),
        )
      }
      Box(
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .height(48.dp)
            .clickable(enabled = friendDraft.length >= 2) {
              onAddFriendCallsign(friendDraft)
              friendDraft = ""
              friendSearchQuery = ""
            }
            .padding(vertical = 4.dp)
            .testTag("squadron_btn_add_friend"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(6.dp))
              .background(if (friendDraft.length >= 2) Color(0xFF0D2D46) else Color(0xFF0B1322))
              .border(
                1.dp,
                if (friendDraft.length >= 2) Color(0xFFFACC15) else Color(0xFF334155),
                RoundedCornerShape(6.dp),
              )
              .padding(horizontal = 8.dp, vertical = 7.dp),
        ) {
          Text(
            text = "+ FRIEND",
            color = if (friendDraft.length >= 2) Color(0xFFFACC15) else Color(0xFF64748B),
            fontSize = 7.4.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
      }
    }

    if (!squadronStatusBanner.isNullOrBlank()) {
      Text(
        text = squadronStatusBanner,
        color = Color(0xFF00F5D4),
        fontSize = 7.4.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(horizontal = 4.dp),
      )
    }

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      if (filteredList.isEmpty()) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .chamferedSciFiPodSurface(
                isRightSide = false,
                primaryBorder = Color(0xFF38BDF8),
                secondaryBorder = Color(0xFF091528),
              )
              .padding(10.dp),
        ) {
          Text(
            text = "NO MATCHING ONLINE PILOTS RIGHT NOW. USE LFG PING OR ADD A FRIEND CALLSIGN ABOVE.",
            color = Color(0xFF94A3B8),
            fontSize = 7.6.sp,
            lineHeight = 11.0.sp,
            fontFamily = FontFamily.Monospace,
          )
        }
      } else {
        filteredList.forEach { pilot ->
          val isSel = selectedPilotUid.isNotBlank() && selectedPilotUid == pilot.uid
          val tagStr = if (pilot.syndicateTag.isNotBlank()) "[${pilot.syndicateTag}] " else ""
          val shipId = (if (pilot.lockedShipId in 1..10) pilot.lockedShipId else pilot.bestShipId).coerceIn(1, 10)
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .heightIn(min = 48.dp)
                .chamferedSciFiPodSurface(
                  isRightSide = false,
                  primaryBorder = if (isSel) Color(0xFF00F5D4) else Color(0xFF38BDF8),
                  secondaryBorder = if (pilot.isInSquadron) Color(0xFFFFD700) else Color(0xFF091528),
                )
                .clickable { onTapPilot(pilot) }
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .testTag("squadron_pilot_${pilot.uid}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Box(
              modifier =
                Modifier.size(7.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF10B981)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
              Text(
                text = "$tagStr${pilot.callsign}",
                color = Color.White,
                fontSize = 8.0.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                ResonanceCrestIcon(sizeDp = 10.dp)
                Text(
                  text = "${String.format(Locale.US, "%,d", pilot.resonance)} • ${cleanCodexShipName(RiftCodex.getShipById(shipId).name)}",
                  color = Color(0xFFFFD700),
                  fontSize = 7.2.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            Text(
              text = if (pilot.isInSquadron) "SQUAD" else "INSPECT >",
              color = if (pilot.isInSquadron) Color(0xFF00F5D4) else Color(0xFF38BDF8),
              fontSize = 7.4.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }
    }
  }
}

@Composable
internal fun SquadronRightPlayfieldShipBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  localCallsign: String,
  localSyndicateTag: String,
  localShipId: Int,
  localShipResonance: Int,
  isLocalShipCharged: Boolean,
  inspectedPilot: SquadronOnlinePilotItem?,
  armadaName: String,
  armadaSlots: List<ArmadaMemberSlot>,
  shipSpinDeg: Float,
  fluxOrbitRad: Float,
  onLeaveSquadron: () -> Unit,
  onInspectPilotPrompt: (SquadronOnlinePilotItem) -> Unit,
  onOpenFullSquadronModal: () -> Unit,
) {
  val context = LocalContext.current
  var voiceEnabled by remember { mutableStateOf(RiftCalibrationState.squadronVoiceEnabled) }
  val targetShipId = (inspectedPilot?.lockedShipId ?: localShipId).coerceIn(1, 10)
  val targetResonance = inspectedPilot?.resonance ?: localShipResonance
  val targetName =
    if (inspectedPilot != null) {
      if (inspectedPilot.syndicateTag.isNotBlank()) "[${inspectedPilot.syndicateTag}] ${inspectedPilot.callsign}"
      else inspectedPilot.callsign
    } else {
      if (localSyndicateTag.isNotBlank()) "[$localSyndicateTag] $localCallsign" else localCallsign
    }
  val targetShipSpec = remember(targetShipId, RiftSpriteBaker.customShip9Version) { RiftCodex.getShipById(targetShipId) }
  val cleanShipTitle = remember(targetShipSpec.name) { cleanCodexShipName(targetShipSpec.name) }

  val avgSquadResonance =
    remember(armadaSlots, localShipResonance) {
      val vals = armadaSlots.map { it.resonance }.ifEmpty { listOf(localShipResonance) }
      vals.average().roundToInt()
    }

  Column(
    modifier = Modifier.fillMaxSize().testTag("squadron_right_formation_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = true,
      isTabletDeck = isTabletDeck,
      sectionTitle = if (armadaName.isNotBlank()) "SQUAD [$armadaName]" else "5-SLOT FORMATION",
      accent = Color(0xFF38BDF8),
      trailingLabel = "ROSTER >",
      trailingTag = "squadron_bay_open_full_roster",
      onTrailingClick = onOpenFullSquadronModal,
    )

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      // Playfield-Style 2.5D Locked-In Ship Stage: [ResonanceCrestIcon] Res ABOVE, 2.5D Ship CENTER, [TAG] Callsign BELOW
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .heightIn(min = if (isTabletDeck) 176.dp else 148.dp)
            .chamferedSciFiPodSurface(
              isRightSide = true,
              primaryBorder = Color(0xFF00F5D4),
              secondaryBorder = Color(0xFFFFD700),
            )
            .clickable(enabled = inspectedPilot != null) {
              if (inspectedPilot != null) onInspectPilotPrompt(inspectedPilot)
            }
            .padding(8.dp)
            .testTag("squadron_playfield_ship_stage"),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Row(
            modifier =
              Modifier.clip(RoundedCornerShape(8.dp))
                .background(Color(0xD902050B))
                .border(1.dp, Color(0xAAFFD700), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            ResonanceCrestIcon(sizeDp = 13.dp)
            Text(
              text = String.format(Locale.US, "%,d", targetResonance),
              color = Color(0xFFFFD700),
              fontSize = 10.0.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }

          Spinning25dShipWithOrbitingFlux(
            shipId = targetShipId,
            shipSpinDeg = shipSpinDeg,
            fluxOrbitRad = fluxOrbitRad,
            isCharged = inspectedPilot?.isInSquadron ?: isLocalShipCharged,
            sizeDp = if (isTabletDeck) 84.dp else 66.dp,
          )

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = targetName,
              color = Color.White,
              fontSize = 9.4.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "LOCKED // $cleanShipTitle",
              color = Color(0xFF00F5D4),
              fontSize = 7.2.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }

      // Squadron Average Resonance, Squadron Voice Toggle & Leave Squadron Controls
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = true,
              primaryBorder = Color(0xFFFFD700),
              secondaryBorder = Color(0xFF091528),
            )
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = "SQUADRON AVG",
            color = Color(0xFFCBD5E1),
            fontSize = 7.4.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          ResonanceCrestIcon(sizeDp = 11.dp)
          Text(
            text = String.format(Locale.US, "%,d", avgSquadResonance),
            color = Color(0xFFFFD700),
            fontSize = 8.0.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Box(
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .height(48.dp)
                .clickable {
                  val next = !voiceEnabled
                  voiceEnabled = next
                  RiftCalibrationState.squadronVoiceEnabled = next
                  RiftCalibrationState.savePersistedLayout(context)
                }
                .padding(vertical = 8.dp)
                .testTag("squadron_voice_toggle"),
            contentAlignment = Alignment.Center,
          ) {
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(5.dp))
                  .background(if (voiceEnabled) Color(0x2800F5D4) else Color(0xFF091528))
                  .border(1.dp, if (voiceEnabled) Color(0xFF00F5D4) else Color(0xFF475569), RoundedCornerShape(5.dp))
                  .padding(horizontal = 6.dp, vertical = 4.dp),
            ) {
              Text(
                text = if (voiceEnabled) "VOICE ON" else "VOICE OFF",
                color = if (voiceEnabled) Color(0xFF00F5D4) else Color(0xFF94A3B8),
                fontSize = 6.8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
          if (armadaSlots.size > 1) {
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable { onLeaveSquadron() }
                  .padding(vertical = 8.dp)
                  .testTag("squadron_btn_leave"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF3B0A18))
                    .border(1.dp, Color(0xFFFB7185), RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
              ) {
                Text(
                  text = "LEAVE",
                  color = Color(0xFFFB7185),
                  fontSize = 6.8.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
          }
        }
      }

      // 5-Slot Vertical Card Formation (Slots 1..5)
      for (slotIdx in 0 until 5) {
        val member = armadaSlots.getOrNull(slotIdx)
        val slotNum = slotIdx + 1
        if (member != null) {
          val tagStr = if (member.syndicateTag.isNotBlank()) "[${member.syndicateTag}] " else ""
          RiftVerticalCard(
            modifier = Modifier.fillMaxWidth(),
            title = "SLOT $slotNum • $tagStr${member.callsign}",
            subtitle = if (member.isLeader) "Squadron Leader" else "Active Wingmate",
            accent = if (member.isLeader) Color(0xFFFFD700) else Color(0xFF00F5D4),
            secondaryAccent = Color(0xFF38BDF8),
            isRightColumn = true,
            isHighlighted = member.isLeader,
            badgeText = "${String.format(Locale.US, "%,d", member.resonance)}",
            badgeColor = Color(0xFFFFD700),
            isTabletDeck = isTabletDeck,
            minHeightDp = if (isTabletDeck) 90.dp else 78.dp,
            testTag = "squadron_slot_card_$slotNum",
            visualContent = { sizeDp ->
              Spinning25dShipWithOrbitingFlux(
                shipId = member.shipId.coerceIn(1, 10),
                shipSpinDeg = shipSpinDeg,
                fluxOrbitRad = fluxOrbitRad,
                isCharged = member.isOnline,
                sizeDp = (sizeDp * 0.86f).coerceAtLeast(32.dp),
              )
            },
            onClick = { onOpenFullSquadronModal() },
          )
        } else {
          RiftVerticalCard(
            modifier = Modifier.fillMaxWidth(),
            title = "SLOT $slotNum • OPEN SLOT",
            subtitle = "Invite an online pilot",
            accent = Color(0xFF475569),
            secondaryAccent = Color(0xFF1E293B),
            isRightColumn = true,
            isHighlighted = false,
            badgeText = "OPEN",
            badgeColor = Color(0xFF94A3B8),
            isTabletDeck = isTabletDeck,
            minHeightDp = if (isTabletDeck) 80.dp else 70.dp,
            testTag = "squadron_slot_card_$slotNum",
            visualContent = { sizeDp ->
              Box(
                modifier =
                  Modifier.size((sizeDp * 0.52f).coerceAtLeast(22.dp))
                    .clip(CircleShape)
                    .border(1.dp, Color(0xFF475569), CircleShape),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "+",
                  color = Color(0xFF94A3B8),
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            },
            onClick = { onOpenFullSquadronModal() },
          )
        }
      }
    }
  }
}

@Composable
internal fun GameModesLeftRankedBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  activeRankedMode: RankedGameMode,
  skirmishBracket: Int,
  onSelectSkirmishBracket: (Int) -> Unit,
  riftControlBracket: Int,
  onSelectRiftControlBracket: (Int) -> Unit,
  syndicateWarBracket: Int,
  onSelectSyndicateWarBracket: (Int) -> Unit,
  humanPilotCountByMode: (RankedGameMode) -> Int,
  canQueueSyndicateWars: Pair<Boolean, String>,
  overwatchInstances: List<OverwatchRiftInstance>,
  onSelectOverwatchKey: (String) -> Unit,
  onBackToHub: () -> Unit,
  onInspectMode: (RankedGameMode, Int, Boolean) -> Unit,
) {
  val modeAccentMap = remember {
    mapOf(
      RankedGameMode.SECTOR_CONVERGENCE to Color(0xFF00F5D4),
      RankedGameMode.FLUX_CORE to Color(0xFFFACC15),
      RankedGameMode.RIFT_SKIRMISH to Color(0xFF38BDF8),
      RankedGameMode.RIFT_CONTROL to Color(0xFF34D399),
      RankedGameMode.STASIS_BREACH to Color(0xFFC084FC),
      RankedGameMode.SYNDICATE_WARS to Color(0xFFFB7185),
    )
  }

  Column(
    modifier = Modifier.fillMaxSize().testTag("game_modes_left_ranked_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = false,
      isTabletDeck = isTabletDeck,
      sectionTitle = "ACTIVE RIFTS",
      accent = Color(0xFFFFD700),
      onBackToHub = onBackToHub,
    )

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      RankedGameMode.entries.forEach { mode ->
        val isActive = mode == activeRankedMode
        val accentCol = modeAccentMap[mode] ?: Color(0xFF00F5D4)
        val humanCount = humanPilotCountByMode(mode)
        val bracketForMode =
          when (mode) {
            RankedGameMode.RIFT_SKIRMISH -> skirmishBracket
            RankedGameMode.RIFT_CONTROL -> riftControlBracket
            RankedGameMode.SYNDICATE_WARS -> syndicateWarBracket
            else -> 0
          }
        val canDeploy =
          mode != RankedGameMode.SYNDICATE_WARS || canQueueSyndicateWars.first
        val shortDesc =
          when (mode) {
            RankedGameMode.SECTOR_CONVERGENCE -> "Open 5x Safe Harbor arena"
            RankedGameMode.FLUX_CORE -> "Hold central Flux Core orb"
            RankedGameMode.RIFT_SKIRMISH -> "Matched team dogfighting"
            RankedGameMode.RIFT_CONTROL -> "Control Alpha & Omega rings"
            RankedGameMode.STASIS_BREACH -> "Breach enemy Brig shield"
            RankedGameMode.SYNDICATE_WARS -> "Chartered Syndicate wars"
          }

        RiftVerticalCard(
          modifier = Modifier.fillMaxWidth(),
          title = mode.title.uppercase(Locale.US),
          subtitle = shortDesc,
          accent = accentCol,
          secondaryAccent = if (isActive) Color(0xFFFFD700) else Color(0xFF38BDF8),
          isRightColumn = false,
          isHighlighted = isActive,
          badgeText = if (humanCount == 1) "1 PILOT" else "$humanCount LIVE",
          badgeColor = accentCol,
          isTabletDeck = isTabletDeck,
          minHeightDp = if (isTabletDeck) 98.dp else 86.dp,
          testTag = "game_mode_card_${mode.name}",
          visualContent = { sizeDp ->
            ActiveRiftsEmblem(sizeDp = (sizeDp * 0.78f).coerceAtLeast(24.dp))
          },
          onClick = {
            val matchingInstance =
              overwatchInstances.firstOrNull { it.customConfig == null && it.rankedMode == mode }
            if (matchingInstance != null) {
              onSelectOverwatchKey(matchingInstance.key)
            }
            onInspectMode(mode, bracketForMode, canDeploy)
          },
        )

        if (mode == RankedGameMode.RIFT_SKIRMISH ||
          mode == RankedGameMode.RIFT_CONTROL ||
          mode == RankedGameMode.SYNDICATE_WARS
        ) {
          val options =
            if (mode == RankedGameMode.RIFT_SKIRMISH) listOf(2, 3, 4, 5)
            else listOf(3, 5, 10)
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            options.forEach { size ->
              val sel = bracketForMode == size
              Box(
                modifier =
                  Modifier.weight(1f)
                    .minimumInteractiveComponentSize()
                    .heightIn(min = 48.dp)
                    .clickable {
                      when (mode) {
                        RankedGameMode.RIFT_SKIRMISH -> onSelectSkirmishBracket(size)
                        RankedGameMode.RIFT_CONTROL -> onSelectRiftControlBracket(size)
                        RankedGameMode.SYNDICATE_WARS -> onSelectSyndicateWarBracket(size)
                        else -> Unit
                      }
                    }
                    .padding(vertical = 6.dp)
                    .testTag("game_mode_bracket_${mode.name}_$size"),
                contentAlignment = Alignment.Center,
              ) {
                Box(
                  modifier =
                    Modifier.fillMaxWidth()
                      .clip(RoundedCornerShape(5.dp))
                      .background(if (sel) accentCol.copy(alpha = 0.24f) else Color(0xFF060D1A))
                      .border(1.dp, if (sel) accentCol else Color(0xFF334155), RoundedCornerShape(5.dp))
                      .padding(vertical = 5.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = "${size}V${size}",
                    color = if (sel) Color.White else Color(0xFF94A3B8),
                    fontSize = 7.4.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun GameModesRightCustomBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  publicCustomGames: List<CustomGameConfig>,
  engineCustomGames: List<CustomGameConfig>,
  unlockedPrivateCodes: List<String>,
  customIsPrivateRoom: Boolean,
  onTogglePrivateRoom: (Boolean) -> Unit,
  generatedRoomCode: String,
  joinRoomCodeDraft: String,
  onJoinRoomCodeDraftChange: (String) -> Unit,
  roomCodeBanner: String?,
  onJoinByRoomCode: () -> Unit,
  customBaseMode: RankedGameMode,
  onSelectCustomBaseMode: (RankedGameMode) -> Unit,
  customDamageTakenScalePct: Int,
  onSelectCustomDamageTakenScalePct: (Int) -> Unit,
  customWeaponFluxCostScalePct: Int,
  onSelectCustomWeaponFluxCostScalePct: (Int) -> Unit,
  customBotCount: Int,
  onSelectCustomBotCount: (Int) -> Unit,
  customBotSkillLevel: Int,
  onSelectCustomBotSkillLevel: (Int) -> Unit,
  customResetMinutes: Int,
  onSelectCustomResetMinutes: (Int) -> Unit,
  customScoreCap: Int,
  onSelectCustomScoreCap: (Int) -> Unit,
  customBulletSizeMult: Float,
  onSelectCustomBulletSizeMult: (Float) -> Unit,
  onJoinCustomInstance: (String) -> Unit,
  onQuickCreateCustomGame: () -> Unit,
  onOpenFullCustomGate: () -> Unit,
) {
  var rightTab by remember { mutableIntStateOf(0) } // 0 = PUBLIC BROWSER, 1 = CREATE RIFT
  val publicVisibleRooms =
    remember(publicCustomGames, engineCustomGames, unlockedPrivateCodes) {
      val merged = LinkedHashMap<String, CustomGameConfig>()
      engineCustomGames.forEach { merged[it.instanceId] = it }
      publicCustomGames.forEach { merged.putIfAbsent(it.instanceId, it) }
      merged.values.filter { !it.privateRoom || it.roomCode in unlockedPrivateCodes }
    }
  val botSkillLabel =
    when (customBotSkillLevel.coerceIn(0, 3)) {
      0 -> "RECRUIT"
      1 -> "ACE"
      2 -> "VETERAN"
      else -> "APEX"
    }

  Column(
    modifier = Modifier.fillMaxSize().testTag("game_modes_right_custom_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = true,
      isTabletDeck = isTabletDeck,
      sectionTitle = "CUSTOM RIFTS",
      accent = Color(0xFF00F5D4),
      trailingLabel = "FULL GATE >",
      trailingTag = "game_modes_open_full_gate",
      onTrailingClick = onOpenFullCustomGate,
    )

    // Tab Switcher: PUBLIC BROWSER | CREATE RIFT
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(48.dp)
          .chamferedSciFiPodSurface(
            isRightSide = true,
            primaryBorder = Color(0xFF38BDF8),
            secondaryBorder = Color(0xFF091528),
          )
          .padding(horizontal = 3.dp),
      horizontalArrangement = Arrangement.spacedBy(3.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      listOf("PUBLIC BROWSER", "CREATE RIFT").forEachIndexed { idx, label ->
        val sel = rightTab == idx
        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxHeight()
              .minimumInteractiveComponentSize()
              .clickable { rightTab = idx }
              .padding(vertical = 5.dp)
              .testTag("custom_right_tab_$idx"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.fillMaxSize()
                .clip(RoundedCornerShape(5.dp))
                .background(if (sel) Color(0x3000F5D4) else Color(0x6602050B))
                .border(1.dp, if (sel) Color(0xFF00F5D4) else Color(0x3338BDF8), RoundedCornerShape(5.dp)),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = label,
              color = if (sel) Color.White else Color(0xFF94A3B8),
              fontSize = 7.6.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
          }
        }
      }
    }

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      // Join Private Room by 6-Character Code
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .heightIn(min = 48.dp)
              .chamferedSciFiPodSurface(
                isRightSide = true,
                primaryBorder = Color(0xFF38BDF8),
                secondaryBorder = Color(0xFF091528),
              )
              .padding(horizontal = 8.dp, vertical = 8.dp),
          contentAlignment = Alignment.CenterStart,
        ) {
          if (joinRoomCodeDraft.isEmpty()) {
            Text(
              text = "6-CHAR ROOM CODE",
              color = Color(0xFF64748B),
              fontSize = 7.6.sp,
              fontFamily = FontFamily.Monospace,
            )
          }
          BasicTextField(
            value = joinRoomCodeDraft,
            onValueChange = onJoinRoomCodeDraftChange,
            singleLine = true,
            cursorBrush = SolidColor(Color(0xFF00F5D4)),
            textStyle =
              TextStyle(
                color = Color.White,
                fontSize = 8.4.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              ),
            modifier = Modifier.fillMaxWidth().testTag("custom_join_code_input"),
          )
        }
        Box(
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .height(48.dp)
              .clickable(enabled = joinRoomCodeDraft.length >= 4) { onJoinByRoomCode() }
              .padding(vertical = 4.dp)
              .testTag("custom_btn_join_code"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0D2D46))
                .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(6.dp))
                .padding(horizontal = 9.dp, vertical = 7.dp),
          ) {
            Text(
              text = "JOIN",
              color = Color.White,
              fontSize = 7.8.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }

      if (!roomCodeBanner.isNullOrBlank()) {
        Text(
          text = roomCodeBanner,
          color = Color(0xFFFACC15),
          fontSize = 7.4.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.padding(horizontal = 4.dp),
        )
      }

      if (rightTab == 0) {
        // Scrollable Public Custom Rifts Browser
        if (publicVisibleRooms.isEmpty()) {
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .chamferedSciFiPodSurface(
                  isRightSide = true,
                  primaryBorder = Color(0xFF38BDF8),
                  secondaryBorder = Color(0xFF091528),
                )
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = "NO PUBLIC CUSTOM RIFTS LIVE",
              color = Color(0xFFFFD700),
              fontSize = 8.2.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
            Text(
              text = "Switch to CREATE RIFT to launch a custom match or enter a 6-character code above.",
              color = Color(0xFFCBD5E1),
              fontSize = 7.6.sp,
              fontFamily = FontFamily.SansSerif,
            )
          }
        } else {
          publicVisibleRooms.forEach { room ->
            RiftVerticalCard(
              modifier = Modifier.fillMaxWidth(),
              title = room.title.uppercase(Locale.US),
              subtitle = "Host ${room.hostCallsign} • ${room.damageTakenScalePct}% DMG • ${room.weaponFluxCostScalePct}% FLUX",
              accent = if (room.privateRoom) Color(0xFFC084FC) else Color(0xFF00F5D4),
              secondaryAccent = Color(0xFFFFD700),
              isRightColumn = true,
              badgeText = if (room.privateRoom) "CODE ${room.roomCode}" else "PUBLIC",
              badgeColor = if (room.privateRoom) Color(0xFFC084FC) else Color(0xFF00F5D4),
              isTabletDeck = isTabletDeck,
              minHeightDp = if (isTabletDeck) 94.dp else 82.dp,
              testTag = "custom_public_room_${room.instanceId}",
              visualContent = { sizeDp ->
                ActiveRiftsEmblem(sizeDp = (sizeDp * 0.72f).coerceAtLeast(22.dp))
              },
              onClick = { onJoinCustomInstance(room.instanceId) },
            )
          }
        }
      } else {
        // CREATE RIFT Rule Customizer (Public/Private + 5%-Stepped Damage Taken & Flux Cost + Bots/Time/Score/Scale)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Box(
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .heightIn(min = 48.dp)
                .chamferedSciFiPodSurface(
                  isRightSide = true,
                  primaryBorder = Color(0xFF38BDF8),
                  secondaryBorder = Color(0xFF091528),
                )
                .clickable { onTogglePrivateRoom(!customIsPrivateRoom) }
                .padding(horizontal = 6.dp)
                .testTag("custom_toggle_privacy"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = if (customIsPrivateRoom) "PRIVATE [$generatedRoomCode]" else "PUBLIC ROOM",
              color = if (customIsPrivateRoom) Color(0xFFC084FC) else Color(0xFF00F5D4),
              fontSize = 7.6.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
          }
          Box(
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .heightIn(min = 48.dp)
                .chamferedSciFiPodSurface(
                  isRightSide = true,
                  primaryBorder = Color(0xFFFACC15),
                  secondaryBorder = Color(0xFF091528),
                )
                .clickable {
                  val modes =
                    listOf(
                      RankedGameMode.SECTOR_CONVERGENCE,
                      RankedGameMode.RIFT_SKIRMISH,
                      RankedGameMode.FLUX_CORE,
                      RankedGameMode.STASIS_BREACH,
                      RankedGameMode.RIFT_CONTROL,
                    )
                  val nextIdx = (modes.indexOf(customBaseMode) + 1) % modes.size
                  onSelectCustomBaseMode(modes[nextIdx])
                }
                .padding(horizontal = 6.dp)
                .testTag("hub_custom_cycle_base_mode"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = customBaseMode.title.uppercase(Locale.US),
              color = Color.White,
              fontSize = 7.4.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Column(
          modifier =
            Modifier.fillMaxWidth()
              .chamferedSciFiPodSurface(
                isRightSide = true,
                primaryBorder = Color(0xFF38BDF8),
                secondaryBorder = Color(0xFF091528),
              )
              .padding(horizontal = 8.dp, vertical = 4.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          HubCustomFivePercentStepperRow(
            label = "DAMAGE TAKEN",
            valuePct = customDamageTakenScalePct,
            minusTag = "custom_dmg_minus",
            valueTag = "custom_dmg_val",
            plusTag = "custom_dmg_plus",
            onMinus = { onSelectCustomDamageTakenScalePct((customDamageTakenScalePct - 5).coerceIn(0, 100)) },
            onPlus = { onSelectCustomDamageTakenScalePct((customDamageTakenScalePct + 5).coerceIn(0, 100)) },
          )
          HubCustomFivePercentStepperRow(
            label = "WEAPON FLUX COST",
            valuePct = customWeaponFluxCostScalePct,
            minusTag = "custom_flux_minus",
            valueTag = "custom_flux_val",
            plusTag = "custom_flux_plus",
            onMinus = { onSelectCustomWeaponFluxCostScalePct((customWeaponFluxCostScalePct - 5).coerceIn(0, 100)) },
            onPlus = { onSelectCustomWeaponFluxCostScalePct((customWeaponFluxCostScalePct + 5).coerceIn(0, 100)) },
          )
        }

        HelmStepperControlRow(
          label = "BOT COUNT",
          valueText = "$customBotCount",
          onMinus = { onSelectCustomBotCount((customBotCount - 1).coerceIn(0, 19)) },
          onPlus = { onSelectCustomBotCount((customBotCount + 1).coerceIn(0, 19)) },
        )
        HelmStepperControlRow(
          label = "BOT SKILL",
          valueText = botSkillLabel,
          onMinus = { onSelectCustomBotSkillLevel((customBotSkillLevel - 1).coerceIn(0, 3)) },
          onPlus = { onSelectCustomBotSkillLevel((customBotSkillLevel + 1).coerceIn(0, 3)) },
        )
        HelmStepperControlRow(
          label = "TIME LIMIT",
          valueText = "${customResetMinutes}M",
          onMinus = { onSelectCustomResetMinutes((customResetMinutes - 5).coerceIn(5, 60)) },
          onPlus = { onSelectCustomResetMinutes((customResetMinutes + 5).coerceIn(5, 60)) },
        )
        HelmStepperControlRow(
          label = "SCORE CAP",
          valueText = "$customScoreCap",
          onMinus = { onSelectCustomScoreCap((customScoreCap - 10).coerceIn(10, 200)) },
          onPlus = { onSelectCustomScoreCap((customScoreCap + 10).coerceIn(10, 200)) },
        )
        HelmStepperControlRow(
          label = "BULLET SCALE",
          valueText = String.format(Locale.US, "%.2fx", customBulletSizeMult),
          onMinus = { onSelectCustomBulletSizeMult((customBulletSizeMult - 0.15f).coerceIn(0.70f, 1.75f)) },
          onPlus = { onSelectCustomBulletSizeMult((customBulletSizeMult + 0.15f).coerceIn(0.70f, 1.75f)) },
        )

        Box(
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .heightIn(min = 48.dp)
              .clickable { onQuickCreateCustomGame() }
              .padding(vertical = 4.dp)
              .testTag("hub_btn_quick_create_custom"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(7.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF0D2D46), Color(0xFF061626))))
                .border(1.2.dp, Color(0xFF00F5D4), RoundedCornerShape(7.dp))
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "CREATE RIFT >",
              color = Color.White,
              fontSize = 8.6.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }
    }
  }
}

@Composable
internal fun HubCustomFivePercentStepperRow(
  label: String,
  valuePct: Int,
  minusTag: String,
  valueTag: String,
  plusTag: String,
  onMinus: () -> Unit,
  onPlus: () -> Unit,
) {
  val snapped = snapCustomFivePercentScale(valuePct)
  Row(
    modifier = Modifier.fillMaxWidth().height(48.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      color = Color(0xFFCBD5E1),
      fontSize = 7.4.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
      maxLines = 1,
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
      Box(
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .size(48.dp)
            .clickable(enabled = snapped > 0) { onMinus() }
            .testTag(minusTag),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "[-5%]",
          color = if (snapped > 0) Color(0xFF00F5D4) else Color(0xFF475569),
          fontSize = 7.6.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
      }
      Text(
        text = "$snapped%",
        color = if (snapped == 100) Color(0xFFE2E8F0) else Color(0xFFFFD700),
        fontSize = 8.0.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.width(34.dp).testTag(valueTag),
        textAlign = TextAlign.Center,
      )
      Box(
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .size(48.dp)
            .clickable(enabled = snapped < 100) { onPlus() }
            .testTag(plusTag),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "[+5%]",
          color = if (snapped < 100) Color(0xFF00F5D4) else Color(0xFF475569),
          fontSize = 7.6.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
        )
      }
    }
  }
}

@Composable
internal fun HelmLeftSystemsBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  sfxEnabled: Boolean,
  musicEnabled: Boolean,
  musicBpm: Int,
  musicVolume: Float,
  sfxVolume: Float,
  cameraZoom: Float,
  playfieldTextScale: Float,
  onToggleSfx: (Boolean) -> Unit,
  onToggleMusic: (Boolean) -> Unit,
  onChangeMusicBpm: (Int) -> Unit,
  onChangeMusicVolume: (Float) -> Unit,
  onChangeSfxVolume: (Float) -> Unit,
  onChangeCameraZoom: (Float) -> Unit,
  onChangePlayfieldTextScale: (Float) -> Unit,
  onOpenChangelogModal: () -> Unit,
  onOpenAudioSynthForge: () -> Unit,
  onOpenStarForge: () -> Unit,
  onStartTutorial: () -> Unit,
  isNetworkOnline: Boolean,
  isGoogleAuthenticated: Boolean,
  cloudSyncHealth: CloudSyncHealth,
  updateStatusLabel: String,
  hasUpdateAvailable: Boolean,
  onCheckOrApplyUpdate: () -> Unit,
  onOpenSwitchAccountPrompt: () -> Unit,
  onBackToHub: () -> Unit,
) {
  val context = LocalContext.current
  var activeSubTab by remember { mutableIntStateOf(0) } // 0 = CONTROLS, 1 = DISPLAY, 2 = SYSTEM
  var touchFloating by remember { mutableStateOf(RiftCalibrationState.touchFloatingJoystick) }
  var swapControls by remember { mutableStateOf(RiftCalibrationState.swapLeftRightControls) }
  var touchDeadzone by remember { mutableStateOf(RiftCalibrationState.touchJoystickDeadzone) }
  var touchSens by remember { mutableStateOf(RiftCalibrationState.touchJoystickSensitivity) }
  var gpDeadLeft by remember { mutableStateOf(RiftCalibrationState.gamepadDeadzoneLeft) }
  var gpDeadRight by remember { mutableStateOf(RiftCalibrationState.gamepadDeadzoneRight) }
  var graphicsTier by remember { mutableIntStateOf(RiftCalibrationState.graphicsQualityTier) }
  var preferMaxHz by remember { mutableStateOf(RiftCalibrationState.preferMaxRefreshRate) }
  var selectedBindSlotId by remember { mutableStateOf<String?>(null) }

  Column(
    modifier = Modifier.fillMaxSize().testTag("helm_left_systems_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = false,
      isTabletDeck = isTabletDeck,
      sectionTitle = "HELM SYSTEMS",
      accent = Color(0xFF00F5D4),
      trailingLabel = "PATCH LOG",
      trailingTag = "helm_open_changelog",
      onTrailingClick = onOpenChangelogModal,
      onBackToHub = onBackToHub,
    )

    // 3 Split Sub-Tabs: CONTROLS | DISPLAY | SYSTEM
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(48.dp)
          .chamferedSciFiPodSurface(
            isRightSide = false,
            primaryBorder = Color(0xFF38BDF8),
            secondaryBorder = Color(0xFF091528),
          )
          .padding(horizontal = 3.dp),
      horizontalArrangement = Arrangement.spacedBy(2.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      listOf("CONTROLS", "DISPLAY", "SYSTEM").forEachIndexed { idx, label ->
        val sel = activeSubTab == idx
        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxHeight()
              .minimumInteractiveComponentSize()
              .clickable { activeSubTab = idx }
              .padding(vertical = 5.dp)
              .testTag("helm_systems_tab_$idx"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.fillMaxSize()
                .clip(RoundedCornerShape(5.dp))
                .background(if (sel) Color(0x3000F5D4) else Color(0x6602050B))
                .border(1.dp, if (sel) Color(0xFF00F5D4) else Color(0x3338BDF8), RoundedCornerShape(5.dp)),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = label,
              color = if (sel) Color.White else Color(0xFF94A3B8),
              fontSize = 7.6.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }
    }

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      when (activeSubTab) {
        0 -> {
          // CONTROLS: Touch Stick Mode, Deadzones, Sensitivity, Swap Left/Right, Gamepad Remapping
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Box(
              modifier =
                Modifier.weight(1f)
                  .minimumInteractiveComponentSize()
                  .height(48.dp)
                  .chamferedSciFiPodSurface(
                    isRightSide = false,
                    primaryBorder = Color(0xFF00F5D4),
                    secondaryBorder = Color(0xFF091528),
                  )
                  .clickable {
                    val next = !touchFloating
                    touchFloating = next
                    RiftCalibrationState.touchFloatingJoystick = next
                    RiftCalibrationState.savePersistedLayout(context)
                  }
                  .testTag("helm_toggle_stick_mode"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = if (touchFloating) "STICK: FLOATING" else "STICK: FIXED",
                color = Color(0xFF00F5D4),
                fontSize = 7.6.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
            Box(
              modifier =
                Modifier.weight(1f)
                  .minimumInteractiveComponentSize()
                  .height(48.dp)
                  .chamferedSciFiPodSurface(
                    isRightSide = false,
                    primaryBorder = if (swapControls) Color(0xFFFACC15) else Color(0xFF38BDF8),
                    secondaryBorder = Color(0xFF091528),
                  )
                  .clickable {
                    val next = !swapControls
                    swapControls = next
                    RiftCalibrationState.swapLeftRightControls = next
                    RiftCalibrationState.savePersistedLayout(context)
                  }
                  .testTag("helm_toggle_swap_controls"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = if (swapControls) "SWAP L/R: ON" else "SWAP L/R: OFF",
                color = if (swapControls) Color(0xFFFACC15) else Color(0xFF38BDF8),
                fontSize = 7.6.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
          }

          HelmStepperControlRow(
            label = "TOUCH DEADZONE",
            valueText = "${(touchDeadzone * 100f).roundToInt()}%",
            onMinus = {
              val v = (touchDeadzone - 0.02f).coerceIn(0.02f, 0.30f)
              touchDeadzone = v
              RiftCalibrationState.touchJoystickDeadzone = v
              RiftCalibrationState.savePersistedLayout(context)
            },
            onPlus = {
              val v = (touchDeadzone + 0.02f).coerceIn(0.02f, 0.30f)
              touchDeadzone = v
              RiftCalibrationState.touchJoystickDeadzone = v
              RiftCalibrationState.savePersistedLayout(context)
            },
          )

          HelmStepperControlRow(
            label = "TOUCH SENSITIVITY",
            valueText = String.format(Locale.US, "%.2fx", touchSens),
            onMinus = {
              val v = (touchSens - 0.10f).coerceIn(0.60f, 1.80f)
              touchSens = v
              RiftCalibrationState.touchJoystickSensitivity = v
              RiftCalibrationState.savePersistedLayout(context)
            },
            onPlus = {
              val v = (touchSens + 0.10f).coerceIn(0.60f, 1.80f)
              touchSens = v
              RiftCalibrationState.touchJoystickSensitivity = v
              RiftCalibrationState.savePersistedLayout(context)
            },
          )

          HelmStepperControlRow(
            label = "PAD L-DEADZONE",
            valueText = "${(gpDeadLeft * 100f).roundToInt()}%",
            onMinus = {
              val v = (gpDeadLeft - 0.02f).coerceIn(0.04f, 0.36f)
              gpDeadLeft = v
              RiftCalibrationState.gamepadDeadzoneLeft = v
              RiftCalibrationState.savePersistedLayout(context)
            },
            onPlus = {
              val v = (gpDeadLeft + 0.02f).coerceIn(0.04f, 0.36f)
              gpDeadLeft = v
              RiftCalibrationState.gamepadDeadzoneLeft = v
              RiftCalibrationState.savePersistedLayout(context)
            },
          )

          HelmStepperControlRow(
            label = "PAD R-DEADZONE",
            valueText = "${(gpDeadRight * 100f).roundToInt()}%",
            onMinus = {
              val v = (gpDeadRight - 0.02f).coerceIn(0.04f, 0.36f)
              gpDeadRight = v
              RiftCalibrationState.gamepadDeadzoneRight = v
              RiftCalibrationState.savePersistedLayout(context)
            },
            onPlus = {
              val v = (gpDeadRight + 0.02f).coerceIn(0.04f, 0.36f)
              gpDeadRight = v
              RiftCalibrationState.gamepadDeadzoneRight = v
              RiftCalibrationState.savePersistedLayout(context)
            },
          )

          // Hardware Controller Remapper
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .chamferedSciFiPodSurface(
                  isRightSide = false,
                  primaryBorder = Color(0xFF38BDF8),
                  secondaryBorder = Color(0xFF091528),
                )
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "GAMEPAD REMAPPING",
                color = Color(0xFF38BDF8),
                fontSize = 7.6.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
              Box(
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .clickable { RiftCalibrationState.resetControllerBindingsToDefault(context) }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "RESET DEFAULTS",
                  color = Color(0xFFFACC15),
                  fontSize = 7.0.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }
            LIVE_CONTROLLER_ACTION_SLOTS.forEach { slot ->
              val currentCode = RiftCalibrationState.getBindingForActionSlot(slot.slotId)
              val currentLabel = RiftCalibrationState.getBindingLabel(currentCode)
              val isEditing = selectedBindSlotId == slot.slotId
              Column(
                modifier =
                  Modifier.fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isEditing) slot.accentColor.copy(alpha = 0.16f) else Color(0x8802050B))
                    .border(
                      0.9.dp,
                      if (isEditing) slot.accentColor else Color(0x3338BDF8),
                      RoundedCornerShape(6.dp),
                    )
                    .clickable {
                      selectedBindSlotId = if (isEditing) null else slot.slotId
                    }
                    .padding(horizontal = 7.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = slot.title,
                    color = Color.White,
                    fontSize = 7.6.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                  )
                  Text(
                    text = if (isEditing) "PICK INPUT v" else currentLabel,
                    color = slot.accentColor,
                    fontSize = 7.6.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                  )
                }
                if (isEditing) {
                  Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    LIVE_HARDWARE_BIND_CHIPS.forEach { (code, chipLabel) ->
                      Box(
                        modifier =
                          Modifier.minimumInteractiveComponentSize()
                            .heightIn(min = 48.dp)
                            .clickable {
                              RiftCalibrationState.assignControllerBinding(slot.slotId, code, context)
                              selectedBindSlotId = null
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                      ) {
                        Box(
                          modifier =
                            Modifier.clip(RoundedCornerShape(5.dp))
                              .background(if (code == currentCode) slot.accentColor.copy(alpha = 0.28f) else Color(0xFF060D1A))
                              .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(5.dp))
                              .padding(horizontal = 7.dp, vertical = 4.dp),
                        ) {
                          Text(
                            text = chipLabel,
                            color = Color.White,
                            fontSize = 7.0.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
        1 -> {
          // AUDIO & DISPLAY: Music/SFX, Volumes, BPM, Arena Zoom, HUD Text Scale, Graphics Tier, 60Hz vs Max Hz
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Box(
              modifier =
                Modifier.weight(1f)
                  .minimumInteractiveComponentSize()
                  .height(48.dp)
                  .chamferedSciFiPodSurface(
                    isRightSide = false,
                    primaryBorder = if (musicEnabled) Color(0xFF00F5D4) else Color(0xFF475569),
                    secondaryBorder = Color(0xFF091528),
                  )
                  .clickable { onToggleMusic(!musicEnabled) }
                  .testTag("helm_toggle_music"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = if (musicEnabled) "MUSIC: ON" else "MUSIC: OFF",
                color = if (musicEnabled) Color(0xFF00F5D4) else Color(0xFF94A3B8),
                fontSize = 8.0.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
            Box(
              modifier =
                Modifier.weight(1f)
                  .minimumInteractiveComponentSize()
                  .height(48.dp)
                  .chamferedSciFiPodSurface(
                    isRightSide = false,
                    primaryBorder = if (sfxEnabled) Color(0xFF00F5D4) else Color(0xFF475569),
                    secondaryBorder = Color(0xFF091528),
                  )
                  .clickable { onToggleSfx(!sfxEnabled) }
                  .testTag("helm_toggle_sfx"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = if (sfxEnabled) "SFX: ON" else "SFX: OFF",
                color = if (sfxEnabled) Color(0xFF00F5D4) else Color(0xFF94A3B8),
                fontSize = 8.0.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            val tierLabel =
              when (graphicsTier.coerceIn(0, 2)) {
                0 -> "QUALITY: BATTERY"
                1 -> "QUALITY: BALANCED"
                else -> "QUALITY: HIGH"
              }
            Box(
              modifier =
                Modifier.weight(1f)
                  .minimumInteractiveComponentSize()
                  .height(48.dp)
                  .chamferedSciFiPodSurface(
                    isRightSide = false,
                    primaryBorder = Color(0xFF38BDF8),
                    secondaryBorder = Color(0xFF091528),
                  )
                  .clickable {
                    val next = (graphicsTier + 1) % 3
                    graphicsTier = next
                    RiftCalibrationState.graphicsQualityTier = next
                    RiftCalibrationState.savePersistedLayout(context)
                  }
                  .testTag("helm_toggle_graphics_tier"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = tierLabel,
                color = Color(0xFF38BDF8),
                fontSize = 7.4.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
            Box(
              modifier =
                Modifier.weight(1f)
                  .minimumInteractiveComponentSize()
                  .height(48.dp)
                  .chamferedSciFiPodSurface(
                    isRightSide = false,
                    primaryBorder = Color(0xFFFACC15),
                    secondaryBorder = Color(0xFF091528),
                  )
                  .clickable {
                    val next = !preferMaxHz
                    preferMaxHz = next
                    RiftCalibrationState.preferMaxRefreshRate = next
                    RiftCalibrationState.savePersistedLayout(context)
                  }
                  .testTag("helm_toggle_frame_pacing"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = if (preferMaxHz) "FPS: MAX HZ" else "FPS: 60 HZ",
                color = Color(0xFFFACC15),
                fontSize = 7.4.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
          }

          HelmStepperControlRow(
            label = "MUSIC VOL",
            valueText = "${(musicVolume * 100f).roundToInt()}%",
            onMinus = { onChangeMusicVolume((musicVolume - 0.10f).coerceIn(0f, 1f)) },
            onPlus = { onChangeMusicVolume((musicVolume + 0.10f).coerceIn(0f, 1f)) },
          )
          HelmStepperControlRow(
            label = "SFX VOL",
            valueText = "${(sfxVolume * 100f).roundToInt()}%",
            onMinus = { onChangeSfxVolume((sfxVolume - 0.10f).coerceIn(0f, 1f)) },
            onPlus = { onChangeSfxVolume((sfxVolume + 0.10f).coerceIn(0f, 1f)) },
          )
          HelmStepperControlRow(
            label = "SYNTH BPM",
            valueText = "$musicBpm",
            onMinus = { onChangeMusicBpm((musicBpm - 6).coerceIn(96, 168)) },
            onPlus = { onChangeMusicBpm((musicBpm + 6).coerceIn(96, 168)) },
          )
          HelmStepperControlRow(
            label = "ARENA ZOOM",
            valueText = String.format(Locale.US, "%.2fx", cameraZoom),
            onMinus = { onChangeCameraZoom((cameraZoom - 0.08f).coerceIn(0.65f, 1.45f)) },
            onPlus = { onChangeCameraZoom((cameraZoom + 0.08f).coerceIn(0.65f, 1.45f)) },
          )
          HelmStepperControlRow(
            label = "HUD TEXT",
            valueText = String.format(Locale.US, "%.2fx", playfieldTextScale),
            onMinus = { onChangePlayfieldTextScale((playfieldTextScale - 0.08f).coerceIn(0.80f, 1.35f)) },
            onPlus = { onChangePlayfieldTextScale((playfieldTextScale + 0.08f).coerceIn(0.80f, 1.35f)) },
          )

          Box(
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .height(48.dp)
                .clickable { onOpenAudioSynthForge() }
                .padding(vertical = 2.dp)
                .testTag("helm_open_audio_synth_forge"),
            contentAlignment = Alignment.Center,
          ) {
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(7.dp))
                  .background(Color(0xFF0D2D46))
                  .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(7.dp))
                  .padding(vertical = 6.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "AUDIO SYNTH LAB >",
                color = Color(0xFFFFD700),
                fontSize = 8.0.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
        }
        else -> {
          // SYSTEM: Tutorial, Star Forge (1:1 Studio), Cloud Sync Health, Check Update, Switch Account
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Box(
              modifier =
                Modifier.weight(0.95f)
                  .minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable { onStartTutorial() }
                  .padding(vertical = 2.dp)
                  .testTag("helm_start_tutorial"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF0D2D46), Color(0xFF091A2E))))
                    .border(1.2.dp, Color(0xFFFACC15), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "TUTORIAL >",
                  color = Color(0xFFFACC15),
                  fontSize = 8.4.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                )
              }
            }
            Box(
              modifier =
                Modifier.weight(1.25f)
                  .minimumInteractiveComponentSize()
                  .height(48.dp)
                  .clickable { onOpenStarForge() }
                  .padding(vertical = 2.dp)
                  .testTag("helm_open_star_forge"),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier.fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF23103C), Color(0xFF0D253E))))
                    .border(
                      1.2.dp,
                      Brush.horizontalGradient(listOf(Color(0xFFC084FC), Color(0xFF00F5D4), Color(0xFFFFD700))),
                      RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "STAR FORGE 1:1 >",
                  color = Color(0xFFF3E8FF),
                  fontSize = 8.2.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }

          val syncHealthy = isNetworkOnline && cloudSyncHealth.isConnected
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .chamferedSciFiPodSurface(
                  isRightSide = false,
                  primaryBorder = if (syncHealthy) Color(0xFF00F5D4) else Color(0xFFFACC15),
                  secondaryBorder = Color(0xFF091528),
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .testTag("helm_cloud_sync_health_pod"),
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(
              text =
                when {
                  !isNetworkOnline -> "CLOUD SYNC • OFFLINE LOCAL"
                  syncHealthy -> "CLOUD SYNC • LIVE CONNECTED"
                  isGoogleAuthenticated -> "CLOUD SYNC • RECONNECTING"
                  else -> "CLOUD SYNC • LOCAL PILOT"
                },
              color = if (syncHealthy) Color(0xFF00F5D4) else Color(0xFFFACC15),
              fontSize = 7.8.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
            Text(
              text = cloudSyncHealth.statusLabel.ifBlank { "Realtime sector presence & profile uplink." },
              color = Color(0xFF94A3B8),
              fontSize = 7.0.sp,
              fontFamily = FontFamily.Monospace,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Box(
              modifier =
                Modifier.weight(1f)
                  .minimumInteractiveComponentSize()
                  .height(48.dp)
                  .chamferedSciFiPodSurface(
                    isRightSide = false,
                    primaryBorder = if (hasUpdateAvailable) Color(0xFFFFD700) else Color(0xFF38BDF8),
                    secondaryBorder = Color(0xFF091528),
                  )
                  .clickable { onCheckOrApplyUpdate() }
                  .padding(horizontal = 4.dp)
                  .testTag("helm_check_update"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text =
                  when {
                    hasUpdateAvailable -> "APPLY UPDATE >"
                    updateStatusLabel.isNotBlank() -> updateStatusLabel.uppercase(Locale.US)
                    else -> "CHECK UPDATE"
                  },
                color = if (hasUpdateAvailable) Color(0xFFFFD700) else Color(0xFF38BDF8),
                fontSize = 7.4.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Box(
              modifier =
                Modifier.weight(1f)
                  .minimumInteractiveComponentSize()
                  .height(48.dp)
                  .chamferedSciFiPodSurface(
                    isRightSide = false,
                    primaryBorder = Color(0xFFFACC15),
                    secondaryBorder = Color(0xFF091528),
                  )
                  .clickable { onOpenSwitchAccountPrompt() }
                  .padding(horizontal = 4.dp)
                  .testTag("helm_switch_account"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "SWITCH ACCOUNT",
                color = Color(0xFFFACC15),
                fontSize = 7.4.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun HelmRightCodexBay(
  isTabletDeck: Boolean,
  headerHeight: Dp,
  wingStackGap: Dp,
  initialSubDeck: Int = 0,
  shipSpinDeg: Float,
  fluxOrbitRad: Float,
  onInspectCodexStage: (OverwatchCircularPrompt.InfoStageInspect) -> Unit,
  onInspectShipStage: (Int) -> Unit,
) {
  var selectedCategory by remember(initialSubDeck) { mutableIntStateOf(initialSubDeck.coerceIn(0, 4)) }
  val categories = remember {
    listOf(
      "BASICS" to Color(0xFF00F5D4),
      "MODES" to Color(0xFFFACC15),
      "BUFFS" to Color(0xFF34D399),
      "RIFTS" to Color(0xFF38BDF8),
      "SHIPS" to Color(0xFFC084FC),
    )
  }

  val basicsCards = remember {
    listOf(
      Triple(
        "TWIN-STICK FLIGHT & SHARED FLUX",
        "FLIGHT",
        "Left stick controls thrust and heading; right stick or touch controls primary fire, secondary ordnance, brake, and Special Crate buffs. Your Flux ring powers both shields and weapons.",
      ),
      Triple(
        "PER-SHIP RESONANCE & 100-PT PARITY",
        "PARITY",
        "All 10 hulls operate on an identical 100-Point stat and weapon budget. Your highest-Resonance ship is highlighted in Solar Gold as your Best Ship.",
      ),
      Triple(
        "5X RIFT STATION SAFE HARBOR",
        "STATION",
        "Sector Convergence and Flux Core feature a central 5x ship-scale Rift Station Safe Zone. Weapons lock inside, and lingering beyond 15 seconds triggers a lockout.",
      ),
      Triple(
        "SYNDICATES, SQUADRONS & SHARD STORE",
        "SOCIAL",
        "Found or join human-chartered Syndicates, form up to 5-pilot Squadrons, and unlock hull cosmetics and Rift Frame bays in the Shard Store.",
      ),
    )
  }

  val modeCards = remember {
    listOf(
      Triple(
        "SECTOR CONVERGENCE",
        "OPEN 60M",
        "Open free-for-all sector dogfighting anchored by the central 5x Rift Station Safe Harbor. Harvest Supply Crates, ride Playfield Rifts, and build Resonance.",
      ),
      Triple(
        "FLUX CORE",
        "OBJECTIVE",
        "Capture and hold the golden Flux Core Singularity Orb in the center of the sector to earn continuous Resonance while fighting off rival teams.",
      ),
      Triple(
        "RIFT SKIRMISH",
        "2V2-5V5",
        "Coordinated Squadron team battles matched against the closest-Resonance opponent squad within a 15-second matchmaking window.",
      ),
      Triple(
        "RIFT CONTROL",
        "2 RINGS",
        "Contest and hold the Alpha and Omega control rings to earn team dominance points with no central Safe Zone active.",
      ),
      Triple(
        "STASIS BREACH",
        "RESCUE",
        "Eliminated pilots are held in the rival Brig Reactor. Breach the enemy Brig shield to free teammates or sweep all rivals.",
      ),
      Triple(
        "CUSTOM RIFTS & TUTORIAL",
        "SANDBOX",
        "Launch public or room-code Custom Rifts with custom damage, Flux cost, and speed rules, or enter the Solo Tutorial anytime.",
      ),
    )
  }

  val allShips = remember(RiftSpriteBaker.customShip9Version) { RiftCodex.getAllTenShips() }

  Column(
    modifier = Modifier.fillMaxSize().testTag("helm_right_codex_bay"),
    verticalArrangement = Arrangement.spacedBy(wingStackGap),
  ) {
    HubDigitizedReturnBar(
      heightDp = headerHeight,
      isRightSide = true,
      isTabletDeck = isTabletDeck,
      sectionTitle = "TACTICAL CODEX",
      accent = Color(0xFFFFD700),
    )

    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(48.dp)
          .chamferedSciFiPodSurface(
            isRightSide = true,
            primaryBorder = Color(0xFF38BDF8),
            secondaryBorder = Color(0xFF091528),
          )
          .padding(horizontal = 3.dp),
      horizontalArrangement = Arrangement.spacedBy(2.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      categories.forEachIndexed { idx, (label, col) ->
        val sel = selectedCategory == idx
        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxHeight()
              .minimumInteractiveComponentSize()
              .clickable { selectedCategory = idx }
              .padding(vertical = 5.dp)
              .testTag("helm_codex_tab_$idx"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.fillMaxSize()
                .clip(RoundedCornerShape(5.dp))
                .background(if (sel) col.copy(alpha = 0.24f) else Color(0x6602050B))
                .border(1.dp, if (sel) col else Color(0x4438BDF8), RoundedCornerShape(5.dp)),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = label,
              color = if (sel) Color.White else Color(0xFF94A3B8),
              fontSize = if (isTabletDeck) 7.8.sp else 7.0.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
          }
        }
      }
    }

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      when (selectedCategory) {
        0 -> {
          basicsCards.forEachIndexed { idx, (heading, badge, body) ->
            RiftVerticalCard(
              modifier = Modifier.fillMaxWidth(),
              title = heading,
              subtitle = body.take(36),
              accent = Color(0xFF00F5D4),
              isRightColumn = true,
              badgeText = badge,
              badgeColor = Color(0xFF00F5D4),
              isTabletDeck = isTabletDeck,
              minHeightDp = 82.dp,
              testTag = "codex_basics_card_$idx",
              visualContent = { sizeDp ->
                ResonanceCrestIcon(sizeDp = (sizeDp * 0.65f).coerceAtLeast(22.dp))
              },
              onClick = {
                onInspectCodexStage(
                  OverwatchCircularPrompt.InfoStageInspect(
                    kicker = "CODEX • FLIGHT BASICS",
                    title = heading,
                    badge = badge,
                    summary = body,
                    accentHex = 0xFF00F5D4L,
                    showStationPreview = idx == 2,
                    shipId = if (idx == 1) 1 else 0,
                  )
                )
              },
            )
          }
        }
        1 -> {
          modeCards.forEachIndexed { idx, (heading, badge, body) ->
            RiftVerticalCard(
              modifier = Modifier.fillMaxWidth(),
              title = heading,
              subtitle = body.take(36),
              accent = Color(0xFFFACC15),
              isRightColumn = true,
              badgeText = badge,
              badgeColor = Color(0xFFFACC15),
              isTabletDeck = isTabletDeck,
              minHeightDp = 82.dp,
              testTag = "codex_modes_card_$idx",
              visualContent = { sizeDp ->
                ActiveRiftsEmblem(sizeDp = (sizeDp * 0.72f).coerceAtLeast(22.dp))
              },
              onClick = {
                onInspectCodexStage(
                  OverwatchCircularPrompt.InfoStageInspect(
                    kicker = "CODEX • GAME MODES",
                    title = heading,
                    badge = badge,
                    summary = body,
                    accentHex = 0xFFFACC15L,
                    showStationPreview = idx <= 1,
                  )
                )
              },
            )
          }
        }
        2 -> {
          RiftCodex.POWERUP_CODEX.forEach { buff ->
            RiftVerticalCard(
              modifier = Modifier.fillMaxWidth(),
              title = buff.title.uppercase(Locale.US),
              subtitle = buff.effectSummary.take(36),
              accent = Color(0xFF34D399),
              isRightColumn = true,
              badgeText = "${buff.dropRatePercent}% DROP",
              badgeColor = Color(0xFF34D399),
              isTabletDeck = isTabletDeck,
              minHeightDp = 86.dp,
              testTag = "codex_buff_card_${buff.buffTypeIndex}",
              visualContent = { sizeDp ->
                BuffIconPreview(
                  buffType = buff.buffTypeIndex,
                  sizeDp = sizeDp.value.roundToInt().coerceAtLeast(26),
                  accentColor = Color(0xFF34D399),
                )
              },
              onClick = {
                onInspectCodexStage(
                  OverwatchCircularPrompt.InfoStageInspect(
                    kicker = "SUPPLY CRATE BUFF",
                    title = buff.title.uppercase(Locale.US),
                    badge = "${buff.dropRatePercent}% DROP",
                    summary = buff.effectSummary,
                    tacticalNote = buff.tacticalNote,
                    accentHex = 0xFF34D399L,
                    buffTypeIndex = buff.buffTypeIndex,
                  )
                )
              },
            )
          }
        }
        3 -> {
          RiftCodex.RIFT_ANOMALIES_CODEX.forEach { rift ->
            val riftAccent =
              when (rift.id) {
                "plasma_rift" -> Color(0xFFEC4899)
                "repulsor_rift" -> Color(0xFF38BDF8)
                "gravity_rift" -> Color(0xFFA855F7)
                "unstable_rift" -> Color(0xFFFACC15)
                else -> Color(0xFF00F5D4)
              }
            val cleanRiftSummary = rift.effectSummary.replace("Spacetime-Tear Rift", "Unstable Rift", ignoreCase = true)
            val cleanRiftCounter = rift.tacticalNote.replace("Spacetime-Tear Rift", "Unstable Rift", ignoreCase = true)
            RiftVerticalCard(
              modifier = Modifier.fillMaxWidth(),
              title = rift.title.uppercase(Locale.US),
              subtitle = cleanRiftSummary.take(36),
              accent = riftAccent,
              isRightColumn = true,
              badgeText = rift.specBadge.uppercase(Locale.US),
              badgeColor = riftAccent,
              isTabletDeck = isTabletDeck,
              minHeightDp = 88.dp,
              testTag = "codex_rift_card_${rift.id}",
              visualContent = { sizeDp ->
                val phase = ((fluxOrbitRad / (2.0 * kotlin.math.PI)).toFloat() % 1f).let { if (it < 0f) it + 1f else it }
                Canvas(modifier = Modifier.size(sizeDp)) {
                  drawIntoCanvas { canvas ->
                    RiftSpriteBaker.drawAnimatedCodexRiftPreview(
                      canvas = canvas.nativeCanvas,
                      riftId = rift.id,
                      cx = size.width * 0.5f,
                      cy = size.height * 0.5f,
                      radiusPx = min(size.width, size.height) * 0.37f,
                      pulsePhase = phase,
                      spinAngle = shipSpinDeg * 0.01745f,
                    )
                  }
                }
              },
              onClick = {
                onInspectCodexStage(
                  OverwatchCircularPrompt.InfoStageInspect(
                    kicker = "PLAYFIELD RIFT",
                    title = rift.title.uppercase(Locale.US),
                    badge = rift.specBadge.uppercase(Locale.US),
                    summary = cleanRiftSummary,
                    tacticalNote = cleanRiftCounter,
                    accentHex =
                      when (rift.id) {
                        "plasma_rift" -> 0xFFEC4899L
                        "repulsor_rift" -> 0xFF38BDF8L
                        "gravity_rift" -> 0xFFA855F7L
                        "unstable_rift" -> 0xFFFACC15L
                        else -> 0xFF00F5D4L
                      },
                    riftAnomalyId = rift.id,
                  )
                )
              },
            )
          }
        }
        else -> {
          // All 10 Ships without numeric #1..#10 prefixes; tapping opens Overwatch ShipInspect with hideChargeButton = true
          allShips.forEach { ship ->
            val cleanName = cleanCodexShipName(ship.name)
            RiftVerticalCard(
              modifier = Modifier.fillMaxWidth(),
              title = cleanName,
              subtitle = "${ship.role} • ${ship.primaryShortLabel}",
              accent = Color(0xFF38BDF8),
              isRightColumn = true,
              badgeText = "100 PT",
              badgeColor = Color(0xFF00F5D4),
              isTabletDeck = isTabletDeck,
              minHeightDp = 90.dp,
              testTag = "codex_ship_card_${ship.id}",
              visualContent = { sizeDp ->
                Spinning25dShipWithOrbitingFlux(
                  shipId = ship.id,
                  shipSpinDeg = shipSpinDeg,
                  fluxOrbitRad = fluxOrbitRad,
                  isCharged = false,
                  isSelected = true,
                  sizeDp = (sizeDp * 0.90f).coerceAtLeast(32.dp),
                )
              },
              onClick = { onInspectShipStage(ship.id) },
            )
          }
        }
      }
    }
  }
}
