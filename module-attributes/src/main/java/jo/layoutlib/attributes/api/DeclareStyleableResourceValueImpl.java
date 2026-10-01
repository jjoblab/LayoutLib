package jo.layoutlib.attributes.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Implémentation de DeclareStyleableResourceValue.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class DeclareStyleableResourceValueImpl implements DeclareStyleableResourceValue {

    private final String name;
    private final List<AttrResourceValueImpl> attributes = new ArrayList<>();

    public DeclareStyleableResourceValueImpl(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void addAttribute(AttrResourceValueImpl attr) {
        if (attr != null) {
            attributes.add(attr);
        }
    }

    @Override
    public List<AttrResourceValueImpl> getAttributes() {
        return new ArrayList<>(attributes);
    }

    @Override
    public int getAttributeCount() {
        return attributes.size();
    }

    public AttrResourceValueImpl findAttribute(String name) {
        for (AttrResourceValueImpl attr : attributes) {
            if (attr.getName().equals(name)) {
                return attr;
            }
        }
        return null;
    }
}
