package jo.layoutlib.resources.namespaces;

import jo.layoutlib.resources.api.ResourceNamespace;

/**
 * Namespace ANDROID (android:).
 *
 * @author jo@Dev
 * @since 1.0
 */
public class AndroidNamespace extends ResourceNamespace {

    public static final AndroidNamespace INSTANCE = new AndroidNamespace();

    private AndroidNamespace() {
        super(ResourceNamespace.ANDROID.getUri(), ResourceNamespace.ANDROID.getPrefix());
    }
}
