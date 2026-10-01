package jo.layoutlib.resources;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository de resources système (cache en mémoire).
 *
 * @author jo@Dev
 * @since 1.0
 */
public class SystemResourceRepository {

    private final Map<String, Integer> systemIds = new HashMap<>();

    public SystemResourceRepository() {
        // Pré-remplir avec quelques ids système connus
        systemIds.put("android:color/black", 0x01080000);
        systemIds.put("android:color/white", 0x01080001);
        systemIds.put("android:color/holo_red_dark", 0x01080002);
        systemIds.put("android:color/holo_blue_dark", 0x01080003);
        systemIds.put("android:color/holo_green_dark", 0x01080004);
    }

    public Integer getSystemId(String name) {
        return systemIds.get(name);
    }

    public void addSystemId(String name, int id) {
        systemIds.put(name, id);
    }

    public int size() {
        return systemIds.size();
    }
}
