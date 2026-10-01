package jo.layoutlib.resources.values;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jo.layoutlib.resources.api.ResourceReference;
import jo.layoutlib.resources.api.ResourceValue;

/**
 * Wrapper immutable de ResourceValueMap.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ImmutableResourceValueMap implements ResourceValueMap {

    private final Map<String, ResourceValue> backing;

    public ImmutableResourceValueMap(ResourceValueMap source) {
        this.backing = new HashMap<>();
        if (source != null) {
            for (ResourceValue v : source.values()) {
                backing.put(v.getName(), v);
            }
        }
    }

    @Override
    public void put(ResourceValue value) {
        throw new UnsupportedOperationException("Map immutable");
    }

    @Override
    public ResourceValue get(ResourceReference ref) {
        if (ref == null) return null;
        return backing.get(ref.getName());
    }

    @Override
    public ResourceValue get(String name) {
        return backing.get(name);
    }

    @Override
    public Collection<ResourceValue> values() {
        return Collections.unmodifiableCollection(backing.values());
    }

    @Override
    public int size() {
        return backing.size();
    }

    @Override
    public boolean contains(String name) {
        return backing.containsKey(name);
    }
}
