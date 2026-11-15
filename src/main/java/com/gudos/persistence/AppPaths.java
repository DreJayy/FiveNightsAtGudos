package com.gudos.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves writable player-data files outside the application and its bundled resources. */
/*
 * DEFENSE OVERVIEW: AppPaths
 *
 * Purpose: Resolves writable player-data paths outside the packaged game.
 *
 * Follow the flow: Main calls resolve; SettingsPanel displays the folder; Database, SettingsStore
 * and ShiftLogStore use the three child paths.
 *
 * Code-based rationale and tradeoff: A JAR contains read-only resources; user saves need a writable
 * external folder. gudo.dataDir supports isolated demos/tests without changing ordinary player data.
 *
 * State to recognize: directory is normalized absolute. database() -> gudo.db, settings() ->
 * settings.txt, shiftLog() -> shift-log.txt.
 *
 * Defense answer: The priority is explicit system property, then LOCALAPPDATA if present, then the
 * home directory fallback. It is based on the available environment value, not an explicit
 * operating-system check.
 */
public record AppPaths(Path directory) {
    /*
     * DEFENSE: AppPaths constructor
     *
     * Normalize the supplied directory into an absolute path. resolve is responsible for creating
     * it; direct construction alone does not create the directory.
     */
    public AppPaths {
        directory = directory.toAbsolutePath().normalize();
    }

    /*
     * DEFENSE: resolve
     *
     * Prefer nonblank -Dgudo.dataDir; otherwise use LOCALAPPDATA/FiveNightsAtGudos when that
     * environment variable exists; otherwise use user.home/.five-nights-at-gudos. Create the folder
     * and return normalized paths. The override is useful for safe isolated demos.
     */
    public static AppPaths resolve() throws IOException {
        // Priority: -Dgudo.dataDir override, Windows LocalAppData, then the user's home folder.
        String override = System.getProperty("gudo.dataDir");
        String local = System.getenv("LOCALAPPDATA");
        Path directory = override != null && !override.isBlank() ? Path.of(override)
                : local != null && !local.isBlank() ? Path.of(local, "FiveNightsAtGudos")
                : Path.of(System.getProperty("user.home"), ".five-nights-at-gudos");
        Files.createDirectories(directory);
        return new AppPaths(directory);
    }

    // SQLite is the source for progress/history; the log is a report and settings are preferences.
    /*
     * DEFENSE: database
     *
     * Resolve gudo.db beneath the chosen writable directory. This is SQLite storage, not a bundled
     * resource.
     */
    public Path database() { return directory.resolve("gudo.db"); }
    /*
     * DEFENSE: settings
     *
     * Resolve settings.txt beneath the chosen writable directory. Preferences are editable UTF-8
     * key/value text.
     */
    public Path settings() { return directory.resolve("settings.txt"); }
    /*
     * DEFENSE: shiftLog
     *
     * Resolve shift-log.txt beneath the chosen writable directory. This is a report, not the
     * authoritative progress file.
     */
    public Path shiftLog() { return directory.resolve("shift-log.txt"); }
}
