package jo.layoutlib.attributes.registry;

import java.util.HashMap;
import java.util.Map;

import jo.layoutlib.attributes.api.AttributeDefinitionImpl;

/**
 * Registre des declare-styleable, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class DeclareStyleableRegistry {

    private final Map<String, Map<String, AttributeDefinitionImpl>> styleableAttrs = new HashMap<>();

    public void registerAttribute(String styleableName, AttributeDefinitionImpl attr) {
        if (styleableName == null || attr == null) return;
        styleableAttrs.computeIfAbsent(styleableName, k -> new HashMap<>())
                .put(attr.getName(), attr);
    }

    public AttributeDefinitionImpl getAttribute(String styleableName, String attrName) {
        Map<String, AttributeDefinitionImpl> attrs = styleableAttrs.get(styleableName);
        return attrs != null ? attrs.get(attrName) : null;
    }

    public Map<String, AttributeDefinitionImpl> getAttributes(String styleableName) {
        Map<String, AttributeDefinitionImpl> attrs = styleableAttrs.get(styleableName);
        return attrs != null ? new HashMap<>(attrs) : new HashMap<>();
    }

    public int getStyleableCount() {
        return styleableAttrs.size();
    }

    public int getAttributeCount(String styleableName) {
        Map<String, AttributeDefinitionImpl> attrs = styleableAttrs.get(styleableName);
        return attrs != null ? attrs.size() : 0;
    }

    public void clear() {
        styleableAttrs.clear();
    }
}
