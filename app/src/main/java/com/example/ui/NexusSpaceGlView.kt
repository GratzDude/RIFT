package com.example.ui

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES30
import android.os.Build
import android.os.SystemClock
import android.view.TextureView
import androidx.compose.ui.graphics.Color
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

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
    RiftTearWorldSpec(
      index = 0,
      node = NexusCoreNode.SUBSPACE_COMMS,
      title = "NEXUS",
      subtitle = "COMMS & AI",
      worldX = 0.00f,
      worldY = -0.04f,
      worldZ = 0.06f,
      halfWidthWorld = 0.70f,
      halfHeightWorld = 0.29f,
      tiltRad = -0.04f,
      lipColorA = Color(0xFF00F5D4),
      lipColorB = Color(0xFFA855F7),
      accentColor = Color(0xFF00F5D4),
      phaseOffset = 0.0f,
      testTag = "nexus_node_subspace_comms",
    ),
    RiftTearWorldSpec(
      index = 1,
      node = NexusCoreNode.CONVERGENCE_GATE,
      title = "CONVERGENCE GATE",
      subtitle = "6 Ranked + Custom Rifts",
      worldX = 0.00f,
      worldY = 0.72f,
      worldZ = -0.14f,
      halfWidthWorld = 0.62f,
      halfHeightWorld = 0.24f,
      tiltRad = 0.03f,
      lipColorA = Color(0xFF00F5D4),
      lipColorB = Color(0xFF0EA5E9),
      accentColor = Color(0xFF00F5D4),
      phaseOffset = 0.85f,
      testTag = "nexus_node_convergence_gate",
    ),
    RiftTearWorldSpec(
      index = 2,
      node = NexusCoreNode.HANGAR_DECK,
      title = "HANGAR DECK",
      subtitle = "10 Hulls • Shipwright",
      worldX = -1.46f,
      worldY = 0.44f,
      worldZ = 0.22f,
      halfWidthWorld = 0.55f,
      halfHeightWorld = 0.22f,
      tiltRad = -0.09f,
      lipColorA = Color(0xFF38BDF8),
      lipColorB = Color(0xFF6366F1),
      accentColor = Color(0xFF38BDF8),
      phaseOffset = 1.70f,
      testTag = "nexus_node_hangar_deck",
    ),
    RiftTearWorldSpec(
      index = 3,
      node = NexusCoreNode.STAR_FORGE,
      title = "STAR FORGE",
      subtitle = "1:1 Playfield Studio",
      worldX = -1.54f,
      worldY = -0.32f,
      worldZ = -0.22f,
      halfWidthWorld = 0.58f,
      halfHeightWorld = 0.23f,
      tiltRad = 0.08f,
      lipColorA = Color(0xFFC084FC),
      lipColorB = Color(0xFFEC4899),
      accentColor = Color(0xFFC084FC),
      phaseOffset = 2.55f,
      testTag = "nexus_node_star_forge",
    ),
    RiftTearWorldSpec(
      index = 4,
      node = NexusCoreNode.RIFT_CONDUIT,
      title = "RIFT CONDUIT",
      subtitle = "Resonance & Directives",
      worldX = -0.74f,
      worldY = -0.88f,
      worldZ = 0.14f,
      halfWidthWorld = 0.55f,
      halfHeightWorld = 0.21f,
      tiltRad = -0.06f,
      lipColorA = Color(0xFFE879F9),
      lipColorB = Color(0xFF8B5CF6),
      accentColor = Color(0xFFE879F9),
      phaseOffset = 3.40f,
      testTag = "nexus_node_rift_conduit",
    ),
    RiftTearWorldSpec(
      index = 5,
      node = NexusCoreNode.SYNDICATE_CITADEL,
      title = "SYNDICATE CITADEL",
      subtitle = "Clans & Strike Wings",
      worldX = 1.46f,
      worldY = 0.44f,
      worldZ = 0.18f,
      halfWidthWorld = 0.57f,
      halfHeightWorld = 0.22f,
      tiltRad = 0.09f,
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
      worldX = 1.54f,
      worldY = -0.32f,
      worldZ = -0.26f,
      halfWidthWorld = 0.59f,
      halfHeightWorld = 0.23f,
      tiltRad = -0.08f,
      lipColorA = Color(0xFF34D399),
      lipColorB = Color(0xFF06B6D4),
      accentColor = Color(0xFF34D399),
      phaseOffset = 5.10f,
      testTag = "nexus_node_archive_codex",
    ),
    RiftTearWorldSpec(
      index = 7,
      node = NexusCoreNode.HELM_SYSTEMS,
      title = "HELM SYSTEMS",
      subtitle = "Audio, FOV & Controls",
      worldX = 0.74f,
      worldY = -0.88f,
      worldZ = 0.10f,
      halfWidthWorld = 0.55f,
      halfHeightWorld = 0.21f,
      tiltRad = 0.06f,
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
  return fp.contains("robolectric", ignoreCase = true) ||
    model.contains("robolectric", ignoreCase = true)
}

private const val CAMERA_FOV_Y_DEG = 46.0f
private const val BASE_CAMERA_Z = 3.35f

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
  val aspectPullback = if (isSplashMode) 1.0f else max(1.0f, 1.68f / aspect.coerceAtLeast(0.55f))
  val baseZ = BASE_CAMERA_Z * aspectPullback

  val orbitX = sin(timeSec * 0.34f) * 0.075f + cos(timeSec * 0.19f) * 0.03f
  val orbitY = cos(timeSec * 0.28f) * 0.052f
  val orbitZ = sin(timeSec * 0.22f) * 0.06f

  var camX = orbitX + sin(lookYawRad) * 0.42f
  var camY = orbitY + sin(lookPitchRad) * 0.32f
  var camZ = baseZ + orbitZ

  var lookX = -sin(lookYawRad) * 0.55f
  var lookY = -sin(lookPitchRad) * 0.44f
  var lookZ = 0.0f

  if (!isSplashMode && diveTearIdx in NEXUS_3D_RIFT_TEARS.indices && diveProgress > 0.001f) {
    val target = NEXUS_3D_RIFT_TEARS[diveTearIdx]
    val ease = diveProgress * diveProgress * (3f - 2f * diveProgress)
    camX += (target.worldX * 0.86f - camX) * (ease * 0.78f)
    camY += (target.worldY * 0.86f - camY) * (ease * 0.78f)
    camZ += ((target.worldZ + 0.62f) - camZ) * (ease * 0.82f)
    lookX += (target.worldX - lookX) * ease
    lookY += (target.worldY - lookY) * ease
    lookZ += (target.worldZ - lookZ) * ease
  }

  var fx = lookX - camX
  var fy = lookY - camY
  var fz = lookZ - camZ
  val fLen = sqrt(max(1e-6f, fx * fx + fy * fy + fz * fz))
  fx /= fLen
  fy /= fLen
  fz /= fLen

  // right = normalize(cross(fwd, worldUp(0, 1, 0))) = (-fz, 0, fx)
  var rx = -fz
  var ry = 0f
  var rz = fx
  val rLen = sqrt(max(1e-6f, rx * rx + ry * ry + rz * rz))
  rx /= rLen
  ry /= rLen
  rz /= rLen

  // up = cross(right, fwd)
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
  timeSec: Float,
): List<ProjectedRiftTearScreenState> {
  val safeW = max(1f, viewportW)
  val safeH = max(1f, viewportH)
  return NEXUS_3D_RIFT_TEARS.map { spec ->
    val bobY = sin(timeSec * 1.15f + spec.phaseOffset) * 0.018f
    val wx = spec.worldX
    val wy = spec.worldY + bobY
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
  timeSec: Float = 0f,
): List<ProjectedRiftTearScreenState> {
  val basis =
    buildCameraBasis3D(
      viewportW = viewportW,
      viewportH = viewportH,
      timeSec = timeSec,
      lookYawRad = 0f,
      lookPitchRad = 0f,
      diveTearIdx = -1,
      diveProgress = 0f,
      isSplashMode = false,
    )
  return projectTearsWithBasis(basis, viewportW, viewportH, timeSec)
}

class NexusSpaceGlView(context: Context) : TextureView(context), TextureView.SurfaceTextureListener {

  @Volatile var isSplashMode: Boolean = false
  @Volatile var dimFactor: Float = 1.0f
  @Volatile var focusedTearIndex: Int = -1

  private val stateLock = Any()
  private var targetYawRad: Float = 0f
  private var targetPitchRad: Float = 0f
  private var currentYawRad: Float = 0f
  private var currentPitchRad: Float = 0f
  private var activeDiveTearIdx: Int = -1
  private var diveProgress: Float = 0f
  private var diveTarget: Float = 0f
  private var animClockSec: Float = 0f

  private var renderThread: GlRenderThread? = null

  init {
    isOpaque = true
    if (!isHeadlessGlEnvironment()) {
      surfaceTextureListener = this
    }
  }

  fun addCameraLookDelta(dxPx: Float, dyPx: Float) {
    val normScale = 1f / max(320f, width.toFloat())
    synchronized(stateLock) {
      targetYawRad = (targetYawRad - dxPx * normScale * 0.68f).coerceIn(-0.26f, 0.26f)
      targetPitchRad = (targetPitchRad + dyPx * normScale * 0.48f).coerceIn(-0.16f, 0.16f)
    }
  }

  fun releaseCameraLookSpring() {
    synchronized(stateLock) {
      targetYawRad = 0f
      targetPitchRad = 0f
    }
  }

  fun triggerDiveIntoTear(tearIndex: Int) {
    synchronized(stateLock) {
      activeDiveTearIdx = tearIndex
      diveTarget = 1.0f
    }
  }

  fun resetCameraDive() {
    synchronized(stateLock) {
      diveTarget = 0.0f
      if (diveProgress < 0.02f) {
        diveProgress = 0f
        activeDiveTearIdx = -1
      }
    }
  }

  fun computeProjectedTearStates(viewportW: Float, viewportH: Float): List<ProjectedRiftTearScreenState> {
    val yaw: Float
    val pitch: Float
    val diveIdx: Int
    val diveProg: Float
    val clock: Float
    synchronized(stateLock) {
      yaw = currentYawRad
      pitch = currentPitchRad
      diveIdx = activeDiveTearIdx
      diveProg = diveProgress
      clock = animClockSec
    }
    val basis =
      buildCameraBasis3D(
        viewportW = viewportW,
        viewportH = viewportH,
        timeSec = clock,
        lookYawRad = yaw,
        lookPitchRad = pitch,
        diveTearIdx = diveIdx,
        diveProgress = diveProg,
        isSplashMode = isSplashMode,
      )
    return projectTearsWithBasis(basis, viewportW, viewportH, clock)
  }

  internal fun stepSimulationAndSnapshot(
    dtSec: Float,
    viewportW: Float,
    viewportH: Float,
  ): Pair<CameraBasis3D, Float> {
    val clampedDt = dtSec.coerceIn(0.001f, 0.05f)
    val yaw: Float
    val pitch: Float
    val diveIdx: Int
    val diveProg: Float
    val clock: Float
    synchronized(stateLock) {
      animClockSec += clampedDt
      // Gentle spring return toward neutral when user isn't dragging
      targetYawRad *= max(0f, 1f - clampedDt * 1.35f)
      targetPitchRad *= max(0f, 1f - clampedDt * 1.35f)
      val lerpLook = min(1f, clampedDt * 9.5f)
      currentYawRad += (targetYawRad - currentYawRad) * lerpLook
      currentPitchRad += (targetPitchRad - currentPitchRad) * lerpLook

      val diveSpeed = if (diveTarget > diveProgress) 4.4f else 3.6f
      diveProgress += (diveTarget - diveProgress) * min(1f, clampedDt * diveSpeed)
      if (diveTarget == 0f && diveProgress < 0.008f) {
        diveProgress = 0f
        activeDiveTearIdx = -1
      }
      yaw = currentYawRad
      pitch = currentPitchRad
      diveIdx = activeDiveTearIdx
      diveProg = diveProgress
      clock = animClockSec
    }
    val basis =
      buildCameraBasis3D(
        viewportW = viewportW,
        viewportH = viewportH,
        timeSec = clock,
        lookYawRad = yaw,
        lookPitchRad = pitch,
        diveTearIdx = diveIdx,
        diveProgress = diveProg,
        isSplashMode = isSplashMode,
      )
    return basis to clock
  }

  override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
    if (isHeadlessGlEnvironment()) return
    stopRenderThread()
    renderThread =
      GlRenderThread(surface, max(1, width), max(1, height)).also { thread -> thread.start() }
  }

  override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
    renderThread?.onResize(max(1, width), max(1, height))
  }

  override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
    stopRenderThread()
    return true
  }

  override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

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

  private inner class GlRenderThread(
    private val surfaceTexture: SurfaceTexture,
    initialW: Int,
    initialH: Int,
  ) : Thread("NexusSpaceGlThread") {

    @Volatile private var running = true
    @Volatile private var pendingW = initialW
    @Volatile private var pendingH = initialH

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    private var sceneProgram = 0
    private var moteProgram = 0
    private var blurProgram = 0
    private var compositeProgram = 0

    private var quadVao = 0
    private var quadVbo = 0
    private var moteVao = 0
    private var moteVbo = 0

    private var fboScene = 0
    private var texScene = 0
    private var fboBloomA = 0
    private var texBloomA = 0
    private var fboBloomB = 0
    private var texBloomB = 0
    private var fboW = 0
    private var fboH = 0
    private var hdrPackScale = 1.0f

    private val moteCount = 260
    private val moteSeeds = FloatArray(moteCount * 4)

    fun onResize(w: Int, h: Int) {
      pendingW = w
      pendingH = h
    }

    fun requestStop() {
      running = false
    }

    override fun run() {
      val initialized = runCatching { initEglAndGl() }.getOrDefault(false)
      if (!initialized) {
        runCatching { releaseGlAndEgl() }
        return
      }

      var lastNs = SystemClock.elapsedRealtimeNanos()
      while (running) {
        val nowNs = SystemClock.elapsedRealtimeNanos()
        val dtSec = ((nowNs - lastNs) * 1e-9f).coerceIn(0.001f, 0.05f)
        lastNs = nowNs

        val w = pendingW.coerceAtLeast(1)
        val h = pendingH.coerceAtLeast(1)
        if (w != fboW || h != fboH) {
          runCatching { rebuildFramebuffers(w, h) }
        }

        val drewOk = runCatching { renderFrame(w, h, dtSec) }.getOrDefault(false)
        if (!drewOk) break

        // Cap smoothly around ~60fps on menu/hub surface to leave thermal headroom while silky smooth
        val elapsedMs = (SystemClock.elapsedRealtimeNanos() - nowNs) / 1_000_000L
        val sleepMs = (16L - elapsedMs).coerceIn(1L, 16L)
        try {
          sleep(sleepMs)
        } catch (_: InterruptedException) {
          break
        }
      }

      runCatching { releaseGlAndEgl() }
    }

    private fun initEglAndGl(): Boolean {
      eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
      if (eglDisplay == EGL14.EGL_NO_DISPLAY) return false
      val version = IntArray(2)
      if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) return false

      val eglEs3Bit = 0x0040
      val attribList =
        intArrayOf(
          EGL14.EGL_RED_SIZE, 8,
          EGL14.EGL_GREEN_SIZE, 8,
          EGL14.EGL_BLUE_SIZE, 8,
          EGL14.EGL_ALPHA_SIZE, 8,
          EGL14.EGL_RENDERABLE_TYPE, eglEs3Bit,
          EGL14.EGL_NONE,
        )
      val configs = arrayOfNulls<EGLConfig>(1)
      val numConfigs = IntArray(1)
      if (!EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, 1, numConfigs, 0) || numConfigs[0] <= 0) {
        return false
      }
      val config = configs[0] ?: return false

      val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
      eglContext = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
      if (eglContext == EGL14.EGL_NO_CONTEXT) return false

      val surfAttribs = intArrayOf(EGL14.EGL_NONE)
      eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, config, surfaceTexture, surfAttribs, 0)
      if (eglSurface == EGL14.EGL_NO_SURFACE) return false

      if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) return false

      sceneProgram = buildProgram(FULLSCREEN_VERT_GLSL, NEXUS_VOLUMETRIC_SCENE_FRAG_GLSL)
      moteProgram = buildProgram(MOTE_PARTICLE_VERT_GLSL, MOTE_PARTICLE_FRAG_GLSL)
      blurProgram = buildProgram(FULLSCREEN_VERT_GLSL, BLOOM_BLUR_FRAG_GLSL)
      compositeProgram = buildProgram(FULLSCREEN_VERT_GLSL, FINAL_COMPOSITE_FRAG_GLSL)
      if (sceneProgram == 0 || moteProgram == 0 || blurProgram == 0 || compositeProgram == 0) {
        return false
      }

      setupFullScreenQuad()
      setupMoteParticles()
      rebuildFramebuffers(pendingW.coerceAtLeast(1), pendingH.coerceAtLeast(1))
      return fboScene != 0
    }

    private fun setupFullScreenQuad() {
      val verts =
        floatArrayOf(
          -1f, -1f,
          1f, -1f,
          -1f, 1f,
          1f, 1f,
        )
      val buf = createFloatBuffer(verts)
      val vaos = IntArray(1)
      val vbos = IntArray(1)
      GLES30.glGenVertexArrays(1, vaos, 0)
      GLES30.glGenBuffers(1, vbos, 0)
      quadVao = vaos[0]
      quadVbo = vbos[0]

      GLES30.glBindVertexArray(quadVao)
      GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadVbo)
      GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, verts.size * 4, buf, GLES30.GL_STATIC_DRAW)
      GLES30.glEnableVertexAttribArray(0)
      GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 8, 0)
      GLES30.glBindVertexArray(0)
    }

    private fun setupMoteParticles() {
      for (i in 0 until moteCount) {
        val h1 = (((i * 73 + 17) % 199) / 199f) * 2f - 1f
        val h2 = (((i * 131 + 43) % 193) / 193f) * 2f - 1f
        val h3 = ((i * 59 + 11) % 181) / 181f
        val palette = (i % 4).toFloat() / 3.0f
        moteSeeds[i * 4] = h1 * 3.2f
        moteSeeds[i * 4 + 1] = h2 * 1.9f
        moteSeeds[i * 4 + 2] = h3
        moteSeeds[i * 4 + 3] = palette
      }
      val buf = createFloatBuffer(moteSeeds)
      val vaos = IntArray(1)
      val vbos = IntArray(1)
      GLES30.glGenVertexArrays(1, vaos, 0)
      GLES30.glGenBuffers(1, vbos, 0)
      moteVao = vaos[0]
      moteVbo = vbos[0]

      GLES30.glBindVertexArray(moteVao)
      GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, moteVbo)
      GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, moteSeeds.size * 4, buf, GLES30.GL_STATIC_DRAW)
      GLES30.glEnableVertexAttribArray(0)
      GLES30.glVertexAttribPointer(0, 4, GLES30.GL_FLOAT, false, 16, 0)
      GLES30.glBindVertexArray(0)
    }

    private fun rebuildFramebuffers(w: Int, h: Int) {
      deleteFramebuffers()
      fboW = w
      fboH = h
      val bw = max(1, w / 2)
      val bh = max(1, h / 2)

      // Try true 16-bit half-float HDR FBOs first; automatically fall back to packed RGBA8 HDR if unsupported
      val scene16 = createColorFbo(w, h, useHalfFloat = true)
      val bloomA16 = if (scene16 != null) createColorFbo(bw, bh, useHalfFloat = true) else null
      val bloomB16 = if (bloomA16 != null) createColorFbo(bw, bh, useHalfFloat = true) else null

      if (scene16 != null && bloomA16 != null && bloomB16 != null) {
        fboScene = scene16.first
        texScene = scene16.second
        fboBloomA = bloomA16.first
        texBloomA = bloomA16.second
        fboBloomB = bloomB16.first
        texBloomB = bloomB16.second
        hdrPackScale = 1.0f
      } else {
        scene16?.let { deleteSingleFbo(it.first, it.second) }
        bloomA16?.let { deleteSingleFbo(it.first, it.second) }
        bloomB16?.let { deleteSingleFbo(it.first, it.second) }

        val s8 = createColorFbo(w, h, useHalfFloat = false) ?: return
        val a8 = createColorFbo(bw, bh, useHalfFloat = false) ?: return
        val b8 = createColorFbo(bw, bh, useHalfFloat = false) ?: return
        fboScene = s8.first
        texScene = s8.second
        fboBloomA = a8.first
        texBloomA = a8.second
        fboBloomB = b8.first
        texBloomB = b8.second
        hdrPackScale = 0.35f
      }
      GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    private fun createColorFbo(w: Int, h: Int, useHalfFloat: Boolean): Pair<Int, Int>? {
      val texIds = IntArray(1)
      val fboIds = IntArray(1)
      GLES30.glGenTextures(1, texIds, 0)
      GLES30.glGenFramebuffers(1, fboIds, 0)
      val tex = texIds[0]
      val fbo = fboIds[0]

      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex)
      if (useHalfFloat) {
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
      } else {
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
      GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
      GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
      GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
      GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

      GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
      GLES30.glFramebufferTexture2D(
        GLES30.GL_FRAMEBUFFER,
        GLES30.GL_COLOR_ATTACHMENT0,
        GLES30.GL_TEXTURE_2D,
        tex,
        0,
      )
      val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
      if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
        deleteSingleFbo(fbo, tex)
        return null
      }
      return fbo to tex
    }

    private fun deleteSingleFbo(fbo: Int, tex: Int) {
      if (fbo != 0) GLES30.glDeleteFramebuffers(1, intArrayOf(fbo), 0)
      if (tex != 0) GLES30.glDeleteTextures(1, intArrayOf(tex), 0)
    }

    private fun deleteFramebuffers() {
      deleteSingleFbo(fboScene, texScene)
      deleteSingleFbo(fboBloomA, texBloomA)
      deleteSingleFbo(fboBloomB, texBloomB)
      fboScene = 0
      texScene = 0
      fboBloomA = 0
      texBloomA = 0
      fboBloomB = 0
      texBloomB = 0
    }

    private val posHalfWUniformBuf = FloatArray(8 * 4)
    private val halfHTiltPhaseFocusBuf = FloatArray(8 * 4)
    private val lipColABuf = FloatArray(8 * 4)
    private val lipColBBuf = FloatArray(8 * 4)

    private fun renderFrame(w: Int, h: Int, dtSec: Float): Boolean {
      if (fboScene == 0) return false
      val (basis, clockSec) = stepSimulationAndSnapshot(dtSec, w.toFloat(), h.toFloat())
      val splash = isSplashMode
      val focusedIdx = focusedTearIndex

      val activeCount: Int
      if (splash) {
        activeCount = 1
        val bobY = sin(clockSec * 0.95f) * 0.02f
        posHalfWUniformBuf[0] = 0.0f
        posHalfWUniformBuf[1] = 0.50f + bobY
        posHalfWUniformBuf[2] = -0.12f
        posHalfWUniformBuf[3] = 1.05f

        halfHTiltPhaseFocusBuf[0] = 0.31f
        halfHTiltPhaseFocusBuf[1] = -0.045f
        halfHTiltPhaseFocusBuf[2] = 0.0f
        halfHTiltPhaseFocusBuf[3] = 1.25f

        lipColABuf[0] = 0.0f
        lipColABuf[1] = 0.96f
        lipColABuf[2] = 0.83f
        lipColABuf[3] = 1.0f

        lipColBBuf[0] = 0.66f
        lipColBBuf[1] = 0.33f
        lipColBBuf[2] = 0.97f
        lipColBBuf[3] = 1.0f
      } else {
        activeCount = NEXUS_3D_RIFT_TEARS.size
        for (i in 0 until activeCount) {
          val spec = NEXUS_3D_RIFT_TEARS[i]
          val bobY = sin(clockSec * 1.15f + spec.phaseOffset) * 0.018f
          val base = i * 4
          posHalfWUniformBuf[base] = spec.worldX
          posHalfWUniformBuf[base + 1] = spec.worldY + bobY
          posHalfWUniformBuf[base + 2] = spec.worldZ
          posHalfWUniformBuf[base + 3] = spec.halfWidthWorld

          halfHTiltPhaseFocusBuf[base] = spec.halfHeightWorld
          halfHTiltPhaseFocusBuf[base + 1] = spec.tiltRad
          halfHTiltPhaseFocusBuf[base + 2] = spec.phaseOffset
          halfHTiltPhaseFocusBuf[base + 3] =
            when {
              i == focusedIdx -> 1.45f
              i == 0 -> 1.20f
              else -> 1.0f
            }

          lipColABuf[base] = spec.lipColorA.red
          lipColABuf[base + 1] = spec.lipColorA.green
          lipColABuf[base + 2] = spec.lipColorA.blue
          lipColABuf[base + 3] = 1.0f

          lipColBBuf[base] = spec.lipColorB.red
          lipColBBuf[base + 1] = spec.lipColorB.green
          lipColBBuf[base + 2] = spec.lipColorB.blue
          lipColBBuf[base + 3] = 1.0f
        }
      }

      // PASS 1: Raymarched 3D Volumetric NEXUS Space + Jagged Spacetime Rift Tears into fboScene
      // Alpha channel stores voidKeepMask (1.0 outside tears, 0.0 inside pure #000000 tear cores)
      GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboScene)
      GLES30.glViewport(0, 0, w, h)
      GLES30.glDisable(GLES30.GL_BLEND)
      GLES30.glColorMask(true, true, true, true)

      GLES30.glUseProgram(sceneProgram)
      GLES30.glUniform2f(GLES30.glGetUniformLocation(sceneProgram, "uResolution"), w.toFloat(), h.toFloat())
      GLES30.glUniform1f(GLES30.glGetUniformLocation(sceneProgram, "uTime"), clockSec)
      GLES30.glUniform1f(GLES30.glGetUniformLocation(sceneProgram, "uTanHalfFovY"), basis.tanHalfFovY)
      GLES30.glUniform1f(GLES30.glGetUniformLocation(sceneProgram, "uAspect"), basis.aspect)
      GLES30.glUniform1f(GLES30.glGetUniformLocation(sceneProgram, "uHdrScale"), hdrPackScale)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(sceneProgram, "uCamPos"), basis.camX, basis.camY, basis.camZ)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(sceneProgram, "uCamFwd"), basis.fwdX, basis.fwdY, basis.fwdZ)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(sceneProgram, "uCamRight"), basis.rightX, basis.rightY, basis.rightZ)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(sceneProgram, "uCamUp"), basis.upX, basis.upY, basis.upZ)
      GLES30.glUniform1i(GLES30.glGetUniformLocation(sceneProgram, "uTearCount"), activeCount)
      GLES30.glUniform4fv(GLES30.glGetUniformLocation(sceneProgram, "uTearPosHalfW"), activeCount, posHalfWUniformBuf, 0)
      GLES30.glUniform4fv(
        GLES30.glGetUniformLocation(sceneProgram, "uTearHalfHTiltPhaseFocus"),
        activeCount,
        halfHTiltPhaseFocusBuf,
        0,
      )
      GLES30.glUniform4fv(GLES30.glGetUniformLocation(sceneProgram, "uTearLipColA"), activeCount, lipColABuf, 0)
      GLES30.glUniform4fv(GLES30.glGetUniformLocation(sceneProgram, "uTearLipColB"), activeCount, lipColBBuf, 0)

      GLES30.glBindVertexArray(quadVao)
      GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

      // PASS 1B: 3D Parallax Space Currents (GL_POINTS) masked by destination alpha (fboScene.a)
      // Since fboScene.a == 0.0 inside every Rift Tear's pure #000000 interior, zero particles ever enter a tear!
      GLES30.glEnable(GLES30.GL_BLEND)
      GLES30.glBlendFunc(GLES30.GL_DST_ALPHA, GLES30.GL_ONE)
      GLES30.glColorMask(true, true, true, false)

      GLES30.glUseProgram(moteProgram)
      GLES30.glUniform1f(GLES30.glGetUniformLocation(moteProgram, "uTime"), clockSec)
      GLES30.glUniform1f(GLES30.glGetUniformLocation(moteProgram, "uTanHalfFovY"), basis.tanHalfFovY)
      GLES30.glUniform1f(GLES30.glGetUniformLocation(moteProgram, "uAspect"), basis.aspect)
      GLES30.glUniform1f(GLES30.glGetUniformLocation(moteProgram, "uViewportH"), h.toFloat())
      GLES30.glUniform1f(GLES30.glGetUniformLocation(moteProgram, "uHdrScale"), hdrPackScale)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(moteProgram, "uCamPos"), basis.camX, basis.camY, basis.camZ)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(moteProgram, "uCamFwd"), basis.fwdX, basis.fwdY, basis.fwdZ)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(moteProgram, "uCamRight"), basis.rightX, basis.rightY, basis.rightZ)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(moteProgram, "uCamUp"), basis.upX, basis.upY, basis.upZ)

      GLES30.glBindVertexArray(moteVao)
      GLES30.glDrawArrays(GLES30.GL_POINTS, 0, moteCount)

      GLES30.glDisable(GLES30.GL_BLEND)
      GLES30.glColorMask(true, true, true, true)

      // PASS 2: Half-Res Bright-Pass + Horizontal HDR Gaussian Bloom Blur into fboBloomA
      val bw = max(1, w / 2)
      val bh = max(1, h / 2)
      GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboBloomA)
      GLES30.glViewport(0, 0, bw, bh)
      GLES30.glUseProgram(blurProgram)
      GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texScene)
      GLES30.glUniform1i(GLES30.glGetUniformLocation(blurProgram, "uTex"), 0)
      GLES30.glUniform2f(GLES30.glGetUniformLocation(blurProgram, "uTexelStep"), 1.35f / bw.toFloat(), 0.0f)
      GLES30.glUniform1i(GLES30.glGetUniformLocation(blurProgram, "uBrightPass"), 1)
      GLES30.glUniform1f(GLES30.glGetUniformLocation(blurProgram, "uHdrUnpack"), 1.0f / hdrPackScale)
      GLES30.glUniform1f(GLES30.glGetUniformLocation(blurProgram, "uHdrPack"), hdrPackScale)
      GLES30.glBindVertexArray(quadVao)
      GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

      // PASS 3: Half-Res Vertical HDR Gaussian Bloom Blur into fboBloomB
      GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboBloomB)
      GLES30.glViewport(0, 0, bw, bh)
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texBloomA)
      GLES30.glUniform2f(GLES30.glGetUniformLocation(blurProgram, "uTexelStep"), 0.0f, 1.35f / bh.toFloat())
      GLES30.glUniform1i(GLES30.glGetUniformLocation(blurProgram, "uBrightPass"), 0)
      GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

      // PASS 4: Final ACES Filmic Composite to Default Framebuffer
      // Multiply final tone-mapped RGB by scene.a (voidKeepMask) so every Rift Tear interior stays pure #000000!
      GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
      GLES30.glViewport(0, 0, w, h)
      GLES30.glUseProgram(compositeProgram)
      GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texScene)
      GLES30.glUniform1i(GLES30.glGetUniformLocation(compositeProgram, "uSceneTex"), 0)
      GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texBloomB)
      GLES30.glUniform1i(GLES30.glGetUniformLocation(compositeProgram, "uBloomTex"), 1)
      GLES30.glUniform1f(GLES30.glGetUniformLocation(compositeProgram, "uHdrUnpack"), 1.0f / hdrPackScale)
      GLES30.glUniform1f(GLES30.glGetUniformLocation(compositeProgram, "uDimFactor"), dimFactor.coerceIn(0.15f, 1.0f))

      GLES30.glBindVertexArray(quadVao)
      GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
      GLES30.glBindVertexArray(0)

      return EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    private fun releaseGlAndEgl() {
      if (eglDisplay != EGL14.EGL_NO_DISPLAY && eglContext != EGL14.EGL_NO_CONTEXT) {
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        deleteFramebuffers()
        if (quadVao != 0) GLES30.glDeleteVertexArrays(1, intArrayOf(quadVao), 0)
        if (quadVbo != 0) GLES30.glDeleteBuffers(1, intArrayOf(quadVbo), 0)
        if (moteVao != 0) GLES30.glDeleteVertexArrays(1, intArrayOf(moteVao), 0)
        if (moteVbo != 0) GLES30.glDeleteBuffers(1, intArrayOf(moteVbo), 0)
        if (sceneProgram != 0) GLES30.glDeleteProgram(sceneProgram)
        if (moteProgram != 0) GLES30.glDeleteProgram(moteProgram)
        if (blurProgram != 0) GLES30.glDeleteProgram(blurProgram)
        if (compositeProgram != 0) GLES30.glDeleteProgram(compositeProgram)
      }
      if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
        EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(eglDisplay, eglSurface)
        if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(eglDisplay, eglContext)
        EGL14.eglTerminate(eglDisplay)
      }
      eglDisplay = EGL14.EGL_NO_DISPLAY
      eglSurface = EGL14.EGL_NO_SURFACE
      eglContext = EGL14.EGL_NO_CONTEXT
    }
  }
}

private fun createFloatBuffer(data: FloatArray): FloatBuffer {
  val bb = ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder())
  val fb = bb.asFloatBuffer()
  fb.put(data)
  fb.position(0)
  return fb
}

private fun compileShader(type: Int, src: String): Int {
  val shader = GLES30.glCreateShader(type)
  if (shader == 0) return 0
  GLES30.glShaderSource(shader, src)
  GLES30.glCompileShader(shader)
  val status = IntArray(1)
  GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
  if (status[0] == 0) {
    GLES30.glDeleteShader(shader)
    return 0
  }
  return shader
}

private fun buildProgram(vertSrc: String, fragSrc: String): Int {
  val vs = compileShader(GLES30.GL_VERTEX_SHADER, vertSrc)
  if (vs == 0) return 0
  val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragSrc)
  if (fs == 0) {
    GLES30.glDeleteShader(vs)
    return 0
  }
  val prog = GLES30.glCreateProgram()
  if (prog == 0) {
    GLES30.glDeleteShader(vs)
    GLES30.glDeleteShader(fs)
    return 0
  }
  GLES30.glAttachShader(prog, vs)
  GLES30.glAttachShader(prog, fs)
  GLES30.glLinkProgram(prog)
  GLES30.glDeleteShader(vs)
  GLES30.glDeleteShader(fs)
  val linkStatus = IntArray(1)
  GLES30.glGetProgramiv(prog, GLES30.GL_LINK_STATUS, linkStatus, 0)
  if (linkStatus[0] == 0) {
    GLES30.glDeleteProgram(prog)
    return 0
  }
  return prog
}

private val FULLSCREEN_VERT_GLSL =
  """
  #version 300 es
  layout(location = 0) in vec2 aPos;
  out vec2 vUv;
  void main() {
    vUv = aPos * 0.5 + 0.5;
    gl_Position = vec4(aPos, 0.0, 1.0);
  }
  """
    .trimIndent()

private val NEXUS_VOLUMETRIC_SCENE_FRAG_GLSL =
  """
  #version 300 es
  precision highp float;
  precision highp int;

  in vec2 vUv;
  out vec4 fragColor;

  uniform vec2 uResolution;
  uniform float uTime;
  uniform float uTanHalfFovY;
  uniform float uAspect;
  uniform float uHdrScale;
  uniform vec3 uCamPos;
  uniform vec3 uCamFwd;
  uniform vec3 uCamRight;
  uniform vec3 uCamUp;

  uniform int uTearCount;
  uniform vec4 uTearPosHalfW[8];
  uniform vec4 uTearHalfHTiltPhaseFocus[8];
  uniform vec4 uTearLipColA[8];
  uniform vec4 uTearLipColB[8];

  float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
  }

  float hash13(vec3 p3) {
    p3 = fract(p3 * 0.1031);
    p3 += dot(p3, p3.zyx + 31.32);
    return fract((p3.x + p3.y) * p3.z);
  }

  float noise2(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash12(i);
    float b = hash12(i + vec2(1.0, 0.0));
    float c = hash12(i + vec2(0.0, 1.0));
    float d = hash12(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
  }

  float noise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);
    float n000 = hash13(i);
    float n100 = hash13(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash13(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash13(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash13(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash13(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash13(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash13(i + vec3(1.0, 1.0, 1.0));
    float nx00 = mix(n000, n100, u.x);
    float nx10 = mix(n010, n110, u.x);
    float nx01 = mix(n001, n101, u.x);
    float nx11 = mix(n011, n111, u.x);
    float nxy0 = mix(nx00, nx10, u.y);
    float nxy1 = mix(nx01, nx11, u.y);
    return mix(nxy0, nxy1, u.z);
  }

  float fbm2_4(vec2 p) {
    float v = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
      v += amp * noise2(p);
      p = vec2(p.x * 1.72 - p.y * 1.18, p.x * 1.18 + p.y * 1.72) + vec2(17.3, 29.1);
      amp *= 0.5;
    }
    return v;
  }

  float fbm3_4(vec3 p) {
    float v = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
      v += amp * noise3(p);
      p = p * 2.03 + vec3(13.1, 7.7, 19.4);
      amp *= 0.5;
    }
    return v;
  }

  vec3 sampleDeepStarfield(vec3 rd) {
    vec3 stars = vec3(0.0);
    vec2 skyUv = vec2(atan(rd.x, -rd.z) * 3.183, rd.y * 3.6);
    for (int layer = 0; layer < 3; layer++) {
      float scale = 28.0 + float(layer) * 26.0;
      vec2 grid = skyUv * scale + vec2(float(layer) * 41.7, float(layer) * 19.3);
      vec2 cell = floor(grid);
      vec2 local = fract(grid) - 0.5;
      float rnd = hash12(cell + float(layer) * 73.0);
      if (rnd > 0.90) {
        vec2 jitter = vec2(hash12(cell + 5.1) - 0.5, hash12(cell + 11.7) - 0.5) * 0.65;
        float d = length(local - jitter);
        float twinkle = 0.68 + 0.32 * sin(uTime * (2.2 + rnd * 4.5) + rnd * 31.4);
        float spark = exp(-d * d * 58.0) * twinkle * (rnd - 0.89) * 11.0;
        vec3 sCol = mix(vec3(0.55, 0.90, 1.0), vec3(0.95, 0.78, 1.0), fract(rnd * 17.3));
        stars += sCol * spark;
      }
    }
    return stars;
  }

  vec3 sampleVolumetricSpace(vec3 ro, vec3 rd) {
    vec3 col = vec3(0.006, 0.014, 0.032);
    col += sampleDeepStarfield(rd);

    // 7-step volumetric 3D FBM nebula march through true 3D NEXUS space
    vec3 drift = vec3(uTime * 0.055, -uTime * 0.032, uTime * 0.085);
    float transmittance = 1.0;
    for (int s = 0; s < 7; s++) {
      float tDepth = 1.35 + float(s) * 0.82;
      vec3 pos = ro + rd * tDepth;
      float warp = fbm3_4(pos * 0.62 + drift);
      float density = fbm3_4(pos * 1.18 - drift * 0.75 + vec3(warp * 1.15));
      float filament = pow(max(0.0, 1.0 - abs(density * 2.0 - 1.0)), 3.1);
      float cloud = smoothstep(0.42, 0.86, density) * 0.14 + filament * 0.11;

      vec3 cyanGas = vec3(0.00, 0.88, 0.80);
      vec3 indigoGas = vec3(0.16, 0.32, 0.88);
      vec3 violetGas = vec3(0.58, 0.24, 0.92);
      vec3 goldRim = vec3(0.98, 0.78, 0.22);

      vec3 stepCol = mix(indigoGas, cyanGas, clamp(filament * 1.35, 0.0, 1.0));
      stepCol = mix(stepCol, violetGas, smoothstep(0.48, 0.82, warp));
      stepCol += goldRim * pow(filament, 3.0) * 0.28;

      col += stepCol * cloud * transmittance;
      transmittance *= (1.0 - cloud * 0.42);
    }

    // Sweeping volumetric starbeam shafts through the deep expanse
    float shaftA = exp(-7.5 * pow(rd.x * 0.82 + rd.y * 0.42 - 0.12 * sin(uTime * 0.28), 2.0));
    float shaftB = exp(-9.0 * pow(rd.x * 0.68 - rd.y * 0.52 + 0.14 * cos(uTime * 0.23), 2.0));
    col += vec3(0.00, 0.68, 0.78) * shaftA * 0.11;
    col += vec3(0.42, 0.22, 0.78) * shaftB * 0.09;
    return col;
  }

  void main() {
    vec2 ndc = vUv * 2.0 - 1.0;
    vec3 rd = normalize(
      uCamFwd +
      uCamRight * (ndc.x * uAspect * uTanHalfFovY) +
      uCamUp * (ndc.y * uTanHalfFovY)
    );

    float denom = dot(rd, uCamFwd);
    vec3 rayWarpOffset = vec3(0.0);
    vec3 plasmaAccum = vec3(0.0);
    float voidKeepMask = 1.0;

    if (denom > 0.05) {
      for (int i = 0; i < 8; i++) {
        if (i >= uTearCount) break;

        vec3 tearPos = uTearPosHalfW[i].xyz;
        float halfW = uTearPosHalfW[i].w;
        float halfH = uTearHalfHTiltPhaseFocus[i].x;
        float tilt = uTearHalfHTiltPhaseFocus[i].y;
        float phase = uTearHalfHTiltPhaseFocus[i].z;
        float focusBoost = uTearHalfHTiltPhaseFocus[i].w;

        float vz = dot(tearPos - uCamPos, uCamFwd);
        if (vz <= 0.12) continue;

        float tHit = vz / denom;
        vec3 hitP = uCamPos + rd * tHit;
        vec3 delta = hitP - tearPos;

        float u0 = dot(delta, uCamRight);
        float v0 = dot(delta, uCamUp);
        float ct = cos(tilt);
        float st = sin(tilt);
        float lx = u0 * ct + v0 * st;
        float ly = -u0 * st + v0 * ct;

        // Early bounding-box skip for distant pixels
        if (abs(lx) > halfW * 2.35 || abs(ly) > halfH * 3.8) continue;

        float nx = lx / halfW;
        float absNx = abs(nx);

        // Asymmetric animated 4-octave FBM fault-line fracture on upper & lower wound lips (never an oval/circle!)
        float jagTop = (fbm2_4(vec2(nx * 3.6 + uTime * 0.48 + phase, phase * 1.7 + uTime * 0.22)) - 0.5) * halfH * 0.44;
        float jagBot = (fbm2_4(vec2(nx * 3.8 - uTime * 0.44 + phase * 2.3, phase * 3.1 - uTime * 0.26)) - 0.5) * halfH * 0.44;
        float microFork = (noise2(vec2(nx * 14.0 - uTime * 2.1, phase * 5.0)) - 0.5) * halfH * 0.13;

        float env = pow(max(0.0, 1.0 - nx * nx), 0.56);
        float taperMask = smoothstep(1.06, 0.82, absNx);
        float yTop = halfH * env + (jagTop + microFork) * taperMask;
        float yBot = -halfH * env + (jagBot - microFork) * taperMask;
        float gapMid = 0.5 * (yTop + yBot);
        float gapHalf = max(0.0, 0.5 * (yTop - yBot));

        float sdf;
        if (absNx <= 1.0) {
          sdf = abs(ly - gapMid) - gapHalf;
        } else {
          float dxTip = (absNx - 1.0) * halfW;
          float dyTip = ly - gapMid;
          sdf = length(vec2(dxTip, dyTip * 1.25));
        }

        // Zone 3: Pure #000000 void interior (rgb = 0, voidKeepMask = 0 so zero bloom & zero motes bleed inside!)
        float lipInner = 0.016 * (0.85 + 0.15 * focusBoost);
        float tearCoreMask = smoothstep(-lipInner, -lipInner * 0.18, sdf);
        voidKeepMask = min(voidKeepMask, tearCoreMask);

        // Zone 1: Spacetime gravitational ray pinch & shear pulling background nebula into the fault seam
        if (sdf > -lipInner) {
          float pull = exp(-max(0.0, sdf) * 7.5) * 0.11 * focusBoost;
          vec3 toCenter = normalize(tearPos - hitP + vec3(1e-5));
          vec3 tangent = normalize(cross(uCamFwd, toCenter) + vec3(1e-5));
          rayWarpOffset += (toCenter * pull + tangent * (pull * 0.42));
        }

        // Zone 2: Blazing HDR multicolored swirling plasma wound lips + crackling electric arc lightning
        float outerAuraDist = max(0.0, sdf + lipInner * 0.5);
        float lipCore = exp(-pow(sdf / (0.020 * focusBoost), 2.0));
        float lipSheath = exp(-outerAuraDist * 15.0) * 0.78;
        float colorMix = 0.5 + 0.5 * sin(nx * 4.4 - uTime * 2.3 + phase + jagTop * 12.0);
        vec3 colA = uTearLipColA[i].rgb;
        vec3 colB = uTearLipColB[i].rgb;
        vec3 goldSpark = vec3(1.0, 0.84, 0.25);

        vec3 lipRgb = mix(colA, colB, colorMix);
        if (i == 0) {
          // Primary central NEXUS tear weaves full cyan + violet + magenta + gold plasma spectrum
          float tri = 0.5 + 0.5 * cos(nx * 6.2 + uTime * 1.8);
          lipRgb = mix(lipRgb, vec3(0.95, 0.28, 0.65), tri * 0.42);
          lipRgb = mix(lipRgb, goldSpark, pow(lipCore, 2.2) * 0.45);
        }

        // High-frequency branching electric lightning filaments crawling along the fracture & arcing outward
        vec2 arcUv = vec2(nx * 7.5 + phase * 2.1, (sdf * 22.0));
        float boltFieldA = fbm2_4(arcUv + vec2(uTime * 5.8, -uTime * 3.4));
        float boltFieldB = fbm2_4(arcUv * 1.9 + vec2(-uTime * 7.2, uTime * 4.9));
        float boltLineA = smoothstep(0.055, 0.0, abs(boltFieldA - 0.50)) * exp(-abs(sdf) * 13.5);
        float boltLineB = smoothstep(0.045, 0.0, abs(boltFieldB - 0.50)) * exp(-outerAuraDist * 9.5);
        float flicker = 0.65 + 0.35 * sin(uTime * 23.0 + phase * 7.0 + nx * 11.0);
        float lightning = (boltLineA * 1.55 + boltLineB * 1.10) * flicker * focusBoost;

        vec3 tearHdr =
          lipRgb * (lipSheath * 1.35 + lipCore * 2.35) * focusBoost +
          vec3(0.92, 0.98, 1.0) * (pow(lipCore, 3.0) * 2.10 * focusBoost) +
          mix(colA, vec3(1.0, 0.97, 0.88), 0.62) * (lightning * 2.45);

        plasmaAccum += tearHdr * tearCoreMask;
      }
    }

    vec3 finalRgb = vec3(0.0);
    if (voidKeepMask > 0.001) {
      vec3 warpedRd = normalize(rd + rayWarpOffset);
      vec3 spaceRgb = sampleVolumetricSpace(uCamPos, warpedRd);
      finalRgb = (spaceRgb + plasmaAccum) * voidKeepMask;
    }

    fragColor = vec4(max(vec3(0.0), finalRgb) * uHdrScale, voidKeepMask);
  }
  """
    .trimIndent()

private val MOTE_PARTICLE_VERT_GLSL =
  """
  #version 300 es
  layout(location = 0) in vec4 aSeed;

  uniform float uTime;
  uniform float uTanHalfFovY;
  uniform float uAspect;
  uniform float uViewportH;
  uniform vec3 uCamPos;
  uniform vec3 uCamFwd;
  uniform vec3 uCamRight;
  uniform vec3 uCamUp;

  out float vAlpha;
  out float vPalette;

  void main() {
    float zLoop = fract(aSeed.z - uTime * 0.048);
    float worldZ = mix(-2.4, 2.75, 1.0 - zLoop);
    float swayX = sin(uTime * 0.55 + aSeed.z * 18.0 + aSeed.y * 3.0) * 0.16;
    float swayY = cos(uTime * 0.44 + aSeed.x * 4.0 + aSeed.z * 13.0) * 0.12;
    vec3 worldP = vec3(aSeed.x + swayX, aSeed.y + swayY, worldZ);

    vec3 rel = worldP - uCamPos;
    float vx = dot(rel, uCamRight);
    float vy = dot(rel, uCamUp);
    float vz = dot(rel, uCamFwd);

    vPalette = aSeed.w;
    if (vz <= 0.18) {
      gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
      gl_PointSize = 0.0;
      vAlpha = 0.0;
    } else {
      float ndcX = vx / (vz * uAspect * uTanHalfFovY);
      float ndcY = vy / (vz * uTanHalfFovY);
      gl_Position = vec4(ndcX, ndcY, 0.0, 1.0);
      float depthFade = smoothstep(0.18, 0.85, vz) * smoothstep(5.8, 1.6, vz);
      float pulse = 0.65 + 0.35 * sin(uTime * 2.4 + aSeed.z * 29.0);
      vAlpha = depthFade * pulse * 0.85;
      gl_PointSize = clamp((uViewportH * 0.0105) / vz, 2.0, 11.0);
    }
  }
  """
    .trimIndent()

private val MOTE_PARTICLE_FRAG_GLSL =
  """
  #version 300 es
  precision highp float;

  in float vAlpha;
  in float vPalette;
  uniform float uHdrScale;
  out vec4 fragColor;

  void main() {
    vec2 pc = gl_PointCoord * 2.0 - 1.0;
    float r2 = dot(pc, pc);
    if (r2 > 1.0 || vAlpha <= 0.001) discard;
    float glow = exp(-r2 * 3.6);
    vec3 c0 = vec3(0.00, 0.96, 0.83);
    vec3 c1 = vec3(0.22, 0.74, 0.97);
    vec3 c2 = vec3(0.68, 0.36, 0.98);
    vec3 c3 = vec3(0.98, 0.80, 0.18);
    vec3 col =
      (vPalette < 0.25) ? c0 :
      (vPalette < 0.55) ? c1 :
      (vPalette < 0.85) ? c2 : c3;
    fragColor = vec4(col * glow * vAlpha * 1.35 * uHdrScale, 1.0);
  }
  """
    .trimIndent()

private val BLOOM_BLUR_FRAG_GLSL =
  """
  #version 300 es
  precision highp float;

  in vec2 vUv;
  out vec4 fragColor;

  uniform sampler2D uTex;
  uniform vec2 uTexelStep;
  uniform int uBrightPass;
  uniform float uHdrUnpack;
  uniform float uHdrPack;

  vec3 fetchSample(vec2 uv) {
    vec3 c = texture(uTex, uv).rgb * uHdrUnpack;
    if (uBrightPass == 1) {
      float lum = max(c.r, max(c.g, c.b));
      float knee = smoothstep(0.62, 1.35, lum);
      return c * knee;
    }
    return c;
  }

  void main() {
    vec3 sum = fetchSample(vUv) * 0.227027;
    sum += fetchSample(vUv + uTexelStep * 1.384615) * 0.316216;
    sum += fetchSample(vUv - uTexelStep * 1.384615) * 0.316216;
    sum += fetchSample(vUv + uTexelStep * 3.230769) * 0.070270;
    sum += fetchSample(vUv - uTexelStep * 3.230769) * 0.070270;
    fragColor = vec4(sum * uHdrPack, 1.0);
  }
  """
    .trimIndent()

private val FINAL_COMPOSITE_FRAG_GLSL =
  """
  #version 300 es
  precision highp float;

  in vec2 vUv;
  out vec4 fragColor;

  uniform sampler2D uSceneTex;
  uniform sampler2D uBloomTex;
  uniform float uHdrUnpack;
  uniform float uDimFactor;

  vec3 acesTonemap(vec3 x) {
    float a = 2.51;
    float b = 0.03;
    float c = 2.43;
    float d = 0.59;
    float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
  }

  void main() {
    vec4 sceneSample = texture(uSceneTex, vUv);
    float voidKeepMask = sceneSample.a;
    if (voidKeepMask <= 0.002) {
      // Strict #000000 interior inside every crackling spacetime Rift Tear
      fragColor = vec4(0.0, 0.0, 0.0, 1.0);
      return;
    }
    vec3 sceneHdr = sceneSample.rgb * uHdrUnpack;
    vec3 bloomHdr = texture(uBloomTex, vUv).rgb * uHdrUnpack;
    vec3 combined = (sceneHdr + bloomHdr * 0.88) * uDimFactor;
    vec3 mapped = acesTonemap(combined);
    mapped = pow(mapped, vec3(0.94));
    fragColor = vec4(mapped * voidKeepMask, 1.0);
  }
  """
    .trimIndent()
