package jo.layoutlib.inflater;

import android.content.Context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.xmlpull.v1.XmlPullParser;

import java.util.HashSet;
import java.util.Set;

import jo.layoutlib.attributes.AttributeRegistry;
import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.themes.ThemeResolverImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Tests des intégrations des modules attributes et themes dans le pipeline
 * de rendu (point 5) :
 * <ul>
 *   <li><strong>module-attributes</strong> : le mode strict de
 *       {@link BridgeInflater} valide les attributs contre un
 *       {@link AttributeRegistry}</li>
 *   <li><strong>module-themes</strong> : {@code ?attr/} résolu via un
 *       {@link ThemeResolverImpl} (themes.xml/styles.xml)</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 1.0
 */
@DisplayName("Intégrations module-attributes (strict) et module-themes (?attr/)")
class ModuleIntegrationTest {

    private Context context;
    private ViewTagRegistry registry;

    /** Stub de registre d'attributs : ne connaît que les noms fournis. */
    private static class StubAttributeRegistry implements AttributeRegistry {
        final Set<String> known = new HashSet<>();

        @Override
        public void registerAttrsFile(String xml) {
        }

        @Override
        public void applyCustomAttributes(android.view.View view,
                                          XmlPullParser parser, Context context) {
        }

        @Override
        public boolean isKnownAttribute(String attrName) {
            return known.contains(attrName);
        }

        @Override
        public String getAttributeFormat(String attrName) {
            return null;
        }

        @Override
        public void clear() {
        }
    }

    @BeforeEach
    void setUp() {
        context = mock(Context.class);
        registry = new ViewTagRegistry(false);
    }

    @Nested
    @DisplayName("Mode strict + AttributeRegistry (module-attributes)")
    class StrictMode {

        private XmlPullParser parserOn(String xml, String tag)
                throws Exception {
            XmlPullParser parser = XmlPreprocessor.createParser(xml);
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG
                        && tag.equals(parser.getName())) {
                    return parser;
                }
                event = parser.next();
            }
            throw new IllegalStateException("tag introuvable : " + tag);
        }

        @Test
        @DisplayName("un attribut inconnu lève InflateException en mode strict")
        void unknownAttributeThrowsInStrictMode() throws Exception {
            StubAttributeRegistry attributeRegistry = new StubAttributeRegistry();
            attributeRegistry.known.add("text");
            attributeRegistry.known.add("layout_width");
            attributeRegistry.known.add("layout_height");

            BridgeInflater inflater = new BridgeInflater(context, registry);
            inflater.setStrictMode(true);
            inflater.setAttributeRegistry(attributeRegistry);

            XmlPullParser parser = parserOn(
                    "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\""
                            + " android:text=\"x\""
                            + " android:layout_width=\"wrap_content\""
                            + " android:layout_height=\"wrap_content\""
                            + " android:attribut_factice=\"1\"/>",
                    "TextView");

            assertThatThrownBy(() -> inflater.checkKnownAttributes(parser))
                    .isInstanceOf(InflateException.class)
                    .hasMessageContaining("attribut_factice");
        }

        @Test
        @DisplayName("des attributs connus passent en mode strict")
        void knownAttributesPass() throws Exception {
            StubAttributeRegistry attributeRegistry = new StubAttributeRegistry();
            attributeRegistry.known.add("text");
            attributeRegistry.known.add("layout_width");
            attributeRegistry.known.add("layout_height");

            BridgeInflater inflater = new BridgeInflater(context, registry);
            inflater.setStrictMode(true);
            inflater.setAttributeRegistry(attributeRegistry);

            XmlPullParser parser = parserOn(
                    "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\""
                            + " android:text=\"x\""
                            + " android:layout_width=\"wrap_content\""
                            + " android:layout_height=\"wrap_content\"/>",
                    "TextView");

            // Ne lève pas
            inflater.checkKnownAttributes(parser);
        }

        @Test
        @DisplayName("sans mode strict, un attribut inconnu est ignoré")
        void unknownAttributeIgnoredWithoutStrictMode() throws Exception {
            StubAttributeRegistry attributeRegistry = new StubAttributeRegistry();

            BridgeInflater inflater = new BridgeInflater(context, registry);
            inflater.setStrictMode(false);
            inflater.setAttributeRegistry(attributeRegistry);

            XmlPullParser parser = parserOn(
                    "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\""
                            + " android:attribut_factice=\"1\"/>",
                    "TextView");

            // Ne lève pas : hors mode strict, on ignore
            inflater.checkKnownAttributes(parser);
        }

        @Test
        @DisplayName("sans registre, le mode strict garde son comportement historique")
        void strictModeWithoutRegistryIsInert() throws Exception {
            BridgeInflater inflater = new BridgeInflater(context, registry);
            inflater.setStrictMode(true);
            // pas de registre

            XmlPullParser parser = parserOn(
                    "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\""
                            + " android:attribut_factice=\"1\"/>",
                    "TextView");

            // Ne lève pas : pas de registre → pas de vérification de noms
            inflater.checkKnownAttributes(parser);
        }
    }

    @Nested
    @DisplayName("?attr/ via ThemeResolverImpl (module-themes)")
    class ThemeAttrs {

        @Test
        @DisplayName("resolveThemeAttr consulte le thème du projet d'abord")
        void resolvesThemeAttrFromProjectTheme() {
            ThemeResolverImpl themeResolver = new ThemeResolverImpl();
            themeResolver.registerThemesFile(
                    "<resources>"
                            + "<style name=\"Theme.App\">"
                            + "<item name=\"colorPrimary\">#FF6750A4</item>"
                            + "</style>"
                            + "</resources>", false);
            themeResolver.setTheme("Theme.App");

            AttributeApplier applier = new AttributeApplier(
                    context, new DimensionConverter(2.75f, 2.75f, 420f));
            applier.setThemeResolver(themeResolver);

            Integer color = applier.resolveThemeAttr("?attr/colorPrimary");

            assertThat(color).isEqualTo(0xFF6750A4);
        }

        @Test
        @DisplayName("sans thème du projet, ?attr/ inconnu retourne null (pas d'exception)")
        void unknownThemeAttrReturnsNull() {
            ThemeResolverImpl themeResolver = new ThemeResolverImpl();
            themeResolver.setTheme("Theme.Inexistant");

            AttributeApplier applier = new AttributeApplier(
                    context, new DimensionConverter(2.75f, 2.75f, 420f));
            applier.setThemeResolver(themeResolver);

            // Le thème natif mocké ne définit rien → null, sans lever
            assertThat(applier.resolveThemeAttr("?attr/colorPrimary")).isNull();
            assertThat(applier.resolveThemeAttr(null)).isNull();
            assertThat(applier.resolveThemeAttr("pas-une-ref")).isNull();
        }

        @Test
        @DisplayName("l'héritage de thème est respecté (chaîne parent)")
        void resolvesThroughInheritanceChain() {
            ThemeResolverImpl themeResolver = new ThemeResolverImpl();
            themeResolver.registerThemesFile(
                    "<resources>"
                            + "<style name=\"BaseTheme\">"
                            + "<item name=\"colorPrimary\">#FF6750A4</item>"
                            + "</style>"
                            + "<style name=\"Theme.App\" parent=\"BaseTheme\">"
                            + "<item name=\"colorSecondary\">#FF625B71</item>"
                            + "</style>"
                            + "</resources>", false);
            themeResolver.setTheme("Theme.App");

            AttributeApplier applier = new AttributeApplier(
                    context, new DimensionConverter(2.75f, 2.75f, 420f));
            applier.setThemeResolver(themeResolver);

            // colorPrimary est défini sur le PARENT, pas sur Theme.App
            assertThat(applier.resolveThemeAttr("?attr/colorPrimary"))
                    .isEqualTo(0xFF6750A4);
            assertThat(applier.resolveThemeAttr("?attr/colorSecondary"))
                    .isEqualTo(0xFF625B71);
        }
    }
}
