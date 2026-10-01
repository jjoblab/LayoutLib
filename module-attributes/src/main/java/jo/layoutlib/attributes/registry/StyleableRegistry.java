package jo.layoutlib.attributes.registry;

import java.util.HashMap;
import java.util.Map;

import jo.layoutlib.attributes.api.DeclareStyleableResourceValueImpl;

/**
 * Registre des styleables, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class StyleableRegistry {

    private final Map<String, DeclareStyleableResourceValueImpl> styleables = new HashMap<>();

    public void register(DeclareStyleableResourceValueImpl styleable) {
        if (styleable != null && styleable.getName() != null) {
            styleables.put(styleable.getName(), styleable);
        }
    }

    public DeclareStyleableResourceValueImpl get(String name) {
        return styleables.get(name);
    }

    public boolean has(String name) {
        return styleables.containsKey(name);
    }

    public int size() {
        return styleables.size();
    }

    public void clear() {
        styleables.clear();
    }
}
