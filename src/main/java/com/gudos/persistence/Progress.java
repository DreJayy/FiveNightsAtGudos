package com.gudos.persistence;

/** Database summary used by Continue, night selection, and records; counts are derived from attempts. */
/*
 * Progress
 *
 * Purpose: Provides a loaded summary for menu, night-selection and records screens.
 *
 * Execution flow: Database.loadProgress calculates counts and reads unlock fields; Application
 * passes the value to panels.
 *
 * Design rationale: Keep one summarized value for UI decisions instead of running
 * SQL during every repaint. EMPTY represents a fresh profile.
 *
 * State: EMPTY=(1,0,0,0). unlocked accepts nights from 1 through nextNight; application
 * data comes from constrained database rows.
 *
 * Summary: nextNight is capped at five. highestCompleted is the furthest normal night
 * survived; totalRuns and completedRuns are counts, not night identifiers.
 */
public record Progress(int nextNight, int highestCompleted, int totalRuns, int completedRuns) {
    public static final Progress EMPTY = new Progress(1, 0, 0, 0);
    /*
     * Return true for a requested night>=1 and <=nextNight. Normal nextNight values are
     * constrained/capped at five by database/application writes. The UI and Application both use
     * this eligibility check.
     */
    public boolean unlocked(int night) { return night >= 1 && night <= nextNight; }
}
