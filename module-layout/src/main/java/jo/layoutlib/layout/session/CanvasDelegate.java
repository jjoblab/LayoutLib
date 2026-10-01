package jo.layoutlib.layout.session;

import android.graphics.Canvas;
import android.graphics.Picture;

/**
 * Delegate de canvas, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class CanvasDelegate {

    private Picture picture;
    private int width;
    private int height;

    public CanvasDelegate() {
    }

    public CanvasDelegate(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Picture getPicture() {
        return picture;
    }

    public void setPicture(Picture picture) {
        this.picture = picture;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Crée un canvas pour dessiner.
     *
     * @return le canvas, ou null
     */
    public Canvas beginRecording() {
        if (width <= 0 || height <= 0) return null;
        picture = new Picture();
        return picture.beginRecording(width, height);
    }

    /**
     * Termine l enregistrement.
     */
    public void endRecording() {
        if (picture != null) {
            picture.endRecording();
        }
    }
}
