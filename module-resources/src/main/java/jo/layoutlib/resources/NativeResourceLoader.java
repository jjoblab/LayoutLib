package jo.layoutlib.resources;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Chargeur de resources utilisant l'API Android native.
 *
 * <p>Cette classe utilise directement {@link Resources} pour charger les
 * resources Android réelles du projet (couleurs, chaînes, dimensions, etc.)
 * via leur id R.color.*, R.string.*, R.dimen.*.</p>
 *
 * <h2>Avantages vs ResourceResolverImpl</h2>
 * <ul>
 *   <li>Accès direct aux resources compilées (plus rapide)</li>
 *   <li>Support automatique des qualifiers (night, land, v31, etc.)</li>
 *   <li>Support des références chainables natif</li>
 *   <li>Support des styles et thèmes</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class NativeResourceLoader {

    /** Contexte Android. */
    private final Context context;

    /** Resources Android. */
    private final Resources resources;

    /** Package name pour la résolution. */
    private final String packageName;

    /**
     * Construit un loader natif.
     *
     * @param context le contexte Android
     */
    public NativeResourceLoader(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Le contexte ne peut pas être null");
        }
        this.context = context;
        this.resources = context.getResources();
        this.packageName = context.getPackageName();
    }

    /**
     * Récupère une couleur par nom.
     *
     * @param name le nom de la couleur (ex. "primary")
     * @return la valeur ARGB, ou null si introuvable
     */
    public Integer getColor(String name) {
        if (name == null) return null;
        int id = resources.getIdentifier(name, "color", packageName);
        if (id == 0) {
            // Tente le framework
            id = resources.getIdentifier(name, "color", "android");
        }
        if (id == 0) return null;
        try {
            return resources.getColor(id, context.getTheme());
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    /**
     * Récupère une chaîne par nom.
     *
     * @param name le nom de la chaîne
     * @return la valeur, ou null si introuvable
     */
    public String getString(String name) {
        if (name == null) return null;
        int id = resources.getIdentifier(name, "string", packageName);
        if (id == 0) {
            id = resources.getIdentifier(name, "string", "android");
        }
        if (id == 0) return null;
        try {
            return resources.getString(id);
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    /**
     * Récupère une dimension par nom (en pixels).
     *
     * @param name le nom de la dimension
     * @return la valeur en pixels, ou null si introuvable
     */
    public Float getDimension(String name) {
        if (name == null) return null;
        int id = resources.getIdentifier(name, "dimen", packageName);
        if (id == 0) {
            id = resources.getIdentifier(name, "dimen", "android");
        }
        if (id == 0) return null;
        try {
            return resources.getDimension(id);
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    /**
     * Récupère un entier par nom.
     *
     * @param name le nom de l'entier
     * @return la valeur, ou null si introuvable
     */
    public Integer getInteger(String name) {
        if (name == null) return null;
        int id = resources.getIdentifier(name, "integer", packageName);
        if (id == 0) {
            id = resources.getIdentifier(name, "integer", "android");
        }
        if (id == 0) return null;
        try {
            return resources.getInteger(id);
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    /**
     * Récupère un booléen par nom.
     *
     * @param name le nom du booléen
     * @return la valeur, ou null si introuvable
     */
    public Boolean getBoolean(String name) {
        if (name == null) return null;
        int id = resources.getIdentifier(name, "bool", packageName);
        if (id == 0) {
            id = resources.getIdentifier(name, "bool", "android");
        }
        if (id == 0) return null;
        try {
            return resources.getBoolean(id);
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    /**
     * Récupère un drawable par nom.
     *
     * @param name le nom du drawable
     * @return le drawable, ou null si introuvable
     */
    public android.graphics.drawable.Drawable getDrawable(String name) {
        if (name == null) return null;
        int id = resources.getIdentifier(name, "drawable", packageName);
        if (id == 0) {
            id = resources.getIdentifier(name, "drawable", "android");
        }
        if (id == 0) return null;
        try {
            return resources.getDrawable(id, context.getTheme());
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    /**
     * Récupère l'id d'une resource par nom et type.
     *
     * @param name le nom de la resource
     * @param type le type (color, string, drawable, etc.)
     * @return l'id, ou 0 si introuvable
     */
    public int getResourceId(String name, String type) {
        if (name == null || type == null) return 0;
        int id = resources.getIdentifier(name, type, packageName);
        if (id == 0) {
            id = resources.getIdentifier(name, type, "android");
        }
        return id;
    }

    /**
     * Récupère le nom d'une resource par id.
     *
     * @param id l'id de la resource
     * @return le nom, ou null
     */
    public String getResourceName(int id) {
        if (id == 0) return null;
        try {
            return resources.getResourceEntryName(id);
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    /**
     * @return les Resources Android
     */
    public Resources getResources() {
        return resources;
    }

    /**
     * @return le contexte
     */
    public Context getContext() {
        return context;
    }

    /**
     * @return le nom du package
     */
    public String getPackageName() {
        return packageName;
    }
}
