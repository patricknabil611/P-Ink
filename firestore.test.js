const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_user_123";
const BOB_UID = "bob_user_456";

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

test("Unauthenticated user cannot read or write tasks or notes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("notes").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set({
      userId: ALICE_UID,
      text: "Buy milk",
      completed: false,
      clientCreatedAt: Date.now() - 1000,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").set({
      userId: ALICE_UID,
      title: "Secret",
      content: "Note content",
      clientCreatedAt: Date.now() - 1000,
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Owner can create, update, list, get, and delete valid tasks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const clientCreatedAt = Date.now() - 5000;
  const tasksRef = aliceDb.collection("users").doc(ALICE_UID).collection("tasks");

  await assertSucceeds(
    tasksRef.doc("task-uuid-1").set({
      userId: ALICE_UID,
      text: "شراء قهوة",
      completed: false,
      clientCreatedAt,
      createdAt: created,
      updatedAt: created,
    })
  );

  // List matches Kotlin query on /users/{uid}/tasks
  await assertSucceeds(tasksRef.orderBy("createdAt", "desc").get());
  await assertSucceeds(tasksRef.doc("task-uuid-1").get());

  // Partial update on mutable keys succeeds
  await assertSucceeds(
    tasksRef.doc("task-uuid-1").update({
      text: "شراء قهوة وحليب",
      completed: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    tasksRef.doc("task-uuid-1").update({
      createdAt: new Date(Date.now() - 200),
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Reject ghost field / @DocumentId 'id' leakage
  await assertFails(
    tasksRef.doc("task-uuid-2").set({
      id: "task-uuid-2",
      userId: ALICE_UID,
      text: "Extra id key",
      completed: false,
      clientCreatedAt,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject tampered future timestamp
  await assertFails(
    tasksRef.doc("task-uuid-3").set({
      userId: ALICE_UID,
      text: "Future clock skew",
      completed: false,
      clientCreatedAt,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: created,
    })
  );

  // Reject oversized text > 2000 chars
  await assertFails(
    tasksRef.doc("task-uuid-4").set({
      userId: ALICE_UID,
      text: "x".repeat(2001),
      completed: false,
      clientCreatedAt,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(tasksRef.doc("task-uuid-1").delete());
});

test("Owner can create, update, list, get, and delete valid notes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const clientCreatedAt = Date.now() - 5000;
  const notesRef = aliceDb.collection("users").doc(ALICE_UID).collection("notes");

  await assertSucceeds(
    notesRef.doc("note-uuid-1").set({
      userId: ALICE_UID,
      title: "Meeting ideas",
      content: "1 2 3 4\nHi Hello",
      clientCreatedAt,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(notesRef.get());
  await assertSucceeds(
    notesRef.doc("note-uuid-1").update({
      title: "Updated meeting ideas",
      content: "1 2 3 4\nHi Hello\nDone",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject empty content or oversized title
  await assertFails(
    notesRef.doc("note-uuid-2").set({
      userId: ALICE_UID,
      title: "Valid",
      content: "",
      clientCreatedAt,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    notesRef.doc("note-uuid-3").set({
      userId: ALICE_UID,
      title: "t".repeat(201),
      content: "Body",
      clientCreatedAt,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(notesRef.doc("note-uuid-1").delete());
});

test("Cross-user isolation: Alice cannot read, list, update, or delete Bob's tasks or notes", async () => {
  const past = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(BOB_UID).collection("tasks").doc("bob-task").set({
      userId: BOB_UID,
      text: "Bob private task",
      completed: false,
      clientCreatedAt: Date.now() - 5000,
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("users").doc(BOB_UID).collection("notes").doc("bob-note").set({
      userId: BOB_UID,
      title: "Bob private note",
      content: "Private",
      clientCreatedAt: Date.now() - 5000,
      createdAt: past,
      updatedAt: past,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobTasks = aliceDb.collection("users").doc(BOB_UID).collection("tasks");
  const bobNotes = aliceDb.collection("users").doc(BOB_UID).collection("notes");

  await assertFails(bobTasks.get());
  await assertFails(bobTasks.doc("bob-task").get());
  await assertFails(bobTasks.doc("bob-task").update({ text: "Hijacked", updatedAt: past }));
  await assertFails(bobTasks.doc("bob-task").delete());

  await assertFails(bobNotes.get());
  await assertFails(bobNotes.doc("bob-note").get());
  await assertFails(bobNotes.doc("bob-note").update({ content: "Hijacked", updatedAt: past }));
  await assertFails(bobNotes.doc("bob-note").delete());

  // Spoofed userId inside Alice's own subcollection must also fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("spoof-task").set({
      userId: BOB_UID,
      text: "Spoof",
      completed: false,
      clientCreatedAt: Date.now() - 5000,
      createdAt: past,
      updatedAt: past,
    })
  );
});
