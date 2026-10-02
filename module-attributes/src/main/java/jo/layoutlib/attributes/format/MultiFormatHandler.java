package jo.layoutlib.attributes.format;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

/**
 * Gestionnaire multi-format, utilise l API Android native.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class MultiFormatHandler {

    /** Tag de journalisation. */
    private static final String TAG = "MultiFormatHandler";

    /**
     * Applique un attribut sur une vue.
     *
     * @param view la vue cible
     * @param attrName le nom de l attribut
     * @param attrValue la valeur
     * @return true si l attribut a été appliqué
     */
    public boolean apply(View view, String attrName, String attrValue) {
        if (view == null || attrName == null || attrValue == null) {
            return false;
        }
        // Délègue aux setters natifs d Android
        switch (attrName) {
            case "visibility":
                if ("gone".equals(attrValue)) view.setVisibility(View.GONE);
                else if ("invisible".equals(attrValue)) view.setVisibility(View.INVISIBLE);
                else view.setVisibility(View.VISIBLE);
                return true;
            case "enabled":
                view.setEnabled("true".equals(attrValue));
                return true;
            case "clickable":
                view.setClickable("true".equals(attrValue));
                return true;
            case "focusable":
                view.setFocusable("true".equals(attrValue));
                return true;
            case "selected":
                view.setSelected("true".equals(attrValue));
                return true;
            case "alpha":
                try { view.setAlpha(Float.parseFloat(attrValue)); } catch (NumberFormatException e) { Log.w(TAG, "Valeur '" + attrValue + "' invalide, ignorée : " + e.getMessage()); }
                return true;
            case "tag":
                view.setTag(attrValue);
                return true;
            default:
                return false;
        }
    }
}
