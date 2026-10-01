package jo.layoutlib.resources.api;

import java.util.Objects;

/**
 * Namespace de resource Android, inspiré de
 * {@code com.android.ide.common.rendering.api.ResourceNamespace} de l'AOSP.
 *
 * <p>Représente l'espace de noms d'une resource. Les trois namespaces
 * principaux sont :</p>
 *
 * <ul>
 *   <li>{@link #ANDROID} : resources du framework ({@code android:})</li>
 *   <li>{@link #RES_AUTO} : resources du projet et des libraries
 *       ({@code app:})</li>
 *   <li>{@link #TOOLS} : resources d'éditeur ({@code tools:})</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ResourceNamespace implements Comparable<ResourceNamespace> {

    /** Namespace Android (framework). */
    public static final ResourceNamespace ANDROID =
            new ResourceNamespace("http://schemas.android.com/apk/res/android", "android");

    /** Namespace app (projet + libraries). */
    public static final ResourceNamespace RES_AUTO =
            new ResourceNamespace("http://schemas.android.com/apk/res-auto", "app");

    /** Namespace tools (éditeur). */
    public static final ResourceNamespace TOOLS =
            new ResourceNamespace("http://schemas.android.com/tools", "tools");

    /** URI du namespace. */
    private final String uri;

    /** Préfixe court du namespace. */
    private final String prefix;

    /**
     * Construit un namespace personnalisé.
     *
     * @param uri    l'URI complète
     * @param prefix le préfixe court
     */
    public ResourceNamespace(String uri, String prefix) {
        if (uri == null) {
            throw new IllegalArgumentException("URI ne peut pas être null");
        }
        this.uri = uri;
        this.prefix = prefix != null ? prefix : "";
    }

    /**
     * Crée un namespace depuis un nom de package (ex. {@code "androidx.appcompat"}).
     *
     * @param packageName le nom du package
     * @return le namespace correspondant
     */
    public static ResourceNamespace fromPackageName(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return RES_AUTO;
        }
        return new ResourceNamespace("urn:android-appcompat:" + packageName, packageName);
    }

    /**
     * Résout un préfixe en namespace en utilisant un resolver.
     *
     * @param prefix        le préfixe à résoudre
     * @param defaultNs     le namespace par défaut si non trouvé
     * @param resolver      le resolver de prefixes
     * @return le namespace résolu, ou {@code defaultNs}
     */
    public static ResourceNamespace fromNamespacePrefix(
            String prefix, ResourceNamespace defaultNs, Resolver resolver) {
        if (prefix == null || prefix.isEmpty()) {
            return defaultNs;
        }
        if ("android".equals(prefix)) {
            return ANDROID;
        }
        if ("app".equals(prefix)) {
            return RES_AUTO;
        }
        if ("tools".equals(prefix)) {
            return TOOLS;
        }
        if (resolver != null) {
            String uri = resolver.prefixToUri(prefix);
            if (uri != null) {
                return new ResourceNamespace(uri, prefix);
            }
        }
        return defaultNs;
    }

    /**
     * @return l'URI du namespace
     */
    public String getUri() {
        return uri;
    }

    /**
     * @return le préfixe court
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @return {@code true} si c'est le namespace Android (framework)
     */
    public boolean isFramework() {
        return this == ANDROID || ANDROID.uri.equals(this.uri);
    }

    /**
     * @return {@code true} si c'est le namespace tools
     */
    public boolean isTools() {
        return this == TOOLS || TOOLS.uri.equals(this.uri);
    }

    @Override
    public int compareTo(ResourceNamespace other) {
        return this.uri.compareTo(other.uri);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceNamespace that = (ResourceNamespace) o;
        return Objects.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }

    @Override
    public String toString() {
        return prefix + " (" + uri + ")";
    }

    /**
     * Resolver de prefixes de namespace.
     *
     * @author jo@Dev
     */
    public interface Resolver {
        /**
         * Convertit un préfixe en URI.
         *
         * @param prefix le préfixe
         * @return l'URI, ou {@code null} si non trouvé
         */
        String prefixToUri(String prefix);

        /**
         * Convertit une URI en préfixe.
         *
         * @param uri l'URI
         * @return le préfixe, ou {@code null} si non trouvé
         */
        default String uriToPrefix(String uri) {
            return null;
        }

        /** Resolver vide qui ne résout rien. */
        Resolver EMPTY_RESOLVER = new Resolver() {
            @Override
            public String prefixToUri(String prefix) {
                return null;
            }
        };
    }
}
