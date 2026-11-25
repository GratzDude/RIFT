# RIFT — Visual & Shader Architecture Specification
**Studio:** Threshold Dynamics Studio, LLC  
**Active Build Baseline:** Build #113 (`v1.0.113`)  
**Application ID / Namespace:** `com.game.rift` / `com.example`

---

## 1. Volumetric Spacetime Geometry & True-Centered Rift Cores
1. **Zero Rigid Lines, Spokes, Line-Attached Orbs, or Central Balls on Volumetric Entities:**
   - Playfield Rifts (`PLASMA_RIFT`, `REPULSOR_RIFT`, `GRAVITY_RIFT`, `PHASE_RIFT_A`, `PHASE_RIFT_B`), Unstable Merged Rifts, the `Rift Station` Safe-Zone perimeter, Eclipse Stasis containment domes, Stasis Brig jails, Rift Control zones, and the Outer Storm Border Wall **must never** render:
     - Solid jagged sawtooth perimeter polylines or polygon crag loops (`jaggedFractureRimPath`, `sharedFractureRimPath`).
     - Radial straight or curved spoke lines (including Safe-Zone radial stabilizer beams).
     - Orbs, dots, or circular nodes attached to lines or perimeter rings.
     - Central core balls/spheres (`throatR` solid circles), 6-node emitter circles, or circular meniscus droplet beads.
2. **True-Centered Rift Cores (`(cx, cy)` Lock):**
   - Every Playfield Rift and Unstable Merged Rift anchors its abyssal void throat (`0.0r .. 0.42r`) and volumetric storm-cloud focal center at the Rift's true geometric center `(cx, cy)`.
   - Never apply camera-relative or diagonal parallax offsets that shift the throat off-center inside the Rift.
   - At most, apply a subtle zero-mean organic breathing drift strictly bounded within `<= 0.025f * r`:
     - `dx = sin(t * 1.1f + seed) * 0.025f * r`
     - `dy = cos(t * 0.85f + seed) * 0.025f * r`
3. **2.5D Hard-Surface Entities Preserved:**
   - Crisp 2.5D 5-stop metallic and basalt styling applies strictly to `128x128` starfighter hulls, the `180px` (`5x` ship-scale) `Rift Station` hull structure, hexagonal `Supply Pods`, and `224x224` Space Obstacles (`ORBITAL_STATION`, `ASTEROID_STATION`, `CRYSTAL_ASTEROID`, `FLOATING_DEBRIS`), with zero solid-white ship hit-flashes.
   - All physical collision, swept weapon absorption, tidal crush, and convergence math in `RiftGameEngine.kt` remains untouched.

---

## 2. Volumetric Storm-Cloud Rifts, Fixed 4-Stop Palettes & Fractal Branching Lightning
1. **Volumetric Cloud Anatomy:**
   - Centered `#000000` -> `#02040A` abyssal void throat (`0.0r .. 0.42r`) smoothly feathered into multi-layered semi-translucent billowing storm clouds (`0.30r .. 1.08r`).
   - **API 33+ Hardware Canvas (`VOLUMETRIC_RIFT_AGSL`):** 3 octaves of Quintic-interpolated (`u * u * u * (u * (u * 6.0 - 15.0) + 10.0)`) domain-warped FBM cloud density inrotated 2D Cartesian coordinates, feathered smoothly at both inner and outer radii.
   - **API 24–32 & Offscreen Bitmap Icon Fallback:** Multi-layer feathered `RadialGradient` anisotropic cloud puffs rendered over a feathered `#000000` -> `#02040A` abyssal throat using pre-allocated `Paint`, `Path`, `Matrix`, and `RadialGradient` instances (zero per-frame heap allocations; all shared objects declared above `init {}` in `RiftSpriteBaker.kt` for `<clinit>` safety).
2. **Strict Fixed Color Palettes (Time-Shifting Strictly Within Range):**
   - `PLASMA_RIFT`: Deep Crimson `#991B1B` -> Hot Scarlet `#EF4444` -> Electric Magenta `#EC4899` -> Rose-White core `#FFE4E6`.
   - `REPULSOR_RIFT`: Deep Cobalt `#0369A1` -> Electric Sky `#0EA5E9` -> Ice Blue `#7DD3FC` -> White-Blue core `#F0F9FF`.
   - `GRAVITY_RIFT`: Deep Indigo `#3730A3` -> Royal Violet `#7C3AED` -> Luminous Purple `#A855F7` -> Lavender-White core `#F3E8FF`.
   - `PHASE_RIFT_A` / `PHASE_RIFT_B`: Deep Teal `#0F766E` -> Turquoise `#06B6D4` -> Electric Mint-Cyan `#00F5D4` -> Pale Cyan-White core `#CCFBF1`.
   - `Unstable Merged Rift`: Swirling multi-color volumetric storm clouds (`#00F5D4`, `#38BDF8`, `#A855F7`, `#EC4899`) centered at `(cx, cy)` over an abyssal throat with multi-hue perimeter branching lightning.
3. **Fractal Branching Perimeter Lightning (`0.68r .. 1.06r`):**
   - `5..8` organic midpoint-displaced fractal electric lightning arcs curving tangentially and diagonally across the `0.68r .. 1.06r` storm tear boundary, each spawning `2..3` thinner forked sub-branches.
   - Drawn with `Paint.Cap.ROUND` and `Paint.Join.ROUND` in two passes:
     1. Wide soft-alpha colored plasma glow (`~0.055r` primary trunk / `~0.034r` fork).
     2. Crisp bright inner filament (`~0.014r` primary trunk / `~0.009r` fork).
   - `riftAnomalyIconBitmaps` bakes directly from the unified volumetric + fractal lightning renderer for 1:1 parity across Tactical Codex, Overwatch, and Star Forge.

---

## 3. Smooth, Box-Free, Non-Snapping Volumetric Outer Storm Wall
1. **Continuous World-Space Coordinates & Quintic FBM (`STORM_BOUNDARY_AGSL`):**
   - Samples continuous 2D Cartesian world-space coordinates `(worldX * scale, worldY * scale)` relative to `uCenter` rather than polar angle projections or un-smoothed integer grid cells.
   - Uses 3 octaves of Quintic-interpolated (`u * u * u * (u * (u * 6.0 - 15.0) + 10.0)`) rotated domain-warped FBM clouds with smooth radial `smoothstep` feathering across the `280px` perimeter band.
2. **Monotonic Elapsed Time:**
   - Driven exclusively by monotonic elapsed seconds (`(SystemClock.elapsedRealtimeNanos() - startElapsedNanos) * 1e-9f`) with zero `time % period` wraps or match-timer resets, preventing any animation snap when flying near the wall.
3. **Proximity & Sheet Lightning:**
   - Renders organic midpoint-displaced sheet lightning inside the storm cloud band plus live `220px` proximity branching lightning arcs toward nearby living ships.

---

## 4. True Deep-Space Playfield Background & Release SHA-1 Telemetry
1. **Zero Background Lines or Grid Artifacts:**
   - Zero sector grid crosshairs, coordinate rings, radar guide lines, linear parallax velocity streaks, megastructure wireframe arcs/ribs, polygonal micro-rock wireframe rims, or rectangular bitmap wrap seams.
2. **Atmospheric Multi-Plane Deep Space:**
   - Deep `#010308` -> `#040917` abyssal space gradient backdrop.
   - Soft feathered multi-plane volumetric nebula and spiral galaxy clouds (`#1E1B4B` indigo, `#3B0764` cosmic violet, `#083344` deep cyan, and stardust wisps) wrapped with full radius padding so clouds never pop at viewport edges.
   - Crisp 3-plane parallax pinpoint stars (`0.04x`, `0.09x`, `0.16x`) with natural stellar hues (blue-white `#DBEAFE`, pure white `#F8FAFC`, warm-gold `#FEF08A`).
   - Distant softly-lit celestial bodies (crescent moon and spiral galaxy) in ultra-deep parallax (`0.032x`–`0.048x`).
3. **GitHub Release Keystore SHA-1 Readout (`.github/workflows/build-apk.yml`):**
   - Extracts `RIFT_APK_SHA1` from `debug.keystore` via `keytool -list -v`, exports it to `$GITHUB_ENV`, and prints it directly in the GitHub Release notes for one-step Firebase Console registration.
