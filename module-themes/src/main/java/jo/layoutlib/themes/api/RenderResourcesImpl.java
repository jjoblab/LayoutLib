package jo.layoutlib.themes.api;

import android.content.Context;
import jo.layoutlib.resources.api.RenderResources;

/**
 * Implémentation de RenderResources pour le module themes.
 * Délègue au RenderResources du module resources.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class RenderResourcesImpl extends RenderResources {

    public RenderResourcesImpl(Context context) {
        super(context);
    }
}
