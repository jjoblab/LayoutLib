package jo.layoutlib.resources;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cache LRU (Least Recently Used) pour les résolutions de resources.
 *
 * <p>Ce cache est utilisé par {@link ResourceResolverImpl} pour éviter de
 * re-résoudre les mêmes références ({@code @color/foo}, {@code @string/bar},
 * etc.) à chaque appel. La politique LRU garantit que la taille reste
 * bornée : quand la capacité est atteinte, l'entrée la plus anciennement
 * utilisée est évincée.</p>
 *
 * <p>Implémentation basée sur {@link LinkedHashMap} avec
 * {@code accessOrder=true}, ce qui déplace automatiquement l'entrée accédée
 * en fin de map. La eviction se fait en surchargeant
 * {@link #removeEldestEntry(Map.Entry)}.</p>
 *
 * <h2>Thread-safety</h2>
 * <p>Cette classe n'est <strong>pas</strong> thread-safe. Si plusieurs
 * threads accèdent au cache simultanément, une synchronisation externe est
 * nécessaire. Pour un usage dans le mini-layoutlib (mono-thread par inflater),
 * ce n'est généralement pas un problème.</p>
 *
 * @param <K> type des clés
 * @param <V> type des valeurs
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ResourceCache<K, V> extends LinkedHashMap<K, V> {

    /** Capacité maximale par défaut (512 entrées). */
    public static final int DEFAULT_CAPACITY = 512;

    /** Capacité maximale du cache. */
    private final int maxCapacity;

    /**
     * Construit un cache avec la capacité par défaut (512 entrées).
     */
    public ResourceCache() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Construit un cache avec la capacité spécifiée.
     *
     * @param maxCapacity capacité maximale (doit être > 0)
     * @throws IllegalArgumentException si {@code maxCapacity <= 0}
     */
    public ResourceCache(int maxCapacity) {
        super(16, 0.75f, true);
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException(
                    "La capacité doit être > 0, reçu : " + maxCapacity);
        }
        this.maxCapacity = maxCapacity;
    }

    /**
     * Détermine si l'entrée la plus ancienne doit être supprimée après un put.
     *
     * @param eldest l'entrée la plus ancienne
     * @return {@code true} si la taille dépasse la capacité maximale
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxCapacity;
    }

    /**
     * @return la capacité maximale du cache
     */
    public int getMaxCapacity() {
        return maxCapacity;
    }

    /**
     * Récupère une valeur sans déclencher l'éviction LRU.
     *
     * <p>Contrairement à {@link #get(Object)} qui marque l'entrée comme
     * récemment utilisée, cette méthode ne modifie pas l'ordre du cache.
     * Utile pour vérifier la présence d'une entrée sans impacter la politique
     * d'éviction.</p>
     *
     * <p>Implémentation : on parcourt manuellement les entries pour éviter
     * le déclenchement de {@code afterNodeAccess} par {@link LinkedHashMap#get}.</p>
     *
     * @param key la clé
     * @return la valeur, ou {@code null} si absente
     */
    public V peek(K key) {
        for (Map.Entry<K, V> entry : entrySet()) {
            if (key == null ? entry.getKey() == null : key.equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
