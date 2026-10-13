package jo.layoutlib.drawables;

import android.view.Gravity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du {@link BitmapParser} (ADR 0116 — P0/L3).
 *
 * <p>Valide le parsing des {@code <bitmap>} : source, modes de tuilage,
 * gravité (constantes {@link Gravity} réelles en JVM — inlining
 * compile-time), drapeaux de qualité, alpha et tint.</p>
 *
 * @author jo@Dev
 * @since 3.0
 */
@DisplayName("BitmapParser — parsing des <bitmap>")
class BitmapParserTest {

    private static final String NS =
            "xmlns:android=\"http://schemas.android.com/apk/res/android\"";

    private BitmapParser parser;

    @BeforeEach
    void setUp() {
        parser = new BitmapParser();
    }

    @Nested
    @DisplayName("Source")
    class Source {

        @Test
        @DisplayName("lit la référence android:src")
        void readsSrcReference() {
            BitmapConfig config = parser.parse(
                    "<bitmap " + NS + " android:src=\"@drawable/tuile\"/>");
            assertThat(config.hasSource()).isTrue();
            assertThat(config.getSrcRef()).isEqualTo("@drawable/tuile");
        }

        @Test
        @DisplayName("une source @color/ est conservée telle quelle")
        void readsColorSource() {
            BitmapConfig config = parser.parse(
                    "<bitmap " + NS + " android:src=\"@color/fond\"/>");
            assertThat(config.getSrcRef()).isEqualTo("@color/fond");
        }

        @Test
        @DisplayName("sans source, la config est vide")
        void noSourceMeansNoConfig() {
            BitmapConfig config = parser.parse("<bitmap " + NS + "/>");
            assertThat(config.hasSource()).isFalse();
            assertThat(config.getSrcRef()).isNull();
        }
    }

    @Nested
    @DisplayName("Tuilage")
    class Tiling {

        @Test
        @DisplayName("lit tileMode combiné")
        void readsCombinedTileMode() {
            BitmapConfig config = parser.parse(
                    "<bitmap " + NS + " android:src=\"@drawable/tuile\"\n"
                            + "       android:tileMode=\"repeat\"/>");
            assertThat(config.getTileMode()).isEqualTo("repeat");
            assertThat(config.getTileModeX()).isNull();
            assertThat(config.getTileModeY()).isNull();
        }

        @Test
        @DisplayName("lit tileModeX et tileModeY indépendants")
        void readsSeparateTileModes() {
            BitmapConfig config = parser.parse(
                    "<bitmap " + NS + " android:src=\"@drawable/tuile\"\n"
                            + "       android:tileModeX=\"mirror\"\n"
                            + "       android:tileModeY=\"clamp\"/>");
            assertThat(config.getTileMode()).isNull();
            assertThat(config.getTileModeX()).isEqualTo("mirror");
            assertThat(config.getTileModeY()).isEqualTo("clamp");
        }
    }

    @Nested
    @DisplayName("Gravité")
    class GravityParsing {

        @Test
        @DisplayName("parse une gravité simple en constante Gravity")
        void parsesSimpleGravity() {
            BitmapConfig config = parser.parse(
                    "<bitmap " + NS + " android:src=\"@drawable/tuile\"\n"
                            + "       android:gravity=\"center\"/>");
            assertThat(config.getGravity()).isEqualTo(Gravity.CENTER);
        }

        @Test
        @DisplayName("combine plusieurs jetons séparés par |")
        void combinesGravityFlags() {
            BitmapConfig config = parser.parse(
                    "<bitmap " + NS + " android:src=\"@drawable/tuile\"\n"
                            + "       android:gravity=\"center|fill_horizontal\"/>");
            assertThat(config.getGravity())
                    .isEqualTo(Gravity.CENTER | Gravity.FILL_HORIZONTAL);
        }

        @Test
        @DisplayName("gravité absente → null")
        void missingGravityIsNull() {
            BitmapConfig config = parser.parse(
                    "<bitmap " + NS + " android:src=\"@drawable/tuile\"/>");
            assertThat(config.getGravity()).isNull();
        }

        @Test
        @DisplayName("un jeton inconnu est ignoré dans la combinaison")
        void unknownTokenIsIgnored() {
            BitmapConfig config = parser.parse(
                    "<bitmap " + NS + " android:src=\"@drawable/tuile\"\n"
                            + "       android:gravity=\"top|pas_une_gravite\"/>");
            assertThat(config.getGravity()).isEqualTo(Gravity.TOP);
        }

        @Test
        @DisplayName("que des jetons inconnus → null")
        void onlyUnknownTokensIsNull() {
            assertThat(BitmapParser.parseGravity("pas_une_gravite")).isNull();
        }

        @Test
        @DisplayName("valeur null ou vide → null")
        void nullOrEmptyIsNull() {
            assertThat(BitmapParser.parseGravity(null)).isNull();
            assertThat(BitmapParser.parseGravity("   ")).isNull();
        }
    }

    @Nested
    @DisplayName("Qualité, alpha et tint")
    class QualityAlphaTint {

        @Test
        @DisplayName("lit les drapeaux de qualité")
        void readsQualityFlags() {
            BitmapConfig config = parser.parse(
                    "<bitmap " + NS + " android:src=\"@drawable/tuile\"\n"
                            + "       android:antialias=\"true\"\n"
                            + "       android:dither=\"false\"\n"
                            + "       android:filter=\"true\"/>");
            assertThat(config.getAntialias()).isTrue();
            assertThat(config.getDither()).isFalse();
            assertThat(config.getFilter()).isTrue();
        }

        @Test
        @DisplayName("drapeaux absents → null (pas false)")
        void missingFlagsAreNull() {
            BitmapConfig config = parser.parse(
                    "<bitmap " + NS + " android:src=\"@drawable/tuile\"/>");
            assertThat(config.getAntialias()).isNull();
            assertThat(config.getDither()).isNull();
            assertThat(config.getFilter()).isNull();
        }

        @Test
        @DisplayName("lit l'alpha flottant")
        void readsAlpha() {
            BitmapConfig config = parser.parse(
                    "<bitmap " + NS + " android:src=\"@drawable/tuile\"\n"
                            + "       android:alpha=\"0.5\"/>");
            assertThat(config.getAlpha()).isEqualTo(0.5f);
        }

        @Test
        @DisplayName("alpha invalide → null, pas d'exception")
        void invalidAlphaIsNull() {
            BitmapConfig config = parser.parse(
                    "<bitmap " + NS + " android:src=\"@drawable/tuile\"\n"
                            + "       android:alpha=\"abc\"/>");
            assertThat(config.getAlpha()).isNull();
        }

        @Test
        @DisplayName("lit le tint et son mode")
        void readsTintAndMode() {
            BitmapConfig config = parser.parse(
                    "<bitmap " + NS + " android:src=\"@drawable/tuile\"\n"
                            + "       android:tint=\"#FF4081\"\n"
                            + "       android:tintMode=\"multiply\"/>");
            assertThat(config.getTint()).isEqualTo("#FF4081");
            assertThat(config.getTintMode()).isEqualTo("multiply");
        }
    }

    @Nested
    @DisplayName("Cas limites")
    class EdgeCases {

        @Test
        @DisplayName("XML null lève une DrawableException")
        void nullXmlThrows() {
            assertThatThrownBy(() -> parser.parse(null))
                    .isInstanceOf(DrawableException.class);
        }

        @Test
        @DisplayName("XML vide lève une DrawableException")
        void emptyXmlThrows() {
            assertThatThrownBy(() -> parser.parse(""))
                    .isInstanceOf(DrawableException.class);
        }

        @Test
        @DisplayName("XML corrompu lève une DrawableException")
        void malformedXmlThrows() {
            assertThatThrownBy(() -> parser.parse("<bitmap " + NS))
                    .isInstanceOf(DrawableException.class);
        }
    }
}
