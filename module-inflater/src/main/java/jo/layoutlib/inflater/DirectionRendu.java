package jo.layoutlib.inflater;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

/**
 * La DIRECTION DE MISE EN PAGE du rendu (v3.7.0 — étude PE14 § 2.4-5, S8
 * du plan) : le QUALIFICATEUR de direction (miroir du
 * {@code LayoutDirectionQualifier} de la référence) et la RÉSOLUTION des
 * marges relatives.
 *
 * <p><strong>Le problème corrigé :</strong> avant v3.7.0, le
 * {@link XmlPreprocessor} détruisait {@code layout_marginStart} en
 * {@code layout_marginLeft} — un layout RTL perdait sa sémantique (la
 * marge de DÉPART n'est pas la marge de GAUCHE). Désormais les attributs
 * relatifs survivent et {@link BridgeInflater} les pose via
 * {@code setMarginStart}/{@code setMarginEnd} (marges) et
 * {@code setPaddingRelative} (paddings).</p>
 *
 * <p><strong>La résolution :</strong> dans le VRAI framework,
 * {@code MarginLayoutParams.resolveLayoutDirection(int)} est appelé par
 * {@code View.requestLayout()} (contrat documenté de l'API : « This will
 * be called by requestLayout() ») — or le moteur de rendu mesure
 * l'arbre HORS FENÊTRE ({@code LayoutEngineImpl} appelle
 * {@code measure}/{@code layout} directement) : personne ne déclenche la
 * résolution. {@link #resoudre(View)} fait exactement ce que l'attachement
 * d'une fenêtre ferait — pour CHAQUE vue, résoudre ses LayoutParams
 * contre sa direction RÉSOLUE ({@code View.getLayoutDirection()} déroule
 * {@code LAYOUT_DIRECTION_INHERIT} le long de la chaîne des parents).
 * Idempotent, appelé à la FIN de l'inflation.</p>
 *
 * <p><strong>Schéma complet :</strong></p>
 * <ol>
 *   <li>{@code android:layoutDirection="rtl"} sur une vue —
 *       {@link AttributeApplier} l'applique ({@link #directionDepuisValeur}) ;
 *   </li>
 *   <li>le QUALIFICATEUR de rendu — {@link BridgeInflater#setLayoutDirectionRendu(int)}
 *       pose la direction sur la RACINE (les enfants héritent) ;</li>
 *   <li>{@link #appliquer(View, int)} : qualificateur sur la racine puis
 *       résolution de TOUT l'arbre.</li>
 * </ol>
 *
 * @author jo@Dev
 * @since 3.7.0
 */
public final class DirectionRendu {

    /** Tag de journalisation. */
    private static final String TAG = "DirectionRendu";

    /**
     * Le qualificateur INDEFINI : le framework seul décide (locale du
     * contexte de rendu) — comportement d'avant v3.7.0, jamais un échec.
     */
    public static final int INDEFINI = -1;

    /** Constructeur privé — utilitaire statique. */
    private DirectionRendu() {
        // Utilitaire — pas d'instance
    }

    /**
     * Applique le QUALIFICATEUR de direction sur la racine, puis résout
     * les marges relatives de TOUT l'arbre (la passe que l'attachement
     * d'une fenêtre ferait — le rendu hors fenêtre doit la faire lui-même).
     *
     * @param racine                  la racine inflatée (peut être null)
     * @param directionQualificateur {@link View#LAYOUT_DIRECTION_LTR},
     *                               {@link View#LAYOUT_DIRECTION_RTL} ou
     *                               {@link #INDEFINI} (ne rien poser)
     */
    public static void appliquer(View racine, int directionQualificateur) {
        if (racine == null) {
            return;
        }
        if (directionQualificateur != INDEFINI) {
            try {
                racine.setLayoutDirection(directionQualificateur);
            } catch (RuntimeException e) {
                // Défensif : jamais un rendu perdu pour une direction.
                Log.w(TAG, "setLayoutDirection(racine) refusé : " + e.getMessage());
            }
        }
        resoudre(racine);
    }

    /**
     * La PASSE DE RÉSOLUTION : pour chaque vue de l'arbre,
     * {@code LayoutParams.resolveLayoutDirection(direction résolue de la
     * vue)} — les marges {@code start}/{@code end} se copient sur
     * {@code left}/{@code right} selon la direction, exactement comme le
     * ferait {@code requestLayout()} à l'attachement d'une fenêtre.
     *
     * <p>Une exception sur UNE vue n'arrête jamais la marche (miroir du
     * garde-fou « isolement au nœud » v3.5.2).</p>
     *
     * @param vue la vue courante (résolution puis récursion)
     */
    static void resoudre(View vue) {
        if (vue == null) {
            return;
        }
        try {
            ViewGroup.LayoutParams lp = vue.getLayoutParams();
            if (lp != null) {
                lp.resolveLayoutDirection(vue.getLayoutDirection());
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "resolveLayoutDirection(" + vue.getClass().getSimpleName()
                    + ") refusé : " + e.getMessage());
        }
        if (vue instanceof ViewGroup) {
            ViewGroup groupe = (ViewGroup) vue;
            for (int i = 0; i < groupe.getChildCount(); i++) {
                resoudre(groupe.getChildAt(i));
            }
        }
    }

    /**
     * La VALEUR de l'attribut {@code android:layoutDirection} vers la
     * constante {@link View} (miroir strict de la table AOSP : toute
     * valeur inconnue retombe sur {@code INHERIT}, jamais d'échec).
     *
     * @param valeur la valeur lue ("ltr", "rtl", "locale", "inherit")
     * @return la constante {@link View} correspondante
     */
    public static int directionDepuisValeur(String valeur) {
        if (valeur == null) {
            return View.LAYOUT_DIRECTION_INHERIT;
        }
        switch (valeur.trim()) {
            case "ltr":
                return View.LAYOUT_DIRECTION_LTR;
            case "rtl":
                return View.LAYOUT_DIRECTION_RTL;
            case "locale":
                return View.LAYOUT_DIRECTION_LOCALE;
            default:
                return View.LAYOUT_DIRECTION_INHERIT;
        }
    }

    /**
     * La direction dérivée de la LANGUE d'aperçu (v3.7.0 — S8, RTL
     * « gratuit ») : une locale RTL (ar, fa, he, ur… — donnée CLDR de
     * {@code TextUtils.getLayoutDirectionFromLocale} de la référence)
     * passe le qualificateur RTL, tout le reste rend INDEFINI (le
     * framework suit sa propre locale, comportement d'avant).
     *
     * <p>Par {@code androidx.core.text.TextUtilsCompat} (miroir portatif
     * du TextUtils du framework) : l'extension
     * {@code java.util.Locale#getLayoutDirection()} est HORS des stubs
     * SDK récents (invisible de javac ET de kotlinc) ; la variante
     * androidx est PURE JDK — elle vit identique en compilation, en test
     * JVM et sur appareil.</p>
     *
     * @param langue l'étiquette BCP 47 de la requête (ou null : INDEFINI)
     * @return RTL si la locale est RTL, sinon INDEFINI
     */
    public static int directionDepuisLangue(String langue) {
        if (langue == null || langue.trim().isEmpty()) {
            return INDEFINI;
        }
        try {
            java.util.Locale locale = java.util.Locale.forLanguageTag(langue);
            return androidx.core.text.TextUtilsCompat
                    .getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL
                    ? View.LAYOUT_DIRECTION_RTL
                    : INDEFINI;
        } catch (RuntimeException e) {
            // Étiquette exotique : le framework seul décide, jamais d'échec.
            return INDEFINI;
        }
    }
}
