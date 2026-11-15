package com.gudos.ui;

import com.gudos.persistence.Settings;

/** Screen requests implemented by Application; panels need not know window or persistence details. */
/*
 * DEFENSE OVERVIEW: Navigation
 *
 * Purpose: Defines the small screen-facing facade contract implemented by Application.
 *
 * Follow the flow: MenuPanel, ExtrasPanel, NightSelectPanel, SettingsPanel, PausePanel and
 * ResultPanel depend on this interface.
 *
 * Code-based rationale and tradeoff: Screens request intent without knowing JFrame, storage workers,
 * GameSession construction or AudioService internals. An interface alone is not proof of Facade; the
 * hidden orchestration in Application supplies the evidence.
 *
 * State to recognize: Page enumerates EXTRAS, NIGHTS, GUIDE, SETTINGS, RECORDS, CREDITS. Operations
 * include menu, page, startNight, saveSettings, resumeShift, leaveShift, quit.
 *
 * Defense answer: startNight is the facade operation used by New Game, Continue, night selection and
 * replay/next-night buttons. There is no method named facade and no separate newGame method.
 */
public interface Navigation {
    /*
     * DEFENSE TYPE: Page
     *
     * Named secondary-screen requests understood by Application.page.
     */
    enum Page { EXTRAS, NIGHTS, GUIDE, SETTINGS, RECORDS, CREDITS }
    /*
     * DEFENSE: menu
     *
     * Request the main menu through Application. Active attempts and incomplete saves may trigger
     * the coordinator abandonment/save flow; the caller does not manipulate cards directly.
     */
    void menu();
    /*
     * DEFENSE: page
     *
     * Request one Page enum. Application selects the panel and asynchronously loads records when
     * needed; panels do not issue SQL themselves.
     */
    void page(Page page);
    /*
     * DEFENSE: startNight
     *
     * Facade contract for starting an eligible fresh attempt. New Game passes 1; Continue passes
     * progress.nextNight; selected/replay/next buttons pass their chosen night. Application hides
     * validation, model/view construction, callback wiring, navigation and audio coordination.
     */
    void startNight(int night);
    /*
     * DEFENSE: saveSettings
     *
     * Submit an immutable Settings value to Application for background preference/profile work and
     * later application. A screen never writes settings.txt directly.
     */
    void saveSettings(Settings settings);
    /*
     * DEFENSE: resumeShift
     *
     * Request resumption of the same paused in-memory normal shift. This is not a database
     * checkpoint restore.
     */
    void resumeShift();
    /*
     * DEFENSE: leaveShift
     *
     * Request confirmed early departure. Application records ABANDONED and waits for saving before
     * returning to the menu.
     */
    void leaveShift();
    /*
     * DEFENSE: quit
     *
     * Request orderly exit. Application handles active/incomplete attempts and resource cleanup
     * before disposing the window.
     */
    void quit();
}
