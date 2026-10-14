package jo.layoutlib.drawables;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du {@link AnimatedVectorParser} (ADR 0116 — P5/L5).
 *
 * <p>Valide le parsing des {@code <animated-vector>} : le vector sous-jacent
 * par référence (forme API 24+) ou par XML inline (forme API 21-23), les
 * cibles {@code <target>} capturées sans être résolues, les enfants inconnus
 * ignorés, et les cas limites.</p>
 *
 * @author jo@Dev
 * @since 3.1
 */
@DisplayName("AnimatedVectorParser — parsing des <animated-vector>")
class AnimatedVectorParserTest {

    private static final String NS =
            "xmlns:android=\"http://schemas.android.com/apk/res/android\"";

    private AnimatedVectorParser parser;

    @BeforeEach
    void setUp() {
        parser = new AnimatedVectorParser();
    }

    @Nested
    @DisplayName("Vector sous-jacent par référence (forme API 24+)")
    class ReferencedVector {

        @Test
        @DisplayName("capture la référence android:drawable de la racine")
        void capturesDrawableRef() {
            AnimatedVectorConfig config = parser.parse(
                    "<animated-vector " + NS + " android:drawable=\"@drawable/icone\"/>");
            assertThat(config.getDrawableRef()).isEqualTo("@drawable/icone");
            assertThat(config.getDrawableXml()).isNull();
            assertThat(config.hasDrawable()).isTrue();
        }

        @Test
        @DisplayName("capture la référence avec des cibles présentes")
        void capturesRefWithTargets() {
            AnimatedVectorConfig config = parser.parse(
                    "<animated-vector " + NS + " android:drawable=\"@drawable/icone\">\n"
                            + "  <target android:animation=\"@anim/rotation\" android:name=\"corps\"/>\n"
                            + "  <target android:animation=\"@anim/morph\" android:name=\"branche\"/>\n"
                            + "</animated-vector>");
            assertThat(config.getDrawableRef()).isEqualTo("@drawable/icone");
            assertThat(config.getTargetCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("une racine sans android:drawable laisse la référence à null")
        void missingRefStaysNull() {
            AnimatedVectorConfig config = parser.parse(
                    "<animated-vector " + NS + ">\n"
                            + "  <target android:animation=\"@anim/a\" android:name=\"x\"/>\n"
                            + "</animated-vector>");
            assertThat(config.getDrawableRef()).isNull();
            assertThat(config.hasDrawable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Vector sous-jacent inline (forme API 21-23)")
    class InlineVector {

        @Test
        @DisplayName("capture le premier enfant non-target comme drawable embarqué")
        void capturesFirstInlineChild() {
            AnimatedVectorConfig config = parser.parse(
                    "<animated-vector " + NS + ">\n"
                            + "  <vector android:width=\"24dp\" android:height=\"24dp\"\n"
                            + "          android:viewportWidth=\"24\" android:viewportHeight=\"24\">\n"
                            + "    <path android:fillColor=\"#FF000000\" android:pathData=\"M4,4\"/>\n"
                            + "  </vector>\n"
                            + "  <target android:animation=\"@anim/a\" android:name=\"p\"/>\n"
                            + "</animated-vector>");
            assertThat(config.getDrawableRef()).isNull();
            assertThat(config.getDrawableXml()).contains("<vector");
            assertThat(config.getDrawableXml()).contains("pathData");
            assertThat(config.hasDrawable()).isTrue();
        }

        @Test
        @DisplayName("la référence a la priorité : l'inline est capturé mais la réf existe")
        void refAndInlineBothCaptured() {
            AnimatedVectorConfig config = parser.parse(
                    "<animated-vector " + NS + " android:drawable=\"@drawable/icone\">\n"
                            + "  <vector android:width=\"24dp\" android:height=\"24dp\"/>\n"
                            + "</animated-vector>");
            // Les deux représentations sont capturées (contrat P0) ;
            // la priorité est appliquée à la résolution, pas au parsing
            assertThat(config.getDrawableRef()).isEqualTo("@drawable/icone");
            assertThat(config.getDrawableXml()).contains("<vector");
        }

        @Test
        @DisplayName("un seul drawable inline est capturé, les suivants ignorés")
        void onlyFirstInlineChildCaptured() {
            AnimatedVectorConfig config = parser.parse(
                    "<animated-vector " + NS + ">\n"
                            + "  <color android:color=\"#FF6750A4\"/>\n"
                            + "  <color android:color=\"#FFFF0000\"/>\n"
                            + "</animated-vector>");
            assertThat(config.getDrawableXml()).contains("#FF6750A4");
            assertThat(config.getDrawableXml()).doesNotContain("#FFFF0000");
        }
    }

    @Nested
    @DisplayName("Cibles <target>")
    class Targets {

        @Test
        @DisplayName("capture nom et référence d'animation de chaque cible")
        void capturesTargetNameAndAnimation() {
            AnimatedVectorConfig config = parser.parse(
                    "<animated-vector " + NS + " android:drawable=\"@drawable/icone\">\n"
                            + "  <target android:animation=\"@anim/rotation\" android:name=\"corps\"/>\n"
                            + "</animated-vector>");
            assertThat(config.getTargetCount()).isEqualTo(1);
            AnimatedVectorConfig.AnimatedTarget target = config.getTargets().get(0);
            assertThat(target.getName()).isEqualTo("corps");
            assertThat(target.getAnimationRef()).isEqualTo("@anim/rotation");
        }

        @Test
        @DisplayName("une cible sans nom ni animation est tolérée (valeurs null)")
        void toleratesEmptyTarget() {
            AnimatedVectorConfig config = parser.parse(
                    "<animated-vector " + NS + " android:drawable=\"@drawable/icone\">\n"
                            + "  <target/>\n"
                            + "</animated-vector>");
            assertThat(config.getTargetCount()).isEqualTo(1);
            assertThat(config.getTargets().get(0).getName()).isNull();
            assertThat(config.getTargets().get(0).getAnimationRef()).isNull();
        }

        @Test
        @DisplayName("plusieurs cibles sont capturées dans l'ordre")
        void capturesMultipleTargetsInOrder() {
            AnimatedVectorConfig config = parser.parse(
                    "<animated-vector " + NS + " android:drawable=\"@drawable/icone\">\n"
                            + "  <target android:animation=\"@anim/un\" android:name=\"a\"/>\n"
                            + "  <target android:animation=\"@anim/deux\" android:name=\"b\"/>\n"
                            + "  <target android:animation=\"@anim/trois\" android:name=\"c\"/>\n"
                            + "</animated-vector>");
            assertThat(config.getTargetCount()).isEqualTo(3);
            assertThat(config.getTargets()).extracting(
                    AnimatedVectorConfig.AnimatedTarget::getName)
                    .containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("la liste des cibles est non modifiable")
        void targetsAreUnmodifiable() {
            AnimatedVectorConfig config = parser.parse(
                    "<animated-vector " + NS + " android:drawable=\"@drawable/i\">\n"
                            + "  <target android:animation=\"@anim/a\" android:name=\"n\"/>\n"
                            + "</animated-vector>");
            assertThatThrownBy(() -> config.getTargets().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Tolérance (philosophie placeholder)")
    class Tolerance {

        @Test
        @DisplayName("les enfants inconnus sont ignorés sans exception")
        void unknownChildrenIgnored() {
            AnimatedVectorConfig config = parser.parse(
                    "<animated-vector " + NS + " android:drawable=\"@drawable/icone\">\n"
                            + "  <chose-inconnue/>\n"
                            + "</animated-vector>");
            assertThat(config.getDrawableRef()).isEqualTo("@drawable/icone");
            assertThat(config.getTargetCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("un racine vide produit une config vide mais valide")
        void emptyRootProducesEmptyConfig() {
            AnimatedVectorConfig config = parser.parse(
                    "<animated-vector " + NS + "/>");
            assertThat(config.hasDrawable()).isFalse();
            assertThat(config.getTargetCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("un XML vide lève DrawableException")
        void emptyXmlThrows() {
            assertThatThrownBy(() -> parser.parse("   "))
                    .isInstanceOf(DrawableException.class);
        }

        @Test
        @DisplayName("un XML syntaxiquement invalide lève DrawableException")
        void invalidXmlThrows() {
            // NB : kxml2 tolère une racine non fermée — on utilise un tag
            // fermant mismaché, réellement rejeté
            assertThatThrownBy(() -> parser.parse(
                    "<animated-vector " + NS + "></faux>"))
                    .isInstanceOf(DrawableException.class);
        }
    }
}
