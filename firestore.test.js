const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_pilot_1";
const BOB_UID = "bob_pilot_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function makeValidDossier(uid, now) {
  return {
    userId: uid,
    callsign: "VIPER_ACE",
    syndicateTag: "AEX",
    syndicateName: "Apex Vanguard",
    preferredShipId: 10,
    preferredFoundry: "Independent",
    preferredMode: "SECTOR_CONVERGENCE",
    pvpKills: 5,
    pvpDeaths: 2,
    pvpBountyPeak: 450,
    allTimeKills: 18,
    allTimeDeaths: 4,
    allTimeBountyPeak: 920,
    convergencesWon: 1,
    createdAt: now,
    updatedAt: now,
  };
}

function makeValidLeaderboard(uid, now) {
  return {
    userId: uid,
    visibility: "public",
    callsign: "VIPER_ACE",
    shipId: 9,
    syndicateTag: "AEX",
    armadaTag: "SOLO",
    armadaTierSize: 1,
    foundry: "Independent",
    pvpKills: 5,
    pvpDeaths: 2,
    pvpBounty: 450,
    allTimeKills: 18,
    allTimeDeaths: 4,
    allTimeBounty: 920,
    convergencesWon: 1,
    createdAt: now,
    updatedAt: now,
  };
}

function makeValidPilotState(uid, now, sector = "NEXUS_LOBBY") {
  return {
    userId: uid,
    visibility: "public",
    sectorId: sector,
    callsign: "VIPER_ACE",
    syndicateTag: "AEX",
    armadaName: "SOLO",
    shipId: 9,
    freq: 0,
    x: 1200.5,
    y: 950.25,
    vx: 14.2,
    vy: -8.5,
    angle: 1.57,
    weaponAngle: 1.57,
    isFiringPrimary: false,
    isAfterburning: true,
    isCloaked: false,
    isRespawning: false,
    isInStasisBrig: false,
    secondarySeq: 2,
    secondaryType: "GRAVITY_BOMB",
    secondaryAim: 1.57,
    energy: 2400,
    maxEnergy: 2400,
    bounty: 250,
    liveResonance: 320,
    lastPingType: "NONE",
    lastPingX: 0,
    lastPingY: 0,
    createdAt: now,
    updatedAt: now,
  };
}

function makeValidSyndicate(uid, now) {
  return {
    ownerId: uid,
    tag: "AEX",
    name: "Apex Vanguard",
    foundryOrigin: "Independent",
    motto: "Strike Fast",
    memberCount: 3,
    combinedBounty: 2400,
    isCustom: true,
    createdAt: now,
    updatedAt: now,
  };
}

test("unauthenticated users cannot read or write presence, dossiers, combat events, or instances", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("sector_pilots").doc(ALICE_UID).set(makeValidPilotState(ALICE_UID, now)));
  await assertFails(unauthDb.collection("sector_pilots").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("sector_instances").doc("FLUX_CORE").get());
});

test("signed-in pilots can publish NEXUS_LOBBY, SECTOR_CONVERGENCE_2, and RIFT_CONTROL presence and discover each other", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(aliceDb.collection("sector_pilots").doc(ALICE_UID).set(makeValidPilotState(ALICE_UID, now, "NEXUS_LOBBY")));
  await assertSucceeds(bobDb.collection("sector_pilots").doc(BOB_UID).set(makeValidPilotState(BOB_UID, now, "SECTOR_CONVERGENCE_2")));

  // Update heartbeat without modifying createdAt
  await assertSucceeds(
    aliceDb.collection("sector_pilots").doc(ALICE_UID).update({
      sectorId: "RIFT_CONTROL",
      x: 3400.0,
      y: 4100.0,
      secondarySeq: 3,
      liveResonance: 350,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-device read of all online pilots (matches observeAllOnlineHumanPilots & findRealSwornPilotByCallsign)
  await assertSucceeds(bobDb.collection("sector_pilots").where("visibility", "==", "public").get());
  // Cross-device read of sector-specific pilots (matches observeSectorPilots)
  await assertSucceeds(
    bobDb.collection("sector_pilots").where("visibility", "==", "public").where("sectorId", "==", "RIFT_CONTROL").get()
  );
  // Unfiltered query without visibility is rejected
  await assertFails(bobDb.collection("sector_pilots").get());
  // Prevent spoofing another pilot's presence document
  await assertFails(bobDb.collection("sector_pilots").doc(ALICE_UID).update({ x: 0 }));
});

test("signed-in pilots can emit verified combat events and synchronize shared Rift objective state", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("sector_combat_events").doc("evt_1001").set({
      senderUid: ALICE_UID,
      targetUid: BOB_UID,
      sectorId: "FLUX_CORE",
      eventType: "ELIMINATED",
      damage: 420,
      bountyAward: 125,
      weaponLabel: "Plasma Cannon",
      senderCallsign: "VIPER_ACE",
      senderTag: "AEX",
      senderShipId: 1,
      createdAtMs: Date.now(),
      createdAt: now,
    })
  );

  // Bob can observe combat events in FLUX_CORE
  await assertSucceeds(bobDb.collection("sector_combat_events").where("sectorId", "==", "FLUX_CORE").get());
  // Bob cannot spoof senderUid as Alice
  await assertFails(
    bobDb.collection("sector_combat_events").doc("evt_spoof").set({
      senderUid: ALICE_UID,
      targetUid: BOB_UID,
      sectorId: "FLUX_CORE",
      eventType: "HIT",
      damage: 100,
      bountyAward: 0,
      weaponLabel: "Pulse",
      senderCallsign: "BOB",
      senderTag: "SOLO",
      senderShipId: 2,
      createdAtMs: Date.now(),
      createdAt: now,
    })
  );

  // Shared Sector Objective state
  await assertSucceeds(
    aliceDb.collection("sector_instances").doc("FLUX_CORE").set({
      sectorId: "FLUX_CORE",
      hostUid: ALICE_UID,
      fluxCarrierUid: ALICE_UID,
      fluxOrbX: 4100.0,
      fluxOrbY: 3750.0,
      fluxOrbVx: 120.0,
      fluxOrbVy: -45.0,
      fluxTier: 3,
      fluxHoldSec: 16.5,
      rcWestScore: 0,
      rcEastScore: 0,
      rcTimerSec: 180.0,
      rcAlphaCtrl: 0,
      rcAlphaLock: -1,
      rcOmegaCtrl: 0,
      rcOmegaLock: -1,
      sbTimerSec: 180.0,
      sbWestCaps: 2,
      sbEastCaps: 1,
      sbWestCoreHp: 38500.0,
      sbEastCoreHp: 40000.0,
      jailedUidsCsv: "",
      updatedAtMs: Date.now(),
      updatedAt: now,
    })
  );
  await assertSucceeds(bobDb.collection("sector_instances").doc("FLUX_CORE").get());
});

test("dossier, leaderboard, syndicates, and syndicate crest global_state enforce ownership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(makeValidDossier(ALICE_UID, now)));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("leaderboard").doc(ALICE_UID).set(makeValidLeaderboard(ALICE_UID, now)));
  await assertSucceeds(bobDb.collection("leaderboard").where("visibility", "==", "public").get());
  await assertSucceeds(aliceDb.collection("syndicates").doc("AEX").set(makeValidSyndicate(ALICE_UID, now)));
  await assertSucceeds(
    aliceDb.collection("global_state").doc("syndicate_meta_AEX").set({
      tag: "AEX",
      ownerId: ALICE_UID,
      crestJson: "{\"shield\":1}",
      customChannelsJson: "[\"#general\"]",
      invitedGuestsCsv: "",
      updatedAtMs: Date.now(),
    })
  );
  await assertFails(
    bobDb.collection("global_state").doc("syndicate_meta_AEX").set({
      tag: "AEX",
      ownerId: BOB_UID,
      crestJson: "{}",
      customChannelsJson: "[]",
      invitedGuestsCsv: "",
      updatedAtMs: Date.now(),
    })
  );
});
