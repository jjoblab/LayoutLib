package jo.layoutlib.layout.session;

import android.view.View;

/**
 * Session de rendu publique, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class RenderSession {

    private final RenderSessionImpl impl;

    public RenderSession() {
        this.impl = new RenderSessionImpl();
    }

    public RenderResult render(View root, int width, int height) {
        try {
            impl.render(root, width, height);
            return new RenderResult(true, impl.getRenderTimeMs(),
                    impl.getViewCount(), impl.getMeasuredWidth(), impl.getMeasuredHeight(), null);
        } catch (Exception e) {
            return new RenderResult(false, 0, 0, 0, 0, e.getMessage());
        }
    }

    public RenderSessionImpl getImpl() {
        return impl;
    }
}
