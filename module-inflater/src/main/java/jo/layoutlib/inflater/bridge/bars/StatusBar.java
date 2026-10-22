package jo.layoutlib.inflater.bridge.bars;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

/**
 * La STATUS BAR du décor — VRAIE vue portant son CONTENU MAQUETTE (PE13,
 * transport à l'identique du {@code GlyphesBarreStatut} v0.166.5 côté
 * CodeIDE, ADR 0137 étape 1).
 *
 * <p>Miroir {@code .sbar} de la maquette {@code apercu-xml.html} (ligne
 * 1080) : l'heure « 9:41 » à gauche, « 5G ▮▮▮ 100% » à droite,
 * sans-serif-medium 11, padding 12 — l'heure FIGÉE de la capture, jamais
 * l'heure réelle. La barre se DESSINE en code (le module n'embarque pas
 * d'icônes par densité comme la référence AOSP charge les siennes : les
 * glyphes maquette sont un choix produit assumé, étude PE13 §1.2/§3.5).</p>
 *
 * <p>La couleur des glyphes est posée par {@link ChromeSysteme} selon la
 * luminance du fond ({@link LisibiliteChrome}) ou l'attribut explicite
 * {@code windowLightStatusBar}. Remplace le stub {@code bars/StatusBar}
 * (constante 24 dp sans vue — code mort vérifié).</p>
 *
 * @author jo@Dev
 * @since 3.5.0
 */
public class StatusBar extends View {

    /** L'heure de la maquette (capture figée — jamais l'heure réelle). */
    private static final String HEURE = "9:41";

    /** Le réseau (miroir {@code 5G}). */
    private static final String RESEAU_5G = "5G";

    /** La batterie (miroir {@code 100%}). */
    private static final String BATTERIE = "100%";

    /** Le padding horizontal (miroir {@code padding: 0 12px}). */
    private static final float PAD_H = 12f;

    /** L'écart entre blocs de texte et barres (les deux espaces). */
    private static final float ECART_BLOC = 6f;

    /** Le nombre de barres du réseau ({@code ▮▮▮}). */
    private static final int NB_BARRES = 3;

    /** La largeur d'une barre, en px bruts (transport v0.166.5 à l'identique). */
    private static final float LARGEUR_BARRE = 3f;

    /** La hauteur d'une barre, en px bruts (transport v0.166.5 à l'identique). */
    private static final float HAUTEUR_BARRE = 8f;

    /** L'écart entre barres, en px bruts (transport v0.166.5 à l'identique). */
    private static final float ECART_BARRE = 2f;

    /** La couleur du contenu (heure, réseau, batterie — posée par le chrome). */
    private int couleurGlyphes = 0xFFFFFFFF;

    /** Le texte (sans-serif-medium 11 — miroir {@code font: 500 11px Roboto}). */
    private final Paint peintureTexte;

    /** Les barres de réseau ({@code ▮▮▮} — trois rectangles verticaux pleins). */
    private final Paint peintureBarres;

    /**
     * Construit la status bar.
     *
     * @param contexte le contexte du rendu
     */
    public StatusBar(Context contexte) {
        super(contexte);
        peintureTexte = new Paint(Paint.ANTI_ALIAS_FLAG);
        peintureTexte.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        peintureTexte.setTextSize(11f * getResources().getDisplayMetrics().density);
        peintureBarres = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    /**
     * Pose la couleur des glyphes (heure, réseau, batterie) — lisible par
     * luminance du fond, décidée par {@link ChromeSysteme}.
     *
     * @param couleurGlyphes la couleur ARGB du contenu
     */
    public void setCouleurGlyphes(int couleurGlyphes) {
        this.couleurGlyphes = couleurGlyphes;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canevas) {
        super.onDraw(canevas);
        float densite = getResources().getDisplayMetrics().density;
        peintureTexte.setColor(couleurGlyphes);
        peintureBarres.setColor(couleurGlyphes);
        // La ligne de base d'un texte OPTIQUEMENT centré (miroir line-height 1).
        float ligne = getHeight() / 2f - (peintureTexte.descent() + peintureTexte.ascent()) / 2f;
        // GAUCHE : « 9:41 » (padding 12 — miroir {@code padding: 0 12px}).
        canevas.drawText(HEURE, PAD_H * densite, ligne, peintureTexte);
        // DROITE : « 5G  ▮▮▮ 100% » aligné sur le bord moins le padding.
        float fin = getWidth() - PAD_H * densite;
        float largeurBatterie = peintureTexte.measureText(BATTERIE);
        float largeur5G = peintureTexte.measureText(RESEAU_5G);
        float largeurBarres = NB_BARRES * LARGEUR_BARRE + (NB_BARRES - 1) * ECART_BARRE;
        float ecart = ECART_BLOC * densite;
        float xBarres = fin - largeurBatterie - ecart - largeurBarres;
        float demiHauteur = HAUTEUR_BARRE / 2f;
        for (int indice = 0; indice < NB_BARRES; indice++) {
            float gauche = xBarres + indice * (LARGEUR_BARRE + ECART_BARRE);
            canevas.drawRect(
                    gauche,
                    getHeight() / 2f - demiHauteur,
                    gauche + LARGEUR_BARRE,
                    getHeight() / 2f + demiHauteur,
                    peintureBarres);
        }
        canevas.drawText(RESEAU_5G, xBarres - ecart - largeur5G, ligne, peintureTexte);
        canevas.drawText(BATTERIE, fin - largeurBatterie, ligne, peintureTexte);
    }
}
