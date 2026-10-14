package jo.layoutlib.drawables;

/**
 * Configuration d'un drawable XML de type {@code <adaptive-icon>}
 * (icône adaptative API 26+, ADR 0116 — P4/L6).
 *
 * <p>Une icône adaptative superpose trois couches, chacune désignée par une
 * référence {@code @drawable/}/  {@code @color/}/  {@code @mipmap/} ou par un
 * drawable inline :</p>
 * <ul>
 *   <li>{@code <background>} : arrière-plan plein écran (couleur ou image) ;</li>
 *   <li>{@code <foreground>} : premier plan (vecteur, image) ;</li>
 *   <li>{@code <monochrome>} (API 33+) : version monochrome pour les icônes
 *       thémées du lanceur.</li>
 * </ul>
 *
 * <p>Les deux représentations sont capturées (référence ET XML inline) ;
 * la résolution donne la priorité à la référence — même contrat que
 * {@link LayerListConfig}/{@link InsetConfig}.</p>
 *
 * @author jo@Dev
 * @since 3.0
 */
public class AdaptiveIconConfig {

    /** Référence de la couche background (ex. {@code @color/ic_launcher_bg}). */
    private String backgroundRef;

    /** XML inline de la couche background, si défini en enfant. */
    private String backgroundXml;

    /** Référence de la couche foreground. */
    private String foregroundRef;

    /** XML inline de la couche foreground. */
    private String foregroundXml;

    /** Référence de la couche monochrome (API 33+). */
    private String monochromeRef;

    /** XML inline de la couche monochrome. */
    private String monochromeXml;

    /**
     * @return la référence de la couche background, ou {@code null}
     */
    public String getBackgroundRef() {
        return backgroundRef;
    }

    /**
     * @param backgroundRef la référence (ex. {@code @color/fond})
     */
    public void setBackgroundRef(String backgroundRef) {
        this.backgroundRef = backgroundRef;
    }

    /**
     * @return le XML inline de la couche background, ou {@code null}
     */
    public String getBackgroundXml() {
        return backgroundXml;
    }

    /**
     * @param backgroundXml le XML inline (élément drawable complet)
     */
    public void setBackgroundXml(String backgroundXml) {
        this.backgroundXml = backgroundXml;
    }

    /**
     * @return la référence de la couche foreground, ou {@code null}
     */
    public String getForegroundRef() {
        return foregroundRef;
    }

    /**
     * @param foregroundRef la référence (ex. {@code @drawable/logo})
     */
    public void setForegroundRef(String foregroundRef) {
        this.foregroundRef = foregroundRef;
    }

    /**
     * @return le XML inline de la couche foreground, ou {@code null}
     */
    public String getForegroundXml() {
        return foregroundXml;
    }

    /**
     * @param foregroundXml le XML inline (élément drawable complet)
     */
    public void setForegroundXml(String foregroundXml) {
        this.foregroundXml = foregroundXml;
    }

    /**
     * @return la référence de la couche monochrome, ou {@code null}
     */
    public String getMonochromeRef() {
        return monochromeRef;
    }

    /**
     * @param monochromeRef la référence (ex. {@code @drawable/logo_mono})
     */
    public void setMonochromeRef(String monochromeRef) {
        this.monochromeRef = monochromeRef;
    }

    /**
     * @return le XML inline de la couche monochrome, ou {@code null}
     */
    public String getMonochromeXml() {
        return monochromeXml;
    }

    /**
     * @param monochromeXml le XML inline (élément drawable complet)
     */
    public void setMonochromeXml(String monochromeXml) {
        this.monochromeXml = monochromeXml;
    }

    /**
     * @return true si au moins une couche est désignée (référence ou inline)
     */
    public boolean hasAnyLayer() {
        return isPresent(backgroundRef, backgroundXml)
                || isPresent(foregroundRef, foregroundXml)
                || isPresent(monochromeRef, monochromeXml);
    }

    private static boolean isPresent(String ref, String xml) {
        return (ref != null && !ref.isEmpty()) || (xml != null && !xml.isEmpty());
    }
}
