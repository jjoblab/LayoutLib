package jo.layoutlib.drawables;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Utilitaire de sérialisation inverse pour les drawables composites.
 *
 * <p>Les drawables {@code <layer-list>} et {@code <ripple>} admettent des
 * enfants <em>inline</em> : un {@code <item>} peut contenir directement un
 * {@code <shape>}, {@code <selector>}, {@code <vector>} ou {@code <color>}
 * plutôt qu'une référence {@code @drawable/}. Cette classe re-sérialise le
 * sous-arbre XML de l'élément enfant en une chaîne autonome, que les parsers
 * spécialisés peuvent ensuite re-parser (récursion).</p>
 *
 * <h2>Fonctionnement</h2>
 * <p>Le parser doit être positionné sur le {@code START_TAG} de l'élément à
 * capturer. La méthode {@link #captureElement(XmlPullParser)} consomme tous
 * les événements jusqu'au {@code END_TAG} correspondant (inclus) et retourne
 * le XML re-sérialisé. Le namespace {@code android} est déclaré sur
 * l'élément racine du fragment ; les attributs d'autres namespaces sont
 * ignorés (tolérance de rendu : mieux vaut perdre un attribut exotique que
 * casser le drawable entier).</p>
 *
 * @author jo@Dev
 * @since 3.0
 */
final class DrawableXmlFragments {

    /** URI du namespace Android. */
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    /** Constructeur privé : classe utilitaire. */
    private DrawableXmlFragments() {
    }

    /**
     * Capture le sous-arbre XML de l'élément courant en chaîne autonome.
     *
     * <p>Le parser doit être positionné sur un {@code START_TAG} ; il ressort
     * positionné sur le {@code END_TAG} correspondant (le fragment capturé
     * inclut cet élément complet, avec sa balise fermante).</p>
     *
     * @param parser le parser positionné sur le START_TAG de l'élément
     * @return le XML re-sérialisé de l'élément (jamais vide)
     * @throws XmlPullParserException si le XML est corrompu
     * @throws IOException            si la lecture échoue
     */
    static String captureElement(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        String rootName = parser.getName();
        int rootDepth = parser.getDepth();
        StringBuilder sb = new StringBuilder();
        appendStartTag(sb, parser, true);
        int event = parser.next();
        while (true) {
            if (event == XmlPullParser.END_DOCUMENT) {
                // XML tronqué — on ferme le fragment proprement
                break;
            }
            if (event == XmlPullParser.END_TAG && parser.getDepth() == rootDepth) {
                break;
            }
            if (event == XmlPullParser.START_TAG) {
                appendStartTag(sb, parser, false);
            } else if (event == XmlPullParser.END_TAG) {
                sb.append("</").append(parser.getName()).append(">");
            } else if (event == XmlPullParser.TEXT) {
                String text = parser.getText();
                if (text != null && !text.trim().isEmpty()) {
                    sb.append(escapeText(text.trim()));
                }
            }
            event = parser.next();
        }
        sb.append("</").append(rootName).append(">");
        return sb.toString();
    }

    /**
     * Sérialise une balise ouvrante avec ses attributs.
     *
     * @param sb       le StringBuilder de destination
     * @param parser   le parser positionné sur le START_TAG
     * @param isRoot   {@code true} pour l'élément racine du fragment (déclare
     *                 le namespace android)
     */
    private static void appendStartTag(StringBuilder sb, XmlPullParser parser,
                                       boolean isRoot) {
        sb.append("<").append(parser.getName());
        int count = parser.getAttributeCount();
        for (int i = 0; i < count; i++) {
            String ns = parser.getAttributeNamespace(i);
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);
            if (value == null) {
                continue;
            }
            if (ANDROID_NS.equals(ns)) {
                sb.append(" android:").append(name)
                        .append("=\"").append(escapeAttribute(value)).append("\"");
            } else if (ns == null || ns.isEmpty()) {
                // Attribut sans namespace — conservé tel quel
                sb.append(" ").append(name)
                        .append("=\"").append(escapeAttribute(value)).append("\"");
            }
            // Autres namespaces (app:, tools:) : ignorés volontairement
        }
        if (isRoot) {
            sb.append(" xmlns:android=\"").append(ANDROID_NS).append("\"");
        }
        sb.append(">");
    }

    /**
     * Échappe les caractères sensibles d'une valeur d'attribut.
     */
    private static String escapeAttribute(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * Échappe les caractères sensibles d'un nœud texte.
     */
    private static String escapeText(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
