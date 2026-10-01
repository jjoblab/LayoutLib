package jo.layoutlib.inflater;

import android.content.Context;

/**
 * Profil d'appareil (device frame) pour le preview.
 *
 * <p>Définit les dimensions et caractéristiques d'un appareil Android,
 * utilisées pour :</p>
 * <ul>
 *   <li>Dimensionner le device frame dans l'OverlayView</li>
 *   <li>Définir les targetDimensions du RenderService (fit au device)</li>
 *   <li>Contraindre les vues aux limites du device frame</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 3.5
 */
public class DeviceProfile {

    public final String name;
    public final int widthDp;
    public final int heightDp;
    public final int densityDpi;
    public final boolean landscape;

    public DeviceProfile(String name, int widthDp, int heightDp, int densityDpi, boolean landscape) {
        this.name = name;
        this.widthDp = widthDp;
        this.heightDp = heightDp;
        this.densityDpi = densityDpi;
        this.landscape = landscape;
    }

    /**
     * @param density la densité de l'écran (getDisplayMetrics().density)
     * @return la largeur en pixels
     */
    public int getWidthPx(float density) {
        return (int) (widthDp * density);
    }

    /**
     * @param density la densité de l'écran (getDisplayMetrics().density)
     * @return la hauteur en pixels
     */
    public int getHeightPx(float density) {
        return (int) (heightDp * density);
    }

    /**
     * @return le facteur de densité (ex: 2.75 pour xxhdpi 420dpi)
     */
    public float getDensityFactor() {
        return densityDpi / 160f;
    }

    /**
     * @return une description courte (ex: "Pixel 4 · 393×851dp · 440dpi")
     */
    public String getDescription() {
        return name + " · " + widthDp + "×" + heightDp + "dp · " + densityDpi + "dpi";
    }

    // ============================================================
    // PROFILS PAR DÉFAUT
    // ============================================================

    /** Pixel 4 — 393×851dp, 440dpi (xxhdpi). Modèle par défaut. */
    public static final DeviceProfile PIXEL_4 = new DeviceProfile(
            "Pixel 4", 393, 851, 440, false);

    /** Pixel 7 Pro — 412×892dp, 420dpi (xxhdpi). */
    public static final DeviceProfile PIXEL_7_PRO = new DeviceProfile(
            "Pixel 7 Pro", 412, 892, 420, false);

    /** Samsung Galaxy S24 — 360×780dp, 480dpi (xxxhdpi). */
    public static final DeviceProfile GALAXY_S24 = new DeviceProfile(
            "Galaxy S24", 360, 780, 480, false);

    /** Tous les profils disponibles (pour un sélecteur). */
    public static final DeviceProfile[] DEFAULTS = {
            PIXEL_4,
            PIXEL_7_PRO,
            GALAXY_S24,
    };
}
