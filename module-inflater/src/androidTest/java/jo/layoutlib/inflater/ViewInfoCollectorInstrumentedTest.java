package jo.layoutlib.inflater;

import android.content.Context;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests instrumentés du {@link ViewInfoCollector} — focus sur la
 * <strong>règle d'or de l'alignement</strong> : les positions
 * {@code left/top/right/bottom} doivent être <em>absolues</em>
 * (root-relative), pas parent-relative.
 *
 * <p>Avant le fix, {@code collectInternal} lisait {@code View.getLeft()/getTop()}
 * qui sont relatifs au parent direct. Le bug se manifestait sur les
 * petits-enfants : leur ViewInfo pointait vers l'offset du parent, ce qui
 * faisait que l'OverlayView dessinait les overlays (sélection, handles,
 * guides, bp-boxes) à la mauvaise position pour toute la hiérarchie
 * profonde.</p>
 *
 * <p>Après le fix, le ViewInfo contient des positions absolues, et la
 * sélection / hit-testing / handles / guides s'alignent parfaitement
 * avec le rendu — même à 3+ niveaux de profondeur.</p>
 *
 * @author jo@Dev
 */
@RunWith(AndroidJUnit4.class)
public class ViewInfoCollectorInstrumentedTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
    }

    /**
     * Scénario Layout simple — TextView seul.
     *
     * <p>Vérifie que pour une vue unique (pas de parent), les positions
     * ViewInfo sont égales aux positions de la View (left=0, top=0 pour
     * la racine).</p>
     */
    @Test
    public void collect_simpleTextView_returnsAbsolutePositions() {
        TextView tv = new TextView(context);
        tv.setText("Hello");
        tv.layout(0, 0, 200, 80);

        ViewInfoCollector.ViewInfo info = ViewInfoCollector.collect(tv);

        assertNotNull(info);
        assertEquals(0, info.left);
        assertEquals(0, info.top);
        assertEquals(200, info.right);
        assertEquals(80, info.bottom);
        assertEquals(200, info.width);
        assertEquals(80, info.height);
    }

    /**
     * Scénario Layout imbriqué — LinearLayout (parent) contenant un TextView
     * positionné à (40, 60).
     *
     * <p>Vérifie que le ViewInfo de l'enfant a des positions ABSOLUES
     * (root-relative), pas parent-relative. C'est le test clé pour le fix :
     * avant le fix, l'enfant aurait {left=40, top=60} (correct car égal à
     * sa position parent-relative dans ce cas simple). Mais pour un
     * petit-enfant (voir test suivant), le bug se manifestait.</p>
     */
    @Test
    public void collect_linearLayoutWithChild_childPositionsAreAbsolute() {
        LinearLayout parent = new LinearLayout(context);
        parent.layout(0, 0, 500, 1000);

        TextView child = new TextView(context);
        child.setText("Child");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(200, 80);
        lp.leftMargin = 40;
        lp.topMargin = 60;
        child.setLayoutParams(lp);
        child.layout(40, 60, 240, 140);
        parent.addView(child);

        ViewInfoCollector.ViewInfo root = ViewInfoCollector.collect(parent);

        assertNotNull(root);
        assertEquals(1, root.children.size());

        ViewInfoCollector.ViewInfo childInfo = root.children.get(0);
        // ── L'enfant doit avoir des positions ABSOLUES (root-relative) ──
        assertEquals("child left must be absolute (root-relative)", 40, childInfo.left);
        assertEquals("child top must be absolute (root-relative)", 60, childInfo.top);
        assertEquals("child right must be absolute (root-relative)", 240, childInfo.right);
        assertEquals("child bottom must be absolute (root-relative)", 140, childInfo.bottom);
    }

    /**
     * Scénario Layout imbriqué 3 niveaux — FrameLayout grand-parent (offset 0,0)
     * > LinearLayout parent (offset 30, 50) > TextView petit-enfant (offset 100, 200
     * relatif au parent).
     *
     * <p><strong>C'est le test qui échouait avant le fix.</strong> Avant, le
     * petit-enfant avait {left=100, top=200} (parent-relative), ce qui faisait
     * que l'overlay le dessinait à (100, 200) au lieu de (130, 250).
     * Après le fix, il a {left=130, top=250} (root-relative) et l'overlay
     * s'aligne parfaitement.</p>
     */
    @Test
    public void collect_threeLevelHierarchy_grandchildPositionsAreAbsolute() {
        FrameLayout grandParent = new FrameLayout(context);
        grandParent.layout(0, 0, 1000, 2000);

        LinearLayout parent = new LinearLayout(context);
        FrameLayout.LayoutParams parentLp = new FrameLayout.LayoutParams(800, 1500);
        parent.setLayoutParams(parentLp);
        parent.layout(30, 50, 830, 1550);
        grandParent.addView(parent);

        TextView grandChild = new TextView(context);
        LinearLayout.LayoutParams childLp = new LinearLayout.LayoutParams(200, 80);
        grandChild.setLayoutParams(childLp);
        grandChild.layout(100, 200, 300, 280);
        parent.addView(grandChild);

        ViewInfoCollector.ViewInfo root = ViewInfoCollector.collect(grandParent);

        assertNotNull(root);
        assertEquals(1, root.children.size());

        ViewInfoCollector.ViewInfo parentInfo = root.children.get(0);
        // ── Parent direct de la racine : sa position est 30,50 (absolue = parent-relative ici) ──
        assertEquals("parent left must be absolute", 30, parentInfo.left);
        assertEquals("parent top must be absolute", 50, parentInfo.top);

        assertEquals(1, parentInfo.children.size());
        ViewInfoCollector.ViewInfo grandChildInfo = parentInfo.children.get(0);

        // ── Le petit-enfant DOIT avoir des positions ABSOLUES (root-relative) ──
        // Avant le fix : left=100, top=200 (parent-relative)
        // Après le fix : left=130, top=250 (root-relative = 30+100, 50+200)
        assertEquals("grandchild left MUST be absolute (root-relative), not parent-relative",
                130, grandChildInfo.left);
        assertEquals("grandchild top MUST be absolute (root-relative), not parent-relative",
                250, grandChildInfo.top);
        assertEquals("grandchild right MUST be absolute (root-relative), not parent-relative",
                330, grandChildInfo.right);
        assertEquals("grandchild bottom MUST be absolute (root-relative), not parent-relative",
                330, grandChildInfo.bottom);
    }

    /**
     * Scénario hit-testing — findAtPoint doit retourner la vue contenant
     * un point, en utilisant les positions ABSOLUES.
     *
     * <p>Avant le fix, le hit-testing échouait pour les petits-enfants
     * car leurs coordonnées ViewInfo étaient parent-relative : un touch
     * à (130, 250) ne matchait pas un enfant qui se croyait à (100, 200).</p>
     */
    @Test
    public void findAtPoint_grandchild_hitTestUsesAbsoluteCoords() {
        FrameLayout grandParent = new FrameLayout(context);
        grandParent.layout(0, 0, 1000, 2000);

        LinearLayout parent = new LinearLayout(context);
        parent.layout(30, 50, 830, 1550);
        grandParent.addView(parent);

        TextView grandChild = new TextView(context);
        grandChild.layout(100, 200, 300, 280);
        parent.addView(grandChild);

        ViewInfoCollector.ViewInfo root = ViewInfoCollector.collect(grandParent);

        // Touch à (130, 250) = (30 + 100, 50 + 200) = position absolue du grandchild
        java.util.List<ViewInfoCollector.ViewInfo> hits =
                ViewInfoCollector.findAtPoint(root, 130, 250);

        // On doit trouver au moins 1 vue (le grandchild), potentiellement 2 (parent + grandchild)
        assertTrue("findAtPoint must find grandchild at absolute position (130, 250)",
                hits.size() >= 1);
        // Le plus profond doit être le grandchild
        assertEquals("deepest hit must be the grandchild (TextView)",
                grandChild.getClass().getSimpleName(),
                hits.get(0).simpleName);
    }

    /**
     * Scénario marges préservées — les marges (parent-relative) doivent
     * toujours être lues correctement, indépendamment du calcul d'offset.
     */
    @Test
    public void collect_withMargins_marginsArePreserved() {
        LinearLayout parent = new LinearLayout(context);
        parent.layout(0, 0, 500, 1000);

        TextView child = new TextView(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(200, 80);
        lp.leftMargin = 40;
        lp.topMargin = 60;
        lp.rightMargin = 20;
        lp.bottomMargin = 30;
        child.setLayoutParams(lp);
        child.layout(40, 60, 240, 140);
        parent.addView(child);

        ViewInfoCollector.ViewInfo root = ViewInfoCollector.collect(parent);
        ViewInfoCollector.ViewInfo childInfo = root.children.get(0);

        assertEquals(40, childInfo.marginLeft);
        assertEquals(60, childInfo.marginTop);
        assertEquals(20, childInfo.marginRight);
        assertEquals(30, childInfo.marginBottom);
    }
}
