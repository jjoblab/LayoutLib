package jo.layoutlib.themes.parser;

import java.util.Map;

/**
 * Parser de styles.xml, inspiré de l AOSP.
 * Réutilise ThemeXmlParser avec isTheme=false.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class StyleXmlParser {

    private final ThemeXmlParser delegate = new ThemeXmlParser();

    /**
     * Parse un XML de styles.
     *
     * @param xml le XML
     * @return map nom -> StyleDefinition
     */
    public Map<String, jo.layoutlib.themes.StyleDefinition> parse(String xml) {
        Map<String, jo.layoutlib.themes.StyleDefinition> result = delegate.parse(xml);
        // Les styles ne sont pas des thèmes — on garde la même structure
        // car StyleDefinition ne distingue pas actuellement
        return result;
    }
}
