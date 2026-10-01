package jo.layoutlib.inflater.bridge.impl;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Pile utilitaire utilisée pendant l'inflation récursive, inspiré de
 * {@code com.android.layoutlib.bridge.impl.Stack} de l'AOSP.
 *
 * <p>Cette classe est utilisée par le {@code BridgeInflater} pour suivre
 * la profondeur d'imbrication pendant l'inflation XML. Elle détecte
 * notamment les boucles infinies (références circulaires dans les
 * {@code <include>}).</p>
 *
 * @param <E> le type des éléments de la pile
 *
 * @author jo@Dev
 * @since 1.0
 */
public class Stack<E> {

    /** Pile interne. */
    private final Deque<E> deque = new ArrayDeque<>();

    /** Profondeur maximale autorisée (anti-boucle). */
    private final int maxDepth;

    /**
     * Construit une pile avec profondeur maximale par défaut (50).
     */
    public Stack() {
        this(50);
    }

    /**
     * Construit une pile avec profondeur maximale personnalisée.
     *
     * @param maxDepth la profondeur maximale
     */
    public Stack(int maxDepth) {
        if (maxDepth <= 0) {
            throw new IllegalArgumentException("maxDepth doit être > 0 : " + maxDepth);
        }
        this.maxDepth = maxDepth;
    }

    /**
     * Empile un élément.
     *
     * @param element l'élément à empiler
     * @throws IllegalStateException si la profondeur maximale est atteinte
     */
    public void push(E element) {
        if (deque.size() >= maxDepth) {
            throw new IllegalStateException(
                    "Profondeur maximale atteinte (" + maxDepth
                            + ") — possible boucle d'inflation");
        }
        deque.push(element);
    }

    /**
     * Dépile l'élément en tête.
     *
     * @return l'élément dépilé, ou {@code null} si la pile est vide
     */
    public E pop() {
        return deque.pollFirst();
    }

    /**
     * @return l'élément en tête sans le dépiler, ou {@code null}
     */
    public E peek() {
        return deque.peekFirst();
    }

    /**
     * @return {@code true} si la pile est vide
     */
    public boolean isEmpty() {
        return deque.isEmpty();
    }

    /**
     * @return la taille actuelle de la pile
     */
    public int size() {
        return deque.size();
    }

    /**
     * Indique si la pile contient un élément.
     *
     * @param element l'élément à rechercher
     * @return {@code true} si présent
     */
    public boolean contains(E element) {
        return deque.contains(element);
    }

    /**
     * Vide la pile.
     */
    public void clear() {
        deque.clear();
    }

    /**
     * @return la profondeur maximale
     */
    public int getMaxDepth() {
        return maxDepth;
    }
}
