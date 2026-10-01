package jo.layoutlib.resources.configuration;

import jo.layoutlib.resources.api.Density;

/**
 * Qualifier de densité d écran (hdpi, xhdpi, etc.).
 *
 * @author jo@Dev
 * @since 1.0
 */
public class DensityQualifier implements ResourceQualifier {

    private final Density density;

    public DensityQualifier(Density density) {
        this.density = density;
    }

    /**
     * Crée un qualifier depuis un nom de dossier (ex. "xhdpi").
     *
     * @param qualifier le nom
     * @return le qualifier, ou null
     */
    public static DensityQualifier fromQualifier(String qualifier) {
        Density d = Density.fromQualifier(qualifier);
        return d != null ? new DensityQualifier(d) : null;
    }

    @Override
    public String getName() {
        return density != null ? density.getQualifier() : "";
    }

    @Override
    public String getLongName() {
        return density != null ? "Densité " + density.getQualifier() + " (" + density.getDpiValue() + " dpi)" : "Aucune densité";
    }

    @Override
    public boolean isValid() {
        return density != null;
    }

    @Override
    public boolean isMatchFor(ResourceQualifier other) {
        if (!(other instanceof DensityQualifier)) return false;
        return true;  // Toute densité match par défaut
    }

    @Override
    public boolean isBetterMatchThan(ResourceQualifier other, ResourceQualifier reference) {
        if (!(other instanceof DensityQualifier) || !(reference instanceof DensityQualifier)) {
            return false;
        }
        DensityQualifier ref = (DensityQualifier) reference;
        DensityQualifier me = this;
        DensityQualifier o = (DensityQualifier) other;
        if (ref.density == null) return false;
        int myDist = me.density != null ? Math.abs(me.density.getDpiValue() - ref.density.getDpiValue()) : Integer.MAX_VALUE;
        int otherDist = o.density != null ? Math.abs(o.density.getDpiValue() - ref.density.getDpiValue()) : Integer.MAX_VALUE;
        return myDist < otherDist;
    }

    public Density getDensity() {
        return density;
    }
}
