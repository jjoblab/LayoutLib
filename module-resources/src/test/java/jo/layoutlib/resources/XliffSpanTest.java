package jo.layoutlib.resources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests des objets valeur {@link XliffSpan} et {@link XliffText} (v3.3).
 *
 * <p>Valide la capture des segments {@code <xliff:g id/example>} : nullabilité
 * des attributs, trimming du placeholder, immuabilité, et surtout la
 * substitution des exemples (remplacement littéral, placeholders sans
 * exemple intacts, exemples contenant des caractères de format jamais
 * réinterprétés).</p>
 *
 * @author jo@Dev
 * @since 3.3
 */
@DisplayName("XliffSpan / XliffText — segments <xliff:g>")
class XliffSpanTest {

    @Test
    @DisplayName("un segment porte id, example et placeholder")
    void shouldExposeIdExampleAndPlaceholder() {
        XliffSpan span = new XliffSpan("artist", "Adèle", "%2$s");
        assertThat(span.getId()).isEqualTo("artist");
        assertThat(span.getExample()).isEqualTo("Adèle");
        assertThat(span.getPlaceholder()).isEqualTo("%2$s");
        assertThat(span.hasExample()).isTrue();
    }

    @Test
    @DisplayName("id et example absents → null, sans example")
    void shouldTolerateMissingAttributes() {
        XliffSpan span = new XliffSpan(null, null, "%d");
        assertThat(span.getId()).isNull();
        assertThat(span.getExample()).isNull();
        assertThat(span.getPlaceholder()).isEqualTo("%d");
        assertThat(span.hasExample()).isFalse();
    }

    @Test
    @DisplayName("le placeholder est trimmé et un null devient vide")
    void shouldTrimPlaceholderAndNullify() {
        assertThat(new XliffSpan("a", "b", "  %1$d  ").getPlaceholder())
                .isEqualTo("%1$d");
        assertThat(new XliffSpan("a", "b", null).getPlaceholder()).isEmpty();
    }

    @Test
    @DisplayName("equals/hashCode : deux captures identiques sont égales")
    void shouldImplementValueEquality() {
        XliffSpan a = new XliffSpan("artist", "Adèle", "%2$s");
        XliffSpan b = new XliffSpan("artist", "Adèle", "%2$s");
        XliffSpan c = new XliffSpan("artist", null, "%2$s");
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("artist");
    }

    @Test
    @DisplayName("toString décrit le segment (introspection)")
    void shouldDescribeItself() {
        String text = new XliffSpan("artist", "Adèle", "%2$s").toString();
        assertThat(text).contains("artist").contains("Adèle").contains("%2$s");
    }

    @Test
    @DisplayName("XliffText : texte brut + spans, liste non modifiable")
    void shouldBuildImmutableRichText() {
        java.util.List<XliffSpan> spans =
                java.util.Arrays.asList(new XliffSpan("a", "1", "%1$d"));
        XliffText rich = new XliffText("%1$d chansons", spans);
        assertThat(rich.getRaw()).isEqualTo("%1$d chansons");
        assertThat(rich.getSpans()).hasSize(1);
        assertThat(rich.hasSpans()).isTrue();
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> rich.getSpans().clear());
    }

    @Test
    @DisplayName("XliffText : constructeur tolérant (nulls)")
    void shouldTolerateNullsInConstructor() {
        XliffText empty = new XliffText(null, null);
        assertThat(empty.getRaw()).isEmpty();
        assertThat(empty.getSpans()).isEmpty();
        assertThat(empty.hasSpans()).isFalse();
        assertThat(empty.applyExamples()).isEmpty();
    }

    @Test
    @DisplayName("applyExamples : substitution littérale du placeholder")
    void shouldSubstituteExampleLiterally() {
        java.util.List<XliffSpan> spans = java.util.Arrays.asList(
                new XliffSpan("artist", "Adèle", "%2$s"));
        assertThat(XliffText.applyExamples("%1$d chansons — %2$s", spans))
                .isEqualTo("%1$d chansons — Adèle");
    }

    @Test
    @DisplayName("applyExamples : plusieurs spans appliqués en cascade")
    void shouldApplySeveralSpans() {
        java.util.List<XliffSpan> spans = java.util.Arrays.asList(
                new XliffSpan("count", "7", "%1$d"),
                new XliffSpan("artist", "Adèle", "%2$s"));
        assertThat(XliffText.applyExamples("%1$d chansons — %2$s", spans))
                .isEqualTo("7 chansons — Adèle");
    }

    @Test
    @DisplayName("applyExamples : span sans example → placeholder intact")
    void shouldKeepPlaceholderWithoutExample() {
        java.util.List<XliffSpan> spans = java.util.Arrays.asList(
                new XliffSpan("count", null, "%1$d"),
                new XliffSpan("artist", "Adèle", "%2$s"));
        assertThat(XliffText.applyExamples("%1$d chansons — %2$s", spans))
                .isEqualTo("%1$d chansons — Adèle");
    }

    @Test
    @DisplayName("applyExamples : example vide → placeholder intact")
    void shouldIgnoreEmptyExample() {
        java.util.List<XliffSpan> spans =
                java.util.Arrays.asList(new XliffSpan("count", "", "%1$d"));
        assertThat(XliffText.applyExamples("%1$d chansons", spans))
                .isEqualTo("%1$d chansons");
    }

    @Test
    @DisplayName("applyExamples : un example contenant % ou $ n'est jamais réinterprété")
    void shouldNotReinterpretFormatCharactersInExample() {
        java.util.List<XliffSpan> spans =
                java.util.Arrays.asList(new XliffSpan("pct", "100%s $1", "%d"));
        assertThat(XliffText.applyExamples(" soldes : %d", spans))
                .isEqualTo(" soldes : 100%s $1");
    }

    @Test
    @DisplayName("applyExamples : placeholder absent du texte → no-op silencieux")
    void shouldIgnoreMissingPlaceholder() {
        java.util.List<XliffSpan> spans =
                java.util.Arrays.asList(new XliffSpan("count", "7", "%1$d"));
        assertThat(XliffText.applyExamples("aucun placeholder ici", spans))
                .isEqualTo("aucun placeholder ici");
        assertThat(XliffText.applyExamples(null, spans)).isNull();
        assertThat(XliffText.applyExamples("texte", null)).isEqualTo("texte");
        assertThat(XliffText.applyExamples("texte", java.util.Collections.emptyList()))
                .isEqualTo("texte");
    }

    @Test
    @DisplayName("applyExamples : chaque occurrence du placeholder est substituée")
    void shouldReplaceEveryOccurrence() {
        java.util.List<XliffSpan> spans =
                java.util.Arrays.asList(new XliffSpan("count", "7", "%d"));
        assertThat(XliffText.applyExamples("%d et %d", spans)).isEqualTo("7 et 7");
    }

    @Test
    @DisplayName("applyExamples d'instance : même résultat que la statique")
    void shouldApplyInstanceExamples() {
        XliffText rich = new XliffText("%1$d chansons — %2$s",
                java.util.Arrays.asList(
                        new XliffSpan("count", "7", "%1$d"),
                        new XliffSpan("artist", "Adèle", "%2$s")));
        assertThat(rich.applyExamples()).isEqualTo("7 chansons — Adèle");
    }
}
