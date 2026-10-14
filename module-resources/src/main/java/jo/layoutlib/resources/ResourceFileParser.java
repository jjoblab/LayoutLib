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
 *       + support {@code <string-array>}, {@code <plurals>} et des segments
 *       {@code <xliff:g id/example>} (v3.3)</li>
 *   <li>{@code dimens.xml} : {@code <dimen name="foo">16dp</dimen>}</li>
 *   <li>{@code integers.xml} : {@code <integer name="foo">42</integer>}</li>
 *   <li>{@code arrays.xml} : {@code <integer-array name="foo">} (v3.3)</li>
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
            case "string": {
                XliffText text = parseRichText(parser);
                table.putString(name, text.getRaw(), qualifier);
                if (text.hasSpans()) {
                    table.putStringXliffSpans(name, text.getSpans(), qualifier);
                }
                return 1;
            }
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
            case "integer-array":
                table.putIntegerArray(name, parseIntegerArrayItems(parser), qualifier);
                return 1;
            case "plurals": {
                ParsedPlurals plurals = parsePluralItems(parser);
                table.putPlurals(name, plurals.items, qualifier);
                if (!plurals.spansByCategory.isEmpty()) {
                    table.putPluralsXliffSpans(name, plurals.spansByCategory, qualifier);
                }
                return 1;
            }
            case "item":
                // Élément générique orphelin (hors <string-array>/<plurals>)
                // — parfois utilisé pour les typed values
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
     * Extrait le contenu textuel riche d'un élément, en capturant les
     * segments {@code <xliff:g>} (v3.3).
     *
     * <p>Le texte brut est device-fidèle (le placeholder interne de chaque
     * segment en fait partie, comme aapt2) ; les attributs {@code id} et
     * {@code example} de chaque segment sont capturés dans des
     * {@link XliffSpan}. Les autres balises inline (ex. {@code <b>}) sont
     * traversées sans capture, comme dans {@link #parseText(XmlPullParser)}.</p>
     *
     * @param parser le parser positionné sur le START_TAG de l'élément
     * @return le texte riche (jamais {@code null})
     * @since 3.3
     */
    private XliffText parseRichText(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        StringBuilder sb = new StringBuilder();
        java.util.List<XliffSpan> spans = new java.util.ArrayList<>();
        int depth = 1;
        while (depth > 0) {
            int event = parser.next();
            if (event == XmlPullParser.START_TAG) {
                if (isXliffG(parser)) {
                    String id = readXliffAttribute(parser, "id");
                    String example = readXliffAttribute(parser, "example");
                    String placeholder = captureInnerText(parser);
                    spans.add(new XliffSpan(id, example, placeholder));
                    sb.append(placeholder);
                } else {
                    depth++;
                }
            } else if (event == XmlPullParser.END_TAG) {
                depth--;
            } else if (event == XmlPullParser.TEXT) {
                sb.append(parser.getText());
            } else if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
        }
        return new XliffText(sb.toString().trim(), spans);
    }

    /**
     * Indique si l'élément courant est un segment {@code <xliff:g>}.
     *
     * <p>Le namespace XLIFF standard d'Android est
     * {@code urn:oasis:names:tc:xliff:document:1.2} ; on accepte tout
     * namespace contenant {@code xliff} (insensible à la casse), ainsi que
     * les noms préfixés non résolus ({@code xliff:g}, {@code x:g}…) par
     * tolérance pour un parser non namespace-aware. En pratique, la
     * déclaration xmlns est requise : aapt2 rejette un préfixe non lié
     * (XML invalide), et kxml2 en mode namespace-aware fait de même.</p>
     *
     * @param parser le parser positionné sur le START_TAG
     * @return {@code true} s'il s'agit d'un segment xliff:g
     */
    private static boolean isXliffG(XmlPullParser parser) {
        String name = parser.getName();
        if (name == null) {
            return false;
        }
        if (name.endsWith(":g") && name.length() > 2) {
            return true;
        }
        if (!"g".equals(name)) {
            return false;
        }
        String ns = parser.getNamespace();
        return ns != null && ns.toLowerCase().contains("xliff");
    }

    /**
     * Lit un attribut d'un segment {@code <xliff:g>}, avec replis.
     *
     * <p>Essaye l'attribut sans namespace (la convention Android :
     * {@code <xliff:g id="…">}), puis dans le namespace du segment, puis
     * préfixé ({@code xliff:id}) — jamais d'exception.</p>
     *
     * @param parser    le parser positionné sur le START_TAG du segment
     * @param localName le nom d'attribut recherché
     * @return la valeur, ou {@code null} si absente
     */
    private static String readXliffAttribute(XmlPullParser parser, String localName) {
        String value = parser.getAttributeValue(null, localName);
        if (value != null) {
            return value;
        }
        String ns = parser.getNamespace();
        if (ns != null) {
            value = parser.getAttributeValue(ns, localName);
            if (value != null) {
                return value;
            }
        }
        return parser.getAttributeValue(null, "xliff:" + localName);
    }

    /**
     * Capture le texte interne complet d'un élément inline.
     *
     * <p>Le parser est positionné sur le START_TAG ; en sortie, il est
     * positionné juste après son END_TAG. Le texte est trimmé : c'est un
     * placeholder de format (ex. {@code %2$s}), les espaces significatifs
     * aux bords n'existent pas en pratique Android.</p>
     *
     * @param parser le parser positionné sur le START_TAG
     * @return le texte interne trimmé (peut être vide)
     */
    private static String captureInnerText(XmlPullParser parser)
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
     * est positionné juste après son END_TAG. Le texte d'un item peut
     * contenir des balises inline (ex. {@code <xliff:g>}) : leur texte
     * interne est conservé (device-fidèle, comme aapt2), les attributs ne
     * sont pas capturés ici (la substitution d'exemples concerne les
     * {@code <string>} et les items de {@code <plurals>}).</p>
     *
     * @param parser le parser positionné sur le START_TAG de l'array
     * @return la liste des éléments (peut être vide)
     */
    private java.util.List<String> parseStringArrayItems(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        java.util.List<String> items = new java.util.ArrayList<>();
        int depth = 1;
        StringBuilder current = new StringBuilder();
        int itemDepth = -1;
        while (depth > 0) {
            int event = parser.next();
            if (event == XmlPullParser.START_TAG) {
                depth++;
                if (depth == 2 && "item".equals(parser.getName())) {
                    itemDepth = depth;
                    current.setLength(0);
                }
            } else if (event == XmlPullParser.TEXT && itemDepth > 0) {
                current.append(parser.getText());
            } else if (event == XmlPullParser.END_TAG) {
                if (itemDepth > 0 && depth == itemDepth) {
                    items.add(current.toString().trim());
                    itemDepth = -1;
                }
                depth--;
            } else if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
        }
        return items;
    }

    /**
     * Extrait les éléments {@code <item>} d'un {@code <integer-array>}.
     *
     * <p>Le parser est positionné sur le START_TAG de l'array ; en sortie, il
     * est positionné juste après son END_TAG. Chaque item est parsé en
     * {@code int} (trimmé) ; un item malformé ou vide est neutralisé en
     * {@code 0} — philosophie placeholder, jamais d'exception, la longueur
     * de l'array est préservée (indices stables pour l'introspection).</p>
     *
     * @param parser le parser positionné sur le START_TAG de l'array
     * @return la liste des entiers (peut être vide)
     * @since 3.3
     */
    private java.util.List<Integer> parseIntegerArrayItems(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        java.util.List<Integer> items = new java.util.ArrayList<>();
        int depth = 1;
        StringBuilder current = new StringBuilder();
        int itemDepth = -1;
        while (depth > 0) {
            int event = parser.next();
            if (event == XmlPullParser.START_TAG) {
                depth++;
                if (depth == 2 && "item".equals(parser.getName())) {
                    itemDepth = depth;
                    current.setLength(0);
                }
            } else if (event == XmlPullParser.TEXT && itemDepth > 0) {
                current.append(parser.getText());
            } else if (event == XmlPullParser.END_TAG) {
                if (itemDepth > 0 && depth == itemDepth) {
                    items.add(parseIntegerLiteral(current.toString()));
                    itemDepth = -1;
                }
                depth--;
            } else if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
        }
        return items;
    }

    /**
     * Parse un littéral entier tolérant.
     *
     * @param text le texte (trimmé avant parsing)
     * @return l'entier parsé, ou {@code 0} si malformé/vide
     */
    private static Integer parseIntegerLiteral(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return 0;
        }
    }

    /**
     * Résultat du parsing d'un {@code <plurals>} : items catégorie → texte
     * + segments {@code <xliff:g>} par catégorie (v3.3).
     *
     * <p>Classe holder privée — les spans ne sont remplis que pour les
     * catégories dont l'item contient des segments.</p>
     */
    private static final class ParsedPlurals {

        /** Items catégorie → texte brut. */
        final java.util.Map<String, String> items = new java.util.LinkedHashMap<>();

        /** Segments xliff par catégorie. */
        final java.util.Map<String, java.util.List<XliffSpan>> spansByCategory =
                new java.util.LinkedHashMap<>();
    }

    /**
     * Extrait les éléments {@code <item quantity="…">} d'un {@code <plurals>}.
     *
     * <p>Le parser est positionné sur le START_TAG du plurals ; en sortie, il
     * est positionné juste après son END_TAG. Les items sans attribut
     * {@code quantity} (ou vide) sont ignorés — philosophie placeholder,
     * jamais d'exception. Les catégories inconnues de CLDR (ex.
     * {@code "lots"}) sont stockées telles quelles : elles ne seront jamais
     * sélectionnées par {@link PluralRules} mais restent visibles par
     * introspection. L'ordre de déclaration est préservé.</p>
     *
     * @param parser le parser positionné sur le START_TAG du plurals
     * @return les items catégorie → texte + les segments xliff par catégorie
     * @since 3.2 (retour riche depuis 3.3)
     */
    private ParsedPlurals parsePluralItems(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        ParsedPlurals result = new ParsedPlurals();
        int depth = 1;
        while (depth > 0) {
            int event = parser.next();
            if (event == XmlPullParser.START_TAG) {
                depth++;
                if (depth == 2 && "item".equals(parser.getName())) {
                    String quantity = parser.getAttributeValue(null, "quantity");
                    XliffText text = parseRichText(parser);
                    // parseRichText a consommé l'END_TAG de l'item : on
                    // re-synchronise la profondeur manuellement.
                    depth--;
                    if (quantity != null && !quantity.isEmpty()) {
                        result.items.put(quantity, text.getRaw());
                        if (text.hasSpans()) {
                            result.spansByCategory.put(quantity, text.getSpans());
                        }
                    }
                }
            } else if (event == XmlPullParser.END_TAG) {
                depth--;
            } else if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
        }
        return result;
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
