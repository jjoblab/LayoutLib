package jo.layoutlib.resources.api;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.TypedValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Résolveur de resources en cascade, inspiré de com.android.ide.common.rendering.api.RenderResources.
 * Utilise android.content.res.Resources.Theme natif pour la résolution.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class RenderResources {

    private final Context context;
    private final Resources.Theme theme;
    private final List<Object> styleStack = new ArrayList<>();

    public RenderResources(Context context) {
        this.context = context;
        this.theme = context != null ? context.getTheme() : null;
    }

    public void pushStyle(Object style) {
        if (style != null) styleStack.add(style);
    }

    public Object popStyle() {
        return styleStack.isEmpty() ? null : styleStack.remove(styleStack.size() - 1);
    }

    public void clearStyles() {
        styleStack.clear();
    }

    public int getStyleStackDepth() {
        return styleStack.size();
    }

    /**
     * Résout un attribut de thème color.
     *
     * @param attrId l'id de l'attribut (ex. android.R.attr.colorPrimary)
     * @return la valeur ARGB, ou 0 si non défini
     */
    public int resolveColor(int attrId) {
        if (theme == null || attrId == 0) return 0;
        TypedValue value = new TypedValue();
        if (theme.resolveAttribute(attrId, value, true)) {
            if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                    && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return value.data;
            }
            if (value.resourceId != 0) {
                return context.getResources().getColor(value.resourceId, theme);
            }
        }
        return 0;
    }

    /**
     * Résout un attribut de thème dimension.
     *
     * @param attrId l'id de l'attribut
     * @return la valeur en pixels, ou 0
     */
    public float resolveDimension(int attrId) {
        if (theme == null || attrId == 0) return 0;
        TypedValue value = new TypedValue();
        if (theme.resolveAttribute(attrId, value, true)) {
            if (value.type == TypedValue.TYPE_DIMENSION) {
                return TypedValue.applyDimension(
                        value.data >> 4, value.data & 0xF,
                        context.getResources().getDisplayMetrics());
            }
            if (value.resourceId != 0) {
                return context.getResources().getDimension(value.resourceId);
            }
        }
        return 0;
    }

    /**
     * Résout un attribut de thème string.
     *
     * @param attrId l'id de l'attribut
     * @return la valeur, ou null
     */
    public String resolveString(int attrId) {
        if (theme == null || attrId == 0) return null;
        TypedValue value = new TypedValue();
        if (theme.resolveAttribute(attrId, value, true)) {
            return value.string != null ? value.string.toString() : null;
        }
        return null;
    }

    public Resources.Theme getTheme() {
        return theme;
    }

    public Context getContext() {
        return context;
    }

    public TypedArray obtainStyledAttributes(int[] attrs) {
        return context.obtainStyledAttributes(attrs);
    }
}
