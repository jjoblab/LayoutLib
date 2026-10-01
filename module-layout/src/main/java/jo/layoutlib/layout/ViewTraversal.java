package jo.layoutlib.layout;

import android.view.View;
import android.view.ViewGroup;

/**
 * Utilitaire de parcours de hiérarchie de vues Android.
 *
 * <p>Utilise directement l'API native android.view.View et ViewGroup
 * pour parcourir, filtrer et inspecter les arbres de vues.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public final class ViewTraversal {

    /**
     * Listener appelé pour chaque vue parcourue.
     */
    public interface ViewVisitor {

        /**
         * Appelé pour chaque vue.
         *
         * @param view  la vue courante
         * @param depth la profondeur dans la hiérarchie (0 pour la racine)
         * @return true pour continuer le parcours, false pour s'arrêter
         */
        boolean visit(View view, int depth);
    }

    private ViewTraversal() {
    }

    /**
     * Parcourt la hiérarchie en profondeur (pre-order).
     *
     * @param root    la vue racine
     * @param visitor le visiteur
     */
    public static void traverse(View root, ViewVisitor visitor) {
        if (root == null || visitor == null) return;
        traverseInternal(root, 0, visitor);
    }

    private static void traverseInternal(View view, int depth, ViewVisitor visitor) {
        if (view == null) return;
        if (!visitor.visit(view, depth)) return;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                traverseInternal(group.getChildAt(i), depth + 1, visitor);
            }
        }
    }

    /**
     * Compte le nombre total de vues.
     *
     * @param root la vue racine
     * @return le nombre de vues
     */
    public static int countViews(View root) {
        if (root == null) return 0;
        final int[] count = {0};
        traverse(root, (view, depth) -> {
            count[0]++;
            return true;
        });
        return count[0];
    }

    /**
     * Calcule la profondeur maximale.
     *
     * @param root la vue racine
     * @return la profondeur maximale
     */
    public static int maxDepth(View root) {
        if (root == null) return 0;
        final int[] maxDepth = {0};
        traverse(root, (view, depth) -> {
            if (depth > maxDepth[0]) {
                maxDepth[0] = depth;
            }
            return true;
        });
        return maxDepth[0] + 1;
    }

    /**
     * Recherche une vue par id.
     *
     * @param root la vue racine
     * @param id   l'id à rechercher
     * @return la vue trouvée, ou null
     */
    public static View findById(View root, int id) {
        if (root == null || id == View.NO_ID) return null;
        // Utilise findViewById natif (plus efficace)
        if (root instanceof ViewGroup) {
            return root.findViewById(id);
        }
        return root.getId() == id ? root : null;
    }

    /**
     * Recherche une vue par classe.
     *
     * @param root        la vue racine
     * @param viewClass   la classe à rechercher
     * @param <T>         le type de vue
     * @return la première vue du type, ou null
     */
    public static <T extends View> T findByClass(View root, Class<T> viewClass) {
        if (root == null || viewClass == null) return null;
        final Object[] result = {null};
        traverse(root, (view, depth) -> {
            if (viewClass.isInstance(view)) {
                result[0] = view;
                return false;  // arrête le parcours
            }
            return true;
        });
        return viewClass.cast(result[0]);
    }

    /**
     * Recherche toutes les vues d'une classe donnée.
     *
     * @param root      la vue racine
     * @param viewClass la classe
     * @param <T>       le type
     * @return la liste des vues trouvées
     */
    public static <T extends View> java.util.List<T> findAllByClass(View root, Class<T> viewClass) {
        java.util.List<T> results = new java.util.ArrayList<>();
        if (root == null || viewClass == null) return results;
        traverse(root, (view, depth) -> {
            if (viewClass.isInstance(view)) {
                results.add(viewClass.cast(view));
            }
            return true;
        });
        return results;
    }

    /**
     * Indique si une vue est visible.
     *
     * @param view la vue
     * @return true si visible
     */
    public static boolean isVisible(View view) {
        return view != null && view.getVisibility() == View.VISIBLE;
    }

    /**
     * Indique si une vue est un ViewGroup.
     *
     * @param view la vue
     * @return true si c'est un ViewGroup
     */
    public static boolean isViewGroup(View view) {
        return view instanceof ViewGroup;
    }
}
