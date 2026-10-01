package jo.layoutlib.layout.cassowary;

import java.util.Objects;

/**
 * Variable du solver Cassowary, inspiré de l'algorithme de
 * Badros & Borning 2001 (Cassowary constraint solver).
 *
 * <p>Une variable représente une quantité inconnue que le solver doit
 * déterminer. Dans le contexte de ConstraintLayout, chaque côté d'une
 * vue (gauche, droit, haut, bas) ou dimension (largeur, hauteur) est
 * une variable.</p>
 *
 * <h2>Exemple</h2>
 * <pre>{@code
 * Variable x = new Variable("x");
 * Variable y = new Variable("y");
 * solver.addConstraint(new Constraint(
 *     new Expression(new Term(x), -1, new Term(y)),
 *     Constraint.Operator.EQ,
 *     0,
 *     Strength.REQUIRED));
 * }</pre>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class Variable {

    /** Nom de la variable (pour debug). */
    private final String name;

    /** Valeur courante (mise à jour par le solver). */
    private double value;

    /**
     * Construit une variable nommée.
     *
     * @param name le nom de la variable
     */
    public Variable(String name) {
        this.name = name != null ? name : "v";
        this.value = 0.0;
    }

    /**
     * Construit une variable anonyme.
     */
    public Variable() {
        this("v");
    }

    /**
     * @return le nom de la variable
     */
    public String getName() {
        return name;
    }

    /**
     * @return la valeur courante
     */
    public double getValue() {
        return value;
    }

    /**
     * Définit la valeur courante (interne au solver).
     *
     * @param value la nouvelle valeur
     */
    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Variable variable = (Variable) o;
        return Objects.equals(name, variable.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name + " = " + value;
    }
}
