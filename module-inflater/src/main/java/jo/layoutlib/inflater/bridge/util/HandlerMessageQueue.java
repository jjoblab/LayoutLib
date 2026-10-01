package jo.layoutlib.inflater.bridge.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Queue de messages du Handler.
 *
 * <p>Inspiré de com.android.layoutlib.bridge.util.HandlerMessageQueue de l'AOSP.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class HandlerMessageQueue<K, V> {

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
