package jo.layoutlib.inflater.bridge.android;

import java.util.HashMap;
import java.util.Map;

/**
 * Resources de rendu dynamiques, inspiré de l AOSP.
 * Permet de surcharger les resources à la volée pendant le rendu.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class DynamicRenderResources {

    private final Map<String, Object> overrides = new HashMap<>();
    private Object delegate;

    public DynamicRenderResources() {
    }

    public DynamicRenderResources(Object delegate) {
        this.delegate = delegate;
    }

    /**
     * Ajoute une surcharge de resource.
     *
     * @param key la clé (ex. "color:primary")
     * @param value la valeur de surcharge
     */
    public void putOverride(String key, Object value) {
        if (key != null) {
            overrides.put(key, value);
        }
    }

    /**
     * Récupère une resource, en priorisant les surcharges.
     *
     * @param key la clé
     * @return la valeur, ou null
     */
    public Object get(String key) {
        if (overrides.containsKey(key)) {
            return overrides.get(key);
        }
        return null;
    }

    /**
     * Indique si une surcharge existe.
     *
     * @param key la clé
     * @return true si surchargée
     */
    public boolean hasOverride(String key) {
        return overrides.containsKey(key);
    }

    /**
     * Supprime une surcharge.
     *
     * @param key la clé
     */
    public void removeOverride(String key) {
        overrides.remove(key);
    }

    /**
     * Vide toutes les surcharges.
     */
    public void clearOverrides() {
        overrides.clear();
    }

    public int getOverrideCount() {
        return overrides.size();
    }

    public Object getDelegate() {
        return delegate;
    }

    public void setDelegate(Object delegate) {
        this.delegate = delegate;
    }
}
