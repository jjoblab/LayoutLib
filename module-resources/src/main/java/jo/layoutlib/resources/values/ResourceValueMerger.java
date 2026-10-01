package jo.layoutlib.resources.values;

import java.util.Map;

import jo.layoutlib.resources.api.ResourceValue;

/**
 * Fusionne plusieurs maps de ResourceValue.
 * Les resources les plus spécifiques (dernières ajoutées) priment.
 *
 * @author jo@Dev
 * @since 1.0
 */
public final class ResourceValueMerger {

    private ResourceValueMerger() {
    }

    /**
     * Fusionne source dans target. Les valeurs de source écrasent target.
     *
     * @param target la map cible (modifiable)
     * @param source la map source
     */
    public static void merge(ResourceValueMap target, ResourceValueMap source) {
        if (target == null || source == null) {
            return;
        }
        for (ResourceValue v : source.values()) {
            target.put(v);
        }
    }

    /**
     * Crée une nouvelle map fusionnant source1 et source2.
     *
     * @param source1 première source
     * @param source2 seconde source (prioritaire)
     * @return la map fusionnée
     */
    public static ResourceValueMap mergeAll(ResourceValueMap source1, ResourceValueMap source2) {
        MutableResourceValueMap result = new MutableResourceValueMap();
        merge(result, source1);
        merge(result, source2);
        return result;
    }
}
