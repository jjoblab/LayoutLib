package jo.layoutlib.resources.api;

/**
 * DataBindingItem, inspiré de l AOSP layoutlib-api.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class DataBindingItem {

    private String name;
    private Object value;

    public DataBindingItem() {
    }

    public DataBindingItem(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
}
