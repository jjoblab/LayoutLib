package jo.layoutlib.resources;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Représente un fichier de resource, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public abstract class ResourceFile {

    protected final File file;
    protected final String qualifier;

    public ResourceFile(File file, String qualifier) {
        this.file = file;
        this.qualifier = qualifier != null ? qualifier : "";
    }

    public File getFile() {
        return file;
    }

    public String getQualifier() {
        return qualifier;
    }

    /**
     * @return les ResourceItem contenus dans ce fichier
     */
    public abstract List<ResourceItem> getResources();
}
