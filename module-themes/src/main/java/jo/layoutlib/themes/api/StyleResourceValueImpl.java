package jo.layoutlib.themes.api;

import java.util.LinkedHashMap;
import java.util.Map;

import jo.layoutlib.resources.api.ResourceNamespace;
import jo.layoutlib.resources.api.ResourceType;
import jo.layoutlib.resources.api.ResourceValueImpl;

/**
 * Implémentation d un style avec ses items, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class StyleResourceValueImpl extends ResourceValueImpl {

    private final Map<String, String> items = new LinkedHashMap<>();
    private String parentStyle;

    public StyleResourceValueImpl(ResourceNamespace namespace, String name, String parentStyle) {
        super(namespace, ResourceType.STYLE, name, null);
        this.parentStyle = parentStyle;
    }

    public void addItem(String name, String value) {
        if (name != null) {
            items.put(name, value);
        }
    }

    public String getItem(String name) {
        return items.get(name);
    }

    public boolean hasItem(String name) {
        return items.containsKey(name);
    }

    public Map<String, String> getItems() {
        return new LinkedHashMap<>(items);
    }

    public int getItemCount() {
        return items.size();
    }

    public String getParentStyle() {
        return parentStyle;
    }

    public void setParentStyle(String parent) {
        this.parentStyle = parent;
    }
}
