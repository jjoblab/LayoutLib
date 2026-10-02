package jo.layoutlib.editor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitaire pour muter un XML de layout Android : modifier un attribut
 * d'une vue par son id, sans reparser tout le document.
 *
 * <p>Utilisé par PropertiesPanel pour wired l'édition des champs au XML :
 * quand l'utilisateur modifie "textSize" dans le panneau, on retrouve la vue
 * par son id et on met à jour l'attribut android:textSize dans le XML.</p>
 *
 * @author jo@Dev
 * @since 2.0
 */
public class XmlMutator {

    /**
     * Modifie un attribut d'une vue par son id.
     */
    public static String setAttributeById(String xml, String idName,
                                            String attrName, String newValue) {
        if (xml == null || idName == null || attrName == null) return xml;

        String fullAttrName = attrName.contains(":") ? attrName : "android:" + attrName;

        // Trouver l'id
        String idPattern = "android:id=\"@\\+id/" + Pattern.quote(idName) + "\"";
        Pattern p = Pattern.compile(idPattern);
        Matcher m = p.matcher(xml);
        if (!m.find()) {
            idPattern = "android:id=\"@id/" + Pattern.quote(idName) + "\"";
            p = Pattern.compile(idPattern);
            m = p.matcher(xml);
            if (!m.find()) return xml;
        }

        int idPos = m.start();
        int tagStart = xml.lastIndexOf('<', idPos);
        if (tagStart < 0) return xml;
        int tagEnd = findTagEnd(xml, tagStart);
        if (tagEnd < 0) return xml;

        String tagContent = xml.substring(tagStart, tagEnd + 1);
        String attrPattern = fullAttrName.replace(":", "\\:") + "=\"[^\"]*\"";
        Pattern attrP = Pattern.compile(attrPattern);
        Matcher attrM = attrP.matcher(tagContent);

        String newTagContent;
        if (attrM.find()) {
            newTagContent = attrM.replaceFirst(fullAttrName + "=\"" + escapeXml(newValue) + "\"");
        } else {
            newTagContent = insertAttribute(tagContent, fullAttrName, escapeXml(newValue));
        }
        return xml.substring(0, tagStart) + newTagContent + xml.substring(tagEnd + 1);
    }

    /**
     * Récupère la valeur d'un attribut d'une vue par son id.
     */
    public static String getAttributeById(String xml, String idName, String attrName) {
        if (xml == null || idName == null || attrName == null) return null;
        String fullAttrName = attrName.contains(":") ? attrName : "android:" + attrName;

        String idPattern = "android:id=\"@\\+id/" + Pattern.quote(idName) + "\"";
        Pattern p = Pattern.compile(idPattern);
        Matcher m = p.matcher(xml);
        if (!m.find()) {
            idPattern = "android:id=\"@id/" + Pattern.quote(idName) + "\"";
            p = Pattern.compile(idPattern);
            m = p.matcher(xml);
            if (!m.find()) return null;
        }

        int idPos = m.start();
        int tagStart = xml.lastIndexOf('<', idPos);
        if (tagStart < 0) return null;
        int tagEnd = findTagEnd(xml, tagStart);
        if (tagEnd < 0) return null;

        String tagContent = xml.substring(tagStart, tagEnd + 1);
        String attrPattern = fullAttrName.replace(":", "\\:") + "=\"([^\"]*)\"";
        Pattern attrP = Pattern.compile(attrPattern);
        Matcher attrM = attrP.matcher(tagContent);
        if (attrM.find()) return attrM.group(1);
        return null;
    }

    /**
     * Trouve l'offset du tag qui contient l'id donné.
     */
    public static int findTagOffsetForId(String xml, String idName) {
        if (xml == null || idName == null) return -1;
        Matcher m = Pattern.compile(
                "android:id=\"@\\+id/" + Pattern.quote(idName) + "\"").matcher(xml);
        if (!m.find()) {
            m = Pattern.compile(
                    "android:id=\"@id/" + Pattern.quote(idName) + "\"").matcher(xml);
            if (!m.find()) return -1;
        }
        return xml.lastIndexOf('<', m.start());
    }

    private static int findTagEnd(String xml, int tagStart) {
        boolean inString = false;
        char stringDelim = '"';
        for (int i = tagStart + 1; i < xml.length(); i++) {
            char c = xml.charAt(i);
            if (inString) {
                if (c == stringDelim) inString = false;
            } else {
                if (c == '"' || c == '\'') {
                    inString = true;
                    stringDelim = c;
                } else if (c == '>') return i;
            }
        }
        return -1;
    }

    private static String insertAttribute(String tagContent, String attrName, String attrValue) {
        int selfClose = tagContent.lastIndexOf("/>");
        if (selfClose >= 0 && selfClose == tagContent.length() - 2) {
            String before = trimEnd(tagContent.substring(0, selfClose));
            return before + " " + attrName + "=\"" + attrValue + "\" />";
        }
        int close = tagContent.lastIndexOf('>');
        if (close >= 0) {
            String before = trimEnd(tagContent.substring(0, close));
            return before + " " + attrName + "=\"" + attrValue + "\" >";
        }
        return tagContent;
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String trimEnd(String s) {
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) end--;
        return s.substring(0, end);
    }
}
