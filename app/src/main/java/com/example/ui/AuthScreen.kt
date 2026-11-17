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
import androidx.compose.ui.viewinterop.AndroidView
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
import kotlin.math.abs
import kotlin.math.cos
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

  val tearTransition = rememberInfiniteTransition(label = "nexus_auth_tear_anim")
  val driftPhase by
    tearTransition.animateFloat(
      initialValue = 0f,
      targetValue = (2.0 * PI).toFloat(),
      animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
      label = "auth_tear_phase",
    )
  val cracklePulse by
    tearTransition.animateFloat(
      initialValue = 0.76f,
      targetValue = 1.0f,
      animationSpec = infiniteRepeatable(tween(680, easing = LinearEasing), RepeatMode.Reverse),
      label = "auth_tear_crackle",
    )

  LaunchedEffect(Unit) {
    if (Firebase.auth.currentUser != null) {
      onAuthSuccess()
    }
  }

  Box(
    modifier = Modifier.fillMaxSize().background(Color(0xFF02050B)),
    contentAlignment = Alignment.Center,
  ) {
    if (!isHeadlessGlEnvironment()) {
      AndroidView(
        factory = { ctx ->
          NexusSpaceGlView(ctx).apply {
            isSplashMode = true
            dimFactor = 0.85f
          }
        },
        update = { view ->
          view.isSplashMode = true
          view.dimFactor = 0.85f
        },
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
          brush =
            Brush.radialGradient(
              colors = listOf(Color(0xFF09182E), Color(0xFF040A16), Color(0xFF02050B)),
              center = Offset(size.width * 0.5f, size.height * 0.36f),
              radius = maxOf(size.width, size.height) * 0.82f,
            )
        )
      }
    }

    Box(
      modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        modifier =
          Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .border(
              1.5.dp,
              Brush.linearGradient(listOf(Color(0xFF00F5D4), Color(0xFFA855F7), Color(0xFF38BDF8))),
              RoundedCornerShape(16.dp),
            ),
        color = Color(0xEC050C18),
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
                .padding(horizontal = 22.dp, vertical = 14.dp),
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
                NexusRiftTearBrandEmblem(
                  phaseRad = driftPhase,
                  cracklePulse = cracklePulse,
                  modifier = Modifier.width(68.dp).height(46.dp),
                )
                Column {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Text(
                      text = stringResource(R.string.auth_title),
                      color = Color(0xFF00F5D4),
                      fontSize = 23.sp,
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace,
                      letterSpacing = 2.5.sp,
                    )
                    Text(
                      text = "// ENTER THE NEXUS",
                      color = Color(0xFFC084FC),
                      fontSize = 10.sp,
                      fontWeight = FontWeight.ExtraBold,
                      fontFamily = FontFamily.Monospace,
                    )
                  }
                  Text(
                    text = stringResource(R.string.auth_subtitle),
                    color = Color(0xFFFACC15),
                    fontSize = 10.sp,
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

            Spacer(modifier = Modifier.height(9.dp))

            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .background(Color(0xB8081426), RoundedCornerShape(10.dp))
                  .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(10.dp))
                  .padding(horizontal = 14.dp, vertical = 9.dp),
              verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              FeatureBullet(icon = RiftAuthIcons.Rocket, text = stringResource(R.string.auth_feature_physics))
              FeatureBullet(icon = RiftAuthIcons.VortexRing, text = stringResource(R.string.auth_feature_ships))
              FeatureBullet(icon = RiftAuthIcons.Globe, text = stringResource(R.string.auth_feature_cloud))
            }

            Spacer(modifier = Modifier.height(7.dp))
            Text(
              text = "3D VOLUMETRIC NEXUS STABILIZED • SELECT PILOT LINK TO ENTER",
              color = Color(0xFF94A3B8),
              fontSize = 9.8.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              textAlign = TextAlign.Center,
            )

            if (errorMessage != null) {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = errorMessage ?: "",
                color = Color(0xFFFF3B4E),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("auth_error_text"),
              )
            }

            Spacer(modifier = Modifier.height(9.dp))

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
                  fontSize = 13.sp,
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.Monospace,
                )
              }
            }

            Spacer(modifier = Modifier.height(7.dp))

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
                imageVector = RiftAuthIcons.WingChevron,
                contentDescription = "Enter as RFT Drifter",
                tint = Color(0xFFFACC15),
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "ENTER AS [RFT] DRIFTER (GUEST)",
                fontSize = 12.8.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
              )
            }

            Spacer(modifier = Modifier.height(7.dp))
            Text(
              text = "DEVELOPED BY THRESHOLD DYNAMICS STUDIO, LLC",
              color = Color(0xFF64748B),
              fontSize = 8.8.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 0.8.sp,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun NexusRiftTearBrandEmblem(
  phaseRad: Float,
  cracklePulse: Float,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    if (w <= 1f || h <= 1f) return@Canvas
    val cx = w * 0.5f
    val cy = h * 0.5f
    val halfW = w * 0.45f
    val halfH = h * 0.29f

    // Jagged asymmetric spacetime fracture path with pure #000000 interior
    val tearPath = Path()
    val steps = 22
    for (s in 0..steps) {
      val nx = (s.toFloat() / steps.toFloat()) * 2f - 1f
      val env = (1f - nx * nx).coerceAtLeast(0f).pow(0.58f)
      val jag =
        (sin(nx * 5.6f + phaseRad * 2.1f) * 0.19f + cos(nx * 11.4f - phaseRad * 3.4f) * 0.08f) *
          halfH *
          (1f - abs(nx))
      val px = cx + nx * halfW
      val py = cy - (halfH * env + jag)
      if (s == 0) tearPath.moveTo(px, py) else tearPath.lineTo(px, py)
    }
    for (s in steps downTo 0) {
      val nx = (s.toFloat() / steps.toFloat()) * 2f - 1f
      val env = (1f - nx * nx).coerceAtLeast(0f).pow(0.58f)
      val jag =
        (cos(nx * 5.2f - phaseRad * 1.9f) * 0.19f + sin(nx * 10.8f + phaseRad * 3.1f) * 0.08f) *
          halfH *
          (1f - abs(nx))
      val px = cx + nx * halfW
      val py = cy + (halfH * env + jag)
      tearPath.lineTo(px, py)
    }
    tearPath.close()

    // Outer multicolored plasma bloom sheath
    drawPath(
      path = tearPath,
      brush =
        Brush.horizontalGradient(
          colors =
            listOf(
              Color(0x6600F5D4),
              Color(0x77A855F7),
              Color(0x66EC4899),
              Color(0x66FACC15),
            )
        ),
      style = Stroke(width = 6.5.dp.toPx(), cap = StrokeCap.Round),
    )

    // Intense crackling plasma wound lip
    drawPath(
      path = tearPath,
      brush =
        Brush.horizontalGradient(
          colors =
            listOf(
              Color(0xFF00F5D4).copy(alpha = cracklePulse),
              Color(0xFFA855F7).copy(alpha = cracklePulse),
              Color(0xFFEC4899).copy(alpha = cracklePulse),
              Color(0xFFFACC15).copy(alpha = cracklePulse),
            )
        ),
      style = Stroke(width = 3.0.dp.toPx(), cap = StrokeCap.Round),
    )

    // Branching electric lightning filaments striking outward from the tear tips & lips
    for (bolt in 0 until 4) {
      val dir = if (bolt % 2 == 0) -1f else 1f
      val anchorFrac = if (bolt < 2) 0.78f else 0.34f
      val ax = cx + dir * halfW * anchorFrac
      val ay = cy + (if (bolt == 1 || bolt == 2) -1f else 1f) * halfH * 0.48f
      val bx = ax + dir * w * 0.08f
      val by = ay + sin(phaseRad * 3f + bolt * 1.7f) * h * 0.16f
      val cx2 = bx + dir * w * 0.06f
      val cy2 = by + cos(phaseRad * 4f + bolt * 2.3f) * h * 0.12f
      val boltCol = if (bolt % 2 == 0) Color(0xFF00F5D4) else Color(0xFFFACC15)
      drawLine(boltCol.copy(alpha = cracklePulse), Offset(ax, ay), Offset(bx, by), strokeWidth = 1.3.dp.toPx())
      drawLine(Color.White.copy(alpha = 0.8f * cracklePulse), Offset(bx, by), Offset(cx2, cy2), strokeWidth = 0.9.dp.toPx())
    }

    // Strict pure #000000 interior inside the crackling spacetime tear
    drawPath(path = tearPath, color = Color.Black)

    // Razor-thin white-hot inner horizon filament
    drawPath(
      path = tearPath,
      color = Color.White.copy(alpha = 0.86f * cracklePulse),
      style = Stroke(width = 1.0.dp.toPx(), cap = StrokeCap.Round),
    )
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
      modifier = Modifier.size(17.dp),
    )
    Spacer(modifier = Modifier.width(10.dp))
    Text(
      text = text,
      color = Color(0xFFD6E4F0),
      fontSize = 12.2.sp,
      fontFamily = FontFamily.Monospace,
    )
  }
}

private object RiftAuthIcons {
  val Rocket: ImageVector by lazy {
    ImageVector.Builder(
        name = "NexusRocket",
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

  val VortexRing: ImageVector by lazy {
    ImageVector.Builder(
        name = "NexusVortexRing",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
      )
      .apply {
        path(fill = SolidColor(Color.White)) {
          moveTo(12f, 3f)
          curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
          curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
          curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
          curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
          close()
          moveTo(12f, 5.6f)
          curveTo(8.47f, 5.6f, 5.6f, 8.47f, 5.6f, 12f)
          curveTo(5.6f, 15.53f, 8.47f, 18.4f, 12f, 18.4f)
          curveTo(15.53f, 18.4f, 18.4f, 15.53f, 18.4f, 12f)
          curveTo(18.4f, 8.47f, 15.53f, 5.6f, 12f, 5.6f)
          close()
          moveTo(12f, 9.2f)
          curveTo(13.55f, 9.2f, 14.8f, 10.45f, 14.8f, 12f)
          curveTo(14.8f, 13.55f, 13.55f, 14.8f, 12f, 14.8f)
          curveTo(10.45f, 14.8f, 9.2f, 13.55f, 9.2f, 12f)
          curveTo(9.2f, 10.45f, 10.45f, 9.2f, 12f, 9.2f)
          close()
        }
      }
      .build()
  }

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

  val Globe: ImageVector by lazy {
    ImageVector.Builder(
        name = "NexusGlobe",
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
