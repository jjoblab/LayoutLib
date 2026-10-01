package jo.layoutlib.layout;

import android.view.View;
import android.view.ViewGroup;

import org.xmlpull.v1.XmlPullParser;

import java.util.HashMap;
import java.util.Map;

/**
 * Résolveur de contraintes ConstraintLayout, inspiré du solver Cassowary
 * utilisé par androidx.constraintlayout.
 *
 * <p>Cette classe traduit les attributs {@code app:layout_constraint*} en
 * règles de positionnement simples, sans nécessiter le solver Cassowary
 * complet. Pour chaque vue, on calcule sa position (left, top, right, bottom)
 * en fonction des contraintes par rapport au parent ou à d'autres vues.</p>
 *
 * <h2>Contraintes supportées</h2>
 * <ul>
 *   <li>Parent : {@code layout_constraintTop_toTopOf="parent"}, etc.</li>
 *   <li>Vue : {@code layout_constraintTop_toBottomOf="@id/other"}, etc.</li>
 *   <li>Biais : {@code layout_constraintHorizontal_bias="0.5"}, etc.</li>
 *   <li>Pourcentage : {@code layout_constraintWidth_percent="0.5"}, etc.</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ConstraintSolver {

    private static final String NS_APP = "http://schemas.android.com/apk/res-auto";
    private static final String PARENT = "parent";

    /** Structure pour stocker les contraintes d'une vue. */
    public static class ConstraintInfo {
        public String topToTop;
        public String topToBottomOf;
        public String bottomToTopOf;
        public String bottomToBottomOf;
        public String startToStartOf;
        public String startToEndOf;
        public String endToStartOf;
        public String endToEndOf;
        public String leftToLeftOf;
        public String leftToRightOf;
        public String rightToLeftOf;
        public String rightToRightOf;
        public float horizontalBias = 0.5f;
        public float verticalBias = 0.5f;
        public float widthPercent = -1f;
        public float heightPercent = -1f;
        public int marginTop = 0;
        public int marginBottom = 0;
        public int marginStart = 0;
        public int marginEnd = 0;
        public int marginLeft = 0;
        public int marginRight = 0;

        public boolean hasHorizontalConstraint() {
            return startToStartOf != null || startToEndOf != null
                    || endToStartOf != null || endToEndOf != null
                    || leftToLeftOf != null || leftToRightOf != null
                    || rightToLeftOf != null || rightToRightOf != null;
        }

        public boolean hasVerticalConstraint() {
            return topToTop != null || topToBottomOf != null
                    || bottomToTopOf != null || bottomToBottomOf != null;
        }
    }

    private final Map<View, ConstraintInfo> constraints = new HashMap<>();
    private final Map<String, View> viewsById = new HashMap<>();
    private int parentWidth = 0;
    private int parentHeight = 0;

    /**
     * Enregistre une vue avec son id et ses contraintes.
     *
     * @param view   la vue
     * @param idName le nom de l'id (ex. "button_save")
     * @param parser le parser sur START_TAG de la vue
     */
    public void registerView(View view, String idName, XmlPullParser parser) {
        ConstraintInfo info = parseConstraints(parser);
        constraints.put(view, info);
        if (idName != null) {
            viewsById.put(idName, view);
        }
    }

    /**
     * Parse les contraintes depuis le parser.
     *
     * @param parser le parser sur START_TAG
     * @return les infos de contrainte
     */
    private ConstraintInfo parseConstraints(XmlPullParser parser) {
        ConstraintInfo info = new ConstraintInfo();
        info.topToTop = getAppAttr(parser, "layout_constraintTop_toTopOf");
        info.topToBottomOf = getAppAttr(parser, "layout_constraintTop_toBottomOf");
        info.bottomToTopOf = getAppAttr(parser, "layout_constraintBottom_toTopOf");
        info.bottomToBottomOf = getAppAttr(parser, "layout_constraintBottom_toBottomOf");
        info.startToStartOf = getAppAttr(parser, "layout_constraintStart_toStartOf");
        info.startToEndOf = getAppAttr(parser, "layout_constraintStart_toEndOf");
        info.endToStartOf = getAppAttr(parser, "layout_constraintEnd_toStartOf");
        info.endToEndOf = getAppAttr(parser, "layout_constraintEnd_toEndOf");
        info.leftToLeftOf = getAppAttr(parser, "layout_constraintLeft_toLeftOf");
        info.leftToRightOf = getAppAttr(parser, "layout_constraintLeft_toRightOf");
        info.rightToLeftOf = getAppAttr(parser, "layout_constraintRight_toLeftOf");
        info.rightToRightOf = getAppAttr(parser, "layout_constraintRight_toRightOf");

        String hBias = getAppAttr(parser, "layout_constraintHorizontal_bias");
        if (hBias != null) { try { info.horizontalBias = Float.parseFloat(hBias); } catch (Exception e) {} }
        String vBias = getAppAttr(parser, "layout_constraintVertical_bias");
        if (vBias != null) { try { info.verticalBias = Float.parseFloat(vBias); } catch (Exception e) {} }

        String wPercent = getAppAttr(parser, "layout_constraintWidth_percent");
        if (wPercent != null) { try { info.widthPercent = Float.parseFloat(wPercent); } catch (Exception e) {} }
        String hPercent = getAppAttr(parser, "layout_constraintHeight_percent");
        if (hPercent != null) { try { info.heightPercent = Float.parseFloat(hPercent); } catch (Exception e) {} }

        // Marges
        String marginTop = getAppAttr(parser, "layout_constraintTop_margin");
        if (marginTop == null) marginTop = getAppAttr(parser, "layout_marginTop");
        if (marginTop != null) { try { info.marginTop = Integer.parseInt(marginTop.replaceAll("[^0-9-]", "")); } catch (Exception e) {} }

        String marginBottom = getAppAttr(parser, "layout_constraintBottom_margin");
        if (marginBottom == null) marginBottom = getAppAttr(parser, "layout_marginBottom");
        if (marginBottom != null) { try { info.marginBottom = Integer.parseInt(marginBottom.replaceAll("[^0-9-]", "")); } catch (Exception e) {} }

        return info;
    }

    /**
     * Résout toutes les contraintes et positionne les vues.
     *
     * @param parentWidth  largeur du parent en pixels
     * @param parentHeight hauteur du parent en pixels
     */
    public void solve(int parentWidth, int parentHeight) {
        this.parentWidth = parentWidth;
        this.parentHeight = parentHeight;

        // Plusieurs passes pour résoudre les dépendances
        for (int pass = 0; pass < 3; pass++) {
            for (Map.Entry<View, ConstraintInfo> entry : constraints.entrySet()) {
                View view = entry.getKey();
                ConstraintInfo info = entry.getValue();
                layoutView(view, info);
            }
        }
    }

    /**
     * Positionne une vue selon ses contraintes.
     *
     * @param view la vue
     * @param info ses contraintes
     */
    private void layoutView(View view, ConstraintInfo info) {
        int measuredWidth = view.getMeasuredWidth();
        int measuredHeight = view.getMeasuredHeight();

        // Position horizontale
        int left = -1;
        int right = -1;

        if (info.widthPercent > 0) {
            measuredWidth = (int) (parentWidth * info.widthPercent);
        }

        // Contraintes horizontales
        if (info.startToStartOf != null && PARENT.equals(info.startToStartOf)) {
            left = info.marginLeft;
            if (info.endToEndOf != null && PARENT.equals(info.endToEndOf)) {
                // Centré horizontalement avec biais
                int available = parentWidth - measuredWidth - info.marginLeft - info.marginRight;
                left = info.marginLeft + (int) (available * info.horizontalBias);
            }
            right = left + measuredWidth;
        } else if (info.startToEndOf != null) {
            View other = resolveRef(info.startToEndOf);
            if (other != null) {
                left = other.getRight() + info.marginLeft;
                right = left + measuredWidth;
            }
        } else if (info.leftToLeftOf != null && PARENT.equals(info.leftToLeftOf)) {
            left = info.marginLeft;
            right = left + measuredWidth;
        } else if (info.leftToRightOf != null) {
            View other = resolveRef(info.leftToRightOf);
            if (other != null) {
                left = other.getRight() + info.marginLeft;
                right = left + measuredWidth;
            }
        } else if (info.endToEndOf != null && PARENT.equals(info.endToEndOf)) {
            right = parentWidth - info.marginRight;
            left = right - measuredWidth;
        } else if (info.rightToRightOf != null && PARENT.equals(info.rightToRightOf)) {
            right = parentWidth - info.marginRight;
            left = right - measuredWidth;
        } else if (info.endToStartOf != null) {
            View other = resolveRef(info.endToStartOf);
            if (other != null) {
                right = other.getLeft() - info.marginRight;
                left = right - measuredWidth;
            }
        }

        // Default: left = 0
        if (left < 0) left = info.marginLeft;
        if (right < 0) right = left + measuredWidth;

        // Position verticale
        int top = -1;
        int bottom = -1;

        if (info.heightPercent > 0) {
            measuredHeight = (int) (parentHeight * info.heightPercent);
        }

        if (info.topToTop != null && PARENT.equals(info.topToTop)) {
            top = info.marginTop;
            if (info.bottomToBottomOf != null && PARENT.equals(info.bottomToBottomOf)) {
                int available = parentHeight - measuredHeight - info.marginTop - info.marginBottom;
                top = info.marginTop + (int) (available * info.verticalBias);
            }
            bottom = top + measuredHeight;
        } else if (info.topToBottomOf != null) {
            View other = resolveRef(info.topToBottomOf);
            if (other != null) {
                top = other.getBottom() + info.marginTop;
                bottom = top + measuredHeight;
            }
        } else if (info.bottomToBottomOf != null && PARENT.equals(info.bottomToBottomOf)) {
            bottom = parentHeight - info.marginBottom;
            top = bottom - measuredHeight;
        } else if (info.bottomToTopOf != null) {
            View other = resolveRef(info.bottomToTopOf);
            if (other != null) {
                bottom = other.getTop() - info.marginBottom;
                top = bottom - measuredHeight;
            }
        }

        // Default: top = 0
        if (top < 0) top = info.marginTop;
        if (bottom < 0) bottom = top + measuredHeight;

        // Appliquer le layout
        view.layout(left, top, right, bottom);
    }

    /**
     * Résout une référence vers une autre vue.
     *
     * @param ref la référence (@id/foo, parent)
     * @return la vue, ou null
     */
    private View resolveRef(String ref) {
        if (ref == null || PARENT.equals(ref)) return null;
        String name;
        if (ref.startsWith("@+id/") || ref.startsWith("@id/")) {
            name = ref.substring(ref.indexOf('/') + 1);
        } else {
            name = ref;
        }
        return viewsById.get(name);
    }

    /**
     * Lit un attribut app:* depuis le parser.
     */
    private String getAppAttr(XmlPullParser parser, String name) {
        String value = parser.getAttributeValue(NS_APP, name);
        if (value == null) {
            int count = parser.getAttributeCount();
            for (int i = 0; i < count; i++) {
                if (name.equals(parser.getAttributeName(i))) {
                    return parser.getAttributeValue(i);
                }
            }
        }
        return value;
    }

    /**
     * @return le nombre de vues enregistrées
     */
    public int getViewCount() {
        return constraints.size();
    }

    /**
     * Vide toutes les contraintes.
     */
    public void clear() {
        constraints.clear();
        viewsById.clear();
    }
}
