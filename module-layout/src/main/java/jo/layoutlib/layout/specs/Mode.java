package jo.layoutlib.layout.specs;

import android.view.View;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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

    /**
     * Retourne la constante {@link View.MeasureSpec} correspondante.
     *
     * @return {@link View.MeasureSpec#UNSPECIFIED}, {@link View.MeasureSpec#EXACTLY}
     *         ou {@link View.MeasureSpec#AT_MOST}
     */
    @MeasureSpecMode
    public int getAndroidValue() {
        return androidValue;
    }

    public static Mode fromAndroidValue(@MeasureSpecMode int value) {
        for (Mode m : values()) {
            if (m.androidValue == value) {
                return m;
            }
        }
        return UNSPECIFIED;
    }

    /**
     * Annotation des valeurs de mode valides de {@link View.MeasureSpec}.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.EXACTLY, View.MeasureSpec.AT_MOST})
    public @interface MeasureSpecMode {
    }
}
