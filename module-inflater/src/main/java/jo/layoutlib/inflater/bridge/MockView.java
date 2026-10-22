package jo.layoutlib.inflater.bridge;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/**
 * Vue de REMPLACEMENT (placeholder) utilisée quand une classe custom ne
 * peut pas être instanciée (G15 — dégradation gracieuse).
 *
 * <p>Inspirée de {@code com.android.layoutlib.bridge.MockView} de l'AOSP.
 * Quand le mini-layoutlib ne peut pas charger une classe (ClassNotFoundException,
 * NoSuchMethodException, etc.), il crée un {@code MockView} à la place pour
 * préserver la structure arborescente et afficher un placeholder visible.</p>
 *
 * <p><strong>v3.4.8 — miroir de la maquette CodeIDE</strong> (docs/mockups/
 * apercu-xml.html, rendu {@code v.mock}) : le cadre de remplacement porte un
 * <em>contour pointillé</em> gris, des <em>hachures à 45°</em> translucides
 * et le <em>nom simple de la classe</em> centré — l'utilisateur VOIT quelle
 * classe manque, au lieu du rectangle ambre anonyme des versions antérieures
 * (qui ne portait le diagnostic que dans les journaux).</p>
 *
 * <p><strong>Tests JVM</strong> : les pinceaux se préparent PARESSEUSEMENT au
 * premier tracé (jamais au constructeur) — les tests unitaires JVM de
 * {@code BridgeInflater} instancient le MockView avec un contexte factice
 * dont {@code getResources()} est null ; le constructeur reste donc
 * strictement sans effet de bord.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class MockView extends View {

    /** Nom de la classe qui n'a pas pu être instanciée. */
    private String originalClassName;

    /** Pinceaux préparés au premier tracé (paresseux — tests JVM). */
    private Paint bordure;

    private Paint hachure;

    private Paint libelle;

    /** Densité résolue avec les pinceaux. */
    private float densite;

    /** Le chemin des hachures, reconstruit à chaque tracé. */
    private final Path chemin = new Path();

    /**
     * Construit un MockView avec un contexte — AUCUN accès aux ressources
     * ici (les pinceaux attendent le premier tracé).
     *
     * @param context le contexte Android
     */
    public MockView(Context context) {
        super(context);
    }

    /**
     * Définit le nom de la classe originale — posé AVANT le premier tracé
     * (le placeholder est configuré à la création, jamais redessiné vif).
     *
     * @param name nom de la classe qui n'a pas pu être instanciée
     */
    public void setOriginalClassName(String name) {
        this.originalClassName = name;
    }

    /**
     * @return le nom de la classe originale
     */
    public String getOriginalClassName() {
        return originalClassName;
    }

    /**
     * Le LIBELLÉ d'un nom de classe : le segment SIMPLE (après le dernier
     * point) — un tag pleinement qualifié s'affiche par son nom court
     * (miroir {@code v.cls} de la maquette), le nom NU reste tel quel.
     * FONCTION PURE (testable JVM).
     *
     * @param nomComplet le nom de balise ou de classe (simple ou FQN)
     * @return le segment après le dernier {@code '.'} ; vide si null
     */
    public static String libelleDe(String nomComplet) {
        if (nomComplet == null) {
            return "";
        }
        int dernier = nomComplet.lastIndexOf('.');
        return dernier >= 0 ? nomComplet.substring(dernier + 1) : nomComplet;
    }

    /** Prépare les pinceaux au premier tracé (densités de l'écran réel). */
    private void preparerPeintures() {
        if (bordure != null) {
            return;
        }
        densite = getResources().getDisplayMetrics().density;

        // Trait du contour pointillé (gris neutre, lisible clair et nuit).
        bordure = new Paint(Paint.ANTI_ALIAS_FLAG);
        bordure.setStyle(Paint.Style.STROKE);
        bordure.setStrokeWidth(1.5f * densite);
        bordure.setColor(0xFF757575);
        bordure.setPathEffect(new DashPathEffect(new float[]{4f * densite, 4f * densite}, 0f));

        // Hachures 45° translucides (miroir `repeating-linear-gradient`).
        hachure = new Paint(Paint.ANTI_ALIAS_FLAG);
        hachure.setStyle(Paint.Style.STROKE);
        hachure.setStrokeWidth(1f * densite);
        hachure.setColor(0x33808080);

        // Le libellé centré (11 sp — miroir `tx(v.cls, 11)`).
        libelle = new Paint(Paint.ANTI_ALIAS_FLAG);
        libelle.setTextAlign(Paint.Align.CENTER);
        libelle.setColor(0xFF616161);
        libelle.setTextSize(11f * getResources().getDisplayMetrics().scaledDensity);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        preparerPeintures();
        final int largeur = getWidth();
        final int hauteur = getHeight();
        if (largeur <= 0 || hauteur <= 0) {
            return;
        }

        // Hachures à 45° : miroir repeating-linear-gradient(45deg, …) —
        // une diagonale tous les 12 dp, sur TOUTE la surface.
        chemin.reset();
        final float pas = 12f * densite;
        for (float depart = -hauteur; depart < largeur; depart += pas) {
            chemin.moveTo(depart, 0f);
            chemin.lineTo(depart + hauteur, hauteur);
        }
        canvas.drawPath(chemin, hachure);

        // Contour pointillé, à demi-trait du bord (le trait reste dans la vue).
        final float demi = bordure.getStrokeWidth() / 2f;
        canvas.drawRect(demi, demi, largeur - demi, hauteur - demi, bordure);

        // Le nom simple de la classe, CENTRÉ, rogné aux limites de la vue.
        final String texte = libelleDe(originalClassName);
        if (texte.isEmpty()) {
            return;
        }
        String affiche = texte;
        while (affiche.length() > 1 && libelle.measureText(affiche) > largeur - 8f * densite) {
            affiche = affiche.substring(0, affiche.length() - 2) + "…";
        }
        // Vertical : le CENTRE de la police sur le centre de la vue.
        final float y = hauteur / 2f - (libelle.descent() + libelle.ascent()) / 2f;
        canvas.drawText(affiche, largeur / 2f, y, libelle);
    }

    @Override
    public String toString() {
        return "MockView{" + originalClassName + "}";
    }
}
