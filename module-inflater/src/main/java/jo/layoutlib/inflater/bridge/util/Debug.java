package jo.layoutlib.inflater.bridge.util;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilitaires de debug, inspiré de
 * {@code com.android.layoutlib.bridge.util.Debug} de l'AOSP.
 *
 * <p>Cette classe centralise les fonctions de logging et de debug
 * utilisées par le mini-layoutlib. Elle permet d'activer/désactiver
 * sélectivement les logs par catégorie.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public final class Debug {

    /** Logger principal. */
    private static final Logger LOGGER = Logger.getLogger("MiniLayoutLib");

    /** Map des catégories de debug actives. */
    private static final Map<String, Boolean> debugFlags = new HashMap<>();

    /** Indique si le debug global est activé. */
    private static boolean globalDebug = false;

    /** Constructeur privé. */
    private Debug() {
    }

    /**
     * Active le debug global.
     */
    public static void enable() {
        globalDebug = true;
    }

    /**
     * Désactive le debug global.
     */
    public static void disable() {
        globalDebug = false;
        debugFlags.clear();
    }

    /**
     * Active le debug pour une catégorie spécifique.
     *
     * @param category la catégorie (ex. {@code "inflater"}, {@code "resources"})
     */
    public static void enableCategory(String category) {
        if (category != null) {
            debugFlags.put(category, true);
        }
    }

    /**
     * Indique si le debug est actif pour une catégorie.
     *
     * @param category la catégorie
     * @return {@code true} si le debug est actif
     */
    public static boolean isDebugEnabled(String category) {
        if (globalDebug) {
            return true;
        }
        return category != null && Boolean.TRUE.equals(debugFlags.get(category));
    }

    /**
     * Log un message d'info.
     *
     * @param category la catégorie
     * @param message  le message
     */
    public static void log(String category, String message) {
        if (isDebugEnabled(category)) {
            LOGGER.info("[" + category + "] " + message);
        }
    }

    /**
     * Log un message d'avertissement.
     *
     * @param category la catégorie
     * @param message  le message
     */
    public static void logWarning(String category, String message) {
        LOGGER.warning("[" + category + "] " + message);
    }

    /**
     * Log un message d'erreur.
     *
     * @param category la catégorie
     * @param message  le message
     * @param throwable l'exception associée, ou {@code null}
     */
    public static void logError(String category, String message, Throwable throwable) {
        if (throwable != null) {
            LOGGER.log(Level.SEVERE, "[" + category + "] " + message, throwable);
        } else {
            LOGGER.severe("[" + category + "] " + message);
        }
    }

    /**
     * Mesure le temps d'exécution d'une tâche.
     *
     * @param category la catégorie
     * @param label    le label de la tâche
     * @param task     la tâche à exécuter
     */
    public static void time(String category, String label, Runnable task) {
        if (task == null) {
            return;
        }
        long start = System.nanoTime();
        task.run();
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        log(category, label + " : " + elapsed + " ms");
    }

    /**
     * @return le logger principal
     */
    public static Logger getLogger() {
        return LOGGER;
    }
}
