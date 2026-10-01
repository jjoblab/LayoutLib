package jo.layoutlib.resources.api;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implémentation d'un attribut custom avec formats, enums et flags.
 * Inspiré de com.android.ide.common.rendering.api.AttrResourceValueImpl.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class AttrResourceValueImpl extends ResourceValueImpl {

    private AttributeFormat[] formats;
    private final Map<String, Integer> enumValues = new LinkedHashMap<>();
    private final Map<String, Integer> flagValues = new LinkedHashMap<>();

    public AttrResourceValueImpl(ResourceNamespace namespace, String name,
                                  AttributeFormat[] formats) {
        super(namespace, ResourceType.ATTR, name, null);
        this.formats = formats != null ? formats : new AttributeFormat[0];
    }

    public AttributeFormat[] getFormats() {
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
