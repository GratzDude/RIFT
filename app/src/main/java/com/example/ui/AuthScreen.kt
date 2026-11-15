package com.example.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
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

  LaunchedEffect(Unit) {
    if (Firebase.auth.currentUser != null) {
      onAuthSuccess()
    }
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.radialGradient(
            colors = listOf(Color(0xFF0A192E), Color(0xFF040914), Color(0xFF000000)),
            radius = 1350f,
          )
        )
        .windowInsetsPadding(WindowInsets.safeDrawing),
    contentAlignment = Alignment.Center,
  ) {
    // Procedural tactical starfield & holographic grid backdrop
    Canvas(modifier = Modifier.fillMaxSize()) {
      val step = 56.dp.toPx()
      var x = 0f
      while (x < size.width) {
        drawLine(
          color = Color(0x1400F5D4),
          start = Offset(x, 0f),
          end = Offset(x, size.height),
          strokeWidth = 1f,
        )
        x += step
      }
      var y = 0f
      while (y < size.height) {
        drawLine(
          color = Color(0x1400F5D4),
          start = Offset(0f, y),
          end = Offset(size.width, y),
          strokeWidth = 1f,
        )
        y += step
      }
    }

    Surface(
      modifier =
        Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
          .widthIn(max = 580.dp)
          .fillMaxWidth()
          .border(1.5.dp, Color(0xFF00F5D4), RoundedCornerShape(16.dp)),
      color = Color(0xEB050C18),
      shape = RoundedCornerShape(16.dp),
      tonalElevation = 10.dp,
    ) {
      Box(modifier = Modifier.fillMaxWidth()) {
        // Chamfered Cyan & Gold Holographic Corner Brackets
        Canvas(modifier = Modifier.matchParentSize()) {
          val stroke = 2.2.dp.toPx()
          val inset = 6.dp.toPx()
          val arm = 22.dp.toPx()
          val cyan = Color(0xFF00F5D4)
          val gold = Color(0xFFFACC15)
          // Top-Left
          drawLine(cyan, Offset(inset, inset), Offset(inset + arm, inset), stroke)
          drawLine(cyan, Offset(inset, inset), Offset(inset, inset + arm), stroke)
          // Top-Right
          drawLine(gold, Offset(size.width - inset - arm, inset), Offset(size.width - inset, inset), stroke)
          drawLine(gold, Offset(size.width - inset, inset), Offset(size.width - inset, inset + arm), stroke)
          // Bottom-Left
          drawLine(gold, Offset(inset, size.height - inset - arm), Offset(inset, size.height - inset), stroke)
          drawLine(gold, Offset(inset, size.height - inset), Offset(inset + arm, size.height - inset), stroke)
          // Bottom-Right
          drawLine(cyan, Offset(size.width - inset - arm, size.height - inset), Offset(size.width - inset, size.height - inset), stroke)
          drawLine(cyan, Offset(size.width - inset, size.height - inset - arm), Offset(size.width - inset, size.height - inset), stroke)
        }

        Column(
          modifier =
            Modifier.verticalScroll(scrollState)
              .padding(horizontal = 24.dp, vertical = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          // Header Row with 3D Swirling NEXUS Singularity Emblem + Title + What's New Patch Notes Pill
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
                  containerColor = Color(0xFF0D2036),
                  contentColor = Color(0xFF38BDF8),
                ),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
              shape = RoundedCornerShape(8.dp),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.height(32.dp).testTag("auth_changelog_button"),
            ) {
              Text(
                text = "📜 BUILD #47",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Holographic Command Briefing Telemetry Panel
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .background(Color(0xB809172A), RoundedCornerShape(10.dp))
                .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            FeatureBullet(icon = RiftAuthIcons.Rocket, text = stringResource(R.string.auth_feature_physics))
            FeatureBullet(icon = RiftAuthIcons.Bolt, text = stringResource(R.string.auth_feature_ships))
            FeatureBullet(icon = RiftAuthIcons.Globe, text = stringResource(R.string.auth_feature_cloud))
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "NEXUS COMMAND READY • SELECT YOUR PILOT LINK TO ENTER MATCHMAKING HUB",
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
                containerColor = Color(0xFF102036),
                contentColor = Color(0xFFFACC15),
              ),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFACC15)),
            shape = RoundedCornerShape(10.dp),
            modifier =
              Modifier.fillMaxWidth()
                .height(48.dp)
                .testTag("skip_sign_in_guest_button"),
          ) {
            Text(
              text = "⚡ ENTER AS [RFT] DRIFTER (GUEST)",
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

  val Bolt: ImageVector by lazy {
    ImageVector.Builder(
        name = "RiftBolt",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
      )
      .apply {
        path(fill = SolidColor(Color.White)) {
          moveTo(11f, 21f)
          lineTo(12f, 14f)
          lineTo(7.5f, 14f)
          lineTo(13f, 3f)
          lineTo(12f, 10f)
          lineTo(16.5f, 10f)
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
