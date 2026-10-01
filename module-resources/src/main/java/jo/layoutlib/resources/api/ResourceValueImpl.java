package jo.layoutlib.resources.api;

/**
 * Implémentation par défaut d'un {@link ResourceValue}, inspiré de
 * {@code com.android.ide.common.rendering.api.ResourceValueImpl} de l'AOSP.
 *
 * <p>Un {@code ResourceValue} représente une resource Android complète :
 * sa référence (namespace + type + nom) et sa valeur brute (telle que
 * définie dans le XML).</p>
 *
 * <h2>Exemple</h2>
 * <pre>{@code
 * ResourceValue color = new ResourceValueImpl(
 *     ResourceNamespace.RES_AUTO,
 *     ResourceType.COLOR,
 *     "primary",
 *     "#FF6750A4");
 * String value = color.getValue();  // "#FF6750A4"
 * }</pre>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ResourceValueImpl implements ResourceValue {

    /** Namespace de la resource. */
    private final ResourceNamespace namespace;

    /** Type de la resource. */
    private final ResourceType type;

    /** Nom de la resource. */
    private final String name;

    /** Nom de la library source, ou {@code null}. */
    private final String libraryName;

    /** Valeur brute de la resource. */
    private String value;

    /** Indique si la resource est définie par l'utilisateur. */
    private final boolean userDefined;

    /**
     * Construit une resource avec une valeur.
     *
     * @param namespace   le namespace
     * @param type        le type
     * @param name        le nom
     * @param value       la valeur brute
     */
    public ResourceValueImpl(ResourceNamespace namespace, ResourceType type,
                             String name, String value) {
        this(namespace, type, name, value, null, true);
    }

    /**
     * Construit une resource complète.
     *
     * @param namespace    le namespace
     * @param type         le type
     * @param name         le nom
     * @param value        la valeur brute
     * @param libraryName  le nom de la library, ou {@code null}
     * @param userDefined  {@code true} si définie par l'utilisateur
     */
    public ResourceValueImpl(ResourceNamespace namespace, ResourceType type,
                             String name, String value,
                             String libraryName, boolean userDefined) {
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
        this.value = value;
        this.libraryName = libraryName;
        this.userDefined = userDefined;
    }

    @Override
    public ResourceType getResourceType() {
        return type;
    }

    @Override
    public ResourceNamespace getNamespace() {
        return namespace;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getLibraryName() {
        return libraryName;
    }

    @Override
    public boolean isUserDefined() {
        return userDefined;
    }

    @Override
    public String getValue() {
        return value;
    }

    /**
     * Définit la valeur brute de la resource.
     *
     * @param value la nouvelle valeur
     */
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public ResourceReference asReference() {
        return new ResourceReference(namespace, type, name);
    }

    @Override
    public String toString() {
        return "ResourceValue{" + namespace.getPrefix() + ":" + type.getName()
                + "/" + name + " = " + value + "}";
    }
}
