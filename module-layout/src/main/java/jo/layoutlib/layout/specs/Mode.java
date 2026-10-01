package jo.layoutlib.layout.specs;

import android.view.View;

/**
 * Mode de MeasureSpec.
 *
 * @author jo@Dev
 * @since 1.0
 */
public enum Mode {

    UNSPECIFIED(View.MeasureSpec.UNSPECIFIED),
    EXACTLY(View.MeasureSpec.EXACTLY),
    AT_MOST(View.MeasureSpec.AT_MOST);

    private final int androidValue;

    Mode(int androidValue) {
        this.androidValue = androidValue;
    }

    public int getAndroidValue() {
        return androidValue;
    }

    public static Mode fromAndroidValue(int value) {
        for (Mode m : values()) {
            if (m.androidValue == value) {
                return m;
            }
        }
        return UNSPECIFIED;
    }
}
