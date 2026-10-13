package jo.layoutlib.drawables;

/**
 * Configuration d'un drawable de type {@code <bitmap>}.
 *
 * <p>Stocke la source ({@code android:src}, obligatoire pour produire un
 * rendu), le mode de tuilage ({@code tileMode/tileModeX/tileModeY}), la
 * gravité, et les drapeaux de qualité ({@code antialias}, {@code dither},
 * {@code filter}), l'alpha et le tint.</p>
 *
 * <p>Exemple de source :</p>
 * <pre>{@code
 * <bitmap xmlns:android="http://schemas.android.com/apk/res/android"
 *         android:src="@drawable/tuile"
 *         android:tileMode="repeat"
 *         android:gravity="center"
 *         android:antialias="true"
 *         android:alpha="0.5"/>
 * }</pre>
 *
 * @author jo@Dev
 * @since 3.0
 */
public class BitmapConfig {

    /** Référence de la source ({@code @drawable/…} ou {@code @color/…}), ou {@code null}. */
    private String srcRef;

    /** Mode de tuilage combiné ({@code clamp}, {@code repeat}, {@code mirror}, {@code disabled}). */
    private String tileMode;

    /** Mode de tuilage horizontal, ou {@code null} si absent. */
    private String tileModeX;

    /** Mode de tuilage vertical, ou {@code null} si absent. */
    private String tileModeY;

    /** Gravité combinée (constantes {@code android.view.Gravity}), ou {@code null}. */
    private Integer gravity;

    /** Antialiasing activé, ou {@code null} si absent. */
    private Boolean antialias;

    /** Dithering activé, ou {@code null} si absent. */
    private Boolean dither;

    /** Filtrage bilinéaire activé, ou {@code null} si absent. */
    private Boolean filter;

    /** Alpha global [0..1], ou {@code null} si absent. */
    private Float alpha;

    /** Couleur de tint (valeur brute, ex. {@code "#FF4081"}), ou {@code null}. */
    private String tint;

    /** Mode de tint ({@code add}, {@code multiply}, {@code screen}, …), ou {@code null}. */
    private String tintMode;

    public BitmapConfig() {
    }

    /**
     * Indique si une source est renseignée.
     *
     * @return {@code true} si {@code android:src} est présent
     */
    public boolean hasSource() {
        return srcRef != null && !srcRef.isEmpty();
    }

    public String getSrcRef() { return srcRef; }
    public void setSrcRef(String srcRef) { this.srcRef = srcRef; }

    public String getTileMode() { return tileMode; }
    public void setTileMode(String tileMode) { this.tileMode = tileMode; }

    public String getTileModeX() { return tileModeX; }
    public void setTileModeX(String tileModeX) { this.tileModeX = tileModeX; }

    public String getTileModeY() { return tileModeY; }
    public void setTileModeY(String tileModeY) { this.tileModeY = tileModeY; }

    public Integer getGravity() { return gravity; }
    public void setGravity(Integer gravity) { this.gravity = gravity; }

    public Boolean getAntialias() { return antialias; }
    public void setAntialias(Boolean antialias) { this.antialias = antialias; }

    public Boolean getDither() { return dither; }
    public void setDither(Boolean dither) { this.dither = dither; }

    public Boolean getFilter() { return filter; }
    public void setFilter(Boolean filter) { this.filter = filter; }

    public Float getAlpha() { return alpha; }
    public void setAlpha(Float alpha) { this.alpha = alpha; }

    public String getTint() { return tint; }
    public void setTint(String tint) { this.tint = tint; }

    public String getTintMode() { return tintMode; }
    public void setTintMode(String tintMode) { this.tintMode = tintMode; }
}
