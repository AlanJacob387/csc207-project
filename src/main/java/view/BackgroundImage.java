package view;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

/**
 * The full-screen photo behind the login and sign-up pages.
 *
 * The image is scaled to cover the whole area while keeping its aspect ratio,
 * and a dark overlay is painted on top so the white cards stand out on a bright
 * photo. If the image is missing from the classpath, a dark gradient is painted
 * instead, so the pages still look deliberate.
 *
 * Shared by both pages so they cannot drift apart visually.
 */
public final class BackgroundImage {

    /** Classpath location of the photo (under src/main/resources). */
    public static final String DEFAULT_RESOURCE = "/images/login_stadium.jpg";

    private static final Color GRADIENT_TOP = new Color(10, 25, 60);
    private static final Color GRADIENT_BOTTOM = new Color(20, 90, 55);
    private static final Color OVERLAY = new Color(0, 0, 0, 70);

    private final Image image;

    /** Loads the default background photo. */
    public BackgroundImage() {
        this(DEFAULT_RESOURCE);
    }

    /**
     * Loads a background photo from the classpath.
     *
     * @param resourcePath the classpath location, for example /images/login_stadium.jpg
     */
    public BackgroundImage(String resourcePath) {
        this.image = load(resourcePath);
    }

    /**
     * Paints the background so it fills the given area.
     *
     * @param g2     the graphics context to paint with
     * @param width  the width to fill
     * @param height the height to fill
     */
    public void paint(Graphics2D g2, int width, int height) {
        if (image == null) {
            g2.setPaint(new GradientPaint(0, 0, GRADIENT_TOP, 0, height, GRADIENT_BOTTOM));
            g2.fillRect(0, 0, width, height);
            return;
        }

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Scale to cover: fill the whole area, keep the aspect ratio, crop the overflow.
        final int imageWidth = image.getWidth(null);
        final int imageHeight = image.getHeight(null);
        final double scale = Math.max((double) width / imageWidth,
                (double) height / imageHeight);
        final int drawWidth = (int) Math.ceil(imageWidth * scale);
        final int drawHeight = (int) Math.ceil(imageHeight * scale);
        g2.drawImage(image, (width - drawWidth) / 2, (height - drawHeight) / 2,
                drawWidth, drawHeight, null);

        g2.setColor(OVERLAY);
        g2.fillRect(0, 0, width, height);
    }

    /** Reads the photo from the classpath, or null if it cannot be read. */
    private static Image load(String resourcePath) {
        final URL url = BackgroundImage.class.getResource(resourcePath);
        if (url == null) {
            System.err.println("Background image not found on classpath: " + resourcePath);
            return null;
        }
        try {
            return ImageIO.read(url);
        }
        catch (IOException ex) {
            System.err.println("Failed to load background image: " + ex.getMessage());
            return null;
        }
    }
}
