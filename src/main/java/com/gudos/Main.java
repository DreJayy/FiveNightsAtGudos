package com.gudos;

import com.gudos.assets.AssetManager;
import com.gudos.persistence.*;
import com.gudos.ui.LoadingPanel;
import com.gudos.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/** Entry point: creates the window, loads local data/assets, then hands control to Application. */
/*
 * Main
 *
 * Purpose: Starts the desktop application and loads its dependencies.
 *
 * Execution flow: main schedules launch on Swing; launch shows LoadingPanel; SwingWorker loads
 * paths, settings, database, progress and images; done constructs Application.
 *
 * Design rationale: The window can repaint while disk and image loading happen.
 * Boot is a record that transfers the loaded values together. Startup failures display a message and
 * dispose the window.
 *
 * State: Boot contains AppPaths, SettingsStore, Settings, Database, Progress and
 * AssetManager. The loading Timer exists only during startup.
 *
 * Summary: Main is the entry point, not the gameplay loop. It prepares the program and hands
 * control to Application.
 */
public final class Main {
    /*
     * Private constructor prevents creating utility entry-point objects. main and launch are static;
     * this is not a Singleton instance.
     */
    private Main() { }

    /*
     * Queue launch on the Swing event dispatch thread, then return. Swing component creation belongs
     * on that thread; doing all startup synchronously here would either use the wrong thread or
     * block the window.
     */
    public static void main(String[] args) {
        // Swing components must be created and changed on its event dispatch thread (EDT).
        SwingUtilities.invokeLater(Main::launch);
    }

    /*
     * Set the visual defaults, create and show a loading window, and start a SwingWorker.
     * doInBackground loads writable paths, preferences, SQLite progress and assets; done stops the
     * loading timer and constructs Application on the EDT. A failed boot displays the real cause and
     * closes the window.
     */
    private static void launch() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) { /* Platform fallback remains usable. */ }
        UIManager.put("OptionPane.background", new Color(18, 18, 18));
        UIManager.put("Panel.background", new Color(18, 18, 18));
        UIManager.put("OptionPane.messageForeground", Theme.PAPER);
        UIManager.put("OptionPane.messageFont", Theme.sans(15));
        UIManager.put("Button.font", Theme.sans(14));
        UIManager.put("Button.background", new Color(55, 55, 55));
        UIManager.put("Button.foreground", Theme.PAPER);
        JFrame frame = new JFrame("Five Nights at Gudo's");
        frame.setIconImage(icon());
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        LoadingPanel loading = new LoadingPanel("Getting the carinderia ready…");
        frame.setContentPane(loading);
        frame.pack();
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setSize(Math.min(1296, screen.width - 40), Math.min(759, screen.height - 40));
        frame.setMinimumSize(new Dimension(Math.min(960, screen.width - 40), Math.min(580, screen.height - 40)));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        // Animate the loading screen while the worker below performs slower disk operations.
        Timer loadingTimer = new Timer(33, event -> { loading.tick(0.033); loading.repaint(); });
        loadingTimer.start();
        new SwingWorker<Boot, Void>() {
            /*
             * Run boot IO and image decoding on the SwingWorker background thread. Resolve paths
             * first, because the stores need those paths; load Settings before profile progress,
             * because the saved playerName selects the profile. Return every dependency in one Boot
             * value.
             */
            @Override protected Boot doInBackground() throws Exception {
                // Resolve the save folder, read preferences, open SQLite, and load this profile.
                AppPaths paths = AppPaths.resolve();
                SettingsStore store = new SettingsStore(paths.settings());
                Settings settings = store.load();
                Database database = new Database(paths.database());
                Progress progress = database.loadProgress(settings.playerName());
                AssetManager assets = new AssetManager();
                return new Boot(paths, store, settings, database, progress, assets);
            }

            /*
             * Run after the boot worker finishes, on the EDT. Stop loading animation; ignore
             * completion if the window was closed; otherwise get the Boot result and create
             * Application. Unwrap/report a failed worker instead of opening a partly initialized
             * game.
             */
            @Override protected void done() {
                // SwingWorker returns here on the EDT, so it is safe to install the game UI.
                loadingTimer.stop();
                if (!frame.isDisplayable()) return;
                try {
                    Boot boot = get();
                    new Application(frame, boot.assets(), boot.paths(), boot.database(), boot.store(), boot.settings(), boot.progress());
                } catch (Exception error) {
                    Throwable cause = error.getCause() == null ? error : error.getCause();
                    cause.printStackTrace(System.err);
                    JOptionPane.showMessageDialog(frame,
                            "The carinderia could not be opened.\nCheck that the data folder is writable and launch the built JAR or run.cmd.\n\n" + cause.getMessage(),
                            "Five Nights at Gudo's — startup error", JOptionPane.ERROR_MESSAGE);
                    frame.dispose();
                }
            }
        }.execute();
    }

    // Draw the window icon in memory; no separate icon file is needed.
    /*
     * Draw a small G icon into an in-memory image and dispose its Graphics2D. This avoids relying on
     * another icon resource file.
     */
    private static Image icon() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        Theme.quality(g);
        g.setColor(Theme.BACKGROUND);
        g.fillRoundRect(0, 0, 64, 64, 12, 12);
        g.setColor(Theme.AMBER);
        g.drawRect(9, 9, 45, 45);
        Theme.text(g, "G", 16, 46, Theme.heading(42), Theme.AMBER);
        g.dispose();
        return image;
    }

    // Immutable bundle carrying the worker's results back to the UI thread.
    /*
     * Boot
     *
     * Immutable startup result bundle carried from SwingWorker background work to done on EDT.
     */
    private record Boot(AppPaths paths, SettingsStore store, Settings settings, Database database,
                        Progress progress, AssetManager assets) { }
}
