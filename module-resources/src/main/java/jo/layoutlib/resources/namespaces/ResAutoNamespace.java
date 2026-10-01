package jo.layoutlib.resources.namespaces;

import jo.layoutlib.resources.api.ResourceNamespace;

/**
 * Namespace RES_AUTO (app:).
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ResAutoNamespace extends ResourceNamespace {

    public static final ResAutoNamespace INSTANCE = new ResAutoNamespace();

    private ResAutoNamespace() {
        super(ResourceNamespace.RES_AUTO.getUri(), ResourceNamespace.RES_AUTO.getPrefix());
    }
}
