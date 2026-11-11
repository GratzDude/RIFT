# Phase 4 Implementation Plan: Hangar 3x3 Comparison Matrix, Level-20 9th Custom Ship & AI Hull Forge

## 1. Hangar Overhaul: 3x3 All-Ships Side-by-Side Comparison Matrix (`HangarModal.kt`)
- **Simultaneous 3x3 Comparison Grid:**
  - Replace the single-column ship list with a **3x3 Side-by-Side Comparison Matrix** where **Cells 1–8** display `1-Viper` through `8-Manta` and **Cell 9** displays the **`9-Custom Modular Chassis`** (showing active loadout slot `9A`, `9B`, or `9C`).
  - Every cell in the 3x3 grid renders:
    - Pre-rendered **3D metallic ship sprite** (live-baked custom visual recipe for Ship 9).
    - Hull ID, Name, Foundry/Slot badge, and **Primary + Secondary Weapon tags**.
    - **5 Aligned Comparative Stat Bars** (`SPD` Top Speed, `THR` Thrust, `TRN` Turn Rate, `FLX` Max Flux, `RCH` Flux Recharge) normalized to the global arena ceiling so players can compare all 9 ships at a glance without hidden tabs.
    - Quick `[⚡ DEPLOY]` selection state and, on Cell 9, a **`[🛠️ CUSTOMIZE & AI FORGE]`** button to open the 9th Ship Modular Bay & AI Hull Forge.

## 2. Calibration Override & Level-20 9th Custom Ship ("Custom Modular Chassis") (`RiftCalibrationState.kt`, `RiftCalibrationDialog.kt`, `ShipSpecs.kt`, `LocalPilotPrefs.kt`, `RiftGameEngine.kt`)
- **Calibration Setting: Full 9th Ship Unlock (No Leveling Required):**
  - Add a persistent **`unlockFullNinthShipOverride: Boolean`** setting inside **`RiftCalibrationState.kt`** and expose a prominent **`[🛸 UNLOCK FULL 9TH SHIP (100 PTS)]`** toggle inside the **`[⚙ CALIBRATION]`** modal (`RiftCalibrationDialog.kt`).
  - When enabled in Calibration, the 9th Ship is immediately unlocked with the **full `100 Customization Points`**, unlocked loadout slots (`9A`, `9B`, `9C`), and free AI Hull Forge testing—without needing to level up your pilot.
  - Also keep a quick **`[DEV TEST: FULL 100 PTS OVERRIDE]`** shortcut inside the 9th Ship editor itself so you can toggle it from either Calibration or the Hangar.
- **Standard Level 20 Unlock Gate (When Calibration Override is Off):**
  - Locked for Pilot Levels `1–19` (`🔒 UNLOCKS AT PILOT LEVEL 20 — OR ENABLE IN ⚙ CALIBRATION`).
  - Unlocks at **Pilot Level 20** with `20 Customization Points` ready to allocate, scaling 1 point per level up to **`100 Points` at Level 100**.
- **70% Baseline Hull Floor & 100-Point Mathematical Balance Cap:**
  - **Baseline Hull Floor (0 Points Invested, ~70% of standard ships):**
    - `Top Speed`: `365 px/s` (Afterburner `515 px/s`)
    - `Thrust`: `610 N/s²`
    - `Turn Rate`: `3.5 rad/s`
    - `Max Flux`: `2,050❖`
    - `Flux Recharge`: `265❖/s`
  - **7 Customizable Attributes (Max `25 Points` per attribute, `100 Points` total budget at full capacity):**
    - Because a full 100-point build spans 7 attributes (max `25 pts` per stat = `175 pts` total capacity), a pilot can max at most 4 attributes (or build a balanced hybrid), and **even a 25/25 maxed attribute is strictly capped at the best-in-class ceiling of Ships 1–8**:
      1. **Top Speed (`0–25 pts`):** `365` → `540 px/s` (matches `1-Viper` ceiling)
      2. **Thrust (`0–25 pts`):** `610` → `920 N/s²` (matches `1-Viper` ceiling)
      3. **Turn Rate (`0–25 pts`):** `3.5` → `5.4 rad/s` (matches `1-Viper` ceiling)
      4. **Max Flux (`0–25 pts`):** `2,050❖` → `4,300❖` (matches `4-Goliath` class ceiling)
      5. **Flux Recharge (`0–25 pts`):** `265❖/s` → `410❖/s` (matches `3-Suppressor` ceiling)
      6. **Primary Weapon Archetype & Tuning (`0–25 pts`):** Choose between `SINGLE RAIL`, `TWIN PULSE`, or `TRI SPREAD`, with points scaling damage-per-flux efficiency up to standard gunship parity.
      7. **Secondary Weapon & Efficiency (`0–25 pts`):** Reduces secondary Flux cost and cooldown by up to `22%` within standard bomber parity.
- **All-Secondary Selector (Native Bombs/Mines OR Special Buff Attacks as Built-In Secondary):**
  - Choose any of the **8 Secondary Options** as the 9th ship's built-in secondary:
    - **Native Ordnance:** `BOUNCE BOMB`, `SIEGE BOMB`, `EMP BOMB`, `PROXIMITY BOMB`, `SPACE MINE` (or `NONE` for pure gunfighter bonus)
    - **Special Buff Attacks (Flux-Powered):** `SHRAPNEL BURST`, `3-WAVE CONE SHOTGUN`, `LOCK-ON BOUNCING MISSILE`
  - **Separate 4-Input Combat Cluster Preserved:**
    - Pressing the **3rd Input (Contextual Secondary Button)** fires the 9th ship's built-in secondary using **Ship Flux** (`bombCost` + `bombCooldown`), even when configured to `BURST`, `SHOTGUN`, or `MISSILE`.
    - Picking up playfield buff crates still populates the **4th Input (Special Buff Weapon Button)** using **weapon charges** independently.

## 3. Rift Shards Monetization: Loadout Slots (`9A`/`9B`/`9C`), Point Respec & AI Hull Forge (`RiftSpriteBaker.kt`, `HangarModal.kt`, `RiftModels.kt`)
- **3 Custom Loadout Slots (`Slot 9A`, `Slot 9B`, `Slot 9C`) & Point Respec:**
  - **`Slot 9A`** is unlocked by default at Level 20 (or when the Calibration override is active).
  - **`Slot 9B`** and **`Slot 9C`** can each be unlocked permanently for **`300❖ Rift Shards`** (or automatically unlocked while the Calibration override is active), saving independent stat allocations, weapon archetypes, and AI Hull visual recipes.
  - **`[🔄 RESET / RESPEC POINTS (50❖)]`** refunds all allocated points in the active slot back to the unspent pool.
- **AI Hull Forge (`<2KB` JSON Visual Recipe + Procedural 3D Metallic Baker + Guardrails):**
  - Prompt input (`"Describe your custom ship hull..."`) + **`[🎨 FORGE AI SHIP DESIGN]`** button (**First design FREE**; subsequent re-designs cost **`150❖ Rift Shards`**).
  - Generates a compact **`<2KB` JSON Visual Recipe**:
    `{ "hullName", "silhouette": "RAZOR_INTERCEPTOR | CRESCENT_SCYTHE | BASTION_DREADNOUGHT | MANTA_STINGRAY | X_VALKYRIE | PINCER_MANDIBLE", "wingSweep": -1.0..1.0, "chassisPlates": 1..4, "primaryHex", "trimHex", "coreGlowHex", "engineStyle": "TWIN_ION | QUAD_THRUSTER | VORTEX_RING" }`
  - **Visibility & Hitbox Guardrails (`RiftSpriteBaker.kt`):**
    - Clamps `primaryHex`, `trimHex`, and `coreGlowHex` to a minimum luminance threshold (`L >= 0.32`) so dark/black prompts can never create invisible camouflage ships against `#000000` space.
    - Enforces the exact **`36x36px` physical hitbox (`radius = 36f`)** and mandatory **Friendly Blue-Yellow / Hostile Red outer readability outline** across all 20 ships in multiplayer.
