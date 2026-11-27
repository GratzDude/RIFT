package com.example.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.KeyEvent
import android.webkit.WebView
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.ArmadaMemberSlot
import com.example.data.CloudSyncHealth
import com.example.data.LeaderboardEntry
import com.example.data.LocalPilotPrefs
import com.example.data.SyndicateInfo
import com.example.data.SyndicateMemberEntry
import com.example.data.SyndicateWarRecord
import com.example.data.formatSurvivalDurationSec
import com.example.data.snapCustomFivePercentScale
import com.example.engine.CustomGameConfig
import com.example.engine.MusicSynthArchetype
import com.example.engine.RankedGameMode
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftCodex
import com.example.engine.RiftGameEngine
import com.example.engine.RiftProgressionCatalog
import com.example.engine.RiftSoundSynthesizer
import com.example.engine.RiftSpriteBaker
import com.example.engine.RiftVisualForgeState
import com.example.engine.SyndicateCrestRecipe
import java.io.File
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import org.json.JSONObject

private fun cleanCodexShipName(rawName: String): String =
  rawName.replace(Regex("^#?[0-9]+[-\\s]*"), "").trim().uppercase(Locale.US)

private fun cleanDisplayShipTitle(shipId: Int, rawName: String): String =
  if (shipId == 10) "RIFT FRAME" else cleanCodexShipName(rawName)

internal fun copyTelemetryTextToClipboard(
  context: Context,
  formatLabel: String,
  payload: String,
): String {
  return runCatching {
      val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
      cm?.setPrimaryClip(ClipData.newPlainText("Rift Telemetry $formatLabel", payload))
      "COPIED $formatLabel TELEMETRY TO CLIPBOARD"
    }
    .getOrDefault("READY // $formatLabel TELEMETRY GENERATED")
}

internal fun saveOrOpenTelemetryHtmlFile(
  context: Context,
  callsign: String,
  htmlPayload: String,
): String {
  val cleanCallsign =
    callsign
      .trim()
      .uppercase(Locale.US)
      .replace(Regex("[^A-Z0-9_\\-]"), "")
      .ifEmpty { "PILOT" }
  val fileName = "rift_telemetry_${cleanCallsign}.html"

  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    val resolver = context.contentResolver
    val values =
      ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "text/html")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
      }
    val insertedUri =
      runCatching { resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) }.getOrNull()
    if (insertedUri != null) {
      val wroteOk =
        runCatching {
            resolver.openOutputStream(insertedUri)?.use { out ->
              out.write(htmlPayload.toByteArray(Charsets.UTF_8))
              out.flush()
            }
            val publishValues = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(insertedUri, publishValues, null, null)
            true
          }
          .getOrDefault(false)
      if (wroteOk) {
        val openedInViewer =
          runCatching {
              val viewIntent =
                Intent(Intent.ACTION_VIEW).apply {
                  setDataAndType(insertedUri, "text/html")
                  clipData = ClipData.newRawUri("Rift Telemetry HTML", insertedUri)
                  addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
              context.startActivity(viewIntent)
              true
            }
            .getOrDefault(false)
        if (!openedInViewer) {
          runCatching {
            val sendFileIntent =
              Intent(Intent.ACTION_SEND).apply {
                type = "text/html"
                putExtra(Intent.EXTRA_STREAM, insertedUri)
                clipData = ClipData.newRawUri("Rift Telemetry HTML", insertedUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
              }
            val chooser =
              Intent.createChooser(sendFileIntent, "Open or Share $fileName")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
          }
        }
        return "SAVED $fileName TO DOWNLOADS"
      }
    }
  }

  return runCatching {
      val localFile = File(context.cacheDir, fileName)
      localFile.writeText(htmlPayload, Charsets.UTF_8)
      "SAVED $fileName // VIEWING LIVE BELOW"
    }
    .getOrDefault("VIEWING LIVE .HTML SHEET BELOW")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun RiftTelemetryWebSheetModal(
  callsign: String,
  liveResonance: Int,
  uidOrNull: String?,
  localPrefs: LocalPilotPrefs,
  onClose: () -> Unit,
) {
  BackHandler(onBack = onClose)
  val context = LocalContext.current
  val htmlPayload =
    remember(callsign, liveResonance, uidOrNull) {
      localPrefs.exportSurvivalTelemetryHtmlSpreadsheet(callsign, liveResonance, uidOrNull)
    }
  var statusBanner by remember { mutableStateOf<String?>(null) }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xE802050B))
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 12.dp, vertical = 8.dp)
        .testTag("rift_telemetry_websheet_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier =
        Modifier.fillMaxSize()
          .widthIn(max = 1080.dp)
          .border(
            1.2.dp,
            Brush.linearGradient(listOf(Color(0xFF00F5D4), Color(0xFF38BDF8), Color(0xFFFFD700))),
            RoundedCornerShape(14.dp),
          ),
      color = Color(0xFA040914),
      shape = RoundedCornerShape(14.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
              text = "PILOT, SURVIVAL & HUB TAP TELEMETRY (.HTML SHEET)",
              color = Color(0xFF00F5D4),
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = statusBanner ?: "Filter hulls, actors, sessions, and Hub tap regions live below or save .HTML to Downloads.",
              color = if (statusBanner != null) Color(0xFFFFD700) else Color(0xFFCBD5E1),
              fontSize = 8.2.sp,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            HubTelemetryMiniExportBtn(
              label = "SAVE / OPEN .HTML",
              accent = Color(0xFF00F5D4),
              testTag = "telemetry_websheet_save_open_html",
              onClick = {
                statusBanner = saveOrOpenTelemetryHtmlFile(context, callsign, htmlPayload)
              },
            )
            HubTelemetryMiniExportBtn(
              label = "COPY CSV",
              accent = Color(0xFF38BDF8),
              testTag = "telemetry_websheet_copy_csv",
              onClick = {
                val csv = localPrefs.exportSurvivalTelemetryCsv(callsign, liveResonance, uidOrNull)
                statusBanner = copyTelemetryTextToClipboard(context, "CSV", csv)
              },
            )
            HubTelemetryMiniExportBtn(
              label = "COPY JSON",
              accent = Color(0xFFFFD700),
              testTag = "telemetry_websheet_copy_json",
              onClick = {
                val json = localPrefs.exportSurvivalTelemetryJson(callsign, liveResonance, uidOrNull)
                statusBanner = copyTelemetryTextToClipboard(context, "JSON", json)
              },
            )
            HubTelemetryMiniExportBtn(
              label = "CLOSE",
              accent = Color(0xFFFB7185),
              testTag = "telemetry_websheet_close",
              onClick = onClose,
            )
          }
        }

        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFF02050B))
              .border(1.dp, Color(0x5538BDF8), RoundedCornerShape(10.dp)),
        ) {
          AndroidView(
            modifier = Modifier.fillMaxSize().testTag("telemetry_websheet_webview"),
            factory = { ctx ->
              WebView(ctx).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#02050B"))
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.domStorageEnabled = false
                loadDataWithBaseURL(
                  "https://rift.telemetry.local/",
                  htmlPayload,
                  "text/html",
                  "UTF-8",
                  null,
                )
              }
            },
            update = { webView ->
              if (webView.tag != htmlPayload.hashCode()) {
                webView.tag = htmlPayload.hashCode()
                webView.loadDataWithBaseURL(
                  "https://rift.telemetry.local/",
                  htmlPayload,
                  "text/html",
                  "UTF-8",
                  null,
                )
              }
            },
          )
        }
      }
    }
  }
}

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
  dossierRefreshNonce: Int = 0,
  onBackToHub: () -> Unit,
  onOpenRenamePrompt: () -> Unit,
  onOpenResetPrompt: () -> Unit,
  onOpenSwitchAccountPrompt: () -> Unit,
  onOpenSurvivalSpreadsheet: () -> Unit,
) {
  val uidOrNull = currentUserId.ifBlank { null }
  val effectivePeakResonance = max(liveResonance, peakResonance)
  val conduitTier =
    remember(liveResonance) {
      RiftProgressionCatalog.getConduitTierForResonance(liveResonance)
    }
  val conduitProgress =
    remember(liveResonance) {
      RiftProgressionCatalog.getConduitProgressFraction(liveResonance)
    }
  val nextTier =
    remember(liveResonance) {
      RiftProgressionCatalog.getNextConduitTierThreshold(liveResonance)
    }
  val startingResonance =
    remember(liveResonance) {
      RiftProgressionCatalog.computeStartingBountyForResonance(liveResonance)
    }
  val tierAccent = Color(conduitTier.accentColorHex)
  val shipResonanceRows =
    remember(currentUserId, liveResonance, dossierRefreshNonce) {
      RiftCodex.getAllTenShips().map { ship ->
        Triple(
          ship.id,
          cleanDisplayShipTitle(ship.id, ship.name),
          localPrefs.getShipResonance(ship.id, uidOrNull),
        )
      }
    }
  val maxShipResonance =
    remember(shipResonanceRows) {
      (shipResonanceRows.maxOfOrNull { it.third } ?: 0).coerceAtLeast(100)
    }
  val callsignChangeCount =
    remember(currentUserId, liveResonance, dossierRefreshNonce, callsign) {
      localPrefs.getCallsignChangeCount(uidOrNull)
    }
  val isFirstRenameFree = callsignChangeCount == 0

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
      // Pilot Identity + Conduit Rank Tier & Live Resonance Header Pod
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
                .background(tierAccent.copy(alpha = 0.18f))
                .border(1.dp, tierAccent, RoundedCornerShape(5.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .testTag("dossier_conduit_tier_badge"),
          ) {
            Text(
              text = conduitTier.title,
              color = tierAccent,
              fontSize = 7.4.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
            )
          }
        }

        // Conduit Rank Tier Progress Bar + Next-Tier Threshold Readout
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF02050B))
                .border(0.6.dp, tierAccent.copy(alpha = 0.45f), RoundedCornerShape(3.dp)),
          ) {
            Box(
              modifier =
                Modifier.fillMaxWidth(conduitProgress.coerceIn(0.05f, 1f))
                  .fillMaxHeight()
                  .background(Brush.horizontalGradient(listOf(tierAccent, Color(0xFFFFD700)))),
            )
          }
          Text(
            text =
              if (liveResonance >= 1500) {
                "APEX ARCHON • MAX TIER"
              } else {
                "${String.format(Locale.US, "%,d", liveResonance)} / ${String.format(Locale.US, "%,d", nextTier.first)} TO ${nextTier.second}"
              },
            color = Color(0xFFCBD5E1),
            fontSize = 7.2.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          ResonanceCrestIcon(sizeDp = 12.dp)
          Text(
            text =
              "LIVE ${String.format(Locale.US, "%,d", liveResonance)} • PEAK ${String.format(Locale.US, "%,d", effectivePeakResonance)} • START $startingResonance",
            color = Color(0xFFFFD700),
            fontSize = 7.8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      // 10-Ship Resonance Breakdown across all 10 numberless hulls (including Rift Frame)
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .chamferedSciFiPodSurface(
              isRightSide = false,
              primaryBorder = Color(0xFF38BDF8),
              secondaryBorder = Color(0xFF091528),
            )
            .padding(horizontal = 9.dp, vertical = 7.dp)
            .testTag("dossier_ship_resonance_breakdown"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          ResonanceCrestIcon(sizeDp = 10.dp)
          Text(
            text = "10-SHIP RESONANCE BREAKDOWN",
            color = Color(0xFF38BDF8),
            fontSize = 7.6.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        shipResonanceRows.forEach { (shipId, hullTitle, shipRes) ->
          val barCol =
            when {
              shipId == 10 -> Color(0xFFFFD700)
              shipRes > 0 -> Color(0xFF00F5D4)
              else -> Color(0xFF475569)
            }
          val frac =
            if (shipRes <= 0) 0.03f
            else (shipRes.toFloat() / maxShipResonance.toFloat()).coerceIn(0.05f, 1f)
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = hullTitle,
                color = if (shipId == 10) Color(0xFFFFD700) else Color(0xFFCBD5E1),
                fontSize = 7.0.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = String.format(Locale.US, "%,d", shipRes),
                color = if (shipRes > 0 || shipId == 10) barCol else Color(0xFF94A3B8),
                fontSize = 7.2.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .height(3.6.dp)
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
  liveResonance: Int,
  dossierRefreshNonce: Int = 0,
  cloudLeaderboardEntries: List<LeaderboardEntry>,
  onInspectShip: (Int) -> Unit,
  onOpenSurvivalSpreadsheet: () -> Unit = {},
) {
  val context = LocalContext.current
  val uidOrNull = currentUserId.ifBlank { null }
  val career =
    remember(currentUserId, liveResonance, dossierRefreshNonce) {
      localPrefs.getPilotCareerStatsSummary(uidOrNull)
    }
  val survival =
    remember(currentUserId, liveResonance, dossierRefreshNonce) {
      localPrefs.getPilotSurvivalTelemetrySummary(uidOrNull)
    }
  val hubTapCount =
    remember(currentUserId, liveResonance, dossierRefreshNonce) {
      localPrefs.getHubTapTelemetryHistory().size
    }
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
          val cleanShip = cleanDisplayShipTitle(ship.id, ship.name)
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
            onClick = onOpenSurvivalSpreadsheet,
          )
          HubTelemetryMiniExportBtn(
            modifier = Modifier.weight(1f),
            label = "CSV",
            accent = Color(0xFF38BDF8),
            testTag = "dossier_export_csv",
            onClick = {
              val payload =
                localPrefs.exportSurvivalTelemetryCsv(
                  localPrefs.getCallsign(uidOrNull),
                  localPrefs.getLiveResonance(uidOrNull),
                  uidOrNull,
                )
              exportBanner = copyTelemetryTextToClipboard(context, "CSV", payload)
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
                  localPrefs.getCallsign(uidOrNull),
                  localPrefs.getLiveResonance(uidOrNull),
                  uidOrNull,
                )
              exportBanner = copyTelemetryTextToClipboard(context, "JSON", payload)
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
        Modifier.height(36.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(accent.copy(alpha = 0.16f))
          .border(1.dp, accent, RoundedCornerShape(6.dp))
          .padding(horizontal = 8.dp),
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
            color = Color(0xFFCBD5E1),
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

      val cleanCurrentTag = currentTag.trim().uppercase(Locale.US)
      val isCurrentTagAffiliated =
        cleanCurrentTag.isNotEmpty() && cleanCurrentTag !in LocalPilotPrefs.NON_PLAYER_SYNDICATE_TAGS

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
            text = "Use the Charter Syndicate bay on the right to found a real-player Syndicate.",
            color = Color(0xFFCBD5E1),
            fontSize = 7.6.sp,
            fontFamily = FontFamily.SansSerif,
          )
        }
      } else {
        realSyndicates.forEach { syn ->
          val isCurrent = isCurrentTagAffiliated && syn.tag.equals(cleanCurrentTag, ignoreCase = true)
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
  val cleanSynTag = syndicateTag.trim().uppercase(Locale.US)
  val isAffiliatedSyndicate =
    cleanSynTag.isNotEmpty() && cleanSynTag !in LocalPilotPrefs.NON_PLAYER_SYNDICATE_TAGS

  var inviteOnly by remember(cleanSynTag, isAffiliatedSyndicate, rosterRefreshTick) {
    mutableStateOf(
      if (isAffiliatedSyndicate) localPrefs.isSyndicateInviteOnly(cleanSynTag) else false
    )
  }

  val activeCrestRecipe =
    remember(cleanSynTag, isAffiliatedSyndicate, RiftVisualForgeState.visualRevision) {
      val savedJson = if (isAffiliatedSyndicate) localPrefs.getSyndicateCrestJson(cleanSynTag) else null
      if (!savedJson.isNullOrBlank()) {
        runCatching { SyndicateCrestRecipe.fromJson(JSONObject(savedJson)) }
          .getOrDefault(RiftVisualForgeState.activeSyndicateCrestRecipe)
      } else {
        RiftVisualForgeState.activeSyndicateCrestRecipe
      }
    }

  val rosterMembers =
    remember(cleanSynTag, isAffiliatedSyndicate, callsign, localShipId, liveResonance, rosterRefreshTick) {
      if (!isAffiliatedSyndicate) {
        emptyList()
      } else {
        val loaded =
          localPrefs.getMySyndicateRoster(currentUserId, callsign, localShipId, liveResonance, kills)
        if (loaded.any { it.isLocalPlayer }) {
          loaded
        } else {
          val fallbackSelf =
            SyndicateMemberEntry(
              uid = currentUserId,
              callsign = callsign,
              tag = cleanSynTag,
              shipId = localShipId.coerceIn(1, 10),
              shipName = cleanDisplayShipTitle(localShipId, RiftCodex.getShipById(localShipId).name),
              role = "Founder",
              bountyContribution = liveResonance.coerceAtLeast(0L),
              kills = kills,
              isOnline = true,
              isLocalPlayer = true,
            )
          listOf(fallbackSelf) + loaded
        }
      }
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
      sectionTitle = if (isAffiliatedSyndicate) "[$cleanSynTag] CITADEL" else "CHARTER SYNDICATE",
      accent = Color(0xFF00F5D4),
      trailingLabel = if (isAffiliatedSyndicate) "LEAVE" else null,
      trailingTag = if (isAffiliatedSyndicate) "syndicate_btn_leave_header" else null,
      onTrailingClick = if (isAffiliatedSyndicate) onLeaveCurrentSyndicate else null,
    )

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
      if (isAffiliatedSyndicate) {
        val onlineDisplayCount =
          kotlin.math.max(syndicateOnlineCount, rosterMembers.count { it.isOnline }).coerceAtLeast(1)
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
                tagText = cleanSynTag,
              )
            }
          }
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
              text = "[$cleanSynTag] $syndicateName",
              color = Color.White,
              fontSize = if (isTabletDeck) 9.8.sp else 8.8.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "$onlineDisplayCount ONLINE • ${syndicateWarRecord.warWins}W-${syndicateWarRecord.warLosses}L",
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
                    .clickable { onOpenCrestBuilder(cleanSynTag) }
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
                        localPrefs.setSyndicateInviteOnly(cleanSynTag, next)
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
          val memberRes =
            if (member.isLocalPlayer) liveResonance.coerceAtLeast(0L)
            else member.bountyContribution.coerceAtLeast(0L)
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
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Box(
                  modifier =
                    Modifier.clip(RoundedCornerShape(4.dp))
                      .background(Color(0x3338BDF8))
                      .border(0.8.dp, Color(0xFF38BDF8), RoundedCornerShape(4.dp))
                      .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                  Text(
                    text = memberRoleUp,
                    color = Color(0xFF7DD3FC),
                    fontSize = 6.6.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                  )
                }
                ResonanceCrestIcon(sizeDp = 10.dp)
                Text(
                  text = String.format(Locale.US, "%,d", memberRes),
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
                        localPrefs.toggleSyndicateOfficer(cleanSynTag, member.callsign)
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
                        localPrefs.kickSyndicateMember(cleanSynTag, member.callsign)
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
            text = "CHARTER NEW SYNDICATE",
            color = Color(0xFF00F5D4),
            fontSize = 8.6.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
          Text(
            text = "Unaffiliated Sworn Pilots can register a 2 to 3 character Syndicate tag and unlock Syndicate Wars.",
            color = Color(0xFFCBD5E1),
            fontSize = 7.4.sp,
            lineHeight = 10.4.sp,
            fontFamily = FontFamily.SansSerif,
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
                text = "3-CHAR TAG (e.g. APX)",
                color = Color(0xFFCBD5E1),
                fontSize = 7.6.sp,
                fontFamily = FontFamily.Monospace,
              )
            }
            BasicTextField(
              value = tagDraft,
              onValueChange = { tagDraft = it.uppercase(Locale.US).replace(Regex("[^A-Z0-9]"), "").take(3) },
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
                color = Color(0xFFCBD5E1),
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
                color = Color(0xFFCBD5E1),
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
                  color = Color(0xFFE2E8F0),
                  fontSize = 7.8.sp,
                  fontFamily = FontFamily.Monospace,
                ),
              modifier = Modifier.fillMaxWidth().testTag("syndicate_charter_motto_input"),
            )
          }

          val canCharter = tagDraft.length in 2..3 && nameDraft.trim().length >= 3
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
                  .border(1.dp, if (canCharter) Color(0xFF00F5D4) else Color(0xFF475569), RoundedCornerShape(7.dp))
                  .padding(vertical = 8.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "CHARTER SYNDICATE >",
                color = if (canCharter) Color.White else Color(0xFFCBD5E1),
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
            text = if (sortMode == SquadronPilotSort.RESONANCE) "SORT: RANK" else "SORT: ACT",
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
            color = Color(0xFFCBD5E1),
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
                if (friendDraft.length >= 2) Color(0xFFFACC15) else Color(0xFF475569),
                RoundedCornerShape(6.dp),
              )
              .padding(horizontal = 8.dp, vertical = 7.dp),
        ) {
          Text(
            text = "+ FRIEND",
            color = if (friendDraft.length >= 2) Color(0xFFFACC15) else Color(0xFFCBD5E1),
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
            color = Color(0xFFCBD5E1),
            fontSize = 7.6.sp,
            lineHeight = 11.0.sp,
            fontFamily = FontFamily.Monospace,
          )
        }
      } else {
        filteredList.forEach { pilot ->
          val isSel = selectedPilotUid.isNotBlank() && selectedPilotUid == pilot.uid
          val cleanPilotTag = pilot.syndicateTag.trim().uppercase(Locale.US)
          val tagStr =
            if (cleanPilotTag.isNotEmpty() && cleanPilotTag !in LocalPilotPrefs.NON_PLAYER_SYNDICATE_TAGS) {
              "[$cleanPilotTag] "
            } else ""
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
                  text = "${String.format(Locale.US, "%,d", pilot.resonance)} • ${cleanDisplayShipTitle(shipId, RiftCodex.getShipById(shipId).name)}",
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
  val cleanLocalTag = localSyndicateTag.trim().uppercase(Locale.US)
  val hasAffiliatedLocalTag =
    cleanLocalTag.isNotEmpty() && cleanLocalTag !in LocalPilotPrefs.NON_PLAYER_SYNDICATE_TAGS
  val targetName =
    if (inspectedPilot != null) {
      val cleanP = inspectedPilot.syndicateTag.trim().uppercase(Locale.US)
      if (cleanP.isNotEmpty() && cleanP !in LocalPilotPrefs.NON_PLAYER_SYNDICATE_TAGS) "[$cleanP] ${inspectedPilot.callsign}"
      else inspectedPilot.callsign
    } else {
      if (hasAffiliatedLocalTag) "[$cleanLocalTag] $localCallsign" else localCallsign
    }
  val targetShipSpec = remember(targetShipId, RiftSpriteBaker.customShip9Version) { RiftCodex.getShipById(targetShipId) }
  val cleanShipTitle = remember(targetShipId, targetShipSpec.name) { cleanDisplayShipTitle(targetShipId, targetShipSpec.name) }
  val canLeaveSquadron = armadaName.isNotBlank() || armadaSlots.size > 1

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
      trailingLabel = if (canLeaveSquadron) "LEAVE" else "ROSTER >",
      trailingTag = if (canLeaveSquadron) "squadron_btn_leave_header" else "squadron_bay_open_full_roster",
      onTrailingClick = if (canLeaveSquadron) onLeaveSquadron else onOpenFullSquadronModal,
    )

    Column(
      modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(wingStackGap),
    ) {
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
              text = "CHARGED // $cleanShipTitle",
              color = Color(0xFF00F5D4),
              fontSize = 7.2.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }

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
                color = if (voiceEnabled) Color(0xFF00F5D4) else Color(0xFFCBD5E1),
                fontSize = 6.8.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
          if (canLeaveSquadron) {
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

      for (slotIdx in 0 until 5) {
        val member = armadaSlots.getOrNull(slotIdx)
        val slotNum = slotIdx + 1
        if (member != null) {
          val cleanMTag = member.syndicateTag.trim().uppercase(Locale.US)
          val tagStr =
            if (cleanMTag.isNotEmpty() && cleanMTag !in LocalPilotPrefs.NON_PLAYER_SYNDICATE_TAGS) "[$cleanMTag] "
            else ""
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
            badgeColor = Color(0xFFCBD5E1),
            isTabletDeck = isTabletDeck,
            minHeightDp = if (isTabletDeck) 80.dp else 70.dp,
            testTag = "squadron_slot_card_$slotNum",
            visualContent = { sizeDp ->
              Box(
                modifier =
                  Modifier.size((sizeDp * 0.52f).coerceAtLeast(22.dp))
                    .clip(CircleShape)
                    .border(1.dp, Color(0xFF64748B), CircleShape),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "+",
                  color = Color(0xFFCBD5E1),
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
            RankedGameMode.SECTOR_CONVERGENCE -> "Open Safe Harbor arena"
            RankedGameMode.FLUX_CORE -> "Hold central Flux Core orb"
            RankedGameMode.RIFT_SKIRMISH -> "Matched Squadron team combat"
            RankedGameMode.RIFT_CONTROL -> "Contest Alpha and Omega rings"
            RankedGameMode.STASIS_BREACH -> "Breach rival Brig reactor"
            RankedGameMode.SYNDICATE_WARS -> "Chartered Syndicate warfare"
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
            RankedModeDistinctEmblem(
              modeIndex = mode.ordinal,
              sizeDp = (sizeDp * 0.78f).coerceAtLeast(24.dp),
            )
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
  customSpeedMult: Float,
  onSelectCustomSpeedMult: (Float) -> Unit,
  customDamageMult: Float,
  onSelectCustomDamageMult: (Float) -> Unit,
  customRechargeMult: Float,
  onSelectCustomRechargeMult: (Float) -> Unit,
  customCrateSpawnMult: Float,
  onSelectCustomCrateSpawnMult: (Float) -> Unit,
  customAllowArmadasInFfa: Boolean,
  onToggleAllowArmadasInFfa: (Boolean) -> Unit,
  onJoinCustomInstance: (String) -> Unit,
  onQuickCreateCustomGame: () -> Unit,
  onOpenSliderWorkbench: (OverwatchCircularPrompt.SliderWorkbench) -> Unit = {},
) {
  var rightTab by remember { mutableIntStateOf(0) } // 0 = PUBLIC BROWSER, 1 = CREATE RIFT
  var hostBroadcastPhysics by remember { mutableStateOf(true) }
  val publicVisibleRooms =
    remember(publicCustomGames, engineCustomGames, unlockedPrivateCodes) {
      val merged = LinkedHashMap<String, CustomGameConfig>()
      engineCustomGames.forEach { merged[it.instanceId] = it }
      publicCustomGames.forEach { merged.putIfAbsent(it.instanceId, it) }
      merged.values.filter { !it.privateRoom || it.roomCode in unlockedPrivateCodes }
    }
  fun formatBotSkillName(level: Int): String =
    when (level.coerceIn(0, 3)) {
      0 -> "CADET"
      1 -> "REGULAR"
      2 -> "VETERAN"
      else -> "ACE"
    }
  val botSkillLabel = formatBotSkillName(customBotSkillLevel)

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
      // Join Private Room by 6-Character Code (High-Contrast #0B1528 Field + #94A3B8 Placeholder)
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
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF0B1528))
              .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
              .padding(horizontal = 8.dp, vertical = 8.dp),
          contentAlignment = Alignment.CenterStart,
        ) {
          if (joinRoomCodeDraft.isEmpty()) {
            Text(
              text = "6-CHAR ROOM CODE",
              color = Color(0xFF94A3B8),
              fontSize = 7.8.sp,
              fontWeight = FontWeight.Bold,
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
        // Explicit VISIBILITY toggle and 2x3 6-Mode BASE MODE holographic chip grid
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "VISIBILITY",
              color = Color(0xFF94A3B8),
              fontSize = 6.8.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              modifier = Modifier.padding(start = 2.dp),
            )
            Box(
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .heightIn(min = 36.dp)
                  .chamferedSciFiPodSurface(
                    isRightSide = true,
                    primaryBorder = if (customIsPrivateRoom) Color(0xFFC084FC) else Color(0xFF00F5D4),
                    secondaryBorder = Color(0xFF091528),
                  )
                  .clickable { onTogglePrivateRoom(!customIsPrivateRoom) }
                  .padding(horizontal = 10.dp, vertical = 5.dp)
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
                softWrap = false,
              )
            }
          }

          val customModeChips = remember {
            listOf(
              RankedGameMode.SECTOR_CONVERGENCE to "FFA",
              RankedGameMode.RIFT_SKIRMISH to "SKIRMISH",
              RankedGameMode.FLUX_CORE to "FLUX CORE",
              RankedGameMode.STASIS_BREACH to "BREACH",
              RankedGameMode.RIFT_CONTROL to "CONTROL",
              RankedGameMode.SYNDICATE_WARS to "SYN WARS",
            )
          }

          Text(
            text = "BASE MODE",
            color = Color(0xFF94A3B8),
            fontSize = 6.8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            modifier =
              Modifier.padding(start = 2.dp)
                .clickable {
                  val nextIdx = (customModeChips.indexOfFirst { it.first == customBaseMode } + 1) % customModeChips.size
                  onSelectCustomBaseMode(customModeChips[nextIdx].first)
                }
                .testTag("hub_custom_cycle_base_mode"),
          )

          for (rowIdx in 0..1) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              for (colIdx in 0..2) {
                val (modeItem, shortLabel) = customModeChips[rowIdx * 3 + colIdx]
                val isSelectedMode = customBaseMode == modeItem
                Box(
                  modifier =
                    Modifier.weight(1f)
                      .minimumInteractiveComponentSize()
                      .heightIn(min = 38.dp)
                      .chamferedSciFiPodSurface(
                        isRightSide = true,
                        primaryBorder = if (isSelectedMode) Color(0xFF00F5D4) else Color(0xFF38BDF8),
                        secondaryBorder = if (isSelectedMode) Color(0xFFFFD700) else Color(0xFF091528),
                      )
                      .background(
                        if (isSelectedMode) Color(0x3800F5D4) else Color.Transparent
                      )
                      .clickable { onSelectCustomBaseMode(modeItem) }
                      .padding(horizontal = 4.dp, vertical = 5.dp)
                      .testTag("hub_custom_base_mode_${modeItem.name}"),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = shortLabel,
                    color = if (isSelectedMode) Color(0xFFFFD700) else Color(0xFFE2E8F0),
                    fontSize = 7.4.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
            }
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
            onTapRow = {
              onOpenSliderWorkbench(
                OverwatchCircularPrompt.SliderWorkbench(
                  key = "custom_dmg_taken",
                  title = "DAMAGE TAKEN",
                  subtitle = "Scales incoming weapon & hazard damage in this Custom Rift (0% = Practice Shield).",
                  currentValue = customDamageTakenScalePct.toFloat(),
                  minValue = 0f,
                  maxValue = 100f,
                  stepSize = 5f,
                  defaultValue = 100f,
                  unitSuffix = "%",
                  accentHex = 0xFF00F5D4L,
                  presets = listOf("0%" to 0f, "50%" to 50f, "75%" to 75f, "100%" to 100f),
                  valueFormatter = { "${snapCustomFivePercentScale(it.roundToInt())}%" },
                  onValueChange = { onSelectCustomDamageTakenScalePct(snapCustomFivePercentScale(it.roundToInt())) },
                )
              )
            },
          )
          HubCustomFivePercentStepperRow(
            label = "WEAPON FLUX COST",
            valuePct = customWeaponFluxCostScalePct,
            minusTag = "custom_flux_minus",
            valueTag = "custom_flux_val",
            plusTag = "custom_flux_plus",
            onMinus = { onSelectCustomWeaponFluxCostScalePct((customWeaponFluxCostScalePct - 5).coerceIn(0, 100)) },
            onPlus = { onSelectCustomWeaponFluxCostScalePct((customWeaponFluxCostScalePct + 5).coerceIn(0, 100)) },
            onTapRow = {
              onOpenSliderWorkbench(
                OverwatchCircularPrompt.SliderWorkbench(
                  key = "custom_flux_cost",
                  title = "WEAPON FLUX COST",
                  subtitle = "Scales Flux energy drained per weapon shot (0% = Unlimited Firing).",
                  currentValue = customWeaponFluxCostScalePct.toFloat(),
                  minValue = 0f,
                  maxValue = 100f,
                  stepSize = 5f,
                  defaultValue = 100f,
                  unitSuffix = "%",
                  accentHex = 0xFF38BDF8L,
                  presets = listOf("0%" to 0f, "50%" to 50f, "75%" to 75f, "100%" to 100f),
                  valueFormatter = { "${snapCustomFivePercentScale(it.roundToInt())}%" },
                  onValueChange = { onSelectCustomWeaponFluxCostScalePct(snapCustomFivePercentScale(it.roundToInt())) },
                )
              )
            },
          )
        }

        HelmStepperControlRow(
          label = "BOT COUNT",
          valueText = "$customBotCount",
          onMinus = { onSelectCustomBotCount((customBotCount - 1).coerceIn(0, 19)) },
          onPlus = { onSelectCustomBotCount((customBotCount + 1).coerceIn(0, 19)) },
          onTapRow = {
            onOpenSliderWorkbench(
              OverwatchCircularPrompt.SliderWorkbench(
                key = "custom_bot_count",
                title = "BOT COUNT",
                subtitle = "Number of AI wingmen & rival bots active inside this Custom Rift.",
                currentValue = customBotCount.toFloat(),
                minValue = 0f,
                maxValue = 19f,
                stepSize = 1f,
                defaultValue = 9f,
                accentHex = 0xFF00F5D4L,
                presets = listOf("0" to 0f, "5" to 5f, "9" to 9f, "19" to 19f),
                valueFormatter = { "${it.roundToInt()} BOTS" },
                onValueChange = { onSelectCustomBotCount(it.roundToInt().coerceIn(0, 19)) },
              )
            )
          },
        )
        HelmStepperControlRow(
          label = "BOT SKILL",
          valueText = botSkillLabel,
          onMinus = { onSelectCustomBotSkillLevel((customBotSkillLevel - 1).coerceIn(0, 3)) },
          onPlus = { onSelectCustomBotSkillLevel((customBotSkillLevel + 1).coerceIn(0, 3)) },
          onTapRow = {
            onOpenSliderWorkbench(
              OverwatchCircularPrompt.SliderWorkbench(
                key = "custom_bot_skill",
                title = "BOT SKILL",
                subtitle = "AI pilot aim prediction, evasion timing, and Supply Crate priority.",
                currentValue = customBotSkillLevel.coerceIn(0, 3).toFloat(),
                minValue = 0f,
                maxValue = 3f,
                stepSize = 1f,
                defaultValue = 1f,
                accentHex = 0xFFFFD700L,
                presets = listOf("CADET" to 0f, "REGULAR" to 1f, "VETERAN" to 2f, "ACE" to 3f),
                valueFormatter = { formatBotSkillName(it.roundToInt()) },
                onValueChange = { onSelectCustomBotSkillLevel(it.roundToInt().coerceIn(0, 3)) },
              )
            )
          },
        )
        HelmStepperControlRow(
          label = "TIME LIMIT",
          valueText = "${customResetMinutes}M",
          onMinus = { onSelectCustomResetMinutes((customResetMinutes - 5).coerceIn(5, 60)) },
          onPlus = { onSelectCustomResetMinutes((customResetMinutes + 5).coerceIn(5, 60)) },
          onTapRow = {
            onOpenSliderWorkbench(
              OverwatchCircularPrompt.SliderWorkbench(
                key = "custom_time_limit",
                title = "TIME LIMIT",
                subtitle = "Match cycle duration in minutes before sector reset.",
                currentValue = customResetMinutes.toFloat(),
                minValue = 5f,
                maxValue = 60f,
                stepSize = 5f,
                defaultValue = 15f,
                unitSuffix = "M",
                accentHex = 0xFF38BDF8L,
                presets = listOf("10M" to 10f, "15M" to 15f, "30M" to 30f, "60M" to 60f),
                valueFormatter = { "${((it / 5f).roundToInt() * 5).coerceIn(5, 60)} MIN" },
                onValueChange = { onSelectCustomResetMinutes(((it / 5f).roundToInt() * 5).coerceIn(5, 60)) },
              )
            )
          },
        )
        HelmStepperControlRow(
          label = "SCORE CAP",
          valueText = "$customScoreCap",
          onMinus = { onSelectCustomScoreCap((customScoreCap - 10).coerceIn(10, 200)) },
          onPlus = { onSelectCustomScoreCap((customScoreCap + 10).coerceIn(10, 200)) },
          onTapRow = {
            onOpenSliderWorkbench(
              OverwatchCircularPrompt.SliderWorkbench(
                key = "custom_score_cap",
                title = "SCORE CAP",
                subtitle = "Target victory score before the Custom Rift concludes.",
                currentValue = customScoreCap.toFloat(),
                minValue = 10f,
                maxValue = 200f,
                stepSize = 10f,
                defaultValue = 50f,
                accentHex = 0xFFFACC15L,
                presets = listOf("25" to 20f, "50" to 50f, "100" to 100f, "200" to 200f),
                valueFormatter = { "${((it / 10f).roundToInt() * 10).coerceIn(10, 200)} PTS" },
                onValueChange = { onSelectCustomScoreCap(((it / 10f).roundToInt() * 10).coerceIn(10, 200)) },
              )
            )
          },
        )
        HelmStepperControlRow(
          label = "BULLET SCALE",
          valueText = String.format(Locale.US, "%.2fx", customBulletSizeMult),
          onMinus = { onSelectCustomBulletSizeMult((customBulletSizeMult - 0.15f).coerceIn(0.70f, 1.75f)) },
          onPlus = { onSelectCustomBulletSizeMult((customBulletSizeMult + 0.15f).coerceIn(0.70f, 1.75f)) },
          onTapRow = {
            onOpenSliderWorkbench(
              OverwatchCircularPrompt.SliderWorkbench(
                key = "custom_bullet_scale",
                title = "BULLET SCALE",
                subtitle = "Visual and collision radius multiplier for primary bolts.",
                currentValue = customBulletSizeMult,
                minValue = 0.70f,
                maxValue = 1.75f,
                stepSize = 0.05f,
                defaultValue = 1.00f,
                unitSuffix = "x",
                accentHex = 0xFF00F5D4L,
                presets = listOf("0.85x" to 0.85f, "1.0x" to 1.00f, "1.25x" to 1.25f, "1.5x" to 1.50f),
                valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                onValueChange = { onSelectCustomBulletSizeMult(it.coerceIn(0.70f, 1.75f)) },
              )
            )
          },
        )
        HelmStepperControlRow(
          label = "SHIP SPEED",
          valueText = String.format(Locale.US, "%.2fx", customSpeedMult),
          onMinus = {
            val next = (((customSpeedMult - 0.25f) * 100f).roundToInt() / 100f).coerceIn(0.75f, 1.75f)
            onSelectCustomSpeedMult(next)
          },
          onPlus = {
            val next = (((customSpeedMult + 0.25f) * 100f).roundToInt() / 100f).coerceIn(0.75f, 1.75f)
            onSelectCustomSpeedMult(next)
          },
          onTapRow = {
            onOpenSliderWorkbench(
              OverwatchCircularPrompt.SliderWorkbench(
                key = "custom_ship_speed",
                title = "SHIP SPEED",
                subtitle = "Global top speed and thrust multiplier for all hulls.",
                currentValue = customSpeedMult,
                minValue = 0.75f,
                maxValue = 1.75f,
                stepSize = 0.05f,
                defaultValue = 1.00f,
                unitSuffix = "x",
                accentHex = 0xFF38BDF8L,
                presets = listOf("0.75x" to 0.75f, "1.0x" to 1.00f, "1.25x" to 1.25f, "1.5x" to 1.50f),
                valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                onValueChange = { onSelectCustomSpeedMult(it.coerceIn(0.75f, 1.75f)) },
              )
            )
          },
        )
        HelmStepperControlRow(
          label = "WEAPON DAMAGE",
          valueText = String.format(Locale.US, "%.2fx", customDamageMult),
          onMinus = {
            val next = (((customDamageMult - 0.25f) * 100f).roundToInt() / 100f).coerceIn(0.50f, 2.00f)
            onSelectCustomDamageMult(next)
          },
          onPlus = {
            val next = (((customDamageMult + 0.25f) * 100f).roundToInt() / 100f).coerceIn(0.50f, 2.00f)
            onSelectCustomDamageMult(next)
          },
          onTapRow = {
            onOpenSliderWorkbench(
              OverwatchCircularPrompt.SliderWorkbench(
                key = "custom_weapon_dmg",
                title = "WEAPON DAMAGE",
                subtitle = "Outgoing weapon damage multiplier across all ship classes.",
                currentValue = customDamageMult,
                minValue = 0.50f,
                maxValue = 2.00f,
                stepSize = 0.05f,
                defaultValue = 1.00f,
                unitSuffix = "x",
                accentHex = 0xFFFB7185L,
                presets = listOf("0.75x" to 0.75f, "1.0x" to 1.00f, "1.5x" to 1.50f, "2.0x" to 2.00f),
                valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                onValueChange = { onSelectCustomDamageMult(it.coerceIn(0.50f, 2.00f)) },
              )
            )
          },
        )
        HelmStepperControlRow(
          label = "FLUX REGEN",
          valueText = String.format(Locale.US, "%.2fx", customRechargeMult),
          onMinus = {
            val next = (((customRechargeMult - 0.25f) * 100f).roundToInt() / 100f).coerceIn(0.75f, 2.00f)
            onSelectCustomRechargeMult(next)
          },
          onPlus = {
            val next = (((customRechargeMult + 0.25f) * 100f).roundToInt() / 100f).coerceIn(0.75f, 2.00f)
            onSelectCustomRechargeMult(next)
          },
          onTapRow = {
            onOpenSliderWorkbench(
              OverwatchCircularPrompt.SliderWorkbench(
                key = "custom_flux_regen",
                title = "FLUX REGEN",
                subtitle = "Passive Flux ring recharge rate multiplier.",
                currentValue = customRechargeMult,
                minValue = 0.75f,
                maxValue = 2.00f,
                stepSize = 0.05f,
                defaultValue = 1.00f,
                unitSuffix = "x",
                accentHex = 0xFF00F5D4L,
                presets = listOf("0.75x" to 0.75f, "1.0x" to 1.00f, "1.5x" to 1.50f, "2.0x" to 2.00f),
                valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                onValueChange = { onSelectCustomRechargeMult(it.coerceIn(0.75f, 2.00f)) },
              )
            )
          },
        )
        HelmStepperControlRow(
          label = "SUPPLY POD RATE",
          valueText = String.format(Locale.US, "%.2fx", customCrateSpawnMult),
          onMinus = {
            val next = (((customCrateSpawnMult - 0.50f) * 100f).roundToInt() / 100f).coerceIn(0.50f, 2.50f)
            onSelectCustomCrateSpawnMult(next)
          },
          onPlus = {
            val next = (((customCrateSpawnMult + 0.50f) * 100f).roundToInt() / 100f).coerceIn(0.50f, 2.50f)
            onSelectCustomCrateSpawnMult(next)
          },
          onTapRow = {
            onOpenSliderWorkbench(
              OverwatchCircularPrompt.SliderWorkbench(
                key = "custom_supply_pods",
                title = "SUPPLY POD RATE",
                subtitle = "Spawn density multiplier for green tactical Supply Crates.",
                currentValue = customCrateSpawnMult,
                minValue = 0.50f,
                maxValue = 2.50f,
                stepSize = 0.10f,
                defaultValue = 1.00f,
                unitSuffix = "x",
                accentHex = 0xFF34D399L,
                presets = listOf("0.5x" to 0.50f, "1.0x" to 1.00f, "1.5x" to 1.50f, "2.5x" to 2.50f),
                valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                onValueChange = { onSelectCustomCrateSpawnMult(it.coerceIn(0.50f, 2.50f)) },
              )
            )
          },
        )
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .heightIn(min = 48.dp)
              .chamferedSciFiPodSurface(
                isRightSide = true,
                primaryBorder = if (customAllowArmadasInFfa) Color(0xFF00F5D4) else Color(0xFF38BDF8),
                secondaryBorder = Color(0xFF091528),
              )
              .clickable { onToggleAllowArmadasInFfa(!customAllowArmadasInFfa) }
              .padding(horizontal = 8.dp)
              .testTag("custom_toggle_allow_squadrons_ffa"),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "ALLOW SQUADRONS IN FFA",
            color = Color(0xFFE2E8F0),
            fontSize = 7.8.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 6.dp),
          )
          Text(
            text = if (customAllowArmadasInFfa) "ON" else "OFF",
            color = if (customAllowArmadasInFfa) Color(0xFF00F5D4) else Color(0xFFFB7185),
            fontSize = 8.4.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }

        // Host Broadcast Physics Toggle inside CREATE RIFT
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .heightIn(min = 48.dp)
              .chamferedSciFiPodSurface(
                isRightSide = true,
                primaryBorder = if (hostBroadcastPhysics) Color(0xFFFFD700) else Color(0xFF38BDF8),
                secondaryBorder = Color(0xFF091528),
              )
              .clickable { hostBroadcastPhysics = !hostBroadcastPhysics }
              .padding(horizontal = 8.dp)
              .testTag("custom_toggle_host_broadcast_physics"),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "HOST BROADCAST PHYSICS",
            color = Color(0xFFE2E8F0),
            fontSize = 7.6.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 6.dp),
          )
          Text(
            text = if (hostBroadcastPhysics) "LIVE" else "LOCAL",
            color = if (hostBroadcastPhysics) Color(0xFFFFD700) else Color(0xFF94A3B8),
            fontSize = 8.2.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
          )
        }

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
  onTapRow: (() -> Unit)? = null,
) {
  val snapped = snapCustomFivePercentScale(valuePct)
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .height(48.dp)
        .let { base -> if (onTapRow != null) base.clickable { onTapRow() } else base },
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
        modifier =
          Modifier.width(34.dp)
            .let { mod -> if (onTapRow != null) mod.clickable { onTapRow() } else mod }
            .testTag(valueTag),
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
  onOpenSliderWorkbench: (OverwatchCircularPrompt.SliderWorkbench) -> Unit = {},
) {
  val context = LocalContext.current
  var activeSubTab by remember { mutableIntStateOf(0) } // 0 = CONTROLS, 1 = AUDIO, 2 = SYSTEM
  var touchFloating by remember { mutableStateOf(RiftCalibrationState.touchFloatingJoystick) }
  var swapControls by remember { mutableStateOf(RiftCalibrationState.swapLeftRightControls) }
  var touchDeadzone by remember { mutableStateOf(RiftCalibrationState.touchJoystickDeadzone) }
  var touchSens by remember { mutableStateOf(RiftCalibrationState.touchJoystickSensitivity) }
  var gpDeadLeft by remember { mutableStateOf(RiftCalibrationState.gamepadDeadzoneLeft) }
  var gpDeadRight by remember { mutableStateOf(RiftCalibrationState.gamepadDeadzoneRight) }
  var hudBtnScale by remember { mutableStateOf(RiftCalibrationState.combatArcScale) }
  var hudIdleOpacity by remember { mutableStateOf(RiftCalibrationState.gamepadIdleTouchOpacity) }
  var graphicsTier by remember { mutableIntStateOf(RiftCalibrationState.graphicsQualityTier) }
  var preferMaxHz by remember { mutableStateOf(RiftCalibrationState.preferMaxRefreshRate) }
  var selectedBindSlotId by remember { mutableStateOf<String?>(null) }
  var synthPromptDraft by remember { mutableStateOf("") }
  var synthStatusNotice by remember { mutableStateOf<String?>(null) }
  val activeSynth = RiftSoundSynthesizer.activeInstance
  val activeTrackIdx = RiftSoundSynthesizer.activeTrackSlotIndex.coerceIn(0, 2)
  val activeMusicArchetype = activeSynth?.currentMusicRecipe?.archetype ?: MusicSynthArchetype.DARK_SYNTHWAVE

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

    // 3 Split Sub-Tabs: CONTROLS | AUDIO | SYSTEM
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
      listOf("CONTROLS", "AUDIO", "SYSTEM").forEachIndexed { idx, label ->
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
          // CONTROLS: Touch Stick Mode, Deadzones, Sensitivity, HUD Scale & Opacity, Swap Left/Right, 5x2 Gamepad Grid
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
            onTapRow = {
              onOpenSliderWorkbench(
                OverwatchCircularPrompt.SliderWorkbench(
                  key = "helm_touch_deadzone",
                  title = "TOUCH DEADZONE",
                  subtitle = "Inner radial deadzone before touch thrust engages.",
                  currentValue = touchDeadzone,
                  minValue = 0.02f,
                  maxValue = 0.30f,
                  stepSize = 0.01f,
                  defaultValue = 0.08f,
                  unitSuffix = "%",
                  accentHex = 0xFF00F5D4L,
                  presets = listOf("4%" to 0.04f, "8%" to 0.08f, "14%" to 0.14f, "22%" to 0.22f),
                  valueFormatter = { "${(it * 100f).roundToInt()}%" },
                  onValueChange = {
                    val v = it.coerceIn(0.02f, 0.30f)
                    touchDeadzone = v
                    RiftCalibrationState.touchJoystickDeadzone = v
                    RiftCalibrationState.savePersistedLayout(context)
                  },
                )
              )
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
            onTapRow = {
              onOpenSliderWorkbench(
                OverwatchCircularPrompt.SliderWorkbench(
                  key = "helm_touch_sens",
                  title = "TOUCH SENSITIVITY",
                  subtitle = "Response curve multiplier for virtual twin-stick deflection.",
                  currentValue = touchSens,
                  minValue = 0.60f,
                  maxValue = 1.80f,
                  stepSize = 0.05f,
                  defaultValue = 1.00f,
                  unitSuffix = "x",
                  accentHex = 0xFF38BDF8L,
                  presets = listOf("0.8x" to 0.80f, "1.0x" to 1.00f, "1.25x" to 1.25f, "1.5x" to 1.50f),
                  valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                  onValueChange = {
                    val v = it.coerceIn(0.60f, 1.80f)
                    touchSens = v
                    RiftCalibrationState.touchJoystickSensitivity = v
                    RiftCalibrationState.savePersistedLayout(context)
                  },
                )
              )
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
            onTapRow = {
              onOpenSliderWorkbench(
                OverwatchCircularPrompt.SliderWorkbench(
                  key = "helm_pad_l_deadzone",
                  title = "PAD L-DEADZONE",
                  subtitle = "Left analog stick drift filter for hardware controllers.",
                  currentValue = gpDeadLeft,
                  minValue = 0.04f,
                  maxValue = 0.36f,
                  stepSize = 0.01f,
                  defaultValue = 0.12f,
                  unitSuffix = "%",
                  accentHex = 0xFF00F5D4L,
                  presets = listOf("8%" to 0.08f, "12%" to 0.12f, "18%" to 0.18f, "26%" to 0.26f),
                  valueFormatter = { "${(it * 100f).roundToInt()}%" },
                  onValueChange = {
                    val v = it.coerceIn(0.04f, 0.36f)
                    gpDeadLeft = v
                    RiftCalibrationState.gamepadDeadzoneLeft = v
                    RiftCalibrationState.savePersistedLayout(context)
                  },
                )
              )
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
            onTapRow = {
              onOpenSliderWorkbench(
                OverwatchCircularPrompt.SliderWorkbench(
                  key = "helm_pad_r_deadzone",
                  title = "PAD R-DEADZONE",
                  subtitle = "Right analog aim stick deadzone before firing engages.",
                  currentValue = gpDeadRight,
                  minValue = 0.04f,
                  maxValue = 0.36f,
                  stepSize = 0.01f,
                  defaultValue = 0.18f,
                  unitSuffix = "%",
                  accentHex = 0xFFFACC15L,
                  presets = listOf("10%" to 0.10f, "18%" to 0.18f, "24%" to 0.24f, "30%" to 0.30f),
                  valueFormatter = { "${(it * 100f).roundToInt()}%" },
                  onValueChange = {
                    val v = it.coerceIn(0.04f, 0.36f)
                    gpDeadRight = v
                    RiftCalibrationState.gamepadDeadzoneRight = v
                    RiftCalibrationState.savePersistedLayout(context)
                  },
                )
              )
            },
          )

          HelmStepperControlRow(
            label = "HUD BUTTON SCALE",
            valueText = String.format(Locale.US, "%.2fx", hudBtnScale),
            onMinus = {
              val v = (hudBtnScale - 0.05f).coerceIn(0.70f, 1.40f)
              hudBtnScale = v
              RiftCalibrationState.combatArcScale = v
              RiftCalibrationState.savePersistedLayout(context)
            },
            onPlus = {
              val v = (hudBtnScale + 0.05f).coerceIn(0.70f, 1.40f)
              hudBtnScale = v
              RiftCalibrationState.combatArcScale = v
              RiftCalibrationState.savePersistedLayout(context)
            },
            onTapRow = {
              onOpenSliderWorkbench(
                OverwatchCircularPrompt.SliderWorkbench(
                  key = "helm_hud_btn_scale",
                  title = "HUD BUTTON SCALE",
                  subtitle = "Scales combat arc action buttons in live arena flight.",
                  currentValue = hudBtnScale,
                  minValue = 0.70f,
                  maxValue = 1.40f,
                  stepSize = 0.05f,
                  defaultValue = 1.00f,
                  unitSuffix = "x",
                  accentHex = 0xFF00F5D4L,
                  presets = listOf("0.85x" to 0.85f, "1.0x" to 1.00f, "1.15x" to 1.15f, "1.30x" to 1.30f),
                  valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                  onValueChange = {
                    val v = it.coerceIn(0.70f, 1.40f)
                    hudBtnScale = v
                    RiftCalibrationState.combatArcScale = v
                    RiftCalibrationState.savePersistedLayout(context)
                  },
                )
              )
            },
          )

          HelmStepperControlRow(
            label = "HUD OPACITY",
            valueText = "${(hudIdleOpacity * 100f).roundToInt()}%",
            onMinus = {
              val v = (hudIdleOpacity - 0.05f).coerceIn(0.15f, 1.00f)
              hudIdleOpacity = v
              RiftCalibrationState.gamepadIdleTouchOpacity = v
              RiftCalibrationState.savePersistedLayout(context)
            },
            onPlus = {
              val v = (hudIdleOpacity + 0.05f).coerceIn(0.15f, 1.00f)
              hudIdleOpacity = v
              RiftCalibrationState.gamepadIdleTouchOpacity = v
              RiftCalibrationState.savePersistedLayout(context)
            },
            onTapRow = {
              onOpenSliderWorkbench(
                OverwatchCircularPrompt.SliderWorkbench(
                  key = "helm_hud_opacity",
                  title = "HUD OPACITY",
                  subtitle = "Transparency of on-screen combat HUD rings and controls.",
                  currentValue = hudIdleOpacity,
                  minValue = 0.15f,
                  maxValue = 1.00f,
                  stepSize = 0.05f,
                  defaultValue = 0.28f,
                  unitSuffix = "%",
                  accentHex = 0xFF38BDF8L,
                  presets = listOf("25%" to 0.25f, "50%" to 0.50f, "75%" to 0.75f, "100%" to 1.00f),
                  valueFormatter = { "${(it * 100f).roundToInt()}%" },
                  onValueChange = {
                    val v = it.coerceIn(0.15f, 1.00f)
                    hudIdleOpacity = v
                    RiftCalibrationState.gamepadIdleTouchOpacity = v
                    RiftCalibrationState.savePersistedLayout(context)
                  },
                )
              )
            },
          )

          // Hardware Controller Remapper (5x2 Non-Scrolling Bind Grid)
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
                  LIVE_HARDWARE_BIND_CHIPS.chunked(2).forEach { rowPair ->
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      rowPair.forEach { (code, chipLabel) ->
                        Box(
                          modifier =
                            Modifier.weight(1f)
                              .minimumInteractiveComponentSize()
                              .heightIn(min = 48.dp)
                              .clickable {
                                RiftCalibrationState.assignControllerBinding(slot.slotId, code, context)
                                selectedBindSlotId = null
                              }
                              .padding(vertical = 3.dp),
                          contentAlignment = Alignment.Center,
                        ) {
                          Box(
                            modifier =
                              Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (code == currentCode) slot.accentColor.copy(alpha = 0.30f) else Color(0xFF060D1A))
                                .border(
                                  1.dp,
                                  if (code == currentCode) slot.accentColor else Color(0xFF38BDF8),
                                  RoundedCornerShape(5.dp),
                                )
                                .padding(horizontal = 4.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center,
                          ) {
                            Text(
                              text = chipLabel,
                              color = Color.White,
                              fontSize = 7.2.sp,
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
            }
          }
        }
        1 -> {
          // AUDIO & DISPLAY: Volumes, BPM, Overwatch SliderWorkbench + Embedded 3-Track Procedural Music & SFX Synth Deck
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

          HelmStepperControlRow(
            label = "MUSIC VOL",
            valueText = "${(musicVolume * 100f).roundToInt()}%",
            onMinus = { onChangeMusicVolume((musicVolume - 0.10f).coerceIn(0f, 1f)) },
            onPlus = { onChangeMusicVolume((musicVolume + 0.10f).coerceIn(0f, 1f)) },
            onTapRow = {
              onOpenSliderWorkbench(
                OverwatchCircularPrompt.SliderWorkbench(
                  key = "helm_music_vol",
                  title = "MUSIC VOLUME",
                  subtitle = "Procedural 3-Track synthesizer master mix.",
                  currentValue = musicVolume,
                  minValue = 0f,
                  maxValue = 1f,
                  stepSize = 0.05f,
                  defaultValue = 0.70f,
                  unitSuffix = "%",
                  accentHex = 0xFF00F5D4L,
                  presets = listOf("25%" to 0.25f, "50%" to 0.50f, "75%" to 0.75f, "100%" to 1.00f),
                  valueFormatter = { "${(it * 100f).roundToInt()}%" },
                  onValueChange = { onChangeMusicVolume(it.coerceIn(0f, 1f)) },
                )
              )
            },
          )
          HelmStepperControlRow(
            label = "SFX VOL",
            valueText = "${(sfxVolume * 100f).roundToInt()}%",
            onMinus = { onChangeSfxVolume((sfxVolume - 0.10f).coerceIn(0f, 1f)) },
            onPlus = { onChangeSfxVolume((sfxVolume + 0.10f).coerceIn(0f, 1f)) },
            onTapRow = {
              onOpenSliderWorkbench(
                OverwatchCircularPrompt.SliderWorkbench(
                  key = "helm_sfx_vol",
                  title = "SFX VOLUME",
                  subtitle = "Primary weapons, explosions, Repel ripples, and UI chimes.",
                  currentValue = sfxVolume,
                  minValue = 0f,
                  maxValue = 1f,
                  stepSize = 0.05f,
                  defaultValue = 0.85f,
                  unitSuffix = "%",
                  accentHex = 0xFF38BDF8L,
                  presets = listOf("25%" to 0.25f, "50%" to 0.50f, "85%" to 0.85f, "100%" to 1.00f),
                  valueFormatter = { "${(it * 100f).roundToInt()}%" },
                  onValueChange = { onChangeSfxVolume(it.coerceIn(0f, 1f)) },
                )
              )
            },
          )
          HelmStepperControlRow(
            label = "SYNTH BPM",
            valueText = "$musicBpm",
            onMinus = { onChangeMusicBpm((musicBpm - 6).coerceIn(96, 168)) },
            onPlus = { onChangeMusicBpm((musicBpm + 6).coerceIn(96, 168)) },
            onTapRow = {
              onOpenSliderWorkbench(
                OverwatchCircularPrompt.SliderWorkbench(
                  key = "helm_synth_bpm",
                  title = "SYNTH BPM",
                  subtitle = "Live tempo clock for the active procedural soundtrack.",
                  currentValue = musicBpm.toFloat(),
                  minValue = 96f,
                  maxValue = 168f,
                  stepSize = 2f,
                  defaultValue = 118f,
                  unitSuffix = " BPM",
                  accentHex = 0xFFFACC15L,
                  presets = listOf("96" to 96f, "118" to 118f, "138" to 138f, "160" to 160f),
                  valueFormatter = { "${it.roundToInt()} BPM" },
                  onValueChange = { onChangeMusicBpm(it.roundToInt().coerceIn(96, 168)) },
                )
              )
            },
          )

          // Embedded 3-Track Procedural Music & Weapon SFX Synthesizer Deck
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .chamferedSciFiPodSurface(
                  isRightSide = false,
                  primaryBorder = Color(0xFFFFD700),
                  secondaryBorder = Color(0xFF00F5D4),
                )
                .padding(8.dp)
                .testTag("helm_procedural_synth_deck"),
            verticalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Text(
              text = "3-TRACK PROCEDURAL SYNTH DECK",
              color = Color(0xFFFFD700),
              fontSize = 7.8.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )

            // Track A / Track B / Track C Selector Pills
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              listOf(0 to "TRACK A", 1 to "TRACK B", 2 to "TRACK C").forEach { (slotIdx, slotLabel) ->
                val isTrackSel = activeTrackIdx == slotIdx
                Box(
                  modifier =
                    Modifier.weight(1f)
                      .minimumInteractiveComponentSize()
                      .height(48.dp)
                      .clickable {
                        val recipe = activeSynth?.selectTrackSlot(slotIdx)
                        if (recipe != null) {
                          onChangeMusicBpm(recipe.bpm.coerceIn(96, 168))
                          synthStatusNotice = "$slotLabel ACTIVE • ${recipe.archetype.label.uppercase(Locale.US)}"
                        }
                      }
                      .padding(vertical = 5.dp)
                      .testTag("helm_synth_track_$slotIdx"),
                  contentAlignment = Alignment.Center,
                ) {
                  Box(
                    modifier =
                      Modifier.fillMaxSize()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isTrackSel) Color(0x3800F5D4) else Color(0xFF060D1A))
                        .border(
                          1.dp,
                          if (isTrackSel) Color(0xFF00F5D4) else Color(0xFF38BDF8),
                          RoundedCornerShape(6.dp),
                        ),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      text = slotLabel,
                      color = if (isTrackSel) Color.White else Color(0xFFCBD5E1),
                      fontSize = 7.4.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace,
                    )
                  }
                }
              }
            }

            // 5 MusicSynthArchetype Preset Chips
            MusicSynthArchetype.entries.chunked(2).forEach { pair ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                pair.forEach { arch ->
                  val isArchActive = activeMusicArchetype == arch
                  Box(
                    modifier =
                      Modifier.weight(1f)
                        .minimumInteractiveComponentSize()
                        .height(48.dp)
                        .clickable {
                          val recipe = activeSynth?.applyMusicArchetype(arch)
                          if (recipe != null) {
                            onChangeMusicBpm(recipe.bpm.coerceIn(96, 168))
                            synthStatusNotice = "PRESET: ${arch.label.uppercase(Locale.US)} (${recipe.bpm} BPM)"
                          }
                        }
                        .padding(vertical = 4.dp)
                        .testTag("helm_synth_arch_${arch.id.lowercase(Locale.US)}"),
                    contentAlignment = Alignment.Center,
                  ) {
                    Box(
                      modifier =
                        Modifier.fillMaxSize()
                          .clip(RoundedCornerShape(5.dp))
                          .background(if (isArchActive) Color(0x33FFD700) else Color(0xFF081324))
                          .border(
                            1.dp,
                            if (isArchActive) Color(0xFFFFD700) else Color(0x5538BDF8),
                            RoundedCornerShape(5.dp),
                          )
                          .padding(horizontal = 4.dp),
                      contentAlignment = Alignment.Center,
                    ) {
                      Text(
                        text = arch.label.uppercase(Locale.US),
                        color = if (isArchActive) Color(0xFFFEF08A) else Color(0xFFE2E8F0),
                        fontSize = 6.9.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }
                }
                if (pair.size == 1) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }

            // Natural-Language Prompt Music Synthesizer
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
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0B1528))
                    .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart,
              ) {
                if (synthPromptDraft.isEmpty()) {
                  Text(
                    text = "Describe mood or tempo (e.g. fast industrial 145)...",
                    color = Color(0xFF94A3B8),
                    fontSize = 7.2.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                BasicTextField(
                  value = synthPromptDraft,
                  onValueChange = { synthPromptDraft = it.take(48) },
                  singleLine = true,
                  cursorBrush = SolidColor(Color(0xFF00F5D4)),
                  textStyle =
                    TextStyle(
                      color = Color.White,
                      fontSize = 8.0.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.Monospace,
                    ),
                  modifier = Modifier.fillMaxWidth().testTag("helm_synth_prompt_input"),
                )
              }
              Box(
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .height(48.dp)
                    .clickable {
                      val recipe =
                        activeSynth?.synthesizeMusicFromPrompt(
                          prompt = synthPromptDraft.ifBlank { "dark synthwave 124 bpm" },
                          currentBpm = musicBpm,
                          saveToActiveSlot = true,
                        )
                      if (recipe != null) {
                        onChangeMusicBpm(recipe.bpm.coerceIn(96, 168))
                        synthStatusNotice = "FORGED -> ${recipe.archetype.label.uppercase(Locale.US)} (${recipe.bpm} BPM)"
                      }
                    }
                    .padding(vertical = 4.dp)
                    .testTag("helm_synth_forge_btn"),
                contentAlignment = Alignment.Center,
              ) {
                Box(
                  modifier =
                    Modifier.clip(RoundedCornerShape(6.dp))
                      .background(Color(0xFF0D2D46))
                      .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(6.dp))
                      .padding(horizontal = 7.dp, vertical = 7.dp),
                ) {
                  Text(
                    text = "FORGE",
                    color = Color(0xFF00F5D4),
                    fontSize = 7.4.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                  )
                }
              }
            }

            if (!synthStatusNotice.isNullOrBlank()) {
              Text(
                text = synthStatusNotice!!,
                color = Color(0xFF00F5D4),
                fontSize = 7.0.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            // 4 Instant Weapon & Combat SFX Audition Pads
            Text(
              text = "AUDITION LIVE COMBAT SFX",
              color = Color(0xFF38BDF8),
              fontSize = 7.2.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
            )
            val sfxPads =
              listOf(
                Triple("PRIMARY FIRE", Color(0xFF00F5D4)) { activeSynth?.playLaser() ?: Unit },
                Triple("EXPLOSION", Color(0xFFFB7185)) { activeSynth?.playExplosion() ?: Unit },
                Triple("SHIELD RIPPLE", Color(0xFF38BDF8)) { activeSynth?.playRepel() ?: Unit },
                Triple("UI CHIME", Color(0xFFFACC15)) { activeSynth?.playPrize() ?: Unit },
              )
            sfxPads.chunked(2).forEach { padRow ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                padRow.forEach { (padLabel, padAccent, triggerSfx) ->
                  Box(
                    modifier =
                      Modifier.weight(1f)
                        .minimumInteractiveComponentSize()
                        .height(48.dp)
                        .clickable { triggerSfx() }
                        .padding(vertical = 4.dp)
                        .testTag("helm_sfx_pad_${padLabel.lowercase(Locale.US).replace(" ", "_")}"),
                    contentAlignment = Alignment.Center,
                  ) {
                    Box(
                      modifier =
                        Modifier.fillMaxSize()
                          .clip(RoundedCornerShape(6.dp))
                          .background(padAccent.copy(alpha = 0.16f))
                          .border(1.dp, padAccent, RoundedCornerShape(6.dp)),
                      contentAlignment = Alignment.Center,
                    ) {
                      Text(
                        text = padLabel,
                        color = Color.White,
                        fontSize = 7.0.sp,
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

          // Display & Refresh Controls
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
            label = "ARENA ZOOM",
            valueText = String.format(Locale.US, "%.2fx", cameraZoom),
            onMinus = { onChangeCameraZoom((cameraZoom - 0.08f).coerceIn(0.65f, 1.45f)) },
            onPlus = { onChangeCameraZoom((cameraZoom + 0.08f).coerceIn(0.65f, 1.45f)) },
            onTapRow = {
              onOpenSliderWorkbench(
                OverwatchCircularPrompt.SliderWorkbench(
                  key = "helm_arena_zoom",
                  title = "ARENA ZOOM",
                  subtitle = "Live combat camera field-of-view scale.",
                  currentValue = cameraZoom,
                  minValue = 0.65f,
                  maxValue = 1.45f,
                  stepSize = 0.05f,
                  defaultValue = 1.00f,
                  unitSuffix = "x",
                  accentHex = 0xFF00F5D4L,
                  presets = listOf("0.75x" to 0.75f, "1.0x" to 1.00f, "1.20x" to 1.20f, "1.40x" to 1.40f),
                  valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                  onValueChange = { onChangeCameraZoom(it.coerceIn(0.65f, 1.45f)) },
                )
              )
            },
          )
          HelmStepperControlRow(
            label = "HUD TEXT",
            valueText = String.format(Locale.US, "%.2fx", playfieldTextScale),
            onMinus = { onChangePlayfieldTextScale((playfieldTextScale - 0.08f).coerceIn(0.80f, 1.35f)) },
            onPlus = { onChangePlayfieldTextScale((playfieldTextScale + 0.08f).coerceIn(0.80f, 1.35f)) },
            onTapRow = {
              onOpenSliderWorkbench(
                OverwatchCircularPrompt.SliderWorkbench(
                  key = "helm_hud_text",
                  title = "HUD TEXT SCALE",
                  subtitle = "Callsign, damage readout, and playfield telemetry text size.",
                  currentValue = playfieldTextScale,
                  minValue = 0.80f,
                  maxValue = 1.35f,
                  stepSize = 0.05f,
                  defaultValue = 1.00f,
                  unitSuffix = "x",
                  accentHex = 0xFF38BDF8L,
                  presets = listOf("0.85x" to 0.85f, "1.0x" to 1.00f, "1.15x" to 1.15f, "1.30x" to 1.30f),
                  valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                  onValueChange = { onChangePlayfieldTextScale(it.coerceIn(0.80f, 1.35f)) },
                )
              )
            },
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
                text = "FULL AUDIO SYNTH STUDIO >",
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
internal fun CodexBasicEmblem(
  basicIndex: Int,
  sizeDp: Dp,
) {
  Canvas(modifier = Modifier.size(sizeDp)) {
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val r = min(size.width, size.height) * 0.44f
    val strokeThin = (r * 0.11f).coerceAtLeast(1.4f)
    val strokeMed = (r * 0.15f).coerceAtLeast(1.8f)
    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(Color(0x4400F5D4), Color(0x18091528), Color.Transparent),
          center = Offset(cx, cy),
          radius = r * 1.15f,
        ),
      radius = r * 1.12f,
      center = Offset(cx, cy),
    )
    when (basicIndex) {
      0 -> {
        drawArc(
          color = Color(0xFF00F5D4),
          startAngle = -140f,
          sweepAngle = 280f,
          useCenter = false,
          topLeft = Offset(cx - r, cy - r),
          size = Size(r * 2f, r * 2f),
          style = Stroke(width = strokeMed, cap = StrokeCap.Round),
        )
        val chevron =
          Path().apply {
            moveTo(cx, cy - r * 0.62f)
            lineTo(cx + r * 0.46f, cy + r * 0.36f)
            lineTo(cx, cy + r * 0.10f)
            lineTo(cx - r * 0.46f, cy + r * 0.36f)
            close()
          }
        drawPath(chevron, color = Color(0xFF38BDF8).copy(alpha = 0.34f))
        drawPath(chevron, color = Color(0xFFE0F2FE), style = Stroke(width = strokeThin))
        drawCircle(Color(0xFFFFD700), radius = r * 0.14f, center = Offset(cx, cy + r * 0.46f))
      }
      1 -> {
        val diamond =
          Path().apply {
            moveTo(cx, cy - r * 0.86f)
            lineTo(cx + r * 0.82f, cy)
            lineTo(cx, cy + r * 0.86f)
            lineTo(cx - r * 0.82f, cy)
            close()
          }
        drawPath(diamond, color = Color(0x33FFD700))
        drawPath(diamond, color = Color(0xFFFFD700), style = Stroke(width = strokeMed))
        drawLine(
          color = Color(0xFF00F5D4),
          start = Offset(cx - r * 0.48f, cy),
          end = Offset(cx + r * 0.48f, cy),
          strokeWidth = strokeMed,
          cap = StrokeCap.Round,
        )
        drawCircle(Color.White, radius = r * 0.15f, center = Offset(cx, cy))
      }
      2 -> {
        val oct = Path()
        for (i in 0 until 8) {
          val ang = (i * 45.0 - 22.5) * PI / 180.0
          val px = cx + (cos(ang) * r * 0.88f).toFloat()
          val py = cy + (sin(ang) * r * 0.88f).toFloat()
          if (i == 0) oct.moveTo(px, py) else oct.lineTo(px, py)
        }
        oct.close()
        drawPath(oct, color = Color(0x2800F5D4))
        drawPath(oct, color = Color(0xFF00F5D4), style = Stroke(width = strokeMed))
        drawCircle(Color(0xFF38BDF8), radius = r * 0.42f, center = Offset(cx, cy), style = Stroke(width = strokeThin))
        drawCircle(Color(0xFFFFD700), radius = r * 0.18f, center = Offset(cx, cy))
      }
      else -> {
        drawCircle(Color(0xFF38BDF8), radius = r * 0.84f, center = Offset(cx, cy), style = Stroke(width = strokeThin))
        val pts =
          listOf(
            Offset(cx, cy - r * 0.50f),
            Offset(cx - r * 0.46f, cy + r * 0.34f),
            Offset(cx + r * 0.46f, cy + r * 0.34f),
          )
        drawLine(Color(0xFF00F5D4), pts[0], pts[1], strokeWidth = strokeThin)
        drawLine(Color(0xFF00F5D4), pts[1], pts[2], strokeWidth = strokeThin)
        drawLine(Color(0xFF00F5D4), pts[2], pts[0], strokeWidth = strokeThin)
        pts.forEachIndexed { idx, pt ->
          drawCircle(
            color = if (idx == 0) Color(0xFFFFD700) else Color(0xFF00F5D4),
            radius = r * 0.18f,
            center = pt,
          )
        }
      }
    }
  }
}

@Composable
internal fun RankedModeDistinctEmblem(
  modeIndex: Int,
  sizeDp: Dp,
) {
  Canvas(modifier = Modifier.size(sizeDp)) {
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val r = min(size.width, size.height) * 0.44f
    val sw = (r * 0.14f).coerceAtLeast(1.6f)
    when (modeIndex) {
      0 -> {
        drawCircle(Color(0x2800F5D4), radius = r * 0.96f, center = Offset(cx, cy))
        for (a in listOf(0f, 120f, 240f)) {
          drawArc(
            color = Color(0xFF00F5D4),
            startAngle = a,
            sweepAngle = 82f,
            useCenter = false,
            topLeft = Offset(cx - r * 0.88f, cy - r * 0.88f),
            size = Size(r * 1.76f, r * 1.76f),
            style = Stroke(width = sw, cap = StrokeCap.Round),
          )
        }
        drawCircle(Color(0xFF38BDF8), radius = r * 0.44f, center = Offset(cx, cy), style = Stroke(width = sw * 0.8f))
        drawCircle(Color.White, radius = r * 0.16f, center = Offset(cx, cy))
      }
      1 -> {
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(Color.White, Color(0xFFFFD700), Color(0x44F59E0B), Color.Transparent),
              center = Offset(cx, cy),
              radius = r,
            ),
          radius = r,
          center = Offset(cx, cy),
        )
        for (a in listOf(-35f, 55f, 145f, 235f)) {
          drawArc(
            color = Color(0xFFFEF08A),
            startAngle = a,
            sweepAngle = 54f,
            useCenter = false,
            topLeft = Offset(cx - r * 0.84f, cy - r * 0.84f),
            size = Size(r * 1.68f, r * 1.68f),
            style = Stroke(width = sw, cap = StrokeCap.Round),
          )
        }
        drawCircle(Color(0xFFFFD700), radius = r * 0.34f, center = Offset(cx, cy))
      }
      2 -> {
        drawCircle(Color(0x2638BDF8), radius = r * 0.92f, center = Offset(cx, cy))
        val leftWing =
          Path().apply {
            moveTo(cx - r * 0.72f, cy + r * 0.48f)
            lineTo(cx - r * 0.12f, cy - r * 0.68f)
            lineTo(cx + r * 0.06f, cy - r * 0.14f)
            close()
          }
        val rightWing =
          Path().apply {
            moveTo(cx + r * 0.72f, cy + r * 0.48f)
            lineTo(cx + r * 0.12f, cy - r * 0.68f)
            lineTo(cx - r * 0.06f, cy - r * 0.14f)
            close()
          }
        drawPath(leftWing, color = Color(0xFF38BDF8))
        drawPath(rightWing, color = Color(0xFF00F5D4))
        drawLine(Color(0xFFFFD700), Offset(cx - r * 0.58f, cy + r * 0.64f), Offset(cx + r * 0.58f, cy - r * 0.64f), strokeWidth = sw * 0.85f, cap = StrokeCap.Round)
      }
      3 -> {
        val rRing = r * 0.56f
        val cA = Offset(cx - r * 0.32f, cy)
        val cB = Offset(cx + r * 0.32f, cy)
        drawCircle(Color(0x2234D399), radius = rRing, center = cA)
        drawCircle(Color(0x2200F5D4), radius = rRing, center = cB)
        drawCircle(Color(0xFF34D399), radius = rRing, center = cA, style = Stroke(width = sw))
        drawCircle(Color(0xFF00F5D4), radius = rRing, center = cB, style = Stroke(width = sw))
        drawCircle(Color(0xFFFFD700), radius = r * 0.14f, center = cA)
        drawCircle(Color(0xFFFFD700), radius = r * 0.14f, center = cB)
      }
      4 -> {
        val hex = Path()
        for (i in 0 until 6) {
          val ang = (i * 60.0 - 30.0) * PI / 180.0
          val px = cx + (cos(ang) * r * 0.84f).toFloat()
          val py = cy + (sin(ang) * r * 0.84f).toFloat()
          if (i == 0) hex.moveTo(px, py) else hex.lineTo(px, py)
        }
        hex.close()
        drawPath(hex, color = Color(0x2BC084FC))
        drawPath(hex, color = Color(0xFFC084FC), style = Stroke(width = sw))
        drawLine(
          color = Color(0xFF00F5D4),
          start = Offset(cx - r * 0.72f, cy + r * 0.56f),
          end = Offset(cx + r * 0.72f, cy - r * 0.56f),
          strokeWidth = sw * 1.15f,
          cap = StrokeCap.Round,
        )
        drawCircle(Color.White, radius = r * 0.16f, center = Offset(cx, cy))
      }
      5 -> {
        val shield =
          Path().apply {
            moveTo(cx, cy - r * 0.86f)
            lineTo(cx + r * 0.74f, cy - r * 0.42f)
            lineTo(cx + r * 0.56f, cy + r * 0.42f)
            lineTo(cx, cy + r * 0.88f)
            lineTo(cx - r * 0.56f, cy + r * 0.42f)
            lineTo(cx - r * 0.74f, cy - r * 0.42f)
            close()
          }
        drawPath(shield, color = Color(0x33FB7185))
        drawPath(shield, color = Color(0xFFFB7185), style = Stroke(width = sw))
        drawLine(Color(0xFFFFD700), Offset(cx - r * 0.44f, cy + r * 0.42f), Offset(cx + r * 0.44f, cy - r * 0.42f), strokeWidth = sw * 0.9f, cap = StrokeCap.Round)
        drawLine(Color(0xFFFFD700), Offset(cx + r * 0.44f, cy + r * 0.42f), Offset(cx - r * 0.44f, cy - r * 0.42f), strokeWidth = sw * 0.9f, cap = StrokeCap.Round)
        drawCircle(Color.White, radius = r * 0.14f, center = Offset(cx, cy))
      }
      else -> {
        drawCircle(Color(0x2600F5D4), radius = r * 0.90f, center = Offset(cx, cy))
        for ((rowIdx, yOff) in listOf(-r * 0.42f, 0f, r * 0.42f).withIndex()) {
          val y = cy + yOff
          drawLine(Color(0xFF38BDF8), Offset(cx - r * 0.68f, y), Offset(cx + r * 0.68f, y), strokeWidth = sw * 0.8f, cap = StrokeCap.Round)
          val knobX = cx + listOf(-r * 0.24f, r * 0.28f, -r * 0.06f)[rowIdx]
          drawCircle(Color(0xFFFFD700), radius = r * 0.17f, center = Offset(knobX, y))
        }
      }
    }
  }
}

private data class CodexCardDescriptor(
  val heading: String,
  val badge: String,
  val shortSubtitle: String,
  val fullSummary: String,
  val tacticalNote: String = "",
  val accentColor: Color,
  val accentHex: Long,
)

private fun cleanCodexBuffHeading(buffTypeIndex: Int, rawTitle: String): String =
  when (buffTypeIndex) {
    11 -> "SWARM DRONES"
    10 -> "5-STAT HULL TUNING"
    else -> rawTitle.replace(Regex("\\s*\\([^)]*\\)"), "").trim().uppercase(Locale.US)
  }

private fun cleanCodexBuffShortSubtitle(buffTypeIndex: Int): String =
  when (buffTypeIndex) {
    0 -> "Stackable 360 plasma shockwaves"
    1 -> "Point-blank kinetic fan blast"
    2 -> "Predictive homing heavy torpedo"
    3 -> "Cloaking decoy with EMP"
    4 -> "Deployable hard-light shield wall"
    5 -> "Zero-Flux supersonic speed burst"
    6 -> "Instant 750 Flux restore"
    7 -> "Extra radial shockwave charge"
    8 -> "Angled side-barrel weapon split"
    9 -> "Wall-bouncing primary weapon bolts"
    10 -> "Stacking hull stat upgrade"
    11 -> "Five weaving hunter micro-drones"
    12 -> "Gravimetric snare laser toggle"
    13 -> "Re-locking corridor patrol warhead"
    14 -> "Stacking thermal cutting beam"
    15 -> "Four-bounce serrated ricochet disc"
    else -> "Tactical Supply Crate ordnance"
  }

private fun cleanCodexRiftShortSubtitle(riftId: String): String =
  when (riftId) {
    "plasma_rift" -> "High-energy Flux burn storm"
    "repulsor_rift" -> "Ice-blue radial knockback field"
    "gravity_rift" -> "Inward singularity pull vortex"
    "unstable_rift" -> "Collapsing shockwave blast hazard"
    else -> "Linked instant sector transit"
  }

private fun sanitizeCodexBodyCopy(raw: String): String =
  raw
    .replace("Spacetime-Tear Rift", "Unstable Rift", ignoreCase = true)
    .replace("High-Value Targets", "High-Resonance Targets", ignoreCase = true)
    .replace("High-Value Target", "High-Resonance Target", ignoreCase = true)

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
      CodexCardDescriptor(
        heading = "TWIN-STICK FLIGHT & SHARED FLUX",
        badge = "FLIGHT",
        shortSubtitle = "Zero-drift thrust and shared Flux",
        fullSummary = "Left stick controls thrust and heading with zero momentum drift on release; right stick or action buttons fire primary weapons, secondary ordnance, brake, Repel, and Crate buffs. Your Flux ring powers both shields and weapons.",
        tacticalNote = "Retreat briefly or dock at the Rift Station Safe Harbor to recharge Flux before re-engaging.",
        accentColor = Color(0xFF00F5D4),
        accentHex = 0xFF00F5D4L,
      ),
      CodexCardDescriptor(
        heading = "PER-SHIP RESONANCE & 100-PT PARITY",
        badge = "PARITY",
        shortSubtitle = "Identical 100-point hull balance",
        fullSummary = "All 10 ships operate on an identical 100-Point stat and weapon budget from 0 Resonance. Ranked rewards unlock Frame C-E bays, Finishers, Halos, and Warps without ever granting paid or ranked stat advantages.",
        tacticalNote = "Your highest-Resonance ship is highlighted in Solar Gold across Hangar and Pilot Dossier.",
        accentColor = Color(0xFFFFD700),
        accentHex = 0xFFFFD700L,
      ),
      CodexCardDescriptor(
        heading = "5X RIFT STATION SAFE HARBOR",
        badge = "STATION",
        shortSubtitle = "Central disarm recharge sanctuary",
        fullSummary = "Sector Convergence and Flux Core anchor the map with a 5x ship-scale Rift Station Safe Harbor. Weapons disarm inside while Flux rapidly recharges; staying past 15 seconds triggers an automatic sector eviction lockout.",
        tacticalNote = "Objective team modes disable the central Safe Harbor to enforce continuous territory control.",
        accentColor = Color(0xFF38BDF8),
        accentHex = 0xFF38BDF8L,
      ),
      CodexCardDescriptor(
        heading = "SYNDICATES, SQUADRONS & SHIP SKINS",
        badge = "SOCIAL",
        shortSubtitle = "Syndicates, Squadrons, Ship Skins",
        fullSummary = "Charter or join a real-player Syndicate, form a 5-pilot Squadron with team voice chat, earn Ranked cosmetics via Live Resonance, or unlock Frame B and five permanent hull liveries in the Shard Store.",
        tacticalNote = "Open Subspace Comms anytime for live channels, Squadron LFG invites, and tactical Rift AI.",
        accentColor = Color(0xFFC084FC),
        accentHex = 0xFFC084FCL,
      ),
    )
  }

  val modeCards = remember {
    listOf(
      CodexCardDescriptor(
        heading = "SECTOR CONVERGENCE",
        badge = "OPEN 60M",
        shortSubtitle = "Open Safe Harbor arena",
        fullSummary = "Continuous 60-minute open sector dogfighting anchored by the central 5x Rift Station Safe Harbor. Harvest Supply Crates, ride Playfield Rifts, and build Live Resonance.",
        tacticalNote = "Eliminating the sector Rift Leader awards bonus Resonance and shuts down their streak.",
        accentColor = Color(0xFF00F5D4),
        accentHex = 0xFF00F5D4L,
      ),
      CodexCardDescriptor(
        heading = "FLUX CORE",
        badge = "OBJECTIVE",
        shortSubtitle = "Hold central Flux Core orb",
        fullSummary = "Capture and carry the golden Flux Core Singularity Orb at the center of the sector to generate continuous Resonance while fending off rival pilots and squads.",
        tacticalNote = "Escort your Flux Core carrier with Stasis Barriers and Repulsor Rifts to extend hold streaks.",
        accentColor = Color(0xFFFACC15),
        accentHex = 0xFFFACC15L,
      ),
      CodexCardDescriptor(
        heading = "RIFT SKIRMISH",
        badge = "2V2-5V5",
        shortSubtitle = "Matched Squadron team combat",
        fullSummary = "Coordinated 2v2 through 5v5 Squadron team battles matched against the closest-Resonance opponent squad within a crisp 15-second matchmaking window.",
        tacticalNote = "Focus fire on low-Flux targets and rotate wingmates to preserve team Resonance.",
        accentColor = Color(0xFF38BDF8),
        accentHex = 0xFF38BDF8L,
      ),
      CodexCardDescriptor(
        heading = "RIFT CONTROL",
        badge = "2 RINGS",
        shortSubtitle = "Contest Alpha and Omega rings",
        fullSummary = "Contest and hold the Alpha and Omega territory beacons in 3v3, 5v5, or 10v10 brackets to score team dominance points with no central Safe Harbor active.",
        tacticalNote = "Split a fast interceptor wing to pressure the far beacon whenever rivals over-stack one ring.",
        accentColor = Color(0xFF34D399),
        accentHex = 0xFF34D399L,
      ),
      CodexCardDescriptor(
        heading = "STASIS BREACH",
        badge = "RESCUE",
        shortSubtitle = "Breach rival Brig reactor",
        fullSummary = "Eliminated pilots are detained inside the opposing Brig Reactor. Crack the enemy Brig shield to trigger a full-team breakout or eliminate every opposing pilot to sweep the round.",
        tacticalNote = "Save heavy piercing ordnance like Lance Beam or Seeker Torpedo to crack the Brig reactor fast.",
        accentColor = Color(0xFFC084FC),
        accentHex = 0xFFC084FCL,
      ),
      CodexCardDescriptor(
        heading = "SYNDICATE WARS",
        badge = "CHARTERED",
        shortSubtitle = "Official chartered Syndicate warfare",
        fullSummary = "Official 3v3, 5v5, and 10v10 chartered Syndicate warfare competing for War Record standings, Syndicate Leaderboard prestige, and sector supremacy.",
        tacticalNote = "Requires an active player-founded Syndicate charter and matching Squadron bracket formation.",
        accentColor = Color(0xFFFB7185),
        accentHex = 0xFFFB7185L,
      ),
      CodexCardDescriptor(
        heading = "CUSTOM RIFTS & TUTORIAL",
        badge = "SANDBOX",
        shortSubtitle = "Custom rules and Flight Guide",
        fullSummary = "Host public or private 6-character room-code Custom Rifts with Overwatch sliders for damage, Flux cost, speed, and bots, or run the 2-phase Guided Tutorial anytime.",
        tacticalNote = "Custom Rifts and Tutorial flight sessions let you test any 100-Point build risk-free.",
        accentColor = Color(0xFF00F5D4),
        accentHex = 0xFF00F5D4L,
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
              color = if (sel) Color.White else Color(0xFFCBD5E1),
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
          basicsCards.forEachIndexed { idx, card ->
            RiftVerticalCard(
              modifier = Modifier.fillMaxWidth(),
              title = card.heading,
              subtitle = card.shortSubtitle,
              accent = card.accentColor,
              isRightColumn = true,
              badgeText = card.badge,
              badgeColor = card.accentColor,
              isTabletDeck = isTabletDeck,
              minHeightDp = 82.dp,
              testTag = "codex_basics_card_$idx",
              visualContent = { sizeDp ->
                CodexBasicEmblem(
                  basicIndex = idx,
                  sizeDp = (sizeDp * 0.72f).coerceAtLeast(24.dp),
                )
              },
              onClick = {
                onInspectCodexStage(
                  OverwatchCircularPrompt.InfoStageInspect(
                    kicker = "CODEX • FLIGHT BASICS",
                    title = card.heading,
                    badge = card.badge,
                    summary = card.fullSummary,
                    tacticalNote = card.tacticalNote,
                    accentHex = card.accentHex,
                    showStationPreview = idx == 2,
                    codexBasicIndex = idx,
                  )
                )
              },
            )
          }
        }
        1 -> {
          modeCards.forEachIndexed { idx, card ->
            RiftVerticalCard(
              modifier = Modifier.fillMaxWidth(),
              title = card.heading,
              subtitle = card.shortSubtitle,
              accent = card.accentColor,
              isRightColumn = true,
              badgeText = card.badge,
              badgeColor = card.accentColor,
              isTabletDeck = isTabletDeck,
              minHeightDp = 82.dp,
              testTag = "codex_modes_card_$idx",
              visualContent = { sizeDp ->
                RankedModeDistinctEmblem(
                  modeIndex = idx,
                  sizeDp = (sizeDp * 0.76f).coerceAtLeast(24.dp),
                )
              },
              onClick = {
                onInspectCodexStage(
                  OverwatchCircularPrompt.InfoStageInspect(
                    kicker = "CODEX • GAME MODES",
                    title = card.heading,
                    badge = card.badge,
                    summary = card.fullSummary,
                    tacticalNote = card.tacticalNote,
                    accentHex = card.accentHex,
                    codexModeIndex = idx,
                  )
                )
              },
            )
          }
        }
        2 -> {
          RiftCodex.POWERUP_CODEX.forEach { buff ->
            val cleanTitle = cleanCodexBuffHeading(buff.buffTypeIndex, buff.title)
            val shortSub = cleanCodexBuffShortSubtitle(buff.buffTypeIndex)
            val cleanSummary = sanitizeCodexBodyCopy(buff.effectSummary)
            val cleanNote = sanitizeCodexBodyCopy(buff.tacticalNote)
            RiftVerticalCard(
              modifier = Modifier.fillMaxWidth(),
              title = cleanTitle,
              subtitle = shortSub,
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
                    title = cleanTitle,
                    badge = "${buff.dropRatePercent}% DROP",
                    summary = cleanSummary,
                    tacticalNote = cleanNote,
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
            val cleanRiftSummary = sanitizeCodexBodyCopy(rift.effectSummary)
            val cleanRiftCounter = sanitizeCodexBodyCopy(rift.tacticalNote)
            RiftVerticalCard(
              modifier = Modifier.fillMaxWidth(),
              title = rift.title.uppercase(Locale.US),
              subtitle = cleanCodexRiftShortSubtitle(rift.id),
              accent = riftAccent,
              isRightColumn = true,
              badgeText = rift.specBadge.uppercase(Locale.US),
              badgeColor = riftAccent,
              isTabletDeck = isTabletDeck,
              minHeightDp = 88.dp,
              testTag = "codex_rift_card_${rift.id}",
              visualContent = { sizeDp ->
                val phase = ((fluxOrbitRad / (2.0 * PI)).toFloat() % 1f).let { if (it < 0f) it + 1f else it }
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
          allShips.forEach { ship ->
            val cleanName = cleanDisplayShipTitle(ship.id, ship.name)
            val shipSubtitle =
              if (ship.id == 10) "Modular 100-point custom hull"
              else "${ship.role} • ${ship.primaryShortLabel}"
            RiftVerticalCard(
              modifier = Modifier.fillMaxWidth(),
              title = cleanName,
              subtitle = shipSubtitle,
              accent = if (ship.id == 10) Color(0xFF00F5D4) else Color(0xFF38BDF8),
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
