package jo.layoutlib.resources.api;

import android.util.TypedValue;

/**
 * Format d'attribut Android, inspiré de com.android.ide.common.rendering.api.AttributeFormat.
 * Utilise android.util.TypedValue pour les constantes de type.
 *
 * @author jo@Dev
 * @since 1.0
 */
public enum AttributeFormat {

    DIMENSION("dimension", TypedValue.TYPE_DIMENSION),
    COLOR("color", TypedValue.TYPE_INT_COLOR_ARGB8),
    REFERENCE("reference", TypedValue.TYPE_REFERENCE),
    STRING("string", TypedValue.TYPE_STRING),
    INTEGER("integer", TypedValue.TYPE_INT_DEC),
    FLOAT("float", TypedValue.TYPE_FLOAT),
    BOOLEAN("boolean", TypedValue.TYPE_INT_BOOLEAN),
    FRACTION("fraction", TypedValue.TYPE_FRACTION),
    ENUM("enum", TypedValue.TYPE_INT_DEC),
    FLAG("flag", TypedValue.TYPE_INT_DEC);

    private final String xmlName;
    private final int typedValueType;

    AttributeFormat(String xmlName, int typedValueType) {
        this.xmlName = xmlName;
        this.typedValueType = typedValueType;
    }

    public String getXmlName() {
        return xmlName;
    }

    public int getTypedValueType() {
        return typedValueType;
    }

    public static AttributeFormat fromXmlName(String name) {
        if (name == null || name.isEmpty()) return null;
        for (AttributeFormat f : values()) {
            if (f.xmlName.equals(name)) return f;
        }
        return null;
    }

    public static AttributeFormat[] parseFormats(String formatsStr) {
        if (formatsStr == null || formatsStr.isEmpty()) return new AttributeFormat[0];
        String[] parts = formatsStr.split("\\|");
        AttributeFormat[] result = new AttributeFormat[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = fromXmlName(parts[i].trim());
        }
        return result;
    }
}
