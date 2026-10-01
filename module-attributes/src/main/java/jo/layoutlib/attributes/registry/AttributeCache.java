package jo.layoutlib.attributes.registry;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache d attributs, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class AttributeCache<V> {

    private final Map<String, V> cache = new HashMap<>();
    private final int maxSize;

    public AttributeCache() {
        this(256);
    }

    public AttributeCache(int maxSize) {
        this.maxSize = maxSize > 0 ? maxSize : 256;
    }

    public void put(String key, V value) {
        if (cache.size() >= maxSize) {
            // Éviction simple : supprime le premier élément
            String firstKey = cache.keySet().iterator().next();
            cache.remove(firstKey);
        }
        cache.put(key, value);
    }

    public V get(String key) {
        return cache.get(key);
    }

    public boolean has(String key) {
        return cache.containsKey(key);
    }

    public void remove(String key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }

    public int getMaxSize() {
        return maxSize;
    }
}
