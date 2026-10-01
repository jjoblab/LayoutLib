package jo.layoutlib.resources.api;

/**
 * Interface LayoutlibCallback, inspiré de l AOSP.
 * Permet au layoutlib de communiquer avec le client.
 *
 * @author jo@Dev
 * @since 1.0
 */
public interface LayoutlibCallback {

    /**
     * Charge une classe par nom.
     *
     * @param name le nom de la classe
     * @return la classe chargée
     */
    Class<?> findClass(String name) throws ClassNotFoundException;

    /**
     * Récupère un resource id par nom.
     *
     * @param type le type (color, string, etc.)
     * @param name le nom
     * @return l'id, ou 0
     */
    int getOrGenerateResourceId(String type, String name);
}
