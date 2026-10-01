package jo.layoutlib.attributes.api;

import java.util.LinkedHashMap;
import java.util.Map;

import jo.layoutlib.attributes.AttributeFormat;

/**
 * Implémentation de AttributeDefinition, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class AttributeDefinitionImpl {

    private final String name;
    private final AttributeFormat[] formats;
    private final Map<String, Integer> enumValues = new LinkedHashMap<>();
    private final Map<String, Integer> flagValues = new LinkedHashMap<>();
    private String styleableName;

    public AttributeDefinitionImpl(String name, AttributeFormat[] formats) {
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

    public String getStyleableName() {
        return styleableName;
    }

    public void setStyleableName(String styleableName) {
        this.styleableName = styleableName;
    }

    public boolean acceptsFormat(AttributeFormat format) {
        for (AttributeFormat f : formats) {
            if (f == format) return true;
        }
        return false;
    }

    public String getFormatsString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < formats.length; i++) {
            if (i > 0) sb.append("|");
            sb.append(formats[i].getXmlName());
        }
        return sb.toString();
    }
}
