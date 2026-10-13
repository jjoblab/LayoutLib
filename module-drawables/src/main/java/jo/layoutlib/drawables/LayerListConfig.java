package jo.layoutlib.drawables;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration d'un drawable de type {@code <layer-list>}.
 *
 * <p>Stocke la liste ordonnée des calques d'un LayerDrawable. Chaque calque
 * référence un drawable soit par référence ({@code @drawable/}, {@code @color/},
 * couleur littérale), soit par un XML imbriqué capturé tel quel (shape,
 * selector, vector, color… inline), et définit des insets optionnels
 * (left/top/right/bottom) décalant le calque dans la pile.</p>
 *
 * <p>Exemple de source :</p>
 * <pre>{@code
 * <layer-list xmlns:android="http://schemas.android.com/apk/res/android">
 *   <item android:drawable="@drawable/fond"/>
 *   <item android:left="8dp" android:top="8dp">
 *     <shape android:shape="rectangle">
 *       <solid android:color="#FF6750A4"/>
 *     </shape>
 *   </item>
 * </layer-list>
 * }</pre>
 *
 * @author jo@Dev
 * @since 3.0
 */
public class LayerListConfig {

    /** Calques, dans l'ordre du document (indice 0 = calque le plus bas). */
    private final List<LayerItem> items = new ArrayList<>();

    public LayerListConfig() {
    }

    /**
     * Ajoute un calque à la fin de la pile.
     *
     * @param item le calque (ignoré si {@code null})
     */
    public void addItem(LayerItem item) {
        if (item != null) items.add(item);
    }

    public List<LayerItem> getItems() {
        return new ArrayList<>(items);
    }

    public int getItemCount() {
        return items.size();
    }

    /**
     * Un calque de {@code <layer-list>}.
     *
     * <p>Référence et XML imbriqué sont mutuellement exclusifs : si les deux
     * sont renseignés, la référence a la priorité (comportement du framework
     * Android, où l'attribut {@code android:drawable} l'emporte sur l'enfant).</p>
     */
    public static class LayerItem {

        /** Référence de drawable ({@code @drawable/…}, {@code @color/…}, {@code #RRGGBB}). */
        private String drawableRef;

        /** XML imbriqué capturé (shape/selector/vector/color inline), ou {@code null}. */
        private String nestedXml;

        /** Identifiant déclaratif ({@code android:id}), conservé à titre informatif. */
        private String id;

        /** Inset gauche en pixels, ou {@code null} si absent. */
        private Float insetLeft;

        /** Inset haut en pixels, ou {@code null} si absent. */
        private Float insetTop;

        /** Inset droite en pixels, ou {@code null} si absent. */
        private Float insetRight;

        /** Inset bas en pixels, ou {@code null} si absent. */
        private Float insetBottom;

        public LayerItem() {
        }

        /**
         * Indique si ce calque porte au moins un inset.
         *
         * @return {@code true} si un inset est renseigné
         */
        public boolean hasAnyInset() {
            return insetLeft != null || insetTop != null
                    || insetRight != null || insetBottom != null;
        }

        /**
         * Indique si ce calque définit une source de drawable
         * (référence ou XML imbriqué).
         *
         * @return {@code true} si le calque est résolvable
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

        public Float getInsetLeft() { return insetLeft; }
        public void setInsetLeft(Float insetLeft) { this.insetLeft = insetLeft; }

        public Float getInsetTop() { return insetTop; }
        public void setInsetTop(Float insetTop) { this.insetTop = insetTop; }

        public Float getInsetRight() { return insetRight; }
        public void setInsetRight(Float insetRight) { this.insetRight = insetRight; }

        public Float getInsetBottom() { return insetBottom; }
        public void setInsetBottom(Float insetBottom) { this.insetBottom = insetBottom; }
    }
}
