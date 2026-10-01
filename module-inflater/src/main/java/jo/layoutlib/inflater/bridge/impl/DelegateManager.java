package jo.layoutlib.inflater.bridge.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gestionnaire des delegates (pattern AOSP pour stubber les classes natives),
 * inspiré de {@code com.android.layoutlib.bridge.impl.DelegateManager} de l'AOSP.
 *
 * <p>Le pattern delegate de l'AOSP permet de remplacer les méthodes natives
 * d'une classe Android par des implémentations JVM. Chaque delegate est
 * identifié par un entier unique (le "delegate manager" maintains une map
 * JavaObject ↔ Delegate).</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * DelegateManager<View_Delegate> manager = new DelegateManager<>();
 * View_Delegate delegate = new View_Delegate();
 * int id = manager.addNewDelegate(delegate);
 * View_Delegate retrieved = manager.getDelegate(id);
 * }</pre>
 *
 * @param <D> le type de delegate géré
 *
 * @author jo@Dev
 * @since 1.0
 */
public class DelegateManager<D> {

    /** Map des delegates par id. */
    private final Map<Integer, D> delegates = new HashMap<>();

    /** Compteur d'ids uniques. */
    private final AtomicInteger nextId = new AtomicInteger(1);

    /**
     * Ajoute un nouveau delegate.
     *
     * @param delegate le delegate à ajouter
     * @return l'id unique attribué
     */
    public int addNewDelegate(D delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate ne peut pas être null");
        }
        int id = nextId.getAndIncrement();
        delegates.put(id, delegate);
        return id;
    }

    /**
     * Récupère un delegate par son id.
     *
     * @param id l'id du delegate
     * @return le delegate, ou {@code null} si introuvable
     */
    public D getDelegate(int id) {
        return delegates.get(id);
    }

    /**
     * Supprime un delegate.
     *
     * @param id l'id du delegate à supprimer
     * @return le delegate supprimé, ou {@code null}
     */
    public D removeDelegate(int id) {
        return delegates.remove(id);
    }

    /**
     * @return le nombre de delegates gérés
     */
    public int size() {
        return delegates.size();
    }

    /**
     * Vide tous les delegates.
     */
    public void clear() {
        delegates.clear();
        nextId.set(1);
    }

    /**
     * Vérifie qu'un delegate existe.
     *
     * @param id l'id à vérifier
     * @return {@code true} si le delegate existe
     */
    public boolean hasDelegate(int id) {
        return delegates.containsKey(id);
    }
}
