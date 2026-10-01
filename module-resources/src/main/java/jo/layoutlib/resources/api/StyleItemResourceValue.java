package jo.layoutlib.resources.api;

/**
 * Représente un <item> dans un <style>.
 *
 * @author jo@Dev
 * @since 1.0
 */
public interface StyleItemResourceValue extends ResourceValue {

    /**
     * @return le nom du style parent
     */
    String getStyleName();
}
