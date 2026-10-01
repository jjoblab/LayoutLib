package jo.layoutlib.drawables.delegates;

/**
 * Delegate pour VectorDrawable, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class VectorDrawable_Delegate {

    /**
     * Indique si un VectorDrawable supporte le tint.
     *
     * @return true (toujours supporté sur API 21+)
     */
    public static boolean supportsTint() {
        return true;
    }

    /**
     * @return la version minimale d API pour VectorDrawable
     */
    public static int getMinApiLevel() {
        return 21;
    }
}
