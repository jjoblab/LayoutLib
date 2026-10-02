package jo.layoutlib.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implémentation principale du {@link ResourceResolver}.
 *
 * <p>Cette classe est le cœur du Module 2 (Resources). Elle agrège les
 * resources parsées depuis un dossier {@code res/} et expose les méthodes
 * de résolution {@code getColor}, {@code getString}, {@code getDimension},
 * etc.</p>
 *
 * <h2>Workflow</h2>
 * <ol>
 *   <li>Construction avec un dossier {@code res/} (chemin absolu) ou un
 *       {@link ResourceTable} pré-rempli.</li>
 *   <li>Configuration optionnelle du mode (jour/nuit, portrait/paysage,
 *       niveau d'API) via {@link #setNightMode(boolean)},
 *       {@link #setLandscape(boolean)}, {@link #setApiLevel(int)}.</li>
 *   <li>Résolution des références via les méthodes
 *       {@link #getColor(String)}, {@link #getString(String)}, etc.</li>
 * </ol>
 *
 * <h2>Résolution chainable</h2>
 * <p>Les références chainables sont supportées : si la valeur d'une couleur
 * est elle-même une référence {@code @color/another}, la résolution est
 * récursive jusqu'à 5 niveaux de profondeur pour éviter les boucles
 * infinies.</p>
 *
 * <h2>Qualifiers</h2>
 * <p>Le resolver sélectionne automatiquement la meilleure valeur en fonction
 * du qualifier courant. Voir {@link ResourceQualifier} pour les règles de
 * priorité.</p>
 *
 * <h2>Cache</h2>
 * <p>Les résolutions sont mises en cache (LRU, 512 entrées) pour éviter de
 * re-parcourir la table à chaque appel. Le cache est invalidé quand le mode
 * (jour/nuit, orientation, API) change.</p>
 *
 * <p>Référence layoutlib original :
 * {@code com.android.layoutlib.bridge.android.BridgeContext} +
 * {@code com.android.ide.common.resources.ResourceResolver}.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ResourceResolverImpl implements ResourceResolver {

    /** Dossier de resources à scanner. */
    private static final String VALUES_DIR = "values";

    /** Extensions de fichiers de resources XML à scanner. */
    private static final Set<String> RESOURCE_FILES = new HashSet<>(Arrays.asList(
            "colors.xml", "strings.xml", "dimens.xml",
            "integers.xml", "bools.xml", "themes.xml", "styles.xml"
    ));

    /** Profondeur maximale de résolution chainable (anti-boucle). */
    private static final int MAX_CHAIN_DEPTH = 5;

    /** Table des resources agrégée. */
    private final ResourceTable table;

    /** Dossier {@code res/} source, ou {@code null} si non lié à un dossier. */
    private final File resFolder;

    /** Convertisseur de dimensions. */
    private DimensionConverter dimensionConverter;

    /** Qualifier courant (contexte de résolution). */
    private ResourceQualifier currentQualifier;

    /** Cache des résolutions de couleurs. */
    private final ResourceCache<String, Integer> colorCache = new ResourceCache<>();

    /** Cache des résolutions de chaînes. */
    private final ResourceCache<String, String> stringCache = new ResourceCache<>();

    /** Cache des résolutions de dimensions. */
    private final ResourceCache<String, Float> dimenCache = new ResourceCache<>();

    /** Cache des résolutions d'entiers. */
    private final ResourceCache<String, Integer> integerCache = new ResourceCache<>();

    /** Cache des résolutions de booléens. */
    private final ResourceCache<String, Boolean> boolCache = new ResourceCache<>();

    /** Cache des résolutions de string-arrays (liste défensive, copiée au retour). */
    private final ResourceCache<String, List<String>> stringArrayCache = new ResourceCache<>();

    /**
     * Construit un resolver lié à un dossier {@code res/}.
     *
     * <p>Le constructeur scanne le dossier et parse tous les fichiers XML de
     * {@code res/values*}. Les resources sont chargées en mémoire et prêtes
     * à être résolues.</p>
     *
     * @param resFolderPath chemin absolu du dossier {@code res/}
     * @throws ResourceException si le dossier n'existe pas ou est illisible
     */
    public ResourceResolverImpl(String resFolderPath) {
        this.resFolder = new File(resFolderPath);
        if (!resFolder.exists() || !resFolder.isDirectory()) {
            throw new ResourceException(
                    "Dossier res/ introuvable : " + resFolderPath, resFolderPath);
        }
        this.table = new ResourceTable();
        this.currentQualifier = ResourceQualifier.DEFAULT;
        this.dimensionConverter = new DimensionConverter(2.0f, 1.0f, 320f);
        scanResources();
    }

    /**
     * Construit un resolver à partir d'une table pré-remplie.
     *
     * <p>Utile pour les tests ou pour charger les resources depuis une
     * source autre qu'un dossier (par exemple un JAR).</p>
     *
     * @param table table pré-remplie
     */
    public ResourceResolverImpl(ResourceTable table) {
        if (table == null) {
            throw new IllegalArgumentException("La table ne peut pas être null");
        }
        this.table = table;
        this.resFolder = null;
        this.currentQualifier = ResourceQualifier.DEFAULT;
        this.dimensionConverter = new DimensionConverter(2.0f, 1.0f, 320f);
    }

    /**
     * Scanne le dossier {@code res/} et parse tous les fichiers de resources.
     */
    private void scanResources() {
        File[] valueDirs = resFolder.listFiles(
                (dir, name) -> name.startsWith(VALUES_DIR));
        if (valueDirs == null) {
            return;
        }
        for (File valueDir : valueDirs) {
            ResourceQualifier qualifier = ResourceQualifier.parse(valueDir.getName());
            File[] files = valueDir.listFiles(
                    (dir, name) -> name.endsWith(".xml"));
            if (files == null) {
                continue;
            }
            for (File file : files) {
                if (!RESOURCE_FILES.contains(file.getName())) {
                    continue;
                }
                String xml = readFile(file);
                if (xml != null) {
                    ResourceFileParser parser = new ResourceFileParser(table, qualifier);
                    parser.parse(xml);
                }
            }
        }
    }

    /**
     * Lit le contenu d'un fichier en UTF-8.
     *
     * @param file le fichier à lire
     * @return le contenu, ou {@code null} en cas d'erreur
     */
    private String readFile(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            int read = fis.read(data);
            if (read <= 0) {
                return "";
            }
            return new String(data, 0, read, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Integer getColor(String reference) {
        if (reference == null || reference.isEmpty()) {
            return null;
        }
        Integer cached = colorCache.get(reference);
        if (cached != null) {
            return cached;
        }
        Integer result = resolveColorChain(reference, 0);
        if (result != null) {
            colorCache.put(reference, result);
        }
        return result;
    }

    /**
     * Résout une référence de couleur, avec support des références chainables.
     *
     * @param reference la référence
     * @param depth     profondeur actuelle (anti-boucle)
     * @return la valeur ARGB, ou {@code null}
     */
    private Integer resolveColorChain(String reference, int depth) {
        if (depth > MAX_CHAIN_DEPTH) {
            return null;
        }
        if (!reference.startsWith("@color/")) {
            // Valeur littérale (hex ou nommée)
            try {
                return ColorParser.parse(reference);
            } catch (ResourceException e) {
                return null;
            }
        }
        String name = reference.substring("@color/".length());
        String raw = table.getColor(name, currentQualifier);
        if (raw == null) {
            // Tentative : la valeur est peut-être stockée comme string
            raw = table.getString(name, currentQualifier);
        }
        if (raw == null) {
            return null;
        }
        // Si la valeur est une référence chainable, on résout récursivement
        if (raw.startsWith("@color/")) {
            return resolveColorChain(raw, depth + 1);
        }
        // Sinon on parse la valeur littérale
        try {
            return ColorParser.parse(raw);
        } catch (ResourceException e) {
            return null;
        }
    }

    @Override
    public String getString(String reference) {
        if (reference == null || reference.isEmpty()) {
            return null;
        }
        String cached = stringCache.get(reference);
        if (cached != null) {
            return cached;
        }
        String result = resolveStringChain(reference, 0);
        if (result != null) {
            stringCache.put(reference, result);
        }
        return result;
    }

    /**
     * Résout une référence de chaîne, avec support des références chainables.
     *
     * @param reference la référence
     * @param depth     profondeur actuelle
     * @return la valeur, ou {@code null}
     */
    private String resolveStringChain(String reference, int depth) {
        if (depth > MAX_CHAIN_DEPTH) {
            return null;
        }
        if (!reference.startsWith("@string/")) {
            return reference;
        }
        String name = reference.substring("@string/".length());
        String value = table.getString(name, currentQualifier);
        if (value == null) {
            return null;
        }
        if (value.startsWith("@string/")) {
            return resolveStringChain(value, depth + 1);
        }
        return value;
    }

    @Override
    public Float getDimension(String reference) {
        if (reference == null || reference.isEmpty()) {
            return null;
        }
        Float cached = dimenCache.get(reference);
        if (cached != null) {
            return cached;
        }
        Float result = resolveDimenChain(reference, 0);
        if (result != null) {
            dimenCache.put(reference, result);
        }
        return result;
    }

    /**
     * Résout une référence de dimension, avec support chainable.
     *
     * @param reference la référence
     * @param depth     profondeur
     * @return la valeur en pixels, ou {@code null}
     */
    private Float resolveDimenChain(String reference, int depth) {
        if (depth > MAX_CHAIN_DEPTH) {
            return null;
        }
        if (!reference.startsWith("@dimen/")) {
            try {
                return dimensionConverter.toPixels(reference);
            } catch (ResourceException e) {
                return null;
            }
        }
        String name = reference.substring("@dimen/".length());
        String raw = table.getDimen(name, currentQualifier);
        if (raw == null) {
            return null;
        }
        if (raw.startsWith("@dimen/")) {
            return resolveDimenChain(raw, depth + 1);
        }
        try {
            return dimensionConverter.toPixels(raw);
        } catch (ResourceException e) {
            return null;
        }
    }

    @Override
    public java.util.List<String> getStringArray(String reference) {
        if (reference == null || reference.isEmpty()) {
            return null;
        }
        List<String> cached = stringArrayCache.get(reference);
        if (cached != null) {
            return new ArrayList<>(cached);
        }
        List<String> result = resolveStringArrayChain(reference, 0);
        if (result != null) {
            stringArrayCache.put(reference, new ArrayList<>(result));
        }
        return result;
    }

    /**
     * Résout un string-array, avec résolution des {@code @string/} contenus
     * dans les éléments.
     */
    private List<String> resolveStringArrayChain(String reference, int depth) {
        if (depth > MAX_CHAIN_DEPTH || !reference.startsWith("@array/")) {
            return null;
        }
        String name = reference.substring("@array/".length());
        List<String> raw = table.getStringArray(name, currentQualifier);
        if (raw == null) {
            return null;
        }
        List<String> resolved = new ArrayList<>(raw.size());
        for (String item : raw) {
            if (item != null && item.startsWith("@string/")) {
                resolved.add(resolveStringChain(item, depth + 1));
            } else {
                resolved.add(item);
            }
        }
        return resolved;
    }

    @Override
    public Integer getInteger(String reference) {
        if (reference == null || reference.isEmpty()) {
            return null;
        }
        Integer cached = integerCache.get(reference);
        if (cached != null) {
            return cached;
        }
        Integer result = resolveIntegerChain(reference, 0);
        if (result != null) {
            integerCache.put(reference, result);
        }
        return result;
    }

    /**
     * Résout une référence d'entier, avec support chainable.
     */
    private Integer resolveIntegerChain(String reference, int depth) {
        if (depth > MAX_CHAIN_DEPTH) {
            return null;
        }
        if (!reference.startsWith("@integer/")) {
            try {
                return Integer.parseInt(reference);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        String name = reference.substring("@integer/".length());
        Integer value = table.getInteger(name, currentQualifier);
        if (value == null) {
            return null;
        }
        return value;
    }

    @Override
    public Boolean getBoolean(String reference) {
        if (reference == null || reference.isEmpty()) {
            return null;
        }
        Boolean cached = boolCache.get(reference);
        if (cached != null) {
            return cached;
        }
        Boolean result = resolveBoolChain(reference, 0);
        if (result != null) {
            boolCache.put(reference, result);
        }
        return result;
    }

    /**
     * Résout une référence booléenne.
     */
    private Boolean resolveBoolChain(String reference, int depth) {
        if (depth > MAX_CHAIN_DEPTH) {
            return null;
        }
        if (!reference.startsWith("@bool/")) {
            String lower = reference.toLowerCase();
            if ("true".equals(lower) || "1".equals(lower)) {
                return Boolean.TRUE;
            }
            if ("false".equals(lower) || "0".equals(lower)) {
                return Boolean.FALSE;
            }
            return null;
        }
        String name = reference.substring("@bool/".length());
        return table.getBoolean(name, currentQualifier);
    }

    @Override
    public String getLayout(String reference) {
        if (reference == null || !reference.startsWith("@layout/")) {
            return null;
        }
        String name = reference.substring("@layout/".length());
        if (resFolder == null) {
            return null;
        }
        File layoutDir = new File(resFolder, "layout");
        File layoutFile = new File(layoutDir, name + ".xml");
        if (!layoutFile.exists()) {
            // Recherche dans les dossiers qualifiés (layout-land, layout-night, etc.)
            File[] layoutDirs = resFolder.listFiles(
                    (dir, n) -> n.startsWith("layout"));
            if (layoutDirs != null) {
                for (File dir : layoutDirs) {
                    File candidate = new File(dir, name + ".xml");
                    if (candidate.exists()) {
                        return readFile(candidate);
                    }
                }
            }
            return null;
        }
        return readFile(layoutFile);
    }

    @Override
    public String getDrawablePath(String reference) {
        if (reference == null || !reference.startsWith("@drawable/")) {
            return null;
        }
        String name = reference.substring("@drawable/".length());
        if (resFolder == null) {
            return null;
        }
        String[] extensions = {".xml", ".png", ".webp", ".jpg", ".jpeg"};
        File[] drawableDirs = resFolder.listFiles(
                (dir, n) -> n.startsWith("drawable"));
        if (drawableDirs == null) {
            return null;
        }
        for (File dir : drawableDirs) {
            for (String ext : extensions) {
                File candidate = new File(dir, name + ext);
                if (candidate.exists()) {
                    return candidate.getAbsolutePath();
                }
            }
        }
        return null;
    }

    @Override
    public void setNightMode(boolean nightMode) {
        this.currentQualifier = new ResourceQualifier(
                nightMode, currentQualifier.isLandscape(), currentQualifier.getApiLevel());
        clearCache();
    }

    @Override
    public void setLandscape(boolean landscape) {
        this.currentQualifier = new ResourceQualifier(
                currentQualifier.isNight(), landscape, currentQualifier.getApiLevel());
        clearCache();
    }

    @Override
    public void setApiLevel(int apiLevel) {
        this.currentQualifier = new ResourceQualifier(
                currentQualifier.isNight(), currentQualifier.isLandscape(), apiLevel);
        clearCache();
    }

    /**
     * Définit le convertisseur de dimensions à utiliser.
     *
     * <p>À appeler avant la première résolution si l'on veut utiliser des
     * métriques d'affichage spécifiques (densité, fontScale, xdpi).</p>
     *
     * @param converter le convertisseur
     */
    public void setDimensionConverter(DimensionConverter converter) {
        this.dimensionConverter = converter;
        clearCache();
    }

    /**
     * @return le qualifier courant
     */
    public ResourceQualifier getCurrentQualifier() {
        return currentQualifier;
    }

    /**
     * @return la table interne (pour inspection ou tests)
     */
    public ResourceTable getTable() {
        return table;
    }

    /**
     * @return le nombre total de couleurs chargées
     */
    public int getColorCount() {
        return table.colorCount();
    }

    /**
     * @return le nombre total de chaînes chargées
     */
    public int getStringCount() {
        return table.stringCount();
    }

    /**
     * @return le nombre total de dimensions chargées
     */
    public int getDimenCount() {
        return table.dimenCount();
    }

    @Override
    public void clearCache() {
        colorCache.clear();
        stringCache.clear();
        dimenCache.clear();
        integerCache.clear();
        boolCache.clear();
        stringArrayCache.clear();
    }
}
