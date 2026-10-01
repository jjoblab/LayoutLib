package jo.layoutlib.inflater.bridge.android;

import org.xmlpull.v1.XmlPullParser;

import java.util.HashMap;
import java.util.Map;

import jo.layoutlib.resources.api.ResourceNamespace;

/**
 * Resolver de prefixes de namespace depuis un XmlPullParser, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class XmlPullParserResolver implements ResourceNamespace.Resolver {

    private final Map<String, String> prefixToUri = new HashMap<>();
    private final Map<String, String> uriToPrefix = new HashMap<>();

    public XmlPullParserResolver() {
        // Pré-remplir avec les namespaces standards
        register(ResourceNamespace.ANDROID);
        register(ResourceNamespace.RES_AUTO);
        register(ResourceNamespace.TOOLS);
    }

    /**
     * Charge les namespaces depuis un parser.
     *
     * @param parser le parser positionné sur un START_TAG
     */
    public void loadFromParser(XmlPullParser parser) {
        if (parser == null) return;
        int count = parser.getAttributeCount();
        for (int i = 0; i < count; i++) {
            String name = parser.getAttributeName(i);
            if (name != null && name.startsWith("xmlns:")) {
                String prefix = name.substring("xmlns:".length());
                String uri = parser.getAttributeValue(i);
                if (uri != null) {
                    prefixToUri.put(prefix, uri);
                    uriToPrefix.put(uri, prefix);
                }
            }
        }
    }

    public void register(ResourceNamespace namespace) {
        if (namespace != null) {
            prefixToUri.put(namespace.getPrefix(), namespace.getUri());
            uriToPrefix.put(namespace.getUri(), namespace.getPrefix());
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
