package jo.layoutlib.themes;

import android.content.Context;
import android.view.ContextThemeWrapper;

import java.util.HashSet;
import java.util.Set;

/**
 * Résout le THÈME PRÉVISUALISÉ DU PROJET en identifiant de ressource réel
 * (v3.6.0 — étude PE14 § 2.4-2, S7 du plan).
 *
 * <p>Le problème : sur appareil, le thème déclaré par le projet
 * ({@code Theme.MyApp}) n'existe PAS dans les {@code Resources} de l'hôte
 * — {@code getIdentifier} rend 0. La RÉFÉRENCE contourne l'obstacle avec
 * son propre moteur de thèmes ({@code Resources_Theme_Delegate}) ; ici,
 * le chaînage d'héritage est déjà parsé par {@link ThemeResolver}, il
 * suffit de le MARCHER pour trouver l'ancêtre RÉSOLVABLE (Material3,
 * MaterialComponents, AppCompat ou thème framework) et de l'appliquer au
 * contexte de rendu via un {@link ContextThemeWrapper}.</p>
 *
 * <p>Avec le contexte enveloppé, chaque constructeur de vue
 * ({@code new Button(ctx)} → {@code obtainStyledAttributes(null,
 * buttonStyle)}) résout ses styles PAR DÉFAUT contre le thème du projet :
 * la table {@code defStyleAttr} par famille de l'étude devient INUTILE —
 * le vrai framework fait le travail (l'avantage appareil de ce
 * layoutlib, miroir inversé de la référence).</p>
 *
 * <h2>Ordre de résolution d'un nom</h2>
 * <ol>
 *   <li>{@code getIdentifier(nom, "style", paquetHôte)} — attrape les
 *       thèmes des bibliothèques embarquées (Material3,
 *       MaterialComponents, AppConsommer… mergés dans l'hôte avec leurs
 *       noms d'origine) ;</li>
 *   <li>{@code getIdentifier(nom, "style", "android")} — thèmes du
 *       framework ;</li>
 *   <li>parent déclaré/implicite via {@link ThemeResolver#getParentStyle}
 *       — on remonte la chaîne d'héritage (garde-fou cycle : jeu des noms
 *       visités + plafond de profondeur).</li>
 * </ol>
 *
 * @author jo@Dev
 * @since 3.6.0
 */
public final class ThemeContexteResolver {

    /** Plafond de remontée de la chaîne d'héritage (garde-fou cycle). */
    private static final int PROFONDEUR_MAX = 20;

    /** Constructeur privé — classe utilitaire. */
    private ThemeContexteResolver() {
        // Classe utilitaire — pas d'instance
    }

    /**
     * Trouve l'identifiant de ressource du thème RÉSOLVABLE le plus proche
     * de {@code themeName} (lui-même d'abord, puis sa chaîne de parents).
     *
     * @param ctx       le contexte hôte (ses {@code Resources} portent les
     *                  thèmes des bibliothèques embarquées)
     * @param resolver  le résolveur de thèmes du projet (chaîne
     *                  d'héritage), ou {@code null}
     * @param themeName le nom du thème (brut {@code Theme.MonApp} ou
     *                  référence {@code @style/…} / {@code @android:style/…})
     * @return l'identifiant (non nul), ou {@code 0} si rien ne se résout
     */
    public static int resIdResolvable(Context ctx, ThemeResolver resolver,
                                      String themeName) {
        if (ctx == null || themeName == null || themeName.isEmpty()) {
            return 0;
        }
        Context base = contexteDesResources(ctx);
        String nom = nomNu(themeName);
        if (nom.isEmpty()) {
            return 0;
        }
        Set<String> visits = new HashSet<>();
        for (int profondeur = 0; profondeur < PROFONDEUR_MAX; profondeur++) {
            if (!visits.add(nom)) {
                return 0;
            }
            int id = identifierStyle(base, nom);
            if (id != 0) {
                return id;
            }
            if (resolver == null) {
                return 0;
            }
            String parent = resolver.getParentStyle(nom);
            if (parent == null || parent.equals(nom)) {
                return 0;
            }
            nom = nomNu(parent);
            if (nom.isEmpty()) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Enveloppe le contexte dans un {@link ContextThemeWrapper} portant le
     * thème résolu — les vues instanciées dessous héritent des styles par
     * défaut de la famille (buttonStyle, editTextStyle…).
     *
     * @param base  le contexte de rendu nominal
     * @param resid l'identifiant du thème (celui de
     *              {@link #resIdResolvable}), ou {@code 0}
     * @return le contexte enveloppé, ou {@code base} tel quel si
     *         {@code resid == 0} (thème du projet introuvable — repli
     *         hôte, comportement d'avant v3.6.0)
     */
    public static Context envelopper(Context base, int resid) {
        if (base == null || resid == 0) {
            return base;
        }
        return new ContextThemeWrapper(base, resid);
    }

    /**
     * Le contexte à interroger pour {@code getIdentifier} : sous un
     * {@link ContextThemeWrapper}, les resources délèguent à la base —
     * on évite l'étape intermédiaire.
     */
    private static Context contexteDesResources(Context ctx) {
        Context base = ctx;
        while (base instanceof ContextThemeWrapper) {
            base = ((ContextThemeWrapper) base).getBaseContext();
        }
        return base;
    }

    /** Hôte d'abord (bibliothèques embarquées), framework ensuite. */
    private static int identifierStyle(Context base, String nom) {
        try {
            int id = base.getResources().getIdentifier(
                    nom, "style", base.getPackageName());
            if (id != 0) {
                return id;
            }
            return base.getResources().getIdentifier(nom, "style", "android");
        } catch (RuntimeException e) {
            // Resources corrompues — repli silencieux sur le thème hôte
            return 0;
        }
    }

    /** Retire les préfixes de référence (@style/, @android:style/, ?). */
    private static String nomNu(String reference) {
        String nom = reference;
        if (nom.startsWith("@android:style/")) {
            nom = nom.substring("@android:style/".length());
        } else if (nom.startsWith("@style/")) {
            nom = nom.substring("@style/".length());
        }
        if (nom.startsWith("?")) {
            return "";
        }
        return nom.trim();
    }
}
