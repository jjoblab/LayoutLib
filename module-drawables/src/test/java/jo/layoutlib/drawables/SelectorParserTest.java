package jo.layoutlib.drawables;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires du {@link SelectorParser}.
 *
 * @author jo@Dev
 */
@DisplayName("SelectorParser — parsing des <selector>")
class SelectorParserTest {

    private SelectorParser parser;

    @BeforeEach
    void setUp() {
        parser = new SelectorParser();
    }

    @Nested
    @DisplayName("Parsing de base")
    class BasicParsing {

        @Test
        @DisplayName("parse un selector avec 3 items")
        void shouldParseThreeItems() {
            SelectorConfig config = parser.parse(
                    "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                            + "<item android:state_pressed=\"true\" android:drawable=\"@drawable/a\"/>"
                            + "<item android:state_enabled=\"false\" android:drawable=\"@drawable/b\"/>"
                            + "<item android:drawable=\"@drawable/c\"/>"
                            + "</selector>");
            assertThat(config.getItemCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("parse la référence drawable d'un item")
        void shouldParseDrawableRef() {
            SelectorConfig config = parser.parse(
                    "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                            + "<item android:drawable=\"@drawable/default\"/>"
                            + "</selector>");
            List<SelectorConfig.SelectorItem> items = config.getItems();
            assertThat(items.get(0).getDrawableRef()).isEqualTo("@drawable/default");
        }
    }

    @Nested
    @DisplayName("États")
    class States {

        @Test
        @DisplayName("parse l'état pressed")
        void shouldParsePressedState() {
            SelectorConfig config = parser.parse(
                    "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                            + "<item android:state_pressed=\"true\" android:drawable=\"@drawable/a\"/>"
                            + "</selector>");
            SelectorConfig.SelectorItem item = config.getItems().get(0);
            assertThat(item.getStatePressed()).isTrue();
            assertThat(item.hasAnyState()).isTrue();
        }

        @Test
        @DisplayName("parse plusieurs états sur un même item")
        void shouldParseMultipleStates() {
            SelectorConfig config = parser.parse(
                    "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                            + "<item android:state_pressed=\"true\" "
                            + "android:state_enabled=\"true\" "
                            + "android:state_focused=\"false\" "
                            + "android:drawable=\"@drawable/a\"/>"
                            + "</selector>");
            SelectorConfig.SelectorItem item = config.getItems().get(0);
            assertThat(item.getStatePressed()).isTrue();
            assertThat(item.getStateEnabled()).isTrue();
            assertThat(item.getStateFocused()).isFalse();
        }

        @Test
        @DisplayName("un item sans état a hasAnyState=false")
        void itemWithoutStateShouldReturnFalse() {
            SelectorConfig config = parser.parse(
                    "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                            + "<item android:drawable=\"@drawable/default\"/>"
                            + "</selector>");
            SelectorConfig.SelectorItem item = config.getItems().get(0);
            assertThat(item.hasAnyState()).isFalse();
        }
    }

    @Nested
    @DisplayName("Recherche d'item correspondant")
    class FindMatchingItem {

        @Test
        @DisplayName("retourne l'item par défaut si aucun état ne correspond")
        void shouldReturnDefaultItemWhenNoMatch() {
            SelectorConfig config = parser.parse(
                    "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                            + "<item android:state_pressed=\"true\" android:drawable=\"@drawable/a\"/>"
                            + "<item android:drawable=\"@drawable/default\"/>"
                            + "</selector>");
            SelectorConfig.SelectorItem match = config.findMatchingItem(
                    false, true, false, false, false);
            assertThat(match.getDrawableRef()).isEqualTo("@drawable/default");
        }

        @Test
        @DisplayName("retourne l'item pressed quand pressed=true")
        void shouldReturnPressedItemWhenPressed() {
            SelectorConfig config = parser.parse(
                    "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                            + "<item android:state_pressed=\"true\" android:drawable=\"@drawable/a\"/>"
                            + "<item android:drawable=\"@drawable/default\"/>"
                            + "</selector>");
            SelectorConfig.SelectorItem match = config.findMatchingItem(
                    true, true, false, false, false);
            assertThat(match.getDrawableRef()).isEqualTo("@drawable/a");
        }

        @Test
        @DisplayName("getDefaultItem retourne l'item sans états")
        void shouldReturnDefaultItem() {
            SelectorConfig config = parser.parse(
                    "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                            + "<item android:state_pressed=\"true\" android:drawable=\"@drawable/a\"/>"
                            + "<item android:drawable=\"@drawable/default\"/>"
                            + "</selector>");
            SelectorConfig.SelectorItem def = config.getDefaultItem();
            assertThat(def).isNotNull();
            assertThat(def.getDrawableRef()).isEqualTo("@drawable/default");
        }
    }

    @Nested
    @DisplayName("Cas d'erreur")
    class ErrorCases {

        @Test
        @DisplayName("rejette un XML null")
        void shouldRejectNullXml() {
            assertThatThrownBy(() -> parser.parse(null))
                    .isInstanceOf(DrawableException.class);
        }

        @Test
        @DisplayName("rejette un XML vide")
        void shouldRejectEmptyXml() {
            assertThatThrownBy(() -> parser.parse(""))
                    .isInstanceOf(DrawableException.class);
        }
    }

    @Nested
    @DisplayName("Méthode matches")
    class MatchesMethod {

        @Test
        @DisplayName("un item pressed=true ne match pas pressed=false")
        void pressedItemShouldNotMatchUnpressed() {
            SelectorConfig.SelectorItem item = new SelectorConfig.SelectorItem();
            item.setStatePressed(true);
            assertThat(item.matches(false, true, false, false, false)).isFalse();
        }

        @Test
        @DisplayName("un item sans états match toujours")
        void itemWithoutStatesShouldAlwaysMatch() {
            SelectorConfig.SelectorItem item = new SelectorConfig.SelectorItem();
            assertThat(item.matches(true, false, true, false, true)).isTrue();
            assertThat(item.matches(false, false, false, false, false)).isTrue();
        }
    }
}
