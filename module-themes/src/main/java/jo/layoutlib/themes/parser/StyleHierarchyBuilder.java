package jo.layoutlib.themes.parser;

import java.util.HashMap;
import java.util.Map;

import jo.layoutlib.themes.StyleDefinition;

/**
 * Construit une hiérarchie de styles, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class StyleHierarchyBuilder {

    /**
     * Construit une map de styles enfants par parent.
     *
     * @param styles tous les styles
     * @return map parent -> map enfant -> StyleDefinition
     */
    public Map<String, Map<String, StyleDefinition>> buildChildrenMap(
            Map<String, StyleDefinition> styles) {
        Map<String, Map<String, StyleDefinition>> children = new HashMap<>();
        if (styles == null) return children;
        for (StyleDefinition style : styles.values()) {
            String parent = style.getParent();
            if (parent != null) {
                children.computeIfAbsent(parent, k -> new HashMap<>())
                        .put(style.getName(), style);
            }
        }
        return children;
    }

    /**
     * Compte le nombre d enfants directs d un style.
     *
     * @param parentName le nom du parent
     * @param styles tous les styles
     * @return le nombre d enfants
     */
    public int countChildren(String parentName, Map<String, StyleDefinition> styles) {
        if (parentName == null || styles == null) return 0;
        int count = 0;
        for (StyleDefinition style : styles.values()) {
            if (parentName.equals(style.getParent())) {
                count++;
            }
        }
        return count;
    }
}
