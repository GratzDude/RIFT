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
    preferredShipId: 1,
    preferredFoundry: "Kestrel",
    preferredMode: "warzone_10v10",
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
    shipId: 1,
    foundry: "Kestrel",
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
    sectorId: "warzone_10v10",
    callsign: "VIPER_ACE",
    shipId: 1,
    freq: 0,
    x: 1200.5,
    y: 950.25,
    vx: 14.2,
    vy: -8.5,
    angle: 1.57,
    energy: 1200,
    maxEnergy: 1200,
    bounty: 250,
    lastPingType: "NONE",
    lastPingX: 0,
    lastPingY: 0,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: rejected across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("leaderboard").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("sector_pilots").where("sectorId", "==", "warzone_10v10").get());
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

  // Non-existent doc check before creation must succeed for authenticated user
  await assertSucceeds(aliceDb.collection("leaderboard").doc(ALICE_UID).get());

  await assertSucceeds(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set(makeValidLeaderboard(ALICE_UID, now))
  );

  // Aligned query succeeds for another authenticated user
  await assertSucceeds(
    bobDb.collection("leaderboard").where("visibility", "==", "public").get()
  );

  // Cross-user mutation rejected
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
    bobDb.collection("sector_pilots").where("sectorId", "==", "warzone_10v10").get()
  );

  await assertFails(
    bobDb.collection("sector_pilots").doc(ALICE_UID).update({ energy: 1, updatedAt: now })
  );
});
