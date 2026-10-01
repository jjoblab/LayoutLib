package jo.layoutlib.resources.configuration;

import java.util.Locale;
import java.util.Objects;

/**
 * Qualifier de langue (fr, en, es, etc.).
 *
 * @author jo@Dev
 * @since 1.0
 */
public class LocaleQualifier implements ResourceQualifier {

    private final String language;
    private final String region;

    public LocaleQualifier(String language) {
        this(language, null);
    }

    public LocaleQualifier(String language, String region) {
        this.language = language != null ? language.toLowerCase() : null;
        this.region = region != null ? region.toUpperCase() : null;
    }

    /**
     * Parse un qualifier de locale (ex. "fr", "fr-rFR").
     *
     * @param qualifier le qualifier
     * @return la locale, ou null
     */
    public static LocaleQualifier fromQualifier(String qualifier) {
        if (qualifier == null || qualifier.isEmpty()) return null;
        if (qualifier.contains("-r")) {
            String[] parts = qualifier.split("-r");
            if (parts.length == 2) {
                return new LocaleQualifier(parts[0], parts[1]);
            }
        }
        return new LocaleQualifier(qualifier);
    }

    @Override
    public String getName() {
        if (region != null) {
            return language + "-r" + region;
        }
        return language != null ? language : "";
    }

    @Override
    public String getLongName() {
        if (language == null) return "Aucune locale";
        Locale locale = new Locale(language, region != null ? region : "");
        return "Locale " + locale.getDisplayName();
    }

    @Override
    public boolean isValid() {
        return language != null && !language.isEmpty();
    }

    @Override
    public boolean isMatchFor(ResourceQualifier other) {
        if (!(other instanceof LocaleQualifier)) return false;
        LocaleQualifier o = (LocaleQualifier) other;
        if (o.language == null) return true;
        return Objects.equals(language, o.language);
    }

    @Override
    public boolean isBetterMatchThan(ResourceQualifier other, ResourceQualifier reference) {
        if (!(other instanceof LocaleQualifier)) return false;
        // Match avec région est meilleur que sans région
        return this.region != null && ((LocaleQualifier) other).region == null;
    }

    public String getLanguage() {
        return language;
    }

    public String getRegion() {
        return region;
    }
}
