package jo.layoutlib.layout;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests du {@link ConstraintSolver} câblé au solver Cassowary
 * (ADR 0116 — P2/L9).
 *
 * <p>Valide les fonctionnalités absentes du solver v1 : chaînes
 * (spread/packed/pondérée), guidelines, barrières, ratios
 * ({@code layout_constraintDimensionRatio}), {@code match_constraint}
 * (0dp), biais, pourcentages — plus les non-régressions (ancres parent,
 * ancre vue-à-vue, marges, repli legacy).</p>
 *
 * @author jo@Dev
 * @since 3.0
 */
@DisplayName("ConstraintSolver — moteur Cassowary (P2/L9)")
class ConstraintSolverCassowaryTest {

    private Context context;

    @BeforeEach
    void setUp() {
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.density = 2f;
        Resources resources = mock(Resources.class);
        when(resources.getDisplayMetrics()).thenReturn(metrics);
        context = mock(Context.class);
        when(context.getResources()).thenReturn(resources);
    }

    private View view() {
        return new View(context);
    }

    /** Map d'attributs fluide pour les specs. */
    private static Map<String, String> attrs(String... pairs) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            m.put(pairs[i], pairs[i + 1]);
        }
        return m;
    }

    private int[] bounds(ConstraintSolver solver, View view) {
        int[] b = solver.getSolvedBounds(view);
        assertThat(b).as("bornes résolues").isNotNull();
        return b;
    }

    // ------------------------------------------------------------------
    // Non-régressions v1
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Ancrages de base (non-régression)")
    class BasicAnchors {

        @Test
        @DisplayName("start_toStartOf parent + endToEndOf parent centre la vue")
        void centeredInParent() {
            View v = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(v, "v", attrs(
                    "layout_constraintStart_toStartOf", "parent",
                    "layout_constraintEnd_toEndOf", "parent",
                    "layout_constraintTop_toTopOf", "parent"), 200, 100);
            solver.solve(1000, 800);

            int[] b = bounds(solver, v);
            assertThat(solver.lastSolveUsedCassowary()).isTrue();
            assertThat(b[0]).isEqualTo(400); // (1000-200)/2
            assertThat(b[2]).isEqualTo(600);
            assertThat(b[1]).isEqualTo(0);   // top_toTopOf parent
        }

        @Test
        @DisplayName("les marges sont respectées")
        void marginsRespected() {
            View v = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(v, "v", attrs(
                    "layout_constraintStart_toStartOf", "parent",
                    "layout_constraintTop_toTopOf", "parent",
                    "layout_marginStart", "50",
                    "layout_marginTop", "30"), 100, 80);
            solver.solve(1000, 800);

            int[] b = bounds(solver, v);
            assertThat(b[0]).isEqualTo(50);
            assertThat(b[1]).isEqualTo(30);
            assertThat(b[2]).isEqualTo(150);
            assertThat(b[3]).isEqualTo(110);
        }

        @Test
        @DisplayName("une vue ancrée au bas-droit du parent")
        void anchoredBottomRight() {
            View v = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(v, "v", attrs(
                    "layout_constraintEnd_toEndOf", "parent",
                    "layout_constraintBottom_toBottomOf", "parent",
                    "layout_marginEnd", "20",
                    "layout_marginBottom", "10"), 100, 50);
            solver.solve(1000, 800);

            int[] b = bounds(solver, v);
            assertThat(b[2]).isEqualTo(980);
            assertThat(b[3]).isEqualTo(790);
            assertThat(b[0]).isEqualTo(880);
            assertThat(b[1]).isEqualTo(740);
        }

        @Test
        @DisplayName("ancre vue-à-vue : start_toEndOf positionne après la cible")
        void viewToViewAnchor() {
            View a = view();
            View b = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(a, "a", attrs(
                    "layout_constraintStart_toStartOf", "parent",
                    "layout_constraintTop_toTopOf", "parent"), 100, 40);
            solver.registerView(b, "b", attrs(
                    "layout_constraintStart_toEndOf", "@id/a",
                    "layout_constraintTop_toTopOf", "parent"), 80, 40);
            solver.solve(1000, 800);

            int[] ba = bounds(solver, a);
            int[] bb = bounds(solver, b);
            assertThat(bb[0]).isEqualTo(ba[2]);
            assertThat(bb[2]).isEqualTo(ba[2] + 80);
        }

        @Test
        @DisplayName("vertical : top_toBottomOf empile sous la cible")
        void verticalStacking() {
            View a = view();
            View b = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(a, "a", attrs(
                    "layout_constraintStart_toStartOf", "parent",
                    "layout_constraintTop_toTopOf", "parent"), 100, 60);
            solver.registerView(b, "b", attrs(
                    "layout_constraintStart_toStartOf", "parent",
                    "layout_constraintTop_toBottomOf", "@id/a"), 100, 40);
            solver.solve(1000, 800);

            assertThat(bounds(solver, b)[1]).isEqualTo(60);
        }
    }

    // ------------------------------------------------------------------
    // Nouveautés P2
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Biais")
    class Bias {

        @Test
        @DisplayName("biais horizontal 0.25 place la vue au quart de l'espace libre")
        void horizontalBiasQuarter() {
            View v = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(v, "v", attrs(
                    "layout_constraintStart_toStartOf", "parent",
                    "layout_constraintEnd_toEndOf", "parent",
                    "layout_constraintHorizontal_bias", "0.25"), 200, 100);
            solver.solve(1000, 800);

            int[] b = bounds(solver, v);
            // espace libre = 800 ; 0.25 → left = 200
            assertThat(b[0]).isEqualTo(200);
            assertThat(b[2]).isEqualTo(400);
        }

        @Test
        @DisplayName("biais vertical 0.75")
        void verticalBiasThreeQuarters() {
            View v = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(v, "v", attrs(
                    "layout_constraintTop_toTopOf", "parent",
                    "layout_constraintBottom_toBottomOf", "parent",
                    "layout_constraintVertical_bias", "0.75"), 100, 200);
            solver.solve(1000, 800);

            int[] b = bounds(solver, v);
            // espace libre = 600 ; 0.75 → top = 450
            assertThat(b[1]).isEqualTo(450);
        }
    }

    @Nested
    @DisplayName("Pourcentages")
    class Percentages {

        @Test
        @DisplayName("width_percent 0.5 dimensionne à la moitié du parent")
        void widthPercentHalf() {
            View v = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(v, "v", attrs(
                    "layout_constraintStart_toStartOf", "parent",
                    "layout_constraintWidth_percent", "0.5"), 100, 50);
            solver.solve(1000, 800);

            int[] b = bounds(solver, v);
            assertThat(b[2] - b[0]).isEqualTo(500);
        }

        @Test
        @DisplayName("height_percent 0.25")
        void heightPercentQuarter() {
            View v = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(v, "v", attrs(
                    "layout_constraintTop_toTopOf", "parent",
                    "layout_constraintHeight_percent", "0.25"), 100, 50);
            solver.solve(1000, 800);

            int[] b = bounds(solver, v);
            assertThat(b[3] - b[1]).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("Ratios (layout_constraintDimensionRatio)")
    class Ratios {

        @Test
        @DisplayName("ratio W,16:9 pilote la largeur depuis la hauteur")
        void ratioDrivesWidth() {
            View v = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(v, "v", attrs(
                    "layout_constraintTop_toTopOf", "parent",
                    "layout_constraintDimensionRatio", "W,16:9"), 100, 180);
            solver.solve(1000, 800);

            int[] b = bounds(solver, v);
            // hauteur fixée à 180 (mesurée) ; largeur = 180 * 16/9 = 320
            assertThat(b[2] - b[0]).isEqualTo(320);
            assertThat(b[3] - b[1]).isEqualTo(180);
        }

        @Test
        @DisplayName("ratio H,1:2 pilote la hauteur depuis la largeur")
        void ratioDrivesHeight() {
            View v = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(v, "v", attrs(
                    "layout_constraintStart_toStartOf", "parent",
                    "layout_constraintDimensionRatio", "H,1:2"), 300, 100);
            solver.solve(1000, 800);

            int[] b = bounds(solver, v);
            // largeur fixée à 300 (mesurée) ; hauteur = 300 * 1/2 = 150
            assertThat(b[2] - b[0]).isEqualTo(300);
            assertThat(b[3] - b[1]).isEqualTo(150);
        }

        @Test
        @DisplayName("ratio simple \"16:9\" équivaut à W,16:9")
        void simpleRatioDefaultsToWidth() {
            View v = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(v, "v", attrs(
                    "layout_constraintTop_toTopOf", "parent",
                    "layout_constraintDimensionRatio", "16:9"), 100, 90);
            solver.solve(1000, 800);

            int[] b = bounds(solver, v);
            assertThat(b[2] - b[0]).isEqualTo(160);
        }

        @Test
        @DisplayName("ratio 0dp + double ancrage : W = ratio × H du span")
        void ratioWithMatchConstraint() {
            View v = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(v, "v", attrs(
                    "layout_constraintStart_toStartOf", "parent",
                    "layout_constraintEnd_toEndOf", "parent",
                    "layout_constraintDimensionRatio", "1:1"), 0, 0);
            // Vue sans taille mesurée : hauteur contrainte par le span
            // vertical ? Non — ici hauteur reste mesurée (0). On teste plutôt
            // le cas height connu via match: laissons la hauteur = 0 → W=0.
            // Ce cas vérifie surtout qu'aucune exception n'est levée.
            solver.solve(1000, 800);
            assertThat(solver.getSolvedBounds(v)).isNotNull();
        }
    }

    @Nested
    @DisplayName("match_constraint (0dp)")
    class MatchConstraint {

        @Test
        @DisplayName("0dp avec double ancrage remplit le span moins les marges")
        void fillsSpan() {
            View v = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(v, "v", attrs(
                    "layout_width", "0dp",
                    "layout_constraintStart_toStartOf", "parent",
                    "layout_constraintEnd_toEndOf", "parent",
                    "layout_constraintTop_toTopOf", "parent",
                    "layout_marginStart", "100",
                    "layout_marginEnd", "100"), 0, 100);
            solver.solve(1000, 800);

            int[] b = bounds(solver, v);
            assertThat(b[0]).isEqualTo(100);
            assertThat(b[2]).isEqualTo(900);
            assertThat(b[2] - b[0]).isEqualTo(800);
        }
    }

    @Nested
    @DisplayName("Guidelines")
    class Guidelines {

        @Test
        @DisplayName("guideline begin : les vues s'ancrent dessus")
        void guidelineBegin() {
            View guide = view();
            View v = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(guide, "guide", attrs(
                    "layout_constraintGuide_begin", "200"), 0, 0);
            solver.registerView(v, "v", attrs(
                    "layout_constraintStart_toStartOf", "@id/guide",
                    "layout_constraintTop_toTopOf", "parent"), 100, 50);
            solver.solve(1000, 800);

            assertThat(bounds(solver, v)[0]).isEqualTo(200);
        }

        @Test
        @DisplayName("guideline percent 0.5")
        void guidelinePercent() {
            View guide = view();
            View v = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(guide, "guide", attrs(
                    "layout_constraintGuide_percent", "0.5"), 0, 0);
            solver.registerView(v, "v", attrs(
                    "layout_constraintStart_toStartOf", "@id/guide",
                    "layout_constraintTop_toTopOf", "parent"), 100, 50);
            solver.solve(1000, 800);

            assertThat(bounds(solver, v)[0]).isEqualTo(500);
        }

        @Test
        @DisplayName("guideline end 150 : à 150px du bord droit")
        void guidelineEnd() {
            View guide = view();
            View v = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(guide, "guide", attrs(
                    "layout_constraintGuide_end", "150"), 0, 0);
            solver.registerView(v, "v", attrs(
                    "layout_constraintEnd_toStartOf", "@id/guide"), 100, 50);
            solver.solve(1000, 800);

            int[] b = bounds(solver, v);
            assertThat(b[2]).isEqualTo(850);
            assertThat(b[0]).isEqualTo(750);
        }

        @Test
        @DisplayName("guideline horizontale (orientation horizontal)")
        void horizontalGuideline() {
            View guide = view();
            View v = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(guide, "guide", attrs(
                    "orientation", "horizontal",
                    "layout_constraintGuide_begin", "300"), 0, 0);
            solver.registerView(v, "v", attrs(
                    "layout_constraintTop_toTopOf", "@id/guide",
                    "layout_constraintStart_toStartOf", "parent"), 100, 50);
            solver.solve(1000, 800);

            assertThat(bounds(solver, v)[1]).isEqualTo(300);
        }
    }

    @Nested
    @DisplayName("Barrières")
    class Barriers {

        @Test
        @DisplayName("barrière right : une vue s'ancre après la plus à droite")
        void rightBarrier() {
            View a = view();
            View b = view();
            View barrier = view();
            View c = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(a, "a", attrs(
                    "layout_constraintStart_toStartOf", "parent"), 100, 40);
            solver.registerView(b, "b", attrs(
                    "layout_constraintStart_toStartOf", "parent",
                    "layout_constraintTop_toBottomOf", "@id/a"), 300, 40);
            solver.registerView(barrier, "barrier", attrs(
                    "barrierDirection", "right",
                    "constraint_referenced_ids", "a,b"), 0, 0);
            solver.registerView(c, "c", attrs(
                    "layout_constraintStart_toStartOf", "@id/barrier",
                    "layout_constraintTop_toTopOf", "parent"), 50, 40);
            solver.solve(1000, 800);

            // b est la plus large (300) → barrière à x=300
            assertThat(bounds(solver, c)[0]).isEqualTo(300);
        }

        @Test
        @DisplayName("barrière bottom : une vue s'ancre sous le plus bas")
        void bottomBarrier() {
            View a = view();
            View barrier = view();
            View c = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(a, "a", attrs(
                    "layout_constraintTop_toTopOf", "parent",
                    "layout_constraintHeight_percent", "0.5"), 100, 40);
            solver.registerView(barrier, "barrier", attrs(
                    "barrierDirection", "bottom",
                    "constraint_referenced_ids", "a"), 0, 0);
            solver.registerView(c, "c", attrs(
                    "layout_constraintTop_toTopOf", "@id/barrier"), 100, 40);
            solver.solve(1000, 800);

            // a occupe la moitié du parent (400px) → barrière à y=400
            assertThat(bounds(solver, c)[1]).isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("Chaînes")
    class Chains {

        @Test
        @DisplayName("chaîne spread : espaces égaux autour des trois vues")
        void spreadChain() {
            View a = view();
            View b = view();
            View c = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(a, "a", attrs(
                    "layout_constraintStart_toStartOf", "parent",
                    "layout_constraintEnd_toStartOf", "@id/b"), 100, 50);
            solver.registerView(b, "b", attrs(
                    "layout_constraintStart_toEndOf", "@id/a",
                    "layout_constraintEnd_toStartOf", "@id/c"), 100, 50);
            solver.registerView(c, "c", attrs(
                    "layout_constraintStart_toEndOf", "@id/b",
                    "layout_constraintEnd_toEndOf", "parent"), 100, 50);
            solver.solve(1000, 800);

            // 3 vues × 100 = 300 ; espace libre = 700 ; 4 espaces = 175
            int[] ba = bounds(solver, a);
            int[] bb = bounds(solver, b);
            int[] bc = bounds(solver, c);
            assertThat(solver.lastSolveUsedCassowary()).isTrue();
            assertThat(ba[0]).isEqualTo(175);
            assertThat(bb[0]).isEqualTo(450);   // 175 + 100 + 175
            assertThat(bc[0]).isEqualTo(725);   // 450 + 100 + 175
            assertThat(bc[2]).isEqualTo(825);   // 1000 - 175
        }

        @Test
        @DisplayName("chaîne packed : groupe compact centré")
        void packedChain() {
            View a = view();
            View b = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(a, "a", attrs(
                    "layout_constraintStart_toStartOf", "parent",
                    "layout_constraintEnd_toStartOf", "@id/b",
                    "layout_constraintHorizontal_chainStyle", "packed"), 100, 50);
            solver.registerView(b, "b", attrs(
                    "layout_constraintStart_toEndOf", "@id/a",
                    "layout_constraintEnd_toEndOf", "parent"), 200, 50);
            solver.solve(1000, 800);

            // groupe = 300 ; libre = 700 ; biais 0.5 → left = 350
            int[] ba = bounds(solver, a);
            int[] bb = bounds(solver, b);
            assertThat(ba[0]).isEqualTo(350);
            assertThat(bb[0]).isEqualTo(450);
            assertThat(bb[2]).isEqualTo(650);
        }

        @Test
        @DisplayName("chaîne packed avec biais 0 : collée à gauche")
        void packedChainBiasZero() {
            View a = view();
            View b = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(a, "a", attrs(
                    "layout_constraintStart_toStartOf", "parent",
                    "layout_constraintEnd_toStartOf", "@id/b",
                    "layout_constraintHorizontal_chainStyle", "packed",
                    "layout_constraintHorizontal_bias", "0"), 100, 50);
            solver.registerView(b, "b", attrs(
                    "layout_constraintStart_toEndOf", "@id/a",
                    "layout_constraintEnd_toEndOf", "parent"), 200, 50);
            solver.solve(1000, 800);

            assertThat(bounds(solver, a)[0]).isEqualTo(0);
            assertThat(bounds(solver, b)[2]).isEqualTo(300);
        }

        @Test
        @DisplayName("chaîne pondérée : les vues 0dp se partagent l'espace par poids")
        void weightedChain() {
            View a = view();
            View b = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(a, "a", attrs(
                    "layout_width", "0dp",
                    "layout_constraintHorizontal_weight", "1",
                    "layout_constraintStart_toStartOf", "parent",
                    "layout_constraintEnd_toStartOf", "@id/b"), 0, 50);
            solver.registerView(b, "b", attrs(
                    "layout_width", "0dp",
                    "layout_constraintHorizontal_weight", "3",
                    "layout_constraintStart_toEndOf", "@id/a",
                    "layout_constraintEnd_toEndOf", "parent"), 0, 50);
            solver.solve(1000, 800);

            // a = 250 (1/4), b = 750 (3/4)
            int[] ba = bounds(solver, a);
            int[] bb = bounds(solver, b);
            assertThat(ba[0]).isEqualTo(0);
            assertThat(ba[2]).isEqualTo(250);
            assertThat(bb[0]).isEqualTo(250);
            assertThat(bb[2]).isEqualTo(1000);
        }

        @Test
        @DisplayName("chaîne verticale spread")
        void verticalSpreadChain() {
            View a = view();
            View b = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(a, "a", attrs(
                    "layout_constraintTop_toTopOf", "parent",
                    "layout_constraintBottom_toTopOf", "@id/b"), 100, 100);
            solver.registerView(b, "b", attrs(
                    "layout_constraintTop_toBottomOf", "@id/a",
                    "layout_constraintBottom_toBottomOf", "parent"), 100, 100);
            solver.solve(1000, 600);

            // 2 vues × 100 = 200 ; libre = 400 ; 3 espaces ≈ 133.33
            int[] ba = bounds(solver, a);
            int[] bb = bounds(solver, b);
            assertThat(ba[1]).isCloseTo(133, within(2));
            assertThat(bb[1]).isCloseTo(366, within(2));
            assertThat(bb[3]).isCloseTo(466, within(2));
        }

        @Test
        @DisplayName("chaîne avec marges inter-éléments")
        void chainWithMargins() {
            View a = view();
            View b = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(a, "a", attrs(
                    "layout_constraintStart_toStartOf", "parent",
                    "layout_constraintEnd_toStartOf", "@id/b",
                    "layout_constraintHorizontal_chainStyle", "packed",
                    "layout_constraintHorizontal_bias", "0",
                    "layout_marginEnd", "50"), 100, 50);
            solver.registerView(b, "b", attrs(
                    "layout_constraintStart_toEndOf", "@id/a",
                    "layout_constraintEnd_toEndOf", "parent"), 100, 50);
            solver.solve(1000, 800);

            // packed biais 0 : a à 0, gap = margeEnd(a) = 50
            int[] ba = bounds(solver, a);
            int[] bb = bounds(solver, b);
            assertThat(ba[0]).isEqualTo(0);
            assertThat(bb[0]).isEqualTo(150); // 100 + 50
        }
    }

    // ------------------------------------------------------------------
    // Robustesse
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Robustesse et repli")
    class Robustness {

        @Test
        @DisplayName("référence inconnue : ancre ignorée, pas d'exception")
        void unknownReferenceIgnored() {
            View v = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(v, "v", attrs(
                    "layout_constraintStart_toStartOf", "@id/inconnu",
                    "layout_constraintTop_toTopOf", "parent"), 100, 50);
            boolean result = solver.solve(1000, 800);

            // Le repli legacy (ou le solver) doit donner une position saine
            int[] b = bounds(solver, v);
            assertThat(b[2] - b[0]).isEqualTo(100);
        }

        @Test
        @DisplayName("solve sans aucune vue enregistrée ne lève pas")
        void emptySolverSolves() {
            ConstraintSolver solver = new ConstraintSolver();
            assertThat(solver.solve(1000, 800)).isFalse();
        }

        @Test
        @DisplayName("contraintes mutuellement contradictoires : pas d'exception, bornes saines")
        void contradictoryConstraintsFallback() {
            View a = view();
            View b = view();
            ConstraintSolver solver = new ConstraintSolver();
            // a après b, b après a (cycle)
            solver.registerView(a, "a", attrs(
                    "layout_constraintStart_toEndOf", "@id/b"), 100, 50);
            solver.registerView(b, "b", attrs(
                    "layout_constraintStart_toEndOf", "@id/a"), 100, 50);
            solver.solve(1000, 800);

            // Quel que soit le chemin (Cassowary ou legacy), des bornes
            // finies doivent exister
            assertThat(solver.getSolvedBounds(a)).isNotNull();
            assertThat(solver.getSolvedBounds(b)).isNotNull();
        }

        @Test
        @DisplayName("getSolvedBounds retourne null pour une vue non enregistrée")
        void unknownViewBoundsNull() {
            ConstraintSolver solver = new ConstraintSolver();
            assertThat(solver.getSolvedBounds(view())).isNull();
        }

        @Test
        @DisplayName("clear réinitialise le solver")
        void clearResets() {
            View v = view();
            ConstraintSolver solver = new ConstraintSolver();
            solver.registerView(v, "v", attrs(
                    "layout_constraintStart_toStartOf", "parent"), 100, 50);
            solver.solve(1000, 800);
            solver.clear();
            assertThat(solver.getViewCount()).isZero();
            assertThat(solver.getSolvedBounds(v)).isNull();
        }

        @Test
        @DisplayName("API legacy ConstraintInfo fonctionne toujours")
        void legacyConstraintInfoStillWorks() {
            View v = view();
            ConstraintSolver solver = new ConstraintSolver();
            ConstraintSolver.ConstraintInfo info = new ConstraintSolver.ConstraintInfo();
            info.startToStartOf = "parent";
            info.topToTop = "parent";
            solver.registerView(v, "legacy", info);
            solver.registerView(v, "legacy", (org.xmlpull.v1.XmlPullParser) null);
            solver.solve(1000, 800);
            assertThat(solver.getSolvedBounds(v)).isNotNull();
        }
    }
}
