package jo.layoutlib.drawables.parsers;

import jo.layoutlib.resources.ColorParser;
import jo.layoutlib.resources.ResourceException;

/**
 * Parser pour <color>.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ColorDrawableParser {

    /**
     * Parse une couleur littérale.
     *
     * @param colorStr la couleur (ex. "#FF6750A4")
     * @return la valeur ARGB
     */
    public int parse(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) {
            throw new ResourceException("Couleur vide");
        }
        return ColorParser.parse(colorStr);
    }
}
