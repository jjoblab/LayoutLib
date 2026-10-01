package jo.layoutlib.inflater;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;

import org.xmlpull.v1.XmlPullParser;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Fabrique chargée d'instancier les vues Android par réflexion.
 *
 * <p><strong>CRITIQUE</strong> : utilise le constructeur {@code (Context, AttributeSet)}
 * en priorité pour que les vues reçoivent leurs attributs XML (text, textColor,
 * background, orientation, gravity, etc.) via {@code obtainStyledAttributes()}.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ViewFactory {

    private final Context context;
    private final ViewTagRegistry tagRegistry;
    private final Map<String, Constructor<?>> constructorCache = new HashMap<>();

    public ViewFactory(Context context, ViewTagRegistry tagRegistry) {
        if (context == null) {
            throw new IllegalArgumentException("Le contexte ne peut pas être null");
        }
        if (tagRegistry == null) {
            throw new IllegalArgumentException("Le registre des tags ne peut pas être null");
        }
        this.context = context;
        this.tagRegistry = tagRegistry;
    }

    /**
     * Crée une vue à partir d'un nom de tag XML, SANS attributs.
     *
     * @param tag nom du tag XML
     * @return la vue instanciée
     * @throws InflateException si la création échoue
     */
    public View createView(String tag) {
        return createView(tag, null);
    }

    /**
     * Crée une vue à partir d'un nom de tag XML ET d'un AttributeSet.
     *
     * <p>Utilise le constructeur {@code (Context, AttributeSet)} en priorité
     * pour que la vue lise ses attributs XML nativement via
     * {@code obtainStyledAttributes(attrs, R.styleable.*)}.</p>
     *
     * @param tag   nom du tag XML
     * @param attrs les attributs XML (peut être null)
     * @return la vue instanciée
     * @throws InflateException si la création échoue
     */
    public View createView(String tag, AttributeSet attrs) {
        String className = tagRegistry.resolveClassName(tag);
        if (className == null) {
            throw new InflateException(
                    "Aucune classe associée au tag '" + tag + "' (tag spécial ?)");
        }
        return createViewByName(className, attrs);
    }

    /**
     * Crée une vue à partir d'un nom de classe, SANS attributs.
     *
     * @param className nom pleinement qualifié
     * @return la vue instanciée
     */
    public View createViewByName(String className) {
        return createViewByName(className, null);
    }

    /**
     * Crée une vue à partir d'un nom de classe ET d'un AttributeSet.
     *
     * @param className nom pleinement qualifié
     * @param attrs     les attributs XML (peut être null)
     * @return la vue instanciée
     */
    public View createViewByName(String className, AttributeSet attrs) {
        Constructor<?> constructor = constructorCache.get(className + (attrs != null ? "_attrs" : ""));
        if (constructor == null) {
            constructor = resolveConstructor(className, attrs != null);
            constructorCache.put(className + (attrs != null ? "_attrs" : ""), constructor);
        }
        try {
            if (attrs != null && constructor.getParameterCount() == 2) {
                return (View) constructor.newInstance(context, attrs);
            } else {
                return (View) constructor.newInstance(context);
            }
        } catch (InstantiationException e) {
            throw new InflateException(
                    "Impossible d'instancier : " + className, e);
        } catch (IllegalAccessException e) {
            throw new InflateException(
                    "Constructeur inaccessible : " + className, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw new InflateException(
                    "Exception du constructeur de " + className + " : "
                            + (cause != null ? cause.getMessage() : "inconnue"), e);
        } catch (ClassCastException e) {
            throw new InflateException(
                    className + " n'hérite pas de View", e);
        }
    }

    /**
     * Résout le constructeur à utiliser.
     *
     * <p><strong>Ordre prioritaire</strong> :</p>
     * <ol>
     *   <li>Si attrs != null : {@code (Context, AttributeSet)} en premier</li>
     *   <li>Sinon : {@code (Context)}</li>
     *   <li>Fallback : {@code (Context, AttributeSet)}</li>
     *   <li>Dernier recours : {@code ()}</li>
     * </ol>
     *
     * @param className nom de la classe
     * @param hasAttrs  true si on a un AttributeSet à passer
     * @return le constructeur trouvé
     */
    private Constructor<?> resolveConstructor(String className, boolean hasAttrs) {
        Class<?> clazz;
        try {
            clazz = Class.forName(className, false, context.getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new InflateException("Classe introuvable : " + className, e);
        }

        // 1. Si on a des attrs, prioriser (Context, AttributeSet)
        if (hasAttrs) {
            try {
                return clazz.getConstructor(Context.class, AttributeSet.class);
            } catch (NoSuchMethodException ignored) {
            }
        }

        // 2. (Context)
        try {
            return clazz.getConstructor(Context.class);
        } catch (NoSuchMethodException ignored) {
        }

        // 3. (Context, AttributeSet) même si pas d'attrs
        try {
            return clazz.getConstructor(Context.class, AttributeSet.class);
        } catch (NoSuchMethodException ignored) {
        }

        // 4. () par défaut
        try {
            return clazz.getConstructor();
        } catch (NoSuchMethodException ignored) {
        }

        throw new InflateException(
                "Aucun constructeur (Context), (Context, AttributeSet) ou () trouvé pour : "
                        + className);
    }

    /**
     * Crée un AttributeSet depuis un XmlPullParser.
     *
     * <p>Le parser doit être positionné sur START_TAG.</p>
     *
     * @param parser le parser
     * @return l'AttributeSet
     */
    public static AttributeSet createAttributeSet(XmlPullParser parser) {
        return Xml.asAttributeSet(parser);
    }

    public boolean canCreate(String className) {
        try {
            Class<?> clazz = Class.forName(className, false,
                    context.getClass().getClassLoader());
            return View.class.isAssignableFrom(clazz);
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    public void clearCache() {
        constructorCache.clear();
    }
}
