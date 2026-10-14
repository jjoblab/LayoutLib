package jo.layoutlib.attributes;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jo.layoutlib.attributes.registry.CustomAttributeApplier;
import jo.layoutlib.resources.DimensionConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests du {@link CustomAttributeApplier} (ADR 0116 — P3/L8).
 *
 * <p>En JVM (mockable android.jar), les setters des vues sont des no-op :
 * le comportement est vérifié par Mockito sur des mocks de vue. Le repli
 * réflexion (vues custom) est vérifié sur une vue de test instrumentée
 * manuellement.</p>
 *
 * @author jo@Dev
 * @since 3.0
 */
@DisplayName("CustomAttributeApplier — attributs app: typés (P3/L8)")
class CustomAttributeApplierTest {

    private CustomAttributeApplier applier;
    private View view;

    /**
     * Vue de test avec des setters « custom » (simule une vue tierce
     * DataBinding/library : app:heroText -> setHeroText).
     */
    static class RecordingView extends View {
        String heroText;
        int heroCount;
        float heroRatio;
        boolean heroEnabled;

        RecordingView() {
            super((Context) null);
        }

        public void setHeroText(String value) {
            this.heroText = value;
        }

        public void setHeroCount(int value) {
            this.heroCount = value;
        }

        public void setHeroRatio(float value) {
            this.heroRatio = value;
        }

        public void setHeroEnabled(boolean value) {
            this.heroEnabled = value;
        }
    }

    @BeforeEach
    void setUp() {
        // densité 2x : 8dp -> 16px
        applier = new CustomAttributeApplier(new DimensionConverter(2f, 2f, 320f));
        view = mock(View.class);
    }

    @Nested
    @DisplayName("Attributs génériques")
    class GenericAttributes {

        @Test
        @DisplayName("elevation convertit la dimension en pixels")
        void elevationConvertsDimension() {
            boolean applied = applier.apply(view, "elevation", "8dp", null);
            assertThat(applied).isTrue();
            verify(view).setElevation(16f);
        }

        @Test
        @DisplayName("alpha parse le flottant")
        void alphaParsesFloat() {
            assertThat(applier.apply(view, "alpha", "0.5", null)).isTrue();
            verify(view).setAlpha(0.5f);
        }

        @Test
        @DisplayName("backgroundTint applique une teinte depuis une couleur hex")
        void backgroundTintAppliesColor() {
            assertThat(applier.apply(view, "backgroundTint", "#FF5722", null)).isTrue();
            // JVM mockable : ColorStateList.valueOf renvoie null (statique
            // stub) — le setter est quand même invoqué avec la valeur résolue
            verify(view).setBackgroundTintList(isNull());
        }

        @Test
        @DisplayName("visibility distingue gone/invisible/visible")
        void visibilityMapsValues() {
            applier.apply(view, "visibility", "gone", null);
            verify(view).setVisibility(View.GONE);
            applier.apply(view, "visibility", "invisible", null);
            verify(view).setVisibility(View.INVISIBLE);
        }

        @Test
        @DisplayName("boolean et tag passent par les setters natifs")
        void booleanAndTagUseSetters() {
            assertThat(applier.apply(view, "enabled", "true", null)).isTrue();
            verify(view).setEnabled(true);
            assertThat(applier.apply(view, "tag", "marqueur", null)).isTrue();
            verify(view).setTag("marqueur");
        }

        @Test
        @DisplayName("une dimension invalide est ignorée sans exception")
        void invalidDimensionIsIgnored() {
            assertThatCode(() ->
                    applier.apply(view, "elevation", "pas-une-dim", null))
                    .doesNotThrowAnyException();
            verify(view).setElevation(0f);
        }
    }

    @Nested
    @DisplayName("Familles de widgets")
    class WidgetFamilies {

        @Test
        @DisplayName("cardCornerRadius sur FrameLayout sans GradientDrawable : reconnu, sans exception")
        void cardCornerRadiusWithoutGradientBackground() {
            FrameLayout frame = mock(FrameLayout.class);
            // getBackground() renvoie null sur un mock -> branche skipping
            assertThatCode(() ->
                    applier.apply(frame, "cardCornerRadius", "8dp", null))
                    .doesNotThrowAnyException();
            assertThat(applier.apply(frame, "cardCornerRadius", "8dp", null))
                    .isTrue();
        }

        @Test
        @DisplayName("cardElevation applique l'élévation")
        void cardElevationSetsElevation() {
            assertThat(applier.apply(view, "cardElevation", "4dp", null)).isTrue();
            verify(view).setElevation(8f);
        }

        @Test
        @DisplayName("contentPadding applique les quatre côtés")
        void contentPaddingSetsAllSides() {
            assertThat(applier.apply(view, "contentPadding", "8dp", null)).isTrue();
            verify(view).setPadding(16, 16, 16, 16);
        }

        @Test
        @DisplayName("attribut Material sans setter natif : reconnu, aucune interaction vue")
        void recognizedNoOpAttribute() {
            assertThat(applier.apply(view, "strokeColor", "#000", null)).isTrue();
            assertThat(applier.apply(view, "layout_behavior",
                    "@string/app_bar_scrolling_view_behavior", null)).isTrue();
            assertThat(applier.apply(view, "fabSize", "normal", null)).isTrue();
            verifyNoMoreInteractions(view);
        }
    }

    @Nested
    @DisplayName("Repli réflexion (vues custom)")
    class ReflectionFallback {

        @Test
        @DisplayName("setHeroText(String) est trouvé et invoqué")
        void stringSetterIsFound() {
            RecordingView recording = new RecordingView();
            assertThat(applier.apply(recording, "heroText", "Bonjour", null))
                    .isTrue();
            assertThat(recording.heroText).isEqualTo("Bonjour");
        }

        @Test
        @DisplayName("setHeroCount(int) convertit l'entier")
        void intSetterIsFound() {
            RecordingView recording = new RecordingView();
            assertThat(applier.apply(recording, "heroCount", "42", null))
                    .isTrue();
            assertThat(recording.heroCount).isEqualTo(42);
        }

        @Test
        @DisplayName("setHeroRatio(float) convertit le flottant")
        void floatSetterIsFound() {
            RecordingView recording = new RecordingView();
            assertThat(applier.apply(recording, "heroRatio", "0.75", null))
                    .isTrue();
            assertThat(recording.heroRatio).isEqualTo(0.75f);
        }

        @Test
        @DisplayName("setHeroEnabled(boolean) convertit le booléen")
        void booleanSetterIsFound() {
            RecordingView recording = new RecordingView();
            assertThat(applier.apply(recording, "heroEnabled", "true", null))
                    .isTrue();
            assertThat(recording.heroEnabled).isTrue();
        }

        @Test
        @DisplayName("un attribut sans setter renvoie false (inconnu)")
        void noSetterReturnsFalse() {
            RecordingView recording = new RecordingView();
            assertThat(applier.apply(recording, "attributSansSetter", "x", null))
                    .isFalse();
        }

        @Test
        @DisplayName("un setter de type non supporté n'est pas invoqué")
        void unsupportedSetterTypeIsSkipped() {
            RecordingView recording = new RecordingView();
            // Aucun setHeroText(Parcelable) etc. -> attribut inconnu
            assertThat(applier.apply(recording, "heroInconnu", "x", null))
                    .isFalse();
            assertThat(recording.heroText).isNull();
        }
    }

    @Nested
    @DisplayName("Garde-fous")
    class GuardClauses {

        @Test
        @DisplayName("arguments null ou vides : false sans exception")
        void nullArgumentsReturnFalse() {
            assertThat(applier.apply(null, "elevation", "8dp", null)).isFalse();
            assertThat(applier.apply(view, null, "8dp", null)).isFalse();
            assertThat(applier.apply(view, "elevation", null, null)).isFalse();
            assertThat(applier.apply(view, "elevation", "", null)).isFalse();
        }

        @Test
        @DisplayName("le cache réflexion renvoie le même résultat au 2e appel")
        void reflectionCacheIsStable() {
            RecordingView recording = new RecordingView();
            assertThat(applier.apply(recording, "heroText", "un", null)).isTrue();
            assertThat(applier.apply(recording, "heroText", "deux", null)).isTrue();
            assertThat(recording.heroText).isEqualTo("deux");
            assertThat(applier.apply(recording, "absent", "x", null)).isFalse();
            assertThat(applier.apply(recording, "absent", "x", null)).isFalse();
        }
    }
}
