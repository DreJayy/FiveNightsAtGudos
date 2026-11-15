package com.gudos.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/*
 * SettingsStoreTest
 *
 * Purpose: JUnit checks for real UTF-8 preferences and normalization.
 *
 * Execution flow: Create defaults, round-trip a name with ñ, load malformed values, and test
 * line/control stripping plus bounds.
 *
 * Design rationale: A text-file round trip verifies actual bytes as well as
 * equality. Validation is tested without the form.
 *
 * State: Each file lives in a temporary test folder.
 *
 * Summary: The settings file is real editable text, and invalid values use defined
 * defaults/bounds.
 */
class SettingsStoreTest {
    @TempDir Path directory;

    /*
     * Load a nonexistent settings.txt, expect DEFAULT and inspect the written text for volume=55.
     */
    @Test void firstLaunchWritesAnActualTextFile() throws Exception {
        Path file = directory.resolve("settings.txt");
        assertEquals(Settings.DEFAULT, new SettingsStore(file).load());
        assertTrue(Files.readString(file).contains("volume=55"));
    }

    /*
     * Save a preference with ñ and nondefault flags/volume; reload equal values and verify the
     * literal UTF-8 name is in the file.
     */
    @Test void settingsRoundTripAsUtf8() throws Exception {
        Path file = directory.resolve("settings.txt");
        SettingsStore store = new SettingsStore(file);
        Settings settings = new Settings("Guido ñ", 73, true, true);
        store.save(settings);
        assertEquals(settings, store.load());
        assertTrue(Files.readString(file).contains("Guido ñ"));
    }

    /*
     * Load malformed number/empty name/unrecognized boolean text, verify fallbacks, then load volume
     * 999 and require clamping to 100.
     */
    @Test void invalidValuesUseSafeDefaultsAndBounds() throws Exception {
        Path file = directory.resolve("settings.txt");
        Files.writeString(file, "volume=garbage\nplayerName=\nreducedEffects=maybe\nfullscreen=true\n");
        assertEquals(new Settings("Night Guard", 55, false, true), new SettingsStore(file).load());
        Files.writeString(file, "volume=999\nplayerName=Guard\n");
        assertEquals(100, new SettingsStore(file).load().volume());
    }

    /*
     * Construct a name containing newline/carriage return and negative volume; assert control
     * removal, zero clamp and the 32-character name cap.
     */
    @Test void namesCannotInjectLinesIntoReports() {
        Settings settings = new Settings("Guard\nName\r", -5, false, false);
        assertEquals("GuardName", settings.playerName());
        assertEquals(0, settings.volume());
        assertEquals(32, new Settings("a".repeat(100), 50, false, false).playerName().length());
    }
}
