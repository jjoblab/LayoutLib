package jo.layoutlib.inflater;

import android.view.View;
import android.view.ViewTreeObserver;

/**
 * Accès sûr au {@link ViewTreeObserver} des vues rendues (ADR 0116 —
 * P6/L11).
 *
 * <p>L'audit ADR 0116 notait l'absence totale de gestion
 * {@code LayoutTransition}/{@code ViewTreeObserver} : aucun chemin du
 * pipeline ne doit toucher {@code view.getViewTreeObserver()} de façon non
 * protégée. En JVM (mockable android.jar), {@code getViewTreeObserver()}
 * retourne null et {@code isAlive()} retourne false — un accès naïf lève une
 * {@link NullPointerException} ou rend un observer inutilisable.</p>
 *
 * <p>Cet utilitaire garantit un accès neutre :</p>
 * <ul>
 *   <li>{@code null} en entrée → {@code null} ;</li>
 *   <li>observer absent (JVM mockable) → {@code null} ;</li>
 *   <li>observer mort ({@code isAlive() == false}) → {@code null} ;</li>
 *   <li>toute exception du framework → {@code null} ;</li>
 *   <li>jamais d'exception propagée à l'appelant.</li>
 * </ul>
 *
 * <p>En preview statique, aucun callback runtime n'est distribué
 * ({@code dispatchOnGlobalLayout()} est une API cachée du framework) : les
 * hôtes peuvent utiliser l'observer vivant retourné pour leurs propres
 * besoins (appareil), et reposent sur {@code null} comme signal
 * « observer indisponible » (JVM/preview).</p>
 *
 * @author jo@Dev
 * @since 3.1
 */
public final class ViewTreeObservers {

    private ViewTreeObservers() {
        // Utilitaire statique — non instanciable
    }

    /**
     * Retourne le ViewTreeObserver vivant d'une vue, ou {@code null}.
     *
     * @param view la vue (peut être {@code null})
     * @return l'observer vivant, ou {@code null} si indisponible/inconnu —
     *         jamais d'exception
     */
    public static ViewTreeObserver of(View view) {
        if (view == null) {
            return null;
        }
        try {
            ViewTreeObserver observer = view.getViewTreeObserver();
            if (observer == null || !observer.isAlive()) {
                return null;
            }
            return observer;
        } catch (RuntimeException e) {
            // Framework indisponible (JVM mockable selon la cible) —
            // signal « observer indisponible », philosophie placeholder
            return null;
        }
    }
}
