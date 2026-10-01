package jo.layoutlib.themes.parser;

import java.util.Map;

import jo.layoutlib.themes.StyleDefinition;
import jo.layoutlib.themes.ThemeException;

/**
 * Validateur de thèmes, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ThemeValidator {

    /**
     * Valide qu un thème existe dans la map.
     *
     * @param themeName le nom du thème
     * @param styles la map de styles
     * @throws ThemeException si le thème n existe pas
     */
    public void validateThemeExists(String themeName, Map<String, StyleDefinition> styles) {
        if (themeName == null || themeName.isEmpty()) {
            throw new ThemeException("Nom de thème vide");
        }
        if (styles == null || !styles.containsKey(themeName)) {
            throw new ThemeException("Thème introuvable : " + themeName, themeName);
        }
    }

    /**
     * Valide qu un attribut est défini dans le thème.
     *
     * @param themeName le nom du thème
     * @param attrName le nom de l attribut
     * @param styles la map de styles
     * @return true si l attribut est défini
     */
    public boolean isAttributeDefined(String themeName, String attrName,
                                       Map<String, StyleDefinition> styles) {
        if (themeName == null || attrName == null || styles == null) return false;
        StyleInheritanceResolver resolver = new StyleInheritanceResolver();
        for (String name : resolver.resolveChain(themeName, styles)) {
            StyleDefinition style = styles.get(name);
            if (style != null && style.hasAttribute(attrName)) {
                return true;
            }
        }
        return false;
    }
}
