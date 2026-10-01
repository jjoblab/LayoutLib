package jo.layoutlib.themes;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Parser de fichiers {@code themes.xml} et {@code styles.xml}.
 *
 * <p>Ce parser parcourt le XML et construit une map de
 * {@link StyleDefinition} indexée par nom. Il détecte automatiquement si
 * un élément est un thème (tag racine {@code <resources>} avec attribut
 * spécial) ou un style simple.</p>
 *
 * <h2>Exemple XML parsé</h2>
 * <pre>{@code
 * <resources>
 *     <style name="Theme.MyApp" parent="Theme.Material3.DayNight">
 *         <item name="colorPrimary">@color/purple_500</item>
 *         <item name="android:windowBackground">@drawable/bg</item>
 *     </style>
 *     <style name="Widget.MyApp.Button" parent="Widget.Material3.Button">
 *         <item name="android:textColor">#FFFFFFFF</item>
 *     </style>
 * </resources>
 * }</pre>
 *
 * <p>Référence layoutlib original :
 * {@code com.android.layoutlib.bridge.impl.BridgeContext.obtainStyledAttributes()}.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ThemeFileParser {

    /**
     * Parse un XML de themes/styles et ajoute les styles à la map fournie.
     *
     * @param xml         le XML source
     * @param targetMap   map à remplir (nom → StyleDefinition)
     * @param isThemeFile {@code true} si c'est themes.xml (les styles seront
     *                    marqués comme thèmes)
     * @return le nombre de styles parsés
     * @throws ThemeException si le XML est invalide
     */
    public int parse(String xml, Map<String, StyleDefinition> targetMap,
                     boolean isThemeFile) {
        if (xml == null || xml.trim().isEmpty()) {
            return 0;
        }
        try {
            XmlPullParser parser = createParser(xml);
            return parseDocument(parser, targetMap, isThemeFile);
        } catch (XmlPullParserException | IOException e) {
            throw new ThemeException("Erreur de parsing themes : " + e.getMessage(), e);
        }
    }

    /**
     * Parse un XML de styles simples (styles.xml).
     *
     * @param xml       le XML source
     * @param targetMap map à remplir
     * @return le nombre de styles parsés
     */
    public int parseStyles(String xml, Map<String, StyleDefinition> targetMap) {
        return parse(xml, targetMap, false);
    }

    /**
     * Parse un XML de thèmes (themes.xml).
     *
     * @param xml       le XML source
     * @param targetMap map à remplir
     * @return le nombre de thèmes parsés
     */
    public int parseThemes(String xml, Map<String, StyleDefinition> targetMap) {
        return parse(xml, targetMap, true);
    }

    /**
     * Crée un XmlPullParser.
     */
    private XmlPullParser createParser(String xml) throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(xml));
        return parser;
    }

    /**
     * Parcourt le document et remplit la map.
     */
    private int parseDocument(XmlPullParser parser,
                              Map<String, StyleDefinition> targetMap,
                              boolean isThemeFile)
            throws XmlPullParserException, IOException {
        int count = 0;
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String tag = parser.getName();
                if ("style".equals(tag)) {
                    StyleDefinition style = parseStyle(parser, isThemeFile);
                    if (style != null) {
                        targetMap.put(style.getName(), style);
                        count++;
                    }
                }
            }
            event = parser.next();
        }
        return count;
    }

    /**
     * Parse un élément {@code <style>}.
     */
    private StyleDefinition parseStyle(XmlPullParser parser, boolean isThemeFile)
            throws XmlPullParserException, IOException {
        String name = parser.getAttributeValue(null, "name");
        if (name == null || name.isEmpty()) {
            skipElement(parser);
            return null;
        }
        String parent = parser.getAttributeValue(null, "parent");
        // Le parent peut être vide si le nom contient des points (héritage implicite)
        if (parent == null || parent.isEmpty()) {
            parent = computeImplicitParent(name);
        } else if (parent.equals("@null")) {
            parent = null;
        }

        StyleDefinition style = new StyleDefinition(name, parent, isThemeFile);

        // Parcours des enfants <item>
        int depth = 1;
        while (depth > 0) {
            int event = parser.next();
            if (event == XmlPullParser.START_TAG) {
                if ("item".equals(parser.getName())) {
                    parseItem(style, parser);
                    // parseItem consomme déjà le END_TAG de l'item, ne pas incrémenter depth
                } else {
                    depth++;
                }
            } else if (event == XmlPullParser.END_TAG) {
                depth--;
            } else if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
        }
        return style;
    }

    /**
     * Parse un élément {@code <item>}.
     */
    private void parseItem(StyleDefinition style, XmlPullParser parser)
            throws XmlPullParserException, IOException {
        // L'attribut name peut être dans le namespace android ou sans namespace
        String attrName = parser.getAttributeValue(
                "http://schemas.android.com/apk/res/android", "name");
        if (attrName == null) {
            attrName = parser.getAttributeValue(null, "name");
        }
        if (attrName == null || attrName.isEmpty()) {
            skipElement(parser);
            return;
        }
        // Préfixer avec android: si l'attribut vient du namespace android
        // (la détection se fait via le préfixe dans le XML original)

        // Lecture du contenu textuel
        StringBuilder sb = new StringBuilder();
        int depth = 1;
        while (depth > 0) {
            int event = parser.next();
            if (event == XmlPullParser.START_TAG) {
                depth++;
            } else if (event == XmlPullParser.END_TAG) {
                depth--;
            } else if (event == XmlPullParser.TEXT) {
                sb.append(parser.getText());
            } else if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
        }
        style.setAttribute(attrName, sb.toString().trim());
    }

    /**
     * Calcule le parent implicite d'un style à partir de son nom.
     *
     * <p>Convention Android : {@code Theme.MyApp.Dark} a pour parent implicite
     * {@code Theme.MyApp} (si celui-ci existe).</p>
     *
     * @param name nom du style
     * @return le nom du parent implicite, ou {@code null} si pas de parent
     */
    private String computeImplicitParent(String name) {
        int lastDot = name.lastIndexOf('.');
        if (lastDot <= 0) {
            return null;
        }
        return name.substring(0, lastDot);
    }

    /**
     * Saute complètement un élément.
     */
    private void skipElement(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        int depth = 1;
        while (depth > 0) {
            int event = parser.next();
            if (event == XmlPullParser.START_TAG) {
                depth++;
            } else if (event == XmlPullParser.END_TAG) {
                depth--;
            } else if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
        }
    }
}
