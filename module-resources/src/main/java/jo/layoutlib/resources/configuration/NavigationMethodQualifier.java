package jo.layoutlib.resources.configuration;

/**
 * Qualifier de méthode de navigation (dpad, trackball, wheel).
 *
 * @author jo@Dev
 * @since 1.0
 */
public class NavigationMethodQualifier implements ResourceQualifier {

    public static final String NAV_DPAD = "dpad";
    public static final String NAV_TRACKBALL = "trackball";
    public static final String NAV_WHEEL = "wheel";
    public static final String NAV_NONAV = "nonav";

    private final String method;

    public NavigationMethodQualifier(String method) {
        this.method = method;
    }

    public static NavigationMethodQualifier fromQualifier(String qualifier) {
        if (qualifier == null) return null;
        switch (qualifier) {
            case NAV_DPAD:
            case NAV_TRACKBALL:
            case NAV_WHEEL:
            case NAV_NONAV:
                return new NavigationMethodQualifier(qualifier);
            default:
                return null;
        }
    }

    @Override
    public String getName() {
        return method;
    }

    @Override
    public String getLongName() {
        return "Navigation " + method;
    }

    @Override
    public boolean isValid() {
        return method != null;
    }

    @Override
    public boolean isMatchFor(ResourceQualifier other) {
        return true;
    }

    @Override
    public boolean isBetterMatchThan(ResourceQualifier other, ResourceQualifier reference) {
        return false;
    }

    public String getMethod() {
        return method;
    }
}
