package jo.layoutlib.resources.configuration;

/**
 * Qualifier de méthode de saisie texte (nokeys, qwerty, 12key).
 *
 * @author jo@Dev
 * @since 1.0
 */
public class TextInputMethodQualifier implements ResourceQualifier {

    public static final String KEYS_NOKEYS = "nokeys";
    public static final String KEYS_QWERTY = "qwerty";
    public static final String KEYS_12KEY = "12key";

    private final String method;

    public TextInputMethodQualifier(String method) {
        this.method = method;
    }

    public static TextInputMethodQualifier fromQualifier(String qualifier) {
        if (qualifier == null) return null;
        switch (qualifier) {
            case KEYS_NOKEYS:
            case KEYS_QWERTY:
            case KEYS_12KEY:
                return new TextInputMethodQualifier(qualifier);
            default:
                return null;
        }
    }

    @Override
    public String getName() {
        return method;
    }

    @Override
    public String getLongName() {
        return "Saisie " + method;
    }

    @Override
    public boolean isValid() {
        return method != null;
    }

    @Override
    public boolean isMatchFor(ResourceQualifier other) {
        return true;
    }

    @Override
    public boolean isBetterMatchThan(ResourceQualifier other, ResourceQualifier reference) {
        return false;
    }

    public String getMethod() {
        return method;
    }
}
