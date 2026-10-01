package jo.layoutlib.layout.session;

/**
 * Implémentation de LayoutlibCallback, inspiré de l'AOSP.
 * Permet au layoutlib de communiquer avec le client.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class LayoutlibCallbackImpl {

    private OnResourceResolveListener resourceListener;
    private OnViewCreatedListener viewListener;

    public interface OnResourceResolveListener {
        int getResourceId(String type, String name);
    }

    public interface OnViewCreatedListener {
        void onViewCreated(Object view, Object cookie);
    }

    public LayoutlibCallbackImpl() {
    }

    public void setOnResourceResolveListener(OnResourceResolveListener listener) {
        this.resourceListener = listener;
    }

    public void setOnViewCreatedListener(OnViewCreatedListener listener) {
        this.viewListener = listener;
    }

    public int getOrGenerateResourceId(String type, String name) {
        if (resourceListener != null) {
            return resourceListener.getResourceId(type, name);
        }
        return 0;
    }

    public void onViewCreated(Object view, Object cookie) {
        if (viewListener != null) {
            viewListener.onViewCreated(view, cookie);
        }
    }
}
