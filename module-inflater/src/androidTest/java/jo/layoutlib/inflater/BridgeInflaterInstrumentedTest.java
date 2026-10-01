package jo.layoutlib.inflater;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests instrumentés du {@link BridgeInflater}.
 *
 * <p>Ces tests nécessitent un émulateur ou un device Android. Ils valident
 * que l'inflation fonctionne réellement avec un contexte Android, des
 * ressources, des display metrics, etc.</p>
 *
 * @author jo@Dev
 */
@RunWith(AndroidJUnit4.class)
public class BridgeInflaterInstrumentedTest {

    private Context context;
    private BridgeInflater inflater;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        inflater = new BridgeInflater(context, new ViewTagRegistry(false));
    }

    @Test
    public void inflate_simpleTextView_returnsTextView() {
        View view = inflater.inflate(
                "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                        + "android:text=\"Hello\" "
                        + "android:layout_width=\"wrap_content\" "
                        + "android:layout_height=\"wrap_content\" />");
        assertNotNull(view);
        assertTrue(view instanceof TextView);
        assertEquals("Hello", ((TextView) view).getText().toString());
    }

    @Test
    public void inflate_linearLayoutWithChildren_returnsNestedViews() {
        View root = inflater.inflate(
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                        + "android:layout_width=\"match_parent\" "
                        + "android:layout_height=\"match_parent\" "
                        + "android:orientation=\"vertical\">"
                        + "<TextView android:text=\"A\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "<TextView android:text=\"B\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "</LinearLayout>");
        assertNotNull(root);
        assertTrue(root instanceof LinearLayout);
        assertEquals(2, ((LinearLayout) root).getChildCount());
    }

    @Test
    public void inflate_viewWithId_indexesViewById() {
        View view = inflater.inflate(
                "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                        + "android:id=\"@+id/my_text\" "
                        + "android:layout_width=\"wrap_content\" "
                        + "android:layout_height=\"wrap_content\" />");
        assertNotNull(view);
        assertTrue(view.getId() != View.NO_ID);
        View found = inflater.findViewById(view.getId());
        assertNotNull(found);
        assertSame(view, found);
    }

    @Test
    public void inflate_viewWithVisibility_setsVisibility() {
        View view = inflater.inflate(
                "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                        + "android:visibility=\"gone\" "
                        + "android:layout_width=\"wrap_content\" "
                        + "android:layout_height=\"wrap_content\" />");
        assertEquals(View.GONE, view.getVisibility());
    }

    @Test
    public void inflate_viewWithPadding_setsPadding() {
        View view = inflater.inflate(
                "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                        + "android:paddingLeft=\"16dp\" "
                        + "android:paddingTop=\"8dp\" "
                        + "android:paddingRight=\"16dp\" "
                        + "android:paddingBottom=\"8dp\" "
                        + "android:layout_width=\"wrap_content\" "
                        + "android:layout_height=\"wrap_content\" />");
        assertTrue(view.getPaddingLeft() > 0);
        assertTrue(view.getPaddingTop() > 0);
    }

    @Test
    public void inflate_matchParent_setsMatchParent() {
        View view = inflater.inflate(
                "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                        + "android:layout_width=\"match_parent\" "
                        + "android:layout_height=\"wrap_content\" />");
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        assertNotNull(lp);
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, lp.width);
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, lp.height);
    }

    @Test
    public void inflate_wrapContent_setsWrapContent() {
        View view = inflater.inflate(
                "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                        + "android:layout_width=\"wrap_content\" "
                        + "android:layout_height=\"wrap_content\" />");
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        assertNotNull(lp);
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, lp.width);
    }

    @Test
    public void inflate_withToolsAttribute_convertsToAndroid() {
        View view = inflater.inflate(
                "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                        + "xmlns:tools=\"http://schemas.android.com/tools\" "
                        + "tools:text=\"Preview\" "
                        + "android:layout_width=\"wrap_content\" "
                        + "android:layout_height=\"wrap_content\" />");
        assertTrue(view instanceof TextView);
        assertEquals("Preview", ((TextView) view).getText().toString());
    }

    @Test
    public void inflate_nestedThreeLevelsDeep_returnsAllViews() {
        View root = inflater.inflate(
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                        + "android:layout_width=\"match_parent\" "
                        + "android:layout_height=\"match_parent\">"
                        + "<LinearLayout android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\">"
                        + "<LinearLayout android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\">"
                        + "<TextView android:text=\"Deep\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "</LinearLayout>"
                        + "</LinearLayout>"
                        + "</LinearLayout>");
        assertNotNull(root);
        LinearLayout level1 = (LinearLayout) root;
        assertEquals(1, level1.getChildCount());
        LinearLayout level2 = (LinearLayout) level1.getChildAt(0);
        assertEquals(1, level2.getChildCount());
        LinearLayout level3 = (LinearLayout) level2.getChildAt(0);
        assertEquals(1, level3.getChildCount());
        assertTrue(level3.getChildAt(0) instanceof TextView);
    }

    @Test(expected = InflateException.class)
    public void inflate_invalidXml_throwsInflateException() {
        inflater.inflate("<TextView><unclosed");
    }
}
