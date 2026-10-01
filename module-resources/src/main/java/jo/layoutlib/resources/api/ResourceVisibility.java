package jo.layoutlib.resources.api;

/**
 * Visibilité d'une resource Android, inspiré de l'AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public enum ResourceVisibility {

    PRIVATE("private"),
    PUBLIC("public"),
    UNKNOWN("unknown");

    private final String xmlName;

    ResourceVisibility(String xmlName) {
        this.xmlName = xmlName;
    }

    public String getXmlName() {
        return xmlName;
    }

    public static ResourceVisibility fromXmlName(String name) {
        if (name == null) return UNKNOWN;
        for (ResourceVisibility v : values()) {
            if (v.xmlName.equals(name)) return v;
        }
        return UNKNOWN;
    }
}
