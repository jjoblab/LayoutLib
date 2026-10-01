package jo.layoutlib.inflater.bridge.android;

import java.util.HashMap;
import java.util.Map;

/**
 * Flags de paramètres de rendu, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class RenderParamsFlags {

    public static final String FLAG_DECOR = "flag.decor";
    public static final String FLAG_HIDE_NAV_BAR = "flag.hide_nav_bar";
    public static final String FLAG_HIDE_STATUS_BAR = "flag.hide_status_bar";
    public static final String FLAG_NO_FRAME = "flag.no_frame";
    public static final String FLAG_FORCE_NIGHT_MODE = "flag.force_night_mode";

    private final Map<String, Object> flags = new HashMap<>();

    public void setFlag(String name, Object value) {
        if (name != null) {
            flags.put(name, value);
        }
    }

    public Object getFlag(String name) {
        return flags.get(name);
    }

    public boolean getBooleanFlag(String name, boolean defaultValue) {
        Object v = flags.get(name);
        if (v instanceof Boolean) {
            return (Boolean) v;
        }
        return defaultValue;
    }

    public boolean hasFlag(String name) {
        return flags.containsKey(name);
    }

    public void removeFlag(String name) {
        flags.remove(name);
    }

    public void clear() {
        flags.clear();
    }

    public int size() {
        return flags.size();
    }
}
