package jo.layoutlib.resources;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Résolveur d ids de resources, inspiré de l AOSP.
 * Gère les ids @+id/foo et @id/foo.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class IdResolver {

    public static final int ID_START = 0x7f0a0001;
    public static final int ID_END = 0x7f0affff;

    private final Map<String, Integer> nameToId = new HashMap<>();
    private final Map<Integer, String> idToName = new HashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(ID_START);

    /**
     * Récupère ou crée l id pour un nom.
     *
     * @param name le nom de l id
     * @return l id entier
     */
    public int resolveId(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name ne peut pas être vide");
        }
        Integer existing = nameToId.get(name);
        if (existing != null) {
            return existing;
        }
        int id = nextId.getAndIncrement();
        if (id > ID_END) {
            throw new IllegalStateException("Plus d id disponible");
        }
        nameToId.put(name, id);
        idToName.put(id, name);
        return id;
    }

    /**
     * Récupère le nom d un id.
     *
     * @param id l id
     * @return le nom, ou null
     */
    public String resolveName(int id) {
        return idToName.get(id);
    }

    public boolean hasId(String name) {
        return nameToId.containsKey(name);
    }

    public int size() {
        return nameToId.size();
    }

    public void clear() {
        nameToId.clear();
        idToName.clear();
        nextId.set(ID_START);
    }
}
