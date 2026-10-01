package jo.layoutlib.layout.session;

import android.view.View;
import android.view.View.MeasureSpec;

/**
 * SessionParamsImpl, utilise l API Android native.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class SessionParamsImpl {

    private int width;
    private int height;
    private Object data;

    public SessionParamsImpl() {
    }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public static int makeMeasureSpec(int size, int mode) {
        return MeasureSpec.makeMeasureSpec(size, mode);
    }
}
