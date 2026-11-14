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

function makeValidPilotState(uid, now) {
  return {
    userId: uid,
    sectorId: "SECTOR_CONVERGENCE",
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
    energy: 2400,
    maxEnergy: 2400,
    bounty: 250,
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

function makeValidSectorComm(uid, now) {
  return {
    senderUid: uid,
    visibility: "public",
    senderCallsign: "VIPER_ACE",
    senderSyndicateTag: "AEX",
    timestampLabel: "JUST NOW",
    messageText: "Forming 3v3 Skirmish Armada!",
    isArmadaRecruitBanner: true,
    armadaName: "APEX_3",
    armadaCurrentSize: 2,
    armadaMaxSize: 3,
    armadaObjective: "Ranked Rift Skirmish",
    requestSent: false,
    createdAtMs: Date.now(),
    createdAt: now,
  };
}

test("Unauthenticated user: rejected across all 5 collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("leaderboard").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("sector_pilots").where("sectorId", "==", "SECTOR_CONVERGENCE").get());
  await assertFails(unauthDb.collection("syndicates").where("isCustom", "==", true).get());
  await assertFails(unauthDb.collection("sector_comms").where("visibility", "==", "public").get());
});

test("PilotDossier: owner can create, read, and update own dossier; cross-user read/write blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(makeValidDossier(ALICE_UID, now))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Cross-user read & write rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({ callsign: "HACKED", updatedAt: now })
  );

  // Ghost field rejected (Shadow Update Test)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      callsign: "VIPER_TWO",
      isAdmin: true,
      updatedAt: now,
    })
  );

  // Valid update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      callsign: "VIPER_TWO",
      pvpKills: 7,
      updatedAt: now,
    })
  );
});

test("Leaderboard: authenticated users can get non-existent doc, list with visibility=='public' filter, owner-only write", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(aliceDb.collection("leaderboard").doc(ALICE_UID).get());

  await assertSucceeds(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set(makeValidLeaderboard(ALICE_UID, now))
  );

  await assertSucceeds(
    bobDb.collection("leaderboard").where("visibility", "==", "public").get()
  );

  await assertFails(
    bobDb.collection("leaderboard").doc(ALICE_UID).update({ pvpKills: 999, updatedAt: now })
  );
});

test("SectorPilots: authenticated users can create own telemetry and query by sectorId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("sector_pilots").doc(ALICE_UID).set(makeValidPilotState(ALICE_UID, now))
  );

  await assertSucceeds(
    bobDb.collection("sector_pilots").where("sectorId", "==", "SECTOR_CONVERGENCE").get()
  );

  await assertFails(
    bobDb.collection("sector_pilots").doc(ALICE_UID).update({ energy: 1, updatedAt: now })
  );
});

test("Syndicates & SectorComms: authenticated users can create and query with aligned filters; cross-user mutation blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("syndicates").doc("AEX").set(makeValidSyndicate(ALICE_UID, now))
  );
  await assertSucceeds(
    bobDb.collection("syndicates").where("isCustom", "==", true).get()
  );
  await assertFails(
    bobDb.collection("syndicates").doc("AEX").update({ motto: "Hijacked", updatedAt: now })
  );

  await assertSucceeds(
    aliceDb.collection("sector_comms").doc("comm_1").set(makeValidSectorComm(ALICE_UID, now))
  );
  await assertSucceeds(
    bobDb.collection("sector_comms").where("visibility", "==", "public").get()
  );
  await assertFails(
    bobDb.collection("sector_comms").doc("comm_1").update({ messageText: "Spoofed" })
  );
});
