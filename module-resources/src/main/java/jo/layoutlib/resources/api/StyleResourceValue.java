package jo.layoutlib.resources.api;

/**
 * Représente un <style> avec ses items.
 *
 * @author jo@Dev
 * @since 1.0
 */
public interface StyleResourceValue extends ResourceValue {

    /**
     * @return le nom du style parent
     */
    String getParentStyleName();

    /**
     * Récupère un item par nom.
     *
     * @param name le nom de l'attribut
     * @return la valeur, ou null
     */
    String getItem(String name);

    /**
     * @return le nombre d'items
     */
    int getItemCount();
}
