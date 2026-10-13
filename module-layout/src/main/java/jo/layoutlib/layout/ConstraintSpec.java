package jo.layoutlib.layout;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Instantané des attributs de contrainte ConstraintLayout d'une vue.
 *
 * <p>Capture immuable des attributs {@code app:layout_constraint*} (ancres,
 * biais, pourcentages, ratios, chaînes, marges) et des attributs des
 * helpers ({@code Guideline}, {@code Barrier}) au moment de l'inflation,
 * pendant que le parser XML est encore sur le {@code START_TAG} de
 * l'élément. {@link ConstraintSolver} consomme ces instantanés pour
 * construire son système Cassowary.</p>
 *
 * <h2>Attributs reconnus</h2>
 * <ul>
 *   <li>Ancres : {@code layout_constraint<Top|Bottom|Start|End|Left|Right|Baseline>_to<Side>Of}</li>
 *   <li>Biais : {@code layout_constraintHorizontal_bias}, {@code layout_constraintVertical_bias}</li>
 *   <li>Pourcentages : {@code layout_constraintWidth_percent}, {@code layout_constraintHeight_percent}</li>
 *   <li>Ratio : {@code layout_constraintDimensionRatio} (ex. {@code "16:9"}, {@code "W,16:9"}, {@code "H,1:2"})</li>
 *   <li>Chaînes : {@code layout_constraintHorizontal_chainStyle} (spread/packed/weighted),
 *       {@code layout_constraintHorizontal_weight}, idem vertical</li>
 *   <li>Marges : {@code layout_marginTop/Bottom/Start/End/Left/Right}</li>
 *   <li>Guideline : {@code layout_constraintGuide_begin/end/percent}, {@code android:orientation}</li>
 *   <li>Barrier : {@code barrierDirection}, {@code constraint_referenced_ids}</li>
 * </ul>
 *
 * <p>Le parsing est tolérant : une valeur numérique invalide retombe sur la
 * valeur par défaut, jamais d'exception (philosophie de rendu LayoutLib).</p>
 *
 * @author jo@Dev
 * @since 3.0
 */
public final class ConstraintSpec {

    /** Styles de chaîne ConstraintLayout. */
    public enum ChainStyle {
        CHAIN_SPREAD, CHAIN_SPREAD_INSIDE, CHAIN_PACKED
    }

    /** Directions de barrière. */
    public enum BarrierDirection {
        LEFT, RIGHT, TOP, BOTTOM, START, END
    }

    // --- Ancres (références : "parent", "@id/foo", "@+id/foo") ---

    private final String topToTopOf;
    private final String topToBottomOf;
    private final String bottomToTopOf;
    private final String bottomToBottomOf;
    private final String baselineToBaselineOf;
    private final String startToStartOf;
    private final String startToEndOf;
    private final String endToStartOf;
    private final String endToEndOf;
    private final String leftToLeftOf;
    private final String leftToRightOf;
    private final String rightToLeftOf;
    private final String rightToRightOf;

    // --- Biais et dimensions ---

    private final float horizontalBias;
    private final float verticalBias;
    private final float widthPercent;
    private final float heightPercent;

    /** Ratio brut (ex. {@code "16:9"}), ou {@code null}. */
    private final String dimensionRatio;
    /** Côté piloté par le ratio : true = largeur ({@code W,…}), false = hauteur. */
    private final boolean ratioDrivesWidth;
    /** Numérateur du ratio. */
    private final float ratioNumerator;
    /** Dénominateur du ratio. */
    private final float ratioDenominator;

    // --- Chaînes ---

    private final ChainStyle horizontalChainStyle;
    private final ChainStyle verticalChainStyle;
    private final float horizontalWeight;
    private final float verticalWeight;

    // --- Marges (pixels) ---

    private final int marginTop;
    private final int marginBottom;
    private final int marginStart;
    private final int marginEnd;

    // --- Dimensions déclarées (layout_width/layout_height) ---

    /** {@code true} si layout_width = 0dp/match_constraint. */
    private final boolean widthMatchConstraint;

    /** {@code true} si layout_height = 0dp/match_constraint. */
    private final boolean heightMatchConstraint;

    // --- Guideline ---

    private final boolean guideline;
    private final boolean guidelineVertical;
    private final Integer guideBegin;
    private final Integer guideEnd;
    private final Float guidePercent;

    // --- Barrier ---

    private final boolean barrier;
    private final BarrierDirection barrierDirection;
    private final String referencedIds;

    private ConstraintSpec(Builder builder) {
        this.topToTopOf = builder.topToTopOf;
        this.topToBottomOf = builder.topToBottomOf;
        this.bottomToTopOf = builder.bottomToTopOf;
        this.bottomToBottomOf = builder.bottomToBottomOf;
        this.baselineToBaselineOf = builder.baselineToBaselineOf;
        this.startToStartOf = builder.startToStartOf;
        this.startToEndOf = builder.startToEndOf;
        this.endToStartOf = builder.endToStartOf;
        this.endToEndOf = builder.endToEndOf;
        this.leftToLeftOf = builder.leftToLeftOf;
        this.leftToRightOf = builder.leftToRightOf;
        this.rightToLeftOf = builder.rightToLeftOf;
        this.rightToRightOf = builder.rightToRightOf;
        this.horizontalBias = builder.horizontalBias;
        this.verticalBias = builder.verticalBias;
        this.widthPercent = builder.widthPercent;
        this.heightPercent = builder.heightPercent;
        this.dimensionRatio = builder.dimensionRatio;
        this.ratioDrivesWidth = builder.ratioDrivesWidth;
        this.ratioNumerator = builder.ratioNumerator;
        this.ratioDenominator = builder.ratioDenominator;
        this.horizontalChainStyle = builder.horizontalChainStyle;
        this.verticalChainStyle = builder.verticalChainStyle;
        this.horizontalWeight = builder.horizontalWeight;
        this.verticalWeight = builder.verticalWeight;
        this.marginTop = builder.marginTop;
        this.marginBottom = builder.marginBottom;
        this.marginStart = builder.marginStart;
        this.marginEnd = builder.marginEnd;
        this.guideline = builder.guideline;
        this.guidelineVertical = builder.guidelineVertical;
        this.guideBegin = builder.guideBegin;
        this.guideEnd = builder.guideEnd;
        this.guidePercent = builder.guidePercent;
        this.barrier = builder.barrier;
        this.barrierDirection = builder.barrierDirection;
        this.referencedIds = builder.referencedIds;
        this.widthMatchConstraint = builder.widthMatchConstraint;
        this.heightMatchConstraint = builder.heightMatchConstraint;
    }

    /**
     * Construit un spec depuis une map d'attributs (ns-agnostique : les clés
     * sont les noms d'attributs nus, ex. {@code layout_constraintTop_toTopOf}).
     *
     * @param attributes la map attribut → valeur (peut être {@code null})
     * @return le spec (jamais {@code null})
     */
    public static ConstraintSpec fromAttributes(Map<String, String> attributes) {
        Builder b = new Builder();
        if (attributes == null) {
            return b.build();
        }
        b.topToTopOf = attributes.get("layout_constraintTop_toTopOf");
        b.topToBottomOf = attributes.get("layout_constraintTop_toBottomOf");
        b.bottomToTopOf = attributes.get("layout_constraintBottom_toTopOf");
        b.bottomToBottomOf = attributes.get("layout_constraintBottom_toBottomOf");
        b.baselineToBaselineOf = attributes.get("layout_constraintBaseline_toBaselineOf");
        b.startToStartOf = attributes.get("layout_constraintStart_toStartOf");
        b.startToEndOf = attributes.get("layout_constraintStart_toEndOf");
        b.endToStartOf = attributes.get("layout_constraintEnd_toStartOf");
        b.endToEndOf = attributes.get("layout_constraintEnd_toEndOf");
        b.leftToLeftOf = attributes.get("layout_constraintLeft_toLeftOf");
        b.leftToRightOf = attributes.get("layout_constraintLeft_toRightOf");
        b.rightToLeftOf = attributes.get("layout_constraintRight_toLeftOf");
        b.rightToRightOf = attributes.get("layout_constraintRight_toRightOf");

        b.horizontalBias = parseFloat(
                attributes.get("layout_constraintHorizontal_bias"), 0.5f);
        b.verticalBias = parseFloat(
                attributes.get("layout_constraintVertical_bias"), 0.5f);
        b.widthPercent = parseFloat(
                attributes.get("layout_constraintWidth_percent"), -1f);
        b.heightPercent = parseFloat(
                attributes.get("layout_constraintHeight_percent"), -1f);

        String ratio = attributes.get("layout_constraintDimensionRatio");
        if (ratio != null && !ratio.trim().isEmpty()) {
            b.dimensionRatio = ratio.trim();
            parseRatio(b, ratio.trim());
        }

        b.horizontalChainStyle = parseChainStyle(
                attributes.get("layout_constraintHorizontal_chainStyle"));
        b.verticalChainStyle = parseChainStyle(
                attributes.get("layout_constraintVertical_chainStyle"));
        b.horizontalWeight = parseFloat(
                attributes.get("layout_constraintHorizontal_weight"), -1f);
        b.verticalWeight = parseFloat(
                attributes.get("layout_constraintVertical_weight"), -1f);

        b.marginTop = parseDimen(attributes.get("layout_marginTop"));
        b.marginBottom = parseDimen(attributes.get("layout_marginBottom"));
        b.marginStart = parseDimen(firstNonNull(
                attributes.get("layout_marginStart"), attributes.get("layout_marginLeft")));
        b.marginEnd = parseDimen(firstNonNull(
                attributes.get("layout_marginEnd"), attributes.get("layout_marginRight")));

        b.widthMatchConstraint = isMatchConstraint(
                firstNonNull(attributes.get("layout_width"),
                        attributes.get("android:layout_width")));
        b.heightMatchConstraint = isMatchConstraint(
                firstNonNull(attributes.get("layout_height"),
                        attributes.get("android:layout_height")));

        // Guideline : détecté par la présence d'un attribut guide
        Integer begin = parseDimenNullable(attributes.get("layout_constraintGuide_begin"));
        Integer end = parseDimenNullable(attributes.get("layout_constraintGuide_end"));
        Float percent = parseFloatNullable(
                attributes.get("layout_constraintGuide_percent"));
        if (begin != null || end != null || percent != null) {
            b.guideline = true;
            b.guideBegin = begin;
            b.guideEnd = end;
            b.guidePercent = percent;
            String orientation = firstNonNull(
                    attributes.get("orientation"), attributes.get("android:orientation"));
            // Une guideline verticale sépare gauche/droite (default Android)
            b.guidelineVertical = !"horizontal".equalsIgnoreCase(orientation);
        }

        // Barrier
        String direction = attributes.get("barrierDirection");
        if (direction != null && !direction.trim().isEmpty()) {
            b.barrier = true;
            b.barrierDirection = parseBarrierDirection(direction.trim());
            b.referencedIds = attributes.get("constraint_referenced_ids");
        }
        return b.build();
    }

    /**
     * Parse un ratio de dimension.
     *
     * <p>Formats : {@code "16:9"} (largeur pilotée par défaut),
     * {@code "W,16:9"} (largeur = num/den × hauteur),
     * {@code "H,1:2"} (hauteur = num/den × largeur), ou un simple
     * flottant {@code "1.5"} (W/H).</p>
     */
    private static void parseRatio(Builder b, String raw) {
        String value = raw;
        boolean drivesWidth = true;
        if (value.length() > 2 && (value.charAt(1) == ',' || value.charAt(1) == ';')) {
            char side = Character.toUpperCase(value.charAt(0));
            drivesWidth = side == 'W';
            value = value.substring(2);
        }
        float num;
        float den = 1f;
        int colon = value.indexOf(':');
        if (colon < 0) {
            colon = value.indexOf(';');
        }
        try {
            if (colon >= 0) {
                num = Float.parseFloat(value.substring(0, colon).trim());
                den = Float.parseFloat(value.substring(colon + 1).trim());
            } else {
                num = Float.parseFloat(value.trim());
            }
        } catch (NumberFormatException e) {
            return; // ratio invalide — ignoré, spec sans ratio
        }
        if (num <= 0f || den <= 0f) {
            return;
        }
        b.ratioDrivesWidth = drivesWidth;
        b.ratioNumerator = num;
        b.ratioDenominator = den;
    }

    private static ChainStyle parseChainStyle(String value) {
        if (value == null) {
            return ChainStyle.CHAIN_SPREAD;
        }
        switch (value.trim().toLowerCase()) {
            case "packed":          return ChainStyle.CHAIN_PACKED;
            case "spread_inside":   return ChainStyle.CHAIN_SPREAD_INSIDE;
            case "spread":
            default:                return ChainStyle.CHAIN_SPREAD;
        }
    }

    private static BarrierDirection parseBarrierDirection(String value) {
        switch (value.toLowerCase()) {
            case "left":    return BarrierDirection.LEFT;
            case "right":   return BarrierDirection.RIGHT;
            case "top":     return BarrierDirection.TOP;
            case "bottom":  return BarrierDirection.BOTTOM;
            case "end":     return BarrierDirection.END;
            case "start":
            default:        return BarrierDirection.START;
        }
    }

    private static float parseFloat(String value, float fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Float parseFloatNullable(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int parseDimen(String value) {
        Integer parsed = parseDimenNullable(value);
        return parsed != null ? parsed : 0;
    }

    private static Integer parseDimenNullable(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        // Retire les unités (dp etc.) — la conversion dp→px relève du
        // convertisseur du côté de l'inflater, qui fournit déjà des px.
        String cleaned = value.trim().replaceAll("[^0-9.\\-]", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return Math.round(Float.parseFloat(cleaned));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String firstNonNull(String first, String second) {
        return first != null ? first : second;
    }

    /**
     * Indique si une valeur layout_width/layout_height vaut 0dp
     * (match_constraint en ConstraintLayout).
     */
    private static boolean isMatchConstraint(String value) {
        if (value == null) {
            return false;
        }
        String v = value.trim();
        return "0dp".equals(v) || "0dip".equals(v) || "match_constraint".equals(v);
    }

    // --- Accesseurs ---

    public String getTopToTopOf() { return topToTopOf; }
    public String getTopToBottomOf() { return topToBottomOf; }
    public String getBottomToTopOf() { return bottomToTopOf; }
    public String getBottomToBottomOf() { return bottomToBottomOf; }
    public String getBaselineToBaselineOf() { return baselineToBaselineOf; }
    public String getStartToStartOf() { return startToStartOf; }
    public String getStartToEndOf() { return startToEndOf; }
    public String getEndToStartOf() { return endToStartOf; }
    public String getEndToEndOf() { return endToEndOf; }
    public String getLeftToLeftOf() { return leftToLeftOf; }
    public String getLeftToRightOf() { return leftToRightOf; }
    public String getRightToLeftOf() { return rightToLeftOf; }
    public String getRightToRightOf() { return rightToRightOf; }

    public float getHorizontalBias() { return horizontalBias; }
    public float getVerticalBias() { return verticalBias; }
    public float getWidthPercent() { return widthPercent; }
    public float getHeightPercent() { return heightPercent; }

    /** @return le ratio brut, ou {@code null} si absent/invalide */
    public String getDimensionRatio() { return dimensionRatio; }
    /** @return {@code true} si le ratio est valide et présent */
    public boolean hasRatio() {
        return ratioNumerator > 0f && ratioDenominator > 0f;
    }
    /** @return {@code true} si le ratio pilote la largeur (côté W) */
    public boolean ratioDrivesWidth() { return ratioDrivesWidth; }
    public float getRatioNumerator() { return ratioNumerator; }
    public float getRatioDenominator() { return ratioDenominator; }

    public ChainStyle getHorizontalChainStyle() { return horizontalChainStyle; }
    public ChainStyle getVerticalChainStyle() { return verticalChainStyle; }
    public float getHorizontalWeight() { return horizontalWeight; }
    public float getVerticalWeight() { return verticalWeight; }

    public int getMarginTop() { return marginTop; }
    public int getMarginBottom() { return marginBottom; }
    public int getMarginStart() { return marginStart; }
    public int getMarginEnd() { return marginEnd; }

    public boolean isGuideline() { return guideline; }
    /** @return {@code true} si la guideline est verticale (sépare gauche/droite) */
    public boolean isGuidelineVertical() { return guidelineVertical; }
    public Integer getGuideBegin() { return guideBegin; }
    public Integer getGuideEnd() { return guideEnd; }
    public Float getGuidePercent() { return guidePercent; }

    /** @return {@code true} si layout_width = 0dp (match_constraint) */
    public boolean isWidthMatchConstraint() { return widthMatchConstraint; }

    /** @return {@code true} si layout_height = 0dp (match_constraint) */
    public boolean isHeightMatchConstraint() { return heightMatchConstraint; }

    public boolean isBarrier() { return barrier; }
    public BarrierDirection getBarrierDirection() { return barrierDirection; }
    public String getReferencedIds() { return referencedIds; }

    /** @return {@code true} si le spec porte au moins une ancre */
    public boolean hasAnyAnchor() {
        return topToTopOf != null || topToBottomOf != null
                || bottomToTopOf != null || bottomToBottomOf != null
                || baselineToBaselineOf != null
                || startToStartOf != null || startToEndOf != null
                || endToStartOf != null || endToEndOf != null
                || leftToLeftOf != null || leftToRightOf != null
                || rightToLeftOf != null || rightToRightOf != null;
    }

    /** @return {@code true} si le spec porte au moins une ancre horizontale */
    public boolean hasHorizontalAnchor() {
        return startToStartOf != null || startToEndOf != null
                || endToStartOf != null || endToEndOf != null
                || leftToLeftOf != null || leftToRightOf != null
                || rightToLeftOf != null || rightToRightOf != null;
    }

    /** @return {@code true} si le spec porte au moins une ancre verticale */
    public boolean hasVerticalAnchor() {
        return topToTopOf != null || topToBottomOf != null
                || bottomToTopOf != null || bottomToBottomOf != null
                || baselineToBaselineOf != null;
    }

    /**
     * Construit la map d'attributs équivalente (inverse de
     * {@link #fromAttributes(Map)} — utile pour les tests et le debug).
     *
     * @return la map non-modifiable attribut → valeur
     */
    public Map<String, String> toAttributes() {
        Map<String, String> m = new HashMap<>();
        putIfNotNull(m, "layout_constraintTop_toTopOf", topToTopOf);
        putIfNotNull(m, "layout_constraintTop_toBottomOf", topToBottomOf);
        putIfNotNull(m, "layout_constraintBottom_toTopOf", bottomToTopOf);
        putIfNotNull(m, "layout_constraintBottom_toBottomOf", bottomToBottomOf);
        putIfNotNull(m, "layout_constraintBaseline_toBaselineOf", baselineToBaselineOf);
        putIfNotNull(m, "layout_constraintStart_toStartOf", startToStartOf);
        putIfNotNull(m, "layout_constraintStart_toEndOf", startToEndOf);
        putIfNotNull(m, "layout_constraintEnd_toStartOf", endToStartOf);
        putIfNotNull(m, "layout_constraintEnd_toEndOf", endToEndOf);
        putIfNotNull(m, "layout_constraintLeft_toLeftOf", leftToLeftOf);
        putIfNotNull(m, "layout_constraintLeft_toRightOf", leftToRightOf);
        putIfNotNull(m, "layout_constraintRight_toLeftOf", rightToLeftOf);
        putIfNotNull(m, "layout_constraintRight_toRightOf", rightToRightOf);
        if (horizontalBias != 0.5f) {
            m.put("layout_constraintHorizontal_bias", String.valueOf(horizontalBias));
        }
        if (verticalBias != 0.5f) {
            m.put("layout_constraintVertical_bias", String.valueOf(verticalBias));
        }
        if (widthPercent >= 0f) {
            m.put("layout_constraintWidth_percent", String.valueOf(widthPercent));
        }
        if (heightPercent >= 0f) {
            m.put("layout_constraintHeight_percent", String.valueOf(heightPercent));
        }
        putIfNotNull(m, "layout_constraintDimensionRatio", dimensionRatio);
        if (marginTop != 0) m.put("layout_marginTop", String.valueOf(marginTop));
        if (marginBottom != 0) m.put("layout_marginBottom", String.valueOf(marginBottom));
        if (marginStart != 0) m.put("layout_marginStart", String.valueOf(marginStart));
        if (marginEnd != 0) m.put("layout_marginEnd", String.valueOf(marginEnd));
        if (guideline) {
            if (guideBegin != null) {
                m.put("layout_constraintGuide_begin", String.valueOf(guideBegin));
            }
            if (guideEnd != null) {
                m.put("layout_constraintGuide_end", String.valueOf(guideEnd));
            }
            if (guidePercent != null) {
                m.put("layout_constraintGuide_percent", String.valueOf(guidePercent));
            }
        }
        if (barrier && barrierDirection != null) {
            m.put("barrierDirection", barrierDirection.name().toLowerCase());
            putIfNotNull(m, "constraint_referenced_ids", referencedIds);
        }
        return Collections.unmodifiableMap(m);
    }

    private static void putIfNotNull(Map<String, String> m, String key, String value) {
        if (value != null) {
            m.put(key, value);
        }
    }

    /** Builder interne. */
    private static class Builder {
        String topToTopOf;
        String topToBottomOf;
        String bottomToTopOf;
        String bottomToBottomOf;
        String baselineToBaselineOf;
        String startToStartOf;
        String startToEndOf;
        String endToStartOf;
        String endToEndOf;
        String leftToLeftOf;
        String leftToRightOf;
        String rightToLeftOf;
        String rightToRightOf;
        float horizontalBias = 0.5f;
        float verticalBias = 0.5f;
        float widthPercent = -1f;
        float heightPercent = -1f;
        String dimensionRatio;
        boolean ratioDrivesWidth = true;
        float ratioNumerator = -1f;
        float ratioDenominator = -1f;
        ChainStyle horizontalChainStyle = ChainStyle.CHAIN_SPREAD;
        ChainStyle verticalChainStyle = ChainStyle.CHAIN_SPREAD;
        float horizontalWeight = -1f;
        float verticalWeight = -1f;
        int marginTop;
        int marginBottom;
        int marginStart;
        int marginEnd;
        boolean widthMatchConstraint;
        boolean heightMatchConstraint;
        boolean guideline;
        boolean guidelineVertical = true;
        Integer guideBegin;
        Integer guideEnd;
        Float guidePercent;
        boolean barrier;
        BarrierDirection barrierDirection;
        String referencedIds;

        ConstraintSpec build() {
            return new ConstraintSpec(this);
        }
    }
}
