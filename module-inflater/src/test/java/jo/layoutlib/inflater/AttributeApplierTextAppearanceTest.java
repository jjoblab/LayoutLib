package jo.layoutlib.inflater;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.widget.TextView;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;

import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.themes.ThemeResolverImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * v3.6.0 — {@code android:textAppearance} honoré (étude PE14 § 2.4-1, S7
 * du plan) : l'attribut était IGNORÉ en silence avant v3.6.0. Le test
 * couvre le chemin PROJET (style absent des resources hôte →
 * {@link ThemeResolverImpl#getStyleAttribute}) et les formes
 * {@code ?attr/} (la valeur du thème EST le nom du style).
 *
 * <p>Le TextView est un MOCK (android.jar mockable : les setters sont
 * no-op invisibles) — les verify attestent l'application. Le parser
 * vient de kxml2 (patron {@code BridgeInflaterDimenReferenceTest} :
 * les lectures d'attributs doivent passer par un VRAI parser
 * namespace-aware).</p>
 *
 * @author jo@Dev
 * @since 3.6.0
 */
@DisplayName("AttributeApplier — android:textAppearance (v3.6.0, S7)")
class AttributeApplierTextAppearanceTest {

    private Context context;
    private AttributeApplier applier;
    private ThemeResolverImpl themeResolver;

    @BeforeEach
    void setUp() {
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.density = 2f;
        metrics.scaledDensity = 2f;
        metrics.xdpi = 320f;
        Resources resources = mock(Resources.class);
        when(resources.getDisplayMetrics()).thenReturn(metrics);
        when(resources.getIdentifier(anyString(), anyString(), anyString()))
                .thenReturn(0);
        context = mock(Context.class);
        when(context.getResources()).thenReturn(resources);

        themeResolver = new ThemeResolverImpl();
        themeResolver.registerThemesFile(
                "<resources>"
                        + "<style name=\"TextAppearance.MonTitre\">"
                        + "<item name=\"textSize\">20sp</item>"
                        + "<item name=\"textColor\">#FF112233</item>"
                        + "<item name=\"textStyle\">bold</item>"
                        + "<item name=\"fontFamily\">sans-serif-medium</item>"
                        + "<item name=\"textAllCaps\">true</item>"
                        + "</style>"
                        + "<style name=\"Theme.MonApp\">"
                        + "<item name=\"textAppearancePetit\">"
                        + "@style/TextAppearance.MonTitre</item>"
                        + "</style>"
                        + "</resources>");
        themeResolver.setTheme("Theme.MonApp");

        applier = new AttributeApplier(context,
                new DimensionConverter(2f, 2f, 320f));
        applier.setThemeResolver(themeResolver);
    }

    /** Un VRAI parser positionné sur le START_TAG donné. */
    private static XmlPullParser parserSur(String tag) throws Exception {
        XmlPullParser parser = new KXmlParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(new StringReader(tag));
        parser.nextTag();
        return parser;
    }

    @Nested
    @DisplayName("Chemin projet — styles.xml du projet via ThemeResolver")
    class CheminProjet {

        @Test
        @DisplayName("@style/ → textSize, textColor, style, famille, casse")
        void styleProjetApplique() throws Exception {
            TextView tv = mock(TextView.class);
            when(tv.getTypeface()).thenReturn(null);

            applier.appliquerTextAppearance(tv, "@style/TextAppearance.MonTitre");

            verify(tv).setTextSize(anyInt(), any(Float.class));
            verify(tv).setTextColor(any(Integer.class));
            // Typeface.create est un stub android.jar (rend null) — la
            // FAMILLE est passée avec les drapeaux du style (bold).
            verify(tv).setTypeface(
                    org.mockito.ArgumentMatchers.isNull(android.graphics.Typeface.class));
            verify(tv).setAllCaps(true);
        }

        @Test
        @DisplayName("?attr/textAppearancePetit → résolu contre le thème")
        void attrThemeResolu() throws Exception {
            TextView tv = mock(TextView.class);
            when(tv.getTypeface()).thenReturn(null);

            applier.appliquerTextAppearance(tv, "?attr/textAppearancePetit");

            verify(tv).setTextSize(anyInt(), any(Float.class));
            verify(tv).setAllCaps(true);
        }

        @Test
        @DisplayName("référence introuvable — JAMAIS d'exception (rendu survit)")
        void referenceIntrouvableSilencieuse() {
            TextView tv = mock(TextView.class);
            assertThatCode(() ->
                    applier.appliquerTextAppearance(tv, "@style/NEXISTE.PAS"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> applier.appliquerTextAppearance(tv, null))
                    .doesNotThrowAnyException();
            assertThatCode(() -> applier.appliquerTextAppearance(tv, "?attr/absent"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Chemin document — l'attribut du tag traverse le dispatch")
    class CheminDocument {

        @Test
        @DisplayName("applyAttributes honore android:textAppearance d'un tag")
        void tagTextAppearance() throws Exception {
            TextView tv = mock(TextView.class);
            when(tv.getTypeface()).thenReturn(null);

            applier.applyAttributes(tv, parserSur(
                    "<TextView\n"
                    + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                    + "    android:layout_width=\"wrap_content\"\n"
                    + "    android:layout_height=\"wrap_content\"\n"
                    + "    android:textAppearance=\"@style/TextAppearance.MonTitre\" />"));

            verify(tv).setTextSize(anyInt(), any(Float.class));
            verify(tv).setAllCaps(true);
        }

        @Test
        @DisplayName("textColor du tag L'EMPORTE sur l'apparence (ordre)")
        void tagGagneSurApparence() throws Exception {
            TextView tv = mock(TextView.class);
            when(tv.getTypeface()).thenReturn(null);

            applier.applyAttributes(tv, parserSur(
                    "<TextView\n"
                    + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                    + "    android:layout_width=\"wrap_content\"\n"
                    + "    android:layout_height=\"wrap_content\"\n"
                    + "    android:textColor=\"#FFFF0000\"\n"
                    + "    android:textAppearance=\"@style/TextAppearance.MonTitre\" />"));

            org.mockito.InOrder ordre = org.mockito.Mockito.inOrder(tv);
            // L'apparence pose SA couleur (#FF112233) puis le tag pose la
            // sienne (#FFFF0000) — le DERNIER appel gagne (sémantique
            // Android : les attributs propres l'emportent).
            ordre.verify(tv).setTextColor(0xFF112233);
            ordre.verify(tv).setTextColor(0xFFFF0000);
        }
    }
}
