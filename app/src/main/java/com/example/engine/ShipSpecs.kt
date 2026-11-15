package com.example.engine

import android.graphics.Color
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONObject

/**
 * Full 100-Point Rift Frame budget unlocked immediately at Level 20 (or with Developer Override).
 */
fun getRiftFramePointBudget(pilotLevel: Int, devOverride: Boolean = false): Int =
  if (pilotLevel >= 20 || devOverride) 100 else 100

data class CosmeticCatalogItem(
  val id: Int,
  val category: String,
  val name: String,
  val summary: String,
  val freeAtLevel: Int,
  val shardCost: Long,
  val accentColorHex: Int,
)

data class FrameSlotGateInfo(
  val slotId: String,
  val archetypeTitle: String,
  val freeAtLevel: Int,
  val shardCost: Long,
  val summary: String,
)

/**
 * 6-Pillar Lv 1-100 Career Progression & 3D Rift Shard Foundry Catalog.
 * Every cosmetic and Rift Frame bay unlocks 100% free via gameplay levels OR early via Rift Shards.
 * Zero combat stat boosts are ever sold for currency.
 */
object RiftProgressionCatalog {

  fun getRiftFramePointBudget(pilotLevel: Int, devOverride: Boolean = false): Int =
    com.example.engine.getRiftFramePointBudget(pilotLevel, devOverride)

  fun getRankTitleForLevel(level: Int): String {
    val clamped = level.coerceIn(1, 100)
    return when {
      clamped >= 100 -> "APEX ARCHON"
      clamped >= 90 -> "SOVEREIGN ACE"
      clamped >= 70 -> "RIFT COMMANDER"
      clamped >= 50 -> "DREADNOUGHT"
      clamped >= 35 -> "PHANTOM"
      clamped >= 20 -> "INTERCEPTOR"
      clamped >= 10 -> "VANGUARD"
      else -> "CADET"
    }
  }

  fun getNextRankMilestoneInfo(level: Int): Pair<Int, String> {
    val clamped = level.coerceIn(1, 100)
    return when {
      clamped < 10 -> 10 to "VANGUARD"
      clamped < 20 -> 20 to "INTERCEPTOR"
      clamped < 35 -> 35 to "PHANTOM"
      clamped < 50 -> 50 to "DREADNOUGHT"
      clamped < 70 -> 70 to "RIFT COMMANDER"
      clamped < 90 -> 90 to "SOVEREIGN ACE"
      clamped < 100 -> 100 to "APEX ARCHON"
      else -> 100 to "APEX ARCHON"
    }
  }

  /**
   * Per-Level Rift Shard Rewards:
   * - Every level 2..100 awards +30 Rift Shards
   * - Every 5th level awards +150 Milestone Bonus (+300 at Lv 20 Rift Frame unlock!)
   */
  fun getShardRewardForLevel(level: Int): Long {
    if (level < 2 || level > 100) return 0L
    val base = 30L
    val milestoneBonus =
      when {
        level == 20 -> 300L
        level % 5 == 0 -> 150L
        else -> 0L
      }
    return base + milestoneBonus
  }

  fun calculateTotalMilestoneShardsUpTo(level: Int): Long {
    val top = level.coerceIn(1, 100)
    if (top < 2) return 0L
    var sum = 0L
    for (lv in 2..top) {
      sum += getShardRewardForLevel(lv)
    }
    return sum
  }

  fun calculateUnclaimedMilestoneShards(highestClaimedLevel: Int, currentLevel: Int): Long {
    val startLv = highestClaimedLevel.coerceAtLeast(1) + 1
    val endLv = currentLevel.coerceIn(1, 100)
    if (startLv > endLv) return 0L
    var sum = 0L
    for (lv in startLv..endLv) {
      sum += getShardRewardForLevel(lv)
    }
    return sum
  }

  val killFinishers: List<CosmeticCatalogItem> =
    listOf(
      CosmeticCatalogItem(
        id = 0,
        category = "KILL_FINISHER",
        name = "Plasma Supernova",
        summary = "Multi-stage cyan-amber reactor detonation with shockwave rings.",
        freeAtLevel = 1,
        shardCost = 0L,
        accentColorHex = 0xFF00E5FF.toInt(),
      ),
      CosmeticCatalogItem(
        id = 1,
        category = "KILL_FINISHER",
        name = "Gravity Implosion",
        summary = "Collapses target hull inward before a high-velocity singularity snap.",
        freeAtLevel = 15,
        shardCost = 250L,
        accentColorHex = 0xFF818CF8.toInt(),
      ),
      CosmeticCatalogItem(
        id = 2,
        category = "KILL_FINISHER",
        name = "Solar Crown Nova",
        summary = "Golden coronal flare burst with radiant executioner callcard banner.",
        freeAtLevel = 35,
        shardCost = 450L,
        accentColorHex = 0xFFFFCA28.toInt(),
      ),
      CosmeticCatalogItem(
        id = 3,
        category = "KILL_FINISHER",
        name = "Void Rift Shatter",
        summary = "Crystalline amethyst rift fracture that splinters space around the wreck.",
        freeAtLevel = 60,
        shardCost = 700L,
        accentColorHex = 0xFFC084FC.toInt(),
      ),
      CosmeticCatalogItem(
        id = 4,
        category = "KILL_FINISHER",
        name = "Apex Singularity",
        summary = "Prismatic horizon collapse reserved for Sovereign and Archon aces.",
        freeAtLevel = 85,
        shardCost = 1000L,
        accentColorHex = 0xFFF43F5E.toInt(),
      ),
    )

  val hullTrims: List<CosmeticCatalogItem> =
    listOf(
      CosmeticCatalogItem(
        id = 0,
        category = "HULL_TRIM",
        name = "Standard Faction Trim",
        summary = "Brushed naval alloy chassis with tactical cyan reactor seams.",
        freeAtLevel = 1,
        shardCost = 0L,
        accentColorHex = 0xFF38BDF8.toInt(),
      ),
      CosmeticCatalogItem(
        id = 1,
        category = "HULL_TRIM",
        name = "Obsidian Ice-Cyan",
        summary = "Stealth matte-obsidian plating with cryogenic ice-cyan specular edges.",
        freeAtLevel = 10,
        shardCost = 200L,
        accentColorHex = 0xFF00F5D4.toInt(),
      ),
      CosmeticCatalogItem(
        id = 2,
        category = "HULL_TRIM",
        name = "Sovereign Solar Gold",
        summary = "Regal gilded auric bevels over deep gunmetal hull armor.",
        freeAtLevel = 30,
        shardCost = 400L,
        accentColorHex = 0xFFFACC15.toInt(),
      ),
      CosmeticCatalogItem(
        id = 3,
        category = "HULL_TRIM",
        name = "Rift Singularity Violet",
        summary = "Faceted amethyst crystal trim with pulsing ultraviolet phase conduits.",
        freeAtLevel = 55,
        shardCost = 650L,
        accentColorHex = 0xFFA855F7.toInt(),
      ),
    )

  val warpSignatures: List<CosmeticCatalogItem> =
    listOf(
      CosmeticCatalogItem(
        id = 0,
        category = "WARP_SIGNATURE",
        name = "Cyan Phase Aperture",
        summary = "Standard sector warp-in ring with clean electric-cyan convergence.",
        freeAtLevel = 1,
        shardCost = 0L,
        accentColorHex = 0xFF00F5D4.toInt(),
      ),
      CosmeticCatalogItem(
        id = 1,
        category = "WARP_SIGNATURE",
        name = "Solar Horizon Flash",
        summary = "Golden coronal warp arrival with twin radial flare arcs.",
        freeAtLevel = 25,
        shardCost = 300L,
        accentColorHex = 0xFFFFB300.toInt(),
      ),
      CosmeticCatalogItem(
        id = 2,
        category = "WARP_SIGNATURE",
        name = "Amethyst Rift Tear",
        summary = "Deep-space violet dimensional rift with crystalline phase sparks.",
        freeAtLevel = 45,
        shardCost = 500L,
        accentColorHex = 0xFFC084FC.toInt(),
      ),
      CosmeticCatalogItem(
        id = 3,
        category = "WARP_SIGNATURE",
        name = "Crimson Nova Entry",
        summary = "High-energy crimson shock-ring arrival signature.",
        freeAtLevel = 75,
        shardCost = 800L,
        accentColorHex = 0xFFFB7185.toInt(),
      ),
    )

  val hvtHalos: List<CosmeticCatalogItem> =
    listOf(
      CosmeticCatalogItem(
        id = 0,
        category = "HVT_HALO",
        name = "Tactical Amber Reticle",
        summary = "Classic #1 High-Value Target crown halo when holding top sector bounty.",
        freeAtLevel = 1,
        shardCost = 0L,
        accentColorHex = 0xFFFACC15.toInt(),
      ),
      CosmeticCatalogItem(
        id = 1,
        category = "HVT_HALO",
        name = "Sovereign Solar Crown",
        summary = "Rotating twin-ring auric halo with radiant sovereign chevrons.",
        freeAtLevel = 40,
        shardCost = 450L,
        accentColorHex = 0xFFFFD54F.toInt(),
      ),
      CosmeticCatalogItem(
        id = 2,
        category = "HVT_HALO",
        name = "Void Monarch Orbitals",
        summary = "Orbiting amethyst singularity nodes circling the #1 Bounty leader.",
        freeAtLevel = 65,
        shardCost = 700L,
        accentColorHex = 0xFFA855F7.toInt(),
      ),
      CosmeticCatalogItem(
        id = 3,
        category = "HVT_HALO",
        name = "Apex Archon Aegis",
        summary = "Prismatic cyan-crimson apex crown reserved for elite sector champions.",
        freeAtLevel = 95,
        shardCost = 1100L,
        accentColorHex = 0xFF00F5D4.toInt(),
      ),
    )

  val frameSlotGates: List<FrameSlotGateInfo> =
    listOf(
      FrameSlotGateInfo(
        slotId = "Frame A",
        archetypeTitle = "Rail Interceptor (100 Pts)",
        freeAtLevel = 20,
        shardCost = 0L,
        summary = "Unlocks automatically at Level 20 with the full 100-Point Parity Budget.",
      ),
      FrameSlotGateInfo(
        slotId = "Frame B",
        archetypeTitle = "Scatter Brawler (100 Pts)",
        freeAtLevel = 35,
        shardCost = 250L,
        summary = "Unlocks free at Lv 35 or instantly for 250 Rift Shards.",
      ),
      FrameSlotGateInfo(
        slotId = "Frame C",
        archetypeTitle = "Bank Specialist (100 Pts)",
        freeAtLevel = 50,
        shardCost = 400L,
        summary = "Unlocks free at Lv 50 or instantly for 400 Rift Shards.",
      ),
      FrameSlotGateInfo(
        slotId = "Frame D",
        archetypeTitle = "Stealth Phantom (100 Pts)",
        freeAtLevel = 75,
        shardCost = 600L,
        summary = "Unlocks free at Lv 75 or instantly for 600 Rift Shards.",
      ),
      FrameSlotGateInfo(
        slotId = "Frame E",
        archetypeTitle = "Stasis Archon (100 Pts)",
        freeAtLevel = 100,
        shardCost = 600L,
        summary = "Unlocks free at Lv 100 or instantly for 600 Rift Shards.",
      ),
    )

  fun getItemsForCategory(category: String): List<CosmeticCatalogItem> {
    return when (category.trim().uppercase(Locale.US)) {
      "KILL_FINISHER" -> killFinishers
      "HULL_TRIM" -> hullTrims
      "WARP_SIGNATURE" -> warpSignatures
      "HVT_HALO" -> hvtHalos
      else -> emptyList()
    }
  }

  fun getCosmeticItem(category: String, id: Int): CosmeticCatalogItem? {
    return getItemsForCategory(category).firstOrNull { it.id == id }
  }

  fun getFrameSlotGate(slotId: String): FrameSlotGateInfo {
    val norm = CustomShipLoadoutSlot.normalizeSlotId(slotId)
    return frameSlotGates.firstOrNull { it.slotId == norm } ?: frameSlotGates.first()
  }
}

enum class BuffAimMode {
  AIM_DIRECTIONAL,
  TAP_INSTANT,
}

enum class CustomBuffEffect(val label: String) {
  NONE("Pure Plasma Damage"),
  PULL("Gravity Vortex Pull"),
  PUSH("Shockwave Repel Knockback"),
  EMP("Flux-Drain EMP Lockout"),
  SLOW("Cryo Velocity Snare"),
  BARRIER("Permanent Hard-Light Barrier"),
  SPEED_BOOST("Hyper-Thruster Surge"),
  CLONE("Holographic Decoy Clone"),
}

data class SpecialBuffRegistryEntry(
  val id: String,
  val buffTypeIndex: Int,
  val officialName: String,
  val shortLabel: String,
  var chargesPerPickup: Int,
  val aimMode: BuffAimMode,
  val strokeColorHex: String,
  val fillColorArgb: Int,
  val summary: String,
  val isCustomForged: Boolean = false,
  var projectileSpeed: Float = 640f,
  var flightDuration: Float = 2.5f,
  var damage: Float = 480f,
  var blastRadius: Float = 200f,
  val specialEffect: CustomBuffEffect = CustomBuffEffect.NONE,
  val wallBounces: Int = 2,
  val promptUsed: String = "",
) {
  val strokeColorInt: Int
    get() =
      try {
        Color.parseColor(strokeColorHex)
      } catch (_: Exception) {
        0xFF00F5D4.toInt()
      }

  fun toJsonObject(): JSONObject =
    JSONObject().apply {
      put("id", id)
      put("buffTypeIndex", buffTypeIndex)
      put("officialName", officialName)
      put("shortLabel", shortLabel)
      put("chargesPerPickup", chargesPerPickup)
      put("aimMode", aimMode.name)
      put("strokeColorHex", strokeColorHex)
      put("fillColorArgb", fillColorArgb)
      put("summary", summary)
      put("isCustomForged", isCustomForged)
      put("projectileSpeed", projectileSpeed.toDouble())
      put("flightDuration", flightDuration.toDouble())
      put("damage", damage.toDouble())
      put("blastRadius", blastRadius.toDouble())
      put("specialEffect", specialEffect.name)
      put("wallBounces", wallBounces)
      put("promptUsed", promptUsed)
    }

  companion object {
    fun fillColorFromStroke(hex: String): Int {
      return try {
        val c = Color.parseColor(hex)
        Color.argb(165, (Color.red(c) * 0.35f).toInt(), (Color.green(c) * 0.35f).toInt(), (Color.blue(c) * 0.35f).toInt())
      } catch (_: Exception) {
        Color.argb(165, 16, 48, 78)
      }
    }

    fun fromJsonObject(obj: JSONObject): SpecialBuffRegistryEntry {
      val strokeHex = obj.optString("strokeColorHex", "#C084FC")
      val aimName = obj.optString("aimMode", BuffAimMode.AIM_DIRECTIONAL.name)
      val effName = obj.optString("specialEffect", CustomBuffEffect.PULL.name)
      return SpecialBuffRegistryEntry(
        id = obj.optString("id", "FORGED_BUFF_${System.currentTimeMillis()}"),
        buffTypeIndex = obj.optInt("buffTypeIndex", 11).coerceAtLeast(11),
        officialName = obj.optString("officialName", "GRAVITY WELL").take(20),
        shortLabel = obj.optString("shortLabel", "GRAVITY").take(8),
        chargesPerPickup = obj.optInt("chargesPerPickup", 2).coerceIn(1, 8),
        aimMode = BuffAimMode.entries.firstOrNull { it.name == aimName } ?: BuffAimMode.AIM_DIRECTIONAL,
        strokeColorHex = strokeHex,
        fillColorArgb = obj.optInt("fillColorArgb", fillColorFromStroke(strokeHex)),
        summary = obj.optString("summary", "Custom forged special buff."),
        isCustomForged = obj.optBoolean("isCustomForged", true),
        projectileSpeed = obj.optDouble("projectileSpeed", 420.0).toFloat().coerceIn(120f, 1600f),
        flightDuration = obj.optDouble("flightDuration", 3.0).toFloat().coerceIn(0.5f, 12.0f),
        damage = obj.optDouble("damage", 420.0).toFloat().coerceIn(0f, 2500f),
        blastRadius = obj.optDouble("blastRadius", 220.0).toFloat().coerceIn(40f, 650f),
        specialEffect = CustomBuffEffect.entries.firstOrNull { it.name == effName } ?: CustomBuffEffect.PULL,
        wallBounces = obj.optInt("wallBounces", 2).coerceIn(0, 6),
        promptUsed = obj.optString("promptUsed", ""),
      )
    }

    fun synthesizeFromPrompt(rawPrompt: String, nextBuffTypeIndex: Int): SpecialBuffRegistryEntry {
      val clean = rawPrompt.trim().ifEmpty { "Gravity Well slow violet orb that pulls nearby enemies inward for 3 seconds, 2 charges" }
      val lower = clean.lowercase(Locale.US)
      val hash = abs(clean.hashCode() + nextBuffTypeIndex * 4999)

      val effect =
        when {
          lower.contains("pull") || lower.contains("vortex") || lower.contains("gravity") || lower.contains("black hole") ->
            CustomBuffEffect.PULL
          lower.contains("push") || lower.contains("knock") || lower.contains("repel") || lower.contains("shockwave") ->
            CustomBuffEffect.PUSH
          lower.contains("emp") || lower.contains("drain") || lower.contains("ion") || lower.contains("disable") ->
            CustomBuffEffect.EMP
          lower.contains("slow") || lower.contains("cryo") || lower.contains("frost") || lower.contains("snare") || lower.contains("ice") ->
            CustomBuffEffect.SLOW
          lower.contains("wall") || lower.contains("barrier") || lower.contains("bulkhead") || lower.contains("shield") ->
            CustomBuffEffect.BARRIER
          lower.contains("speed") || lower.contains("boost") || lower.contains("dash") || lower.contains("surge") || lower.contains("turbo") ->
            CustomBuffEffect.SPEED_BOOST
          lower.contains("clone") || lower.contains("decoy") || lower.contains("phantom") || lower.contains("holo") ->
            CustomBuffEffect.CLONE
          else -> CustomBuffEffect.NONE
        }

      val aim =
        if (effect in setOf(CustomBuffEffect.SPEED_BOOST, CustomBuffEffect.CLONE) && lower.contains("instant")) {
          BuffAimMode.TAP_INSTANT
        } else {
          BuffAimMode.AIM_DIRECTIONAL
        }

      val strokeHex =
        when {
          lower.contains("violet") || lower.contains("purple") || effect == CustomBuffEffect.PULL -> "#C084FC"
          lower.contains("cyan") || lower.contains("frost") || lower.contains("ice") || effect == CustomBuffEffect.EMP -> "#22D3EE"
          lower.contains("emerald") || lower.contains("green") || effect == CustomBuffEffect.CLONE -> "#34D399"
          lower.contains("crimson") || lower.contains("red") || lower.contains("rose") -> "#FB7185"
          lower.contains("amber") || lower.contains("solar") || lower.contains("gold") || effect == CustomBuffEffect.PUSH -> "#FACC15"
          else -> "#FB923C"
        }

      val calledMatch = Regex("(?i)called\\s+([a-z0-9][a-z0-9 \\-]{2,18}?)(?:\\s+that|\\s+which|,|$)").find(clean)
      val officialName =
        (calledMatch?.groupValues?.getOrNull(1)?.trim()?.uppercase(Locale.US)
          ?: when (effect) {
            CustomBuffEffect.PULL -> "GRAVITY WELL"
            CustomBuffEffect.PUSH -> "SOLAR PULSE"
            CustomBuffEffect.EMP -> "ION LANCE"
            CustomBuffEffect.SLOW -> "CRYO WEB"
            CustomBuffEffect.BARRIER -> "AEGIS WALL"
            CustomBuffEffect.SPEED_BOOST -> "HYPER DRIVE"
            CustomBuffEffect.CLONE -> "MIRAGE CLONE"
            CustomBuffEffect.NONE -> "PLASMA ORB"
          }).take(20)

      val shortLabel = officialName.split(" ").firstOrNull()?.take(8)?.ifEmpty { "FORGED" } ?: "FORGED"

      val chargesMatch = Regex("(\\d+)\\s*charge").find(lower)
      val charges = (chargesMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: (2 + (hash % 2))).coerceIn(1, 8)

      val secMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:second|sec|s\\b)").find(lower)
      val duration = (secMatch?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 3.0f).coerceIn(0.8f, 10.0f)

      val dmgMatch = Regex("(\\d{2,4})\\s*(?:damage|dmg|flux)").find(lower)
      val dmg = (dmgMatch?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: if (effect == CustomBuffEffect.BARRIER) 0f else 440f).coerceIn(0f, 2000f)

      val speed =
        when {
          lower.contains("fast") || lower.contains("lance") -> 920f
          lower.contains("slow") -> 340f
          else -> 580f
        }
      val radius =
        when {
          lower.contains("wide") || lower.contains("large") -> 300f
          else -> 230f
        }
      val bounces = if (lower.contains("bounce")) 3 else 2
      val idSlug =
        "FORGED_" +
          officialName.replace(Regex("[^A-Z0-9]"), "_").trim('_').ifEmpty { "BUFF" } +
          "_$nextBuffTypeIndex"
      val summary =
        "${effect.label} • ${charges}x Charges • ${String.format(Locale.US, "%.1fs", duration)} • ${dmg.toInt()}⚡ DMG (${radius.toInt()}px)."

      return SpecialBuffRegistryEntry(
        id = idSlug,
        buffTypeIndex = nextBuffTypeIndex.coerceAtLeast(11),
        officialName = officialName,
        shortLabel = shortLabel,
        chargesPerPickup = charges,
        aimMode = aim,
        strokeColorHex = strokeHex,
        fillColorArgb = fillColorFromStroke(strokeHex),
        summary = summary,
        isCustomForged = true,
        projectileSpeed = speed,
        flightDuration = duration,
        damage = dmg,
        blastRadius = radius,
        specialEffect = effect,
        wallBounces = bounces,
        promptUsed = clean.take(160),
      )
    }
  }
}

/**
 * Ship Specification for all 10 Playable Hulls in RIFT (9 Signature Hulls + Ship #10 Modular Rift Frame).
 */
data class ShipSpec(
  val id: Int,
  val name: String,
  val role: String,
  val tacticalLore: String,
  val silhouetteDesc: String,
  val weaponSummary: String,
  val primaryTitle: String,
  val secondaryTitle: String,
  val primaryShortLabel: String,
  val secondaryShortLabel: String,
  var maxFlux: Float,
  var rechargeRate: Float,
  var topSpeed: Float,
  var afterburnerSpeed: Float,
  var thrust: Float,
  var turnRate: Float,
  var primaryCost: Float,
  var primaryCooldown: Float,
  var primaryDamage: Float,
  var primarySpeed: Float,
  val primaryStreams: Int,
  val primarySpreadRad: Float,
  val primaryBounces: Int,
  val isParallelTwin: Boolean = false,
  val hasBomb: Boolean,
  val bombType: String,
  var bombCost: Float,
  var bombCooldown: Float,
  var bombDamage: Float,
  val bombBounces: Int,
  var bombRecoil: Float,
  val supportsNativeMultifire: Boolean,
  val initialRepels: Int,
  val accentColorHex: Int,
  val speedPts: Int = 10,
  val thrustPts: Int = 10,
  val turnPts: Int = 10,
  val maxFluxPts: Int = 10,
  val rechargePts: Int = 10,
  val primaryPts: Int = 15,
  val secondaryPts: Int = 15,
  val isCustomModularFrame: Boolean = false,
  val activeCustomSlotId: String = "Frame A",
) {
  val totalHullStatPoints: Int
    get() = speedPts + thrustPts + turnPts + maxFluxPts + rechargePts

  val totalAllocatedPoints: Int
    get() = totalHullStatPoints + primaryPts + secondaryPts
}

data class SpatialAnomalyCodexEntry(
  val id: String,
  val title: String,
  val badge: String,
  val accentColorHex: Long,
  val radiusRule: String,
  val mechanicsSummary: String,
  val tacticalCounterplay: String,
)

data class RiftAnomalyCodexEntry(
  val id: String,
  val title: String,
  val classification: String,
  val specBadge: String,
  val accentHex: String,
  val effectSummary: String,
  val tacticalNote: String,
)

/**
 * Procedural Hull Visual Recipe generated by the Hangar Hull Forge (Ship #10).
 */
data class CustomHullVisualRecipe(
  val hullName: String = "RIFT FRAME",
  val silhouette: String = "DELTA_INTERCEPTOR",
  val wingSweep: Float = 1.0f,
  val chassisPlates: Int = 2,
  val primaryHex: String = "#1D4ED8",
  val trimHex: String = "#38BDF8",
  val coreGlowHex: String = "#00F5D4",
  val engineStyle: String = "TWIN_PLASMA",
  val promptUsed: String = "Default cobalt-cyan modular Rift Frame interceptor",
) {
  fun toJsonString(): String {
    return JSONObject()
      .apply {
        put("hullName", hullName)
        put("silhouette", silhouette)
        put("wingSweep", wingSweep.toDouble())
        put("chassisPlates", chassisPlates)
        put("primaryHex", primaryHex)
        put("trimHex", trimHex)
        put("coreGlowHex", coreGlowHex)
        put("engineStyle", engineStyle)
        put("promptUsed", promptUsed)
      }
      .toString()
  }

  companion object {
    fun ensureMinLuminanceHex(hex: String, minLum: Float): String {
      return try {
        val c = Color.parseColor(hex)
        val r = Color.red(c) / 255f
        val g = Color.green(c) / 255f
        val b = Color.blue(c) / 255f
        val lum = 0.2126f * r + 0.7152f * g + 0.0722f * b
        if (lum >= minLum) {
          hex
        } else {
          val boost = (minLum - lum).coerceIn(0f, 1f)
          val nr = ((r + boost * (1f - r)) * 255f).roundToInt().coerceIn(0, 255)
          val ng = ((g + boost * (1f - g)) * 255f).roundToInt().coerceIn(0, 255)
          val nb = ((b + boost * (1f - b)) * 255f).roundToInt().coerceIn(0, 255)
          String.format(Locale.US, "#%02X%02X%02X", nr, ng, nb)
        }
      } catch (_: Exception) {
        "#38BDF8"
      }
    }

    fun fromJsonString(raw: String?): CustomHullVisualRecipe {
      if (raw.isNullOrBlank()) return CustomHullVisualRecipe()
      return try {
        val obj = JSONObject(raw)
        CustomHullVisualRecipe(
          hullName = obj.optString("hullName", "RIFT FRAME").take(20),
          silhouette = obj.optString("silhouette", "DELTA_INTERCEPTOR"),
          wingSweep = obj.optDouble("wingSweep", 1.0).toFloat().coerceIn(0.65f, 1.35f),
          chassisPlates = obj.optInt("chassisPlates", 2).coerceIn(1, 4),
          primaryHex = obj.optString("primaryHex", "#1D4ED8"),
          trimHex = obj.optString("trimHex", "#38BDF8"),
          coreGlowHex = obj.optString("coreGlowHex", "#00F5D4"),
          engineStyle = obj.optString("engineStyle", "TWIN_PLASMA"),
          promptUsed = obj.optString("promptUsed", "Custom modular hull"),
        )
      } catch (_: Exception) {
        CustomHullVisualRecipe()
      }
    }

    fun synthesizeFromPrompt(rawPrompt: String, variationSalt: Int = 0): CustomHullVisualRecipe {
      val clean = rawPrompt.trim().ifEmpty { "Sleek obsidian stealth scythe with crimson plasma core" }
      val lower = clean.lowercase(Locale.US)
      val hash = abs(clean.hashCode() + variationSalt * 3571)

      val silhouette =
        when {
          lower.contains("scythe") || lower.contains("crescent") || lower.contains("blade") || lower.contains("talon") ->
            "CRESCENT_SCYTHE"
          lower.contains("heavy") || lower.contains("tank") || lower.contains("dreadnought") || lower.contains("fortress") || lower.contains("bastion") || lower.contains("goliath") ->
            "BASTION_DREADNOUGHT"
          lower.contains("manta") || lower.contains("ray") || lower.contains("bio") || lower.contains("organic") || lower.contains("alien") ->
            "MANTA_STINGRAY"
          lower.contains("twin") || lower.contains("fork") || lower.contains("prong") || lower.contains("rail") || lower.contains("sniper") ->
            "TWIN_BOOM_STRIKER"
          lower.contains("predator") || lower.contains("raptor") || lower.contains("fang") || lower.contains("claw") || lower.contains("swept") ->
            "FORWARD_SWEPT_PREDATOR"
          else -> {
            val all =
              listOf(
                "DELTA_INTERCEPTOR",
                "FORWARD_SWEPT_PREDATOR",
                "TWIN_BOOM_STRIKER",
                "CRESCENT_SCYTHE",
                "BASTION_DREADNOUGHT",
                "MANTA_STINGRAY",
              )
            all[hash % all.size]
          }
        }

      val (pri, trim, glow) =
        when {
          lower.contains("crimson") || lower.contains("red") || lower.contains("blood") || lower.contains("inferno") || lower.contains("lava") ->
            Triple("#7F1D1D", "#F87171", "#FF1744")
          lower.contains("gold") || lower.contains("solar") || lower.contains("amber") || lower.contains("sun") || lower.contains("royal") ->
            Triple("#78350F", "#FACC15", "#FFEA00")
          lower.contains("void") || lower.contains("purple") || lower.contains("violet") || lower.contains("nebula") || lower.contains("amethyst") ->
            Triple("#4C1D95", "#C084FC", "#E040FB")
          lower.contains("emerald") || lower.contains("green") || lower.contains("jade") || lower.contains("venom") || lower.contains("acid") ->
            Triple("#064E3B", "#34D399", "#00E676")
          lower.contains("obsidian") || lower.contains("stealth") || lower.contains("shadow") || lower.contains("black") || lower.contains("phantom") ->
            Triple("#0F172A", "#94A3B8", "#38BDF8")
          lower.contains("white") || lower.contains("ivory") || lower.contains("ice") || lower.contains("frost") || lower.contains("arctic") ->
            Triple("#CBD5E1", "#38BDF8", "#00E5FF")
          else -> {
            val palettes =
              listOf(
                Triple("#1E3A8A", "#38BDF8", "#00F5D4"),
                Triple("#581C87", "#E879F9", "#00E5FF"),
                Triple("#881337", "#FB7185", "#FBBF24"),
                Triple("#134E4A", "#2DD4BF", "#A3E635"),
              )
            palettes[hash % palettes.size]
          }
        }

      val engines = listOf("TWIN_PLASMA", "QUAD_NOVA", "HEAVY_THRUSTER", "BIO_ARC")
      val engineStyle = engines[(hash / 7) % engines.size]
      val sweep = (0.72f + (hash % 36) / 100f).coerceIn(0.70f, 1.08f)
      val plates = 1 + (hash % 4)

      val titleWords =
        clean
          .uppercase(Locale.US)
          .replace(Regex("[^A-Z0-9 ]"), " ")
          .split(Regex("\\s+"))
          .filter { it.length in 3..9 && it !in setOf("WITH", "AND", "THE", "SHIP", "HULL", "CORE") }
      val generatedTitle =
        if (titleWords.size >= 2) {
          "${titleWords[0]} ${titleWords[1]}".take(18)
        } else if (titleWords.size == 1) {
          "${titleWords[0]} MK-IX".take(18)
        } else {
          "APEX RIFT-IX"
        }

      return CustomHullVisualRecipe(
        hullName = generatedTitle,
        silhouette = silhouette,
        wingSweep = sweep,
        chassisPlates = plates,
        primaryHex = pri,
        trimHex = trim,
        coreGlowHex = glow,
        engineStyle = engineStyle,
        promptUsed = clean,
      )
    }
  }
}

data class PrimaryWeaponOption(
  val id: String,
  val title: String,
  val shortLabel: String,
  val pointCost: Int,
  val summary: String,
)

data class SecondaryAbilityOption(
  val id: String,
  val title: String,
  val shortLabel: String,
  val pointCost: Int,
  val fluxAndCdLabel: String,
  val summary: String,
) {
  val fluxCostLabel: String
    get() = fluxAndCdLabel
}

/**
 * Saved Loadout Slot (Frame A, Frame B, Frame C, Frame D, Frame E) for Ship #10 (Rift Frame).
 */
data class CustomShipLoadoutSlot(
  val slotId: String = "Frame A",
  val customName: String = "Rift Frame",
  val speedPts: Int = 25,
  val thrustPts: Int = 8,
  val turnPts: Int = 18,
  val maxFluxPts: Int = 0,
  val rechargePts: Int = 14,
  val primaryArchetype: String = "SNIPER_RAIL",
  val secondaryType: String = "PHASE_DASH",
  val visualRecipe: CustomHullVisualRecipe = CustomHullVisualRecipe(),
  val aiGenerationsCount: Int = 0,
) {
  val primaryPts: Int
    get() = primaryPointCost()

  val secondaryPts: Int
    get() = secondaryPointCost()

  fun primaryPointCost(): Int = getPrimaryPointCost(primaryArchetype)
  fun secondaryPointCost(): Int = getSecondaryPointCost(secondaryType)

  fun hullStatPoints(): Int = speedPts + thrustPts + turnPts + maxFluxPts + rechargePts
  fun totalAllocatedPoints(): Int = hullStatPoints() + primaryPointCost() + secondaryPointCost()

  fun toJsonString(): String {
    return JSONObject()
      .apply {
        put("slotId", slotId)
        put("customName", customName)
        put("speedPts", speedPts)
        put("thrustPts", thrustPts)
        put("turnPts", turnPts)
        put("maxFluxPts", maxFluxPts)
        put("rechargePts", rechargePts)
        put("primaryArchetype", primaryArchetype)
        put("secondaryType", secondaryType)
        put("visualRecipe", JSONObject(visualRecipe.toJsonString()))
        put("aiGenerationsCount", aiGenerationsCount)
      }
      .toString()
  }

  companion object {
    val ALL_SLOT_IDS: List<String> = listOf("Frame A", "Frame B", "Frame C", "Frame D", "Frame E")

    // Exactly 9 Primary Weapon Archetypes (3x3 grid in HangarModal)
    val PRIMARY_OPTIONS: List<PrimaryWeaponOption> =
      listOf(
        PrimaryWeaponOption(
          "LIGHT_PULSE",
          "Light Pulse Repeater",
          "PULSE",
          6,
          "Fast single-bolt repeater (16 Flux • 115 DMG • 0 Bounces)",
        ),
        PrimaryWeaponOption(
          "ION_NEEDLER",
          "Twin Ion Needlers",
          "NEEDLER",
          10,
          "Rapid twin-stream ion bolts (18 Flux • 2x82 DMG • 0 Bounces)",
        ),
        PrimaryWeaponOption(
          "TWIN_SCATTER",
          "Twin Scatter Cannons",
          "SCATTER",
          10,
          "Wide close-quarters twin spread (20 Flux • 2x88 DMG • 0 Bounces)",
        ),
        PrimaryWeaponOption(
          "ANGLED_PULSE",
          "Ricochet Twin-Pulse",
          "RICOCHET",
          12,
          "1-Bounce bank-shot twin cannons (20 Flux • 2x92 DMG • 1 Bounce)",
        ),
        PrimaryWeaponOption(
          "QUAD_PULSE",
          "Quad-Converging Flak",
          "QUAD",
          12,
          "4-bolt converging suppression volley (24 Flux • 4x68 DMG • 0 Bounces)",
        ),
        PrimaryWeaponOption(
          "HEAVY_PLASMA",
          "Heavy Twin Plasma",
          "PLASMA",
          12,
          "High-caliber twin siege bolts (38 Flux • 2x150 DMG • 0 Bounces)",
        ),
        PrimaryWeaponOption(
          "TRI_PRISM",
          "Tri-Prism Splitter",
          "PRISM",
          14,
          "3-stream prism volley (24 Flux • 3x80 DMG • 0 Bounces)",
        ),
        PrimaryWeaponOption(
          "SNIPER_RAIL",
          "1-Bounce Rail-Laser",
          "RAILGUN",
          15,
          "1,280 px/s sniper beam (26 Flux • 225 DMG • 1 Wall Bounce)",
        ),
        PrimaryWeaponOption(
          "PARALLEL_AUTOCANNON",
          "Parallel Autocannon",
          "AUTOCANNON",
          15,
          "Parallel dogfighter twin stream (22 Flux • 2x128 DMG • 0 Bounces)",
        ),
      )

    val SECONDARY_OPTIONS: List<SecondaryAbilityOption> =
      listOf(
        SecondaryAbilityOption(
          "NONE",
          "No Secondary (Pure Hull Focus)",
          "NONE",
          0,
          "0 Flux • Passive",
          "Save all 20-25 weapon points to max out Hull Speed, Flux & Recharge!",
        ),
        SecondaryAbilityOption(
          "NATIVE_MULTI",
          "Native 4-Bolt Multi-Toggle",
          "MULTI",
          15,
          "Toggle • 0 Extra Flux",
          "Unlocks permanent [MULTI] fire mode without needing Crate pickups.",
        ),
        SecondaryAbilityOption(
          "PHASE_DASH",
          "Phase Dash + +35% Overcharge",
          "DASH",
          20,
          "320 Flux • 0.65s CD",
          "Instant forward phase lunge + 0.55s shield + +35% next shot damage.",
        ),
        SecondaryAbilityOption(
          "MINE",
          "Magnetic Proximity Mines (Max 6)",
          "MINE",
          18,
          "300 Flux • 0.80s CD",
          "Drops stationary 1,040 DMG mines with 165px magnetic lunge + +1 Repel.",
        ),
        SecondaryAbilityOption(
          "BOUNCE",
          "3-Bounce Bank Torpedo",
          "TORPEDO",
          20,
          "420 Flux • 0.90s CD",
          "980 DMG plasma torpedo that reflects off up to 3 sector walls.",
        ),
        SecondaryAbilityOption(
          "PROX",
          "Proximity Flak Warhead",
          "FLAK",
          22,
          "450 Flux • 0.95s CD",
          "820 DMG air-burst warhead erupting into 8 shrapnel bolts near enemies.",
        ),
        SecondaryAbilityOption(
          "CLUSTER",
          "Cluster Shrapnel Bomb",
          "CLUSTER",
          22,
          "520 Flux • 1.05s CD",
          "920 DMG warhead splitting into 8 radial shrapnel bolts on impact.",
        ),
        SecondaryAbilityOption(
          "NOVA_RING",
          "360° 16-Bolt Nova Ring",
          "NOVA",
          22,
          "480 Flux • 1.15s CD",
          "Fires 16 1-bounce plasma bolts in a full 360° ring around your hull.",
        ),
        SecondaryAbilityOption(
          "SHATTER_CONE",
          "7-Pellet Shatter Cone",
          "SHATTER",
          22,
          "380 Flux • 0.85s CD",
          "Devastating close-range 7-pellet kinetic blast cone (7x195 DMG).",
        ),
        SecondaryAbilityOption(
          "SEEKER_TORPEDO",
          "Homing Seeker Torpedo",
          "SEEKER",
          24,
          "460 Flux • 1.05s CD",
          "880 DMG guided torpedo that steers toward the nearest enemy signature.",
        ),
        SecondaryAbilityOption(
          "CLOAK_EMP",
          "Stealth Cloak + 0-Flux Decloak EMP",
          "CLOAK",
          25,
          "180 Flux/s • 0 Flux EMP",
          "Invisible stealth cloak; decloaking drains 25% Max Flux (cap 850) + 2.0s freeze.",
        ),
        SecondaryAbilityOption(
          "SIEGE",
          "Heavy Siege Bomb (Air-Burst)",
          "SIEGE",
          25,
          "650 Flux • 1.20s CD",
          "Massive 1,350 DMG siege bomb; tap Secondary mid-flight to air-burst.",
        ),
        SecondaryAbilityOption(
          "INTERDICTOR_DOME",
          "Stasis Containment Bubble",
          "BUBBLE",
          25,
          "360 Flux • 10.0s CD",
          "8.0s two-way stasis bubble (275px radius). Nothing enters or leaves.",
        ),
      )

    fun getPrimaryPointCost(archetype: String): Int =
      PRIMARY_OPTIONS.firstOrNull { it.id == archetype }?.pointCost ?: 0

    fun getSecondaryPointCost(secType: String): Int {
      val norm = normalizeLegacySecondaryCode(secType)
      return SECONDARY_OPTIONS.firstOrNull { it.id == norm }?.pointCost ?: 0
    }

    private fun normalizeLegacySecondaryCode(rawCode: String): String =
      when {
        rawCode == "BURST" -> "NOVA_RING"
        rawCode.equals("SH" + "OTGUN", ignoreCase = true) -> "SHATTER_CONE"
        rawCode.equals("MI" + "SSILE", ignoreCase = true) -> "SEEKER_TORPEDO"
        else -> rawCode
      }

    fun normalizeSlotId(slotId: String): String =
      when (slotId.trim().uppercase(Locale.US)) {
        "FRAME B", "9B", "B" -> "Frame B"
        "FRAME C", "9C", "C" -> "Frame C"
        "FRAME D", "9D", "D" -> "Frame D"
        "FRAME E", "9E", "E" -> "Frame E"
        else -> "Frame A"
      }

    fun defaultSlot(slotId: String): CustomShipLoadoutSlot {
      return when (normalizeSlotId(slotId)) {
        "Frame B" ->
          CustomShipLoadoutSlot(
            slotId = "Frame B",
            customName = "Scatter Brawler",
            speedPts = 14,
            thrustPts = 22,
            turnPts = 12,
            maxFluxPts = 12,
            rechargePts = 8,
            primaryArchetype = "TWIN_SCATTER",
            secondaryType = "SHATTER_CONE",
            visualRecipe =
              CustomHullVisualRecipe(
                hullName = "FRAME B",
                silhouette = "BASTION_DREADNOUGHT",
                wingSweep = 1.04f,
                chassisPlates = 4,
                primaryHex = "#4C1D95",
                trimHex = "#FACC15",
                coreGlowHex = "#FFAB00",
                engineStyle = "QUAD_NOVA",
                promptUsed = "Obsidian violet dreadnought with gold bastion armor and solar core",
              ),
          )
        "Frame C" ->
          CustomShipLoadoutSlot(
            slotId = "Frame C",
            customName = "Bank Specialist",
            speedPts = 18,
            thrustPts = 14,
            turnPts = 18,
            maxFluxPts = 8,
            rechargePts = 10,
            primaryArchetype = "ANGLED_PULSE",
            secondaryType = "BOUNCE",
            visualRecipe =
              CustomHullVisualRecipe(
                hullName = "FRAME C",
                silhouette = "CRESCENT_SCYTHE",
                wingSweep = 1.06f,
                chassisPlates = 3,
                primaryHex = "#065F46",
                trimHex = "#34D399",
                coreGlowHex = "#00E676",
                engineStyle = "BIO_ARC",
                promptUsed = "Emerald crescent scythe with bank-torpedo vectoring arrays",
              ),
          )
        "Frame D" ->
          CustomShipLoadoutSlot(
            slotId = "Frame D",
            customName = "Stealth Phantom",
            speedPts = 20,
            thrustPts = 8,
            turnPts = 20,
            maxFluxPts = 5,
            rechargePts = 12,
            primaryArchetype = "ION_NEEDLER",
            secondaryType = "CLOAK_EMP",
            visualRecipe =
              CustomHullVisualRecipe(
                hullName = "FRAME D",
                silhouette = "MANTA_STINGRAY",
                wingSweep = 1.08f,
                chassisPlates = 2,
                primaryHex = "#0F172A",
                trimHex = "#C084FC",
                coreGlowHex = "#00F5D4",
                engineStyle = "TWIN_PLASMA",
                promptUsed = "Obsidian stealth stingray with violet phase emitters",
              ),
          )
        "Frame E" ->
          CustomShipLoadoutSlot(
            slotId = "Frame E",
            customName = "Stasis Archon",
            speedPts = 11,
            thrustPts = 12,
            turnPts = 9,
            maxFluxPts = 18,
            rechargePts = 13,
            primaryArchetype = "ANGLED_PULSE",
            secondaryType = "INTERDICTOR_DOME",
            visualRecipe =
              CustomHullVisualRecipe(
                hullName = "FRAME E",
                silhouette = "FORWARD_SWEPT_PREDATOR",
                wingSweep = 1.02f,
                chassisPlates = 4,
                primaryHex = "#78350F",
                trimHex = "#FACC15",
                coreGlowHex = "#EC4899",
                engineStyle = "HEAVY_THRUSTER",
                promptUsed = "Sovereign gold gravimetric cruiser with magenta stasis core",
              ),
          )
        else ->
          CustomShipLoadoutSlot(
            slotId = "Frame A",
            customName = "Rail Interceptor",
            speedPts = 25,
            thrustPts = 8,
            turnPts = 18,
            maxFluxPts = 0,
            rechargePts = 14,
            primaryArchetype = "SNIPER_RAIL",
            secondaryType = "PHASE_DASH",
            visualRecipe =
              CustomHullVisualRecipe(
                hullName = "FRAME A",
                silhouette = "TWIN_BOOM_STRIKER",
                wingSweep = 1.00f,
                chassisPlates = 2,
                primaryHex = "#1D4ED8",
                trimHex = "#38BDF8",
                coreGlowHex = "#00F5D4",
                engineStyle = "TWIN_PLASMA",
                promptUsed = "Cobalt-cyan twin-boom rail sniper interceptor with phase capacitor",
              ),
          )
      }
    }

    fun fromJsonString(raw: String?, fallbackSlotId: String = "Frame A"): CustomShipLoadoutSlot {
      val cleanFallback = normalizeSlotId(fallbackSlotId)
      if (raw.isNullOrBlank()) return defaultSlot(cleanFallback)
      return try {
        val obj = JSONObject(raw)
        val recipeObj = obj.optJSONObject("visualRecipe")
        val rawPri = obj.optString("primaryArchetype", "SNIPER_RAIL")
        val validPri = if (PRIMARY_OPTIONS.any { it.id == rawPri }) rawPri else "SNIPER_RAIL"
        val rawSec = normalizeLegacySecondaryCode(obj.optString("secondaryType", "PHASE_DASH"))
        val validSec = if (SECONDARY_OPTIONS.any { it.id == rawSec }) rawSec else "NONE"
        val cleanSlot = normalizeSlotId(obj.optString("slotId", cleanFallback))

        val sp = obj.optInt("speedPts", 25).coerceIn(0, 30)
        val th = obj.optInt("thrustPts", 8).coerceIn(0, 30)
        val tu = obj.optInt("turnPts", 18).coerceIn(0, 30)
        val mf = obj.optInt("maxFluxPts", 0).coerceIn(0, 30)
        val rc = obj.optInt("rechargePts", 14).coerceIn(0, 30)
        val aiCount = obj.optInt("aiGenerationsCount", 0).coerceAtLeast(0)

        val isUntouchedLegacyA =
          cleanSlot == "Frame A" &&
            aiCount == 0 &&
            sp == 4 &&
            th == 4 &&
            tu == 4 &&
            mf == 4 &&
            rc == 4 &&
            validPri == "LIGHT_PULSE" &&
            validSec == "NONE"
        val isUntouchedLegacyB =
          cleanSlot == "Frame B" &&
            aiCount == 0 &&
            sp == 2 &&
            th == 2 &&
            tu == 2 &&
            mf == 2 &&
            rc == 2 &&
            validPri == "TWIN_SCATTER" &&
            validSec == "NONE"
        val isUntouchedLegacyC =
          cleanSlot == "Frame C" &&
            aiCount == 0 &&
            sp == 5 &&
            th == 0 &&
            tu == 0 &&
            mf == 0 &&
            rc == 0 &&
            validPri == "LIGHT_PULSE" &&
            validSec == "PHASE_DASH"
        if (isUntouchedLegacyA || isUntouchedLegacyB || isUntouchedLegacyC) {
          return defaultSlot(cleanSlot)
        }

        val rawCustomName = obj.optString("customName", "Rift Frame")
        val cleanName =
          rawCustomName
            .replace(Regex("(?i)^RIFT FRAME\\s*9[ABCDE]$"), "Rift Frame")
            .replace("9A", "Frame A")
            .replace("9B", "Frame B")
            .replace("9C", "Frame C")
            .replace("9D", "Frame D")
            .replace("9E", "Frame E")
            .take(18)
            .ifBlank { "Rift Frame" }

        CustomShipLoadoutSlot(
          slotId = cleanSlot,
          customName = cleanName,
          speedPts = sp,
          thrustPts = th,
          turnPts = tu,
          maxFluxPts = mf,
          rechargePts = rc,
          primaryArchetype = validPri,
          secondaryType = validSec,
          visualRecipe =
            if (recipeObj != null) {
              CustomHullVisualRecipe.fromJsonString(recipeObj.toString())
            } else {
              defaultSlot(cleanSlot).visualRecipe
            },
          aiGenerationsCount = aiCount,
        )
      } catch (_: Exception) {
        defaultSlot(cleanFallback)
      }
    }
  }
}

private data class PrimaryTuple(
  val title: String,
  val shortLabel: String,
  val cost: Float,
  val cooldown: Float,
  val damage: Float,
  val speed: Float,
  val streams: Int,
  val spread: Float,
  val bounces: Int,
  val isParallel: Boolean,
)

private data class SecondaryTuple(
  val title: String,
  val shortLabel: String,
  val cost: Float,
  val cooldown: Float,
  val damage: Float,
  val bounces: Int,
  val recoil: Float,
)

data class PowerUpCodexEntry(
  val id: String,
  val buffTypeIndex: Int,
  val title: String,
  val dropRatePercent: Int,
  val category: String,
  val effectSummary: String,
  val tacticalNote: String,
)

object RiftCodex {
  const val BASE_TOP_SPEED = 380f
  const val SPEED_PER_PT = 8f

  const val BASE_THRUST = 600f
  const val THRUST_PER_PT = 15f

  const val BASE_TURN_RATE = 3.2f
  const val TURN_PER_PT = 0.10f

  const val BASE_MAX_FLUX = 2200f
  const val MAX_FLUX_PER_PT = 80f

  const val BASE_RECHARGE = 240f
  const val RECHARGE_PER_PT = 8f

  val primaryWeaponOptions: List<PrimaryWeaponOption>
    get() = CustomShipLoadoutSlot.PRIMARY_OPTIONS

  val secondaryAbilityOptions: List<SecondaryAbilityOption>
    get() = CustomShipLoadoutSlot.SECONDARY_OPTIONS

  fun getRiftFramePointBudget(pilotLevel: Int, devOverride: Boolean = false): Int =
    com.example.engine.getRiftFramePointBudget(pilotLevel, devOverride)

  fun computeTopSpeed(pts: Int): Float = BASE_TOP_SPEED + pts.coerceIn(0, 30) * SPEED_PER_PT
  fun computeThrust(pts: Int): Float = BASE_THRUST + pts.coerceIn(0, 30) * THRUST_PER_PT
  fun computeTurnRate(pts: Int): Float = BASE_TURN_RATE + pts.coerceIn(0, 30) * TURN_PER_PT
  fun computeMaxFlux(pts: Int): Float = BASE_MAX_FLUX + pts.coerceIn(0, 30) * MAX_FLUX_PER_PT
  fun computeRecharge(pts: Int): Float = BASE_RECHARGE + pts.coerceIn(0, 30) * RECHARGE_PER_PT

  @Volatile
  var activeCustomShipSlot: CustomShipLoadoutSlot = CustomShipLoadoutSlot.defaultSlot("Frame A")

  val customShip9Spec: ShipSpec
    get() = buildShipSpecFromCustomSlot(activeCustomShipSlot)

  fun updateActiveCustomShipSlot(slot: CustomShipLoadoutSlot) {
    activeCustomShipSlot = slot
    RiftSpriteBaker.updateCustomShip9Sprites(slot.visualRecipe)
  }

  private fun createCoreBuffRegistry(): MutableMap<String, SpecialBuffRegistryEntry> =
    linkedMapOf(
      "NOVA_RING" to
        SpecialBuffRegistryEntry(
          id = "NOVA_RING",
          buffTypeIndex = 0,
          officialName = "NOVA RING",
          shortLabel = "NOVA",
          chargesPerPickup = 1,
          aimMode = BuffAimMode.TAP_INSTANT,
          strokeColorHex = "#F97316",
          fillColorArgb = SpecialBuffRegistryEntry.fillColorFromStroke("#F97316"),
          summary = "Fires 16 1-bounce plasma bolts in a full 360° ring at 0 Flux cost.",
          isCustomForged = false,
          projectileSpeed = 780f,
          flightDuration = 1.6f,
          damage = 145f,
          blastRadius = 240f,
        ),
      "SHATTER_CONE" to
        SpecialBuffRegistryEntry(
          id = "SHATTER_CONE",
          buffTypeIndex = 1,
          officialName = "SHATTER CONE",
          shortLabel = "SHATTER",
          chargesPerPickup = 2,
          aimMode = BuffAimMode.AIM_DIRECTIONAL,
          strokeColorHex = "#FB7185",
          fillColorArgb = SpecialBuffRegistryEntry.fillColorFromStroke("#FB7185"),
          summary = "Unleashes a 3-wave 60° cone of bouncing plasma shards at 0 Flux cost.",
          isCustomForged = false,
          projectileSpeed = 860f,
          flightDuration = 1.2f,
          damage = 95f,
          blastRadius = 180f,
        ),
      "SEEKER_TORPEDO" to
        SpecialBuffRegistryEntry(
          id = "SEEKER_TORPEDO",
          buffTypeIndex = 2,
          officialName = "SEEKER TORPEDO",
          shortLabel = "SEEKER",
          chargesPerPickup = 1,
          aimMode = BuffAimMode.AIM_DIRECTIONAL,
          strokeColorHex = "#EF4444",
          fillColorArgb = SpecialBuffRegistryEntry.fillColorFromStroke("#EF4444"),
          summary = "Launches a homing Seeker Torpedo that steers onto hostile targets.",
          isCustomForged = false,
          projectileSpeed = 620f,
          flightDuration = 2.5f,
          damage = 880f,
          blastRadius = 160f,
        ),
      "HOLO_PHANTOM" to
        SpecialBuffRegistryEntry(
          id = "HOLO_PHANTOM",
          buffTypeIndex = 3,
          officialName = "HOLO PHANTOM",
          shortLabel = "PHANTOM",
          chargesPerPickup = 1,
          aimMode = BuffAimMode.AIM_DIRECTIONAL,
          strokeColorHex = "#2DD4BF",
          fillColorArgb = SpecialBuffRegistryEntry.fillColorFromStroke("#2DD4BF"),
          summary = "Projects a mirrored holographic decoy ship and cloaks your hull.",
          isCustomForged = false,
          projectileSpeed = 520f,
          flightDuration = 4.5f,
          damage = 420f,
          blastRadius = 200f,
          specialEffect = CustomBuffEffect.CLONE,
        ),
      "STASIS_BARRIER" to
        SpecialBuffRegistryEntry(
          id = "STASIS_BARRIER",
          buffTypeIndex = 4,
          officialName = "STASIS BARRIER",
          shortLabel = "BARRIER",
          chargesPerPickup = 1,
          aimMode = BuffAimMode.AIM_DIRECTIONAL,
          strokeColorHex = "#38BDF8",
          fillColorArgb = SpecialBuffRegistryEntry.fillColorFromStroke("#38BDF8"),
          summary = "Deploys a permanent 3D shield-emitter bulkhead wall ahead of your ship.",
          isCustomForged = false,
          projectileSpeed = 0f,
          flightDuration = 999999f,
          damage = 0f,
          blastRadius = 220f,
          specialEffect = CustomBuffEffect.BARRIER,
        ),
      "HYPER_SURGE" to
        SpecialBuffRegistryEntry(
          id = "HYPER_SURGE",
          buffTypeIndex = 5,
          officialName = "HYPER SURGE",
          shortLabel = "SURGE",
          chargesPerPickup = 1,
          aimMode = BuffAimMode.AIM_DIRECTIONAL,
          strokeColorHex = "#F43F5E",
          fillColorArgb = SpecialBuffRegistryEntry.fillColorFromStroke("#F43F5E"),
          summary = "Ignites hyper-thrusters for supersonic intercept velocity.",
          isCustomForged = false,
          projectileSpeed = 880f,
          flightDuration = 3.2f,
          damage = 0f,
          blastRadius = 190f,
          specialEffect = CustomBuffEffect.SPEED_BOOST,
        ),
    )

  val BUFF_REGISTRY: MutableMap<String, SpecialBuffRegistryEntry> = createCoreBuffRegistry()

  fun normalizeSpecialBuffId(raw: String): String {
    val clean = raw.trim().uppercase(Locale.US)
    return when (clean) {
      "BURST", "NOVA", "NOVA_RING" -> "NOVA_RING"
      "SHOTGUN", "SHATTER", "SHATTER_CONE" -> "SHATTER_CONE"
      "MISSILE", "SEEKER", "SEEKER_TORPEDO", "THOR" -> "SEEKER_TORPEDO"
      "DECOY", "PHANTOM", "HOLO_PHANTOM" -> "HOLO_PHANTOM"
      "BRICK", "BARRIER", "STASIS_BARRIER" -> "STASIS_BARRIER"
      "ROCKET", "SURGE", "HYPER_SURGE", "PHASE_BLINK" -> "HYPER_SURGE"
      else -> clean
    }
  }

  fun getBuffRegistryEntry(idOrAlias: String): SpecialBuffRegistryEntry? {
    val norm = normalizeSpecialBuffId(idOrAlias)
    return BUFF_REGISTRY[norm] ?: BUFF_REGISTRY[idOrAlias]
  }

  fun getBuffRegistryByIndex(index: Int): SpecialBuffRegistryEntry? {
    return BUFF_REGISTRY.values.firstOrNull { it.buffTypeIndex == index }
  }

  fun getCustomForgedBuffs(): List<SpecialBuffRegistryEntry> {
    return BUFF_REGISTRY.values.filter { it.isCustomForged }.sortedBy { it.buffTypeIndex }
  }

  fun nextCustomBuffTypeIndex(): Int {
    val maxExisting = BUFF_REGISTRY.values.maxOfOrNull { it.buffTypeIndex } ?: 10
    return max(11, maxExisting + 1)
  }

  fun registerCustomBuff(entry: SpecialBuffRegistryEntry) {
    BUFF_REGISTRY[entry.id] = entry
    RiftSpriteBaker.invalidateCustomBuffIcon(entry.buffTypeIndex)
  }

  fun removeCustomBuff(id: String) {
    val removed = BUFF_REGISTRY.remove(id)
    if (removed != null) {
      RiftSpriteBaker.invalidateCustomBuffIcon(removed.buffTypeIndex)
    }
  }

  fun resetBuffRegistryToDefaults(preserveCustomForged: Boolean = true) {
    val preserved = if (preserveCustomForged) getCustomForgedBuffs() else emptyList()
    BUFF_REGISTRY.clear()
    BUFF_REGISTRY.putAll(createCoreBuffRegistry())
    for (custom in preserved) {
      BUFF_REGISTRY[custom.id] = custom
    }
  }

  fun buildShipSpecFromCustomSlot(slot: CustomShipLoadoutSlot): ShipSpec {
    val spPts = slot.speedPts.coerceIn(0, 30)
    val thPts = slot.thrustPts.coerceIn(0, 30)
    val tuPts = slot.turnPts.coerceIn(0, 30)
    val mfPts = slot.maxFluxPts.coerceIn(0, 30)
    val rcPts = slot.rechargePts.coerceIn(0, 30)

    val topSpd = computeTopSpeed(spPts)
    val thrustVal = computeThrust(thPts)
    val turnVal = computeTurnRate(tuPts)
    val maxFlx = computeMaxFlux(mfPts)
    val rechVal = computeRecharge(rcPts)

    val priPts = slot.primaryPointCost()
    val secPts = slot.secondaryPointCost()

    val (pTitle, pShort, pCost, pCd, pDmg, pSpeed, pStreams, pSpread, pBounce, isParallel) =
      when (slot.primaryArchetype) {
        "ION_NEEDLER" ->
          PrimaryTuple("Twin Ion Needlers", "NEEDLER", 18f, 0.13f, 82f, 950f, 2, 0.04f, 0, false)
        "TWIN_SCATTER" ->
          PrimaryTuple("Twin Scatter Cannons", "SCATTER", 20f, 0.14f, 88f, 900f, 2, 0.065f, 0, false)
        "ANGLED_PULSE" ->
          PrimaryTuple("Ricochet Twin-Pulse", "RICOCHET", 20f, 0.15f, 92f, 960f, 2, 0.055f, 1, false)
        "QUAD_PULSE" ->
          PrimaryTuple("Quad-Converging Flak", "QUAD", 24f, 0.15f, 68f, 940f, 4, 0.045f, 0, false)
        "HEAVY_PLASMA" ->
          PrimaryTuple("Heavy Twin Plasma", "PLASMA", 38f, 0.21f, 150f, 880f, 2, 0.05f, 0, false)
        "TRI_PRISM" ->
          PrimaryTuple("Tri-Prism Splitter", "PRISM", 24f, 0.15f, 80f, 960f, 3, 0.05f, 0, false)
        "SNIPER_RAIL" ->
          PrimaryTuple("1-Bounce Rail-Laser", "RAILGUN", 26f, 0.18f, 225f, 1280f, 1, 0f, 1, false)
        "PARALLEL_AUTOCANNON" ->
          PrimaryTuple("Parallel Autocannon", "AUTOCANNON", 22f, 0.13f, 128f, 1000f, 2, 0.025f, 0, true)
        else ->
          PrimaryTuple("Light Pulse Repeater", "PULSE", 16f, 0.12f, 115f, 980f, 1, 0f, 0, false)
      }

    val supportsNativeMulti = slot.secondaryType == "NATIVE_MULTI"
    val extraRepel = if (slot.secondaryType == "MINE") 1 else 0
    val hasSecBomb = slot.secondaryType != "NONE" && slot.secondaryType != "NATIVE_MULTI"

    val (sTitle, sShort, bCost, bCd, bDmg, bBounces, bRecoil) =
      when (slot.secondaryType) {
        "NATIVE_MULTI" ->
          SecondaryTuple("Native 4-Bolt Multi-Toggle", "TWIN MULTI", 0f, 0f, 0f, 0, 0f)
        "PHASE_DASH" ->
          SecondaryTuple("Phase Dash + +35% Overcharge", "PHASE DASH", 320f, 0.65f, 0f, 0, 0f)
        "MINE" ->
          SecondaryTuple("Magnetic Proximity Mines (300 Flux • Max 6)", "PROX MINE", 300f, 0.80f, 1040f, 0, 0f)
        "BOUNCE" ->
          SecondaryTuple("3-Bounce Bank Torpedo", "TORPEDO", 420f, 0.90f, 980f, 3, 220f)
        "PROX" ->
          SecondaryTuple("Proximity Flak Warhead", "PROX FLAK", 450f, 0.95f, 820f, 1, 210f)
        "CLUSTER" ->
          SecondaryTuple("Cluster Shrapnel Bomb", "CLUSTER", 520f, 1.05f, 920f, 0, 250f)
        "NOVA_RING" ->
          SecondaryTuple("360° 16-Bolt Nova Ring", "NOVA RING", 480f, 1.15f, 145f, 1, 0f)
        "SHATTER_CONE" ->
          SecondaryTuple("7-Pellet Shatter Cone", "SHATTER", 380f, 0.85f, 195f, 0, 260f)
        "SEEKER_TORPEDO" ->
          SecondaryTuple("Homing Seeker Torpedo", "SEEKER", 460f, 1.05f, 880f, 0, 190f)
        "CLOAK_EMP" ->
          SecondaryTuple("Stealth Cloak + 0-Flux Decloak EMP", "CLOAK", 0f, 0.95f, 0f, 0, 0f)
        "SIEGE" ->
          SecondaryTuple("Heavy Siege Bomb (Air-Burst)", "SIEGE BOMB", 650f, 1.20f, 1350f, 1, 360f)
        "INTERDICTOR_DOME" ->
          SecondaryTuple("Stasis Bubble (360 Flux • 8.0s)", "BUBBLE", 360f, 10.0f, 0f, 0, 0f)
        else ->
          SecondaryTuple("Pure Hull Focus (No Secondary)", "NONE", 0f, 0f, 0f, 0, 0f)
      }

    val cleanHullTitle = slot.customName.trim().ifEmpty { "Rift Frame" }
    return ShipSpec(
      id = 10,
      name = cleanHullTitle,
      role = "Modular Custom Hull (${slot.totalAllocatedPoints()}/100 Pts)",
      tacticalLore = "Pilot-engineered modular Rift Frame combining custom 100-Point stat allocation, curated weapon hardpoints, and procedural 3D chassis geometry.",
      silhouetteDesc = "${slot.visualRecipe.silhouette.replace('_', ' ')} • Prompt: \"${slot.visualRecipe.promptUsed.take(64)}\"",
      weaponSummary = "PRI: $pShort (${pCost.toInt()} Flux) • SEC: $sShort" + (if (bCost > 0f) " (${bCost.toInt()} Flux)" else ""),
      primaryTitle = pTitle,
      secondaryTitle = sTitle,
      primaryShortLabel = pShort,
      secondaryShortLabel = sShort,
      maxFlux = maxFlx,
      rechargeRate = rechVal,
      topSpeed = topSpd,
      afterburnerSpeed = topSpd * 1.32f,
      thrust = thrustVal,
      turnRate = turnVal,
      primaryCost = pCost,
      primaryCooldown = pCd,
      primaryDamage = pDmg,
      primarySpeed = pSpeed,
      primaryStreams = pStreams,
      primarySpreadRad = pSpread,
      primaryBounces = pBounce,
      isParallelTwin = isParallel,
      hasBomb = hasSecBomb,
      bombType = if (hasSecBomb) slot.secondaryType else "NONE",
      bombCost = bCost,
      bombCooldown = bCd,
      bombDamage = bDmg,
      bombBounces = bBounces,
      bombRecoil = bRecoil,
      supportsNativeMultifire = supportsNativeMulti,
      initialRepels = 2 + extraRepel,
      accentColorHex = 0xFF38BDF8.toInt(),
      speedPts = spPts,
      thrustPts = thPts,
      turnPts = tuPts,
      maxFluxPts = mfPts,
      rechargePts = rcPts,
      primaryPts = priPts,
      secondaryPts = secPts,
      isCustomModularFrame = true,
      activeCustomSlotId = slot.slotId,
    )
  }

  val RIFT_ANOMALIES_CODEX: List<RiftAnomalyCodexEntry> =
    listOf(
      RiftAnomalyCodexEntry(
        id = "plasma_rift",
        title = "PLASMA RIFT",
        classification = "THERMAL HAZARD",
        specBadge = "180px • BURNS FLUX",
        accentHex = "#FB7185",
        effectSummary = "Superheated crimson event horizon that sears ship shields on contact and can finish critically low-Flux hulls.",
        tacticalNote = "Use Repel or knockback warheads to shove wounded enemies directly into the crimson core.",
      ),
      RiftAnomalyCodexEntry(
        id = "repulsor_rift",
        title = "REPULSOR RIFT",
        classification = "KINETIC DEFLECTOR",
        specBadge = "180px • SLINGSHOT",
        accentHex = "#38BDF8",
        effectSummary = "Inverted gravimetric field that violently deflects ships and curves incoming ordnance outward.",
        tacticalNote = "Graze the outer cyan rim to slingshot across the sector at supersonic intercept speed.",
      ),
      RiftAnomalyCodexEntry(
        id = "gravity_rift",
        title = "GRAVITY RIFT",
        classification = "GROWING SINGULARITY",
        specBadge = "UP TO 420px • PULL",
        accentHex = "#C084FC",
        effectSummary = "Pulls ships, bullets (+6px), bombs (+18px), and Holo Phantoms (+14px) inward, growing up to 420px radius.",
        tacticalNote = "Ignite Afterburners tangential to the horizon to escape, or bank shots around its gravity lens.",
      ),
      RiftAnomalyCodexEntry(
        id = "phase_rift",
        title = "PHASE WORMHOLE",
        classification = "PAIRED TELEPORT",
        specBadge = "A <-> B • MOMENTUM",
        accentHex = "#34D399",
        effectSummary = "Linked spatial wormhole pair that teleports ships, lasers, and bombs across quadrants while preserving 100% velocity.",
        tacticalNote = "Fire Siege Bombs or 3-Bounce Torpedoes through Wormhole A to ambush targets near Wormhole B.",
      ),
    )

  val ANOMALY_CODEX: List<SpatialAnomalyCodexEntry> =
    listOf(
      SpatialAnomalyCodexEntry(
        id = "GRAVITY_WELL",
        title = "SINGULARITY WELL",
        badge = "GRAV",
        accentColorHex = 0xFFC084FC,
        radiusRule = "380px Pull Horizon • 45px Event Core",
        mechanicsSummary = "Bends ship and projectile trajectories inward with inverse-distance Newtonian pull.",
        tacticalCounterplay = "Slingshot around the outer rim to gain free intercept speed.",
      ),
      SpatialAnomalyCodexEntry(
        id = "SOLAR_FLARE",
        title = "CORONAL FLARE VENT",
        badge = "SOLAR",
        accentColorHex = 0xFFFF7043,
        radiusRule = "240px Thermal Zone",
        mechanicsSummary = "Searing plasma vent that drains Flux on contact.",
        tacticalCounterplay = "Knock pursuers into active crimson rifts with Repel.",
      ),
      SpatialAnomalyCodexEntry(
        id = "PHASE_RIFT_PAIR",
        title = "LINKED PHASE RIFTS (A <-> B)",
        badge = "WARP",
        accentColorHex = 0xFF00F5D4,
        radiusRule = "Paired Aperture",
        mechanicsSummary = "Paired spatial wormholes preserving 100% momentum for ships and ordnance.",
        tacticalCounterplay = "Fire torpedoes through Rift A to ambush enemies near Rift B.",
      ),
    )

  private fun createDefaultSignatureShips(): List<ShipSpec> =
    listOf(
      // 1. Viper (Railgun Sniper — 100 Pts)
      ShipSpec(
        id = 1,
        name = "Viper",
        role = "Railgun Sniper",
        tacticalLore = "Fastest hull in the sector (620 px/s) armed with a 1-bounce rail-laser and instant Phase Dash.",
        silhouetteDesc = "Forward-swept gold/bronze prongs framing a glowing V-shaped cyan reactor core.",
        weaponSummary = "PRI: RAILGUN (26 Flux • 1 Bounce) • CLASS: PHASE DASH (320 Flux • +35% Overcharge)",
        primaryTitle = "1-Bounce Rail-Laser",
        secondaryTitle = "Phase Dash (320 Flux) + +35% Overcharge",
        primaryShortLabel = "RAILGUN",
        secondaryShortLabel = "PHASE DASH",
        speedPts = 30,
        turnPts = 18,
        rechargePts = 12,
        thrustPts = 5,
        maxFluxPts = 0,
        primaryPts = 15,
        secondaryPts = 20,
        maxFlux = computeMaxFlux(0),
        rechargeRate = computeRecharge(12),
        topSpeed = computeTopSpeed(30),
        afterburnerSpeed = computeTopSpeed(30) * 1.32f,
        thrust = computeThrust(5),
        turnRate = computeTurnRate(18),
        primaryCost = 26f,
        primaryCooldown = 0.18f,
        primaryDamage = 235f,
        primarySpeed = 1280f,
        primaryStreams = 1,
        primarySpreadRad = 0f,
        primaryBounces = 1,
        hasBomb = true,
        bombType = "PHASE_DASH",
        bombCost = 320f,
        bombCooldown = 0.65f,
        bombDamage = 0f,
        bombBounces = 0,
        bombRecoil = 0f,
        supportsNativeMultifire = false,
        initialRepels = 2,
        accentColorHex = 0xFF38BDF8.toInt(),
      ),
      // 2. Ricochet (Bank-Shot Specialist — 100 Pts)
      ShipSpec(
        id = 2,
        name = "Ricochet",
        role = "Bank-Shot Specialist",
        tacticalLore = "Geometry master firing 1-bounce twin pulse bolts and 3-bounce plasma bank torpedoes.",
        silhouetteDesc = "Swept delta interceptor with brushed bronze armor and dual V-cyan intake vents.",
        weaponSummary = "PRI: RICOCHET (20 Flux • 1 Bounce) • SEC: TORPEDO (420 Flux • 3 Bounces)",
        primaryTitle = "Angled Ricochet Twin-Pulse",
        secondaryTitle = "3-Bounce Plasma Torpedo (420 Flux)",
        primaryShortLabel = "RICOCHET",
        secondaryShortLabel = "TORPEDO",
        turnPts = 22,
        speedPts = 16,
        thrustPts = 12,
        rechargePts = 10,
        maxFluxPts = 8,
        primaryPts = 12,
        secondaryPts = 20,
        maxFlux = computeMaxFlux(8),
        rechargeRate = computeRecharge(10),
        topSpeed = computeTopSpeed(16),
        afterburnerSpeed = computeTopSpeed(16) * 1.32f,
        thrust = computeThrust(12),
        turnRate = computeTurnRate(22),
        primaryCost = 20f,
        primaryCooldown = 0.15f,
        primaryDamage = 185f,
        primarySpeed = 960f,
        primaryStreams = 2,
        primarySpreadRad = 0.055f,
        primaryBounces = 1,
        hasBomb = true,
        bombType = "BOUNCE",
        bombCost = 420f,
        bombCooldown = 0.90f,
        bombDamage = 980f,
        bombBounces = 3,
        bombRecoil = 220f,
        supportsNativeMultifire = false,
        initialRepels = 2,
        accentColorHex = 0xFF42A5F5.toInt(),
      ),
      // 3. Suppressor (Cluster Skirmisher — 100 Pts)
      ShipSpec(
        id = 3,
        name = "Suppressor",
        role = "Cluster Skirmisher",
        tacticalLore = "Balanced twin-ion brawler firing cluster bombs that erupt into 8 radial shrapnel bolts.",
        silhouetteDesc = "Twin-tail tactical gunship with heavy gold stabilizer fins and a central cyan core.",
        weaponSummary = "PRI: ION TWIN (18 Flux) • SEC: CLUSTER BOMB (520 Flux • 8 Shrapnel)",
        primaryTitle = "Rapid Twin-Ion Stream",
        secondaryTitle = "Cluster Shrapnel Bomb (520 Flux)",
        primaryShortLabel = "ION TWIN",
        secondaryShortLabel = "CLUSTER",
        speedPts = 18,
        thrustPts = 15,
        turnPts = 15,
        maxFluxPts = 10,
        rechargePts = 10,
        primaryPts = 10,
        secondaryPts = 22,
        maxFlux = computeMaxFlux(10),
        rechargeRate = computeRecharge(10),
        topSpeed = computeTopSpeed(18),
        afterburnerSpeed = computeTopSpeed(18) * 1.32f,
        thrust = computeThrust(15),
        turnRate = computeTurnRate(15),
        primaryCost = 18f,
        primaryCooldown = 0.14f,
        primaryDamage = 155f,
        primarySpeed = 980f,
        primaryStreams = 2,
        primarySpreadRad = 0.035f,
        primaryBounces = 0,
        hasBomb = true,
        bombType = "CLUSTER",
        bombCost = 520f,
        bombCooldown = 1.05f,
        bombDamage = 920f,
        bombBounces = 0,
        bombRecoil = 250f,
        supportsNativeMultifire = false,
        initialRepels = 2,
        accentColorHex = 0xFFAB47BC.toInt(),
      ),
      // 4. Goliath (Heavy Siege Tank — 100 Pts)
      ShipSpec(
        id = 4,
        name = "Goliath",
        role = "Heavy Siege",
        tacticalLore = "4,600-Flux dreadnought firing twin heavy plasma cannons and 1,350-damage Siege Bombs; tap Secondary mid-flight to air-burst on command.",
        silhouetteDesc = "Broad bastion dreadnought with thick gold-trimmed prow plates and twin V-cyan conduits.",
        weaponSummary = "PRI: PLASMA (40 Flux) • SEC: SIEGE BOMB (650 Flux • Tap Again to Air-Burst)",
        primaryTitle = "Heavy Twin Plasma",
        secondaryTitle = "Siege Bomb (650 Flux • Mid-Flight Air-Burst)",
        primaryShortLabel = "PLASMA",
        secondaryShortLabel = "SIEGE BOMB",
        maxFluxPts = 30,
        rechargePts = 20,
        thrustPts = 8,
        turnPts = 5,
        speedPts = 0,
        primaryPts = 12,
        secondaryPts = 25,
        maxFlux = computeMaxFlux(30),
        rechargeRate = computeRecharge(20),
        topSpeed = computeTopSpeed(0),
        afterburnerSpeed = computeTopSpeed(0) * 1.35f,
        thrust = computeThrust(8),
        turnRate = computeTurnRate(5),
        primaryCost = 40f,
        primaryCooldown = 0.21f,
        primaryDamage = 310f,
        primarySpeed = 880f,
        primaryStreams = 2,
        primarySpreadRad = 0.05f,
        primaryBounces = 0,
        hasBomb = true,
        bombType = "SIEGE",
        bombCost = 650f,
        bombCooldown = 1.20f,
        bombDamage = 1350f,
        bombBounces = 1,
        bombRecoil = 360f,
        supportsNativeMultifire = false,
        initialRepels = 2,
        accentColorHex = 0xFFFFB300.toInt(),
      ),
      // 5. Twinfang (Dual-Barrel Dogfighter — 100 Pts)
      ShipSpec(
        id = 5,
        name = "Twinfang",
        role = "Dogfighter",
        tacticalLore = "Agile dogfighter firing parallel twin autocannons with an optional 4-bolt fan split.",
        silhouetteDesc = "Predatory twin-boom manta frame with bronze fangs and a split electric-cyan core.",
        weaponSummary = "PRI: AUTOCANNON (22 Flux) • CLASS: TWIN MULTI ON/OFF",
        primaryTitle = "Parallel Twin-Stream Autocannon",
        secondaryTitle = "Native Multi-Shot + Point-Defense",
        primaryShortLabel = "AUTOCANNON",
        secondaryShortLabel = "TWIN MULTI",
        speedPts = 22,
        thrustPts = 18,
        turnPts = 15,
        maxFluxPts = 8,
        rechargePts = 7,
        primaryPts = 15,
        secondaryPts = 15,
        maxFlux = computeMaxFlux(8),
        rechargeRate = computeRecharge(7),
        topSpeed = computeTopSpeed(22),
        afterburnerSpeed = computeTopSpeed(22) * 1.32f,
        thrust = computeThrust(18),
        turnRate = computeTurnRate(15),
        primaryCost = 22f,
        primaryCooldown = 0.13f,
        primaryDamage = 135f,
        primarySpeed = 1000f,
        primaryStreams = 2,
        primarySpreadRad = 0.025f,
        primaryBounces = 0,
        isParallelTwin = true,
        hasBomb = false,
        bombType = "NONE",
        bombCost = 0f,
        bombCooldown = 0f,
        bombDamage = 0f,
        bombBounces = 0,
        bombRecoil = 0f,
        supportsNativeMultifire = true,
        initialRepels = 2,
        accentColorHex = 0xFF00E5FF.toInt(),
      ),
      // 6. Disruptor (Stealth & EMP — 100 Pts)
      ShipSpec(
        id = 6,
        name = "Disruptor",
        role = "Stealth & EMP",
        tacticalLore = "Cloaks invisibly (180 Flux/s) and unleashes a 0-Flux 360° EMP burst on decloak (-25% Max Flux capped at 850 + 2.0s recharge freeze).",
        silhouetteDesc = "Wide stingray hydrofoil with gold-plated arc emitters and a pulsing V-cyan capacitor.",
        weaponSummary = "PRI: NEEDLER (18 Flux) • CLS: Cloak (180/s) + 0-Flux Decloak EMP (25% Cap 850 • 2.0s Freeze)",
        primaryTitle = "Twin Ion Needlers",
        secondaryTitle = "Tactical Cloak (180 Flux/s) + 0-Flux Decloak EMP (25% Cap 850 • 2.0s)",
        primaryShortLabel = "NEEDLER",
        secondaryShortLabel = "CLOAK",
        turnPts = 22,
        speedPts = 20,
        rechargePts = 13,
        thrustPts = 5,
        maxFluxPts = 5,
        primaryPts = 10,
        secondaryPts = 25,
        maxFlux = computeMaxFlux(5),
        rechargeRate = computeRecharge(13),
        topSpeed = computeTopSpeed(20),
        afterburnerSpeed = computeTopSpeed(20) * 1.32f,
        thrust = computeThrust(5),
        turnRate = computeTurnRate(22),
        primaryCost = 18f,
        primaryCooldown = 0.13f,
        primaryDamage = 165f,
        primarySpeed = 950f,
        primaryStreams = 2,
        primarySpreadRad = 0.04f,
        primaryBounces = 0,
        hasBomb = true,
        bombType = "CLOAK_EMP",
        bombCost = 0f,
        bombCooldown = 0.95f,
        bombDamage = 0f,
        bombBounces = 0,
        bombRecoil = 0f,
        supportsNativeMultifire = false,
        initialRepels = 2,
        accentColorHex = 0xFFB388FF.toInt(),
      ),
      // 7. Valkyrie (Proximity Flak — 100 Pts)
      ShipSpec(
        id = 7,
        name = "Valkyrie",
        role = "Flak Gunship",
        tacticalLore = "Fires 4 converging pulse bolts and proximity flak warheads that burst into 8 shrapnel bolts.",
        silhouetteDesc = "Aggressive X-wing skeletal interceptor with bronze thruster pods and V-cyan reactor.",
        weaponSummary = "PRI: QUAD PULSE (24 Flux) • SEC: PROX FLAK (450 Flux • 8 Shrapnel)",
        primaryTitle = "Quad-Converging Pulse",
        secondaryTitle = "Proximity Flak Warhead (450 Flux)",
        primaryShortLabel = "QUAD PULSE",
        secondaryShortLabel = "PROX FLAK",
        maxFluxPts = 20,
        rechargePts = 16,
        thrustPts = 12,
        speedPts = 10,
        turnPts = 8,
        primaryPts = 12,
        secondaryPts = 22,
        maxFlux = computeMaxFlux(20),
        rechargeRate = computeRecharge(16),
        topSpeed = computeTopSpeed(10),
        afterburnerSpeed = computeTopSpeed(10) * 1.32f,
        thrust = computeThrust(12),
        turnRate = computeTurnRate(8),
        primaryCost = 24f,
        primaryCooldown = 0.15f,
        primaryDamage = 72f,
        primarySpeed = 940f,
        primaryStreams = 4,
        primarySpreadRad = 0.045f,
        primaryBounces = 0,
        hasBomb = true,
        bombType = "PROX",
        bombCost = 450f,
        bombCooldown = 0.95f,
        bombDamage = 820f,
        bombBounces = 1,
        bombRecoil = 210f,
        supportsNativeMultifire = false,
        initialRepels = 2,
        accentColorHex = 0xFFFF7043.toInt(),
      ),
      // 8. Manta (Mine-Layer — 100 Pts)
      ShipSpec(
        id = 8,
        name = "Manta",
        role = "Mine-Layer",
        tacticalLore = "High-thrust area-denial hull that drops up to 6 magnetic proximity mines (165px lunge, 300 Flux) and deploys with +1 starting Repel.",
        silhouetteDesc = "Armored pincer hull with gold-beveled mandible plates and a twin-cell V-cyan core.",
        weaponSummary = "PRI: SCATTER (20 Flux) • SEC: MAG MINE (300 Flux • 165px Lunge • Max 6) +1 Repel",
        primaryTitle = "Short-Range Twin Scatter",
        secondaryTitle = "Magnetic Proximity Mines (300 Flux • 165px Lunge • Max 6) +1 Repel",
        primaryShortLabel = "SCATTER",
        secondaryShortLabel = "PROX MINE",
        thrustPts = 30,
        maxFluxPts = 16,
        rechargePts = 12,
        turnPts = 10,
        speedPts = 4,
        primaryPts = 10,
        secondaryPts = 18,
        maxFlux = computeMaxFlux(16),
        rechargeRate = computeRecharge(12),
        topSpeed = computeTopSpeed(4),
        afterburnerSpeed = computeTopSpeed(4) * 1.34f,
        thrust = computeThrust(30),
        turnRate = computeTurnRate(10),
        primaryCost = 20f,
        primaryCooldown = 0.14f,
        primaryDamage = 175f,
        primarySpeed = 900f,
        primaryStreams = 2,
        primarySpreadRad = 0.065f,
        primaryBounces = 0,
        hasBomb = true,
        bombType = "MINE",
        bombCost = 300f,
        bombCooldown = 0.80f,
        bombDamage = 1040f,
        bombBounces = 0,
        bombRecoil = 0f,
        supportsNativeMultifire = false,
        initialRepels = 3,
        accentColorHex = 0xFF66BB6A.toInt(),
      ),
      // 9. Eclipse — Zone Lockdown (100 Pts)
      ShipSpec(
        id = 9,
        name = "Eclipse",
        role = "Zone Lockdown",
        tacticalLore = "Anchors an 8.0s two-way Stasis Bubble (275px radius, 360 Flux, 10.0s CD). Nothing enters or leaves.",
        silhouetteDesc = "Twin-ringed amethyst gravimetric cruiser with forward containment prongs and a glowing magenta-cyan singularity core.",
        weaponSummary = "PRI: PULSE (20 Flux) • SEC: STASIS BUBBLE (360 Flux • 8.0s • 275px • 10.0s CD)",
        primaryTitle = "Twin Pulse",
        secondaryTitle = "Stasis Bubble (360 Flux • 8.0s • 10.0s CD)",
        primaryShortLabel = "PULSE",
        secondaryShortLabel = "BUBBLE",
        speedPts = 11,
        thrustPts = 12,
        turnPts = 9,
        maxFluxPts = 18,
        rechargePts = 13,
        primaryPts = 12,
        secondaryPts = 25,
        maxFlux = computeMaxFlux(18),
        rechargeRate = computeRecharge(13),
        topSpeed = computeTopSpeed(11),
        afterburnerSpeed = computeTopSpeed(11) * 1.32f,
        thrust = computeThrust(12),
        turnRate = computeTurnRate(9),
        primaryCost = 20f,
        primaryCooldown = 0.16f,
        primaryDamage = 112f,
        primarySpeed = 930f,
        primaryStreams = 2,
        primarySpreadRad = 0.045f,
        primaryBounces = 1,
        hasBomb = true,
        bombType = "INTERDICTOR_DOME",
        bombCost = 360f,
        bombCooldown = 10.0f,
        bombDamage = 0f,
        bombBounces = 0,
        bombRecoil = 0f,
        supportsNativeMultifire = false,
        initialRepels = 2,
        accentColorHex = 0xFFEC4899.toInt(),
      ),
    )

  var SHIPS: List<ShipSpec> = createDefaultSignatureShips()
    private set

  fun resetShipsToDefaults() {
    val fresh = createDefaultSignatureShips()
    if (SHIPS.size == fresh.size) {
      for (i in fresh.indices) {
        val dst = SHIPS[i]
        val src = fresh[i]
        dst.maxFlux = src.maxFlux
        dst.rechargeRate = src.rechargeRate
        dst.topSpeed = src.topSpeed
        dst.afterburnerSpeed = src.afterburnerSpeed
        dst.thrust = src.thrust
        dst.turnRate = src.turnRate
        dst.primaryCost = src.primaryCost
        dst.primaryCooldown = src.primaryCooldown
        dst.primaryDamage = src.primaryDamage
        dst.primarySpeed = src.primarySpeed
        dst.bombCost = src.bombCost
        dst.bombCooldown = src.bombCooldown
        dst.bombDamage = src.bombDamage
        dst.bombRecoil = src.bombRecoil
      }
    } else {
      SHIPS = fresh
    }
  }

  val POWERUP_CODEX: List<PowerUpCodexEntry> =
    listOf(
      PowerUpCodexEntry(
        id = "special_nova_ring",
        buffTypeIndex = 0,
        title = "NOVA RING (+1 CHARGE • 0 FLUX)",
        dropRatePercent = 9,
        category = "BUCKET A: SPECIAL BUFF SLOT (0 FLUX)",
        effectSummary = "Loads +1 Nova Ring charge (up to 3) into your 4th Special Buff Button. Fires 16 1-bounce plasma bolts in a full 360° ring at 0 Flux cost!",
        tacticalNote = "Picking up another Nova Ring crate stacks up to 3 charges; picking up a different Special Buff swaps your active slot!",
      ),
      PowerUpCodexEntry(
        id = "special_shatter_cone",
        buffTypeIndex = 1,
        title = "SHATTER CONE (+2 CHARGES • 0 FLUX)",
        dropRatePercent = 9,
        category = "BUCKET A: SPECIAL BUFF SLOT (0 FLUX)",
        effectSummary = "Loads +2 Shatter Cone charges (up to 4) into your Special Buff Button. Unleashes a close-quarters kinetic fan blast!",
        tacticalNote = "Devastating point-blank ambush tool—melts heavy hulls in two zero-Flux pulls!",
      ),
      PowerUpCodexEntry(
        id = "special_seeker_torpedo",
        buffTypeIndex = 2,
        title = "SEEKER TORPEDO (+1 CHARGE • 0 FLUX)",
        dropRatePercent = 9,
        category = "BUCKET A: SPECIAL BUFF SLOT (0 FLUX)",
        effectSummary = "Loads +1 Homing Seeker Torpedo into your Special Buff Button. Actively steers toward the nearest enemy signature!",
        tacticalNote = "Forces evasive hulls to burn a Repel or duck behind sector stations!",
      ),
      PowerUpCodexEntry(
        id = "special_holo_decoy",
        buffTypeIndex = 3,
        title = "HOLO PHANTOM (+1 CHARGE • 0 FLUX)",
        dropRatePercent = 8,
        category = "BUCKET A: SPECIAL BUFF SLOT (0 FLUX)",
        effectSummary = "Launches a mirrored holographic clone flying forward while cloaking your real hull!",
        tacticalNote = "Enemy Seekers and bots lock onto your Holo Phantom—detonate it for EMP Flux drain!",
      ),
      PowerUpCodexEntry(
        id = "special_stasis_barrier",
        buffTypeIndex = 4,
        title = "STASIS BARRIER (+1 CHARGE • 0 FLUX)",
        dropRatePercent = 7,
        category = "BUCKET A: SPECIAL BUFF SLOT (0 FLUX)",
        effectSummary = "Deploys a permanent 3D hard-light bulkhead wall ahead of your ship that reflects ships & shots!",
        tacticalNote = "Block enemy escape corridors or create instant bank-shot geometry in open space!",
      ),
      PowerUpCodexEntry(
        id = "special_hyper_surge",
        buffTypeIndex = 5,
        title = "HYPER SURGE (+1 CHARGE • 0 FLUX)",
        dropRatePercent = 8,
        category = "BUCKET A: SPECIAL BUFF SLOT (0 FLUX)",
        effectSummary = "Ignites hyper-thrusters for supersonic intercept velocity at 0 Flux cost!",
        tacticalNote = "Escape gravity wells or chase down fleeing High-Value Targets!",
      ),
      PowerUpCodexEntry(
        id = "flux_restore",
        buffTypeIndex = 6,
        title = "FLUX SURGE (+750 FLUX)",
        dropRatePercent = 12,
        category = "BUCKET B: INSTANT SHIP ENHANCER",
        effectSummary = "Instantly restores +750 Flux to your ship's health/ammo capacitor.",
        tacticalNote = "Grab mid-dogfight to survive lethal volleys without retreating to Safe Zone.",
      ),
      PowerUpCodexEntry(
        id = "repel_charge",
        buffTypeIndex = 7,
        title = "REPEL CHARGE (+1 CHARGE)",
        dropRatePercent = 10,
        category = "BUCKET B: INSTANT SHIP ENHANCER",
        effectSummary = "Adds +1 Repel Shockwave charge (up to 4 max).",
        tacticalNote = "Deflects enemy bombs, mines, seekers, and ships within 250px!",
      ),
      PowerUpCodexEntry(
        id = "multi_fire_unlock",
        buffTypeIndex = 8,
        title = "MULTI-SHOT (+25 SHOTS)",
        dropRatePercent = 7,
        category = "BUCKET B: INSTANT SHIP ENHANCER",
        effectSummary = "Unlocks angled multi-shot side barrels on ANY ship!",
        tacticalNote = "Turns Viper into a 3-beam rail prism or Goliath into a 4-stream siege wall!",
      ),
      PowerUpCodexEntry(
        id = "bounce_bullets",
        buffTypeIndex = 9,
        title = "RICOCHET ROUNDS (+30 SHOTS)",
        dropRatePercent = 6,
        category = "BUCKET B: INSTANT SHIP ENHANCER",
        effectSummary = "Grants wall-bounce capability to primary weapons for +30 shots.",
        tacticalNote = "Bank shots around orbital stations and permanent barriers!",
      ),
      PowerUpCodexEntry(
        id = "hull_stat_tuning",
        buffTypeIndex = 10,
        title = "5-STAT HULL TUNING (+1..+3 STACKS)",
        dropRatePercent = 15,
        category = "BUCKET B: ALL-5-STAT HULL UPGRADES",
        effectSummary = "Awards +1 stack (up to +3 max per life) to Speed, Thrust, Turn Rate, Max Flux, or Flux Recharge!",
        tacticalNote = "Each of the 5 hull stats drops as its own color-coded tuning icon on the HUD buff bar!",
      ),
    )

  fun getAllTenShips(): List<ShipSpec> = SHIPS + buildShipSpecFromCustomSlot(activeCustomShipSlot)

  fun getAllNineShips(): List<ShipSpec> = getAllTenShips()

  fun getShipById(id: Int): ShipSpec {
    if (id == 10) return buildShipSpecFromCustomSlot(activeCustomShipSlot)
    return SHIPS.find { it.id == id } ?: SHIPS.first()
  }
}
