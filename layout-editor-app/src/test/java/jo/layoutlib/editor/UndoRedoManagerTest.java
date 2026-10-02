package jo.layoutlib.editor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires JVM du {@link UndoRedoManager} (point 9 : l'app n'avait
 * aucun test).
 *
 * @author jo@Dev
 * @since 3.0
 */
@DisplayName("UndoRedoManager — historique XML complet")
class UndoRedoManagerTest {

    @Nested
    @DisplayName("Undo / Redo")
    class UndoRedo {

        @Test
        @DisplayName("undo revient à l'état précédent")
        void undoReturnsPreviousState() {
            UndoRedoManager manager = new UndoRedoManager(10, 0);
            manager.pushState("<A/>");
            manager.pushState("<B/>");
            manager.pushState("<C/>");

            assertThat(manager.undo()).isEqualTo("<B/>");
            assertThat(manager.undo()).isEqualTo("<A/>");
        }

        @Test
        @DisplayName("redo rejoue l'état annulé")
        void redoReplaysUndoneState() {
            UndoRedoManager manager = new UndoRedoManager(10, 0);
            manager.pushState("<A/>");
            manager.pushState("<B/>");
            manager.undo();

            assertThat(manager.redo()).isEqualTo("<B/>");
        }

        @Test
        @DisplayName("un nouvel état purge la pile redo")
        void newPushClearsRedoStack() {
            UndoRedoManager manager = new UndoRedoManager(10, 0);
            manager.pushState("<A/>");
            manager.pushState("<B/>");
            manager.undo();

            manager.pushState("<D/>");

            assertThat(manager.canRedo()).isFalse();
        }

        @Test
        @DisplayName("undo sur pile minimale retourne null")
        void undoWithSingleStateReturnsNull() {
            UndoRedoManager manager = new UndoRedoManager(10, 0);
            manager.pushState("<A/>");

            assertThat(manager.canUndo()).isFalse();
            assertThat(manager.undo()).isNull();
        }

        @Test
        @DisplayName("redo sur pile vide retourne null")
        void redoWithEmptyStackReturnsNull() {
            UndoRedoManager manager = new UndoRedoManager(10, 0);
            manager.pushState("<A/>");

            assertThat(manager.canRedo()).isFalse();
            assertThat(manager.redo()).isNull();
        }
    }

    @Nested
    @DisplayName("Debounce et bornes")
    class DebounceAndLimits {

        @Test
        @DisplayName("deux poussées rapprochées et proches en taille fusionnent")
        void debounceMergesCloseStates() throws InterruptedException {
            UndoRedoManager manager = new UndoRedoManager(10, 60_000);
            manager.pushState("<A/>");
            // simulé immédiat : même nombre de lignes → remplace la pile
            manager.pushState("<B/>");

            // la 2e poussée (proche) a remplacé la 1re : un seul état undoable
            assertThat(manager.getUndoCount()).isEqualTo(1);
            assertThat(manager.getCurrentState()).isEqualTo("<B/>");
        }

        @Test
        @DisplayName("un écart de taille important ne fusionne pas")
        void bigChangeIsNotMerged() {
            String big = "<A/>\n<B/>\n<C/>\n<D/>\n<E/>\n<F/>";
            UndoRedoManager manager = new UndoRedoManager(10, 60_000);
            manager.pushState("<A/>");
            manager.pushState(big);

            assertThat(manager.getUndoCount()).isEqualTo(2);
            assertThat(manager.undo()).isEqualTo("<A/>");
        }

        @Test
        @DisplayName("la pile est bornée à maxSize")
        void stackIsBounded() {
            UndoRedoManager manager = new UndoRedoManager(3, 0);
            for (int i = 0; i < 10; i++) {
                String state = "<state" + i + "/>\n<a/>\n<b/>\n<c/>\n<d/>";
                manager.pushState(state);
            }

            assertThat(manager.getUndoCount()).isLessThanOrEqualTo(3);
        }

        @Test
        @DisplayName("clear réinitialise tout")
        void clearResetsEverything() {
            UndoRedoManager manager = new UndoRedoManager(10, 0);
            manager.pushState("<A/>");
            manager.pushState("<B/>");
            manager.undo();

            manager.clear();

            assertThat(manager.canUndo()).isFalse();
            assertThat(manager.canRedo()).isFalse();
            assertThat(manager.getCurrentState()).isNull();
        }

        @Test
        @DisplayName("pushState(null) est ignoré")
        void nullStateIsIgnored() {
            UndoRedoManager manager = new UndoRedoManager(10, 0);
            manager.pushState("<A/>");
            manager.pushState(null);

            assertThat(manager.getUndoCount()).isEqualTo(1);
            assertThat(manager.getCurrentState()).isEqualTo("<A/>");
        }
    }
}
