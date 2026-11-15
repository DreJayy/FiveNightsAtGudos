package com.gudos.game;

/** Room identifiers shared by gameplay, camera controls, and the scene-image manifest. */
/*
 * DEFENSE OVERVIEW: Scene
 *
 * Purpose: Defines the office and six canonical camera rooms.
 *
 * Follow the flow: Game controls validate a camera number with Scene.camera(number); logic and
 * AssetManager share the enum id.
 *
 * Code-based rationale and tradeoff: Central identifiers keep camera labels, route locations and
 * image manifest keys aligned. CAM 01 is currently Front entrance, despite some older dining
 * filenames/text.
 *
 * State to recognize: OFFICE has camera=0; camera rooms occupy enum positions 1-6. id values such as
 * cam04 match scenes.properties.
 *
 * Defense answer: Scene identifies a place; the separate state string identifies clean, occupied or
 * another render for that place.
 */
public enum Scene {
    OFFICE("office", "Security office", 0),
    CAM_01("cam01", "Front entrance", 1),
    CAM_02("cam02", "Serving counter", 2),
    CAM_03("cam03", "Kitchen", 3),
    CAM_04("cam04", "Stockroom", 4),
    CAM_05("cam05", "Side corridor", 5),
    CAM_06("cam06", "Back alley", 6);

    private final String id;
    private final String label;
    private final int camera;

    /*
     * DEFENSE: Scene constructor
     *
     * Store canonical manifest id, readable label and camera number for this enum entry. Logic, UI
     * and assets all share the same room identity.
     */
    Scene(String id, String label, int camera) {
        this.id = id;
        this.label = label;
        this.camera = camera;
    }

    /*
     * DEFENSE: id
     *
     * Read the stable attempt UUID used for save deduplication. This accessor does not advance time,
     * save data or change the model; callers use it to draw or coordinate behavior.
     */
    public String id() { return id; }
    /*
     * DEFENSE: label
     *
     * Read the readable UI label. This accessor does not advance time, save data or change the
     * model; callers use it to draw or coordinate behavior.
     */
    public String label() { return label; }
    /*
     * DEFENSE: camera
     *
     * The instance accessor returns this scene camera number; the static overload validates an input
     * in 1..6 and uses its enum position. OFFICE is position zero, so values()[number] matches the
     * camera scenes. Reordering enum entries without updating this rule would break that mapping.
     */
    public int camera() { return camera; }

    /*
     * DEFENSE: camera
     *
     * The instance accessor returns this scene camera number; the static overload validates an input
     * in 1..6 and uses its enum position. OFFICE is position zero, so values()[number] matches the
     * camera scenes. Reordering enum entries without updating this rule would break that mapping.
     */
    public static Scene camera(int number) {
        // OFFICE occupies ordinal 0, so validated camera numbers map to the remaining entries.
        if (number < 1 || number > 6) throw new IllegalArgumentException("Camera must be 1–6");
        return values()[number];
    }
}
