package jo.layoutlib.resources;

import java.io.File;
import java.util.Collections;
import java.util.List;

import jo.layoutlib.resources.api.ResourceNamespace;
import jo.layoutlib.resources.api.ResourceType;

/**
 * Fichier de resource contenant une seule resource (images, etc.).
 *
 * @author jo@Dev
 * @since 1.0
 */
public class SingleResourceFile extends ResourceFile {

    private final ResourceItem item;

    public SingleResourceFile(File file, String qualifier, ResourceNamespace namespace,
                              ResourceType type, String resourceName) {
        super(file, qualifier);
        this.item = new ResourceItem(resourceName, type, namespace, file.getAbsolutePath(), file.getName());
    }

    @Override
    public List<ResourceItem> getResources() {
        return Collections.singletonList(item);
    }
}
