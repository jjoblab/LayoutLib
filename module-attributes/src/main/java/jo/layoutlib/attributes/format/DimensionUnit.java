package jo.layoutlib.attributes.format;

/**
 * Unités de dimension Android.
 *
 * @author jo@Dev
 * @since 1.0
 */
public enum DimensionUnit {

    PX("px"),
    DP("dp"),
    DIP("dip"),
    SP("sp"),
    MM("mm"),
    IN("in"),
    PT("pt");

    private final String suffix;

    DimensionUnit(String suffix) {
        this.suffix = suffix;
    }

    public String getSuffix() {
        return suffix;
    }

    /**
     * Parse une unité depuis un suffixe.
     *
     * @param suffix le suffixe
     * @return l unité, ou null
     */
    public static DimensionUnit fromSuffix(String suffix) {
        if (suffix == null) return null;
        for (DimensionUnit u : values()) {
            if (u.suffix.equals(suffix)) {
                return u;
            }
        }
        return null;
    }
}
