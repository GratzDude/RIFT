package com.example.ui

import android.content.Context
import android.graphics.Paint as NativePaint
import android.graphics.RuntimeShader
import android.os.Build
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
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

private const val AUTH_RING_SPACE_SHADER_SRC =
  """
uniform float2 uResolution;
uniform float uTime;

float hash21(float2 p) {
  p = fract(p * float2(127.1, 311.7));
  p += dot(p, p + 34.23);
  return fract(p.x * p.y);
}

float noise2(float2 p) {
  float2 i = floor(p);
  float2 f = fract(p);
  float2 u = f * f * (3.0 - 2.0 * f);
  float a = hash21(i);
  float b = hash21(i + float2(1.0, 0.0));
  float c = hash21(i + float2(0.0, 1.0));
  float d = hash21(i + float2(1.0, 1.0));
  return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm5(float2 p) {
  float v = 0.0;
  float amp = 0.5;
  float2 shift = float2(19.7, 37.3);
  for (int i = 0; i < 5; i++) {
    v += amp * noise2(p);
    p = float2(p.x * 1.62 - p.y * 1.18, p.x * 1.18 + p.y * 1.62) + shift;
    amp *= 0.5;
  }
  return v;
}

half4 main(float2 fragCoord) {
  float2 res = max(uResolution, float2(1.0, 1.0));
  float2 uv = (fragCoord - 0.5 * res) / min(res.x, res.y);
  float r2 = dot(uv, uv);
  float sphereZ = sqrt(max(0.06, 1.0 - r2 * 0.68));
  float2 domeUv = uv / (0.62 + 0.38 * sphereZ);

  float t = uTime * 0.22;
  float warp = fbm5(domeUv * 2.4 + float2(t * 0.55, -t * 0.38));
  float membrane = fbm5(domeUv * 3.8 + float2(warp * 1.35 - t * 0.42, warp * 1.15 + t * 0.31));
  float ridge = pow(max(0.0, 1.0 - abs(membrane * 2.0 - 1.0)), 3.2);

  float horizonRing = exp(-14.0 * pow(length(uv) - 0.56, 2.0));
  float caustic = 0.5 + 0.5 * sin(domeUv.x * 9.0 + warp * 5.0 + uTime * 0.9) * cos(domeUv.y * 7.5 - uTime * 0.7);

  float3 voidBase = float3(0.008, 0.020, 0.043);
  float3 cyanCol = float3(0.0, 0.96, 0.83);
  float3 cobaltCol = float3(0.16, 0.52, 0.96);
  float3 violetCol = float3(0.62, 0.31, 0.97);

  float3 col = voidBase;
  col += cobaltCol * (membrane * 0.26);
  col += cyanCol * (ridge * 0.32 + horizonRing * 0.22 + caustic * 0.06);
  col += violetCol * (pow(membrane, 2.2) * 0.28 + horizonRing * 0.14);

  float shaft = exp(-6.0 * pow(uv.x * 0.85 + uv.y * 0.35 - 0.08 * sin(uTime * 0.4), 2.0));
  col += float3(0.05, 0.45, 0.62) * shaft * 0.14;

  float vignette = clamp(1.08 - r2 * 0.58, 0.18, 1.0);
  col *= vignette;
  return half4(clamp(col, 0.0, 1.0), 1.0);
}
"""

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

  val ringTransition = rememberInfiniteTransition(label = "nexus_auth_ring_space")
  val shaderClockSec by
    ringTransition.animateFloat(
      initialValue = 0f,
      targetValue = 120f,
      animationSpec = infiniteRepeatable(tween(120000, easing = LinearEasing), RepeatMode.Restart),
      label = "auth_shader_clock",
    )
  val driftPhase by
    ringTransition.animateFloat(
      initialValue = 0f,
      targetValue = (2.0 * PI).toFloat(),
      animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
      label = "auth_drift_phase",
    )
  val emblemSpinDeg by
    ringTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Restart),
      label = "auth_emblem_spin",
    )

  val agslShaderHolder = remember {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      runCatching { RuntimeShader(AUTH_RING_SPACE_SHADER_SRC) to NativePaint(NativePaint.ANTI_ALIAS_FLAG) }.getOrNull()
    } else {
      null
    }
  }

  LaunchedEffect(Unit) {
    if (Firebase.auth.currentUser != null) {
      onAuthSuccess()
    }
  }

  Box(
    modifier = Modifier.fillMaxSize().background(Color(0xFF02050B)),
    contentAlignment = Alignment.Center,
  ) {
    // Full-bleed Ring-Space Containment Sphere backdrop (AGSL API 33+ with FBM Canvas fallback on API 24-32)
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      if (w <= 1f || h <= 1f) return@Canvas

      var renderedAgsl = false
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && agslShaderHolder != null) {
        val (shader, paint) = agslShaderHolder
        renderedAgsl =
          runCatching {
              shader.setFloatUniform("uResolution", w, h)
              shader.setFloatUniform("uTime", shaderClockSec)
              paint.shader = shader
              drawContext.canvas.nativeCanvas.drawRect(0f, 0f, w, h, paint)
              true
            }
            .getOrDefault(false)
      }

      if (!renderedAgsl) {
        drawRect(
          brush =
            Brush.radialGradient(
              colors = listOf(Color(0xFF0B1C36), Color(0xFF050B18), Color(0xFF02050B)),
              center = Offset(w * 0.5f, h * 0.44f),
              radius = maxOf(w, h) * 0.82f,
            )
        )
        drawCircle(
          color = Color(0x2400F5D4),
          radius = minOf(w, h) * 0.52f,
          center = Offset(w * 0.5f, h * 0.48f),
          style = Stroke(width = 2.2.dp.toPx()),
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
            color = if (band == 0) Color(0x2000F5D4) else Color(0x1EA855F7),
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
          )
        }
      }

      for (i in 0 until 38) {
        val seed = (i * 73 + 19) % 97
        val baseX = (seed / 97f) * w
        val baseY = (((i * 41 + 11) % 89) / 89f) * h
        val dx = sin(driftPhase + i * 0.65f) * 14.dp.toPx()
        val dy = cos(driftPhase * 0.85f + i * 0.9f) * 10.dp.toPx()
        val alpha = 0.22f + 0.36f * (0.5f + 0.5f * sin(driftPhase * 2f + i))
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
                NexusSingularityWingsEmblem(
                  spinDeg = emblemSpinDeg,
                  modifier = Modifier.width(64.dp).height(50.dp),
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
              text = "RING SPACE STABILIZED • SELECT PILOT LINK TO ENTER NEXUS HUB",
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
private fun NexusSingularityWingsEmblem(spinDeg: Float, modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    if (w <= 1f || h <= 1f) return@Canvas
    val cx = w * 0.5f
    val cy = h * 0.52f
    val r = minOf(w, h) * 0.36f
    val rad = spinDeg * (PI.toFloat() / 180f)

    // Ambient singularity halo
    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(Color(0x5500F5D4), Color(0x38A855F7), Color.Transparent),
          center = Offset(cx, cy),
          radius = r * 1.85f,
        ),
      radius = r * 1.85f,
      center = Offset(cx, cy),
    )

    // Left & right swept titanium-cyan starfighter wings framing the singularity
    for (side in listOf(-1f, 1f)) {
      val outerWing =
        Path().apply {
          moveTo(cx + side * r * 0.72f, cy - r * 0.18f)
          lineTo(cx + side * w * 0.48f, cy - h * 0.36f)
          lineTo(cx + side * w * 0.40f, cy + h * 0.04f)
          lineTo(cx + side * r * 0.86f, cy + r * 0.22f)
          close()
        }
      drawPath(
        path = outerWing,
        brush =
          Brush.linearGradient(
            colors = listOf(Color(0xFF00F5D4), Color(0xFF1E3A5F)),
            start = Offset(cx, cy - r),
            end = Offset(cx + side * w * 0.48f, cy + r),
          ),
      )
      drawPath(outerWing, Color(0xCC7DD3FC), style = Stroke(width = 1.2.dp.toPx()))

      val lowerPinion =
        Path().apply {
          moveTo(cx + side * r * 0.76f, cy + r * 0.26f)
          lineTo(cx + side * w * 0.36f, cy + h * 0.30f)
          lineTo(cx + side * w * 0.24f, cy + h * 0.42f)
          lineTo(cx + side * r * 0.55f, cy + r * 0.45f)
          close()
        }
      drawPath(lowerPinion, Color(0xCC6D28D9))
      drawPath(lowerPinion, Color(0xCCFACC15), style = Stroke(width = 1.0.dp.toPx()))
    }

    // Back half of 3D-tilted orbital ring
    val ringW = r * 2.15f
    val ringH = r * 0.82f
    drawArc(
      color = Color(0xAA38BDF8),
      startAngle = 180f,
      sweepAngle = 180f,
      useCenter = false,
      topLeft = Offset(cx - ringW * 0.5f, cy - ringH * 0.5f),
      size = Size(ringW, ringH),
      style = Stroke(width = 2.0.dp.toPx(), cap = StrokeCap.Round),
    )

    // Swirling multicolored NEXUS singularity spiral arms
    val armPalette = listOf(Color(0xFF00F5D4), Color(0xFFA855F7), Color(0xFF38BDF8), Color(0xFFFACC15))
    for (arm in 0 until 4) {
      val baseA = rad + arm * (PI.toFloat() * 0.5f)
      val p = Path()
      for (s in 0..12) {
        val f = s / 12f
        val sr = r * (0.20f + 0.76f * f)
        val sa = baseA + f * 2.2f
        val px = cx + cos(sa) * sr
        val py = cy + sin(sa) * sr
        if (s == 0) p.moveTo(px, py) else p.lineTo(px, py)
      }
      drawPath(
        path = p,
        color = armPalette[arm],
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
      )
    }

    // Event horizon & white-hot singularity core
    drawCircle(Color(0xFF030712), radius = r * 0.40f, center = Offset(cx, cy))
    drawCircle(
      color = Color(0xFF00F5D4),
      radius = r * 0.42f,
      center = Offset(cx, cy),
      style = Stroke(width = 1.8.dp.toPx()),
    )
    drawCircle(Color.White, radius = r * 0.16f, center = Offset(cx, cy))

    // Front half of 3D-tilted orbital ring overlapping the singularity
    drawArc(
      color = Color(0xFFFACC15),
      startAngle = 12f,
      sweepAngle = 156f,
      useCenter = false,
      topLeft = Offset(cx - ringW * 0.5f, cy - ringH * 0.5f),
      size = Size(ringW, ringH),
      style = Stroke(width = 2.3.dp.toPx(), cap = StrokeCap.Round),
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
