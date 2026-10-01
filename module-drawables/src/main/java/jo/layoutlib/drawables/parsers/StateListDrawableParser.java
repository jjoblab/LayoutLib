package jo.layoutlib.drawables.parsers;

import jo.layoutlib.drawables.SelectorConfig;
import jo.layoutlib.drawables.SelectorParser;

/**
 * Parser pour StateListDrawable, délègue à SelectorParser.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class StateListDrawableParser {

    private final SelectorParser selectorParser;

    public StateListDrawableParser() {
        this.selectorParser = new SelectorParser();
    }

    public SelectorConfig parse(String xml) {
        return selectorParser.parse(xml);
    }
}
