package jo.layoutlib.resources;

import java.util.Locale;

/**
 * Règles de pluriel CLDR pour la sélection de la catégorie d'un
 * {@code <plurals>} Android.
 *
 * <p>Android sélectionne l'item d'un {@code <plurals>} (déclaré dans les
 * fichiers {@code strings.xml} des dossiers {@code res/values*} avec des
 * items {@code <item quantity="...">}) via les <em>règles de pluriel</em>
 * Unicode CLDR de la <strong>locale des resources</strong> — voir
 * {@code Resources.getQuantityText(int, int)} côté AOSP et
 * <a href="https://unicode.org/reports/tr35/tr35-numbers.html#Language_Plural_Rules">
 * UTS #35 — Language Plural Rules</a>.</p>
 *
 * <h2>Périmètre</h2>
 * <p>Le moteur de preview ne manipule que des quantités <strong>entières</strong>
 * (l'API Android est {@code getQuantityString(int id, int quantity)}), donc
 * seule la partie entière des règles CLDR est implémentée ({@code v = 0},
 * pas de décimaux, pas de notation scientifique). Les langues les plus
 * courantes sont couvertes ; une langue inconnue (ou {@code null}) retombe
 * sur la règle par défaut « angloïde » : {@code one} pour 1, {@code other}
 * sinon.</p>
 *
 * <h2>Catégories</h2>
 * <p>Les six catégories CLDR correspondent aux valeurs d'attribut
 * {@code quantity} acceptées par aapt2 :</p>
 * <ul>
 *   <li>{@code zero}, {@code one}, {@code two}, {@code few}, {@code many},
 *       {@code other}</li>
 * </ul>
 *
 * <h2>Exemples</h2>
 * <pre>{@code
 * PluralRules.select("en", 1);   // "one"   → "1 item"
 * PluralRules.select("en", 2);   // "other" → "2 items"
 * PluralRules.select("fr", 0);   // "one"   → "0 item" (le français traite 0 comme singulier)
 * PluralRules.select("fr", 2);   // "other" → "2 éléments"
 * PluralRules.select("ru", 21);  // "one"   → "21 книга"
 * PluralRules.select("ru", 11);  // "many"  → "11 книг"
 * PluralRules.select("ar", 2);   // "two"   → "كتابان"
 * }</pre>
 *
 * <h2>Usage</h2>
 * <p>Cette classe est un utilitaire statique sans état : elle est appelée par
 * {@link ResourceResolverImpl#getQuantityString(String, int, Object...)} au
 * moment de résoudre une référence {@code @plurals/}. La catégorie renvoyée
 * est une clé de la map des items parsés ; l'appelant assure le repli sur
 * {@code other} si la catégorie exacte manque.</p>
 *
 * @author jo@Dev
 * @since 3.2
 */
public final class PluralRules {

    /** Catégorie CLDR {@code zero} (ar, lv). */
    public static final String CATEGORY_ZERO = "zero";

    /** Catégorie CLDR {@code one} (singulier — toutes les langues sauf zh/ja/ko/vi/th/id…). */
    public static final String CATEGORY_ONE = "one";

    /** Catégorie CLDR {@code two} (ar, he). */
    public static final String CATEGORY_TWO = "two";

    /** Catégorie CLDR {@code few} (ru, pl, cs, ar, lt…). */
    public static final String CATEGORY_FEW = "few";

    /** Catégorie CLDR {@code many} (ru, pl, fr, ar…). */
    public static final String CATEGORY_MANY = "many";

    /** Catégorie CLDR {@code other} (toujours requise par aapt2). */
    public static final String CATEGORY_OTHER = "other";

    /**
     * Constructeur privé — utilitaire statique.
     */
    private PluralRules() {
    }

    /**
     * Sélectionne la catégorie CLDR pour une quantité entière et une langue.
     *
     * @param language la langue ISO 639 (ex. {@code "fr"}), ou {@code null}
     *                 pour la règle par défaut
     * @param quantity la quantité (les négatifs sont ramenés à leur valeur
     *                 absolue, comme le fait CLDR avec {@code n})
     * @return la catégorie ({@code "zero"}, {@code "one"}, {@code "two"},
     *         {@code "few"}, {@code "many"} ou {@code "other"})
     */
    public static String select(String language, int quantity) {
        int n = Math.abs(quantity);
        String lang = language == null
                ? ""
                : language.trim().toLowerCase(Locale.ROOT);
        switch (lang) {
            case "fr":
                // CLDR fr : one = i (0, 1) ; many = i != 0 et i % 1e6 == 0
                // (« 2 millions », « 1 000 000 »).
                if (n == 0 || n == 1) {
                    return CATEGORY_ONE;
                }
                if (n != 0 && n % 1_000_000 == 0) {
                    return CATEGORY_MANY;
                }
                return CATEGORY_OTHER;

            case "pt":
                // CLDR pt : one = i (0, 1).
                return (n == 0 || n == 1) ? CATEGORY_ONE : CATEGORY_OTHER;

            case "ru":
            case "uk":
            case "sr":
            case "hr":
            case "bs":
            case "sh":
                // CLDR ru : one = v=0 et i%10=1 et i%100!=11 ;
                // few = v=0 et i%10 dans 2..4 et i%100 hors 12..14 ;
                // many = v=0 et le reste (0, 5..9, 11..14, 100…).
                if (n % 10 == 1 && n % 100 != 11) {
                    return CATEGORY_ONE;
                }
                if (n % 10 >= 2 && n % 10 <= 4
                        && (n % 100 < 12 || n % 100 > 14)) {
                    return CATEGORY_FEW;
                }
                return CATEGORY_MANY;

            case "cs":
            case "sk":
                // CLDR cs : one = i=1 ; few = i dans 2..4 ; other = le reste.
                if (n == 1) {
                    return CATEGORY_ONE;
                }
                if (n >= 2 && n <= 4) {
                    return CATEGORY_FEW;
                }
                return CATEGORY_OTHER;

            case "pl":
                // CLDR pl : one = i=1 et v=0 ; few = i%10 dans 2..4 et
                // i%100 hors 12..14 ; many = le reste des entiers.
                if (n == 1) {
                    return CATEGORY_ONE;
                }
                if (n % 10 >= 2 && n % 10 <= 4
                        && (n % 100 < 12 || n % 100 > 14)) {
                    return CATEGORY_FEW;
                }
                return CATEGORY_MANY;

            case "ar":
                // CLDR ar : zero = n=0 ; one = n=1 ; two = n=2 ;
                // few = i%100 dans 3..10 ; many = i%100 dans 11..99.
                if (n == 0) {
                    return CATEGORY_ZERO;
                }
                if (n == 1) {
                    return CATEGORY_ONE;
                }
                if (n == 2) {
                    return CATEGORY_TWO;
                }
                if (n % 100 >= 3 && n % 100 <= 10) {
                    return CATEGORY_FEW;
                }
                if (n % 100 >= 11 && n % 100 <= 99) {
                    return CATEGORY_MANY;
                }
                return CATEGORY_OTHER;

            case "he":
                // CLDR he : one = i=1 ; two = i=2.
                if (n == 1) {
                    return CATEGORY_ONE;
                }
                if (n == 2) {
                    return CATEGORY_TWO;
                }
                return CATEGORY_OTHER;

            case "lt":
                // CLDR lt : one = n%10=1 et n%100!=11 ;
                // few = n%10 dans 2..9 et n%100 hors 10..20.
                if (n % 10 == 1 && n % 100 != 11) {
                    return CATEGORY_ONE;
                }
                if (n % 10 >= 2 && n % 10 <= 9
                        && (n % 100 < 10 || n % 100 > 20)) {
                    return CATEGORY_FEW;
                }
                return CATEGORY_OTHER;

            case "lv":
                // CLDR lv : zero = n%10=0 ou n%100 dans 11..19 ;
                // one = n%10=1 et n%100!=11.
                if (n % 10 == 0 || (n % 100 >= 11 && n % 100 <= 19)) {
                    return CATEGORY_ZERO;
                }
                if (n % 10 == 1 && n % 100 != 11) {
                    return CATEGORY_ONE;
                }
                return CATEGORY_OTHER;

            case "zh":
            case "ja":
            case "ko":
            case "vi":
            case "th":
            case "id":
            case "ms":
                // Langues sans distinction de nombre — toujours other.
                return CATEGORY_OTHER;

            case "en":
            case "de":
            case "es":
            case "it":
            case "nl":
            case "sv":
            case "da":
            case "nb":
            case "no":
            case "fi":
            case "et":
            case "el":
            case "tr":
            case "ca":
            case "eu":
            case "hu":
            default:
                // Règle par défaut (angloïde) : one pour 1, other sinon.
                return n == 1 ? CATEGORY_ONE : CATEGORY_OTHER;
        }
    }
}
