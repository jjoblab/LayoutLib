package jo.layoutlib.inflater;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Conteneur de preview avec support zoom pincé et pan, inspiré du
 * canvas zoomable d'Android Studio.
 *
 * <p>Wrappe un unique enfant (le rendu du layout) et permet :</p>
 * <ul>
 *   <li>Zoom pincé (pinch-to-zoom) entre 0.5x et 3.0x</li>
 *   <li>Drag pour pan quand zoom &gt; 1</li>
 *   <li>API programmatique {@link #setZoom(float)}, {@link #resetZoom()}</li>
 *   <li>Grille optionnelle en arrière-plan</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 1.1
 */
public class ZoomablePreviewView extends FrameLayout {

    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 3.0f;

    private float zoom = 1.0f;
    private float panX = 0f;
    private float panY = 0f;
    private boolean showGrid = true;
    private int gridSizeDp = 16;
    private int gridColor = Color.parseColor("#08FFFFFF");

    private ScaleGestureDetector scaleDetector;
    private Paint gridPaint;

    private float touchStartX, touchStartY;
    private float panStartX, panStartY;
    private boolean isPanning = false;

    /** Listener de changement de zoom. */
    public interface OnZoomChangeListener {
        void onZoomChanged(float zoom);
    }

    private OnZoomChangeListener zoomListener;

    public ZoomablePreviewView(Context context) {
        super(context);
        init();
    }

    public ZoomablePreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ZoomablePreviewView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        gridPaint = new Paint();
        gridPaint.setColor(gridColor);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(0.5f);
        setClipChildren(false);
        setClipToPadding(false);
    }

    public void setShowGrid(boolean show) {
        this.showGrid = show;
        invalidate();
    }

    public void setGridColor(int color) {
        this.gridColor = color;
        gridPaint.setColor(color);
        invalidate();
    }

    public void setZoom(float z) {
        this.zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, z));
        applyTransform();
        if (zoomListener != null) zoomListener.onZoomChanged(this.zoom);
    }

    public float getZoom() {
        return zoom;
    }

    public void resetZoom() {
        this.zoom = 1.0f;
        this.panX = 0f;
        this.panY = 0f;
        applyTransform();
        if (zoomListener != null) zoomListener.onZoomChanged(this.zoom);
    }

    public void setOnZoomChangeListener(OnZoomChangeListener l) {
        this.zoomListener = l;
    }

    private void applyTransform() {
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            child.setScaleX(zoom);
            child.setScaleY(zoom);
            child.setTranslationX(panX);
            child.setTranslationY(panY);
        }
        invalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        // Dessiner la grille avant les enfants
        if (showGrid) {
            drawGrid(canvas);
        }
        super.dispatchDraw(canvas);
    }

    private void drawGrid(Canvas canvas) {
        int gridPx = (int) (gridSizeDp * getResources().getDisplayMetrics().density);
        if (gridPx <= 0) return;
        for (int x = 0; x < getWidth(); x += gridPx) {
            canvas.drawLine(x, 0, x, getHeight(), gridPaint);
        }
        for (int y = 0; y < getHeight(); y += gridPx) {
            canvas.drawLine(0, y, getWidth(), y, gridPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        if (event.getPointerCount() == 1 && zoom > 1.0f) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    touchStartX = event.getX();
                    touchStartY = event.getY();
                    panStartX = panX;
                    panStartY = panY;
                    isPanning = true;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (isPanning) {
                        panX = panStartX + (event.getX() - touchStartX);
                        panY = panStartY + (event.getY() - touchStartY);
                        applyTransform();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isPanning = false;
                    return true;
            }
        }
        return super.onTouchEvent(event);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float factor = detector.getScaleFactor();
            setZoom(zoom * factor);
            return true;
        }
    }
}
