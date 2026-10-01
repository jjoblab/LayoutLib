package jo.layoutlib.layout.specs;

import android.view.View;

/**
 * Resolver de MeasureSpec, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class MeasureSpecResolver {

    /**
     * Extrait la taille d un MeasureSpec.
     *
     * @param spec le MeasureSpec
     * @return la taille
     */
    public static int getSize(int spec) {
        return View.MeasureSpec.getSize(spec);
    }

    /**
     * Extrait le mode d un MeasureSpec.
     *
     * @param spec le MeasureSpec
     * @return le mode
     */
    public static Mode getMode(int spec) {
        return MeasureSpecImpl.getMode(spec);
    }

    /**
     * Crée un Size depuis un MeasureSpec.
     *
     * @param spec le MeasureSpec
     * @return le Size
     */
    public static Size toSize(int spec) {
        return new Size(getSize(spec), getMode(spec));
    }

    /**
     * Indique si un MeasureSpec est EXACTLY.
     *
     * @param spec le MeasureSpec
     * @return true si EXACTLY
     */
    public static boolean isExactly(int spec) {
        return getMode(spec) == Mode.EXACTLY;
    }

    /**
     * Indique si un MeasureSpec est AT_MOST.
     *
     * @param spec le MeasureSpec
     * @return true si AT_MOST
     */
    public static boolean isAtMost(int spec) {
        return getMode(spec) == Mode.AT_MOST;
    }
}
