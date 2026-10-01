package jo.layoutlib.drawables.delegates;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

/**
 * Delegate pour BitmapDrawable, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class BitmapDrawable_Delegate {

    /**
     * Crée un BitmapDrawable depuis un Bitmap.
     *
     * @param bitmap le bitmap
     * @return le drawable
     */
    public static BitmapDrawable createFromBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;
        return new BitmapDrawable(null, bitmap);
    }

    /**
     * Crée un BitmapDrawable depuis un fichier.
     *
     * @param path le chemin du fichier
     * @return le drawable, ou null
     */
    public static BitmapDrawable createFromFile(String path) {
        if (path == null) return null;
        Bitmap bm = android.graphics.BitmapFactory.decodeFile(path);
        if (bm == null) return null;
        return createFromBitmap(bm);
    }

    /**
     * Récupère le bitmap d un BitmapDrawable.
     *
     * @param drawable le drawable
     * @return le bitmap, ou null
     */
    public static Bitmap getBitmap(BitmapDrawable drawable) {
        if (drawable == null) return null;
        return drawable.getBitmap();
    }
}
