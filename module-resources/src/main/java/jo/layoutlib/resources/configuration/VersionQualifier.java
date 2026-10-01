package jo.layoutlib.resources.configuration;

/**
 * Qualifier de version API (v21, v31, etc.).
 *
 * @author jo@Dev
 * @since 1.0
 */
public class VersionQualifier implements ResourceQualifier {

    private final int apiLevel;

    public VersionQualifier(int apiLevel) {
        this.apiLevel = apiLevel;
    }

    public static VersionQualifier fromQualifier(String qualifier) {
        if (qualifier == null || !qualifier.startsWith("v")) return null;
        try {
            int api = Integer.parseInt(qualifier.substring(1));
            return new VersionQualifier(api);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String getName() {
        return "v" + apiLevel;
    }

    @Override
    public String getLongName() {
        return "API level " + apiLevel;
    }

    @Override
    public boolean isValid() {
        return apiLevel > 0;
    }

    @Override
    public boolean isMatchFor(ResourceQualifier other) {
        if (!(other instanceof VersionQualifier)) return true;
        return this.apiLevel <= ((VersionQualifier) other).apiLevel;
    }

    @Override
    public boolean isBetterMatchThan(ResourceQualifier other, ResourceQualifier reference) {
        if (!(other instanceof VersionQualifier)) return false;
        if (!(reference instanceof VersionQualifier)) return false;
        int refApi = ((VersionQualifier) reference).apiLevel;
        int myDist = refApi - this.apiLevel;
        int otherDist = refApi - ((VersionQualifier) other).apiLevel;
        if (myDist < 0) myDist = Integer.MAX_VALUE;  // this.apiLevel > refApi = pas compatible
        if (otherDist < 0) otherDist = Integer.MAX_VALUE;
        return myDist < otherDist;
    }

    public int getApiLevel() {
        return apiLevel;
    }
}
