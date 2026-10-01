package jo.layoutlib.drawables;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Chargeur de drawables utilisant l'API Android native.
 *
 * <p>Cette classe utilise directement {@link Resources#getDrawable(int, Resources.Theme)}
 * et {@link Drawable#createFromXml(Resources, XmlPullParser)} pour charger les
 * drawables nativement, avec support complet de :</p>
 *
 * <ul>
 *   <li>Tous les types XML (shape, selector, vector, layer-list, ripple, etc.)</li>
 *   <li>Drawables images (PNG, WebP, JPEG)</li>
 *   <li>Références @drawable/foo et @android:drawable/foo</li>
 *   <li>Qualifiers (night, land, density)</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class NativeDrawableLoader {

    /** Contexte Android. */
    private final Context context;

    /** Resources Android. */
    private final Resources resources;

    /**
     * Construit un loader natif.
     *
     * @param context le contexte Android
     */
    public NativeDrawableLoader(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Le contexte ne peut pas être null");
        }
        this.context = context;
        this.resources = context.getResources();
    }

    /**
     * Charge un drawable depuis une référence @drawable/foo.
     *
     * @param reference la référence
     * @return le drawable, ou null si introuvable
     */
    public Drawable loadFromReference(String reference) {
        if (reference == null || reference.isEmpty()) return null;

        // Couleur littérale
        if (reference.startsWith("#")) {
            try {
                int color = jo.layoutlib.resources.ColorParser.parse(reference);
                return new android.graphics.drawable.ColorDrawable(color);
            } catch (Exception e) {
                return null;
            }
        }

        // Référence @drawable/foo ou @android:drawable/foo
        String name;
        boolean framework = false;
        if (reference.startsWith("@android:drawable/")) {
            name = reference.substring("@android:drawable/".length());
            framework = true;
        } else if (reference.startsWith("@drawable/")) {
            name = reference.substring("@drawable/".length());
        } else {
            return null;
        }

        int id;
        if (framework) {
            id = resources.getIdentifier(name, "drawable", "android");
        } else {
            id = resources.getIdentifier(name, "drawable", context.getPackageName());
        }
        if (id == 0) return null;

        try {
            return resources.getDrawable(id, context.getTheme());
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    /**
     * Charge un drawable depuis un XML inline.
     *
     * @param xml le XML du drawable
     * @return le drawable, ou null si invalide
     */
    public Drawable loadFromXml(String xml) {
        if (xml == null || xml.trim().isEmpty()) return null;
        try {
            XmlPullParser parser = android.util.Xml.newPullParser();
            parser.setInput(new java.io.StringReader(xml));
            return Drawable.createFromXml(resources, parser);
        } catch (XmlPullParserException | IOException | Resources.NotFoundException e) {
            return null;
        }
    }

    /**
     * Charge un drawable par id.
     *
     * @param id l'id du drawable
     * @return le drawable, ou null
     */
    public Drawable loadById(int id) {
        if (id == 0) return null;
        try {
            return resources.getDrawable(id, context.getTheme());
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    /**
     * Charge un drawable par nom.
     *
     * @param name le nom du drawable
     * @return le drawable, ou null
     */
    public Drawable loadByName(String name) {
        if (name == null) return null;
        int id = resources.getIdentifier(name, "drawable", context.getPackageName());
        if (id == 0) {
            id = resources.getIdentifier(name, "drawable", "android");
        }
        return loadById(id);
    }

    /**
     * @return le contexte
     */
    public Context getContext() {
        return context;
    }

    /**
     * @return les resources
     */
    public Resources getResources() {
        return resources;
    }
}
