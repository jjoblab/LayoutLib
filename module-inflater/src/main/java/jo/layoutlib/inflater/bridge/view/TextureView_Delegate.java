package jo.layoutlib.inflater.bridge.view;

import android.view.TextureView;
import android.view.View;

/**
 * Delegate pour TextureView_Delegate, utilise l API Android native.
 * Inspiré de com.android.layoutlib.bridge.android.view.TextureView_Delegate de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class TextureView_Delegate {

    public TextureView_Delegate() {
    }

    public static void setOpaque(TextureView tv, boolean opaque) {
        tv.setOpaque(opaque);
    }

    public static boolean isOpaque(TextureView tv) {
        return tv.isOpaque();
    }
}
