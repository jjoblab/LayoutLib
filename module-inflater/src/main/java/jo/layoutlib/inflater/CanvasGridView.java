package jo.layoutlib.inflater;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Vue qui dessine une grille de fond sur tout le canvas area (16x16dp, blanc 2.5% opacity).
 * Cette grille est derrière le device frame (dessiné par OverlayView).
 * Simule le .canvas-grid du HTML preview.
 *
 * @author jo@Dev
 * @since 3.6
 */
public class CanvasGridView extends View {

    private final Paint linePaint;
    private float gridSizePx;

    public CanvasGridView(Context context) {
        this(context, null);
    }

    public CanvasGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = context.getResources().getDisplayMetrics().density;
        gridSizePx = 16 * density; // 16dp
        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#06FFFFFF")); // blanc 2.5%
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1f);
        setClickable(false);
        setFocusable(false);
        setWillNotDraw(false);
    }

    public void setGridSizeDp(int gridDp) {
        float density = getResources().getDisplayMetrics().density;
        gridSizePx = gridDp * density;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        for (float x = 0; x <= w; x += gridSizePx) {
            canvas.drawLine(x, 0, x, h, linePaint);
        }
        for (float y = 0; y <= h; y += gridSizePx) {
            canvas.drawLine(0, y, w, y, linePaint);
        }
    }
}
