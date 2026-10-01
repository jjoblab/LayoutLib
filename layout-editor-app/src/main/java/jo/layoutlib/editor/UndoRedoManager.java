package jo.layoutlib.editor;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Gestionnaire undo/redo pour l'éditeur XML, avec pile d'historique
 * limitée et debounce pour éviter de pousser chaque frappe.
 *
 * <p>Utilisé en complément de l'EditorSession de code-editor-lib quand
 * on veut un historique au niveau XML complet (et non au niveau caractères).</p>
 *
 * @author jo@Dev
 * @since 2.0
 */
public class UndoRedoManager {

    private final Deque<String> undoStack = new ArrayDeque<>();
    private final Deque<String> redoStack = new ArrayDeque<>();
    private final int maxSize;
    private final long debounceMs;
    private long lastPushTime = 0;
    private String lastPushedState = null;

    public UndoRedoManager() {
        this(50, 800);
    }

    public UndoRedoManager(int maxSize, long debounceMs) {
        this.maxSize = maxSize;
        this.debounceMs = debounceMs;
    }

    public void pushState(String state) {
        if (state == null) return;
        long now = System.currentTimeMillis();

        if (now - lastPushTime < debounceMs && lastPushedState != null) {
            int lastLines = countLines(lastPushedState);
            int newLines = countLines(state);
            if (Math.abs(lastLines - newLines) <= 2) {
                lastPushedState = state;
                if (!undoStack.isEmpty()) undoStack.pop();
                undoStack.push(state);
                lastPushTime = now;
                trimStack(undoStack);
                return;
            }
        }

        undoStack.push(state);
        lastPushedState = state;
        lastPushTime = now;
        redoStack.clear();
        trimStack(undoStack);
    }

    public String undo() {
        if (undoStack.size() <= 1) return null;
        String current = undoStack.pop();
        redoStack.push(current);
        String previous = undoStack.peek();
        lastPushedState = previous;
        lastPushTime = System.currentTimeMillis();
        return previous;
    }

    public String redo() {
        if (redoStack.isEmpty()) return null;
        String state = redoStack.pop();
        undoStack.push(state);
        lastPushedState = state;
        lastPushTime = System.currentTimeMillis();
        return state;
    }

    public boolean canUndo() { return undoStack.size() > 1; }
    public boolean canRedo() { return !redoStack.isEmpty(); }
    public int getUndoCount() { return undoStack.size(); }
    public int getRedoCount() { return redoStack.size(); }
    public String getCurrentState() { return undoStack.peek(); }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
        lastPushedState = null;
        lastPushTime = 0;
    }

    private void trimStack(Deque<String> stack) {
        while (stack.size() > maxSize) {
            ((ArrayDeque<String>) stack).removeLast();
        }
    }

    private int countLines(String s) {
        if (s == null || s.isEmpty()) return 0;
        int count = 1;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') count++;
        }
        return count;
    }
}
