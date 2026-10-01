package jo.layoutlib.inflater.bridge;

/**
 * Constantes du Bridge, inspirées de
 * {@code com.android.layoutlib.bridge.BridgeConstants} de l'AOSP.
 *
 * <p>Cette classe centralise toutes les constantes utilisées par le
 * mini-layoutlib : noms de namespaces, valeurs spéciales, etc.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public final class BridgeConstants {

    /** Namespace Android standard. */
    public static final String NS_ANDROID = "http://schemas.android.com/apk/res/android";

    /** Namespace app (auto-résolu). */
    public static final String NS_APP = "http://schemas.android.com/apk/res-auto";

    /** Namespace tools (éditeur). */
    public static final String NS_TOOLS = "http://schemas.android.com/tools";

    /** Préfixe des références de resources. */
    public static final String REFERENCE_PREFIX = "@";

    /** Préfixe des nouveaux ids. */
    public static final String NEW_ID_PREFIX = "@+";

    /** Préfixe des attributs de thème. */
    public static final String THEME_ATTR_PREFIX = "?";

    /** Préfixe des attributs du framework. */
    public static final String ANDROID_ATTR_PREFIX = "?android:";

    /** Préfixe des attributs custom. */
    public static final String ATTR_PREFIX = "?attr/";

    private BridgeConstants() {
    }
}
