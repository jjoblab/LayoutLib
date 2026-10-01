package jo.layoutlib.themes.api;

import jo.layoutlib.resources.api.ResourceNamespace;
import jo.layoutlib.resources.api.ResourceType;
import jo.layoutlib.resources.api.ResourceValueImpl;

/**
 * Représente un <item> dans un <style>.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class StyleItemResourceValueImpl extends ResourceValueImpl {

    private final String styleName;

    public StyleItemResourceValueImpl(ResourceNamespace namespace, String styleName,
                                       String itemName, String value) {
        super(namespace, ResourceType.ATTR, itemName, value);
        this.styleName = styleName;
    }

    public String getStyleName() {
        return styleName;
    }
}
