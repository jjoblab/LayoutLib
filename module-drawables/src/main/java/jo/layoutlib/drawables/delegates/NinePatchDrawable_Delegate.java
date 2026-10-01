package jo.layoutlib.drawables.delegates;

import android.graphics.drawable.NinePatchDrawable;
import android.content.Context;
import android.content.res.Resources;

/**
 * Delegate pour NinePatchDrawable, utilise l API Android native.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class NinePatchDrawable_Delegate {

    public NinePatchDrawable_Delegate() {
    }

    /**
     * Crée un drawable depuis un id de resource.
     *
     * @param context le contexte
     * @param resId l id de resource
     * @return le drawable, ou null
     */
    public static Object createFromResource(Context context, int resId) {
        if (context == null || resId == 0) return null;
        try {
            return context.getResources().getDrawable(resId, context.getTheme());
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    /**
     * Indique si ce type de drawable est supporté sur l API courante.
     *
     * @return true
     */
    public static boolean isSupported() {
        return true;
    }
}
