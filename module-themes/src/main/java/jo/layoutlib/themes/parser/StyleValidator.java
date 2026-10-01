package jo.layoutlib.themes.parser;

import java.util.Map;

import jo.layoutlib.themes.StyleDefinition;
import jo.layoutlib.themes.ThemeException;

/**
 * Validateur de styles, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class StyleValidator {

    /**
     * Valide un style.
     *
     * @param style le style à valider
     * @throws ThemeException si invalide
     */
    public void validate(StyleDefinition style) {
        if (style == null) {
            throw new ThemeException("Style null");
        }
        if (style.getName() == null || style.getName().isEmpty()) {
            throw new ThemeException("Nom de style vide", (String) null);
        }
    }

    /**
     * Valide une map de styles (vérifie les références circulaires).
     *
     * @param styles la map à valider
     * @throws ThemeException si une boucle est détectée
     */
    public void validateAll(Map<String, StyleDefinition> styles) {
        if (styles == null) return;
        StyleInheritanceResolver resolver = new StyleInheritanceResolver();
        for (String name : styles.keySet()) {
            resolver.resolveChain(name, styles);
        }
    }
}
