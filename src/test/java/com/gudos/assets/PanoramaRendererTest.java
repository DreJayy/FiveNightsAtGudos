package com.gudos.assets;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/*
 * DEFENSE OVERVIEW: PanoramaRendererTest
 *
 * Purpose: JUnit checks for curved projection and output reuse.
 *
 * Follow the flow: Generate a colored gradient; compare vertical edge/center samples and left/right
 * center pixels; check image object identity.
 *
 * Code-based rationale and tradeoff: A gradient reveals sampling math without relying on a
 * particular room photo. Reused identity proves allocation behavior.
 *
 * State to recognize: gradient is an artificial diagnostic image, not a game asset.
 *
 * Defense answer: The edge samples bend differently from the middle, and turning reveals different
 * source columns.
 */
class PanoramaRendererTest {
    /*
     * DEFENSE: gradient
     *
     * Build a synthetic horizontal/vertical color gradient whose pixel channels reveal the sampled
     * source position.
     */
    private BufferedImage gradient() {
        BufferedImage image = new BufferedImage(1916, 821, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            image.setRGB(x, y, ((x * 255 / 1916) << 16) | ((y * 255 / 821) << 8) | 80);
        }
        return image;
    }

    /*
     * DEFENSE: perspectiveCurvesVerticalSamplingInsteadOfJustCropping
     *
     * Render centered gradient and compare top edge versus top center green channel; require a
     * difference large enough to prove nonflat vertical sampling.
     */
    @Test void perspectiveCurvesVerticalSamplingInsteadOfJustCropping() {
        BufferedImage projected = new PanoramaRenderer().render(gradient(), 0.5);
        int topEdge = (projected.getRGB(0, 0) >>> 8) & 255;
        int topCenter = (projected.getRGB(PanoramaRenderer.WIDTH / 2, 0) >>> 8) & 255;
        assertTrue(topEdge - topCenter > 15, "Cylindrical projection must bend the edges, unlike a flat crop");
    }

    /*
     * DEFENSE: turningRevealsDifferentPartsOfTheRoomAndReusesOutputBuffer
     *
     * Render left then right, compare center pixels for changed view, and assert both calls return
     * the same reused image object. An unchanged view must also reuse it.
     */
    @Test void turningRevealsDifferentPartsOfTheRoomAndReusesOutputBuffer() {
        PanoramaRenderer renderer = new PanoramaRenderer();
        BufferedImage source = gradient();
        BufferedImage left = renderer.render(source, 0);
        int leftCenter = left.getRGB(PanoramaRenderer.WIDTH / 2, PanoramaRenderer.HEIGHT / 2);
        BufferedImage right = renderer.render(source, 1);
        int rightCenter = right.getRGB(PanoramaRenderer.WIDTH / 2, PanoramaRenderer.HEIGHT / 2);
        assertSame(left, right);
        assertNotEquals(leftCenter, rightCenter);
        assertSame(right, renderer.render(source, 1));
    }
}
