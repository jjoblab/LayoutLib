package jo.layoutlib.inflater.bridge.android;

import android.content.Context;
import android.content.res.Resources;

import java.util.HashMap;
import java.util.Map;

/**
 * Contexte Android personnalisé pour le mini-layoutlib, inspiré de
 * com.android.layoutlib.bridge.android.BridgeContext de l AOSP.
 *
 * <p>Wraps un Context Android standard et ajoute :</p>
 * <ul>
 *   <li>Un cache de resources résolues</li>
 *   <li>Un accès au ResourceResolver configuré</li>
 *   <li>Des méthodes utilitaires pour le rendu</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class BridgeContext {

    private final Context delegate;
    private final Map<String, Object> cache = new HashMap<>();
    private Object resourceResolver;
    private Object themeResolver;
    private boolean nightMode = false;

    public BridgeContext(Context delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("Context delegate ne peut pas être null");
        }
        this.delegate = delegate;
    }

    public Context getDelegate() {
        return delegate;
    }

    public Resources getResources() {
        return delegate.getResources();
    }

    public String getPackageName() {
        return delegate.getPackageName();
    }

    public Object getResourceResolver() {
        return resourceResolver;
    }

    public void setResourceResolver(Object resolver) {
        this.resourceResolver = resolver;
    }

    public Object getThemeResolver() {
        return themeResolver;
    }

    public void setThemeResolver(Object themeResolver) {
        this.themeResolver = themeResolver;
    }

    public boolean isNightMode() {
        return nightMode;
    }

    public void setNightMode(boolean nightMode) {
        this.nightMode = nightMode;
    }

    /**
     * Met en cache une valeur.
     *
     * @param key la clé
     * @param value la valeur
     */
    public void putCached(String key, Object value) {
        cache.put(key, value);
    }

    /**
     * Récupère une valeur en cache.
     *
     * @param key la clé
     * @return la valeur, ou null
     */
    public Object getCached(String key) {
        return cache.get(key);
    }

    /**
     * Vide le cache.
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * @return le nombre d entrées en cache
     */
    public int getCacheSize() {
        return cache.size();
    }
}
