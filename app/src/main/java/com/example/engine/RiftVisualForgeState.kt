package com.example.engine

import android.content.Context
import android.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import org.json.JSONObject

enum class ForgeElementLayer(val label: String) {
 WORLD("WORLD"),
 HUD("HUD"),
}

enum class ForgeElementTarget(
 val key: String,
 val title: String,
 val subtitle: String,
 val layer: ForgeElementLayer,
 val presetPrompts: List<Pair<String, String>>,
) {
 SAFE_ZONE_WALL("safe_zone_wall", "Safe Zone Rift Rim", "Circular wall-less Rift rim & sanctuary boundary", ForgeElementLayer.WORLD, listOf(
  "Cyan Horizon" to "Crisp circular cyan Ring-Space Rift rim with luminous containment ticks",
  "Sovereign Gold" to "Imperial gold circular Rift sanctuary rim with warm amber corona",
  "Cryo Horizon" to "Glacial ice-blue circular Rift rim with frost-white horizon shimmer",
  "Crimson Lockdown" to "High-energy crimson circular Rift containment rim with deep obsidian edge",
 )),
 SAFE_ZONE_PAD("safe_zone_pad", "Safe Zone DS9 Station", "5x ship-scale metallic DS9 citadel & Ops holographic ring", ForgeElementLayer.WORLD, listOf(
  "Sovereign Titanium" to "Brushed titanium steel DS9 docking ring and curved pylons with sky-cyan Ops holo-ring",
  "Cardassian Gold" to "Gilded bronze-gold Cardassian bastion armor with warm solar-amber Ops holo-ring",
  "Obsidian Bastion" to "Pitch-black obsidian citadel armor with electric teal clamps and Ops holo-ring",
 )),
 PLACED_STASIS_BARRIER("placed_stasis_barrier", "Placed Stasis Barrier", "45.0s deployed tethered hard-light forcefield", ForgeElementLayer.WORLD, listOf(
  "Tethered Cyan" to "Tethered electric cyan hard-light forcefield beam with twin emitter nodes",
  "Violet Phase Wall" to "Amethyst phase-locked energy barrier with bright magenta core",
  "Solar Plasma" to "Blazing amber-gold plasma wall with white-hot core filament",
  "Emerald Aegis" to "Bio-emerald tactical shield wall with crisp teal tethers",
 )),
 OBSTACLE_ORBITAL_STATION("obstacle_orbital_station", "Orbital Ring Station", "3D ringed orbital station with lit decks & reactor core", ForgeElementLayer.WORLD, listOf(
  "Obsidian Ring" to "Pitch-black obsidian torus station with lit cyan viewports and reactor hub",
  "Solar Array Hub" to "Titanium orbital station with solar wings and warm amber deck windows",
  "Sovereign Bastion" to "Armored gunmetal citadel ring with gold trim and turquoise core",
  "Deep Void Relay" to "Dark indigo research station with violet energy conduits",
 )),
 OBSTACLE_ASTEROID_STATION("obstacle_asteroid_station", "Asteroid Outpost Base", "Hollowed asteroid mining outpost with lit hangar bay", ForgeElementLayer.WORLD, listOf(
  "Mining Outpost" to "Craggy dark basalt asteroid fused with recessed cyan-lit hangar bay",
  "Smuggler Dock" to "Hollowed obsidian rock base with amber landing lights and radar dish",
  "Cryo Research" to "Frost-rimmed glacier asteroid outpost with bright sky-blue bay",
  "Foundry Core" to "Volcanic dark iron asteroid station with molten orange reactor vents",
 )),
 OBSTACLE_SPACE_ASTEROID("obstacle_space_asteroid", "Space Basalt Asteroid", "Multi-faceted craggy basalt & obsidian space rock", ForgeElementLayer.WORLD, listOf(
  "Obsidian Monolith" to "Faceted pitch-black obsidian asteroid with glowing teal mineral veins",
  "Cratered Basalt" to "Weathered dark slate space rock with deep craters and sunlit rim",
  "Amethyst Geode" to "Fractured dark asteroid exposing luminous violet Rift crystal seams",
  "Magma Fissure" to "Charred volcanic asteroid with glowing crimson-amber magma cracks",
 )),
 OBSTACLE_FLOATING_DEBRIS("obstacle_floating_debris", "Derelict Hull Debris", "Drifting armored reactor pylon & sheared hull plate", ForgeElementLayer.WORLD, listOf(
  "Reactor Pylon" to "Obsidian reactor pylon wreckage with pulsing cyan conduit core",
  "Dreadnought Plate" to "Sheared gunmetal battleship armor girder with amber warning nodes",
  "Rift Conduit" to "Shattered violet phase-gate strut with glowing magenta arc",
  "Cryo Bulkhead" to "Frosted titanium hull rib with pale ice-blue specular edge",
 )),
 ARENA_PERIMETER_RING("arena_perimeter_ring", "Playfield Rift Storm Wall", "Rippling outer Rift clouds, mist & proximity lightning", ForgeElementLayer.WORLD, listOf(
  "Cyan Storm Cloud" to "Billowing electric-cyan Rift storm cloud wall with inward plasma mist and arc lightning",
  "Crimson Tempest" to "High-voltage crimson Rift storm cloud boundary with branching rose lightning",
  "Violet Horizon" to "Deep amethyst Ring-Space storm wall with indigo mist and electric arcs",
  "Gold Sovereign" to "Solar-gold Rift storm cloud perimeter with crisp white lightning filaments",
 )),
 BACKGROUND_NEBULA("background_nebula", "Deep-Space Nebula & Stars", "Parallax starfield, cosmic dust & volumetric nebula", ForgeElementLayer.WORLD, listOf(
  "Abyssal Cyan" to "Deep pitch-black void with subtle cyan and indigo nebula clouds",
  "Crimson Forge" to "Dark obsidian cosmos with distant crimson and violet star-forming dust",
  "Pure Pitch Void" to "Ultra-clean pitch-black competitive starfield with minimal nebula haze",
  "Emerald Drift" to "Deep-space teal and emerald cosmic aurora with crisp starlight",
 )),
 RIFT_PLASMA("rift_plasma", "Plasma Rift", "Crimson thermal burn vortex anomaly", ForgeElementLayer.WORLD, listOf(
  "Crimson Solar" to "Swirling crimson and magenta plasma vortex with white-hot core",
  "Blue Supergiant" to "White-hot cobalt-cyan stellar plasma vortex with radiant flare",
  "Molten Gold" to "Blazing solar-gold and orange fusion vortex with intense corona",
  "Venom Plasma" to "Corrosive acid-emerald and lime thermal plasma swirl",
 )),
 RIFT_REPULSOR("rift_repulsor", "Repulsor Rift", "Shockwave outward-push field anomaly", ForgeElementLayer.WORLD, listOf(
  "Sky Shockwave" to "Bright sky-cyan outward shockwave vortex with crisp white core",
  "Emerald Pulse" to "Kinetic emerald-teal repulsor vortex with concentric pulse rings",
  "Amber Kinetic" to "High-impulse amber-gold shockwave field with radiant rim",
  "Frost Gale" to "Crystalline ice-blue repulsor anomaly with diamond-white center",
 )),
 RIFT_GRAVITY("rift_gravity", "Gravity Rift", "Inward event-horizon singularity vortex", ForgeElementLayer.WORLD, listOf(
  "Violet Singularity" to "Deep amethyst and indigo inward spiral around pitch-black void core",
  "Golden Gargantua" to "Relativistic amber-gold accretion disk around dark event horizon",
  "Crimson Collapse" to "Collapsed crimson star singularity with dark void center",
  "Abyssal Teal" to "Cold deep-sea cyan accretion spiral around pitch-black core",
 )),
 RIFT_PHASE_PAIR("rift_phase_pair", "Paired Phase Rifts", "Linked Phase Rift A & B instant warp rifts", ForgeElementLayer.WORLD, listOf(
  "Teal & Violet" to "Paired teal-cyan and violet Phase Rifts with dark warp singularity",
  "Gold & Azure" to "Paired sovereign gold and electric blue Phase Rifts with twin rings",
  "Emerald & Magenta" to "High-contrast emerald and neon magenta linked Phase Rifts",
  "Ice & Obsidian" to "Crisp glacier-white and cyan Phase Rifts with razor ticks",
 )),
 SUPPLY_CRATE("supply_crate", "Supply Crate", "Floating 3D octagonal tactical pickup pod", ForgeElementLayer.WORLD, listOf(
  "Tactical Pod" to "Dark metallic octagonal supply pod with glowing cyan-emerald rim",
  "Sovereign Vault" to "Gilded obsidian loot pod with radiant gold bevels",
  "Holo Prism" to "Translucent crystalline supply cube with neon teal core",
  "Heavy Ordnance" to "Armored gunmetal ammo crate with bright amber beacon",
 )),
 PROJECTILE_PRIMARY_BOLT("projectile_primary_bolt", "Primary Plasma Bolt", "Main weapon laser bolts, railgun slugs & tracers", ForgeElementLayer.WORLD, listOf(
  "Crisp Plasma" to "High-contrast cyan-white plasma bolts with tight radiant halo",
  "Overcharged Rail" to "Thick blazing electric-teal lance bolts with intense bloom",
  "Solar Tracer" to "Golden-amber superheated bolts with bright white core",
  "Razor Minimal" to "Ultra-crisp needle laser bolts with subtle tournament glow",
 )),
 PROJECTILE_PROXIMITY_MINE("projectile_proximity_mine", "Proximity Mine & Bomb", "Armed proximity mines, siege warheads & trigger ring", ForgeElementLayer.WORLD, listOf(
  "Obsidian Spike" to "Spiked obsidian naval mine with pulsing crimson-amber fuse ring",
  "Plasma Sphere" to "Glowing violet-magenta gravimetric warhead with crisp danger ring",
  "Cryo Charge" to "Faceted ice-cyan stasis bomb with bright sky-blue sensor ring",
  "Stealth Mine" to "Low-profile gunmetal tactical mine with sharp red strobe core",
 )),
 ECLIPSE_STASIS_BUBBLE("eclipse_stasis_bubble", "Eclipse Stasis", "Two-way 275px Stasis containment field (6.0s)", ForgeElementLayer.WORLD, listOf(
  "Amethyst Stasis" to "Two-way magenta-violet gravimetric Stasis field with cyan inner horizon",
  "Cryo Stasis" to "Translucent glacier-cyan Stasis containment field with white hex rim",
  "Solar Quarantine" to "Golden-amber hard-light Stasis containment field with crisp edge",
  "Void Horizon" to "Deep indigo void Stasis horizon with electric violet boundary ring",
 )),
 FLUX_CORE_ORB("flux_core_orb", "Volatile Flux Core", "Golden singularity orb & carrier crown aura", ForgeElementLayer.WORLD, listOf(
  "Sovereign Gold" to "Pulsing golden singularity orb with radiant solar corona",
  "Supernova Cyan" to "Blazing diamond-cyan reactor core with electric white arcs",
  "Crimson Nova" to "Unstable crimson-gold fusion core with fiery shockwave",
  "Royal Amethyst" to "Luminous violet-gold crown singularity with intense halo",
 )),
 STASIS_BRIG_JAIL("stasis_brig_jail", "Stasis Brig Jail & Core", "Team brig containment ring & 8,000 Flux reactor core", ForgeElementLayer.WORLD, listOf(
  "Hard-Light Brig" to "Circular cyan-crimson team stasis brig with armored 8,000 Flux core",
  "Obsidian Vault" to "Heavy obsidian containment ring with blazing gold reactor heart",
  "Violet Lockdown" to "High-energy amethyst prison dome with pulsing teal generator",
  "Glacier Cell" to "Frost-blue cryo containment ring with crystalline core",
 )),
 SHIP_AURA_LABELS("ship_aura_labels", "Ship Flux Ring & Nametags", "In-world ship Flux arc, HVT crown & callsign plates", ForgeElementLayer.WORLD, listOf(
  "Tactical Clean" to "Crisp cyan-emerald ship Flux arc with high-contrast callsign readout",
  "Tournament Bold" to "Thick luminous Flux ring with bright gold HVT crown accents",
  "Minimal Pro" to "Sleek thin Flux ring and compact low-glare callsign plates",
  "Neon Cyber" to "Vibrant electric-teal Flux gauge with glowing violet trim",
 )),
 HUD_TOP_BAR("hud_top_bar", "Top Status & Mode Bar", "Match mode pill, score readout & Rift Control bar", ForgeElementLayer.HUD, listOf(
  "Obsidian Glass" to "Sleek obsidian glass top status pill with crisp cyan border",
  "Sovereign Gold" to "Dark command header pill with regal gold trim and warm glow",
  "Minimal Ghost" to "Ultra-translucent low-profile status bar with hairline teal rim",
  "Violet Tactics" to "Deep space-indigo header bar with glowing amethyst edge",
 )),
 HUD_NEXUS_BUTTON("hud_nexus_button", "NEXUS Command Button", "Top-left NEXUS console launcher button", ForgeElementLayer.HUD, listOf(
  "Cyan Command" to "Beveled obsidian NEXUS button with electric-cyan emblem glow",
  "Gold Crest" to "Armored command button with sovereign gold border and bright core",
  "Violet Singularity" to "Rift-violet command launcher with glowing magenta trim",
  "Stealth Slate" to "Compact dark titanium button with subtle sky-blue rim",
 )),
 HUD_MINIMAP_CONSOLE("hud_minimap_console", "Circular Radar & Telemetry", "1:1 circular minimap, Flux readout & pilot LVL bar", ForgeElementLayer.HUD, listOf(
  "Tactical Cyan" to "1:1 circular obsidian radar scope with crisp cyan sweep ring",
  "Emerald Sonar" to "Deep-space tactical radar with emerald-teal grid and crisp rim",
  "Imperial Gold" to "Obsidian command radar framed in burnished gold with cyan core",
  "Translucent HUD" to "Lightweight low-opacity radar scope for maximum arena visibility",
 )),
 HUD_BUFF_STRIP("hud_buff_strip", "Active Buff Strip", "Held Special Buff & Overcharge stack display", ForgeElementLayer.HUD, listOf(
  "Hex Tactical" to "Obsidian buff strip with glowing cyan-teal slot frames",
  "Gold Overcharge" to "High-contrast buff strip with amber-gold upgrade borders",
  "Violet Foundry" to "Sleek indigo buff bar with amethyst slot highlights",
  "Compact Ghost" to "Minimal translucent buff strip with crisp thin outlines",
 )),
 HUD_KILL_FEED("hud_kill_feed", "Tactical Kill Feed", "Top-right elimination & bounty payout log", ForgeElementLayer.HUD, listOf(
  "Clean Obsidian" to "Crisp dark obsidian kill cards with cyan and crimson accents",
  "Bounty Gold" to "Elimination feed highlighted with warm gold bounty trim",
  "Ghost Feed" to "Ultra-light translucent kill log pills with zero visual clutter",
  "Cyber Violet" to "Sharp neon-violet bordered combat log entries",
 )),
 HUD_LEFT_STICK("hud_left_stick", "Left Thruster Stick", "Newtonian flight & afterburner thumbstick ring", ForgeElementLayer.HUD, listOf(
  "Cyan Gimbal" to "Precision cyan thruster gimbal ring with afterburner outer arc",
  "Minimal Ring" to "Subtle low-opacity flight ring with crisp center puck",
  "Amber Thruster" to "High-visibility amber-gold flight stick with bright core",
  "Emerald Vector" to "Bio-teal vector flight ring with glowing directional ticks",
 )),
 HUD_FIRE_STICK("hud_fire_stick", "Right Aim & Fire Stick", "360° turret aim & primary trigger stick", ForgeElementLayer.HUD, listOf(
  "Crimson Reticle" to "Tactical crimson-cyan weapon aim ring with precision crosshair",
  "Cyan Lance" to "Electric-cyan turret fire ring with glowing white reticle",
  "Gold Ordnance" to "Sovereign gold weapon gimbal with crisp degree marks",
  "Ghost Reticle" to "Translucent pro-grade aim stick with razor-thin ring",
 )),
 HUD_SECONDARY_BTN("hud_secondary_btn", "Secondary Ability Button", "Ship class bomb, cloak, dash or Stasis trigger", ForgeElementLayer.HUD, listOf(
  "Amber Ordnance" to "Beveled obsidian ability button with blazing amber-gold ring",
  "Violet Class" to "Amethyst-trimmed secondary trigger with bright magenta glow",
  "Crimson Strike" to "Heavy crimson warhead trigger button with bold border",
  "Cyan Pulse" to "Sleek electric-cyan ability button with clean cooldown arc",
 )),
 HUD_SPECIAL_BUFF_BTN("hud_special_buff_btn", "Special Buff Trigger", "Crate-equipped tactical weapon activation button", ForgeElementLayer.HUD, listOf(
  "Emerald Crate" to "Vibrant teal-emerald Special Buff button with hexagonal rim",
  "Solar Nova" to "Radiant orange-gold Special Buff trigger with bright halo",
  "Rift Violet" to "Glowing violet Special Buff button with crisp white glyph frame",
  "Minimal Tactician" to "Low-glare dark slate buff trigger with sharp cyan border",
 )),
 HUD_REPEL_BTN("hud_repel_btn", "Repel Shockwave Button", "360° defensive Repel pulse button", ForgeElementLayer.HUD, listOf(
  "Shockwave Cyan" to "Concentric electric-cyan Repel shockwave button with bright rim",
  "Aegis Gold" to "Golden defensive shockwave trigger with radiant pulse ring",
  "Violet Push" to "Gravimetric violet Repel button with twin wave rings",
  "Clean Ice" to "Crisp glacier-blue Repel button with subtle translucent fill",
 )),
 HUD_BRAKE_BTN("hud_brake_btn", "Retro-Brake Button", "Manual Newtonian velocity damper button", ForgeElementLayer.HUD, listOf(
  "Titanium Brake" to "Crisp slate-cyan retro-brake damper button with thruster chevrons",
  "Amber Caution" to "High-contrast amber retro-brake button with bold rim",
  "Crimson Stop" to "Sharp crimson velocity anchor button with bright core",
  "Ghost Damper" to "Subtle translucent brake button with clean sky-blue outline",
 )),
 HUD_MULTI_BTN("hud_multi_btn", "Multi-Shot Toggle", "Diverging multi-barrel fire toggle button", ForgeElementLayer.HUD, listOf(
  "Gold Trident" to "Golden triple-beam Multi-Shot toggle with obsidian housing",
  "Cyan Spread" to "Electric-cyan diverging beam button with glowing border",
  "Magenta Volley" to "Neon-magenta multi-barrel toggle with high-contrast rim",
  "Minimal Toggle" to "Compact dark slate Multi-Shot toggle with thin gold trim",
 )),
 HUD_SPECTATOR_BOX("hud_spectator_box", "Teammate Spectator Switcher", "Living ally camera cycling HUD panel", ForgeElementLayer.HUD, listOf(
  "Command Link" to "Obsidian tactical spectator switcher with cyan uplink border",
  "Squad Gold" to "Armada gold-trimmed ally camera bar with crisp arrows",
  "Violet Feed" to "Deep-indigo spectator telemetry pill with violet glow",
  "Ghost Uplink" to "Low-profile translucent spectator switcher pill",
 )),
 HUD_WARNING_BANNER("hud_warning_banner", "Tactical Alert Banner", "Safe Zone lockout & AFK Flux drain warning pill", ForgeElementLayer.HUD, listOf(
  "Crimson Alert" to "High-contrast obsidian warning pill with pulsing crimson-amber border",
  "Solar Caution" to "Bright amber-gold tactical alert banner with clean dark backing",
  "Cyan Notice" to "Cool electric-cyan status alert pill with crisp rim",
  "Compact Alert" to "Sleek compact warning pill with subtle red edge",
 )),
 CURRENCY_FLUX_ICON("currency_flux_icon", "Swirling Plasma Flux Toroid", "3D tri-arc magnetic fusion Flux coil icon", ForgeElementLayer.HUD, listOf(
  "Canonical Toroid" to "3D swirling cyan-sapphire plasma toroid with counter-rotating magnetic containment arcs",
  "Overcharged Coil" to "Radiant mint-teal tri-arc fusion Flux coil with intense plasma core",
  "Cobalt Vortex" to "Deep royal-blue swirling magnetic Flux toroid with icy plasma filaments",
  "Golden Fusion" to "Solar-gold and cyan hybrid magnetic fusion toroid with bright core",
 )),
 CURRENCY_SHARD_ICON("currency_shard_icon", "Rift Shard Crystal Icon", "3D faceted amethyst Rift Shard spire icon", ForgeElementLayer.HUD, listOf(
  "Amethyst Spire" to "3D faceted amethyst crystal spire emerging from a cyan-violet Rift ring",
  "Rose Quartz" to "Luminous magenta-rose crystal cluster with bright white ridges",
  "Void Sapphire" to "Deep indigo-cyan Rift crystal spire with glowing teal vortex",
  "Sovereign Gem" to "Regal violet-gold crystal shard cluster with intense aura",
 ));

 companion object {
  fun fromKey(rawKey: String?): ForgeElementTarget? {
   if (rawKey.isNullOrBlank()) return null
   val trimmed = rawKey.trim()
   return entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) || it.name.equals(trimmed, ignoreCase = true) }
  }
 }
}

data class VisualElementRecipe(
 val targetKey: String,
 val primaryHex: String = "#040914",
 val trimHex: String = "#38BDF8",
 val glowHex: String = "#00F5D4",
 val geometryStyle: Int = 0,
 val pixelScale: Float = 1.0f,
 val borderWeight: Float = 1.0f,
 val glowIntensity: Float = 1.0f,
 val surfaceOpacity: Float = 0.90f,
 val rimLightDepth: Float = 0.70f,
 val detailDensity: Float = 0.70f,
 val shaderTurbulence: Float = 0.75f,
 val promptUsed: String = "Factory Default",
) {
 val primaryColorInt: Int get() = parseHexColorSafe(primaryHex, 0xFF040914.toInt())
 val trimColorInt: Int get() = parseHexColorSafe(trimHex, 0xFF38BDF8.toInt())
 val glowColorInt: Int get() = parseHexColorSafe(glowHex, 0xFF00F5D4.toInt())

 val isFactoryDefault: Boolean
  get() {
   val target = ForgeElementTarget.fromKey(targetKey) ?: return false
   val def = defaultFor(target)
   return primaryHex.equals(def.primaryHex, ignoreCase = true) &&
    trimHex.equals(def.trimHex, ignoreCase = true) &&
    glowHex.equals(def.glowHex, ignoreCase = true) &&
    geometryStyle == def.geometryStyle &&
    abs(pixelScale - def.pixelScale) < 0.005f &&
    abs(borderWeight - def.borderWeight) < 0.005f &&
    abs(glowIntensity - def.glowIntensity) < 0.005f &&
    abs(surfaceOpacity - def.surfaceOpacity) < 0.005f &&
    abs(rimLightDepth - def.rimLightDepth) < 0.005f &&
    abs(detailDensity - def.detailDensity) < 0.005f &&
    abs(shaderTurbulence - def.shaderTurbulence) < 0.005f
  }

 fun toJson(): JSONObject = JSONObject().apply {
  put("targetKey", targetKey)
  put("primaryHex", normalizeHexString(primaryHex, "#040914"))
  put("trimHex", normalizeHexString(trimHex, "#38BDF8"))
  put("glowHex", normalizeHexString(glowHex, "#00F5D4"))
  put("geometryStyle", geometryStyle.coerceIn(0, 3))
  put("pixelScale", ((pixelScale.coerceIn(0.50f, 2.00f) * 100f).roundToInt() / 100.0))
  put("borderWeight", ((borderWeight.coerceIn(0.40f, 2.50f) * 100f).roundToInt() / 100.0))
  put("glowIntensity", ((glowIntensity.coerceIn(0.00f, 2.50f) * 100f).roundToInt() / 100.0))
  put("surfaceOpacity", ((surfaceOpacity.coerceIn(0.15f, 1.00f) * 100f).roundToInt() / 100.0))
  put("rimLightDepth", ((rimLightDepth.coerceIn(0.00f, 1.50f) * 100f).roundToInt() / 100.0))
  put("detailDensity", ((detailDensity.coerceIn(0.00f, 1.50f) * 100f).roundToInt() / 100.0))
  put("shaderTurbulence", ((shaderTurbulence.coerceIn(0.00f, 2.00f) * 100f).roundToInt() / 100.0))
  put("promptUsed", promptUsed.take(120))
 }

 companion object {
  fun parseHexColorSafe(hex: String?, fallback: Int): Int {
   if (hex.isNullOrBlank()) return fallback
   val cleaned = if (hex.trim().startsWith("#")) hex.trim() else "#${hex.trim()}"
   return try { Color.parseColor(cleaned) } catch (_: Exception) { fallback }
  }

  fun normalizeHexString(hex: String?, fallback: String): String {
   val parsed = parseHexColorSafe(hex, parseHexColorSafe(fallback, 0xFF38BDF8.toInt()))
   return String.format(Locale.US, "#%02X%02X%02X", Color.red(parsed), Color.green(parsed), Color.blue(parsed))
  }

  fun defaultFor(target: ForgeElementTarget): VisualElementRecipe {
   val k = target.key
   return when (target) {
    ForgeElementTarget.SAFE_ZONE_WALL -> VisualElementRecipe(k, "#040914", "#38BDF8", "#00F5D4", surfaceOpacity = 0.92f, rimLightDepth = 0.75f, detailDensity = 0.70f)
    ForgeElementTarget.SAFE_ZONE_PAD -> VisualElementRecipe(k, "#0F172A", "#38BDF8", "#00F5D4", surfaceOpacity = 0.96f, rimLightDepth = 0.85f, detailDensity = 0.80f)
    ForgeElementTarget.PLACED_STASIS_BARRIER -> VisualElementRecipe(k, "#040914", "#38BDF8", "#00F5D4", surfaceOpacity = 0.94f, rimLightDepth = 0.80f, detailDensity = 0.75f)
    ForgeElementTarget.OBSTACLE_ORBITAL_STATION -> VisualElementRecipe(k, "#040914", "#1E3A5F", "#00F5D4", surfaceOpacity = 0.98f, rimLightDepth = 0.80f, detailDensity = 0.75f)
    ForgeElementTarget.OBSTACLE_ASTEROID_STATION -> VisualElementRecipe(k, "#060D1C", "#38BDF8", "#00F5D4", surfaceOpacity = 0.98f, rimLightDepth = 0.80f, detailDensity = 0.75f)
    ForgeElementTarget.OBSTACLE_SPACE_ASTEROID -> VisualElementRecipe(k, "#040914", "#1E3A5F", "#00F5D4", surfaceOpacity = 0.98f, rimLightDepth = 0.82f, detailDensity = 0.75f)
    ForgeElementTarget.OBSTACLE_FLOATING_DEBRIS -> VisualElementRecipe(k, "#040914", "#38BDF8", "#00F5D4", surfaceOpacity = 0.96f, rimLightDepth = 0.75f, detailDensity = 0.70f)
    ForgeElementTarget.ARENA_PERIMETER_RING -> VisualElementRecipe(k, "#040914", "#38BDF8", "#00F5D4", surfaceOpacity = 0.90f, rimLightDepth = 0.70f, detailDensity = 0.70f)
    ForgeElementTarget.BACKGROUND_NEBULA -> VisualElementRecipe(k, "#02050B", "#0E7490", "#312E81", surfaceOpacity = 0.88f, rimLightDepth = 0.60f, detailDensity = 0.70f)
    ForgeElementTarget.RIFT_PLASMA -> VisualElementRecipe(k, "#EF4444", "#D946EF", "#FFE4E6", surfaceOpacity = 0.95f, rimLightDepth = 0.85f, detailDensity = 0.75f)
    ForgeElementTarget.RIFT_REPULSOR -> VisualElementRecipe(k, "#38BDF8", "#BAE6FD", "#FFFFFF", surfaceOpacity = 0.95f, rimLightDepth = 0.85f, detailDensity = 0.75f)
    ForgeElementTarget.RIFT_GRAVITY -> VisualElementRecipe(k, "#A855F7", "#6366F1", "#E9D5FF", surfaceOpacity = 0.95f, rimLightDepth = 0.85f, detailDensity = 0.75f)
    ForgeElementTarget.RIFT_PHASE_PAIR -> VisualElementRecipe(k, "#2DD4BF", "#38BDF8", "#F0FDFA", surfaceOpacity = 0.95f, rimLightDepth = 0.85f, detailDensity = 0.75f)
    ForgeElementTarget.SUPPLY_CRATE -> VisualElementRecipe(k, "#081120", "#10B981", "#00F5D4", surfaceOpacity = 0.94f, rimLightDepth = 0.80f, detailDensity = 0.75f)
    ForgeElementTarget.PROJECTILE_PRIMARY_BOLT -> VisualElementRecipe(k, "#38BDF8", "#E0F2FE", "#00F5D4", surfaceOpacity = 0.96f, rimLightDepth = 0.75f, detailDensity = 0.70f)
    ForgeElementTarget.PROJECTILE_PROXIMITY_MINE -> VisualElementRecipe(k, "#0B1324", "#F59E0B", "#EF4444", surfaceOpacity = 0.95f, rimLightDepth = 0.80f, detailDensity = 0.75f)
    ForgeElementTarget.ECLIPSE_STASIS_BUBBLE -> VisualElementRecipe(k, "#3B0764", "#EC4899", "#00F5D4", surfaceOpacity = 0.78f, rimLightDepth = 0.80f, detailDensity = 0.75f)
    ForgeElementTarget.FLUX_CORE_ORB -> VisualElementRecipe(k, "#F59E0B", "#FACC15", "#FEF08A", surfaceOpacity = 0.98f, rimLightDepth = 0.90f, detailDensity = 0.80f)
    ForgeElementTarget.STASIS_BRIG_JAIL -> VisualElementRecipe(k, "#061020", "#38BDF8", "#EF4444", surfaceOpacity = 0.90f, rimLightDepth = 0.80f, detailDensity = 0.75f)
    ForgeElementTarget.SHIP_AURA_LABELS -> VisualElementRecipe(k, "#06101E", "#38BDF8", "#00F5D4", surfaceOpacity = 0.90f, rimLightDepth = 0.70f, detailDensity = 0.70f)
    ForgeElementTarget.HUD_SECONDARY_BTN -> VisualElementRecipe(k, "#0A1326", "#F59E0B", "#FBBF24", surfaceOpacity = 0.88f)
    ForgeElementTarget.HUD_SPECIAL_BUFF_BTN -> VisualElementRecipe(k, "#081624", "#10B981", "#2DD4BF", surfaceOpacity = 0.88f)
    ForgeElementTarget.HUD_REPEL_BTN -> VisualElementRecipe(k, "#071526", "#00F5D4", "#38BDF8", surfaceOpacity = 0.88f)
    ForgeElementTarget.HUD_MULTI_BTN -> VisualElementRecipe(k, "#0A1424", "#FACC15", "#FDE047", surfaceOpacity = 0.88f)
    ForgeElementTarget.HUD_WARNING_BANNER -> VisualElementRecipe(k, "#160912", "#EF4444", "#F59E0B", surfaceOpacity = 0.92f)
    ForgeElementTarget.CURRENCY_FLUX_ICON -> VisualElementRecipe(k, "#0284C7", "#38BDF8", "#00F5D4", surfaceOpacity = 1.00f, rimLightDepth = 0.85f, detailDensity = 0.75f)
    ForgeElementTarget.CURRENCY_SHARD_ICON -> VisualElementRecipe(k, "#9333EA", "#E879F9", "#00F5D4", surfaceOpacity = 1.00f, rimLightDepth = 0.88f, detailDensity = 0.80f)
    else -> VisualElementRecipe(k, "#071022", "#38BDF8", "#00F5D4", surfaceOpacity = 0.86f, rimLightDepth = 0.70f, detailDensity = 0.70f)
   }
  }

  fun fromJson(obj: JSONObject): VisualElementRecipe {
   val key = obj.optString("targetKey", ForgeElementTarget.SAFE_ZONE_WALL.key)
   val fallbackTarget = ForgeElementTarget.fromKey(key) ?: ForgeElementTarget.SAFE_ZONE_WALL
   val def = defaultFor(fallbackTarget)
   return VisualElementRecipe(
    targetKey = fallbackTarget.key,
    primaryHex = normalizeHexString(obj.optString("primaryHex", def.primaryHex), def.primaryHex),
    trimHex = normalizeHexString(obj.optString("trimHex", def.trimHex), def.trimHex),
    glowHex = normalizeHexString(obj.optString("glowHex", def.glowHex), def.glowHex),
    geometryStyle = obj.optInt("geometryStyle", def.geometryStyle).coerceIn(0, 3),
    pixelScale = obj.optDouble("pixelScale", def.pixelScale.toDouble()).toFloat().coerceIn(0.50f, 2.00f),
    borderWeight = obj.optDouble("borderWeight", def.borderWeight.toDouble()).toFloat().coerceIn(0.40f, 2.50f),
    glowIntensity = obj.optDouble("glowIntensity", def.glowIntensity.toDouble()).toFloat().coerceIn(0.00f, 2.50f),
    surfaceOpacity = obj.optDouble("surfaceOpacity", def.surfaceOpacity.toDouble()).toFloat().coerceIn(0.15f, 1.00f),
    rimLightDepth = obj.optDouble("rimLightDepth", def.rimLightDepth.toDouble()).toFloat().coerceIn(0.00f, 1.50f),
    detailDensity = obj.optDouble("detailDensity", def.detailDensity.toDouble()).toFloat().coerceIn(0.00f, 1.50f),
    shaderTurbulence = obj.optDouble("shaderTurbulence", def.shaderTurbulence.toDouble()).toFloat().coerceIn(0.00f, 2.00f),
    promptUsed = obj.optString("promptUsed", def.promptUsed).ifBlank { def.promptUsed }.take(120),
   )
  }

  fun synthesizeFromPrompt(target: ForgeElementTarget, rawPrompt: String, variationSalt: Int = 0): VisualElementRecipe {
   val cleaned = rawPrompt.trim()
   val def = defaultFor(target)
   if (cleaned.isEmpty()) return def
   val lower = cleaned.lowercase(Locale.US)
   val hash = abs((lower + "#$variationSalt#${target.key}").hashCode())
   fun hasAny(vararg words: String): Boolean = words.any { lower.contains(it) }
   val isBrightRiftOrIcon = target in setOf(
    ForgeElementTarget.RIFT_PLASMA, ForgeElementTarget.RIFT_REPULSOR, ForgeElementTarget.RIFT_GRAVITY,
    ForgeElementTarget.RIFT_PHASE_PAIR, ForgeElementTarget.FLUX_CORE_ORB, ForgeElementTarget.PROJECTILE_PRIMARY_BOLT,
    ForgeElementTarget.CURRENCY_FLUX_ICON, ForgeElementTarget.CURRENCY_SHARD_ICON,
   )
   val (primary, trim, glow) = when {
    hasAny("gold", "sovereign", "imperial", "royal", "gilded", "amber", "solar", "sun") ->
     if (isBrightRiftOrIcon) Triple("#F59E0B", "#FACC15", "#FEF08A") else Triple("#120D05", "#FACC15", "#F59E0B")
    hasAny("crimson", "red", "blood", "magma", "lava", "volcanic", "molten", "hazard", "alert") ->
     if (isBrightRiftOrIcon) Triple("#EF4444", "#F97316", "#FFE4E6") else Triple("#14060A", "#EF4444", "#FB7185")
    hasAny("violet", "amethyst", "purple", "magenta", "phase", "void", "quantum", "singularity") ->
     if (isBrightRiftOrIcon) Triple("#A855F7", "#EC4899", "#F5D0FE") else Triple("#0B0618", "#A855F7", "#E879F9")
    hasAny("emerald", "green", "bio", "venom", "acid", "jade", "sonar") ->
     if (isBrightRiftOrIcon) Triple("#10B981", "#2DD4BF", "#D1FAE5") else Triple("#041410", "#10B981", "#00F5D4")
    hasAny("ice", "cryo", "glacier", "frost", "arctic", "diamond", "cobalt", "sapphire") ->
     if (isBrightRiftOrIcon) Triple("#0284C7", "#7DD3FC", "#F0F9FF") else Triple("#051122", "#7DD3FC", "#E0F2FE")
    hasAny("titanium", "gunmetal", "steel", "iron", "chrome", "basalt", "rock", "crater", "slate") ->
     if (isBrightRiftOrIcon) Triple("#475569", "#94A3B8", "#38BDF8") else Triple("#0B121E", "#64748B", "#38BDF8")
    hasAny("rose", "pink", "coral", "neon") ->
     if (isBrightRiftOrIcon) Triple("#EC4899", "#38BDF8", "#FCE7F3") else Triple("#140716", "#EC4899", "#00F5D4")
    hasAny("obsidian", "pitch", "black", "stealth", "ghost", "dark") ->
     if (isBrightRiftOrIcon) Triple("#0E7490", "#38BDF8", "#00F5D4") else Triple("#030710", "#1E3A5F", "#00F5D4")
    hasAny("cyan", "teal", "hard-light", "hardlight", "laser", "turquoise", "electric") ->
     if (isBrightRiftOrIcon) Triple("#00F5D4", "#38BDF8", "#E0F2FE") else Triple("#040914", "#38BDF8", "#00F5D4")
    else -> {
     val palettes = if (isBrightRiftOrIcon) arrayOf(
      Triple("#00F5D4", "#38BDF8", "#E0F2FE"), Triple("#A855F7", "#38BDF8", "#F5D0FE"),
      Triple("#F59E0B", "#FACC15", "#FEF08A"), Triple("#EF4444", "#D946EF", "#FFE4E6"),
     ) else arrayOf(
      Triple("#040914", "#38BDF8", "#00F5D4"), Triple("#090D1C", "#A855F7", "#00F5D4"),
      Triple("#0F0C06", "#F59E0B", "#38BDF8"), Triple("#051314", "#10B981", "#38BDF8"),
     )
     palettes[hash % palettes.size]
    }
   }
   val style = when {
    hasAny("solar", "wing", "array", "kite", "trident", "crystal", "geode") -> 1
    hasAny("crater", "boulder", "round", "octagon", "bastion", "sphere", "dome") -> 2
    hasAny("hollow", "minimal", "razor", "thin", "diamond", "frame", "lattice") -> 3
    variationSalt > 0 -> hash % 4
    else -> def.geometryStyle
   }
   val scale = when {
    hasAny("giant", "massive", "huge", "large") -> 1.25f
    hasAny("compact", "small", "tiny", "micro") -> 0.84f
    else -> def.pixelScale
   }
   val border = when {
    hasAny("thick", "heavy", "bold", "fortress", "armored", "bastion") -> 1.45f
    hasAny("thin", "razor", "hairline", "minimal", "sleek", "subtle") -> 0.68f
    else -> def.borderWeight
   }
   val glowAmt = when {
    hasAny("blazing", "intense", "radiant", "overcharged", "supernova", "neon", "bright") -> 1.65f
    hasAny("matte", "muted", "stealth", "low-glare", "minimal", "pure pitch") -> 0.42f
    else -> def.glowIntensity
   }
   val opacity = when {
    hasAny("ghost", "translucent", "transparent", "glass", "holo", "lightweight") -> 0.52f
    hasAny("solid", "opaque", "armored", "heavy", "obsidian") -> 0.96f
    else -> def.surfaceOpacity
   }
   val rimDepth = when {
    hasAny("3d", "bevel", "faceted", "craggy", "deep", "sculpted", "sunlit") -> 1.15f
    hasAny("flat", "clean", "minimal") -> 0.45f
    else -> def.rimLightDepth
   }
   val density = when {
    hasAny("ornate", "detailed", "intricate", "complex", "cratered", "lit", "windows", "conduits") -> 1.20f
    hasAny("clean", "minimal", "smooth", "pure", "simple") -> 0.38f
    else -> def.detailDensity
   }
   val turbulence = when {
    hasAny("storm", "tempest", "swirl", "chaotic", "turbulent", "arc", "lightning", "unstable", "vortex") -> 1.45f
    hasAny("calm", "still", "smooth", "minimal", "pure", "laminar") -> 0.28f
    variationSalt > 0 -> 0.45f + ((hash % 110) / 100f)
    else -> def.shaderTurbulence
   }
   return VisualElementRecipe(
    targetKey = target.key, primaryHex = primary, trimHex = trim, glowHex = glow, geometryStyle = style,
    pixelScale = scale.coerceIn(0.50f, 2.00f), borderWeight = border.coerceIn(0.40f, 2.50f),
    glowIntensity = glowAmt.coerceIn(0.00f, 2.50f), surfaceOpacity = opacity.coerceIn(0.15f, 1.00f),
    rimLightDepth = rimDepth.coerceIn(0.00f, 1.50f), detailDensity = density.coerceIn(0.00f, 1.50f),
    shaderTurbulence = turbulence.coerceIn(0.00f, 2.00f),
    promptUsed = cleaned.take(120),
   )
  }
 }
}

data class SyndicateCrestRecipe(
 val shieldShape: Int = 0,
 val emblemGlyph: Int = 0,
 val primaryHex: String = "#0B1528",
 val trimHex: String = "#FACC15",
 val glowHex: String = "#00F5D4",
 val promptUsed: String = "Sovereign Gold & Cyan Aegis",
) {
 val primaryColorInt: Int get() = VisualElementRecipe.parseHexColorSafe(primaryHex, 0xFF0B1528.toInt())
 val trimColorInt: Int get() = VisualElementRecipe.parseHexColorSafe(trimHex, 0xFFFACC15.toInt())
 val glowColorInt: Int get() = VisualElementRecipe.parseHexColorSafe(glowHex, 0xFF00F5D4.toInt())

 val shieldShapeLabel: String
  get() = when (shieldShape.coerceIn(0, 3)) {
   1 -> "Chevron Kite"; 2 -> "Octagon Bastion"; 3 -> "Diamond Sovereign"; else -> "Hexagon Aegis"
  }

 val emblemGlyphLabel: String
  get() = when (emblemGlyph.coerceIn(0, 5)) {
   1 -> "Twin Blades"; 2 -> "Crown Spire"; 3 -> "Vortex Eye"; 4 -> "Winged Falcon"; 5 -> "Tactician Visor"; else -> "Rift Singularity"
  }

 fun toJson(): JSONObject = JSONObject().apply {
  put("shieldShape", shieldShape.coerceIn(0, 3))
  put("emblemGlyph", emblemGlyph.coerceIn(0, 5))
  put("primaryHex", VisualElementRecipe.normalizeHexString(primaryHex, "#0B1528"))
  put("trimHex", VisualElementRecipe.normalizeHexString(trimHex, "#FACC15"))
  put("glowHex", VisualElementRecipe.normalizeHexString(glowHex, "#00F5D4"))
  put("promptUsed", promptUsed.take(120))
 }

 companion object {
  fun fromJson(obj: JSONObject?): SyndicateCrestRecipe {
   if (obj == null) return SyndicateCrestRecipe()
   return SyndicateCrestRecipe(
    shieldShape = obj.optInt("shieldShape", 0).coerceIn(0, 3),
    emblemGlyph = obj.optInt("emblemGlyph", 0).coerceIn(0, 5),
    primaryHex = VisualElementRecipe.normalizeHexString(obj.optString("primaryHex", "#0B1528"), "#0B1528"),
    trimHex = VisualElementRecipe.normalizeHexString(obj.optString("trimHex", "#FACC15"), "#FACC15"),
    glowHex = VisualElementRecipe.normalizeHexString(obj.optString("glowHex", "#00F5D4"), "#00F5D4"),
    promptUsed = obj.optString("promptUsed", "Sovereign Gold & Cyan Aegis").ifBlank { "Sovereign Gold & Cyan Aegis" }.take(120),
   )
  }

  fun synthesizeFromPrompt(rawPrompt: String, variationSalt: Int = 0): SyndicateCrestRecipe {
   val cleaned = rawPrompt.trim()
   if (cleaned.isEmpty()) return SyndicateCrestRecipe()
   val lower = cleaned.lowercase(Locale.US)
   val hash = abs((lower + "#$variationSalt").hashCode())
   fun hasAny(vararg w: String): Boolean = w.any { lower.contains(it) }
   val shape = when {
    hasAny("kite", "chevron", "shield", "knight", "herald") -> 1
    hasAny("octagon", "bastion", "fortress", "heavy", "iron") -> 2
    hasAny("diamond", "prism", "rhombus", "crystal") -> 3
    hasAny("hex", "aegis", "hive") -> 0
    else -> hash % 4
   }
   val glyph = when {
    hasAny("blade", "sword", "dagger", "sabre", "duel", "fang") -> 1
    hasAny("crown", "king", "sovereign", "royal", "empire", "monarch") -> 2
    hasAny("vortex", "eye", "rift", "singularity", "black hole", "abyss") -> 3
    hasAny("wing", "falcon", "hawk", "eagle", "valkyrie", "phoenix", "bird") -> 4
    hasAny("skull", "visor", "phantom", "reaper", "ghost", "helm") -> 5
    hasAny("star", "nova", "astral", "sun") -> 0
    else -> (hash / 5) % 6
   }
   val (primary, trim, glow) = when {
    hasAny("crimson", "blood", "red", "war", "wrath") -> Triple("#1A070C", "#EF4444", "#F59E0B")
    hasAny("violet", "amethyst", "void", "purple", "eclipse") -> Triple("#0F0722", "#E879F9", "#00F5D4")
    hasAny("emerald", "venom", "jade", "green", "viper") -> Triple("#051914", "#10B981", "#00F5D4")
    hasAny("ice", "frost", "glacier", "cyan", "arctic") -> Triple("#061528", "#38BDF8", "#E0F2FE")
    hasAny("obsidian", "black", "shadow", "stealth") -> Triple("#040812", "#94A3B8", "#00F5D4")
    else -> Triple("#0B1528", "#FACC15", "#00F5D4")
   }
   return SyndicateCrestRecipe(shape, glyph, primary, trim, glow, cleaned.take(120))
  }
 }
}

object RiftVisualForgeState {
 const val SHOW_DEV_FORGE: Boolean = true
 private const val MAX_UNDO_PER_TARGET = 24

 var visualRevision by mutableIntStateOf(0)
 var pointLightEnabled by mutableStateOf(true)
 var pointLightTestOrbWorldPos by mutableStateOf<Offset?>(null)
 var dynamicLightIntensity by mutableFloatStateOf(1.0f)
 var projectileLightRadius by mutableFloatStateOf(1.0f)
 var ambientSpaceDarkness by mutableFloatStateOf(0.0f)
 var activeSyndicateCrestRecipe by mutableStateOf(SyndicateCrestRecipe())
 val activeRecipes = mutableStateMapOf<String, VisualElementRecipe>()

 private data class TargetUndoSnapshot(
  val recipe: VisualElementRecipe,
  val calibrationValues: Map<String, Float>,
  val hudOffset: Offset?,
 )

 private val undoStacks = mutableMapOf<String, ArrayDeque<TargetUndoSnapshot>>()

 init {
  for (target in ForgeElementTarget.entries) {
   activeRecipes[target.key] = VisualElementRecipe.defaultFor(target)
  }
 }

 fun getRecipe(target: ForgeElementTarget): VisualElementRecipe =
  activeRecipes[target.key] ?: VisualElementRecipe.defaultFor(target).also { activeRecipes[target.key] = it }

 fun getRecipeByKey(targetKey: String): VisualElementRecipe =
  getRecipe(ForgeElementTarget.fromKey(targetKey) ?: ForgeElementTarget.SAFE_ZONE_WALL)

 fun updateRecipe(target: ForgeElementTarget, recipe: VisualElementRecipe, recordUndo: Boolean = true, context: Context? = null) {
  if (recordUndo) pushUndoSnapshotForTarget(target)
  activeRecipes[target.key] = recipe.copy(targetKey = target.key)
  RiftSpriteBaker.invalidateVisualTarget(target)
  if (context != null) RiftCalibrationState.savePersistedLayout(context)
  visualRevision++
 }

 fun reforgeTargetFromPrompt(target: ForgeElementTarget, prompt: String, variationSalt: Int = 0, recordUndo: Boolean = true, context: Context? = null): VisualElementRecipe {
  val synthesized = VisualElementRecipe.synthesizeFromPrompt(target, prompt, variationSalt)
  updateRecipe(target, synthesized, recordUndo = recordUndo, context = context)
  return synthesized
 }

 fun updateSyndicateCrest(recipe: SyndicateCrestRecipe, context: Context? = null) {
  activeSyndicateCrestRecipe = recipe
  if (context != null) RiftCalibrationState.savePersistedLayout(context)
  visualRevision++
 }

 fun reforgeSyndicateCrestFromPrompt(prompt: String, variationSalt: Int = 0, context: Context? = null): SyndicateCrestRecipe {
  val forged = SyndicateCrestRecipe.synthesizeFromPrompt(prompt, variationSalt)
  activeSyndicateCrestRecipe = forged
  if (context != null) RiftCalibrationState.savePersistedLayout(context)
  visualRevision++
  return forged
 }

 fun getHudOffsetForTarget(target: ForgeElementTarget): Offset? = when (target) {
  ForgeElementTarget.HUD_TOP_BAR -> RiftCalibrationState.topBarOffset
  ForgeElementTarget.HUD_NEXUS_BUTTON -> RiftCalibrationState.nexusBtnOffset
  ForgeElementTarget.HUD_MINIMAP_CONSOLE -> RiftCalibrationState.bottomConsoleOffset
  ForgeElementTarget.HUD_BUFF_STRIP -> RiftCalibrationState.buffBarOffset
  ForgeElementTarget.HUD_KILL_FEED -> RiftCalibrationState.killLogOffset
  ForgeElementTarget.HUD_LEFT_STICK -> RiftCalibrationState.leftStickOffset
  ForgeElementTarget.HUD_FIRE_STICK -> RiftCalibrationState.fireStickOffset
  ForgeElementTarget.HUD_SECONDARY_BTN -> RiftCalibrationState.bombBtnOffset
  ForgeElementTarget.HUD_SPECIAL_BUFF_BTN -> RiftCalibrationState.burstBtnOffset
  ForgeElementTarget.HUD_REPEL_BTN -> RiftCalibrationState.repelBtnOffset
  ForgeElementTarget.HUD_BRAKE_BTN -> RiftCalibrationState.brakeBtnOffset
  ForgeElementTarget.HUD_MULTI_BTN -> RiftCalibrationState.multiBtnOffset
  ForgeElementTarget.HUD_SPECTATOR_BOX -> RiftCalibrationState.spectatorBoxOffset
  ForgeElementTarget.HUD_WARNING_BANNER -> RiftCalibrationState.idleWarningBannerOffset
  else -> null
 }

 fun setHudOffsetForTarget(target: ForgeElementTarget, offset: Offset, recordUndo: Boolean = false) {
  if (recordUndo) pushUndoSnapshotForTarget(target)
  when (target) {
   ForgeElementTarget.HUD_TOP_BAR -> RiftCalibrationState.topBarOffset = offset
   ForgeElementTarget.HUD_NEXUS_BUTTON -> RiftCalibrationState.nexusBtnOffset = offset
   ForgeElementTarget.HUD_MINIMAP_CONSOLE -> RiftCalibrationState.bottomConsoleOffset = offset
   ForgeElementTarget.HUD_BUFF_STRIP -> RiftCalibrationState.buffBarOffset = offset
   ForgeElementTarget.HUD_KILL_FEED -> RiftCalibrationState.killLogOffset = offset
   ForgeElementTarget.HUD_LEFT_STICK -> RiftCalibrationState.leftStickOffset = offset
   ForgeElementTarget.HUD_FIRE_STICK -> RiftCalibrationState.fireStickOffset = offset
   ForgeElementTarget.HUD_SECONDARY_BTN -> RiftCalibrationState.bombBtnOffset = offset
   ForgeElementTarget.HUD_SPECIAL_BUFF_BTN -> RiftCalibrationState.burstBtnOffset = offset
   ForgeElementTarget.HUD_REPEL_BTN -> RiftCalibrationState.repelBtnOffset = offset
   ForgeElementTarget.HUD_BRAKE_BTN -> RiftCalibrationState.brakeBtnOffset = offset
   ForgeElementTarget.HUD_MULTI_BTN -> RiftCalibrationState.multiBtnOffset = offset
   ForgeElementTarget.HUD_SPECTATOR_BOX -> RiftCalibrationState.spectatorBoxOffset = offset
   ForgeElementTarget.HUD_WARNING_BANNER -> RiftCalibrationState.idleWarningBannerOffset = offset
   else -> Unit
  }
  RiftCalibrationState.calibrationRevision++
  visualRevision++
 }

 fun getCalibrationDescriptorsForTarget(target: ForgeElementTarget): List<CalibrationParamDescriptor> {
  val boundKeys: List<String> = when (target) {
   ForgeElementTarget.SAFE_ZONE_WALL -> listOf("safeZoneScale", "safeZoneReentryLockoutTime", "safeZoneRepelForce", "wallElasticity")
   ForgeElementTarget.SAFE_ZONE_PAD -> listOf("safeZoneScale", "safeZoneRechargeMult", "safeZoneIdleLimitSec", "exitPhaseShieldSec")
   ForgeElementTarget.PLACED_STASIS_BARRIER -> listOf("stasisBarrierDuration", "stasisBarrierLength", "wallElasticity")
   ForgeElementTarget.OBSTACLE_ORBITAL_STATION, ForgeElementTarget.OBSTACLE_ASTEROID_STATION,
   ForgeElementTarget.OBSTACLE_SPACE_ASTEROID, ForgeElementTarget.OBSTACLE_FLOATING_DEBRIS,
   ForgeElementTarget.ARENA_PERIMETER_RING -> listOf("wallElasticity")
   ForgeElementTarget.BACKGROUND_NEBULA -> listOf("publicMatchCycleLimitSec", "hitStopDurationSec")
   ForgeElementTarget.RIFT_PLASMA -> listOf("anomalyScale", "riftLifespanSec", "riftPhysicalRadius", "plasmaRiftDamagePct")
   ForgeElementTarget.RIFT_REPULSOR -> listOf("anomalyScale", "riftLifespanSec", "riftPhysicalRadius", "repulsorFieldRadius", "repulsorBaseForce")
   ForgeElementTarget.RIFT_GRAVITY -> listOf("anomalyScale", "riftLifespanSec", "riftPhysicalRadius", "gravityPullRadius", "gravityPullForce", "gravityStopImmunitySec")
   ForgeElementTarget.RIFT_PHASE_PAIR -> listOf("anomalyScale", "riftLifespanSec", "riftPhysicalRadius", "phaseRiftCooldownSec")
   ForgeElementTarget.SUPPLY_CRATE -> listOf("crateScale", "buffCapFfa", "buffRespawnFrequency", "buffBaseRespawnDelay", "cratePickupBounty", "fullChargeCrateBounty")
   ForgeElementTarget.PROJECTILE_PRIMARY_BOLT -> listOf("projectileScale", "aimTelegraphScale", "crateMultiShotAmmo")
   ForgeElementTarget.PROJECTILE_PROXIMITY_MINE -> listOf("projectileScale", "maxMinesPerShip", "proxFuseRadius")
   ForgeElementTarget.ECLIPSE_STASIS_BUBBLE -> listOf("interdictorBubbleRadius", "interdictorBubbleDuration", "interdictorBubbleCooldown")
   ForgeElementTarget.FLUX_CORE_ORB -> listOf("fluxCoreBountyPerSec", "fluxCoreEjectSpeed")
   ForgeElementTarget.STASIS_BRIG_JAIL -> listOf("stasisBrigReactorMaxHp")
   ForgeElementTarget.SHIP_AURA_LABELS -> listOf("shipScale", "labelDistanceOffset", "hvtBountyThreshold", "bountyKillBonusPct")
   ForgeElementTarget.HUD_TOP_BAR -> listOf("topBarScale", "skirmishKillTargetPerRound")
   ForgeElementTarget.HUD_NEXUS_BUTTON -> listOf("topBarScale")
   ForgeElementTarget.HUD_MINIMAP_CONSOLE -> listOf("bottomConsoleScale")
   ForgeElementTarget.HUD_BUFF_STRIP -> listOf("buffBarScale", "overchargeSurgePctPerStack")
   ForgeElementTarget.HUD_KILL_FEED -> listOf("killLogScale", "bountyKillBonusPct")
   ForgeElementTarget.HUD_LEFT_STICK -> listOf("leftStickScale", "gamepadDeadzoneLeft", "afterburnerThrustMult", "afterburnerDrainPerSec")
   ForgeElementTarget.HUD_FIRE_STICK -> listOf("combatArcScale", "aimTelegraphScale", "gamepadDeadzoneRight")
   ForgeElementTarget.HUD_SECONDARY_BTN -> listOf("combatArcScale", "cloakDrainPerSec", "empPulseRadius", "empFluxDrainPct", "empFreezeSec", "phaseDashImpulse")
   ForgeElementTarget.HUD_SPECIAL_BUFF_BTN -> listOf("combatArcScale", "seekerDamage", "seekerSpeed", "novaBoltDamage", "shatterPelletDamage", "hyperSurgeSpeedMult")
   ForgeElementTarget.HUD_REPEL_BTN -> listOf("combatArcScale", "repelCost", "repelRadius", "repelPushForce")
   ForgeElementTarget.HUD_BRAKE_BTN -> listOf("combatArcScale", "retroBrakeDamping")
   ForgeElementTarget.HUD_MULTI_BTN -> listOf("combatArcScale", "crateMultiShotAmmo")
   ForgeElementTarget.HUD_SPECTATOR_BOX -> listOf("respawnDelaySec")
   ForgeElementTarget.HUD_WARNING_BANNER -> listOf("idleWarningBannerScale", "playfieldIdleLimitSec", "playfieldIdleDrainPctPerSec")
   ForgeElementTarget.CURRENCY_FLUX_ICON, ForgeElementTarget.CURRENCY_SHARD_ICON -> listOf("bottomConsoleScale", "topBarScale")
  }
  val byKey = RiftCalibrationState.buildAllParameterDescriptors().associateBy { it.key }
  val lighting = if (target == ForgeElementTarget.BACKGROUND_NEBULA) buildDynamicLightingDescriptors() else emptyList()
  return lighting + boundKeys.mapNotNull { byKey[it] }
 }

 fun buildDynamicLightingDescriptors(): List<CalibrationParamDescriptor> = listOf(
  CalibrationParamDescriptor(
   "forge_dynamicLightIntensity", 0, 0, "Dynamic Point-Light Intensity", "x",
   "Scales real-time radial light cast by plasma bolts, Rifts, and test orbs.",
   0.0f, 2.5f, 0.05f, 2, { dynamicLightIntensity },
   { dynamicLightIntensity = it.coerceIn(0.0f, 2.5f); visualRevision++ },
  ),
  CalibrationParamDescriptor(
   "forge_projectileLightRadius", 0, 0, "Projectile Light Radius", "x",
   "Scales illumination radius around flying bolts, bombs, and Flux Core.",
   0.3f, 2.5f, 0.05f, 2, { projectileLightRadius },
   { projectileLightRadius = it.coerceIn(0.3f, 2.5f); visualRevision++ },
  ),
  CalibrationParamDescriptor(
   "forge_ambientSpaceDarkness", 0, 0, "Ambient Void Contrast", "%",
   "Deepens ambient space darkness so dynamic point lights pop with higher contrast.",
   0.0f, 0.85f, 0.05f, 2, { ambientSpaceDarkness },
   { ambientSpaceDarkness = it.coerceIn(0.0f, 0.85f); visualRevision++ },
  ),
 )

 fun getCombatSandboxDescriptors(shipIdFilter: Int = 0): List<CalibrationParamDescriptor> {
  val all = RiftCalibrationState.buildAllParameterDescriptors()
  return if (shipIdFilter in 1..10) {
   all.filter { it.shipIdFilter == shipIdFilter || (it.tabIndex == 1 && it.shipIdFilter == 0) }
  } else {
   buildDynamicLightingDescriptors() + all.filter { it.shipIdFilter == 0 && it.tabIndex in setOf(1, 3, 4, 5, 6, 7) }
  }
 }

 fun pushUndoSnapshotForTarget(target: ForgeElementTarget) {
  val boundDescriptors = getCalibrationDescriptorsForTarget(target).filter { !it.key.startsWith("forge_") }
  val calSnapshot = LinkedHashMap<String, Float>()
  for (desc in boundDescriptors) calSnapshot[desc.key] = desc.getter()
  val snap = TargetUndoSnapshot(getRecipe(target), calSnapshot, getHudOffsetForTarget(target))
  val deque = undoStacks.getOrPut(target.key) { ArrayDeque() }
  if (deque.lastOrNull() == snap) return
  if (deque.size >= MAX_UNDO_PER_TARGET) deque.removeFirst()
  deque.addLast(snap)
  visualRevision++
 }

 fun canUndoTarget(target: ForgeElementTarget): Boolean = undoStacks[target.key]?.isNotEmpty() == true

 fun undoTarget(target: ForgeElementTarget, engine: RiftGameEngine? = null, context: Context? = null): Boolean {
  val deque = undoStacks[target.key] ?: return false
  val snap = deque.removeLastOrNull() ?: return false
  activeRecipes[target.key] = snap.recipe
  val byKey = RiftCalibrationState.buildAllParameterDescriptors().associateBy { it.key }
  for ((k, v) in snap.calibrationValues) byKey[k]?.setter?.invoke(v)
  if (snap.hudOffset != null) setHudOffsetForTarget(target, snap.hudOffset, recordUndo = false)
  RiftSpriteBaker.invalidateVisualTarget(target)
  RiftCalibrationState.applyCalibrationChangesLive(engine, context)
  visualRevision++
  return true
 }

 fun resetTargetToDefault(target: ForgeElementTarget, engine: RiftGameEngine? = null, context: Context? = null) {
  RiftCalibrationState.ensureBaselineCaptured()
  pushUndoSnapshotForTarget(target)
  activeRecipes[target.key] = VisualElementRecipe.defaultFor(target)
  if (target == ForgeElementTarget.BACKGROUND_NEBULA) {
   dynamicLightIntensity = 1.0f
   projectileLightRadius = 1.0f
   ambientSpaceDarkness = 0.0f
   pointLightEnabled = true
  }
  val boundDescriptors = getCalibrationDescriptorsForTarget(target).filter { !it.key.startsWith("forge_") }
  for (desc in boundDescriptors) {
   RiftCalibrationState.DEFAULT_GAME_CONFIG[desc.key]?.let { defVal -> desc.setter(defVal) }
  }
  if (getHudOffsetForTarget(target) != null) setHudOffsetForTarget(target, Offset.Zero, recordUndo = false)
  RiftSpriteBaker.invalidateVisualTarget(target)
  RiftCalibrationState.applyCalibrationChangesLive(engine, context)
  visualRevision++
 }

 fun resetAllVisualTargetsToDefault() {
  val hadCustomRecipes = activeRecipes.values.any { !it.isFactoryDefault }
  for (target in ForgeElementTarget.entries) activeRecipes[target.key] = VisualElementRecipe.defaultFor(target)
  pointLightEnabled = true
  pointLightTestOrbWorldPos = null
  dynamicLightIntensity = 1.0f
  projectileLightRadius = 1.0f
  ambientSpaceDarkness = 0.0f
  activeSyndicateCrestRecipe = SyndicateCrestRecipe()
  undoStacks.clear()
  if (hadCustomRecipes) {
   RiftSpriteBaker.invalidateAllVisualTargets()
  }
  visualRevision++
 }

 fun toJsonObject(): JSONObject {
  val recipesObj = JSONObject()
  for (target in ForgeElementTarget.entries) {
   val rec = getRecipe(target)
   if (!rec.isFactoryDefault) recipesObj.put(target.key, rec.toJson())
  }
  return JSONObject().apply {
   put("pointLightEnabled", pointLightEnabled)
   put("dynamicLightIntensity", ((dynamicLightIntensity * 100f).roundToInt() / 100.0))
   put("projectileLightRadius", ((projectileLightRadius * 100f).roundToInt() / 100.0))
   put("ambientSpaceDarkness", ((ambientSpaceDarkness * 100f).roundToInt() / 100.0))
   put("recipes", recipesObj)
  }
 }

 fun loadFromJsonObject(obj: JSONObject?) {
  val hadCustomBefore = activeRecipes.values.any { !it.isFactoryDefault }
  var loadedCustomNow = false
  for (target in ForgeElementTarget.entries) activeRecipes[target.key] = VisualElementRecipe.defaultFor(target)
  if (obj != null) {
   pointLightEnabled = obj.optBoolean("pointLightEnabled", true)
   dynamicLightIntensity = obj.optDouble("dynamicLightIntensity", 1.0).toFloat().coerceIn(0.0f, 2.5f)
   projectileLightRadius = obj.optDouble("projectileLightRadius", 1.0).toFloat().coerceIn(0.3f, 2.5f)
   ambientSpaceDarkness = obj.optDouble("ambientSpaceDarkness", 0.0).toFloat().coerceIn(0.0f, 0.85f)
   val recipesObj = obj.optJSONObject("recipes")
   if (recipesObj != null) {
    val keys = recipesObj.keys()
    while (keys.hasNext()) {
     val k = keys.next()
     val target = ForgeElementTarget.fromKey(k) ?: continue
     val recJson = recipesObj.optJSONObject(k) ?: continue
     val loadedRec = VisualElementRecipe.fromJson(recJson).copy(targetKey = target.key)
     activeRecipes[target.key] = loadedRec
     if (!loadedRec.isFactoryDefault) loadedCustomNow = true
    }
   }
  }
  if (hadCustomBefore || loadedCustomNow) {
   RiftSpriteBaker.invalidateAllVisualTargets()
  }
  visualRevision++
 }
}
