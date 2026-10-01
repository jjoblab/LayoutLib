package jo.layoutlib.validation;

/**
 * Cas de test individuel : un nom + un XML + des dimensions optionnelles.
 *
 * <p>Utilisé par {@link LayoutTestHarness#runTests(java.util.List)} pour
 * exécuter une série de tests en une fois.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class LayoutTestCase {

    /** Nom du test. */
    private final String name;

    /** XML source. */
    private final String xml;

    /** Largeur cible (0 = utiliser la valeur par défaut du harness). */
    private final int width;

    /** Hauteur cible (0 = utiliser la valeur par défaut). */
    private final int height;

    /** Catégorie du test (simple, nested, resources, theme, etc.). */
    private final String category;

    /**
     * Construit un cas de test simple.
     *
     * @param name nom du test
     * @param xml  XML source
     */
    public LayoutTestCase(String name, String xml) {
        this(name, xml, 0, 0, "default");
    }

    /**
     * Construit un cas de test avec dimensions.
     *
     * @param name   nom
     * @param xml    XML
     * @param width  largeur (0 = défaut)
     * @param height hauteur (0 = défaut)
     */
    public LayoutTestCase(String name, String xml, int width, int height) {
        this(name, xml, width, height, "default");
    }

    /**
     * Construit un cas de test complet.
     *
     * @param name     nom
     * @param xml      XML
     * @param width    largeur
     * @param height   hauteur
     * @param category catégorie
     */
    public LayoutTestCase(String name, String xml, int width, int height, String category) {
        this.name = name;
        this.xml = xml;
        this.width = width;
        this.height = height;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public String getXml() {
        return xml;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return name + " [" + category + "]";
    }
}
