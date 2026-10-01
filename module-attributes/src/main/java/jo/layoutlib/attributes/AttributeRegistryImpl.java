package jo.layoutlib.attributes;

import android.content.Context;
import android.view.View;

import org.xmlpull.v1.XmlPullParser;

import java.util.HashMap;
import java.util.Map;

import jo.layoutlib.attributes.api.AttributeDefinitionImpl;
import jo.layoutlib.attributes.format.FormatRegistry;
import jo.layoutlib.attributes.registry.FrameworkAttributeRegistry;
import jo.layoutlib.attributes.registry.MaterialAttributeRegistry;
import jo.layoutlib.attributes.registry.AndroidXAttributeRegistry;
import jo.layoutlib.attributes.registry.CompositeAttributeRegistry;
import jo.layoutlib.resources.DimensionConverter;

/**
 * Implémentation principale du AttributeRegistry.
 * Utilise les registres natifs Framework, Material et AndroidX.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class AttributeRegistryImpl implements AttributeRegistry {

    private final Map<String, AttributeDefinitionImpl> attributes = new HashMap<>();
    private final Map<String, java.util.List<String>> styleables = new HashMap<>();
    private final CompositeAttributeRegistry compositeRegistry;

    public AttributeRegistryImpl() {
        this(null);
    }

    public AttributeRegistryImpl(DimensionConverter converter) {
        this.compositeRegistry = new CompositeAttributeRegistry();
        this.compositeRegistry.addRegistry(new FrameworkAttributeRegistry());
        this.compositeRegistry.addRegistry(new MaterialAttributeRegistry());
        this.compositeRegistry.addRegistry(new AndroidXAttributeRegistry());
    }

    @Override
    public void registerAttrsFile(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            return;
        }
        try {
            org.xmlpull.v1.XmlPullParserFactory factory =
                    org.xmlpull.v1.XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            org.xmlpull.v1.XmlPullParser parser = factory.newPullParser();
            parser.setInput(new java.io.StringReader(xml));
            parseDocument(parser);
        } catch (Exception e) {
            throw new AttributeException("Erreur parsing attrs.xml: " + e.getMessage(), e);
        }
    }

    private void parseDocument(org.xmlpull.v1.XmlPullParser parser)
            throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {
        int event = parser.getEventType();
        while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            if (event == org.xmlpull.v1.XmlPullParser.START_TAG) {
                String tag = parser.getName();
                if ("declare-styleable".equals(tag)) {
                    parseStyleable(parser);
                } else if ("attr".equals(tag)) {
                    parseAttr(parser, null);
                }
            }
            event = parser.next();
        }
    }

    private void parseStyleable(org.xmlpull.v1.XmlPullParser parser)
            throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {
        String styleableName = parser.getAttributeValue(null, "name");
        if (styleableName == null) return;
        java.util.List<String> attrNames = new java.util.ArrayList<>();
        int depth = 1;
        while (depth > 0) {
            int event = parser.next();
            if (event == org.xmlpull.v1.XmlPullParser.START_TAG) {
                if ("attr".equals(parser.getName())) {
                    AttributeDefinitionImpl attr = parseAttr(parser, styleableName);
                    if (attr != null) {
                        attrNames.add(attr.getName());
                    }
                } else {
                    depth++;
                }
            } else if (event == org.xmlpull.v1.XmlPullParser.END_TAG) {
                depth--;
            } else if (event == org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                break;
            }
        }
        styleables.put(styleableName, attrNames);
    }

    private AttributeDefinitionImpl parseAttr(org.xmlpull.v1.XmlPullParser parser,
                                              String styleableName)
            throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {
        String name = parser.getAttributeValue(null, "name");
        if (name == null || name.isEmpty()) return null;
        String formatStr = parser.getAttributeValue(null, "format");
        AttributeFormat[] formats = AttributeFormat.parseFormats(formatStr);
        AttributeDefinitionImpl attr = new AttributeDefinitionImpl(name, formats);
        attr.setStyleableName(styleableName);
        attributes.put(name, attr);
        // Consomme les enfants (enum, flag)
        int depth = 1;
        while (depth > 0) {
            int event = parser.next();
            if (event == org.xmlpull.v1.XmlPullParser.START_TAG) {
                String childTag = parser.getName();
                if ("enum".equals(childTag)) {
                    String enumName = parser.getAttributeValue(null, "name");
                    String enumValue = parser.getAttributeValue(null, "value");
                    if (enumName != null && enumValue != null) {
                        try {
                            attr.addEnumValue(enumName, Integer.parseInt(enumValue));
                        } catch (NumberFormatException ignored) {}
                    }
                } else if ("flag".equals(childTag)) {
                    String flagName = parser.getAttributeValue(null, "name");
                    String flagValue = parser.getAttributeValue(null, "value");
                    if (flagName != null && flagValue != null) {
                        try {
                            int v = flagValue.startsWith("0x") || flagValue.startsWith("0X")
                                    ? Integer.parseInt(flagValue.substring(2), 16)
                                    : Integer.parseInt(flagValue);
                            attr.addFlagValue(flagName, v);
                        } catch (NumberFormatException ignored) {}
                    }
                }
                depth++;
            } else if (event == org.xmlpull.v1.XmlPullParser.END_TAG) {
                depth--;
            } else if (event == org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                break;
            }
        }
        return attr;
    }

    @Override
    public void applyCustomAttributes(View view, XmlPullParser parser, Context context) {
        // Délègue à l'applier
    }

    @Override
    public boolean isKnownAttribute(String attrName) {
        if (attributes.containsKey(attrName)) return true;
        return compositeRegistry.hasAttribute(attrName);
    }

    @Override
    public String getAttributeFormat(String attrName) {
        AttributeDefinitionImpl attr = attributes.get(attrName);
        if (attr != null) return attr.getFormatsString();
        jo.layoutlib.attributes.api.AttributeDefinitionImpl frameworkAttr =
                compositeRegistry.getAttribute(attrName);
        return frameworkAttr != null ? frameworkAttr.getFormatsString() : null;
    }

    public AttributeDefinitionImpl getAttributeDefinition(String attrName) {
        AttributeDefinitionImpl attr = attributes.get(attrName);
        if (attr != null) return attr;
        return compositeRegistry.getAttribute(attrName);
    }

    public int getAttributeCount() {
        return attributes.size();
    }

    public int getStyleableCount() {
        return styleables.size();
    }

    public Map<String, java.util.List<String>> getStyleables() {
        return new HashMap<>(styleables);
    }

    @Override
    public void clear() {
        attributes.clear();
        styleables.clear();
    }
}
