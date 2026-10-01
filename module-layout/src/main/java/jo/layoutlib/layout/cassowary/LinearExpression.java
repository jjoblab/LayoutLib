package jo.layoutlib.layout.cassowary;

/**
 * Expression linéaire (alias pour Expression).
 *
 * @author jo@Dev
 * @since 1.0
 */
public class LinearExpression extends Expression {

    public LinearExpression() {
        super();
    }

    public LinearExpression(double constant) {
        super(constant);
    }

    public LinearExpression(Term term) {
        super(term);
    }
}
