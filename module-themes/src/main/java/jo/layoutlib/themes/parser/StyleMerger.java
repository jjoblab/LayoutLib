package jo.layoutlib.themes.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jo.layoutlib.themes.StyleDefinition;
import jo.layoutlib.themes.ThemeException;

/**
 * Fusionneur de styles, inspiré de l'algorithme AOSP
 * ({@code com.android.layoutlib.bridge.impl.BridgeContext}).
 *
 * <p>Cette classe prend une chaîne d'héritage de styles et produit un
 * style "fusionné" qui combine les attributs de tous les styles de la
 * chaîne. Les styles les plus spécifiques (en début de chaîne) priment
 * sur les styles plus généraux (en fin de chaîne).</p>
 *
 * <h2>Algorithme</h2>
 * <ol>
 *   <li>On parcourt la chaîne dans l'ordre inverse (du plus général au plus spécifique)</li>
 *   <li>Pour chaque style, on ajoute ses attributs à la map fusionnée</li>
 *   <li>Si un attribut est déjà présent, il est écrasé (le plus spécifique gagne)</li>
 * </ol>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class StyleMerger {

    /** Résolveur d'héritage. */
    private final StyleInheritanceResolver resolver;

    /**
     * Construit un fusionneur.
     *
     * @param resolver le résolveur d'héritage
     */
    public StyleMerger(StyleInheritanceResolver resolver) {
        this.resolver = resolver != null ? resolver : new StyleInheritanceResolver();
    }

    /**
     * Fusionne une chaîne de styles en un seul style virtuel.
     *
     * @param styleName le nom du style de départ
     * @param allStyles la map de tous les styles connus
     * @return un StyleDefinition fusionné, ou {@code null} si introuvable
     */
    public StyleDefinition mergeChain(String styleName,
                                      Map<String, StyleDefinition> allStyles) {
        if (styleName == null || allStyles == null || allStyles.isEmpty()) {
            return null;
        }
        List<String> chain = resolver.resolveChain(styleName, allStyles);
        if (chain.isEmpty()) {
            return null;
        }

        // Map fusionnée des attributs
        Map<String, String> mergedAttrs = new LinkedHashMap<>();

        // Parcours inverse : du plus général au plus spécifique
        for (int i = chain.size() - 1; i >= 0; i--) {
            String name = chain.get(i);
            StyleDefinition style = allStyles.get(name);
            if (style != null) {
                Map<String, String> attrs = style.getAttributes();
                for (Map.Entry<String, String> entry : attrs.entrySet()) {
                    mergedAttrs.put(entry.getKey(), entry.getValue());
                }
            }
        }

        // Création du style fusionné
        StyleDefinition result = new StyleDefinition(styleName, null, false);
        for (Map.Entry<String, String> entry : mergedAttrs.entrySet()) {
            result.setAttribute(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Récupère la valeur d'un attribut dans la chaîne d'héritage.
     *
     * <p>Parcourt la chaîne du plus spécifique au plus général et retourne
     * la première valeur trouvée.</p>
     *
     * @param styleName le nom du style de départ
     * @param attrName  le nom de l'attribut
     * @param allStyles la map de tous les styles
     * @return la valeur, ou {@code null} si non trouvée
     */
    public String getMergedAttribute(String styleName, String attrName,
                                     Map<String, StyleDefinition> allStyles) {
        if (styleName == null || attrName == null) {
            return null;
        }
        List<String> chain = resolver.resolveChain(styleName, allStyles);
        for (String name : chain) {
            StyleDefinition style = allStyles.get(name);
            if (style != null) {
                String value = style.getAttribute(attrName);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Indique si un attribut est défini dans la chaîne d'héritage.
     *
     * @param styleName le nom du style
     * @param attrName  le nom de l'attribut
     * @param allStyles la map de tous les styles
     * @return {@code true} si l'attribut est défini
     */
    public boolean hasAttribute(String styleName, String attrName,
                                Map<String, StyleDefinition> allStyles) {
        return getMergedAttribute(styleName, attrName, allStyles) != null;
    }

    /**
     * Compte le nombre total d'attributs uniques dans la chaîne.
     *
     * @param styleName le nom du style
     * @param allStyles la map de tous les styles
     * @return le nombre d'attributs
     */
    public int getMergedAttributeCount(String styleName,
                                       Map<String, StyleDefinition> allStyles) {
        StyleDefinition merged = mergeChain(styleName, allStyles);
        return merged != null ? merged.getAttributeCount() : 0;
    }
}
