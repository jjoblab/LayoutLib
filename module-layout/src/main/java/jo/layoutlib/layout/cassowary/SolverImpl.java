package jo.layoutlib.layout.cassowary;

/**
 * Implémentation alternative du solver Cassowary.
 *
 * <p>Cette classe fournit une API statique pour résoudre des systèmes
 * simples de contraintes linéaires.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public final class SolverImpl {

    private SolverImpl() {
    }

    /**
     * Résout un système de contraintes simple à 2 variables.
     *
     * @param c1 première contrainte (equality)
     * @param c2 seconde contrainte (equality)
     * @return un tableau [v1, v2] avec les valeurs résolues, ou null si impossible
     */
    public static double[] solve2x2(Constraint c1, Constraint c2) {
        if (c1 == null || c2 == null) return null;
        if (c1.getOperator() != Constraint.Operator.EQ
                || c2.getOperator() != Constraint.Operator.EQ) {
            return null;
        }
        // Système : a11*v1 + a12*v2 = b1, a21*v1 + a22*v2 = b2
        java.util.List<Term> terms1 = c1.getExpression().getTerms();
        java.util.List<Term> terms2 = c2.getExpression().getTerms();
        if (terms1.size() < 2 || terms2.size() < 2) return null;

        double a11 = terms1.get(0).getCoefficient();
        double a12 = terms1.get(1).getCoefficient();
        double b1 = c1.getConstant() - c1.getExpression().getConstant();

        double a21 = terms2.get(0).getCoefficient();
        double a22 = terms2.get(1).getCoefficient();
        double b2 = c2.getConstant() - c2.getExpression().getConstant();

        double det = a11 * a22 - a12 * a21;
        if (Math.abs(det) < 1e-9) return null;

        double v1 = (b1 * a22 - b2 * a12) / det;
        double v2 = (a11 * b2 - a21 * b1) / det;
        return new double[]{v1, v2};
    }
}
