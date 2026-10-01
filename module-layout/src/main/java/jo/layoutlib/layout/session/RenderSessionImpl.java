package jo.layoutlib.layout.session;

import android.view.View;
import android.view.ViewGroup;

import jo.layoutlib.layout.LayoutEngineImpl;

/**
 * Implémentation d une session de rendu, inspiré de
 * com.android.layoutlib.bridge.impl.RenderSessionImpl de l AOSP.
 *
 * <p>Une session de rendu encapsule une inflation + mesure + layout
 * complets d un arbre de vues.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class RenderSessionImpl {

    private final LayoutEngineImpl layoutEngine;
    private View rootView;
    private int measuredWidth;
    private int measuredHeight;
    private long renderTimeMs;
    private boolean rendered;

    public RenderSessionImpl() {
        this.layoutEngine = new LayoutEngineImpl();
    }

    public RenderSessionImpl(LayoutEngineImpl layoutEngine) {
        this.layoutEngine = layoutEngine != null ? layoutEngine : new LayoutEngineImpl();
    }

    /**
     * Effectue le rendu de la vue racine.
     *
     * @param root la vue racine
     * @param width la largeur cible
     * @param height la hauteur cible
     */
    public void render(View root, int width, int height) {
        if (root == null) {
            throw new IllegalArgumentException("Vue racine null");
        }
        this.rootView = root;
        long start = System.nanoTime();
        layoutEngine.render(root, width, height);
        this.renderTimeMs = (System.nanoTime() - start) / 1_000_000;
        this.measuredWidth = root.getMeasuredWidth();
        this.measuredHeight = root.getMeasuredHeight();
        this.rendered = true;
    }

    public View getRootView() {
        return rootView;
    }

    public int getMeasuredWidth() {
        return measuredWidth;
    }

    public int getMeasuredHeight() {
        return measuredHeight;
    }

    public long getRenderTimeMs() {
        return renderTimeMs;
    }

    public boolean isRendered() {
        return rendered;
    }

    public LayoutEngineImpl getLayoutEngine() {
        return layoutEngine;
    }

    /**
     * Compte le nombre total de vues dans l arbre.
     *
     * @return le nombre de vues
     */
    public int getViewCount() {
        return countViews(rootView);
    }

    private int countViews(View view) {
        if (view == null) return 0;
        int count = 1;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                count += countViews(group.getChildAt(i));
            }
        }
        return count;
    }
}
