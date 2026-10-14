package jo.layoutlib.inflater;

import android.content.Context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.xmlpull.v1.XmlPullParser;

import android.view.View;

import jo.layoutlib.attributes.AttributeRegistry;
import jo.layoutlib.attributes.AttributeRegistryImpl;
import jo.layoutlib.resources.DimensionConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests du câblage {@code BridgeInflater → AttributeRegistry} (ADR 0116 —
 * P3/L8) : l'inflater doit invoquer {@code applyCustomAttributes} pour chaque
 * vue créée quand un registre est attaché, et rester silencieux sinon.
 *
 * <p>En JVM (mockable android.jar), l'application réelle des setters est
 * couverte par {@code AttributeRegistryImplCustomAttributesTest} et
 * {@code CustomAttributeApplierTest} (module-attributes) ; ce test valide le
 * <em>câblage</em> du pipeline.</p>
 *
 * @author jo@Dev
 * @since 3.0
 */
@DisplayName("BridgeInflater — câblage applyCustomAttributes (P3/L8)")
class BridgeInflaterCustomAttributesTest {

    private static final String NS =
            "xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                    + "        xmlns:app=\"http://schemas.android.com/apk/res-auto\"";

    private Context context;
    private ViewTagRegistry tagRegistry;

    @BeforeEach
    void setUp() {
        context = mock(Context.class);
        tagRegistry = new ViewTagRegistry(false);
    }

    @Nested
    @DisplayName("Invocation du registre")
    class RegistryInvocation {

        @Test
        @DisplayName("applyCustomAttributes est invoqué pour chaque vue créée")
        void invokesRegistryPerView() {
            AttributeRegistry registry = mock(AttributeRegistry.class);
            BridgeInflater inflater = new BridgeInflater(context, tagRegistry);
            inflater.setAttributeRegistry(registry);

            View root = inflater.inflate(
                    "<LinearLayout\n        " + NS + "\n"
                            + "        android:layout_width=\"match_parent\"\n"
                            + "        android:layout_height=\"match_parent\">\n"
                            + "  <TextView\n        " + NS + "\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        app:elevation=\"8dp\"/>\n"
                            + "  <Button\n        " + NS + "\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        app:cornerRadius=\"12dp\"/>\n"
                            + "</LinearLayout>");

            assertThat(root).isNotNull();
            verify(registry, times(3)).applyCustomAttributes(
                    any(View.class), any(XmlPullParser.class), eq(context));
        }

        @Test
        @DisplayName("le registre est exposé par getAttributeRegistry")
        void registryGetterExposesInstance() {
            AttributeRegistry registry = mock(AttributeRegistry.class);
            BridgeInflater inflater = new BridgeInflater(context, tagRegistry);
            assertThat(inflater.getAttributeRegistry()).isNull();
            inflater.setAttributeRegistry(registry);
            assertThat(inflater.getAttributeRegistry()).isSameAs(registry);
        }
    }

    @Nested
    @DisplayName("Intégration réelle")
    class RealRegistryIntegration {

        @Test
        @DisplayName("un registre réel applique les app:* sans lever")
        void realRegistryAppliesAttributes() {
            AttributeRegistryImpl registry =
                    new AttributeRegistryImpl(new DimensionConverter(2f, 2f, 320f));
            BridgeInflater inflater = new BridgeInflater(context, tagRegistry);
            inflater.setAttributeRegistry(registry);

            View root = inflater.inflate(
                    "<TextView\n        " + NS + "\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        app:elevation=\"8dp\"\n"
                            + "        app:alpha=\"0.5\"\n"
                            + "        app:attribut_factice=\"ignore\"/>");

            assertThat(root).isNotNull();
            assertThat(registry.getLastCustomAppliedCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("sans registre attaché, l'inflation est inchangée")
        void withoutRegistryInflationIsUnchanged() {
            BridgeInflater inflater = new BridgeInflater(context, tagRegistry);
            assertThatCode(() -> inflater.inflate(
                    "<TextView\n        " + NS + "\n"
                            + "        android:layout_width=\"wrap_content\"\n"
                            + "        android:layout_height=\"wrap_content\"\n"
                            + "        app:elevation=\"8dp\"/>"))
                    .doesNotThrowAnyException();
        }
    }
}
