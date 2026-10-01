package jo.layoutlib.resources.namespaces;

import java.util.HashMap;
import java.util.Map;

import jo.layoutlib.resources.api.ResourceNamespace;

/**
 * Resolver de prefixes de namespace.
 * Map les prefixes (ex. "android", "app") vers les URIs.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class NamespaceResolver implements ResourceNamespace.Resolver {

    private final Map<String, String> prefixToUri = new HashMap<>();
    private final Map<String, String> uriToPrefix = new HashMap<>();

    public NamespaceResolver() {
        // Pré-remplir avec les namespaces standards
        register(ResourceNamespace.ANDROID);
        register(ResourceNamespace.RES_AUTO);
        register(ResourceNamespace.TOOLS);
    }

    public void register(ResourceNamespace namespace) {
        if (namespace != null) {
            prefixToUri.put(namespace.getPrefix(), namespace.getUri());
            uriToPrefix.put(namespace.getUri(), namespace.getPrefix());
        }
    }

    public void register(String prefix, String uri) {
        if (prefix != null && uri != null) {
            prefixToUri.put(prefix, uri);
            uriToPrefix.put(uri, prefix);
        }
    }

    @Override
    public String prefixToUri(String prefix) {
        return prefixToUri.get(prefix);
    }

    @Override
    public String uriToPrefix(String uri) {
        return uriToPrefix.get(uri);
    }

    public int size() {
        return prefixToUri.size();
    }
}
