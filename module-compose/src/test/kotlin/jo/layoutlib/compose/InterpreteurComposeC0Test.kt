package jo.layoutlib.compose

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * v3.7.0 — S9 (étude § 5.4) : l'INTERPRÉTEUR d'une `@Composable` triviale
 * — PUR (chaînes), testé sans Android. Le contrat X4 : sous-rapporter
 * plutôt que deviner (annotée sans `fun`, corps vide, `Text` à variable —
 * rien), les littéraux dans l'ordre du document.
 *
 * @author jo@Dev
 * @since 3.7.0
 */
@DisplayName("InterpreteurComposeC0 — la @Composable triviale (S9, C0)")
class InterpreteurComposeC0Test {
    @Test
    @DisplayName("la forme triviale — nom + littéraux dans l'ordre")
    fun shouldReadTheTrivialShape() {
        val source =
            """
            import androidx.compose.runtime.Composable

            @Composable
            fun BonjourCompose() {
                Text("Premier")
                Text("Deuxième")
            }
            """.trimIndent()
        val interpretee = InterpreteurComposeC0.interpreter(source)
        assertThat(interpretee).isNotNull
        assertThat(interpretee!!.nom).isEqualTo("BonjourCompose")
        assertThat(interpretee.textes).containsExactly("Premier", "Deuxième")
    }

    @Test
    @DisplayName("paramètres et annotations — le nom se lit quand même")
    fun shouldReadThroughParameters() {
        val source =
            """
            @Preview
            @Composable
            fun Ecran(nom: String = "x") {
                Text("Contenu")
            }
            """.trimIndent()
        val interpretee = InterpreteurComposeC0.interpreter(source)
        assertThat(interpretee!!.nom).isEqualTo("Ecran")
        assertThat(interpretee.textes).containsExactly("Contenu")
    }

    @Test
    @DisplayName("accolades imbriquées, littéraux pièges — le corps s'arrête au bon }")
    fun shouldMatchNestedBraces() {
        val source =
            """
            @Composable
            fun Imbriquee() {
                if (true) {
                    Text("Dans le if")
                }
                Column {
                    Text("Dans la colonne")
                }
            }
            """.trimIndent()
        val interpretee = InterpreteurComposeC0.interpreter(source)
        assertThat(interpretee!!.textes).containsExactly("Dans le if", "Dans la colonne")
    }

    @Test
    @DisplayName("Text à variable — SOUS-RAPPORTÉ (jamais deviné)")
    fun shouldSkipVariableText() {
        val source =
            """
            @Composable
            fun AvecVariable(saisie: String) {
                Text(saisie)
                Text("Littéral gardé")
            }
            """.trimIndent()
        val interpretee = InterpreteurComposeC0.interpreter(source)
        assertThat(interpretee!!.textes).containsExactly("Littéral gardé")
    }

    @Test
    @DisplayName("sans @Composable — rien")
    fun shouldReportNothingWithoutAnnotation() {
        assertThat(InterpreteurComposeC0.interpreter("fun Simple() { Text(\"x\") }")).isNull()
    }

    @Test
    @DisplayName("sans fun ni corps — rien")
    fun shouldReportNothingWithoutFun() {
        assertThat(InterpreteurComposeC0.interpreter("@Composable\nval constante = 3")).isNull()
        assertThat(InterpreteurComposeC0.interpreter("@Composable\nfun Orpheline()")).isNull()
    }

    @Test
    @DisplayName("sans Text littéral — rien (le C0 ne compose pas du vide)")
    fun shouldReportNothingWithoutTexts() {
        assertThat(InterpreteurComposeC0.interpreter("@Composable\nfun Vide() { val x = 1 }")).isNull()
    }

    @Test
    @DisplayName("guillemets échappés — le littéral les traverse")
    fun shouldReadEscapedLiterals() {
        val source = "@Composable fun Echappee() { Text(\"Bonjour \\\"le monde\\\"\") }"
        val interpretee = InterpreteurComposeC0.interpreter(source)
        assertThat(interpretee!!.textes).containsExactly("Bonjour \\\"le monde\\\"")
    }

    @Test
    @DisplayName("corps jamais fermé (frappe en cours) — rien")
    fun shouldReportNothingWhileTyping() {
        assertThat(InterpreteurComposeC0.interpreter("@Composable fun EnCours() { Text(")).isNull()
    }
}
