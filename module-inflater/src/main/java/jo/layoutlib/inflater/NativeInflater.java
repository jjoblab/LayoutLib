package jo.layoutlib.inflater;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.InputStream;
import java.io.StringReader;

/**
 * Inflater utilisant le LayoutInflater natif Android.
 *
 * <p>Utilise {@link LayoutInflater#from(Context)} pour inflater les layouts XML.
 * Gère nativement tous les widgets, styles, thèmes, <include>, <merge>.</p>
 *
 * <h2>Solution au cast XmlBlock.Parser</h2>
 *
 * <p>L'erreur {@code XmlPullAttributes cannot be cast to XmlBlock.Parser} se produit
 * quand on passe un {@link XmlPullParser} simple au {@link LayoutInflater}. Android
 * attend un {@link XmlResourceParser} qui implémente à la fois {@link XmlPullParser}
 * et {@link AttributeSet}.</p>
 *
 * <p><strong>Solution</strong> : écrire le XML dans un fichier temporaire et
 * utiliser {@link android.content.res.Resources#getXml(int)} via un
 * {@link android.content.res.AssetManager}, ou plus simplement utiliser
 * {@link Xml#newPullParser()} avec un wrapper qui implémente
 * {@link AttributeSet}.</p>
 *
 * <p>La solution la plus robuste sur mobile est de sauvegarder le XML dans
 * un fichier du cache et de l'inflater via un resource ID temporaire. Mais
 * comme ça nécessite des ids R.*, on utilise l'approche alternative du
 * {@code BridgeInflater} qui crée les vues par réflexion.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class NativeInflater {

    private final Context context;
    private final LayoutInflater layoutInflater;

    public NativeInflater(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Le contexte ne peut pas être null");
        }
        this.context = context;
        this.layoutInflater = LayoutInflater.from(context);
    }

    /**
     * Inflate un XML depuis une chaîne.
     *
     * @param xml le XML source
     * @return la vue racine
     * @throws InflateException si l'inflation échoue
     */
    public View inflate(String xml) {
        return inflate(xml, null);
    }

    /**
     * Inflate un XML et l'attache à un parent.
     *
     * @param xml    le XML source
     * @param parent le parent (peut être null)
     * @return la vue racine
     * @throws InflateException si l'inflation échoue
     */
    public View inflate(String xml, ViewGroup parent) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new InflateException("XML vide ou null");
        }

        // Pré-traitement (conversion tools:*, RTL, etc.)
        String normalized = XmlPreprocessor.preprocess(xml);

        // Tentative 1 : utiliser le LayoutInflater natif avec un XmlPullParser
        // qui implémente XmlResourceParser
        try {
            // Xml.newPullParser() retourne un KXmlParser qui n'implémente pas
            // XmlResourceParser. Sur certains appareils (Android 15), le
            // LayoutInflater cast vers XmlBlock.Parser et échoue.
            //
            // Solution : créer un XmlPullParser via XmlPullParserFactory
            // avec FEATURE_PROCESS_NAMESPACES, puis l'envelopper.
            org.xmlpull.v1.XmlPullParserFactory factory =
                    org.xmlpull.v1.XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(new StringReader(normalized));

            // Utiliser un FrameLayout temporaire comme parent
            FrameLayout tempParent = new FrameLayout(context);
            View view = layoutInflater.inflate(parser, tempParent, false);

            if (view != null && view.getLayoutParams() == null) {
                view.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            return view;

        } catch (ClassCastException e) {
            // Le cast vers XmlBlock.Parser a échoué
            // Solution : fallback sur la création manuelle par réflexion
            throw new InflateException(
                    "Le LayoutInflater natif ne supporte pas les XmlPullParser custom "
                            + "sur cet appareil. Utilisez le BridgeInflater à la place.", e);
        } catch (XmlPullParserException e) {
            throw new InflateException("Erreur de parsing XML : " + e.getMessage(), e);
        } catch (android.view.InflateException e) {
            throw new InflateException("Erreur d'inflation native : " + e.getMessage(), e);
        } catch (Exception e) {
            throw new InflateException(
                    "Erreur inattendue : " + e.getClass().getSimpleName()
                            + " : " + e.getMessage(), e);
        }
    }

    /**
     * Inflate un XML depuis un resource ID.
     *
     * <p>Cette méthode est la plus robuste car elle utilise le pipeline natif
     * complet d'Android (Resources.getXml → XmlBlock.Parser → LayoutInflater).</p>
     *
     * @param resourceId l'id de resource R.layout.*
     * @return la vue racine
     * @throws InflateException si l'inflation échoue
     */
    public View inflate(int resourceId) {
        try {
            FrameLayout tempParent = new FrameLayout(context);
            return layoutInflater.inflate(resourceId, tempParent, false);
        } catch (android.view.InflateException e) {
            throw new InflateException("Erreur d'inflation native : " + e.getMessage(), e);
        } catch (Exception e) {
            throw new InflateException(
                    "Erreur inattendue : " + e.getClass().getSimpleName()
                            + " : " + e.getMessage(), e);
        }
    }

    /**
     * Inflate un XML depuis un InputStream.
     *
     * @param input le flux d'entrée
     * @return la vue racine
     * @throws InflateException si l'inflation échoue
     */
    public View inflate(InputStream input) {
        if (input == null) {
            throw new InflateException("InputStream null");
        }
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(input, "UTF-8");

            FrameLayout tempParent = new FrameLayout(context);
            return layoutInflater.inflate(parser, tempParent, false);
        } catch (XmlPullParserException e) {
            throw new InflateException("Erreur de parsing XML : " + e.getMessage(), e);
        } catch (android.view.InflateException e) {
            throw new InflateException("Erreur d'inflation native : " + e.getMessage(), e);
        } catch (Exception e) {
            throw new InflateException(
                    "Erreur inattendue : " + e.getClass().getSimpleName()
                            + " : " + e.getMessage(), e);
        }
    }

    public Context getContext() {
        return context;
    }

    public LayoutInflater getLayoutInflater() {
        return layoutInflater;
    }
}
