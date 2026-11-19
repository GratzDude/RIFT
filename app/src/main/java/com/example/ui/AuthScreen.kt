package com.example.ui

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.NetworkConnectivityMonitor
import com.example.engine.RiftSpriteBaker
import com.example.engine.RiftVisualForgeState
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope,
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  if (!NetworkConnectivityMonitor.isCurrentlyOnline(context)) {
    onUnauthenticated()
    return
  }
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      onUnauthenticated()
      return
    }

  val googleIdOption =
    GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()

  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (e: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {},
) {
  if (!NetworkConnectivityMonitor.isCurrentlyOnline(context)) {
    if (Firebase.auth.currentUser != null) {
      onAuthSuccess()
    } else {
      onAuthError(
        "Device is currently offline. Tap Play as Drifter below for instant offline play, or connect to the internet to sign in with Google."
      )
    }
    return
  }
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
      return
    }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onAuthError("Unexpected credential type")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Sign in failed")
    }
  }
}

fun signOut(
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSignOutComplete: () -> Unit = {},
) {
  scope.launch {
    withContext(NonCancellable) {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w("Auth", "Failed to clear credential state: ${e.message}")
      } finally {
        Firebase.auth.signOut()
        onSignOutComplete()
      }
    }
  }
}

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
  onPlayAsGuest: () -> Unit = {},
  onOpenChangelog: () -> Unit = {},
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val scrollState = rememberScrollState()
  val isOnline by
    remember(context) { NetworkConnectivityMonitor.observeOnlineState(context) }
      .collectAsState(initial = NetworkConnectivityMonitor.isCurrentlyOnline(context))

  LaunchedEffect(Unit) {
    if (Firebase.auth.currentUser != null) {
      onAuthSuccess()
    }
  }

  Box(
    modifier = Modifier.fillMaxSize().background(Color(0xFF02050B)),
    contentAlignment = Alignment.Center,
  ) {
    // Deep #02050B starlight backdrop + subtle tactical nebula glow
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      drawRect(
        brush =
          Brush.radialGradient(
            colors = listOf(Color(0xFF08162E), Color(0xFF040A16), Color(0xFF02050B)),
            center = Offset(w * 0.34f, h * 0.50f),
            radius = maxOf(w, h) * 0.80f,
          )
      )
      val starCount = 46
      for (i in 0 until starCount) {
        val hx = ((i * 73856093) xor 0x5A17) and 0xFFFF
        val hy = ((i * 19349663) xor 0x2F91) and 0xFFFF
        val sx = (hx / 65535f) * w
        val sy = (hy / 65535f) * h
        val starCol =
          when {
            i % 7 == 0 -> Color(0xFF00F5D4).copy(alpha = 0.42f)
            i % 5 == 0 -> Color(0xFFFACC15).copy(alpha = 0.36f)
            else -> Color(0xFFCBD5E1).copy(alpha = 0.28f)
          }
        val r = if (i % 6 == 0) 1.6.dp.toPx() else 1.0.dp.toPx()
        drawCircle(color = starCol, radius = r, center = Offset(sx, sy))
      }
    }

    BoxWithConstraints(
      modifier =
        Modifier.fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(horizontal = 14.dp, vertical = 8.dp),
      contentAlignment = Alignment.Center,
    ) {
      val viewportMaxHeight = maxHeight
      val aspect = if (viewportMaxHeight.value > 0f) maxWidth.value / viewportMaxHeight.value else 1.6f
      val isWideLandscape = maxWidth >= 560.dp && aspect >= 1.15f
      val isUltraCompactHeight = viewportMaxHeight < 410.dp
      val cardMaxWidth = if (maxWidth >= 1000.dp) 980.dp else 940.dp

      Surface(
        modifier =
          Modifier.widthIn(max = cardMaxWidth)
            .fillMaxWidth()
            .then(
              if (isWideLandscape && !isUltraCompactHeight) {
                Modifier.heightIn(max = (maxHeight * 0.94f).coerceAtMost(540.dp))
              } else {
                Modifier.heightIn(max = maxHeight * 0.96f)
              }
            )
            .border(
              1.2.dp,
              Brush.linearGradient(
                listOf(Color(0xFF00F5D4), Color(0xFF38BDF8), Color(0xFFFACC15))
              ),
              RoundedCornerShape(14.dp),
            ),
        color = Color(0xFF060D1A),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 10.dp,
      ) {
        Box(modifier = Modifier.fillMaxWidth()) {
          // Sci-Fi Command Tablet chamfer/corner registration brackets
          Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = 1.8.dp.toPx()
            val inset = 5.dp.toPx()
            val arm = 20.dp.toPx()
            val cyan = Color(0xFF00F5D4)
            val gold = Color(0xFFFACC15)
            drawLine(cyan, Offset(inset, inset), Offset(inset + arm, inset), stroke)
            drawLine(cyan, Offset(inset, inset), Offset(inset, inset + arm), stroke)
            drawLine(gold, Offset(size.width - inset - arm, inset), Offset(size.width - inset, inset), stroke)
            drawLine(gold, Offset(size.width - inset, inset), Offset(size.width - inset, inset + arm), stroke)
            drawLine(gold, Offset(inset, size.height - inset - arm), Offset(inset, size.height - inset), stroke)
            drawLine(gold, Offset(inset, size.height - inset), Offset(inset + arm, size.height - inset), stroke)
            drawLine(
              cyan,
              Offset(size.width - inset - arm, size.height - inset),
              Offset(size.width - inset, size.height - inset),
              stroke,
            )
            drawLine(
              cyan,
              Offset(size.width - inset, size.height - inset - arm),
              Offset(size.width - inset, size.height - inset),
              stroke,
            )
          }

          val bodyModifier =
            if (!isWideLandscape || isUltraCompactHeight) {
              Modifier.fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
            } else {
              Modifier.fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp)
            }

          Column(
            modifier = bodyModifier,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            // TOP TABLET HEADER STRIP
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(Color(0xFF081326))
                  .border(1.dp, Color(0x3D38BDF8), RoundedCornerShape(10.dp))
                  .padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
              ) {
                NexusMergedBrandEmblem(modifier = Modifier.size(28.dp))
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Text(
                    text = stringResource(R.string.auth_title),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.0.sp,
                    maxLines = 1,
                  )
                  Text(
                    text = "// THRESHOLD DYNAMICS STUDIO, LLC",
                    color = Color(0xFFFACC15),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }

              Spacer(modifier = Modifier.width(8.dp))

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                val netColor = if (isOnline) Color(0xFF10B981) else Color(0xFFF59E0B)
                val netBg = if (isOnline) Color(0x2610B981) else Color(0x2EF59E0B)
                Row(
                  modifier =
                    Modifier.clip(RoundedCornerShape(8.dp))
                      .background(netBg)
                      .border(1.dp, netColor, RoundedCornerShape(8.dp))
                      .padding(horizontal = 8.dp, vertical = 5.dp)
                      .testTag("auth_network_status_badge"),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                  Canvas(modifier = Modifier.size(7.dp)) {
                    drawCircle(color = netColor)
                  }
                  Text(
                    text = if (isOnline) "ONLINE" else "OFFLINE",
                    color = netColor,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                  )
                }

                Button(
                  onClick = onOpenChangelog,
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = Color(0xFF0B1C32),
                      contentColor = Color(0xFF00F5D4),
                    ),
                  border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 11.dp, vertical = 4.dp),
                  modifier =
                    Modifier.minimumInteractiveComponentSize()
                      .heightIn(min = 36.dp)
                      .testTag("auth_changelog_button"),
                ) {
                  Icon(
                    imageVector = RiftAuthIcons.Terminal,
                    contentDescription = "Patch Log",
                    tint = Color(0xFF00F5D4),
                    modifier = Modifier.size(14.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "[ BUILD #$CURRENT_RIFT_BUILD_VERSION PATCH LOG ]",
                    fontSize = 9.8.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                  )
                }
              }
            }

            // 2-COLUMN SCI-FI COMMAND TABLET DECK (Left: Live Safe-Harbor Showcase | Right: Briefing & Auth)
            if (isWideLandscape) {
              val showcaseMaxH = (viewportMaxHeight * 0.66f).coerceIn(185.dp, 310.dp)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                AuthSafeHarborRadarShowcase(
                  modifier =
                    Modifier.weight(0.46f)
                      .heightIn(min = 180.dp, max = showcaseMaxH)
                      .fillMaxWidth()
                )
                AuthCommandBriefingColumn(
                  modifier =
                    Modifier.weight(0.54f)
                      .then(if (isUltraCompactHeight) Modifier else Modifier.verticalScroll(scrollState)),
                  isOnline = isOnline,
                  isLoading = isLoading,
                  errorMessage = errorMessage,
                  onSignInGoogle = {
                    errorMessage = null
                    isLoading = true
                    onGoogleSignInClicked(
                      context = context,
                      credentialManager = credentialManager,
                      onAuthSuccess = {
                        isLoading = false
                        onAuthSuccess()
                      },
                      onAuthError = { msg ->
                        isLoading = false
                        errorMessage = msg
                      },
                      scope = coroutineScope,
                      onAuthCancelled = { isLoading = false },
                    )
                  },
                  onEnterDrifter = onPlayAsGuest,
                )
              }
            } else {
              val portraitShowcaseH = (viewportMaxHeight * 0.28f).coerceIn(160.dp, 225.dp)
              AuthSafeHarborRadarShowcase(
                modifier =
                  Modifier.fillMaxWidth()
                    .height(portraitShowcaseH)
              )
              AuthCommandBriefingColumn(
                modifier = Modifier.fillMaxWidth(),
                isOnline = isOnline,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onSignInGoogle = {
                  errorMessage = null
                  isLoading = true
                  onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {
                      isLoading = false
                      onAuthSuccess()
                    },
                    onAuthError = { msg ->
                      isLoading = false
                      errorMessage = msg
                    },
                    scope = coroutineScope,
                    onAuthCancelled = { isLoading = false },
                  )
                },
                onEnterDrifter = onPlayAsGuest,
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Left Column (`weight(0.46f)`): Live Gameplay-Honest Circular Safe-Harbor Radar Showcase.
 * Renders a round rippling cyan/gold digitized aperture framing the real `5x` `Nexus Station`
 * sprite (`RiftSpriteBaker.safeZoneStationBitmap` + `drawSafeZoneStationHoloRing`) and
 * 3 real `128x128` ship sprites (`getShipSprite`) banking along orbital patrol arcs with thruster trails.
 */
@Composable
private fun AuthSafeHarborRadarShowcase(modifier: Modifier = Modifier) {
  val headless = remember { isHeadlessGlEnvironment() }
  val visualRev = RiftVisualForgeState.visualRevision
  val customVer = RiftSpriteBaker.customShip9Version

  val orbitPhaseRad =
    if (headless) {
      0.85f
    } else {
      val transition = rememberInfiniteTransition(label = "auth_safe_harbor_showcase")
      val p by
        transition.animateFloat(
          initialValue = 0f,
          targetValue = (2.0 * PI).toFloat(),
          animationSpec = infiniteRepeatable(tween(9600, easing = LinearEasing), RepeatMode.Restart),
          label = "auth_orbit_rad",
        )
      p
    }

  val stationBmp = remember(visualRev) { RiftSpriteBaker.safeZoneStationBitmap }
  val patrolShip1 = remember(customVer, visualRev) { RiftSpriteBaker.getShipSprite(1, isAllied = true, isHitFlashing = false) }
  val patrolShip2 = remember(customVer, visualRev) { RiftSpriteBaker.getShipSprite(5, isAllied = true, isHitFlashing = false) }
  val patrolShip3 = remember(customVer, visualRev) { RiftSpriteBaker.getShipSprite(9, isAllied = false, isHitFlashing = false) }
  val shipMatrix = remember { Matrix() }
  val stationRect = remember { RectF() }

  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF040A16))
        .border(1.dp, Color(0x3838BDF8), RoundedCornerShape(12.dp))
        .padding(6.dp),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true)) {
      val w = size.width
      val h = size.height
      if (w <= 8f || h <= 8f) return@Canvas
      val cx = w * 0.5f
      val cy = h * 0.5f
      val center = Offset(cx, cy)
      val apertureR = (min(w, h) * 0.41f).coerceAtLeast(36f)
      val timeSec = (orbitPhaseRad / (2f * PI.toFloat())) * 9.6f

      // 1. Deep Safe-Harbor abyssal space inside the circular Overwatch aperture
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(Color(0xFF0A1D3B), Color(0xFF050E20), Color(0xFF020611)),
            center = center,
            radius = apertureR,
          ),
        radius = apertureR,
        center = center,
      )

      // Subtle tactical polar radar rings & crosshairs
      val gridColor = Color(0xFF38BDF8).copy(alpha = 0.14f)
      drawCircle(gridColor, radius = apertureR * 0.42f, center = center, style = Stroke(width = 1.dp.toPx()))
      drawCircle(gridColor, radius = apertureR * 0.72f, center = center, style = Stroke(width = 1.dp.toPx()))
      drawLine(gridColor, Offset(cx - apertureR * 0.92f, cy), Offset(cx + apertureR * 0.92f, cy), strokeWidth = 0.9.dp.toPx())
      drawLine(gridColor, Offset(cx, cy - apertureR * 0.92f), Offset(cx, cy + apertureR * 0.92f), strokeWidth = 0.9.dp.toPx())

      // 8 Radial Cyan/Gold Safe-Zone Stabilizer Beams bracing the Safe Harbor perimeter
      val safeShieldR = apertureR * 0.86f
      for (b in 0 until 8) {
        val bAng = (b * PI * 0.25).toFloat() + sin(orbitPhaseRad) * 0.02f
        val innerPt = Offset(cx + cos(bAng) * (apertureR * 0.22f), cy + sin(bAng) * (apertureR * 0.22f))
        val outerPt = Offset(cx + cos(bAng) * safeShieldR, cy + sin(bAng) * safeShieldR)
        drawLine(
          brush =
            Brush.linearGradient(
              listOf(Color(0x99FACC15), Color(0xAA00F5D4), Color(0x2238BDF8)),
              start = innerPt,
              end = outerPt,
            ),
          start = innerPt,
          end = outerPt,
          strokeWidth = 1.6.dp.toPx(),
          cap = StrokeCap.Round,
        )
      }
      drawCircle(
        color = Color(0xFF00F5D4).copy(alpha = 0.55f),
        radius = safeShieldR,
        center = center,
        style = Stroke(width = 1.6.dp.toPx()),
      )

      // 2. Real 5x Nexus Station Sprite + Revolving Ops Holo-Ring + 3 Real 128x128 Patrol Ships
      val stationR = apertureR * 0.34f
      val patrolSpecs =
        arrayOf(
          Triple(patrolShip1, apertureR * 0.58f, orbitPhaseRad),
          Triple(patrolShip2, apertureR * 0.73f, -orbitPhaseRad * 1.0f + 2.15f),
          Triple(patrolShip3, apertureR * 0.66f, orbitPhaseRad * 1.0f + 4.20f),
        )

      // Draw curved cyan/gold thruster wakes first (under hulls)
      patrolSpecs.forEachIndexed { idx, (_, orbitR, angRad) ->
        val dirSign = if (idx == 1) -1f else 1f
        val trailColor = if (idx == 2) Color(0xFFFACC15) else Color(0xFF00F5D4)
        val segments = 8
        for (s in 0 until segments) {
          val a0 = angRad - dirSign * (s * 0.065f)
          val a1 = angRad - dirSign * ((s + 1) * 0.065f)
          val frac = 1f - (s / segments.toFloat())
          drawLine(
            color = trailColor.copy(alpha = 0.58f * frac),
            start = Offset(cx + cos(a0) * orbitR, cy + sin(a0) * orbitR),
            end = Offset(cx + cos(a1) * orbitR, cy + sin(a1) * orbitR),
            strokeWidth = (3.4f * frac).dp.toPx(),
            cap = StrokeCap.Round,
          )
        }
      }

      drawIntoCanvas { composeCanvas ->
        val nc = composeCanvas.nativeCanvas
        // Center 5x Nexus Station
        nc.save()
        nc.translate(cx, cy)
        nc.rotate((orbitPhaseRad * 180f / PI.toFloat()) * 0.25f)
        stationRect.set(-stationR, -stationR, stationR, stationR)
        nc.drawBitmap(stationBmp, null, stationRect, null)
        nc.restore()

        // Shrunk 3D-tilted revolving cyan/gold Ops Holo-Ring
        RiftSpriteBaker.drawSafeZoneStationHoloRing(
          c = nc,
          cx = cx,
          cy = cy,
          stationRadius = stationR,
          timeSec = timeSec,
          isLockedOut = false,
          alphaScale = 1f,
        )

        // 3 Real 128x128 Ship Sprites banking along orbital patrol arcs
        val shipRenderSize = (apertureR * 0.22f).coerceAtLeast(18f)
        patrolSpecs.forEachIndexed { idx, (bmp, orbitR, angRad) ->
          val dirSign = if (idx == 1) -1f else 1f
          val sx = cx + cos(angRad) * orbitR
          val sy = cy + sin(angRad) * orbitR
          // Tangent heading (+ slight inward bank toward station)
          val headingRad = angRad + dirSign * (PI.toFloat() * 0.5f) - dirSign * 0.12f
          val halfBmp = bmp.width * 0.5f
          val drawScale = shipRenderSize / bmp.width.toFloat()
          shipMatrix.reset()
          shipMatrix.postTranslate(-halfBmp, -halfBmp)
          shipMatrix.postScale(drawScale, drawScale)
          shipMatrix.postRotate(headingRad * 180f / PI.toFloat())
          shipMatrix.postTranslate(sx, sy)
          nc.drawBitmap(bmp, shipMatrix, null)
        }
      }

      // 3. Mask #060D1A tablet glass outside aperture & draw Round Rippling Polar Digitization Rings
      val outerMask =
        Path().apply {
          fillType = PathFillType.EvenOdd
          addRect(Rect(0f, 0f, w, h))
          addOval(Rect(cx - apertureR, cy - apertureR, cx + apertureR, cy + apertureR))
        }
      drawPath(outerMask, color = Color(0xFF060D1A))

      val numBands = 6
      val numSegs = 40
      val segSweepDeg = 360f / numSegs.toFloat()
      val segGapDeg = 1.5f
      val borderSw = 0.9.dp.toPx()

      for (band in 0 until numBands) {
        val outerFrac = 1.20f - band * 0.038f
        val innerFrac = outerFrac - 0.032f
        val rMid = apertureR * ((outerFrac + innerFrac) * 0.5f)
        val bandThickness = (apertureR * (outerFrac - innerFrac)).coerceAtLeast(1.4f)
        val arcRect = Rect(cx - rMid, cy - rMid, cx + rMid, cy + rMid)
        val keepThreshold = 1.0f - band * 0.16f

        for (seg in 0 until numSegs) {
          val hash = ((band * 73856093) xor (seg * 19349663)) and 0xFF
          val normHash = hash / 255f
          val segAng = ((seg + 0.5f) / numSegs.toFloat()) * (2f * PI.toFloat())
          val inwardWave = 0.5f + 0.5f * sin(band * 0.95f - orbitPhaseRad * 3.0f + sin(segAng * 3f) * 0.5f)
          if (normHash > keepThreshold + inwardWave * 0.12f) continue

          val startDeg = seg * segSweepDeg + segGapDeg * 0.5f
          val sweepDeg = (segSweepDeg - segGapDeg).coerceAtLeast(1.2f)
          val cyanLit = (0.5f + 0.5f * cos(segAng - orbitPhaseRad)).pow(2.0f)
          val goldLit = (0.5f + 0.5f * cos(segAng + orbitPhaseRad * 0.7f - 1.8f)).pow(2.4f)
          val litAlpha = ((0.18f + 0.52f * cyanLit + 0.38f * goldLit) * inwardWave).coerceIn(0.08f, 0.82f)
          val cellColor = if (goldLit > cyanLit * 1.06f) Color(0xFFFACC15) else Color(0xFF00F5D4)

          drawArc(
            color = cellColor.copy(alpha = litAlpha * (1f - band * 0.10f).coerceAtLeast(0.25f)),
            startAngle = startDeg,
            sweepAngle = sweepDeg,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = Stroke(width = if (band >= 4) borderSw * 1.2f else borderSw, cap = StrokeCap.Butt),
          )
        }
      }

      rotate(degrees = (orbitPhaseRad * 180f / PI.toFloat())) {
        drawCircle(
          brush =
            Brush.sweepGradient(
              0.00f to Color(0xFF00F5D4),
              0.25f to Color(0xFF38BDF8).copy(alpha = 0.48f),
              0.50f to Color(0xFFFACC15),
              0.75f to Color(0xFF1E293B).copy(alpha = 0.50f),
              1.00f to Color(0xFF00F5D4),
              center = center,
            ),
          radius = apertureR,
          center = center,
          style = Stroke(width = 1.7.dp.toPx()),
        )
      }
    }

    // Compact live telemetry label pinned at bottom of showcase
    Box(
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .padding(bottom = 4.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(Color(0xE6060D1A))
          .border(0.9.dp, Color(0x8800F5D4), RoundedCornerShape(6.dp))
          .padding(horizontal = 8.dp, vertical = 2.5.dp)
    ) {
      Text(
        text = "NEXUS STATION • LIVE SAFE HARBOR",
        color = Color(0xFF00F5D4),
        fontSize = 8.8.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
      )
    }
  }
}

/**
 * Right Column (`weight(0.54f)`): Plain-English Briefing, Tagline #2 & Auth Actions.
 */
@Composable
private fun AuthCommandBriefingColumn(
  modifier: Modifier = Modifier,
  isOnline: Boolean = true,
  isLoading: Boolean,
  errorMessage: String?,
  onSignInGoogle: () -> Unit,
  onEnterDrifter: () -> Unit,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(7.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(
        text = "Fast-Paced Competitive 2.5D Space Dogfighter",
        color = Color.White,
        fontSize = 14.0.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
      )
      Text(
        text = "Solo • Syndicate • Strike Wing — 6 Competitive Modes + Custom Arenas",
        color = Color(0xFF38BDF8),
        fontSize = 10.2.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
      )
      Text(
        text = stringResource(R.string.auth_tagline_two),
        color = Color(0xFFFACC15),
        fontSize = 10.6.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace,
      )
    }

    if (!isOnline) {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x26F59E0B))
            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
      ) {
        Text(
          text = "OFFLINE MODE READY — Practice Rift, Custom Arenas & Bot Matches work offline.",
          color = Color(0xFFFDE68A),
          fontSize = 9.8.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
        )
      }
    }

    Column(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xD9091528))
          .border(1.dp, Color(0x4738BDF8), RoundedCornerShape(10.dp))
          .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      AuthTelemetryRow(
        iconSlot = { NexusMergedBrandEmblem(modifier = Modifier.size(16.dp)) },
        text = stringResource(R.string.auth_feature_physics),
      )
      AuthTelemetryRow(
        iconSlot = { ResonanceCrestIcon(sizeDp = 16.dp) },
        text = stringResource(R.string.auth_feature_ships),
      )
      AuthTelemetryRow(
        iconSlot = { RiftShardGemIcon(sizeDp = 16.dp) },
        text = stringResource(R.string.auth_feature_cloud),
      )
    }

    if (errorMessage != null) {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x33FF3B4E))
            .border(1.dp, Color(0xFFFF3B4E), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
      ) {
        Text(
          text = errorMessage,
          color = Color(0xFFFF6B7A),
          fontSize = 10.5.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth().testTag("auth_error_text"),
        )
      }
    }

    Button(
      onClick = onSignInGoogle,
      enabled = !isLoading,
      colors =
        ButtonDefaults.buttonColors(
          containerColor = Color(0xFF00F5D4),
          contentColor = Color(0xFF030A14),
        ),
      shape = RoundedCornerShape(10.dp),
      modifier =
        Modifier.fillMaxWidth()
          .minimumInteractiveComponentSize()
          .height(48.dp)
          .testTag("google_sign_in_button"),
    ) {
      if (isLoading) {
        CircularProgressIndicator(
          modifier = Modifier.size(20.dp),
          color = Color(0xFF030A14),
          strokeWidth = 2.5.dp,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = stringResource(R.string.signing_in),
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
        )
      } else {
        Text(
          text = stringResource(R.string.sign_in_with_google),
          fontSize = 12.2.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    Button(
      onClick = onEnterDrifter,
      enabled = !isLoading,
      colors =
        ButtonDefaults.buttonColors(
          containerColor = Color(0xFF0D1D34),
          contentColor = Color(0xFFFACC15),
        ),
      border = BorderStroke(1.4.dp, Color(0xFFFACC15)),
      shape = RoundedCornerShape(10.dp),
      modifier =
        Modifier.fillMaxWidth()
          .minimumInteractiveComponentSize()
          .height(48.dp)
          .testTag("skip_sign_in_guest_button"),
    ) {
      Icon(
        imageVector = RiftAuthIcons.WingChevron,
        contentDescription = "Enter as RFT Drifter",
        tint = Color(0xFFFACC15),
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = stringResource(R.string.enter_as_drifter),
        fontSize = 11.8.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun AuthTelemetryRow(
  iconSlot: @Composable () -> Unit,
  text: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
      iconSlot()
    }
    Text(
      text = text,
      color = Color(0xFFE2E8F0),
      fontSize = 11.0.sp,
      fontWeight = FontWeight.SemiBold,
      fontFamily = FontFamily.Monospace,
      lineHeight = 14.sp,
    )
  }
}

private object RiftAuthIcons {
  val WingChevron: ImageVector by lazy {
    ImageVector.Builder(
        name = "NexusWingChevron",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
      )
      .apply {
        path(fill = SolidColor(Color.White)) {
          moveTo(4.5f, 5f)
          lineTo(12.8f, 12f)
          lineTo(4.5f, 19f)
          lineTo(7.9f, 19f)
          lineTo(16.2f, 12f)
          lineTo(7.9f, 5f)
          close()
          moveTo(13.2f, 5f)
          lineTo(20.5f, 12f)
          lineTo(13.2f, 19f)
          lineTo(16.0f, 19f)
          lineTo(23.0f, 12f)
          lineTo(16.0f, 5f)
          close()
        }
      }
      .build()
  }

  val Terminal: ImageVector by lazy {
    ImageVector.Builder(
        name = "NexusTerminalLog",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
      )
      .apply {
        path(fill = SolidColor(Color.White)) {
          moveTo(4f, 4f)
          horizontalLineTo(20f)
          verticalLineTo(20f)
          horizontalLineTo(4f)
          close()
          moveTo(6f, 7f)
          verticalLineTo(17f)
          horizontalLineTo(18f)
          verticalLineTo(7f)
          close()
          moveTo(8f, 9.5f)
          lineTo(11.5f, 12f)
          lineTo(8f, 14.5f)
          verticalLineTo(12.8f)
          lineTo(9.4f, 12f)
          lineTo(8f, 11.2f)
          close()
          moveTo(12.5f, 13.5f)
          horizontalLineTo(16.5f)
          verticalLineTo(15f)
          horizontalLineTo(12.5f)
          close()
        }
      }
      .build()
  }
}
