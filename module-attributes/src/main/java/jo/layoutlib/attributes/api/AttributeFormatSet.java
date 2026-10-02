package jo.layoutlib.attributes.api;

import jo.layoutlib.resources.api.AttributeFormat;
import java.util.EnumSet;
import java.util.Set;


/**
 * Set de formats d attribut, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class AttributeFormatSet {

    private final Set<AttributeFormat> formats;

    public AttributeFormatSet() {
        this.formats = EnumSet.noneOf(AttributeFormat.class);
    }

    public AttributeFormatSet(AttributeFormat[] formats) {
        this.formats = EnumSet.noneOf(AttributeFormat.class);
        if (formats != null) {
            for (AttributeFormat f : formats) {
                if (f != null) {
                    this.formats.add(f);
                }
            }
        }
    }

    public boolean contains(AttributeFormat format) {
        return formats.contains(format);
    }

    public void add(AttributeFormat format) {
        if (format != null) {
            formats.add(format);
        }
    }

    public void remove(AttributeFormat format) {
        formats.remove(format);
    }

    public int size() {
        return formats.size();
    }

    public boolean isEmpty() {
        return formats.isEmpty();
    }

    public AttributeFormat[] toArray() {
        return formats.toArray(new AttributeFormat[0]);
    }

    public String toFormatsString() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (AttributeFormat f : formats) {
            if (i > 0) sb.append("|");
            sb.append(f.getXmlName());
            i++;
        }
        return sb.toString();
    }
}
