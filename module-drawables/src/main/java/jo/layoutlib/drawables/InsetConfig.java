package jo.layoutlib.drawables;

/**
 * Configuration d'un drawable de type {@code <inset>}.
 *
 * <p>Stocke le drawable encapsulé (référence ou XML imbriqué) et les quatre
 * insets (left/top/right/bottom, en pixels) qui resserrent le drawable dans
 * ses propres bounds.</p>
 *
 * <p>Exemple de source :</p>
 * <pre>{@code
 * <inset xmlns:android="http://schemas.android.com/apk/res/android"
 *        android:drawable="@drawable/fond"
 *        android:insetLeft="8dp" android:insetTop="4dp"
 *        android:insetRight="8dp" android:insetBottom="4dp"/>
 * }</pre>
 *
 * @author jo@Dev
 * @since 3.0
 */
public class InsetConfig {

    /** Référence du drawable encapsulé, ou {@code null} si imbriqué. */
    private String drawableRef;

    /** XML imbriqué capturé (shape/selector/vector inline), ou {@code null}. */
    private String nestedXml;

    /** Inset gauche en pixels, ou {@code null} si absent. */
    private Float insetLeft;

    /** Inset haut en pixels, ou {@code null} si absent. */
    private Float insetTop;

    /** Inset droite en pixels, ou {@code null} si absent. */
    private Float insetRight;

    /** Inset bas en pixels, ou {@code null} si absent. */
    private Float insetBottom;

    public InsetConfig() {
    }

    /**
     * Indique si la config définit une source de drawable
     * (référence ou XML imbriqué).
     *
     * @return {@code true} si l'inset encapsule quelque chose
     */
    public boolean hasDrawableSource() {
        return (drawableRef != null && !drawableRef.isEmpty())
                || (nestedXml != null && !nestedXml.isEmpty());
    }

    /**
     * Indique si au moins un inset est renseigné.
     *
     * @return {@code true} si un inset est présent
     */
    public boolean hasAnyInset() {
        return insetLeft != null || insetTop != null
                || insetRight != null || insetBottom != null;
    }

    public String getDrawableRef() { return drawableRef; }
    public void setDrawableRef(String ref) { this.drawableRef = ref; }

    public String getNestedXml() { return nestedXml; }
    public void setNestedXml(String xml) { this.nestedXml = xml; }

    public Float getInsetLeft() { return insetLeft; }
    public void setInsetLeft(Float insetLeft) { this.insetLeft = insetLeft; }

    public Float getInsetTop() { return insetTop; }
    public void setInsetTop(Float insetTop) { this.insetTop = insetTop; }

    public Float getInsetRight() { return insetRight; }
    public void setInsetRight(Float insetRight) { this.insetRight = insetRight; }

    public Float getInsetBottom() { return insetBottom; }
    public void setInsetBottom(Float insetBottom) { this.insetBottom = insetBottom; }
}
