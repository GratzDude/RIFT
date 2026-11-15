package com.example.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.RiftCalibrationState
import com.example.engine.RiftGameEngine

private fun resetAllHudElementOffsets(context: Context) {
  RiftCalibrationState.topBarOffset = Offset.Zero
  RiftCalibrationState.buffBarOffset = Offset.Zero
  RiftCalibrationState.killLogOffset = Offset.Zero
  RiftCalibrationState.bottomConsoleOffset = Offset.Zero
  RiftCalibrationState.combatArcOffset = Offset.Zero
  RiftCalibrationState.leftStickOffset = Offset.Zero
  RiftCalibrationState.fireStickOffset = Offset.Zero
  RiftCalibrationState.bombBtnOffset = Offset.Zero
  RiftCalibrationState.repelBtnOffset = Offset.Zero
  RiftCalibrationState.burstBtnOffset = Offset.Zero
  RiftCalibrationState.brakeBtnOffset = Offset.Zero
  RiftCalibrationState.multiBtnOffset = Offset.Zero
  RiftCalibrationState.calibrationBtnOffset = Offset.Zero
  RiftCalibrationState.nexusBtnOffset = Offset.Zero
  RiftCalibrationState.toastContainerOffset = Offset.Zero
  RiftCalibrationState.calibrationRevision++
  RiftCalibrationState.savePersistedLayout(context)
}

@Composable
fun RiftCalibrationDialog(
  engine: RiftGameEngine? = null,
  onDismiss: () -> Unit,
  onSyncToAllTesters: (() -> Unit)? = null,
  onEnterHudDragArenaMode: (() -> Unit)? = null,
  onReturnToNexusCalibration: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val isCompactPhone = LocalConfiguration.current.screenHeightDp < 480
  val uiUnlocked = RiftCalibrationState.isUiUnlockedForDragging

  DisposableEffect(Unit) {
    RiftCalibrationState.ensureBaselineCaptured()
    onDispose {
      RiftCalibrationState.isDraggingAnySlider = false
      RiftCalibrationState.activeDraggingParamKey = null
    }
  }

  BackHandler {
    RiftCalibrationState.isDraggingAnySlider = false
    RiftCalibrationState.activeDraggingParamKey = null
    if (uiUnlocked) {
      RiftCalibrationState.isUiUnlockedForDragging = false
      RiftCalibrationState.savePersistedLayout(context)
      onReturnToNexusCalibration?.invoke() ?: onDismiss()
    } else {
      onDismiss()
    }
  }

  if (!uiUnlocked) return

  Box(
    modifier =
      Modifier.fillMaxSize()
        .padding(top = 8.dp, start = 12.dp, end = 12.dp)
        .testTag("full_screen_calibration_panel"),
    contentAlignment = Alignment.TopCenter,
  ) {
    Surface(
      modifier = Modifier.border(1.2.dp, Color(0xFF00E676), RoundedCornerShape(12.dp)),
      color = Color(0xEB050E1C),
      shape = RoundedCornerShape(12.dp),
    ) {
      Row(
        modifier =
          Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "🔓 DRAG ANY HUD BUTTON OR STICK DIRECTLY ON ARENA",
          color = Color(0xFF00E676),
          fontSize = if (isCompactPhone) 9.5.sp else 10.5.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
        )
        CalibrationHeaderActionBtn(
          label = "✓ LOCK & RETURN TO NEXUS",
          bgColor = Color(0xFF063A28),
          borderColor = Color(0xFF00E676),
          textColor = Color(0xFF6EE7B7),
          isCompactPhone = isCompactPhone,
          testTag = "calibration_lock_return_button",
          onClick = {
            RiftCalibrationState.isUiUnlockedForDragging = false
            RiftCalibrationState.savePersistedLayout(context)
            onReturnToNexusCalibration?.invoke() ?: onDismiss()
          },
        )
        CalibrationHeaderActionBtn(
          label = "↺ RESET HUD OFFSETS",
          bgColor = Color(0xFF2B1810),
          borderColor = Color(0xFFF59E0B),
          textColor = Color(0xFFFBBF24),
          isCompactPhone = isCompactPhone,
          testTag = "calibration_reset_hud_offsets_button",
          onClick = {
            resetAllHudElementOffsets(context)
            engine?.onPrizeToast("↺ HUD OFFSETS RESET TO DEFAULT POSITIONS")
          },
        )
      }
    }
  }
}

@Composable
private fun CalibrationHeaderActionBtn(
  label: String,
  bgColor: Color,
  borderColor: Color,
  textColor: Color,
  isCompactPhone: Boolean,
  testTag: String,
  onClick: () -> Unit,
) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(8.dp))
        .background(bgColor)
        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 9.dp, vertical = if (isCompactPhone) 4.dp else 5.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      color = textColor,
      fontSize = if (isCompactPhone) 8.5.sp else 9.5.sp,
      fontWeight = FontWeight.ExtraBold,
      fontFamily = FontFamily.Monospace,
      maxLines = 1,
    )
  }
}
