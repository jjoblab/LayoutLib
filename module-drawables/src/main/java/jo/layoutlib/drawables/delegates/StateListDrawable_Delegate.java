package jo.layoutlib.drawables.delegates;

import android.graphics.drawable.StateListDrawable;

/**
 * Delegate pour StateListDrawable, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class StateListDrawable_Delegate {

    /**
     * Ajoute un état avec son drawable.
     *
     * @param drawable le StateListDrawable
     * @param stateSet l ensemble d états
     * @param child le drawable à afficher pour cet état
     */
    public static void addState(StateListDrawable drawable, int[] stateSet,
                                 android.graphics.drawable.Drawable child) {
        if (drawable != null && child != null) {
            drawable.addState(stateSet != null ? stateSet : new int[0], child);
        }
    }

    /**
     * Récupère le nombre d états.
     *
     * @param drawable le StateListDrawable
     * @return le nombre d états
     */
    public static int getStateCount(StateListDrawable drawable) {
        if (drawable == null) return 0;
        return drawable.getStateCount();
    }
}
