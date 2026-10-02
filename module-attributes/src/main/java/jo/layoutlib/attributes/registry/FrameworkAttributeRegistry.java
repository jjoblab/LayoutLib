package jo.layoutlib.attributes.registry;

import jo.layoutlib.resources.api.AttributeFormat;
import java.util.HashMap;
import java.util.Map;

import jo.layoutlib.attributes.api.AttributeDefinitionImpl;

/**
 * Registre des attributs du framework Android, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class FrameworkAttributeRegistry implements AttributeRegistrySource {

    private final Map<String, AttributeDefinitionImpl> attributes = new HashMap<>();

    public FrameworkAttributeRegistry() {
        registerDefaults();
    }

    private void registerDefaults() {
        // Attributs framework courants
        register("text", new AttributeFormat[]{AttributeFormat.STRING});
        register("textColor", new AttributeFormat[]{AttributeFormat.COLOR});
        register("textSize", new AttributeFormat[]{AttributeFormat.DIMENSION});
        register("background", new AttributeFormat[]{AttributeFormat.REFERENCE, AttributeFormat.COLOR});
        register("padding", new AttributeFormat[]{AttributeFormat.DIMENSION});
        register("paddingLeft", new AttributeFormat[]{AttributeFormat.DIMENSION});
        register("paddingRight", new AttributeFormat[]{AttributeFormat.DIMENSION});
        register("paddingTop", new AttributeFormat[]{AttributeFormat.DIMENSION});
        register("paddingBottom", new AttributeFormat[]{AttributeFormat.DIMENSION});
        register("layout_width", new AttributeFormat[]{AttributeFormat.DIMENSION, AttributeFormat.ENUM});
        register("layout_height", new AttributeFormat[]{AttributeFormat.DIMENSION, AttributeFormat.ENUM});
        register("layout_margin", new AttributeFormat[]{AttributeFormat.DIMENSION});
        register("id", new AttributeFormat[]{AttributeFormat.REFERENCE});
        register("visibility", new AttributeFormat[]{AttributeFormat.ENUM});
        register("enabled", new AttributeFormat[]{AttributeFormat.BOOLEAN});
        register("clickable", new AttributeFormat[]{AttributeFormat.BOOLEAN});
        register("focusable", new AttributeFormat[]{AttributeFormat.BOOLEAN});
    }

    public void register(String name, AttributeFormat[] formats) {
        AttributeDefinitionImpl def = new AttributeDefinitionImpl(name, formats);
        def.setStyleableName("android");
        attributes.put(name, def);
    }

    public AttributeDefinitionImpl getAttribute(String name) {
        return attributes.get(name);
    }

    public boolean hasAttribute(String name) {
        return attributes.containsKey(name);
    }

    public int size() {
        return attributes.size();
    }
}
