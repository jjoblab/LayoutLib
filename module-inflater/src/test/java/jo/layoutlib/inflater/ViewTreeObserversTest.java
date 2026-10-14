package jo.layoutlib.inflater;

import android.content.Context;
import android.view.View;
import android.view.ViewTreeObserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests de l'accès sûr au {@code ViewTreeObserver} via
 * {@link ViewTreeObservers} (ADR 0116 — P6/L11).
 *
 * <p>En JVM (mockable android.jar), {@code getViewTreeObserver()} retourne
 * null et {@code isAlive()} retourne false : l'utilitaire doit retourner
 * null dans tous ces cas — jamais d'exception.</p>
 *
 * @author jo@Dev
 * @since 3.1
 */
@DisplayName("ViewTreeObservers — accès sûr au ViewTreeObserver (P6/L11)")
class ViewTreeObserversTest {

    @Nested
    @DisplayName("Cas null / indisponible")
    class NullAndUnavailable {

        @Test
        @DisplayName("vue null → observer null")
        void nullViewReturnsNull() {
            assertThat(ViewTreeObservers.of(null)).isNull();
        }

        @Test
        @DisplayName("vue sans observer (JVM mockable) → null")
        void viewWithoutObserverReturnsNull() {
            // Vue réelle du mockable jar : getViewTreeObserver() → null
            View view = new android.view.View(
                    mock(Context.class));
            assertThat(ViewTreeObservers.of(view)).isNull();
        }

        @Test
        @DisplayName("observer null → null")
        void nullObserverReturnsNull() {
            View view = mock(View.class);
            when(view.getViewTreeObserver()).thenReturn(null);
            assertThat(ViewTreeObservers.of(view)).isNull();
        }

        @Test
        @DisplayName("observer mort (isAlive false) → null")
        void deadObserverReturnsNull() {
            View view = mock(View.class);
            ViewTreeObserver observer = mock(ViewTreeObserver.class);
            when(view.getViewTreeObserver()).thenReturn(observer);
            when(observer.isAlive()).thenReturn(false);
            assertThat(ViewTreeObservers.of(view)).isNull();
        }

        @Test
        @DisplayName("une exception du framework est neutralisée → null")
        void frameworkExceptionIsNeutralised() {
            View view = mock(View.class);
            when(view.getViewTreeObserver())
                    .thenThrow(new IllegalStateException("boom"));
            assertThat(ViewTreeObservers.of(view)).isNull();
        }
    }

    @Nested
    @DisplayName("Observer vivant")
    class AliveObserver {

        @Test
        @DisplayName("observer vivant → retourné tel quel")
        void aliveObserverIsReturned() {
            View view = mock(View.class);
            ViewTreeObserver observer = mock(ViewTreeObserver.class);
            when(view.getViewTreeObserver()).thenReturn(observer);
            when(observer.isAlive()).thenReturn(true);
            assertThat(ViewTreeObservers.of(view)).isSameAs(observer);
        }
    }
}
