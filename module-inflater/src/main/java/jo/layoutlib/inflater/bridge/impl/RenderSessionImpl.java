package jo.layoutlib.inflater.bridge.impl;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Implémentation d'une session de rendu, inspiré de
 * com.android.layoutlib.bridge.impl.RenderSessionImpl.
 * Utilise LayoutInflater natif + View.measure/layout.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class RenderSessionImpl {

    private final Context context;
    private final LayoutInflater layoutInflater;
    private View rootView;
    private int measuredWidth;
    private int measuredHeight;
    private long renderTimeMs;
    private boolean rendered;

    public RenderSessionImpl(Context context) {
        this.context = context;
        this.layoutInflater = LayoutInflater.from(context);
    }

    public void render(View root, int width, int height) {
        if (root == null) {
            throw new IllegalArgumentException("Vue racine null");
        }
        this.rootView = root;
        long start = System.nanoTime();
        root.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST));
        root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
        this.renderTimeMs = (System.nanoTime() - start) / 1_000_000;
        this.measuredWidth = root.getMeasuredWidth();
        this.measuredHeight = root.getMeasuredHeight();
        this.rendered = true;
    }

    public View getRootView() { return rootView; }
    public int getMeasuredWidth() { return measuredWidth; }
    public int getMeasuredHeight() { return measuredHeight; }
    public long getRenderTimeMs() { return renderTimeMs; }
    public boolean isRendered() { return rendered; }
    public LayoutInflater getLayoutInflater() { return layoutInflater; }
    public Context getContext() { return context; }

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
