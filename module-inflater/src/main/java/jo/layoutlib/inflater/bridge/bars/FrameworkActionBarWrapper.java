package jo.layoutlib.inflater.bridge.bars;

/**
 * Wrapper pour FrameworkActionBar, utilise l API Android native.
 * Inspiré de com.android.layoutlib.bridge.bars.FrameworkActionBarWrapper de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class FrameworkActionBarWrapper {

    
    

    public FrameworkActionBarWrapper() {
    }

    public static FrameworkActionBar wrap() {
        return new FrameworkActionBar();
    }
}
