package jo.layoutlib.attributes.api;

/**
 * StyleItemResourceValueImpl, implémentation pour le module attributes.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class StyleItemResourceValueImpl {

    private String name;
    private Object value;

    public StyleItemResourceValueImpl() {
    }

    public StyleItemResourceValueImpl(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
}
