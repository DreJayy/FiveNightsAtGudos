package com.gudos.game.minigame;

import java.awt.Rectangle;
import java.util.List;

/** Level configuration produced by the builder, independent of gameplay and Swing rendering. */
/*
 * DEFENSE OVERVIEW: SecretLevel
 *
 * Purpose: Stores the level configuration produced by SecretLevelBuilder.
 *
 * Follow the flow: Application creates the product; SecretGame reads dimensions, spawn points,
 * cover, platforms, destination and patrols.
 *
 * Code-based rationale and tradeoff: The constructor copies incoming Rectangle objects and lists, so
 * later changes to builder inputs do not change this product. Returned Rectangles remain mutable;
 * this is not deep immutability after access.
 *
 * State to recognize: playerX, bundleX, destination, platforms, cover, patrols, seconds and width.
 * Nested Patrol holds left/right world-coordinate endpoints.
 *
 * Defense answer: SecretLevel is configuration, SecretGame is changing runtime state, and
 * SecretPanel is the view/controller.
 */
public record SecretLevel(double playerX, double bundleX, Rectangle destination,
                          List<Rectangle> platforms, List<Rectangle> cover,
                          List<Patrol> patrols, double seconds, int width) {
    // Patrol endpoints and all rectangles use world coordinates, not the scrolling screen position.
    /*
     * DEFENSE TYPE: Patrol
     *
     * Immutable left/right world-coordinate endpoints for one witness patrol.
     */
    public record Patrol(double left, double right) { }
    /*
     * DEFENSE: SecretLevel constructor
     *
     * Copy destination and every platform/cover Rectangle, and copy the patrol list during
     * construction. This protects against later mutation of input objects; returned Rectangle
     * accessors still expose mutable objects and should be treated read-only.
     */
    public SecretLevel {
        // Copy construction inputs so later changes to the builder's rectangles do not change the level.
        // Rectangle itself is mutable, so callers should treat the returned configuration as read-only.
        destination = new Rectangle(destination);
        platforms = platforms.stream().map(Rectangle::new).toList();
        cover = cover.stream().map(Rectangle::new).toList();
        patrols = List.copyOf(patrols);
    }
}
