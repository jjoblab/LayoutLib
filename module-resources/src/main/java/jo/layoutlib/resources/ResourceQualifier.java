package jo.layoutlib.resources;

import java.util.Objects;

/**
 * Représente un qualifier de dossier de resources Android.
 *
 * <p>Dans Android, un dossier de resources peut avoir plusieurs qualifiers
 * séparés par des tirets : {@code values-night-v31-land}. Cette classe
 * encapsule ces trois dimensions principales :</p>
 *
 * <ul>
 *   <li>{@code night} : mode jour (DayNight) ou nuit</li>
 *   <li>{@code land} : orientation portrait ou paysage</li>
 *   <li>{@code apiLevel} : niveau d'API minimum (ex. v31 pour Android 12)</li>
 * </ul>
 *
 * <p>L'algorithme de priorité suit les règles AOSP : les qualifiers plus
 * spécifiques priment. Par exemple, en mode nuit avec API 31, le dossier
 * {@code values-night-v31} prime sur {@code values-night} qui prime sur
 * {@code values}.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public final class ResourceQualifier implements Comparable<ResourceQualifier> {

    /** Qualifier par défaut : jour, portrait, API 1. */
    public static final ResourceQualifier DEFAULT = new ResourceQualifier(false, false, 1);

    /** {@code true} si le qualifier correspond au mode nuit. */
    private final boolean night;

    /** {@code true} si le qualifier correspond au mode paysage. */
    private final boolean landscape;

    /** Niveau d'API minimum du qualifier (1 si non spécifié). */
    private final int apiLevel;

    /**
     * Construit un nouveau qualifier.
     *
     * @param night      {@code true} pour le mode nuit
     * @param landscape  {@code true} pour le mode paysage
     * @param apiLevel   niveau d'API minimum (doit être ≥ 1)
     */
    public ResourceQualifier(boolean night, boolean landscape, int apiLevel) {
        if (apiLevel < 1) {
            throw new IllegalArgumentException("apiLevel doit être >= 1, reçu : " + apiLevel);
        }
        this.night = night;
        this.landscape = landscape;
        this.apiLevel = apiLevel;
    }

    /**
     * Parse un nom de dossier de resources et en extrait le qualifier.
     *
     * <p>Exemples :</p>
     * <ul>
     *   <li>{@code "values"} → {@code DEFAULT}</li>
     *   <li>{@code "values-night"} → night=true</li>
     *   <li>{@code "values-land-v31"} → landscape=true, apiLevel=31</li>
     *   <li>{@code "values-night-land-v31"} → night=true, landscape=true, apiLevel=31</li>
     * </ul>
     *
     * @param folderName nom du dossier (ex. {@code "values-night-v31"})
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
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if ("night".equals(part)) {
                night = true;
            } else if ("land".equals(part)) {
                landscape = true;
            } else if (part.startsWith("v") && part.length() > 1) {
                try {
                    api = Integer.parseInt(part.substring(1));
                } catch (NumberFormatException e) {
                    throw new ResourceException(
                            "Niveau d'API invalide dans le dossier : " + folderName, folderName);
                }
            }
        }
        return new ResourceQualifier(night, landscape, api);
    }

    /**
     * Indique si ce qualifier est compatible avec un contexte cible donné.
     *
     * <p>Un qualifier est compatible si :</p>
     * <ul>
     *   <li>son mode nuit correspond au mode cible (ou il est en mode jour par défaut)</li>
     *   <li>son mode paysage correspond au mode cible (ou il est en mode portrait par défaut)</li>
     *   <li>son niveau d'API est ≤ au niveau cible</li>
     * </ul>
     *
     * @param target le qualifier cible (le contexte courant)
     * @return {@code true} si ce qualifier peut être utilisé dans le contexte cible
     */
    public boolean isCompatibleWith(ResourceQualifier target) {
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
     * <p>Le score est calculé ainsi :</p>
     * <ul>
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
        // Tri par spécificité décroissante
        int c = Boolean.compare(other.night, this.night);
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
                && apiLevel == that.apiLevel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(night, landscape, apiLevel);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("values");
        if (night) sb.append("-night");
        if (landscape) sb.append("-land");
        if (apiLevel > 1) sb.append("-v").append(apiLevel);
        return sb.toString();
    }
}
