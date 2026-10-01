package jo.layoutlib.resources.api;

import java.util.Objects;

/**
 * URL d'une resource Android, inspiré de
 * {@code com.android.resources.ResourceUrl} de l'AOSP.
 *
 * <p>Représente une référence vers une resource sous la forme
 * {@code @type/name} ou {@code @+id/name} (création d'id) ou
 * {@code ?attr/name} (attribut de thème).</p>
 *
 * <h2>Exemples</h2>
 * <ul>
 *   <li>{@code @color/foo} → référence vers la couleur {@code foo}</li>
 *   <li>{@code @android:color/holo_red} → référence framework</li>
 *   <li>{@code @+id/button} → création d'id</li>
 *   <li>{@code ?attr/colorPrimary} → attribut de thème</li>
 *   <li>{@code ?android:attr/windowBackground} → attribut framework</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ResourceUrl {

    /** Type de référence. */
    public enum Type {
        /** Référence normale (@type/name). */
        NORMAL,
        /** Création d'id (@+id/name). */
        CREATE,
        /** Attribut de thème (?attr/name). */
        THEME_ATTR
    }

    /** Namespace de la resource. */
    private final ResourceNamespace namespace;

    /** Type de la resource. */
    private final ResourceType type;

    /** Nom de la resource. */
    private final String name;

    /** Type de référence. */
    private final Type refType;

    /**
     * Construit une URL de resource.
     *
     * @param namespace le namespace
     * @param type      le type
     * @param name      le nom
     * @param refType   le type de référence
     */
    private ResourceUrl(ResourceNamespace namespace, ResourceType type,
                        String name, Type refType) {
        this.namespace = namespace;
        this.type = type;
        this.name = name;
        this.refType = refType;
    }

    /**
     * Crée une URL normale.
     *
     * @param namespace le namespace
     * @param type      le type
     * @param name      le nom
     * @return l'URL
     */
    public static ResourceUrl create(ResourceNamespace namespace,
                                     ResourceType type, String name) {
        return new ResourceUrl(namespace, type, name, Type.NORMAL);
    }

    /**
     * Parse une chaîne de référence en ResourceUrl.
     *
     * <p>Formats acceptés :</p>
     * <ul>
     *   <li>{@code @type/name}</li>
     *   <li>{@code @+id/name} (création)</li>
     *   <li>{@code @android:type/name}</li>
     *   <li>{@code ?attr/name}</li>
     *   <li>{@code ?android:attr/name}</li>
     *   <li>{@code ?name} (attribut court)</li>
     * </ul>
     *
     * @param value la chaîne à parser
     * @return l'URL parsée, ou {@code null} si invalide
     */
    public static ResourceUrl parse(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        value = value.trim();

        // Cas ?attr/name ou ?android:attr/name ou ?name
        if (value.startsWith("?")) {
            String rest = value.substring(1);
            ResourceNamespace ns = ResourceNamespace.RES_AUTO;
            if (rest.startsWith("android:")) {
                ns = ResourceNamespace.ANDROID;
                rest = rest.substring("android:".length());
            }
            if (rest.startsWith("attr/")) {
                rest = rest.substring("attr/".length());
            }
            if (rest.isEmpty()) {
                return null;
            }
            return new ResourceUrl(ns, ResourceType.ATTR, rest, Type.THEME_ATTR);
        }

        // Cas @type/name ou @+id/name ou @android:type/name
        if (!value.startsWith("@")) {
            return null;
        }
        String rest = value.substring(1);
        Type type = Type.NORMAL;
        if (rest.startsWith("+")) {
            type = Type.CREATE;
            rest = rest.substring(1);
        }

        ResourceNamespace ns = ResourceNamespace.RES_AUTO;
        if (rest.startsWith("android:")) {
            ns = ResourceNamespace.ANDROID;
            rest = rest.substring("android:".length());
        }

        int slash = rest.indexOf('/');
        if (slash < 0) {
            return null;
        }
        String typeName = rest.substring(0, slash);
        String resName = rest.substring(slash + 1);
        if (resName.isEmpty()) {
            return null;
        }
        ResourceType resType = ResourceType.fromName(typeName);
        if (resType == null) {
            return null;
        }
        return new ResourceUrl(ns, resType, resName, type);
    }

    /**
     * @return le namespace
     */
    public ResourceNamespace getNamespace() {
        return namespace;
    }

    /**
     * @return le type de resource
     */
    public ResourceType getType() {
        return type;
    }

    /**
     * @return le nom
     */
    public String getName() {
        return name;
    }

    /**
     * @return le type de référence
     */
    public Type getRefType() {
        return refType;
    }

    /**
     * @return {@code true} si c'est une création d'id
     */
    public boolean isCreate() {
        return refType == Type.CREATE;
    }

    /**
     * @return {@code true} si c'est un attribut de thème
     */
    public boolean isThemeAttr() {
        return refType == Type.THEME_ATTR;
    }

    /**
     * @return {@code true} si c'est une resource du framework
     */
    public boolean isFramework() {
        return namespace.isFramework();
    }

    /**
     * Convertit en {@link ResourceReference}.
     *
     * @return la référence équivalente
     */
    public ResourceReference toReference() {
        return new ResourceReference(namespace, type, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceUrl that = (ResourceUrl) o;
        return Objects.equals(namespace, that.namespace)
                && type == that.type
                && Objects.equals(name, that.name)
                && refType == that.refType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, type, name, refType);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (refType == Type.THEME_ATTR) {
            sb.append('?');
            if (namespace.isFramework()) {
                sb.append("android:");
            }
            sb.append("attr/").append(name);
        } else {
            sb.append('@');
            if (refType == Type.CREATE) {
                sb.append('+');
            }
            if (namespace.isFramework()) {
                sb.append("android:");
            }
            sb.append(type.getName()).append('/').append(name);
        }
        return sb.toString();
    }
}
