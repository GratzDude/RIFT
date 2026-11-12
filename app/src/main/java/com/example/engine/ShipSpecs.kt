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
  val speedPts: Int,
  val thrustPts: Int,
  val turnPts: Int,
  val maxFluxPts: Int,
  val rechargePts: Int,
  val primaryPts: Int,
  val secondaryPts: Int,
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
  val isParallelTwin: Boolean = false,
  val hasBomb: Boolean,
  val bombType: String, // "NONE", "PHASE_DASH", "BOUNCE", "SIEGE", "CLOAK_EMP", "PROX", "MINE", "BURST", "SHOTGUN", "MISSILE"
  val bombCost: Float,
  val bombCooldown: Float,
  val bombDamage: Float,
  val bombBounces: Int,
  val bombRecoil: Float = 0f,
  val supportsNativeMultifire: Boolean = false,
  val initialRepels: Int = 2,
  val accentColorHex: Int,
) {
  val totalAllocatedPoints: Int
    get() = speedPts + thrustPts + turnPts + maxFluxPts + rechargePts + primaryPts + secondaryPts
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

/**
 * Represents one of the 3 saveable loadout slots (Slot 9A, Slot 9B, Slot 9C) for the 9th Ship ("Rift Frame").
 * Implements Option A (Free-Spend + Universal Baseline Hull Floor, 0..30 pts per stat, 100-Point Parity Matrix).
 */
data class CustomShipLoadoutSlot(
  val slotId: String = "9A", // "9A", "9B", "9C"
  val customName: String = "RIFT FRAME 9A",
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

    val (priTitle, pCost, pCd, pDmg, pSpd, pStreams, pSpread, pBounces, isParallel) =
      when (primaryArchetype) {
        "TWIN_SCATTER" ->
          PrimaryTuple("Short-Range Twin Scatter", 56f, 0.14f, 175f, 900f * velBonus, 2, 0.065f, 0, false)
        "ION_NEEDLER" ->
          PrimaryTuple("Twin Ion Needlers", 54f, 0.13f, 165f, 950f * velBonus, 2, 0.04f, 0, false)
        "ANGLED_PULSE" ->
          PrimaryTuple("Twin Angled Pulse", 58f, 0.16f, 195f, 930f * velBonus, 2, 0.045f, 1, false)
        "HEAVY_CANNON" ->
          PrimaryTuple("Heavy Dual Cannon", 82f, 0.21f, 310f, 880f * velBonus, 2, 0.05f, 0, false)
        "QUAD_PULSE" ->
          PrimaryTuple("Quad-Converging Pulse", 64f, 0.15f, 72f, 940f * velBonus, 4, 0.045f, 0, false)
        "SNIPER_RAIL" ->
          PrimaryTuple("Hyper-Velocity Dual Rail", 62f, 0.15f, 285f, 1120f * velBonus, 1, 0f, 0, false)
        "ROTARY_PULSE" ->
          PrimaryTuple("High-RPM Rotary Pulse", 48f, 0.095f, 240f, 980f * velBonus, 1, 0f, 0, false)
        "PARALLEL_TWIN" ->
          PrimaryTuple("Parallel Twin-Stream Autocannon", 60f, 0.13f, 135f, 1000f * velBonus, 2, 0.025f, 0, true)
        else -> // "LIGHT_PULSE" (0 Pts Free Starter Primary)
          PrimaryTuple("Standard Light Pulse", 50f, 0.16f, 155f, 920f * velBonus, 1, 0f, 0, false)
      }

    val nativeMultifire = secondaryType == "NATIVE_MULTISHOT"
    val extraRepels = if (secondaryType == "MINE") 1 else 0
    val hasActiveSecondaryButton =
      secondaryType !in setOf("NONE", "PASSIVE_GUNFIGHTER", "NATIVE_MULTISHOT")

    val (secTitle, bCost, bCd, bDmg, bBounces, bRecoil) =
      when (secondaryType) {
        "PHASE_DASH" ->
          SecondaryTuple("Phase Dash / Sniper Overcharge (300❖)", 300f, 2.5f, 0f, 0, 0f)
        "NATIVE_MULTISHOT" ->
          SecondaryTuple("Native 3x / 1/3x Split Multi-Shot", 0f, 0f, 0f, 0, 0f)
        "MINE" ->
          SecondaryTuple("Proximity Mines +1 Repel (340❖)", 340f, 0.80f, 1040f, 0, 0f)
        "BOUNCE" ->
          SecondaryTuple("Bounce Bomb (350❖)", 350f, 0.85f, 780f, 3, if (isPureGunfighterPassive) 0f else 180f)
        "PROX" ->
          SecondaryTuple("Proximity Flak Bomb (450❖)", 450f, 0.95f, 820f, 1, if (isPureGunfighterPassive) 0f else 210f)
        "BURST" ->
          SecondaryTuple("Native Radial Burst (500❖ • 4s CD)", 500f, 4.0f, 280f, 2, 0f)
        "SHOTGUN" ->
          SecondaryTuple("Native 3-Wave Shotgun (500❖ • 4s CD)", 500f, 4.0f, 55f, 2, 0f)
        "SIEGE" ->
          SecondaryTuple("Siege Bomb (650❖ • Heavy Recoil)", 650f, 1.20f, 1350f, 1, if (isPureGunfighterPassive) 0f else 360f)
        "CLOAK_EMP" ->
          SecondaryTuple("Tactical Cloak (180❖/s) + EMP Bomb (350❖)", 350f, 0.95f, 580f, 2, 0f)
        "MISSILE" ->
          SecondaryTuple("Native Lock-On Missile (600❖ • 5s CD)", 600f, 5.0f, 780f, 6, 0f)
        "PASSIVE_GUNFIGHTER" ->
          SecondaryTuple("Pure Gunfighter Passive (+10% Vel, 0 Recoil)", 0f, 0f, 0f, 0, 0f)
        else ->
          SecondaryTuple("No Secondary (0 Pts)", 0f, 0f, 0f, 0, 0f)
      }

    val accentInt =
      try {
        Color.parseColor(visualRecipe.coreGlowHex)
      } catch (_: Exception) {
        0xFF00F5D4.toInt()
      }

    return ShipSpec(
      id = 9,
      name = customName.ifBlank { "Rift Frame $slotId" },
      role = "Custom Modular Rift Frame ($slotId • ${totalAllocatedPoints()} Pts)",
      tacticalLore = "Progressive Level 20–100 modular chassis engineered on the universal Baseline Hull Floor with AI Hull Forge shader geometry.",
      silhouetteDesc = visualRecipe.promptUsed,
      weaponSummary = "PRIMARY: $priTitle (${primaryPts}P) • CLASS/SEC: $secTitle (${secondaryPts}P)",
      primaryTitle = priTitle,
      secondaryTitle = secTitle,
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
      bombType = secondaryType,
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
        PrimaryArchetypeOption("LIGHT_PULSE", "Standard Light Pulse", 0, "Free starter pulse blaster (0 Pts)."),
        PrimaryArchetypeOption("TWIN_SCATTER", "Short-Range Twin Scatter", 10, "Fast close-quarters twin defensive blasters (8-Manta style)."),
        PrimaryArchetypeOption("ION_NEEDLER", "Twin Ion Needlers", 10, "Rapid-fire dual ion needlers (6-Disruptor style)."),
        PrimaryArchetypeOption("ANGLED_PULSE", "Twin Angled Pulse", 12, "Dual-wing angled pulse with 1 native wall bounce (2-Ricochet style)."),
        PrimaryArchetypeOption("HEAVY_CANNON", "Heavy Dual Cannon", 12, "Slow fire rate, heavy kinetic shells (4-Goliath style)."),
        PrimaryArchetypeOption("QUAD_PULSE", "Quad-Converging Pulse", 12, "4 light bolts converging at mid-range (7-Valkyrie style)."),
        PrimaryArchetypeOption("SNIPER_RAIL", "Hyper-Velocity Dual Rail", 15, "Fastest projectile velocity, pinpoint single-shot punch (1-Viper style)."),
        PrimaryArchetypeOption("ROTARY_PULSE", "High-RPM Adaptive Rotary", 15, "Fastest fire rate sustained suppressive stream (3-Suppressor style)."),
        PrimaryArchetypeOption("PARALLEL_TWIN", "Parallel Twin-Stream", 15, "2 parallel high-velocity bolts with wide dogfight hitbox (5-Twinfang style)."),
      )

    val SECONDARY_OPTIONS: List<SecondaryAbilityOption> =
      listOf(
        SecondaryAbilityOption("NONE", "No Secondary (Starter)", 0, "0❖", "Leaves Secondary slot empty so all points go to hull stats or primary."),
        SecondaryAbilityOption("PASSIVE_GUNFIGHTER", "Pure Gunfighter Passive", 10, "PASSIVE", "+10% Primary Bullet Velocity and 0 firing recoil."),
        SecondaryAbilityOption("PHASE_DASH", "Phase Dash + Overcharge", 15, "300❖", "Instant directional dodge burst + 35% next-shot rail overcharge (2.5s CD)."),
        SecondaryAbilityOption("NATIVE_MULTISHOT", "Native Split Multi-Shot", 15, "TOGGLE", "Permanently unlocks Multi-Shot ON/OFF (100% single vs 3x 1/3 split)."),
        SecondaryAbilityOption("MINE", "Proximity Mines (+1 Repel)", 18, "340❖", "Drops armed proximity mines (max 6, 30s life) + starts with +1 Repel."),
        SecondaryAbilityOption("BOUNCE", "Bounce Bomb", 20, "350❖", "Plasma warhead reflecting off walls up to 3 times with raycast line."),
        SecondaryAbilityOption("PROX", "Proximity Flak Bomb", 22, "450❖", "65px proximity fuse warhead bursting into 8 shrapnel pellets."),
        SecondaryAbilityOption("BURST", "Native Shrapnel Burst", 22, "500❖", "Fires 360° bouncing shrapnel ring on Secondary Button (4s CD)."),
        SecondaryAbilityOption("SHOTGUN", "Native 3-Wave Shotgun", 22, "500❖", "Fires 3 rapid 60° cone pellet waves on Secondary Button (4s CD)."),
        SecondaryAbilityOption("SIEGE", "Siege Bomb", 25, "650❖", "Massive nuclear dreadnought warhead with heavy backward recoil."),
        SecondaryAbilityOption("CLOAK_EMP", "Tactical Cloak + EMP Bomb", 25, "180❖/s • 350❖", "Tap to Cloak invisibly; fire while cloaked to launch 35% Flux-drain EMP Bomb."),
        SecondaryAbilityOption("MISSILE", "Native Lock-On Missile", 25, "600❖", "Fires homing wall-bouncing missile (1.5s detonation, 5s CD)."),
      )

    fun getPrimaryPointCost(archetype: String): Int =
      PRIMARY_OPTIONS.firstOrNull { it.id == archetype }?.pointCost ?: 0

    fun getSecondaryPointCost(secType: String): Int =
      SECONDARY_OPTIONS.firstOrNull { it.id == secType }?.pointCost ?: 0

    fun defaultSlot(slotId: String): CustomShipLoadoutSlot {
      return when (slotId) {
        "9B" ->
          CustomShipLoadoutSlot(
            slotId = "9B",
            customName = "RIFT FRAME 9B",
            speedPts = 2,
            thrustPts = 2,
            turnPts = 2,
            maxFluxPts = 2,
            rechargePts = 2,
            primaryArchetype = "TWIN_SCATTER",
            secondaryType = "NONE",
            visualRecipe =
              CustomHullVisualRecipe(
                hullName = "RIFT FRAME 9B",
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
        "9C" ->
          CustomShipLoadoutSlot(
            slotId = "9C",
            customName = "RIFT FRAME 9C",
            speedPts = 5,
            thrustPts = 0,
            turnPts = 0,
            maxFluxPts = 0,
            rechargePts = 0,
            primaryArchetype = "LIGHT_PULSE",
            secondaryType = "PHASE_DASH",
            visualRecipe =
              CustomHullVisualRecipe(
                hullName = "RIFT FRAME 9C",
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
            slotId = "9A",
            customName = "RIFT FRAME 9A",
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

    fun fromJsonString(raw: String?, fallbackSlotId: String = "9A"): CustomShipLoadoutSlot {
      if (raw.isNullOrBlank()) return defaultSlot(fallbackSlotId)
      return try {
        val obj = JSONObject(raw)
        val recipeObj = obj.optJSONObject("visualRecipe")
        val rawPri = obj.optString("primaryArchetype", "LIGHT_PULSE")
        val validPri = if (PRIMARY_OPTIONS.any { it.id == rawPri }) rawPri else "LIGHT_PULSE"
        val rawSec = obj.optString("secondaryType", "NONE")
        val validSec = if (SECONDARY_OPTIONS.any { it.id == rawSec }) rawSec else "NONE"
        CustomShipLoadoutSlot(
          slotId = obj.optString("slotId", fallbackSlotId),
          customName = obj.optString("customName", "RIFT FRAME $fallbackSlotId").take(18),
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
        defaultSlot(fallbackSlotId)
      }
    }
  }
}

private data class PrimaryTuple(
  val title: String,
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

  var activeCustomShipSlot: CustomShipLoadoutSlot = CustomShipLoadoutSlot.defaultSlot("9A")
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
        weaponSummary = "PRI: Hyper-Velocity Dual Rail (15P) • CLASS: Phase Dash + Sniper Overcharge (300❖ / 2.5s CD)",
        primaryTitle = "Hyper-Velocity Dual Rail",
        secondaryTitle = "Phase Dash / Sniper Overcharge (300❖)",
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
        primaryCost = 62f,
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
        weaponSummary = "PRI: Twin Angled Pulse (12P) • SEC: 3-Wall Bounce Bomb (350❖ + Recoil)",
        primaryTitle = "Twin Angled Pulse",
        secondaryTitle = "Bounce Bomb (350❖ • 3 Bounces)",
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
        primaryCost = 58f,
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
        weaponSummary = "PRI: High-RPM Rotary Pulse (15P) • CLASS: Native 3x / 1/3x Split Multi-Shot Toggle",
        primaryTitle = "High-RPM Rotary Pulse",
        secondaryTitle = "Native 3x / 1/3x Split Multi-Shot",
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
        primaryCost = 48f,
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
        weaponSummary = "PRI: Heavy Dual Cannon (12P) • SEC: Siege Bomb (650❖ + Heavy Newtonian Recoil)",
        primaryTitle = "Heavy Dual Cannon",
        secondaryTitle = "Siege Bomb (650❖ • Heavy Recoil)",
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
        primaryCost = 82f,
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
        weaponSummary = "PRI: Parallel Twin-Stream (15P) • CLASS: Native Multi-Shot (4-Bolt Fan Split)",
        primaryTitle = "Parallel Twin-Stream Autocannon",
        secondaryTitle = "Native Multi-Shot + Point-Defense",
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
        primaryCost = 60f,
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
      // 6-Disruptor (Stealth & EMP Infiltrator — 100 Pts Total: 10 Pri + 25 Sec + 65 Stats)
      ShipSpec(
        id = 6,
        name = "Disruptor",
        role = "Stealth & EMP Infiltrator",
        tacticalLore = "Cloak-capable infiltrator that vanishes from enemy radar before unleashing a 35% Flux-draining EMP warhead.",
        silhouetteDesc = "Wide stingray hydrofoil with gold-plated arc emitters and a pulsing V-cyan capacitor.",
        weaponSummary = "PRI: Twin Ion Needlers (10P) • SEC: Tactical Cloak (180❖/s) + EMP Bomb (350❖)",
        primaryTitle = "Twin Ion Needlers",
        secondaryTitle = "Tactical Cloak + EMP Bomb (350❖)",
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
        primaryCost = 54f,
        primaryCooldown = 0.13f,
        primaryDamage = 165f,
        primarySpeed = 950f,
        primaryStreams = 2,
        primarySpreadRad = 0.04f,
        primaryBounces = 0,
        hasBomb = true,
        bombType = "CLOAK_EMP",
        bombCost = 350f,
        bombCooldown = 0.95f,
        bombDamage = 580f,
        bombBounces = 2,
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
        weaponSummary = "PRI: Quad-Converging Pulse (12P) • SEC: Proximity Flak Bomb (450❖ • 8 Shrapnel)",
        primaryTitle = "Quad-Converging Pulse",
        secondaryTitle = "Proximity Flak Bomb (450❖)",
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
        primaryCost = 64f,
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
        weaponSummary = "PRI: Short-Range Twin Scatter (10P) • SEC: Proximity Mines (340❖ • Max 6) +1 Repel",
        primaryTitle = "Short-Range Twin Scatter",
        secondaryTitle = "Proximity Mines (340❖) +1 Repel",
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
        primaryCost = 56f,
        primaryCooldown = 0.14f,
        primaryDamage = 175f,
        primarySpeed = 900f,
        primaryStreams = 2,
        primarySpreadRad = 0.065f,
        primaryBounces = 0,
        hasBomb = true,
        bombType = "MINE",
        bombCost = 340f,
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
   * - Bucket A (0..5): Special Buff Slot Pickups (4th Right-Hand Button — Charge-Based, 0❖ Cost)
   * - Bucket B (6..10): Instant Ship & Weapon Enhancers (Top-Center Expandable Buff Bar)
   */
  val POWERUP_CODEX: List<PowerUpCodexEntry> =
    listOf(
      // Bucket A: Special Buff Slot Pickups (0..5) — Charge-Based, 0❖ Cost
      PowerUpCodexEntry(
        id = "special_burst",
        buffTypeIndex = 0,
        title = "BURST (+1 CHARGE • 0❖)",
        dropRatePercent = 9,
        category = "BUCKET A: SPECIAL BUFF SLOT (0❖ COST)",
        effectSummary = "Fires a 360° radial ring of 24 high-speed wall-bouncing shrapnel bullets.",
        tacticalNote = "Stacks +1 charge if already holding Burst, or overwrites a different Special Buff Slot item.",
      ),
      PowerUpCodexEntry(
        id = "special_shotgun",
        buffTypeIndex = 1,
        title = "SHOTGUN (+2 CHARGES • 0❖)",
        dropRatePercent = 9,
        category = "BUCKET A: SPECIAL BUFF SLOT (0❖ COST)",
        effectSummary = "Fires a dense 60° cone blast of bouncing pellets in 3 rapid consecutive waves.",
        tacticalNote = "Massive close-quarters burst damage at zero Flux cost. Same-type pickups stack +2 charges.",
      ),
      PowerUpCodexEntry(
        id = "special_missile",
        buffTypeIndex = 2,
        title = "LOCK-ON BOUNCING MISSILE (+1 CHARGE • 0❖)",
        dropRatePercent = 8,
        category = "BUCKET A: SPECIAL BUFF SLOT (0❖ COST)",
        effectSummary = "Locks onto target (red MISSILE LOCK reticle/beep), travels at 4-Goliath speed (380 PX/S), bounces off walls, and detonates after 1.5s.",
        tacticalNote = "Forces enemies to burn Repel, Phase Dash, or hide behind a Brick barrier.",
      ),
      PowerUpCodexEntry(
        id = "special_decoy",
        buffTypeIndex = 3,
        title = "DECOY HOLOGRAM (+1 CHARGE • 0❖)",
        dropRatePercent = 8,
        category = "BUCKET A: SPECIAL BUFF SLOT (0❖ COST)",
        effectSummary = "Spawns a hologram clone of your exact ship (with callsign and bounty tag) along a mirrored/divergent heading for 8 seconds.",
        tacticalNote = "Draws enemy AI fire and soaks active Lock-On Bouncing Missiles.",
      ),
      PowerUpCodexEntry(
        id = "special_brick",
        buffTypeIndex = 4,
        title = "BRICK ENERGY BARRIER (+1 CHARGE • 0❖)",
        dropRatePercent = 8,
        category = "BUCKET A: SPECIAL BUFF SLOT (0❖ COST)",
        effectSummary = "Deploys a solid 120px energy barrier wall perpendicular to your heading for 10 seconds.",
        tacticalNote = "Physically blocks ships, bullets, and bombs while reflecting ricochet bank shots.",
      ),
      PowerUpCodexEntry(
        id = "special_rocket",
        buffTypeIndex = 5,
        title = "ROCKET HYPER-THRUSTER (+1 CHARGE • 0❖)",
        dropRatePercent = 8,
        category = "BUCKET A: SPECIAL BUFF SLOT (0❖ COST)",
        effectSummary = "Ignites a 3.0-second hyper-thruster boost (1.65x max speed) costing 0❖ Flux with a blazing plasma trail.",
        tacticalNote = "Ultimate escape or high-bounty chase tool when your Flux capacitor is depleted.",
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
        title = "SHOT-LIMITED MULTI-SHOT (+25 SHOTS)",
        dropRatePercent = 9,
        category = "BUCKET B: SHOT-LIMITED AUGMENT",
        effectSummary = "Grants +25 primary trigger pulls of Multi-Shot (ticks down -1 per shot while Multi-Shot is ON).",
        tacticalNote = "On native Multi-Shot ships (3-Suppressor, 5-Twinfang, or 9-Rift Frame), grants +25% damage / 5-stream overdrive for 25 shots!",
      ),
      PowerUpCodexEntry(
        id = "bouncing_bullets",
        buffTypeIndex = 9,
        title = "BOUNCING BULLETS (+30 SHOTS)",
        dropRatePercent = 9,
        category = "BUCKET B: SHOT-LIMITED AUGMENT",
        effectSummary = "Primary gun projectiles reflect off arena walls (+1 wall bounce) for your next 30 primary trigger pulls.",
        tacticalNote = "Stackable shot counter displayed in the top-center Expandable Buff Bar.",
      ),
      PowerUpCodexEntry(
        id = "overcharge_surge",
        buffTypeIndex = 10,
        title = "OVERCHARGE SURGE (+8% • MAX 3x)",
        dropRatePercent = 8,
        category = "BUCKET B: LIFE HULL SURGE",
        effectSummary = "Increases Top Speed, Thrust, and Flux Recharge by +8% per stack (up to 3 stacks) until death.",
        tacticalNote = "At 3 stacks (+24% Speed/Thrust/Recharge), turns any hull into an apex sector predator.",
      ),
    )

  fun getAllNineShips(): List<ShipSpec> = SHIPS + listOf(customShip9Spec)

  fun getShipById(id: Int): ShipSpec =
    if (id == 9) customShip9Spec else (SHIPS.find { it.id == id } ?: SHIPS[0])
}
