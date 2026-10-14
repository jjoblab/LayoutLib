package jo.layoutlib.resources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests des règles de pluriel CLDR ({@link PluralRules}).
 *
 * <p>Valide la sélection de catégorie pour les langues courantes —
 * quantités entières uniquement, sémantique UTS #35 — ainsi que le repli
 * par défaut (langue inconnue ou {@code null} : règle angloïde) et le
 * traitement des quantités négatives (valeur absolue, comme CLDR).</p>
 *
 * @author jo@Dev
 * @since 3.2
 */
@DisplayName("PluralRules — règles de pluriel CLDR")
class PluralRulesTest {

    @Nested
    @DisplayName("Règle par défaut (langue inconnue ou null)")
    class DefaultRule {

        @Test
        @DisplayName("langue null : one pour 1, other sinon")
        void shouldUseAngloidRuleForNullLanguage() {
            assertThat(PluralRules.select(null, 1)).isEqualTo("one");
            assertThat(PluralRules.select(null, 0)).isEqualTo("other");
            assertThat(PluralRules.select(null, 2)).isEqualTo("other");
            assertThat(PluralRules.select(null, 42)).isEqualTo("other");
        }

        @Test
        @DisplayName("langue inconnue : repli angloïde")
        void shouldFallBackForUnknownLanguage() {
            assertThat(PluralRules.select("xx", 1)).isEqualTo("one");
            assertThat(PluralRules.select("xx", 2)).isEqualTo("other");
        }

        @Test
        @DisplayName("langue vide : repli angloïde")
        void shouldFallBackForEmptyLanguage() {
            assertThat(PluralRules.select("", 1)).isEqualTo("one");
            assertThat(PluralRules.select("  ", 5)).isEqualTo("other");
        }

        @Test
        @DisplayName("langue en majuscules ou avec espaces : normalisée")
        void shouldNormalizeLanguageCase() {
            assertThat(PluralRules.select("FR", 0)).isEqualTo("one");
            assertThat(PluralRules.select(" fr ", 2)).isEqualTo("other");
            assertThat(PluralRules.select("RU", 21)).isEqualTo("one");
        }
    }

    @Nested
    @DisplayName("Français (fr)")
    class FrenchRule {

        @Test
        @DisplayName("0 et 1 sont singuliers (one)")
        void shouldTreatZeroAndOneAsSingular() {
            assertThat(PluralRules.select("fr", 0)).isEqualTo("one");
            assertThat(PluralRules.select("fr", 1)).isEqualTo("one");
        }

        @Test
        @DisplayName("2 et plus : other")
        void shouldTreatTwoAndMoreAsOther() {
            assertThat(PluralRules.select("fr", 2)).isEqualTo("other");
            assertThat(PluralRules.select("fr", 42)).isEqualTo("other");
            assertThat(PluralRules.select("fr", 999999)).isEqualTo("other");
        }

        @Test
        @DisplayName("multiples de un million : many (2 millions, 3 millions)")
        void shouldTreatExactMillionsAsMany() {
            assertThat(PluralRules.select("fr", 1_000_000)).isEqualTo("many");
            assertThat(PluralRules.select("fr", 2_000_000)).isEqualTo("many");
            assertThat(PluralRules.select("fr", 3_000_000)).isEqualTo("many");
        }
    }

    @Nested
    @DisplayName("Angloïde (en, de, es, it, nl, pt…)")
    class AngloidRule {

        @Test
        @DisplayName("en/de/es/it : one pour 1 seulement")
        void shouldSelectOneOnlyForExactlyOne() {
            for (String lang : new String[]{"en", "de", "es", "it", "nl"}) {
                assertThat(PluralRules.select(lang, 1)).isEqualTo("one");
                assertThat(PluralRules.select(lang, 0)).isEqualTo("other");
                assertThat(PluralRules.select(lang, 2)).isEqualTo("other");
            }
        }

        @Test
        @DisplayName("pt : 0 et 1 sont singuliers (comme fr, sans many)")
        void shouldTreatPortugueseZeroAndOneAsSingular() {
            assertThat(PluralRules.select("pt", 0)).isEqualTo("one");
            assertThat(PluralRules.select("pt", 1)).isEqualTo("one");
            assertThat(PluralRules.select("pt", 1_000_000)).isEqualTo("other");
        }
    }

    @Nested
    @DisplayName("Slave oriental (ru, uk, sr, hr)")
    class EastSlavicRule {

        @Test
        @DisplayName("ru : one pour …1 sauf …11 (21, 31, 101)")
        void shouldSelectOneForEndingInOneExceptEleven() {
            assertThat(PluralRules.select("ru", 1)).isEqualTo("one");
            assertThat(PluralRules.select("ru", 21)).isEqualTo("one");
            assertThat(PluralRules.select("ru", 31)).isEqualTo("one");
            assertThat(PluralRules.select("ru", 101)).isEqualTo("one");
        }

        @Test
        @DisplayName("ru : few pour …2..4 sauf …12..14")
        void shouldSelectFewForTwoToFourExceptTeens() {
            assertThat(PluralRules.select("ru", 2)).isEqualTo("few");
            assertThat(PluralRules.select("ru", 4)).isEqualTo("few");
            assertThat(PluralRules.select("ru", 22)).isEqualTo("few");
            assertThat(PluralRules.select("ru", 103)).isEqualTo("few");
        }

        @Test
        @DisplayName("ru : many pour 0, 5..9, 11..14, 100")
        void shouldSelectManyForZeroFiveToNineAndTeens() {
            assertThat(PluralRules.select("ru", 0)).isEqualTo("many");
            assertThat(PluralRules.select("ru", 5)).isEqualTo("many");
            assertThat(PluralRules.select("ru", 11)).isEqualTo("many");
            assertThat(PluralRules.select("ru", 14)).isEqualTo("many");
            assertThat(PluralRules.select("ru", 100)).isEqualTo("many");
        }

        @Test
        @DisplayName("uk et sr partagent la règle russe")
        void shouldShareRuleAcrossSlavicLanguages() {
            assertThat(PluralRules.select("uk", 21)).isEqualTo("one");
            assertThat(PluralRules.select("uk", 11)).isEqualTo("many");
            assertThat(PluralRules.select("sr", 2)).isEqualTo("few");
            assertThat(PluralRules.select("hr", 5)).isEqualTo("many");
        }
    }

    @Nested
    @DisplayName("Tchèque/slovaque (cs, sk) et polonais (pl)")
    class CzechAndPolishRule {

        @Test
        @DisplayName("cs : one=1, few=2..4, other=0,5,11…")
        void shouldFollowCzechRule() {
            assertThat(PluralRules.select("cs", 1)).isEqualTo("one");
            assertThat(PluralRules.select("cs", 2)).isEqualTo("few");
            assertThat(PluralRules.select("cs", 4)).isEqualTo("few");
            assertThat(PluralRules.select("cs", 0)).isEqualTo("other");
            assertThat(PluralRules.select("cs", 5)).isEqualTo("other");
        }

        @Test
        @DisplayName("pl : one=1, few=…2..4 sauf 12..14, many=reste")
        void shouldFollowPolishRule() {
            assertThat(PluralRules.select("pl", 1)).isEqualTo("one");
            assertThat(PluralRules.select("pl", 2)).isEqualTo("few");
            assertThat(PluralRules.select("pl", 22)).isEqualTo("few");
            assertThat(PluralRules.select("pl", 12)).isEqualTo("many");
            assertThat(PluralRules.select("pl", 0)).isEqualTo("many");
            assertThat(PluralRules.select("pl", 5)).isEqualTo("many");
            assertThat(PluralRules.select("pl", 112)).isEqualTo("many");
        }
    }

    @Nested
    @DisplayName("Arabe (ar)")
    class ArabicRule {

        @Test
        @DisplayName("ar : six catégories — zero, one, two, few, many, other")
        void shouldFollowArabicRule() {
            assertThat(PluralRules.select("ar", 0)).isEqualTo("zero");
            assertThat(PluralRules.select("ar", 1)).isEqualTo("one");
            assertThat(PluralRules.select("ar", 2)).isEqualTo("two");
            assertThat(PluralRules.select("ar", 3)).isEqualTo("few");
            assertThat(PluralRules.select("ar", 10)).isEqualTo("few");
            assertThat(PluralRules.select("ar", 11)).isEqualTo("many");
            assertThat(PluralRules.select("ar", 99)).isEqualTo("many");
            assertThat(PluralRules.select("ar", 100)).isEqualTo("other");
            assertThat(PluralRules.select("ar", 103)).isEqualTo("few");
        }
    }

    @Nested
    @DisplayName("Langues asiatiques et autres")
    class OtherLanguages {

        @Test
        @DisplayName("zh/ja/ko/vi/th/id : toujours other (pas de nombre)")
        void shouldAlwaysSelectOtherForAsianLanguages() {
            for (String lang : new String[]{"zh", "ja", "ko", "vi", "th", "id"}) {
                assertThat(PluralRules.select(lang, 0)).isEqualTo("other");
                assertThat(PluralRules.select(lang, 1)).isEqualTo("other");
                assertThat(PluralRules.select(lang, 2)).isEqualTo("other");
            }
        }

        @Test
        @DisplayName("he : one=1, two=2, other=reste")
        void shouldFollowHebrewRule() {
            assertThat(PluralRules.select("he", 1)).isEqualTo("one");
            assertThat(PluralRules.select("he", 2)).isEqualTo("two");
            assertThat(PluralRules.select("he", 3)).isEqualTo("other");
        }

        @Test
        @DisplayName("lv : zero pour multiples de 10 et 11..19")
        void shouldFollowLatvianRule() {
            assertThat(PluralRules.select("lv", 0)).isEqualTo("zero");
            assertThat(PluralRules.select("lv", 10)).isEqualTo("zero");
            assertThat(PluralRules.select("lv", 11)).isEqualTo("zero");
            assertThat(PluralRules.select("lv", 19)).isEqualTo("zero");
            assertThat(PluralRules.select("lv", 1)).isEqualTo("one");
            assertThat(PluralRules.select("lv", 21)).isEqualTo("one");
            assertThat(PluralRules.select("lv", 2)).isEqualTo("other");
        }

        @Test
        @DisplayName("lt : one=…1 sauf …11, few=…2..9 sauf 10..20")
        void shouldFollowLithuanianRule() {
            assertThat(PluralRules.select("lt", 1)).isEqualTo("one");
            assertThat(PluralRules.select("lt", 21)).isEqualTo("one");
            assertThat(PluralRules.select("lt", 11)).isNotEqualTo("one");
            assertThat(PluralRules.select("lt", 2)).isEqualTo("few");
            assertThat(PluralRules.select("lt", 9)).isEqualTo("few");
            assertThat(PluralRules.select("lt", 10)).isEqualTo("other");
            assertThat(PluralRules.select("lt", 15)).isEqualTo("other");
        }
    }

    @Nested
    @DisplayName("Quantités négatives")
    class NegativeQuantities {

        @Test
        @DisplayName("valeur absolue appliquée (comme CLDR n)")
        void shouldUseAbsoluteValue() {
            assertThat(PluralRules.select("en", -1)).isEqualTo("one");
            assertThat(PluralRules.select("fr", -1)).isEqualTo("one");
            assertThat(PluralRules.select("en", -2)).isEqualTo("other");
            assertThat(PluralRules.select("ru", -21)).isEqualTo("one");
        }
    }
}
