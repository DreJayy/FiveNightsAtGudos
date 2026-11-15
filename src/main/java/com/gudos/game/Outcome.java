package com.gudos.game;

/** Stored enum names remain stable in SQLite; labels are the player-facing descriptions. */
/*
 * DEFENSE OVERVIEW: Outcome
 *
 * Purpose: Defines the six final result categories stored in SQLite.
 *
 * Follow the flow: GameSession.finish selects the enum; Database stores name(); UI/logs display
 * label().
 *
 * Code-based rationale and tradeoff: Stable machine names separate storage from readable labels.
 * Only COMPLETED advances normal-night progress; MINIGAME_COMPLETED is a different outcome.
 *
 * State to recognize: COMPLETED, POWER_OUT, ABANDONED, CAUGHT, MINIGAME_FAILED, MINIGAME_COMPLETED.
 *
 * Defense answer: Caught, power failure, leaving early and secret results all count as attempts;
 * only normal survival unlocks nights.
 */
public enum Outcome {
    COMPLETED("Completed"), POWER_OUT("Power failure"), ABANDONED("Left shift"), CAUGHT("Caught by Sir Gudo"), MINIGAME_FAILED("Secret failed"), MINIGAME_COMPLETED("Secret completed");

    private final String label;

    /*
     * DEFENSE: Outcome constructor
     *
     * Store the readable label alongside the stable enum name. SQL uses the name; UI and text
     * reports use the label.
     */
    Outcome(String label) { this.label = label; }
    /*
     * DEFENSE: label
     *
     * Read the readable UI label. This accessor does not advance time, save data or change the
     * model; callers use it to draw or coordinate behavior.
     */
    public String label() { return label; }
}
