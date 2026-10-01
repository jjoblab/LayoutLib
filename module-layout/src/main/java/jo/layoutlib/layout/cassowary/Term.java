package jo.layoutlib.layout.cassowary;

import java.util.ArrayList;
import java.util.List;

/**
 * Terme d'une expression linéaire Cassowary.
 *
 * <p>Un terme est le produit d'une variable par un coefficient :
 * {@code coefficient * variable}. Une {@link Expression} linéaire est la
 * somme de plusieurs termes plus une constante.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class Term {

    /** Variable du terme. */
    private final Variable variable;

    /** Coefficient multiplicateur. */
    private double coefficient;

    /**
     * Construit un terme avec un coefficient de 1.
     *
     * @param variable la variable
     */
    public Term(Variable variable) {
        this(variable, 1.0);
    }

    /**
     * Construit un terme avec un coefficient.
     *
     * @param variable    la variable
     * @param coefficient le coefficient
     */
    public Term(Variable variable, double coefficient) {
        if (variable == null) {
            throw new IllegalArgumentException("variable ne peut pas être null");
        }
        this.variable = variable;
        this.coefficient = coefficient;
    }

    /**
     * @return la variable du terme
     */
    public Variable getVariable() {
        return variable;
    }

    /**
     * @return le coefficient
     */
    public double getCoefficient() {
        return coefficient;
    }

    /**
     * Définit le coefficient.
     *
     * @param coefficient le nouveau coefficient
     */
    public void setCoefficient(double coefficient) {
        this.coefficient = coefficient;
    }

    /**
     * Multiplie ce terme par un scalaire.
     *
     * @param scalar le scalaire
     * @return un nouveau terme multiplié
     */
    public Term multiply(double scalar) {
        return new Term(variable, coefficient * scalar);
    }

    /**
     * Ajoute un autre terme (même variable).
     *
     * @param other l'autre terme
     * @return un nouveau terme combiné, ou {@code null} si variables différentes
     */
    public Term add(Term other) {
        if (other == null || !variable.equals(other.variable)) {
            return null;
        }
        return new Term(variable, coefficient + other.coefficient);
    }

    /**
     * @return la valeur courante du terme (coefficient * variable.value)
     */
    public double computeValue() {
        return coefficient * variable.getValue();
    }

    @Override
    public String toString() {
        if (coefficient == 1.0) {
            return variable.getName();
        }
        if (coefficient == -1.0) {
            return "-" + variable.getName();
        }
        return coefficient + "*" + variable.getName();
    }

    /**
     * Convertit ce terme en liste (utilitaire pour Expression).
     *
     * @return une liste contenant uniquement ce terme
     */
    public List<Term> asList() {
        List<Term> list = new ArrayList<>(1);
        list.add(this);
        return list;
    }
}
