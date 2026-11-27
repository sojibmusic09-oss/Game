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

function pastTime(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

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

test("Unauthenticated requests are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("multiplayerSessions").where("status", "in", ["WAITING", "ACTIVE"]).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Unauth",
      callsign: "BD001",
      pilotRank: "STUDENT_PILOT",
      xp: 0,
      flightHours: 0,
      bdasCoins: 1000,
      airlineCredits: 10,
      totalFlights: 0,
      unlockedAircraftIds: ["atr72"],
      unlockedAirportIds: ["VGHS"],
      unlockedAchievementIds: [],
      createdAt: pastTime(),
      updatedAt: pastTime(),
    })
  );
});

test("PilotProfile: owner create/update succeeds; cross-user, ghost fields, and future timestamps fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTime(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Capt. Alice",
      callsign: "BD-101",
      pilotRank: "STUDENT_PILOT",
      xp: 0,
      flightHours: 0.0,
      bdasCoins: 25000,
      airlineCredits: 50,
      totalFlights: 0,
      unlockedAircraftIds: ["atr72"],
      unlockedAirportIds: ["VGHS", "VGEG"],
      unlockedAchievementIds: [],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Partial update preserving createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      xp: 450,
      pilotRank: "JUNIOR_PILOT",
      updatedAt: pastTime(1000),
    })
  );

  // Ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastTime(500),
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      xp: 999,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user read/write rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      bdasCoins: 999999,
      updatedAt: pastTime(500),
    })
  );
});

test("User subcollections (airline, fleet, route, transaction, flightLog, friend): owner CRUD & isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTime(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("airlines").doc("airline_1").set({
      userId: ALICE_UID,
      name: "Bengal Skyways",
      iataCode: "BSW",
      logoId: "emerald_sun",
      primaryColorHex: "#00A86B",
      secondaryColorHex: "#EF5350",
      reputation: 72,
      pilotsHired: 2,
      cabinCrewHired: 4,
      dailyRevenue: 15000,
      dailyExpenses: 6200,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("fleet").doc("fleet_1").set({
      userId: ALICE_UID,
      airlineId: "airline_1",
      aircraftModelId: "atr72",
      registration: "S2-BDA",
      liveryName: "Emerald Delta",
      conditionPercent: 100,
      assignedRouteId: "route_1",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("routes").doc("route_1").set({
      userId: ALICE_UID,
      airlineId: "airline_1",
      originIcao: "VGHS",
      destinationIcao: "VGEG",
      ticketPriceCoins: 4200,
      demandScore: 88,
      assignedFleetId: "fleet_1",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      txType: "INCOME",
      category: "FLIGHT_REWARD",
      amountCoins: 3500,
      amountCredits: 5,
      summary: "Dhaka to Chattogram Flight",
      createdAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("flightLogs").doc("log_1").set({
      userId: ALICE_UID,
      mode: "CAREER",
      aircraftId: "atr72",
      originIcao: "VGHS",
      destinationIcao: "VGEG",
      missionId: "mission_clear_dhaka",
      landingFpm: -145,
      score: 940,
      xpEarned: 320,
      coinsEarned: 3500,
      weatherPreset: "CLEAR",
      durationSec: 420,
      createdAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("friends").doc(BOB_UID).set({
      userId: ALICE_UID,
      friendUid: BOB_UID,
      friendCallsign: "BD-202",
      friendRank: "FIRST_OFFICER",
      createdAt: ts,
    })
  );

  // List queries succeed for owner
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("airlines").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("routes").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("flightLogs").get());

  // Cross-user list and read rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("airlines").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("flightLogs").doc("log_1").get());
});

test("Multiplayer sessions & aircraftStates: join/leave/telemetry parity and security bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTime(10000);

  // Alice creates session
  await assertSucceeds(
    aliceDb.collection("multiplayerSessions").doc("sess_1").set({
      hostUserId: ALICE_UID,
      hostCallsign: "BD-101",
      title: "Dhaka Monsoon Approach",
      airportIcao: "VGHS",
      weatherPreset: "LIGHT_RAIN",
      missionType: "FREE_FLIGHT",
      status: "WAITING",
      maxPlayers: 8,
      playerCount: 1,
      memberUids: [ALICE_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Aligned list query succeeds; unfiltered list query fails
  await assertSucceeds(
    bobDb.collection("multiplayerSessions").where("status", "in", ["WAITING", "ACTIVE"]).get()
  );
  await assertFails(bobDb.collection("multiplayerSessions").get());

  // Bob joins session
  await assertSucceeds(
    bobDb.collection("multiplayerSessions").doc("sess_1").update({
      memberUids: [ALICE_UID, BOB_UID],
      playerCount: 2,
      updatedAt: pastTime(2000),
    })
  );

  // Bob cannot hijack host status
  await assertFails(
    bobDb.collection("multiplayerSessions").doc("sess_1").update({
      status: "FINISHED",
      updatedAt: pastTime(1500),
    })
  );

  // Alice and Bob publish aircraftStates
  await assertSucceeds(
    aliceDb.collection("multiplayerSessions").doc("sess_1").collection("aircraftStates").doc(ALICE_UID).set({
      sessionId: "sess_1",
      playerUid: ALICE_UID,
      callsign: "BD-101",
      aircraftId: "a320",
      latitude: 23.8433,
      longitude: 90.3978,
      altitudeFt: 2500.0,
      headingDeg: 140.0,
      pitchDeg: 4.5,
      rollDeg: -2.0,
      speedKts: 185.0,
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: pastTime(1000),
    })
  );

  // Bob can list aircraftStates using where("memberUids", "array-contains", BOB_UID)
  await assertSucceeds(
    bobDb.collection("multiplayerSessions").doc("sess_1").collection("aircraftStates")
      .where("memberUids", "array-contains", BOB_UID)
      .get()
  );

  // Bob cannot write into Alice's aircraftState doc
  await assertFails(
    bobDb.collection("multiplayerSessions").doc("sess_1").collection("aircraftStates").doc(ALICE_UID).update({
      altitudeFt: 100.0,
      updatedAt: pastTime(500),
    })
  );

  // Host finishes session -> terminal state locks subsequent updates
  await assertSucceeds(
    aliceDb.collection("multiplayerSessions").doc("sess_1").update({
      status: "FINISHED",
      updatedAt: pastTime(400),
    })
  );
  await assertFails(
    aliceDb.collection("multiplayerSessions").doc("sess_1").update({
      weatherPreset: "CLEAR",
      updatedAt: pastTime(200),
    })
  );
});
