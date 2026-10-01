package jo.layoutlib.resources.namespaces;

import jo.layoutlib.resources.api.ResourceNamespace;

/**
 * Namespace TOOLS (tools:).
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ToolsNamespace extends ResourceNamespace {

    public static final ToolsNamespace INSTANCE = new ToolsNamespace();

    private ToolsNamespace() {
        super(ResourceNamespace.TOOLS.getUri(), ResourceNamespace.TOOLS.getPrefix());
    }
}
