package com.gudos.persistence;

import com.gudos.game.GameSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/*
 * ShiftLogStoreTest
 *
 * Purpose: JUnit check for appending separate attempts and deduplicating a retry.
 *
 * Execution flow: Append a completed run twice, append an abandoned run once, then check two lines
 * and readable fields.
 *
 * Design rationale: The repeated UUID must not produce a second ordinary retry
 * line. UTF-8 names and duration labels are checked.
 *
 * State: Uses TempDir and direct file content assertions.
 *
 * Summary: SQLite and text have distinct deduplication mechanisms that share the same stable
 * run ID.
 */
class ShiftLogStoreTest {
    @TempDir Path directory;

    /*
     * Append one completed UUID twice and a different abandoned UUID once; assert exactly two
     * readable UTF-8 lines with expected outcome/name/duration fields.
     */
    @Test void reportsAppendAndRetriesDoNotDuplicate() throws Exception {
        Path file = directory.resolve("shift-log.txt");
        ShiftLogStore store = new ShiftLogStore(file);
        GameSession first = new GameSession(1, 2);
        first.update(2);
        store.append("Guard ñ", first.result());
        store.append("Guard ñ", first.result());
        GameSession second = new GameSession(2);
        second.update(15);
        second.abandon();
        store.append("Guard ñ", second.result());
        assertEquals(2, Files.readAllLines(file).size());
        String text = Files.readString(file);
        assertTrue(text.contains("player=Guard ñ"));
        assertTrue(text.contains("result=Completed"));
        assertTrue(text.contains("result=Left shift"));
        assertTrue(text.contains("duration=00:15"));
    }
}
