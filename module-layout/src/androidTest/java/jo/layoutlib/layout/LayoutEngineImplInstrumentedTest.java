package jo.layoutlib.layout;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests instrumentés du {@link LayoutEngineImpl}.
 *
 * <p>Ces tests valident que le moteur de layout produit des mesures et
 * positions correctes sur des vues réelles Android.</p>
 *
 * @author jo@Dev
 */
@RunWith(AndroidJUnit4.class)
public class LayoutEngineImplInstrumentedTest {

    private android.content.Context context;
    private LayoutEngineImpl engine;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        engine = new LayoutEngineImpl();
        engine.setDensity(context.getResources().getDisplayMetrics().density);
        engine.setFontScale(context.getResources().getDisplayMetrics().scaledDensity
                / context.getResources().getDisplayMetrics().density);
    }

    @Test
    public void measure_textViewWithExactSize_setsMeasuredDimensions() {
        TextView tv = new TextView(context);
        tv.setText("Hello");
        tv.setLayoutParams(new ViewGroup.LayoutParams(200, 100));
        engine.measure(tv, 1080, 1920);
        assertEquals(200, tv.getMeasuredWidth());
        assertEquals(100, tv.getMeasuredHeight());
    }

    @Test
    public void measure_textViewWithWrapContent_measuresContent() {
        TextView tv = new TextView(context);
        tv.setText("Hello");
        tv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        engine.measure(tv, 1080, 1920);
        assertTrue("Width should be > 0", tv.getMeasuredWidth() > 0);
        assertTrue("Height should be > 0", tv.getMeasuredHeight() > 0);
    }

    @Test
    public void measure_matchParent_returnsExactSize() {
        TextView tv = new TextView(context);
        tv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        engine.measure(tv, 500, 800);
        assertEquals(500, tv.getMeasuredWidth());
        assertEquals(800, tv.getMeasuredHeight());
    }

    @Test
    public void layout_positionsViewCorrectly() {
        TextView tv = new TextView(context);
        tv.measure(
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY));
        engine.layout(tv, 10, 20, 110, 70);
        assertEquals(10, tv.getLeft());
        assertEquals(20, tv.getTop());
        assertEquals(110, tv.getRight());
        assertEquals(70, tv.getBottom());
    }

    @Test
    public void render_measuresAndLayoutsInOneCall() {
        TextView tv = new TextView(context);
        tv.setText("Test");
        tv.setLayoutParams(new ViewGroup.LayoutParams(200, 100));
        engine.render(tv, 1080, 1920);
        assertEquals(200, tv.getMeasuredWidth());
        assertEquals(100, tv.getMeasuredHeight());
        assertEquals(0, tv.getLeft());
        assertEquals(0, tv.getTop());
        assertEquals(200, tv.getRight());
        assertEquals(100, tv.getBottom());
    }

    @Test
    public void measure_linearLayoutWithChildren_distributesSpace() {
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        TextView child1 = new TextView(context);
        child1.setText("A");
        child1.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView child2 = new TextView(context);
        child2.setText("B");
        child2.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        ll.addView(child1);
        ll.addView(child2);
        engine.measure(ll, 500, 1000);
        assertTrue(child1.getMeasuredHeight() > 0);
        assertTrue(child2.getMeasuredHeight() > 0);
    }

    @Test
    public void measure_frameLayoutWithChildren_overlaps() {
        FrameLayout fl = new FrameLayout(context);
        TextView child1 = new TextView(context);
        child1.setText("A");
        child1.setLayoutParams(new FrameLayout.LayoutParams(100, 50));
        TextView child2 = new TextView(context);
        child2.setText("B");
        child2.setLayoutParams(new FrameLayout.LayoutParams(80, 40));
        fl.addView(child1);
        fl.addView(child2);
        engine.measure(fl, 500, 500);
        engine.layout(fl, 0, 0, fl.getMeasuredWidth(), fl.getMeasuredHeight());
        // Les deux enfants sont positionnés au même endroit (FrameLayout)
        assertEquals(0, child1.getLeft());
        assertEquals(0, child2.getLeft());
    }

    @Test(expected = LayoutException.class)
    public void measure_nullView_throwsException() {
        engine.measure(null, 100, 100);
    }

    @Test(expected = LayoutException.class)
    public void setDensity_zeroOrNegative_throwsException() {
        engine.setDensity(0);
    }

    @Test(expected = LayoutException.class)
    public void setFontScale_zeroOrNegative_throwsException() {
        engine.setFontScale(-1);
    }

    @Test
    public void setDensity_validValue_updatesField() {
        engine.setDensity(3.0f);
        assertEquals(3.0f, engine.getDensity(), 0.001f);
    }
}
