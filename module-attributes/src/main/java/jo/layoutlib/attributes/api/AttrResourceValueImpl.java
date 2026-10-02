package jo.layoutlib.attributes.api;

import jo.layoutlib.resources.api.AttributeFormat;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Représente un attribut custom avec ses formats et ses enums/flags.
 * Version locale au module attributes (le module themes a la sienne).
 *
 * @author jo@Dev
 * @since 1.0
 */
public class AttrResourceValueImpl {

    private final String name;
    private final AttributeFormat[] formats;
    private final Map<String, Integer> enumValues = new LinkedHashMap<>();
    private final Map<String, Integer> flagValues = new LinkedHashMap<>();

    public AttrResourceValueImpl(String name, AttributeFormat[] formats) {
        this.name = name;
        this.formats = formats != null ? formats : new AttributeFormat[0];
    }

    public String getName() {
        return name;
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
