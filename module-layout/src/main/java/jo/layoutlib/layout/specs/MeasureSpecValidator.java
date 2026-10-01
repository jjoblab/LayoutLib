package jo.layoutlib.layout.specs;

/**
 * Validateur de MeasureSpec, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public final class MeasureSpecValidator {

    private MeasureSpecValidator() {
    }

    /**
     * Valide qu un MeasureSpec a une taille valide.
     *
     * @param spec le MeasureSpec
     * @return true si valide
     */
    public static boolean isValid(int spec) {
        int size = MeasureSpecResolver.getSize(spec);
        Mode mode = MeasureSpecResolver.getMode(spec);
        if (mode == Mode.UNSPECIFIED) {
            return true;  // size peut être 0
        }
        return size >= 0;
    }

    /**
     * Valide qu un MeasureSpec est compatible avec une taille cible.
     *
     * @param spec le MeasureSpec
     * @param targetSize la taille cible
     * @return true si compatible
     */
    public static boolean isCompatible(int spec, int targetSize) {
        if (!isValid(spec)) return false;
        Mode mode = MeasureSpecResolver.getMode(spec);
        int size = MeasureSpecResolver.getSize(spec);
        if (mode == Mode.EXACTLY) {
            return size == targetSize;
        }
        if (mode == Mode.AT_MOST) {
            return size <= targetSize;
        }
        return true;  // UNSPECIFIED toujours compatible
    }
}
