package jo.layoutlib.inflater.bridge.android;

import java.util.HashMap;
import java.util.Map;

/**
 * PowerManager simulé, inspiré de com.android.layoutlib.bridge.android.BridgePowerManager de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class BridgePowerManager {

    private final Map<String, Object> data = new HashMap<>();

    public BridgePowerManager() {
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public Object get(String key) {
        return data.get(key);
    }

    public boolean contains(String key) {
        return data.containsKey(key);
    }

    public int size() {
        return data.size();
    }

    public void clear() {
        data.clear();
    }
}
