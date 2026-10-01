package jo.layoutlib.inflater.bridge.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Callbacks du Choreographer (animations).
 *
 * <p>Inspiré de com.android.layoutlib.bridge.util.ChoreographerCallbacks de l'AOSP.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ChoreographerCallbacks<K, V> {

    /** Stockage interne. */
    private final Map<K, V> backing = new HashMap<>();

    /**
     * Ajoute une entrée.
     *
     * @param key   la clé
     * @param value la valeur
     */
    public void put(K key, V value) {
        backing.put(key, value);
    }

    /**
     * Récupère une valeur.
     *
     * @param key la clé
     * @return la valeur, ou null
     */
    public V get(K key) {
        return backing.get(key);
    }

    /**
     * @return la taille
     */
    public int size() {
        return backing.size();
    }
}
