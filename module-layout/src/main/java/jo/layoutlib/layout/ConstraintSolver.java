package jo.layoutlib.layout;

import android.util.Log;
import android.view.View;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jo.layoutlib.layout.cassowary.Constraint;
import jo.layoutlib.layout.cassowary.Expression;
import jo.layoutlib.layout.cassowary.Solver;
import jo.layoutlib.layout.cassowary.Strength;
import jo.layoutlib.layout.cassowary.Term;
import jo.layoutlib.layout.cassowary.Variable;

/**
 * Résolveur de contraintes ConstraintLayout, câblé au solver
 * {@link Solver Cassowary} (ADR 0116 — P2/L9).
 *
 * <p>Chaque vue contrainte est enregistrée avec un {@link ConstraintSpec}
 * (instantané de ses attributs {@code app:layout_constraint*} capturé à
 * l'inflation). La résolution construit un système linéaire : une variable
 * par position ({@code left}/{@code top}) et par dimension
 * ({@code width}/{@code height}), des contraintes {@code REQUIRED} pour
 * les ancrages, {@code STRONG} pour les biais, et le solveur Cassowary
 * produit les positions finales.</p>
 *
 * <h2>Fonctionnalités</h2>
 * <ul>
 *   <li>Ancres parent/vue/guideline/barrier (12 directions + baseline)</li>
 *   <li>Biais horizontal/vertical (répartition de l'espace libre)</li>
 *   <li>Pourcentages ({@code layout_constraintWidth_percent})</li>
 *   <li>Ratios ({@code layout_constraintDimensionRatio} : {@code 16:9},
 *       {@code W,16:9}, {@code H,1:2}) — couplage cross-axe</li>
 *   <li>Chaînes ({@code spread}, {@code spread_inside}, {@code packed},
 *       pondérées via {@code layout_constraintHorizontal_weight})</li>
 *   <li>Guidelines ({@code begin}, {@code end}, {@code percent})</li>
 *   <li>Barrières ({@code barrierDirection} + {@code constraint_referenced_ids},
 *       résolues en deux passes)</li>
 *   <li>{@code match_constraint} (0dp) : dimension pilotée par les
 *       contraintes (span, ratio ou poids de chaîne)</li>
 * </ul>
 *
 * <h2>Sécurité de rendu</h2>
 * <p>Si le solver échoue (contraintes circulaires, valeurs non finies), la
 * méthode {@link #solve(int, int)} retombe sur l'algorithme impératif
 * historique ({@link #solveLegacy(int, int)}) : le rendu n'est jamais pire
 * qu'avant le câblage de Cassowary.</p>
 *
 * @author jo@Dev
 * @since 1.0 (Cassowary depuis 3.0)
 */
public class ConstraintSolver {

    /** Tag de journalisation. */
    private static final String TAG = "ConstraintSolver";

    private static final String NS_APP = "http://schemas.android.com/apk/res-auto";
    private static final String PARENT = "parent";

    /** Tolérance de validation des valeurs résolues. */
    private static final double VALID_TOLERANCE = 0.5;

    /**
     * Structure legacy des contraintes d'une vue (v1, conservée pour
     * compatibilité de l'API publique — convertie en {@link ConstraintSpec}).
     *
     * @deprecated utiliser {@link ConstraintSpec#fromAttributes(Map)}
     */
    @Deprecated
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

    /** Enregistrement interne d'une vue contrainte. */
    private static final class Entry {
        final View view;
        final String id;
        final ConstraintSpec spec;
        final int measuredWidth;
        final int measuredHeight;

        Entry(View view, String id, ConstraintSpec spec,
              int measuredWidth, int measuredHeight) {
            this.view = view;
            this.id = id;
            this.spec = spec;
            this.measuredWidth = measuredWidth;
            this.measuredHeight = measuredHeight;
        }
    }

    /**
     * Ancre résolue : soit une constante (parent, guideline, barrière),
     * soit une expression variable (bord d'une autre vue).
     */
    private static final class Anchor {
        final Double constant;
        final Variable targetPos;
        final Variable targetSize;
        final double sizeCoeff; // 0 = bord début, 1 = bord fin

        static Anchor ofConstant(double value) {
            return new Anchor(value, null, null, 0);
        }

        static Anchor ofVariable(Variable pos, Variable size, double coeff) {
            return new Anchor(null, pos, size, coeff);
        }

        private Anchor(Double constant, Variable targetPos,
                       Variable targetSize, double sizeCoeff) {
            this.constant = constant;
            this.targetPos = targetPos;
            this.targetSize = targetSize;
            this.sizeCoeff = sizeCoeff;
        }

        boolean isConstant() {
            return constant != null;
        }

        /** Ajoute les termes de cette ancre à une expression (coeff appliqué). */
        void addTo(Expression e, double coeff) {
            if (isConstant()) {
                e.addConstant(constant * coeff);
            } else {
                e.addTerm(new Term(targetPos, coeff));
                if (targetSize != null && sizeCoeff != 0) {
                    e.addTerm(new Term(targetSize, coeff * sizeCoeff));
                }
            }
        }
    }

    private final Map<View, Entry> entries = new LinkedHashMap<>();
    private final Map<String, View> viewsById = new HashMap<>();
    private final Map<String, ConstraintSpec> specsById = new HashMap<>();
    private int parentWidth = 0;
    private int parentHeight = 0;

    /** Positions résolues (left, top, right, bottom) par vue. */
    private final Map<View, int[]> solvedBounds = new HashMap<>();

    /** {@code true} si la dernière résolution a utilisé Cassowary. */
    private boolean lastSolveUsedCassowary = false;

    /** Dernière erreur de résolution Cassowary (diagnostic), ou {@code null}. */
    private String lastSolveError;

    // ------------------------------------------------------------------
    // Enregistrement
    // ------------------------------------------------------------------

    /**
     * Enregistre une vue avec son id et ses contraintes.
     *
     * @param view   la vue
     * @param idName le nom de l'id (ex. "button_save")
     * @param parser le parser sur START_TAG de la vue (peut être {@code null})
     */
    public void registerView(View view, String idName, XmlPullParser parser) {
        registerView(view, idName,
                parser != null ? parseAttributes(parser) : null);
    }

    /**
     * Enregistre une vue depuis une map d'attributs de contrainte
     * (clés = noms d'attributs nus, ex. {@code layout_constraintTop_toTopOf},
     * {@code layout_width}, {@code barrierDirection}…).
     *
     * @param view       la vue
     * @param idName     le nom de l'id, ou {@code null}
     * @param attributes la map d'attributs (tolère {@code null})
     */
    public void registerView(View view, String idName, Map<String, String> attributes) {
        registerView(view, idName, ConstraintSpec.fromAttributes(attributes));
    }

    /**
     * Enregistre une vue depuis une map d'attributs avec tailles mesurées
     * explicites.
     *
     * @param view           la vue
     * @param idName         le nom de l'id, ou {@code null}
     * @param attributes     la map d'attributs (tolère {@code null})
     * @param measuredWidth  largeur mesurée
     * @param measuredHeight hauteur mesurée
     */
    public void registerView(View view, String idName, Map<String, String> attributes,
                             int measuredWidth, int measuredHeight) {
        registerView(view, idName, ConstraintSpec.fromAttributes(attributes),
                measuredWidth, measuredHeight);
    }

    /**
     * Enregistre une vue avec un {@link ConstraintSpec} déjà construit.
     *
     * @param view   la vue
     * @param idName le nom de l'id, ou {@code null}
     * @param spec   le spec de contraintes
     */
    public void registerView(View view, String idName, ConstraintSpec spec) {
        registerView(view, idName, spec,
                view.getMeasuredWidth(), view.getMeasuredHeight());
    }

    /**
     * Enregistre une vue avec des tailles mesurées explicites.
     *
     * <p>Utile quand la mesure n'a pas encore été propagée (tests JVM,
     * résolution avant {@code View.measure}).</p>
     *
     * @param view           la vue
     * @param idName         le nom de l'id, ou {@code null}
     * @param spec           le spec de contraintes
     * @param measuredWidth  largeur mesurée
     * @param measuredHeight hauteur mesurée
     */
    public void registerView(View view, String idName, ConstraintSpec spec,
                             int measuredWidth, int measuredHeight) {
        if (view == null) {
            return;
        }
        Entry entry = new Entry(view, idName,
                spec != null ? spec : ConstraintSpec.fromAttributes(null),
                Math.max(0, measuredWidth), Math.max(0, measuredHeight));
        entries.put(view, entry);
        if (idName != null) {
            viewsById.put(idName, view);
            specsById.put(idName, entry.spec);
        }
    }

    /**
     * Enregistre une vue avec un {@link ConstraintInfo} legacy.
     *
     * @param view   la vue
     * @param idName le nom de l'id
     * @param info   les contraintes legacy (converties en spec interne)
     */
    public void registerView(View view, String idName, ConstraintInfo info) {
        if (view == null) {
            return;
        }
        registerView(view, idName, specFromLegacy(info != null ? info : new ConstraintInfo()),
                view.getMeasuredWidth(), view.getMeasuredHeight());
    }

    /** Convertit un ConstraintInfo legacy en spec. */
    private static ConstraintSpec specFromLegacy(ConstraintInfo info) {
        Map<String, String> attrs = new HashMap<>();
        putAnchor(attrs, "layout_constraintTop_toTopOf", info.topToTop);
        putAnchor(attrs, "layout_constraintTop_toBottomOf", info.topToBottomOf);
        putAnchor(attrs, "layout_constraintBottom_toTopOf", info.bottomToTopOf);
        putAnchor(attrs, "layout_constraintBottom_toBottomOf", info.bottomToBottomOf);
        putAnchor(attrs, "layout_constraintStart_toStartOf", info.startToStartOf);
        putAnchor(attrs, "layout_constraintStart_toEndOf", info.startToEndOf);
        putAnchor(attrs, "layout_constraintEnd_toStartOf", info.endToStartOf);
        putAnchor(attrs, "layout_constraintEnd_toEndOf", info.endToEndOf);
        putAnchor(attrs, "layout_constraintLeft_toLeftOf", info.leftToLeftOf);
        putAnchor(attrs, "layout_constraintLeft_toRightOf", info.leftToRightOf);
        putAnchor(attrs, "layout_constraintRight_toLeftOf", info.rightToLeftOf);
        putAnchor(attrs, "layout_constraintRight_toRightOf", info.rightToRightOf);
        if (info.horizontalBias != 0.5f) {
            attrs.put("layout_constraintHorizontal_bias", String.valueOf(info.horizontalBias));
        }
        if (info.verticalBias != 0.5f) {
            attrs.put("layout_constraintVertical_bias", String.valueOf(info.verticalBias));
        }
        if (info.widthPercent > 0) {
            attrs.put("layout_constraintWidth_percent", String.valueOf(info.widthPercent));
        }
        if (info.heightPercent > 0) {
            attrs.put("layout_constraintHeight_percent", String.valueOf(info.heightPercent));
        }
        attrs.put("layout_marginTop", String.valueOf(info.marginTop));
        attrs.put("layout_marginBottom", String.valueOf(info.marginBottom));
        attrs.put("layout_marginStart", String.valueOf(info.marginStart + info.marginLeft));
        attrs.put("layout_marginEnd", String.valueOf(info.marginEnd + info.marginRight));
        return ConstraintSpec.fromAttributes(attrs);
    }

    private static void putAnchor(Map<String, String> attrs, String key, String value) {
        if (value != null) {
            attrs.put(key, value);
        }
    }

    /**
     * Parse les attributs de contrainte depuis le parser (START_TAG).
     */
    private static Map<String, String> parseAttributes(XmlPullParser parser) {
        Map<String, String> attrs = new HashMap<>();
        int count = parser.getAttributeCount();
        for (int i = 0; i < count; i++) {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);
            if (value == null) {
                continue;
            }
            String ns = parser.getAttributeNamespace(i);
            boolean isApp = NS_APP.equals(ns);
            boolean isNoNs = ns == null || ns.isEmpty();
            if (isApp || isNoNs) {
                if (name.startsWith("layout_constraint")
                        || "barrierDirection".equals(name)
                        || "constraint_referenced_ids".equals(name)
                        || "orientation".equals(name)
                        || "layout_width".equals(name)
                        || "layout_height".equals(name)) {
                    attrs.put(name, value);
                }
            }
        }
        return attrs;
    }

    // ------------------------------------------------------------------
    // Résolution
    // ------------------------------------------------------------------

    /**
     * Résout toutes les contraintes et positionne les vues.
     *
     * <p>Tente d'abord la résolution Cassowary (chaînes, guidelines,
     * barrières, ratios) ; en cas d'échec, retombe sur l'algorithme
     * impératif historique. Dans les deux cas, chaque vue finit
     * positionnée par {@link View#layout(int, int, int, int)}.</p>
     *
     * @param parentWidth  largeur du parent en pixels
     * @param parentHeight hauteur du parent en pixels
     * @return {@code true} si la résolution Cassowary a abouti
     */
    public boolean solve(int parentWidth, int parentHeight) {
        this.parentWidth = parentWidth;
        this.parentHeight = parentHeight;
        solvedBounds.clear();
        lastSolveUsedCassowary = false;

        if (entries.isEmpty()) {
            return false;
        }

        boolean solved = false;
        lastSolveError = null;
        try {
            solved = solveWithCassowary();
        } catch (RuntimeException e) {
            lastSolveError = "exception : " + e;
            Log.w(TAG, "Résolution Cassowary échouée, repli legacy : " + e);
            solved = false;
        }

        if (!solved && lastSolveError == null) {
            lastSolveError = "contraintes REQUIRED non satisfaites ou résultats invalides";
        }
        if (!solved) {
            solveLegacy(parentWidth, parentHeight);
        }
        return lastSolveUsedCassowary;
    }

    /**
     * Résolution Cassowary : construit le système, le résout (avec une
     * seconde passe si des barrières sont présentes), valide et applique.
     */
    private boolean solveWithCassowary() {
        Map<String, double[]> guidelineBounds = computeGuidelineBounds();

        // Les barrières n'ont pas encore de position : passe 1 neutre,
        // calcul des vraies positions, puis passe 2
        Map<String, Double> barrierPositions = new HashMap<>();
        for (Map.Entry<String, ConstraintSpec> e : specsById.entrySet()) {
            if (e.getValue().isBarrier()) {
                barrierPositions.put(e.getKey(), 0.0);
            }
        }

        Map<View, double[]> results = buildAndSolve(guidelineBounds, barrierPositions);

        if (!barrierPositions.isEmpty() && results != null) {
            boolean changed = computeBarrierPositions(results, barrierPositions);
            if (changed) {
                Map<View, double[]> second = buildAndSolve(guidelineBounds, barrierPositions);
                if (second != null) {
                    results = second;
                }
            }
        }

        if (results == null || !validateResults(results)) {
            return false;
        }

        for (Map.Entry<View, double[]> e : results.entrySet()) {
            double[] b = e.getValue();
            int l = (int) Math.round(b[0]);
            int t = (int) Math.round(b[1]);
            int r = (int) Math.round(b[2]);
            int bo = (int) Math.round(b[3]);
            View view = e.getKey();
            try {
                view.layout(l, t, r, bo);
            } catch (RuntimeException layoutError) {
                // En JVM sans toolkit complet, View.layout peut échouer :
                // les bornes résolues restent exposées via getSolvedBounds()
                Log.w(TAG, "layout() refusé pour " + view + " : " + layoutError);
            }
            solvedBounds.put(view, new int[]{l, t, r, bo});
        }
        lastSolveUsedCassowary = true;
        return true;
    }

    /**
     * Construit et résout le système complet pour la passe courante.
     *
     * @param guidelineBounds  bornes des guidelines par id
     * @param barrierPositions position (constante) de chaque barrière par id
     * @return les bornes résolues par vue, ou {@code null} si échec
     */
    private Map<View, double[]> buildAndSolve(Map<String, double[]> guidelineBounds,
                                              Map<String, Double> barrierPositions) {
        Solver solver = new Solver();

        Map<View, Variable> leftVars = new LinkedHashMap<>();
        Map<View, Variable> topVars = new LinkedHashMap<>();
        Map<View, Variable> widthVars = new LinkedHashMap<>();
        Map<View, Variable> heightVars = new LinkedHashMap<>();
        Map<View, Entry> regular = new LinkedHashMap<>();

        int anonymous = 0;
        for (Entry entry : entries.values()) {
            if (entry.spec.isGuideline() || entry.spec.isBarrier()) {
                continue; // constantes, pas des variables
            }
            regular.put(entry.view, entry);
            String suffix = entry.id != null ? entry.id : "v" + (++anonymous);
            Variable left = new Variable(suffix + ".left");
            Variable top = new Variable(suffix + ".top");
            Variable width = new Variable(suffix + ".width");
            Variable height = new Variable(suffix + ".height");
            leftVars.put(entry.view, left);
            topVars.put(entry.view, top);
            widthVars.put(entry.view, width);
            heightVars.put(entry.view, height);
            solver.addVariable(left);
            solver.addVariable(top);
            solver.addVariable(width);
            solver.addVariable(height);
            // Warm start : tailles mesurées, origine
            width.setValue(entry.measuredWidth);
            height.setValue(entry.measuredHeight);
        }

        addDimensionConstraints(solver, regular, widthVars, heightVars);

        // Chaînes : positions déterministes épinglées REQUIRED. Les membres
        // épinglés sur un axe n'ont pas leurs ancres sur cet axe (sinon les
        // deux formulations se disputent les mêmes variables).
        Set<View> pinnedH = pinChains(solver, regular, leftVars, widthVars,
                true, guidelineBounds, barrierPositions);
        Set<View> pinnedV = pinChains(solver, regular, topVars, heightVars,
                false, guidelineBounds, barrierPositions);

        // Ancres et biais pour les vues non épinglées sur l'axe considéré
        for (Entry entry : regular.values()) {
            if (!pinnedH.contains(entry.view)) {
                addAxisConstraints(solver, entry, true,
                        leftVars.get(entry.view), widthVars.get(entry.view),
                        leftVars, widthVars, guidelineBounds, barrierPositions);
            }
            if (!pinnedV.contains(entry.view)) {
                addAxisConstraints(solver, entry, false,
                        topVars.get(entry.view), heightVars.get(entry.view),
                        topVars, heightVars, guidelineBounds, barrierPositions);
            }
        }

        if (!solver.solve()) {
            return null;
        }

        Map<View, double[]> results = new LinkedHashMap<>();
        for (Entry entry : regular.values()) {
            double l = leftVars.get(entry.view).getValue();
            double t = topVars.get(entry.view).getValue();
            double w = widthVars.get(entry.view).getValue();
            double h = heightVars.get(entry.view).getValue();
            results.put(entry.view, new double[]{l, t, l + w, t + h});
        }
        return results;
    }

    /**
     * Contraintes de dimension : mesurée, pourcentage, ratio (cross-axe),
     * match_constraint sans ancre double.
     */
    private void addDimensionConstraints(Solver solver, Map<View, Entry> regular,
                                         Map<View, Variable> widthVars,
                                         Map<View, Variable> heightVars) {
        for (Entry entry : regular.values()) {
            ConstraintSpec spec = entry.spec;
            Variable w = widthVars.get(entry.view);
            Variable h = heightVars.get(entry.view);

            boolean widthDriven = spec.getWidthPercent() > 0f && spec.getWidthPercent() <= 1f
                    || spec.hasRatio() && spec.ratioDrivesWidth()
                    || spec.isWidthMatchConstraint();

            if (widthDriven) {
                if (spec.getWidthPercent() > 0f && spec.getWidthPercent() <= 1f) {
                    solver.addConstraint(pin(w, parentWidth * spec.getWidthPercent()));
                } else if (spec.hasRatio() && spec.ratioDrivesWidth()) {
                    double ratio = spec.getRatioNumerator() / spec.getRatioDenominator();
                    Expression ratioExpr = expr()
                            .addTerm(term(w, 1.0))
                            .addTerm(term(h, -ratio));
                    solver.addConstraint(new Constraint(ratioExpr,
                            Constraint.Operator.EQ, 0, Strength.REQUIRED));
                }
                // match_constraint pur : rempli par le span (addAxisConstraints)
                // ou par le poids de chaîne (pinChains) — pas de contrainte ici
                if (spec.isWidthMatchConstraint() && !spec.hasHorizontalAnchor()
                        && spec.getWidthPercent() <= 0f
                        && !spec.hasRatio()) {
                    solver.addConstraint(pin(w, entry.measuredWidth));
                }
            } else {
                solver.addConstraint(pin(w, entry.measuredWidth));
            }

            boolean heightDriven = spec.getHeightPercent() > 0f && spec.getHeightPercent() <= 1f
                    || spec.hasRatio() && !spec.ratioDrivesWidth()
                    || spec.isHeightMatchConstraint();

            if (heightDriven) {
                if (spec.getHeightPercent() > 0f && spec.getHeightPercent() <= 1f) {
                    solver.addConstraint(pin(h, parentHeight * spec.getHeightPercent()));
                } else if (spec.hasRatio() && !spec.ratioDrivesWidth()) {
                    double ratio = spec.getRatioNumerator() / spec.getRatioDenominator();
                    Expression ratioExpr = expr()
                            .addTerm(term(h, 1.0))
                            .addTerm(term(w, -ratio));
                    solver.addConstraint(new Constraint(ratioExpr,
                            Constraint.Operator.EQ, 0, Strength.REQUIRED));
                }
                if (spec.isHeightMatchConstraint() && !spec.hasVerticalAnchor()
                        && spec.getHeightPercent() <= 0f
                        && !spec.hasRatio()) {
                    solver.addConstraint(pin(h, entry.measuredHeight));
                }
            } else {
                solver.addConstraint(pin(h, entry.measuredHeight));
            }
        }
    }

    /**
     * Contraintes d'axe (ancres + biais) d'une vue régulière.
     *
     * @param horizontal {@code true} pour l'axe horizontal
     */
    private void addAxisConstraints(Solver solver, Entry entry, boolean horizontal,
                                    Variable pos, Variable size,
                                    Map<View, Variable> posVars,
                                    Map<View, Variable> sizeVars,
                                    Map<String, double[]> guidelineBounds,
                                    Map<String, Double> barrierPositions) {
        ConstraintSpec spec = entry.spec;

        // Ancre de début : start_toStartOf / start_toEndOf (ou legacy left)
        String startRef = null;
        boolean startToTargetEnd = false;
        if (horizontal) {
            startRef = spec.getStartToStartOf() != null ? spec.getStartToStartOf()
                    : spec.getLeftToLeftOf();
            if (startRef == null && spec.getStartToEndOf() != null) {
                startRef = spec.getStartToEndOf();
                startToTargetEnd = true;
            }
            if (startRef == null && spec.getLeftToRightOf() != null) {
                startRef = spec.getLeftToRightOf();
                startToTargetEnd = true;
            }
        } else {
            startRef = spec.getTopToTopOf();
            if (startRef == null && spec.getTopToBottomOf() != null) {
                startRef = spec.getTopToBottomOf();
                startToTargetEnd = true;
            }
        }

        // Ancre de fin : end_toEndOf / end_toStartOf (ou legacy right)
        String endRef = null;
        boolean endToTargetStart = false;
        if (horizontal) {
            endRef = spec.getEndToEndOf() != null ? spec.getEndToEndOf()
                    : spec.getRightToRightOf();
            if (endRef == null && spec.getEndToStartOf() != null) {
                endRef = spec.getEndToStartOf();
                endToTargetStart = true;
            }
            if (endRef == null && spec.getRightToLeftOf() != null) {
                endRef = spec.getRightToLeftOf();
                endToTargetStart = true;
            }
        } else {
            endRef = spec.getBottomToBottomOf();
            if (endRef == null && spec.getBottomToTopOf() != null) {
                endRef = spec.getBottomToTopOf();
                endToTargetStart = true;
            }
        }

        int marginStart = horizontal ? spec.getMarginStart() : spec.getMarginTop();
        int marginEnd = horizontal ? spec.getMarginEnd() : spec.getMarginBottom();

        Anchor startAnchor = resolveAnchor(startRef, startToTargetEnd, horizontal,
                posVars, sizeVars, guidelineBounds, barrierPositions);
        // end_toEndOf vise le bord FIN de la cible ; end_toStartOf son bord DÉBUT
        Anchor endAnchor = resolveAnchor(endRef, !endToTargetStart, horizontal,
                posVars, sizeVars, guidelineBounds, barrierPositions);

        boolean matchFill = horizontal
                ? spec.isWidthMatchConstraint() && spec.getWidthPercent() <= 0f
                && !(spec.hasRatio() && spec.ratioDrivesWidth())
                : spec.isHeightMatchConstraint() && spec.getHeightPercent() <= 0f
                && !(spec.hasRatio() && !spec.ratioDrivesWidth());

        if (startAnchor != null && endAnchor != null) {
            if (matchFill) {
                // 0dp double ancrage : remplit le span exactement
                Expression startEq = expr().addTerm(term(pos, 1.0));
                startAnchor.addTo(startEq, -1.0);
                solver.addConstraint(new Constraint(startEq, Constraint.Operator.EQ,
                        marginStart, Strength.REQUIRED));
                Expression endEq = expr().addTerm(term(pos, 1.0))
                        .addTerm(term(size, 1.0));
                endAnchor.addTo(endEq, -1.0);
                solver.addConstraint(new Constraint(endEq, Constraint.Operator.EQ,
                        -marginEnd, Strength.REQUIRED));
            } else {
                // Double ancrage : biais dans l'espace libre
                // pos = S' + bias * (E' - S' - size)
                //   ⟺ pos + bias*size + (1-bias)*S' - bias*E' = 0
                double bias = horizontal ? spec.getHorizontalBias() : spec.getVerticalBias();
                Expression e = expr().addTerm(term(pos, 1.0))
                        .addTerm(term(size, bias));
                startAnchor.addTo(e, 1.0 - bias);
                endAnchor.addTo(e, -bias);
                solver.addConstraint(new Constraint(e, Constraint.Operator.EQ, 0,
                        Strength.STRONG));
                // Bornes douces : la vue reste dans le span
                Expression lower = expr().addTerm(term(pos, 1.0));
                startAnchor.addTo(lower, -1.0);
                solver.addConstraint(new Constraint(lower, Constraint.Operator.GEQ,
                        marginStart, Strength.WEAK));
                Expression upper = expr().addTerm(term(pos, 1.0))
                        .addTerm(term(size, 1.0));
                endAnchor.addTo(upper, -1.0);
                solver.addConstraint(new Constraint(upper, Constraint.Operator.LEQ,
                        -marginEnd, Strength.WEAK));
            }
        } else if (startAnchor != null) {
            // pos = startAnchor + marginStart
            Expression e = expr().addTerm(term(pos, 1.0));
            startAnchor.addTo(e, -1.0);
            solver.addConstraint(new Constraint(e, Constraint.Operator.EQ,
                    marginStart, Strength.REQUIRED));
        } else if (endAnchor != null) {
            // pos + size = endAnchor - marginEnd
            Expression e = expr().addTerm(term(pos, 1.0)).addTerm(term(size, 1.0));
            endAnchor.addTo(e, -1.0);
            solver.addConstraint(new Constraint(e, Constraint.Operator.EQ,
                    -marginEnd, Strength.REQUIRED));
        } else if (horizontal ? false : spec.getBaselineToBaselineOf() != null) {
            // Baseline ≈ centre vertical (approximation preview)
            Anchor baseline = resolveAnchor(spec.getBaselineToBaselineOf(), false,
                    false, posVars, sizeVars, guidelineBounds, barrierPositions);
            if (baseline != null) {
                Expression e = expr().addTerm(term(pos, 1.0))
                        .addTerm(term(size, 0.5));
                baseline.addTo(e, -0.5);
                solver.addConstraint(new Constraint(e, Constraint.Operator.EQ, 0,
                        Strength.STRONG));
            }
        } else {
            solver.addConstraint(new Constraint(
                    expr().addTerm(term(pos, 1.0)), Constraint.Operator.EQ,
                    marginStart, Strength.WEAK));
        }
    }

    /**
     * Résout une référence d'ancre en {@link Anchor} : constante (parent,
     * guideline, barrière) ou expression variable (bord d'une vue).
     *
     * @param ref            la référence brute ({@code parent}, {@code @id/foo})
     * @param targetEndEdge  {@code true} si l'ancre vise le bord FIN de la cible
     * @param horizontal     {@code true} pour l'axe horizontal
     */
    private Anchor resolveAnchor(String ref, boolean targetEndEdge, boolean horizontal,
                                 Map<View, Variable> posVars,
                                 Map<View, Variable> sizeVars,
                                 Map<String, double[]> guidelineBounds,
                                 Map<String, Double> barrierPositions) {
        if (ref == null) {
            return null;
        }
        double span = horizontal ? parentWidth : parentHeight;
        if (PARENT.equals(ref)) {
            return Anchor.ofConstant(targetEndEdge ? span : 0.0);
        }
        String id = stripRef(ref);
        double[] guideline = guidelineBounds.get(id);
        if (guideline != null) {
            double edge = horizontal
                    ? (targetEndEdge ? guideline[2] : guideline[0])
                    : (targetEndEdge ? guideline[3] : guideline[1]);
            return Anchor.ofConstant(edge);
        }
        Double barrier = barrierPositions.get(id);
        if (barrier != null) {
            return Anchor.ofConstant(barrier);
        }
        View target = viewsById.get(id);
        if (target != null && posVars.containsKey(target)) {
            return Anchor.ofVariable(posVars.get(target), sizeVars.get(target),
                    targetEndEdge ? 1.0 : 0.0);
        }
        return null; // référence inconnue — ignorée
    }

    // ------------------------------------------------------------------
    // Guidelines et barrières
    // ------------------------------------------------------------------

    /**
     * Calcule les bornes des guidelines (left/top/right/bottom).
     */
    private Map<String, double[]> computeGuidelineBounds() {
        Map<String, double[]> bounds = new HashMap<>();
        for (Map.Entry<String, ConstraintSpec> e : specsById.entrySet()) {
            ConstraintSpec spec = e.getValue();
            if (!spec.isGuideline()) {
                continue;
            }
            double pos;
            if (spec.getGuideBegin() != null) {
                pos = spec.getGuideBegin();
            } else if (spec.getGuideEnd() != null) {
                pos = (spec.isGuidelineVertical() ? parentWidth : parentHeight)
                        - spec.getGuideEnd();
            } else if (spec.getGuidePercent() != null) {
                pos = (spec.isGuidelineVertical() ? parentWidth : parentHeight)
                        * spec.getGuidePercent();
            } else {
                pos = 0;
            }
            if (spec.isGuidelineVertical()) {
                bounds.put(e.getKey(), new double[]{pos, 0, pos, parentHeight});
            } else {
                bounds.put(e.getKey(), new double[]{0, pos, parentWidth, pos});
            }
        }
        return bounds;
    }

    /**
     * Recalcule les positions des barrières depuis les bornes résolues.
     *
     * @return {@code true} si une position a changé
     */
    private boolean computeBarrierPositions(Map<View, double[]> results,
                                             Map<String, Double> barrierPositions) {
        boolean changed = false;
        for (Map.Entry<String, ConstraintSpec> e : specsById.entrySet()) {
            ConstraintSpec spec = e.getValue();
            if (!spec.isBarrier() || spec.getReferencedIds() == null) {
                continue;
            }
            double previous = barrierPositions.getOrDefault(e.getKey(), 0.0);
            ConstraintSpec.BarrierDirection dir = spec.getBarrierDirection();
            boolean horizontal = dir == ConstraintSpec.BarrierDirection.LEFT
                    || dir == ConstraintSpec.BarrierDirection.RIGHT
                    || dir == ConstraintSpec.BarrierDirection.START
                    || dir == ConstraintSpec.BarrierDirection.END;
            boolean maxEdge = dir == ConstraintSpec.BarrierDirection.RIGHT
                    || dir == ConstraintSpec.BarrierDirection.END
                    || dir == ConstraintSpec.BarrierDirection.BOTTOM;

            double extreme = maxEdge ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
            boolean any = false;
            for (String refId : spec.getReferencedIds().split(",")) {
                View view = viewsById.get(refId.trim());
                if (view == null) {
                    continue;
                }
                double[] b = results.get(view);
                if (b == null) {
                    continue;
                }
                double edge = horizontal ? (maxEdge ? b[2] : b[0])
                        : (maxEdge ? b[3] : b[1]);
                extreme = maxEdge ? Math.max(extreme, edge) : Math.min(extreme, edge);
                any = true;
            }
            if (any) {
                if (Math.abs(extreme - previous) > 0.5) {
                    changed = true;
                }
                barrierPositions.put(e.getKey(), extreme);
            }
        }
        return changed;
    }

    // ------------------------------------------------------------------
    // Chaînes
    // ------------------------------------------------------------------

    /**
     * Détecte et épingle les chaînes sur un axe.
     *
     * <p>Une chaîne est une séquence de vues reliées par
     * {@code vi → v(i+1)} (fin de vi ancrée au début de v(i+1)) dont la
     * tête et la queue sont ancrées au parent/guideline. Le style est lu
     * sur la tête ({@code spread} par défaut, {@code packed},
     * {@code spread_inside}) ; les membres en {@code 0dp} avec poids se
     * partagent l'espace (chaîne pondérée).</p>
     *
     * @param horizontal {@code true} pour l'axe horizontal
     * @return les vues épinglées sur cet axe
     */
    private Set<View> pinChains(Solver solver, Map<View, Entry> regular,
                                Map<View, Variable> posVars,
                                Map<View, Variable> sizeVars, boolean horizontal,
                                Map<String, double[]> guidelineBounds,
                                Map<String, Double> barrierPositions) {
        Set<View> pinned = new HashSet<>();

        // Liens de chaîne : B.start_toEndOf = A signifie A → B (A avant B).
        // On enregistre prev[B] = A et next[A] = B.
        Map<View, View> nextByView = new HashMap<>();
        Map<View, View> prevByView = new HashMap<>();
        for (Entry entry : regular.values()) {
            String startTo = horizontal
                    ? firstNonNull(entry.spec.getStartToEndOf(), entry.spec.getLeftToRightOf())
                    : entry.spec.getTopToBottomOf();
            if (startTo != null) {
                View before = viewsById.get(stripRef(startTo));
                if (before != null && regular.containsKey(before) && before != entry.view) {
                    prevByView.put(entry.view, before);
                    nextByView.put(before, entry.view);
                }
            }
        }

        // Têtes : vues avec successeur mais sans prédécesseur
        List<List<View>> chains = new ArrayList<>();
        for (View view : nextByView.keySet()) {
            if (prevByView.containsKey(view)) {
                continue;
            }
            List<View> chain = new ArrayList<>();
            View current = view;
            while (current != null && !chain.contains(current)) {
                chain.add(current);
                current = nextByView.get(current);
            }
            if (chain.size() >= 2) {
                chains.add(chain);
            }
        }

        for (List<View> chain : chains) {
            if (pinSingleChain(solver, chain, regular, posVars, sizeVars,
                    horizontal, guidelineBounds, barrierPositions)) {
                pinned.addAll(chain);
            }
        }
        return pinned;
    }

    /**
     * Épingle une chaîne détectée. Retourne {@code true} si épinglée.
     */
    private boolean pinSingleChain(Solver solver, List<View> chain,
                                   Map<View, Entry> regular,
                                   Map<View, Variable> posVars,
                                   Map<View, Variable> sizeVars, boolean horizontal,
                                   Map<String, double[]> guidelineBounds,
                                   Map<String, Double> barrierPositions) {
        Entry head = regular.get(chain.get(0));
        Entry tail = regular.get(chain.get(chain.size() - 1));

        Double spanStart = resolveChainEdge(head, true, horizontal,
                guidelineBounds, barrierPositions);
        Double spanEnd = resolveChainEdge(tail, false, horizontal,
                guidelineBounds, barrierPositions);
        if (spanStart == null || spanEnd == null) {
            return false;
        }

        int headMargin = horizontal ? head.spec.getMarginStart() : head.spec.getMarginTop();
        int tailMargin = horizontal ? tail.spec.getMarginEnd() : tail.spec.getMarginBottom();

        double a = spanStart + headMargin;
        double b = spanEnd - tailMargin;
        if (b < a) {
            return false;
        }

        ConstraintSpec.ChainStyle style = horizontal
                ? head.spec.getHorizontalChainStyle()
                : head.spec.getVerticalChainStyle();

        // Marges inter-éléments : marge fin de vi + marge début de vj
        List<Double> gaps = new ArrayList<>();
        for (int i = 0; i < chain.size() - 1; i++) {
            ConstraintSpec current = regular.get(chain.get(i)).spec;
            ConstraintSpec next = regular.get(chain.get(i + 1)).spec;
            gaps.add(0.0 + (horizontal ? current.getMarginEnd() : current.getMarginBottom())
                    + (horizontal ? next.getMarginStart() : next.getMarginTop()));
        }

        // Membres 0dp (match_constraint) → chaîne pondérée
        List<Boolean> match = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        double totalWeights = 0;
        int matchCount = 0;
        double fixedTotal = 0;
        for (View view : chain) {
            Entry entry = regular.get(view);
            boolean isMatch = horizontal
                    ? entry.spec.isWidthMatchConstraint()
                    : entry.spec.isHeightMatchConstraint();
            match.add(isMatch);
            double weight = 0;
            if (isMatch) {
                weight = horizontal ? entry.spec.getHorizontalWeight()
                        : entry.spec.getVerticalWeight();
                if (weight <= 0) {
                    weight = 1; // poids implicite 1
                }
                matchCount++;
                totalWeights += weight;
            } else {
                fixedTotal += horizontal ? entry.measuredWidth : entry.measuredHeight;
            }
            weights.add(weight);
        }

        double available = b - a - fixedTotal;
        for (double g : gaps) {
            available -= g;
        }

        // Tailles : pondérées pour les 0dp, mesurées sinon
        Map<View, Double> sizes = new LinkedHashMap<>();
        for (int i = 0; i < chain.size(); i++) {
            Entry entry = regular.get(chain.get(i));
            double size;
            if (match.get(i) && matchCount > 0 && totalWeights > 0) {
                size = Math.max(0, available * weights.get(i) / totalWeights);
            } else {
                size = horizontal ? entry.measuredWidth : entry.measuredHeight;
            }
            sizes.put(chain.get(i), size);
        }

        // Positions selon le style
        Map<View, Double> positions = new LinkedHashMap<>();
        if (style == ConstraintSpec.ChainStyle.CHAIN_PACKED) {
            double bias = horizontal ? head.spec.getHorizontalBias()
                    : head.spec.getVerticalBias();
            double total = 0;
            for (int i = 0; i < chain.size(); i++) {
                total += sizes.get(chain.get(i));
                if (i < gaps.size()) {
                    total += gaps.get(i);
                }
            }
            double free = Math.max(0, b - a - total);
            double pos = a + bias * free;
            for (int i = 0; i < chain.size(); i++) {
                positions.put(chain.get(i), pos);
                pos += sizes.get(chain.get(i));
                if (i < gaps.size()) {
                    pos += gaps.get(i);
                }
            }
        } else {
            // SPREAD : espaces égaux autour (n+1 espaces pour n membres) ;
            // SPREAD_INSIDE : collé aux bornes, espaces seulement entre
            double used = 0;
            for (int i = 0; i < chain.size(); i++) {
                used += sizes.get(chain.get(i));
                if (i < gaps.size()) {
                    used += gaps.get(i);
                }
            }
            double slack = Math.max(0, b - a - used);
            // SPREAD : espaces avant, entre (n-1), après → n+1 au total
            int dividers = style == ConstraintSpec.ChainStyle.CHAIN_SPREAD_INSIDE
                    ? Math.max(1, gaps.size())
                    : gaps.size() + 2;
            double evenGap = slack / dividers;
            double pos = style == ConstraintSpec.ChainStyle.CHAIN_SPREAD_INSIDE
                    ? a : a + evenGap;
            for (int i = 0; i < chain.size(); i++) {
                positions.put(chain.get(i), pos);
                pos += sizes.get(chain.get(i));
                if (i < gaps.size()) {
                    pos += gaps.get(i) + evenGap;
                }
            }
        }

        // Épinglage REQUIRED des positions et des tailles des membres 0dp
        for (Map.Entry<View, Double> e : positions.entrySet()) {
            Variable pos = posVars.get(e.getKey());
            if (pos != null) {
                solver.addConstraint(pin(pos, e.getValue()));
            }
        }
        for (int i = 0; i < chain.size(); i++) {
            if (match.get(i)) {
                Variable size = sizeVars.get(chain.get(i));
                if (size != null) {
                    solver.addConstraint(pin(size, sizes.get(chain.get(i))));
                }
            }
        }
        return true;
    }

    /**
     * Résout le bord de chaîne (début de tête / fin de queue) vers une
     * constante (parent, guideline, barrière).
     */
    private Double resolveChainEdge(Entry entry, boolean start, boolean horizontal,
                                    Map<String, double[]> guidelineBounds,
                                    Map<String, Double> barrierPositions) {
        ConstraintSpec spec = entry.spec;
        String anchor;
        boolean toTargetEnd;
        if (horizontal) {
            if (start) {
                anchor = firstNonNull(spec.getStartToStartOf(), spec.getLeftToLeftOf());
                toTargetEnd = false;
                if (anchor == null) {
                    anchor = firstNonNull(spec.getStartToEndOf(), spec.getLeftToRightOf());
                    toTargetEnd = true;
                }
            } else {
                anchor = firstNonNull(spec.getEndToEndOf(), spec.getRightToRightOf());
                toTargetEnd = true;
                if (anchor == null) {
                    anchor = firstNonNull(spec.getEndToStartOf(), spec.getRightToLeftOf());
                    toTargetEnd = false;
                }
            }
        } else {
            if (start) {
                anchor = spec.getTopToTopOf();
                toTargetEnd = false;
                if (anchor == null) {
                    anchor = spec.getTopToBottomOf();
                    toTargetEnd = true;
                }
            } else {
                anchor = spec.getBottomToBottomOf();
                toTargetEnd = true;
                if (anchor == null) {
                    anchor = spec.getBottomToTopOf();
                    toTargetEnd = false;
                }
            }
        }
        if (anchor == null) {
            return null;
        }
        double span = horizontal ? parentWidth : parentHeight;
        if (PARENT.equals(anchor)) {
            return toTargetEnd ? span : 0.0;
        }
        String id = stripRef(anchor);
        double[] guideline = guidelineBounds.get(id);
        if (guideline != null) {
            return horizontal
                    ? (toTargetEnd ? guideline[2] : guideline[0])
                    : (toTargetEnd ? guideline[3] : guideline[1]);
        }
        Double barrier = barrierPositions.get(id);
        if (barrier != null) {
            return barrier;
        }
        return null; // ancre vers une vue : pas une constante
    }

    // ------------------------------------------------------------------
    // Validation et helpers
    // ------------------------------------------------------------------

    /**
     * Valide les résultats : valeurs finies, dimensions non négatives,
     * positions raisonnables.
     */
    private boolean validateResults(Map<View, double[]> results) {
        int maxW = Math.max(1, parentWidth);
        int maxH = Math.max(1, parentHeight);
        for (double[] b : results.values()) {
            if (!Double.isFinite(b[0]) || !Double.isFinite(b[1])
                    || !Double.isFinite(b[2]) || !Double.isFinite(b[3])) {
                return false;
            }
            if (b[2] < b[0] - VALID_TOLERANCE || b[3] < b[1] - VALID_TOLERANCE) {
                return false;
            }
            if (Math.abs(b[0]) > 2 * maxW || Math.abs(b[1]) > 2 * maxH
                    || Math.abs(b[2]) > 3 * maxW || Math.abs(b[3]) > 3 * maxH) {
                return false;
            }
        }
        return true;
    }

    private static String stripRef(String ref) {
        if (ref == null) {
            return null;
        }
        if (ref.startsWith("@+id/") || ref.startsWith("@id/")) {
            return ref.substring(ref.indexOf('/') + 1);
        }
        return ref;
    }

    private static String firstNonNull(String first, String second) {
        return first != null ? first : second;
    }

    private static Term term(Variable v, double coefficient) {
        return new Term(v, coefficient);
    }

    private static Expression expr() {
        return new Expression();
    }

    /** Contrainte d'égalité REQUIRED : variable = constante. */
    private static Constraint pin(Variable v, double constant) {
        return new Constraint(new Expression(term(v, 1.0)),
                Constraint.Operator.EQ, constant, Strength.REQUIRED);
    }

    // ------------------------------------------------------------------
    // Accesseurs
    // ------------------------------------------------------------------

    /**
     * Retourne les bornes résolues d'une vue.
     *
     * @param view la vue enregistrée
     * @return {@code {left, top, right, bottom}}, ou {@code null}
     */
    public int[] getSolvedBounds(View view) {
        int[] bounds = solvedBounds.get(view);
        return bounds != null ? bounds.clone() : null;
    }

    /**
     * @return {@code true} si la dernière résolution a utilisé Cassowary
     */
    public boolean lastSolveUsedCassowary() {
        return lastSolveUsedCassowary;
    }

    /**
     * @return la dernière erreur de résolution Cassowary (diagnostic), ou
     *         {@code null} si succès / pas encore résolu
     */
    public String getLastSolveError() {
        return lastSolveError;
    }

    /**
     * @return le nombre de vues enregistrées
     */
    public int getViewCount() {
        return entries.size();
    }

    /**
     * Vide toutes les contraintes.
     */
    public void clear() {
        entries.clear();
        viewsById.clear();
        specsById.clear();
        solvedBounds.clear();
        lastSolveUsedCassowary = false;
    }

    // ------------------------------------------------------------------
    // Algorithme legacy (repli)
    // ------------------------------------------------------------------

    /**
     * Algorithme impératif historique (v1) — conservé comme repli lorsque
     * la résolution Cassowary échoue.
     */
    private void solveLegacy(int parentWidth, int parentHeight) {
        for (int pass = 0; pass < 3; pass++) {
            for (Entry entry : entries.values()) {
                legacyLayoutView(entry);
            }
        }
    }

    /**
     * Positionne une vue selon l'algorithme v1.
     */
    private void legacyLayoutView(Entry entry) {
        View view = entry.view;
        ConstraintSpec spec = entry.spec;
        int measuredWidth = entry.measuredWidth;
        int measuredHeight = entry.measuredHeight;

        if (spec.getWidthPercent() > 0) {
            measuredWidth = (int) (parentWidth * spec.getWidthPercent());
        }
        if (spec.getHeightPercent() > 0) {
            measuredHeight = (int) (parentHeight * spec.getHeightPercent());
        }
        if (spec.hasRatio()) {
            double ratio = spec.getRatioNumerator() / spec.getRatioDenominator();
            if (spec.ratioDrivesWidth()) {
                measuredWidth = (int) Math.round(measuredHeight * ratio);
            } else {
                measuredHeight = (int) Math.round(measuredWidth * ratio);
            }
        }

        int left = -1;
        int right = -1;

        String start = firstNonNull(spec.getStartToStartOf(), spec.getLeftToLeftOf());
        if (start != null && PARENT.equals(start)) {
            left = spec.getMarginStart();
            String end = firstNonNull(spec.getEndToEndOf(), spec.getRightToRightOf());
            if (end != null && PARENT.equals(end)) {
                int available = parentWidth - measuredWidth
                        - spec.getMarginStart() - spec.getMarginEnd();
                left = spec.getMarginStart()
                        + (int) (available * spec.getHorizontalBias());
            }
            right = left + measuredWidth;
        } else if (spec.getStartToEndOf() != null || spec.getLeftToRightOf() != null) {
            View other = resolveRef(firstNonNull(spec.getStartToEndOf(),
                    spec.getLeftToRightOf()));
            if (other != null) {
                int[] ob = targetBounds(other);
                left = ob[2] + spec.getMarginStart();
                right = left + measuredWidth;
            }
        } else if (spec.getEndToEndOf() != null && PARENT.equals(spec.getEndToEndOf())) {
            right = parentWidth - spec.getMarginEnd();
            left = right - measuredWidth;
        } else if (spec.getRightToRightOf() != null && PARENT.equals(spec.getRightToRightOf())) {
            right = parentWidth - spec.getMarginEnd();
            left = right - measuredWidth;
        } else if (spec.getEndToStartOf() != null || spec.getRightToLeftOf() != null) {
            View other = resolveRef(firstNonNull(spec.getEndToStartOf(),
                    spec.getRightToLeftOf()));
            if (other != null) {
                int[] ob = targetBounds(other);
                right = ob[0] - spec.getMarginEnd();
                left = right - measuredWidth;
            }
        }

        if (left < 0) left = spec.getMarginStart();
        if (right < 0) right = left + measuredWidth;

        int top = -1;
        int bottom = -1;

        if (spec.getTopToTopOf() != null && PARENT.equals(spec.getTopToTopOf())) {
            top = spec.getMarginTop();
            if (spec.getBottomToBottomOf() != null
                    && PARENT.equals(spec.getBottomToBottomOf())) {
                int available = parentHeight - measuredHeight
                        - spec.getMarginTop() - spec.getMarginBottom();
                top = spec.getMarginTop()
                        + (int) (available * spec.getVerticalBias());
            }
            bottom = top + measuredHeight;
        } else if (spec.getTopToBottomOf() != null) {
            View other = resolveRef(spec.getTopToBottomOf());
            if (other != null) {
                int[] ob = targetBounds(other);
                top = ob[3] + spec.getMarginTop();
                bottom = top + measuredHeight;
            }
        } else if (spec.getBottomToBottomOf() != null
                && PARENT.equals(spec.getBottomToBottomOf())) {
            bottom = parentHeight - spec.getMarginBottom();
            top = bottom - measuredHeight;
        } else if (spec.getBottomToTopOf() != null) {
            View other = resolveRef(spec.getBottomToTopOf());
            if (other != null) {
                int[] ob = targetBounds(other);
                bottom = ob[1] - spec.getMarginBottom();
                top = bottom - measuredHeight;
            }
        }

        if (top < 0) top = spec.getMarginTop();
        if (bottom < 0) bottom = top + measuredHeight;

        try {
            view.layout(left, top, right, bottom);
        } catch (RuntimeException layoutError) {
            Log.w(TAG, "layout() refusé pour " + view + " : " + layoutError);
        }
        solvedBounds.put(view, new int[]{left, top, right, bottom});
    }

    /**
     * Résout une référence vers une autre vue.
     */
    private View resolveRef(String ref) {
        if (ref == null || PARENT.equals(ref)) {
            return null;
        }
        return viewsById.get(stripRef(ref));
    }

    /**
     * Bornes courantes d'une vue cible : bornes résolues si disponibles
     * (indépendantes du toolkit), sinon état de la vue.
     */
    private int[] targetBounds(View other) {
        int[] solved = solvedBounds.get(other);
        if (solved != null) {
            return solved;
        }
        return new int[]{other.getLeft(), other.getTop(),
                other.getRight(), other.getBottom()};
    }
}
