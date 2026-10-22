package jo.layoutlib.inflater.bridge.bars;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import jo.layoutlib.resources.api.ActionBarCallback;

/**
 * L'ACTION BAR du décor — VRAIE vue maquette (PE13, transport à l'identique
 * du bloc {@code creerActionBar} v0.166.5 + capacités étape 5, ADR 0137 D6).
 *
 * <p>Miroir {@code .abar} de la maquette : fond {@code colorPrimary} (repli
 * jeton {@code --s-pri} de la planche), titre Roboto 500 20 lisible par
 * LUMINANCE du fond ({@link LisibiliteChrome}, miroir {@code --s-onpri}),
 * padding 16. Étapes 5 : la FLÈCHE RETOUR ({@code homeAsUp}) et le BOUTON
 * DE DÉBORDEMENT (⋮) deviennent des capacités pilotées par
 * {@link ActionBarCallback} — aux DÉFAUTS MAQUETTE (titre seul). La VRAIE
 * action bar AppCompat du projet (réflexion {@code WindowDecorActionBar})
 * reste hors périmètre : action bar FICTICE assumée (étude PE13 §3.5/§4).</p>
 *
 * <p>Remplace le stub {@code bars/FrameworkActionBar} (constante 56 dp sans
 * vue — code mort vérifié).</p>
 *
 * @author jo@Dev
 * @since 3.5.0
 */
public class FrameworkActionBar extends LinearLayout {

    /** Le padding horizontal de la barre (miroir {@code .abar}). */
    private static final float PAD_H_DP = 16f;

    /** L'écart entre la flèche retour et le titre, en dp. */
    private static final float ECART_FLECHE_DP = 16f;

    /**
     * Construit l'action bar complète (fond, titre, capacités du callback).
     *
     * @param contexte le contexte du rendu
     * @param fond     la couleur ARGB du fond ({@code colorPrimary} résolu)
     * @param titre    le titre de l'activité, ou {@code null} (barre vide de
     *                 texte — miroir v0.134.4)
     * @param callback la configuration de l'action bar (défauts maquette)
     */
    public FrameworkActionBar(Context contexte, int fond, String titre, ActionBarCallback callback) {
        super(contexte);
        int couleurContenu = LisibiliteChrome.couleurContenu(fond);
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setBackgroundColor(fond);
        float densite = getResources().getDisplayMetrics().density;
        int pad = (int) (PAD_H_DP * densite);
        setPadding(pad, 0, pad, 0);

        if (callback.isHomeAsUp()) {
            FlecheRetour fleche = new FlecheRetour(contexte, couleurContenu);
            LayoutParams paramsFleche =
                    new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            paramsFleche.setMargins(Math.round(ECART_FLECHE_DP * densite), 0, 0, 0);
            fleche.setLayoutParams(paramsFleche);
            addView(fleche);
        }
        if (titre != null && !titre.isEmpty()) {
            TextView vueTitre = new TextView(contexte);
            vueTitre.setText(titre);
            vueTitre.setTextColor(couleurContenu);
            vueTitre.setTextSize(20f);
            vueTitre.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            vueTitre.setMaxLines(1);
            vueTitre.setEllipsize(android.text.TextUtils.TruncateAt.END);
            addView(vueTitre);
        }
        if (callback.isPopupDebordement()) {
            LayoutParams paramsEspace =
                    new LayoutParams(0, LayoutParams.WRAP_CONTENT);
            paramsEspace.weight = 1f;
            addView(new View(contexte), paramsEspace);
            addView(new Debordement(contexte, couleurContenu));
        }
    }

    /**
     * Le GLYPHE FLÈCHE RETOUR ({@code homeAsUp}) — chevron, même style de
     * trait que les glyphes de la barre navigation, à la couleur du CONTENU
     * de l'action bar.
     */
    private static final class FlecheRetour extends View {

        private final Paint peinture;

        private FlecheRetour(Context contexte, int couleurContenu) {
            super(contexte);
            peinture = new Paint(Paint.ANTI_ALIAS_FLAG);
            peinture.setColor(couleurContenu);
            peinture.setStyle(Paint.Style.STROKE);
            peinture.setStrokeWidth(2f * getResources().getDisplayMetrics().density);
            peinture.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        protected void onDraw(Canvas canevas) {
            super.onDraw(canevas);
            float densite = getResources().getDisplayMetrics().density;
            float demi = 7f * densite;
            float centreY = getHeight() / 2f;
            Path chemin = new Path();
            chemin.moveTo(getWidth() / 2f + demi, centreY - demi);
            chemin.lineTo(getWidth() / 2f - demi, centreY);
            chemin.lineTo(getWidth() / 2f + demi, centreY + demi);
            canevas.drawPath(chemin, peinture);
        }
    }

    /**
     * Le GLYPHE BOUTON DE DÉBORDEMENT (⋮) — trois points verticaux à la
     * couleur du CONTENU, même style maquette.
     */
    private static final class Debordement extends View {

        private final Paint peinture;

        private Debordement(Context contexte, int couleurContenu) {
            super(contexte);
            peinture = new Paint(Paint.ANTI_ALIAS_FLAG);
            peinture.setColor(couleurContenu);
            peinture.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canevas) {
            super.onDraw(canevas);
            float densite = getResources().getDisplayMetrics().density;
            float rayon = 2f * densite;
            float ecart = 6f * densite;
            float centreX = getWidth() / 2f;
            float centreY = getHeight() / 2f;
            canevas.drawCircle(centreX, centreY - ecart, rayon, peinture);
            canevas.drawCircle(centreX, centreY, rayon, peinture);
            canevas.drawCircle(centreX, centreY + ecart, rayon, peinture);
        }
    }
}
