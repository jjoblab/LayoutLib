package jo.layoutlib.inflater;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;

import jo.layoutlib.resources.DimensionConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * v3.7.0 — les attributs RELATIFS de l'applier (étude PE14 § 2.4-5, S8) :
 * {@code paddingStart/paddingEnd} posés via {@code setPaddingRelative} (le
 * côté relatif absent retombe sur l'ABSOLU du même côté — miroir de la
 * lecture AOSP) et {@code android:layoutDirection} par vue
 * ({@link DirectionRendu#directionDepuisValeur}).
 *
 * <p>Patron {@code AttributeApplierTextAppearanceTest} : la vue est un
 * MOCK (android.jar mockable — setters no-op invisibles, les verify
 * attestent l'application), le parser vient de kxml2 (namespace-aware
 * VRAI).</p>
 *
 * @author jo@Dev
 * @since 3.7.0
 */
@DisplayName("AttributeApplier — attributs relatifs RTL (v3.7.0, S8)")
class AttributeApplierRtlTest {

    private Context context;
    private AttributeApplier applier;

    @BeforeEach
    void setUp() {
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.density = 2f;
        metrics.scaledDensity = 2f;
        metrics.xdpi = 320f;
        Resources resources = mock(Resources.class);
        when(resources.getDisplayMetrics()).thenReturn(metrics);
        // Repli natif neutralisé : getIdentifier rend toujours 0 (patron
        // BridgeInflaterDimenReferenceTest).
        when(resources.getIdentifier(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(0);
        context = mock(Context.class);
        when(context.getResources()).thenReturn(resources);
        applier = new AttributeApplier(context, new DimensionConverter(2f, 2f, 320f));
    }

    /** Gonfle un parser kxml2 sur le fragment (patron textAppearance). */
    private XmlPullParser parserSur(String fragment) throws Exception {
        String xml = "<root xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                + "<View " + fragment + "/></root>";
        KXmlParser parser = new KXmlParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(new StringReader(xml));
        parser.nextTag(); // <root>
        parser.nextTag(); // <View …/> — START_TAG de la vue
        return parser;
    }

    @Test
    @DisplayName("paddingStart seul — setPaddingRelative(start, 0, 0, 0)")
    void shouldApplyPaddingStartRelative() throws Exception {
        View vue = mock(View.class);
        XmlPullParser parser =
                parserSur("android:paddingStart=\"4dp\"");
        applier.applyAttributes(vue, parser);
        // Densité 2f : 4dp → 8. Le côté END absent retombe sur l'absolu (0).
        verify(vue).setPaddingRelative(8, 0, 0, 0);
    }

    @Test
    @DisplayName("paddingStart + paddingRight — l'absolu nourrit le côté relatif absent")
    void shouldFallBackToAbsoluteWhenRelativeMissing() throws Exception {
        View vue = mock(View.class);
        XmlPullParser parser =
                parserSur("android:paddingStart=\"4dp\" android:paddingRight=\"6dp\"");
        applier.applyAttributes(vue, parser);
        // Densité 2f : start 4dp → 8 ; END absent → retombe sur right 6dp → 12.
        verify(vue).setPaddingRelative(8, 0, 12, 0);
        // Le chemin ABSOLU n'est pas pris quand un relatif existe.
        verify(vue, never()).setPadding(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("paddingStart + paddingEnd — les DEUX relatifs")
    void shouldApplyBothRelativePaddings() throws Exception {
        View vue = mock(View.class);
        XmlPullParser parser =
                parserSur("android:paddingStart=\"4dp\" android:paddingEnd=\"10dp\"");
        applier.applyAttributes(vue, parser);
        verify(vue).setPaddingRelative(8, 0, 20, 0);
    }

    @Test
    @DisplayName("paddings absolus seuls — setPadding classique, jamais relatif")
    void shouldKeepAbsolutePaddingPath() throws Exception {
        View vue = mock(View.class);
        XmlPullParser parser =
                parserSur("android:paddingLeft=\"4dp\" android:paddingRight=\"6dp\"");
        applier.applyAttributes(vue, parser);
        verify(vue).setPadding(8, 0, 12, 0);
        verify(vue, never()).setPaddingRelative(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("android:layoutDirection=\"rtl\" — posé sur la vue")
    void shouldApplyLayoutDirectionAttribute() throws Exception {
        View vue = mock(View.class);
        XmlPullParser parser =
                parserSur("android:layoutDirection=\"rtl\"");
        applier.applyAttributes(vue, parser);
        verify(vue).setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    }

    @Test
    @DisplayName("android:layoutDirection inconnu — retombe sur INHERIT, jamais d'échec")
    void shouldFallBackToInheritForUnknownDirection() throws Exception {
        View vue = mock(View.class);
        XmlPullParser parser =
                parserSur("android:layoutDirection=\"bizarre\"");
        applier.applyAttributes(vue, parser);
        verify(vue).setLayoutDirection(View.LAYOUT_DIRECTION_INHERIT);
    }

    @Test
    @DisplayName("l'échec du setter layoutDirection est journalisé, jamais fatal")
    void shouldTolerateLayoutDirectionFailure() throws Exception {
        View vue = mock(View.class);
        org.mockito.Mockito.doThrow(new RuntimeException("mock : refus"))
                .when(vue).setLayoutDirection(anyInt());
        XmlPullParser parser =
                parserSur("android:layoutDirection=\"rtl\"");
        applier.applyAttributes(vue, parser);
        // Pas d'exception — le rendu continue (miroir logAttrError).
        verify(vue).setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    }

    @Test
    @DisplayName("DirectionRendu.directionDepuisValeur — cohérence de table avec l'applier")
    void shouldShareTheDirectionTable() {
        assertThat(DirectionRendu.directionDepuisValeur("locale"))
                .isEqualTo(View.LAYOUT_DIRECTION_LOCALE);
    }
}
