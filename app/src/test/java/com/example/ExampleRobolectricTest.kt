package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import com.example.engine.RiftSpriteBaker
import com.example.engine.RiftVisualForgeState
import com.example.engine.SpaceObstacleStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("RIFT", appName)
  }

  @Test
  fun `build103 custom 5pct scale adjusters survival spreadsheet and gaussian splat state work`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = com.example.data.LocalPilotPrefs(context)
    prefs.recordCompletedShipLife(
      record =
        com.example.data.CompletedShipLifeRecord(
          shipId = 1,
          shipName = "VIPER",
          aliveDurationSec = 42.5f,
          isLocalPilot = true,
          isHuman = true,
          modeId = "CUSTOM_RIFT",
          isCustomGame = true,
          damageTakenScalePct = 65,
          weaponFluxCostScalePct = 40,
          endReason = "ELIMINATED",
        ),
      uid = "test_u103",
    )
    val summary = prefs.getPilotSurvivalTelemetrySummary("test_u103")
    assertTrue(summary.perShip.first { it.shipId == 1 }.completedLives >= 1)
    val html = prefs.exportSurvivalTelemetryHtmlSpreadsheet("PILOT103", 120, "test_u103")
    assertTrue(html.contains("VIPER") && html.contains("applyFilters"))

    val customCfg =
      com.example.engine.CustomGameConfig(
        instanceId = "pub_test103",
        title = "TEST CUSTOM RIFT",
        hostCallsign = "PILOT103",
        baseMode = com.example.engine.RankedGameMode.SECTOR_CONVERGENCE,
        damageTakenScalePct = 65,
        weaponFluxCostScalePct = 40,
      )
    assertEquals(65, customCfg.damageTakenScalePct)
    assertEquals(40, customCfg.weaponFluxCostScalePct)

    val splatRecipe =
      com.example.engine.VisualElementRecipe.defaultFor(com.example.engine.ForgeElementTarget.RIFT_PLASMA)
        .copy(splatSoftness = 1.35f, splatRadiusMult = 1.25f)
    assertEquals(1.35f, splatRecipe.splatSoftness, 0.01f)
    RiftVisualForgeState.resetAllVisualTargetsToDefault()
  }

  @Test
  fun `riftSpriteBaker and AuthSafeHarborRadarShowcase assets initialize and render without ExceptionInInitializerError`() {
    assertTrue(RiftVisualForgeState.visualRevision >= 0)
    assertTrue(RiftSpriteBaker.customShip9Version >= 1)
    assertNotNull(RiftSpriteBaker.safeZoneStationBitmap)
    assertNotNull(RiftSpriteBaker.supplyPodFrameBitmap)
    assertNotNull(RiftSpriteBaker.fluxIconBitmap)
    assertNotNull(RiftSpriteBaker.resonanceIconBitmap)
    assertNotNull(RiftSpriteBaker.riftShardIconBitmap)
    assertNotNull(RiftSpriteBaker.getRiftAnomalyIconBitmap("plasma_rift"))
    assertNotNull(RiftSpriteBaker.getRiftAnomalyIconBitmap("repulsor_rift"))
    assertNotNull(RiftSpriteBaker.getRiftAnomalyIconBitmap("gravity_rift"))
    assertNotNull(RiftSpriteBaker.getRiftAnomalyIconBitmap("phase_rift"))
    assertNotNull(RiftSpriteBaker.getRiftAnomalyIconBitmap("unstable_rift"))

    for (shipId in 1..10) {
      assertNotNull(RiftSpriteBaker.getShipSprite(shipId, isAllied = true, isHitFlashing = false))
      assertNotNull(RiftSpriteBaker.getShipSprite(shipId, isAllied = false, isHitFlashing = false))
    }

    assertNotNull(RiftSpriteBaker.getSpaceObstacleBitmap(SpaceObstacleStyle.ORBITAL_STATION, 0))
    assertNotNull(RiftSpriteBaker.getNexusHubModuleEmblemBitmap("HANGAR_DECK", activeShipId = 1))

    // Exercise the exact Safe-Harbor Holo-Ring & Playfield Rift Canvas calls used by AuthScreen & Arena
    val testBmp = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
    val testCanvas = Canvas(testBmp)
    RiftSpriteBaker.drawSafeZoneStationHoloRing(
      c = testCanvas,
      cx = 64f,
      cy = 64f,
      stationRadius = 48f,
      timeSec = 1.25f,
      isLockedOut = false,
      alphaScale = 1f,
    )
    RiftSpriteBaker.drawUnstableMergedRiftOnCanvas(
      canvas = testCanvas,
      cx = 64f,
      cy = 64f,
      r = 36f,
      spinAngle = 0.5f,
      pulsePhase = 0.4f,
    )
  }
}
