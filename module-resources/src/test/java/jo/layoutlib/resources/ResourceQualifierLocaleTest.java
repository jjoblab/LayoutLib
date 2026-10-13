package jo.layoutlib.resources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires du {@link ResourceQualifier} — dimension langue/région
 * (ADR 0116 — P1/L2).
 *
 * <p>Valide le parsing des dossiers {@code values-<lang>/}
 * ({@code values-fr}, {@code values-en}, {@code values-fr-rFR}), la
 * sémantique de compatibilité (une resource localisée ne sert que sa
 * locale ; une resource sans langue sert de repli), et la priorité AOSP
 * (locale &gt; mode nuit &gt; orientation &gt; API).</p>
 *
 * @author jo@Dev
 * @since 3.0
 */
@DisplayName("ResourceQualifier — qualifiers de langue (P1/L2)")
class ResourceQualifierLocaleTest {

    @Nested
    @DisplayName("Parsing des noms de dossiers localisés")
    class LocaleParsing {

        @Test
        @DisplayName("parse 'values-fr' en qualifier langue fr")
        void shouldParseFrenchFolder() {
            ResourceQualifier q = ResourceQualifier.parse("values-fr");
            assertThat(q.getLanguage()).isEqualTo("fr");
            assertThat(q.hasLanguage()).isTrue();
            assertThat(q.getRegion()).isNull();
        }

        @Test
        @DisplayName("parse 'values-en' en qualifier langue en")
        void shouldParseEnglishFolder() {
            ResourceQualifier q = ResourceQualifier.parse("values-en");
            assertThat(q.getLanguage()).isEqualTo("en");
        }

        @Test
        @DisplayName("parse 'values-fr-rFR' en langue fr + région FR")
        void shouldParseLanguageWithRegion() {
            ResourceQualifier q = ResourceQualifier.parse("values-fr-rFR");
            assertThat(q.getLanguage()).isEqualTo("fr");
            assertThat(q.getRegion()).isEqualTo("FR");
        }

        @Test
        @DisplayName("parse 'values-en-rUS' en langue en + région US")
        void shouldParseEnglishWithRegion() {
            ResourceQualifier q = ResourceQualifier.parse("values-en-rUS");
            assertThat(q.getLanguage()).isEqualTo("en");
            assertThat(q.getRegion()).isEqualTo("US");
        }

        @Test
        @DisplayName("parse 'values-fr-night-v31' (langue combinée aux autres qualifiers)")
        void shouldParseLanguageCombinedWithOthers() {
            ResourceQualifier q = ResourceQualifier.parse("values-fr-night-v31");
            assertThat(q.getLanguage()).isEqualTo("fr");
            assertThat(q.isNight()).isTrue();
            assertThat(q.getApiLevel()).isEqualTo(31);
        }

        @Test
        @DisplayName("parse 'values-night-fr' dans n'importe quel ordre")
        void shouldParseLanguageInAnyOrder() {
            ResourceQualifier q = ResourceQualifier.parse("values-night-fr");
            assertThat(q.getLanguage()).isEqualTo("fr");
            assertThat(q.isNight()).isTrue();
        }

        @Test
        @DisplayName("parse 'values-b+fr' (BCP47, toléré)")
        void shouldParseBcp47Language() {
            ResourceQualifier q = ResourceQualifier.parse("values-b+fr");
            assertThat(q.getLanguage()).isEqualTo("fr");
        }

        @Test
        @DisplayName("'values' reste sans langue")
        void defaultFolderHasNoLanguage() {
            ResourceQualifier q = ResourceQualifier.parse("values");
            assertThat(q.hasLanguage()).isFalse();
            assertThat(q.getLanguage()).isNull();
        }

        @Test
        @DisplayName("les qualifiers de densité ne sont pas pris pour des langues")
        void densityQualifierIsNotALanguage() {
            // hdpi = 4 lettres → pas une langue ; sw600dp → ignoré
            ResourceQualifier q = ResourceQualifier.parse("values-hdpi-sw600dp");
            assertThat(q.hasLanguage()).isFalse();
        }

        @Test
        @DisplayName("'car' (UI-mode voiture) n'est pas une langue")
        void carIsNotALanguage() {
            ResourceQualifier q = ResourceQualifier.parse("values-car");
            assertThat(q.hasLanguage()).isFalse();
        }

        @Test
        @DisplayName("normalise la casse de la langue et de la région")
        void normalizesCase() {
            ResourceQualifier q = new ResourceQualifier(false, false, 1, "FR", "fr");
            assertThat(q.getLanguage()).isEqualTo("fr");
            assertThat(q.getRegion()).isEqualTo("FR");
        }

        @Test
        @DisplayName("forLanguage crée un qualifier de locale")
        void forLanguageFactory() {
            ResourceQualifier q = ResourceQualifier.forLanguage("fr");
            assertThat(q.getLanguage()).isEqualTo("fr");
            assertThat(q.isNight()).isFalse();
            assertThat(q.getApiLevel()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Compatibilité (sémantique de repli Android)")
    class LocaleCompatibility {

        @Test
        @DisplayName("une resource fr est compatible avec une cible fr")
        void frenchResourceMatchesFrenchTarget() {
            ResourceQualifier fr = ResourceQualifier.parse("values-fr");
            assertThat(fr.isCompatibleWith(ResourceQualifier.forLanguage("fr"))).isTrue();
        }

        @Test
        @DisplayName("une resource fr n'est PAS compatible avec une cible en")
        void frenchResourceDoesNotMatchEnglishTarget() {
            ResourceQualifier fr = ResourceQualifier.parse("values-fr");
            assertThat(fr.isCompatibleWith(ResourceQualifier.forLanguage("en"))).isFalse();
        }

        @Test
        @DisplayName("une resource fr n'est PAS compatible avec une cible sans locale")
        void frenchResourceDoesNotMatchDefaultTarget() {
            ResourceQualifier fr = ResourceQualifier.parse("values-fr");
            assertThat(fr.isCompatibleWith(ResourceQualifier.DEFAULT)).isFalse();
        }

        @Test
        @DisplayName("une resource sans langue est compatible avec toute locale (repli)")
        void defaultResourceMatchesAnyLocale() {
            ResourceQualifier def = ResourceQualifier.parse("values");
            assertThat(def.isCompatibleWith(ResourceQualifier.forLanguage("fr"))).isTrue();
            assertThat(def.isCompatibleWith(ResourceQualifier.forLanguage("en"))).isTrue();
        }

        @Test
        @DisplayName("une resource fr-rFR exige une cible région FR")
        void regionResourceRequiresRegionTarget() {
            ResourceQualifier frFr = ResourceQualifier.parse("values-fr-rFR");
            assertThat(frFr.isCompatibleWith(
                    ResourceQualifier.forLanguage("fr", "FR"))).isTrue();
            assertThat(frFr.isCompatibleWith(
                    ResourceQualifier.forLanguage("fr"))).isFalse();
            assertThat(frFr.isCompatibleWith(
                    ResourceQualifier.forLanguage("fr", "BE"))).isFalse();
        }

        @Test
        @DisplayName("une resource fr (sans région) reste compatible avec fr-FR")
        void languageResourceMatchesRegionalTarget() {
            ResourceQualifier fr = ResourceQualifier.parse("values-fr");
            assertThat(fr.isCompatibleWith(
                    ResourceQualifier.forLanguage("fr", "FR"))).isTrue();
        }
    }

    @Nested
    @DisplayName("Score de spécificité (priorité AOSP)")
    class LocaleSpecificity {

        @Test
        @DisplayName("fr bat DEFAULT sur une cible fr")
        void frenchBeatsDefault() {
            ResourceQualifier fr = ResourceQualifier.parse("values-fr");
            ResourceQualifier def = ResourceQualifier.parse("values");
            ResourceQualifier target = ResourceQualifier.forLanguage("fr");
            assertThat(fr.specificityScore(target))
                    .isGreaterThan(def.specificityScore(target));
        }

        @Test
        @DisplayName("fr-rFR bat fr sur une cible fr-FR")
        void regionBeatsLanguageOnly() {
            ResourceQualifier frFr = ResourceQualifier.parse("values-fr-rFR");
            ResourceQualifier fr = ResourceQualifier.parse("values-fr");
            ResourceQualifier target = ResourceQualifier.forLanguage("fr", "FR");
            assertThat(frFr.specificityScore(target))
                    .isGreaterThan(fr.specificityScore(target));
        }

        @Test
        @DisplayName("la langue domine le mode nuit (ordre AOSP)")
        void languageDominatesNight() {
            ResourceQualifier frDay = ResourceQualifier.parse("values-fr");
            ResourceQualifier defNight = ResourceQualifier.parse("values-night");
            ResourceQualifier target = new ResourceQualifier(
                    true, false, 1, "fr", null);
            assertThat(frDay.specificityScore(target))
                    .isGreaterThan(defNight.specificityScore(target));
        }

        @Test
        @DisplayName("fr-night bat fr et night sur une cible fr nuit")
        void combinedQualifierBeatsSingles() {
            ResourceQualifier frNight = ResourceQualifier.parse("values-fr-night");
            ResourceQualifier fr = ResourceQualifier.parse("values-fr");
            ResourceQualifier night = ResourceQualifier.parse("values-night");
            ResourceQualifier target = new ResourceQualifier(true, false, 1, "fr", null);
            assertThat(frNight.specificityScore(target))
                    .isGreaterThan(fr.specificityScore(target));
            assertThat(frNight.specificityScore(target))
                    .isGreaterThan(night.specificityScore(target));
        }
    }

    @Nested
    @DisplayName("Égalité et toString")
    class EqualityAndToString {

        @Test
        @DisplayName("deux qualifiers fr sont égaux")
        void frenchQualifiersAreEqual() {
            assertThat(ResourceQualifier.parse("values-fr"))
                    .isEqualTo(ResourceQualifier.parse("values-fr"));
        }

        @Test
        @DisplayName("fr et en ne sont pas égaux")
        void frenchAndEnglishAreNotEqual() {
            assertThat(ResourceQualifier.parse("values-fr"))
                    .isNotEqualTo(ResourceQualifier.parse("values-en"));
        }

        @Test
        @DisplayName("toString reproduit le nom du dossier localisé")
        void toStringIncludesLanguage() {
            assertThat(ResourceQualifier.parse("values-fr").toString())
                    .isEqualTo("values-fr");
            assertThat(ResourceQualifier.parse("values-fr-rFR").toString())
                    .isEqualTo("values-fr-rFR");
            assertThat(ResourceQualifier.parse("values-fr-night-v31").toString())
                    .isEqualTo("values-fr-night-v31");
        }

        @Test
        @DisplayName("l'ordre de compareTo place les locales d'abord")
        void compareToOrdersLocalesFirst() {
            ResourceQualifier fr = ResourceQualifier.parse("values-fr");
            ResourceQualifier def = ResourceQualifier.parse("values");
            ResourceQualifier night = ResourceQualifier.parse("values-night");
            assertThat(fr.compareTo(night)).isLessThan(0);
            assertThat(night.compareTo(def)).isLessThan(0);
        }
    }
}
