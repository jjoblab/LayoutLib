package jo.layoutlib.inflater.bridge.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * Représentation interne d'un layout mesuré et positionné, inspiré de
 * {@code com.android.layoutlib.bridge.impl.Layout} de l'AOSP.
 *
 * <p>Cette classe stocke le résultat d'un layout complet : dimensions
 * de la vue racine, liste des vues indexées par id, profondeur de
 * hiérarchie, etc. Elle est produite par {@code RenderSessionImpl} après
 * l'appel à {@code View.measure()} et {@code View.layout()}.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class Layout {

    /** Largeur mesurée du layout. */
    private int measuredWidth;

    /** Hauteur mesurée du layout. */
    private int measuredHeight;

    /** Padding gauche du layout. */
    private int paddingLeft;

    /** Padding haut du layout. */
    private int paddingTop;

    /** Padding droit du layout. */
    private int paddingRight;

    /** Padding bas du layout. */
    private int paddingBottom;

    /** Liste des vues indexées par id. */
    private final List<ViewInfo> views = new ArrayList<>();

    /** Profondeur maximale de la hiérarchie. */
    private int maxDepth;

    /** Nombre total de vues. */
    private int viewCount;

    /**
     * Construit un layout vide.
     */
    public Layout() {
    }

    /**
     * @return la largeur mesurée
     */
    public int getMeasuredWidth() {
        return measuredWidth;
    }

    /**
     * Définit la largeur mesurée.
     *
     * @param width la nouvelle largeur
     */
    public void setMeasuredWidth(int width) {
        this.measuredWidth = width;
    }

    /**
     * @return la hauteur mesurée
     */
    public int getMeasuredHeight() {
        return measuredHeight;
    }

    /**
     * Définit la hauteur mesurée.
     *
     * @param height la nouvelle hauteur
     */
    public void setMeasuredHeight(int height) {
        this.measuredHeight = height;
    }

    /**
     * Ajoute une info de vue au layout.
     *
     * @param view l'info à ajouter
     */
    public void addView(ViewInfo view) {
        if (view != null) {
            views.add(view);
            viewCount++;
            if (view.getDepth() > maxDepth) {
                maxDepth = view.getDepth();
            }
        }
    }

    /**
     * @return la liste des vues
     */
    public List<ViewInfo> getViews() {
        return new ArrayList<>(views);
    }

    /**
     * @return le nombre total de vues
     */
    public int getViewCount() {
        return viewCount;
    }

    /**
     * @return la profondeur maximale
     */
    public int getMaxDepth() {
        return maxDepth;
    }

    /**
     * @return le padding gauche
     */
    public int getPaddingLeft() {
        return paddingLeft;
    }

    /**
     * Définit le padding gauche.
     *
     * @param padding le nouveau padding
     */
    public void setPaddingLeft(int padding) {
        this.paddingLeft = padding;
    }

    /**
     * @return le padding haut
     */
    public int getPaddingTop() {
        return paddingTop;
    }

    /**
     * Définit le padding haut.
     *
     * @param padding le nouveau padding
     */
    public void setPaddingTop(int padding) {
        this.paddingTop = padding;
    }

    /**
     * @return le padding droit
     */
    public int getPaddingRight() {
        return paddingRight;
    }

    /**
     * Définit le padding droit.
     *
     * @param padding le nouveau padding
     */
    public void setPaddingRight(int padding) {
        this.paddingRight = padding;
    }

    /**
     * @return le padding bas
     */
    public int getPaddingBottom() {
        return paddingBottom;
    }

    /**
     * Définit le padding bas.
     *
     * @param padding le nouveau padding
     */
    public void setPaddingBottom(int padding) {
        this.paddingBottom = padding;
    }

    /**
     * Définit les 4 paddings d'un coup.
     *
     * @param left   padding gauche
     * @param top    padding haut
     * @param right  padding droit
     * @param bottom padding bas
     */
    public void setPadding(int left, int top, int right, int bottom) {
        this.paddingLeft = left;
        this.paddingTop = top;
        this.paddingRight = right;
        this.paddingBottom = bottom;
    }

    /**
     * Information sur une vue dans le layout.
     *
     * @author jo@Dev
     */
    public static class ViewInfo {

        /** Nom de la classe de la vue. */
        private final String className;

        /** Id de la vue (ou 0 si pas d'id). */
        private final int id;

        /** Position gauche. */
        private final int left;

        /** Position haute. */
        private final int top;

        /** Position droite. */
        private final int right;

        /** Position basse. */
        private final int bottom;

        /** Profondeur dans la hiérarchie. */
        private final int depth;

        /**
         * Construit une ViewInfo.
         *
         * @param className nom de la classe
         * @param id        id de la vue
         * @param left      position gauche
         * @param top       position haute
         * @param right     position droite
         * @param bottom    position basse
         * @param depth     profondeur
         */
        public ViewInfo(String className, int id,
                        int left, int top, int right, int bottom, int depth) {
            this.className = className;
            this.id = id;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.depth = depth;
        }

        public String getClassName() {
            return className;
        }

        public int getId() {
            return id;
        }

        public int getLeft() {
            return left;
        }

        public int getTop() {
            return top;
        }

        public int getRight() {
            return right;
        }

        public int getBottom() {
            return bottom;
        }

        public int getWidth() {
            return right - left;
        }

        public int getHeight() {
            return bottom - top;
        }

        public int getDepth() {
            return depth;
        }

        @Override
        public String toString() {
            return className + " [" + left + "," + top + "," + right + "," + bottom + "]";
        }
    }
}
