package jo.layoutlib.themes.parser;

/**
 * StyleableItemResourceValue, implémentation pour le module themes.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class StyleableItemResourceValue {

    private String name;
    private String value;

    public StyleableItemResourceValue() {
    }

    public StyleableItemResourceValue(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
