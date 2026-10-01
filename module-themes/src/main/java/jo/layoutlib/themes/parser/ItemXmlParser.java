package jo.layoutlib.themes.parser;

/**
 * Parser d un élément <item>, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ItemXmlParser {

    /**
     * Représente un <item> parsé.
     */
    public static class ParsedItem {
        public String name;
        public String value;
    }

    /**
     * Parse un attribut name et une valeur depuis une chaîne simple.
     *
     * @param name le nom de l attribut
     * @param value la valeur
     * @return le ParsedItem
     */
    public ParsedItem parse(String name, String value) {
        ParsedItem item = new ParsedItem();
        item.name = name != null ? name.trim() : null;
        item.value = value != null ? value.trim() : null;
        return item;
    }
}
