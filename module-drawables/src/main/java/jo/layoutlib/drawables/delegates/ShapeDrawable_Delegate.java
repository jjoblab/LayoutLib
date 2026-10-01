package jo.layoutlib.drawables.delegates;

/**
 * Delegate pour ShapeDrawable, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ShapeDrawable_Delegate {

    /**
     * Indique si ce type de drawable est supporté.
     *
     * @return true
     */
    public static boolean isSupported() {
        return true;
    }

    /**
     * @return la version minimale d API
     */
    public static int getMinApiLevel() {
        return 21;
    }
}
