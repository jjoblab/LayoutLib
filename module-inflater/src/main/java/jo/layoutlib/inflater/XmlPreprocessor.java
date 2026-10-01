package jo.layoutlib.inflater;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Préprocesseur XML appliqué avant le parsing par {@link BridgeInflater}.
 *
 * <p>Le but de cette classe est de normaliser le XML d'entrée pour qu'il soit
 * compatible avec les conventions attendues par le moteur d'inflation. Les
 * transformations appliquées sont les suivantes :</p>
 *
 * <ul>
 *   <li>Suppression de la déclaration {@code xmlns:tools} (espace de noms
 *       réservé à l'éditeur, sans effet à l'exécution).</li>
 *   <li>Conversion des attributs {@code tools:*} vers leur équivalent
 *       {@code android:*} lorsque c'est pertinent (ex. {@code tools:text}
 *       devient {@code android:text}). Cette convention permet au designer
 *       d'afficher du contenu de prévisualisation sans impacter l'app réelle.</li>
 *   <li>Conversion de {@code layout_marginStart} en {@code layout_marginLeft}
 *       et {@code layout_marginEnd} en {@code layout_marginRight} pour
 *       assurer la compatibilité avec l'API de layout historique.</li>
 *   <li>Conversion de {@code paddingStart} en {@code paddingLeft} et
 *       {@code paddingEnd} en {@code paddingRight} pour la même raison.</li>
 *   <li>Conversion de {@code layout_marginHorizontal} en
 *       {@code layout_marginLeft} + {@code layout_marginRight}.</li>
 * </ul>
 *
 * <p>Le préprocesseur ne modifie pas la structure arborescente du XML : il se
 * contente de transformations lexicales. Il préserve les commentaires et le
 * formatage.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public final class XmlPreprocessor {

    /** Pattern pour identifier les attributs tools:* à convertir. */
    private static final Pattern TOOLS_ATTR_PATTERN =
            Pattern.compile("tools:(text|textColor|visibility|layout_width|layout_height|src|background)\\s*=\\s*\"([^\"]*)\"");

    /** Pattern pour identifier la déclaration xmlns:tools. */
    private static final Pattern XMLNS_TOOLS_PATTERN =
            Pattern.compile("\\s+xmlns:tools=\"http://schemas\\.android\\.com/tools\"");

    /** Pattern pour layout_marginStart → layout_marginLeft. */
    private static final Pattern MARGIN_START_PATTERN =
            Pattern.compile("layout_marginStart\\s*=\\s*\"([^\"]*)\"");

    /** Pattern pour layout_marginEnd → layout_marginRight. */
    private static final Pattern MARGIN_END_PATTERN =
            Pattern.compile("layout_marginEnd\\s*=\\s*\"([^\"]*)\"");

    /** Pattern pour paddingStart → paddingLeft. */
    private static final Pattern PADDING_START_PATTERN =
            Pattern.compile("paddingStart\\s*=\\s*\"([^\"]*)\"");

    /** Pattern pour paddingEnd → paddingRight. */
    private static final Pattern PADDING_END_PATTERN =
            Pattern.compile("paddingEnd\\s*=\\s*\"([^\"]*)\"");

    /**
     * Constructeur privé : la classe n'est pas instanciable, elle ne fournit
     * que des méthodes statiques.
     */
    private XmlPreprocessor() {
        // Classe utilitaire — pas d'instance
    }

    /**
     * Pré-traite un XML source en appliquant toutes les normalisations.
     *
     * @param xmlSource le XML brut à normaliser
     * @return le XML normalisé prêt à être parsé
     * @throws IllegalArgumentException si {@code xmlSource} est {@code null}
     */
    public static String preprocess(String xmlSource) {
        if (xmlSource == null || xmlSource.trim().isEmpty()) {
            throw new IllegalArgumentException("Le XML source ne peut pas être null ou vide");
        }
        String result = xmlSource;
        result = stripXmlnsTools(result);
        result = convertToolsAttributes(result);
        result = convertMarginStart(result);
        result = convertMarginEnd(result);
        result = convertPaddingStart(result);
        result = convertPaddingEnd(result);
        return result;
    }

    /**
     * Supprime la déclaration {@code xmlns:tools}.
     *
     * @param xml le XML source
     * @return le XML sans la déclaration xmlns:tools
     */
    private static String stripXmlnsTools(String xml) {
        return XMLNS_TOOLS_PATTERN.matcher(xml).replaceAll("");
    }

    /**
     * Convertit les attributs {@code tools:*} listés en {@code android:*}.
     *
     * @param xml le XML source
     * @return le XML avec les attributs convertis
     */
    private static String convertToolsAttributes(String xml) {
        Matcher matcher = TOOLS_ATTR_PATTERN.matcher(xml);
        return matcher.replaceAll("android:$1=\"$2\"");
    }

    /**
     * Convertit {@code layout_marginStart} en {@code layout_marginLeft}.
     *
     * @param xml le XML source
     * @return le XML converti
     */
    private static String convertMarginStart(String xml) {
        return MARGIN_START_PATTERN.matcher(xml).replaceAll("layout_marginLeft=\"$1\"");
    }

    /**
     * Convertit {@code layout_marginEnd} en {@code layout_marginRight}.
     *
     * @param xml le XML source
     * @return le XML converti
     */
    private static String convertMarginEnd(String xml) {
        return MARGIN_END_PATTERN.matcher(xml).replaceAll("layout_marginRight=\"$1\"");
    }

    /**
     * Convertit {@code paddingStart} en {@code paddingLeft}.
     *
     * @param xml le XML source
     * @return le XML converti
     */
    private static String convertPaddingStart(String xml) {
        return PADDING_START_PATTERN.matcher(xml).replaceAll("paddingLeft=\"$1\"");
    }

    /**
     * Convertit {@code paddingEnd} en {@code paddingRight}.
     *
     * @param xml le XML source
     * @return le XML converti
     */
    private static String convertPaddingEnd(String xml) {
        return PADDING_END_PATTERN.matcher(xml).replaceAll("paddingRight=\"$1\"");
    }

    /**
     * Crée un {@link XmlPullParser} configuré pour parser le XML donné.
     *
     * <p>Le parser est configuré pour être namespace-aware (séparation
     * préfixe/URI) afin de distinguer correctement les attributs
     * {@code android:*} des attributs {@code app:*} ou des attributs sans
     * préfixe.</p>
     *
     * @param xml le XML à parser
     * @return un parser prêt à être utilisé
     * @throws InflateException si le parser ne peut pas être créé ou si le XML
     *                          est malformé
     */
    public static XmlPullParser createParser(String xml) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            // CRITIQUE : activer le processing des namespaces comme l'AOSP
            // Sans cette ligne, android:layout_width n'est pas résolu correctement
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(new StringReader(xml));
            return parser;
        } catch (XmlPullParserException e) {
            throw new InflateException("Impossible de créer le parser XML", e);
        }
    }

    /**
     * Valide qu'un XML est bien formé (syntaxiquement correct).
     *
     * <p>Cette méthode ne valide pas le schéma Android, uniquement la
     * conformité XML. Elle est utile pour produire un message d'erreur clair
     * avant de tenter l'inflation.</p>
     *
     * @param xml le XML à valider
     * @throws InflateException si le XML est malformé
     */
    public static void validateWellFormed(String xml) {
        try {
            XmlPullParser parser = createParser(xml);
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                event = parser.next();
            }
        } catch (XmlPullParserException | IOException e) {
            throw new InflateException("XML malformé : " + e.getMessage(), e);
        }
    }
}
