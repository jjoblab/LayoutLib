package jo.layoutlib.attributes.format;

/**
 * Unités de fraction Android.
 *
 * @author jo@Dev
 * @since 1.0
 */
public enum FractionUnit {

    PERCENT("%"),
    PERCENT_OF_PARENT("%p");

    private final String suffix;

    FractionUnit(String suffix) {
        this.suffix = suffix;
    }

    public String getSuffix() {
        return suffix;
    }

    public static FractionUnit fromSuffix(String suffix) {
        if ("%".equals(suffix)) return PERCENT;
        if ("%p".equals(suffix)) return PERCENT_OF_PARENT;
        return null;
    }
}
