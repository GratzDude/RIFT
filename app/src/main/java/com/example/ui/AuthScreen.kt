package com.example.ui

import android.content.Context
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlin.math.PI
import kotlin.math.cos
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

  val abyssTransition = rememberInfiniteTransition(label = "auth_abyssal_void")
  val driftPhase by
    abyssTransition.animateFloat(
      initialValue = 0f,
      targetValue = (2.0 * PI).toFloat(),
      animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
      label = "auth_drift_phase",
    )

  LaunchedEffect(Unit) {
    if (Firebase.auth.currentUser != null) {
      onAuthSuccess()
    }
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xFF02050B))
        .windowInsetsPadding(WindowInsets.safeDrawing),
    contentAlignment = Alignment.Center,
  ) {
    // Abyssal void atmosphere: angled volumetric starbeams, dual-sine Rift caustics & bioluminescent dust
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      if (w <= 1f || h <= 1f) return@Canvas

      drawRect(
        brush =
          Brush.radialGradient(
            colors = listOf(Color(0xFF09162B), Color(0xFF040914), Color(0xFF02050B)),
            center = Offset(w * 0.5f, h * 0.42f),
            radius = maxOf(w, h) * 0.78f,
          )
      )

      val beamSpecs =
        listOf(
          Triple(0.18f, 0.52f, Color(0x1600F5D4)),
          Triple(0.50f, 0.76f, Color(0x1438BDF8)),
          Triple(0.82f, 0.44f, Color(0x166366F1)),
        )
      beamSpecs.forEachIndexed { idx, (topXFrac, botXFrac, beamColor) ->
        val sway = sin(driftPhase + idx * 1.7f) * w * 0.025f
        val topX = w * topXFrac + sway
        val botX = w * botXFrac - sway * 0.6f
        val halfTop = w * 0.055f
        val halfBot = w * 0.14f
        val beamPath =
          Path().apply {
            moveTo(topX - halfTop, 0f)
            lineTo(topX + halfTop, 0f)
            lineTo(botX + halfBot, h)
            lineTo(botX - halfBot, h)
            close()
          }
        drawPath(
          path = beamPath,
          brush =
            Brush.verticalGradient(
              colors = listOf(beamColor, beamColor.copy(alpha = 0.03f), Color.Transparent),
              startY = 0f,
              endY = h,
            ),
        )
      }

      for (band in 0 until 2) {
        val causticPath = Path()
        val steps = 36
        for (s in 0..steps) {
          val t = s.toFloat() / steps.toFloat()
          val px = t * w
          val py =
            h * (0.28f + band * 0.42f) +
              sin(t * 5.2f + driftPhase + band * 1.4f) * (h * 0.045f) +
              cos(t * 9.0f - driftPhase * 1.3f) * (h * 0.018f)
          if (s == 0) causticPath.moveTo(px, py) else causticPath.lineTo(px, py)
        }
        drawPath(
          path = causticPath,
          color = if (band == 0) Color(0x1E00F5D4) else Color(0x1CA855F7),
          style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
        )
      }

      for (i in 0 until 34) {
        val seed = (i * 73 + 19) % 97
        val baseX = (seed / 97f) * w
        val baseY = (((i * 41 + 11) % 89) / 89f) * h
        val dx = sin(driftPhase + i * 0.65f) * 14.dp.toPx()
        val dy = cos(driftPhase * 0.85f + i * 0.9f) * 10.dp.toPx()
        val alpha = 0.20f + 0.35f * (0.5f + 0.5f * sin(driftPhase * 2f + i))
        val moteCol =
          when (i % 3) {
            0 -> Color(0xFF00F5D4).copy(alpha = alpha)
            1 -> Color(0xFF38BDF8).copy(alpha = alpha)
            else -> Color(0xFFA855F7).copy(alpha = alpha)
          }
        drawCircle(
          color = moteCol,
          radius = (1.2f + (i % 3) * 0.85f).dp.toPx(),
          center = Offset((baseX + dx).coerceIn(0f, w), (baseY + dy).coerceIn(0f, h)),
        )
      }
    }

    Surface(
      modifier =
        Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
          .widthIn(max = 580.dp)
          .fillMaxWidth()
          .border(
            1.5.dp,
            Brush.linearGradient(listOf(Color(0xFF00F5D4), Color(0xFFA855F7), Color(0xFF38BDF8))),
            RoundedCornerShape(16.dp),
          ),
      color = Color(0xF2060D1A),
      shape = RoundedCornerShape(16.dp),
      tonalElevation = 10.dp,
    ) {
      Box(modifier = Modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.matchParentSize()) {
          val stroke = 2.0.dp.toPx()
          val inset = 6.dp.toPx()
          val arm = 22.dp.toPx()
          val cyan = Color(0xFF00F5D4)
          val gold = Color(0xFFFACC15)
          drawLine(cyan, Offset(inset, inset), Offset(inset + arm, inset), stroke)
          drawLine(cyan, Offset(inset, inset), Offset(inset, inset + arm), stroke)
          drawLine(gold, Offset(size.width - inset - arm, inset), Offset(size.width - inset, inset), stroke)
          drawLine(gold, Offset(size.width - inset, inset), Offset(size.width - inset, inset + arm), stroke)
          drawLine(gold, Offset(inset, size.height - inset - arm), Offset(inset, size.height - inset), stroke)
          drawLine(gold, Offset(inset, size.height - inset), Offset(inset + arm, size.height - inset), stroke)
          drawLine(cyan, Offset(size.width - inset - arm, size.height - inset), Offset(size.width - inset, size.height - inset), stroke)
          drawLine(cyan, Offset(size.width - inset, size.height - inset - arm), Offset(size.width - inset, size.height - inset), stroke)
        }

        Column(
          modifier =
            Modifier.verticalScroll(scrollState)
              .padding(horizontal = 24.dp, vertical = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.weight(1f),
            ) {
              Box(
                modifier =
                  Modifier.size(44.dp)
                    .background(Color(0xFF030812), RoundedCornerShape(22.dp))
                    .border(1.2.dp, Color(0xFF00F5D4), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center,
              ) {
                SwirlingRiftVortexGlyph(
                  modifier = Modifier.size(38.dp),
                  isStreaming = true,
                )
              }
              Column {
                Text(
                  text = stringResource(R.string.auth_title),
                  color = Color(0xFF00F5D4),
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Black,
                  fontFamily = FontFamily.Monospace,
                )
                Text(
                  text = stringResource(R.string.auth_subtitle),
                  color = Color(0xFFFACC15),
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }

            Button(
              onClick = onOpenChangelog,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF0B1C30),
                  contentColor = Color(0xFF38BDF8),
                ),
              border = BorderStroke(1.dp, Color(0xFF38BDF8)),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.height(34.dp).testTag("auth_changelog_button"),
            ) {
              Icon(
                imageVector = RiftAuthIcons.Terminal,
                contentDescription = "Version Log",
                tint = Color(0xFF00F5D4),
                modifier = Modifier.size(14.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "BUILD #$CURRENT_RIFT_BUILD_VERSION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Column(
            modifier =
              Modifier.fillMaxWidth()
                .background(Color(0xB809172A), RoundedCornerShape(10.dp))
                .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FeatureBullet(icon = RiftAuthIcons.Rocket, text = stringResource(R.string.auth_feature_physics))
            FeatureBullet(icon = RiftAuthIcons.Prism, text = stringResource(R.string.auth_feature_ships))
            FeatureBullet(icon = RiftAuthIcons.Globe, text = stringResource(R.string.auth_feature_cloud))
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "NEXUS CORE READY • SELECT YOUR PILOT LINK TO ENTER SECTOR ORBIT",
            color = Color(0xFF94A3B8),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
          )

          if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = errorMessage ?: "",
              color = Color(0xFFFF3B4E),
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace,
              textAlign = TextAlign.Center,
              modifier = Modifier.testTag("auth_error_text"),
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Button(
            onClick = {
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
            enabled = !isLoading,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00F5D4),
                contentColor = Color(0xFF030A14),
              ),
            shape = RoundedCornerShape(10.dp),
            modifier =
              Modifier.fillMaxWidth()
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
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
              )
            } else {
              Text(
                text = stringResource(R.string.sign_in_with_google),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Button(
            onClick = onPlayAsGuest,
            enabled = !isLoading,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0F1E34),
                contentColor = Color(0xFFFACC15),
              ),
            border = BorderStroke(1.5.dp, Color(0xFFFACC15)),
            shape = RoundedCornerShape(10.dp),
            modifier =
              Modifier.fillMaxWidth()
                .height(48.dp)
                .testTag("skip_sign_in_guest_button"),
          ) {
            Icon(
              imageVector = RiftAuthIcons.ChevronPrism,
              contentDescription = "Enter as RFT Drifter",
              tint = Color(0xFFFACC15),
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "ENTER AS [RFT] DRIFTER (GUEST)",
              fontSize = 13.sp,
              fontWeight = FontWeight.ExtraBold,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FeatureBullet(
  icon: ImageVector,
  text: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Start,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = text,
      tint = Color(0xFF00F5D4),
      modifier = Modifier.size(18.dp),
    )
    Spacer(modifier = Modifier.width(10.dp))
    Text(
      text = text,
      color = Color(0xFFD6E4F0),
      fontSize = 13.sp,
      fontFamily = FontFamily.Monospace,
    )
  }
}

private object RiftAuthIcons {
  val Rocket: ImageVector by lazy {
    ImageVector.Builder(
        name = "RiftRocket",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
      )
      .apply {
        path(fill = SolidColor(Color.White)) {
          moveTo(12f, 2.5f)
          lineTo(16.5f, 9f)
          lineTo(15f, 16.5f)
          lineTo(18.5f, 19.5f)
          lineTo(14f, 19.5f)
          lineTo(12f, 22f)
          lineTo(10f, 19.5f)
          lineTo(5.5f, 19.5f)
          lineTo(9f, 16.5f)
          lineTo(7.5f, 9f)
          close()
        }
      }
      .build()
  }

  val Prism: ImageVector by lazy {
    ImageVector.Builder(
        name = "RiftFluxPrism",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
      )
      .apply {
        path(fill = SolidColor(Color.White)) {
          moveTo(12f, 2f)
          lineTo(20f, 12f)
          lineTo(12f, 22f)
          lineTo(4f, 12f)
          close()
        }
      }
      .build()
  }

  val ChevronPrism: ImageVector by lazy {
    ImageVector.Builder(
        name = "RiftChevronPrism",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
      )
      .apply {
        path(fill = SolidColor(Color.White)) {
          moveTo(5f, 5f)
          lineTo(13f, 12f)
          lineTo(5f, 19f)
          lineTo(8.2f, 19f)
          lineTo(16.2f, 12f)
          lineTo(8.2f, 5f)
          close()
          moveTo(15.5f, 8.5f)
          lineTo(19.5f, 12f)
          lineTo(15.5f, 15.5f)
          lineTo(13.5f, 12f)
          close()
        }
      }
      .build()
  }

  val Terminal: ImageVector by lazy {
    ImageVector.Builder(
        name = "RiftTerminalLog",
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

  val Globe: ImageVector by lazy {
    ImageVector.Builder(
        name = "RiftGlobe",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
      )
      .apply {
        path(fill = SolidColor(Color.White)) {
          moveTo(12f, 2f)
          curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
          curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
          curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
          curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
          close()
          moveTo(11f, 19.93f)
          curveTo(7.05f, 19.44f, 4f, 16.08f, 4f, 12f)
          curveTo(4f, 11.38f, 4.08f, 10.79f, 4.21f, 10.21f)
          lineTo(9f, 15f)
          verticalLineTo(16f)
          curveTo(9f, 17.1f, 9.9f, 18f, 11f, 18f)
          verticalLineTo(19.93f)
          close()
          moveTo(17.9f, 17.39f)
          curveTo(17.64f, 16.58f, 16.9f, 16f, 16f, 16f)
          horizontalLineTo(15f)
          verticalLineTo(13f)
          curveTo(15f, 12.45f, 14.55f, 12f, 14f, 12f)
          horizontalLineTo(8f)
          verticalLineTo(10f)
          horizontalLineTo(10f)
          curveTo(10.55f, 10f, 11f, 9.55f, 11f, 9f)
          verticalLineTo(7f)
          horizontalLineTo(13f)
          curveTo(14.1f, 7f, 15f, 6.1f, 15f, 5f)
          verticalLineTo(4.59f)
          curveTo(17.93f, 5.78f, 20f, 8.65f, 20f, 12f)
          curveTo(20f, 14.08f, 19.2f, 15.97f, 17.9f, 17.39f)
          close()
        }
      }
      .build()
  }
}
