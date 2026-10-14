package jo.layoutlib.resources;

/**
 * Résolveur de resources Android.
 *
 * <p>Cette interface est le contrat que doit implémenter le Module 2
 * (Resources). Elle permet de résoudre les références Android suivantes :</p>
 *
 * <ul>
 *   <li>{@code @color/foo} → valeur ARGB entière</li>
 *   <li>{@code @string/foo} → chaîne de caractères</li>
 *   <li>{@code @dimen/foo} → valeur flottante en pixels</li>
 *   <li>{@code @integer/foo} → valeur entière</li>
 *   <li>{@code @bool/foo} → booléen</li>
 *   <li>{@code @array/foo} → liste de chaînes ({@code <string-array>})</li>
 *   <li>{@code @plurals/foo} → chaîne de pluriel sélectionnée par les règles
 *       CLDR de la locale courante ({@code <plurals>})</li>
 *   <li>{@code @layout/foo} → contenu XML du layout</li>
 *   <li>{@code @drawable/foo} → chemin vers le fichier drawable (XML ou image)</li>
 * </ul>
 *
 * <p>Le résolveur doit aussi gérer :</p>
 * <ul>
 *   <li>Les références chainables : {@code <color name="primary">@color/purple_500</color>}</li>
 *   <li>Les qualifiers : {@code values-night/}, {@code values-land/}, {@code values-v31/}</li>
 *   <li>Le cache des valeurs déjà résolues</li>
 * </ul>
 *
 * <h2>Référence layoutlib original</h2>
 * <p>Voir {@code com.android.layoutlib.bridge.android.BridgeContext} et
 * {@code com.android.ide.common.resources.ResourceResolver}.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public interface ResourceResolver {

    /**
     * Résout une référence de couleur {@code @color/foo}.
     *
     * @param reference la référence (ex. {@code @color/my_color})
     * @return la valeur ARGB, ou {@code null} si introuvable
     */
    Integer getColor(String reference);

    /**
     * Résout une référence de chaîne {@code @string/foo}.
     *
     * @param reference la référence
     * @return la valeur, ou {@code null} si introuvable
     */
    String getString(String reference);

    /**
     * Résout une référence de dimension {@code @dimen/foo}.
     *
     * @param reference la référence
     * @return la valeur en pixels, ou {@code null} si introuvable
     */
    Float getDimension(String reference);

    /**
     * Résout une référence d'entier {@code @integer/foo}.
     *
     * @param reference la référence
     * @return la valeur, ou {@code null} si introuvable
     */
    Integer getInteger(String reference);

    /**
     * Résout une référence booléenne {@code @bool/foo}.
     *
     * @param reference la référence
     * @return la valeur, ou {@code null} si introuvable
     */
    Boolean getBoolean(String reference);

    /**
     * Résout une référence de string-array {@code @array/foo}.
     *
     * <p>Les éléments de l'array peuvent eux-mêmes être des références
     * {@code @string/} ; elles sont résolues récursivement.</p>
     *
     * @param reference la référence (ex. {@code @array/planets})
     * @return une nouvelle liste des éléments résolus, ou {@code null} si
     *         introuvable
     */
    java.util.List<String> getStringArray(String reference);

    /**
     * Résout une référence de pluriel {@code @plurals/foo} pour une quantité
     * donnée (v3.2).
     *
     * <p>Sélectionne l'item du {@code <plurals>} dont la catégorie CLDR
     * ({@code one}, {@code other}, {@code few}…) correspond à la quantité et
     * à la langue courante (voir {@link #setLanguage(String)}) — sémantique
     * d'{@code android.content.res.Resources#getQuantityString(int, int,
     * Object...)}. Si la catégorie exacte manque dans le plurals, repli sur
     * {@code other} ; si {@code other} manque aussi, retourne {@code null}
     * (jamais d'exception).</p>
     *
     * <p>Quand des arguments de formatage sont fournis, le texte sélectionné
     * est formaté par {@code String.format} (les items peuvent référencer
     * d'autres chaînes via {@code @string/}, résolues récursivement). Un
     * pattern invalide retourne le texte brut non formaté.</p>
     *
     * @param reference   la référence (ex. {@code @plurals/item_count} — le
     *                    nom nu est aussi accepté)
     * @param quantity    la quantité pilotant la catégorie CLDR
     * @param formatArgs  arguments de formatage du pattern (aucun = texte
     *                    brut avec le pattern intact, comme Android)
     * @return la chaîne formatée, ou {@code null} si le plurals est
     *         introuvable ou sans item applicable
     * @since 3.2
     */
    String getQuantityString(String reference, int quantity, Object... formatArgs);

    /**
     * Récupère les items d'un pluriel {@code @plurals/foo} (v3.2).
     *
     * <p>Utile pour l'introspection (éditeur de resources, preview qui
     * choisit lui-même la catégorie). La map renvoyée est une copie défensive
     * catégorie → texte, dans l'ordre de déclaration du XML.</p>
     *
     * @param reference la référence (ex. {@code @plurals/item_count} — le
     *                  nom nu est aussi accepté)
     * @return une copie de la map des items, ou {@code null} si introuvable
     * @since 3.2
     */
    java.util.Map<String, String> getPlurals(String reference);

    /**
     * Récupère le contenu XML d'un layout référencé par {@code @layout/foo}.
     *
     * @param reference la référence
     * @return le XML, ou {@code null} si introuvable
     */
    String getLayout(String reference);

    /**
     * Récupère le chemin absolu du fichier drawable référencé par
     * {@code @drawable/foo}.
     *
     * @param reference la référence
     * @return le chemin, ou {@code null} si introuvable
     */
    String getDrawablePath(String reference);

    /**
     * Active ou désactive le mode nuit (DayNight).
     *
     * @param nightMode {@code true} pour le mode nuit
     */
    void setNightMode(boolean nightMode);

    /**
     * Active ou désactive le mode paysage.
     *
     * @param landscape {@code true} pour le paysage
     */
    void setLandscape(boolean landscape);

    /**
     * Définit le niveau d'API simulé (pour les qualifiers {@code -vXX}).
     *
     * @param apiLevel niveau d'API Android (ex. 31)
     */
    void setApiLevel(int apiLevel);

    /**
     * Définit la locale simulée (pour les qualifiers {@code values-<lang>/}).
     *
     * <p>Après cet appel, les resources localisées (ex.
     * {@code res/values-fr/strings.xml}) l'emportent sur les resources par
     * défaut ({@code res/values/}), avec repli automatique sur la défaut
     * quand la traduction manque.</p>
     *
     * @param language la langue ISO 639 (ex. {@code "fr"}, {@code "en"}),
     *                 ou {@code null} pour revenir à la locale par défaut
     */
    void setLanguage(String language);

    /**
     * Définit le smallest width simulé en dp (ADR 0116 — L1).
     *
     * <p>Après cet appel, les resources tablette (ex.
     * {@code res/values-sw600dp/dimens.xml}) l'emportent sur les resources
     * par défaut, avec repli automatique sur {@code res/values/} quand la
     * resource manque — et sans jamais concourir pour la config téléphone
     * (sw &lt; 600).</p>
     *
     * @param smallestWidthDp le smallest width de l'écran simulé (ex. 600,
     *                        720), 0 pour revenir à la config sans dimension
     * @since 3.1
     */
    void setSmallestScreenWidth(int smallestWidthDp);

    /**
     * Vide le cache des valeurs résolues.
     */
    void clearCache();
}
