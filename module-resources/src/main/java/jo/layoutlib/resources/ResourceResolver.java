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
     * Vide le cache des valeurs résolues.
     */
    void clearCache();
}
