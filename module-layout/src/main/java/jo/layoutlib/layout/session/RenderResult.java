package jo.layoutlib.layout.session;

/**
 * Résultat d un rendu.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class RenderResult {

    private final boolean success;
    private final long renderTimeMs;
    private final int viewCount;
    private final int measuredWidth;
    private final int measuredHeight;
    private final String errorMessage;

    public RenderResult(boolean success, long renderTimeMs, int viewCount,
                        int measuredWidth, int measuredHeight, String errorMessage) {
        this.success = success;
        this.renderTimeMs = renderTimeMs;
        this.viewCount = viewCount;
        this.measuredWidth = measuredWidth;
        this.measuredHeight = measuredHeight;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public long getRenderTimeMs() {
        return renderTimeMs;
    }

    public int getViewCount() {
        return viewCount;
    }

    public int getMeasuredWidth() {
        return measuredWidth;
    }

    public int getMeasuredHeight() {
        return measuredHeight;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean meetsPerformanceTarget() {
        return renderTimeMs < 200;
    }

    @Override
    public String toString() {
        if (success) {
            return "OK : " + viewCount + " vues, " + renderTimeMs + " ms, "
                    + measuredWidth + "x" + measuredHeight;
        }
        return "ECHEC : " + errorMessage;
    }
}
