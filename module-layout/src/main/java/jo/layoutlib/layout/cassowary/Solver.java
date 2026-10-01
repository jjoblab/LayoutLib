package jo.layoutlib.layout.cassowary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Solver de contraintes Cassowary simplifié, inspiré de l'algorithme
 * de Badros & Borning 2001.
 *
 * <p>Le solver résout un système de contraintes linéaires en trouvant
 * les valeurs des variables qui satisfont toutes les contraintes
 * {@link Strength#REQUIRED}, et qui minimisent la violation des
 * contraintes plus faibles.</p>
 *
 * <h2>Algorithme</h2>
 * <p>Cette implémentation utilise une approche simplifiée :</p>
 * <ol>
 *   <li>Tri des contraintes par force décroissante</li>
 *   <li>Résolution itérative : pour chaque contrainte, on ajuste les
 *       variables pour la satisfaire si possible</li>
 *   <li>Pour les contraintes REQUIRED, échec si non satisfaisable</li>
 *   <li>Pour les contraintes plus faibles, on minimise la violation</li>
 * </ol>
 *
 * <p>Note : cette version est simplifiée par rapport au Cassowary original
 * qui utilise un simplexe modifié. Elle suffit pour la plupart des
 * layouts ConstraintLayout courants.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class Solver {

    /** Contraintes enregistrées. */
    private final List<Constraint> constraints = new ArrayList<>();

    /** Variables enregistrées. */
    private final Map<String, Variable> variables = new HashMap<>();

    /** Indique si le solver a été résolu. */
    private boolean solved = false;

    /** Nombre maximum d'itérations de résolution. */
    private static final int MAX_ITERATIONS = 100;

    /** Tolérance pour la convergence. */
    private static final double TOLERANCE = 1e-6;

    /**
     * Constructeur par défaut.
     */
    public Solver() {
    }

    /**
     * Ajoute une variable au solver.
     *
     * @param variable la variable à ajouter
     */
    public void addVariable(Variable variable) {
        if (variable != null) {
            variables.put(variable.getName(), variable);
        }
    }

    /**
     * Ajoute une contrainte au solver.
     *
     * @param constraint la contrainte à ajouter
     * @return {@code true} si la contrainte a été ajoutée
     */
    public boolean addConstraint(Constraint constraint) {
        if (constraint == null) {
            return false;
        }
        if (constraints.contains(constraint)) {
            return false;
        }
        constraints.add(constraint);
        // Enregistrer les variables de la contrainte
        for (Term term : constraint.getExpression().getTerms()) {
            Variable v = term.getVariable();
            if (!variables.containsKey(v.getName())) {
                variables.put(v.getName(), v);
            }
        }
        solved = false;
        return true;
    }

    /**
     * Supprime une contrainte du solver.
     *
     * @param constraint la contrainte à supprimer
     * @return {@code true} si elle a été supprimée
     */
    public boolean removeConstraint(Constraint constraint) {
        boolean removed = constraints.remove(constraint);
        if (removed) {
            solved = false;
        }
        return removed;
    }

    /**
     * Résout le système de contraintes.
     *
     * @return {@code true} si toutes les contraintes REQUIRED sont satisfaites
     */
    public boolean solve() {
        if (constraints.isEmpty()) {
            solved = true;
            return true;
        }

        // Tri des contraintes par force décroissante
        List<Constraint> sorted = new ArrayList<>(constraints);
        sorted.sort((a, b) -> Double.compare(b.getStrength(), a.getStrength()));

        // Résolution par relaxation itérative
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            boolean allSatisfied = true;
            double totalViolation = 0;

            for (Constraint c : sorted) {
                if (!c.isSatisfied()) {
                    allSatisfied = false;
                    totalViolation += c.computeViolation();
                    relaxConstraint(c);
                }
            }

            if (allSatisfied || totalViolation < TOLERANCE) {
                solved = true;
                return checkRequiredSatisfied(sorted);
            }
        }

        solved = true;
        return checkRequiredSatisfied(sorted);
    }

    /**
     * Vérifie que toutes les contraintes REQUIRED sont satisfaites.
     *
     * @param sorted la liste triée des contraintes
     * @return {@code true} si toutes les REQUIRED sont satisfaites
     */
    private boolean checkRequiredSatisfied(List<Constraint> sorted) {
        for (Constraint c : sorted) {
            if (c.getStrength() >= Strength.REQUIRED && !c.isSatisfied()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Relaxe une contrainte en ajustant les variables.
     *
     * <p>Pour chaque contrainte non satisfaite, on ajuste les variables
     * proportionnellement à leurs coefficients pour rapprocher la
     * contrainte de la satisfaction.</p>
     *
     * @param c la contrainte à relaxer
     */
    private void relaxConstraint(Constraint c) {
        Expression expr = c.getExpression();
        double value = expr.computeValue();
        double target = c.getConstant();
        double diff = target - value;

        List<Term> terms = expr.getTerms();
        if (terms.isEmpty()) {
            return;
        }

        // Calcul du coefficient total absolu pour répartir l'ajustement
        double totalAbsCoeff = 0;
        for (Term t : terms) {
            totalAbsCoeff += Math.abs(t.getCoefficient());
        }
        if (totalAbsCoeff < TOLERANCE) {
            return;
        }

        // Ajustement de chaque variable
        for (Term t : terms) {
            Variable v = t.getVariable();
            double coeff = t.getCoefficient();
            // Ajustement proportionnel au coefficient
            double adjustment = (diff * coeff) / totalAbsCoeff;
            v.setValue(v.getValue() + adjustment);
        }
    }

    /**
     * Récupère la valeur d'une variable après résolution.
     *
     * @param name le nom de la variable
     * @return la valeur, ou 0 si non trouvée
     */
    public double getVariableValue(String name) {
        Variable v = variables.get(name);
        return v != null ? v.getValue() : 0;
    }

    /**
     * Récupère une variable par nom.
     *
     * @param name le nom
     * @return la variable, ou {@code null}
     */
    public Variable getVariable(String name) {
        return variables.get(name);
    }

    /**
     * @return la liste des contraintes
     */
    public List<Constraint> getConstraints() {
        return new ArrayList<>(constraints);
    }

    /**
     * @return le nombre de contraintes
     */
    public int getConstraintCount() {
        return constraints.size();
    }

    /**
     * @return le nombre de variables
     */
    public int getVariableCount() {
        return variables.size();
    }

    /**
     * @return {@code true} si le solver a été résolu
     */
    public boolean isSolved() {
        return solved;
    }

    /**
     * Vide toutes les contraintes et variables.
     */
    public void reset() {
        constraints.clear();
        variables.clear();
        solved = false;
    }

    /**
     * Calcule la violation totale du système (somme des violations).
     *
     * @return la violation totale
     */
    public double computeTotalViolation() {
        double total = 0;
        for (Constraint c : constraints) {
            if (!c.isSatisfied()) {
                total += c.computeViolation() * c.getStrength();
            }
        }
        return total;
    }
}
