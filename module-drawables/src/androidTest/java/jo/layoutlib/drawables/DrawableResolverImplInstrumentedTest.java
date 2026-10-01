package jo.layoutlib.drawables;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests instrumentés du {@link DrawableResolverImpl}.
 *
 * <p>Ces tests valident la création de véritables Drawables Android à partir
 * des configs parsées.</p>
 *
 * @author jo@Dev
 */
@RunWith(AndroidJUnit4.class)
public class DrawableResolverImplInstrumentedTest {

    private Context context;
    private DrawableResolverImpl resolver;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        resolver = new DrawableResolverImpl();
    }

    @Test
    public void resolve_colorLiteral_returnsColorDrawable() {
        Drawable drawable = resolver.resolve("#FF6750A4", context);
        assertNotNull(drawable);
        assertTrue(drawable instanceof ColorDrawable);
        assertEquals(0xFF6750A4, ((ColorDrawable) drawable).getColor());
    }

    @Test
    public void parse_shapeXml_returnsGradientDrawable() {
        String xml = "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                + "android:shape=\"rectangle\">"
                + "<solid android:color=\"#FF6750A4\"/>"
                + "<corners android:radius=\"8dp\"/>"
                + "</shape>";
        Drawable drawable = resolver.parse(xml, context);
        assertNotNull(drawable);
        assertTrue(drawable instanceof GradientDrawable);
    }

    @Test
    public void parse_colorXml_returnsColorDrawable() {
        String xml = "<color xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                + "android:color=\"#FF0000FF\"/>";
        Drawable drawable = resolver.parse(xml, context);
        assertNotNull(drawable);
        assertTrue(drawable instanceof ColorDrawable);
        assertEquals(0xFF0000FF, ((ColorDrawable) drawable).getColor());
    }

    @Test
    public void resolve_null_returnsNull() {
        assertNull(resolver.resolve(null, context));
        assertNull(resolver.resolve("", context));
    }

    @Test
    public void parse_null_returnsNull() {
        assertNull(resolver.parse(null, context));
        assertNull(resolver.parse("", context));
    }

    @Test
    public void parse_shapeWithGradient_returnsGradientDrawable() {
        String xml = "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                + "<gradient android:startColor=\"#FF0000\" android:endColor=\"#00FF00\" "
                + "android:angle=\"90\"/>"
                + "</shape>";
        Drawable drawable = resolver.parse(xml, context);
        assertNotNull(drawable);
        assertTrue(drawable instanceof GradientDrawable);
    }

    @Test
    public void parse_shapeWithStroke_returnsGradientDrawable() {
        String xml = "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                + "<solid android:color=\"#FFFFFFFF\"/>"
                + "<stroke android:width=\"2dp\" android:color=\"#FF000000\"/>"
                + "</shape>";
        Drawable drawable = resolver.parse(xml, context);
        assertNotNull(drawable);
        assertTrue(drawable instanceof GradientDrawable);
    }

    @Test
    public void parse_ovalShape_returnsGradientDrawable() {
        String xml = "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                + "android:shape=\"oval\">"
                + "<solid android:color=\"#FF6750A4\"/>"
                + "</shape>";
        Drawable drawable = resolver.parse(xml, context);
        assertNotNull(drawable);
        assertTrue(drawable instanceof GradientDrawable);
    }

    @Test
    public void clearCache_doesNotThrow() {
        resolver.parseConfig("<shape xmlns:android=\"http://schemas.android.com/apk/res/android\"/>");
        resolver.clearCache();
        assertTrue(true);
    }

    @Test
    public void parseConfig_shape_returnsShapeConfig() {
        Object config = resolver.parseConfig(
                "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\"/>");
        assertNotNull(config);
        assertTrue(config instanceof ShapeConfig);
    }
}
