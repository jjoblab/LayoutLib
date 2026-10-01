package jo.layoutlib.inflater;

import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Collecte les informations de hiérarchie après rendu, inspiré de
 * {@code com.android.ide.common.rendering.api.ViewInfo} de l'AOSP.
 *
 * <p>Après qu'un layout a été mesuré et positionné, cette classe parcourt
 * l'arbre de vues et collecte pour chaque vue :</p>
 * <ul>
 *   <li>Position (left, top, right, bottom)</li>
 *   <li>Dimensions (width, height)</li>
 *   <li>Classe de la vue</li>
 *   <li>Id (si défini)</li>
 *   <li>Visibilité</li>
 *   <li>Padding et marges</li>
 *   <li>Profondeur dans la hiérarchie</li>
 *   <li>Enfants (récursif)</li>
 * </ul>
 *
 * <p>Ces informations sont utilisées par le mode design/blueprint/overlays
 * pour dessiner les overlays (marges, padding, sélection, wireframe).</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ViewInfoCollector {

    /**
     * Information sur une vue dans la hiérarchie rendue.
     *
     * @author jo@Dev
     */
    public static class ViewInfo {

        public final String className;
        public final String simpleName;
        public final int id;
        public final String idName;
        public final int left, top, right, bottom;
        public final int width, height;
        public final int paddingLeft, paddingTop, paddingRight, paddingBottom;
        public final int marginLeft, marginTop, marginRight, marginBottom;
        public final int visibility;
        public final int depth;
        public final boolean isViewGroup;
        public final List<ViewInfo> children;

        public ViewInfo(String className, String simpleName, int id, String idName,
                        int left, int top, int right, int bottom,
                        int width, int height,
                        int paddingLeft, int paddingTop, int paddingRight, int paddingBottom,
                        int marginLeft, int marginTop, int marginRight, int marginBottom,
                        int visibility, int depth, boolean isViewGroup,
                        List<ViewInfo> children) {
            this.className = className;
            this.simpleName = simpleName;
            this.id = id;
            this.idName = idName;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.width = width;
            this.height = height;
            this.paddingLeft = paddingLeft;
            this.paddingTop = paddingTop;
            this.paddingRight = paddingRight;
            this.paddingBottom = paddingBottom;
            this.marginLeft = marginLeft;
            this.marginTop = marginTop;
            this.marginRight = marginRight;
            this.marginBottom = marginBottom;
            this.visibility = visibility;
            this.depth = depth;
            this.isViewGroup = isViewGroup;
            this.children = children;
        }

        public boolean isVisible() {
            return visibility == View.VISIBLE;
        }

        public boolean hasId() {
            return id != View.NO_ID;
        }

        @Override
        public String toString() {
            return simpleName + (idName != null ? " @" + idName : "")
                    + " [" + left + "," + top + "," + right + "," + bottom + "]"
                    + " " + width + "x" + height;
        }
    }

    /**
     * Collecte la hiérarchie complète d'une vue racine.
     *
     * <p>Les positions renvoyées ({@code left/top/right/bottom}) sont
     * <strong>absolues</strong>, c'est-à-dire relatives à la racine du
     * snapshot — pas au parent direct. C'est ce que consomme l'OverlayView
     * et ce que renvoie le {@code ViewInfo} officiel de l'AOSP. Sans cette
     * convention, les petits-enfants seraient dessinés à l'offset du parent,
     * et la sélection / hit-testing / guides / handles seraient décalés.</p>
     *
     * @param rootView la vue racine (déjà mesurée et positionnée)
     * @return les ViewInfo de la racine
     */
    public static ViewInfo collect(View rootView) {
        return collectInternal(rootView, 0, null, 0, 0);
    }

    /**
     * Collecte récursive.
     *
     * @param view     la vue courante
     * @param depth    la profondeur
     * @param parent   le parent (pour lire les marges)
     * @param offsetX  offset X cumulé des ancêtres (pour calculer la position absolue)
     * @param offsetY  offset Y cumulé des ancêtres
     * @return les ViewInfo
     */
    private static ViewInfo collectInternal(View view, int depth, ViewGroup parent,
                                            int offsetX, int offsetY) {
        if (view == null) return null;

        String className = view.getClass().getName();
        String simpleName = view.getClass().getSimpleName();
        int id = view.getId();
        String idName = null;
        if (id != View.NO_ID) {
            try {
                idName = view.getContext().getResources().getResourceEntryName(id);
            } catch (Exception ignored) {}
        }

        // ── Positions ABSOLUES (root-relative) ───────────────────────────
        // View.getLeft()/getTop()/getRight()/getBottom() sont relatifs au
        // parent direct. On ajoute l'offset cumulé pour obtenir une position
        // absolue (relative à la racine du snapshot).
        int absLeft   = view.getLeft()   + offsetX;
        int absTop    = view.getTop()    + offsetY;
        int absRight  = view.getRight()  + offsetX;
        int absBottom = view.getBottom() + offsetY;

        // Marges (depuis les LayoutParams du parent)
        int marginLeft = 0, marginTop = 0, marginRight = 0, marginBottom = 0;
        if (parent != null && view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            marginLeft = mlp.leftMargin;
            marginTop = mlp.topMargin;
            marginRight = mlp.rightMargin;
            marginBottom = mlp.bottomMargin;
        }

        // Enfants — on propage l'offset cumulé (absLeft/absTop du parent)
        List<ViewInfo> children = new ArrayList<>();
        boolean isViewGroup = view instanceof ViewGroup;
        if (isViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                ViewInfo child = collectInternal(group.getChildAt(i), depth + 1, group,
                        absLeft, absTop);
                if (child != null) {
                    children.add(child);
                }
            }
        }

        return new ViewInfo(
                className, simpleName, id, idName,
                absLeft, absTop, absRight, absBottom,
                view.getWidth(), view.getHeight(),
                view.getPaddingLeft(), view.getPaddingTop(),
                view.getPaddingRight(), view.getPaddingBottom(),
                marginLeft, marginTop, marginRight, marginBottom,
                view.getVisibility(), depth, isViewGroup, children);
    }

    /**
     * Compte le nombre total de vues dans la hiérarchie.
     *
     * @param info les ViewInfo racine
     * @return le nombre total
     */
    public static int countViews(ViewInfo info) {
        if (info == null) return 0;
        int count = 1;
        for (ViewInfo child : info.children) {
            count += countViews(child);
        }
        return count;
    }

    /**
     * Trouve une vue par id dans la hiérarchie.
     *
     * @param info les ViewInfo racine
     * @param id   l'id à chercher
     * @return les ViewInfo trouvés, ou null
     */
    public static ViewInfo findById(ViewInfo info, int id) {
        if (info == null) return null;
        if (info.id == id) return info;
        for (ViewInfo child : info.children) {
            ViewInfo found = findById(child, id);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * Trouve toutes les vues à une position (x, y).
     *
     * @param info les ViewInfo racine
     * @param x    coordonnée X
     * @param y    coordonnée Y
     * @return la liste des vues à cette position (du plus profond au moins profond)
     */
    public static List<ViewInfo> findAtPoint(ViewInfo info, int x, int y) {
        List<ViewInfo> results = new ArrayList<>();
        findAtPointInternal(info, x, y, results);
        return results;
    }

    private static void findAtPointInternal(ViewInfo info, int x, int y, List<ViewInfo> results) {
        if (info == null || !info.isVisible()) return;
        if (x >= info.left && x <= info.right && y >= info.top && y <= info.bottom) {
            // Chercher d'abord dans les enfants (plus profond = prioritaire)
            for (ViewInfo child : info.children) {
                findAtPointInternal(child, x, y, results);
            }
            results.add(info);
        }
    }
}
