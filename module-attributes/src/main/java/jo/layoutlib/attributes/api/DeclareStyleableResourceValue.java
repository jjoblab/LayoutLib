package jo.layoutlib.attributes.api;

import java.util.List;

/**
 * Représente un <declare-styleable>, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public interface DeclareStyleableResourceValue {

    String getName();

    List<AttrResourceValueImpl> getAttributes();

    int getAttributeCount();
}
