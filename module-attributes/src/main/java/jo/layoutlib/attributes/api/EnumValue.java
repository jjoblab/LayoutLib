package jo.layoutlib.attributes.api;

/**
 * Représente une valeur d enum, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class EnumValue {

    private final String name;
    private final int value;

    public EnumValue(String name, int value) {
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
        return name + " = " + value;
    }
}
