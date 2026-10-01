package jo.layoutlib.inflater.bridge.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Map dynamique d'ids générés, inspiré de
 * {@code com.android.layoutlib.bridge.util.DynamicIdMap} de l'AOSP.
 *
 * <p>Cette classe maintient une correspondance entre des clés (généralement
 * des noms de resources) et des ids entiers uniques. Elle est utilisée
 * par le BridgeInflater pour attribuer des ids aux vues créées via
 * {@code @+id/foo}.</p>
 *
 * <h2>Algorithme</h2>
 * <ul>
 *   <li>Le premier id généré est {@code 0x7f0a0001}</li>
 *   <li>Chaque nouvel id incrémente le compteur</li>
 *   <li>Les ids sont mis en cache pour retourner le même id pour la même clé</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class DynamicIdMap {

    /** Id de départ pour les ids générés. */
    public static final int ID_START = 0x7f0a0001;

    /** Map clé → id. */
    private final Map<String, Integer> keyToId = new HashMap<>();

    /** Map id → clé (reverse lookup). */
    private final Map<Integer, String> idToKey = new HashMap<>();

    /** Compteur d'ids. */
    private final AtomicInteger nextId = new AtomicInteger(ID_START);

    /**
     * Construit une map vide.
     */
    public DynamicIdMap() {
    }

    /**
     * Récupère ou crée l'id pour une clé.
     *
     * <p>Si la clé existe déjà, retourne l'id existant. Sinon, génère un
     * nouvel id unique et l'associe à la clé.</p>
     *
     * @param key la clé (ex. {@code "button_save"})
     * @return l'id entier unique
     */
    public int getId(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key ne peut pas être vide");
        }
        Integer existing = keyToId.get(key);
        if (existing != null) {
            return existing;
        }
        int id = nextId.getAndIncrement();
        keyToId.put(key, id);
        idToKey.put(id, key);
        return id;
    }

    /**
     * Récupère la clé associée à un id.
     *
     * @param id l'id à rechercher
     * @return la clé, ou {@code null} si non trouvée
     */
    public String getKey(int id) {
        return idToKey.get(id);
    }

    /**
     * Indique si une clé a déjà un id.
     *
     * @param key la clé à vérifier
     * @return {@code true} si la clé a un id
     */
    public boolean hasId(String key) {
        return keyToId.containsKey(key);
    }

    /**
     * @return le nombre d'ids générés
     */
    public int size() {
        return keyToId.size();
    }

    /**
     * Vide la map et réinitialise le compteur.
     */
    public void clear() {
        keyToId.clear();
        idToKey.clear();
        nextId.set(ID_START);
    }

    /**
     * @return une copie de la map clé → id
     */
    public Map<String, Integer> getKeyToIdMap() {
        return new HashMap<>(keyToId);
    }

    /**
     * @return le prochain id qui serait généré (sans le consommer)
     */
    public int peekNextId() {
        return nextId.get();
    }
}
