package jo.layoutlib.attributes;

import android.content.Context;
import android.view.View;

import org.xmlpull.v1.XmlPullParser;

/**
 * Registre des attributs custom (styleables).
 *
 * <p>Cette interface est le contrat que doit implémenter le Module 5
 * (Attributes). Elle connaît tous les attributs déclarés via
 * {@code <declare-styleable>} et sait comment les appliquer aux vues.</p>
 *
 * <h2>Fonctionnalités attendues</h2>
 * <ul>
 *   <li>Parsing de {@code res/values/attrs.xml}</li>
 *   <li>Support des formats : {@code dimension}, {@code color},
 *       {@code reference}, {@code string}, {@code integer}, {@code float},
 *       {@code boolean}, {@code fraction}, {@code enum}, {@code flag}</li>
 *   <li>Application des attributs {@code app:*} (namespace custom)</li>
 *   <li>Mapping attribut → Caller (méthode d'application)</li>
 * </ul>
 *
 * <p>Référence layoutlib original :
 * {@code com.android.layoutlib.bridge.impl.BridgeContext.resolveStyleable()}.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public interface AttributeRegistry {

    /**
     * Enregistre un fichier {@code attrs.xml}.
     *
     * @param xml contenu du fichier attrs.xml
     */
    void registerAttrsFile(String xml);

    /**
     * Applique tous les attributs custom d'une vue.
     *
     * @param view    la vue à configurer
     * @param parser  le parser positionné sur le START_TAG de la vue
     * @param context le contexte Android
     */
    void applyCustomAttributes(View view, XmlPullParser parser, Context context);

    /**
     * Indique si un attribut custom est connu.
     *
     * @param attrName nom de l'attribut (ex. {@code cornerRadius})
     * @return {@code true} si l'attribut est déclaré dans un styleable
     */
    boolean isKnownAttribute(String attrName);

    /**
     * Récupère le format attendu d'un attribut.
     *
     * @param attrName nom de l'attribut
     * @return le format (ex. {@code dimension}, {@code color}), ou
     *         {@code null} si inconnu
     */
    String getAttributeFormat(String attrName);

    /**
     * Vide le registre.
     */
    void clear();
}
