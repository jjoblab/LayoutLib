package jo.layoutlib.themes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jo.layoutlib.resources.ColorParser;
import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.resources.ResourceException;
import jo.layoutlib.resources.ResourceResolver;

/**
 * Implémentation principale du {@link ThemeResolver}.
 *
 * <p>Cette classe est le cœur du Module 4 (Themes). Elle agrège les styles
 * parsés depuis {@code themes.xml} et {@code styles.xml}, construit les
 * chaînes d'héritage, et résout les références {@code ?attr/foo} et
 * {@code ?android:attr/foo}.</p>
 *
 * <h2>Workflow</h2>
 * <ol>
 *   <li>Enregistrement des fichiers XML via
 *       {@link #registerThemesFile(String)} et
 *       {@link #registerStylesFile(String)}.</li>
 *   <li>Sélection du thème courant via {@link #setTheme(String)}.</li>
 *   <li>Résolution des attributs via {@link #resolveAttr(String)},
 *       {@link #getColorAttr(String)}, etc.</li>
 * </ol>
 *
 * <h2>Héritage</h2>
 * <p>L'algorithme de résolution d'un attribut parcourt la chaîne d'héritage
 * du thème courant jusqu'à trouver une définition. La profondeur maximale
 * est de 20 pour éviter les boucles infinies.</p>
 *
 * <h2>DayNight</h2>
 * <p>Le résolveur supporte le mode nuit via {@link #setNightMode(boolean)}.
 * Cela déclenche la re-résolution des attributs à partir des variantes
 * {@code values-night/themes.xml} si elles ont été chargées.</p>
 *
 * <p>Référence layoutlib original :
 * {@code com.android.layoutlib.bridge.impl.BridgeContext.obtainStyledAttributes()}
 * + {@code com.android.ide.common.resources.configuration.FolderConfiguration}.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ThemeResolverImpl implements ThemeResolver {

    /** Profondeur maximale d'héritage (anti-boucle). */
    private static final int MAX_INHERITANCE_DEPTH = 20;

    /** Map des styles par nom (mode jour). */
    private final Map<String, StyleDefinition> styles = new HashMap<>();

    /** Map des styles par nom (mode nuit, peut être vide). */
    private final Map<String, StyleDefinition> nightStyles = new HashMap<>();

    /** Parser de fichiers. */
    private final ThemeFileParser fileParser = new ThemeFileParser();

    /** Résolveur de resources pour la résolution récursive. */
    private final ResourceResolver resourceResolver;

    /** Convertisseur de dimensions. */
    private final DimensionConverter dimensionConverter;

    /** Thème courant. */
    private String currentTheme;

    /** Mode nuit activé. */
    private boolean nightMode = false;

    /** Cache des attributs résolus. */
    private final Map<String, Object> attrCache = new HashMap<>();

    /**
     * Construit un resolver avec un résolveur de resources et convertisseur.
     *
     * @param resourceResolver  résolveur de resources (peut être {@code null})
     * @param dimensionConverter convertisseur de dimensions (peut être {@code null})
     */
    public ThemeResolverImpl(ResourceResolver resourceResolver,
                             DimensionConverter dimensionConverter) {
        this.resourceResolver = resourceResolver;
        this.dimensionConverter = dimensionConverter;
    }

    /**
     * Construit un resolver minimal.
     */
    public ThemeResolverImpl() {
        this(null, null);
    }

    /**
     * Enregistre un fichier themes.xml.
     *
     * @param xml      contenu XML
     * @param nightMode {@code true} si c'est {@code values-night/themes.xml}
     * @return le nombre de thèmes parsés
     */
    public int registerThemesFile(String xml, boolean nightMode) {
        Map<String, StyleDefinition> target = nightMode ? nightStyles : styles;
        int count = fileParser.parseThemes(xml, target);
        if (nightMode) {
            attrCache.clear();
        }
        return count;
    }

    /**
     * Enregistre un fichier themes.xml (mode jour).
     *
     * @param xml contenu XML
     * @return le nombre de thèmes parsés
     */
    public int registerThemesFile(String xml) {
        return registerThemesFile(xml, false);
    }

    /**
     * Enregistre un fichier styles.xml.
     *
     * @param xml contenu XML
     * @return le nombre de styles parsés
     */
    public int registerStylesFile(String xml) {
        return fileParser.parseStyles(xml, styles);
    }

    @Override
    public void setTheme(String themeName) {
        this.currentTheme = themeName;
        attrCache.clear();
    }

    @Override
    public Object resolveAttr(String reference) {
        if (reference == null || reference.isEmpty()) {
            return null;
        }
        Object cached = attrCache.get(reference);
        if (cached != null) {
            return cached;
        }
        Object result = resolveAttrInternal(reference);
        if (result != null) {
            attrCache.put(reference, result);
        }
        return result;
    }

    /**
     * Implémentation interne de la résolution d'attribut.
     *
     * @param reference la référence (ex. {@code ?attr/colorPrimary} ou
     *                  {@code ?android:attr/colorBackground})
     * @return la valeur résolue, ou {@code null}
     */
    private Object resolveAttrInternal(String reference) {
        String attrName = extractAttrName(reference);
        if (attrName == null) {
            return null;
        }
        // Parcours de la chaîne d'héritage
        List<String> chain = buildInheritanceChain(currentTheme);
        for (String styleName : chain) {
            StyleDefinition style = getStyle(styleName);
            if (style != null) {
                String value = style.getAttribute(attrName);
                if (value == null && attrName.startsWith("android:")) {
                    // Tentative sans le préfixe android:
                    value = style.getAttribute(attrName.substring("android:".length()));
                }
                if (value != null) {
                    return resolveValue(value);
                }
            }
        }
        return null;
    }

    /**
     * Extrait le nom de l'attribut depuis une référence {@code ?attr/...} ou
     * {@code ?android:attr/...}.
     *
     * @param reference la référence
     * @return le nom de l'attribut, ou {@code null}
     */
    private String extractAttrName(String reference) {
        if (!reference.startsWith("?")) {
            return null;
        }
        String rest = reference.substring(1);
        if (rest.startsWith("android:attr/")) {
            return "android:" + rest.substring("android:attr/".length());
        }
        if (rest.startsWith("attr/")) {
            return rest.substring("attr/".length());
        }
        // Format court : ?colorPrimary
        return rest;
    }

    /**
     * Construit la chaîne d'héritage d'un style, du plus spécifique au plus
     * général.
     *
     * @param styleName nom du style de départ
     * @return liste ordonnée des styles (le premier est le plus spécifique)
     */
    private List<String> buildInheritanceChain(String styleName) {
        List<String> chain = new ArrayList<>();
        String current = styleName;
        int depth = 0;
        while (current != null && depth < MAX_INHERITANCE_DEPTH) {
            if (chain.contains(current)) {
                break;  // Anti-boucle
            }
            chain.add(current);
            StyleDefinition style = getStyle(current);
            if (style == null) {
                break;
            }
            current = style.getParent();
            depth++;
        }
        return chain;
    }

    /**
     * Récupère un style depuis la bonne map (jour ou nuit).
     *
     * @param name nom du style
     * @return le style, ou {@code null}
     */
    private StyleDefinition getStyle(String name) {
        if (nightMode) {
            StyleDefinition night = nightStyles.get(name);
            if (night != null) {
                return night;
            }
        }
        return styles.get(name);
    }

    /**
     * Résout une valeur d'attribut (référence @color/, @drawable/, valeur
     * littérale, etc.) en utilisant le ResourceResolver.
     *
     * @param value la valeur brute
     * @return la valeur résolue (Integer pour couleur, Float pour dimension,
     *         String pour autre)
     */
    private Object resolveValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        if (value.startsWith("@color/") && resourceResolver != null) {
            return resourceResolver.getColor(value);
        }
        if (value.startsWith("@string/") && resourceResolver != null) {
            return resourceResolver.getString(value);
        }
        if (value.startsWith("@dimen/") && resourceResolver != null) {
            return resourceResolver.getDimension(value);
        }
        if (value.startsWith("@drawable/") || value.startsWith("@layout/")) {
            return value;  // On retourne la référence, résolue plus tard
        }
        if (value.startsWith("#")) {
            try {
                return ColorParser.parse(value);
            } catch (ResourceException e) {
                return value;
            }
        }
        // Pour les dimensions littérales (16dp, 14sp, ...)
        if (dimensionConverter != null && value.matches("[0-9.]+(dp|dip|sp|px|mm|in|pt)")) {
            try {
                return dimensionConverter.toPixels(value);
            } catch (ResourceException e) {
                return value;
            }
        }
        return value;
    }

    @Override
    public Integer getColorAttr(String attrName) {
        Object value = resolveAttr("?attr/" + attrName);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        return null;
    }

    @Override
    public Float getDimensionAttr(String attrName) {
        Object value = resolveAttr("?attr/" + attrName);
        if (value instanceof Float) {
            return (Float) value;
        }
        return null;
    }

    @Override
    public void setNightMode(boolean nightMode) {
        this.nightMode = nightMode;
        attrCache.clear();
    }

    @Override
    public String getParentStyle(String styleName) {
        StyleDefinition style = getStyle(styleName);
        return style != null ? style.getParent() : null;
    }

    @Override
    public void clearCache() {
        attrCache.clear();
    }

    /**
     * @return le nombre total de styles chargés
     */
    public int getStyleCount() {
        return styles.size();
    }

    /**
     * @return le nombre de thèmes chargés en mode nuit
     */
    public int getNightStyleCount() {
        return nightStyles.size();
    }

    /**
     * @return le thème courant
     */
    public String getCurrentTheme() {
        return currentTheme;
    }

    /**
     * @return {@code true} si le mode nuit est actif
     */
    public boolean isNightMode() {
        return nightMode;
    }

    /**
     * Construit la chaîne d'héritage publique d'un style.
     *
     * @param styleName nom du style
     * @return la liste ordonnée des styles parents
     */
    public List<String> getInheritanceChain(String styleName) {
        return buildInheritanceChain(styleName);
    }
}
