package jo.layoutlib.layout;

import android.view.View;
import android.view.ViewGroup;

/**
 * Utilitaire de manipulation de LayoutParams natifs Android.
 *
 * <p>Utilise directement android.view.ViewGroup.LayoutParams et ses sous-classes
 * pour créer et configurer les paramètres de layout.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public final class LayoutParam {

    private LayoutParam() {
    }

    /**
     * Crée des LayoutParams pour un ViewGroup donné.
     *
     * @param parent le ViewGroup parent
     * @param width  la largeur (MATCH_PARENT, WRAP_CONTENT, ou dimension en px)
     * @param height la hauteur
     * @return les LayoutParams créés
     */
    public static ViewGroup.LayoutParams create(ViewGroup parent, int width, int height) {
        if (parent == null) {
            return new ViewGroup.LayoutParams(width, height);
        }
        // Utilise generateLayoutParams qui retourne le bon type selon le parent
        ViewGroup.LayoutParams lp = parent.generateLayoutParams(null);
        lp.width = width;
        lp.height = height;
        return lp;
    }

    /**
     * Crée des LayoutParams match_parent.
     *
     * @return les LayoutParams
     */
    public static ViewGroup.LayoutParams matchParent() {
        return new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
    }

    /**
     * Crée des LayoutParams wrap_content.
     *
     * @return les LayoutParams
     */
    public static ViewGroup.LayoutParams wrapContent() {
        return new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /**
     * Crée des LayoutParams avec dimensions exactes.
     *
     * @param width  largeur en pixels
     * @param height hauteur en pixels
     * @return les LayoutParams
     */
    public static ViewGroup.LayoutParams exact(int width, int height) {
        return new ViewGroup.LayoutParams(width, height);
    }

    /**
     * Assure qu'une vue a des LayoutParams valides.
     * Si null, crée des WRAP_CONTENT par défaut.
     *
     * @param view  la vue à vérifier
     * @param parent le parent (peut être null)
     */
    public static void ensureLayoutParams(View view, ViewGroup parent) {
        if (view == null) return;
        if (view.getLayoutParams() == null) {
            ViewGroup.LayoutParams lp;
            if (parent != null) {
                lp = parent.generateLayoutParams(null);
            } else {
                lp = wrapContent();
            }
            view.setLayoutParams(lp);
        }
    }

    /**
     * Vérifie si des LayoutParams ont une largeur valide.
     *
     * @param lp les LayoutParams
     * @return true si la largeur est définie (pas WRAP_CONTENT négatif)
     */
    public static boolean hasValidWidth(ViewGroup.LayoutParams lp) {
        return lp != null && lp.width >= 0;
    }

    /**
     * Vérifie si des LayoutParams ont une hauteur valide.
     *
     * @param lp les LayoutParams
     * @return true si la hauteur est définie
     */
    public static boolean hasValidHeight(ViewGroup.LayoutParams lp) {
        return lp != null && lp.height >= 0;
    }
}
