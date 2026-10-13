package jo.layoutlib.inflater;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests de la capture des attributs de contrainte ConstraintLayout par
 * {@link BridgeInflater} (ADR 0116 — P2/L9).
 *
 * <p>La capture doit se produire pendant que le parser XML est encore sur
 * le {@code START_TAG} de la vue — l'ancien code la faisait après
 * {@code parseChildren} (attributs inaccessibles : capture toujours vide,
 * bug latent corrigé par ce travail).</p>
 *
 * @author jo@Dev
 * @since 3.0
 */
@DisplayName("BridgeInflater — capture des contraintes ConstraintLayout")
class BridgeInflaterConstraintCaptureTest {

    private static final String NS_ANDROID =
            "xmlns:android=\"http://schemas.android.com/apk/res/android\"";
    private static final String NS_APP =
            "xmlns:app=\"http://schemas.android.com/apk/res-auto\"";

    private Context context;
    private BridgeInflater inflater;

    @BeforeEach
    void setUp() {
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.density = 2f;
        metrics.scaledDensity = 2f;
        metrics.xdpi = 320f;
        Resources resources = mock(Resources.class);
        when(resources.getDisplayMetrics()).thenReturn(metrics);
        context = mock(Context.class);
        when(context.getResources()).thenReturn(resources);
        inflater = new BridgeInflater(context, new ViewTagRegistry(false));
    }

    @Test
    @DisplayName("capture les ancres app:layout_constraint* des enfants")
    void capturesConstraintAnchors() {
        View root = inflater.inflate(
                "<FrameLayout " + NS_ANDROID + " " + NS_APP
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\">\n"
                        + "  <View android:id=\"@+id/vue_a\"\n"
                        + "        android:layout_width=\"100dp\"\n"
                        + "        android:layout_height=\"50dp\"\n"
                        + "        app:layout_constraintStart_toStartOf=\"parent\"\n"
                        + "        app:layout_constraintTop_toTopOf=\"parent\"/>\n"
                        + "  <View android:id=\"@+id/vue_b\"\n"
                        + "        android:layout_width=\"100dp\"\n"
                        + "        android:layout_height=\"50dp\"\n"
                        + "        app:layout_constraintStart_toEndOf=\"@id/vue_a\"\n"
                        + "        app:layout_constraintEnd_toEndOf=\"parent\"\n"
                        + "        app:layout_constraintHorizontal_chainStyle=\"packed\"/>\n"
                        + "</FrameLayout>");

        assertThat(inflater.hasConstraintAttributes()).isTrue();
        Map<View, Map<String, String>> attrs = inflater.getConstraintAttributes();
        assertThat(attrs).hasSize(2);

        Map<View, String> ids = inflater.getConstraintViewIds();
        assertThat(ids).hasSize(2);

        // Les maps portent les ids nus et les attributs complets
        assertThat(ids.values()).contains("vue_a", "vue_b");
        View vueB = attrs.keySet().stream()
                .filter(v -> "vue_b".equals(ids.get(v)))
                .findFirst().orElse(null);
        assertThat(vueB).isNotNull();
        Map<String, String> bAttrs = attrs.get(vueB);
        assertThat(bAttrs.get("layout_constraintStart_toEndOf"))
                .isEqualTo("@id/vue_a");
        assertThat(bAttrs.get("layout_constraintEnd_toEndOf"))
                .isEqualTo("parent");
        assertThat(bAttrs.get("layout_constraintHorizontal_chainStyle"))
                .isEqualTo("packed");
        // Dimensions déclarées capturées aussi (match_constraint)
        assertThat(bAttrs.get("layout_width")).isEqualTo("100dp");
        assertThat(bAttrs.get("layout_height")).isEqualTo("50dp");
    }

    @Test
    @DisplayName("capture une guideline (begin + orientation)")
    void capturesGuideline() {
        inflater.inflate(
                "<FrameLayout " + NS_ANDROID + " " + NS_APP
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\">\n"
                        + "  <View android:id=\"@+id/guide\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:orientation=\"vertical\"\n"
                        + "        app:layout_constraintGuide_begin=\"100dp\"/>\n"
                        + "</FrameLayout>");

        Map<View, Map<String, String>> attrs = inflater.getConstraintAttributes();
        assertThat(attrs).hasSize(1);
        Map<String, String> guide = attrs.values().iterator().next();
        assertThat(guide.get("layout_constraintGuide_begin")).isEqualTo("100dp");
        assertThat(guide.get("orientation")).isEqualTo("vertical");
        assertThat(inflater.getConstraintViewIds().values()).contains("guide");
    }

    @Test
    @DisplayName("capture une barrière (direction + références)")
    void capturesBarrier() {
        inflater.inflate(
                "<FrameLayout " + NS_ANDROID + " " + NS_APP
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\">\n"
                        + "  <View android:id=\"@+id/barriere\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        app:barrierDirection=\"right\"\n"
                        + "        app:constraint_referenced_ids=\"a,b\"/>\n"
                        + "</FrameLayout>");

        Map<View, Map<String, String>> attrs = inflater.getConstraintAttributes();
        assertThat(attrs).hasSize(1);
        Map<String, String> barrier = attrs.values().iterator().next();
        assertThat(barrier.get("barrierDirection")).isEqualTo("right");
        assertThat(barrier.get("constraint_referenced_ids")).isEqualTo("a,b");
    }

    @Nested
    @DisplayName("Cas négatifs")
    class NegativeCases {

        @Test
        @DisplayName("un layout sans contraintes ne capture rien")
        void noCaptureWithoutConstraints() {
            inflater.inflate(
                    "<FrameLayout " + NS_ANDROID + " " + NS_APP
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\">\n"
                            + "  <View android:layout_width=\"100dp\"\n"
                            + "        android:layout_height=\"50dp\"/>\n"
                            + "</FrameLayout>");
            assertThat(inflater.hasConstraintAttributes()).isFalse();
            assertThat(inflater.hasConstraintViews()).isFalse();
        }

        @Test
        @DisplayName("reset vide la capture")
        void resetClearsCapture() {
            inflater.inflate(
                    "<FrameLayout " + NS_ANDROID + " " + NS_APP
                            + "    android:layout_width=\"match_parent\"\n"
                            + "    android:layout_height=\"match_parent\">\n"
                            + "  <View android:layout_width=\"100dp\"\n"
                            + "        android:layout_height=\"50dp\"\n"
                            + "        app:layout_constraintTop_toTopOf=\"parent\"/>\n"
                            + "</FrameLayout>");
            assertThat(inflater.hasConstraintAttributes()).isTrue();
            inflater.reset();
            assertThat(inflater.hasConstraintAttributes()).isFalse();
            assertThat(inflater.hasConstraintViews()).isFalse();
        }
    }

    @Test
    @DisplayName("la capture survit aux vues imbriquées (contraintes au niveau 2)")
    void capturesNestedConstraintViews() {
        View root = inflater.inflate(
                "<FrameLayout " + NS_ANDROID + " " + NS_APP
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\">\n"
                        + "  <LinearLayout\n"
                        + "      android:layout_width=\"match_parent\"\n"
                        + "      android:layout_height=\"match_parent\">\n"
                        + "    <View android:id=\"+id/imbrique\"\n"
                        + "          android:layout_width=\"0dp\"\n"
                        + "          android:layout_height=\"wrap_content\"\n"
                        + "          app:layout_constraintDimensionRatio=\"16:9\"/>\n"
                        + "  </LinearLayout>\n"
                        + "</FrameLayout>");

        assertThat(root).isInstanceOf(ViewGroup.class);
        Map<View, Map<String, String>> attrs = inflater.getConstraintAttributes();
        assertThat(attrs).hasSize(1);
        Map<String, String> nested = attrs.values().iterator().next();
        assertThat(nested.get("layout_constraintDimensionRatio")).isEqualTo("16:9");
        assertThat(nested.get("layout_width")).isEqualTo("0dp");
    }
}
