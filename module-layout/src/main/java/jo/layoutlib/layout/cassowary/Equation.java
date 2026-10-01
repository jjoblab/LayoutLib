package jo.layoutlib.layout.cassowary;

/**
 * Équation linéaire Cassowary (expression = constante).
 *
 * @author jo@Dev
 * @since 1.0
 */
public class Equation {

    private final Expression leftSide;
    private final double rightSide;

    public Equation(Expression leftSide, double rightSide) {
        if (leftSide == null) {
            throw new IllegalArgumentException("leftSide ne peut pas être null");
        }
        this.leftSide = leftSide;
        this.rightSide = rightSide;
    }

    public Expression getLeftSide() {
        return leftSide;
    }

    public double getRightSide() {
        return rightSide;
    }

    /**
     * Convertit en contrainte EQ.
     *
     * @param strength la force
     * @return la contrainte
     */
    public Constraint toEqualityConstraint(double strength) {
        Expression expr = new Expression();
        expr.add(leftSide);
        expr.addConstant(-rightSide);
        return new Constraint(expr, Constraint.Operator.EQ, 0, strength);
    }

    /**
     * Convertit en contrainte GEQ.
     *
     * @param strength la force
     * @return la contrainte
     */
    public Constraint toGreaterOrEqualConstraint(double strength) {
        Expression expr = new Expression();
        expr.add(leftSide);
        expr.addConstant(-rightSide);
        return new Constraint(expr, Constraint.Operator.GEQ, 0, strength);
    }

    /**
     * Convertit en contrainte LEQ.
     *
     * @param strength la force
     * @return la contrainte
     */
    public Constraint toLessOrEqualConstraint(double strength) {
        Expression expr = new Expression();
        expr.add(leftSide);
        expr.addConstant(-rightSide);
        return new Constraint(expr, Constraint.Operator.LEQ, 0, strength);
    }

    @Override
    public String toString() {
        return leftSide + " = " + rightSide;
    }
}
