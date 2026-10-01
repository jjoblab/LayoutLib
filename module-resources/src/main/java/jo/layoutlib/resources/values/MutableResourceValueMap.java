package jo.layoutlib.resources.values;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jo.layoutlib.resources.api.ResourceReference;
import jo.layoutlib.resources.api.ResourceValue;

/**
 * Implémentation mutable de ResourceValueMap.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class MutableResourceValueMap implements ResourceValueMap {

    private final Map<String, ResourceValue> byName = new HashMap<>();

    @Override
    public void put(ResourceValue value) {
        if (value != null && value.getName() != null) {
            byName.put(value.getName(), value);
        }
    }

    @Override
    public ResourceValue get(ResourceReference ref) {
        if (ref == null) {
            return null;
        }
        return byName.get(ref.getName());
    }

    @Override
    public ResourceValue get(String name) {
        return byName.get(name);
    }

    @Override
    public Collection<ResourceValue> values() {
        return byName.values();
    }

    @Override
    public int size() {
        return byName.size();
    }

    @Override
    public boolean contains(String name) {
        return byName.containsKey(name);
    }
}
