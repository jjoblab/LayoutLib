package jo.layoutlib.drawables.parsers;

import jo.layoutlib.drawables.ShapeConfig;
import jo.layoutlib.drawables.ShapeParser;

/**
 * Parser spécialisé pour GradientDrawable, inspiré de l AOSP.
 * Délègue au ShapeParser car <shape> produit un GradientDrawable.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class GradientDrawableParser {

    private final ShapeParser shapeParser;

    public GradientDrawableParser(ShapeParser shapeParser) {
        this.shapeParser = shapeParser;
    }

    public ShapeConfig parse(String xml) {
        return shapeParser.parse(xml);
    }
}
