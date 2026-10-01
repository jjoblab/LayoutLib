package jo.layoutlib.resources;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jo.layoutlib.resources.api.ResourceNamespace;

/**
 * Repository de resources du projet (dossier res/).
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ProjectResourceRepository {

    private final File resFolder;
    private final ResourceNamespace namespace;
    private final List<ResourceFile> files = new ArrayList<>();

    public ProjectResourceRepository(File resFolder) {
        this(resFolder, ResourceNamespace.RES_AUTO);
    }

    public ProjectResourceRepository(File resFolder, ResourceNamespace namespace) {
        this.resFolder = resFolder;
        this.namespace = namespace;
        if (resFolder != null && resFolder.isDirectory()) {
            scanFolder();
        }
    }

    private void scanFolder() {
        File[] subdirs = resFolder.listFiles(File::isDirectory);
        if (subdirs == null) return;
        for (File subdir : subdirs) {
            String name = subdir.getName();
            String qualifier = "";
            int dash = name.indexOf('-');
            if (dash >= 0) {
                qualifier = name.substring(dash + 1);
                name = name.substring(0, dash);
            }
            File[] xmlFiles = subdir.listFiles((d, n) -> n.endsWith(".xml"));
            if (xmlFiles != null) {
                for (File xml : xmlFiles) {
                    files.add(new MultiResourceFile(xml, qualifier, namespace,
                            resolveType(name)));
                }
            }
        }
    }

    private jo.layoutlib.resources.api.ResourceType resolveType(String folderName) {
        switch (folderName) {
            case "values": return jo.layoutlib.resources.api.ResourceType.STRING;
            case "drawable":
            case "mipmap": return jo.layoutlib.resources.api.ResourceType.DRAWABLE;
            case "layout": return jo.layoutlib.resources.api.ResourceType.LAYOUT;
            case "anim": return jo.layoutlib.resources.api.ResourceType.ANIM;
            case "color": return jo.layoutlib.resources.api.ResourceType.COLOR;
            case "menu": return jo.layoutlib.resources.api.ResourceType.MENU;
            default: return jo.layoutlib.resources.api.ResourceType.STRING;
        }
    }

    public List<ResourceFile> getFiles() {
        return new ArrayList<>(files);
    }

    public File getResFolder() {
        return resFolder;
    }

    public ResourceNamespace getNamespace() {
        return namespace;
    }
}
