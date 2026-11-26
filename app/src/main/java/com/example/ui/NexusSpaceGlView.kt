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
 * 3D World-Space Node Specification for the Sci-Fi Command Tablet `NEXUS` Hub:
 * - Index `0` (`CONVERGENCE_GATE`) anchors the central **3D Safe-Zone Sanctuary** (`shieldR = 0.92f`)
 *   and its `[ PLAY // ENTER THE RIFT ]` launch console.
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
    // Index 0: Central 3D Safe-Zone Sanctuary — PLAY // ENTER THE RIFT (CONVERGENCE GATE)
    RiftTearWorldSpec(
      index = 0,
      node = NexusCoreNode.CONVERGENCE_GATE,
      title = "PLAY",
      subtitle = "ENTER THE RIFT",
      worldX = 0.00f,
      worldY = 0.04f,
      worldZ = 0.08f,
      halfWidthWorld = 0.92f,
      halfHeightWorld = 0.92f,
      tiltRad = -0.14f,
      lipColorA = Color(0xFF00F5D4),
      lipColorB = Color(0xFF38BDF8),
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
      lipColorA = Color(0xFF38BDF8),
      lipColorB = Color(0xFF00F5D4),
      accentColor = Color(0xFF38BDF8),
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
      lipColorA = Color(0xFF0EA5E9),
      lipColorB = Color(0xFF00F5D4),
      accentColor = Color(0xFF38BDF8),
      phaseOffset = 2.55f,
      testTag = "nexus_node_star_forge",
    ),
    // Right Tactical Terminal Column (Indices 4..6)
    RiftTearWorldSpec(
      index = 4,
      node = NexusCoreNode.SUBSPACE_COMMS,
      title = "SUBSPACE COMMS",
      subtitle = "Live chat, voice, AI",
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
      subtitle = "Syndicates & Strike Wings",
      worldX = 1.55f,
      worldY = -0.02f,
      worldZ = 0.00f,
      halfWidthWorld = 0.40f,
      halfHeightWorld = 0.36f,
      tiltRad = 0.04f,
      lipColorA = Color(0xFFFACC15),
      lipColorB = Color(0xFF38BDF8),
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
private const val BASE_CAMERA_Z = 3.16f

private const val STATION_CENTER_X = 0.00f
private const val STATION_CENTER_Y = 0.05f
private const val STATION_CENTER_Z = 0.08f
private const val SANCTUARY_SHIELD_RADIUS = 0.92f
private const val SANCTUARY_PITCH_X_RAD = 0.46f
private const val SANCTUARY_ROLL_Z_RAD = -0.14f

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

/**
 * Orthonormal 3D frame for the tilted Safe-Zone Sanctuary disc & `NEXUS` Orbital Citadel:
 * - `(ux, uy, uz)` & `(vx, vy, vz)` span the tilted Safe-Zone plane (`R = 0.92f`).
 * - `(nx, ny, nz)` is the outward/upward citadel spire axis perpendicular to the Safe-Zone plane.
 */
private data class SanctuaryFrame3D(
  val ux: Float,
  val uy: Float,
  val uz: Float,
  val vx: Float,
  val vy: Float,
  val vz: Float,
  val nx: Float,
  val ny: Float,
  val nz: Float,
)

private fun buildSanctuaryFrame3D(
  pitchX: Float = SANCTUARY_PITCH_X_RAD,
  rollZ: Float = SANCTUARY_ROLL_Z_RAD,
): SanctuaryFrame3D {
  val cp = cos(pitchX)
  val sp = sin(pitchX)
  val cr = cos(rollZ)
  val sr = sin(rollZ)

  // Start with disc in (X, Y) and spire along +Z, pitch back by Rx(-pitchX) so top recedes into -Z,
  // then bank by Rz(rollZ):
  // u0 = (1, 0, 0) -> Rx(-pitchX) = (1, 0, 0) -> Rz(rollZ) = (cr, sr, 0)
  // v0 = (0, 1, 0) -> Rx(-pitchX) = (0, cp, -sp) -> Rz(rollZ) = (-sr*cp, cr*cp, -sp)
  // n0 = (0, 0, 1) -> Rx(-pitchX) = (0, sp, cp) -> Rz(rollZ) = (-sr*sp, cr*sp, cp)
  return SanctuaryFrame3D(
    ux = cr,
    uy = sr,
    uz = 0f,
    vx = -sr * cp,
    vy = cr * cp,
    vz = -sp,
    nx = -sr * sp,
    ny = cr * sp,
    nz = cp,
  )
}

private val DEFAULT_SANCTUARY_FRAME = buildSanctuaryFrame3D()

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

  val orbitX = if (isSplashMode) sin(timeSec * 0.22f) * 0.032f else 0f
  val orbitY = if (isSplashMode) cos(timeSec * 0.18f) * 0.022f else 0f
  val orbitZ = if (isSplashMode) sin(timeSec * 0.15f) * 0.024f else 0f

  var camX = orbitX + sin(lookYawRad) * 0.44f
  var camY = orbitY + sin(lookPitchRad) * 0.34f
  var camZ = baseZ + orbitZ

  var lookX = -sin(lookYawRad) * 0.42f
  var lookY = STATION_CENTER_Y - sin(lookPitchRad) * 0.32f
  var lookZ = STATION_CENTER_Z

  if (!isSplashMode && diveTearIdx in NEXUS_3D_RIFT_TEARS.indices && diveProgress > 0.001f) {
    val target = NEXUS_3D_RIFT_TEARS[diveTearIdx]
    val ease = diveProgress * diveProgress * (3f - 2f * diveProgress)
    val diveTargetX = if (diveTearIdx == 0) STATION_CENTER_X else target.worldX * 0.34f
    val diveTargetY = if (diveTearIdx == 0) STATION_CENTER_Y else target.worldY * 0.34f
    camX += (diveTargetX - camX) * (ease * 0.74f)
    camY += (diveTargetY - camY) * (ease * 0.74f)
    camZ += (0.86f - camZ) * (ease * 0.80f)
    lookX += (diveTargetX - lookX) * ease
    lookY += (diveTargetY - lookY) * ease
    lookZ += (STATION_CENTER_Z - lookZ) * ease
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
 * Procedural 3D Mesh Builder (`11` floats per vertex: `pos(3) + normal(3) + color(3) + emissive(1) + radialT(1)`)
 * for the Multi-Ring 3D `NEXUS` Orbital Citadel, Counter-Rotating Docking Rings, 3D Containment Shield Ring,
 * 8 Golden-Cyan 3D Stabilizer Beams, and 8 Outward-Bracing Emitter Chevrons.
 */
private class StationMeshBuilder(initialFloats: Int = 24576) {
  private var data = FloatArray(initialFloats)
  var floatCount: Int = 0
    private set

  val vertexCount: Int
    get() = floatCount / FLOATS_PER_VERTEX

  private fun ensureCapacity(extraFloats: Int) {
    if (floatCount + extraFloats <= data.size) return
    var next = data.size * 2
    while (next < floatCount + extraFloats) next *= 2
    data = data.copyOf(next)
  }

  private fun pushVertex(
    px: Float, py: Float, pz: Float,
    nx: Float, ny: Float, nz: Float,
    r: Float, g: Float, b: Float,
    emissive: Float,
    radialT: Float,
  ) {
    ensureCapacity(FLOATS_PER_VERTEX)
    var i = floatCount
    data[i++] = px; data[i++] = py; data[i++] = pz
    data[i++] = nx; data[i++] = ny; data[i++] = nz
    data[i++] = r; data[i++] = g; data[i++] = b
    data[i++] = emissive
    data[i++] = radialT
    floatCount = i
  }

  fun addTri(
    ax: Float, ay: Float, az: Float,
    bx: Float, by: Float, bz: Float,
    cx: Float, cy: Float, cz: Float,
    r: Float, g: Float, b: Float,
    emissive: Float = 0f,
    radialT: Float = -1f,
  ) {
    val ux = bx - ax; val uy = by - ay; val uz = bz - az
    val vx = cx - ax; val vy = cy - ay; val vz = cz - az
    var nx = uy * vz - uz * vy
    var ny = uz * vx - ux * vz
    var nz = ux * vy - uy * vx
    val nLen = sqrt(max(1e-8f, nx * nx + ny * ny + nz * nz))
    nx /= nLen; ny /= nLen; nz /= nLen
    pushVertex(ax, ay, az, nx, ny, nz, r, g, b, emissive, radialT)
    pushVertex(bx, by, bz, nx, ny, nz, r, g, b, emissive, radialT)
    pushVertex(cx, cy, cz, nx, ny, nz, r, g, b, emissive, radialT)
  }

  fun addTriGradRadial(
    ax: Float, ay: Float, az: Float, ta: Float, ra: Float, ga: Float, ba: Float,
    bx: Float, by: Float, bz: Float, tb: Float, rb: Float, gb: Float, bb: Float,
    cx: Float, cy: Float, cz: Float, tc: Float, rc: Float, gc: Float, bc: Float,
    emissive: Float,
  ) {
    val ux = bx - ax; val uy = by - ay; val uz = bz - az
    val vx = cx - ax; val vy = cy - ay; val vz = cz - az
    var nx = uy * vz - uz * vy
    var ny = uz * vx - ux * vz
    var nz = ux * vy - uy * vx
    val nLen = sqrt(max(1e-8f, nx * nx + ny * ny + nz * nz))
    nx /= nLen; ny /= nLen; nz /= nLen
    pushVertex(ax, ay, az, nx, ny, nz, ra, ga, ba, emissive, ta)
    pushVertex(bx, by, bz, nx, ny, nz, rb, gb, bb, emissive, tb)
    pushVertex(cx, cy, cz, nx, ny, nz, rc, gc, bc, emissive, tc)
  }

  fun addQuad(
    x0: Float, y0: Float, z0: Float,
    x1: Float, y1: Float, z1: Float,
    x2: Float, y2: Float, z2: Float,
    x3: Float, y3: Float, z3: Float,
    r: Float, g: Float, b: Float,
    emissive: Float = 0f,
    radialT: Float = -1f,
  ) {
    addTri(x0, y0, z0, x1, y1, z1, x2, y2, z2, r, g, b, emissive, radialT)
    addTri(x0, y0, z0, x2, y2, z2, x3, y3, z3, r, g, b, emissive, radialT)
  }

  /**
   * Builds a tapered polygonal frustum around the local `Z` spire axis (`z0 -> z1`, radii `r0 -> r1`).
   */
  fun addSpireFrustumZ(
    sides: Int,
    z0: Float,
    z1: Float,
    r0: Float,
    r1: Float,
    r: Float,
    g: Float,
    b: Float,
    emissive: Float = 0f,
    capBottom: Boolean = false,
    capTop: Boolean = false,
    angleOffset: Float = 0f,
  ) {
    val step = (PI.toFloat() * 2f) / sides.toFloat()
    for (i in 0 until sides) {
      val a0 = angleOffset + i * step
      val a1 = angleOffset + (i + 1) * step
      val c0 = cos(a0); val s0 = sin(a0)
      val c1 = cos(a1); val s1 = sin(a1)
      if (r0 > 1e-4f && r1 > 1e-4f) {
        addQuad(
          c0 * r0, s0 * r0, z0,
          c1 * r0, s1 * r0, z0,
          c1 * r1, s1 * r1, z1,
          c0 * r1, s0 * r1, z1,
          r, g, b, emissive,
        )
      } else if (r1 <= 1e-4f) {
        addTri(
          c0 * r0, s0 * r0, z0,
          c1 * r0, s1 * r0, z0,
          0f, 0f, z1,
          r, g, b, emissive,
        )
      } else {
        addTri(
          0f, 0f, z0,
          c1 * r1, s1 * r1, z1,
          c0 * r1, s0 * r1, z1,
          r, g, b, emissive,
        )
      }
      if (capTop && r1 > 1e-4f) {
        addTri(0f, 0f, z1, c0 * r1, s0 * r1, z1, c1 * r1, s1 * r1, z1, r * 1.12f, g * 1.12f, b * 1.12f, emissive)
      }
      if (capBottom && r0 > 1e-4f) {
        addTri(0f, 0f, z0, c1 * r0, s1 * r0, z0, c0 * r0, s0 * r0, z0, r * 0.82f, g * 0.82f, b * 0.82f, emissive)
      }
    }
  }

  /**
   * Builds a beveled 4-sided diamond/chamfered 3D ring lying in the local `(X, Y)` disc plane.
   */
  fun addBeveledRingXY(
    segments: Int,
    majorR: Float,
    halfRadialW: Float,
    halfHeightZ: Float,
    hullR: Float, hullG: Float, hullB: Float,
    innerTrimR: Float, innerTrimG: Float, innerTrimB: Float,
    innerEmissive: Float,
    outerTrimR: Float, outerTrimG: Float, outerTrimB: Float,
    outerEmissive: Float,
  ) {
    val step = (PI.toFloat() * 2f) / segments.toFloat()
    val rIn = majorR - halfRadialW
    val rMid = majorR
    val rOut = majorR + halfRadialW
    for (i in 0 until segments) {
      val a0 = i * step
      val a1 = (i + 1) * step
      val c0 = cos(a0); val s0 = sin(a0)
      val c1 = cos(a1); val s1 = sin(a1)

      // Top-inner slope (brushed titanium)
      addQuad(
        c0 * rIn, s0 * rIn, 0f,
        c1 * rIn, s1 * rIn, 0f,
        c1 * rMid, s1 * rMid, halfHeightZ,
        c0 * rMid, s0 * rMid, halfHeightZ,
        innerTrimR, innerTrimG, innerTrimB, innerEmissive,
      )
      // Top-outer slope (armored hull)
      addQuad(
        c0 * rMid, s0 * rMid, halfHeightZ,
        c1 * rMid, s1 * rMid, halfHeightZ,
        c1 * rOut, s1 * rOut, 0f,
        c0 * rOut, s0 * rOut, 0f,
        hullR, hullG, hullB, 0f,
      )
      // Bottom-outer slope
      addQuad(
        c0 * rOut, s0 * rOut, 0f,
        c1 * rOut, s1 * rOut, 0f,
        c1 * rMid, s1 * rMid, -halfHeightZ,
        c0 * rMid, s0 * rMid, -halfHeightZ,
        outerTrimR, outerTrimG, outerTrimB, outerEmissive,
      )
      // Bottom-inner slope
      addQuad(
        c0 * rMid, s0 * rMid, -halfHeightZ,
        c1 * rMid, s1 * rMid, -halfHeightZ,
        c1 * rIn, s1 * rIn, 0f,
        c0 * rIn, s0 * rIn, 0f,
        hullR * 0.82f, hullG * 0.82f, hullB * 0.86f, 0f,
      )
    }
  }

  fun addOrientedBoxXY(
    angleRad: Float,
    rStart: Float,
    rEnd: Float,
    halfTangentialW: Float,
    zBottom: Float,
    zTop: Float,
    r: Float, g: Float, b: Float,
    emissive: Float = 0f,
  ) {
    val ca = cos(angleRad); val sa = sin(angleRad)
    val px = -sa * halfTangentialW; val py = ca * halfTangentialW

    val x0L = ca * rStart + px; val y0L = sa * rStart + py
    val x0R = ca * rStart - px; val y0R = sa * rStart - py
    val x1L = ca * rEnd + px; val y1L = sa * rEnd + py
    val x1R = ca * rEnd - px; val y1R = sa * rEnd - py

    // Top (+Z)
    addQuad(x0R, y0R, zTop, x1R, y1R, zTop, x1L, y1L, zTop, x0L, y0L, zTop, r, g, b, emissive)
    // Bottom (-Z)
    addQuad(x0L, y0L, zBottom, x1L, y1L, zBottom, x1R, y1R, zBottom, x0R, y0R, zBottom, r * 0.78f, g * 0.78f, b * 0.82f, emissive)
    // Outer end (rEnd)
    addQuad(x1R, y1R, zBottom, x1L, y1L, zBottom, x1L, y1L, zTop, x1R, y1R, zTop, r * 1.08f, g * 1.08f, b * 1.08f, emissive)
    // Inner end (rStart)
    addQuad(x0L, y0L, zBottom, x0R, y0R, zBottom, x0R, y0R, zTop, x0L, y0L, zTop, r * 0.88f, g * 0.88f, b * 0.88f, emissive)
    // Left flank
    addQuad(x0L, y0L, zBottom, x0L, y0L, zTop, x1L, y1L, zTop, x1L, y1L, zBottom, r * 0.92f, g * 0.92f, b * 0.95f, emissive)
    // Right flank
    addQuad(x1R, y1R, zBottom, x1R, y1R, zTop, x0R, y0R, zTop, x0R, y0R, zBottom, r * 0.92f, g * 0.92f, b * 0.95f, emissive)
  }

  fun toFloatBuffer(): FloatBuffer {
    val buf = ByteBuffer.allocateDirect(floatCount * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
    buf.put(data, 0, floatCount)
    buf.position(0)
    return buf
  }

  companion object {
    const val FLOATS_PER_VERTEX = 11
    const val STRIDE_BYTES = FLOATS_PER_VERTEX * 4

    /**
     * Builds the central dark-titanium octagonal `NEXUS` Command Citadel Spire with twinkling
     * cyan/gold micro-windows, central reactor heart, and 4 armored docking pylons.
     */
    fun buildCentralSpireMesh(): StationMeshBuilder {
      val m = StationMeshBuilder(12288)
      // Dark brushed titanium-obsidian palette (#0D1729 / #1C2E4A / #2A4365)
      val darkHullR = 0.052f; val darkHullG = 0.092f; val darkHullB = 0.165f
      val midHullR = 0.110f; val midHullG = 0.180f; val midHullB = 0.292f
      val litBevelR = 0.175f; val litBevelG = 0.275f; val litBevelB = 0.420f

      // Lower deep-space reactor keel (-0.34 -> -0.05)
      m.addSpireFrustumZ(8, -0.34f, -0.22f, 0.0f, 0.055f, darkHullR, darkHullG, darkHullB)
      m.addSpireFrustumZ(8, -0.22f, -0.09f, 0.055f, 0.115f, midHullR, midHullG, midHullB, capTop = true)
      // Glowing lower keel cyan thruster/stabilizer band
      m.addSpireFrustumZ(8, -0.105f, -0.088f, 0.117f, 0.119f, 0.0f, 0.96f, 0.83f, emissive = 1.35f)

      // Central Reactor Heart & Octagonal Command Citadel (-0.09 -> +0.16)
      m.addSpireFrustumZ(8, -0.09f, -0.02f, 0.138f, 0.156f, darkHullR, darkHullG, darkHullB, capBottom = true)
      // Glowing Solar-Gold & Cyan Reactor Core Band around z = -0.01..+0.03
      m.addSpireFrustumZ(12, -0.018f, 0.026f, 0.148f, 0.148f, 0.05f, 0.95f, 0.88f, emissive = 1.65f)
      m.addSpireFrustumZ(8, 0.026f, 0.125f, 0.156f, 0.122f, midHullR, midHullG, midHullB, capTop = true)
      m.addSpireFrustumZ(8, 0.125f, 0.225f, 0.102f, 0.072f, litBevelR, litBevelG, litBevelB, capTop = true)
      // Upper Command Bridge & Tall Sensor Mast (+0.225 -> +0.40)
      m.addSpireFrustumZ(8, 0.225f, 0.305f, 0.052f, 0.028f, midHullR, midHullG, midHullB)
      m.addSpireFrustumZ(6, 0.305f, 0.405f, 0.018f, 0.002f, litBevelR, litBevelG, litBevelB)
      // Apex Beacon Tip (HDR Solar-Gold / Cyan)
      m.addSpireFrustumZ(8, 0.385f, 0.415f, 0.014f, 0.0f, 0.98f, 0.82f, 0.15f, emissive = 1.85f)

      // Twinkling Cyan & Solar-Gold Micro-Windows across the 8 Citadel Faces
      val octStep = (PI.toFloat() * 2f) / 8f
      for (face in 0 until 8) {
        val midAng = (face + 0.5f) * octStep
        for (tier in 0 until 3) {
          val zBot = 0.042f + tier * 0.025f
          val zTop = zBot + 0.012f
          val tierFrac = (zBot - 0.026f) / (0.125f - 0.026f)
          val faceR = (0.156f + (0.122f - 0.156f) * tierFrac) * cos(octStep * 0.5f) + 0.003f
          val isGold = (face + tier) % 3 == 0
          val wr = if (isGold) 0.98f else 0.00f
          val wg = if (isGold) 0.80f else 0.96f
          val wb = if (isGold) 0.12f else 0.85f
          m.addOrientedBoxXY(
            angleRad = midAng,
            rStart = faceR - 0.002f,
            rEnd = faceR + 0.003f,
            halfTangentialW = 0.018f,
            zBottom = zBot,
            zTop = zTop,
            r = wr, g = wg, b = wb,
            emissive = 1.42f,
          )
        }
      }

      // 8 Radial Collar Anchor Buttresses around z = -0.015..+0.025 (from r = 0.135 -> 0.185)
      for (k in 0 until 8) {
        val ang = k * (PI.toFloat() * 0.25f)
        m.addOrientedBoxXY(
          angleRad = ang,
          rStart = 0.130f,
          rEnd = 0.186f,
          halfTangentialW = if (k % 2 == 0) 0.022f else 0.015f,
          zBottom = -0.018f,
          zTop = 0.026f,
          r = midHullR, g = midHullG, b = midHullB,
        )
        m.addOrientedBoxXY(
          angleRad = ang,
          rStart = 0.168f,
          rEnd = 0.192f,
          halfTangentialW = 0.010f,
          zBottom = 0.026f,
          zTop = 0.035f,
          r = 0.98f, g = 0.80f, b = 0.10f,
          emissive = 1.35f,
        )
      }
      return m
    }

    /**
     * Builds the Outer Sovereign Docking Ring (`R = 0.44f`) with 8 armored docking clamps.
     */
    fun buildOuterDockingRingMesh(): StationMeshBuilder {
      val m = StationMeshBuilder(8192)
      m.addBeveledRingXY(
        segments = 44,
        majorR = 0.44f,
        halfRadialW = 0.032f,
        halfHeightZ = 0.019f,
        hullR = 0.115f, hullG = 0.190f, hullB = 0.305f,
        innerTrimR = 0.05f, innerTrimG = 0.82f, innerTrimB = 0.96f,
        innerEmissive = 1.22f,
        outerTrimR = 0.075f, outerTrimG = 0.130f, outerTrimB = 0.220f,
        outerEmissive = 0f,
      )
      for (k in 0 until 8) {
        val ang = k * (PI.toFloat() * 0.25f)
        m.addOrientedBoxXY(
          angleRad = ang,
          rStart = 0.395f,
          rEnd = 0.485f,
          halfTangentialW = 0.020f,
          zBottom = -0.024f,
          zTop = 0.024f,
          r = 0.165f, g = 0.260f, b = 0.395f,
        )
        // Solar-gold docking beacon light on each clamp
        m.addOrientedBoxXY(
          angleRad = ang,
          rStart = 0.402f,
          rEnd = 0.422f,
          halfTangentialW = 0.009f,
          zBottom = 0.024f,
          zTop = 0.031f,
          r = 0.98f, g = 0.80f, b = 0.10f,
          emissive = 1.45f,
        )
      }
      return m
    }

    /**
     * Builds the Inner Counter-Rotating Gyro Ring (`R = 0.28f`).
     */
    fun buildInnerGyroRingMesh(): StationMeshBuilder {
      val m = StationMeshBuilder(6144)
      m.addBeveledRingXY(
        segments = 36,
        majorR = 0.28f,
        halfRadialW = 0.022f,
        halfHeightZ = 0.014f,
        hullR = 0.140f, hullG = 0.225f, hullB = 0.355f,
        innerTrimR = 0.98f, innerTrimG = 0.80f, innerTrimB = 0.12f,
        innerEmissive = 1.28f,
        outerTrimR = 0.00f, outerTrimG = 0.96f, outerTrimB = 0.83f,
        outerEmissive = 1.15f,
      )
      for (k in 0 until 4) {
        val ang = k * (PI.toFloat() * 0.5f) + 0.39f
        m.addOrientedBoxXY(
          angleRad = ang,
          rStart = 0.248f,
          rEnd = 0.312f,
          halfTangentialW = 0.015f,
          zBottom = -0.018f,
          zTop = 0.018f,
          r = 0.20f, g = 0.31f, b = 0.46f,
        )
      }
      return m
    }

    /**
     * Builds the **3D Containment Shield Ring (`R = 0.92f`) + 8 Golden-Cyan 3D Stabilizer Beams +
     * 8 Outward-Bracing 3D Emitter Chevrons** visibly pushing outward to hold back storm collapse.
     */
    fun buildContainmentAssemblyMesh(): StationMeshBuilder {
      val m = StationMeshBuilder(16384)
      val shieldR = SANCTUARY_SHIELD_RADIUS

      // 1. Crisp 3D Containment Shield Torus Ring at R = 0.92f
      m.addBeveledRingXY(
        segments = 64,
        majorR = shieldR,
        halfRadialW = 0.018f,
        halfHeightZ = 0.012f,
        hullR = 0.08f, hullG = 0.28f, hullB = 0.44f,
        innerTrimR = 0.00f, innerTrimG = 0.96f, innerTrimB = 0.85f,
        innerEmissive = 1.52f,
        outerTrimR = 0.22f, outerTrimG = 0.74f, outerTrimB = 0.98f,
        outerEmissive = 1.38f,
      )

      // 2. 8 Golden-Cyan 3D Stabilizer Beams (r = 0.175f -> 0.905f) + 8 Outward-Bracing Emitter Chevrons
      val beamSteps = 6
      val rStart = 0.175f
      val rEnd = shieldR - 0.015f
      for (k in 0 until 8) {
        val ang = k * (PI.toFloat() * 0.25f)
        val ca = cos(ang)
        val sa = sin(ang)
        val px = -sa
        val py = ca

        // Segmented 3D diamond-cross-section energy beam from Solar-Gold collar emitter to Bioluminescent Cyan shield rim
        for (s in 0 until beamSteps) {
          val t0 = s.toFloat() / beamSteps.toFloat()
          val t1 = (s + 1).toFloat() / beamSteps.toFloat()
          val rad0 = rStart + (rEnd - rStart) * t0
          val rad1 = rStart + (rEnd - rStart) * t1
          val w0 = 0.0105f * (1f - 0.22f * t0)
          val w1 = 0.0105f * (1f - 0.22f * t1)
          val h0 = 0.0085f * (1f - 0.20f * t0)
          val h1 = 0.0085f * (1f - 0.20f * t1)

          // Smooth gradient from Solar-Gold (#FACC15) near station collar -> Cyan (#00F5D4) into containment rim
          val goldMix0 = (1f - t0 * 1.45f).coerceIn(0f, 1f)
          val goldMix1 = (1f - t1 * 1.45f).coerceIn(0f, 1f)
          val rCol0 = 0.00f * (1f - goldMix0) + 0.98f * goldMix0
          val gCol0 = 0.96f * (1f - goldMix0) + 0.82f * goldMix0
          val bCol0 = 0.84f * (1f - goldMix0) + 0.12f * goldMix0
          val rCol1 = 0.00f * (1f - goldMix1) + 0.98f * goldMix1
          val gCol1 = 0.96f * (1f - goldMix1) + 0.82f * goldMix1
          val bCol1 = 0.84f * (1f - goldMix1) + 0.12f * goldMix1

          val top0x = ca * rad0; val top0y = sa * rad0; val top0z = h0
          val top1x = ca * rad1; val top1y = sa * rad1; val top1z = h1
          val bot0x = ca * rad0; val bot0y = sa * rad0; val bot0z = -h0
          val bot1x = ca * rad1; val bot1y = sa * rad1; val bot1z = -h1
          val lft0x = ca * rad0 + px * w0; val lft0y = sa * rad0 + py * w0
          val lft1x = ca * rad1 + px * w1; val lft1y = sa * rad1 + py * w1
          val rgt0x = ca * rad0 - px * w0; val rgt0y = sa * rad0 - py * w0
          val rgt1x = ca * rad1 - px * w1; val rgt1y = sa * rad1 - py * w1

          val beamEmissive = 1.48f
          // Top-left facet
          m.addTriGradRadial(top0x, top0y, top0z, t0, rCol0, gCol0, bCol0, lft0x, lft0y, 0f, t0, rCol0, gCol0, bCol0, lft1x, lft1y, 0f, t1, rCol1, gCol1, bCol1, beamEmissive)
          m.addTriGradRadial(top0x, top0y, top0z, t0, rCol0, gCol0, bCol0, lft1x, lft1y, 0f, t1, rCol1, gCol1, bCol1, top1x, top1y, top1z, t1, rCol1, gCol1, bCol1, beamEmissive)
          // Top-right facet
          m.addTriGradRadial(rgt0x, rgt0y, 0f, t0, rCol0, gCol0, bCol0, top0x, top0y, top0z, t0, rCol0, gCol0, bCol0, top1x, top1y, top1z, t1, rCol1, gCol1, bCol1, beamEmissive)
          m.addTriGradRadial(rgt0x, rgt0y, 0f, t0, rCol0, gCol0, bCol0, top1x, top1y, top1z, t1, rCol1, gCol1, bCol1, rgt1x, rgt1y, 0f, t1, rCol1, gCol1, bCol1, beamEmissive)
          // Bottom-left facet
          m.addTriGradRadial(lft0x, lft0y, 0f, t0, rCol0, gCol0, bCol0, bot0x, bot0y, bot0z, t0, rCol0, gCol0, bCol0, bot1x, bot1y, bot1z, t1, rCol1, gCol1, bCol1, beamEmissive)
          m.addTriGradRadial(lft0x, lft0y, 0f, t0, rCol0, gCol0, bCol0, bot1x, bot1y, bot1z, t1, rCol1, gCol1, bCol1, lft1x, lft1y, 0f, t1, rCol1, gCol1, bCol1, beamEmissive)
          // Bottom-right facet
          m.addTriGradRadial(bot0x, bot0y, bot0z, t0, rCol0, gCol0, bCol0, rgt0x, rgt0y, 0f, t0, rCol0, gCol0, bCol0, rgt1x, rgt1y, 0f, t1, rCol1, gCol1, bCol1, beamEmissive)
          m.addTriGradRadial(bot0x, bot0y, bot0z, t0, rCol0, gCol0, bCol0, rgt1x, rgt1y, 0f, t1, rCol1, gCol1, bCol1, bot1x, bot1y, bot1z, t1, rCol1, gCol1, bCol1, beamEmissive)
        }

        // Armored Pylon Node Socket on the Containment Ring (r = 0.89f..0.95f)
        m.addOrientedBoxXY(
          angleRad = ang,
          rStart = shieldR - 0.030f,
          rEnd = shieldR + 0.026f,
          halfTangentialW = 0.026f,
          zBottom = -0.018f,
          zTop = 0.018f,
          r = 0.125f, g = 0.215f, b = 0.345f,
        )

        // 3D Outward-Bracing V-Shaped Emitter Chevron (r = 0.935f -> 1.035f) visibly bracing back the outer storm!
        val chevBaseR = shieldR + 0.015f
        val chevTipR = shieldR + 0.108f
        val chevWingSpan = 0.068f
        val tipX = ca * chevTipR
        val tipY = sa * chevTipR
        val wingLx = ca * chevBaseR + px * chevWingSpan
        val wingLy = sa * chevBaseR + py * chevWingSpan
        val wingRx = ca * chevBaseR - px * chevWingSpan
        val wingRy = sa * chevBaseR - py * chevWingSpan
        val throatX = ca * (chevBaseR + 0.028f)
        val throatY = sa * (chevBaseR + 0.028f)

        // Top armored solar-gold & cyan chevron wings
        m.addTri(throatX, throatY, 0.016f, wingLx, wingLy, 0.004f, tipX, tipY, 0.0f, 0.98f, 0.82f, 0.14f, emissive = 1.46f, radialT = 1.0f)
        m.addTri(throatX, throatY, 0.016f, tipX, tipY, 0.0f, wingRx, wingRy, 0.004f, 0.00f, 0.96f, 0.85f, emissive = 1.52f, radialT = 1.0f)
        // Bottom chevron wings
        m.addTri(throatX, throatY, -0.016f, tipX, tipY, 0.0f, wingLx, wingLy, -0.004f, 0.00f, 0.82f, 0.92f, emissive = 1.32f, radialT = 1.0f)
        m.addTri(throatX, throatY, -0.016f, wingRx, wingRy, -0.004f, tipX, tipY, 0.0f, 0.98f, 0.78f, 0.12f, emissive = 1.32f, radialT = 1.0f)
      }
      return m
    }
  }
}

/**
 * Live 3D Playfield Ship Entity undocking, patrolling in 3D inside `shieldR`, and docking back at the
 * central 3D `NEXUS` Orbital Station.
 */
private class HubTrafficShip(
  var shipId: Int = 1,
  var routeType: Int = 0, // 0 = UNDOCK_TO_PATROL, 1 = SANCTUARY_ORBIT_PATROL, 2 = PATROL_TO_DOCK
  var progress: Float = 0f,
  var durationSec: Float = 6.5f,
  var p0x: Float = 0f, var p0y: Float = 0f, var p0z: Float = 0f,
  var p1x: Float = 0f, var p1y: Float = 0f, var p1z: Float = 0f,
  var p2x: Float = 0f, var p2y: Float = 0f, var p2z: Float = 0f,
  var p3x: Float = 0f, var p3y: Float = 0f, var p3z: Float = 0f,
  var curX: Float = 0f, var curY: Float = 0f, var curZ: Float = 0f,
  var dirX: Float = 1f, var dirY: Float = 0f, var dirZ: Float = 0f,
  var bankRad: Float = 0f,
  var orbitAnchorAngle: Float = 0f,
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
 * Hardware-accelerated OpenGL ES 3.0 (`#version 300 es`) `Abyssal-Garden` 3D Safe-Zone Sanctuary Renderer:
 * - Choreographer hardware VSYNC pacing (`eglSwapInterval(1)` + EMA `dt` smoothing).
 * - Zero tie-dye magenta/rainbow swirls:
 *   - Inside `r < shieldR`: Calm volumetric `#020814` -> `#041428` abyssal space-ocean clarity with angled
 *     light shafts, gentle cyan/sapphire caustic shimmer (`0.10..0.22`), crisp `1.0x` starlight, and drifting
 *     cyan/gold depth motes.
 *   - At `r == shieldR`: Braced `#00F5D4` / `#38BDF8` 3D Containment Shield Ring (`R = 0.92f`) with 8
 *     golden-cyan 3D stabilizer beams & 8 outward-bracing emitter chevrons radiating outward ripples.
 *   - Outside `r > shieldR`: Dark encroaching abyssal storm clouds (`#050914`, `#0B192E`, `#1E3A5F` / `#0284C7`
 *     ridges + fine ice-cyan veins) pressing inward and physically held back from collapsing in.
 * - High-contrast 3D Multi-Ring `NEXUS` Orbital Citadel + live `128x128` Playfield ships patrolling in 3D under
 *   hardware `GL_DEPTH_TEST`, finished with half-res `>1.05` HDR bloom & haze-free ACES Filmic Tonemap.
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
    private var progStation = 0
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

    // 4 Static 3D Station & Containment Mesh Groups
    private var vaoSpire = 0
    private var vboSpire = 0
    private var countSpire = 0

    private var vaoOuterRing = 0
    private var vboOuterRing = 0
    private var countOuterRing = 0

    private var vaoInnerRing = 0
    private var vboInnerRing = 0
    private var countInnerRing = 0

    private var vaoContainment = 0
    private var vboContainment = 0
    private var countContainment = 0

    private var texShipAtlas = 0
    private var bakedCustomShipVersion = -1
    private var bakedHullTrimStyle = -1
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

    private val trafficRng = Random(0x68AB54L)
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

        progScene = linkProgram("progScene", FULLSCREEN_TRI_VERT_GLSL, ABYSSAL_SANCTUARY_SCENE_FRAG_GLSL)
        progSceneUpsample = linkProgram("progSceneUpsample", FULLSCREEN_TRI_VERT_GLSL, ABYSSAL_SCENE_UPSAMPLE_FRAG_GLSL)
        progStation = linkProgram("progStation", STATION_VERT_GLSL, STATION_FRAG_GLSL)
        progShipHull = linkProgram("progShipHull", SHIP_HULL_VERT_GLSL, SHIP_HULL_FRAG_GLSL)
        progShipTrail = linkProgram("progShipTrail", SHIP_TRAIL_VERT_GLSL, SHIP_TRAIL_FRAG_GLSL)
        progBright = linkProgram("progBright", FULLSCREEN_TRI_VERT_GLSL, BLOOM_BRIGHT_EXTRACT_FRAG_GLSL)
        progBlur = linkProgram("progBlur", FULLSCREEN_TRI_VERT_GLSL, BLOOM_GAUSSIAN_BLUR_FRAG_GLSL)
        progComposite = linkProgram("progComposite", FULLSCREEN_TRI_VERT_GLSL, FINAL_ACES_COMPOSITE_FRAG_GLSL)

        if (progScene == 0 || progSceneUpsample == 0 || progStation == 0 || progBright == 0 || progBlur == 0 || progComposite == 0) {
          Log.e(
            "NexusSpaceGlView",
            "Core GL program link failed: scene=$progScene upsample=$progSceneUpsample station=$progStation bright=$progBright blur=$progBlur comp=$progComposite",
          )
          return false
        }

        initStaticStationMeshes()
        initDynamicShipBuffers()
        true
      }.onFailure { err ->
        Log.e("NexusSpaceGlView", "initEglAndGl failed", err)
      }.getOrDefault(false)
    }

    private fun uploadStaticStationMesh(mesh: StationMeshBuilder): IntArray {
      val vaoArr = IntArray(1)
      val vboArr = IntArray(1)
      GLES30.glGenVertexArrays(1, vaoArr, 0)
      GLES30.glGenBuffers(1, vboArr, 0)
      val vao = vaoArr[0]
      val vbo = vboArr[0]
      val buf = mesh.toFloatBuffer()
      val stride = StationMeshBuilder.STRIDE_BYTES

      GLES30.glBindVertexArray(vao)
      GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
      GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, mesh.floatCount * 4, buf, GLES30.GL_STATIC_DRAW)

      // aPos (vec3)
      GLES30.glEnableVertexAttribArray(0)
      GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
      // aNormal (vec3)
      GLES30.glEnableVertexAttribArray(1)
      GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
      // aColor (vec3)
      GLES30.glEnableVertexAttribArray(2)
      GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, stride, 6 * 4)
      // aEmissiveRadial (vec2: emissive, radialT)
      GLES30.glEnableVertexAttribArray(3)
      GLES30.glVertexAttribPointer(3, 2, GLES30.GL_FLOAT, false, stride, 9 * 4)

      GLES30.glBindVertexArray(0)
      GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
      return intArrayOf(vao, vbo, mesh.vertexCount)
    }

    private fun initStaticStationMeshes() {
      val spireTriple = uploadStaticStationMesh(StationMeshBuilder.buildCentralSpireMesh())
      vaoSpire = spireTriple[0]; vboSpire = spireTriple[1]; countSpire = spireTriple[2]

      val outerTriple = uploadStaticStationMesh(StationMeshBuilder.buildOuterDockingRingMesh())
      vaoOuterRing = outerTriple[0]; vboOuterRing = outerTriple[1]; countOuterRing = outerTriple[2]

      val innerTriple = uploadStaticStationMesh(StationMeshBuilder.buildInnerGyroRingMesh())
      vaoInnerRing = innerTriple[0]; vboInnerRing = innerTriple[1]; countInnerRing = innerTriple[2]

      val containTriple = uploadStaticStationMesh(StationMeshBuilder.buildContainmentAssemblyMesh())
      vaoContainment = containTriple[0]; vboContainment = containTriple[1]; countContainment = containTriple[2]
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
      val curTrimStyle = RiftSpriteBaker.activeHullTrimStyle.coerceIn(0, 5)
      if (
        texShipAtlas != 0 &&
          curCustomVer == bakedCustomShipVersion &&
          curTrimStyle == bakedHullTrimStyle
      ) {
        return
      }
      bakedCustomShipVersion = curCustomVer
      bakedHullTrimStyle = curTrimStyle

      val tilePx = 256
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
            trimStyleOverride = curTrimStyle,
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

    private fun sanctuaryLocalToWorldX(lu: Float, lv: Float, ln: Float): Float {
      val f = DEFAULT_SANCTUARY_FRAME
      return STATION_CENTER_X + f.ux * lu + f.vx * lv + f.nx * ln
    }

    private fun sanctuaryLocalToWorldY(lu: Float, lv: Float, ln: Float): Float {
      val f = DEFAULT_SANCTUARY_FRAME
      return STATION_CENTER_Y + f.uy * lu + f.vy * lv + f.ny * ln
    }

    private fun sanctuaryLocalToWorldZ(lu: Float, lv: Float, ln: Float): Float {
      val f = DEFAULT_SANCTUARY_FRAME
      return STATION_CENTER_Z + f.uz * lu + f.vz * lv + f.nz * ln
    }

    /**
     * Assigns smooth 3D Bezier routes inside the tilted `R = 0.92f` Safe-Zone Sanctuary:
     * - `0`: Undock from the Outer Sovereign Docking Ring (`r ~ 0.24..0.44`) out to patrol orbit (`r ~ 0.62..0.80`).
     * - `1`: Curved 3D Sanctuary Patrol Arc inside `shieldR` (varying local `ln` height so ships pass
     *        both in front of and behind the 3D `NEXUS` Spire & Docking Rings under `GL_DEPTH_TEST`!).
     * - `2`: Return approach banking inward from Sanctuary Patrol to dock at the station collar (`r ~ 0.22`).
     */
    private fun assignNextRoute(ship: HubTrafficShip, slotIdx: Int, randomizeInitialProgress: Boolean) {
      val nextType =
        when {
          randomizeInitialProgress -> slotIdx % 3
          ship.routeType == 0 -> 1
          ship.routeType == 1 -> if (trafficRng.nextFloat() < 0.45f) 1 else 2
          else -> 0
        }
      ship.routeType = nextType
      ship.durationSec = 5.4f + trafficRng.nextFloat() * 2.6f
      ship.progress = if (randomizeInitialProgress) trafficRng.nextFloat() * 0.90f else 0f
      ship.trailInitialized = false

      if (randomizeInitialProgress) {
        ship.orbitAnchorAngle = (slotIdx * 1.18f) + trafficRng.nextFloat() * (PI.toFloat() * 2f)
      }
      val dir = if (slotIdx % 2 == 0) 1f else -1f
      val a0 = ship.orbitAnchorAngle
      val dockR = 0.24f + (slotIdx % 2) * 0.16f
      val patrolR = 0.56f + (slotIdx % 4) * 0.068f
      val hWave = if (slotIdx % 2 == 0) 0.09f else -0.08f

      when (nextType) {
        0 -> { // UNDOCK_TO_PATROL
          val a1 = a0 + dir * 0.55f
          val a2 = a0 + dir * 1.15f
          val a3 = a0 + dir * 1.65f
          ship.orbitAnchorAngle = a3
          val u0 = cos(a0) * dockR; val v0 = sin(a0) * dockR; val n0 = 0.02f
          val u1 = cos(a1) * (dockR + 0.14f); val v1 = sin(a1) * (dockR + 0.14f); val n1 = hWave * 0.65f
          val u2 = cos(a2) * (patrolR * 0.92f); val v2 = sin(a2) * (patrolR * 0.92f); val n2 = hWave
          val u3 = cos(a3) * patrolR; val v3 = sin(a3) * patrolR; val n3 = hWave * 0.45f
          ship.p0x = sanctuaryLocalToWorldX(u0, v0, n0); ship.p0y = sanctuaryLocalToWorldY(u0, v0, n0); ship.p0z = sanctuaryLocalToWorldZ(u0, v0, n0)
          ship.p1x = sanctuaryLocalToWorldX(u1, v1, n1); ship.p1y = sanctuaryLocalToWorldY(u1, v1, n1); ship.p1z = sanctuaryLocalToWorldZ(u1, v1, n1)
          ship.p2x = sanctuaryLocalToWorldX(u2, v2, n2); ship.p2y = sanctuaryLocalToWorldY(u2, v2, n2); ship.p2z = sanctuaryLocalToWorldZ(u2, v2, n2)
          ship.p3x = sanctuaryLocalToWorldX(u3, v3, n3); ship.p3y = sanctuaryLocalToWorldY(u3, v3, n3); ship.p3z = sanctuaryLocalToWorldZ(u3, v3, n3)
        }
        1 -> { // SANCTUARY_ORBIT_PATROL (inside r < 0.84f, weaving smoothly in 3D depth)
          val sweep = dir * (1.75f + trafficRng.nextFloat() * 0.55f)
          val a1 = a0 + sweep * 0.33f
          val a2 = a0 + sweep * 0.67f
          val a3 = a0 + sweep
          ship.orbitAnchorAngle = a3
          val u0 = cos(a0) * patrolR; val v0 = sin(a0) * patrolR; val n0 = hWave * 0.45f
          val u1 = cos(a1) * (patrolR * 1.08f); val v1 = sin(a1) * (patrolR * 1.08f); val n1 = -hWave * 0.85f
          val u2 = cos(a2) * (patrolR * 1.06f); val v2 = sin(a2) * (patrolR * 1.06f); val n2 = hWave * 0.90f
          val u3 = cos(a3) * patrolR; val v3 = sin(a3) * patrolR; val n3 = -hWave * 0.35f
          ship.p0x = sanctuaryLocalToWorldX(u0, v0, n0); ship.p0y = sanctuaryLocalToWorldY(u0, v0, n0); ship.p0z = sanctuaryLocalToWorldZ(u0, v0, n0)
          ship.p1x = sanctuaryLocalToWorldX(u1, v1, n1); ship.p1y = sanctuaryLocalToWorldY(u1, v1, n1); ship.p1z = sanctuaryLocalToWorldZ(u1, v1, n1)
          ship.p2x = sanctuaryLocalToWorldX(u2, v2, n2); ship.p2y = sanctuaryLocalToWorldY(u2, v2, n2); ship.p2z = sanctuaryLocalToWorldZ(u2, v2, n2)
          ship.p3x = sanctuaryLocalToWorldX(u3, v3, n3); ship.p3y = sanctuaryLocalToWorldY(u3, v3, n3); ship.p3z = sanctuaryLocalToWorldZ(u3, v3, n3)
        }
        else -> { // PATROL_TO_DOCK
          val a1 = a0 + dir * 0.55f
          val a2 = a0 + dir * 1.10f
          val a3 = a0 + dir * 1.55f
          ship.orbitAnchorAngle = a3
          val u0 = cos(a0) * patrolR; val v0 = sin(a0) * patrolR; val n0 = -hWave * 0.35f
          val u1 = cos(a1) * (patrolR * 0.86f); val v1 = sin(a1) * (patrolR * 0.86f); val n1 = hWave * 0.55f
          val u2 = cos(a2) * (dockR + 0.12f); val v2 = sin(a2) * (dockR + 0.12f); val n2 = 0.04f
          val u3 = cos(a3) * dockR; val v3 = sin(a3) * dockR; val n3 = 0.015f
          ship.p0x = sanctuaryLocalToWorldX(u0, v0, n0); ship.p0y = sanctuaryLocalToWorldY(u0, v0, n0); ship.p0z = sanctuaryLocalToWorldZ(u0, v0, n0)
          ship.p1x = sanctuaryLocalToWorldX(u1, v1, n1); ship.p1y = sanctuaryLocalToWorldY(u1, v1, n1); ship.p1z = sanctuaryLocalToWorldZ(u1, v1, n1)
          ship.p2x = sanctuaryLocalToWorldX(u2, v2, n2); ship.p2y = sanctuaryLocalToWorldY(u2, v2, n2); ship.p2z = sanctuaryLocalToWorldZ(u2, v2, n2)
          ship.p3x = sanctuaryLocalToWorldX(u3, v3, n3); ship.p3y = sanctuaryLocalToWorldY(u3, v3, n3); ship.p3z = sanctuaryLocalToWorldZ(u3, v3, n3)
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
      ship.bankRad = (ship.bankRad * 0.85f + turnSignal.coerceIn(-0.65f, 0.65f) * 0.15f)
    }

    private fun stepTrafficSimulation(dtSec: Float) {
      val onlineIds = onlinePilotShipIds
      val desiredCount = max(5, onlineIds.size).coerceAtMost(24)

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

        s.progress += dtSec / max(2.8f, s.durationSec)
        if (s.progress >= 1f) {
          assignNextRoute(s, i, randomizeInitialProgress = false)
        }
        evalBezierPosAndVel(s, s.progress.coerceIn(0f, 1f))

        if (!s.trailInitialized) {
          for (k in 0 until HubTrafficShip.TRAIL_POINTS) {
            val back = k * 0.016f
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
          if (headDist >= 0.018f) {
            for (k in HubTrafficShip.TRAIL_POINTS - 1 downTo 1) {
              s.trailHistoryX[k] = s.trailHistoryX[k - 1]
              s.trailHistoryY[k] = s.trailHistoryY[k - 1]
              s.trailHistoryZ[k] = s.trailHistoryZ[k - 1]
            }
            s.trailHistoryX[0] = s.curX - s.dirX * 0.026f
            s.trailHistoryY[0] = s.curY - s.dirY * 0.026f
            s.trailHistoryZ[0] = s.curZ - s.dirZ * 0.026f
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

    private fun drawStationMeshGroup(
      vao: Int,
      vertCount: Int,
      spinRad: Float,
      extraPitchX: Float = 0f,
    ) {
      if (vao == 0 || vertCount <= 0) return
      val f =
        if (abs(extraPitchX) > 1e-4f) {
          buildSanctuaryFrame3D(SANCTUARY_PITCH_X_RAD + extraPitchX, SANCTUARY_ROLL_Z_RAD)
        } else {
          DEFAULT_SANCTUARY_FRAME
        }
      val cs = cos(spinRad)
      val ss = sin(spinRad)
      val rUx = f.ux * cs + f.vx * ss
      val rUy = f.uy * cs + f.vy * ss
      val rUz = f.uz * cs + f.vz * ss
      val rVx = -f.ux * ss + f.vx * cs
      val rVy = -f.uy * ss + f.vy * cs
      val rVz = -f.uz * ss + f.vz * cs

      GLES30.glUniform3f(GLES30.glGetUniformLocation(progStation, "uAxisU"), rUx, rUy, rUz)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progStation, "uAxisV"), rVx, rVy, rVz)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progStation, "uAxisN"), f.nx, f.ny, f.nz)

      GLES30.glBindVertexArray(vao)
      GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertCount)
    }

    private fun drawOrbitalCitadelAndContainment3D(basis: CameraBasis3D, timeSec: Float) {
      if (progStation == 0) return
      GLES30.glDisable(GLES30.GL_BLEND)
      GLES30.glEnable(GLES30.GL_DEPTH_TEST)
      GLES30.glDepthFunc(GLES30.GL_LEQUAL)
      GLES30.glDepthMask(true)

      GLES30.glUseProgram(progStation)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progStation, "uOrigin"), STATION_CENTER_X, STATION_CENTER_Y, STATION_CENTER_Z)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progStation, "uCamPos"), basis.camX, basis.camY, basis.camZ)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progStation, "uCamFwd"), basis.fwdX, basis.fwdY, basis.fwdZ)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progStation, "uCamRight"), basis.rightX, basis.rightY, basis.rightZ)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progStation, "uCamUp"), basis.upX, basis.upY, basis.upZ)
      GLES30.glUniform1f(GLES30.glGetUniformLocation(progStation, "uTanHalfFovY"), basis.tanHalfFovY)
      GLES30.glUniform1f(GLES30.glGetUniformLocation(progStation, "uAspect"), basis.aspect)
      GLES30.glUniform1f(GLES30.glGetUniformLocation(progStation, "uTime"), timeSec)

      // 1. Central Octagonal Command Spire (slow regal rotation)
      drawStationMeshGroup(vaoSpire, countSpire, spinRad = timeSec * 0.14f)
      // 2. Outer Sovereign Docking Ring (R = 0.44f)
      drawStationMeshGroup(vaoOuterRing, countOuterRing, spinRad = timeSec * 0.30f)
      // 3. Inner Counter-Rotating Gyro Ring (R = 0.28f with subtle secondary gimbal cant)
      drawStationMeshGroup(vaoInnerRing, countInnerRing, spinRad = -timeSec * 0.52f, extraPitchX = 0.11f * sin(timeSec * 0.35f))
      // 4. 3D Containment Shield Ring (R = 0.92f) + 8 Golden-Cyan 3D Stabilizer Beams + 8 Outward Emitter Chevrons
      drawStationMeshGroup(vaoContainment, countContainment, spinRad = timeSec * 0.045f)

      GLES30.glBindVertexArray(0)
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
        var dockGlow = 0f
        if (s.routeType == 0 && p < 0.16f) { // UNDOCKING
          val k = (p / 0.16f).coerceIn(0f, 1f)
          hullScale = 0.38f + 0.62f * k
          dockGlow = sin(k * PI.toFloat()) * 0.85f
        } else if (s.routeType == 2 && p > 0.84f) { // DOCKING
          val k = ((p - 0.84f) / 0.16f).coerceIn(0f, 1f)
          hullScale = 1f - 0.62f * k
          dockGlow = sin(k * PI.toFloat()) * 0.85f
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

        val halfSize = 0.064f * hullScale
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

        val boostGlow = 1.0f + dockGlow * 1.15f
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

        var prevLx = s.curX - nx * 0.72f + wx * (0.014f * hullScale)
        var prevLy = s.curY - ny * 0.72f + wy * (0.014f * hullScale)
        var prevLz = s.curZ - nz * 0.72f + wz * (0.014f * hullScale)
        var prevRx = s.curX - nx * 0.72f - wx * (0.014f * hullScale)
        var prevRy = s.curY - ny * 0.72f - wy * (0.014f * hullScale)
        var prevRz = s.curZ - nz * 0.72f - wz * (0.014f * hullScale)
        var prevAlpha = 0.82f * hullScale

        for (k in 0 until HubTrafficShip.TRAIL_POINTS) {
          val frac = 1f - (k + 1).toFloat() / HubTrafficShip.TRAIL_POINTS.toFloat()
          val wHalf = 0.013f * frac * hullScale
          val curAlpha = 0.62f * frac * frac * hullScale
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
      }

      if (hullFloats > 0) {
        shipHullBuffer.clear()
        shipHullBuffer.put(shipHullScratch, 0, hullFloats)
        shipHullBuffer.position(0)

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
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
        GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE)
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

      val f = DEFAULT_SANCTUARY_FRAME
      val focusGain = if (!isSplashMode && (focusedTearIndex == 0 || activeDiveTearIdx >= 0)) 1.16f else 1.0f
      val spinPhase = timeSec * 0.045f
      val cs = cos(spinPhase)
      val ss = sin(spinPhase)
      val planeUx = f.ux * cs + f.vx * ss
      val planeUy = f.uy * cs + f.vy * ss
      val planeUz = f.uz * cs + f.vz * ss
      val planeVx = -f.ux * ss + f.vx * cs
      val planeVy = -f.uy * ss + f.vy * cs
      val planeVz = -f.uz * ss + f.vz * cs

      // PASS 1A-1: 0.62x Split-Res Abyssal-Garden Safe-Zone Sanctuary + Outer Storm Backdrop -> fboPlasma
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
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progScene, "uPlaneOrigin"), STATION_CENTER_X, STATION_CENTER_Y, STATION_CENTER_Z)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progScene, "uPlaneU"), planeUx, planeUy, planeUz)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progScene, "uPlaneV"), planeVx, planeVy, planeVz)
      GLES30.glUniform3f(GLES30.glGetUniformLocation(progScene, "uPlaneN"), f.nx, f.ny, f.nz)
      GLES30.glUniform2f(GLES30.glGetUniformLocation(progScene, "uShieldCfg"), SANCTUARY_SHIELD_RADIUS, focusGain)
      GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)

      // PASS 1A-2: Full-Res 1.0x Upsample + Crisp Multi-Depth Starlight -> fboScene
      GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboScene)
      GLES30.glViewport(0, 0, w, h)
      GLES30.glColorMask(true, true, true, true)
      GLES30.glDepthMask(true)
      GLES30.glDisable(GLES30.GL_DEPTH_TEST)
      GLES30.glDisable(GLES30.GL_BLEND)
      GLES30.glClearColor(0.008f, 0.031f, 0.078f, 1.0f)
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
      GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)

      // PASS 1B: 3D Multi-Ring NEXUS Orbital Citadel + 8 Golden-Cyan Stabilizer Beams + Containment Shield Ring
      drawOrbitalCitadelAndContainment3D(basis, timeSec)

      // PASS 1C: Live Real-Ship Sanctuary Patrols (128x128 Playfield Ships under GL_DEPTH_TEST)
      drawLiveShipTraffic3D(basis)

      GLES30.glDisable(GLES30.GL_DEPTH_TEST)
      GLES30.glColorMask(true, true, true, true)

      val bw = max(1, w / 2)
      val bh = max(1, h / 2)
      GLES30.glBindVertexArray(emptyVao)

      // PASS 2: Half-Res High-Threshold (>1.05) HDR Bright-Pass Extraction -> fboBloomA
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
      GLES30.glUniform2f(GLES30.glGetUniformLocation(progBlur, "uDir"), 1.28f / bw.toFloat(), 0f)
      GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)

      // PASS 4: Vertical Separable 9-Tap Gaussian Blur -> fboBloomA
      GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboBloomA)
      GLES30.glViewport(0, 0, bw, bh)
      GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texBloomB)
      GLES30.glUniform2f(GLES30.glGetUniformLocation(progBlur, "uDir"), 0f, 1.28f / bh.toFloat())
      GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)

      // PASS 5: Final ACES Filmic Tonemap + Crisp HDR Bloom + Bioluminescent Abyssal Motes -> Default Screen
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
          val vaos = intArrayOf(emptyVao, vaoSpire, vaoOuterRing, vaoInnerRing, vaoContainment, vaoShipHull, vaoShipTrail)
          for (v in vaos) if (v != 0) GLES30.glDeleteVertexArrays(1, intArrayOf(v), 0)
          val vbos = intArrayOf(vboSpire, vboOuterRing, vboInnerRing, vboContainment, vboShipHull, vboShipTrail)
          for (b in vbos) if (b != 0) GLES30.glDeleteBuffers(1, intArrayOf(b), 0)
          val progs = intArrayOf(progScene, progSceneUpsample, progStation, progShipHull, progShipTrail, progBright, progBlur, progComposite)
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

private const val STATION_VERT_GLSL =
  """#version 300 es
precision highp float;
precision highp int;

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec3 aColor;
layout(location = 3) in vec2 aEmissiveRadial;

uniform vec3 uOrigin;
uniform vec3 uAxisU;
uniform vec3 uAxisV;
uniform vec3 uAxisN;

uniform vec3 uCamPos;
uniform vec3 uCamFwd;
uniform vec3 uCamRight;
uniform vec3 uCamUp;
uniform float uTanHalfFovY;
uniform float uAspect;

out vec3 vWorldPos;
out vec3 vWorldNormal;
out vec3 vColor;
out vec2 vEmissiveRadial;

void main() {
    vec3 worldPos = uOrigin + uAxisU * aPos.x + uAxisV * aPos.y + uAxisN * aPos.z;
    vec3 worldNrm = normalize(uAxisU * aNormal.x + uAxisV * aNormal.y + uAxisN * aNormal.z);

    vWorldPos = worldPos;
    vWorldNormal = worldNrm;
    vColor = aColor;
    vEmissiveRadial = aEmissiveRadial;

    vec3 d = worldPos - uCamPos;
    float vx = dot(d, uCamRight);
    float vy = dot(d, uCamUp);
    float vz = max(0.08, dot(d, uCamFwd));

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
precision highp int;

in vec3 vWorldPos;
in vec3 vWorldNormal;
in vec3 vColor;
in vec2 vEmissiveRadial;

uniform vec3 uCamPos;
uniform float uTime;

out vec4 fragColor;

void main() {
    vec3 N = normalize(vWorldNormal);
    vec3 V = normalize(uCamPos - vWorldPos);
    if (dot(N, V) < 0.0) {
        N = -N;
    }

    // Key light aligned with angled abyssal-garden volumetric shafts from upper-left-front
    vec3 keyDir = normalize(vec3(-0.52, 0.72, 0.68));
    // Subtle warm solar-gold rim bounce from lower-right
    vec3 fillDir = normalize(vec3(0.64, -0.38, 0.55));

    float diffKey = max(dot(N, keyDir), 0.0);
    float diffFill = max(dot(N, fillDir), 0.0);

    vec3 halfKey = normalize(keyDir + V);
    float specKey = pow(max(dot(N, halfKey), 0.0), 42.0);
    float fresnel = pow(1.0 - max(dot(N, V), 0.0), 3.4);

    vec3 lit =
        vColor * (0.24 + 0.88 * diffKey + 0.26 * diffFill) +
        vec3(0.55, 0.88, 1.0) * (specKey * 0.44 + fresnel * 0.18) +
        vec3(0.98, 0.80, 0.16) * (diffFill * 0.08);

    float emissive = vEmissiveRadial.x;
    if (emissive > 0.01) {
        float pulse = 0.90 + 0.10 * sin(uTime * 2.6 + vWorldPos.x * 7.0 + vWorldPos.y * 5.0);
        // Outward-traveling stabilizer wave along the 8 golden-cyan beams (vEmissiveRadial.y >= 0.0)
        if (vEmissiveRadial.y >= 0.0) {
            float wave = 0.72 + 0.46 * pow(0.5 + 0.5 * sin(vEmissiveRadial.y * 10.5 - uTime * 3.8), 2.2);
            pulse *= wave;
        }
        lit = mix(lit, vColor * emissive * pulse, 0.88);
    }

    fragColor = vec4(max(lit, vec3(0.0)), 1.0);
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

/**
 * Volumetric `Abyssal-Garden` 3D Safe-Zone Sanctuary + Braced Outer Storm Backdrop (`progScene`):
 * - Zero tie-dye magenta/pink/purple spiral vortex!
 * - `r < shieldR`: Calm `#020814` -> `#041428` abyssal space-ocean clarity with angled volumetric light shafts
 *   and gentle cyan/sapphire caustic shimmer (`0.10..0.22`).
 * - `r ~ shieldR`: Crisp `#00F5D4` / `#38BDF8` braced containment energy barrier with outward deflection waves.
 * - `r > shieldR`: Dark encroaching abyssal storm clouds (`#050914`, `#0B192E`, `#1E3A5F` / `#0284C7` ridges
 *   + fine ice-cyan static veins) pressing inward and physically held back from collapsing in!
 */
private const val ABYSSAL_SANCTUARY_SCENE_FRAG_GLSL =
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

uniform vec3 uPlaneOrigin;
uniform vec3 uPlaneU;
uniform vec3 uPlaneV;
uniform vec3 uPlaneN;
uniform vec2 uShieldCfg; // (shieldRadius = 0.92, focusGain)

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
        p = p * 2.03 + vec3(6.7, -4.3, 5.4);
        amp *= 0.5;
    }
    return v;
}

float causticPattern(vec2 p, float t) {
    float n1 = noise3(vec3(p * 3.8, t * 0.28));
    float n2 = noise3(vec3(p * 3.8 + vec2(2.4, -1.7), -t * 0.24 + 4.1));
    float ridge = 1.0 - abs(n1 - n2) * 3.6;
    return pow(clamp(ridge, 0.0, 1.0), 2.8);
}

void main() {
    float aspect = uResolution.x / max(1.0, uResolution.y);
    vec2 ndc = vUv * 2.0 - 1.0;

    vec3 rayDir = normalize(
        uCamFwd +
        ndc.x * aspect * uTanHalfFovY * uCamRight +
        ndc.y * uTanHalfFovY * uCamUp
    );

    float shieldR = uShieldCfg.x;
    float focusGain = uShieldCfg.y;

    // Calm #020814 -> #041428 abyssal space-ocean base gradient
    float vertGrad = clamp(vUv.y * 0.85 + 0.15, 0.0, 1.0);
    vec3 abyssalDeep = vec3(0.008, 0.031, 0.078);   // #020814
    vec3 abyssalMid  = vec3(0.016, 0.078, 0.157);   // #041428
    vec3 col = mix(abyssalDeep, abyssalMid, vertGrad * 0.65);

    // Subtle angled volumetric god-ray light shafts descending from upper-left across the sanctuary
    vec2 screenMetric = vec2(ndc.x * aspect, ndc.y);
    float shaftCoord = dot(screenMetric, vec2(0.76, 0.65));
    float shaftNoise = fbm4(vec3(shaftCoord * 3.2 - uTime * 0.05, screenMetric.y * 0.85, uTime * 0.09));
    float shaftBands = pow(0.5 + 0.5 * sin(shaftCoord * 7.5 + shaftNoise * 3.2 - uTime * 0.18), 3.2);
    float shaftTopFade = smoothstep(-0.75, 0.92, ndc.y - ndc.x * 0.38);

    float denom = dot(rayDir, uPlaneN);
    if (abs(denom) > 1e-4) {
        float tHit = dot(uPlaneOrigin - uCamPos, uPlaneN) / denom;
        if (tHit > 0.15) {
            vec3 hit = uCamPos + rayDir * tHit - uPlaneOrigin;
            vec2 q = vec2(dot(hit, uPlaneU), dot(hit, uPlaneV));
            float r = length(q);
            vec2 dir = q / max(r, 1e-4);

            float insideMask = 1.0 - smoothstep(shieldR - 0.035, shieldR + 0.035, r);
            float outsideMask = smoothstep(shieldR - 0.015, shieldR + 0.065, r);

            // 1. INSIDE SANCTUARY (r < shieldR): Calm Abyssal-Garden Clarity + Low-Intensity Cyan/Sapphire Caustics
            if (insideMask > 0.002) {
                float c1 = causticPattern(q * 1.45, uTime);
                float c2 = causticPattern(q * 2.55 + vec2(1.3, -0.8), uTime * 1.18);
                float caustic = (c1 * 0.65 + c2 * 0.35);

                vec3 sapphireCol = vec3(0.055, 0.647, 0.914); // #0EA5E9
                vec3 cyanCol     = vec3(0.000, 0.961, 0.831); // #00F5D4

                // Strictly subtle 0.10..0.21 caustic shimmer + soft volumetric light shafts + gentle station heart aura
                float radialCalm = smoothstep(shieldR, 0.15, r);
                vec3 causticGlow = mix(sapphireCol, cyanCol, 0.42) * (0.09 + 0.12 * caustic) * (0.45 + 0.55 * radialCalm);
                vec3 godRays = mix(sapphireCol, cyanCol, 0.35) * (shaftBands * shaftTopFade * 0.13);
                float stationHalo = exp(-r * r * 6.5) * 0.16;
                vec3 heartGlow = mix(vec3(0.012, 0.412, 0.631), cyanCol, 0.38) * stationHalo;

                col += (causticGlow + godRays + heartGlow) * insideMask * focusGain;
            }

            // 2. BRACED CONTAINMENT PERIMETER (r ~ shieldR): Crisp #00F5D4 / #38BDF8 Barrier + Outward Deflection Waves
            float distFromShield = r - shieldR;
            float rimCore = exp(-abs(distFromShield) * 28.0);
            // 8 Nodal Brace Hot-Spots where the 8 Stabilizer Beams & Chevrons brace the Containment Ring!
            // Compute cos(8*theta) via Chebyshev without atan seam!
            float c2t = dir.x * dir.x - dir.y * dir.y;
            float s2t = 2.0 * dir.x * dir.y;
            float c4t = c2t * c2t - s2t * s2t;
            float s4t = 2.0 * c2t * s2t;
            float cos8Theta = c4t * c4t - s4t * s4t;
            float braceNodes = 0.72 + 0.38 * max(0.0, cos8Theta);

            // Outward-pushing deflection ripples (r = shieldR -> shieldR + 0.18)
            float outwardZone = smoothstep(-0.01, 0.02, distFromShield) * exp(-max(distFromShield, 0.0) * 11.5);
            float outwardRipple = pow(0.5 + 0.5 * sin(distFromShield * 44.0 - uTime * 3.6), 3.0) * outwardZone;

            vec3 rimCyan = vec3(0.000, 0.961, 0.831);
            vec3 rimSky  = vec3(0.220, 0.741, 0.973);
            vec3 rimGold = vec3(0.980, 0.804, 0.082);
            vec3 barrierGlow =
                mix(rimSky, rimCyan, 0.62) * (rimCore * 0.92 * braceNodes + outwardRipple * 0.52) +
                rimGold * (rimCore * pow(max(0.0, cos8Theta), 3.0) * 0.34);
            col += barrierGlow * focusGain;

            // 3. OUTSIDE PERIMETER (r > shieldR): Dark Billowing Abyssal Storm Clouds Pressing Inward & Held Back!
            if (outsideMask > 0.002) {
                float safeR = max(r, shieldR * 0.95);
                float logR = log(safeR);
                // +uTime in radial coordinate advects storm clouds INWARD until they pile up against shieldR!
                float stormA = fbm4(vec3(dir * 3.2, logR * 2.8 + uTime * 0.38));
                float stormB = fbm4(vec3(dir * 6.4 + vec2(3.1, -2.4), logR * 4.2 + uTime * 0.54 - stormA * 1.4));

                // Storm compression pile-up right against the outer side of shieldR
                float pileUp = exp(-max(distFromShield, 0.0) * 3.2);
                float farFade = exp(-max(distFromShield - 0.55, 0.0) * 0.95);

                vec3 stormVoid  = vec3(0.020, 0.035, 0.078); // #050914 dark abyssal storm shadow
                vec3 stormSlate = vec3(0.043, 0.098, 0.180); // #0B192E heavy storm cloud body
                vec3 stormRidge = vec3(0.118, 0.227, 0.373); // #1E3A5F muted deep-cobalt ridge
                vec3 stormSteel = vec3(0.008, 0.518, 0.780); // #0284C7 muted storm-crest highlight

                float billow = smoothstep(0.32, 0.78, stormA * 0.65 + stormB * 0.35);
                vec3 stormCloud = mix(stormVoid, stormSlate, billow);
                stormCloud = mix(stormCloud, stormRidge, pow(billow, 2.2) * (0.45 + 0.55 * pileUp));
                stormCloud += stormSteel * (pow(stormB, 3.4) * pileUp * 0.28);

                // Occasional fine ice-cyan electric filaments crackling along outer storm ridges
                float filament = max(0.0, 1.0 - abs(stormA - stormB) * 5.2);
                float sparkFlicker = 0.55 + 0.45 * sin(uTime * 6.4 + dot(dir, vec2(8.3, -6.7)));
                vec3 iceFilament = vec3(0.55, 0.86, 1.0) * (pow(filament, 4.4) * pileUp * sparkFlicker * 0.68);

                vec3 outerTotal = (stormCloud + iceFilament) * farFade;
                col = mix(col, outerTotal + barrierGlow * 0.65, outsideMask * 0.92);
            }
        }
    } else {
        col += vec3(0.02, 0.10, 0.18) * (shaftBands * shaftTopFade * 0.08);
    }

    fragColor = vec4(max(col, vec3(0.0)), 1.0);
}
"""

private const val ABYSSAL_SCENE_UPSAMPLE_FRAG_GLSL =
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

vec2 hash22(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx + p3.yz) * p3.zy);
}

vec3 starFieldLayer(vec2 dirUv, float scale, float densityCut, float t) {
    vec2 gv = fract(dirUv * scale) - 0.5;
    vec2 id = floor(dirUv * scale);
    vec2 rnd = hash22(id);
    if (rnd.x < densityCut) return vec3(0.0);
    vec2 offs = (rnd - 0.5) * 0.68;
    float d = length(gv - offs);
    float twinkle = 0.76 + 0.24 * sin(t * (1.2 + rnd.y * 2.4) + rnd.x * 37.0);
    float core = smoothstep(0.074, 0.0, d) * twinkle;
    vec3 tint = mix(vec3(0.72, 0.90, 1.0), vec3(1.0, 0.92, 0.74), fract(rnd.y * 7.3));
    return tint * core * (0.58 + 0.78 * fract(rnd.x * 13.7));
}

vec3 sampleSanctuaryStars(vec3 rayDir, float t) {
    vec2 sph = rayDir.xy / max(0.32, abs(rayDir.z));
    vec3 stars = vec3(0.0);
    stars += starFieldLayer(sph + vec2(t * 0.0005, 0.0), 38.0, 0.85, t) * 0.90;
    stars += starFieldLayer(sph * 1.35 - vec2(0.0, t * 0.0004), 70.0, 0.88, t) * 0.68;
    stars += starFieldLayer(sph * 1.80 + vec2(4.2, -1.9), 118.0, 0.91, t) * 0.46;
    return stars;
}

void main() {
    vec3 col = texture(uPlasmaTex, vUv).rgb;

    float aspect = uResolution.x / max(1.0, uResolution.y);
    vec2 ndc = vUv * 2.0 - 1.0;
    vec3 rayDir = normalize(
        uCamFwd +
        ndc.x * aspect * uTanHalfFovY * uCamRight +
        ndc.y * uTanHalfFovY * uCamUp
    );

    vec3 stars = sampleSanctuaryStars(rayDir, uTime);
    float bgLum = dot(col, vec3(0.2126, 0.7152, 0.0722));
    col += stars * (0.46 + 0.34 * clamp(1.0 - bgLum * 0.75, 0.20, 1.0));

    fragColor = vec4(max(col, vec3(0.0)), 1.0);
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
    float knee = smoothstep(1.05, 2.15, maxCh);
    fragColor = vec4(s.rgb * knee, 1.0);
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

vec3 abyssalBioluminescentMotes(vec2 uv, float aspect, float t, vec2 parallax) {
    vec3 acc = vec3(0.0);
    for (int layer = 0; layer < 2; layer++) {
        float fl = float(layer);
        float scale = mix(8.5, 14.5, fl);
        vec2 drift = vec2(t * (0.008 + fl * 0.005), t * (0.010 + fl * 0.004));
        vec2 p = vec2(uv.x * aspect, uv.y) * scale + drift + parallax * (1.1 + fl * 0.7);
        vec2 id = floor(p);
        vec2 gv = fract(p) - 0.5;
        vec2 rnd = hash22(id + fl * 31.0);
        if (rnd.x > 0.84) {
            vec2 pos = (rnd - 0.5) * 0.62;
            float d = length(gv - pos);
            float pulse = 0.55 + 0.45 * sin(t * 1.85 + rnd.y * 23.0);
            float glow = smoothstep(0.078, 0.0, d) * pulse;
            vec3 c = mix(vec3(0.0, 0.96, 0.83), vec3(0.98, 0.82, 0.18), fract(rnd.y * 5.7) * 0.65);
            acc += c * glow * (0.24 - fl * 0.08);
        }
    }
    return acc;
}

void main() {
    vec3 scene = texture(uSceneTex, vUv).rgb;
    vec3 bloom = texture(uBloomTex, vUv).rgb;

    float aspect = uResolution.x / max(1.0, uResolution.y);
    float centerSoft = smoothstep(0.06, 0.24, length((vUv - vec2(0.5)) * vec2(aspect, 1.0)));
    vec3 motes = abyssalBioluminescentMotes(vUv, aspect, uTime, uParallax) * (0.45 + 0.55 * centerSoft);

    vec3 hdr = scene + bloom * 0.28 + motes;
    vec3 mapped = acesTonemap(hdr * 1.12);
    mapped = pow(mapped, vec3(0.96));
    mapped = smoothstep(vec3(0.004), vec3(0.996), mapped);

    vec2 vigUv = vUv * 2.0 - 1.0;
    float vig = clamp(1.0 - dot(vigUv, vigUv) * 0.09, 0.84, 1.0);
    mapped *= vig;

    fragColor = vec4(mapped * uDimFactor, 1.0);
}
"""
