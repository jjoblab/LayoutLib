package jo.layoutlib.resources.configuration;

/**
 * Qualifier de plus petite largeur d écran (sw600dp, sw720dp, etc.).
 *
 * @author jo@Dev
 * @since 1.0
 */
public class SmallestScreenWidthQualifier implements ResourceQualifier {

    private final int widthDp;

    public SmallestScreenWidthQualifier(int widthDp) {
        this.widthDp = widthDp;
    }

    public static SmallestScreenWidthQualifier fromQualifier(String qualifier) {
        if (qualifier == null || !qualifier.startsWith("sw") || !qualifier.endsWith("dp")) {
            return null;
        }
        try {
            int w = Integer.parseInt(qualifier.substring(2, qualifier.length() - 2));
            return new SmallestScreenWidthQualifier(w);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String getName() {
        return "sw" + widthDp + "dp";
    }

    @Override
    public String getLongName() {
        return "Plus petite largeur " + widthDp + "dp";
    }

    @Override
    public boolean isValid() {
        return widthDp > 0;
    }

    @Override
    public boolean isMatchFor(ResourceQualifier other) {
        if (!(other instanceof SmallestScreenWidthQualifier)) return true;
        return this.widthDp <= ((SmallestScreenWidthQualifier) other).widthDp;
    }

    @Override
    public boolean isBetterMatchThan(ResourceQualifier other, ResourceQualifier reference) {
        if (!(other instanceof SmallestScreenWidthQualifier)) return false;
        if (!(reference instanceof SmallestScreenWidthQualifier)) return false;
        int refW = ((SmallestScreenWidthQualifier) reference).widthDp;
        return this.widthDp <= refW && this.widthDp > ((SmallestScreenWidthQualifier) other).widthDp;
    }

    public int getWidthDp() {
        return widthDp;
    }
}
