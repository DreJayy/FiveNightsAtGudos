package com.gudos.ui;

import com.gudos.assets.AssetManager;

import java.awt.*;

/** Links the Extras menu's labels to the corresponding Navigation.Page requests. */
/*
 * ExtrasPanel
 *
 * Purpose: Routes five text entries to secondary pages.
 *
 * Execution flow: Select Night, How to Play, Shift Records, Settings and Credits map in order to
 * Navigation.Page values.
 *
 * Design rationale: Keep a small routing screen without placing database or
 * gameplay logic here. Capturing index per loop keeps each callback attached to the correct page.
 *
 * State: Labels and Page arrays must stay in the same order.
 *
 * Summary: Extras is an entry menu; Application.page chooses and loads the actual panel.
 */
public final class ExtrasPanel extends BackdropPanel {
    /*
     * Install shared Back and build the five label/Page pairs. Capture each iteration index for its
     * callback so buttons navigate to the intended page.
     */
    public ExtrasPanel(AssetManager assets, Navigation navigation) {
        super(assets);
        backButton(navigation);
        String[] labels = {"Select Night", "How to Play", "Shift Records", "Settings", "Credits"};
        Navigation.Page[] pages = {Navigation.Page.NIGHTS, Navigation.Page.GUIDE, Navigation.Page.RECORDS, Navigation.Page.SETTINGS, Navigation.Page.CREDITS};
        for (int i = 0; i < labels.length; i++) {
            int index = i;
            GameButton button = new GameButton(labels[i], GameButton.Style.TEXT, () -> navigation.page(pages[index]));
            button.setFont(Theme.pixel(42));
            place(button, 104, 236 + i * 66, 620, 55);
        }
    }

    /*
     * Draw the shared Extras page chrome. The five real navigation buttons were constructed earlier
     * and Swing paints them as children.
     */
    @Override protected void render(Graphics2D g) {
        pageChrome(g, "Extras", "");
    }
}
