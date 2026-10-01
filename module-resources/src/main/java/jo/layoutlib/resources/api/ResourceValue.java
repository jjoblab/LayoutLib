package jo.layoutlib.resources.api;

/**
 * Interface représentant une resource Android, inspiré de
 * {@code com.android.ide.common.rendering.api.ResourceValue} de l'AOSP.
 *
 * <p>Une resource est identifiée par :</p>
 * <ul>
 *   <li>un {@link ResourceNamespace}</li>
 *   <li>un {@link ResourceType}</li>
 *   <li>un nom</li>
 * </ul>
 *
 * <p>Elle a une valeur brute (string) telle que définie dans le XML, qui
 * peut être une référence vers une autre resource, une valeur littérale,
 * ou {@code null} (pour les styles).</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public interface ResourceValue {

    /**
     * @return le type de la resource
     */
    ResourceType getResourceType();

    /**
     * @return le namespace de la resource
     */
    ResourceNamespace getNamespace();

    /**
     * @return le nom de la resource
     */
    String getName();

    /**
     * @return le nom de la library où la resource a été trouvée, ou
     *         {@code null} si ce n'est pas une library
     */
    String getLibraryName();

    /**
     * @return {@code true} si la resource est définie par l'utilisateur
     */
    boolean isUserDefined();

    /**
     * @return {@code true} si c'est une resource du framework Android
     */
    default boolean isFramework() {
        return getNamespace().isFramework();
    }

    /**
     * @return la valeur brute de la resource, ou {@code null}
     */
    String getValue();

    /**
     * @return une {@link ResourceReference} vers cette resource
     */
    ResourceReference asReference();

    /**
     * @return l'URL de la resource
     */
    default ResourceUrl getResourceUrl() {
        return asReference().getResourceUrl();
    }

    /**
     * Si cette resource référence une autre resource, retourne la
     * référence vers celle-ci.
     *
     * @return la référence, ou {@code null} si la valeur n'est pas une référence
     */
    default ResourceReference getReference() {
        String value = getValue();
        if (value == null) {
            return null;
        }
        ResourceUrl url = ResourceUrl.parse(value);
        if (url == null || url.isThemeAttr()) {
            return null;
        }
        return url.toReference();
    }
}
