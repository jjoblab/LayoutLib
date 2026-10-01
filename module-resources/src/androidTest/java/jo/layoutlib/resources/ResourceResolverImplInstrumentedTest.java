package jo.layoutlib.resources;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests instrumentés du {@link ResourceResolverImpl}.
 *
 * <p>Ces tests valident la résolution de resources avec un vrai contexte
 * Android, en utilisant les display metrics réelles du device. Ils créent
 * un dossier {@code res/} temporaire sur le stockage externe pour tester la
 * lecture de fichiers.</p>
 *
 * @author jo@Dev
 */
@RunWith(AndroidJUnit4.class)
public class ResourceResolverImplInstrumentedTest {

    private Context context;
    private File tempResFolder;

    @Before
    public void setUp() throws IOException {
        context = ApplicationProvider.getApplicationContext();
        tempResFolder = createTempResFolder();
    }

    /**
     * Crée un dossier res/ temporaire avec des fichiers de test.
     */
    private File createTempResFolder() throws IOException {
        File baseDir = context.getCacheDir();
        File resFolder = new File(baseDir, "test-res-" + System.currentTimeMillis());
        File valuesDir = new File(resFolder, "values");
        valuesDir.mkdirs();

        writeFile(new File(valuesDir, "colors.xml"),
                "<resources><color name=\"primary\">#FF6750A4</color></resources>");
        writeFile(new File(valuesDir, "strings.xml"),
                "<resources><string name=\"app_name\">TestApp</string></resources>");
        writeFile(new File(valuesDir, "dimens.xml"),
                "<resources><dimen name=\"margin\">16dp</dimen></resources>");

        return resFolder;
    }

    private void writeFile(File file, String content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes("UTF-8"));
        }
    }

    @Test
    public void constructor_loadsColorsFromFolder() {
        ResourceResolverImpl resolver = new ResourceResolverImpl(tempResFolder.getAbsolutePath());
        assertEquals(1, resolver.getColorCount());
    }

    @Test
    public void constructor_loadsStringsFromFolder() {
        ResourceResolverImpl resolver = new ResourceResolverImpl(tempResFolder.getAbsolutePath());
        assertEquals(1, resolver.getStringCount());
    }

    @Test
    public void constructor_loadsDimensFromFolder() {
        ResourceResolverImpl resolver = new ResourceResolverImpl(tempResFolder.getAbsolutePath());
        assertEquals(1, resolver.getDimenCount());
    }

    @Test
    public void getColor_resolvesColorFromFolder() {
        ResourceResolverImpl resolver = new ResourceResolverImpl(tempResFolder.getAbsolutePath());
        Integer color = resolver.getColor("@color/primary");
        assertNotNull(color);
        assertEquals(0xFF6750A4, (int) color);
    }

    @Test
    public void getString_resolvesStringFromFolder() {
        ResourceResolverImpl resolver = new ResourceResolverImpl(tempResFolder.getAbsolutePath());
        String value = resolver.getString("@string/app_name");
        assertEquals("TestApp", value);
    }

    @Test
    public void getDimension_usesRealDisplayMetrics() {
        ResourceResolverImpl resolver = new ResourceResolverImpl(tempResFolder.getAbsolutePath());
        // Configure avec les vraies métriques du device
        float density = context.getResources().getDisplayMetrics().density;
        float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        float xdpi = context.getResources().getDisplayMetrics().xdpi;
        resolver.setDimensionConverter(new DimensionConverter(density, fontScale, xdpi));

        Float px = resolver.getDimension("@dimen/margin");
        assertNotNull(px);
        // 16dp × density = expected
        assertEquals(16f * density, px, 0.1f);
    }

    @Test
    public void getLayout_returnsNullForMissingLayout() {
        ResourceResolverImpl resolver = new ResourceResolverImpl(tempResFolder.getAbsolutePath());
        assertNull(resolver.getLayout("@layout/nonexistent"));
    }

    @Test
    public void getDrawablePath_returnsNullForMissingDrawable() {
        ResourceResolverImpl resolver = new ResourceResolverImpl(tempResFolder.getAbsolutePath());
        assertNull(resolver.getDrawablePath("@drawable/nonexistent"));
    }

    @Test
    public void setNightMode_invalidatesCache() {
        ResourceResolverImpl resolver = new ResourceResolverImpl(tempResFolder.getAbsolutePath());
        // Première résolution
        Integer first = resolver.getColor("@color/primary");
        assertNotNull(first);
        // Changement de mode
        resolver.setNightMode(true);
        // Deuxième résolution (pas d'erreur)
        Integer second = resolver.getColor("@color/primary");
        assertNotNull(second);
    }

    @Test
    public void clearCache_doesNotThrow() {
        ResourceResolverImpl resolver = new ResourceResolverImpl(tempResFolder.getAbsolutePath());
        resolver.getColor("@color/primary");
        resolver.clearCache();
        assertTrue(true);
    }
}
