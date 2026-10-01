package jo.layoutlib.resources;

import jo.layoutlib.resources.api.ResourceNamespace;
import jo.layoutlib.resources.api.ResourceType;
import jo.layoutlib.resources.api.ResourceValue;

/**
 * Représente une resource individuelle, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ResourceItem {

    private final String name;
    private final ResourceType type;
    private final ResourceNamespace namespace;
    private final String value;
    private final String sourceFile;

    public ResourceItem(String name, ResourceType type, ResourceNamespace namespace,
                        String value, String sourceFile) {
        this.name = name;
        this.type = type;
        this.namespace = namespace;
        this.value = value;
        this.sourceFile = sourceFile;
    }

    public String getName() {
        return name;
    }

    public ResourceType getType() {
        return type;
    }

    public ResourceNamespace getNamespace() {
        return namespace;
    }

    public String getValue() {
        return value;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    /**
     * Convertit en ResourceValue.
     *
     * @return le ResourceValue équivalent
     */
    public ResourceValue toResourceValue() {
        return new jo.layoutlib.resources.api.ResourceValueImpl(
                namespace, type, name, value);
    }

    @Override
    public String toString() {
        return namespace.getPrefix() + ":" + type.getName() + "/" + name;
    }
}
