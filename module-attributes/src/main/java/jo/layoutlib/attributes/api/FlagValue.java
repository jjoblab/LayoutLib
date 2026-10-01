package jo.layoutlib.attributes.api;

/**
 * Représente une valeur de flag, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class FlagValue {

    private final String name;
    private final int value;

    public FlagValue(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name + " = 0x" + Integer.toHexString(value);
    }
}
