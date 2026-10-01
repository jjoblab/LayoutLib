package jo.layoutlib.layout.specs;

/**
 * Représente une taille avec un mode.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class Size {

    private final int size;
    private final Mode mode;

    public Size(int size, Mode mode) {
        this.size = size;
        this.mode = mode != null ? mode : Mode.UNSPECIFIED;
    }

    public int getSize() {
        return size;
    }

    public Mode getMode() {
        return mode;
    }

    public boolean isExactly() {
        return mode == Mode.EXACTLY;
    }

    public boolean isAtMost() {
        return mode == Mode.AT_MOST;
    }

    public boolean isUnspecified() {
        return mode == Mode.UNSPECIFIED;
    }

    @Override
    public String toString() {
        return mode + ":" + size;
    }
}
