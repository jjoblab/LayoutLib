package jo.layoutlib.layout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * LayoutInfo — informations sur un layout après inflation.
 *
 * <p>Utilise l'API Android native pour extraire les informations réelles
 * d'une vue mesurée et positionnée.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class LayoutInfo {

    /** Vue racine du layout. */
    private final View rootView;

    /** Largeur mesurée. */
    private final int measuredWidth;

    /** Hauteur mesurée. */
    private final int measuredHeight;

    /** Profondeur de la hiérarchie. */
    private final int depth;

    /** Nombre total de vues. */
    private final int viewCount;

    /**
     * Construit un LayoutInfo depuis une vue racine.
     *
     * @param rootView la vue racine déjà mesurée et positionnée
     */
    public LayoutInfo(View rootView) {
        this.rootView = rootView;
        this.measuredWidth = rootView != null ? rootView.getMeasuredWidth() : 0;
        this.measuredHeight = rootView != null ? rootView.getMeasuredHeight() : 0;
        this.depth = computeDepth(rootView);
        this.viewCount = countViews(rootView);
    }

    /**
     * Compte la profondeur de la hiérarchie.
     *
     * @param view la vue racine
     * @return la profondeur (1 pour une vue seule)
     */
    private static int computeDepth(View view) {
        if (view == null) return 0;
        if (!(view instanceof ViewGroup)) return 1;
        ViewGroup group = (ViewGroup) view;
        int maxChildDepth = 0;
        for (int i = 0; i < group.getChildCount(); i++) {
            maxChildDepth = Math.max(maxChildDepth, computeDepth(group.getChildAt(i)));
        }
        return 1 + maxChildDepth;
    }

    /**
     * Compte le nombre total de vues dans l'arbre.
     *
     * @param view la vue racine
     * @return le nombre total
     */
    private static int countViews(View view) {
        if (view == null) return 0;
        int count = 1;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                count += countViews(group.getChildAt(i));
            }
        }
        return count;
    }

    public View getRootView() {
        return rootView;
    }

    public int getMeasuredWidth() {
        return measuredWidth;
    }

    public int getMeasuredHeight() {
        return measuredHeight;
    }

    public int getDepth() {
        return depth;
    }

    public int getViewCount() {
        return viewCount;
    }

    /**
     * @return la classe de la vue racine
     */
    public String getRootViewClassName() {
        return rootView != null ? rootView.getClass().getSimpleName() : "null";
    }

    @Override
    public String toString() {
        return "LayoutInfo{" + getRootViewClassName()
                + " " + measuredWidth + "x" + measuredHeight
                + ", depth=" + depth
                + ", views=" + viewCount + "}";
    }
}
