package jo.layoutlib.inflater;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * v3.6.0 — non-régression « preview de listes » du préprocesseur (étude
 * PE14 § 2.4-3, S6 du plan) : les attributs
 * {@code tools:listitem/itemCount/listheader/listfooter} doivent
 * SURVIVRE à la normalisation (lus par
 * {@code BridgeInflater.brancherApercuListe} via l'URI tools), et la
 * déclaration {@code xmlns:tools} doit rester avec eux — sans quoi le
 * parser namespace-aware lève {@code Undefined Prefix: tools}.
 *
 * <p>Avant v3.6.0, TOUS les attributs {@code tools:*} partaient dès
 * {@code v3.4.5} : une ListView ne pouvait JAMAIS être prévisualisée
 * remplie depuis {@code tools:listitem}.</p>
 *
 * @author jo@Dev
 * @since 3.6.0
 */
@DisplayName("XmlPreprocessor — survie des attributs tools:list* (v3.6.0, S6)")
class XmlPreprocessorListPreviewTest {

    /** Un ListView de test, complet des deux espaces de noms. */
    private static final String LISTE_OUTILS =
            "<ListView xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
            + "    android:layout_width=\"match_parent\"\n"
            + "    android:layout_height=\"wrap_content\"\n"
            + "    tools:listitem=\"@layout/item_ticket\"\n"
            + "    tools:itemCount=\"4\"\n"
            + "    tools:listheader=\"@layout/entete\"\n"
            + "    tools:listfooter=\"@layout/pied\"\n"
            + "    tools:context=\".MainActivity\" />";

    @Nested
    @DisplayName("Survie des attributs de preview de listes")
    class SurvieListes {

        @Test
        @DisplayName("tools:listitem/itemCount/listheader/listfooter survivent")
        void attributsListesSurvivent() {
            String resultat = XmlPreprocessor.preprocess(LISTE_OUTILS);
            assertThat(resultat).contains("tools:listitem=\"@layout/item_ticket\"");
            assertThat(resultat).contains("tools:itemCount=\"4\"");
            assertThat(resultat).contains("tools:listheader=\"@layout/entete\"");
            assertThat(resultat).contains("tools:listfooter=\"@layout/pied\"");
        }

        @Test
        @DisplayName("xmlns:tools est conservé quand des tools:list* restent")
        void xmlnsConservé() {
            String resultat = XmlPreprocessor.preprocess(LISTE_OUTILS);
            assertThat(resultat).contains("xmlns:tools=\"http://schemas.android.com/tools\"");
        }

        @Test
        @DisplayName("les attributs tools: hors listes continuent de partir")
        void autresToolsStrippes() {
            String resultat = XmlPreprocessor.preprocess(LISTE_OUTILS);
            assertThat(resultat).doesNotContain("tools:context");
        }

        @Test
        @DisplayName("le XML normalisé reste bien formé (parse complet)")
        void xmlParsable() {
            String resultat = XmlPreprocessor.preprocess(LISTE_OUTILS);
            org.assertj.core.api.Assertions.assertThatCode(
                    () -> XmlPreprocessor.validateWellFormed(resultat))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Comportement inchangé sans attributs tools:list*")
    class SansListes {

        @Test
        @DisplayName("xmlns:tools part quand plus AUCUN tools: ne reste")
        void xmlnsPartToujoursHorsListes() {
            String xml = "<FrameLayout\n"
                    + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                    + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                    + "    android:layout_width=\"match_parent\"\n"
                    + "    android:layout_height=\"wrap_content\"\n"
                    + "    tools:context=\".MainActivity\" />";
            String resultat = XmlPreprocessor.preprocess(xml);
            assertThat(resultat).doesNotContain("xmlns:tools");
            assertThat(resultat).doesNotContain("tools:");
        }

        @Test
        @DisplayName("tools:text se convertit toujours en android:text")
        void conversionInchangee() {
            String xml = "<TextView xmlns:tools=\"http://schemas.android.com/tools\"\n"
                    + "    android:layout_width=\"wrap_content\"\n"
                    + "    android:layout_height=\"wrap_content\"\n"
                    + "    tools:text=\"Aperçu\" />";
            String resultat = XmlPreprocessor.preprocess(xml);
            assertThat(resultat).contains("android:text=\"Aperçu\"");
            assertThat(resultat).doesNotContain("tools:");
        }

        @Test
        @DisplayName("un attribut tools:listitemSuffix (préfixe ressemblant) part bien")
        void suffixeRessemblantPart() {
            String xml = "<ListView xmlns:tools=\"http://schemas.android.com/tools\"\n"
                    + "    tools:listitemSuffix=\"x\" />";
            String resultat = XmlPreprocessor.preprocess(xml);
            assertThat(resultat).doesNotContain("tools:listitemSuffix");
        }
    }
}
