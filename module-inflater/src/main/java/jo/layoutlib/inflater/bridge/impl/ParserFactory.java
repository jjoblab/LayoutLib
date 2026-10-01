package jo.layoutlib.inflater.bridge.impl;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

/**
 * Factory de parsers XML, inspiré de
 * {@code com.android.layoutlib.bridge.impl.ParserFactory} de l'AOSP.
 *
 * <p>Cette classe centralise la création de {@link XmlPullParser} configurés
 * pour le mini-layoutlib. Elle met en cache la factory XmlPullParser pour
 * éviter de la recréer à chaque appel.</p>
 *
 * <h2>Configuration</h2>
 * <ul>
 *   <li>Namespace-aware (séparation préfixe/URI)</li>
 *   <li>Validation désactivée (pour la performance)</li>
 *   <li>Support des commentaires de documentation</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 1.0
 */
public final class ParserFactory {

    /** Factory XmlPullParser mise en cache. */
    private static XmlPullParserFactory cachedFactory;

    /** Indique si les logs de parsing sont activés. */
    public static boolean logParser = false;

    /** Constructeur privé. */
    private ParserFactory() {
    }

    /**
     * Crée un parser pour un XML en chaîne.
     *
     * @param xml le XML source
     * @return le parser configuré et prêt à parser
     * @throws XmlPullParserException si la création échoue
     */
    public static XmlPullParser create(String xml) throws XmlPullParserException {
        if (xml == null) {
            throw new XmlPullParserException("XML source null");
        }
        XmlPullParser parser = create();
        parser.setInput(new StringReader(xml));
        return parser;
    }

    /**
     * Crée un parser pour un InputStream.
     *
     * @param stream le flux à parser
     * @param encoding l'encodage (ex. {@code "UTF-8"}), ou {@code null} pour auto
     * @return le parser configuré
     * @throws XmlPullParserException si la création échoue
     */
    public static XmlPullParser create(InputStream stream, String encoding)
            throws XmlPullParserException {
        if (stream == null) {
            throw new XmlPullParserException("InputStream null");
        }
        XmlPullParser parser = create();
        parser.setInput(stream, encoding);
        return parser;
    }

    /**
     * Crée un parser vide (sans input).
     *
     * @return le parser configuré
     * @throws XmlPullParserException si la factory ne peut pas être créée
     */
    public static XmlPullParser create() throws XmlPullParserException {
        XmlPullParserFactory factory = getFactory();
        return factory.newPullParser();
    }

    /**
     * Récupère ou crée la factory XmlPullParser.
     *
     * @return la factory
     * @throws XmlPullParserException si la création échoue
     */
    private static XmlPullParserFactory getFactory() throws XmlPullParserException {
        if (cachedFactory == null) {
            cachedFactory = XmlPullParserFactory.newInstance();
            cachedFactory.setNamespaceAware(true);
            cachedFactory.setValidating(false);
        }
        return cachedFactory;
    }

    /**
     * Avance le parser jusqu'au prochain tag de début.
     *
     * <p>Ignore les commentaires, processing instructions et whitespace.</p>
     *
     * @param parser le parser à avancer
     * @return le nom du tag trouvé, ou {@code null} si fin de document
     * @throws XmlPullParserException en cas d'erreur de parsing
     * @throws IOException            en cas d'erreur d'E/S
     */
    public static String advanceToNextStartTag(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                return parser.getName();
            }
            event = parser.next();
        }
        return null;
    }

    /**
     * Saute complètement l'élément courant (jusqu'à son END_TAG).
     *
     * @param parser le parser positionné sur un START_TAG
     * @throws XmlPullParserException en cas d'erreur
     * @throws IOException            en cas d'erreur d'E/S
     */
    public static void skipCurrentElement(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            return;
        }
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

    /**
     * Extrait le contenu textuel d'un élément.
     *
     * <p>Le parser doit être positionné sur le START_TAG. Après l'appel,
     * le parser est positionné sur le END_TAG correspondant.</p>
     *
     * @param parser le parser
     * @return le texte contenu (trim), ou chaîne vide
     * @throws XmlPullParserException en cas d'erreur
     * @throws IOException            en cas d'erreur
     */
    public static String getElementText(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            return "";
        }
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
}
