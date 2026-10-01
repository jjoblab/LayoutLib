package jo.layoutlib.layout.cassowary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests unitaires du solver Cassowary.
 *
 * <p>Valide que le solver résout correctement les systèmes de contraintes
 * linéaires : égalités, inégalités, contraintes REQUIRED vs WEAK, etc.</p>
 *
 * @author jo@Dev
 */
@DisplayName("Solver Cassowary — résolution de contraintes")
class SolverTest {

    private Solver solver;

    @BeforeEach
    void setUp() {
        solver = new Solver();
    }

    @Nested
    @DisplayName("Contraintes d'égalité")
    class EqualityConstraints {

        @Test
        @DisplayName("résout x = 10")
        void shouldSolveSimpleEquality() {
            Variable x = new Variable("x");
            solver.addVariable(x);
            Expression expr = new Expression(new Term(x)).addConstant(-10);
            solver.addConstraint(new Constraint(expr, Constraint.Operator.EQ, 0));
            boolean success = solver.solve();
            assertThat(success).isTrue();
            assertThat(x.getValue()).isCloseTo(10.0, within(0.01));
        }

        @Test
        @DisplayName("résout x + y = 20 avec x = y")
        void shouldSolveSystemWithTwoVariables() {
            Variable x = new Variable("x");
            Variable y = new Variable("y");
            solver.addVariable(x);
            solver.addVariable(y);

            Expression e1 = new Expression()
                    .addTerm(new Term(x))
                    .addTerm(new Term(y))
                    .addConstant(-20);
            solver.addConstraint(new Constraint(e1, Constraint.Operator.EQ, 0));

            Expression e2 = new Expression()
                    .addTerm(new Term(x))
                    .addTerm(new Term(y, -1));
            solver.addConstraint(new Constraint(e2, Constraint.Operator.EQ, 0));

            boolean success = solver.solve();
            assertThat(success).isTrue();
            assertThat(x.getValue()).isCloseTo(10.0, within(0.1));
            assertThat(y.getValue()).isCloseTo(10.0, within(0.1));
        }
    }

    @Nested
    @DisplayName("Contraintes d'inégalité")
    class InequalityConstraints {

        @Test
        @DisplayName("résout x >= 5")
        void shouldSolveGreaterThanOrEqual() {
            Variable x = new Variable("x");
            solver.addVariable(x);
            Expression expr = new Expression(new Term(x)).addConstant(-5);
            solver.addConstraint(new Constraint(expr, Constraint.Operator.GEQ, 0));
            boolean success = solver.solve();
            assertThat(success).isTrue();
            assertThat(x.getValue()).isGreaterThanOrEqualTo(5.0 - 0.01);
        }

        @Test
        @DisplayName("résout x <= 5")
        void shouldSolveLessThanOrEqual() {
            Variable x = new Variable("x");
            x.setValue(10);
            solver.addVariable(x);
            Expression expr = new Expression(new Term(x)).addConstant(-5);
            solver.addConstraint(new Constraint(expr, Constraint.Operator.LEQ, 0));
            boolean success = solver.solve();
            assertThat(success).isTrue();
            assertThat(x.getValue()).isLessThanOrEqualTo(5.0 + 0.01);
        }
    }

    @Nested
    @DisplayName("Gestion")
    class Management {

        @Test
        @DisplayName("addVariable enregistre la variable")
        void shouldRegisterVariable() {
            Variable v = new Variable("test");
            solver.addVariable(v);
            assertThat(solver.getVariableCount()).isEqualTo(1);
            assertThat(solver.getVariable("test")).isSameAs(v);
        }

        @Test
        @DisplayName("removeConstraint supprime une contrainte")
        void shouldRemoveConstraint() {
            Variable x = new Variable("x");
            solver.addVariable(x);
            Expression expr = new Expression(new Term(x)).addConstant(-5);
            Constraint c = new Constraint(expr, Constraint.Operator.EQ, 0);
            solver.addConstraint(c);
            boolean removed = solver.removeConstraint(c);
            assertThat(removed).isTrue();
            assertThat(solver.getConstraintCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("reset vide tout")
        void shouldResetSolver() {
            Variable x = new Variable("x");
            solver.addVariable(x);
            Expression expr = new Expression(new Term(x)).addConstant(-5);
            solver.addConstraint(new Constraint(expr, Constraint.Operator.EQ, 0));
            solver.reset();
            assertThat(solver.getConstraintCount()).isEqualTo(0);
            assertThat(solver.getVariableCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("solve retourne true pour un système vide")
        void shouldReturnTrueForEmptySystem() {
            assertThat(solver.solve()).isTrue();
        }
    }
}
