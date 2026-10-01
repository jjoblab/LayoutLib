package jo.layoutlib.layout.specs;

import android.view.View;

/**
 * Mode de MeasureSpec, utilise android.view.View.MeasureSpec natif.
 *
 * @author jo@Dev
 * @since 1.0
 */
public enum MeasureSpecMode {

    UNSPECIFIED(View.MeasureSpec.UNSPECIFIED),
    EXACTLY(View.MeasureSpec.EXACTLY),
    AT_MOST(View.MeasureSpec.AT_MOST);

    private final int androidValue;

    MeasureSpecMode(int androidValue) {
        this.androidValue = androidValue;
    }

    public int getAndroidValue() {
        return androidValue;
    }

    public static MeasureSpecMode fromAndroidValue(int value) {
        for (MeasureSpecMode m : values()) {
            if (m.androidValue == value) return m;
        }
        return UNSPECIFIED;
    }
}
