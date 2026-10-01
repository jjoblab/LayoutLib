package jo.layoutlib.layout;

import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;

/**
 * Moteur de layout complet, inspiré de
 * {@code com.android.layoutlib.bridge.impl.RenderSessionImpl} de l'AOSP.
 *
 * <p>Implémente le pipeline de rendu complet :</p>
 * <ol>
 *   <li><strong>Passe 1</strong> : measure avec EXACTLY pour obtenir la taille
 *       du contenu dans le decor/dialog</li>
 *   <li><strong>Passe 2</strong> : measure avec UNSPECIFIED (si mode EXPAND)
 *       pour connaître la taille réelle du contenu</li>
 *   <li><strong>Layout</strong> : positionne les vues selon les dimensions
 *       mesurées</li>
 *   <li><strong>Validation</strong> : vérifie que les dimensions sont cohérentes</li>
 * </ol>
 *
 * <h2>Rendering modes</h2>
 * <ul>
 *   <li>{@link RenderingMode#NORMAL} : mesure avec EXACTLY</li>
 *   <li>{@link RenderingMode#EXPAND} : mesure avec UNSPECIFIED pour connaître
 *       la taille naturelle du contenu</li>
 *   <li>{@link RenderingMode#SHRINK} : mesure avec AT_MOST</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class LayoutEngineImpl implements LayoutEngine {

    /** Densité d'affichage par défaut (xhdpi). */
    private static final float DEFAULT_DENSITY = 2.0f;

    /** Échelle de police par défaut. */
    private static final float DEFAULT_FONT_SCALE = 1.0f;

    /** Densité X DPI par défaut. */
    private static final float DEFAULT_XDPI = 320f;

    private float density = DEFAULT_DENSITY;
    private float fontScale = DEFAULT_FONT_SCALE;
    private float xdpi = DEFAULT_XDPI;

    /** Mode de rendering courant. */
    private RenderingMode renderingMode = RenderingMode.NORMAL;

    /** Largeur mesurée après rendu. */
    private int measuredWidth;

    /** Hauteur mesurée après rendu. */
    private int measuredHeight;

    /** Temps de la dernière opération de rendu (ms). */
    private long renderTimeMs;

    /**
     * Mode de rendering, inspiré de
     * {@code com.android.ide.common.rendering.api.SessionParams.RenderingMode}.
     */
    public enum RenderingMode {
        /** Mesure avec EXACTLY (taille fixe). */
        NORMAL,
        /** Mesure avec UNSPECIFIED pour connaître la taille naturelle. */
        EXPAND,
        /** Mesure avec AT_MOST (taille max). */
        SHRINK
    }

    /**
     * Constructeur par défaut.
     */
    public LayoutEngineImpl() {
    }

    @Override
    public void measure(View root, int width, int height) {
        if (root == null) {
            throw new LayoutException("Vue racine null");
        }

        long start = System.nanoTime();

        // Assure des LayoutParams valides
        ensureLayoutParams(root);

        // Passe 1 : measure avec EXACTLY (comportement par défaut)
        int widthSpec = buildMeasureSpec(width, root, true, MeasureSpec.EXACTLY);
        int heightSpec = buildMeasureSpec(height, root, false, MeasureSpec.EXACTLY);
        root.measure(widthSpec, heightSpec);

        // Passe 2 : si mode EXPAND, re-measure avec UNSPECIFIED
        if (renderingMode == RenderingMode.EXPAND) {
            int expandWidthSpec = buildMeasureSpec(width, root, true, MeasureSpec.UNSPECIFIED);
            int expandHeightSpec = buildMeasureSpec(height, root, false, MeasureSpec.UNSPECIFIED);
            root.measure(expandWidthSpec, expandHeightSpec);
        } else if (renderingMode == RenderingMode.SHRINK) {
            int shrinkWidthSpec = buildMeasureSpec(width, root, true, MeasureSpec.AT_MOST);
            int shrinkHeightSpec = buildMeasureSpec(height, root, false, MeasureSpec.AT_MOST);
            root.measure(shrinkWidthSpec, shrinkHeightSpec);
        }

        measuredWidth = root.getMeasuredWidth();
        measuredHeight = root.getMeasuredHeight();
        renderTimeMs = (System.nanoTime() - start) / 1_000_000;
    }

    @Override
    public void layout(View root, int left, int top, int right, int bottom) {
        if (root == null) {
            throw new LayoutException("Vue racine null");
        }
        root.layout(left, top, right, bottom);
    }

    @Override
    public void render(View root, int width, int height) {
        if (root == null) {
            throw new LayoutException("Vue racine null");
        }

        long start = System.nanoTime();

        // Assure des LayoutParams valides
        ensureLayoutParams(root);

        // Étape 1 : Measure avec EXACTLY en largeur ET hauteur
        // pour fit le root au device frame (comme Android Studio).
        // On utilise MeasureSpec.EXACTLY directement (pas buildMeasureSpec)
        // car buildMeasureSpec utilise AT_MOST quand LayoutParams = wrap_content,
        // ce qui empêche le root de remplir le device frame.
        int widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        root.measure(widthSpec, heightSpec);

        // Étape 2 : Si le contenu est plus grand que l'espace, re-measure en UNSPECIFIED
        if (renderingMode == RenderingMode.EXPAND
                && root.getMeasuredHeight() >= height) {
            int expandHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            root.measure(widthSpec, expandHeightSpec);
        }

        // Étape 3 : Layout
        int measuredWidth = root.getMeasuredWidth();
        int measuredHeight = root.getMeasuredHeight();
        root.layout(0, 0, measuredWidth, measuredHeight);

        this.measuredWidth = measuredWidth;
        this.measuredHeight = measuredHeight;
        this.renderTimeMs = (System.nanoTime() - start) / 1_000_000;
    }

    /**
     * Construit un MeasureSpec intelligent en fonction :
     * <ul>
     *   <li>Des LayoutParams de la vue</li>
     *   <li>De la taille cible disponible</li>
     *   <li>Du mode forcé (EXACTLY, AT_MOST, UNSPECIFIED)</li>
     * </ul>
     *
     * @param targetSize dimension cible disponible (largeur ou hauteur)
     * @param view       la vue à mesurer
     * @param isWidth    true si on calcule la largeur
     * @param forcedMode mode forcé si pas de LayoutParams
     * @return le MeasureSpec
     */
    private int buildMeasureSpec(int targetSize, View view, boolean isWidth, int forcedMode) {
        // Si targetSize <= 0, on utilise UNSPECIFIED
        if (targetSize <= 0) {
            return MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }

        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp == null) {
            return MeasureSpec.makeMeasureSpec(targetSize, forcedMode);
        }

        int size = isWidth ? lp.width : lp.height;

        if (size == ViewGroup.LayoutParams.MATCH_PARENT) {
            return MeasureSpec.makeMeasureSpec(targetSize, MeasureSpec.EXACTLY);
        }
        if (size == ViewGroup.LayoutParams.WRAP_CONTENT) {
            return MeasureSpec.makeMeasureSpec(targetSize, MeasureSpec.AT_MOST);
        }
        // Dimension fixe en pixels
        return MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
    }

    /**
     * Assure qu'une vue a des LayoutParams valides.
     * Si null, crée des WRAP_CONTENT par défaut.
     *
     * @param view la vue à vérifier
     */
    private void ensureLayoutParams(View view) {
        if (view == null) return;
        if (view.getLayoutParams() == null) {
            view.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    /**
     * Configure le mode de rendering.
     *
     * @param mode le mode (NORMAL, EXPAND, SHRINK)
     */
    public void setRenderingMode(RenderingMode mode) {
        this.renderingMode = mode != null ? mode : RenderingMode.NORMAL;
    }

    /**
     * @return le mode de rendering courant
     */
    public RenderingMode getRenderingMode() {
        return renderingMode;
    }

    @Override
    public void setDensity(float density) {
        if (density <= 0) {
            throw new LayoutException("La densité doit être > 0, reçu : " + density);
        }
        this.density = density;
    }

    @Override
    public void setFontScale(float fontScale) {
        if (fontScale <= 0) {
            throw new LayoutException("fontScale doit être > 0, reçu : " + fontScale);
        }
        this.fontScale = fontScale;
    }

    /**
     * Définit la densité X DPI simulée.
     *
     * @param xdpi densité en dpi (ex. 320)
     */
    public void setXdpi(float xdpi) {
        this.xdpi = xdpi;
    }

    /**
     * Configure les DisplayMetrics de la ressource d'affichage.
     *
     * @param metrics les DisplayMetrics à configurer
     */
    public void configureDisplayMetrics(DisplayMetrics metrics) {
        if (metrics == null) return;
        metrics.density = density;
        metrics.scaledDensity = density * fontScale;
        metrics.xdpi = xdpi;
        metrics.ydpi = xdpi;
        metrics.densityDpi = (int) (density * 160);
    }

    /**
     * @return la largeur mesurée après le dernier rendu
     */
    public int getMeasuredWidth() {
        return measuredWidth;
    }

    /**
     * @return la hauteur mesurée après le dernier rendu
     */
    public int getMeasuredHeight() {
        return measuredHeight;
    }

    /**
     * @return le temps du dernier rendu en ms
     */
    public long getRenderTimeMs() {
        return renderTimeMs;
    }

    /**
     * Indique si le dernier rendu respecte le seuil de performance.
     *
     * @return true si le temps de rendu est < 200 ms
     */
    public boolean meetsPerformanceTarget() {
        return renderTimeMs < 200;
    }

    public float getDensity() {
        return density;
    }

    public float getFontScale() {
        return fontScale;
    }

    public float getXdpi() {
        return xdpi;
    }
}
