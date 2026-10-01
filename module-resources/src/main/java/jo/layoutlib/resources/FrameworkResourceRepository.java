package jo.layoutlib.resources;

import jo.layoutlib.resources.api.ResourceNamespace;

/**
 * Repository de resources du framework Android (namespace android).
 *
 * @author jo@Dev
 * @since 1.0
 */
public class FrameworkResourceRepository extends ProjectResourceRepository {

    public FrameworkResourceRepository(java.io.File resFolder) {
        super(resFolder, ResourceNamespace.ANDROID);
    }
}
