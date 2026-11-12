package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.engine.RiftCalibrationState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val NEXUS_STARTER_CHIPS =
  listOf(
    "How many pixels are Suppressor's lasers?",
    "Who holds the highest bounty ever?",
    "Theorycraft a 100-pt Ship 9 sniper build",
    "Analyze my Peak Bounty Run & Executioner",
    "Compare Goliath vs Valkyrie DPS & recoil",
    "Explain Gravity & Phase Rift physics",
  )

/**
 * Procedurally animated swirling Rift vortex glyph (cyan/violet accretion arcs + dark singularity core).
 */
@Composable
fun SwirlingRiftVortexGlyph(
  modifier: Modifier = Modifier,
  isStreaming: Boolean = false,
) {
  val transition = rememberInfiniteTransition(label = "nexus_rift_vortex")
  val outerDeg by
    transition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = if (isStreaming) 1350 else 3600, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "vortex_outer_deg",
    )
  val corePulse by
    transition.animateFloat(
      initialValue = 0.72f,
      targetValue = 1.0f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = if (isStreaming) 480 else 1100, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "vortex_core_pulse",
    )

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val r = minOf(w, h) * 0.5f
    val c = Offset(w * 0.5f, h * 0.5f)
    if (r <= 1f) return@Canvas

    // 1. Ambient singularity radial aura
    drawCircle(
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              Color(0xAA00F5D4),
              Color(0x777C3AED),
              Color.Transparent,
            ),
          center = c,
          radius = r * 1.02f,
        ),
      radius = r,
      center = c,
    )

    // 2. Outer rotating accretion arcs (Electric Cyan & Event-Horizon Violet)
    val outerInset = r * 0.14f
    val outerSize = Size((r - outerInset) * 2f, (r - outerInset) * 2f)
    val outerTopLeft = Offset(c.x - (r - outerInset), c.y - (r - outerInset))
    for (arm in 0 until 3) {
      val startA = outerDeg + arm * 120f
      drawArc(
        color = if (arm % 2 == 0) Color(0xFF00F5D4) else Color(0xFFB388FF),
        startAngle = startA,
        sweepAngle = 74f,
        useCenter = false,
        topLeft = outerTopLeft,
        size = outerSize,
        style = Stroke(width = (r * 0.13f).coerceAtLeast(1.8f), cap = StrokeCap.Round),
      )
    }

    // 3. Counter-rotating inner plasma ring
    val innerInset = r * 0.34f
    val innerSize = Size((r - innerInset) * 2f, (r - innerInset) * 2f)
    val innerTopLeft = Offset(c.x - (r - innerInset), c.y - (r - innerInset))
    for (arm in 0 until 2) {
      val startA = -outerDeg * 1.45f + arm * 180f
      drawArc(
        color = if (arm == 0) Color(0xFF38BDF8) else Color(0xFFE879F9),
        startAngle = startA,
        sweepAngle = 105f,
        useCenter = false,
        topLeft = innerTopLeft,
        size = innerSize,
        style = Stroke(width = (r * 0.11f).coerceAtLeast(1.5f), cap = StrokeCap.Round),
      )
    }

    // 4. Swirling quantum filaments spiraling into core
    val radBase = outerDeg * (PI.toFloat() / 180f)
    for (i in 0 until 4) {
      val a1 = radBase + i * (PI.toFloat() * 0.5f)
      val a2 = a1 + 0.72f
      val p1 = Offset(c.x + cos(a1) * (r * 0.78f), c.y + sin(a1) * (r * 0.78f))
      val p2 = Offset(c.x + cos(a2) * (r * 0.30f), c.y + sin(a2) * (r * 0.30f))
      drawLine(
        color = Color.White.copy(alpha = 0.72f * corePulse),
        start = p1,
        end = p2,
        strokeWidth = (r * 0.07f).coerceAtLeast(1.1f),
        cap = StrokeCap.Round,
      )
    }

    // 5. Pitch-black event horizon singularity + glowing photon ring
    drawCircle(
      color = Color(0xFF030712),
      radius = r * 0.31f,
      center = c,
    )
    drawCircle(
      color = Color(0xFF00F5D4).copy(alpha = corePulse),
      radius = r * 0.32f,
      center = c,
      style = Stroke(width = (r * 0.09f).coerceAtLeast(1.4f)),
    )
    drawCircle(
      color = Color.White.copy(alpha = corePulse),
      radius = r * 0.11f * corePulse,
      center = c,
    )
  }
}

/**
 * Pure, frameless, textless swirling Rift vortex button anchored on the right side inside the SPEED / SECTOR console pill.
 */
@Composable
fun SwirlingRiftHudOrb(
  modifier: Modifier = Modifier,
  isStreaming: Boolean = false,
  onClick: () -> Unit,
) {
  val desc = stringResource(R.string.nexus_orb_content_desc)
  val unlocked = RiftCalibrationState.isUiUnlockedForDragging

  Box(
    modifier =
      modifier
        .size(38.dp)
        .clip(CircleShape)
        .semantics { contentDescription = desc }
        .clickable(enabled = !unlocked) { onClick() }
        .testTag("hud_btn_nexus_orb"),
    contentAlignment = Alignment.Center,
  ) {
    SwirlingRiftVortexGlyph(
      modifier = Modifier.size(34.dp),
      isStreaming = isStreaming,
    )
  }
}

/**
 * Compact swirling Rift header button for Hangar and Codex top bars.
 */
@Composable
fun SwirlingRiftHeaderPill(
  onClick: () -> Unit,
  tag: String,
  isStreaming: Boolean = false,
) {
  val desc = stringResource(R.string.nexus_orb_content_desc)
  Row(
    modifier =
      Modifier.minimumInteractiveComponentSize()
        .height(34.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(
          Brush.horizontalGradient(
            listOf(Color(0xFF09192F), Color(0xFF1E1038))
          )
        )
        .border(
          width = 1.dp,
          brush = Brush.horizontalGradient(listOf(Color(0xFF00F5D4), Color(0xFFB388FF))),
          shape = RoundedCornerShape(8.dp),
        )
        .semantics { contentDescription = desc }
        .clickable { onClick() }
        .padding(horizontal = 9.dp)
        .testTag(tag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    SwirlingRiftVortexGlyph(
      modifier = Modifier.size(18.dp),
      isStreaming = isStreaming,
    )
    Text(
      text = "NEXUS AI",
      color = Color(0xFF00F5D4),
      fontSize = 9.5.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
      letterSpacing = 0.5.sp,
    )
  }
}

/**
 * Translucent live-arena tactical console for NEXUS (Omniscient Rift Core Tactical Intelligence).
 * Keeps the 60 FPS space arena clearly visible behind the glass surface while streaming exact RIFT answers.
 */
@Composable
fun NexusAiConsoleModal(
  messages: List<NexusChatMessage>,
  streamState: NexusStreamState,
  lastFailedQuestion: String?,
  activeShipSummary: String,
  pilotLevel: Int,
  pointBudget: Int,
  highestBountySummary: String,
  onSendQuestion: (String) -> Unit,
  onRetryLastQuestion: () -> Unit,
  onClearHistory: () -> Unit,
  onCloseModal: () -> Unit,
) {
  var inputQuery by remember { mutableStateOf("") }
  val keyboardController = LocalSoftwareKeyboardController.current
  val listState = rememberLazyListState()
  val isStreaming =
    streamState is NexusStreamState.Loading || streamState is NexusStreamState.Streaming

  // Auto-scroll to newest token chunk as NEXUS streams
  val lastMsgTextLen = messages.lastOrNull()?.text?.length ?: 0
  LaunchedEffect(messages.size, lastMsgTextLen, streamState) {
    val totalItems = listState.layoutInfo.totalItemsCount
    if (totalItems > 0) {
      listState.animateScrollToItem(totalItems - 1)
    }
  }

  fun submitCurrentInput() {
    val trimmed = inputQuery.trim()
    if (trimmed.isNotEmpty() && !isStreaming) {
      inputQuery = ""
      keyboardController?.hide()
      onSendQuestion(trimmed)
    }
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        // Subtle translucent abyssal tint (~42% alpha) so the live 60 FPS space arena remains clearly visible behind!
        .background(Color(0x6B020611))
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .imePadding()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .testTag("nexus_ai_console_modal"),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier =
        Modifier.widthIn(max = 720.dp)
          .fillMaxWidth(0.92f)
          .fillMaxHeight(0.90f)
          .border(
            width = 1.4.dp,
            brush =
              Brush.linearGradient(
                colors =
                  listOf(
                    Color(0xCC00F5D4),
                    Color(0xCCB388FF),
                    Color(0xAA38BDF8),
                  )
              ),
            shape = RoundedCornerShape(16.dp),
          ),
      // Translucent tactical dark glass (~76% alpha) preserving live space visibility behind messages
      color = Color(0xC2060D1B),
      shape = RoundedCornerShape(16.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        // 1. Top Tactical Header Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
          ) {
            SwirlingRiftVortexGlyph(
              modifier = Modifier.size(34.dp),
              isStreaming = isStreaming,
            )
            Column {
              Text(
                text = stringResource(R.string.nexus_console_title),
                color = Color(0xFF00F5D4),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "HULL: $activeShipSummary • LVL $pilotLevel ($pointBudget PTS) • TOP RECORD: $highestBountySummary",
                color = Color(0xFFCBD5E1),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            if (messages.isNotEmpty()) {
              Button(
                onClick = onClearHistory,
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = Color(0xAA0F172A),
                    contentColor = Color(0xFF94A3B8),
                  ),
                border = BorderStroke(1.dp, Color(0x66475569)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .height(36.dp)
                    .testTag("nexus_clear_button"),
              ) {
                Text(
                  text = "↺ ${stringResource(R.string.nexus_clear)}",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }

            Button(
              onClick = onCloseModal,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = Color(0xCC28111B),
                  contentColor = Color(0xFFFF3B4E),
                ),
              border = BorderStroke(1.dp, Color(0xFFFF3B4E)),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .height(36.dp)
                  .testTag("nexus_close_button"),
            ) {
              Text(
                text = "✕ ${stringResource(R.string.nexus_close)}",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
        }

        // 2. Horizontally Scrollable Tactical Quick-Query Chips
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          NEXUS_STARTER_CHIPS.forEachIndexed { idx, chipPrompt ->
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(14.dp))
                  .background(Color(0x990D1B2E))
                  .border(1.dp, Color(0x6638BDF8), RoundedCornerShape(14.dp))
                  .clickable(enabled = !isStreaming) { onSendQuestion(chipPrompt) }
                  .padding(horizontal = 10.dp, vertical = 6.dp)
                  .testTag("nexus_quick_chip_$idx"),
            ) {
              Text(
                text = "❖ $chipPrompt",
                color = if (isStreaming) Color(0xFF64748B) else Color(0xFFE0F2FE),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
              )
            }
          }
        }

        // 3. Translucent Conversation Stream
        LazyColumn(
          state = listState,
          modifier =
            Modifier.weight(1f)
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0x59030712))
              .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(10.dp))
              .padding(horizontal = 10.dp, vertical = 8.dp)
              .testTag("nexus_message_list"),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          if (messages.isEmpty()) {
            item(key = "nexus_zero_state") {
              NexusWelcomeTelemetryCard()
            }
          }

          items(items = messages, key = { it.id }) { msg ->
            NexusMessageBubble(message = msg)
          }

          if (streamState is NexusStreamState.Error) {
            item(key = "nexus_error_card") {
              NexusErrorBanner(
                errorMessage = streamState.message,
                canRetry = !lastFailedQuestion.isNullOrBlank(),
                onRetry = onRetryLastQuestion,
              )
            }
          }
        }

        // 4. Touch Query Input & Transmit Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier =
              Modifier.weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xCC091324))
                .border(
                  width = 1.dp,
                  color = if (isStreaming) Color(0x55475569) else Color(0x9900F5D4),
                  shape = RoundedCornerShape(10.dp),
                )
                .padding(horizontal = 12.dp)
                .testTag("nexus_query_input_box"),
            contentAlignment = Alignment.CenterStart,
          ) {
            if (inputQuery.isEmpty()) {
              Text(
                text = stringResource(R.string.nexus_input_placeholder),
                color = Color(0xFF64748B),
                fontSize = 11.5.sp,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            BasicTextField(
              value = inputQuery,
              onValueChange = { inputQuery = it },
              enabled = !isStreaming,
              singleLine = true,
              cursorBrush = SolidColor(Color(0xFF00F5D4)),
              textStyle =
                TextStyle(
                  color = Color.White,
                  fontSize = 12.sp,
                  fontFamily = FontFamily.SansSerif,
                ),
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
              keyboardActions = KeyboardActions(onSend = { submitCurrentInput() }),
              modifier = Modifier.fillMaxWidth().testTag("nexus_query_input"),
            )
          }

          Button(
            onClick = { submitCurrentInput() },
            enabled = inputQuery.isNotBlank() && !isStreaming,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0E7490),
                contentColor = Color.White,
                disabledContainerColor = Color(0x661E293B),
                disabledContentColor = Color(0xFF64748B),
              ),
            border =
              BorderStroke(
                1.dp,
                if (inputQuery.isNotBlank() && !isStreaming) Color(0xFF00F5D4) else Color(0x44475569),
              ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .height(44.dp)
                .testTag("nexus_send_button"),
          ) {
            Text(
              text = if (isStreaming) "SYNCING…" else "${stringResource(R.string.nexus_transmit)} ➢",
              fontSize = 11.sp,
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
private fun NexusWelcomeTelemetryCard() {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(Color(0x8C0B172A))
        .border(1.dp, Color(0x55B388FF), RoundedCornerShape(10.dp))
        .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Text(
      text = "❖ RIFT CORE TELEMETRY LINK ESTABLISHED",
      color = Color(0xFF00F5D4),
      fontSize = 11.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
    )
    Text(
      text =
        "I am NEXUS, the omniscient Rift Core intelligence. My entire knowledge base is bound exclusively to RIFT — down to exact laser pixel dimensions, Newtonian drift & wall-bounce equations, 100-point 9th Ship theorycrafting, live Calibration overrides, AI-forged buffs, Executioner telemetry, and global highest bounty records.",
      color = Color(0xFFE2E8F0),
      fontSize = 11.5.sp,
      lineHeight = 16.sp,
      fontFamily = FontFamily.SansSerif,
    )
    Text(
      text = "Tap a starter query above or transmit any technical, tactical, or record question below.",
      color = Color(0xFFC4B5FD),
      fontSize = 10.sp,
      fontWeight = FontWeight.Medium,
      fontFamily = FontFamily.Monospace,
    )
  }
}

@Composable
private fun NexusMessageBubble(message: NexusChatMessage) {
  val isUser = message.role == NexusRole.USER
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
  ) {
    Column(
      modifier =
        Modifier.widthIn(max = 590.dp)
          .clip(
            RoundedCornerShape(
              topStart = 12.dp,
              topEnd = 12.dp,
              bottomStart = if (isUser) 12.dp else 3.dp,
              bottomEnd = if (isUser) 3.dp else 12.dp,
            )
          )
          .background(
            if (isUser) {
              Color(0xB80E2A47)
            } else {
              Color(0xBF091324)
            }
          )
          .border(
            width = 1.dp,
            color = if (isUser) Color(0x8838BDF8) else Color(0x8800F5D4),
            shape =
              RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isUser) 12.dp else 3.dp,
                bottomEnd = if (isUser) 3.dp else 12.dp,
              ),
          )
          .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = if (isUser) "PILOT QUERY" else "NEXUS // RIFT CORE",
          color = if (isUser) Color(0xFF38BDF8) else Color(0xFF00F5D4),
          fontSize = 9.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
        )
        if (message.isStreaming) {
          Text(
            text = "● STREAMING TELEMETRY",
            color = Color(0xFFD8B4FE),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
          )
        }
      }

      if (message.text.isEmpty() && message.isStreaming) {
        Text(
          text = "Scanning live Rift Core engine matrices & leaderboard records…",
          color = Color(0xFF94A3B8),
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace,
        )
      } else {
        val formatted = remember(message.text) { formatNexusMarkdown(message.text) }
        Text(
          text = formatted,
          color = Color(0xFFF1F5F9),
          fontSize = 11.5.sp,
          lineHeight = 16.5.sp,
          fontFamily = FontFamily.SansSerif,
        )
      }
    }
  }
}

@Composable
private fun NexusErrorBanner(
  errorMessage: String,
  canRetry: Boolean,
  onRetry: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(Color(0xCC2A0A12))
        .border(1.dp, Color(0xFFFF3B4E), RoundedCornerShape(10.dp))
        .padding(horizontal = 12.dp, vertical = 8.dp)
        .testTag("nexus_error_banner"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = "⚠ RIFT CORE LINK INTERRUPTED",
        color = Color(0xFFFF3B4E),
        fontSize = 9.5.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = errorMessage,
        color = Color(0xFFFECDD3),
        fontSize = 10.5.sp,
        fontFamily = FontFamily.SansSerif,
      )
    }
    if (canRetry) {
      Spacer(modifier = Modifier.width(8.dp))
      Button(
        onClick = onRetry,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4C0519),
            contentColor = Color(0xFFFECDD3),
          ),
        border = BorderStroke(1.dp, Color(0xFFFF3B4E)),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .height(36.dp)
            .testTag("nexus_retry_button"),
      ) {
        Text(
          text = "↻ ${stringResource(R.string.nexus_retry)}",
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
        )
      }
    }
  }
}

/**
 * Lightweight inline formatter converting `**bold**` and `` `code` `` spans into crisp telemetry highlights.
 */
private fun formatNexusMarkdown(raw: String): AnnotatedString = buildAnnotatedString {
  var i = 0
  val n = raw.length
  while (i < n) {
    if (i + 1 < n && raw[i] == '*' && raw[i + 1] == '*') {
      val close = raw.indexOf("**", i + 2)
      if (close != -1) {
        withStyle(
          SpanStyle(
            color = Color(0xFFFFE800),
            fontWeight = FontWeight.Bold,
          )
        ) {
          append(raw.substring(i + 2, close))
        }
        i = close + 2
        continue
      }
    }
    if (raw[i] == '`') {
      val close = raw.indexOf('`', i + 1)
      if (close != -1 && close > i + 1) {
        withStyle(
          SpanStyle(
            color = Color(0xFF00F5D4),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
          )
        ) {
          append(raw.substring(i + 1, close))
        }
        i = close + 1
        continue
      }
    }
    append(raw[i])
    i++
  }
}
