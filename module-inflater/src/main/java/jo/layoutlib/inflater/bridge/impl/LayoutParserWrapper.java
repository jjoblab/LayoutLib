package jo.layoutlib.inflater.bridge.impl;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Wrapper autour d'un XmlPullParser pour layouts, inspiré de
 * {@code com.android.layoutlib.bridge.impl.LayoutParserWrapper} de l'AOSP.
 *
 * <p>Ce wrapper avance le parser jusqu'au premier tag de layout
 * (en sautant les prologs, processing instructions et commentaires).
 * Il est utilisé par le BridgeInflater pour ignorer les éléments
 * non pertinents au début d'un fichier XML.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class LayoutParserWrapper {

    /** Parser wrappé. */
    private final XmlPullParser parser;

    /**
     * Construit un wrapper autour d'un parser.
     *
     * @param parser le parser à wrapper
     * @throws XmlPullParserException si le parser est null
     */
    public LayoutParserWrapper(XmlPullParser parser) throws XmlPullParserException {
        if (parser == null) {
            throw new XmlPullParserException("parser ne peut pas être null");
        }
        this.parser = parser;
    }

    /**
     * Avance le parser jusqu'au premier tag de début.
     *
     * <p>Si le document est vide, lève une exception.</p>
     *
     * @return ce wrapper (chaînage)
     * @throws XmlPullParserException en cas d'erreur de parsing
     * @throws IOException            en cas d'erreur d'E/S
     */
    public LayoutParserWrapper peekTillLayoutStart()
            throws XmlPullParserException, IOException {
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                return this;
            }
            event = parser.next();
        }
        throw new XmlPullParserException("Document XML vide (aucun tag de début)");
    }

    /**
     * @return le parser wrappé
     */
    public XmlPullParser getParser() {
        return parser;
    }

    /**
     * Délègue à {@link XmlPullParser#next()}.
     *
     * @return l'événement suivant
     * @throws XmlPullParserException en cas d'erreur
     * @throws IOException            en cas d'erreur d'E/S
     */
    public int next() throws XmlPullParserException, IOException {
        return parser.next();
    }

    /**
     * @return le nom du tag courant
     */
    public String getName() {
        return parser.getName();
    }

    /**
     * @return le type d'événement courant
     * @throws XmlPullParserException en cas d'erreur
     */
    public int getEventType() throws XmlPullParserException {
        return parser.getEventType();
    }

    /**
     * Récupère la valeur d'un attribut par namespace et nom.
     *
     * @param namespace l'URI du namespace (ou {@code null})
     * @param name      le nom de l'attribut
     * @return la valeur, ou {@code null}
     */
    public String getAttributeValue(String namespace, String name) {
        return parser.getAttributeValue(namespace, name);
    }
}
