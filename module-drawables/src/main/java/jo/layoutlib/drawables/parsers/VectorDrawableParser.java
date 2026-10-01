package jo.layoutlib.drawables.parsers;

import jo.layoutlib.drawables.VectorConfig;
import jo.layoutlib.drawables.VectorParser;
import jo.layoutlib.resources.DimensionConverter;

/**
 * Parser pour VectorDrawable, délègue à VectorParser.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class VectorDrawableParser {

    private final VectorParser vectorParser;

    public VectorDrawableParser(DimensionConverter converter) {
        this.vectorParser = new VectorParser(converter);
    }

    public VectorConfig parse(String xml) {
        return vectorParser.parse(xml);
    }
}
