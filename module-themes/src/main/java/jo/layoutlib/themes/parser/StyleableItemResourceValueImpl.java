package jo.layoutlib.themes.parser;

/**
 * StyleableItemResourceValueImpl, implémentation pour le module themes.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class StyleableItemResourceValueImpl {

    private String name;
    private String value;

    public StyleableItemResourceValueImpl() {
    }

    public StyleableItemResourceValueImpl(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
