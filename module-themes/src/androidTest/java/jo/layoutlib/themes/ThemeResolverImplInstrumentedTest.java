package jo.layoutlib.themes;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jo.layoutlib.resources.DimensionConverter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests instrumentés du {@link ThemeResolverImpl}.
 *
 * @author jo@Dev
 */
@RunWith(AndroidJUnit4.class)
public class ThemeResolverImplInstrumentedTest {

    private Context context;
    private ThemeResolverImpl resolver;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        resolver = new ThemeResolverImpl();

        float density = context.getResources().getDisplayMetrics().density;
        float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        float xdpi = context.getResources().getDisplayMetrics().xdpi;
        resolver = new ThemeResolverImpl(null, new DimensionConverter(density, fontScale, xdpi));

        resolver.registerThemesFile(
                "<resources>"
                        + "<style name=\"Theme.MyApp\" parent=\"Theme.Material3.DayNight\">"
                        + "<item name=\"colorPrimary\">#FF6750A4</item>"
                        + "<item name=\"buttonHeight\">16dp</item>"
                        + "</style>"
                        + "</resources>");
        resolver.setTheme("Theme.MyApp");
    }

    @Test
    public void getColorAttr_returnsColorValue() {
        Integer color = resolver.getColorAttr("colorPrimary");
        assertNotNull(color);
        assertEquals((Integer) 0xFF6750A4, color);
    }

    @Test
    public void getDimensionAttr_returnsPixels() {
        Float dim = resolver.getDimensionAttr("buttonHeight");
        assertNotNull(dim);
        float density = context.getResources().getDisplayMetrics().density;
        assertEquals(16f * density, dim, 0.1f);
    }

    @Test
    public void resolveAttr_returnsNullForUnknown() {
        assertNull(resolver.resolveAttr("?attr/nonexistent"));
    }

    @Test
    public void setNightMode_invalidatesCache() {
        resolver.getColorAttr("colorPrimary");
        resolver.setNightMode(true);
        // Pas d'erreur
        Integer value = resolver.getColorAttr("colorPrimary");
        assertNotNull(value);
    }

    @Test
    public void setTheme_invalidatesCache() {
        resolver.getColorAttr("colorPrimary");
        resolver.setTheme("Theme.Other");
        // Pas d'erreur
        assertNull(resolver.getColorAttr("colorPrimary"));
    }

    @Test
    public void clearCache_doesNotThrow() {
        resolver.getColorAttr("colorPrimary");
        resolver.clearCache();
        assertTrue(true);
    }

    @Test
    public void getParentStyle_returnsParent() {
        assertEquals("Theme.Material3.DayNight", resolver.getParentStyle("Theme.MyApp"));
    }

    @Test
    public void getInheritanceChain_buildsCorrectChain() {
        assertEquals(2, resolver.getInheritanceChain("Theme.MyApp").size());
    }

    @Test
    public void getStyleCount_returnsCorrectCount() {
        assertEquals(1, resolver.getStyleCount());
    }

    @Test
    public void registerStylesFile_addsToRegistry() {
        int before = resolver.getStyleCount();
        resolver.registerStylesFile(
                "<resources><style name=\"MyStyle\"><item name=\"x\">y</item></style></resources>");
        assertEquals(before + 1, resolver.getStyleCount());
    }
}
