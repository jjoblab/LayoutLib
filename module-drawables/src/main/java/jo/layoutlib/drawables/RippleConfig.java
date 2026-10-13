package jo.layoutlib.drawables;

/**
 * Configuration d'un drawable de type {@code <ripple>}.
 *
 * <p>Stocke la couleur du ripple (obligatoire), le contenu optionnel sur
 * lequel le ripple est superposé, et le masque optionnel qui délimite la zone
 * d'effet. Le contenu et le masque sont des {@link RippleItem} : référence
 * ({@code @drawable/}, {@code @color/}, couleur littérale) ou XML imbriqué
 * capturé (shape inline, color inline…).</p>
 *
 * <p>Exemple de source :</p>
 * <pre>{@code
 * <ripple xmlns:android="http://schemas.android.com/apk/res/android"
 *         android:color="#FF4081">
 *   <item android:id="@android:id/mask">
 *     <color android:color="#FFFFFF"/>
 *   </item>
 *   <item android:drawable="@drawable/fond"/>
 * </ripple>
 * }</pre>
 *
 * @author jo@Dev
 * @since 3.0
 */
public class RippleConfig {

    /** Couleur du ripple (ARGB), ou {@code null} si absente (drawable invalide). */
    private Integer color;

    /** Contenu superposé au ripple (optionnel), ou {@code null}. */
    private RippleItem content;

    /** Masque délimitant la zone d'effet (optionnel), ou {@code null}. */
    private RippleItem mask;

    public RippleConfig() {
    }

    public Integer getColor() { return color; }
    public void setColor(Integer color) { this.color = color; }

    public RippleItem getContent() { return content; }
    public void setContent(RippleItem content) { this.content = content; }

    public RippleItem getMask() { return mask; }
    public void setMask(RippleItem mask) { this.mask = mask; }

    /**
     * Indique si la couleur obligatoire du ripple est renseignée.
     *
     * @return {@code true} si le ripple est suffisamment défini pour être créé
     */
    public boolean hasColor() {
        return color != null;
    }

    /**
     * Un enfant de {@code <ripple>}.
     *
     * <p>Référence et XML imbriqué sont mutuellement exclusifs : la référence
     * a la priorité lorsqu'elle est présente (comportement du framework).</p>
     */
    public static class RippleItem {

        /** Référence de drawable ({@code @drawable/…}, {@code @color/…}, {@code #RRGGBB}). */
        private String drawableRef;

        /** XML imbriqué capturé (shape/color inline), ou {@code null}. */
        private String nestedXml;

        /** Identifiant déclaratif ({@code android:id}), ex. {@code @android:id/mask}. */
        private String id;

        public RippleItem() {
        }

        /**
         * Indique si cet item est le masque du ripple.
         *
         * @return {@code true} si l'id référence {@code android:id/mask}
         */
        public boolean isMask() {
            return id != null && id.endsWith("mask");
        }

        /**
         * Indique si cet item définit une source de drawable.
         *
         * @return {@code true} si l'item est résolvable
         */
        public boolean hasDrawableSource() {
            return (drawableRef != null && !drawableRef.isEmpty())
                    || (nestedXml != null && !nestedXml.isEmpty());
        }

        public String getDrawableRef() { return drawableRef; }
        public void setDrawableRef(String ref) { this.drawableRef = ref; }

        public String getNestedXml() { return nestedXml; }
        public void setNestedXml(String xml) { this.nestedXml = xml; }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }
}
