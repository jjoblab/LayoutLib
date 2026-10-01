package jo.layoutlib.themes.parser;

import java.util.LinkedHashMap;
import java.util.Map;

import jo.layoutlib.themes.StyleDefinition;

/**
 * Fusionne des styles en surchargeant les attributs, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class StyleOverrideMerger {

    /**
     * Fusionne deux styles : base + override.
     * Les attributs de override priment sur ceux de base.
     *
     * @param base le style de base
     * @param override le style de surcharge
     * @return le style fusionné
     */
    public StyleDefinition merge(StyleDefinition base, StyleDefinition override) {
        if (base == null) return override;
        if (override == null) return base;

        Map<String, String> mergedAttrs = new LinkedHashMap<>(base.getAttributes());
        mergedAttrs.putAll(override.getAttributes());

        StyleDefinition result = new StyleDefinition(
                override.getName(),
                override.getParent() != null ? override.getParent() : base.getParent(),
                override.isTheme());
        for (Map.Entry<String, String> entry : mergedAttrs.entrySet()) {
            result.setAttribute(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
