package com.example.engine

import android.graphics.Color
import java.util.Locale
import kotlin.math.abs
import org.json.JSONObject

data class ShipSpec(
  val id: Int,
  val name: String,
  val role: String,
  val tacticalLore: String,
  val silhouetteDesc: String,
  val weaponSummary: String,
  val primaryTitle: String,
  val secondaryTitle: String,
  val primaryShortLabel: String = "FIRE",
  val secondaryShortLabel: String = "BOMB",
  val speedPts: Int,
  val thrustPts: Int,
  val turnPts: Int,
  val maxFluxPts: Int,
  val rechargePts: Int,
  val primaryPts: Int,
  val secondaryPts: Int,
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
  var primaryBounces: Int,
  val isParallelTwin: Boolean = false,
  val hasBomb: Boolean,
  val bombType: String, // "NONE", "PHASE_DASH", "BOUNCE", "SIEGE", "CLOAK_EMP", "PROX", "MINE", "NOVA_RING", "SHATTER_CONE", "SEEKER_TORPEDO"
  var bombCost: Float,
  var bombCooldown: Float,
  var bombDamage: Float,
  var bombBounces: Int,
  var bombRecoil: Float = 0f,
  val supportsNativeMultifire: Boolean = false,
  val initialRepels: Int = 2,
  val accentColorHex: Int,
) {
  val totalAllocatedPoints: Int
    get() = speedPts + thrustPts + turnPts + maxFluxPts + rechargePts + primaryPts + secondaryPts
}

enum class BuffAimMode {
  TAP_INSTANT,
  AIM_DIRECTIONAL,
  AIM_BOUNCE_RAY,
}

enum class CustomBuffEffect(val label: String) {
  NONE("Kinetic / Plasma Blast"),
  EMP("EMP Flux Drain & Freeze"),
  PULL("Gravity Vortex Pull"),
  PUSH("Shockwave Knockback"),
  SLOW("Cryo / Stasis Slow"),
  BARRIER("Hard-Light Energy Wall"),
  SPEED_BOOST("Hyper Thruster Overdrive"),
  CLONE("Holographic Decoy Clone"),
}

data class SpecialBuffRegistryEntry(
  val id: String,
  val buffTypeIndex: Int,
  var officialName: String,
  var shortLabel: String,
  var chargesPerPickup: Int,
  var aimMode: BuffAimMode,
  var strokeColorHex: String,
  var fillColorArgb: Int,
  var summary: String,
  val isCustomForged: Boolean = false,
  var projectileSpeed: Float = 720f,
  var flightDuration: Float = 2.5f,
  var damage: Float = 420f,
  var blastRadius: Float = 180f,
  var specialEffect: CustomBuffEffect = CustomBuffEffect.NONE,
  var wallBounces: Int = 2,
  val promptUsed: String = "",
) {
  val strokeColorInt: Int
    get() =
      try {
        Color.parseColor(strokeColorHex)
      } catch (_: Exception) {
        Color.parseColor("#22D3EE")
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
        Color.argb(155, (Color.red(c) * 0.34f).toInt(), (Color.green(c) * 0.34f).toInt(), (Color.blue(c) * 0.34f).toInt())
      } catch (_: Exception) {
        Color.argb(155, 14, 58, 78)
      }
    }

    fun fromJsonObject(obj: JSONObject): SpecialBuffRegistryEntry {
      val stroke = CustomHullVisualRecipe.ensureMinLuminanceHex(obj.optString("strokeColorHex", "#22D3EE"), 0.45f)
      val aim =
        runCatching { BuffAimMode.valueOf(obj.optString("aimMode", "AIM_DIRECTIONAL")) }
          .getOrDefault(BuffAimMode.AIM_DIRECTIONAL)
      val eff =
        runCatching { CustomBuffEffect.valueOf(obj.optString("specialEffect", "NONE")) }
          .getOrDefault(CustomBuffEffect.NONE)
      return SpecialBuffRegistryEntry(
        id = obj.optString("id", "CUSTOM_BUFF_${System.currentTimeMillis()}"),
        buffTypeIndex = obj.optInt("buffTypeIndex", 11).coerceAtLeast(11),
        officialName = obj.optString("officialName", "CUSTOM ORB").take(22).uppercase(Locale.US),
        shortLabel = obj.optString("shortLabel", "ORB").take(8).uppercase(Locale.US),
        chargesPerPickup = obj.optInt("chargesPerPickup", 2).coerceIn(1, 8),
        aimMode = aim,
        strokeColorHex = stroke,
        fillColorArgb = obj.optInt("fillColorArgb", fillColorFromStroke(stroke)),
        summary = obj.optString("summary", "Custom AI-forged tactical arena ordnance."),
        isCustomForged = obj.optBoolean("isCustomForged", true),
        projectileSpeed = obj.optDouble("projectileSpeed", 640.0).toFloat().coerceIn(120f, 1600f),
        flightDuration = obj.optDouble("flightDuration", 2.5).toFloat().coerceIn(0.4f, 12.0f),
        damage = obj.optDouble("damage", 480.0).toFloat().coerceIn(0f, 2500f),
        blastRadius = obj.optDouble("blastRadius", 200.0).toFloat().coerceIn(40f, 650f),
        specialEffect = eff,
        wallBounces = obj.optInt("wallBounces", 2).coerceIn(0, 8),
        promptUsed = obj.optString("promptUsed", ""),
      )
    }

    fun synthesizeFromPrompt(rawPrompt: String, nextBuffIndex: Int): SpecialBuffRegistryEntry {
      val clean = rawPrompt.trim().ifEmpty { "Create a buff called Gravity Well that fires a slow orb that pulls nearby enemies inward for 3 seconds, 2 charges" }
      val lower = clean.lowercase(Locale.US)

      // 1. Extract explicit name ("called X", "named X", or quoted "X")
      val quotedMatch = Regex("\"([^\"]{2,22})\"").find(clean)?.groupValues?.getOrNull(1)
      val calledMatch =
        Regex("(?i)(?:called|named)\\s+([A-Za-z0-9 _\\-]{2,22}?)(?:\\s+(?:that|which|with|firing|fires|for|costing|,)|$)")
          .find(clean)
          ?.groupValues
          ?.getOrNull(1)
      val rawName =
        (quotedMatch ?: calledMatch ?: run {
          val words =
            clean.uppercase(Locale.US)
              .replace(Regex("[^A-Z0-9 ]"), "")
              .split(" ")
              .filter { it.length >= 3 && it !in setOf("CREATE", "BUFF", "CALLED", "NAMED", "THAT", "FIRES", "WITH", "THE", "AND", "FOR", "CHARGES", "CHARGE", "SECONDS") }
          if (words.size >= 2) "${words[0]} ${words[1]}" else words.firstOrNull() ?: "VOID ORB"
        }).trim().uppercase(Locale.US).take(20)

      val shortLbl =
        rawName.split(" ")
          .firstOrNull { it.length >= 3 }
          ?.take(8)
          ?: rawName.take(7).ifEmpty { "FORGE" }

      // 2. Extract Special Effect
      val effect =
        when {
          lower.contains("pull") || lower.contains("gravity") || lower.contains("vortex") || lower.contains("black hole") || lower.contains("inward") || lower.contains("suck") ->
            CustomBuffEffect.PULL
          lower.contains("push") || lower.contains("knock") || lower.contains("repulse") || lower.contains("shockwave") || lower.contains("impulse") ->
            CustomBuffEffect.PUSH
          lower.contains("emp") || lower.contains("drain") || lower.contains("disable") || lower.contains("jam") || lower.contains("ion") ->
            CustomBuffEffect.EMP
          lower.contains("slow") || lower.contains("stasis") || lower.contains("cryo") || lower.contains("freeze") || lower.contains("snare") || lower.contains("web") ->
            CustomBuffEffect.SLOW
          lower.contains("wall") || lower.contains("barrier") || lower.contains("shield") || lower.contains("block") ->
            CustomBuffEffect.BARRIER
          lower.contains("boost") || lower.contains("speed") || lower.contains("dash") || lower.contains("rush") || lower.contains("overdrive") || lower.contains("sprint") ->
            CustomBuffEffect.SPEED_BOOST
          lower.contains("clone") || lower.contains("decoy") || lower.contains("hologram") || lower.contains("illusion") || lower.contains("phantom") ->
            CustomBuffEffect.CLONE
          else -> CustomBuffEffect.NONE
        }

      // 3. Extract Aiming Mode
      val aimMode =
        when {
          effect in setOf(CustomBuffEffect.BARRIER, CustomBuffEffect.SPEED_BOOST, CustomBuffEffect.CLONE) ->
            BuffAimMode.TAP_INSTANT
          lower.contains("bounce") || lower.contains("ricochet") || lower.contains("bank") || lower.contains("ray") ->
            BuffAimMode.AIM_BOUNCE_RAY
          lower.contains("instant") || lower.contains("nova") || lower.contains("360") || lower.contains("around") ->
            BuffAimMode.TAP_INSTANT
          else -> BuffAimMode.AIM_DIRECTIONAL
        }

      // 4. Extract Charges, Duration, Speed, Damage, Radius
      val chargesMatch = Regex("(\\d+)\\s*charge").find(lower)?.groupValues?.getOrNull(1)?.toIntOrNull()
      val charges = (chargesMatch ?: if (lower.contains("single")) 1 else 2).coerceIn(1, 8)

      val durationMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:s|sec|second)").find(lower)?.groupValues?.getOrNull(1)?.toFloatOrNull()
      val duration =
        (durationMatch ?: when (effect) {
          CustomBuffEffect.BARRIER -> 8.0f
          CustomBuffEffect.CLONE -> 7.0f
          CustomBuffEffect.SPEED_BOOST -> 3.0f
          CustomBuffEffect.PULL -> 3.0f
          else -> 2.5f
        }).coerceIn(0.5f, 12.0f)

      val speed =
        when {
          lower.contains("very slow") || lower.contains("stationary") -> 180f
          lower.contains("slow") || effect == CustomBuffEffect.PULL -> 340f
          lower.contains("hyper") || lower.contains("rail") || lower.contains("sniper") || lower.contains("instant") -> 1150f
          lower.contains("fast") || lower.contains("rapid") -> 920f
          else -> 640f
        }

      val dmgMatch = Regex("(\\d{2,4})\\s*(?:dmg|damage|flux)").find(lower)?.groupValues?.getOrNull(1)?.toFloatOrNull()
      val damage =
        (dmgMatch ?: when {
          lower.contains("heavy") || lower.contains("nuke") || lower.contains("massive") -> 880f
          lower.contains("non-lethal") || lower.contains("zero damage") || effect in setOf(CustomBuffEffect.SPEED_BOOST, CustomBuffEffect.CLONE, CustomBuffEffect.BARRIER) -> 0f
          effect == CustomBuffEffect.PULL -> 360f
          effect == CustomBuffEffect.EMP -> 440f
          else -> 520f
        }).coerceIn(0f, 2500f)

      val radius =
        when {
          lower.contains("wide") || lower.contains("huge") || lower.contains("massive") -> 320f
          lower.contains("small") || lower.contains("pinpoint") -> 110f
          effect in setOf(CustomBuffEffect.PULL, CustomBuffEffect.PUSH, CustomBuffEffect.EMP) -> 260f
          else -> 190f
        }

      val bounces = if (aimMode == BuffAimMode.AIM_BOUNCE_RAY || lower.contains("bounce")) 3 else 1

      // 5. Color Selection
      val colorHex =
        when {
          lower.contains("purple") || lower.contains("violet") || lower.contains("void") || effect == CustomBuffEffect.PULL -> "#C084FC"
          lower.contains("emerald") || lower.contains("green") || lower.contains("acid") || lower.contains("plasma") -> "#34D399"
          lower.contains("red") || lower.contains("crimson") || lower.contains("blood") || lower.contains("ruby") -> "#FB7185"
          lower.contains("gold") || lower.contains("solar") || lower.contains("amber") || lower.contains("yellow") -> "#FACC15"
          lower.contains("orange") || lower.contains("fire") || lower.contains("magma") -> "#FB923C"
          lower.contains("cyan") || lower.contains("ice") || lower.contains("frost") || effect == CustomBuffEffect.EMP || effect == CustomBuffEffect.SLOW -> "#38BDF8"
          else -> "#22D3EE"
        }

      val idSlug =
        "FORGED_" +
          rawName.replace(Regex("[^A-Z0-9]"), "_").trim('_').ifEmpty { "BUFF" } +
          "_$nextBuffIndex"

      val summaryText =
        "${effect.label} • ${charges}x Charges • ${String.format(Locale.US, "%.1fs", duration)} flight • ${damage.toInt()}❖ DMG (${radius.toInt()}px radius)."

      return SpecialBuffRegistryEntry(
        id = idSlug,
        buffTypeIndex = nextBuffIndex,
        officialName = rawName,
        shortLabel = shortLbl,
        chargesPerPickup = charges,
        aimMode = aimMode,
        strokeColorHex = colorHex,
        fillColorArgb = fillColorFromStroke(colorHex),
        summary = summaryText,
        isCustomForged = true,
        projectileSpeed = speed,
        flightDuration = duration,
        damage = damage,
        blastRadius = radius,
        specialEffect = effect,
        wallBounces = bounces,
        promptUsed = clean.take(160),
      )
    }
  }
}

/**
 * Compact (<2KB JSON) Visual Hull Recipe for the 9th Custom Modular Ship ("AI Hull Forge").
 * Enforces minimum luminance contrast (L >= 0.32) against #000000 space and fits inside the 36x36px physical hitbox.
 */
data class CustomHullVisualRecipe(
  val hullName: String = "RIFT FRAME",
  val silhouette: String = "RAZOR_INTERCEPTOR", // RAZOR_INTERCEPTOR, CRESCENT_SCYTHE, BASTION_DREADNOUGHT, MANTA_STINGRAY, X_VALKYRIE, PINCER_MANDIBLE
  val wingSweep: Float = 1.05f, // 0.65f .. 1.35f
  val chassisPlates: Int = 3, // 1 .. 4
  val primaryHex: String = "#1E3A8A",
  val trimHex: String = "#FACC15",
  val coreGlowHex: String = "#00F5D4",
  val engineStyle: String = "TWIN_ION", // TWIN_ION, QUAD_NOVA, BIO_ARC, HEAVY_THRUSTER
  val promptUsed: String = "Obsidian cobalt interceptor with gold trim and cyan plasma core",
) {
  fun toJsonString(): String {
    return JSONObject().apply {
      put("hullName", hullName.take(22))
      put("silhouette", silhouette)
      put("wingSweep", wingSweep.toDouble())
      put("chassisPlates", chassisPlates.coerceIn(1, 4))
      put("primaryHex", ensureMinLuminanceHex(primaryHex, 0.32f))
      put("trimHex", ensureMinLuminanceHex(trimHex, 0.45f))
      put("coreGlowHex", ensureMinLuminanceHex(coreGlowHex, 0.55f))
      put("engineStyle", engineStyle)
      put("promptUsed", promptUsed.take(120))
    }.toString()
  }

  companion object {
    val SILHOUETTES =
      listOf(
        "RAZOR_INTERCEPTOR",
        "CRESCENT_SCYTHE",
        "BASTION_DREADNOUGHT",
        "MANTA_STINGRAY",
        "X_VALKYRIE",
        "PINCER_MANDIBLE",
      )

    val ENGINE_STYLES =
      listOf(
        "TWIN_ION",
        "QUAD_NOVA",
        "BIO_ARC",
        "HEAVY_THRUSTER",
      )

    fun ensureMinLuminanceHex(hex: String, minLum: Float = 0.32f): String {
      return try {
        val parsed = Color.parseColor(hex)
        val r = Color.red(parsed) / 255f
        val g = Color.green(parsed) / 255f
        val b = Color.blue(parsed) / 255f
        val lum = 0.2126f * r + 0.7152f * g + 0.0722f * b
        if (lum >= minLum) {
          String.format(Locale.US, "#%02X%02X%02X", Color.red(parsed), Color.green(parsed), Color.blue(parsed))
        } else {
          val boost = (minLum - lum).coerceIn(0f, 0.65f)
          val nr = ((r + boost * 0.85f).coerceIn(0.18f, 1f) * 255f).toInt()
          val ng = ((g + boost * 0.95f).coerceIn(0.20f, 1f) * 255f).toInt()
          val nb = ((b + boost * 1.05f).coerceIn(0.24f, 1f) * 255f).toInt()
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
          hullName = obj.optString("hullName", "RIFT FRAME").take(22),
          silhouette = obj.optString("silhouette", "RAZOR_INTERCEPTOR"),
          wingSweep = obj.optDouble("wingSweep", 1.05).toFloat().coerceIn(0.65f, 1.35f),
          chassisPlates = obj.optInt("chassisPlates", 3).coerceIn(1, 4),
          primaryHex = ensureMinLuminanceHex(obj.optString("primaryHex", "#1E3A8A"), 0.32f),
          trimHex = ensureMinLuminanceHex(obj.optString("trimHex", "#FACC15"), 0.45f),
          coreGlowHex = ensureMinLuminanceHex(obj.optString("coreGlowHex", "#00F5D4"), 0.55f),
          engineStyle = obj.optString("engineStyle", "TWIN_ION"),
          promptUsed = obj.optString("promptUsed", "Custom Modular Rift Frame").take(120),
        )
      } catch (_: Exception) {
        CustomHullVisualRecipe()
      }
    }

    fun synthesizeFromPrompt(rawPrompt: String, variationSalt: Int = 0): CustomHullVisualRecipe {
      val cleanPrompt = rawPrompt.trim().ifEmpty { "Cybernetic emerald manta interceptor with gold trim" }
      val lower = cleanPrompt.lowercase(Locale.US)
      val hash = abs(cleanPrompt.hashCode() + variationSalt * 7919)

      val silhouette =
        when {
          lower.contains("crescent") || lower.contains("scythe") || lower.contains("moon") || lower.contains("arc") ->
            "CRESCENT_SCYTHE"
          lower.contains("dreadnought") || lower.contains("tank") || lower.contains("bastion") || lower.contains("fortress") || lower.contains("heavy") || lower.contains("goliath") ->
            "BASTION_DREADNOUGHT"
          lower.contains("manta") || lower.contains("stingray") || lower.contains("bio") || lower.contains("organic") || lower.contains("wing") ->
            "MANTA_STINGRAY"
          lower.contains("x-") || lower.contains("valkyrie") || lower.contains("cross") || lower.contains("starfighter") ->
            "X_VALKYRIE"
          lower.contains("pincer") || lower.contains("claw") || lower.contains("mandible") || lower.contains("scorpion") || lower.contains("beetle") ->
            "PINCER_MANDIBLE"
          lower.contains("razor") || lower.contains("viper") || lower.contains("needle") || lower.contains("sniper") || lower.contains("blade") ->
            "RAZOR_INTERCEPTOR"
          else -> SILHOUETTES[hash % SILHOUETTES.size]
        }

      val primaryHex =
        when {
          lower.contains("crimson") || lower.contains("red") || lower.contains("blood") || lower.contains("ruby") -> "#991B1B"
          lower.contains("emerald") || lower.contains("green") || lower.contains("jade") || lower.contains("venom") -> "#065F46"
          lower.contains("violet") || lower.contains("purple") || lower.contains("amethyst") || lower.contains("void") -> "#4C1D95"
          lower.contains("gold") || lower.contains("solar") || lower.contains("imperial") || lower.contains("amber") -> "#78350F"
          lower.contains("silver") || lower.contains("chrome") || lower.contains("titanium") || lower.contains("white") -> "#475569"
          lower.contains("cyan") || lower.contains("ice") || lower.contains("frost") || lower.contains("cobalt") -> "#1E3A8A"
          else -> {
            val palette = listOf("#1E3A8A", "#4C1D95", "#065F46", "#7F1D1D", "#334155", "#78350F")
            palette[hash % palette.size]
          }
        }

      val trimHex =
        when {
          lower.contains("gold") || lower.contains("brass") || lower.contains("imperial") -> "#FACC15"
          lower.contains("copper") || lower.contains("bronze") || lower.contains("rust") || lower.contains("orange") -> "#FB923C"
          lower.contains("rose") || lower.contains("pink") || lower.contains("magenta") || lower.contains("neon") -> "#F472B6"
          lower.contains("emerald") || lower.contains("lime") || lower.contains("acid") -> "#34D399"
          lower.contains("chrome") || lower.contains("platinum") || lower.contains("silver") -> "#E2E8F0"
          else -> {
            val trims = listOf("#FACC15", "#38BDF8", "#FB923C", "#C084FC", "#34D399", "#F472B6")
            trims[(hash / 7) % trims.size]
          }
        }

      val coreGlowHex =
        when {
          lower.contains("plasma") || lower.contains("emerald") || lower.contains("toxic") -> "#00E676"
          lower.contains("solar") || lower.contains("fire") || lower.contains("flame") || lower.contains("magma") -> "#FFAB00"
          lower.contains("void") || lower.contains("quantum") || lower.contains("violet") || lower.contains("emp") -> "#D500F9"
          lower.contains("crimson") || lower.contains("laser") || lower.contains("sith") -> "#FF5252"
          else -> "#00F5D4"
        }

      val wingSweep =
        when {
          lower.contains("wide") || lower.contains("broad") -> 1.28f
          lower.contains("compact") || lower.contains("narrow") || lower.contains("sleek") -> 0.78f
          lower.contains("swept") || lower.contains("aggressive") -> 1.18f
          else -> 0.82f + ((hash % 48) / 100f)
        }

      val chassisPlates = ((hash / 13) % 4) + 1
      val engineStyle = ENGINE_STYLES[(hash / 29) % ENGINE_STYLES.size]

      val words =
        cleanPrompt
          .uppercase(Locale.US)
          .replace(Regex("[^A-Z0-9 ]"), "")
          .split(" ")
          .filter { it.length >= 3 && it !in setOf("WITH", "THE", "AND", "SHIP", "HULL", "CUSTOM") }
      val generatedTitle =
        if (words.size >= 2) {
          "${words[0]} ${words[1]}".take(18)
        } else if (words.size == 1) {
          "${words[0]} MK-IX".take(18)
        } else {
          "RIFT FRAME IX"
        }

      return CustomHullVisualRecipe(
        hullName = generatedTitle,
        silhouette = silhouette,
        wingSweep = wingSweep.coerceIn(0.65f, 1.35f),
        chassisPlates = chassisPlates,
        primaryHex = ensureMinLuminanceHex(primaryHex, 0.32f),
        trimHex = ensureMinLuminanceHex(trimHex, 0.45f),
        coreGlowHex = ensureMinLuminanceHex(coreGlowHex, 0.55f),
        engineStyle = engineStyle,
        promptUsed = cleanPrompt.take(120),
      )
    }
  }
}

data class PrimaryArchetypeOption(
  val id: String,
  val title: String,
  val pointCost: Int,
  val summary: String,
)

data class SecondaryAbilityOption(
  val id: String,
  val title: String,
  val pointCost: Int,
  val fluxCostLabel: String,
  val summary: String,
)

data class PlayfieldRiftCodexEntry(
  val id: String,
  val title: String,
  val classification: String,
  val specBadge: String,
  val accentHex: String,
  val effectSummary: String,
  val tacticalNote: String,
)

/**
 * Represents one of the 3 saveable loadout slots (Frame A, Frame B, Frame C) for the 9th Ship ("Rift Frame").
 * Implements Option A (Free-Spend + Universal Baseline Hull Floor, 0..30 pts per stat, 100-Point Parity Matrix).
 */
data class CustomShipLoadoutSlot(
  val slotId: String = "Frame A", // "Frame A", "Frame B", "Frame C"
  val customName: String = "Rift Frame",
  val speedPts: Int = 4,
  val thrustPts: Int = 4,
  val turnPts: Int = 4,
  val maxFluxPts: Int = 4,
  val rechargePts: Int = 4,
  val primaryArchetype: String = "LIGHT_PULSE", // 0, 10, 12, or 15 pts
  val secondaryType: String = "NONE", // 0, 10, 15, 18, 20, 22, or 25 pts
  val visualRecipe: CustomHullVisualRecipe = CustomHullVisualRecipe(),
  val aiGenerationsCount: Int = 0,
) {
  val primaryPts: Int
    get() = getPrimaryPointCost(primaryArchetype)

  val secondaryPts: Int
    get() = getSecondaryPointCost(secondaryType)

  fun totalAllocatedPoints(): Int =
    speedPts.coerceIn(0, 30) +
      thrustPts.coerceIn(0, 30) +
      turnPts.coerceIn(0, 30) +
      maxFluxPts.coerceIn(0, 30) +
      rechargePts.coerceIn(0, 30) +
      primaryPts +
      secondaryPts

  fun toShipSpec(): ShipSpec {
    val sPts = speedPts.coerceIn(0, 30)
    val thPts = thrustPts.coerceIn(0, 30)
    val tuPts = turnPts.coerceIn(0, 30)
    val flPts = maxFluxPts.coerceIn(0, 30)
    val rePts = rechargePts.coerceIn(0, 30)

    // Universal Baseline Hull Floor + Point Scaling (Exact 100-Point Parity Formula)
    val computedTopSpeed = RiftCodex.computeTopSpeed(sPts)
    val computedBurnSpeed = computedTopSpeed * 1.32f
    val computedThrust = RiftCodex.computeThrust(thPts)
    val computedTurnRate = RiftCodex.computeTurnRate(tuPts)
    val computedMaxFlux = RiftCodex.computeMaxFlux(flPts)
    val computedRecharge = RiftCodex.computeRecharge(rePts)

    val isPureGunfighterPassive = secondaryType == "PASSIVE_GUNFIGHTER"
    val velBonus = if (isPureGunfighterPassive) 1.10f else 1.0f

    val (priTitle, priShort, pCost, pCd, pDmg, pSpd, pStreams, pSpread, pBounces, isParallel) =
      when (primaryArchetype) {
        "TWIN_SCATTER" ->
          PrimaryTuple("Short-Range Twin Scatter", "SCATTER", 20f, 0.14f, 175f, 900f * velBonus, 2, 0.065f, 0, false)
        "ION_NEEDLER" ->
          PrimaryTuple("Twin Ion Needlers", "NEEDLER", 18f, 0.13f, 165f, 950f * velBonus, 2, 0.04f, 0, false)
        "ANGLED_PULSE" ->
          PrimaryTuple("Twin Angled Pulse", "PULSE", 20f, 0.16f, 195f, 930f * velBonus, 2, 0.045f, 1, false)
        "HEAVY_CANNON" ->
          PrimaryTuple("Heavy Dual Cannon", "CANNON", 40f, 0.21f, 310f, 880f * velBonus, 2, 0.05f, 0, false)
        "QUAD_PULSE" ->
          PrimaryTuple("Quad-Converging Pulse", "QUAD PULSE", 24f, 0.15f, 72f, 940f * velBonus, 4, 0.045f, 0, false)
        "SNIPER_RAIL" ->
          PrimaryTuple("Hyper-Velocity Dual Rail", "RAIL", 25f, 0.15f, 285f, 1120f * velBonus, 1, 0f, 0, false)
        "ROTARY_PULSE" ->
          PrimaryTuple("High-RPM Rotary Pulse", "ROTARY", 15f, 0.095f, 240f, 980f * velBonus, 1, 0f, 0, false)
        "PARALLEL_TWIN" ->
          PrimaryTuple("Parallel Twin-Stream Autocannon", "AUTOCANNON", 22f, 0.13f, 135f, 1000f * velBonus, 2, 0.025f, 0, true)
        else -> // "LIGHT_PULSE" (0 Pts Free Starter Primary)
          PrimaryTuple("Standard Light Pulse", "LIGHT PULSE", 16f, 0.16f, 155f, 920f * velBonus, 1, 0f, 0, false)
      }

    val normalizedSecondary =
      when (secondaryType) {
        "BURST" -> "NOVA_RING"
        "SHOTGUN" -> "SHATTER_CONE"
        "MISSILE" -> "SEEKER_TORPEDO"
        else -> secondaryType
      }
    val nativeMultifire = normalizedSecondary == "NATIVE_MULTISHOT"
    val extraRepels = if (normalizedSecondary == "MINE") 1 else 0
    val hasActiveSecondaryButton =
      normalizedSecondary !in setOf("NONE", "PASSIVE_GUNFIGHTER", "NATIVE_MULTISHOT")

    val (secTitle, secShort, bCost, bCd, bDmg, bBounces, bRecoil) =
      when (normalizedSecondary) {
        "PHASE_DASH" ->
          SecondaryTuple("Phase Dash / Sniper Overcharge (300❖)", "PHASE", 300f, 2.5f, 0f, 0, 0f)
        "NATIVE_MULTISHOT" ->
          SecondaryTuple("Native 3x / 1/3x Split Multi-Shot", "SPLIT MULTI", 0f, 0f, 0f, 0, 0f)
        "MINE" ->
          SecondaryTuple("Proximity Mines +1 Repel (300❖)", "PROX MINE", 300f, 0.80f, 1040f, 0, 0f)
        "BOUNCE" ->
          SecondaryTuple("Bounce Bomb (350❖)", "BOUNCE BOMB", 350f, 0.85f, 780f, 3, if (isPureGunfighterPassive) 0f else 180f)
        "PROX" ->
          SecondaryTuple("Proximity Flak Bomb (450❖)", "PROX FLAK", 450f, 0.95f, 820f, 1, if (isPureGunfighterPassive) 0f else 210f)
        "NOVA_RING" ->
          SecondaryTuple("Native Nova Ring (500❖ • 4s CD)", "NOVA RING", 500f, 4.0f, 280f, 2, 0f)
        "SHATTER_CONE" ->
          SecondaryTuple("Native Shatter Cone (500❖ • 4s CD)", "SHATTER CONE", 500f, 4.0f, 55f, 2, 0f)
        "SIEGE" ->
          SecondaryTuple("Siege Bomb (650❖ • Heavy Recoil)", "SIEGE BOMB", 650f, 1.20f, 1350f, 1, if (isPureGunfighterPassive) 0f else 360f)
        "CLOAK_EMP" ->
          SecondaryTuple("Tactical Cloak (180❖/s) + 0❖ Decloak EMP", "CLOAK", 0f, 0.95f, 0f, 0, 0f)
        "SEEKER_TORPEDO" ->
          SecondaryTuple("Native Seeker Torpedo (600❖ • 5s CD)", "SEEKER", 600f, 5.0f, 780f, 6, 0f)
        "PASSIVE_GUNFIGHTER" ->
          SecondaryTuple("Pure Gunfighter Passive (+10% Vel, 0 Recoil)", "PASSIVE", 0f, 0f, 0f, 0, 0f)
        else ->
          SecondaryTuple("No Secondary (0 Pts)", "NONE", 0f, 0f, 0f, 0, 0f)
      }

    val accentInt =
      try {
        Color.parseColor(visualRecipe.coreGlowHex)
      } catch (_: Exception) {
        0xFF00F5D4.toInt()
      }

    val cleanCustomTitle =
      customName
        .replace(Regex("(?i)RIFT FRAME\\s*9([ABC])"), "Rift Frame")
        .replace("9A", "Frame A")
        .replace("9B", "Frame B")
        .replace("9C", "Frame C")
        .trim()
        .ifBlank { "Rift Frame" }

    return ShipSpec(
      id = 9,
      name = cleanCustomTitle,
      role = "Custom Modular Rift Frame ($slotId • ${totalAllocatedPoints()} Pts)",
      tacticalLore = "Progressive Level 20–100 modular chassis engineered on the universal Baseline Hull Floor with AI Hull Forge shader geometry.",
      silhouetteDesc = visualRecipe.promptUsed,
      weaponSummary = "PRIMARY: $priTitle (${primaryPts}P) • CLASS/SEC: $secTitle (${secondaryPts}P)",
      primaryTitle = priTitle,
      secondaryTitle = secTitle,
      primaryShortLabel = priShort,
      secondaryShortLabel = secShort,
      speedPts = sPts,
      thrustPts = thPts,
      turnPts = tuPts,
      maxFluxPts = flPts,
      rechargePts = rePts,
      primaryPts = primaryPts,
      secondaryPts = secondaryPts,
      maxFlux = computedMaxFlux,
      rechargeRate = computedRecharge,
      topSpeed = computedTopSpeed,
      afterburnerSpeed = computedBurnSpeed,
      thrust = computedThrust,
      turnRate = computedTurnRate,
      primaryCost = pCost,
      primaryCooldown = pCd,
      primaryDamage = pDmg,
      primarySpeed = pSpd,
      primaryStreams = pStreams,
      primarySpreadRad = pSpread,
      primaryBounces = pBounces,
      isParallelTwin = isParallel,
      hasBomb = hasActiveSecondaryButton,
      bombType = normalizedSecondary,
      bombCost = bCost,
      bombCooldown = bCd,
      bombDamage = bDmg,
      bombBounces = bBounces,
      bombRecoil = bRecoil,
      supportsNativeMultifire = nativeMultifire,
      initialRepels = 2 + extraRepels,
      accentColorHex = accentInt,
    )
  }

  fun toJsonString(): String {
    return JSONObject().apply {
      put("slotId", slotId)
      put("customName", customName.take(18))
      put("speedPts", speedPts.coerceIn(0, 30))
      put("thrustPts", thrustPts.coerceIn(0, 30))
      put("turnPts", turnPts.coerceIn(0, 30))
      put("maxFluxPts", maxFluxPts.coerceIn(0, 30))
      put("rechargePts", rechargePts.coerceIn(0, 30))
      put("primaryArchetype", primaryArchetype)
      put("secondaryType", secondaryType)
      put("visualRecipe", JSONObject(visualRecipe.toJsonString()))
      put("aiGenerationsCount", aiGenerationsCount)
    }.toString()
  }

  companion object {
    val PRIMARY_OPTIONS: List<PrimaryArchetypeOption> =
      listOf(
        PrimaryArchetypeOption("LIGHT_PULSE", "Standard Light Pulse", 0, "Free starter pulse blaster (16❖)."),
        PrimaryArchetypeOption("TWIN_SCATTER", "Short-Range Twin Scatter", 10, "Fast close-quarters twin defensive blasters (20❖, Manta style)."),
        PrimaryArchetypeOption("ION_NEEDLER", "Twin Ion Needlers", 10, "Rapid-fire dual ion needlers (18❖, Disruptor style)."),
        PrimaryArchetypeOption("ANGLED_PULSE", "Twin Angled Pulse", 12, "Dual-wing angled pulse with 1 native wall bounce (20❖, Ricochet style)."),
        PrimaryArchetypeOption("HEAVY_CANNON", "Heavy Dual Cannon", 12, "Slow fire rate, heavy kinetic shells (40❖, Goliath style)."),
        PrimaryArchetypeOption("QUAD_PULSE", "Quad-Converging Pulse", 12, "4 light bolts converging at mid-range (24❖, Valkyrie style)."),
        PrimaryArchetypeOption("SNIPER_RAIL", "Hyper-Velocity Dual Rail", 15, "Fastest projectile velocity, pinpoint single-shot punch (25❖, Viper style)."),
        PrimaryArchetypeOption("ROTARY_PULSE", "High-RPM Adaptive Rotary", 15, "Fastest fire rate sustained suppressive stream (15❖, Suppressor style)."),
        PrimaryArchetypeOption("PARALLEL_TWIN", "Parallel Twin-Stream", 15, "2 parallel high-velocity bolts with wide dogfight hitbox (22❖, Twinfang style)."),
      )

    val SECONDARY_OPTIONS: List<SecondaryAbilityOption> =
      listOf(
        SecondaryAbilityOption("NONE", "No Secondary (Starter)", 0, "0❖", "Leaves Secondary slot empty so all points go to hull stats or primary."),
        SecondaryAbilityOption("PASSIVE_GUNFIGHTER", "Pure Gunfighter Passive", 10, "PASSIVE", "+10% Primary Bullet Velocity and 0 firing recoil."),
        SecondaryAbilityOption("PHASE_DASH", "Phase Dash + Overcharge", 15, "300❖", "Instant 135px phase-blink + supersonic dodge burst, 0.65s i-frames & +35% next-shot rail overcharge (2.5s CD)."),
        SecondaryAbilityOption("NATIVE_MULTISHOT", "Native Split Multi-Shot", 15, "TOGGLE", "Permanently unlocks Multi-Shot ON/OFF (100% single vs 3x 1/3 split)."),
        SecondaryAbilityOption("MINE", "Proximity Mines (+1 Repel)", 18, "300❖", "Drops armed proximity mines (max 6, 30s life) + starts with +1 Repel."),
        SecondaryAbilityOption("BOUNCE", "Bounce Bomb", 20, "350❖", "Plasma warhead reflecting off walls up to 3 times with raycast line."),
        SecondaryAbilityOption("PROX", "Proximity Flak Bomb", 22, "450❖", "65px proximity fuse warhead bursting into 8 shrapnel pellets."),
        SecondaryAbilityOption("NOVA_RING", "Native Nova Ring", 22, "500❖", "Fires 360° ricochet plasma bolt ring on Secondary Button (4s CD)."),
        SecondaryAbilityOption("SHATTER_CONE", "Native Shatter Cone", 22, "500❖", "Fires 3 rapid 60° cone plasma shard waves on Secondary Button (4s CD)."),
        SecondaryAbilityOption("SIEGE", "Siege Bomb", 25, "650❖", "Massive nuclear dreadnought warhead with heavy backward recoil."),
        SecondaryAbilityOption("CLOAK_EMP", "Tactical Cloak + 0❖ Decloak EMP", 25, "180❖/s • 0❖ EMP", "Tap to Cloak invisibly (180❖/s); decloaking via EMP PULSE or primary fire unleashes a free 0❖ 220px 360° AoE EMP (-35% Max Flux, 2.5s recharge lockout)."),
        SecondaryAbilityOption("SEEKER_TORPEDO", "Native Seeker Torpedo", 25, "600❖", "Fires homing wall-bouncing Seeker Torpedo (2.5s flight, 5s CD)."),
      )

    fun getPrimaryPointCost(archetype: String): Int =
      PRIMARY_OPTIONS.firstOrNull { it.id == archetype }?.pointCost ?: 0

    fun getSecondaryPointCost(secType: String): Int {
      val norm =
        when (secType) {
          "BURST" -> "NOVA_RING"
          "SHOTGUN" -> "SHATTER_CONE"
          "MISSILE" -> "SEEKER_TORPEDO"
          else -> secType
        }
      return SECONDARY_OPTIONS.firstOrNull { it.id == norm }?.pointCost ?: 0
    }

    fun normalizeSlotId(slotId: String): String =
      when (slotId.trim().uppercase(Locale.US)) {
        "FRAME B", "9B", "B" -> "Frame B"
        "FRAME C", "9C", "C" -> "Frame C"
        else -> "Frame A"
      }

    fun defaultSlot(slotId: String): CustomShipLoadoutSlot {
      return when (normalizeSlotId(slotId)) {
        "Frame B" ->
          CustomShipLoadoutSlot(
            slotId = "Frame B",
            customName = "Rift Frame",
            speedPts = 2,
            thrustPts = 2,
            turnPts = 2,
            maxFluxPts = 2,
            rechargePts = 2,
            primaryArchetype = "TWIN_SCATTER",
            secondaryType = "NONE",
            visualRecipe =
              CustomHullVisualRecipe(
                hullName = "FRAME B",
                silhouette = "BASTION_DREADNOUGHT",
                wingSweep = 1.15f,
                chassisPlates = 4,
                primaryHex = "#4C1D95",
                trimHex = "#FACC15",
                coreGlowHex = "#FFAB00",
                engineStyle = "HEAVY_THRUSTER",
                promptUsed = "Obsidian violet dreadnought with gold bastion armor and solar core",
              ),
          )
        "Frame C" ->
          CustomShipLoadoutSlot(
            slotId = "Frame C",
            customName = "Rift Frame",
            speedPts = 5,
            thrustPts = 0,
            turnPts = 0,
            maxFluxPts = 0,
            rechargePts = 0,
            primaryArchetype = "LIGHT_PULSE",
            secondaryType = "PHASE_DASH",
            visualRecipe =
              CustomHullVisualRecipe(
                hullName = "FRAME C",
                silhouette = "MANTA_STINGRAY",
                wingSweep = 1.25f,
                chassisPlates = 2,
                primaryHex = "#065F46",
                trimHex = "#34D399",
                coreGlowHex = "#00E676",
                engineStyle = "BIO_ARC",
                promptUsed = "Bio-tech emerald stingray manta with phase dash capacitor",
              ),
          )
        else ->
          CustomShipLoadoutSlot(
            slotId = "Frame A",
            customName = "Rift Frame",
            speedPts = 4,
            thrustPts = 4,
            turnPts = 4,
            maxFluxPts = 4,
            rechargePts = 4,
            primaryArchetype = "LIGHT_PULSE",
            secondaryType = "NONE",
            visualRecipe = CustomHullVisualRecipe(),
          )
      }
    }

    fun fromJsonString(raw: String?, fallbackSlotId: String = "Frame A"): CustomShipLoadoutSlot {
      val cleanFallback = normalizeSlotId(fallbackSlotId)
      if (raw.isNullOrBlank()) return defaultSlot(cleanFallback)
      return try {
        val obj = JSONObject(raw)
        val recipeObj = obj.optJSONObject("visualRecipe")
        val rawPri = obj.optString("primaryArchetype", "LIGHT_PULSE")
        val validPri = if (PRIMARY_OPTIONS.any { it.id == rawPri }) rawPri else "LIGHT_PULSE"
        val rawSec =
          when (obj.optString("secondaryType", "NONE")) {
            "BURST" -> "NOVA_RING"
            "SHOTGUN" -> "SHATTER_CONE"
            "MISSILE" -> "SEEKER_TORPEDO"
            else -> obj.optString("secondaryType", "NONE")
          }
        val validSec = if (SECONDARY_OPTIONS.any { it.id == rawSec }) rawSec else "NONE"
        val cleanSlot = normalizeSlotId(obj.optString("slotId", cleanFallback))
        val rawCustomName = obj.optString("customName", "Rift Frame")
        val cleanName =
          rawCustomName
            .replace(Regex("(?i)^RIFT FRAME\\s*9[ABC]$"), "Rift Frame")
            .replace("9A", "Frame A")
            .replace("9B", "Frame B")
            .replace("9C", "Frame C")
            .take(18)
            .ifBlank { "Rift Frame" }
        CustomShipLoadoutSlot(
          slotId = cleanSlot,
          customName = cleanName,
          speedPts = obj.optInt("speedPts", 4).coerceIn(0, 30),
          thrustPts = obj.optInt("thrustPts", 4).coerceIn(0, 30),
          turnPts = obj.optInt("turnPts", 4).coerceIn(0, 30),
          maxFluxPts = obj.optInt("maxFluxPts", 4).coerceIn(0, 30),
          rechargePts = obj.optInt("rechargePts", 4).coerceIn(0, 30),
          primaryArchetype = validPri,
          secondaryType = validSec,
          visualRecipe =
            if (recipeObj != null) {
              CustomHullVisualRecipe.fromJsonString(recipeObj.toString())
            } else {
              CustomHullVisualRecipe()
            },
          aiGenerationsCount = obj.optInt("aiGenerationsCount", 0).coerceAtLeast(0),
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
  // Universal Baseline Hull Floor (0 Points Invested) & Per-Point Linear Scaling
  const val BASE_TOP_SPEED = 380f
  const val SPEED_PER_PT = 8f // Max 30 pts = 620 PX/S

  const val BASE_THRUST = 600f
  const val THRUST_PER_PT = 15f // Max 30 pts = 1,050 N/S^2

  const val BASE_TURN_RATE = 3.2f
  const val TURN_PER_PT = 0.10f // Max 30 pts = 6.2 RAD/S

  const val BASE_MAX_FLUX = 2200f
  const val MAX_FLUX_PER_PT = 80f // Max 30 pts = 4,600 Flux

  const val BASE_RECHARGE = 240f
  const val RECHARGE_PER_PT = 8f // Max 30 pts = 480 Flux/S

  fun computeTopSpeed(pts: Int): Float = BASE_TOP_SPEED + pts.coerceIn(0, 30) * SPEED_PER_PT
  fun computeThrust(pts: Int): Float = BASE_THRUST + pts.coerceIn(0, 30) * THRUST_PER_PT
  fun computeTurnRate(pts: Int): Float = BASE_TURN_RATE + pts.coerceIn(0, 30) * TURN_PER_PT
  fun computeMaxFlux(pts: Int): Float = BASE_MAX_FLUX + pts.coerceIn(0, 30) * MAX_FLUX_PER_PT
  fun computeRecharge(pts: Int): Float = BASE_RECHARGE + pts.coerceIn(0, 30) * RECHARGE_PER_PT

  private fun createDefaultBuffRegistry(): LinkedHashMap<String, SpecialBuffRegistryEntry> =
    linkedMapOf(
      "NOVA_RING" to
        SpecialBuffRegistryEntry(
          id = "NOVA_RING",
          buffTypeIndex = 0,
          officialName = "NOVA RING",
          shortLabel = "NOVA",
          chargesPerPickup = 1,
          aimMode = BuffAimMode.TAP_INSTANT,
          strokeColorHex = "#F59E0B",
          fillColorArgb = Color.argb(155, 80, 40, 6),
          summary = "360° radial ring of high-velocity ricochet plasma bolts.",
          projectileSpeed = 860f,
          flightDuration = 1.05f,
          damage = 280f,
          blastRadius = 360f,
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
          fillColorArgb = Color.argb(155, 88, 18, 38),
          summary = "Fires 3 rapid waves of bouncing plasma shards in a 60° cone.",
          projectileSpeed = 980f,
          flightDuration = 0.72f,
          damage = 55f,
          blastRadius = 60f,
        ),
      "SEEKER_TORPEDO" to
        SpecialBuffRegistryEntry(
          id = "SEEKER_TORPEDO",
          buffTypeIndex = 2,
          officialName = "SEEKER TORPEDO",
          shortLabel = "SEEKER",
          chargesPerPickup = 1,
          aimMode = BuffAimMode.TAP_INSTANT,
          strokeColorHex = "#EF4444",
          fillColorArgb = Color.argb(155, 88, 14, 18),
          summary = "Locks onto hostile targets, bounces off walls, and flies for a full 2.5 seconds before detonating.",
          projectileSpeed = 380f,
          flightDuration = 2.5f,
          damage = 780f,
          blastRadius = 155f,
        ),
      "HOLO_PHANTOM" to
        SpecialBuffRegistryEntry(
          id = "HOLO_PHANTOM",
          buffTypeIndex = 3,
          officialName = "HOLO PHANTOM",
          shortLabel = "PHANTOM",
          chargesPerPickup = 1,
          aimMode = BuffAimMode.TAP_INSTANT,
          strokeColorHex = "#38BDF8",
          fillColorArgb = Color.argb(155, 12, 52, 78),
          summary = "Spawns a holographic clone with your [TAG] Callsign and Bounty that mirrors velocity and breaks enemy Seeker locks.",
          flightDuration = 8.0f,
          damage = 0f,
          specialEffect = CustomBuffEffect.CLONE,
        ),
      "STASIS_BARRIER" to
        SpecialBuffRegistryEntry(
          id = "STASIS_BARRIER",
          buffTypeIndex = 4,
          officialName = "STASIS BARRIER",
          shortLabel = "BARRIER",
          chargesPerPickup = 1,
          aimMode = BuffAimMode.TAP_INSTANT,
          strokeColorHex = "#22D3EE",
          fillColorArgb = Color.argb(155, 10, 62, 72),
          summary = "Deploys a crackling cyan/amber hard-light energy barrier perpendicular to the ship that blocks ships and bounces projectiles for 10s.",
          flightDuration = 10.0f,
          damage = 0f,
          blastRadius = 120f,
          specialEffect = CustomBuffEffect.BARRIER,
        ),
      "HYPER_SURGE" to
        SpecialBuffRegistryEntry(
          id = "HYPER_SURGE",
          buffTypeIndex = 5,
          officialName = "HYPER SURGE",
          shortLabel = "SURGE",
          chargesPerPickup = 1,
          aimMode = BuffAimMode.TAP_INSTANT,
          strokeColorHex = "#A855F7",
          fillColorArgb = Color.argb(155, 58, 18, 88),
          summary = "3.0-second 1.65x hyper-thruster speed burn costing 0❖ Flux.",
          flightDuration = 3.0f,
          damage = 0f,
          specialEffect = CustomBuffEffect.SPEED_BOOST,
        ),
    )

  /**
   * Dynamic, Extensible Special Buff Registry (`BUFF_REGISTRY`) for the 4th Right-Hand Combat Button.
   * Supports both the 6 core Bucket A buffs (0..5) and live AI-Forged custom buffs (11+).
   */
  val BUFF_REGISTRY: MutableMap<String, SpecialBuffRegistryEntry> = createDefaultBuffRegistry()

  fun nextCustomBuffTypeIndex(): Int {
    val maxExisting = BUFF_REGISTRY.values.maxOfOrNull { it.buffTypeIndex } ?: 10
    return kotlin.math.max(11, maxExisting + 1)
  }

  fun registerCustomBuff(entry: SpecialBuffRegistryEntry) {
    BUFF_REGISTRY[entry.id.uppercase(Locale.US)] = entry
    RiftSpriteBaker.invalidateCustomBuffIcon(entry.buffTypeIndex)
  }

  fun removeCustomBuff(id: String) {
    val key = id.uppercase(Locale.US)
    val existing = BUFF_REGISTRY[key]
    if (existing != null && existing.isCustomForged) {
      BUFF_REGISTRY.remove(key)
      RiftSpriteBaker.invalidateCustomBuffIcon(existing.buffTypeIndex)
    }
  }

  fun getCustomForgedBuffs(): List<SpecialBuffRegistryEntry> =
    BUFF_REGISTRY.values.filter { it.isCustomForged }

  fun resetBuffRegistryToDefaults(preserveCustomForged: Boolean = false) {
    val customs = if (preserveCustomForged) getCustomForgedBuffs() else emptyList()
    BUFF_REGISTRY.clear()
    BUFF_REGISTRY.putAll(createDefaultBuffRegistry())
    customs.forEach { BUFF_REGISTRY[it.id] = it }
  }

  fun normalizeSpecialBuffId(rawType: String): String =
    when (rawType.uppercase(Locale.US)) {
      "BURST", "NOVA_RING", "NOVA" -> "NOVA_RING"
      "SHOTGUN", "SHATTER_CONE", "SHATTER" -> "SHATTER_CONE"
      "MISSILE", "SEEKER_TORPEDO", "SEEKER" -> "SEEKER_TORPEDO"
      "DECOY", "HOLO_PHANTOM", "PHANTOM" -> "HOLO_PHANTOM"
      "BRICK", "STASIS_BARRIER", "BARRIER" -> "STASIS_BARRIER"
      "ROCKET", "HYPER_SURGE", "SURGE" -> "HYPER_SURGE"
      else -> rawType.uppercase(Locale.US)
    }

  fun getBuffRegistryEntry(rawType: String): SpecialBuffRegistryEntry? =
    BUFF_REGISTRY[normalizeSpecialBuffId(rawType)]

  fun getBuffRegistryByIndex(buffTypeIndex: Int): SpecialBuffRegistryEntry? =
    BUFF_REGISTRY.values.firstOrNull { it.buffTypeIndex == buffTypeIndex }

  var activeCustomShipSlot: CustomShipLoadoutSlot = CustomShipLoadoutSlot.defaultSlot("Frame A")
  var customShip9Spec: ShipSpec = activeCustomShipSlot.toShipSpec()

  fun updateActiveCustomShipSlot(slot: CustomShipLoadoutSlot) {
    activeCustomShipSlot = slot
    customShip9Spec = slot.toShipSpec()
    RiftSpriteBaker.updateCustomShip9Sprites(slot.visualRecipe)
  }

  val SHIPS: List<ShipSpec> =
    listOf(
      // 1-Viper (Rail Sniper Gunship — 100 Pts Total: 15 Pri + 15 Class + 70 Stats)
      ShipSpec(
        id = 1,
        name = "Viper",
        role = "Rail Sniper Gunship",
        tacticalLore = "High-velocity interceptor built around a twin hyper-rail spine and directional Phase Dash capacitor.",
        silhouetteDesc = "Forward-swept obsidian rail-prongs with bronze wing-tips and a narrow V-cyan reactor spine.",
        weaponSummary = "PRI: RAIL (25❖) • CLASS: PHASE (300❖ / 2.5s CD)",
        primaryTitle = "Hyper-Velocity Dual Rail",
        secondaryTitle = "Phase Dash / Sniper Overcharge (300❖)",
        primaryShortLabel = "RAIL",
        secondaryShortLabel = "PHASE",
        speedPts = 30,
        turnPts = 20,
        rechargePts = 15,
        thrustPts = 5,
        maxFluxPts = 0,
        primaryPts = 15,
        secondaryPts = 15,
        maxFlux = computeMaxFlux(0), // 2,200❖
        rechargeRate = computeRecharge(15), // 360❖/S
        topSpeed = computeTopSpeed(30), // 620 PX/S
        afterburnerSpeed = computeTopSpeed(30) * 1.30f,
        thrust = computeThrust(5), // 675 N/S²
        turnRate = computeTurnRate(20), // 5.2 RAD/S
        primaryCost = 25f,
        primaryCooldown = 0.15f,
        primaryDamage = 285f,
        primarySpeed = 1120f,
        primaryStreams = 1,
        primarySpreadRad = 0f,
        primaryBounces = 0,
        hasBomb = true,
        bombType = "PHASE_DASH",
        bombCost = 300f,
        bombCooldown = 2.5f,
        bombDamage = 0f,
        bombBounces = 0,
        bombRecoil = 0f,
        supportsNativeMultifire = false,
        initialRepels = 2,
        accentColorHex = 0xFF00F5D4.toInt(),
      ),
      // 2-Ricochet (Trick-Shot Bounce Bomber — 100 Pts Total: 12 Pri + 20 Sec + 68 Stats)
      ShipSpec(
        id = 2,
        name = "Ricochet",
        role = "Trick-Shot Bounce Bomber",
        tacticalLore = "Geometry specialist engineered to bank angled plasma warheads around riveted steel corridors.",
        silhouetteDesc = "Twin-forked ballistic hull with gold-trimmed deflection vanes and a deep cyan V-core.",
        weaponSummary = "PRI: PULSE (20❖) • SEC: BOUNCE BOMB (350❖ + Recoil)",
        primaryTitle = "Twin Angled Pulse",
        secondaryTitle = "Bounce Bomb (350❖ • 3 Bounces)",
        primaryShortLabel = "PULSE",
        secondaryShortLabel = "BOUNCE BOMB",
        speedPts = 18,
        turnPts = 18,
        thrustPts = 14,
        maxFluxPts = 8,
        rechargePts = 10,
        primaryPts = 12,
        secondaryPts = 20,
        maxFlux = computeMaxFlux(8), // 2,840❖
        rechargeRate = computeRecharge(10), // 320❖/S
        topSpeed = computeTopSpeed(18), // 524 PX/S
        afterburnerSpeed = computeTopSpeed(18) * 1.32f,
        thrust = computeThrust(14), // 810 N/S²
        turnRate = computeTurnRate(18), // 5.0 RAD/S
        primaryCost = 20f,
        primaryCooldown = 0.16f,
        primaryDamage = 195f,
        primarySpeed = 930f,
        primaryStreams = 2,
        primarySpreadRad = 0.045f,
        primaryBounces = 1,
        hasBomb = true,
        bombType = "BOUNCE",
        bombCost = 350f,
        bombCooldown = 0.85f,
        bombDamage = 780f,
        bombBounces = 3,
        bombRecoil = 185f,
        supportsNativeMultifire = false,
        initialRepels = 2,
        accentColorHex = 0xFF29B6F6.toInt(),
      ),
      // 3-Suppressor (Adaptive Rotary Gunship — 100 Pts Total: 15 Pri + 15 Class + 70 Stats)
      ShipSpec(
        id = 3,
        name = "Suppressor",
        role = "Adaptive Rotary Gunship",
        tacticalLore = "High-RPM rotary gunship with native 1x focused punch or 3x 1/3-damage split corridor suppression.",
        silhouetteDesc = "Swept crescent wings in dark indigo & gold with an overcharged V-cyan heart.",
        weaponSummary = "PRI: ROTARY (15❖) • CLASS: SPLIT MULTI ON/OFF (0❖ Extra)",
        primaryTitle = "High-RPM Rotary Pulse",
        secondaryTitle = "Native 3x / 1/3x Split Multi-Shot",
        primaryShortLabel = "ROTARY",
        secondaryShortLabel = "SPLIT MULTI",
        rechargePts = 30,
        maxFluxPts = 15,
        speedPts = 10,
        turnPts = 10,
        thrustPts = 5,
        primaryPts = 15,
        secondaryPts = 15,
        maxFlux = computeMaxFlux(15), // 3,400❖
        rechargeRate = computeRecharge(30), // 480❖/S
        topSpeed = computeTopSpeed(10), // 460 PX/S
        afterburnerSpeed = computeTopSpeed(10) * 1.32f,
        thrust = computeThrust(5), // 675 N/S²
        turnRate = computeTurnRate(10), // 4.2 RAD/S
        primaryCost = 15f,
        primaryCooldown = 0.095f,
        primaryDamage = 240f, // 100% single stream when Multi-Shot OFF; 3 x 80f (33.3%) when Multi-Shot ON
        primarySpeed = 980f,
        primaryStreams = 1,
        primarySpreadRad = 0f,
        primaryBounces = 0,
        hasBomb = false,
        bombType = "NONE",
        bombCost = 0f,
        bombCooldown = 0f,
        bombDamage = 0f,
        bombBounces = 0,
        bombRecoil = 0f,
        supportsNativeMultifire = true,
        initialRepels = 2,
        accentColorHex = 0xFFFFD54F.toInt(),
      ),
      // 4-Goliath (Cathedral Siege Dreadnought — 100 Pts Total: 12 Pri + 25 Sec + 63 Stats)
      ShipSpec(
        id = 4,
        name = "Goliath",
        role = "Cathedral Siege Dreadnought",
        tacticalLore = "Maximum-capacitor siege dreadnought armed with devastating nuclear warheads and heavy kinetic cannons.",
        silhouetteDesc = "Heavy armored dreadnought chassis with bronze bastion shoulders and a massive V-reactor.",
        weaponSummary = "PRI: CANNON (40❖) • SEC: SIEGE BOMB (650❖ + Recoil)",
        primaryTitle = "Heavy Dual Cannon",
        secondaryTitle = "Siege Bomb (650❖ • Heavy Recoil)",
        primaryShortLabel = "CANNON",
        secondaryShortLabel = "SIEGE BOMB",
        maxFluxPts = 30,
        rechargePts = 20,
        thrustPts = 8,
        turnPts = 5,
        speedPts = 0,
        primaryPts = 12,
        secondaryPts = 25,
        maxFlux = computeMaxFlux(30), // 4,600❖
        rechargeRate = computeRecharge(20), // 400❖/S
        topSpeed = computeTopSpeed(0), // 380 PX/S
        afterburnerSpeed = computeTopSpeed(0) * 1.35f,
        thrust = computeThrust(8), // 720 N/S²
        turnRate = computeTurnRate(5), // 3.7 RAD/S
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
      // 5-Twinfang (Dual-Barrel Dogfighter — 100 Pts Total: 15 Pri + 15 Class + 70 Stats)
      ShipSpec(
        id = 5,
        name = "Twinfang",
        role = "Dual-Barrel Dogfighter",
        tacticalLore = "Agile mid-range dogfighter firing parallel twin autocannon streams with native 4-bolt fan split.",
        silhouetteDesc = "Predatory twin-boom manta frame with bronze fangs and a split electric-cyan core.",
        weaponSummary = "PRI: AUTOCANNON (22❖) • CLASS: TWIN MULTI ON/OFF",
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
        maxFlux = computeMaxFlux(8), // 2,840❖
        rechargeRate = computeRecharge(7), // 296❖/S
        topSpeed = computeTopSpeed(22), // 556 PX/S
        afterburnerSpeed = computeTopSpeed(22) * 1.32f,
        thrust = computeThrust(18), // 870 N/S²
        turnRate = computeTurnRate(15), // 4.7 RAD/S
        primaryCost = 22f,
        primaryCooldown = 0.13f,
        primaryDamage = 135f, // 2 parallel bolts x 135f = 270f total; when Multi-Shot ON: 4 bolts x 67.5f = 270f total
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
      // Disruptor (Stealth & EMP Infiltrator — 100 Pts Total: 10 Pri + 25 Class + 65 Stats)
      ShipSpec(
        id = 6,
        name = "Disruptor",
        role = "Stealth & EMP Infiltrator",
        tacticalLore = "Cloak-capable infiltrator that drains Flux while invisible (180❖/s) and detonates a free 0❖ 220px 360° AoE EMP pulse upon decloaking (-35% Max Flux, 2.5s recharge lockout).",
        silhouetteDesc = "Wide stingray hydrofoil with gold-plated arc emitters and a pulsing V-cyan capacitor.",
        weaponSummary = "PRI: NEEDLER (18❖) • CLS: Cloak (180❖/s) + 0❖ Decloak AoE EMP",
        primaryTitle = "Twin Ion Needlers",
        secondaryTitle = "Tactical Cloak (180❖/s) + 0❖ Decloak EMP",
        primaryShortLabel = "NEEDLER",
        secondaryShortLabel = "CLOAK",
        turnPts = 22,
        speedPts = 20,
        rechargePts = 13,
        thrustPts = 5,
        maxFluxPts = 5,
        primaryPts = 10,
        secondaryPts = 25,
        maxFlux = computeMaxFlux(5), // 2,600❖
        rechargeRate = computeRecharge(13), // 344❖/S
        topSpeed = computeTopSpeed(20), // 540 PX/S
        afterburnerSpeed = computeTopSpeed(20) * 1.32f,
        thrust = computeThrust(5), // 675 N/S²
        turnRate = computeTurnRate(22), // 5.4 RAD/S
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
      // 7-Valkyrie (Proximity Flak Gunship — 100 Pts Total: 12 Pri + 22 Sec + 66 Stats)
      ShipSpec(
        id = 7,
        name = "Valkyrie",
        role = "Proximity Flak Gunship",
        tacticalLore = "Heavy X-frame gunship firing 4 converging bolts and 65px proximity-fused shrapnel warheads.",
        silhouetteDesc = "Aggressive X-wing skeletal interceptor with bronze thruster pods and V-cyan reactor.",
        weaponSummary = "PRI: QUAD PULSE (24❖) • SEC: PROX FLAK (450❖ • 8 Shrapnel)",
        primaryTitle = "Quad-Converging Pulse",
        secondaryTitle = "Proximity Flak Bomb (450❖)",
        primaryShortLabel = "QUAD PULSE",
        secondaryShortLabel = "PROX FLAK",
        maxFluxPts = 20,
        rechargePts = 16,
        thrustPts = 12,
        speedPts = 10,
        turnPts = 8,
        primaryPts = 12,
        secondaryPts = 22,
        maxFlux = computeMaxFlux(20), // 3,800❖
        rechargeRate = computeRecharge(16), // 368❖/S
        topSpeed = computeTopSpeed(10), // 460 PX/S
        afterburnerSpeed = computeTopSpeed(10) * 1.32f,
        thrust = computeThrust(12), // 780 N/S²
        turnRate = computeTurnRate(8), // 4.0 RAD/S
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
      // 8-Manta (Area-Denial Mine-Layer — 100 Pts Total: 10 Pri + 18 Sec + 72 Stats)
      ShipSpec(
        id = 8,
        name = "Manta",
        role = "Area-Denial Mine-Layer",
        tacticalLore = "High-thrust area-denial specialist deploying up to 6 armed proximity mines and +1 innate Repel charge.",
        silhouetteDesc = "Armored pincer hull with gold-beveled mandible plates and a twin-cell V-cyan core.",
        weaponSummary = "PRI: SCATTER (20❖) • SEC: PROX MINE (300❖ • X/6) +1 Repel",
        primaryTitle = "Short-Range Twin Scatter",
        secondaryTitle = "Proximity Mines (300❖ • Max 6) +1 Repel",
        primaryShortLabel = "SCATTER",
        secondaryShortLabel = "PROX MINE",
        thrustPts = 30,
        maxFluxPts = 16,
        rechargePts = 12,
        turnPts = 10,
        speedPts = 4,
        primaryPts = 10,
        secondaryPts = 18,
        maxFlux = computeMaxFlux(16), // 3,480❖
        rechargeRate = computeRecharge(12), // 336❖/S
        topSpeed = computeTopSpeed(4), // 412 PX/S
        afterburnerSpeed = computeTopSpeed(4) * 1.34f,
        thrust = computeThrust(30), // 1,050 N/S²
        turnRate = computeTurnRate(10), // 4.2 RAD/S
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
        initialRepels = 3, // Starts every life with +1 bonus Repel charge
        accentColorHex = 0xFF66BB6A.toInt(),
      ),
    )

  /**
   * Expanded 11-Buff Playfield Pool (89% Ship Size Glowing Icons, Capped at 40 in 20-player FFA):
   * - Bucket A (0..5): Official RIFT Special Buff Slot Pickups (4th Right-Hand Button — Charge-Based, 0❖ Cost)
   * - Bucket B (6..10): Instant Ship & Weapon Enhancers (Top-Center Expandable Buff Bar)
   */
  val POWERUP_CODEX: List<PowerUpCodexEntry> =
    listOf(
      // Bucket A: Official RIFT Special Buff Slot Pickups (0..5) — Charge-Based, 0❖ Cost
      PowerUpCodexEntry(
        id = "special_nova_ring",
        buffTypeIndex = 0,
        title = "NOVA RING (+1 CHARGE • 0❖)",
        dropRatePercent = 9,
        category = "BUCKET A: SPECIAL BUFF SLOT (0❖ COST)",
        effectSummary = "Fires a 360° radial ring of 24 high-velocity ricochet plasma bolts.",
        tacticalNote = "HUD Label: NOVA [X]. Stacks +1 charge if already holding Nova Ring, or overwrites a different Special Buff Slot item.",
      ),
      PowerUpCodexEntry(
        id = "special_shatter_cone",
        buffTypeIndex = 1,
        title = "SHATTER CONE (+2 CHARGES • 0❖)",
        dropRatePercent = 9,
        category = "BUCKET A: SPECIAL BUFF SLOT (0❖ COST)",
        effectSummary = "Fires 3 rapid waves of bouncing plasma shards in a 60° cone.",
        tacticalNote = "HUD Label: SHATTER [X]. Massive close-quarters burst damage at zero Flux cost. Same-type pickups stack +2 charges.",
      ),
      PowerUpCodexEntry(
        id = "special_seeker_torpedo",
        buffTypeIndex = 2,
        title = "SEEKER TORPEDO (+1 CHARGE • 0❖)",
        dropRatePercent = 8,
        category = "BUCKET A: SPECIAL BUFF SLOT (0❖ COST)",
        effectSummary = "Locks onto hostile targets, travels at Goliath speed (380 PX/S), bounces off walls, and flies for a full 2.5 seconds before detonating.",
        tacticalNote = "HUD Label: SEEKER [X]. Forces enemies to burn Repel, Phase Dash, Holo Phantom, or Stasis Barrier.",
      ),
      PowerUpCodexEntry(
        id = "special_holo_phantom",
        buffTypeIndex = 3,
        title = "HOLO PHANTOM (+1 CHARGE • 0❖)",
        dropRatePercent = 8,
        category = "BUCKET A: SPECIAL BUFF SLOT (0❖ COST)",
        effectSummary = "Spawns a holographic clone with your [TAG] Callsign and Bounty that mirrors velocity for 8 seconds.",
        tacticalNote = "HUD Label: PHANTOM [X]. Draws enemy AI fire and breaks active Seeker Torpedo locks.",
      ),
      PowerUpCodexEntry(
        id = "special_stasis_barrier",
        buffTypeIndex = 4,
        title = "STASIS BARRIER (+1 CHARGE • 0❖)",
        dropRatePercent = 8,
        category = "BUCKET A: SPECIAL BUFF SLOT (0❖ COST)",
        effectSummary = "Deploys a crackling cyan/amber 120px hard-light energy barrier perpendicular to the ship for 10 seconds.",
        tacticalNote = "HUD Label: BARRIER [X]. Physically blocks ships and bounces projectiles.",
      ),
      PowerUpCodexEntry(
        id = "special_hyper_surge",
        buffTypeIndex = 5,
        title = "HYPER SURGE (+1 CHARGE • 0❖)",
        dropRatePercent = 8,
        category = "BUCKET A: SPECIAL BUFF SLOT (0❖ COST)",
        effectSummary = "Ignites a 3.0-second 1.65x hyper-thruster speed burn costing 0❖ Flux with a blazing plasma trail.",
        tacticalNote = "HUD Label: SURGE [X]. Ultimate escape or high-bounty chase tool when your Flux capacitor is depleted.",
      ),
      // Bucket B: Instant Ship & Weapon Enhancers (6..10)
      PowerUpCodexEntry(
        id = "full_charge",
        buffTypeIndex = 6,
        title = "FULL FLUX CHARGE (+100% ❖)",
        dropRatePercent = 14,
        category = "BUCKET B: INSTANT ENHANCER",
        effectSummary = "Instantly restores 100% Maximum Flux (❖) and grants +15 Bounty.",
        tacticalNote = "Grab mid-dogfight to invert a low-Flux engagement without retreating to the Safe Zone.",
      ),
      PowerUpCodexEntry(
        id = "repel_charge",
        buffTypeIndex = 7,
        title = "REPEL CHARGE (+1 REPEL)",
        dropRatePercent = 10,
        category = "BUCKET B: INSTANT ENHANCER",
        effectSummary = "Adds +1 Repel to your dedicated Repel button counter.",
        tacticalNote = "Reflects enemy ships, bullets, bombs, mines, and Supply Crates in a 360° shockwave.",
      ),
      PowerUpCodexEntry(
        id = "shot_multishot",
        buffTypeIndex = 8,
        title = "MULTI-SHOT CRATE (+25 SHOTS)",
        dropRatePercent = 9,
        category = "BUCKET B: PRIMARY TRIGGER AUGMENT",
        effectSummary = "Modifies the Primary Fire Stick trigger directly for 25 shots (never overwrites your Special Buff Slot).",
        tacticalNote = "Displays a MULTI [25] badge on the Primary Fire Stick ring and automatically fires multi-stream volleys.",
      ),
      PowerUpCodexEntry(
        id = "bouncing_bullets",
        buffTypeIndex = 9,
        title = "BOUNCING BULLETS (PERMANENT LIFE UPGRADE)",
        dropRatePercent = 9,
        category = "BUCKET B: PERMANENT HULL UPGRADE",
        effectSummary = "Permanently upgrades your primary guns with +1 arena wall bounce for the rest of your current life.",
        tacticalNote = "Tracked in the compact 4-Slot Buff Bar (BOUNCE ON) until ship destruction or Convergence.",
      ),
      PowerUpCodexEntry(
        id = "overcharge_speed",
        buffTypeIndex = 10,
        title = "SPEED OVERCHARGE (+8% • MAX 3/3)",
        dropRatePercent = 3,
        category = "BUCKET B: PERMANENT HULL UPGRADE",
        effectSummary = "Permanently increases Top Cruise & Afterburner Speed by +8% per stack (up to +24% at 3/3) until death.",
        tacticalNote = "Shares the Overcharge crate slot evenly with Thrust Overcharge and Reactor Overcharge.",
      ),
      PowerUpCodexEntry(
        id = "overcharge_thrust",
        buffTypeIndex = -11,
        title = "THRUST OVERCHARGE (+8% • MAX 3/3)",
        dropRatePercent = 3,
        category = "BUCKET B: PERMANENT HULL UPGRADE",
        effectSummary = "Permanently increases Newtonian Engine Thrust by +8% per stack (up to +24% at 3/3) until death.",
        tacticalNote = "Tracked in the compact 4-Slot Buff Bar (THR +X%).",
      ),
      PowerUpCodexEntry(
        id = "overcharge_recharge",
        buffTypeIndex = -12,
        title = "REACTOR OVERCHARGE (+8% • MAX 3/3)",
        dropRatePercent = 2,
        category = "BUCKET B: PERMANENT HULL UPGRADE",
        effectSummary = "Permanently increases passive Flux Recharge Rate by +8% per stack (up to +24% at 3/3) until death.",
        tacticalNote = "Tracked in the compact 4-Slot Buff Bar (RCH +X%).",
      ),
    )

  val RIFT_ANOMALIES_CODEX: List<PlayfieldRiftCodexEntry> =
    listOf(
      PlayfieldRiftCodexEntry(
        id = "plasma_rift",
        title = "PLASMA RIFT (LETHAL THERMAL TEAR)",
        classification = "HAZARD ANOMALY • CRIMSON RADAR BLIP",
        specBadge = "10% MAX FLUX / 0.6s • 180PX RADIUS",
        accentHex = "#EF4444",
        effectSummary = "Unstable crimson-magenta dimensional tear that sears any hull crossing its 180px perimeter for 10% Maximum Flux every 0.6 seconds and can destroy ships outright if Flux drops to 0❖.",
        tacticalNote = "Use Repel, Gravity pulls, or heavy warhead knockback to force low-Flux enemies into a Plasma Rift for instant arena area denial.",
      ),
      PlayfieldRiftCodexEntry(
        id = "repulsor_rift",
        title = "REPULSOR RIFT (KINETIC SLINGSHOT DOME)",
        classification = "KINETIC FIELD • WHITE RADAR BLIP",
        specBadge = "520PX FIELD • 1.35x BURN SLINGSHOT",
        accentHex = "#38BDF8",
        effectSummary = "High-pressure radial shock dome that hurls approaching ships outward up to 1.35x Afterburner velocity across a 520px field while deflecting incoming bullets and warheads away from its core.",
        tacticalNote = "Skim the outer 520px boundary at an angle to slingshot across sectors at zero Flux cost, or camp behind its core to curve enemy rail shots off-target.",
      ),
      PlayfieldRiftCodexEntry(
        id = "gravity_rift",
        title = "GRAVITY RIFT (SINGULARITY WELL)",
        classification = "GRAVITY WELL • VIOLET RADAR BLIP",
        specBadge = "600PX PULL • CORE FULL STOP (0 PX/S)",
        accentHex = "#A855F7",
        effectSummary = "Collapsed violet singularity that drags ships inward across a 600px pull field. Touching the inner event horizon zeroes your velocity (Full Stop) with 1.75s escape immunity and swallows live projectiles.",
        tacticalNote = "Fire Seeker Torpedoes or Siege Bombs as an enemy gets dragged toward the core, or trigger Phase Dash / Hyper Surge to break free of the pull well.",
      ),
      PlayfieldRiftCodexEntry(
        id = "phase_rift_pair",
        title = "PAIRED PHASE RIFTS (NODE A ⇄ NODE B)",
        classification = "LINKED PORTAL PAIR • TEAL/CYAN BLIPS",
        specBadge = "INSTANT TRANSIT • 1.0s COOLDOWN",
        accentHex = "#2DD4BF",
        effectSummary = "Twin entangled spatial portals (Node A & Node B) spawned across distant sectors. Ships, primary bolts, warheads, and aim-guide raycasts entering either gate instantaneously exit the partner gate with full momentum preserved.",
        tacticalNote = "Thread Bounce Bombs, Siege Bombs, or aim sightlines through Node A to ambush pilots camping near Node B, or dive through to escape HVT hunters.",
      ),
    )

  val DEFAULT_SHIPS: List<ShipSpec> = SHIPS.map { it.copy() }

  fun resetShipsToDefaults() {
    for (i in SHIPS.indices) {
      val def = DEFAULT_SHIPS[i]
      val target = SHIPS[i]
      target.maxFlux = def.maxFlux
      target.rechargeRate = def.rechargeRate
      target.topSpeed = def.topSpeed
      target.afterburnerSpeed = def.afterburnerSpeed
      target.thrust = def.thrust
      target.turnRate = def.turnRate
      target.primaryCost = def.primaryCost
      target.primaryCooldown = def.primaryCooldown
      target.primaryDamage = def.primaryDamage
      target.primarySpeed = def.primarySpeed
      target.primaryBounces = def.primaryBounces
      target.bombCost = def.bombCost
      target.bombCooldown = def.bombCooldown
      target.bombDamage = def.bombDamage
      target.bombBounces = def.bombBounces
      target.bombRecoil = def.bombRecoil
    }
    customShip9Spec = activeCustomShipSlot.toShipSpec()
  }

  fun getAllNineShips(): List<ShipSpec> = SHIPS + listOf(customShip9Spec)

  fun getShipById(id: Int): ShipSpec =
    if (id == 9) customShip9Spec else (SHIPS.find { it.id == id } ?: SHIPS[0])
}
