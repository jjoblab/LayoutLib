package jo.layoutlib.inflater.bridge.binding;

/**
 * Item d'adapter factice, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class AdapterItem {

    private String text;
    private int id;

    public AdapterItem() {
    }

    public AdapterItem(String text) {
        this.text = text;
    }

    public AdapterItem(String text, int id) {
        this.text = text;
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
