package jo.layoutlib.resources;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository de resources générées (pour les tests et le rendu).
 *
 * @author jo@Dev
 * @since 1.0
 */
public class GeneratedResourceRepository {

    private final List<ResourceItem> items = new ArrayList<>();

    public void addResource(ResourceItem item) {
        if (item != null) {
            items.add(item);
        }
    }

    public List<ResourceItem> getResources() {
        return new ArrayList<>(items);
    }

    public int size() {
        return items.size();
    }

    public void clear() {
        items.clear();
    }
}
