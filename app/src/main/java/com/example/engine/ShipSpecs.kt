package com.example.engine

import android.graphics.Color
import java.util.Locale
import kotlin.math.abs
import org.json.JSONObject

data class ShipSpec(
  val id: Int,
  val name: String,
  val foundry: String,
  val syndicateTitle: String,
  val role: String,
  val silhouetteDesc: String,
  val weaponSummary: String,
  val maxFlux: Float,
  val rechargeRate: Float,
  val topSpeed: Float,
  val afterburnerSpeed: Float,
  val thrust: Float,
  val turnRate: Float,
  val primaryCost: Float,
  val primaryCooldown: Float,
  val primaryDamage: Float,
  val primarySpeed: Float,
  val primaryStreams: Int,
  val primarySpreadRad: Float,
  val primaryBounces: Int,
  val hasBomb: Boolean,
  val bombType: String, // "NONE", "BOUNCE", "SIEGE", "EMP", "PROX", "MINE", "BURST", "SHOTGUN", "MISSILE"
  val bombCost: Float,
  val bombCooldown: Float,
  val bombDamage: Float,
  val bombBounces: Int,
  val supportsMultifire: Boolean,
  val initialRepels: Int,
  val initialBursts: Int,
  val accentColorHex: Int,
)

/**
 * Compact (<2KB JSON) Visual Hull Recipe for the 9th Custom Modular Ship ("AI Hull Forge").
 * Enforces minimum luminance contrast (L >= 0.32) against #000000 space and fits inside the 36x36px physical hitbox.
 */
data class CustomHullVisualRecipe(
  val hullName: String = "APEX CHIMERA",
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
          hullName = obj.optString("hullName", "APEX CHIMERA").take(22),
          silhouette = obj.optString("silhouette", "RAZOR_INTERCEPTOR"),
          wingSweep = obj.optDouble("wingSweep", 1.05).toFloat().coerceIn(0.65f, 1.35f),
          chassisPlates = obj.optInt("chassisPlates", 3).coerceIn(1, 4),
          primaryHex = ensureMinLuminanceHex(obj.optString("primaryHex", "#1E3A8A"), 0.32f),
          trimHex = ensureMinLuminanceHex(obj.optString("trimHex", "#FACC15"), 0.45f),
          coreGlowHex = ensureMinLuminanceHex(obj.optString("coreGlowHex", "#00F5D4"), 0.55f),
          engineStyle = obj.optString("engineStyle", "TWIN_ION"),
          promptUsed = obj.optString("promptUsed", "Custom Modular Chassis").take(120),
        )
      } catch (_: Exception) {
        CustomHullVisualRecipe()
      }
    }

    /**
     * Deterministic NLP / keyword-to-shader recipe synthesizer that translates any natural language
     * hull prompt into a rich, distinct 3D metallic ship design (<2KB JSON) with guaranteed contrast.
     */
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
          "CHIMERA MK-IX"
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

/**
 * Represents one of the 3 saveable loadout slots (Slot 9A, Slot 9B, Slot 9C) for the 9th Custom Modular Ship.
 * Enforces the ~70% Baseline Hull Floor and 100-Point Competitive Cap (0..25 pts per attribute, max 100 total).
 */
data class CustomShipLoadoutSlot(
  val slotId: String = "9A", // "9A", "9B", "9C"
  val customName: String = "CHIMERA-9A",
  val speedPts: Int = 4,
  val thrustPts: Int = 4,
  val turnPts: Int = 4,
  val maxFluxPts: Int = 4,
  val rechargePts: Int = 4,
  val primaryPts: Int = 0,
  val secondaryPts: Int = 0,
  val primaryArchetype: String = "TWIN_PULSE", // "SINGLE_RAIL", "TWIN_PULSE", "TRI_SPREAD"
  val secondaryType: String = "BOUNCE", // "BOUNCE", "SIEGE", "EMP", "PROX", "MINE", "BURST", "SHOTGUN", "MISSILE", "NONE"
  val visualRecipe: CustomHullVisualRecipe = CustomHullVisualRecipe(),
  val aiGenerationsCount: Int = 0,
) {
  fun totalAllocatedPoints(): Int =
    speedPts + thrustPts + turnPts + maxFluxPts + rechargePts + primaryPts + secondaryPts

  /**
   * Computes the mathematically balanced ShipSpec for Ship #9:
   * - Starts at ~70% Baseline Hull Floor so it is immediately playable at Level 20.
   * - Scales up to 100 Points at Level 100 (or Calibration Full Override), strictly capped to Ships 1–8 ceilings.
   */
  fun toShipSpec(): ShipSpec {
    val sPts = speedPts.coerceIn(0, 25)
    val thPts = thrustPts.coerceIn(0, 25)
    val tuPts = turnPts.coerceIn(0, 25)
    val flPts = maxFluxPts.coerceIn(0, 25)
    val rePts = rechargePts.coerceIn(0, 25)
    val prPts = primaryPts.coerceIn(0, 25)
    val secPts = secondaryPts.coerceIn(0, 25)

    // 1. Hull Kinematics & Capacitor (70% floor -> best-in-class ceiling at 25/25)
    val computedTopSpeed = (365f + sPts * 7.0f).coerceAtMost(540f)
    val computedBurnSpeed = (computedTopSpeed * 1.34f).coerceAtMost(720f)
    val computedThrust = (610f + thPts * 12.4f).coerceAtMost(920f)
    val computedTurnRate = (3.5f + tuPts * 0.076f).coerceAtMost(5.4f)
    val computedMaxFlux = (2050f + flPts * 94.0f).coerceAtMost(4400f)
    val computedRecharge = (265f + rePts * 5.8f).coerceAtMost(410f)

    // 2. Primary Weapon Archetype + Point Scaling (prPts 0..25 boosts damage/cooldown efficiency up to +22%)
    val priMult = 1.0f + (prPts / 25f) * 0.22f
    val priCostReduction = 1.0f - (prPts / 25f) * 0.12f
    val (pCost, pCd, pDmg, pSpd, pStreams, pSpread) =
      when (primaryArchetype) {
        "SINGLE_RAIL" ->
           Sextuple(
            62f * priCostReduction,
            0.14f,
            225f * priMult,
            1020f,
            1,
            0f,
          )
        "TRI_SPREAD" ->
          Sextuple(
            68f * priCostReduction,
            0.14f,
            150f * priMult,
            950f,
            3,
            0.11f,
          )
        else -> // "TWIN_PULSE"
          Sextuple(
            58f * priCostReduction,
            0.15f,
            188f * priMult,
            935f,
            2,
            0.045f,
          )
      }

    // 3. Built-In Secondary Weapon Selector (Any native Bomb/Mine OR any Special Buff Attack costing Flux)
    val secMult = 1.0f + (secPts / 25f) * 0.22f
    val secCostMult = 1.0f - (secPts / 25f) * 0.15f
    val hasSecondary = secondaryType != "NONE"
    val (bCost, bCd, bDmg, bBounces) =
      when (secondaryType) {
        "BOUNCE" -> Quadruple(320f * secCostMult, 0.85f, 680f * secMult, 3)
        "SIEGE" -> Quadruple(520f * secCostMult, 1.15f, 1080f * secMult, 1)
        "EMP" -> Quadruple(380f * secCostMult, 0.95f, 590f * secMult, 2)
        "PROX" -> Quadruple(360f * secCostMult, 0.88f, 760f * secMult, 1)
        "MINE" -> Quadruple(340f * secCostMult, 0.80f, 890f * secMult, 0)
        "BURST" -> Quadruple(360f * secCostMult, 1.05f, 240f * secMult, 2)
        "SHOTGUN" -> Quadruple(330f * secCostMult, 0.92f, 52f * secMult, 2)
        "MISSILE" -> Quadruple(400f * secCostMult, 1.10f, 680f * secMult, 6)
        else -> Quadruple(0f, 0f, 0f, 0)
      }

    val accentInt =
      try {
        Color.parseColor(visualRecipe.coreGlowHex)
      } catch (_: Exception) {
        0xFF00F5D4.toInt()
      }

    val priLabel =
      when (primaryArchetype) {
        "SINGLE_RAIL" -> "Hyper Rail"
        "TRI_SPREAD" -> "Tri-Spread"
        else -> "Twin Pulse"
      }
    val secLabel =
      if (hasSecondary) "$secondaryType (${bCost.toInt()}⚡)" else "NONE (Pure Gunship)"

    return ShipSpec(
      id = 9,
      name = customName.ifBlank { "Chimera-$slotId" },
      foundry = "Modular",
      syndicateTitle = "Modular AI Hull Forge ($slotId • ${totalAllocatedPoints()} Pts)",
      role = "Custom Modular Chassis (${visualRecipe.silhouette.replace('_', ' ')})",
      silhouetteDesc = visualRecipe.promptUsed,
      weaponSummary = "PRIMARY: $priLabel + SECONDARY: $secLabel",
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
      primaryBounces = if (primaryArchetype == "TWIN_PULSE" && prPts >= 15) 1 else 0,
      hasBomb = hasSecondary,
      bombType = secondaryType,
      bombCost = bCost,
      bombCooldown = bCd,
      bombDamage = bDmg,
      bombBounces = bBounces,
      supportsMultifire = true,
      initialRepels = 2,
      initialBursts = 1,
      accentColorHex = accentInt,
    )
  }

  fun toJsonString(): String {
    return JSONObject().apply {
      put("slotId", slotId)
      put("customName", customName.take(18))
      put("speedPts", speedPts)
      put("thrustPts", thrustPts)
      put("turnPts", turnPts)
      put("maxFluxPts", maxFluxPts)
      put("rechargePts", rechargePts)
      put("primaryPts", primaryPts)
      put("secondaryPts", secondaryPts)
      put("primaryArchetype", primaryArchetype)
      put("secondaryType", secondaryType)
      put("visualRecipe", JSONObject(visualRecipe.toJsonString()))
      put("aiGenerationsCount", aiGenerationsCount)
    }.toString()
  }

  companion object {
    fun defaultSlot(slotId: String): CustomShipLoadoutSlot {
      return when (slotId) {
        "9B" ->
          CustomShipLoadoutSlot(
            slotId = "9B",
            customName = "BASTION-9B",
            speedPts = 2,
            thrustPts = 2,
            turnPts = 2,
            maxFluxPts = 6,
            rechargePts = 4,
            primaryPts = 2,
            secondaryPts = 2,
            primaryArchetype = "TRI_SPREAD",
            secondaryType = "SIEGE",
            visualRecipe =
              CustomHullVisualRecipe(
                hullName = "BASTION-9B",
                silhouette = "BASTION_DREADNOUGHT",
                wingSweep = 1.15f,
                chassisPlates = 4,
                primaryHex = "#4C1D95",
                trimHex = "#FACC15",
                coreGlowHex = "#FFAB00",
                engineStyle = "HEAVY_THRUSTER",
                promptUsed = "Imperial violet dreadnought with gold bastion armor and solar core",
              ),
          )
        "9C" ->
          CustomShipLoadoutSlot(
            slotId = "9C",
            customName = "PHANTOM-9C",
            speedPts = 5,
            thrustPts = 5,
            turnPts = 4,
            maxFluxPts = 2,
            rechargePts = 2,
            primaryPts = 1,
            secondaryPts = 1,
            primaryArchetype = "SINGLE_RAIL",
            secondaryType = "MISSILE",
            visualRecipe =
              CustomHullVisualRecipe(
                hullName = "PHANTOM-9C",
                silhouette = "MANTA_STINGRAY",
                wingSweep = 1.25f,
                chassisPlates = 2,
                primaryHex = "#065F46",
                trimHex = "#34D399",
                coreGlowHex = "#00E676",
                engineStyle = "BIO_ARC",
                promptUsed = "Bio-tech emerald stingray manta with plasma lock-on missiles",
              ),
          )
        else ->
          CustomShipLoadoutSlot(
            slotId = "9A",
            customName = "CHIMERA-9A",
            speedPts = 4,
            thrustPts = 4,
            turnPts = 4,
            maxFluxPts = 4,
            rechargePts = 4,
            primaryPts = 0,
            secondaryPts = 0,
            primaryArchetype = "TWIN_PULSE",
            secondaryType = "BOUNCE",
            visualRecipe = CustomHullVisualRecipe(),
          )
      }
    }

    fun fromJsonString(raw: String?, fallbackSlotId: String = "9A"): CustomShipLoadoutSlot {
      if (raw.isNullOrBlank()) return defaultSlot(fallbackSlotId)
      return try {
        val obj = JSONObject(raw)
        val recipeObj = obj.optJSONObject("visualRecipe")
        CustomShipLoadoutSlot(
          slotId = obj.optString("slotId", fallbackSlotId),
          customName = obj.optString("customName", "CHIMERA-$fallbackSlotId").take(18),
          speedPts = obj.optInt("speedPts", 4).coerceIn(0, 25),
          thrustPts = obj.optInt("thrustPts", 4).coerceIn(0, 25),
          turnPts = obj.optInt("turnPts", 4).coerceIn(0, 25),
          maxFluxPts = obj.optInt("maxFluxPts", 4).coerceIn(0, 25),
          rechargePts = obj.optInt("rechargePts", 4).coerceIn(0, 25),
          primaryPts = obj.optInt("primaryPts", 0).coerceIn(0, 25),
          secondaryPts = obj.optInt("secondaryPts", 0).coerceIn(0, 25),
          primaryArchetype = obj.optString("primaryArchetype", "TWIN_PULSE"),
          secondaryType = obj.optString("secondaryType", "BOUNCE"),
          visualRecipe =
            if (recipeObj != null) {
              CustomHullVisualRecipe.fromJsonString(recipeObj.toString())
            } else {
              CustomHullVisualRecipe()
            },
          aiGenerationsCount = obj.optInt("aiGenerationsCount", 0).coerceAtLeast(0),
        )
      } catch (_: Exception) {
        defaultSlot(fallbackSlotId)
      }
    }
  }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
private data class Sextuple<A, B, C, D, E, F>(
  val first: A,
  val second: B,
  val third: C,
  val fourth: D,
  val fifth: E,
  val sixth: F,
)

data class PowerUpCodexEntry(
  val id: String,
  val title: String,
  val dropRatePercent: Int,
  val category: String,
  val effectSummary: String,
  val tacticalNote: String,
)

object RiftCodex {
  var activeCustomShipSlot: CustomShipLoadoutSlot = CustomShipLoadoutSlot.defaultSlot("9A")
  var customShip9Spec: ShipSpec = activeCustomShipSlot.toShipSpec()

  fun updateActiveCustomShipSlot(slot: CustomShipLoadoutSlot) {
    activeCustomShipSlot = slot
    customShip9Spec = slot.toShipSpec()
    RiftSpriteBaker.updateCustomShip9Sprites(slot.visualRecipe)
  }

  val SHIPS: List<ShipSpec> =
    listOf(
      ShipSpec(
        id = 1,
        name = "Viper",
        foundry = "Kestrel",
        syndicateTitle = "Kestrel Syndicate (Angular Military Rail)",
        role = "High-Velocity Rail Sniper",
        silhouetteDesc = "Forward-swept obsidian rail-prongs with bronze wing-tips and a narrow V-cyan reactor spine.",
        weaponSummary = "PRIMARY ONLY: Twin Hyper-Velocity Railgun (Zero Bomb Slot — Pure Gunship)",
        maxFlux = 2400f,
        rechargeRate = 360f,
        topSpeed = 540f,
        afterburnerSpeed = 720f,
        thrust = 920f,
        turnRate = 5.4f,
        primaryCost = 62f,
        primaryCooldown = 0.14f,
        primaryDamage = 260f,
        primarySpeed = 1040f,
        primaryStreams = 1,
        primarySpreadRad = 0f,
        primaryBounces = 0,
        hasBomb = false,
        bombType = "NONE",
        bombCost = 0f,
        bombCooldown = 0f,
        bombDamage = 0f,
        bombBounces = 0,
        supportsMultifire = true,
        initialRepels = 2,
        initialBursts = 1,
        accentColorHex = 0xFF00F5D4.toInt(),
      ),
      ShipSpec(
        id = 2,
        name = "Ricochet",
        foundry = "Kestrel",
        syndicateTitle = "Kestrel Syndicate (Angular Military Rail)",
        role = "Trench Bounce-Bomber",
        silhouetteDesc = "Twin-forked ballistic hull with gold-trimmed deflection vanes and a deep cyan V-core.",
        weaponSummary = "PRIMARY: Ricochet Pulse + SECONDARY: 3-Bounce Bank Torpedo (320⚡ FLUX)",
        maxFlux = 2600f,
        rechargeRate = 340f,
        topSpeed = 500f,
        afterburnerSpeed = 680f,
        thrust = 860f,
        turnRate = 4.9f,
        primaryCost = 58f,
        primaryCooldown = 0.16f,
        primaryDamage = 210f,
        primarySpeed = 920f,
        primaryStreams = 2,
        primarySpreadRad = 0.04f,
        primaryBounces = 1,
        hasBomb = true,
        bombType = "BOUNCE",
        bombCost = 320f,
        bombCooldown = 0.85f,
        bombDamage = 780f,
        bombBounces = 3,
        supportsMultifire = true,
        initialRepels = 2,
        initialBursts = 1,
        accentColorHex = 0xFF29B6F6.toInt(),
      ),
      ShipSpec(
        id = 3,
        name = "Suppressor",
        foundry = "Aurelia",
        syndicateTitle = "Aurelia Syndicate (Imperial Gold/Crescent)",
        role = "Crescent Rapid Interceptor",
        silhouetteDesc = "Swept imperial crescent wings in dark indigo & gold with an overcharged V-cyan heart.",
        weaponSummary = "PRIMARY ONLY: Quad-Stream Rapid Plasma Array (Zero Bomb Slot — Pure Gunship)",
        maxFlux = 2800f,
        rechargeRate = 410f,
        topSpeed = 515f,
        afterburnerSpeed = 695f,
        thrust = 880f,
        turnRate = 5.1f,
        primaryCost = 52f,
        primaryCooldown = 0.11f,
        primaryDamage = 195f,
        primarySpeed = 960f,
        primaryStreams = 2,
        primarySpreadRad = 0.06f,
        primaryBounces = 0,
        hasBomb = false,
        bombType = "NONE",
        bombCost = 0f,
        bombCooldown = 0f,
        bombDamage = 0f,
        bombBounces = 0,
        supportsMultifire = true,
        initialRepels = 3,
        initialBursts = 2,
        accentColorHex = 0xFFFFD54F.toInt(),
      ),
      ShipSpec(
        id = 4,
        name = "Goliath",
        foundry = "Aurelia",
        syndicateTitle = "Aurelia Syndicate (Imperial Gold/Crescent)",
        role = "Cathedral Siege Dreadnought",
        silhouetteDesc = "Heavy armored dreadnought chassis with bronze bastion shoulders and a massive V-reactor.",
        weaponSummary = "PRIMARY: Heavy Twin Autocannon + SECONDARY: Lvl-3 Siege Nova Bomb (520⚡ FLUX)",
        maxFlux = 4400f,
        rechargeRate = 390f,
        topSpeed = 420f,
        afterburnerSpeed = 590f,
        thrust = 720f,
        turnRate = 3.8f,
        primaryCost = 85f,
        primaryCooldown = 0.20f,
        primaryDamage = 330f,
        primarySpeed = 880f,
        primaryStreams = 2,
        primarySpreadRad = 0.05f,
        primaryBounces = 0,
        hasBomb = true,
        bombType = "SIEGE",
        bombCost = 520f,
        bombCooldown = 1.15f,
        bombDamage = 1280f,
        bombBounces = 1,
        supportsMultifire = true,
        initialRepels = 3,
        initialBursts = 2,
        accentColorHex = 0xFFFFB300.toInt(),
      ),
      ShipSpec(
        id = 5,
        name = "Twinfang",
        foundry = "Vortex",
        syndicateTitle = "Vortex Syndicate (Bio-Tech Manta)",
        role = "Dual-Boom Spread Gunner",
        silhouetteDesc = "Predatory twin-boom manta frame with bronze fangs and a split electric-cyan core.",
        weaponSummary = "PRIMARY ONLY: Tri-Spread Bio-Plasma Volley (Zero Bomb Slot — Pure Gunship)",
        maxFlux = 2500f,
        rechargeRate = 375f,
        topSpeed = 530f,
        afterburnerSpeed = 710f,
        thrust = 910f,
        turnRate = 5.3f,
        primaryCost = 68f,
        primaryCooldown = 0.14f,
        primaryDamage = 175f,
        primarySpeed = 950f,
        primaryStreams = 3,
        primarySpreadRad = 0.11f,
        primaryBounces = 0,
        hasBomb = false,
        bombType = "NONE",
        bombCost = 0f,
        bombCooldown = 0f,
        bombDamage = 0f,
        bombBounces = 0,
        supportsMultifire = true,
        initialRepels = 2,
        initialBursts = 2,
        accentColorHex = 0xFF00E5FF.toInt(),
      ),
      ShipSpec(
        id = 6,
        name = "Disruptor",
        foundry = "Vortex",
        syndicateTitle = "Vortex Syndicate (Bio-Tech Manta)",
        role = "EMP Stingray Specialist",
        silhouetteDesc = "Wide stingray hydrofoil with gold-plated arc emitters and a pulsing V-cyan capacitor.",
        weaponSummary = "PRIMARY: Ion Needler + SECONDARY: EMP Capacitor Freeze Bomb (380⚡ FLUX)",
        maxFlux = 2700f,
        rechargeRate = 365f,
        topSpeed = 495f,
        afterburnerSpeed = 675f,
        thrust = 850f,
        turnRate = 4.8f,
        primaryCost = 60f,
        primaryCooldown = 0.15f,
        primaryDamage = 215f,
        primarySpeed = 930f,
        primaryStreams = 2,
        primarySpreadRad = 0.04f,
        primaryBounces = 0,
        hasBomb = true,
        bombType = "EMP",
        bombCost = 380f,
        bombCooldown = 0.95f,
        bombDamage = 690f,
        bombBounces = 2,
        supportsMultifire = true,
        initialRepels = 3,
        initialBursts = 1,
        accentColorHex = 0xFFB388FF.toInt(),
      ),
      ShipSpec(
        id = 7,
        name = "Valkyrie",
        foundry = "Rustweld",
        syndicateTitle = "Rustweld Syndicate (Skeletal Heavy-Thruster)",
        role = "X-Frame Proximity Gunship",
        silhouetteDesc = "Aggressive X-wing skeletal interceptor with bronze thruster pods and V-cyan reactor.",
        weaponSummary = "PRIMARY: Heavy Rotary + SECONDARY: Proximity Flak Detonator (360⚡ FLUX)",
        maxFlux = 3200f,
        rechargeRate = 355f,
        topSpeed = 485f,
        afterburnerSpeed = 665f,
        thrust = 840f,
        turnRate = 4.6f,
        primaryCost = 66f,
        primaryCooldown = 0.15f,
        primaryDamage = 240f,
        primarySpeed = 940f,
        primaryStreams = 2,
        primarySpreadRad = 0.05f,
        primaryBounces = 0,
        hasBomb = true,
        bombType = "PROX",
        bombCost = 360f,
        bombCooldown = 0.88f,
        bombDamage = 880f,
        bombBounces = 1,
        supportsMultifire = true,
        initialRepels = 2,
        initialBursts = 2,
        accentColorHex = 0xFFFF7043.toInt(),
      ),
      ShipSpec(
        id = 8,
        name = "Manta",
        foundry = "Rustweld",
        syndicateTitle = "Rustweld Syndicate (Skeletal Heavy-Thruster)",
        role = "Pincer Tactical Mine-Layer",
        silhouetteDesc = "Armored pincer hull with gold-beveled mandible plates and a twin-cell V-cyan core.",
        weaponSummary = "PRIMARY: Dual Gauss + SECONDARY: Stationary Proximity Space Mine (340⚡ FLUX)",
        maxFlux = 3500f,
        rechargeRate = 370f,
        topSpeed = 465f,
        afterburnerSpeed = 640f,
        thrust = 810f,
        turnRate = 4.5f,
        primaryCost = 64f,
        primaryCooldown = 0.16f,
        primaryDamage = 225f,
        primarySpeed = 910f,
        primaryStreams = 2,
        primarySpreadRad = 0.06f,
        primaryBounces = 0,
        hasBomb = true,
        bombType = "MINE",
        bombCost = 340f,
        bombCooldown = 0.80f,
        bombDamage = 1040f,
        bombBounces = 0,
        supportsMultifire = true,
        initialRepels = 3,
        initialBursts = 1,
        accentColorHex = 0xFF66BB6A.toInt(),
      ),
    )

  val POWERUP_CODEX: List<PowerUpCodexEntry> =
    listOf(
      PowerUpCodexEntry(
        id = "full_charge",
        title = "FULL FLUX CAPACITOR CHARGE",
        dropRatePercent = 18,
        category = "INSTANT REACTOR RESTORE",
        effectSummary = "Immediately restores 100% of your ship's FLUX Capacitor and grants +15 Bounty.",
        tacticalNote = "Grab mid-dogfight to invert a low-FLUX engagement without retreating to Safe Zone.",
      ),
      PowerUpCodexEntry(
        id = "recharge_upgrade",
        title = "RECHARGE MATRIX UPGRADE (+15%)",
        dropRatePercent = 14,
        category = "PERMANENT HULL TUNING",
        effectSummary = "Boosts passive FLUX generation rate by +15% per stack (up to 3 stacks).",
        tacticalNote = "Allows sustained continuous primary fire and longer afterburner burns.",
      ),
      PowerUpCodexEntry(
        id = "top_speed",
        title = "THRUSTER OVERDRIVE (+8% TOP SPEED)",
        dropRatePercent = 14,
        category = "PROPULSION UPGRADE",
        effectSummary = "Increases maximum Newtonian drift velocity and afterburner ceiling by +8%.",
        tacticalNote = "Essential for intercepting High-Value Targets (Bounty >= 250).",
      ),
      PowerUpCodexEntry(
        id = "multifire",
        title = "MULTIFIRE SPREAD CALIBRATOR",
        dropRatePercent = 12,
        category = "ORDNANCE AUGMENT",
        effectSummary = "Unlocks or upgrades the [MULTI] console toggle, adding +2 angled weapon streams.",
        tacticalNote = "Toggle [MULTI] off in tight corridors to conserve FLUX; toggle on for trench sweeps.",
      ),
      PowerUpCodexEntry(
        id = "bouncing_bullets",
        title = "RICOCHET PHASE ROUNDS",
        dropRatePercent = 10,
        category = "BALLISTIC AUGMENT",
        effectSummary = "Primary gun projectiles reflect off riveted steel walls (+1 wall bounce).",
        tacticalNote = "Fires around corners into the center base trench without exposing your hull.",
      ),
      PowerUpCodexEntry(
        id = "special_burst",
        title = "BURST SHRAPNEL RING (+1 CHARGE & BOMB TIER)",
        dropRatePercent = 12,
        category = "SPECIAL BUFF WEAPON (+1 CHARGE / 260⚡)",
        effectSummary = "Fires a 360° ring of 24 wall-bouncing shrapnel projectiles (+1 REPEL & Bomb Tier +1).",
        tacticalNote = "Stacks +1 charge if already holding BURST, or overwrites a different Special Buff Weapon.",
      ),
      PowerUpCodexEntry(
        id = "special_shotgun",
        title = "SHOTGUN CONE BLASTER (+2 CHARGES)",
        dropRatePercent = 10,
        category = "SPECIAL BUFF WEAPON (+2 CHARGES / 260⚡)",
        effectSummary = "Grants 2 charges. Fires a dense 60° cone blast of bouncing pellets in 3 rapid consecutive waves.",
        tacticalNote = "Devastating close-range burst damage when all 3 waves connect. Same-type pickups stack +2 charges.",
      ),
      PowerUpCodexEntry(
        id = "special_missile",
        title = "LOCK-ON BOUNCING MISSILE (+1 CHARGE)",
        dropRatePercent = 10,
        category = "SPECIAL BUFF WEAPON (+1 CHARGE / 260⚡)",
        effectSummary = "Homing wall-bouncing missile capped to 4-Goliath top speed (420 px/s); detonates at 1.5s or on impact.",
        tacticalNote = "Triggers a pulsing red MISSILE LOCK reticle & beep on the target. Same-type pickups stack +1 charge.",
      ),
    )

  fun getAllNineShips(): List<ShipSpec> = SHIPS + listOf(customShip9Spec)

  fun getShipById(id: Int): ShipSpec =
    if (id == 9) customShip9Spec else (SHIPS.find { it.id == id } ?: SHIPS[0])
}

