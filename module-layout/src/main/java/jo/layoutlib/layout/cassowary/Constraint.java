package jo.layoutlib.layout.cassowary;

/**
 * Contrainte linéaire Cassowary.
 *
 * <p>Une contrainte est une relation linéaire entre variables :
 * {@code expression OP constant}, où {@code OP} est l'un des opérateurs
 * {@code <=}, {@code =} ou {@code >=}.</p>
 *
 * <h2>Exemples</h2>
 * <ul>
 *   <li>{@code x + y = 10} : la somme de x et y vaut 10</li>
 *   <li>{@code width >= 100} : la largeur doit être au moins 100</li>
 *   <li>{@code left + width <= right} : contrainte de positionnement</li>
 * </ul>
 *
 * <p>Chaque contrainte a une {@link Strength} qui indique sa priorité.
 * Les contraintes {@link Strength#REQUIRED} doivent toujours être
 * satisfaites ; les autres peuvent être relaxées si nécessaire.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class Constraint {

    /** Opérateur de la contrainte. */
    public enum Operator {
        /** Inférieur ou égal (≤). */
        LEQ("<="),
        /** Égal (=). */
        EQ("="),
        /** Supérieur ou égal (≥). */
        GEQ(">=");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public String toString() {
            return symbol;
        }
    }

    /** Expression linéaire de la contrainte. */
    private final Expression expression;

    /** Opérateur de comparaison. */
    private final Operator operator;

    /** Constante à droite de l'opérateur. */
    private final double constant;

    /** Force de la contrainte. */
    private final double strength;

    /**
     * Construit une contrainte avec la force REQUIRED.
     *
     * @param expression l'expression
     * @param operator   l'opérateur
     * @param constant   la constante à droite
     */
    public Constraint(Expression expression, Operator operator, double constant) {
        this(expression, operator, constant, Strength.REQUIRED);
    }

    /**
     * Construit une contrainte complète.
     *
     * @param expression l'expression
     * @param operator   l'opérateur
     * @param constant   la constante
     * @param strength   la force
     */
    public Constraint(Expression expression, Operator operator,
                      double constant, double strength) {
        if (expression == null) {
            throw new IllegalArgumentException("expression ne peut pas être null");
        }
        if (operator == null) {
            throw new IllegalArgumentException("operator ne peut pas être null");
        }
        this.expression = expression;
        this.operator = operator;
        this.constant = constant;
        this.strength = strength;
    }

    /**
     * @return l'expression de la contrainte
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * @return l'opérateur
     */
    public Operator getOperator() {
        return operator;
    }

    /**
     * @return la constante
     */
    public double getConstant() {
        return constant;
    }

    /**
     * @return la force
     */
    public double getStrength() {
        return strength;
    }

    /**
     * Indique si la contrainte est satisfaite avec les valeurs courantes
     * des variables.
     *
     * @return {@code true} si la contrainte est satisfaite
     */
    public boolean isSatisfied() {
        double value = expression.computeValue();
        switch (operator) {
            case LEQ:
                return value <= constant;
            case EQ:
                return Math.abs(value - constant) < 1e-9;
            case GEQ:
                return value >= constant;
            default:
                return false;
        }
    }

    /**
     * Calcule la violation de la contrainte (0 si satisfaite).
     *
     * @return la violation (positive si non satisfaite)
     */
    public double computeViolation() {
        double value = expression.computeValue();
        switch (operator) {
            case LEQ:
                return Math.max(0, value - constant);
            case EQ:
                return Math.abs(value - constant);
            case GEQ:
                return Math.max(0, constant - value);
            default:
                return 0;
        }
    }

    @Override
    public String toString() {
        return expression + " " + operator + " " + constant
                + " (strength=" + strength + ")";
    }
}
