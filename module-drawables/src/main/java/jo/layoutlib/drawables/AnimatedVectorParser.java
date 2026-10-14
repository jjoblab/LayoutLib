package jo.layoutlib.drawables;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;

/**
 * Parser de drawables XML de type {@code <animated-vector>}
 * (ADR 0116 — P5/L5).
 *
 * <p>Transforme un XML d'animated-vector en {@link AnimatedVectorConfig} :</p>
 * <ul>
 *   <li><strong>API 24+</strong> : {@code <animated-vector
 *       android:drawable="@drawable/icone">} — le vector est référencé ;</li>
 *   <li><strong>API 21-23</strong> : le vector (ou tout drawable toléré) est
 *       embarqué en premier enfant inline du
 *       {@code <animated-vector>} ;</li>
 *   <li>les cibles {@code <target android:name="…"
 *       android:animation="@anim/…"/>} sont capturées mais jamais résolues
 *       (preview statique : seule la première frame importe).</li>
 * </ul>
 *
 * <p>Format AOSP (forme API 24+) :</p>
 * <pre>{@code
 * <animated-vector xmlns:android="…" android:drawable="@drawable/icone">
 *   <target android:animation="@anim/rotation" android:name="corps"/>
 *   <target android:animation="@anim/chemin" android:name="branche"/>
 * </animated-vector>
 * }</pre>
 *
 * <p>Philosophie placeholder : un enfant inconnu est ignoré, jamais
 * d'exception de contenu (seul un XML syntaxiquement invalide lève
 * {@link DrawableException}).</p>
 *
 * <p>Référence layoutlib original :
 * {@code com.android.layoutlib.bridge.impl.ResourceHelper.getDrawable()}.</p>
 *
 * @author jo@Dev
 * @since 3.1
 */
public class AnimatedVectorParser {

    /**
     * Parse un XML d'animated-vector en AnimatedVectorConfig.
     *
     * @param xml le XML source
     * @return la configuration parsée (vide si aucun vector désigné)
     * @throws DrawableException si le XML est syntaxiquement invalide
     */
    public AnimatedVectorConfig parse(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new DrawableException("XML d'animated-vector vide", "animated-vector");
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));
            return parseDocument(parser);
        } catch (XmlPullParserException | IOException e) {
            throw new DrawableException(
                    "Erreur de parsing animated-vector : " + e.getMessage(),
                    "animated-vector", e);
        }
    }

    /**
     * Parcourt le document et remplit la config.
     *
     * <p>Le premier élément {@code <animated-vector>} est la racine : son
     * attribut {@code android:drawable} (forme API 24+) est capturé. Les
     * enfants sont ensuite soit des {@code <target>} (capturés), soit un
     * drawable inline (le premier seul est capturé, forme API 21-23), soit
     * des éléments inconnus ignorés (placeholder).</p>
     */
    private AnimatedVectorConfig parseDocument(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        AnimatedVectorConfig config = new AnimatedVectorConfig();
        boolean rootSeen = false;
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                if (!rootSeen && "animated-vector".equals(parser.getName())) {
                    rootSeen = true;
                    config.setDrawableRef(
                            getAttribute(parser, "android", "drawable"));
                } else if (rootSeen) {
                    captureChild(parser, config);
                    // captureChild a avancé le parser (END_TAG de la cible ou
                    // du drawable inline capturé) : re-synchroniser l'événement
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
     * Capture un enfant de {@code <animated-vector>}.
     *
     * <p>Un {@code <target>} alimente la liste des cibles (nom + animation,
     * jamais résolues). Tout autre élément est le drawable embarqué : seul le
     * premier est capturé (contrat API 21-23), les suivants sont ignorés.</p>
     */
    private void captureChild(XmlPullParser parser, AnimatedVectorConfig config)
            throws XmlPullParserException, IOException {
        String tag = parser.getName();
        if ("target".equals(tag)) {
            config.addTarget(new AnimatedVectorConfig.AnimatedTarget(
                    getAttribute(parser, "android", "name"),
                    getAttribute(parser, "android", "animation")));
            // <target/> est vide : consommer jusqu'à son END_TAG
            skipToEndTag(parser, "target");
            return;
        }
        // Drawable inline (vector en pratique, formes API 21-23) — le premier
        // seul : la référence de la racine (API 24+) ne bloque pas la capture,
        // les deux représentations coexistent (contrat P0, réf prioritaire à
        // la résolution)
        if (config.getDrawableXml() == null) {
            config.setDrawableXml(DrawableXmlFragments.captureElement(parser));
        } else {
            skipToEndTag(parser, tag);
        }
        // Enfants inconnus supplémentaires : ignorés (placeholder)
    }

    /**
     * Consomme les événements jusqu'au END_TAG de l'élément courant
     * (inclus), en tolérant les enfants imbriqués. Le parser reste positionné
     * sur ce END_TAG — même contrat que
     * {@link DrawableXmlFragments#captureElement(XmlPullParser)}.
     */
    private void skipToEndTag(XmlPullParser parser, String tagName)
            throws XmlPullParserException, IOException {
        int depth = 1;
        while (depth > 0) {
            int event = parser.next();
            if (event == XmlPullParser.END_DOCUMENT) {
                return;
            }
            if (event == XmlPullParser.START_TAG) {
                depth++;
            } else if (event == XmlPullParser.END_TAG) {
                depth--;
            }
        }
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
