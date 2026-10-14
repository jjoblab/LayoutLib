package jo.layoutlib.resources;

import java.util.Collections;
import java.util.List;

/**
 * Texte riche d'une chaîne Android, avec ses segments {@code <xliff:g>}
 * capturés.
 *
 * <p>Le texte brut ({@link #getRaw()}) est device-fidèle : c'est exactement
 * ce que aapt2 compilerait — le placeholder de chaque {@code <xliff:g>}
 * (son texte interne) en fait partie, les attributs n'en font pas partie.
 * Les segments capturés ({@link #getSpans()}) portent les attributs
 * {@code id}/{@code example} perdus autrement.</p>
 *
 * <p>La substitution des exemples ({@link #applyExamples()}) produit la
 * chaîne de preview lisible, comme le layout editor d'Android Studio :
 * chaque span portant un {@code example} voit son placeholder remplacé par
 * cette valeur (remplacement littéral, jamais un reformatage). Les
 * placeholders sans exemple — et les placeholders hors de tout span —
 * restent visibles : on ne substitue que ce que l'auteur de la resource a
 * explicitement documenté.</p>
 *
 * <p>Exemple :</p>
 * <pre>{@code
 * // <string name="songs">%1$d chansons — <xliff:g id="artist"
 * //     example="Adèle">%2$s</xliff:g></string>
 * raw     = "%1$d chansons — %2$s"
 * spans   = [XliffSpan{id=artist, example=Adèle, placeholder=%2$s}]
 * preview = "%1$d chansons — Adèle"
 * }</pre>
 *
 * <p>Instances immuables (la liste des spans est non modifiable).</p>
 *
 * @author jo@Dev
 * @since 3.3
 */
public final class XliffText {

    /** Texte brut device-fidèle (placeholders inclus). */
    private final String raw;

    /** Segments {@code <xliff:g>} capturés (jamais null, possiblement vide). */
    private final List<XliffSpan> spans;

    /**
     * Construit un texte riche.
     *
     * @param raw   le texte brut (device-fidèle)
     * @param spans les segments capturés ({@code null} = aucun)
     */
    public XliffText(String raw, List<XliffSpan> spans) {
        this.raw = raw != null ? raw : "";
        this.spans = spans != null
                ? Collections.unmodifiableList(new java.util.ArrayList<>(spans))
                : Collections.emptyList();
    }

    /**
     * @return le texte brut device-fidèle (placeholders de format inclus)
     */
    public String getRaw() {
        return raw;
    }

    /**
     * @return les segments {@code <xliff:g>} capturés (non modifiable,
     *         jamais {@code null})
     */
    public List<XliffSpan> getSpans() {
        return spans;
    }

    /**
     * Indique si le texte porte au moins un segment {@code <xliff:g>}.
     *
     * @return {@code true} si au moins un span a été capturé
     */
    public boolean hasSpans() {
        return !spans.isEmpty();
    }

    /**
     * Substitue les exemples de ce texte.
     *
     * @return la chaîne de preview (voir {@link #applyExamples(String, List)})
     */
    public String applyExamples() {
        return applyExamples(raw, spans);
    }

    /**
     * Substitue les exemples des spans à leurs placeholders dans un texte.
     *
     * <p>Pour chaque span portant un {@code example} non vide, chaque
     * occurrence littérale de son placeholder est remplacée par la valeur
     * d'exemple. Remplacement strictement littéral : une valeur d'exemple
     * contenant {@code %} ou {@code $} n'est jamais réinterprétée. Les
     * placeholders absents du texte sont silencieusement ignorés (chaîne
     * déjà transformée, chaînage de resources…), jamais d'exception.</p>
     *
     * @param raw   le texte brut (peut être {@code null})
     * @param spans les segments à appliquer ({@code null} = aucun)
     * @return le texte avec les exemples substitués, ou le texte brut
     *         inchangé si aucun span applicable
     */
    public static String applyExamples(String raw, List<XliffSpan> spans) {
        if (raw == null || raw.isEmpty() || spans == null || spans.isEmpty()) {
            return raw;
        }
        String result = raw;
        for (XliffSpan span : spans) {
            if (span == null || !span.hasExample()) {
                continue;
            }
            String placeholder = span.getPlaceholder();
            if (placeholder.isEmpty() || !result.contains(placeholder)) {
                continue;
            }
            result = result.replace(placeholder, span.getExample());
        }
        return result;
    }
}
