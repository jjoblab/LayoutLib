package jo.layoutlib.attributes.registry;

import jo.layoutlib.resources.api.AttributeFormat;
import java.util.HashMap;
import java.util.Map;

import jo.layoutlib.attributes.api.AttributeDefinitionImpl;

/**
 * Registre des attributs Material Components, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class MaterialAttributeRegistry implements AttributeRegistrySource {

    private final Map<String, AttributeDefinitionImpl> attributes = new HashMap<>();

    public MaterialAttributeRegistry() {
        registerDefaults();
    }

    private void registerDefaults() {
        // Attributs Material courants
        register("cornerRadius", new AttributeFormat[]{AttributeFormat.DIMENSION});
        register("icon", new AttributeFormat[]{AttributeFormat.REFERENCE});
        register("iconSize", new AttributeFormat[]{AttributeFormat.DIMENSION});
        register("iconTint", new AttributeFormat[]{AttributeFormat.COLOR});
        register("strokeColor", new AttributeFormat[]{AttributeFormat.COLOR});
        register("strokeWidth", new AttributeFormat[]{AttributeFormat.DIMENSION});
        register("cardBackgroundColor", new AttributeFormat[]{AttributeFormat.COLOR});
        register("cardCornerRadius", new AttributeFormat[]{AttributeFormat.DIMENSION});
        register("cardElevation", new AttributeFormat[]{AttributeFormat.DIMENSION});
        register("rippleColor", new AttributeFormat[]{AttributeFormat.COLOR});
        register("elevation", new AttributeFormat[]{AttributeFormat.DIMENSION});
    }

    public void register(String name, AttributeFormat[] formats) {
        AttributeDefinitionImpl def = new AttributeDefinitionImpl(name, formats);
        def.setStyleableName("MaterialComponents");
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
