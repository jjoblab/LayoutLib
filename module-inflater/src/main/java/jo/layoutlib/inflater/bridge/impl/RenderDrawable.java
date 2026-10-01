package jo.layoutlib.inflater.bridge.impl;

import android.content.Context;
import android.graphics.drawable.Drawable;

/**
 * Rendu d'un drawable individuel, inspiré de l'AOSP.
 * Utilise android.graphics.drawable.Drawable natif.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class RenderDrawable {

    private final Context context;
    private Drawable drawable;
    private int width;
    private int height;

    public RenderDrawable(Context context) {
        this.context = context;
    }

    public void setDrawable(Drawable drawable) {
        this.drawable = drawable;
        if (drawable != null) {
            this.width = drawable.getIntrinsicWidth();
            this.height = drawable.getIntrinsicHeight();
        }
    }

    public Drawable getDrawable() { return drawable; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Context getContext() { return context; }
}
