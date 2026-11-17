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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * 3D World-Space Node Specification for the OpenGL ES 3.0 `NEXUS` Hub:
 * - Index `0` (`SUBSPACE_COMMS`) anchors the **Central 3D Rotating Jita-Style `NEXUS` Space Station**
 *   (projected to screen space for the diegetic `"NEXUS / COMMS & AI"` tap target, NOT rendered as a Rift).
 * - Indices `1..7` anchor the **7 Surrounding Round (`~1.05 : 1.0`) Outward-Bleeding Playfield-Spirit 3D Rifts**
 *   whose `#000000` event-horizon cores hold crisp projected button typography.
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
    // Index 0: Central 3D Rotating NEXUS Orbital Space Station (SUBSPACE COMMS & NEXUS AI)
    RiftTearWorldSpec(
      index = 0,
      node = NexusCoreNode.SUBSPACE_COMMS,
      title = "NEXUS",
      subtitle = "COMMS & AI",
      worldX = 0.00f,
      worldY = -0.02f,
      worldZ = 0.10f,
      halfWidthWorld = 0.54f,
      halfHeightWorld = 0.44f,
      tiltRad = 0.00f,
      lipColorA = Color(0xFF00F5D4),
      lipColorB = Color(0xFFFACC15),
      accentColor = Color(0xFF00F5D4),
      phaseOffset = 0.0f,
      testTag = "nexus_node_subspace_comms",
    ),
    // Indices 1..7: 7 Surrounding Round (~1.05:1.0) Outward-Bleeding Playfield-Spirit 3D Rifts
    RiftTearWorldSpec(
      index = 1,
      node = NexusCoreNode.CONVERGENCE_GATE,
      title = "CONVERGENCE GATE",
      subtitle = "Ranked & Custom Rifts",
      worldX = -1.68f,
      worldY = 0.68f,
      worldZ = -0.05f,
      halfWidthWorld = 0.42f,
      halfHeightWorld = 0.40f,
      tiltRad = -0.06f,
      lipColorA = Color(0xFF00F5D4),
      lipColorB = Color(0xFF38BDF8),
      accentColor = Color(0xFF00F5D4),
      phaseOffset = 0.85f,
      testTag = "nexus_node_convergence_gate",
    ),
    RiftTearWorldSpec(
      index = 2,
      node = NexusCoreNode.HANGAR_DECK,
      title = "HANGAR DECK",
      subtitle = "10 Hulls & Rift Frame",
      worldX = 0.00f,
      worldY = 0.88f,
      worldZ = -0.08f,
      halfWidthWorld = 0.41f,
      halfHeightWorld = 0.39f,
      tiltRad = 0.04f,
      lipColorA = Color(0xFF38BDF8),
      lipColorB = Color(0xFF00F5D4),
      accentColor = Color(0xFF38BDF8),
      phaseOffset = 1.70f,
      testTag = "nexus_node_hangar_deck",
    ),
    RiftTearWorldSpec(
      index = 3,
      node = NexusCoreNode.STAR_FORGE,
      title = "STAR FORGE",
      subtitle = "1:1 Asset Studio",
      worldX = 1.68f,
      worldY = 0.68f,
      worldZ = -0.05f,
      halfWidthWorld = 0.42f,
      halfHeightWorld = 0.40f,
      tiltRad = 0.06f,
      lipColorA = Color(0xFFC084FC),
      lipColorB = Color(0xFFF472B6),
      accentColor = Color(0xFFC084FC),
      phaseOffset = 2.55f,
      testTag = "nexus_node_star_forge",
    ),
    RiftTearWorldSpec(
      index = 4,
      node = NexusCoreNode.RIFT_CONDUIT,
      title = "RIFT CONDUIT",
      subtitle = "Resonance & Shards",
      worldX = -1.86f,
      worldY = -0.22f,
      worldZ = 0.02f,
      halfWidthWorld = 0.42f,
      halfHeightWorld = 0.40f,
      tiltRad = -0.05f,
      lipColorA = Color(0xFFE879F9),
      lipColorB = Color(0xFF00F5D4),
      accentColor = Color(0xFFE879F9),
      phaseOffset = 3.40f,
      testTag = "nexus_node_rift_conduit",
    ),
    RiftTearWorldSpec(
      index = 5,
      node = NexusCoreNode.SYNDICATE_CITADEL,
      title = "SYNDICATE CITADEL",
      subtitle = "Wars & Strike Wing",
      worldX = 1.86f,
      worldY = -0.22f,
      worldZ = 0.02f,
      halfWidthWorld = 0.42f,
      halfHeightWorld = 0.40f,
      tiltRad = 0.05f,
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
      worldX = -1.06f,
      worldY = -0.92f,
      worldZ = 0.05f,
      halfWidthWorld = 0.40f,
      halfHeightWorld = 0.38f,
      tiltRad = -0.04f,
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
      worldX = 1.06f,
      worldY = -0.92f,
      worldZ = 0.05f,
      halfWidthWorld = 0.40f,
      halfHeightWorld = 0.38f,
      tiltRad = 0.04f,
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
private const val BASE_CAMERA_Z = 3.42f

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
  val aspectPullback = if (isSplashMode) 1.0f else max(1.0f, 1.70f / aspect.coerceAtLeast(0.55f))
  val baseZ = BASE_CAMERA_Z * aspectPullback

  val orbitX = sin(timeSec * 0.30f) * 0.065f + cos(timeSec * 0.17f) * 0.025f
  val orbitY = cos(timeSec * 0.24f) * 0.045f
  val orbitZ = sin(timeSec * 0.20f) * 0.05f

  var camX = orbitX + sin(lookYawRad) * 0.44f
  var camY = orbitY + sin(lookPitchRad) * 0.34f
  var camZ = baseZ + orbitZ

  var lookX = -sin(lookYawRad) * 0.55f
  var lookY = -sin(lookPitchRad) * 0.44f
  var lookZ = 0.0f

  if (!isSplashMode && diveTearIdx in NEXUS_3D_RIFT_TEARS.indices && diveProgress > 0.001f) {
    val target = NEXUS_3D_RIFT_TEARS[diveTearIdx]
    val ease = diveProgress * diveProgress * (3f - 2f * diveProgress)
    camX += (target.worldX * 0.86f - camX) * (ease * 0.78f)
    camY += (target.worldY * 0.86f - camY) * (ease * 0.78f)
    camZ += ((target.worldZ + 0.66f) - camZ) * (ease * 0.82f)
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
  timeSec: Float,
): List<ProjectedRiftTearScreenState> {
  val safeW = max(1f, viewportW)
  val safeH = max(1f, viewportH)
  return NEXUS_3D_RIFT_TEARS.map { spec ->
    // Central station (index 0) rotates smoothly around its axis without vertical bobbing; outer Rifts gently float
    val bobY = if (spec.index == 0) 0f else sin(timeSec * 0.95f + spec.phaseOffset) * 0.012f
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
 * Procedural 3D Jita-Style Central `NEXUS` Orbital Space Station Mesh Builder.
 * Generates 3 articulated sub-meshes (`stride = 11 floats`: `posXYZ, normXYZ, rgb, emissive, winSeed`):
 *  - `part 0`: Central Armored Command Spire + Singularity Reactor Core + 4 Approach Beacon Lanes
 *  - `part 1`: Outer Sovereign Docking Ring (`R = 0.54f`) with 6 Radial Pylons & 4 Glowing Docking Bay Trenches
 *  - `part 2`: Inner Counter-Rotating Ring-Space Gyro Ring (`R = 0.35f`) with Solar-Gold & Cyan Energy Rails
 */
private class StationPartMesh(
  val vertexBuffer: FloatBuffer,
  val vertexCount: Int,
)

private object StationMeshBuilder {
  const val FLOATS_PER_VERTEX = 11

  private class VertSink(initialFloats: Int = 24576) {
    var data = FloatArray(initialFloats)
    var size = 0

    private fun ensure(extra: Int) {
      if (size + extra > data.size) {
        data = data.copyOf(max(data.size * 2, size + extra))
      }
    }

    fun pushVert(
      x: Float,
      y: Float,
      z: Float,
      nx: Float,
      ny: Float,
      nz: Float,
      r: Float,
      g: Float,
      b: Float,
      emissive: Float,
      winSeed: Float,
    ) {
      ensure(FLOATS_PER_VERTEX)
      val i = size
      data[i] = x
      data[i + 1] = y
      data[i + 2] = z
      data[i + 3] = nx
      data[i + 4] = ny
      data[i + 5] = nz
      data[i + 6] = r
      data[i + 7] = g
      data[i + 8] = b
      data[i + 9] = emissive
      data[i + 10] = winSeed
      size += FLOATS_PER_VERTEX
    }

    fun addQuad(
      x0: Float, y0: Float, z0: Float,
      x1: Float, y1: Float, z1: Float,
      x2: Float, y2: Float, z2: Float,
      x3: Float, y3: Float, z3: Float,
      r: Float, g: Float, b: Float,
      emissive: Float = 0f,
      winSeed: Float = 0f,
    ) {
      val ux = x1 - x0
      val uy = y1 - y0
      val uz = z1 - z0
      val vx = x3 - x0
      val vy = y3 - y0
      val vz = z3 - z0
      var nx = uy * vz - uz * vy
      var ny = uz * vx - ux * vz
      var nz = ux * vy - uy * vx
      val len = sqrt(max(1e-8f, nx * nx + ny * ny + nz * nz))
      nx /= len
      ny /= len
      nz /= len
      pushVert(x0, y0, z0, nx, ny, nz, r, g, b, emissive, winSeed)
      pushVert(x1, y1, z1, nx, ny, nz, r, g, b, emissive, winSeed)
      pushVert(x2, y2, z2, nx, ny, nz, r, g, b, emissive, winSeed)
      pushVert(x0, y0, z0, nx, ny, nz, r, g, b, emissive, winSeed)
      pushVert(x2, y2, z2, nx, ny, nz, r, g, b, emissive, winSeed)
      pushVert(x3, y3, z3, nx, ny, nz, r, g, b, emissive, winSeed)
    }

    fun addTri(
      x0: Float, y0: Float, z0: Float,
      x1: Float, y1: Float, z1: Float,
      x2: Float, y2: Float, z2: Float,
      r: Float, g: Float, b: Float,
      emissive: Float = 0f,
      winSeed: Float = 0f,
    ) {
      val ux = x1 - x0
      val uy = y1 - y0
      val uz = z1 - z0
      val vx = x2 - x0
      val vy = y2 - y0
      val vz = z2 - z0
      var nx = uy * vz - uz * vy
      var ny = uz * vx - ux * vz
      var nz = ux * vy - uy * vx
      val len = sqrt(max(1e-8f, nx * nx + ny * ny + nz * nz))
      nx /= len
      ny /= len
      nz /= len
      pushVert(x0, y0, z0, nx, ny, nz, r, g, b, emissive, winSeed)
      pushVert(x1, y1, z1, nx, ny, nz, r, g, b, emissive, winSeed)
      pushVert(x2, y2, z2, nx, ny, nz, r, g, b, emissive, winSeed)
    }

    fun addFrustumY(
      yBot: Float,
      yTop: Float,
      rBot: Float,
      rTop: Float,
      sides: Int,
      r: Float,
      g: Float,
      b: Float,
      emissive: Float = 0f,
      winSeed: Float = 0f,
      capTop: Boolean = true,
      capBot: Boolean = true,
    ) {
      val step = (2.0 * PI / sides).toFloat()
      for (i in 0 until sides) {
        val a0 = i * step
        val a1 = (i + 1) * step
        val c0 = cos(a0)
        val s0 = sin(a0)
        val c1 = cos(a1)
        val s1 = sin(a1)
        addQuad(
          c0 * rBot, yBot, s0 * rBot,
          c1 * rBot, yBot, s1 * rBot,
          c1 * rTop, yTop, s1 * rTop,
          c0 * rTop, yTop, s0 * rTop,
          r, g, b, emissive, if (winSeed > 0f) winSeed + i * 0.37f else 0f,
        )
        if (capTop && rTop > 0.002f) {
          addTri(0f, yTop, 0f, c0 * rTop, yTop, s0 * rTop, c1 * rTop, yTop, s1 * rTop, r * 1.15f, g * 1.15f, b * 1.15f, emissive, 0f)
        }
        if (capBot && rBot > 0.002f) {
          addTri(0f, yBot, 0f, c1 * rBot, yBot, s1 * rBot, c0 * rBot, yBot, s0 * rBot, r * 0.85f, g * 0.85f, b * 0.85f, emissive, 0f)
        }
      }
    }

    fun addBox(
      cx: Float, cy: Float, cz: Float,
      hx: Float, hy: Float, hz: Float,
      yawRad: Float,
      r: Float, g: Float, b: Float,
      emissive: Float = 0f,
      winSeed: Float = 0f,
    ) {
      val cyaw = cos(yawRad)
      val syaw = sin(yawRad)
      fun wx(lx: Float, lz: Float) = cx + lx * cyaw - lz * syaw
      fun wz(lx: Float, lz: Float) = cz + lx * syaw + lz * cyaw

      val xNN = wx(-hx, -hz); val zNN = wz(-hx, -hz)
      val xPN = wx(hx, -hz); val zPN = wz(hx, -hz)
      val xPP = wx(hx, hz); val zPP = wz(hx, hz)
      val xNP = wx(-hx, hz); val zNP = wz(-hx, hz)
      val y0 = cy - hy
      val y1 = cy + hy

      addQuad(xNP, y0, zNP, xPP, y0, zPP, xPP, y1, zPP, xNP, y1, zNP, r, g, b, emissive, winSeed)
      addQuad(xPN, y0, zPN, xNN, y0, zNN, xNN, y1, zNN, xPN, y1, zPN, r, g, b, emissive, winSeed)
      addQuad(xPP, y0, zPP, xPN, y0, zPN, xPN, y1, zPN, xPP, y1, zPP, r, g, b, emissive, winSeed)
      addQuad(xNN, y0, zNN, xNP, y0, zNP, xNP, y1, zNP, xNN, y1, zNN, r, g, b, emissive, winSeed)
      addQuad(xNP, y1, zNP, xPP, y1, zPP, xPN, y1, zPN, xNN, y1, zNN, r * 1.12f, g * 1.12f, b * 1.12f, emissive, 0f)
      addQuad(xNN, y0, zNN, xPN, y0, zPN, xPP, y0, zPP, xNP, y0, zNP, r * 0.85f, g * 0.85f, b * 0.85f, emissive, 0f)
    }

    fun toMesh(): StationPartMesh {
      val buf =
        ByteBuffer.allocateDirect(size * 4)
          .order(ByteOrder.nativeOrder())
          .asFloatBuffer()
      buf.put(data, 0, size)
      buf.position(0)
      return StationPartMesh(buf, size / FLOATS_PER_VERTEX)
    }
  }

  fun buildCommandSpirePart(): StationPartMesh {
    val s = VertSink(20000)
    // Brushed titanium-obsidian armor palette
    val hullR = 0.082f; val hullG = 0.125f; val hullB = 0.205f // #152034
    val trimR = 0.135f; val trimG = 0.205f; val trimB = 0.325f // #223453
    val goldR = 0.98f; val goldG = 0.80f; val goldB = 0.09f     // #FACC15
    val cyanR = 0.00f; val cyanG = 0.96f; val cyanB = 0.83f     // #00F5D4
    val skyR = 0.22f; val skyG = 0.74f; val skyB = 0.97f        // #38BDF8

    // Central Singularity Reactor Heart (glowing cyan-white core suspended between upper & lower reactor shields)
    s.addFrustumY(-0.055f, 0.0f, 0.048f, 0.088f, 10, cyanR, cyanG, cyanB, emissive = 1.65f, capTop = false, capBot = false)
    s.addFrustumY(0.0f, 0.055f, 0.088f, 0.048f, 10, 0.82f, 0.98f, 1.0f, emissive = 1.85f, capTop = false, capBot = false)

    // Upper Citadel Command Tower (tiered octagonal decks with lit micro-windows)
    s.addFrustumY(0.052f, 0.078f, 0.125f, 0.148f, 8, goldR, goldG, goldB, emissive = 0.55f)
    s.addFrustumY(0.078f, 0.185f, 0.142f, 0.112f, 8, hullR, hullG, hullB, winSeed = 1.3f)
    s.addFrustumY(0.185f, 0.204f, 0.122f, 0.122f, 8, cyanR, cyanG, cyanB, emissive = 0.95f)
    s.addFrustumY(0.204f, 0.305f, 0.106f, 0.064f, 8, trimR, trimG, trimB, winSeed = 4.7f)
    s.addFrustumY(0.305f, 0.395f, 0.052f, 0.012f, 6, hullR, hullG, hullB)
    // Crown communications beacon tip
    s.addFrustumY(0.395f, 0.435f, 0.015f, 0.004f, 6, cyanR, cyanG, cyanB, emissive = 1.9f)

    // Lower Armored Keel & Reactor Containment Spire
    s.addFrustumY(-0.078f, -0.052f, 0.148f, 0.125f, 8, goldR, goldG, goldB, emissive = 0.55f)
    s.addFrustumY(-0.195f, -0.078f, 0.104f, 0.142f, 8, hullR, hullG, hullB, winSeed = 8.1f)
    s.addFrustumY(-0.214f, -0.195f, 0.114f, 0.114f, 8, skyR, skyG, skyB, emissive = 0.90f)
    s.addFrustumY(-0.320f, -0.214f, 0.048f, 0.098f, 8, trimR, trimG, trimB, winSeed = 11.4f)
    s.addFrustumY(-0.410f, -0.320f, 0.008f, 0.042f, 6, hullR, hullG, hullB)
    s.addFrustumY(-0.440f, -0.410f, 0.004f, 0.014f, 6, goldR, goldG, goldB, emissive = 1.6f)

    // 4 Sovereign Bastion Buttress Fins around the command core + 4 Approach Beacon Guide Lanes
    for (k in 0 until 4) {
      val ang = k * (PI.toFloat() * 0.5f) + 0.38f
      val ca = cos(ang)
      val sa = sin(ang)
      s.addBox(ca * 0.148f, 0.13f, sa * 0.148f, 0.032f, 0.068f, 0.016f, -ang, trimR, trimG, trimB, winSeed = 15f + k)
      s.addBox(ca * 0.148f, -0.13f, sa * 0.148f, 0.032f, 0.068f, 0.016f, -ang, trimR, trimG, trimB, winSeed = 21f + k)

      // Sequential Jita-style approach lane beacon lights leading into the station perimeter
      for (b in 1..3) {
        val dist = 0.58f + b * 0.095f
        val bx = ca * dist
        val bz = sa * dist
        val by = (if (k % 2 == 0) 0.03f else -0.03f) * b
        val isGold = (b == 1)
        s.addBox(
          bx, by, bz,
          0.009f, 0.009f, 0.009f,
          0f,
          if (isGold) goldR else cyanR,
          if (isGold) goldG else cyanG,
          if (isGold) goldB else cyanB,
          emissive = 2.0f + b * 0.25f,
          winSeed = -1f * b, // Negative winSeed signals sequential approach beacon pulse in shader
        )
      }
    }
    return s.toMesh()
  }

  fun buildOuterDockingRingPart(): StationPartMesh {
    val s = VertSink(36000)
    val hullR = 0.078f; val hullG = 0.122f; val hullB = 0.198f
    val armorR = 0.132f; val armorG = 0.198f; val armorB = 0.315f
    val cyanR = 0.00f; val cyanG = 0.96f; val cyanB = 0.83f
    val goldR = 0.98f; val goldG = 0.80f; val goldB = 0.09f

    val segments = 36
    val rInner = 0.455f
    val rOuter = 0.545f
    val halfH = 0.034f
    val step = (2.0 * PI / segments).toFloat()

    for (i in 0 until segments) {
      val a0 = i * step
      val a1 = (i + 1) * step
      val c0 = cos(a0); val s0 = sin(a0)
      val c1 = cos(a1); val s1 = sin(a1)

      // 4 Recessed Glowing Docking Bay Trenches spaced 90° apart (segments 0, 9, 18, 27)
      val isDockBay = (i % 9 == 0 || i % 9 == 1)
      val segHalfH = if (isDockBay) halfH * 1.28f else halfH
      val topColR = if (isDockBay) armorR else hullR
      val topColG = if (isDockBay) armorG else hullG
      val topColB = if (isDockBay) armorB else hullB

      // Top & bottom armored ring decks
      s.addQuad(
        c0 * rInner, segHalfH, s0 * rInner,
        c0 * rOuter, segHalfH, s0 * rOuter,
        c1 * rOuter, segHalfH, s1 * rOuter,
        c1 * rInner, segHalfH, s1 * rInner,
        topColR, topColG, topColB,
        winSeed = if (i % 2 == 0) 30f + i else 0f,
      )
      s.addQuad(
        c1 * rInner, -segHalfH, s1 * rInner,
        c1 * rOuter, -segHalfH, s1 * rOuter,
        c0 * rOuter, -segHalfH, s0 * rOuter,
        c0 * rInner, -segHalfH, s0 * rInner,
        hullR, hullG, hullB,
      )

      // Outer circumferential wall: glowing gold/cyan docking bay mouth on bay segments, armored hull with windows elsewhere
      if (isDockBay) {
        s.addQuad(
          c0 * rOuter, -segHalfH * 0.65f, s0 * rOuter,
          c1 * rOuter, -segHalfH * 0.65f, s1 * rOuter,
          c1 * rOuter, segHalfH * 0.65f, s1 * rOuter,
          c0 * rOuter, segHalfH * 0.65f, s0 * rOuter,
          goldR, goldG, goldB,
          emissive = 1.75f,
        )
      } else {
        s.addQuad(
          c0 * rOuter, -segHalfH, s0 * rOuter,
          c1 * rOuter, -segHalfH, s1 * rOuter,
          c1 * rOuter, segHalfH, s1 * rOuter,
          c0 * rOuter, segHalfH, s0 * rOuter,
          armorR, armorG, armorB,
          winSeed = 60f + i * 0.7f,
        )
        // Crisp cyan outer plasma trim rail
        s.addQuad(
          c0 * (rOuter + 0.004f), -0.006f, s0 * (rOuter + 0.004f),
          c1 * (rOuter + 0.004f), -0.006f, s1 * (rOuter + 0.004f),
          c1 * (rOuter + 0.004f), 0.006f, s1 * (rOuter + 0.004f),
          c0 * (rOuter + 0.004f), 0.006f, s0 * (rOuter + 0.004f),
          cyanR, cyanG, cyanB,
          emissive = 1.35f,
        )
      }

      // Inner circumferential wall facing the central Reactor Spire (glowing cyan energy track)
      s.addQuad(
        c1 * rInner, -segHalfH, s1 * rInner,
        c0 * rInner, -segHalfH, s0 * rInner,
        c0 * rInner, segHalfH, s0 * rInner,
        c1 * rInner, segHalfH, s1 * rInner,
        hullR, hullG, hullB,
      )
      s.addQuad(
        c1 * (rInner - 0.003f), -0.008f, s1 * (rInner - 0.003f),
        c0 * (rInner - 0.003f), -0.008f, s0 * (rInner - 0.003f),
        c0 * (rInner - 0.003f), 0.008f, s0 * (rInner - 0.003f),
        c1 * (rInner - 0.003f), 0.008f, s1 * (rInner - 0.003f),
        cyanR, cyanG, cyanB,
        emissive = 1.45f,
      )
    }

    // 6 Radial Structural Pylons linking the Outer Sovereign Ring inward toward the central hub collar
    for (p in 0 until 6) {
      val ang = p * (PI.toFloat() / 3f) + 0.26f
      val midR = 0.295f
      val halfLen = 0.165f
      val cx = cos(ang) * midR
      val cz = sin(ang) * midR
      s.addBox(cx, 0f, cz, halfLen, 0.015f, 0.020f, -ang, armorR, armorG, armorB, winSeed = 95f + p)
    }
    return s.toMesh()
  }

  fun buildInnerGyroRingPart(): StationPartMesh {
    val s = VertSink(18000)
    val hullR = 0.105f; val hullG = 0.158f; val hullB = 0.255f
    val goldR = 0.98f; val goldG = 0.80f; val goldB = 0.09f
    val skyR = 0.22f; val skyG = 0.74f; val skyB = 0.97f

    val segments = 28
    val rInner = 0.305f
    val rOuter = 0.358f
    val halfH = 0.020f
    val step = (2.0 * PI / segments).toFloat()

    for (i in 0 until segments) {
      val a0 = i * step
      val a1 = (i + 1) * step
      val c0 = cos(a0); val s0 = sin(a0)
      val c1 = cos(a1); val s1 = sin(a1)

      s.addQuad(
        c0 * rInner, halfH, s0 * rInner,
        c0 * rOuter, halfH, s0 * rOuter,
        c1 * rOuter, halfH, s1 * rOuter,
        c1 * rInner, halfH, s1 * rInner,
        hullR, hullG, hullB,
      )
      s.addQuad(
        c1 * rInner, -halfH, s1 * rInner,
        c1 * rOuter, -halfH, s1 * rOuter,
        c0 * rOuter, -halfH, s0 * rOuter,
        c0 * rInner, -halfH, s0 * rInner,
        hullR, hullG, hullB,
      )
      val isGoldNode = (i % 7 == 0)
      s.addQuad(
        c0 * rOuter, -halfH, s0 * rOuter,
        c1 * rOuter, -halfH, s1 * rOuter,
        c1 * rOuter, halfH, s1 * rOuter,
        c0 * rOuter, halfH, s0 * rOuter,
        if (isGoldNode) goldR else skyR,
        if (isGoldNode) goldG else skyG,
        if (isGoldNode) goldB else skyB,
        emissive = if (isGoldNode) 1.65f else 1.15f,
      )
      s.addQuad(
        c1 * rInner, -halfH, s1 * rInner,
        c0 * rInner, -halfH, s0 * rInner,
        c0 * rInner, halfH, s0 * rInner,
        c1 * rInner, halfH, s1 * rInner,
        goldR, goldG, goldB,
        emissive = 1.25f,
      )
    }
    return s.toMesh()
  }
}

/**
 * Live 3D Jita-Style Ship Traffic Entity flying cubic Bezier corridors between the Central `NEXUS`
 * Space Station docking bays and the 7 surrounding Playfield-Spirit Rifts.
 */
private class HubTrafficShip(
  var shipId: Int = 1,
  var routeType: Int = 0, // 0 = UNDOCK_TO_RIFT, 1 = RIFT_TO_DOCK, 2 = STATION_ORBIT
  var targetRiftIdx: Int = 1,
  var dockBayIdx: Int = 0,
  var progress: Float = 0f,
  var durationSec: Float = 6.8f,
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
 * Hardware-accelerated OpenGL ES 3.0 (`#version 300 es`) Volumetric 3D Space View:
 * - Pass 1A: Deep `#010308` Abyssal Dark Space + 7 Round (`~1.05:1.0`) Outward-Bleeding Playfield-Spirit
 *   3D Singularities with `#000000` event-horizon cores (`fboScene.a = voidKeepMask` + `GL_DEPTH_COMPONENT16`).
 * - Pass 1B: Central 3D Rotating Jita-Style `NEXUS` Orbital Space Station (`GL_TRIANGLES` + `GL_DEPTH_TEST`)
 *   with counter-rotating 3D-tilted rings, lit micro-window command spire, docking bays, and approach beacons.
 * - Pass 1C: Live Real-Ship Playfield Traffic (`max(5, onlineHumanPilots.size)`) using a `GLES30` `5x2`
 *   `128x128` ship sprite atlas, 3D Bezier corridors, glowing ion wakes, and Rift warp flashes.
 * - Pass 2–4: Quarter-resolution HDR bright-pass extraction + 9-tap separable Gaussian Bloom.
 * - Pass 5: ACES Filmic Tonemap + 3D parallax cosmic motes masked strictly outside `#000000` Rift cores.
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

  private var renderThread: GlSpaceRenderThread? = null

  init {
    isOpaque = false
    if (!isHeadlessGlEnvironment()) {
      surfaceTextureListener = this
    }
  }

  /** Updates the live online human pilots' ship IDs (`1..10`) for Jita-style hub ship traffic. */
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
  }

  fun releaseCameraLookSpring() {
    springBackActive = true
  }

  fun triggerTearDive(tearIndex: Int) {
    activeDiveTearIdx = tearIndex
    activeDiveProgress = 0.01f
  }

  fun triggerDiveIntoTear(tearIndex: Int) = triggerTearDive(tearIndex)

  fun clearTearDive() {
    activeDiveTearIdx = -1
    activeDiveProgress = 0f
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
    stopRenderThread()
    renderThread =
      GlSpaceRenderThread(surface, max(1, width), max(1, height)).also { it.start() }
  }

  override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
    renderThread?.updateViewportSize(max(1, width), max(1, height))
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
    private var progStation = 0
    private var progShipHull = 0
    private var progShipTrail = 0
    private var progBright = 0
    private var progBlur = 0
    private var progComposite = 0

    private var fboScene = 0
    private var texScene = 0
    private var rboSceneDepth = 0
    private var fboBloomA = 0
    private var texBloomA = 0
    private var fboBloomB = 0
    private var texBloomB = 0
    private var allocatedW = 0
    private var allocatedH = 0

    // Station VBOs/VAOs
    private var vaoSpire = 0
    private var vboSpire = 0
    private var spireVertCount = 0
    private var vaoOuterRing = 0
    private var vboOuterRing = 0
    private var outerRingVertCount = 0
    private var vaoInnerRing = 0
    private var vboInnerRing = 0
    private var innerRingVertCount = 0

    // Dynamic Ship Hull + Ion Trail streaming buffers + Ship Sprite Atlas
    private var texShipAtlas = 0
    private var bakedCustomShipVersion = -1
    private var vaoShipHull = 0
    private var vboShipHull = 0
    private var vaoShipTrail = 0
    private var vboShipTrail = 0
    private val shipHullScratch = FloatArray(32 * 6 * 7) // max 32 ships * 6 verts * (xyz, uv, alpha, boost)
    private val shipHullBuffer: FloatBuffer =
      ByteBuffer.allocateDirect(shipHullScratch.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private val shipTrailScratch = FloatArray(32 * (HubTrafficShip.TRAIL_POINTS + 2) * 6 * 7) // trails + warp flashes
    private val shipTrailBuffer: FloatBuffer =
      ByteBuffer.allocateDirect(shipTrailScratch.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()

    // Cached uniform buffers for the 7 Rifts (or 3 ambient Rifts in splash mode)
    private val tearPosSizeBuf = FloatArray(8 * 4)
    private val tearStyleBuf = FloatArray(8 * 4)
    private val tearColorABuf = FloatArray(8 * 3)
    private val tearColorBBuf = FloatArray(8 * 3)
    private val modelMat = FloatArray(16)

    // Live Jita-style ship traffic pool
    private val trafficRng = Random(0x5EED63L)
    private val trafficShips = ArrayList<HubTrafficShip>(28)

    fun updateViewportSize(w: Int, h: Int) {
      viewportW = w
      viewportH = h
    }

    fun requestStop() {
      running.set(false)
    }

    override fun run() {
      if (!initEglAndGl()) {
        releaseAllGlResources()
        return
      }

      val startNs = System.nanoTime()
      var prevFrameNs = startNs

      while (running.get()) {
        val frameStartNs = System.nanoTime()
        val dtSec = ((frameStartNs - prevFrameNs) * 1e-9f).coerceIn(0.001f, 0.05f)
        prevFrameNs = frameStartNs
        val elapsedSec = ((frameStartNs - startNs) * 1e-9f)
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

        val w = max(1, viewportW)
        val h = max(1, viewportH)
        if (w != allocatedW || h != allocatedH) {
          allocateFramebuffers(w, h)
        }
        ensureShipAtlasUpToDate()
        stepTrafficSimulation(dtSec, elapsedSec)

        drawFrame(w, h, elapsedSec)
        EGL14.eglSwapBuffers(eglDisplay, eglSurface)

        val frameCostMs = (System.nanoTime() - frameStartNs) / 1_000_000L
        val sleepMs = 16L - frameCostMs
        if (sleepMs > 1L) {
          runCatching { sleep(sleepMs) }
        }
      }

      releaseAllGlResources()
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

        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        emptyVao = vaos[0]

        progScene = linkProgram(FULLSCREEN_TRI_VERT_GLSL, NEXUS_VOLUMETRIC_SCENE_FRAG_GLSL)
        progStation = linkProgram(STATION_VERT_GLSL, STATION_FRAG_GLSL)
        progShipHull = linkProgram(SHIP_HULL_VERT_GLSL, SHIP_HULL_FRAG_GLSL)
        progShipTrail = linkProgram(SHIP_TRAIL_VERT_GLSL, SHIP_TRAIL_FRAG_GLSL)
        progBright = linkProgram(FULLSCREEN_TRI_VERT_GLSL, BLOOM_BRIGHT_EXTRACT_FRAG_GLSL)
        progBlur = linkProgram(FULLSCREEN_TRI_VERT_GLSL, BLOOM_GAUSSIAN_BLUR_FRAG_GLSL)
        progComposite = linkProgram(FULLSCREEN_TRI_VERT_GLSL, FINAL_ACES_COMPOSITE_FRAG_GLSL)

        if (progScene == 0 || progStation == 0 || progShipHull == 0 || progShipTrail == 0 ||
          progBright == 0 || progBlur == 0 || progComposite == 0
        ) {
          return false
        }

        uploadStationMeshes()
        initDynamicShipBuffers()
        true
      }.getOrDefault(false)
    }

    private fun uploadStationMeshes() {
      val spire = StationMeshBuilder.buildCommandSpirePart()
      val outer = StationMeshBuilder.buildOuterDockingRingPart()
      val inner = StationMeshBuilder.buildInnerGyroRingPart()

      val vaos = IntArray(3)
      val vbos = IntArray(3)
      GLES30.glGenVertexArrays(3, vaos, 0)
      GLES30.glGenBuffers(3, vbos, 0)

      fun setupPart(vao: Int, vbo: Int, part: StationPartMesh) {
        val strideBytes = StationMeshBuilder.FLOATS_PER_VERTEX * 4
        GLES30.glBindVertexArray(vao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(
          GLES30.GL_ARRAY_BUFFER,
          part.vertexCount * strideBytes,
          part.vertexBuffer,
          GLES30.GL_STATIC_DRAW,
        )
        // layout 0: vec3 aPos
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, strideBytes, 0)
        // layout 1: vec3 aNormal
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, strideBytes, 3 * 4)
        // layout 2: vec3 aColor
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, strideBytes, 6 * 4)
        // layout 3: vec2 aEmissiveWin (emissive, winSeed)
        GLES30.glEnableVertexAttribArray(3)
        GLES30.glVertexAttribPointer(3, 2, GLES30.GL_FLOAT, false, strideBytes, 9 * 4)
      }

      vaoSpire = vaos[0]; vboSpire = vbos[0]; spireVertCount = spire.vertexCount
      setupPart(vaoSpire, vboSpire, spire)

      vaoOuterRing = vaos[1]; vboOuterRing = vbos[1]; outerRingVertCount = outer.vertexCount
      setupPart(vaoOuterRing, vboOuterRing, outer)

      vaoInnerRing = vaos[2]; vboInnerRing = vbos[2]; innerRingVertCount = inner.vertexCount
      setupPart(vaoInnerRing, vboInnerRing, inner)

      GLES30.glBindVertexArray(0)
      GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
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

      // Pack all 10 128x128 Playfield ship sprites into a 5x2 (640x256) RGBA texture atlas
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

    /** Computes the world-space position of one of the 4 rotating station docking bays. */
    private fun computeDockingBayWorldPos(bayIdx: Int, timeSec: Float, radiusScale: Float = 1.0f): FloatArray {
      val st = NEXUS_3D_RIFT_TEARS[0]
      val ringSpin = timeSec * 0.22f
      val localAng = (bayIdx % 4) * (PI.toFloat() * 0.5f) + ringSpin
      val r = 0.50f * radiusScale
      val lx = cos(localAng) * r
      val ly = 0f
      val lz = sin(localAng) * r
      // Apply outer ring tilt (pitch = 0.48f around X, roll = -0.16f around Z)
      val cp = cos(0.48f); val sp = sin(0.48f)
      val cr = cos(-0.16f); val sr = sin(-0.16f)
      val x1 = lx * cr - ly * sr
      val y1 = lx * sr + ly * cr
      val z1 = lz
      val wx = st.worldX + x1
      val wy = st.worldY + (y1 * cp - z1 * sp)
      val wz = st.worldZ + (y1 * sp + z1 * cp)
      return floatArrayOf(wx, wy, wz)
    }

    private fun assignNextRoute(ship: HubTrafficShip, slotIdx: Int, timeSec: Float, randomizeInitialProgress: Boolean) {
      val nextType =
        when {
          randomizeInitialProgress -> slotIdx % 3
          ship.routeType == 0 -> 1
          ship.routeType == 1 -> if (trafficRng.nextFloat() < 0.35f) 2 else 0
          else -> 0
        }
      ship.routeType = nextType
      val riftIdx = 1 + ((slotIdx * 3 + trafficRng.nextInt(7)) % 7)
      val bayIdx = (slotIdx + trafficRng.nextInt(4)) % 4
      ship.targetRiftIdx = riftIdx
      ship.dockBayIdx = bayIdx
      ship.durationSec = 5.6f + trafficRng.nextFloat() * 3.2f
      ship.progress = if (randomizeInitialProgress) trafficRng.nextFloat() * 0.92f else 0f
      ship.trailInitialized = false

      val rift = NEXUS_3D_RIFT_TEARS[riftIdx]
      val bayPos = computeDockingBayWorldPos(bayIdx, timeSec, radiusScale = 1.04f)
      val bayApproach = computeDockingBayWorldPos(bayIdx, timeSec, radiusScale = 1.65f)
      val st = NEXUS_3D_RIFT_TEARS[0]

      // Point on the swirling outer rim of the target Rift so the ship spirals into the event horizon
      val rimAng = trafficRng.nextFloat() * (PI.toFloat() * 2f)
      val riftRimX = rift.worldX + cos(rimAng) * (rift.halfWidthWorld * 0.72f)
      val riftRimY = rift.worldY + sin(rimAng) * (rift.halfHeightWorld * 0.72f)
      val riftRimZ = rift.worldZ + 0.04f

      val arcLiftY = (if (slotIdx % 2 == 0) 1f else -1f) * (0.28f + trafficRng.nextFloat() * 0.26f)
      val arcDepthZ = if (slotIdx % 2 == 0) 0.42f else -0.34f // Passes in front of AND behind station rings!

      when (nextType) {
        0 -> { // UNDOCK_TO_RIFT: Station Docking Bay -> Curved 3D Corridor -> Rift Horizon
          ship.p0x = bayPos[0]; ship.p0y = bayPos[1]; ship.p0z = bayPos[2]
          ship.p1x = bayApproach[0]; ship.p1y = bayApproach[1] + arcLiftY * 0.45f; ship.p1z = bayApproach[2] + arcDepthZ
          ship.p2x = rift.worldX * 0.62f; ship.p2y = rift.worldY * 0.62f + arcLiftY; ship.p2z = rift.worldZ + arcDepthZ * 0.65f
          ship.p3x = riftRimX; ship.p3y = riftRimY; ship.p3z = riftRimZ
        }
        1 -> { // RIFT_TO_DOCK: Rift Horizon -> Approach Beacon Lane -> Station Docking Bay
          ship.p0x = riftRimX; ship.p0y = riftRimY; ship.p0z = riftRimZ
          ship.p1x = rift.worldX * 0.58f; ship.p1y = rift.worldY * 0.58f - arcLiftY * 0.7f; ship.p1z = rift.worldZ - arcDepthZ * 0.6f
          ship.p2x = bayApproach[0]; ship.p2y = bayApproach[1] + 0.06f; ship.p2z = bayApproach[2] + 0.18f
          ship.p3x = bayPos[0]; ship.p3y = bayPos[1]; ship.p3z = bayPos[2]
        }
        else -> { // STATION_ORBIT: Sweeping 3D orbital patrol around the rotating NEXUS station rings
          val aStart = (slotIdx * 1.35f) + trafficRng.nextFloat() * 1.2f
          val aMid1 = aStart + 1.15f
          val aMid2 = aStart + 2.30f
          val aEnd = aStart + 3.45f
          val orbitR = 0.74f + (slotIdx % 3) * 0.11f
          val incl = if (slotIdx % 2 == 0) 0.42f else -0.38f
          fun ox(a: Float) = st.worldX + cos(a) * orbitR
          fun oy(a: Float) = st.worldY + sin(a) * orbitR * sin(incl)
          fun oz(a: Float) = st.worldZ + sin(a) * orbitR * cos(incl)
          ship.p0x = ox(aStart); ship.p0y = oy(aStart); ship.p0z = oz(aStart)
          ship.p1x = ox(aMid1) * 1.18f; ship.p1y = oy(aMid1) * 1.18f; ship.p1z = oz(aMid1) * 1.18f
          ship.p2x = ox(aMid2) * 1.18f; ship.p2y = oy(aMid2) * 1.18f; ship.p2z = oz(aMid2) * 1.18f
          ship.p3x = ox(aEnd); ship.p3y = oy(aEnd); ship.p3z = oz(aEnd)
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
      val prevDz = ship.dirZ
      ship.dirX = vx
      ship.dirY = vy
      ship.dirZ = vz
      val turnSignal = (vx * prevDz - vz * prevDx) * 14f
      ship.bankRad = (ship.bankRad * 0.86f + turnSignal.coerceIn(-0.68f, 0.68f) * 0.14f)
    }

    private fun stepTrafficSimulation(dtSec: Float, timeSec: Float) {
      val onlineIds = onlinePilotShipIds
      val desiredCount =
        if (isSplashMode) {
          5
        } else {
          max(5, onlineIds.size).coerceAtMost(28)
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
        assignNextRoute(s, idx, timeSec, randomizeInitialProgress = true)
        evalBezierPosAndVel(s, s.progress)
        trafficShips.add(s)
      }
      while (trafficShips.size > desiredCount) {
        trafficShips.removeAt(trafficShips.lastIndex)
      }

      for (i in 0 until trafficShips.size) {
        val s = trafficShips[i]
        // Keep real online human pilot ship hulls synced 1:1
        s.shipId =
          if (i < onlineIds.size) {
            onlineIds[i].coerceIn(1, 10)
          } else {
            ((i * 3 + 1) % 10) + 1
          }

        s.progress += dtSec / max(2.5f, s.durationSec)
        if (s.progress >= 1f) {
          assignNextRoute(s, i, timeSec, randomizeInitialProgress = false)
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
          if (headDist >= 0.022f) {
            for (k in HubTrafficShip.TRAIL_POINTS - 1 downTo 1) {
              s.trailHistoryX[k] = s.trailHistoryX[k - 1]
              s.trailHistoryY[k] = s.trailHistoryY[k - 1]
              s.trailHistoryZ[k] = s.trailHistoryZ[k - 1]
            }
            s.trailHistoryX[0] = s.curX - s.dirX * 0.032f
            s.trailHistoryY[0] = s.curY - s.dirY * 0.032f
            s.trailHistoryZ[0] = s.curZ - s.dirZ * 0.032f
          }
        }
      }
    }

    private fun allocateFramebuffers(w: Int, h: Int) {
      deleteFramebuffers()
      allocatedW = w
      allocatedH = h

      val scenePair = createSceneColorDepthFbo(w, h)
      fboScene = scenePair[0]
      texScene = scenePair[1]
      rboSceneDepth = scenePair[2]

      val bw = max(1, w / 4)
      val bh = max(1, h / 4)
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

    private fun buildRotTransMatrix(
      tx: Float, ty: Float, tz: Float,
      spinY: Float, pitchX: Float, rollZ: Float,
      out: FloatArray,
    ) {
      // M = Translate(tx,ty,tz) * RotZ(rollZ) * RotX(pitchX) * RotY(spinY) (column-major for GLSL)
      val cy = cos(spinY); val sy = sin(spinY)
      val cx = cos(pitchX); val sx = sin(pitchX)
      val cz = cos(rollZ); val sz = sin(rollZ)

      // R = Rz * Rx * Ry
      val r00 = cz * cy - sz * sx * sy
      val r10 = sz * cy + cz * sx * sy
      val r20 = -cx * sy

      val r01 = -sz * cx
      val r11 = cz * cx
      val r21 = sx

      val r02 = cz * sy + sz * sx * cy
      val r12 = sz * sy - cz * sx * cy
      val r22 = cx * cy

      out[0] = r00; out[1] = r10; out[2] = r20; out[3] = 0f
      out[4] = r01; out[5] = r11; out[6] = r21; out[7] = 0f
      out[8] = r02; out[9] = r12; out[10] = r22; out[11] = 0f
      out[12] = tx; out[13] = ty; out[14] = tz; out[15] = 1f
    }

    private fun drawStation3D(basis: CameraBasis3D, timeSec: Float) {
      val st = NEXUS_3D_RIFT_TEARS[0]
      val sx = if (isSplashMode) 0.0f else st.worldX
      val sy = if (isSplashMode) 0.04f else st.worldY
      val sz = if (isSplashMode) -0.55f else st.worldZ

      GLES30.glUseProgram(progStation)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progStation, "uCamPos"), basis.camX, basis.camY, basis.camZ)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progStation, "uCamFwd"), basis.fwdX, basis.fwdY, basis.fwdZ)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progStation, "uCamRight"), basis.rightX, basis.rightY, basis.rightZ)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progStation, "uCamUp"), basis.upX, basis.upY, basis.upZ)
      GLES30.glUniform1f(GLES30.glGetUniformLocation(progStation, "uTanHalfFovY"), basis.tanHalfFovY)
      GLES30.glUniform1f(GLES30.glGetUniformLocation(progStation, "uAspect"), basis.aspect)
      GLES30.glUniform1f(GLES30.glGetUniformLocation(progStation, "uTime"), timeSec)
      val focusGain = if (!isSplashMode && focusedTearIndex == 0) 1.32f else 1.0f
      GLES30.glUniform1f(GLES30.glGetUniformLocation(progStation, "uFocusGain"), focusGain)

      val locModel = GLES30.glGetUniformLocation(progStation, "uModel")

      // Part 0: Central Command Spire + Reactor Heart + Approach Beacons (majestic slow rotation + gentle tilt)
      buildRotTransMatrix(sx, sy, sz, spinY = timeSec * 0.11f, pitchX = 0.16f, rollZ = -0.05f, modelMat)
      GLES30.glUniformMatrix4fv(locModel, 1, false, modelMat, 0)
      GLES30.glBindVertexArray(vaoSpire)
      GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, spireVertCount)

      // Part 1: Outer Sovereign Docking Ring (tilted 3D plane rotating clockwise)
      buildRotTransMatrix(sx, sy, sz, spinY = timeSec * 0.22f, pitchX = 0.48f, rollZ = -0.16f, modelMat)
      GLES30.glUniformMatrix4fv(locModel, 1, false, modelMat, 0)
      GLES30.glBindVertexArray(vaoOuterRing)
      GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, outerRingVertCount)

      // Part 2: Inner Ring-Space Gyro Ring (cross-tilted 3D plane counter-rotating)
      buildRotTransMatrix(sx, sy, sz, spinY = -timeSec * 0.36f, pitchX = -0.38f, rollZ = 0.28f, modelMat)
      GLES30.glUniformMatrix4fv(locModel, 1, false, modelMat, 0)
      GLES30.glBindVertexArray(vaoInnerRing)
      GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, innerRingVertCount)

      GLES30.glBindVertexArray(0)
    }

    private fun drawLiveShipTraffic3D(basis: CameraBasis3D, timeSec: Float) {
      if (texShipAtlas == 0 || trafficShips.isEmpty()) return

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

        // Smooth scale & warp pinch envelope during docking/undocking and Rift entry/exit
        var hullScale = 1f
        var warpFlash = 0f
        if (s.routeType == 0) { // UNDOCK_TO_RIFT
          if (p < 0.14f) hullScale = 0.32f + 0.68f * (p / 0.14f)
          if (p > 0.84f) {
            val k = ((p - 0.84f) / 0.16f).coerceIn(0f, 1f)
            hullScale = 1f - 0.88f * k
            warpFlash = sin(k * PI.toFloat())
          }
        } else if (s.routeType == 1) { // RIFT_TO_DOCK
          if (p < 0.16f) {
            val k = (p / 0.16f).coerceIn(0f, 1f)
            hullScale = 0.12f + 0.88f * k
            warpFlash = sin(k * PI.toFloat())
          }
          if (p > 0.86f) hullScale = 1f - 0.68f * ((p - 0.86f) / 0.14f)
        }

        // Compute 3D oriented ship basis: nose along velocity, wing perpendicular to view ray + banked by turn
        var toCamX = basis.camX - s.curX
        var toCamY = basis.camY - s.curY
        var toCamZ = basis.camZ - s.curZ
        val tcLen = sqrt(max(1e-6f, toCamX * toCamX + toCamY * toCamY + toCamZ * toCamZ))
        toCamX /= tcLen; toCamY /= tcLen; toCamZ /= tcLen

        // rawWing = normalize(cross(toCam, noseDir))
        var wx = toCamY * s.dirZ - toCamZ * s.dirY
        var wy = toCamZ * s.dirX - toCamX * s.dirZ
        var wz = toCamX * s.dirY - toCamY * s.dirX
        val wLen = sqrt(max(1e-6f, wx * wx + wy * wy + wz * wz))
        wx /= wLen; wy /= wLen; wz /= wLen

        // dorsal = cross(noseDir, rawWing)
        val dx = s.dirY * wz - s.dirZ * wy
        val dy = s.dirZ * wx - s.dirX * wz
        val dz = s.dirX * wy - s.dirY * wx

        val cb = cos(s.bankRad)
        val sb = sin(s.bankRad)
        val bankedWx = wx * cb + dx * sb
        val bankedWy = wy * cb + dy * sb
        val bankedWz = wz * cb + dz * sb

        val halfSize = 0.068f * hullScale
        val nx = s.dirX * halfSize
        val ny = s.dirY * halfSize
        val nz = s.dirZ * halfSize
        val sx = bankedWx * halfSize
        val sy = bankedWy * halfSize
        val sz = bankedWz * halfSize

        // Atlas UV for shipId (1..10) in 5x2 grid; inset by 1.5px to prevent neighbor bleed
        val col = (s.shipId - 1) % 5
        val row = (s.shipId - 1) / 5
        val u0 = (col + 0.012f) / 5f
        val u1 = (col + 0.988f) / 5f
        val v0 = (row + 0.012f) / 2f
        val v1 = (row + 0.988f) / 2f

        // 4 corners: stern-top(u0,v0), nose-top(u1,v0), nose-bot(u1,v1), stern-bot(u0,v1)
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

        // Tapering 3D Ion Wake Ribbon behind the ship's stern
        val isGoldTrail = (s.shipId == 4 || s.shipId == 7 || s.shipId == 10)
        val trR = if (isGoldTrail) 0.98f else 0.05f
        val trG = if (isGoldTrail) 0.80f else 0.92f
        val trB = if (isGoldTrail) 0.18f else 0.96f

        var prevLx = s.curX - nx * 0.72f + wx * (0.016f * hullScale)
        var prevLy = s.curY - ny * 0.72f + wy * (0.016f * hullScale)
        var prevLz = s.curZ - nz * 0.72f + wz * (0.016f * hullScale)
        var prevRx = s.curX - nx * 0.72f - wx * (0.016f * hullScale)
        var prevRy = s.curY - ny * 0.72f - wy * (0.016f * hullScale)
        var prevRz = s.curZ - nz * 0.72f - wz * (0.016f * hullScale)
        var prevAlpha = 0.88f * hullScale

        for (k in 0 until HubTrafficShip.TRAIL_POINTS) {
          val frac = 1f - (k + 1).toFloat() / HubTrafficShip.TRAIL_POINTS.toFloat()
          val wHalf = 0.015f * frac * hullScale
          val curAlpha = 0.68f * frac * frac * hullScale
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

        // Rift Warp Pinch Flash diamond when a ship enters or exits one of the 7 Rifts
        if (warpFlash > 0.04f) {
          val flashR = (0.055f + 0.075f * warpFlash)
          val fx = basis.rightX * flashR
          val fy = basis.rightY * flashR
          val fz = basis.rightZ * flashR
          val ux = basis.upX * flashR
          val uy = basis.upY * flashR
          val uz = basis.upZ * flashR
          val fa = warpFlash * 1.35f
          val pTopX = s.curX + ux; val pTopY = s.curY + uy; val pTopZ = s.curZ + uz
          val pBotX = s.curX - ux; val pBotY = s.curY - uy; val pBotZ = s.curZ - uz
          val pRgtX = s.curX + fx; val pRgtY = s.curY + fy; val pRgtZ = s.curZ + fz
          val pLftX = s.curX - fx; val pLftY = s.curY - fy; val pLftZ = s.curZ - fz
          pushTrailVert(s.curX, s.curY, s.curZ, 1.0f, 1.0f, 1.0f, fa)
          pushTrailVert(pTopX, pTopY, pTopZ, 0.0f, 0.96f, 0.85f, 0f)
          pushTrailVert(pRgtX, pRgtY, pRgtZ, 0.25f, 0.80f, 1.0f, 0f)

          pushTrailVert(s.curX, s.curY, s.curZ, 1.0f, 1.0f, 1.0f, fa)
          pushTrailVert(pRgtX, pRgtY, pRgtZ, 0.25f, 0.80f, 1.0f, 0f)
          pushTrailVert(pBotX, pBotY, pBotZ, 0.0f, 0.96f, 0.85f, 0f)

          pushTrailVert(s.curX, s.curY, s.curZ, 1.0f, 1.0f, 1.0f, fa)
          pushTrailVert(pBotX, pBotY, pBotZ, 0.0f, 0.96f, 0.85f, 0f)
          pushTrailVert(pLftX, pLftY, pLftZ, 0.25f, 0.80f, 1.0f, 0f)

          pushTrailVert(s.curX, s.curY, s.curZ, 1.0f, 1.0f, 1.0f, fa)
          pushTrailVert(pLftX, pLftY, pLftZ, 0.25f, 0.80f, 1.0f, 0f)
          pushTrailVert(pTopX, pTopY, pTopZ, 0.0f, 0.96f, 0.85f, 0f)
        }
      }

      // 1. Draw opaque/alpha-tested 3D Ship Hulls with GL_DEPTH_TEST & DST_ALPHA Rift-void masking
      if (hullFloats > 0) {
        shipHullBuffer.clear()
        shipHullBuffer.put(shipHullScratch, 0, hullFloats)
        shipHullBuffer.position(0)

        GLES30.glEnable(GLES30.GL_BLEND)
        // Multiply src by DST_ALPHA (voidKeepMask) so ships entering a Rift never draw over #000000 button text!
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

      // 2. Draw additive 3D Ion Trails & Warp Pinch Flashes (occluded by 3D station depth, masked by DST_ALPHA)
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

      // Fill uniform arrays for the 7 surrounding Rifts (indices 1..7) or 2 flanking ambient Rifts in splash mode
      val tearCount = if (isSplashMode) 2 else 7
      if (isSplashMode) {
        // Flanking outward-bleeding Singularities in deep #010308 space behind the Auth glass card
        tearPosSizeBuf[0] = -1.42f; tearPosSizeBuf[1] = 0.42f; tearPosSizeBuf[2] = 0.44f; tearPosSizeBuf[3] = 0.42f
        tearStyleBuf[0] = -0.15f; tearStyleBuf[1] = -0.06f; tearStyleBuf[2] = 0.5f; tearStyleBuf[3] = 1.0f
        tearColorABuf[0] = 0.0f; tearColorABuf[1] = 0.96f; tearColorABuf[2] = 0.83f
        tearColorBBuf[0] = 0.22f; tearColorBBuf[1] = 0.74f; tearColorBBuf[2] = 0.97f

        tearPosSizeBuf[4] = 1.42f; tearPosSizeBuf[5] = -0.36f; tearPosSizeBuf[6] = 0.44f; tearPosSizeBuf[7] = 0.42f
        tearStyleBuf[4] = -0.15f; tearStyleBuf[5] = 0.06f; tearStyleBuf[6] = 3.1f; tearStyleBuf[7] = 1.0f
        tearColorABuf[3] = 0.75f; tearColorABuf[4] = 0.52f; tearColorABuf[5] = 0.99f
        tearColorBBuf[3] = 0.98f; tearColorBBuf[4] = 0.80f; tearColorBBuf[5] = 0.09f
      } else {
        for (k in 0 until 7) {
          val spec = NEXUS_3D_RIFT_TEARS[k + 1]
          val bobY = sin(timeSec * 0.95f + spec.phaseOffset) * 0.012f
          val p4 = k * 4
          val p3 = k * 3
          tearPosSizeBuf[p4] = spec.worldX
          tearPosSizeBuf[p4 + 1] = spec.worldY + bobY
          tearPosSizeBuf[p4 + 2] = spec.halfWidthWorld
          tearPosSizeBuf[p4 + 3] = spec.halfHeightWorld

          val isFocused = (focusedTearIndex == spec.index || activeDiveTearIdx == spec.index)
          tearStyleBuf[p4] = spec.worldZ
          tearStyleBuf[p4 + 1] = spec.tiltRad
          tearStyleBuf[p4 + 2] = spec.phaseOffset
          tearStyleBuf[p4 + 3] = if (isFocused) 1.38f else 1.0f

          tearColorABuf[p3] = spec.lipColorA.red
          tearColorABuf[p3 + 1] = spec.lipColorA.green
          tearColorABuf[p3 + 2] = spec.lipColorA.blue

          tearColorBBuf[p3] = spec.lipColorB.red
          tearColorBBuf[p3 + 1] = spec.lipColorB.green
          tearColorBBuf[p3 + 2] = spec.lipColorB.blue
        }
      }

      // PASS 1A: Deep #010308 Space + 7 Round Outward-Bleeding Playfield-Spirit 3D Rifts into fboScene
      GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboScene)
      GLES30.glViewport(0, 0, w, h)
      GLES30.glColorMask(true, true, true, true)
      GLES30.glDepthMask(true)
      GLES30.glDisable(GLES30.GL_DEPTH_TEST)
      GLES30.glDisable(GLES30.GL_BLEND)
      GLES30.glClearColor(0.004f, 0.011f, 0.031f, 1.0f)
      GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

      GLES30.glBindVertexArray(emptyVao)
      GLES30.glUseProgram(progScene)
      GLES30.glUniform2f(GLES30.glGetUniformLocation(progScene, "uResolution"), w.toFloat(), h.toFloat())
      GLES30.glUniform1f(GLES30.glGetUniformLocation(progScene, "uTime"), timeSec)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progScene, "uCamPos"), basis.camX, basis.camY, basis.camZ)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progScene, "uCamFwd"), basis.fwdX, basis.fwdY, basis.fwdZ)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progScene, "uCamRight"), basis.rightX, basis.rightY, basis.rightZ)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progScene, "uCamUp"), basis.upX, basis.upY, basis.upZ)
      GLES30.glUniform1f(GLES30.glGetUniformLocation(progScene, "uTanHalfFovY"), basis.tanHalfFovY)
      GLES30.glUniform1i(GLES30.glGetUniformLocation(progScene, "uTearCount"), tearCount)
      GLES30.glUniform4fv(GLES30.glGetUniformLocation(progScene, "uTearPosSize"), tearCount, tearPosSizeBuf, 0)
      GLES30.glUniform4fv(GLES30.glGetUniformLocation(progScene, "uTearStyle"), tearCount, tearStyleBuf, 0)
      GLES30.glUniform3fv(GLES30.glGetUniformLocation(progScene, "uTearColorA"), tearCount, tearColorABuf, 0)
      GLES30.glUniform3fv(GLES30.glGetUniformLocation(progScene, "uTearColorB"), tearCount, tearColorBBuf, 0)
      GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)

      // PASS 1B & 1C: Hardware Depth-Tested 3D Central NEXUS Station + Live Real-Ship Playfield Traffic
      // Preserve fboScene.a (voidKeepMask) so #000000 Rift button-text cores stay 100% pure black!
      GLES30.glColorMask(true, true, true, false)
      GLES30.glEnable(GLES30.GL_DEPTH_TEST)
      GLES30.glDepthFunc(GLES30.GL_LEQUAL)
      GLES30.glDepthMask(true)

      drawStation3D(basis, timeSec)
      drawLiveShipTraffic3D(basis, timeSec)

      GLES30.glDisable(GLES30.GL_DEPTH_TEST)
      GLES30.glColorMask(true, true, true, true)

      val bw = max(1, w / 4)
      val bh = max(1, h / 4)
      GLES30.glBindVertexArray(emptyVao)

      // PASS 2: Quarter-Res HDR Bright-Pass Extraction -> fboBloomA
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

      // PASS 5: Final ACES Filmic Tonemap + Void-Masked HDR Bloom + 3D Cosmic Motes -> Default Screen
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

    private fun compileShader(type: Int, src: String): Int {
      val s = GLES30.glCreateShader(type)
      if (s == 0) return 0
      GLES30.glShaderSource(s, src)
      GLES30.glCompileShader(s)
      val status = IntArray(1)
      GLES30.glGetShaderiv(s, GLES30.GL_COMPILE_STATUS, status, 0)
      if (status[0] == 0) {
        GLES30.glDeleteShader(s)
        return 0
      }
      return s
    }

    private fun linkProgram(vertSrc: String, fragSrc: String): Int {
      val vs = compileShader(GLES30.GL_VERTEX_SHADER, vertSrc)
      if (vs == 0) return 0
      val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragSrc)
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
        GLES30.glDeleteProgram(prog)
        return 0
      }
      return prog
    }

    private fun deleteFramebuffers() {
      val fbos = intArrayOf(fboScene, fboBloomA, fboBloomB)
      val texs = intArrayOf(texScene, texBloomA, texBloomB)
      val rbos = intArrayOf(rboSceneDepth)
      for (i in fbos.indices) {
        if (fbos[i] != 0) GLES30.glDeleteFramebuffers(1, intArrayOf(fbos[i]), 0)
        if (texs[i] != 0) GLES30.glDeleteTextures(1, intArrayOf(texs[i]), 0)
      }
      if (rbos[0] != 0) GLES30.glDeleteRenderbuffers(1, rbos, 0)
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
          val vaos = intArrayOf(emptyVao, vaoSpire, vaoOuterRing, vaoInnerRing, vaoShipHull, vaoShipTrail)
          for (v in vaos) if (v != 0) GLES30.glDeleteVertexArrays(1, intArrayOf(v), 0)
          val vbos = intArrayOf(vboSpire, vboOuterRing, vboInnerRing, vboShipHull, vboShipTrail)
          for (b in vbos) if (b != 0) GLES30.glDeleteBuffers(1, intArrayOf(b), 0)
          val progs = intArrayOf(progScene, progStation, progShipHull, progShipTrail, progBright, progBlur, progComposite)
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
out vec2 vUv;
void main() {
    vec2 pos = vec2((gl_VertexID == 1) ? 3.0 : -1.0, (gl_VertexID == 2) ? 3.0 : -1.0);
    vUv = pos * 0.5 + 0.5;
    gl_Position = vec4(pos, 0.0, 1.0);
}
"""

private const val STATION_VERT_GLSL =
  """#version 300 es
precision highp float;

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec3 aColor;
layout(location = 3) in vec2 aEmissiveWin;

uniform mat4 uModel;
uniform vec3 uCamPos;
uniform vec3 uCamFwd;
uniform vec3 uCamRight;
uniform vec3 uCamUp;
uniform float uTanHalfFovY;
uniform float uAspect;

out vec3 vWorldPos;
out vec3 vLocalPos;
out vec3 vWorldNormal;
out vec3 vColor;
out vec2 vEmissiveWin;

void main() {
    vec4 wp = uModel * vec4(aPos, 1.0);
    vWorldPos = wp.xyz;
    vLocalPos = aPos;
    vWorldNormal = normalize(mat3(uModel) * aNormal);
    vColor = aColor;
    vEmissiveWin = aEmissiveWin;

    vec3 d = wp.xyz - uCamPos;
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

private const val STATION_FRAG_GLSL =
  """#version 300 es
precision highp float;

in vec3 vWorldPos;
in vec3 vLocalPos;
in vec3 vWorldNormal;
in vec3 vColor;
in vec2 vEmissiveWin;

uniform vec3 uCamPos;
uniform float uTime;
uniform float uFocusGain;

out vec4 fragColor;

float hash21(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

void main() {
    vec3 N = normalize(vWorldNormal);
    vec3 V = normalize(uCamPos - vWorldPos);

    // Key cyan-white directional starlight + warm solar-gold rim fill + subtle ambient
    vec3 L1 = normalize(vec3(-0.55, 0.72, 0.68));
    vec3 L2 = normalize(vec3(0.75, -0.42, 0.48));
    float diff1 = max(dot(N, L1), 0.0);
    float diff2 = max(dot(N, L2), 0.0);

    vec3 H1 = normalize(L1 + V);
    float spec1 = pow(max(dot(N, H1), 0.0), 38.0);
    float fresnel = pow(1.0 - max(dot(N, V), 0.0), 3.0);

    vec3 litHull = vColor * (0.34 + 0.85 * diff1 + 0.32 * diff2);
    litHull += vec3(0.42, 0.88, 1.0) * (spec1 * 0.55 + fresnel * 0.22);
    litHull += vec3(0.98, 0.80, 0.15) * (diff2 * 0.12);

    float emissive = vEmissiveWin.x;
    float winSeed = vEmissiveWin.y;

    // Sequential approach-lane guide beacon pulse (winSeed < 0)
    if (winSeed < -0.5) {
        float beaconIdx = -winSeed;
        float wave = 0.5 + 0.5 * sin(uTime * 5.2 - beaconIdx * 1.45);
        emissive *= (0.45 + 1.15 * pow(wave, 2.0));
    }

    vec3 glowAdd = vColor * emissive * uFocusGain;

    // Lit Citadel micro-windows on armored command spire & outer docking ring plates
    if (winSeed > 0.5) {
        float ang = atan(vLocalPos.z, vLocalPos.x);
        vec2 winGrid = vec2(ang * 14.0 + winSeed * 1.7, vLocalPos.y * 92.0);
        vec2 cell = floor(winGrid);
        vec2 f = fract(winGrid);
        float inPane = step(0.24, f.x) * step(f.x, 0.76) * step(0.30, f.y) * step(f.y, 0.70);
        float rnd = hash21(cell + floor(winSeed));
        if (inPane > 0.5 && rnd > 0.46) {
            vec3 winCol = (rnd > 0.84) ? vec3(1.0, 0.84, 0.22) : vec3(0.18, 0.92, 1.0);
            float twinkle = 0.78 + 0.22 * sin(uTime * 1.8 + rnd * 19.0);
            glowAdd += winCol * (1.25 * twinkle * uFocusGain);
        }
    }

    fragColor = vec4(litHull + glowAdd, 1.0);
}
"""

private const val SHIP_HULL_VERT_GLSL =
  """#version 300 es
precision highp float;

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

in vec2 vUv;
in vec2 vAlphaBoost;

uniform sampler2D uAtlasTex;

out vec4 fragColor;

void main() {
    vec4 texel = texture(uAtlasTex, vUv);
    if (texel.a < 0.06) {
        discard;
    }
    // texel.rgb is premultiplied from Android Bitmap; boost metallic specular & warp entry rim
    float alpha = texel.a * vAlphaBoost.x;
    vec3 boosted = texel.rgb * (1.12 * vAlphaBoost.y);
    fragColor = vec4(boosted, alpha);
}
"""

private const val SHIP_TRAIL_VERT_GLSL =
  """#version 300 es
precision highp float;

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

in vec4 vColor;
out vec4 fragColor;

void main() {
    fragColor = vec4(vColor.rgb * vColor.a, vColor.a);
}
"""

private const val NEXUS_VOLUMETRIC_SCENE_FRAG_GLSL =
  """#version 300 es
precision highp float;

in vec2 vUv;
out vec4 fragColor;

uniform vec2 uResolution;
uniform float uTime;
uniform vec3 uCamPos;
uniform vec3 uCamFwd;
uniform vec3 uCamRight;
uniform vec3 uCamUp;
uniform float uTanHalfFovY;
uniform int uTearCount;
uniform vec4 uTearPosSize[8]; // (worldX, worldY, halfW, halfH)
uniform vec4 uTearStyle[8];   // (worldZ, tiltRad, phaseOffset, focusGain)
uniform vec3 uTearColorA[8];
uniform vec3 uTearColorB[8];

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

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
    float twinkle = 0.72 + 0.28 * sin(t * (1.5 + rnd.y * 3.0) + rnd.x * 37.0);
    float core = smoothstep(0.085, 0.0, d) * twinkle;
    vec3 tint = mix(vec3(0.68, 0.86, 1.0), vec3(1.0, 0.90, 0.72), fract(rnd.y * 7.3));
    return tint * core * (0.55 + 0.80 * fract(rnd.x * 13.7));
}

// Deep #010308–#030712 Abyssal Dark Space (background luminance strictly < 0.032; ZERO bright teal fog!)
vec3 sampleAbyssalDarkSpace(vec3 rayDir, float t) {
    vec2 sph = rayDir.xy / max(0.32, abs(rayDir.z));
    float dustA = fbm4(vec3(sph * 1.15, t * 0.018));
    float dustB = fbm4(vec3(sph * 2.25 + vec2(3.7, -2.1), -t * 0.014));
    float faintWisp = smoothstep(0.46, 0.88, dustA * 0.62 + dustB * 0.38);

    vec3 deepVoid = vec3(0.004, 0.011, 0.031);     // #010308
    vec3 midnightDust = vec3(0.014, 0.026, 0.068); // faint <6% deep midnight-indigo dust
    vec3 col = mix(deepVoid, midnightDust, faintWisp * 0.52);

    // Crisp 3-depth parallax starfield
    col += starFieldLayer(sph + vec2(t * 0.0008, 0.0), 34.0, 0.86, t) * 1.05;
    col += starFieldLayer(sph * 1.32 - vec2(0.0, t * 0.0006), 64.0, 0.89, t) * 0.75;
    col += starFieldLayer(sph * 1.75 + vec2(4.2, -1.9), 112.0, 0.92, t) * 0.50;
    return col;
}

void main() {
    float aspect = uResolution.x / max(1.0, uResolution.y);
    vec2 ndc = vUv * 2.0 - 1.0;

    vec3 rayDir = normalize(
        uCamFwd +
        ndc.x * aspect * uTanHalfFovY * uCamRight +
        ndc.y * uTanHalfFovY * uCamUp
    );

    // Gravitational micro-lensing bend of background starlight around the 7 Singularities
    vec3 lensedDir = rayDir;
    for (int i = 0; i < 8; i++) {
        if (i >= uTearCount) break;
        vec4 ps = uTearPosSize[i];
        vec4 st = uTearStyle[i];
        float denom = rayDir.z;
        if (abs(denom) > 1e-4) {
            float tHit = (st.x - uCamPos.z) / denom;
            if (tHit > 0.1) {
                vec3 hit = uCamPos + rayDir * tHit;
                vec2 d = (hit.xy - ps.xy) / max(vec2(0.05), ps.zw);
                float r2 = dot(d, d);
                if (r2 < 5.5) {
                    float pull = exp(-r2 * 0.95) * 0.032;
                    lensedDir.xy -= normalize(d + vec2(1e-5)) * pull;
                }
            }
        }
    }
    lensedDir = normalize(lensedDir);

    vec3 col = sampleAbyssalDarkSpace(lensedDir, uTime);

    // Subtle volumetric station core halo at dead-center (world 0, -0.02, 0.10) behind the 3D station mesh
    if (abs(rayDir.z) > 1e-4) {
        float tHub = (0.10 - uCamPos.z) / rayDir.z;
        if (tHub > 0.1) {
            vec3 hubHit = uCamPos + rayDir * tHub;
            float hubDist = length(hubHit.xy - vec2(0.0, -0.02));
            col += vec3(0.0, 0.42, 0.58) * exp(-hubDist * 4.2) * 0.14;
        }
    }

    float voidKeepMask = 1.0;

    // 7 Surrounding Round (~1.05:1.0) Outward-Bleeding Playfield-Spirit 3D Singularities
    for (int i = 0; i < 8; i++) {
        if (i >= uTearCount) break;
        vec4 ps = uTearPosSize[i];
        vec4 st = uTearStyle[i];
        float denom = rayDir.z;
        if (abs(denom) < 1e-4) continue;
        float tHit = (st.x - uCamPos.z) / denom;
        if (tHit <= 0.1) continue;

        vec3 hit = uCamPos + rayDir * tHit;
        vec2 rel = hit.xy - ps.xy;

        float c = cos(-st.y);
        float s = sin(-st.y);
        vec2 rot = vec2(rel.x * c - rel.y * s, rel.x * s + rel.y * c);
        vec2 q = rot / max(vec2(0.04), ps.zw);

        float r = length(q);
        if (r > 2.65) continue;

        float theta = atan(q.y, q.x);
        float phase = st.z;
        float focusGain = st.w;
        vec3 colA = uTearColorA[i];
        vec3 colB = uTearColorB[i];

        // Toned-down breathing shimmer (<3.5% amplitude — 85%+ calmer, ZERO shockwave rings!)
        float breathe = 1.0 + 0.032 * sin(uTime * 1.25 + phase);

        // Organic round #000000 event-horizon core housing projected button text (only in 7-Rift Hub mode)
        float rimNoise = (fbm4(vec3(cos(theta) * 2.1, sin(theta) * 2.1, uTime * 0.30 + phase)) - 0.5) * 0.065;
        float coreR = (uTearCount == 7) ? (0.68 + rimNoise) : (0.34 + rimNoise * 0.6);

        float rDist = r - coreR;

        // 1. Outward-Bleeding 3D Swirling Logarithmic Spiral Plasma Arms + Soft FBM Nebula Wisps
        if (rDist > -0.06) {
            float safeR = max(r, 0.16);
            float logR = log(safeR);
            float warp = fbm4(vec3(q * 1.85, uTime * 0.26 + phase));

            // Twin counter-structured Playfield-spirit spiral arms bleeding outward into #010308 dark space
            float armPrimary = 0.5 + 0.5 * sin(3.0 * theta - 5.4 * logR - uTime * 1.75 + warp * 2.8 + phase);
            float armSecondary = 0.5 + 0.5 * sin(5.0 * theta - 3.6 * logR - uTime * 1.15 - warp * 2.1);
            float spiral = pow(armPrimary, 2.2) * 0.72 + pow(armSecondary, 2.8) * 0.42;

            float radialFalloff = exp(-max(rDist, 0.0) * 2.35);
            float wispFalloff = exp(-max(rDist, 0.0) * 1.48);
            float nebulaWisp = smoothstep(0.32, 0.85, warp) * wispFalloff;

            vec3 plasmaTint = mix(colA, colB, clamp(0.5 + 0.5 * sin(theta * 2.0 - logR * 2.2 + uTime * 0.6), 0.0, 1.0));
            col += plasmaTint * (spiral * radialFalloff * 1.42 + nebulaWisp * 0.46) * breathe * focusGain;

            // Razor-crisp white-hot inner photon accretion ring hugging the event horizon
            float photonRing = exp(-abs(rDist) * 20.0);
            vec3 lipHot = mix(plasmaTint, vec3(1.0, 0.99, 1.0), 0.68);
            col += lipHot * photonRing * 2.15 * breathe * focusGain;
        }

        // 2. Pure #000000 Singularity Event-Horizon Interior (guarantees 100% crisp button text contrast)
        float innerVoid = 1.0 - smoothstep(-0.045, 0.025, rDist);
        col = mix(col, vec3(0.0), innerVoid);
        voidKeepMask *= (1.0 - innerVoid);
    }

    fragColor = vec4(max(col, vec3(0.0)), clamp(voidKeepMask, 0.0, 1.0));
}
"""

private const val BLOOM_BRIGHT_EXTRACT_FRAG_GLSL =
  """#version 300 es
precision highp float;
in vec2 vUv;
out vec4 fragColor;
uniform sampler2D uSceneTex;

void main() {
    vec4 s = texture(uSceneTex, vUv);
    float lum = dot(s.rgb, vec3(0.2126, 0.7152, 0.0722));
    float knee = smoothstep(0.52, 1.38, lum);
    fragColor = vec4(s.rgb * knee * s.a, 1.0);
}
"""

private const val BLOOM_GAUSSIAN_BLUR_FRAG_GLSL =
  """#version 300 es
precision highp float;
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
        if (rnd.x > 0.82) {
            vec2 pos = (rnd - 0.5) * 0.60;
            float d = length(gv - pos);
            float pulse = 0.55 + 0.45 * sin(t * 2.1 + rnd.y * 25.0);
            float glow = smoothstep(0.10, 0.0, d) * pulse;
            vec3 c = mix(vec3(0.0, 0.96, 0.83), vec3(0.98, 0.80, 0.09), fract(rnd.y * 5.7));
            acc += c * glow * (0.36 - fl * 0.12);
        }
    }
    return acc;
}

void main() {
    vec4 scene = texture(uSceneTex, vUv);
    vec3 bloom = texture(uBloomTex, vUv).rgb;
    float voidKeep = scene.a;

    float aspect = uResolution.x / max(1.0, uResolution.y);
    vec3 motes = foregroundCosmicMotes(vUv, aspect, uTime, uParallax) * voidKeep;

    vec3 hdr = (scene.rgb + bloom * 0.72 * voidKeep + motes) * voidKeep;
    vec3 mapped = acesTonemap(hdr);
    mapped = pow(mapped, vec3(1.0 / 2.15));

    // Subtle deep-space corner vignette
    vec2 vigUv = vUv * 2.0 - 1.0;
    float vig = clamp(1.0 - dot(vigUv, vigUv) * 0.14, 0.74, 1.0);
    mapped *= mix(1.0, vig, voidKeep);

    fragColor = vec4(mapped * voidKeep * uDimFactor, 1.0);
}
"""
