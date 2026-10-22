package jo.layoutlib.inflater.bridge.bars;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/**
 * La BARRE DE NAVIGATION du décor — VRAIE vue portant les TROIS glyphes de
 * la maquette (PE13, transport à l'identique du
 * {@code GlyphesBarreNavigation} PE11j, ADR 0137 étape 1).
 *
 * <p>Miroir {@code .nbar i} de la maquette {@code apercu-xml.html} :
 * 14 × 14, trait 2, écart 56 — retour = chevron, accueil = cercle,
 * applications = carré arrondi, le tout {@code #CFCFD6}. Dessinés en code
 * (l'hôte n'a pas de ressources : cette vue est le CONTENU du RENDU, pas
 * du chrome de l'app). Le fond (sombre fixe {@code --s-sys} : clair
 * {@code #1C1B1F}, nuit {@code #000}) est posé par {@link ChromeSysteme}.
 * La variante GESTUELLE de cette zone est {@link NavigationHandle}.
 * Remplace le stub {@code bars/NavigationBar} (constante 48 dp sans vue —
 * code mort vérifié).</p>
 *
 * @author jo@Dev
 * @since 3.5.0
 */
public class NavigationBar extends View {

    /** Le trait des glyphes ({@code #CFCFD6}, trait 2 — miroir {@code .nbar i}). */
    private final Paint peinture;

    /**
     * Construit la barre de navigation.
     *
     * @param contexte le contexte du rendu
     */
    public NavigationBar(Context contexte) {
        super(contexte);
        peinture = new Paint(Paint.ANTI_ALIAS_FLAG);
        peinture.setColor(0xFFCFCFD6);
        peinture.setStyle(Paint.Style.STROKE);
        peinture.setStrokeWidth(2f * getResources().getDisplayMetrics().density);
        peinture.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onDraw(Canvas canevas) {
        super.onDraw(canevas);
        float densite = getResources().getDisplayMetrics().density;
        float centreY = getHeight() / 2f;
        // Trois glyphes espacés de 56 + 14 (miroir {@code .nbar{gap:56px}}),
        // centrés horizontalement.
        float ecart = 70f * densite;
        float demi = 7f * densite;
        dessinerRetour(canevas, getWidth() / 2f - ecart, centreY, demi);
        dessinerAccueil(canevas, getWidth() / 2f, centreY, demi);
        dessinerApplications(canevas, getWidth() / 2f + ecart, centreY, demi);
    }

    /**
     * Le glyphe RETOUR (« &lt; » — miroir {@code i:nth-child(1)}, bordures
     * bas + gauche pivotées).
     */
    private void dessinerRetour(Canvas canevas, float centreX, float centreY, float demi) {
        Path chemin = new Path();
        chemin.moveTo(centreX + demi, centreY - demi);
        chemin.lineTo(centreX - demi, centreY);
        chemin.lineTo(centreX + demi, centreY + demi);
        canevas.drawPath(chemin, peinture);
    }

    /**
     * Le glyphe ACCUEIL (cercle — miroir {@code i:nth-child(2){border-radius:50%}}).
     */
    private void dessinerAccueil(Canvas canevas, float centreX, float centreY, float demi) {
        canevas.drawCircle(centreX, centreY, demi, peinture);
    }

    /**
     * Le glyphe APPLICATIONS (carré arrondi rayon 3 — miroir {@code i:nth-child(3)}).
     */
    private void dessinerApplications(Canvas canevas, float centreX, float centreY, float demi) {
        float rayon = 3f * getResources().getDisplayMetrics().density;
        canevas.drawRoundRect(centreX - demi, centreY - demi,
                centreX + demi, centreY + demi, rayon, rayon, peinture);
    }
}
