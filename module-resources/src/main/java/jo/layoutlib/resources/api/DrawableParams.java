package jo.layoutlib.resources.api;

/**
 * DrawableParams, inspiré de l AOSP layoutlib-api.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class DrawableParams {

    private String name;
    private Object value;

    public DrawableParams() {
    }

    public DrawableParams(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
}
