package jo.layoutlib.themes.api;

import java.util.LinkedHashMap;
import java.util.Map;

import jo.layoutlib.resources.api.ResourceNamespace;
import jo.layoutlib.resources.api.ResourceType;
import jo.layoutlib.resources.api.ResourceValueImpl;

/**
 * Représente un <attr> avec ses formats et ses enums/flags.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class AttrResourceValueImpl extends ResourceValueImpl {

    private String[] formats;
    private final Map<String, Integer> enumValues = new LinkedHashMap<>();
    private final Map<String, Integer> flagValues = new LinkedHashMap<>();

    public AttrResourceValueImpl(ResourceNamespace namespace, String name, String[] formats) {
        super(namespace, ResourceType.ATTR, name, null);
        this.formats = formats != null ? formats : new String[0];
    }

    public String[] getFormats() {
        return formats.clone();
    }

    public void addEnumValue(String name, int value) {
        enumValues.put(name, value);
    }

    public Integer getEnumValue(String name) {
        return enumValues.get(name);
    }

    public void addFlagValue(String name, int value) {
        flagValues.put(name, value);
    }

    public Integer getFlagValue(String name) {
        return flagValues.get(name);
    }

    public Map<String, Integer> getEnumValues() {
        return new LinkedHashMap<>(enumValues);
    }

    public Map<String, Integer> getFlagValues() {
        return new LinkedHashMap<>(flagValues);
    }
}
