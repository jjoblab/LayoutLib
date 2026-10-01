package jo.layoutlib.resources.namespaces;

import jo.layoutlib.resources.api.ResourceNamespace;

/**
 * Namespace AndroidX AppCompat.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class AppCompatNamespace extends ResourceNamespace {

    public static final AppCompatNamespace INSTANCE =
            new AppCompatNamespace();

    private AppCompatNamespace() {
        super("urn:android-appcompat", "appcompat");
    }
}
