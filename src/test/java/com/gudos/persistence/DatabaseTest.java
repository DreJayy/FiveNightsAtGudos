package com.gudos.persistence;

import com.gudos.game.GameSession;
import com.gudos.game.Outcome;
import com.gudos.game.RunRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/*
 * DEFENSE OVERVIEW: DatabaseTest
 *
 * Purpose: JUnit integration checks against real temporary SQLite files.
 *
 * Follow the flow: Create a fresh DB per test; save/reopen/retry/replay; test profiles, constraints,
 * unlock cap, secret outcomes and legacy schema migrations.
 *
 * Code-based rationale and tradeoff: Real JDBC catches SQL and transaction behavior. TempDir
 * protects ordinary player data.
 *
 * State to recognize: completed builds a short finished session. create runs before each test;
 * directory is injected by JUnit.
 *
 * Defense answer: An invalid attempt must not unlock a night; retries must not duplicate; migration
 * must retain old rows/progress.
 */
class DatabaseTest {
    @TempDir Path directory;
    private Database database;

    /*
     * DEFENSE: create
     *
     * Before each test open a fresh temporary SQLite file. Each test is isolated from the ordinary
     * save directory and other tests.
     */
    @BeforeEach void create() throws SQLException { database = new Database(directory.resolve("test.db")); }

    /*
     * DEFENSE: completed
     *
     * Create a two-second GameSession for the requested night, advance to morning and return its
     * completed immutable record.
     */
    private RunRecord completed(int night) {
        GameSession game = new GameSession(night, 2);
        game.update(2);
        return game.result();
    }

    /*
     * DEFENSE: createsProfileAndPersistsAcrossNewConnections
     *
     * Verify fresh Progress, save Night 1, reopen Database and assert progress/result survive
     * connection recreation.
     */
    @Test void createsProfileAndPersistsAcrossNewConnections() throws Exception {
        assertEquals(Progress.EMPTY, database.loadProgress("Night Guard"));
        RunRecord run = completed(1);
        Progress progress = database.saveRun("Night Guard", run);
        assertEquals(2, progress.nextNight());
        assertEquals(1, progress.highestCompleted());
        Database reopened = new Database(directory.resolve("test.db"));
        assertEquals(progress, reopened.loadProgress("Night Guard"));
        assertEquals(run, reopened.loadRuns("Night Guard").get(0));
    }

    /*
     * DEFENSE: retriesAreIdempotentAndReplaysDoNotRelockNights
     *
     * Save the same UUID twice, then later/earlier completed nights; require one first row and
     * monotonic unlocks with the correct final run count.
     */
    @Test void retriesAreIdempotentAndReplaysDoNotRelockNights() throws Exception {
        RunRecord first = completed(1);
        database.saveRun("Player", first);
        database.saveRun("Player", first);
        assertEquals(1, database.loadProgress("Player").totalRuns());
        database.saveRun("Player", completed(2));
        database.saveRun("Player", completed(1));
        assertEquals(3, database.loadProgress("Player").nextNight());
        assertEquals(2, database.loadProgress("Player").highestCompleted());
        assertEquals(3, database.loadRuns("Player").size());
    }

    /*
     * DEFENSE: onlySuccessfulShiftsUnlockProgress
     *
     * Save an abandoned attempt and assert total attempts increases while completed count and next
     * night do not.
     */
    @Test void onlySuccessfulShiftsUnlockProgress() throws Exception {
        GameSession game = new GameSession(1);
        game.update(10);
        game.abandon();
        Progress progress = database.saveRun("Player", game.result());
        assertEquals(1, progress.nextNight());
        assertEquals(0, progress.completedRuns());
        assertEquals(1, progress.totalRuns());
        assertEquals(Outcome.ABANDONED, database.loadRuns("Player").get(0).outcome());
    }

    /*
     * DEFENSE: separateProfilesAndQuotedNamesAreSafe
     *
     * Save a name containing SQL-looking quoted text, assert the table still works, another player
     * stays empty and case-insensitive lookup finds the same profile.
     */
    @Test void separateProfilesAndQuotedNamesAreSafe() throws Exception {
        String unusual = "O'Brien'); DROP TABLE profiles;--";
        database.saveRun(unusual, completed(1));
        assertEquals(1, database.loadProgress(unusual).totalRuns());
        assertEquals(Progress.EMPTY, database.loadProgress("Somebody Else"));
        assertEquals(1, database.loadProgress(unusual.toLowerCase(java.util.Locale.ROOT)).totalRuns());
    }

    /*
     * DEFENSE: failedInsertDoesNotAdvanceProgress
     *
     * Try an invalid night zero completed record, require SQLException, then assert no
     * attempt/unlock update persisted. This exercises database constraint/rollback behavior.
     */
    @Test void failedInsertDoesNotAdvanceProgress() throws Exception {
        RunRecord invalid = new RunRecord(UUID.randomUUID().toString(), 0, 10, 80, Outcome.COMPLETED, Instant.now(), Instant.now());
        assertThrows(SQLException.class, () -> database.saveRun("Player", invalid));
        assertEquals(Progress.EMPTY, database.loadProgress("Player"));
        assertTrue(database.loadRuns("Player").isEmpty());
    }

    /*
     * DEFENSE: fifthNightDoesNotUnlockANonexistentSixth
     *
     * Save completed Night 5 directly through the storage API and assert nextNight remains five and
     * unlocked(6) is false. This test does not imply the UI allows skipping locked nights.
     */
    @Test void fifthNightDoesNotUnlockANonexistentSixth() throws Exception {
        database.saveRun("Player", completed(5));
        Progress progress = database.loadProgress("Player");
        assertEquals(5, progress.nextNight());
        assertFalse(progress.unlocked(6));
    }

    /*
     * DEFENSE: secretAttemptsRemainRecordedWithoutCompletingTheNight
     *
     * Save failed and successful secret outcomes, retry the same successful UUID and reopen; assert
     * exactly two secret rows and no normal progress advancement.
     */
    @Test void secretAttemptsRemainRecordedWithoutCompletingTheNight() throws Exception {
        GameSession failed = new GameSession(1);
        failed.failSecret();
        database.saveRun("Player", failed.result());
        assertEquals(Outcome.MINIGAME_FAILED, database.loadRuns("Player").get(0).outcome());
        assertEquals(1, database.loadProgress("Player").nextNight());

        GameSession won = new GameSession(1);
        won.completeSecret();
        database.saveRun("Player", won.result());
        database.saveRun("Player", won.result());
        Database reopened = new Database(directory.resolve("test.db"));
        assertEquals(1, reopened.loadProgress("Player").nextNight());
        assertEquals(0, reopened.loadProgress("Player").completedRuns());
        assertEquals(Outcome.MINIGAME_COMPLETED, won.result().outcome());
        assertEquals(2, reopened.loadRuns("Player").size());
        assertTrue(reopened.loadRuns("Player").contains(won.result()));
    }

    /*
     * DEFENSE: legacyProfileWideSecretFlagIsPreservedWithoutGuessingANight
     *
     * Create a legacy version-three completion table and flag, reopen/migrate, and verify the flag
     * row remains without inventing a night completion.
     */
    @Test void legacyProfileWideSecretFlagIsPreservedWithoutGuessingANight() throws Exception {
        Path file = directory.resolve("version3.db");
        Database old = new Database(file);
        old.loadProgress("Veteran");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath()); var sql = connection.createStatement()) {
            sql.execute("CREATE TABLE secret_completions (profile_id INTEGER PRIMARY KEY REFERENCES profiles(id))");
            sql.execute("INSERT INTO secret_completions SELECT id FROM profiles WHERE name='Veteran'");
            sql.execute("PRAGMA user_version=3");
        }
        Database migrated = new Database(file);
        assertEquals(Progress.EMPTY, migrated.loadProgress("Veteran"));
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
             var sql = connection.createStatement(); var rows = sql.executeQuery("SELECT COUNT(*) FROM secret_completions")) {
            assertEquals(1, rows.getInt(1));
        }
    }

    /*
     * DEFENSE: migratesVersionTwoRunsBeforeSavingASecretFailure
     *
     * Rebuild a legacy outcome-constrained version-two table containing an old run; reopen through
     * Database, save a secret failure and require both old/new records.
     */
    @Test void migratesVersionTwoRunsBeforeSavingASecretFailure() throws Exception {
        Path file = directory.resolve("version2.db");
        Database old = new Database(file);
        RunRecord existing = completed(1);
        old.saveRun("Veteran", existing);
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
             var sql = connection.createStatement()) {
            sql.execute("ALTER TABLE night_runs RENAME TO newer_runs");
            sql.execute("DROP INDEX runs_by_profile");
            sql.execute("""
                    CREATE TABLE night_runs (
                        id TEXT PRIMARY KEY, profile_id INTEGER NOT NULL REFERENCES profiles(id),
                        night INTEGER NOT NULL CHECK(night BETWEEN 1 AND 5),
                        elapsed_seconds REAL NOT NULL CHECK(elapsed_seconds >= 0),
                        power_remaining REAL NOT NULL CHECK(power_remaining BETWEEN 0 AND 100),
                        outcome TEXT NOT NULL CHECK(outcome IN ('COMPLETED','POWER_OUT','ABANDONED','CAUGHT')),
                        started_at TEXT NOT NULL, ended_at TEXT NOT NULL
                    )
                    """);
            sql.execute("INSERT INTO night_runs SELECT * FROM newer_runs");
            sql.execute("DROP TABLE newer_runs");
            sql.execute("CREATE INDEX runs_by_profile ON night_runs(profile_id, ended_at DESC)");
            sql.execute("PRAGMA user_version=2");
        }
        Database migrated = new Database(file);
        GameSession failed = new GameSession(2);
        failed.failSecret();
        migrated.saveRun("Veteran", failed.result());
        assertTrue(migrated.loadRuns("Veteran").contains(existing));
        assertTrue(migrated.loadRuns("Veteran").contains(failed.result()));
    }

    /*
     * DEFENSE: migratesLegacyRunsAndPreservesProgressWhenRecordingCaughtOutcome
     *
     * Construct version-one schema with three old outcomes, retain a normal completed result,
     * migrate and save CAUGHT. Verify history/completion count/unlock state across another reopen.
     */
    @Test void migratesLegacyRunsAndPreservesProgressWhenRecordingCaughtOutcome() throws Exception {
        Path file = directory.resolve("legacy.db");
        Database legacy = new Database(file);
        // Reproduce the previous release's constrained table and schema version.
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
             var sql = connection.createStatement()) {
            sql.execute("DROP TABLE night_runs");
            sql.execute("""
                    CREATE TABLE night_runs (
                        id TEXT PRIMARY KEY,
                        profile_id INTEGER NOT NULL REFERENCES profiles(id),
                        night INTEGER NOT NULL CHECK(night BETWEEN 1 AND 5),
                        elapsed_seconds REAL NOT NULL CHECK(elapsed_seconds >= 0),
                        power_remaining REAL NOT NULL CHECK(power_remaining BETWEEN 0 AND 100),
                        outcome TEXT NOT NULL CHECK(outcome IN ('COMPLETED','POWER_OUT','ABANDONED')),
                        started_at TEXT NOT NULL,
                        ended_at TEXT NOT NULL
                    )
                    """);
            sql.execute("CREATE INDEX runs_by_profile ON night_runs(profile_id, ended_at DESC)");
            sql.execute("PRAGMA user_version=1");
        }
        RunRecord old = completed(1);
        legacy.saveRun("Veteran", old);
        Database upgraded = new Database(file);
        assertEquals(old, upgraded.loadRuns("Veteran").get(0));
        GameSession caught = new GameSession(2);
        caught.update(1000);
        Progress progress = upgraded.saveRun("Veteran", caught.result());
        assertEquals(Outcome.CAUGHT, caught.result().outcome());
        assertEquals(2, progress.totalRuns());
        assertEquals(1, progress.completedRuns());
        assertEquals(2, progress.nextNight());
        Database reopened = new Database(file);
        assertEquals(2, reopened.loadRuns("Veteran").size());
        assertTrue(reopened.loadRuns("Veteran").contains(caught.result()));
    }
}
