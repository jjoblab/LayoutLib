package jo.layoutlib.resources;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jo.layoutlib.resources.api.ResourceNamespace;
import jo.layoutlib.resources.api.ResourceType;

/**
 * Fichier XML contenant plusieurs resources (colors.xml, strings.xml, etc.).
 *
 * @author jo@Dev
 * @since 1.0
 */
public class MultiResourceFile extends ResourceFile {

    private final ResourceNamespace namespace;
    private final ResourceType type;
    private final List<ResourceItem> items = new ArrayList<>();

    public MultiResourceFile(File file, String qualifier, ResourceNamespace namespace,
                             ResourceType type) {
        super(file, qualifier);
        this.namespace = namespace;
        this.type = type;
    }

    /**
     * Ajoute une resource à ce fichier.
     *
     * @param name le nom de la resource
     * @param value la valeur
     */
    public void addResource(String name, String value) {
        items.add(new ResourceItem(name, type, namespace, value, file.getName()));
    }

    @Override
    public List<ResourceItem> getResources() {
        return new ArrayList<>(items);
    }

    public ResourceType getType() {
        return type;
    }

    public ResourceNamespace getNamespace() {
        return namespace;
    }
}
