package jo.layoutlib.resources.api;

import java.util.Objects;

/**
 * Référence vers une resource Android, inspiré de
 * {@code com.android.ide.common.rendering.api.ResourceReference} de l'AOSP.
 *
 * <p>Une {@code ResourceReference} identifie de manière unique une resource
 * par son namespace, son type et son nom. Par exemple, la référence
 * {@code @color/foo} dans le namespace {@code app} correspond à une
 * {@code ResourceReference} avec :</p>
 *
 * <ul>
 *   <li>namespace = {@link ResourceNamespace#RES_AUTO}</li>
 *   <li>type = {@link ResourceType#COLOR}</li>
 *   <li>name = {@code "foo"}</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ResourceReference {

    /** Namespace de la resource. */
    private final ResourceNamespace namespace;

    /** Type de la resource. */
    private final ResourceType type;

    /** Nom de la resource. */
    private final String name;

    /**
     * Construit une référence de resource.
     *
     * @param namespace le namespace
     * @param type      le type
     * @param name      le nom
     */
    public ResourceReference(ResourceNamespace namespace, ResourceType type, String name) {
        if (namespace == null) {
            throw new IllegalArgumentException("namespace ne peut pas être null");
        }
        if (type == null) {
            throw new IllegalArgumentException("type ne peut pas être null");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name ne peut pas être vide");
        }
        this.namespace = namespace;
        this.type = type;
        this.name = name;
    }

    /**
     * Crée une référence vers un attribut.
     *
     * @param namespace le namespace
     * @param name      le nom de l'attribut
     * @return la référence
     */
    public static ResourceReference attr(ResourceNamespace namespace, String name) {
        return new ResourceReference(namespace, ResourceType.ATTR, name);
    }

    /**
     * Crée une référence vers un style.
     *
     * @param namespace le namespace
     * @param name      le nom du style
     * @return la référence
     */
    public static ResourceReference style(ResourceNamespace namespace, String name) {
        return new ResourceReference(namespace, ResourceType.STYLE, name);
    }

    /**
     * Crée une référence vers un styleable.
     *
     * @param namespace le namespace
     * @param name      le nom du styleable
     * @return la référence
     */
    public static ResourceReference styleable(ResourceNamespace namespace, String name) {
        return new ResourceReference(namespace, ResourceType.STYLEABLE, name);
    }

    /**
     * @return le namespace
     */
    public ResourceNamespace getNamespace() {
        return namespace;
    }

    /**
     * @return le type
     */
    public ResourceType getResourceType() {
        return type;
    }

    /**
     * @return le nom
     */
    public String getName() {
        return name;
    }

    /**
     * @return {@code true} si c'est une resource du framework
     */
    public boolean isFramework() {
        return namespace.isFramework();
    }

    /**
     * @return l'URL de la resource (ex. {@code "@color/foo"} ou {@code "@android:color/foo"})
     */
    public ResourceUrl getResourceUrl() {
        return ResourceUrl.create(namespace, type, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceReference that = (ResourceReference) o;
        return Objects.equals(namespace, that.namespace)
                && type == that.type
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, type, name);
    }

    @Override
    public String toString() {
        if (namespace.isFramework()) {
            return "@android:" + type.getName() + "/" + name;
        }
        return "@" + type.getName() + "/" + name;
    }
}
