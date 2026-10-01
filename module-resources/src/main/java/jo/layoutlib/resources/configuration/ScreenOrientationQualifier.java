package jo.layoutlib.resources.configuration;

/**
 * Qualifier d orientation (port, land).
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ScreenOrientationQualifier implements ResourceQualifier {

    private final boolean landscape;

    public ScreenOrientationQualifier(boolean landscape) {
        this.landscape = landscape;
    }

    public static ScreenOrientationQualifier fromQualifier(String qualifier) {
        if ("land".equals(qualifier)) return new ScreenOrientationQualifier(true);
        if ("port".equals(qualifier)) return new ScreenOrientationQualifier(false);
        return null;
    }

    @Override
    public String getName() {
        return landscape ? "land" : "port";
    }

    @Override
    public String getLongName() {
        return landscape ? "Paysage" : "Portrait";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isMatchFor(ResourceQualifier other) {
        if (!(other instanceof ScreenOrientationQualifier)) return true;
        return this.landscape == ((ScreenOrientationQualifier) other).landscape;
    }

    @Override
    public boolean isBetterMatchThan(ResourceQualifier other, ResourceQualifier reference) {
        if (!(other instanceof ScreenOrientationQualifier)) return false;
        if (!(reference instanceof ScreenOrientationQualifier)) return false;
        return this.landscape == ((ScreenOrientationQualifier) reference).landscape
                && ((ScreenOrientationQualifier) other).landscape != this.landscape;
    }

    public boolean isLandscape() {
        return landscape;
    }
}
