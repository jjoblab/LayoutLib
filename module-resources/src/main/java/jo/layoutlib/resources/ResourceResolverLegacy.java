package jo.layoutlib.resources;

import java.util.Map;

/**
 * Wrapper legacy pour la compatibilité avec l ancien API.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ResourceResolverLegacy {

    private final ResourceResolverImpl delegate;

    public ResourceResolverLegacy(ResourceResolverImpl delegate) {
        this.delegate = delegate;
    }

    public Integer getColor(String ref) {
        return delegate.getColor(ref);
    }

    public String getString(String ref) {
        return delegate.getString(ref);
    }

    public Float getDimension(String ref) {
        return delegate.getDimension(ref);
    }

    public Integer getInteger(String ref) {
        return delegate.getInteger(ref);
    }

    public Boolean getBoolean(String ref) {
        return delegate.getBoolean(ref);
    }

    public ResourceResolverImpl getDelegate() {
        return delegate;
    }
}
