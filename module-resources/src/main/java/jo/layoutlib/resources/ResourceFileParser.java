package jo.layoutlib.resources;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;

/**
 * Parser de fichiers de resources XML Android.
 *
 * <p>Cette classe parse tous les types de fichiers de {@code res/values/} :</p>
 *
 * <ul>
 *   <li>{@code colors.xml} : {@code <color name="foo">#FF0000</color>}</li>
 *   <li>{@code strings.xml} : {@code <string name="foo">Hello</string>}
 *       + support {@code <string-array>} et {@code <plurals>}</li>
 *   <li>{@code dimens.xml} : {@code <dimen name="foo">16dp</dimen>}</li>
 *   <li>{@code integers.xml} : {@code <integer name="foo">42</integer>}</li>
 *   <li>{@code bools.xml} : {@code <bool name="foo">true</bool>}</li>
 * </ul>
 *
 * <p>Le parser détecte automatiquement le type de chaque élément XML et
 * remplit la {@link ResourceTable} passée en paramètre. Il supporte les
 * références chainables : {@code <color name="primary">@color/purple_500</color>}
 * (la valeur est stockée telle quelle, la résolution se fait au moment du
 * {@link ResourceResolverImpl#getColor(String)}).</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ResourceTable table = new ResourceTable();
 * ResourceQualifier qualifier = ResourceQualifier.parse("values-night");
 * ResourceFileParser parser = new ResourceFileParser(table, qualifier);
 * parser.parse("<resources><color name=\"red\">#FF0000</color></resources>");
 * }</pre>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ResourceFileParser {

    /** Table à remplir. */
    private final ResourceTable table;

    /** Qualifier du dossier source (appliqué à toutes les resources parsées). */
    private final ResourceQualifier qualifier;

    /**
     * Construit un parser pour un qualifier donné.
     *
     * @param table     table à remplir
     * @param qualifier qualifier du dossier source
     * @throws IllegalArgumentException si un argument est {@code null}
     */
    public ResourceFileParser(ResourceTable table, ResourceQualifier qualifier) {
        if (table == null) {
            throw new IllegalArgumentException("La table ne peut pas être null");
        }
        if (qualifier == null) {
            throw new IllegalArgumentException("Le qualifier ne peut pas être null");
        }
        this.table = table;
        this.qualifier = qualifier;
    }

    /**
     * Parse le contenu XML d'un fichier de resources.
     *
     * @param xml le XML à parser
     * @return le nombre d'éléments parsés
     * @throws ResourceException si le XML est malformé
     */
    public int parse(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            return 0;
        }
        try {
            XmlPullParser parser = createParser(xml);
            return parseDocument(parser);
        } catch (XmlPullParserException | IOException e) {
            throw new ResourceException("Erreur de parsing XML : " + e.getMessage(), e);
        }
    }

    /**
     * Crée un {@link XmlPullParser} configuré pour le XML donné.
     *
     * @param xml le XML source
     * @return le parser
     * @throws XmlPullParserException si la création échoue
     */
    private XmlPullParser createParser(String xml) throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(xml));
        return parser;
    }

    /**
     * Parcourt le document XML et remplit la table.
     *
     * @param parser le parser positionné au début du document
     * @return le nombre d'éléments parsés
     * @throws XmlPullParserException en cas d'erreur de parsing
     * @throws IOException            en cas d'erreur d'E/S
     */
    private int parseDocument(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        int count = 0;
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String tag = parser.getName();
                if ("resources".equals(tag)) {
                    // Conteneur racine, on continue
                } else {
                    count += parseResourceElement(parser, tag);
                }
            }
            event = parser.next();
        }
        return count;
    }

    /**
     * Parse un élément individuel de resource.
     *
     * @param parser le parser positionné sur le START_TAG
     * @param tag    nom du tag (color, string, dimen, integer, bool, etc.)
     * @return 1 si l'élément a été parsé, 0 sinon
     */
    private int parseResourceElement(XmlPullParser parser, String tag)
            throws XmlPullParserException, IOException {
        String name = parser.getAttributeValue(null, "name");
        if (name == null || name.isEmpty()) {
            // Élément sans attribut name, on l'ignore
            skipElement(parser);
            return 0;
        }
        switch (tag) {
            case "color":
                table.putColor(name, parseText(parser), qualifier);
                return 1;
            case "string":
                table.putString(name, parseText(parser), qualifier);
                return 1;
            case "dimen":
                table.putDimen(name, parseText(parser), qualifier);
                return 1;
            case "integer":
                table.putInteger(name, parseIntegerText(parser), qualifier);
                return 1;
            case "bool":
                table.putBoolean(name, parseBooleanText(parser), qualifier);
                return 1;
            case "string-array":
                table.putStringArray(name, parseStringArrayItems(parser), qualifier);
                return 1;
            case "plurals":
            case "item":
                // Élément générique (parfois utilisé pour les typed values)
                return 0;
            case "eat-comment":
            case "eat":
                // Commentaires de documentation à ignorer
                skipElement(parser);
                return 0;
            default:
                // Tag inconnu, on l'ignore
                skipElement(parser);
                return 0;
        }
    }

    /**
     * Extrait le contenu textuel d'un élément.
     *
     * @param parser le parser positionné sur le START_TAG
     * @return le texte contenu dans l'élément
     */
    private String parseText(XmlPullParser parser)
            throws XmlPullParserException, IOException {
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
        return sb.toString().trim();
    }

    /**
     * Extrait les éléments {@code <item>} d'un {@code <string-array>}.
     *
     * <p>Le parser est positionné sur le START_TAG de l'array ; en sortie, il
     * est positionné juste après son END_TAG.</p>
     *
     * @param parser le parser positionné sur le START_TAG de l'array
     * @return la liste des éléments (peut être vide)
     */
    private java.util.List<String> parseStringArrayItems(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        java.util.List<String> items = new java.util.ArrayList<>();
        int depth = 1;
        StringBuilder current = new StringBuilder();
        boolean inItem = false;
        while (depth > 0) {
            int event = parser.next();
            if (event == XmlPullParser.START_TAG) {
                depth++;
                if ("item".equals(parser.getName())) {
                    inItem = true;
                    current.setLength(0);
                }
            } else if (event == XmlPullParser.TEXT && inItem) {
                current.append(parser.getText());
            } else if (event == XmlPullParser.END_TAG) {
                depth--;
                if (inItem) {
                    items.add(current.toString().trim());
                    inItem = false;
                }
            } else if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
        }
        return items;
    }

    /**
     * Parse le texte d'un élément comme un entier.
     *
     * @param parser le parser
     * @return l'entier parsé
     */
    private Integer parseIntegerText(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        String text = parseText(parser);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new ResourceException(
                    "Entier invalide : " + text, text);
        }
    }

    /**
     * Parse le texte d'un élément comme un booléen.
     *
     * <p>Android accepte {@code "true"} / {@code "false"} mais aussi
     * {@code "TRUE"} / {@code "FALSE"} et {@code "1"} / {@code "0"}.</p>
     *
     * @param parser le parser
     * @return le booléen parsé
     */
    private Boolean parseBooleanText(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        String text = parseText(parser).toLowerCase();
        switch (text) {
            case "true":
            case "1":
                return Boolean.TRUE;
            case "false":
            case "0":
                return Boolean.FALSE;
            default:
                throw new ResourceException(
                        "Booléen invalide : " + text, text);
        }
    }

    /**
     * Saute complètement un élément.
     *
     * @param parser le parser positionné sur le START_TAG
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
