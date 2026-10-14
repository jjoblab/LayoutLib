package jo.layoutlib.drawables;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;

/**
 * Parser de drawables XML de type {@code <adaptive-icon>}
 * (ADR 0116 — P4/L6).
 *
 * <p>Transforme un XML d'icône adaptative en {@link AdaptiveIconConfig} :
 * les couches {@code <background>}, {@code <foreground>} et
 * {@code <monochrome>} (API 33+) sont capturées, chacune par référence
 * {@code android:drawable="@…"} ou par drawable XML inline enfant.</p>
 *
 * <p>Format AOSP :</p>
 * <pre>{@code
 * <adaptive-icon>
 *   <background android:drawable="@color/ic_launcher_bg"/>
 *   <foreground android:drawable="@drawable/ic_launcher_fg"/>
 *   <monochrome android:drawable="@drawable/ic_launcher_mono"/>
 * </adaptive-icon>
 * }</pre>
 *
 * <p>Philosophie placeholder : un enfant inconnu ou une couche manquante est
 * ignoré, jamais d'exception de contenu (seul un XML syntaxiquement invalide
 * lève {@link DrawableException}).</p>
 *
 * <p>Référence layoutlib original :
 * {@code com.android.layoutlib.bridge.impl.ResourceHelper.getDrawable()}.</p>
 *
 * @author jo@Dev
 * @since 3.0
 */
public class AdaptiveIconParser {

    /**
     * Parse un XML d'icône adaptative en AdaptiveIconConfig.
     *
     * @param xml le XML source
     * @return la configuration parsée (aucune couche si vide)
     * @throws DrawableException si le XML est syntaxiquement invalide
     */
    public AdaptiveIconConfig parse(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new DrawableException("XML d'adaptive-icon vide", "adaptive-icon");
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));
            return parseDocument(parser);
        } catch (XmlPullParserException | IOException e) {
            throw new DrawableException(
                    "Erreur de parsing adaptive-icon : " + e.getMessage(),
                    "adaptive-icon", e);
        }
    }

    /**
     * Parcourt le document et remplit la config.
     *
     * <p>Le premier élément {@code <adaptive-icon>} est la racine ; ses
     * enfants {@code <background>}/{@code <foreground>}/{@code <monochrome>}
     * portent soit un attribut {@code android:drawable} (référence), soit un
     * drawable inline unique capturé tel quel. Les autres enfants sont
     * ignorés (placeholder).</p>
     */
    private AdaptiveIconConfig parseDocument(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        AdaptiveIconConfig config = new AdaptiveIconConfig();
        boolean rootSeen = false;
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                if (!rootSeen && "adaptive-icon".equals(parser.getName())) {
                    rootSeen = true;
                } else if (rootSeen) {
                    captureLayer(parser, config);
                    // captureLayer a avancé le parser (END_TAG de la couche
                    // ou de l'inline capturé) : re-synchroniser l'événement
                    // de boucle avant de continuer
                    event = parser.getEventType();
                    continue;
                }
            }
            event = parser.next();
        }
        return config;
    }

    /**
     * Capture une couche ({@code <background>}, {@code <foreground>} ou
     * {@code <monochrome>}) dans la config.
     *
     * <p>La couche peut désigner son drawable par référence
     * ({@code android:drawable="@…"}) ou l'embarquer en XML inline : la
     * référence a la priorité à la résolution (contrat P0).</p>
     */
    private void captureLayer(XmlPullParser parser, AdaptiveIconConfig config)
            throws XmlPullParserException, IOException {
        String layerName = parser.getName();
        String drawableRef = getAttribute(parser, "android", "drawable");

        // Enfant inline éventuel (shape, vector, color, bitmap…)
        String nestedXml = null;
        int event = parser.next();
        while (event == XmlPullParser.TEXT && parser.getText() != null
                && parser.getText().trim().isEmpty()) {
            event = parser.next();
        }
        if (event == XmlPullParser.START_TAG) {
            nestedXml = DrawableXmlFragments.captureElement(parser);
        }

        if ("background".equals(layerName)) {
            config.setBackgroundRef(drawableRef);
            config.setBackgroundXml(nestedXml);
        } else if ("foreground".equals(layerName)) {
            config.setForegroundRef(drawableRef);
            config.setForegroundXml(nestedXml);
        } else if ("monochrome".equals(layerName)) {
            config.setMonochromeRef(drawableRef);
            config.setMonochromeXml(nestedXml);
        }
        // Autres enfants : ignorés (placeholder, jamais d'exception)
    }

    /**
     * Récupère un attribut par namespace et nom.
     */
    private String getAttribute(XmlPullParser parser, String ns, String name) {
        String nsUri;
        if ("android".equals(ns)) {
            nsUri = "http://schemas.android.com/apk/res/android";
        } else {
            nsUri = ns;
        }
        String value = parser.getAttributeValue(nsUri, name);
        if (value == null) {
            value = parser.getAttributeValue(null, name);
        }
        return value;
    }
}
