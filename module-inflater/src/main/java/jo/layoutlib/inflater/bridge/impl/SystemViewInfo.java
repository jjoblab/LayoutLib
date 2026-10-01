package jo.layoutlib.inflater.bridge.impl;

/**
 * Informations système sur une vue (visibilité, focus), inspiré de
 * {@code com.android.layoutlib.bridge.impl.SystemViewInfo} de l'AOSP.
 *
 * <p>Cette classe étend {@link Layout.ViewInfo} en ajoutant des informations
 * système comme la visibilité, le focus, et l'accessibilité.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class SystemViewInfo extends Layout.ViewInfo {

    /** Visibilité : visible. */
    public static final int VISIBLE = 0;

    /** Visibilité : invisible (occupe de la place). */
    public static final int INVISIBLE = 4;

    /** Visibilité : gone (ne prend pas de place). */
    public static final int GONE = 8;

    /** Indique si la vue a le focus. */
    private final boolean hasFocus;

    /** Indique si la vue est focusable. */
    private final boolean focusable;

    /** Visibilité de la vue. */
    private final int visibility;

    /** Indique si la vue est enabled. */
    private final boolean enabled;

    /**
     * Construit une SystemViewInfo complète.
     *
     * @param className  nom de la classe
     * @param id         id de la vue
     * @param left       position gauche
     * @param top        position haute
     * @param right      position droite
     * @param bottom     position basse
     * @param depth      profondeur
     * @param visibility visibilité (VISIBLE / INVISIBLE / GONE)
     * @param hasFocus   {@code true} si la vue a le focus
     * @param focusable  {@code true} si la vue est focusable
     * @param enabled    {@code true} si la vue est enabled
     */
    public SystemViewInfo(String className, int id,
                          int left, int top, int right, int bottom, int depth,
                          int visibility, boolean hasFocus, boolean focusable,
                          boolean enabled) {
        super(className, id, left, top, right, bottom, depth);
        this.visibility = visibility;
        this.hasFocus = hasFocus;
        this.focusable = focusable;
        this.enabled = enabled;
    }

    /**
     * @return la visibilité
     */
    public int getVisibility() {
        return visibility;
    }

    /**
     * @return {@code true} si la vue a le focus
     */
    public boolean hasFocus() {
        return hasFocus;
    }

    /**
     * @return {@code true} si la vue est focusable
     */
    public boolean isFocusable() {
        return focusable;
    }

    /**
     * @return {@code true} si la vue est enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return {@code true} si la vue est visible
     */
    public boolean isVisible() {
        return visibility == VISIBLE;
    }

    /**
     * @return {@code true} si la vue est GONE
     */
    public boolean isGone() {
        return visibility == GONE;
    }

    @Override
    public String toString() {
        return super.toString() + " vis=" + visibility
                + " focus=" + hasFocus + " enabled=" + enabled;
    }
}
