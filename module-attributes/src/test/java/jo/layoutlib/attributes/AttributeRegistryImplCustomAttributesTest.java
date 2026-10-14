package jo.layoutlib.attributes;

import android.content.Context;
import android.view.View;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;

import jo.layoutlib.resources.DimensionConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests de {@link AttributeRegistryImpl#applyCustomAttributes} (ADR 0116 —
 * P3/L8) : énumération des attributs {@code app:} du parser, application via
 * le {@code CustomAttributeApplier}, exclusion des {@code app:layout_constraint*}
 * (P2/L9) et des namespaces non {@code app:}.
 *
 * @author jo@Dev
 * @since 3.0
 */
@DisplayName("AttributeRegistryImpl.applyCustomAttributes — app:* (P3/L8)")
class AttributeRegistryImplCustomAttributesTest {

    private static final String NS_DECL =
            "xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                    + "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"";

    private AttributeRegistryImpl registry;
    private View view;

    @BeforeEach
    void setUp() {
        // densité 2x : 8dp -> 16px
        registry = new AttributeRegistryImpl(new DimensionConverter(2f, 2f, 320f));
        view = mock(View.class);
    }

    /** Construit un parser positionné sur le premier START_TAG du XML. */
    private XmlPullParser parserOn(String xml) throws Exception {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(xml));
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                return parser;
            }
            event = parser.next();
        }
        throw new IllegalStateException("aucun START_TAG dans le XML");
    }

    @Nested
    @DisplayName("Application des attributs app:")
    class Application {

        @Test
        @DisplayName("applique elevation et alpha avec conversion typée")
        void appliesTypedAttributes() throws Exception {
            XmlPullParser parser = parserOn(
                    "<LinearLayout\n    " + NS_DECL + "\n"
                            + "    app:elevation=\"8dp\"\n"
                            + "    app:alpha=\"0.5\"/>");
            registry.applyCustomAttributes(view, parser, null);
            verify(view).setElevation(16f);
            verify(view).setAlpha(0.5f);
            assertThat(registry.getLastCustomAppliedCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("ignore les attributs android: et sans namespace")
        void ignoresNonAppAttributes() throws Exception {
            XmlPullParser parser = parserOn(
                    "<LinearLayout\n    " + NS_DECL + "\n"
                            + "    android:layout_width=\"wrap_content\"\n"
                            + "    android:visibility=\"gone\"\n"
                            + "    elevation=\"8dp\"/>");
            registry.applyCustomAttributes(view, parser, null);
            // Aucun setter ne doit passer par le registre custom
            verifyNoInteractions(view);
            assertThat(registry.getLastCustomAppliedCount()).isZero();
        }

        @Test
        @DisplayName("exclut les app:layout_constraint* (solver Cassowary P2)")
        void excludesConstraintAttributes() throws Exception {
            XmlPullParser parser = parserOn(
                    "<Button\n    " + NS_DECL + "\n"
                            + "    app:layout_constraintWidth_default=\"spread\"\n"
                            + "    app:layout_constraintLeft_toLeftOf=\"parent\"\n"
                            + "    app:elevation=\"4dp\"/>");
            registry.applyCustomAttributes(view, parser, null);
            verify(view).setElevation(8f);
            // Seul elevation est compté : les contraintes sont au solver
            assertThat(registry.getLastCustomAppliedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("un attribut app: inconnu est ignoré sans exception")
        void unknownAppAttributeIsIgnored() throws Exception {
            XmlPullParser parser = parserOn(
                    "<LinearLayout\n    " + NS_DECL + "\n"
                            + "    app:attribut_factice=\"x\"/>");
            assertThatCode(() ->
                    registry.applyCustomAttributes(view, parser, null))
                    .doesNotThrowAnyException();
            assertThat(registry.getLastCustomAppliedCount()).isZero();
        }

        @Test
        @DisplayName("un attribut reconnu sans setter natif est compté appliqué")
        void recognizedNoOpIsCounted() throws Exception {
            XmlPullParser parser = parserOn(
                    "<FrameLayout\n    " + NS_DECL + "\n"
                            + "    app:layout_behavior=\"@string/bhv\"\n"
                            + "    app:cardUseCompatPadding=\"true\"/>");
            registry.applyCustomAttributes(view, parser, null);
            assertThat(registry.getLastCustomAppliedCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Garde-fous")
    class GuardClauses {

        @Test
        @DisplayName("vue ou parser null : aucune exception, compteur à zéro")
        void nullArgumentsAreSafe() throws Exception {
            XmlPullParser parser = parserOn(
                    "<View " + NS_DECL + " app:elevation=\"8dp\"/>");
            assertThatCode(() ->
                    registry.applyCustomAttributes(null, parser, null))
                    .doesNotThrowAnyException();
            assertThatCode(() ->
                    registry.applyCustomAttributes(view, null, null))
                    .doesNotThrowAnyException();
            assertThat(registry.getLastCustomAppliedCount()).isZero();
        }

        @Test
        @DisplayName("parser sans attribut app: : compteur à zéro")
        void noAppAttributes() throws Exception {
            XmlPullParser parser = parserOn(
                    "<LinearLayout\n    " + NS_DECL + "\n"
                            + "    android:layout_width=\"match_parent\"/>");
            registry.applyCustomAttributes(view, parser, null);
            assertThat(registry.getLastCustomAppliedCount()).isZero();
        }

        @Test
        @DisplayName("le contexte null est accepté (JVM)")
        void nullContextIsAccepted() throws Exception {
            XmlPullParser parser = parserOn(
                    "<View " + NS_DECL + " app:tag=\"marqueur\"/>");
            Context context = null;
            assertThatCode(() ->
                    registry.applyCustomAttributes(view, parser, context))
                    .doesNotThrowAnyException();
            assertThat(registry.getLastCustomAppliedCount()).isEqualTo(1);
        }
    }
}
