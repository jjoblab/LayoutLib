package jo.layoutlib.attributes.registry;

import java.util.ArrayList;
import java.util.List;

import jo.layoutlib.attributes.api.AttributeDefinitionImpl;

/**
 * Registre composite combinant plusieurs registres.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class CompositeAttributeRegistry {

    private final List<AttributeRegistrySource> registries = new ArrayList<>();

    public void addRegistry(AttributeRegistrySource registry) {
        if (registry != null) {
            registries.add(registry);
        }
    }

    public AttributeDefinitionImpl getAttribute(String name) {
        for (AttributeRegistrySource registry : registries) {
            AttributeDefinitionImpl attr = registry.getAttribute(name);
            if (attr != null) {
                return attr;
            }
        }
        return null;
    }

    public boolean hasAttribute(String name) {
        for (AttributeRegistrySource registry : registries) {
            if (registry.hasAttribute(name)) {
                return true;
            }
        }
        return false;
    }

    public int getRegistryCount() {
        return registries.size();
    }

    public int getTotalAttributeCount() {
        int total = 0;
        for (AttributeRegistrySource registry : registries) {
            total += registry.size();
        }
        return total;
    }
}
