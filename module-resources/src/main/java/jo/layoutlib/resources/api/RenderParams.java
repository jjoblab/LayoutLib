package jo.layoutlib.resources.api;

/**
 * Paramètres de rendu, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class RenderParams {

    private final HardwareConfig hardwareConfig;
    private int timeoutMs = 0;
    private boolean decor = false;

    public RenderParams(HardwareConfig hardwareConfig) {
        this.hardwareConfig = hardwareConfig;
    }

    public HardwareConfig getHardwareConfig() { return hardwareConfig; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeout) { this.timeoutMs = timeout; }
    public boolean isDecor() { return decor; }
    public void setDecor(boolean decor) { this.decor = decor; }
}
