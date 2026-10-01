package jo.layoutlib.layout.cassowary;

import java.util.ArrayList;
import java.util.List;

/**
 * Expression linéaire Cassowary.
 *
 * <p>Une expression linéaire est la somme de plusieurs {@link Term} plus
 * une constante :</p>
 *
 * <pre>
 * expression = c0 * v0 + c1 * v1 + ... + cN * vN + constant
 * </pre>
 *
 * <p>Les expressions sont utilisées pour construire les contraintes du
 * solver. Par exemple, la contrainte {@code x + y = 10} correspond à
 * l'expression {@code x + y - 10 = 0}.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class Expression {

    /** Termes de l'expression. */
    private final List<Term> terms;

    /** Constante de l'expression. */
    private double constant;

    /**
     * Construit une expression vide avec constante 0.
     */
    public Expression() {
        this.terms = new ArrayList<>();
        this.constant = 0.0;
    }

    /**
     * Construit une expression avec une constante.
     *
     * @param constant la constante
     */
    public Expression(double constant) {
        this.terms = new ArrayList<>();
        this.constant = constant;
    }

    /**
     * Construit une expression avec un terme unique.
     *
     * @param term le terme
     */
    public Expression(Term term) {
        this.terms = new ArrayList<>();
        if (term != null) {
            this.terms.add(term);
        }
        this.constant = 0.0;
    }

    /**
     * Construit une expression complète.
     *
     * @param terms    les termes
     * @param constant la constante
     */
    public Expression(List<Term> terms, double constant) {
        this.terms = new ArrayList<>(terms);
        this.constant = constant;
    }

    /**
     * @return la liste des termes
     */
    public List<Term> getTerms() {
        return new ArrayList<>(terms);
    }

    /**
     * @return la constante
     */
    public double getConstant() {
        return constant;
    }

    /**
     * Ajoute un terme à l'expression.
     *
     * @param term le terme à ajouter
     * @return cette expression (chaînage)
     */
    public Expression addTerm(Term term) {
        if (term != null) {
            // Si la variable existe déjà, on combine les coefficients
            for (int i = 0; i < terms.size(); i++) {
                Term existing = terms.get(i);
                if (existing.getVariable().equals(term.getVariable())) {
                    terms.set(i, existing.add(term));
                    return this;
                }
            }
            terms.add(term);
        }
        return this;
    }

    /**
     * Ajoute une constante.
     *
     * @param c la constante à ajouter
     * @return cette expression
     */
    public Expression addConstant(double c) {
        this.constant += c;
        return this;
    }

    /**
     * Ajoute une autre expression.
     *
     * @param other l'autre expression
     * @return cette expression
     */
    public Expression add(Expression other) {
        if (other != null) {
            for (Term t : other.terms) {
                addTerm(t);
            }
            this.constant += other.constant;
        }
        return this;
    }

    /**
     * Soustrait une autre expression.
     *
     * @param other l'autre expression
     * @return cette expression
     */
    public Expression subtract(Expression other) {
        if (other != null) {
            for (Term t : other.terms) {
                addTerm(new Term(t.getVariable(), -t.getCoefficient()));
            }
            this.constant -= other.constant;
        }
        return this;
    }

    /**
     * Multiplie l'expression par un scalaire.
     *
     * @param scalar le scalaire
     * @return une nouvelle expression multipliée
     */
    public Expression multiply(double scalar) {
        Expression result = new Expression(constant * scalar);
        for (Term t : terms) {
            result.addTerm(t.multiply(scalar));
        }
        return result;
    }

    /**
     * Calcule la valeur courante de l'expression.
     *
     * @return la valeur (somme des termes + constante)
     */
    public double computeValue() {
        double sum = constant;
        for (Term t : terms) {
            sum += t.computeValue();
        }
        return sum;
    }

    /**
     * @return le nombre de termes
     */
    public int size() {
        return terms.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < terms.size(); i++) {
            if (i > 0) {
                sb.append(" + ");
            }
            sb.append(terms.get(i));
        }
        if (constant != 0 || terms.isEmpty()) {
            if (!terms.isEmpty()) {
                sb.append(" + ");
            }
            sb.append(constant);
        }
        return sb.toString();
    }
}
