package jo.layoutlib.themes.parser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import jo.layoutlib.themes.StyleDefinition;
import jo.layoutlib.themes.ThemeException;

/**
 * Résolveur d'héritage de styles, inspiré de l'algorithme AOSP
 * ({@code com.android.layoutlib.bridge.impl.BridgeContext.obtainStyledAttributes}).
 *
 * <p>Cette classe construit la chaîne d'héritage d'un style en remontant
 * les parents jusqu'à la racine. Elle gère :</p>
 *
 * <ul>
 *   <li>L'héritage explicite via {@code parent="..."} dans le XML</li>
 *   <li>L'héritage implicite par point du nom
 *       (ex. {@code Theme.MyApp.Dark} hérite de {@code Theme.MyApp})</li>
 *   <li>La détection de boucles (profondeur max 20)</li>
 *   <li>La résolution des parents via une map de styles</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class StyleInheritanceResolver {

    /** Profondeur maximale d'héritage. */
    public static final int MAX_DEPTH = 20;

    /**
     * Construit la chaîne d'héritage d'un style.
     *
     * <p>Le résultat est ordonné du plus spécifique (le style lui-même)
     * au plus général (le style racine).</p>
     *
     * @param styleName le nom du style de départ
     * @param allStyles map de tous les styles connus
     * @return la liste ordonnée des noms de styles
     * @throws ThemeException si une boucle est détectée
     */
    public List<String> resolveChain(String styleName,
                                     java.util.Map<String, StyleDefinition> allStyles) {
        List<String> chain = new ArrayList<>();
        if (styleName == null || styleName.isEmpty()) {
            return chain;
        }
        String current = styleName;
        int depth = 0;
        while (current != null && depth < MAX_DEPTH) {
            // Détection de boucle
            if (chain.contains(current)) {
                throw new ThemeException(
                        "Boucle d'héritage détectée : " + chain + " → " + current,
                        current);
            }
            chain.add(current);

            StyleDefinition style = allStyles.get(current);
            if (style == null) {
                break;
            }
            String explicitParent = style.getParent();
            if (explicitParent != null && !explicitParent.isEmpty()
                    && !"@null".equals(explicitParent)) {
                current = resolveParentName(explicitParent);
            } else {
                // Héritage implicite par point
                current = computeImplicitParent(current);
            }
            depth++;
        }
        return chain;
    }

    /**
     * Résout le nom d'un parent explicite.
     *
     * <p>Le parent peut être au format {@code @style/Theme.Foo} ou
     * {@code @android:style/Theme.Foo} ou juste {@code Theme.Foo}.</p>
     *
     * @param parentRef la référence du parent
     * @return le nom du parent résolu
     */
    public String resolveParentName(String parentRef) {
        if (parentRef == null || parentRef.isEmpty()) {
            return null;
        }
        if (parentRef.startsWith("@android:style/")) {
            return parentRef.substring("@android:style/".length());
        }
        if (parentRef.startsWith("@style/")) {
            return parentRef.substring("@style/".length());
        }
        return parentRef;
    }

    /**
     * Calcule le parent implicite d'un style à partir de son nom.
     *
     * <p>Convention Android : {@code Theme.MyApp.Dark} a pour parent implicite
     * {@code Theme.MyApp}.</p>
     *
     * @param styleName le nom du style
     * @return le nom du parent implicite, ou {@code null} si pas de parent
     */
    public String computeImplicitParent(String styleName) {
        if (styleName == null) {
            return null;
        }
        int lastDot = styleName.lastIndexOf('.');
        if (lastDot <= 0) {
            return null;
        }
        return styleName.substring(0, lastDot);
    }

    /**
     * Indique si un style hérite (explicitement ou implicitement) d'un autre.
     *
     * @param childName  le nom du style enfant
     * @param parentName le nom du style parent présumé
     * @param allStyles  la map de tous les styles
     * @return {@code true} si childName hérite de parentName
     */
    public boolean inheritsFrom(String childName, String parentName,
                                java.util.Map<String, StyleDefinition> allStyles) {
        if (childName == null || parentName == null) {
            return false;
        }
        List<String> chain = resolveChain(childName, allStyles);
        return chain.contains(parentName);
    }

    /**
     * Trouve le style racine (sans parent) d'une chaîne d'héritage.
     *
     * @param styleName  le nom du style de départ
     * @param allStyles  la map de tous les styles
     * @return le nom du style racine, ou {@code null} si introuvable
     */
    public String findRootStyle(String styleName,
                                java.util.Map<String, StyleDefinition> allStyles) {
        List<String> chain = resolveChain(styleName, allStyles);
        if (chain.isEmpty()) {
            return null;
        }
        return chain.get(chain.size() - 1);
    }
}
