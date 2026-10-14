package jo.layoutlib.resources;

import java.util.Objects;

/**
 * Segment {@code <xliff:g>} capturé dans une chaîne Android.
 *
 * <p>Le sous-ensemble Android de XLIFF s'écrit dans les fichiers de
 * resources localisés :</p>
 *
 * <pre>{@code
 * <string name="songs">%1$d chansons — <xliff:g id="artist"
 *     example="Adèle">%2$s</xliff:g></string>
 * }</pre>
 *
 * <p>aapt2 compile le texte interne (le placeholder de format) dans la
 * chaîne finale ; les attributs {@code id} (identité du segment pour les
 * traducteurs) et {@code example} (valeur d'exemple) servent uniquement aux
 * outils. LayoutLib les capture pour deux usages :</p>
 *
 * <ul>
 *   <li>l'introspection : un éditeur de resources peut afficher les segments
 *       et leurs exemples (panneaux de localisation) ;</li>
 *   <li>la preview lisible : substituer chaque {@code example} à son
 *       placeholder, comme le layout editor d'Android Studio — voir
 *       {@link XliffText#applyExamples(String, java.util.List)}.</li>
 * </ul>
 *
 * <p>Instances immuables — {@code id} et {@code example} peuvent être
 * {@code null} (attribut absent), {@code placeholder} est toujours non vide.</p>
 *
 * @author jo@Dev
 * @since 3.3
 */
public final class XliffSpan {

    /** Identité du segment déclarée par {@code id} (peut être {@code null}). */
    private final String id;

    /** Valeur d'exemple déclarée par {@code example} (peut être {@code null}). */
    private final String example;

    /** Texte interne du segment — le placeholder de format (ex. {@code %2$s}). */
    private final String placeholder;

    /**
     * Construit un segment.
     *
     * @param id          l'attribut {@code id} (nullable)
     * @param example     l'attribut {@code example} (nullable)
     * @param placeholder le texte interne (trimmé ; vide interprété comme
     *                    {@code ""} — le segment devient inopérant)
     */
    public XliffSpan(String id, String example, String placeholder) {
        this.id = id;
        this.example = example;
        this.placeholder = placeholder != null ? placeholder.trim() : "";
    }

    /**
     * @return l'attribut {@code id}, ou {@code null} si absent
     */
    public String getId() {
        return id;
    }

    /**
     * @return l'attribut {@code example}, ou {@code null} si absent
     */
    public String getExample() {
        return example;
    }

    /**
     * @return le placeholder de format (texte interne du segment, trimmé)
     */
    public String getPlaceholder() {
        return placeholder;
    }

    /**
     * Indique si ce segment porte une valeur d'exemple exploitable.
     *
     * @return {@code true} si {@code example} est présent et non vide
     */
    public boolean hasExample() {
        return example != null && !example.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof XliffSpan)) {
            return false;
        }
        XliffSpan other = (XliffSpan) o;
        return Objects.equals(id, other.id)
                && Objects.equals(example, other.example)
                && Objects.equals(placeholder, other.placeholder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, example, placeholder);
    }

    @Override
    public String toString() {
        return "XliffSpan{id=" + id + ", example=" + example
                + ", placeholder=" + placeholder + "}";
    }
}
