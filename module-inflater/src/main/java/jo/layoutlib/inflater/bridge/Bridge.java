package jo.layoutlib.inflater.bridge;

/**
 * Point d'entrée principal du Bridge, inspiré de
 * {@code com.android.layoutlib.bridge.Bridge} de l'AOSP.
 *
 * <p>Cette classe est un singleton qui maintient l'état global du
 * mini-layoutlib : cache des resources, configuration, etc. Elle est
 * utilisée en complément de {@link jo.layoutlib.inflater.MiniLayoutLib}
 * pour les usages avancés.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public final class Bridge {

    /** Instance unique (singleton). */
    private static Bridge instance;

    /** Indique si le Bridge a été initialisé. */
    private boolean initialized = false;

    /** Version du Bridge. */
    public static final String VERSION = "1.0.0";

    /**
     * Constructeur privé (singleton).
     */
    private Bridge() {
    }

    /**
     * @return l'instance unique du Bridge
     */
    public static synchronized Bridge getInstance() {
        if (instance == null) {
            instance = new Bridge();
        }
        return instance;
    }

    /**
     * Initialise le Bridge.
     *
     * @return true si l'initialisation a réussi
     */
    public boolean init() {
        if (initialized) {
            return true;
        }
        initialized = true;
        return true;
    }

    /**
     * Libère les ressources du Bridge.
     */
    public void cleanup() {
        initialized = false;
    }

    /**
     * @return true si le Bridge est initialisé
     */
    public boolean isInitialized() {
        return initialized;
    }
}
