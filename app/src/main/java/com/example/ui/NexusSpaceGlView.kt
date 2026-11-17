package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES30
import android.opengl.GLUtils
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Choreographer
import android.view.TextureView
import androidx.compose.ui.graphics.Color
import com.example.engine.RiftSpriteBaker
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Random
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * 3D World-Space Node Specification for the Breached-Terminal OpenGL ES 3.0 `NEXUS` Hub:
 * - Index `0` (`CONVERGENCE_GATE`) anchors the **Colossal Center `PLAY // ENTER THE RIFT` Singularity**
 *   punching open the center ~52% of the `#060D1A` Tactical Cockpit Terminal.
 * - Indices `1..7` map the Left Column (`HANGAR_DECK`, `RIFT_CONDUIT`, `STAR_FORGE`),
 *   Right Column (`SUBSPACE_COMMS`, `SYNDICATE_CITADEL`, `ARCHIVE_CODEX`), and Top Bar (`HELM_SYSTEMS`)
 *   for unified D-Pad/Controller focus & dive camera cues.
 */
data class RiftTearWorldSpec(
  val index: Int,
  val node: NexusCoreNode,
  val title: String,
  val subtitle: String,
  val worldX: Float,
  val worldY: Float,
  val worldZ: Float,
  val halfWidthWorld: Float,
  val halfHeightWorld: Float,
  val tiltRad: Float,
  val lipColorA: Color,
  val lipColorB: Color,
  val accentColor: Color,
  val phaseOffset: Float,
  val testTag: String,
)

data class ProjectedRiftTearScreenState(
  val index: Int,
  val node: NexusCoreNode,
  val screenCenterX: Float,
  val screenCenterY: Float,
  val projectedWidthPx: Float,
  val projectedHeightPx: Float,
  val depthScale: Float,
  val visible: Boolean,
)

val NEXUS_3D_RIFT_TEARS: List<RiftTearWorldSpec> =
  listOf(
    // Index 0: Colossal Center Terminal Breach — PLAY // ENTER THE RIFT (CONVERGENCE GATE)
    RiftTearWorldSpec(
      index = 0,
      node = NexusCoreNode.CONVERGENCE_GATE,
      title = "PLAY",
      subtitle = "ENTER THE RIFT",
      worldX = 0.00f,
      worldY = -0.02f,
      worldZ = 0.00f,
      halfWidthWorld = 0.96f,
      halfHeightWorld = 0.96f,
      tiltRad = 0.00f,
      lipColorA = Color(0xFF00F5D4),
      lipColorB = Color(0xFFA855F7),
      accentColor = Color(0xFF00F5D4),
      phaseOffset = 0.0f,
      testTag = "nexus_node_convergence_gate",
    ),
    // Left Tactical Terminal Column (Indices 1..3)
    RiftTearWorldSpec(
      index = 1,
      node = NexusCoreNode.HANGAR_DECK,
      title = "HANGAR DECK",
      subtitle = "10 Ships & 100-Pt Builder",
      worldX = -1.55f,
      worldY = 0.56f,
      worldZ = 0.00f,
      halfWidthWorld = 0.40f,
      halfHeightWorld = 0.36f,
      tiltRad = -0.04f,
      lipColorA = Color(0xFF38BDF8),
      lipColorB = Color(0xFF00F5D4),
      accentColor = Color(0xFF38BDF8),
      phaseOffset = 0.85f,
      testTag = "nexus_node_hangar_deck",
    ),
    RiftTearWorldSpec(
      index = 2,
      node = NexusCoreNode.RIFT_CONDUIT,
      title = "RIFT CONDUIT",
      subtitle = "Resonance, Quests & Cosmetics",
      worldX = -1.55f,
      worldY = -0.02f,
      worldZ = 0.00f,
      halfWidthWorld = 0.40f,
      halfHeightWorld = 0.36f,
      tiltRad = -0.04f,
      lipColorA = Color(0xFFE879F9),
      lipColorB = Color(0xFF00F5D4),
      accentColor = Color(0xFFE879F9),
      phaseOffset = 1.70f,
      testTag = "nexus_node_rift_conduit",
    ),
    RiftTearWorldSpec(
      index = 3,
      node = NexusCoreNode.STAR_FORGE,
      title = "STAR FORGE",
      subtitle = "1:1 Asset & Audio Studio",
      worldX = -1.55f,
      worldY = -0.60f,
      worldZ = 0.00f,
      halfWidthWorld = 0.40f,
      halfHeightWorld = 0.36f,
      tiltRad = -0.04f,
      lipColorA = Color(0xFFC084FC),
      lipColorB = Color(0xFFF472B6),
      accentColor = Color(0xFFC084FC),
      phaseOffset = 2.55f,
      testTag = "nexus_node_star_forge",
    ),
    // Right Tactical Terminal Column (Indices 4..6)
    RiftTearWorldSpec(
      index = 4,
      node = NexusCoreNode.SUBSPACE_COMMS,
      title = "SUBSPACE COMMS",
      subtitle = "Live Chat & NEXUS AI",
      worldX = 1.55f,
      worldY = 0.56f,
      worldZ = 0.00f,
      halfWidthWorld = 0.40f,
      halfHeightWorld = 0.36f,
      tiltRad = 0.04f,
      lipColorA = Color(0xFF00F5D4),
      lipColorB = Color(0xFFFACC15),
      accentColor = Color(0xFF00F5D4),
      phaseOffset = 3.40f,
      testTag = "nexus_node_subspace_comms",
    ),
    RiftTearWorldSpec(
      index = 5,
      node = NexusCoreNode.SYNDICATE_CITADEL,
      title = "SYNDICATE CITADEL",
      subtitle = "Clans & Strike Wings",
      worldX = 1.55f,
      worldY = -0.02f,
      worldZ = 0.00f,
      halfWidthWorld = 0.40f,
      halfHeightWorld = 0.36f,
      tiltRad = 0.04f,
      lipColorA = Color(0xFFFACC15),
      lipColorB = Color(0xFFF97316),
      accentColor = Color(0xFFFACC15),
      phaseOffset = 4.25f,
      testTag = "nexus_node_syndicate_citadel",
    ),
    RiftTearWorldSpec(
      index = 6,
      node = NexusCoreNode.ARCHIVE_CODEX,
      title = "ARCHIVE & CODEX",
      subtitle = "Dossier & Field Manual",
      worldX = 1.55f,
      worldY = -0.60f,
      worldZ = 0.00f,
      halfWidthWorld = 0.40f,
      halfHeightWorld = 0.36f,
      tiltRad = 0.04f,
      lipColorA = Color(0xFF34D399),
      lipColorB = Color(0xFF06B6D4),
      accentColor = Color(0xFF34D399),
      phaseOffset = 5.10f,
      testTag = "nexus_node_archive_codex",
    ),
    // Top Terminal Header System Node (Index 7)
    RiftTearWorldSpec(
      index = 7,
      node = NexusCoreNode.HELM_SYSTEMS,
      title = "HELM SYSTEMS",
      subtitle = "Audio, FOV & Controls",
      worldX = 1.25f,
      worldY = 0.92f,
      worldZ = 0.00f,
      halfWidthWorld = 0.36f,
      halfHeightWorld = 0.32f,
      tiltRad = 0.02f,
      lipColorA = Color(0xFF94A3B8),
      lipColorB = Color(0xFF38BDF8),
      accentColor = Color(0xFFCBD5E1),
      phaseOffset = 5.95f,
      testTag = "nexus_node_helm_systems",
    ),
  )

internal fun isHeadlessGlEnvironment(): Boolean {
  val fp = Build.FINGERPRINT ?: ""
  val model = Build.MODEL ?: ""
  if (fp.contains("robolectric", ignoreCase = true) ||
    model.contains("robolectric", ignoreCase = true)
  ) {
    return true
  }
  return runCatching {
    Class.forName("org.robolectric.Robolectric")
    true
  }.getOrDefault(false)
}

private const val CAMERA_FOV_Y_DEG = 46.0f
private const val BASE_CAMERA_Z = 3.18f

internal data class CameraBasis3D(
  val camX: Float,
  val camY: Float,
  val camZ: Float,
  val fwdX: Float,
  val fwdY: Float,
  val fwdZ: Float,
  val rightX: Float,
  val rightY: Float,
  val rightZ: Float,
  val upX: Float,
  val upY: Float,
  val upZ: Float,
  val tanHalfFovY: Float,
  val aspect: Float,
)

private fun buildCameraBasis3D(
  viewportW: Float,
  viewportH: Float,
  timeSec: Float,
  lookYawRad: Float,
  lookPitchRad: Float,
  diveTearIdx: Int,
  diveProgress: Float,
  isSplashMode: Boolean,
): CameraBasis3D {
  val safeW = max(1f, viewportW)
  val safeH = max(1f, viewportH)
  val aspect = safeW / safeH
  val tanHalfFovY = tan((CAMERA_FOV_Y_DEG * 0.5f) * (PI.toFloat() / 180f))
  val aspectPullback = if (isSplashMode) 1.04f else max(1.0f, 1.55f / aspect.coerceAtLeast(0.55f))
  val baseZ = BASE_CAMERA_Z * aspectPullback

  val orbitX = if (isSplashMode) sin(timeSec * 0.24f) * 0.035f else 0f
  val orbitY = if (isSplashMode) cos(timeSec * 0.19f) * 0.025f else 0f
  val orbitZ = if (isSplashMode) sin(timeSec * 0.16f) * 0.028f else 0f

  var camX = orbitX + sin(lookYawRad) * 0.42f
  var camY = orbitY + sin(lookPitchRad) * 0.32f
  var camZ = baseZ + orbitZ

  var lookX = -sin(lookYawRad) * 0.48f
  var lookY = -sin(lookPitchRad) * 0.38f
  var lookZ = 0.0f

  if (!isSplashMode && diveTearIdx in NEXUS_3D_RIFT_TEARS.indices && diveProgress > 0.001f) {
    val target = NEXUS_3D_RIFT_TEARS[diveTearIdx]
    val ease = diveProgress * diveProgress * (3f - 2f * diveProgress)
    // Dive straight toward the Colossal Center Rift throat (0, -0.02, 0)
    val diveTargetX = if (diveTearIdx == 0) 0.0f else target.worldX * 0.35f
    val diveTargetY = if (diveTearIdx == 0) -0.02f else target.worldY * 0.35f
    camX += (diveTargetX - camX) * (ease * 0.76f)
    camY += (diveTargetY - camY) * (ease * 0.76f)
    camZ += (0.72f - camZ) * (ease * 0.82f)
    lookX += (diveTargetX - lookX) * ease
    lookY += (diveTargetY - lookY) * ease
    lookZ += (0.0f - lookZ) * ease
  }

  var fx = lookX - camX
  var fy = lookY - camY
  var fz = lookZ - camZ
  val fLen = sqrt(max(1e-6f, fx * fx + fy * fy + fz * fz))
  fx /= fLen
  fy /= fLen
  fz /= fLen

  var rx = -fz
  var ry = 0f
  var rz = fx
  val rLen = sqrt(max(1e-6f, rx * rx + ry * ry + rz * rz))
  rx /= rLen
  ry /= rLen
  rz /= rLen

  val ux = ry * fz - rz * fy
  val uy = rz * fx - rx * fz
  val uz = rx * fy - ry * fx

  return CameraBasis3D(
    camX = camX,
    camY = camY,
    camZ = camZ,
    fwdX = fx,
    fwdY = fy,
    fwdZ = fz,
    rightX = rx,
    rightY = ry,
    rightZ = rz,
    upX = ux,
    upY = uy,
    upZ = uz,
    tanHalfFovY = tanHalfFovY,
    aspect = aspect,
  )
}

internal fun projectTearsWithBasis(
  basis: CameraBasis3D,
  viewportW: Float,
  viewportH: Float,
  @Suppress("UNUSED_PARAMETER") timeSec: Float,
): List<ProjectedRiftTearScreenState> {
  val safeW = max(1f, viewportW)
  val safeH = max(1f, viewportH)
  return NEXUS_3D_RIFT_TEARS.map { spec ->
    val wx = spec.worldX
    val wy = spec.worldY
    val wz = spec.worldZ

    val dx = wx - basis.camX
    val dy = wy - basis.camY
    val dz = wz - basis.camZ

    val vx = dx * basis.rightX + dy * basis.rightY + dz * basis.rightZ
    val vy = dx * basis.upX + dy * basis.upY + dz * basis.upZ
    val vz = dx * basis.fwdX + dy * basis.fwdY + dz * basis.fwdZ

    if (vz <= 0.12f) {
      ProjectedRiftTearScreenState(
        index = spec.index,
        node = spec.node,
        screenCenterX = safeW * 0.5f,
        screenCenterY = safeH * 0.5f,
        projectedWidthPx = 0f,
        projectedHeightPx = 0f,
        depthScale = 0f,
        visible = false,
      )
    } else {
      val ndcX = vx / (vz * basis.aspect * basis.tanHalfFovY)
      val ndcY = vy / (vz * basis.tanHalfFovY)
      val pxPerWorld = (safeH * 0.5f) / (vz * basis.tanHalfFovY)
      val projW = 2f * spec.halfWidthWorld * pxPerWorld
      val projH = 2f * spec.halfHeightWorld * pxPerWorld
      val cx = (0.5f + 0.5f * ndcX) * safeW
      val cy = (0.5f - 0.5f * ndcY) * safeH
      val visible =
        cx + projW * 0.5f > 0f &&
          cx - projW * 0.5f < safeW &&
          cy + projH * 0.5f > 0f &&
          cy - projH * 0.5f < safeH
      ProjectedRiftTearScreenState(
        index = spec.index,
        node = spec.node,
        screenCenterX = cx,
        screenCenterY = cy,
        projectedWidthPx = projW,
        projectedHeightPx = projH,
        depthScale = (BASE_CAMERA_Z / vz).coerceIn(0.55f, 2.2f),
        visible = visible,
      )
    }
  }
}

fun computeStaticProjectedTearStates(
  viewportW: Float,
  viewportH: Float,
): List<ProjectedRiftTearScreenState> {
  val basis =
    buildCameraBasis3D(
      viewportW = viewportW,
      viewportH = viewportH,
      timeSec = 0f,
      lookYawRad = 0f,
      lookPitchRad = 0f,
      diveTearIdx = -1,
      diveProgress = 0f,
      isSplashMode = false,
    )
  return projectTearsWithBasis(basis, viewportW, viewportH, 0f)
}

/**
 * Live 3D Playfield Ship Entity banking along curved 3D Bezier trajectories into and out of the
 * Colossal Center `NEXUS` Rift Breach.
 */
private class HubTrafficShip(
  var shipId: Int = 1,
  var routeType: Int = 0, // 0 = OUTER_TO_BREACH, 1 = BREACH_TO_OUTER, 2 = ACCRETION_RIM_SKIM
  var progress: Float = 0f,
  var durationSec: Float = 6.2f,
  var p0x: Float = 0f, var p0y: Float = 0f, var p0z: Float = 0f,
  var p1x: Float = 0f, var p1y: Float = 0f, var p1z: Float = 0f,
  var p2x: Float = 0f, var p2y: Float = 0f, var p2z: Float = 0f,
  var p3x: Float = 0f, var p3y: Float = 0f, var p3z: Float = 0f,
  var curX: Float = 0f, var curY: Float = 0f, var curZ: Float = 0f,
  var dirX: Float = 1f, var dirY: Float = 0f, var dirZ: Float = 0f,
  var bankRad: Float = 0f,
  val trailHistoryX: FloatArray = FloatArray(TRAIL_POINTS),
  val trailHistoryY: FloatArray = FloatArray(TRAIL_POINTS),
  val trailHistoryZ: FloatArray = FloatArray(TRAIL_POINTS),
  var trailInitialized: Boolean = false,
) {
  companion object {
    const val TRAIL_POINTS = 10
  }
}

/**
 * Hardware-accelerated OpenGL ES 3.0 (`#version 300 es`) Breached-Terminal `NEXUS` Rift Renderer:
 * - Choreographer hardware VSYNC pacing (`eglSwapInterval(1)` + EMA `dt` smoothing).
 * - Pass 1A: Even-aligned `0.62x` linear `fboPlasma` + `1.0x` `fboScene` rendering gameplay-honest `#02050B`
 *   dark space, crisp `1.0x` multi-depth starlight, and a **Colossal High-Contrast Swirling Playfield Rift Singularity**
 *   at dead center (`(0.0, -0.02, 0.0)`, `r = 0.96`) with luminous cyan/sky/violet/magenta/gold spiral plasma ribbons
 *   whipping inward into a feathered `#000000` event-horizon throat (`voidKeepMask`).
 * - Pass 1B: Live Real-Ship Playfield Traffic (`max(5, onlinePilotShipIds.size)`) using a `GLES30` `5x2`
 *   `128x128` ship sprite atlas banking into and out of the central Rift breach with glowing ion wakes & warp flashes.
 * - Pass 2–5: Half-res bright-pass HDR Gaussian Bloom + haze-free ACES Filmic Tonemap.
 */
class NexusSpaceGlView(context: Context) :
  TextureView(context), TextureView.SurfaceTextureListener {

  @Volatile var isSplashMode: Boolean = false
  @Volatile var dimFactor: Float = 1.0f
  @Volatile var focusedTearIndex: Int = -1
  @Volatile private var activeDiveTearIdx: Int = -1
  @Volatile private var activeDiveProgress: Float = 0f

  @Volatile private var targetLookYawRad: Float = 0f
  @Volatile private var targetLookPitchRad: Float = 0f
  @Volatile private var currentLookYawRad: Float = 0f
  @Volatile private var currentLookPitchRad: Float = 0f
  @Volatile private var springBackActive: Boolean = true
  @Volatile private var latestTimeSec: Float = 0f

  @Volatile private var onlinePilotShipIds: IntArray = intArrayOf(1)
  @Volatile private var holoOnlinePilots: Int = 1
  @Volatile private var holoUnreadComms: Int = 0
  @Volatile private var projectionDirty: Boolean = true

  private var renderThread: GlSpaceRenderThread? = null

  init {
    isOpaque = false
    if (!isHeadlessGlEnvironment()) {
      surfaceTextureListener = this
    }
  }

  fun consumeProjectionDirty(): Boolean {
    if (projectionDirty) {
      projectionDirty = false
      return true
    }
    return false
  }

  fun updateStationHologramTelemetry(onlinePilots: Int, unreadComms: Int) {
    holoOnlinePilots = onlinePilots.coerceAtLeast(1)
    holoUnreadComms = unreadComms.coerceAtLeast(0)
  }

  fun updateOnlinePilotShips(shipIds: IntArray) {
    onlinePilotShipIds =
      if (shipIds.isEmpty()) {
        intArrayOf(1)
      } else {
        IntArray(shipIds.size) { i -> shipIds[i].coerceIn(1, 10) }
      }
  }

  fun addCameraLookDelta(dxPx: Float, dyPx: Float) {
    val w = max(1f, width.toFloat())
    val h = max(1f, height.toFloat())
    springBackActive = false
    targetLookYawRad = (targetLookYawRad - (dxPx / w) * 0.42f).coerceIn(-0.22f, 0.22f)
    targetLookPitchRad = (targetLookPitchRad + (dyPx / h) * 0.30f).coerceIn(-0.15f, 0.15f)
    projectionDirty = true
  }

  fun releaseCameraLookSpring() {
    springBackActive = true
    projectionDirty = true
  }

  fun triggerTearDive(tearIndex: Int) {
    activeDiveTearIdx = tearIndex
    activeDiveProgress = 0.01f
    projectionDirty = true
  }

  fun triggerDiveIntoTear(tearIndex: Int) = triggerTearDive(tearIndex)

  fun clearTearDive() {
    activeDiveTearIdx = -1
    activeDiveProgress = 0f
    projectionDirty = true
  }

  fun resetCameraDive() = clearTearDive()

  fun computeProjectedTearStates(
    viewportW: Float = width.toFloat(),
    viewportH: Float = height.toFloat(),
  ): List<ProjectedRiftTearScreenState> {
    val basis =
      buildCameraBasis3D(
        viewportW = viewportW,
        viewportH = viewportH,
        timeSec = latestTimeSec,
        lookYawRad = currentLookYawRad,
        lookPitchRad = currentLookPitchRad,
        diveTearIdx = activeDiveTearIdx,
        diveProgress = activeDiveProgress,
        isSplashMode = isSplashMode,
      )
    return projectTearsWithBasis(basis, viewportW, viewportH, latestTimeSec)
  }

  override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
    if (isHeadlessGlEnvironment()) return
    projectionDirty = true
    stopRenderThread()
    renderThread =
      GlSpaceRenderThread(surface, max(1, width), max(1, height)).also { it.start() }
  }

  override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
    projectionDirty = true
    renderThread?.updateViewportSize(max(1, width), max(1, height))
  }

  override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
    stopRenderThread()
    return true
  }

  override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (!isHeadlessGlEnvironment() && isAvailable && renderThread == null) {
      surfaceTexture?.let { st ->
        onSurfaceTextureAvailable(st, max(1, width), max(1, height))
      }
    }
  }

  override fun onDetachedFromWindow() {
    stopRenderThread()
    super.onDetachedFromWindow()
  }

  private fun stopRenderThread() {
    val thread = renderThread ?: return
    renderThread = null
    thread.requestStop()
    runCatching { thread.join(350L) }
  }

  private inner class GlSpaceRenderThread(
    private val surfaceTexture: SurfaceTexture,
    initialWidth: Int,
    initialHeight: Int,
  ) : Thread("NexusSpaceGlES30Thread") {

    private val running = AtomicBoolean(true)
    @Volatile private var viewportW: Int = initialWidth
    @Volatile private var viewportH: Int = initialHeight

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    private var emptyVao = 0
    private var progScene = 0
    private var progSceneUpsample = 0
    private var progShipHull = 0
    private var progShipTrail = 0
    private var progBright = 0
    private var progBlur = 0
    private var progComposite = 0

    private var fboPlasma = 0
    private var texPlasma = 0
    private var plasmaW = 0
    private var plasmaH = 0
    private var fboScene = 0
    private var texScene = 0
    private var rboSceneDepth = 0
    private var fboBloomA = 0
    private var texBloomA = 0
    private var fboBloomB = 0
    private var texBloomB = 0
    private var allocatedW = 0
    private var allocatedH = 0

    private var texShipAtlas = 0
    private var bakedCustomShipVersion = -1
    private var vaoShipHull = 0
    private var vboShipHull = 0
    private var vaoShipTrail = 0
    private var vboShipTrail = 0
    private val shipHullScratch = FloatArray(32 * 6 * 7)
    private val shipHullBuffer: FloatBuffer =
      ByteBuffer.allocateDirect(shipHullScratch.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private val shipTrailScratch = FloatArray(32 * (HubTrafficShip.TRAIL_POINTS + 2) * 6 * 7)
    private val shipTrailBuffer: FloatBuffer =
      ByteBuffer.allocateDirect(shipTrailScratch.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

    private val tearPosSizeBuf = FloatArray(4)
    private val tearStyleBuf = FloatArray(4)
    private val tearColorABuf = FloatArray(3)
    private val tearColorBBuf = FloatArray(3)

    private val trafficRng = Random(0x66EE53L)
    private val trafficShips = ArrayList<HubTrafficShip>(28)

    private val vsyncLock = Object()
    @Volatile private var pendingVsyncFrameNs: Long = 0L
    @Volatile private var hasPendingVsync: Boolean = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val vsyncCallback =
      object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
          if (!running.get()) return
          synchronized(vsyncLock) {
            pendingVsyncFrameNs = frameTimeNanos
            hasPendingVsync = true
            vsyncLock.notifyAll()
          }
          Choreographer.getInstance().postFrameCallback(this)
        }
      }

    fun updateViewportSize(w: Int, h: Int) {
      viewportW = w
      viewportH = h
      projectionDirty = true
    }

    fun requestStop() {
      running.set(false)
      synchronized(vsyncLock) {
        hasPendingVsync = true
        vsyncLock.notifyAll()
      }
      mainHandler.post {
        runCatching { Choreographer.getInstance().removeFrameCallback(vsyncCallback) }
      }
    }

    override fun run() {
      if (!initEglAndGl()) {
        releaseAllGlResources()
        return
      }

      mainHandler.post {
        if (running.get()) {
          runCatching { Choreographer.getInstance().postFrameCallback(vsyncCallback) }
        }
      }

      var startNs = 0L
      var prevFrameNs = 0L
      var smoothedDtSec = 1f / 60f
      var wasCameraMoving = false

      try {
        while (running.get()) {
          var vsyncNs: Long
          synchronized(vsyncLock) {
            if (!hasPendingVsync && running.get()) {
              runCatching { vsyncLock.wait(32L) }
            }
            hasPendingVsync = false
            vsyncNs = pendingVsyncFrameNs
          }
          if (!running.get()) break

          val frameNs = if (vsyncNs > 0L) vsyncNs else System.nanoTime()
          if (startNs == 0L) startNs = frameNs
          val rawDt =
            if (prevFrameNs == 0L) (1f / 60f)
            else ((frameNs - prevFrameNs) * 1e-9f).coerceIn(0.002f, 0.05f)
          prevFrameNs = frameNs
          smoothedDtSec += (rawDt - smoothedDtSec) * 0.22f
          val dtSec = smoothedDtSec.coerceIn(0.004f, 0.034f)
          val elapsedSec = ((frameNs - startNs) * 1e-9f).coerceAtLeast(0f)
          latestTimeSec = elapsedSec

          if (springBackActive) {
            targetLookYawRad += (0f - targetLookYawRad) * min(1f, dtSec * 4.5f)
            targetLookPitchRad += (0f - targetLookPitchRad) * min(1f, dtSec * 4.5f)
          }
          val smooth = min(1f, dtSec * 9.5f)
          currentLookYawRad += (targetLookYawRad - currentLookYawRad) * smooth
          currentLookPitchRad += (targetLookPitchRad - currentLookPitchRad) * smooth

          if (activeDiveTearIdx >= 0 && activeDiveProgress > 0f) {
            activeDiveProgress = (activeDiveProgress + dtSec * 2.8f).coerceAtMost(1f)
          }

          val cameraActiveNow =
            abs(currentLookYawRad) > 1e-4f ||
              abs(currentLookPitchRad) > 1e-4f ||
              abs(targetLookYawRad) > 1e-4f ||
              abs(targetLookPitchRad) > 1e-4f ||
              activeDiveProgress > 0f
          if (cameraActiveNow) {
            projectionDirty = true
            wasCameraMoving = true
          } else if (wasCameraMoving) {
            currentLookYawRad = 0f
            currentLookPitchRad = 0f
            targetLookYawRad = 0f
            targetLookPitchRad = 0f
            projectionDirty = true
            wasCameraMoving = false
          }

          val w = max(1, viewportW)
          val h = max(1, viewportH)
          runCatching {
            if (w != allocatedW || h != allocatedH) {
              allocateFramebuffers(w, h)
              projectionDirty = true
            }
            ensureShipAtlasUpToDate()
            stepTrafficSimulation(dtSec)

            drawFrame(w, h, elapsedSec)
            EGL14.eglSwapBuffers(eglDisplay, eglSurface)
          }.onFailure { err ->
            Log.e("NexusSpaceGlView", "Frame render error", err)
          }
        }
      } finally {
        mainHandler.post {
          runCatching { Choreographer.getInstance().removeFrameCallback(vsyncCallback) }
        }
        releaseAllGlResources()
      }
    }

    private fun initEglAndGl(): Boolean {
      return runCatching {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) return false
        val vers = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, vers, 0, vers, 1)) return false

        val configAttribs =
          intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, 0x0040, // EGL_OPENGL_ES3_BIT_KHR
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 0,
            EGL14.EGL_STENCIL_SIZE, 0,
            EGL14.EGL_NONE,
          )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0) ||
          numConfigs[0] <= 0
        ) {
          return false
        }
        val chosenConfig = configs[0] ?: return false

        val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(eglDisplay, chosenConfig, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
        if (eglContext == EGL14.EGL_NO_CONTEXT) return false

        val surfAttribs = intArrayOf(EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, chosenConfig, surfaceTexture, surfAttribs, 0)
        if (eglSurface == EGL14.EGL_NO_SURFACE) return false

        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) return false
        EGL14.eglSwapInterval(eglDisplay, 1)

        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        emptyVao = vaos[0]

        progScene = linkProgram("progScene", FULLSCREEN_TRI_VERT_GLSL, NEXUS_VOLUMETRIC_SCENE_FRAG_GLSL)
        progSceneUpsample = linkProgram("progSceneUpsample", FULLSCREEN_TRI_VERT_GLSL, NEXUS_SCENE_UPSAMPLE_FRAG_GLSL)
        progShipHull = linkProgram("progShipHull", SHIP_HULL_VERT_GLSL, SHIP_HULL_FRAG_GLSL)
        progShipTrail = linkProgram("progShipTrail", SHIP_TRAIL_VERT_GLSL, SHIP_TRAIL_FRAG_GLSL)
        progBright = linkProgram("progBright", FULLSCREEN_TRI_VERT_GLSL, BLOOM_BRIGHT_EXTRACT_FRAG_GLSL)
        progBlur = linkProgram("progBlur", FULLSCREEN_TRI_VERT_GLSL, BLOOM_GAUSSIAN_BLUR_FRAG_GLSL)
        progComposite = linkProgram("progComposite", FULLSCREEN_TRI_VERT_GLSL, FINAL_ACES_COMPOSITE_FRAG_GLSL)

        if (progScene == 0 || progSceneUpsample == 0 || progBright == 0 || progBlur == 0 || progComposite == 0) {
          Log.e(
            "NexusSpaceGlView",
            "Core GL program link failed: scene=$progScene upsample=$progSceneUpsample bright=$progBright blur=$progBlur comp=$progComposite",
          )
          return false
        }

        initDynamicShipBuffers()
        true
      }.onFailure { err ->
        Log.e("NexusSpaceGlView", "initEglAndGl failed", err)
      }.getOrDefault(false)
    }

    private fun initDynamicShipBuffers() {
      val vaos = IntArray(2)
      val vbos = IntArray(2)
      GLES30.glGenVertexArrays(2, vaos, 0)
      GLES30.glGenBuffers(2, vbos, 0)

      vaoShipHull = vaos[0]
      vboShipHull = vbos[0]
      val hullStride = 7 * 4
      GLES30.glBindVertexArray(vaoShipHull)
      GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboShipHull)
      GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, shipHullScratch.size * 4, null, GLES30.GL_DYNAMIC_DRAW)
      GLES30.glEnableVertexAttribArray(0)
      GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, hullStride, 0)
      GLES30.glEnableVertexAttribArray(1)
      GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, hullStride, 3 * 4)
      GLES30.glEnableVertexAttribArray(2)
      GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, hullStride, 5 * 4)

      vaoShipTrail = vaos[1]
      vboShipTrail = vbos[1]
      val trailStride = 7 * 4
      GLES30.glBindVertexArray(vaoShipTrail)
      GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboShipTrail)
      GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, shipTrailScratch.size * 4, null, GLES30.GL_DYNAMIC_DRAW)
      GLES30.glEnableVertexAttribArray(0)
      GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, trailStride, 0)
      GLES30.glEnableVertexAttribArray(1)
      GLES30.glVertexAttribPointer(1, 4, GLES30.GL_FLOAT, false, trailStride, 3 * 4)

      GLES30.glBindVertexArray(0)
      GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    private fun ensureShipAtlasUpToDate() {
      val curCustomVer = RiftSpriteBaker.customShip9Version
      if (texShipAtlas != 0 && curCustomVer == bakedCustomShipVersion) return
      bakedCustomShipVersion = curCustomVer

      val tilePx = 128
      val atlasW = tilePx * 5
      val atlasH = tilePx * 2
      val atlasBmp = Bitmap.createBitmap(atlasW, atlasH, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(atlasBmp)
      val dstRect = android.graphics.Rect()
      for (shipId in 1..10) {
        val col = (shipId - 1) % 5
        val row = (shipId - 1) / 5
        val left = col * tilePx
        val top = row * tilePx
        dstRect.set(left, top, left + tilePx, top + tilePx)
        val sprite =
          RiftSpriteBaker.getShipSprite(
            shipId = shipId,
            isAllied = true,
            isHitFlashing = false,
            trimStyleOverride = 0,
          )
        canvas.drawBitmap(sprite, null, dstRect, null)
      }

      if (texShipAtlas == 0) {
        val texArr = IntArray(1)
        GLES30.glGenTextures(1, texArr, 0)
        texShipAtlas = texArr[0]
      }
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texShipAtlas)
      GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR)
      GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
      GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
      GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
      GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, atlasBmp, 0)
      GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
      atlasBmp.recycle()
    }

    private fun assignNextRoute(ship: HubTrafficShip, slotIdx: Int, randomizeInitialProgress: Boolean) {
      val nextType =
        when {
          randomizeInitialProgress -> if (slotIdx % 4 == 3) 2 else (slotIdx % 2)
          ship.routeType == 0 -> if (trafficRng.nextFloat() < 0.25f) 2 else 1
          ship.routeType == 1 -> 0
          else -> 0
        }
      ship.routeType = nextType
      ship.durationSec = 4.8f + trafficRng.nextFloat() * 2.8f
      ship.progress = if (randomizeInitialProgress) trafficRng.nextFloat() * 0.92f else 0f
      ship.trailInitialized = false

      val centerRift = NEXUS_3D_RIFT_TEARS[0]
      val cx = centerRift.worldX
      val cy = centerRift.worldY
      val cz = centerRift.worldZ

      // Outer corridor spawn/exit around the colossal center breach
      val outerAng = (slotIdx * 1.15f) + trafficRng.nextFloat() * (PI.toFloat() * 2f)
      val outerR = 1.18f + trafficRng.nextFloat() * 0.45f
      val ox = cx + cos(outerAng) * outerR
      val oy = cy + sin(outerAng) * (outerR * 0.86f)
      val oz = cz + 0.22f + trafficRng.nextFloat() * 0.28f

      // Spiraling entry/exit point along the inner event-horizon accretion rim (r ~ 0.34)
      val swirlDelta = if (slotIdx % 2 == 0) 1.35f else -1.35f
      val midAng = outerAng + swirlDelta * 0.55f
      val throatAng = outerAng + swirlDelta
      val throatR = 0.33f
      val tx = cx + cos(throatAng) * throatR
      val ty = cy + sin(throatAng) * throatR
      val tz = cz + 0.04f

      when (nextType) {
        0 -> { // OUTER_TO_BREACH: Real Playfield ship banks inward along spiral arm and dives into the Rift!
          ship.p0x = ox; ship.p0y = oy; ship.p0z = oz
          ship.p1x = cx + cos(outerAng + swirlDelta * 0.28f) * (outerR * 0.78f)
          ship.p1y = cy + sin(outerAng + swirlDelta * 0.28f) * (outerR * 0.74f)
          ship.p1z = oz * 0.75f + 0.12f
          ship.p2x = cx + cos(midAng) * 0.58f
          ship.p2y = cy + sin(midAng) * 0.58f
          ship.p2z = cz + 0.16f
          ship.p3x = tx; ship.p3y = ty; ship.p3z = tz
        }
        1 -> { // BREACH_TO_OUTER: Ship erupts from the swirling Rift rim and banks outward into dark space
          ship.p0x = tx; ship.p0y = ty; ship.p0z = tz
          ship.p1x = cx + cos(throatAng + swirlDelta * 0.45f) * 0.56f
          ship.p1y = cy + sin(throatAng + swirlDelta * 0.45f) * 0.56f
          ship.p1z = cz + 0.18f
          ship.p2x = cx + cos(outerAng + swirlDelta * 0.85f) * (outerR * 0.80f)
          ship.p2y = cy + sin(outerAng + swirlDelta * 0.85f) * (outerR * 0.76f)
          ship.p2z = oz * 0.82f
          ship.p3x = cx + cos(outerAng + swirlDelta * 1.15f) * outerR
          ship.p3y = cy + sin(outerAng + swirlDelta * 1.15f) * (outerR * 0.86f)
          ship.p3z = oz
        }
        else -> { // ACCRETION_RIM_SKIM: High-speed curved skim along the glowing cyan/violet accretion rim
          val a0 = outerAng
          val dir = if (slotIdx % 2 == 0) 1f else -1f
          val skimR = 0.68f + (slotIdx % 3) * 0.09f
          val a1 = a0 + dir * 0.95f
          val a2 = a0 + dir * 1.90f
          val a3 = a0 + dir * 2.85f
          ship.p0x = cx + cos(a0) * skimR; ship.p0y = cy + sin(a0) * skimR; ship.p0z = cz + 0.14f
          ship.p1x = cx + cos(a1) * (skimR * 1.14f); ship.p1y = cy + sin(a1) * (skimR * 1.14f); ship.p1z = cz + 0.26f
          ship.p2x = cx + cos(a2) * (skimR * 1.14f); ship.p2y = cy + sin(a2) * (skimR * 1.14f); ship.p2z = cz + 0.22f
          ship.p3x = cx + cos(a3) * skimR; ship.p3y = cy + sin(a3) * skimR; ship.p3z = cz + 0.12f
        }
      }
    }

    private fun evalBezierPosAndVel(ship: HubTrafficShip, t: Float) {
      val u = 1f - t
      val b0 = u * u * u
      val b1 = 3f * u * u * t
      val b2 = 3f * u * t * t
      val b3 = t * t * t
      ship.curX = b0 * ship.p0x + b1 * ship.p1x + b2 * ship.p2x + b3 * ship.p3x
      ship.curY = b0 * ship.p0y + b1 * ship.p1y + b2 * ship.p2y + b3 * ship.p3y
      ship.curZ = b0 * ship.p0z + b1 * ship.p1z + b2 * ship.p2z + b3 * ship.p3z

      val d0 = 3f * u * u
      val d1 = 6f * u * t
      val d2 = 3f * t * t
      var vx = d0 * (ship.p1x - ship.p0x) + d1 * (ship.p2x - ship.p1x) + d2 * (ship.p3x - ship.p2x)
      var vy = d0 * (ship.p1y - ship.p0y) + d1 * (ship.p2y - ship.p1y) + d2 * (ship.p3y - ship.p2y)
      var vz = d0 * (ship.p1z - ship.p0z) + d1 * (ship.p2z - ship.p1z) + d2 * (ship.p3z - ship.p2z)
      val vLen = sqrt(max(1e-6f, vx * vx + vy * vy + vz * vz))
      vx /= vLen
      vy /= vLen
      vz /= vLen

      val prevDx = ship.dirX
      val prevDy = ship.dirY
      ship.dirX = vx
      ship.dirY = vy
      ship.dirZ = vz
      val turnSignal = (vx * prevDy - vy * prevDx) * 10f
      ship.bankRad = (ship.bankRad * 0.85f + turnSignal.coerceIn(-0.68f, 0.68f) * 0.15f)
    }

    private fun stepTrafficSimulation(dtSec: Float) {
      val onlineIds = onlinePilotShipIds
      val desiredCount =
        if (isSplashMode) {
          5
        } else {
          max(6, onlineIds.size).coerceAtMost(24)
        }

      while (trafficShips.size < desiredCount) {
        val idx = trafficShips.size
        val assignedHull =
          if (idx < onlineIds.size) {
            onlineIds[idx].coerceIn(1, 10)
          } else {
            ((idx * 3 + 1) % 10) + 1
          }
        val s = HubTrafficShip(shipId = assignedHull)
        assignNextRoute(s, idx, randomizeInitialProgress = true)
        evalBezierPosAndVel(s, s.progress)
        trafficShips.add(s)
      }
      while (trafficShips.size > desiredCount) {
        trafficShips.removeAt(trafficShips.lastIndex)
      }

      for (i in 0 until trafficShips.size) {
        val s = trafficShips[i]
        s.shipId =
          if (i < onlineIds.size) {
            onlineIds[i].coerceIn(1, 10)
          } else {
            ((i * 3 + 1) % 10) + 1
          }

        s.progress += dtSec / max(2.4f, s.durationSec)
        if (s.progress >= 1f) {
          assignNextRoute(s, i, randomizeInitialProgress = false)
        }
        evalBezierPosAndVel(s, s.progress.coerceIn(0f, 1f))

        if (!s.trailInitialized) {
          for (k in 0 until HubTrafficShip.TRAIL_POINTS) {
            val back = k * 0.018f
            s.trailHistoryX[k] = s.curX - s.dirX * back
            s.trailHistoryY[k] = s.curY - s.dirY * back
            s.trailHistoryZ[k] = s.curZ - s.dirZ * back
          }
          s.trailInitialized = true
        } else {
          val headDist =
            hypot(
              hypot((s.curX - s.trailHistoryX[0]).toDouble(), (s.curY - s.trailHistoryY[0]).toDouble()),
              (s.curZ - s.trailHistoryZ[0]).toDouble(),
            ).toFloat()
          if (headDist >= 0.020f) {
            for (k in HubTrafficShip.TRAIL_POINTS - 1 downTo 1) {
              s.trailHistoryX[k] = s.trailHistoryX[k - 1]
              s.trailHistoryY[k] = s.trailHistoryY[k - 1]
              s.trailHistoryZ[k] = s.trailHistoryZ[k - 1]
            }
            s.trailHistoryX[0] = s.curX - s.dirX * 0.030f
            s.trailHistoryY[0] = s.curY - s.dirY * 0.030f
            s.trailHistoryZ[0] = s.curZ - s.dirZ * 0.030f
          }
        }
      }
    }

    private fun allocateFramebuffers(w: Int, h: Int) {
      deleteFramebuffers()
      allocatedW = w
      allocatedH = h

      plasmaW = max(2, (w * 0.62f).toInt() and -2)
      plasmaH = max(2, (h * 0.62f).toInt() and -2)
      val plasmaPair = createColorFbo(plasmaW, plasmaH)
      fboPlasma = plasmaPair[0]
      texPlasma = plasmaPair[1]

      val scenePair = createSceneColorDepthFbo(w, h)
      fboScene = scenePair[0]
      texScene = scenePair[1]
      rboSceneDepth = scenePair[2]

      val bw = max(1, w / 2)
      val bh = max(1, h / 2)
      val pairA = createColorFbo(bw, bh)
      fboBloomA = pairA[0]
      texBloomA = pairA[1]

      val pairB = createColorFbo(bw, bh)
      fboBloomB = pairB[0]
      texBloomB = pairB[1]
    }

    private fun createSceneColorDepthFbo(w: Int, h: Int): IntArray {
      val texArr = IntArray(1)
      val fboArr = IntArray(1)
      val rboArr = IntArray(1)
      GLES30.glGenTextures(1, texArr, 0)
      GLES30.glGenFramebuffers(1, fboArr, 0)
      GLES30.glGenRenderbuffers(1, rboArr, 0)

      val tex = texArr[0]
      val fbo = fboArr[0]
      val rbo = rboArr[0]

      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)
      GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
      GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
      GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
      GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

      GLES30.glTexImage2D(
        GLES30.GL_TEXTURE_2D,
        0,
        GLES30.GL_RGBA16F,
        w,
        h,
        0,
        GLES30.GL_RGBA,
        GLES30.GL_HALF_FLOAT,
        null,
      )

      GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, rbo)
      GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT16, w, h)

      GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
      GLES30.glFramebufferTexture2D(
        GLES30.GL_FRAMEBUFFER,
        GLES30.GL_COLOR_ATTACHMENT0,
        GLES30.GL_TEXTURE_2D,
        tex,
        0,
      )
      GLES30.glFramebufferRenderbuffer(
        GLES30.GL_FRAMEBUFFER,
        GLES30.GL_DEPTH_ATTACHMENT,
        GLES30.GL_RENDERBUFFER,
        rbo,
      )
      if (GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER) != GLES30.GL_FRAMEBUFFER_COMPLETE) {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)
        GLES30.glTexImage2D(
          GLES30.GL_TEXTURE_2D,
          0,
          GLES30.GL_RGBA8,
          w,
          h,
          0,
          GLES30.GL_RGBA,
          GLES30.GL_UNSIGNED_BYTE,
          null,
        )
      }
      GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
      GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, 0)
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
      return intArrayOf(fbo, tex, rbo)
    }

    private fun createColorFbo(w: Int, h: Int): IntArray {
      val texArr = IntArray(1)
      val fboArr = IntArray(1)
      GLES30.glGenTextures(1, texArr, 0)
      GLES30.glGenFramebuffers(1, fboArr, 0)
      val tex = texArr[0]
      val fbo = fboArr[0]

      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)
      GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
      GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
      GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
      GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

      GLES30.glTexImage2D(
        GLES30.GL_TEXTURE_2D,
        0,
        GLES30.GL_RGBA16F,
        w,
        h,
        0,
        GLES30.GL_RGBA,
        GLES30.GL_HALF_FLOAT,
        null,
      )
      GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
      GLES30.glFramebufferTexture2D(
        GLES30.GL_FRAMEBUFFER,
        GLES30.GL_COLOR_ATTACHMENT0,
        GLES30.GL_TEXTURE_2D,
        tex,
        0,
      )
      if (GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER) != GLES30.GL_FRAMEBUFFER_COMPLETE) {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)
        GLES30.glTexImage2D(
          GLES30.GL_TEXTURE_2D,
          0,
          GLES30.GL_RGBA8,
          w,
          h,
          0,
          GLES30.GL_RGBA,
          GLES30.GL_UNSIGNED_BYTE,
          null,
        )
      }
      GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
      return intArrayOf(fbo, tex)
    }

    private fun drawLiveShipTraffic3D(basis: CameraBasis3D) {
      if (progShipHull == 0 || progShipTrail == 0 || texShipAtlas == 0 || trafficShips.isEmpty()) return

      var hullFloats = 0
      var trailFloats = 0

      fun pushHullVert(x: Float, y: Float, z: Float, u: Float, v: Float, alpha: Float, boost: Float) {
        if (hullFloats + 7 > shipHullScratch.size) return
        shipHullScratch[hullFloats++] = x
        shipHullScratch[hullFloats++] = y
        shipHullScratch[hullFloats++] = z
        shipHullScratch[hullFloats++] = u
        shipHullScratch[hullFloats++] = v
        shipHullScratch[hullFloats++] = alpha
        shipHullScratch[hullFloats++] = boost
      }

      fun pushTrailVert(x: Float, y: Float, z: Float, r: Float, g: Float, b: Float, a: Float) {
        if (trailFloats + 7 > shipTrailScratch.size) return
        shipTrailScratch[trailFloats++] = x
        shipTrailScratch[trailFloats++] = y
        shipTrailScratch[trailFloats++] = z
        shipTrailScratch[trailFloats++] = r
        shipTrailScratch[trailFloats++] = g
        shipTrailScratch[trailFloats++] = b
        shipTrailScratch[trailFloats++] = a
      }

      for (i in 0 until trafficShips.size) {
        val s = trafficShips[i]
        val p = s.progress.coerceIn(0f, 1f)

        var hullScale = 1f
        var warpFlash = 0f
        if (s.routeType == 0) { // OUTER_TO_BREACH
          if (p < 0.14f) hullScale = 0.28f + 0.72f * (p / 0.14f)
          if (p > 0.80f) {
            val k = ((p - 0.80f) / 0.20f).coerceIn(0f, 1f)
            hullScale = 1f - 0.90f * k
            warpFlash = sin(k * PI.toFloat())
          }
        } else if (s.routeType == 1) { // BREACH_TO_OUTER
          if (p < 0.18f) {
            val k = (p / 0.18f).coerceIn(0f, 1f)
            hullScale = 0.12f + 0.88f * k
            warpFlash = sin(k * PI.toFloat())
          }
          if (p > 0.86f) hullScale = 1f - 0.72f * ((p - 0.86f) / 0.14f)
        } else {
          if (p < 0.12f) hullScale = 0.25f + 0.75f * (p / 0.12f)
          if (p > 0.88f) hullScale = 1f - 0.75f * ((p - 0.88f) / 0.12f)
        }

        var toCamX = basis.camX - s.curX
        var toCamY = basis.camY - s.curY
        var toCamZ = basis.camZ - s.curZ
        val tcLen = sqrt(max(1e-6f, toCamX * toCamX + toCamY * toCamY + toCamZ * toCamZ))
        toCamX /= tcLen; toCamY /= tcLen; toCamZ /= tcLen

        var wx = toCamY * s.dirZ - toCamZ * s.dirY
        var wy = toCamZ * s.dirX - toCamX * s.dirZ
        var wz = toCamX * s.dirY - toCamY * s.dirX
        val wLen = sqrt(max(1e-6f, wx * wx + wy * wy + wz * wz))
        wx /= wLen; wy /= wLen; wz /= wLen

        val dx = s.dirY * wz - s.dirZ * wy
        val dy = s.dirZ * wx - s.dirX * wz
        val dz = s.dirX * wy - s.dirY * wx

        val cb = cos(s.bankRad)
        val sb = sin(s.bankRad)
        val bankedWx = wx * cb + dx * sb
        val bankedWy = wy * cb + dy * sb
        val bankedWz = wz * cb + dz * sb

        val halfSize = 0.072f * hullScale
        val nx = s.dirX * halfSize
        val ny = s.dirY * halfSize
        val nz = s.dirZ * halfSize
        val sx = bankedWx * halfSize
        val sy = bankedWy * halfSize
        val sz = bankedWz * halfSize

        val col = (s.shipId - 1) % 5
        val row = (s.shipId - 1) / 5
        val u0 = (col + 0.012f) / 5f
        val u1 = (col + 0.988f) / 5f
        val v0 = (row + 0.012f) / 2f
        val v1 = (row + 0.988f) / 2f

        val stX = s.curX - nx + sx; val stY = s.curY - ny + sy; val stZ = s.curZ - nz + sz
        val ntX = s.curX + nx + sx; val ntY = s.curY + ny + sy; val ntZ = s.curZ + nz + sz
        val nbX = s.curX + nx - sx; val nbY = s.curY + ny - sy; val nbZ = s.curZ + nz - sz
        val sbX = s.curX - nx - sx; val sbY = s.curY - ny - sy; val sbZ = s.curZ - nz - sz

        val boostGlow = 1.0f + warpFlash * 2.2f
        pushHullVert(stX, stY, stZ, u0, v0, 1f, boostGlow)
        pushHullVert(ntX, ntY, ntZ, u1, v0, 1f, boostGlow)
        pushHullVert(nbX, nbY, nbZ, u1, v1, 1f, boostGlow)
        pushHullVert(stX, stY, stZ, u0, v0, 1f, boostGlow)
        pushHullVert(nbX, nbY, nbZ, u1, v1, 1f, boostGlow)
        pushHullVert(sbX, sbY, sbZ, u0, v1, 1f, boostGlow)

        val isGoldTrail = (s.shipId == 4 || s.shipId == 7 || s.shipId == 10)
        val trR = if (isGoldTrail) 0.98f else 0.00f
        val trG = if (isGoldTrail) 0.80f else 0.96f
        val trB = if (isGoldTrail) 0.14f else 0.85f

        var prevLx = s.curX - nx * 0.72f + wx * (0.017f * hullScale)
        var prevLy = s.curY - ny * 0.72f + wy * (0.017f * hullScale)
        var prevLz = s.curZ - nz * 0.72f + wz * (0.017f * hullScale)
        var prevRx = s.curX - nx * 0.72f - wx * (0.017f * hullScale)
        var prevRy = s.curY - ny * 0.72f - wy * (0.017f * hullScale)
        var prevRz = s.curZ - nz * 0.72f - wz * (0.017f * hullScale)
        var prevAlpha = 0.90f * hullScale

        for (k in 0 until HubTrafficShip.TRAIL_POINTS) {
          val frac = 1f - (k + 1).toFloat() / HubTrafficShip.TRAIL_POINTS.toFloat()
          val wHalf = 0.016f * frac * hullScale
          val curAlpha = 0.70f * frac * frac * hullScale
          val tx = s.trailHistoryX[k]
          val ty = s.trailHistoryY[k]
          val tz = s.trailHistoryZ[k]
          val curLx = tx + wx * wHalf; val curLy = ty + wy * wHalf; val curLz = tz + wz * wHalf
          val curRx = tx - wx * wHalf; val curRy = ty - wy * wHalf; val curRz = tz - wz * wHalf

          pushTrailVert(prevLx, prevLy, prevLz, trR, trG, trB, prevAlpha)
          pushTrailVert(prevRx, prevRy, prevRz, trR, trG, trB, prevAlpha)
          pushTrailVert(curRx, curRy, curRz, trR * 0.6f, trG * 0.8f, trB, curAlpha)
          pushTrailVert(prevLx, prevLy, prevLz, trR, trG, trB, prevAlpha)
          pushTrailVert(curRx, curRy, curRz, trR * 0.6f, trG * 0.8f, trB, curAlpha)
          pushTrailVert(curLx, curLy, curLz, trR * 0.6f, trG * 0.8f, trB, curAlpha)

          prevLx = curLx; prevLy = curLy; prevLz = curLz
          prevRx = curRx; prevRy = curRy; prevRz = curRz
          prevAlpha = curAlpha
        }

        if (warpFlash > 0.04f) {
          val flashR = (0.058f + 0.082f * warpFlash)
          val fx = basis.rightX * flashR
          val fy = basis.rightY * flashR
          val fz = basis.rightZ * flashR
          val ux = basis.upX * flashR
          val uy = basis.upY * flashR
          val uz = basis.upZ * flashR
          val fa = warpFlash * 1.40f
          val pTopX = s.curX + ux; val pTopY = s.curY + uy; val pTopZ = s.curZ + uz
          val pBotX = s.curX - ux; val pBotY = s.curY - uy; val pBotZ = s.curZ - uz
          val pRgtX = s.curX + fx; val pRgtY = s.curY + fy; val pRgtZ = s.curZ + fz
          val pLftX = s.curX - fx; val pLftY = s.curY - fy; val pLftZ = s.curZ - fz
          pushTrailVert(s.curX, s.curY, s.curZ, 1.0f, 1.0f, 1.0f, fa)
          pushTrailVert(pTopX, pTopY, pTopZ, 0.0f, 0.96f, 0.85f, 0f)
          pushTrailVert(pRgtX, pRgtY, pRgtZ, 0.66f, 0.33f, 0.97f, 0f)

          pushTrailVert(s.curX, s.curY, s.curZ, 1.0f, 1.0f, 1.0f, fa)
          pushTrailVert(pRgtX, pRgtY, pRgtZ, 0.66f, 0.33f, 0.97f, 0f)
          pushTrailVert(pBotX, pBotY, pBotZ, 0.0f, 0.96f, 0.85f, 0f)

          pushTrailVert(s.curX, s.curY, s.curZ, 1.0f, 1.0f, 1.0f, fa)
          pushTrailVert(pBotX, pBotY, pBotZ, 0.0f, 0.96f, 0.85f, 0f)
          pushTrailVert(pLftX, pLftY, pLftZ, 0.66f, 0.33f, 0.97f, 0f)

          pushTrailVert(s.curX, s.curY, s.curZ, 1.0f, 1.0f, 1.0f, fa)
          pushTrailVert(pLftX, pLftY, pLftZ, 0.66f, 0.33f, 0.97f, 0f)
          pushTrailVert(pTopX, pTopY, pTopZ, 0.0f, 0.96f, 0.85f, 0f)
        }
      }

      if (hullFloats > 0) {
        shipHullBuffer.clear()
        shipHullBuffer.put(shipHullScratch, 0, hullFloats)
        shipHullBuffer.position(0)

        GLES30.glEnable(GLES30.GL_BLEND)
        // Multiply src by DST_ALPHA (voidKeepMask) so ships diving into the center Rift vanish cleanly into the event horizon!
        GLES30.glBlendFunc(GLES30.GL_DST_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDepthMask(true)

        GLES30.glUseProgram(progShipHull)
        GLES30.glUniform3f(GLES30.glGetUniformLocation(progShipHull, "uCamPos"), basis.camX, basis.camY, basis.camZ)
        GLES30.glUniform3f(GLES30.glGetUniformLocation(progShipHull, "uCamFwd"), basis.fwdX, basis.fwdY, basis.fwdZ)
        GLES30.glUniform3f(GLES30.glGetUniformLocation(progShipHull, "uCamRight"), basis.rightX, basis.rightY, basis.rightZ)
        GLES30.glUniform3f(GLES30.glGetUniformLocation(progShipHull, "uCamUp"), basis.upX, basis.upY, basis.upZ)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(progShipHull, "uTanHalfFovY"), basis.tanHalfFovY)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(progShipHull, "uAspect"), basis.aspect)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texShipAtlas)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(progShipHull, "uAtlasTex"), 0)

        GLES30.glBindVertexArray(vaoShipHull)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboShipHull)
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, hullFloats * 4, shipHullBuffer)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, hullFloats / 7)
      }

      if (trailFloats > 0) {
        shipTrailBuffer.clear()
        shipTrailBuffer.put(shipTrailScratch, 0, trailFloats)
        shipTrailBuffer.position(0)

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_DST_ALPHA, GLES30.GL_ONE)
        GLES30.glDepthMask(false)

        GLES30.glUseProgram(progShipTrail)
        GLES30.glUniform3f(GLES30.glGetUniformLocation(progShipTrail, "uCamPos"), basis.camX, basis.camY, basis.camZ)
        GLES30.glUniform3f(GLES30.glGetUniformLocation(progShipTrail, "uCamFwd"), basis.fwdX, basis.fwdY, basis.fwdZ)
        GLES30.glUniform3f(GLES30.glGetUniformLocation(progShipTrail, "uCamRight"), basis.rightX, basis.rightY, basis.rightZ)
        GLES30.glUniform3f(GLES30.glGetUniformLocation(progShipTrail, "uCamUp"), basis.upX, basis.upY, basis.upZ)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(progShipTrail, "uTanHalfFovY"), basis.tanHalfFovY)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(progShipTrail, "uAspect"), basis.aspect)

        GLES30.glBindVertexArray(vaoShipTrail)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboShipTrail)
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, trailFloats * 4, shipTrailBuffer)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, trailFloats / 7)
      }

      GLES30.glDepthMask(true)
      GLES30.glDisable(GLES30.GL_BLEND)
      GLES30.glBindVertexArray(0)
      GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    private fun drawFrame(w: Int, h: Int, timeSec: Float) {
      val basis =
        buildCameraBasis3D(
          viewportW = w.toFloat(),
          viewportH = h.toFloat(),
          timeSec = timeSec,
          lookYawRad = currentLookYawRad,
          lookPitchRad = currentLookPitchRad,
          diveTearIdx = activeDiveTearIdx,
          diveProgress = activeDiveProgress,
          isSplashMode = isSplashMode,
        )

      val centerSpec = NEXUS_3D_RIFT_TEARS[0]
      val riftHalfR = if (isSplashMode) 0.88f else centerSpec.halfWidthWorld
      val isFocused = (!isSplashMode && (focusedTearIndex == 0 || activeDiveTearIdx >= 0))
      tearPosSizeBuf[0] = centerSpec.worldX
      tearPosSizeBuf[1] = if (isSplashMode) 0.0f else centerSpec.worldY
      tearPosSizeBuf[2] = riftHalfR
      tearPosSizeBuf[3] = riftHalfR

      tearStyleBuf[0] = centerSpec.worldZ
      tearStyleBuf[1] = 0.0f
      tearStyleBuf[2] = 0.0f
      tearStyleBuf[3] = if (isFocused) 1.24f else 1.0f

      tearColorABuf[0] = 0.00f; tearColorABuf[1] = 0.96f; tearColorABuf[2] = 0.83f // #00F5D4 cyan
      tearColorBBuf[0] = 0.66f; tearColorBBuf[1] = 0.33f; tearColorBBuf[2] = 0.97f // #A855F7 violet

      // PASS 1A-1: 0.62x Split-Res Colossal Swirling Playfield Rift Singularity -> fboPlasma
      GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboPlasma)
      GLES30.glViewport(0, 0, plasmaW, plasmaH)
      GLES30.glColorMask(true, true, true, true)
      GLES30.glDepthMask(false)
      GLES30.glDisable(GLES30.GL_DEPTH_TEST)
      GLES30.glDisable(GLES30.GL_BLEND)

      GLES30.glBindVertexArray(emptyVao)
      GLES30.glUseProgram(progScene)
      GLES30.glUniform2f(GLES30.glGetUniformLocation(progScene, "uResolution"), plasmaW.toFloat(), plasmaH.toFloat())
      GLES30.glUniform1f(GLES30.glGetUniformLocation(progScene, "uTime"), timeSec)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progScene, "uCamPos"), basis.camX, basis.camY, basis.camZ)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progScene, "uCamFwd"), basis.fwdX, basis.fwdY, basis.fwdZ)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progScene, "uCamRight"), basis.rightX, basis.rightY, basis.rightZ)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progScene, "uCamUp"), basis.upX, basis.upY, basis.upZ)
      GLES30.glUniform1f(GLES30.glGetUniformLocation(progScene, "uTanHalfFovY"), basis.tanHalfFovY)
      GLES30.glUniform4fv(GLES30.glGetUniformLocation(progScene, "uTearPosSize"), 1, tearPosSizeBuf, 0)
      GLES30.glUniform4fv(GLES30.glGetUniformLocation(progScene, "uTearStyle"), 1, tearStyleBuf, 0)
      GLES30.glUniform3fv(GLES30.glGetUniformLocation(progScene, "uTearColorA"), 1, tearColorABuf, 0)
      GLES30.glUniform3fv(GLES30.glGetUniformLocation(progScene, "uTearColorB"), 1, tearColorBBuf, 0)
      GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)

      // PASS 1A-2: Full-Res 1.0x Upsample + Crisp Multi-Depth Starlight + Feathered #000000 Throat Mask -> fboScene
      GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboScene)
      GLES30.glViewport(0, 0, w, h)
      GLES30.glColorMask(true, true, true, true)
      GLES30.glDepthMask(true)
      GLES30.glDisable(GLES30.GL_DEPTH_TEST)
      GLES30.glDisable(GLES30.GL_BLEND)
      GLES30.glClearColor(0.008f, 0.020f, 0.043f, 1.0f)
      GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

      GLES30.glUseProgram(progSceneUpsample)
      GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texPlasma)
      GLES30.glUniform1i(GLES30.glGetUniformLocation(progSceneUpsample, "uPlasmaTex"), 0)
      GLES30.glUniform2f(GLES30.glGetUniformLocation(progSceneUpsample, "uResolution"), w.toFloat(), h.toFloat())
      GLES30.glUniform1f(GLES30.glGetUniformLocation(progSceneUpsample, "uTime"), timeSec)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progSceneUpsample, "uCamPos"), basis.camX, basis.camY, basis.camZ)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progSceneUpsample, "uCamFwd"), basis.fwdX, basis.fwdY, basis.fwdZ)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progSceneUpsample, "uCamRight"), basis.rightX, basis.rightY, basis.rightZ)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progSceneUpsample, "uCamUp"), basis.upX, basis.upY, basis.upZ)
      GLES30.glUniform1f(GLES30.glGetUniformLocation(progSceneUpsample, "uTanHalfFovY"), basis.tanHalfFovY)
      GLES30.glUniform4fv(GLES30.glGetUniformLocation(progSceneUpsample, "uTearPosSize"), 1, tearPosSizeBuf, 0)
      GLES30.glUniform4fv(GLES30.glGetUniformLocation(progSceneUpsample, "uTearStyle"), 1, tearStyleBuf, 0)
      GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)

      // PASS 1B: Full-Res 1.0x Live Real-Ship Playfield Traffic Banking Into & Out Of the Rift Breach
      GLES30.glColorMask(true, true, true, false)
      GLES30.glEnable(GLES30.GL_DEPTH_TEST)
      GLES30.glDepthFunc(GLES30.GL_LEQUAL)
      GLES30.glDepthMask(true)

      drawLiveShipTraffic3D(basis)

      GLES30.glDisable(GLES30.GL_DEPTH_TEST)
      GLES30.glColorMask(true, true, true, true)

      val bw = max(1, w / 2)
      val bh = max(1, h / 2)
      GLES30.glBindVertexArray(emptyVao)

      // PASS 2: Half-Res High-Threshold (>1.02) HDR Bright-Pass Extraction -> fboBloomA
      GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboBloomA)
      GLES30.glViewport(0, 0, bw, bh)
      GLES30.glUseProgram(progBright)
      GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texScene)
      GLES30.glUniform1i(GLES30.glGetUniformLocation(progBright, "uSceneTex"), 0)
      GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)

      // PASS 3: Horizontal Separable 9-Tap Gaussian Blur -> fboBloomB
      GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboBloomB)
      GLES30.glViewport(0, 0, bw, bh)
      GLES30.glUseProgram(progBlur)
      GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texBloomA)
      GLES30.glUniform1i(GLES30.glGetUniformLocation(progBlur, "uTex"), 0)
      GLES30.glUniform2f(GLES30.glGetUniformLocation(progBlur, "uDir"), 1.35f / bw.toFloat(), 0f)
      GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)

      // PASS 4: Vertical Separable 9-Tap Gaussian Blur -> fboBloomA
      GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboBloomA)
      GLES30.glViewport(0, 0, bw, bh)
      GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texBloomB)
      GLES30.glUniform2f(GLES30.glGetUniformLocation(progBlur, "uDir"), 0f, 1.35f / bh.toFloat())
      GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)

      // PASS 5: Final ACES Filmic Tonemap + Void-Masked HDR Bloom + Deep-Space Motes -> Default Screen
      GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
      GLES30.glViewport(0, 0, w, h)
      GLES30.glUseProgram(progComposite)
      GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texScene)
      GLES30.glUniform1i(GLES30.glGetUniformLocation(progComposite, "uSceneTex"), 0)
      GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texBloomA)
      GLES30.glUniform1i(GLES30.glGetUniformLocation(progComposite, "uBloomTex"), 1)
      GLES30.glUniform2f(GLES30.glGetUniformLocation(progComposite, "uResolution"), w.toFloat(), h.toFloat())
      GLES30.glUniform1f(GLES30.glGetUniformLocation(progComposite, "uTime"), timeSec)
      GLES30.glUniform1f(
        GLES30.glGetUniformLocation(progComposite, "uDimFactor"),
        dimFactor.coerceIn(0.15f, 1.25f),
      )
      GLES30.glUniform2f(
        GLES30.glGetUniformLocation(progComposite, "uParallax"),
        currentLookYawRad,
        currentLookPitchRad,
      )
      GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)

      GLES30.glBindVertexArray(0)
    }

    private fun compileShader(label: String, type: Int, src: String): Int {
      val s = GLES30.glCreateShader(type)
      if (s == 0) return 0
      GLES30.glShaderSource(s, src)
      GLES30.glCompileShader(s)
      val status = IntArray(1)
      GLES30.glGetShaderiv(s, GLES30.GL_COMPILE_STATUS, status, 0)
      if (status[0] == 0) {
        val log = GLES30.glGetShaderInfoLog(s)
        Log.e("NexusSpaceGlView", "Shader compile failed [$label]: $log")
        GLES30.glDeleteShader(s)
        return 0
      }
      return s
    }

    private fun linkProgram(label: String, vertSrc: String, fragSrc: String): Int {
      val vs = compileShader("$label:VS", GLES30.GL_VERTEX_SHADER, vertSrc)
      if (vs == 0) return 0
      val fs = compileShader("$label:FS", GLES30.GL_FRAGMENT_SHADER, fragSrc)
      if (fs == 0) {
        GLES30.glDeleteShader(vs)
        return 0
      }
      val prog = GLES30.glCreateProgram()
      GLES30.glAttachShader(prog, vs)
      GLES30.glAttachShader(prog, fs)
      GLES30.glLinkProgram(prog)
      GLES30.glDeleteShader(vs)
      GLES30.glDeleteShader(fs)
      val status = IntArray(1)
      GLES30.glGetProgramiv(prog, GLES30.GL_LINK_STATUS, status, 0)
      if (status[0] == 0) {
        val log = GLES30.glGetProgramInfoLog(prog)
        Log.e("NexusSpaceGlView", "Program link failed [$label]: $log")
        GLES30.glDeleteProgram(prog)
        return 0
      }
      return prog
    }

    private fun deleteFramebuffers() {
      val fbos = intArrayOf(fboPlasma, fboScene, fboBloomA, fboBloomB)
      val texs = intArrayOf(texPlasma, texScene, texBloomA, texBloomB)
      val rbos = intArrayOf(rboSceneDepth)
      for (i in fbos.indices) {
        if (fbos[i] != 0) GLES30.glDeleteFramebuffers(1, intArrayOf(fbos[i]), 0)
        if (texs[i] != 0) GLES30.glDeleteTextures(1, intArrayOf(texs[i]), 0)
      }
      if (rbos[0] != 0) GLES30.glDeleteRenderbuffers(1, rbos, 0)
      fboPlasma = 0; texPlasma = 0; plasmaW = 0; plasmaH = 0
      fboScene = 0; texScene = 0; rboSceneDepth = 0
      fboBloomA = 0; texBloomA = 0
      fboBloomB = 0; texBloomB = 0
      allocatedW = 0; allocatedH = 0
    }

    private fun releaseAllGlResources() {
      runCatching {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY && eglContext != EGL14.EGL_NO_CONTEXT) {
          deleteFramebuffers()
          if (texShipAtlas != 0) GLES30.glDeleteTextures(1, intArrayOf(texShipAtlas), 0)
          val vaos = intArrayOf(emptyVao, vaoShipHull, vaoShipTrail)
          for (v in vaos) if (v != 0) GLES30.glDeleteVertexArrays(1, intArrayOf(v), 0)
          val vbos = intArrayOf(vboShipHull, vboShipTrail)
          for (b in vbos) if (b != 0) GLES30.glDeleteBuffers(1, intArrayOf(b), 0)
          val progs = intArrayOf(progScene, progSceneUpsample, progShipHull, progShipTrail, progBright, progBlur, progComposite)
          for (p in progs) if (p != 0) GLES30.glDeleteProgram(p)
        }
      }
      runCatching {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
          EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
          if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(eglDisplay, eglSurface)
          if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(eglDisplay, eglContext)
          EGL14.eglTerminate(eglDisplay)
        }
      }
      eglDisplay = EGL14.EGL_NO_DISPLAY
      eglContext = EGL14.EGL_NO_CONTEXT
      eglSurface = EGL14.EGL_NO_SURFACE
    }
  }
}

private const val FULLSCREEN_TRI_VERT_GLSL =
  """#version 300 es
precision highp float;
precision highp int;
out vec2 vUv;
void main() {
    vec2 pos = vec2((gl_VertexID == 1) ? 3.0 : -1.0, (gl_VertexID == 2) ? 3.0 : -1.0);
    vUv = pos * 0.5 + 0.5;
    gl_Position = vec4(pos, 0.0, 1.0);
}
"""

private const val SHIP_HULL_VERT_GLSL =
  """#version 300 es
precision highp float;
precision highp int;

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aUv;
layout(location = 2) in vec2 aAlphaBoost;

uniform vec3 uCamPos;
uniform vec3 uCamFwd;
uniform vec3 uCamRight;
uniform vec3 uCamUp;
uniform float uTanHalfFovY;
uniform float uAspect;

out vec2 vUv;
out vec2 vAlphaBoost;

void main() {
    vUv = aUv;
    vAlphaBoost = aAlphaBoost;

    vec3 d = aPos - uCamPos;
    float vx = dot(d, uCamRight);
    float vy = dot(d, uCamUp);
    float vz = max(0.05, dot(d, uCamFwd));

    float clipX = vx / (uAspect * uTanHalfFovY);
    float clipY = vy / uTanHalfFovY;
    float zNear = 0.20;
    float zFar = 12.0;
    float clipZ = ((zFar + zNear) * vz - 2.0 * zFar * zNear) / (zFar - zNear);
    gl_Position = vec4(clipX, clipY, clipZ, vz);
}
"""

private const val SHIP_HULL_FRAG_GLSL =
  """#version 300 es
precision highp float;
precision highp int;

in vec2 vUv;
in vec2 vAlphaBoost;

uniform sampler2D uAtlasTex;

out vec4 fragColor;

void main() {
    vec4 texel = texture(uAtlasTex, vUv);
    if (texel.a < 0.06) {
        discard;
    }
    float alpha = texel.a * vAlphaBoost.x;
    vec3 boosted = texel.rgb * (1.06 * vAlphaBoost.y);
    fragColor = vec4(boosted, alpha);
}
"""

private const val SHIP_TRAIL_VERT_GLSL =
  """#version 300 es
precision highp float;
precision highp int;

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec4 aColor;

uniform vec3 uCamPos;
uniform vec3 uCamFwd;
uniform vec3 uCamRight;
uniform vec3 uCamUp;
uniform float uTanHalfFovY;
uniform float uAspect;

out vec4 vColor;

void main() {
    vColor = aColor;

    vec3 d = aPos - uCamPos;
    float vx = dot(d, uCamRight);
    float vy = dot(d, uCamUp);
    float vz = max(0.05, dot(d, uCamFwd));

    float clipX = vx / (uAspect * uTanHalfFovY);
    float clipY = vy / uTanHalfFovY;
    float zNear = 0.20;
    float zFar = 12.0;
    float clipZ = ((zFar + zNear) * vz - 2.0 * zFar * zNear) / (zFar - zNear);
    gl_Position = vec4(clipX, clipY, clipZ, vz);
}
"""

private const val SHIP_TRAIL_FRAG_GLSL =
  """#version 300 es
precision highp float;
precision highp int;

in vec4 vColor;
out vec4 fragColor;

void main() {
    fragColor = vec4(vColor.rgb * vColor.a, vColor.a);
}
"""

private const val NEXUS_VOLUMETRIC_SCENE_FRAG_GLSL =
  """#version 300 es
precision highp float;
precision highp int;

in vec2 vUv;
out vec4 fragColor;

uniform vec2 uResolution;
uniform float uTime;
uniform vec3 uCamPos;
uniform vec3 uCamFwd;
uniform vec3 uCamRight;
uniform vec3 uCamUp;
uniform float uTanHalfFovY;
uniform vec4 uTearPosSize; // (worldX, worldY, halfW, halfH)
uniform vec4 uTearStyle;   // (worldZ, tiltRad, phaseOffset, focusGain)
uniform vec3 uTearColorA;
uniform vec3 uTearColorB;

float noise3(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    float n = i.x + i.y * 57.0 + 113.0 * i.z;
    float a = fract(sin(n + 0.0) * 43758.5453);
    float b = fract(sin(n + 1.0) * 43758.5453);
    float c = fract(sin(n + 57.0) * 43758.5453);
    float d = fract(sin(n + 58.0) * 43758.5453);
    float e = fract(sin(n + 113.0) * 43758.5453);
    float f1 = fract(sin(n + 114.0) * 43758.5453);
    float g = fract(sin(n + 170.0) * 43758.5453);
    float h = fract(sin(n + 171.0) * 43758.5453);
    return mix(
        mix(mix(a, b, f.x), mix(c, d, f.x), f.y),
        mix(mix(e, f1, f.x), mix(g, h, f.x), f.y),
        f.z
    );
}

float fbm4(vec3 p) {
    float v = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
        v += amp * noise3(p);
        p = p * 2.04 + vec3(7.3, -4.9, 5.1);
        amp *= 0.5;
    }
    return v;
}

void main() {
    float aspect = uResolution.x / max(1.0, uResolution.y);
    vec2 ndc = vUv * 2.0 - 1.0;

    vec3 rayDir = normalize(
        uCamFwd +
        ndc.x * aspect * uTanHalfFovY * uCamRight +
        ndc.y * uTanHalfFovY * uCamUp
    );

    // Deep gameplay-honest #02050B dark space baseline
    vec3 col = vec3(0.008, 0.018, 0.044);
    float voidKeepMask = 1.0;

    if (abs(rayDir.z) > 1e-4) {
        float tHit = (uTearStyle.x - uCamPos.z) / rayDir.z;
        if (tHit > 0.1) {
            vec3 hit = uCamPos + rayDir * tHit;
            vec2 rel = hit.xy - uTearPosSize.xy;
            vec2 q = rel / max(vec2(0.08), uTearPosSize.zw);

            float r = length(q);
            vec2 dir = q / max(r, 1e-4);
            float theta = atan(q.y, q.x);
            float safeR = max(r, 0.12);
            float logR = log(safeR);
            float focusGain = uTearStyle.w;

            // Seamless polar FBM tendrils whipping INWARD (+uTime in logR shrinks radius over time!)
            float inwardFlow = fbm4(vec3(dir * 3.1, logR * 2.55 + uTime * 1.68));
            float fineTurb = fbm4(vec3(dir * 6.2, logR * 3.8 + uTime * 2.35 - inwardFlow * 1.6));

            // Feathered #000000 event-horizon throat at dead center (holds PLAY // ENTER THE RIFT over pure black!)
            float innerBlackR = 0.30;
            float outerFeatherR = 0.50 + (inwardFlow - 0.5) * 0.15;
            float coreFade = smoothstep(innerBlackR, outerFeatherR, r);
            voidKeepMask = coreFade;

            if (r > innerBlackR * 0.85) {
                float breathe = 1.0 + 0.03 * sin(uTime * 1.45);
                // High-contrast razor-sharp Playfield-style logarithmic spiral arms separated by dark lanes
                float phaseA = 3.0 * theta + 5.8 * logR + uTime * 1.95 + (inwardFlow - 0.5) * 2.4;
                float phaseB = 4.0 * theta + 4.4 * logR + uTime * 1.42 - (fineTurb - 0.5) * 2.1;
                float phaseC = 2.0 * theta + 6.6 * logR + uTime * 2.45 + inwardFlow * 1.8;

                float ribbonA = pow(0.5 + 0.5 * sin(phaseA), 3.6);
                float ribbonB = pow(0.5 + 0.5 * sin(phaseB), 4.2);
                float ribbonC = pow(0.5 + 0.5 * sin(phaseC), 5.0);

                float radialEnvelope = smoothstep(innerBlackR, 0.54, r) * exp(-max(r - 0.58, 0.0) * 1.65);
                float outerReach = exp(-max(r - 0.92, 0.0) * 1.05);

                vec3 cyanCol = uTearColorA;                  // #00F5D4 bioluminescent cyan
                vec3 skyCol = vec3(0.220, 0.741, 0.973);     // #38BDF8 electric sky
                vec3 violetCol = uTearColorB;                // #A855F7 rift violet
                vec3 magentaCol = vec3(0.925, 0.282, 0.600); // #EC4899 rift magenta
                vec3 goldCol = vec3(0.980, 0.804, 0.082);    // #FACC15 solar-gold shear

                vec3 spiralMix =
                    cyanCol * (ribbonA * 1.42) +
                    violetCol * (ribbonB * 1.18) +
                    magentaCol * (ribbonC * 0.92) +
                    skyCol * (pow(inwardFlow, 2.4) * 0.42);

                // Blazing inner photon accretion lip whipping right around the black throat
                float photonRing = smoothstep(innerBlackR * 0.95, innerBlackR + 0.16, r) * exp(-max(r - 0.52, 0.0) * 5.2);
                vec3 photonTint = mix(cyanCol, vec3(0.96, 0.99, 1.0), 0.52) + goldCol * (ribbonC * 0.38);

                // Fine crackling electric lightning veins along the outer breach rim (r ~ 0.72..1.18)
                float ridge = max(0.0, 1.0 - abs(inwardFlow - fineTurb) * 4.6);
                float rimBand = smoothstep(0.46, 0.76, r) * exp(-max(r - 0.96, 0.0) * 2.8);
                float arcFlash = pow(ridge, 3.8) * rimBand * (0.65 + 0.35 * sin(uTime * 8.5 + dot(dir, vec2(6.1, -5.3))));
                vec3 arcCol = mix(skyCol, vec3(1.0), 0.55) * (arcFlash * 1.85);

                // Subtle ambient outer Ring-Space convergence tendrils extending behind the terminal columns
                float farNebula = pow(0.5 + 0.5 * sin(2.0 * theta + 3.2 * logR + uTime * 0.72 + inwardFlow * 2.2), 2.4);
                vec3 ambientOuter = mix(vec3(0.02, 0.09, 0.20), violetCol * 0.28, farNebula) * outerReach * 0.55;

                col += (spiralMix * radialEnvelope + photonTint * photonRing * (0.82 + 0.95 * ribbonA) + arcCol + ambientOuter) *
                    coreFade * breathe * focusGain;
            }
        }
    }

    float cleanVoidMask = clamp(voidKeepMask, 0.0, 1.0);
    fragColor = vec4(max(col * cleanVoidMask, vec3(0.0)), cleanVoidMask);
}
"""

private const val NEXUS_SCENE_UPSAMPLE_FRAG_GLSL =
  """#version 300 es
precision highp float;
precision highp int;

in vec2 vUv;
out vec4 fragColor;

uniform sampler2D uPlasmaTex;
uniform vec2 uResolution;
uniform float uTime;
uniform vec3 uCamPos;
uniform vec3 uCamFwd;
uniform vec3 uCamRight;
uniform vec3 uCamUp;
uniform float uTanHalfFovY;
uniform vec4 uTearPosSize;
uniform vec4 uTearStyle;

vec2 hash22(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx + p3.yz) * p3.zy);
}

float noise3(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    float n = i.x + i.y * 57.0 + 113.0 * i.z;
    float a = fract(sin(n + 0.0) * 43758.5453);
    float b = fract(sin(n + 1.0) * 43758.5453);
    float c = fract(sin(n + 57.0) * 43758.5453);
    float d = fract(sin(n + 58.0) * 43758.5453);
    float e = fract(sin(n + 113.0) * 43758.5453);
    float f1 = fract(sin(n + 114.0) * 43758.5453);
    float g = fract(sin(n + 170.0) * 43758.5453);
    float h = fract(sin(n + 171.0) * 43758.5453);
    return mix(
        mix(mix(a, b, f.x), mix(c, d, f.x), f.y),
        mix(mix(e, f1, f.x), mix(g, h, f.x), f.y),
        f.z
    );
}

float fbm4(vec3 p) {
    float v = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
        v += amp * noise3(p);
        p = p * 2.04 + vec3(7.3, -4.9, 5.1);
        amp *= 0.5;
    }
    return v;
}

vec3 starFieldLayer(vec2 dirUv, float scale, float densityCut, float t) {
    vec2 gv = fract(dirUv * scale) - 0.5;
    vec2 id = floor(dirUv * scale);
    vec2 rnd = hash22(id);
    if (rnd.x < densityCut) return vec3(0.0);
    vec2 offs = (rnd - 0.5) * 0.68;
    float d = length(gv - offs);
    float twinkle = 0.74 + 0.26 * sin(t * (1.4 + rnd.y * 2.8) + rnd.x * 37.0);
    float core = smoothstep(0.078, 0.0, d) * twinkle;
    vec3 tint = mix(vec3(0.72, 0.88, 1.0), vec3(1.0, 0.91, 0.74), fract(rnd.y * 7.3));
    return tint * core * (0.62 + 0.85 * fract(rnd.x * 13.7));
}

vec3 sampleRingSpaceStars(vec3 rayDir, float t) {
    vec2 sph = rayDir.xy / max(0.32, abs(rayDir.z));
    vec3 stars = vec3(0.0);
    stars += starFieldLayer(sph + vec2(t * 0.0007, 0.0), 36.0, 0.84, t) * 0.98;
    stars += starFieldLayer(sph * 1.34 - vec2(0.0, t * 0.0005), 68.0, 0.87, t) * 0.75;
    stars += starFieldLayer(sph * 1.78 + vec2(4.2, -1.9), 116.0, 0.90, t) * 0.52;
    return stars;
}

void main() {
    vec4 plasmaSample = texture(uPlasmaTex, vUv);
    vec3 col = plasmaSample.rgb;

    float aspect = uResolution.x / max(1.0, uResolution.y);
    vec2 ndc = vUv * 2.0 - 1.0;
    vec3 rayDir = normalize(
        uCamFwd +
        ndc.x * aspect * uTanHalfFovY * uCamRight +
        ndc.y * uTanHalfFovY * uCamUp
    );

    vec3 lensedDir = rayDir;
    float voidKeepMask = 1.0;
    if (abs(rayDir.z) > 1e-4) {
        float tHit = (uTearStyle.x - uCamPos.z) / rayDir.z;
        if (tHit > 0.1) {
            vec3 hit = uCamPos + rayDir * tHit;
            vec2 rel = hit.xy - uTearPosSize.xy;
            vec2 q = rel / max(vec2(0.08), uTearPosSize.zw);
            float r = length(q);
            if (r < 2.4) {
                float pull = exp(-r * r * 0.85) * 0.055;
                lensedDir.xy -= normalize(q + vec2(1e-5)) * pull;
            }
            if (r < 0.85) {
                vec2 dir = q / max(r, 1e-4);
                float inwardFlow = fbm4(vec3(dir * 3.1, log(max(r, 0.12)) * 2.55 + uTime * 1.68));
                float innerBlackR = 0.30;
                float outerFeatherR = 0.50 + (inwardFlow - 0.5) * 0.15;
                voidKeepMask = smoothstep(innerBlackR, outerFeatherR, r);
            }
        }
    }
    lensedDir = normalize(lensedDir);

    vec3 stars = sampleRingSpaceStars(lensedDir, uTime);
    float plasmaLum = dot(col, vec3(0.2126, 0.7152, 0.0722));
    float cleanVoidMask = clamp(min(plasmaSample.a, voidKeepMask), 0.0, 1.0);
    col = col * (cleanVoidMask / max(plasmaSample.a, 1e-4)) +
        stars * (0.48 + 0.36 * clamp(1.0 - plasmaLum * 0.55, 0.20, 1.0)) * cleanVoidMask;

    fragColor = vec4(max(col, vec3(0.0)), cleanVoidMask);
}
"""

private const val BLOOM_BRIGHT_EXTRACT_FRAG_GLSL =
  """#version 300 es
precision highp float;
precision highp int;
in vec2 vUv;
out vec4 fragColor;
uniform sampler2D uSceneTex;

void main() {
    vec4 s = texture(uSceneTex, vUv);
    float maxCh = max(s.r, max(s.g, s.b));
    float knee = smoothstep(1.02, 2.10, maxCh);
    fragColor = vec4(s.rgb * knee * s.a, 1.0);
}
"""

private const val BLOOM_GAUSSIAN_BLUR_FRAG_GLSL =
  """#version 300 es
precision highp float;
precision highp int;
in vec2 vUv;
out vec4 fragColor;
uniform sampler2D uTex;
uniform vec2 uDir;

void main() {
    vec3 sum = texture(uTex, vUv).rgb * 0.227027;
    sum += texture(uTex, vUv + uDir * 1.384615).rgb * 0.316216;
    sum += texture(uTex, vUv - uDir * 1.384615).rgb * 0.316216;
    sum += texture(uTex, vUv + uDir * 3.230769).rgb * 0.070270;
    sum += texture(uTex, vUv - uDir * 3.230769).rgb * 0.070270;
    fragColor = vec4(sum, 1.0);
}
"""

private const val FINAL_ACES_COMPOSITE_FRAG_GLSL =
  """#version 300 es
precision highp float;
precision highp int;
in vec2 vUv;
out vec4 fragColor;

uniform sampler2D uSceneTex;
uniform sampler2D uBloomTex;
uniform vec2 uResolution;
uniform float uTime;
uniform float uDimFactor;
uniform vec2 uParallax;

vec2 hash22(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx + p3.yz) * p3.zy);
}

vec3 acesTonemap(vec3 x) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

vec3 foregroundCosmicMotes(vec2 uv, float aspect, float t, vec2 parallax) {
    vec3 acc = vec3(0.0);
    for (int layer = 0; layer < 2; layer++) {
        float fl = float(layer);
        float scale = mix(9.5, 16.0, fl);
        vec2 drift = vec2(t * (0.011 + fl * 0.007), t * (0.008 - fl * 0.004));
        vec2 p = vec2(uv.x * aspect, uv.y) * scale + drift + parallax * (1.2 + fl * 0.8);
        vec2 id = floor(p);
        vec2 gv = fract(p) - 0.5;
        vec2 rnd = hash22(id + fl * 29.0);
        if (rnd.x > 0.85) {
            vec2 pos = (rnd - 0.5) * 0.60;
            float d = length(gv - pos);
            float pulse = 0.55 + 0.45 * sin(t * 2.1 + rnd.y * 25.0);
            float glow = smoothstep(0.082, 0.0, d) * pulse;
            vec3 c = mix(vec3(0.0, 0.96, 0.83), vec3(0.98, 0.80, 0.09), fract(rnd.y * 5.7));
            acc += c * glow * (0.28 - fl * 0.10);
        }
    }
    return acc;
}

void main() {
    vec4 scene = texture(uSceneTex, vUv);
    vec3 bloom = texture(uBloomTex, vUv).rgb;
    float voidKeep = scene.a;

    float aspect = uResolution.x / max(1.0, uResolution.y);
    float centerClear = smoothstep(0.14, 0.30, length((vUv - vec2(0.5)) * vec2(aspect, 1.0)));
    vec3 motes = foregroundCosmicMotes(vUv, aspect, uTime, uParallax) * centerClear * voidKeep;

    vec3 hdr = (scene.rgb + bloom * 0.32 * voidKeep + motes) * voidKeep;
    vec3 mapped = acesTonemap(hdr * 1.14);
    mapped = pow(mapped, vec3(0.95));
    mapped = smoothstep(vec3(0.006), vec3(0.994), mapped);

    vec2 vigUv = vUv * 2.0 - 1.0;
    float vig = clamp(1.0 - dot(vigUv, vigUv) * 0.10, 0.82, 1.0);
    mapped *= mix(1.0, vig, voidKeep);

    fragColor = vec4(mapped * voidKeep * uDimFactor, 1.0);
}
"""
