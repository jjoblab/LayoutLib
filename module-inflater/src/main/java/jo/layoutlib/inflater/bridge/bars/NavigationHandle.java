package jo.layoutlib.inflater.bridge.bars;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/**
 * La PILULE GESTUELLE de la barre de navigation (PE13 étape 5, ADR 0137 D6)
 * — la variante {@link ModeNavigation#GESTUELLE} de la zone 48 dp.
 *
 * <p>Le « handle » d'Android 10+ : une pilule horizontale centrée. La
 * maquette ne montre que les trois boutons ({@code .nbar}) : ce mode est
 * donc une CAPACITÉ moteur aux défauts maquette, sans UI de branchement
 * aujourd'hui. Géométrie du stub historique conservée (48×4 dp, la
 * référence AOSP a sa {@code NavigationHandle} équivalente) ; couleur
 * {@code #CFCFD6}, la même que les trois boutons — la zone garde son fond
 * sombre fixe posé par {@link ChromeSysteme}.</p>
 *
 * @author jo@Dev
 * @since 3.5.0
 */
public class NavigationHandle extends View {

    /** La largeur de la pilule, en dp (géométrie du stub conservée). */
    public static final int LARGEUR_DP = 48;

    /** La hauteur de la pilule, en dp (géométrie du stub conservée). */
    public static final int HAUTEUR_DP = 4;

    /** Le fond de la pilule ({@code #CFCFD6} — la couleur des trois boutons). */
    private final Paint peinture;

    /**
     * Construit la pilule gestuelle.
     *
     * @param contexte le contexte du rendu
     */
    public NavigationHandle(Context contexte) {
        super(contexte);
        peinture = new Paint(Paint.ANTI_ALIAS_FLAG);
        peinture.setColor(0xFFCFCFD6);
        peinture.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canevas) {
        super.onDraw(canevas);
        float densite = getResources().getDisplayMetrics().density;
        float largeur = LARGEUR_DP * densite;
        float hauteur = HAUTEUR_DP * densite;
        float gauche = (getWidth() - largeur) / 2f;
        float haut = (getHeight() - hauteur) / 2f;
        canevas.drawRoundRect(gauche, haut, gauche + largeur, haut + hauteur,
                hauteur / 2f, hauteur / 2f, peinture);
    }
}
