package jo.layoutlib.themes.api;

/**
 * ILayoutPullParserImpl, implémentation pour le module themes.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ILayoutPullParserImpl {

    private String name;
    private Object value;

    public ILayoutPullParserImpl() {
    }

    public ILayoutPullParserImpl(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
}
