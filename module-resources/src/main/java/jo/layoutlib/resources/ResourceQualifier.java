package jo.layoutlib.resources;

import java.util.Locale;
import java.util.Objects;

/**
 * Représente un qualifier de dossier de resources Android.
 *
 * <p>Dans Android, un dossier de resources peut avoir plusieurs qualifiers
 * séparés par des tirets : {@code values-fr-night-v31-land}. Cette classe
 * encapsule ces dimensions principales :</p>
 *
 * <ul>
 *   <li>{@code fr} : langue ISO 639-1/639-2 (avec région optionnelle
 *       {@code rFR}) — dimension la plus prioritaire selon l'AOSP</li>
 *   <li>{@code night} : mode jour (DayNight) ou nuit</li>
 *   <li>{@code land} : orientation portrait ou paysage</li>
 *   <li>{@code apiLevel} : niveau d'API minimum (ex. v31 pour Android 12)</li>
 * </ul>
 *
 * <p>L'algorithme de priorité suit les règles AOSP : les qualifiers plus
 * spécifiques priment. Par exemple, en locale fr avec mode nuit et API 31,
 * le dossier {@code values-fr-night-v31} prime sur {@code values-fr-night}
 * qui prime sur {@code values-fr} qui prime sur {@code values}.</p>
 *
 * <p>La langue suit la sémantique de repli Android : une resource sans
 * qualifier de langue (dossier {@code values/}) est utilisée pour toutes
 * les locales comme repli, mais une resource localisée
 * ({@code values-fr/}) ne l'emporte que si la locale cible correspond.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public final class ResourceQualifier implements Comparable<ResourceQualifier> {

    /** Qualifier par défaut : jour, portrait, API 1, aucune locale. */
    public static final ResourceQualifier DEFAULT = new ResourceQualifier(false, false, 1);

    /** Langue ISO 639 (2-3 lettres minuscules), ou {@code null} si absente. */
    private final String language;

    /** Région ISO 3166 (2 lettres majuscules, sans le préfixe r), ou {@code null}. */
    private final String region;

    /** {@code true} si le qualifier correspond au mode nuit. */
    private final boolean night;

    /** {@code true} si le qualifier correspond au mode paysage. */
    private final boolean landscape;

    /** Niveau d'API minimum du qualifier (1 si non spécifié). */
    private final int apiLevel;

    /**
     * Construit un qualifier sans locale (compatibilité v1).
     *
     * @param night      {@code true} pour le mode nuit
     * @param landscape  {@code true} pour le mode paysage
     * @param apiLevel   niveau d'API minimum (doit être ≥ 1)
     */
    public ResourceQualifier(boolean night, boolean landscape, int apiLevel) {
        this(night, landscape, apiLevel, null, null);
    }

    /**
     * Construit un qualifier complet.
     *
     * @param night      {@code true} pour le mode nuit
     * @param landscape  {@code true} pour le mode paysage
     * @param apiLevel   niveau d'API minimum (doit être ≥ 1)
     * @param language   langue ISO 639 (ex. {@code "fr"}), ou {@code null}
     * @param region     région ISO 3166 (ex. {@code "FR"}), ou {@code null}
     */
    public ResourceQualifier(boolean night, boolean landscape, int apiLevel,
                             String language, String region) {
        if (apiLevel < 1) {
            throw new IllegalArgumentException("apiLevel doit être >= 1, reçu : " + apiLevel);
        }
        this.night = night;
        this.landscape = landscape;
        this.apiLevel = apiLevel;
        this.language = normalizeLanguage(language);
        this.region = normalizeRegion(region);
    }

    /**
     * Crée un qualifier de locale (même night/land/api que DEFAULT).
     *
     * @param language la langue ISO 639 (ex. {@code "fr"}), ou {@code null}
     * @return le qualifier
     */
    public static ResourceQualifier forLanguage(String language) {
        return forLanguage(language, null);
    }

    /**
     * Crée un qualifier de locale avec région.
     *
     * @param language la langue ISO 639 (ex. {@code "fr"}), ou {@code null}
     * @param region   la région ISO 3166 (ex. {@code "FR"}), ou {@code null}
     * @return le qualifier
     */
    public static ResourceQualifier forLanguage(String language, String region) {
        return new ResourceQualifier(false, false, 1, language, region);
    }

    /**
     * Parse un nom de dossier de resources et en extrait le qualifier.
     *
     * <p>Exemples :</p>
     * <ul>
     *   <li>{@code "values"} → DEFAULT</li>
     *   <li>{@code "values-night"} → night=true</li>
     *   <li>{@code "values-land-v31"} → landscape=true, apiLevel=31</li>
     *   <li>{@code "values-fr"} → language="fr"</li>
     *   <li>{@code "values-en-rUS"} → language="en", region="US"</li>
     *   <li>{@code "values-fr-night-v31"} → language="fr", night, v31</li>
     *   <li>{@code "values-b+fr"} → language="fr" (BCP47, toléré)</li>
     * </ul>
     *
     * <p>Les qualifiers inconnus ({@code hdpi}, {@code sw600dp}, …) sont
     * ignorés silencieusement, comme par le passé — la tolérance prime.</p>
     *
     * @param folderName nom du dossier (ex. {@code "values-fr-night"})
     * @return le qualifier parsé, jamais {@code null}
     * @throws ResourceException si le nom du dossier est invalide
     */
    public static ResourceQualifier parse(String folderName) {
        if (folderName == null || folderName.isEmpty()) {
            return DEFAULT;
        }
        String[] parts = folderName.split("-");
        boolean night = false;
        boolean landscape = false;
        int api = 1;
        String language = null;
        String region = null;
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if ("night".equals(part)) {
                night = true;
            } else if ("land".equals(part)) {
                landscape = true;
            } else if (part.startsWith("v") && part.length() > 1
                    && isAllDigits(part.substring(1))) {
                try {
                    api = Integer.parseInt(part.substring(1));
                } catch (NumberFormatException e) {
                    throw new ResourceException(
                            "Niveau d'API invalide dans le dossier : " + folderName, folderName);
                }
            } else if (part.startsWith("r") && part.length() > 1
                    && isAllUpperLetters(part.substring(1))) {
                // Region : values-fr-rFR
                region = part.substring(1);
            } else if (part.startsWith("b+") && part.length() > 2) {
                // BCP47 : values-b+fr+Latn — best effort sur la langue
                language = normalizeLanguage(part.substring(2));
            } else if (isLanguageCode(part)) {
                // Langue ISO 639 : 2-3 lettres minuscules ("car" est un
                // qualifier UI-mode, exclu volontairement)
                language = part;
            }
            // Autres qualifiers (hdpi, sw600dp, port, …) : ignorés
        }
        return new ResourceQualifier(night, landscape, api, language, region);
    }

    /**
     * Indique si ce qualifier est compatible avec un contexte cible donné.
     *
     * <p>Un qualifier est compatible si :</p>
     * <ul>
     *   <li>sa langue correspond à la langue cible (une resource localisée
     *       ne sert que sa locale ; une resource sans langue sert toutes
     *       les locales — repli Android)</li>
     *   <li>sa région correspond à la région cible si elle en porte une</li>
     *   <li>son mode nuit correspond au mode cible (ou il est en mode jour par défaut)</li>
     *   <li>son mode paysage correspond au mode cible (ou il est en mode portrait par défaut)</li>
     *   <li>son niveau d'API est ≤ au niveau cible</li>
     * </ul>
     *
     * @param target le qualifier cible (le contexte courant)
     * @return {@code true} si ce qualifier peut être utilisé dans le contexte cible
     */
    public boolean isCompatibleWith(ResourceQualifier target) {
        if (language != null && !language.equals(target.language)) {
            return false;
        }
        if (region != null && !region.equals(target.region)) {
            return false;
        }
        if (this.night != target.night && this.night) {
            return false;
        }
        if (this.landscape != target.landscape && this.landscape) {
            return false;
        }
        return this.apiLevel <= target.apiLevel;
    }

    /**
     * Calcule un score de spécificité utilisé pour trier les qualifiers
     * compatibles. Plus le score est élevé, plus le qualifier est spécifique
     * (et donc prioritaire).
     *
     * <p>Le score suit l'ordre de priorité AOSP des qualifiers :</p>
     * <ul>
     *   <li>+10000 si langue correspondante (locale &gt; tout le reste)</li>
     *   <li>+5000 si région correspondante</li>
     *   <li>+1000 si mode nuit correspondant</li>
     *   <li>+100 si mode paysage correspondant</li>
     *   <li>+ apiLevel (les API plus élevées sont plus spécifiques)</li>
     * </ul>
     *
     * @param target le contexte cible
     * @return le score de spécificité
     */
    public int specificityScore(ResourceQualifier target) {
        int score = 0;
        if (language != null && language.equals(target.language)) {
            score += 10000;
        }
        if (region != null && region.equals(target.region)) {
            score += 5000;
        }
        if (this.night && this.night == target.night) {
            score += 1000;
        }
        if (this.landscape && this.landscape == target.landscape) {
            score += 100;
        }
        if (this.apiLevel <= target.apiLevel) {
            score += this.apiLevel;
        }
        return score;
    }

    /**
     * @return la langue ISO 639 (ex. {@code "fr"}), ou {@code null}
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @return la région ISO 3166 (ex. {@code "FR"}), ou {@code null}
     */
    public String getRegion() {
        return region;
    }

    /**
     * @return {@code true} si le qualifier porte une langue
     */
    public boolean hasLanguage() {
        return language != null;
    }

    /**
     * @return {@code true} si le qualifier correspond au mode nuit
     */
    public boolean isNight() {
        return night;
    }

    /**
     * @return {@code true} si le qualifier correspond au mode paysage
     */
    public boolean isLandscape() {
        return landscape;
    }

    /**
     * @return le niveau d'API minimum du qualifier
     */
    public int getApiLevel() {
        return apiLevel;
    }

    @Override
    public int compareTo(ResourceQualifier other) {
        // Tri par spécificité décroissante (plus spécifique = plus petit)
        int c = compareNullable(this.language, other.language);
        if (c != 0) return c;
        c = compareNullable(this.region, other.region);
        if (c != 0) return c;
        c = Boolean.compare(other.night, this.night);
        if (c != 0) return c;
        c = Boolean.compare(other.landscape, this.landscape);
        if (c != 0) return c;
        return Integer.compare(other.apiLevel, this.apiLevel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceQualifier that = (ResourceQualifier) o;
        return night == that.night
                && landscape == that.landscape
                && apiLevel == that.apiLevel
                && Objects.equals(language, that.language)
                && Objects.equals(region, that.region);
    }

    @Override
    public int hashCode() {
        return Objects.hash(language, region, night, landscape, apiLevel);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("values");
        if (language != null) {
            sb.append("-").append(language);
            if (region != null) {
                sb.append("-r").append(region);
            }
        }
        if (night) sb.append("-night");
        if (landscape) sb.append("-land");
        if (apiLevel > 1) sb.append("-v").append(apiLevel);
        return sb.toString();
    }

    /**
     * Normalise une langue : minuscules, null si vide.
     */
    private static String normalizeLanguage(String language) {
        if (language == null || language.isEmpty()) {
            return null;
        }
        return language.toLowerCase(Locale.ROOT);
    }

    /**
     * Normalise une région : majuscules, null si vide.
     */
    private static String normalizeRegion(String region) {
        if (region == null || region.isEmpty()) {
            return null;
        }
        return region.toUpperCase(Locale.ROOT);
    }

    /**
     * Indique si un token est un code de langue ISO 639 plausible :
     * 2 ou 3 lettres minuscules, hors mots-clés réservés.
     */
    private static boolean isLanguageCode(String token) {
        if (token == null || token.length() < 2 || token.length() > 3) {
            return false;
        }
        if (!isAllLowerLetters(token)) {
            return false;
        }
        // "car" est le qualifier UI-mode (dock voiture), pas une langue
        return !"car".equals(token);
    }

    /**
     * Indique si une chaîne ne contient que des chiffres.
     */
    private static boolean isAllDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return !s.isEmpty();
    }

    /**
     * Indique si une chaîne ne contient que des majuscules ASCII.
     */
    private static boolean isAllUpperLetters(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 'A' || c > 'Z') {
                return false;
            }
        }
        return !s.isEmpty();
    }

    /**
     * Indique si une chaîne ne contient que des minuscules ASCII.
     */
    private static boolean isAllLowerLetters(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 'a' || c > 'z') {
                return false;
            }
        }
        return !s.isEmpty();
    }

    /**
     * Compare deux chaînes nullables : les non-null priment (null = moins
     * spécifique, donc plus grand dans l'ordre), puis ordre
     * lexicographique.
     */
    private static int compareNullable(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return a.compareTo(b);
    }
}
