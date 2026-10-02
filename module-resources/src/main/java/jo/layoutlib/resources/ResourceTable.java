package jo.layoutlib.resources;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Table de resources agrégeant les valeurs parsées depuis tous les fichiers
 * XML de {@code res/values/}.
 *
 * <p>Cette classe est le conteneur interne utilisé par
 * {@link ResourceResolverImpl}. Elle indexe les resources par nom et par
 * type, et supporte plusieurs qualifiers (jour/nuit, portrait/paysage,
 * niveau d'API).</p>
 *
 * <h2>Structure de stockage</h2>
 * <p>Pour chaque type de resource (color, string, dimen, integer, bool), on
 * maintient une map indexée par nom, qui contient elle-même une map
 * qualifier → valeur. Cela permet de retrouver la bonne valeur en fonction
 * du contexte (mode nuit, orientation, niveau d'API) au moment de la
 * résolution.</p>
 *
 * <pre>
 * colorTable = {
 *   "primary" = {
 *     DEFAULT → 0xFF6750A4,
 *     NIGHT   → 0xFFD0BCFF
 *   }
 * }
 * </pre>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ResourceTable {

    /** Table des couleurs, indexée par nom puis qualifier. */
    private final Map<String, Map<ResourceQualifier, String>> colorTable = new HashMap<>();

    /** Table des chaînes, indexée par nom puis qualifier. */
    private final Map<String, Map<ResourceQualifier, String>> stringTable = new HashMap<>();

    /** Table des dimensions, indexée par nom puis qualifier. */
    private final Map<String, Map<ResourceQualifier, String>> dimenTable = new HashMap<>();

    /** Table des entiers, indexée par nom puis qualifier. */
    private final Map<String, Map<ResourceQualifier, Integer>> integerTable = new HashMap<>();

    /** Table des booléens, indexée par nom puis qualifier. */
    private final Map<String, Map<ResourceQualifier, Boolean>> boolTable = new HashMap<>();

    /** Table des string-arrays, indexée par nom puis qualifier. */
    private final Map<String, Map<ResourceQualifier, List<String>>> stringArrayTable =
            new HashMap<>();

    /**
     * Ajoute une couleur dans la table.
     *
     * @param name      nom de la couleur
     * @param value     valeur ARGB
     * @param qualifier qualifier du dossier source
     */
    public void putColor(String name, String value, ResourceQualifier qualifier) {
        colorTable.computeIfAbsent(name, k -> new HashMap<>()).put(qualifier, value);
    }

    /**
     * Ajoute une chaîne dans la table.
     *
     * @param name      nom de la chaîne
     * @param value     valeur
     * @param qualifier qualifier du dossier source
     */
    public void putString(String name, String value, ResourceQualifier qualifier) {
        stringTable.computeIfAbsent(name, k -> new HashMap<>()).put(qualifier, value);
    }

    /**
     * Ajoute une dimension dans la table.
     *
     * @param name      nom de la dimension
     * @param value     valeur brute (ex. {@code "16dp"})
     * @param qualifier qualifier du dossier source
     */
    public void putDimen(String name, String value, ResourceQualifier qualifier) {
        dimenTable.computeIfAbsent(name, k -> new HashMap<>()).put(qualifier, value);
    }

    /**
     * Ajoute un entier dans la table.
     *
     * @param name      nom de l'entier
     * @param value     valeur
     * @param qualifier qualifier du dossier source
     */
    public void putInteger(String name, Integer value, ResourceQualifier qualifier) {
        integerTable.computeIfAbsent(name, k -> new HashMap<>()).put(qualifier, value);
    }

    /**
     * Ajoute un booléen dans la table.
     *
     * @param name      nom du booléen
     * @param value     valeur
     * @param qualifier qualifier du dossier source
     */
    public void putBoolean(String name, Boolean value, ResourceQualifier qualifier) {
        boolTable.computeIfAbsent(name, k -> new HashMap<>()).put(qualifier, value);
    }

    /**
     * Ajoute un string-array ({@code <string-array>}) dans la table.
     *
     * @param name      nom de l'array (référencé par {@code @array/name})
     * @param values    les éléments de l'array
     * @param qualifier qualifier du dossier source
     */
    public void putStringArray(String name, List<String> values,
                               ResourceQualifier qualifier) {
        stringArrayTable.computeIfAbsent(name, k -> new HashMap<>())
                .put(qualifier, values != null ? values : java.util.Collections.emptyList());
    }

    /**
     * Récupère une couleur pour le qualifier cible.
     *
     * @param name   nom de la couleur
     * @param target qualifier cible (contexte courant)
     * @return la valeur ARGB, ou {@code null} si absente
     */
    public String getColor(String name, ResourceQualifier target) {
        return resolveBest(colorTable.get(name), target);
    }

    /**
     * Récupère une chaîne pour le qualifier cible.
     *
     * @param name   nom de la chaîne
     * @param target qualifier cible
     * @return la valeur, ou {@code null} si absente
     */
    public String getString(String name, ResourceQualifier target) {
        return resolveBest(stringTable.get(name), target);
    }

    /**
     * Récupère une dimension brute (non convertie) pour le qualifier cible.
     *
     * @param name   nom de la dimension
     * @param target qualifier cible
     * @return la valeur brute (ex. {@code "16dp"}), ou {@code null} si absente
     */
    public String getDimen(String name, ResourceQualifier target) {
        return resolveBest(dimenTable.get(name), target);
    }

    /**
     * Récupère un entier pour le qualifier cible.
     *
     * @param name   nom de l'entier
     * @param target qualifier cible
     * @return la valeur, ou {@code null} si absente
     */
    public Integer getInteger(String name, ResourceQualifier target) {
        return resolveBest(integerTable.get(name), target);
    }

    /**
     * Récupère un booléen pour le qualifier cible.
     *
     * @param name   nom du booléen
     * @param target qualifier cible
     * @return la valeur, ou {@code null} si absente
     */
    public Boolean getBoolean(String name, ResourceQualifier target) {
        return resolveBest(boolTable.get(name), target);
    }

    /**
     * Récupère un string-array pour le qualifier cible.
     *
     * @param name   nom de l'array
     * @param target qualifier cible
     * @return une copie de la liste des éléments, ou {@code null} si absente
     */
    public List<String> getStringArray(String name, ResourceQualifier target) {
        List<String> values = resolveBest(stringArrayTable.get(name), target);
        return values != null ? new java.util.ArrayList<>(values) : null;
    }

    /**
     * Indique si un string-array existe dans la table.
     *
     * @param name nom de l'array
     * @return {@code true} si l'array existe
     */
    public boolean hasStringArray(String name) {
        return stringArrayTable.containsKey(name);
    }

    /**
     * @return le nombre total de string-arrays stockés
     */
    public int stringArrayCount() {
        return stringArrayTable.size();
    }

    /**
     * Sélectionne la meilleure valeur pour un qualifier cible en suivant
     * les règles de priorité AOSP : plus spécifique = plus prioritaire.
     *
     * @param qualifierToValue map qualifier → valeur
     * @param target           qualifier cible
     * @param <T>              type de la valeur
     * @return la meilleure valeur, ou {@code null} si aucune compatible
     */
    private static <T> T resolveBest(Map<ResourceQualifier, T> qualifierToValue,
                                     ResourceQualifier target) {
        if (qualifierToValue == null || qualifierToValue.isEmpty()) {
            return null;
        }
        T bestValue = null;
        int bestScore = -1;
        for (Map.Entry<ResourceQualifier, T> entry : qualifierToValue.entrySet()) {
            ResourceQualifier q = entry.getKey();
            if (q.isCompatibleWith(target)) {
                int score = q.specificityScore(target);
                if (score > bestScore) {
                    bestScore = score;
                    bestValue = entry.getValue();
                }
            }
        }
        return bestValue;
    }

    /**
     * Indique si une couleur existe dans la table (n'importe quel qualifier).
     *
     * @param name nom de la couleur
     * @return {@code true} si la couleur existe
     */
    public boolean hasColor(String name) {
        return colorTable.containsKey(name);
    }

    /**
     * Indique si une chaîne existe dans la table.
     *
     * @param name nom de la chaîne
     * @return {@code true} si la chaîne existe
     */
    public boolean hasString(String name) {
        return stringTable.containsKey(name);
    }

    /**
     * Indique si une dimension existe dans la table.
     *
     * @param name nom de la dimension
     * @return {@code true} si la dimension existe
     */
    public boolean hasDimen(String name) {
        return dimenTable.containsKey(name);
    }

    /**
     * @return le nombre total de couleurs stockées (tous qualifiers confondus)
     */
    public int colorCount() {
        return colorTable.size();
    }

    /**
     * @return le nombre total de chaînes stockées
     */
    public int stringCount() {
        return stringTable.size();
    }

    /**
     * @return le nombre total de dimensions stockées
     */
    public int dimenCount() {
        return dimenTable.size();
    }

    /**
     * @return le nombre total d'entiers stockés
     */
    public int integerCount() {
        return integerTable.size();
    }

    /**
     * @return le nombre total de booléens stockés
     */
    public int booleanCount() {
        return boolTable.size();
    }

    /**
     * Vide toutes les tables.
     */
    public void clear() {
        colorTable.clear();
        stringTable.clear();
        dimenTable.clear();
        integerTable.clear();
        boolTable.clear();
        stringArrayTable.clear();
    }
}
