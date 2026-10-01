package jo.layoutlib.inflater.bridge.util;

/**
 * Utilitaire de calcul d'insets, inspiré de
 * {@code com.android.layoutlib.bridge.util.InsetUtil} de l'AOSP.
 *
 * <p>Cette classe calcule les insets (marges internes) d'une vue en
 * fonction des attributs XML {@code paddingStart}, {@code paddingEnd},
 * {@code paddingLeft}, {@code paddingRight}, {@code paddingTop},
 * {@code paddingBottom}, et {@code padding}.</p>
 *
 * <h2>Algorithme</h2>
 * <p>Les règles de précédence Android sont :</p>
 * <ol>
 *   <li>{@code padding} définit les 4 côtés</li>
 *   <li>{@code paddingHorizontal} définit gauche + droit</li>
 *   <li>{@code paddingVertical} définit haut + bas</li>
 *   <li>{@code paddingLeft/Right/Top/Bottom} surchargent individuellement</li>
 *   <li>{@code paddingStart/End} surchargent left/right en LTR</li>
 * </ol>
 *
 * @author jo@Dev
 * @since 1.0
 */
public final class InsetUtil {

    /** Constructeur privé. */
    private InsetUtil() {
    }

    /**
     * Calcule les 4 paddings finaux à partir des valeurs XML.
     *
     * @param padding      valeur de {@code padding} (ou 0)
     * @param paddingHorizontal valeur de {@code paddingHorizontal} (ou -1)
     * @param paddingVertical   valeur de {@code paddingVertical} (ou -1)
     * @param paddingLeft  valeur de {@code paddingLeft} (ou -1)
     * @param paddingRight valeur de {@code paddingRight} (ou -1)
     * @param paddingTop   valeur de {@code paddingTop} (ou -1)
     * @param paddingBottom valeur de {@code paddingBottom} (ou -1)
     * @param paddingStart valeur de {@code paddingStart} (ou -1)
     * @param paddingEnd   valeur de {@code paddingEnd} (ou -1)
     * @param isRtl        {@code true} si le layout est RTL
     * @return tableau [left, top, right, bottom]
     */
    public static int[] computePadding(
            int padding, int paddingHorizontal, int paddingVertical,
            int paddingLeft, int paddingRight, int paddingTop, int paddingBottom,
            int paddingStart, int paddingEnd, boolean isRtl) {

        int left = padding;
        int top = padding;
        int right = padding;
        int bottom = padding;

        // paddingHorizontal surcharge left + right
        if (paddingHorizontal >= 0) {
            left = paddingHorizontal;
            right = paddingHorizontal;
        }
        // paddingVertical surcharge top + bottom
        if (paddingVertical >= 0) {
            top = paddingVertical;
            bottom = paddingVertical;
        }
        // paddingLeft/Right/Top/Bottom surchargent individuellement
        if (paddingLeft >= 0) left = paddingLeft;
        if (paddingRight >= 0) right = paddingRight;
        if (paddingTop >= 0) top = paddingTop;
        if (paddingBottom >= 0) bottom = paddingBottom;

        // paddingStart/End surchargent en fonction de la direction
        if (paddingStart >= 0) {
            if (isRtl) {
                right = paddingStart;
            } else {
                left = paddingStart;
            }
        }
        if (paddingEnd >= 0) {
            if (isRtl) {
                left = paddingEnd;
            } else {
                right = paddingEnd;
            }
        }

        return new int[]{left, top, right, bottom};
    }

    /**
     * Calcule les margins finaux (même algorithme que padding).
     *
     * @param margin         valeur de {@code layout_margin}
     * @param marginHorizontal valeur de {@code layout_marginHorizontal}
     * @param marginVertical   valeur de {@code layout_marginVertical}
     * @param marginLeft      valeur de {@code layout_marginLeft}
     * @param marginRight     valeur de {@code layout_marginRight}
     * @param marginTop       valeur de {@code layout_marginTop}
     * @param marginBottom    valeur de {@code layout_marginBottom}
     * @param marginStart     valeur de {@code layout_marginStart}
     * @param marginEnd       valeur de {@code layout_marginEnd}
     * @param isRtl           {@code true} si RTL
     * @return tableau [left, top, right, bottom]
     */
    public static int[] computeMargin(
            int margin, int marginHorizontal, int marginVertical,
            int marginLeft, int marginRight, int marginTop, int marginBottom,
            int marginStart, int marginEnd, boolean isRtl) {
        return computePadding(margin, marginHorizontal, marginVertical,
                marginLeft, marginRight, marginTop, marginBottom,
                marginStart, marginEnd, isRtl);
    }

    /**
     * Convertit une chaîne en int, ou retourne -1 si invalide.
     *
     * @param value la chaîne (ex. {@code "16dp"})
     * @return la valeur entière, ou -1
     */
    public static int parseOptionalInt(String value) {
        if (value == null || value.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(value.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
