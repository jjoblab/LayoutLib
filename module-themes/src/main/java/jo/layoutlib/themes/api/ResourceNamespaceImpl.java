package jo.layoutlib.themes.api;

import jo.layoutlib.resources.api.ResourceNamespace;

/**
 * Implémentation de ResourceNamespace.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ResourceNamespaceImpl extends ResourceNamespace {

    public static final ResourceNamespaceImpl ANDROID =
            new ResourceNamespaceImpl(ResourceNamespace.ANDROID.getUri(),
                    ResourceNamespace.ANDROID.getPrefix());
    public static final ResourceNamespaceImpl RES_AUTO =
            new ResourceNamespaceImpl(ResourceNamespace.RES_AUTO.getUri(),
                    ResourceNamespace.RES_AUTO.getPrefix());

    public ResourceNamespaceImpl(String uri, String prefix) {
        super(uri, prefix);
    }
}
