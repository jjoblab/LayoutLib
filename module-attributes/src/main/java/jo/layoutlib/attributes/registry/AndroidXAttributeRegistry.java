package jo.layoutlib.attributes.registry;

import jo.layoutlib.resources.api.AttributeFormat;
import java.util.HashMap;
import java.util.Map;

import jo.layoutlib.attributes.api.AttributeDefinitionImpl;

/**
 * Registre des attributs AndroidX, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class AndroidXAttributeRegistry implements AttributeRegistrySource {

    private final Map<String, AttributeDefinitionImpl> attributes = new HashMap<>();

    public AndroidXAttributeRegistry() {
        registerDefaults();
    }

    private void registerDefaults() {
        // Attributs AndroidX courants
        register("cardBackgroundColor", new AttributeFormat[]{AttributeFormat.COLOR});
        register("cardCornerRadius", new AttributeFormat[]{AttributeFormat.DIMENSION});
        register("cardElevation", new AttributeFormat[]{AttributeFormat.DIMENSION});
        register("cardMaxElevation", new AttributeFormat[]{AttributeFormat.DIMENSION});
        register("cardPreventCornerOverlap", new AttributeFormat[]{AttributeFormat.BOOLEAN});
        register("cardUseCompatPadding", new AttributeFormat[]{AttributeFormat.BOOLEAN});
        register("contentPadding", new AttributeFormat[]{AttributeFormat.DIMENSION});
        register("contentPaddingLeft", new AttributeFormat[]{AttributeFormat.DIMENSION});
        register("contentPaddingRight", new AttributeFormat[]{AttributeFormat.DIMENSION});
        register("contentPaddingTop", new AttributeFormat[]{AttributeFormat.DIMENSION});
        register("contentPaddingBottom", new AttributeFormat[]{AttributeFormat.DIMENSION});
    }

    public void register(String name, AttributeFormat[] formats) {
        AttributeDefinitionImpl def = new AttributeDefinitionImpl(name, formats);
        def.setStyleableName("androidx");
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
