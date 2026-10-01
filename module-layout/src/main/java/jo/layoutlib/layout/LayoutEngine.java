package jo.layoutlib.layout;

import android.view.View;
import android.view.ViewGroup;

/**
 * Moteur de layout personnalisé.
 *
 * <p>Cette interface est le contrat que doit implémenter le Module 6
 * (Layout). Elle wrappe les appels à {@link View#measure(int, int)} et
 * {@link View#layout(int, int, int, int)} avec les bons
 * {@link View.MeasureSpec} et gère les callbacks.</p>
 *
 * <h2>Cas d'usage</h2>
 * <ul>
 *   <li>Quand {@code ConstraintLayout} a besoin d'un solver custom (Cassowary)</li>
 *   <li>Quand des layouts custom doivent être supportés</li>
 *   <li>Quand le rendu diffère du device à cause de constantes différentes
 *       (density, font scale)</li>
 * </ul>
 *
 * <p>Référence layoutlib original :
 * {@code com.android.layoutlib.bridge.impl.RenderSessionImpl}.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public interface LayoutEngine {

    /**
     * Mesure une vue racine avec les dimensions spécifiées.
     *
     * @param root   la vue racine
     * @param width  largeur en pixels (ou 0 pour wrap_content)
     * @param height hauteur en pixels (ou 0 pour wrap_content)
     */
    void measure(View root, int width, int height);

    /**
     * Positionne la vue racine aux coordonnées spécifiées.
     *
     * @param root la vue racine déjà mesurée
     * @param left coordonnée gauche
     * @param top  coordonnée haute
     * @param right  coordonnée droite
     * @param bottom coordonnée basse
     */
    void layout(View root, int left, int top, int right, int bottom);

    /**
     * Mesure et positionne une vue en une fois.
     *
     * @param root   la vue racine
     * @param width  largeur disponible
     * @param height hauteur disponible
     */
    void render(View root, int width, int height);

    /**
     * Configure la densité d'affichage simulée.
     *
     * @param density densité en dpi (ex. 2.0 pour xhdpi)
     */
    void setDensity(float density);

    /**
     * Configure le facteur d'échelle de police.
     *
     * @param fontScale échelle (ex. 1.0 par défaut, 1.3 pour grand)
     */
    void setFontScale(float fontScale);
}
