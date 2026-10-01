package jo.layoutlib.resources.api;

/**
 * Résultat d un rendu, inspiré de com.android.ide.common.rendering.api.Result.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class Result {

    public static final int SUCCESS = 0;
    public static final int ERROR_UNKNOWN = 1;
    public static final int ERROR_INFLATION = 2;
    public static final int ERROR_RENDER = 3;

    private final int statusCode;
    private final String message;
    private final Throwable cause;

    public Result(int statusCode, String message) {
        this(statusCode, message, null);
    }

    public Result(int statusCode, String message, Throwable cause) {
        this.statusCode = statusCode;
        this.message = message;
        this.cause = cause;
    }

    public static Result createSuccess() {
        return new Result(SUCCESS, "OK");
    }

    public static Result createError(String message, Throwable cause) {
        return new Result(ERROR_UNKNOWN, message, cause);
    }

    public int getStatusCode() { return statusCode; }
    public String getMessage() { return message; }
    public Throwable getCause() { return cause; }
    public boolean isSuccess() { return statusCode == SUCCESS; }
}
