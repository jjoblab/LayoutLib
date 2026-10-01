package jo.layoutlib.layout.specs;

import android.view.View;

/**
 * Implémentation de construction de MeasureSpec.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class MeasureSpecImpl {

    /**
     * Construit un MeasureSpec depuis une taille et un mode.
     *
     * @param size la taille
     * @param mode le mode
     * @return le MeasureSpec entier
     */
    public static int makeMeasureSpec(int size, Mode mode) {
        return View.MeasureSpec.makeMeasureSpec(size, mode.getAndroidValue());
    }

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
        return Mode.fromAndroidValue(View.MeasureSpec.getMode(spec));
    }

    /**
     * Crée un MeasureSpec EXACTLY.
     *
     * @param size la taille exacte
     * @return le MeasureSpec
     */
    public static int exactly(int size) {
        return makeMeasureSpec(size, Mode.EXACTLY);
    }

    /**
     * Crée un MeasureSpec AT_MOST.
     *
     * @param size la taille max
     * @return le MeasureSpec
     */
    public static int atMost(int size) {
        return makeMeasureSpec(size, Mode.AT_MOST);
    }

    /**
     * Crée un MeasureSpec UNSPECIFIED.
     *
     * @return le MeasureSpec
     */
    public static int unspecified() {
        return makeMeasureSpec(0, Mode.UNSPECIFIED);
    }
}
