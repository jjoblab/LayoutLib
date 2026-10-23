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
 *   <li>Conservation des attributs de PREVIEW DE LISTES (v3.6.0 — étude
 *       PE14 § 2.4-3) : {@code tools:listitem}, {@code tools:itemCount},
 *       {@code tools:listheader} et {@code tools:listfooter} restent dans
 *       le flux (lus par {@code BridgeInflater.brancherApercuListe} via
 *       l'URI <em>tools</em>), la déclaration {@code xmlns:tools} est
 *       conservée AVANT CEUX-CI pour que le parseur noms-espace les
 *       accepte — miroir du {@code TOOLS_URI} de la référence.</li>
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

    /** Pattern pour identifier les attributs tools:* restants (à stripper)
     * — SAUF les attributs de preview de listes (v3.6.0) qui doivent
     * survivre jusqu'au parser. */
    private static final Pattern TOOLS_ATTR_STRIP_PATTERN =
            Pattern.compile("\\s+tools:(?!listitem\\b|itemCount\\b|listheader\\b|listfooter\\b)"
                    + "[a-zA-Z_]+\\s*=\\s*\"[^\"]*\"");

    /** Pattern pour identifier un ATTRIBUT tools:* quelconque (pour la
     * décision de garder xmlns:tools — v3.6.0). Ne touche pas la
     * DÉCLARATION xmlns:tools (caractère ':' avant, jamais une espace). */
    private static final Pattern TOOLS_ATTR_PRESENT_PATTERN =
            Pattern.compile("\\s+tools:[a-zA-Z_]+\\s*=\\s*\"[^\"]*\"");

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
        result = convertToolsAttributes(result);
        // v3.4.5 — stripper les attributs tools:* restants (context,
        // showIn, etc.) qui ne sont pas convertis ; v3.6.0 — les attributs
        // de PREVIEW DE LISTES (listitem/itemCount/listheader/listfooter)
        // SURVIVENT : BridgeInflater les lit via l'URI tools.
        result = stripRemainingToolsAttributes(result);
        // v3.6.0 — la déclaration xmlns:tools ne part que si PLUS AUCUN
        // attribut tools: ne survit (sinon « Undefined Prefix: tools »).
        result = stripXmlnsTools(result);
        result = convertMarginStart(result);
        result = convertMarginEnd(result);
        result = convertPaddingStart(result);
        result = convertPaddingEnd(result);
        return result;
    }

    /**
     * Supprime la déclaration {@code xmlns:tools} — SAUF si des attributs
     * {@code tools:*} survivent encore (v3.6.0 — preview de listes) : la
     * déclaration est alors nécessaire pour que le parser namespace-aware
     * accepte le préfixe.
     *
     * @param xml le XML source (après conversion et strip des tools:*
     *            non-listes)
     * @return le XML sans la déclaration superflue, ou intact si des
     *         attributs tools: restent
     */
    private static String stripXmlnsTools(String xml) {
        String sansDeclaration = XMLNS_TOOLS_PATTERN.matcher(xml).replaceAll("");
        if (TOOLS_ATTR_PRESENT_PATTERN.matcher(sansDeclaration).find()) {
            return xml;
        }
        return sansDeclaration;
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
     * v3.4.5 — Supprime les attributs {@code tools:*} restants qui n'ont pas
     * été convertis (ex: {@code tools:context}, {@code tools:showIn}) ;
     * v3.6.0 — ÉPARGNE {@code tools:listitem}, {@code tools:itemCount},
     * {@code tools:listheader} et {@code tools:listfooter} (preview de
     * listes, étude PE14 § 2.4-3).
     *
     * <p>Sans cette épargne, le parseur XML lèverait (après suppression
     * éventuelle de xmlns:tools) {@code Undefined Prefix: tools} — ou
     * pire, les attributs partiraient avant que l'inflater ne les lise.</p>
     *
     * @param xml le XML source (sans xmlns:tools, après conversion)
     * @return le XML sans les attributs tools:* hors listes
     */
    private static String stripRemainingToolsAttributes(String xml) {
        return TOOLS_ATTR_STRIP_PATTERN.matcher(xml).replaceAll("");
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
