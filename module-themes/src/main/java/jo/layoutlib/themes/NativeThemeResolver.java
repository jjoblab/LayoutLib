package jo.layoutlib.themes;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.TypedValue;

/**
 * Résolveur de thèmes utilisant l'API Android native.
 *
 * <p>Cette classe utilise directement {@link Resources.Theme} et
 * {@link TypedArray} pour résoudre les attributs de thème nativement.
 * C'est beaucoup plus précis que l'implémentation manuelle car Android
 * gère l'héritage des thèmes, les qualifiers (night, land, etc.) et
 * la résolution des références chainables automatiquement.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class NativeThemeResolver {

    /** Contexte Android. */
    private final Context context;

    /** Theme Android natif. */
    private final Resources.Theme theme;

    /**
     * Construit un résolveur natif.
     *
     * @param context le contexte Android
     */
    public NativeThemeResolver(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Le contexte ne peut pas être null");
        }
        this.context = context;
        this.theme = context.getTheme();
    }

    /**
     * Résout un attribut de thème color.
     *
     * @param attrId l'id de l'attribut (ex. android.R.attr.colorPrimary)
     * @return la valeur ARGB, ou null si non définie
     */
    public Integer resolveColorAttr(int attrId) {
        if (attrId == 0) return null;
        TypedValue value = new TypedValue();
        if (!theme.resolveAttribute(attrId, value, true)) {
            return null;
        }
        if (value.type == TypedValue.TYPE_INT_COLOR_ARGB8
                || value.type == TypedValue.TYPE_INT_COLOR_ARGB4
                || value.type == TypedValue.TYPE_INT_COLOR_RGB8
                || value.type == TypedValue.TYPE_INT_COLOR_RGB4) {
            return value.data;
        }
        if (value.type == TypedValue.TYPE_STRING && value.string != null) {
            // Référence @color/foo — tente de résoudre
            return resolveColorByName(value.string.toString());
        }
        if (value.resourceId != 0) {
            try {
                return context.getResources().getColor(value.resourceId, theme);
            } catch (Resources.NotFoundException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Résout un attribut de thème dimension.
     *
     * @param attrId l'id de l'attribut
     * @return la valeur en pixels, ou null si non définie
     */
    public Float resolveDimensionAttr(int attrId) {
        if (attrId == 0) return null;
        TypedValue value = new TypedValue();
        if (!theme.resolveAttribute(attrId, value, true)) {
            return null;
        }
        if (value.type == TypedValue.TYPE_DIMENSION) {
            return TypedValue.applyDimension(
                    value.data >> 4,
                    value.data & 0xF,
                    context.getResources().getDisplayMetrics());
        }
        if (value.resourceId != 0) {
            try {
                return context.getResources().getDimension(value.resourceId);
            } catch (Resources.NotFoundException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Résout un attribut de thème entier.
     *
     * @param attrId l'id de l'attribut
     * @return la valeur, ou null si non définie
     */
    public Integer resolveIntAttr(int attrId) {
        if (attrId == 0) return null;
        TypedValue value = new TypedValue();
        if (!theme.resolveAttribute(attrId, value, true)) {
            return null;
        }
        if (value.type == TypedValue.TYPE_INT_DEC
                || value.type == TypedValue.TYPE_INT_HEX) {
            return value.data;
        }
        return null;
    }

    /**
     * Résout un attribut de thème booléen.
     *
     * @param attrId l'id de l'attribut
     * @return la valeur, ou null si non définie
     */
    public Boolean resolveBooleanAttr(int attrId) {
        if (attrId == 0) return null;
        TypedValue value = new TypedValue();
        if (!theme.resolveAttribute(attrId, value, true)) {
            return null;
        }
        if (value.type == TypedValue.TYPE_INT_BOOLEAN) {
            return value.data != 0;
        }
        return null;
    }

    /**
     * Résout un attribut de thème chaîne.
     *
     * @param attrId l'id de l'attribut
     * @return la valeur, ou null si non définie
     */
    public String resolveStringAttr(int attrId) {
        if (attrId == 0) return null;
        TypedValue value = new TypedValue();
        if (!theme.resolveAttribute(attrId, value, true)) {
            return null;
        }
        return value.string != null ? value.string.toString() : null;
    }

    /**
     * Résout un attribut de thème drawable.
     *
     * @param attrId l'id de l'attribut
     * @return le drawable, ou null si non défini
     */
    public android.graphics.drawable.Drawable resolveDrawableAttr(int attrId) {
        if (attrId == 0) return null;
        TypedValue value = new TypedValue();
        if (!theme.resolveAttribute(attrId, value, true)) {
            return null;
        }
        if (value.resourceId != 0) {
            try {
                return context.getResources().getDrawable(value.resourceId, theme);
            } catch (Resources.NotFoundException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Récupère plusieurs attributs d'un coup via TypedArray.
     *
     * @param attrs les ids d'attributs à résoudre
     * @return le TypedArray rempli, à recycler par l'appelant
     */
    public TypedArray obtainStyledAttributes(int[] attrs) {
        return context.obtainStyledAttributes(attrs);
    }

    /**
     * Récupère un attribut par nom.
     *
     * @param attrName le nom de l'attribut (ex. "colorPrimary")
     * @return l'id, ou 0 si introuvable
     */
    public int getAttrId(String attrName) {
        if (attrName == null) return 0;
        // Tente le framework
        int id = context.getResources().getIdentifier(attrName, "attr", "android");
        if (id != 0) return id;
        // Tente le package local
        return context.getResources().getIdentifier(attrName, "attr", context.getPackageName());
    }

    /**
     * Résout une couleur par nom d'attribut.
     *
     * @param attrName le nom de l'attribut
     * @return la valeur ARGB, ou null
     */
    public Integer resolveColorAttrByName(String attrName) {
        int id = getAttrId(attrName);
        return id != 0 ? resolveColorAttr(id) : null;
    }

    /**
     * Résout une couleur depuis une référence @color/foo.
     *
     * @param ref la référence
     * @return la valeur, ou null
     */
    private Integer resolveColorByName(String ref) {
        if (ref == null) return null;
        String name;
        if (ref.startsWith("@color/")) {
            name = ref.substring("@color/".length());
        } else if (ref.startsWith("@android:color/")) {
            name = ref.substring("@android:color/".length());
        } else {
            return null;
        }
        int id;
        if (ref.startsWith("@android:")) {
            id = context.getResources().getIdentifier(name, "color", "android");
        } else {
            id = context.getResources().getIdentifier(name, "color", context.getPackageName());
        }
        if (id == 0) return null;
        try {
            return context.getResources().getColor(id, theme);
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    /**
     * @return le contexte
     */
    public Context getContext() {
        return context;
    }

    /**
     * @return le theme natif
     */
    public Resources.Theme getTheme() {
        return theme;
    }
}
