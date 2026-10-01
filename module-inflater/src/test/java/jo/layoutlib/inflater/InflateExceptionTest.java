package jo.layoutlib.inflater;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de la classe {@link InflateException}.
 *
 * @author jo@Dev
 */
@DisplayName("InflateException — contexte d'erreur")
class InflateExceptionTest {

    @Nested
    @DisplayName("Constructeurs")
    class Constructors {

        @Test
        @DisplayName("construit avec un message simple")
        void shouldConstructWithMessage() {
            InflateException e = new InflateException("erreur test");
            assertThat(e.getMessage()).isEqualTo("erreur test");
            assertThat(e.getLineNumber()).isEqualTo(-1);
            assertThat(e.getSourceFile()).isNull();
        }

        @Test
        @DisplayName("construit avec un message et une cause")
        void shouldConstructWithCause() {
            Throwable cause = new RuntimeException("cause racine");
            InflateException e = new InflateException("erreur test", cause);
            assertThat(e.getMessage()).isEqualTo("erreur test");
            assertThat(e.getCause()).isSameAs(cause);
        }

        @Test
        @DisplayName("construit avec contexte de ligne et fichier")
        void shouldConstructWithContext() {
            InflateException e = new InflateException("erreur", 42, "layout.xml");
            assertThat(e.getMessage()).contains("ligne 42");
            assertThat(e.getMessage()).contains("layout.xml");
            assertThat(e.getLineNumber()).isEqualTo(42);
            assertThat(e.getSourceFile()).isEqualTo("layout.xml");
        }
    }

    @Nested
    @DisplayName("Héritage")
    class Inheritance {

        @Test
        @DisplayName("hérite de RuntimeException")
        void shouldBeRuntimeException() {
            InflateException e = new InflateException("test");
            assertThat(e).isInstanceOf(RuntimeException.class);
        }
    }
}
