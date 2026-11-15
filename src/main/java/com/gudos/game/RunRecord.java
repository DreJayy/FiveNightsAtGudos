package com.gudos.game;

import java.time.Instant;

/** Immutable result of one ended attempt. Database links it to a profile when saving. */
/*
 * DEFENSE OVERVIEW: RunRecord
 *
 * Purpose: Carries the immutable snapshot of one ended attempt.
 *
 * Follow the flow: GameSession creates it once; Application retains it; Database and ShiftLogStore
 * save the same ID and values; RecordsPanel displays it.
 *
 * Code-based rationale and tradeoff: A record supplies value equality and accessors. Capture final
 * values once so retries cannot silently turn the attempt into a new one.
 *
 * State to recognize: id, night, elapsedSeconds, powerRemaining, outcome, startedAt and endedAt.
 * durationLabel uses actual active seconds, not the fictional clock.
 *
 * Defense answer: The record is the receipt for an attempt, not a resumable checkpoint. Its UUID
 * identifies the attempt independently of the player profile.
 */
public record RunRecord(String id, int night, double elapsedSeconds, double powerRemaining,
                        Outcome outcome, Instant startedAt, Instant endedAt) {
    /*
     * DEFENSE: durationLabel
     *
     * Round active elapsed seconds to the nearest whole second and format minutes:seconds. A paused
     * 18-second attempt displays 00:18 even if more real wall time passed.
     */
    public String durationLabel() {
        // Display actual active play time as minutes:seconds, not the fictional in-game clock.
        int seconds = (int) Math.round(elapsedSeconds);
        return "%02d:%02d".formatted(seconds / 60, seconds % 60);
    }
}
