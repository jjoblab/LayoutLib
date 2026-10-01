package jo.layoutlib.resources.configuration;

/**
 * Qualifier de taille d écran (small, normal, large, xlarge).
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ScreenSizeQualifier implements ResourceQualifier {

    public static final int SIZE_SMALL = 1;
    public static final int SIZE_NORMAL = 2;
    public static final int SIZE_LARGE = 3;
    public static final int SIZE_XLARGE = 4;

    private final int size;

    public ScreenSizeQualifier(int size) {
        this.size = size;
    }

    public static ScreenSizeQualifier fromQualifier(String qualifier) {
        if (qualifier == null) return null;
        switch (qualifier) {
            case "small": return new ScreenSizeQualifier(SIZE_SMALL);
            case "normal": return new ScreenSizeQualifier(SIZE_NORMAL);
            case "large": return new ScreenSizeQualifier(SIZE_LARGE);
            case "xlarge": return new ScreenSizeQualifier(SIZE_XLARGE);
            default: return null;
        }
    }

    @Override
    public String getName() {
        switch (size) {
            case SIZE_SMALL: return "small";
            case SIZE_NORMAL: return "normal";
            case SIZE_LARGE: return "large";
            case SIZE_XLARGE: return "xlarge";
            default: return "";
        }
    }

    @Override
    public String getLongName() {
        return "Taille écran " + getName();
    }

    @Override
    public boolean isValid() {
        return size >= SIZE_SMALL && size <= SIZE_XLARGE;
    }

    @Override
    public boolean isMatchFor(ResourceQualifier other) {
        return true;  // Toute taille match
    }

    @Override
    public boolean isBetterMatchThan(ResourceQualifier other, ResourceQualifier reference) {
        if (!(other instanceof ScreenSizeQualifier)) return false;
        ScreenSizeQualifier ref = (ScreenSizeQualifier) reference;
        return Math.abs(this.size - ref.size) < Math.abs(((ScreenSizeQualifier) other).size - ref.size);
    }

    public int getSize() {
        return size;
    }
}
