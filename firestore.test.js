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
    armadaTag: "APEX_SQUADRON",
    armadaTierSize: 2,
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
    armadaName: "APEX_SQUADRON",
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

function makeValidCustomGame(uid, roomCode, privateRoom, now) {
  return {
    roomCode,
    instanceId: `${privateRoom ? "priv" : "pub"}_${roomCode}`,
    hostUid: uid,
    hostCallsign: "VIPER_ACE",
    title: `VIPER CONVERGENCE [${roomCode}]`,
    visibility: privateRoom ? "private" : "public",
    privateRoom,
    baseMode: "SECTOR_CONVERGENCE",
    teamSizeBracket: 5,
    matchDurationMinutes: 10,
    allowArmadasInFfa: true,
    botsEnabled: true,
    bulletSizeMult: 2.2,
    damageMult: 1.0,
    speedMult: 1.0,
    healthMult: 1.0,
    rechargeMult: 1.5,
    crateSpawnMult: 1.0,
    createdAtMs: Date.now() - 1000,
    createdAt: now,
    updatedAt: now,
  };
}

test("unauthenticated users cannot read or write presence, custom games, dossiers, combat events, or instances", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("sector_pilots").doc(ALICE_UID).set(makeValidPilotState(ALICE_UID, now)));
  await assertFails(unauthDb.collection("sector_pilots").get());
  await assertFails(unauthDb.collection("custom_games").doc("PUB123").get());
  await assertFails(unauthDb.collection("custom_games").where("visibility", "==", "public").where("privateRoom", "==", false).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("sector_instances").doc("FLUX_CORE").get());
});

test("signed-in pilots can publish NEXUS_LOBBY, CUSTOM_pub_PUB123, and RIFT_CONTROL presence and discover each other", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(aliceDb.collection("sector_pilots").doc(ALICE_UID).set(makeValidPilotState(ALICE_UID, now, "NEXUS_LOBBY")));
  await assertSucceeds(bobDb.collection("sector_pilots").doc(BOB_UID).set(makeValidPilotState(BOB_UID, now, "CUSTOM_pub_PUB123")));

  // Update heartbeat without modifying createdAt
  await assertSucceeds(
    aliceDb.collection("sector_pilots").doc(ALICE_UID).update({
      sectorId: "CUSTOM_pub_PUB123",
      x: 3400.0,
      y: 4100.0,
      vx: 220.0,
      vy: -140.0,
      isFiringPrimary: true,
      secondarySeq: 3,
      liveResonance: 350,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-device read of all online pilots
  await assertSucceeds(bobDb.collection("sector_pilots").where("visibility", "==", "public").get());
  // Cross-device read of sector-specific pilots (including Custom Game sector)
  await assertSucceeds(
    bobDb.collection("sector_pilots").where("visibility", "==", "public").where("sectorId", "==", "CUSTOM_pub_PUB123").get()
  );
  // Unfiltered query without visibility is rejected
  await assertFails(bobDb.collection("sector_pilots").get());
  // Prevent spoofing another pilot's presence document
  await assertFails(bobDb.collection("sector_pilots").doc(ALICE_UID).update({ x: 0 }));
});

test("public custom games appear in public list query while private custom games are joinable only via 6-char Join Code", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Alice creates a Public Custom Game (PUB777) and a Private Custom Game (PRV999)
  await assertSucceeds(
    aliceDb.collection("custom_games").doc("PUB777").set(makeValidCustomGame(ALICE_UID, "PUB777", false, now))
  );
  await assertSucceeds(
    aliceDb.collection("custom_games").doc("PRV999").set(makeValidCustomGame(ALICE_UID, "PRV999", true, now))
  );

  // Bob queries the Public Custom Games list (matches observePublicCustomGames)
  const publicSnap = await assertSucceeds(
    bobDb.collection("custom_games").where("visibility", "==", "public").where("privateRoom", "==", false).get()
  );
  if (publicSnap.docs.length !== 1 || publicSnap.docs[0].id !== "PUB777") {
    throw new Error("Expected strictly PUB777 in public custom games directory");
  }

  // Unfiltered list query (attempting to scrape private join codes) is rejected
  await assertFails(bobDb.collection("custom_games").get());
  await assertFails(bobDb.collection("custom_games").where("privateRoom", "==", true).get());

  // Bob joins Alice's Private Custom Game directly using the 6-char Join Code PRV999
  const privDoc = await assertSucceeds(bobDb.collection("custom_games").doc("PRV999").get());
  if (!privDoc.exists || privDoc.data().instanceId !== "priv_PRV999") {
    throw new Error("Expected direct Join Code lookup for PRV999 to succeed");
  }

  // Bob cannot hijack or overwrite Alice's custom game room
  await assertFails(
    bobDb.collection("custom_games").doc("PRV999").update({
      title: "HIJACKED",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  // Shadow field injection is rejected
  await assertFails(
    aliceDb.collection("custom_games").doc("BAD888").set({
      ...makeValidCustomGame(ALICE_UID, "BAD888", false, now),
      isAdminRoom: true,
    })
  );
});

test("signed-in pilots can emit verified combat events and synchronize shared Rift objective state", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("sector_combat_events").doc("evt_1001").set({
      senderUid: ALICE_UID,
      targetUid: BOB_UID,
      sectorId: "CUSTOM_priv_PRV999",
      eventType: "HIT",
      damage: 185,
      bountyAward: 0,
      weaponLabel: "Plasma Cannon",
      senderCallsign: "VIPER_ACE",
      senderTag: "APEX_SQUADRON",
      senderShipId: 1,
      createdAtMs: Date.now(),
      createdAt: now,
    })
  );

  await assertSucceeds(bobDb.collection("sector_combat_events").where("sectorId", "==", "CUSTOM_priv_PRV999").get());
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
  await assertSucceeds(
    aliceDb.collection("global_state").doc(`pilot_prog_${ALICE_UID}`).set({
      ownerId: ALICE_UID,
      payloadJson: "{\"liveRes\":450,\"lvl\":12}",
      updatedAtMs: Date.now(),
    })
  );
  await assertSucceeds(aliceDb.collection("global_state").doc(`pilot_prog_${ALICE_UID}`).get());
  await assertFails(bobDb.collection("global_state").doc(`pilot_prog_${ALICE_UID}`).get());
  await assertFails(
    bobDb.collection("global_state").doc(`pilot_prog_${ALICE_UID}`).set({
      ownerId: BOB_UID,
      payloadJson: "{\"liveRes\":9999}",
      updatedAtMs: Date.now(),
    })
  );
});
