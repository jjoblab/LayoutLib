package jo.layoutlib.inflater;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import java.util.List;

/**
 * Vue d'overlay pour le mode design/blueprint, inspiré d'Android Studio.
 *
 * <p>Cette vue se superpose au rendu du layout et dessine :</p>
 * <ul>
 *   <li><strong>Mode Design</strong> : bordures de sélection sur la vue sélectionnée</li>
 *   <li><strong>Mode Blueprint</strong> : wireframe de toutes les vues (rectangles vides)</li>
 *   <li><strong>Overlays</strong> : marges (pointillés), padding (tirets), baselines</li>
 *   <li><strong>Grille</strong> : grille optionnelle pour l'alignement</li>
 *   <li><strong>Poignées de redimensionnement</strong> : 4 coins + 4 bords (8 handles)</li>
 *   <li><strong>Label de sélection</strong> : tag avec nom de classe + id (coin haut-gauche)</li>
 *   <li><strong>Tooltip de dimensions</strong> : affiche largeur × hauteur (en bas de la sélection)</li>
 *   <li><strong>Bp-box style</strong> : rectangles dashed avec label monospace classe + id</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * FrameLayout previewContainer = findViewById(R.id.previewContainer);
 * OverlayView overlay = new OverlayView(context);
 * overlay.setViewInfo(rootInfo);
 * overlay.setMode(OverlayView.Mode.DESIGN);
 * overlay.setSelectedView(selectedInfo);
 * overlay.setShowHandles(true);
 * overlay.setShowDimensionTooltip(true);
 * overlay.setOnSelectionListener(info -> { ... });
 * overlay.setOnHandleDragListener((info, dir, w, h) -> { ... });
 * previewContainer.addView(overlay);
 * }</pre>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class OverlayView extends FrameLayout {

    /**
     * Mode d'affichage.
     */
    public enum Mode {
        /** Design normal — rendu réel + overlays optionnels. */
        DESIGN,
        /** Blueprint — wireframe seulement (lignes simples). */
        BLUEPRINT,
        /** Blueprint — style bp-box (rectangles dashed avec label classe + id). */
        BLUEPRINT_BOXES,
        /** Design + Blueprint — overlays sur le rendu réel. */
        DESIGN_WITH_OVERLAYS
    }

    /**
     * Direction d'un handle de redimensionnement.
     */
    public enum HandleDirection {
        TL, TR, BL, BR,
        TOP, BOTTOM, LEFT, RIGHT
    }

    /**
     * Listener de sélection d'une vue.
     */
    public interface OnSelectionListener {
        /**
         * Appelé quand l'utilisateur tape sur une vue dans l'overlay.
         *
         * @param info les ViewInfo de la vue tapée, ou null si tape dans le vide
         */
        void onSelectionChanged(ViewInfoCollector.ViewInfo info);
    }

    /**
     * Listener de drag d'un handle de redimensionnement.
     */
    public interface OnHandleDragListener {
        /**
         * Appelé pendant le drag d'un handle.
         *
         * @param info     les ViewInfo de la vue sélectionnée
         * @param dir      la direction du handle
         * @param newWidth la nouvelle largeur en px
         * @param newHeight la nouvelle hauteur en px
         */
        void onHandleDrag(ViewInfoCollector.ViewInfo info, HandleDirection dir,
                          int newWidth, int newHeight);

        /**
         * Appelé quand le drag se termine.
         *
         * <p><strong>Important :</strong> contrairement à {@code info.width/height},
         * {@code finalWidth} et {@code finalHeight} reflètent la taille réellement
         * obtenue à la fin du drag. L'implémenteur doit écrire ces valeurs dans le
         * XML ; écrire {@code info.width/height} (qui est immutable et pré-drag)
         * annulerait silencieusement l'édition utilisateur au prochain rendu.</p>
         */
        default void onHandleDragEnd(ViewInfoCollector.ViewInfo info,
                                     int finalWidth, int finalHeight) {}
    }

    /**
     * Listener pour les changements de zoom/pan.
     *
     * <p>L'{@code OverlayView} ne modifie plus sa propre transformation
     * ({@code setScaleX/Y}). Elle notifie ce listener qui doit appliquer
     * la transformation au conteneur parent (typiquement le
     * {@code previewContainer}) — ainsi le device frame (background du
     * container), le root rendu (enfant du container), et l'overlay
     * (enfant du container) scalent <strong>tous ensemble</strong>.</p>
     */
    public interface OnZoomPanListener {
        /**
         * Appelé à chaque changement de zoom/pan.
         *
         * @param zoom facteur d'échelle (1 = 100 %)
         * @param panX translation X en px (espace local du parent)
         * @param panY translation Y en px (espace local du parent)
         */
        void onZoomPanChanged(float zoom, float panX, float panY);
    }

    // ---- Couleurs par défaut (Material 3 dark) ----
    private int colorBlueprint = Color.parseColor("#4AA3FF");
    private int colorSelection = Color.parseColor("#7C5CFF");
    private int colorSelectionFill = Color.parseColor("#267C5CFF"); // 15% alpha
    private int colorLabel = Color.parseColor("#7C5CFF");
    private int colorLabelText = Color.WHITE;
    private int colorMargin = Color.parseColor("#4CAF50");
    private int colorPadding = Color.parseColor("#FF9800");
    private int colorGrid = Color.parseColor("#15000000");
    private int colorHandleFill = Color.WHITE;
    private int colorHandleStroke = Color.parseColor("#7C5CFF");
    private int colorTooltipBg = Color.parseColor("#0A0A18");
    private int colorTooltipText = Color.WHITE;
    private int colorBpIdText = Color.parseColor("#994AA3FF");

    private Mode mode = Mode.DESIGN;
    private ViewInfoCollector.ViewInfo viewInfo;
    private ViewInfoCollector.ViewInfo selectedView;
    private boolean showMargins = false;
    private boolean showPadding = false;
    private boolean showGrid = false;
    private boolean showHandles = true;
    private boolean showSelectionLabel = true;
    private boolean showDimensionTooltip = true;
    private int gridSize = 8; // dp
    private int handleSizePx = 14; // dp converted in onDraw
    private int handleStrokeWidthPx = 3; // px
    private int selectionStrokeWidthPx = 3; // px

    private OnSelectionListener selectionListener;
    private OnHandleDragListener handleDragListener;

    // Drag state
    private boolean dragging = false;
    private float dragStartX, dragStartY;
    private int dragStartW, dragStartH;
    private HandleDirection dragDir;

    // ---- Paints (initialisées paresseusement) ----
    private Paint blueprintPaint;
    private Paint bpBoxFillPaint;
    private Paint selectionPaint;
    private Paint selectionFillPaint;
    private Paint marginPaint;
    private Paint paddingPaint;
    private Paint gridPaint;
    private Paint textPaint;
    private Paint bpIdTextPaint;
    private Paint handleFillPaint;
    private Paint handleStrokePaint;
    private Paint labelBgPaint;
    private Paint labelTextPaint;
    private Paint tooltipBgPaint;
    private Paint tooltipTextPaint;
    private Paint tooltipBorderPaint;
    private Path notchPath;

    public OverlayView(Context context) {
        super(context);
        init();
    }

    public OverlayView(Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        handleSizePx = (int) (14 * density);
        handleStrokeWidthPx = (int) (2.5f * density);
        selectionStrokeWidthPx = (int) (2.5f * density);
        gridSizePx = (int) (8 * density); // 8dp grid for snap

        // Guide d'alignement (ligne rose/orange comme Android Studio)
        guidePaint = new Paint();
        guidePaint.setColor(Color.parseColor("#FFFF4081")); // pink Material
        guidePaint.setStyle(Paint.Style.STROKE);
        guidePaint.setStrokeWidth(1f * density);
        guidePaint.setAntiAlias(true);
        guidePaint.setPathEffect(new android.graphics.DashPathEffect(
                new float[]{8f * density, 4f * density}, 0));

        // Rendre l'overlay clickable pour qu'il reçoive et consomme les touch events
        // (sinon le ScrollView parent et le root rendu les interceptent)
        setClickable(true);
        setFocusable(true);
        setFocusableInTouchMode(true);

        blueprintPaint = new Paint();
        blueprintPaint.setColor(colorBlueprint);
        blueprintPaint.setStyle(Paint.Style.STROKE);
        blueprintPaint.setStrokeWidth(1.5f * density);
        blueprintPaint.setAntiAlias(true);
        blueprintPaint.setPathEffect(new android.graphics.DashPathEffect(
                new float[]{6f * density, 4f * density}, 0));

        bpBoxFillPaint = new Paint();
        bpBoxFillPaint.setColor(Color.parseColor("#0F4AA3FF")); // 6% alpha
        bpBoxFillPaint.setStyle(Paint.Style.FILL);
        bpBoxFillPaint.setAntiAlias(true);

        selectionPaint = new Paint();
        selectionPaint.setColor(colorSelection);
        selectionPaint.setStyle(Paint.Style.STROKE);
        selectionPaint.setStrokeWidth(selectionStrokeWidthPx);
        selectionPaint.setAntiAlias(true);

        selectionFillPaint = new Paint();
        selectionFillPaint.setColor(colorSelectionFill);
        selectionFillPaint.setStyle(Paint.Style.FILL);
        selectionFillPaint.setAntiAlias(true);

        marginPaint = new Paint();
        marginPaint.setColor(colorMargin);
        marginPaint.setStyle(Paint.Style.STROKE);
        marginPaint.setStrokeWidth(1f * density);
        marginPaint.setPathEffect(new android.graphics.DashPathEffect(
                new float[]{4f * density, 4f * density}, 0));

        paddingPaint = new Paint();
        paddingPaint.setColor(colorPadding);
        paddingPaint.setStyle(Paint.Style.STROKE);
        paddingPaint.setStrokeWidth(1f * density);
        paddingPaint.setPathEffect(new android.graphics.DashPathEffect(
                new float[]{6f * density, 3f * density}, 0));

        textPaint = new Paint();
        textPaint.setColor(colorBlueprint);
        textPaint.setTextSize(10f * density);
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(android.graphics.Typeface.MONOSPACE);

        bpIdTextPaint = new Paint();
        bpIdTextPaint.setColor(colorBpIdText);
        bpIdTextPaint.setTextSize(10f * density);
        bpIdTextPaint.setAntiAlias(true);
        bpIdTextPaint.setTypeface(android.graphics.Typeface.MONOSPACE);

        handleFillPaint = new Paint();
        handleFillPaint.setColor(colorHandleFill);
        handleFillPaint.setStyle(Paint.Style.FILL);
        handleFillPaint.setAntiAlias(true);

        handleStrokePaint = new Paint();
        handleStrokePaint.setColor(colorHandleStroke);
        handleStrokePaint.setStyle(Paint.Style.STROKE);
        handleStrokePaint.setStrokeWidth(handleStrokeWidthPx);
        handleStrokePaint.setAntiAlias(true);

        labelBgPaint = new Paint();
        labelBgPaint.setColor(colorLabel);
        labelBgPaint.setStyle(Paint.Style.FILL);
        labelBgPaint.setAntiAlias(true);

        labelTextPaint = new Paint();
        labelTextPaint.setColor(colorLabelText);
        labelTextPaint.setTextSize(9.5f * density);
        labelTextPaint.setAntiAlias(true);
        labelTextPaint.setTypeface(android.graphics.Typeface.MONOSPACE);
        labelTextPaint.setFakeBoldText(true);

        tooltipBgPaint = new Paint();
        tooltipBgPaint.setColor(colorTooltipBg);
        tooltipBgPaint.setStyle(Paint.Style.FILL);
        tooltipBgPaint.setAntiAlias(true);

        tooltipBorderPaint = new Paint();
        tooltipBorderPaint.setColor(colorSelection);
        tooltipBorderPaint.setStyle(Paint.Style.STROKE);
        tooltipBorderPaint.setStrokeWidth(1f * density);
        tooltipBorderPaint.setAntiAlias(true);

        tooltipTextPaint = new Paint();
        tooltipTextPaint.setColor(colorTooltipText);
        tooltipTextPaint.setTextSize(11f * density);
        tooltipTextPaint.setAntiAlias(true);
        tooltipTextPaint.setTypeface(android.graphics.Typeface.MONOSPACE);
        tooltipTextPaint.setFakeBoldText(true);

        notchPath = new Path();

        // Device frame paints
        frameGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        frameGlowPaint.setColor(Color.parseColor("#0A7C5CFF")); // rgba(124,92,255,0.04)
        frameGlowPaint.setStyle(Paint.Style.FILL);

        frameBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        frameBorderPaint.setColor(Color.parseColor("#FF322F47")); // outline-2
        frameBorderPaint.setStyle(Paint.Style.FILL);

        frameBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        frameBgPaint.setColor(Color.parseColor("#FF15152A")); // device canvas bg
        frameBgPaint.setStyle(Paint.Style.FILL);

        // Pinch-to-zoom detector
        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());

        // Grille de fond du device frame (8dp, blanc 3% opacity)
        gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#08FFFFFF")); // blanc 3%
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(0.5f * density);

        // FrameLayout dessine déjà les enfants ; nos overlays sont dans dispatchDraw
    }

    // ---- Device frame + Zoom (comme Android Studio) ----

    /**
     * Active/désactive le device frame.
     */
    public void setShowDeviceFrame(boolean show) {
        this.showDeviceFrame = show;
        invalidate();
    }

    /**
     * Active/désactive la grille à l'intérieur du device frame.
     */
    public void setShowCanvasGrid(boolean show) {
        this.showCanvasGrid = show;
        invalidate();
    }

    /**
     * Définit le profil d'appareil (dimensions du device frame).
     * Configure aussi le padding de l'OverlayView pour que le root
     * soit positionné avec le bon offset (glow + framePadding).
     */
    public void setDeviceProfile(DeviceProfile profile) {
        this.deviceProfile = profile;
        requestLayout();
        invalidate();
    }

    /**
     * Définit le padding intérieur du device frame.
     * Reconfigure le padding de l'OverlayView (glow + framePadding).
     */
    public void setFramePaddingDp(float paddingDp) {
        this.framePaddingDp = paddingDp;
        invalidate();
    }

    /**
     * @return le profil d'appareil courant.
     */
    public DeviceProfile getDeviceProfile() {
        return deviceProfile;
    }

    /**
     * @return le zoom actuel (1.0 = 100%).
     */
    public float getZoom() {
        return zoom;
    }

    /**
     * Définit le zoom (0.25 à 4.0).
     *
     * @param z le facteur de zoom
     */
    public void setZoom(float z) {
        this.zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, z));
        applyZoom();
    }

    /**
     * Zoom in (×1.1).
     */
    public void zoomIn() {
        setZoom(zoom * 1.1f);
    }

    /**
     * Zoom out (÷1.1).
     */
    public void zoomOut() {
        setZoom(zoom / 1.1f);
    }

    /**
     * Reset zoom à 100%.
     */
    public void resetZoom() {
        zoom = 1.0f;
        panX = 0f;
        panY = 0f;
        applyZoom();
    }

    private void applyZoom() {
        // ── Scaller l'OverlayView elle-même (pivot au centre) ─────────────
        // Plus de device frame ni de previewContainer — l'overlay est un
        // enfant direct de designScroll, au même titre que le root rendu.
        // On scalle l'overlay ET on notifie le listener pour qu'il scalle le
        // root rendu avec les MÊMES paramètres (zoom, pan, pivot=center).
        // Comme les deux ont les mêmes dimensions et la même position
        // (gravity=CENTER dans designScroll), leurs pivots (centre du view)
        // sont au même endroit écran → ils scalent symétriquement et
        // restent alignés.
        float pivotX = getWidth() > 0 ? getWidth() / 2f : 0f;
        float pivotY = getHeight() > 0 ? getHeight() / 2f : 0f;
        setPivotX(pivotX);
        setPivotY(pivotY);
        setScaleX(zoom);
        setScaleY(zoom);
        setTranslationX(panX);
        setTranslationY(panY);
        // ── Notifier le listener pour qu'il scalle le root rendu ──────────
        if (zoomPanListener != null) {
            zoomPanListener.onZoomPanChanged(zoom, panX, panY);
        }
    }

    /**
     * Listener pour pinch-to-zoom.
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float factor = detector.getScaleFactor();
            setZoom(zoom * factor);
            return true;
        }
    }

    /**
     * Dessine le device frame (glow + bordure + fond) AVANT les overlays.
     *
     * <p>L'OverlayView a les MÊMES dimensions que le root rendu (rootW × rootH).
     * Le device frame est donc dessiné autour du root, exactement à sa taille.
     * Comme l'OverlayView est scalée par son parent (previewContainer), le
     * device frame scalera automatiquement avec le zoom.</p>
     *
     * <p>Couches :</p>
     * <ol>
     *   <li><strong>Glow ring</strong> (6dp, rgba(124,92,255,0.04)) — extérieur</li>
     *   <li><strong>Bordure</strong> 1dp (#322F47)</li>
     *   <li><strong>Fond</strong> (#15152A) — surface intérieure</li>
     * </ol>
     */
    private void drawDeviceFrame(Canvas canvas) {
        if (!showDeviceFrame) return;
        float density = getResources().getDisplayMetrics().density;
        float glow = frameGlowDp * density;
        float corner = frameCornerDp * density;
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Layer 1 : glow ring (6dp, rgba(124,92,255,0.04))
        RectF glowRect = new RectF(0, 0, w, h);
        canvas.drawRoundRect(glowRect, corner + glow, corner + glow, frameGlowPaint);

        // Layer 2 : border 1dp outline-2 (#322F47)
        RectF borderRect = new RectF(glow, glow, w - glow, h - glow);
        canvas.drawRoundRect(borderRect, corner, corner, frameBorderPaint);

        // Layer 3 : fond #15152A sur TOUTE la surface intérieure (le root
        // rendu — sibling de l'overlay, pas enfant — sera dessiné par-dessus
        // par previewContainer).
        float inset = glow + 1 * density;
        RectF bgRect = new RectF(inset, inset, w - inset, h - inset);
        canvas.drawRoundRect(bgRect, corner - 1 * density, corner - 1 * density, frameBgPaint);
    }

    /**
     * Dessine la grille APRÈS le device frame (par-dessus le fond),
     * mais AVANT les enfants (root rendu).
     */
    private void drawCanvasGrid(Canvas canvas) {
        if (!showCanvasGrid) return;
        float density = getResources().getDisplayMetrics().density;
        float glow = frameGlowDp * density;
        float corner = frameCornerDp * density;
        float w = getWidth();
        float h = getHeight();
        float pad = framePaddingDp * density;
        float inset = glow + 1 * density;
        RectF bgRect = new RectF(inset, inset, w - inset, h - inset);

        int saveCount = canvas.save();
        // Clip la grille au rounded rect du device frame
        Path clipPath = new Path();
        clipPath.addRoundRect(bgRect, corner - 1 * density, corner - 1 * density, Path.Direction.CW);
        canvas.clipPath(clipPath);
        float gridStep = gridStepDp * density;
        for (float x = pad; x < w - pad; x += gridStep) {
            canvas.drawLine(x, pad, x, h - pad, gridPaint);
        }
        for (float y = pad; y < h - pad; y += gridStep) {
            canvas.drawLine(pad, y, w - pad, y, gridPaint);
        }
        canvas.restoreToCount(saveCount);
    }

    public void setViewInfo(ViewInfoCollector.ViewInfo info) {
        this.viewInfo = info;
        invalidate();
    }

    public void setSelectedView(ViewInfoCollector.ViewInfo info) {
        this.selectedView = info;
        invalidate();
    }

    public ViewInfoCollector.ViewInfo getSelectedView() {
        return selectedView;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        invalidate();
    }

    public Mode getMode() {
        return mode;
    }

    public void setShowMargins(boolean show) { this.showMargins = show; invalidate(); }
    public void setShowPadding(boolean show) { this.showPadding = show; invalidate(); }
    public void setShowGrid(boolean show) { this.showGrid = show; invalidate(); }
    public void setShowHandles(boolean show) { this.showHandles = show; invalidate(); }
    public void setShowSelectionLabel(boolean show) { this.showSelectionLabel = show; invalidate(); }
    public void setShowDimensionTooltip(boolean show) { this.showDimensionTooltip = show; invalidate(); }

    public void setSelectionListener(OnSelectionListener l) { this.selectionListener = l; }
    public void setHandleDragListener(OnHandleDragListener l) { this.handleDragListener = l; }

    /**
     * Définit le listener de zoom/pan.
     *
     * <p>L'{@code OverlayView} ne modifie plus sa propre transformation.
     * À chaque changement de zoom/pan, ce listener est notifié et doit
     * appliquer la transformation au conteneur parent
     * (le {@code previewContainer} dans le {@code MainActivity}), pour
     * que device frame + root rendu + overlay scalent symétriquement.</p>
     */
    public void setOnZoomPanListener(OnZoomPanListener l) {
        this.zoomPanListener = l;
        // Notifier immédiatement avec l'état courant
        if (l != null) l.onZoomPanChanged(zoom, panX, panY);
    }

    private OnZoomPanListener zoomPanListener;

    /** Couleurs thématiques. */
    public void setSelectionColor(int color) {
        this.colorSelection = color;
        this.colorLabel = color;
        this.colorHandleStroke = color;
        selectionPaint.setColor(color);
        labelBgPaint.setColor(color);
        handleStrokePaint.setColor(color);
        tooltipBorderPaint.setColor(color);
        invalidate();
    }

    public void setBlueprintColor(int color) {
        this.colorBlueprint = color;
        blueprintPaint.setColor(color);
        textPaint.setColor(color);
        invalidate();
    }

    // onMeasure supprimé — le FrameLayout mesure naturellement les enfants.
    // Le device frame est dessiné dans dispatchDraw, pas via onMeasure.
    // La taille de l'OverlayView est déterminée par le LayoutParams du root
    // (rootW × rootH) + le padding (setPadding) ajouté dans setDeviceProfile.

    /**
     * Dessine les overlays (sélection, handles, guides, etc.) APRÈS les enfants.
     * Comme Android Studio : le parent contient le rendu et dessine l'overlay
     * par-dessus dans dispatchDraw.
     *
     * <p><strong>Note :</strong> le device frame est dessiné par
     * {@link DeviceFrameView} (vue sœur placée en z=0 dans previewContainer,
     * derrière le root et l'overlay). L'OverlayView ne dessine QUE les
     * overlays (sélection, handles, guides, marges, padding).</p>
     */
    @Override
    protected void dispatchDraw(Canvas canvas) {
        // OverlayView est un FRÈRE du root (pas son parent).
        // Il est positionné au-dessus du root avec les MÊMES dimensions.
        // Les ViewInfo.left/top sont dans le même repère que le canvas.
        // Pas de translation, pas de padding, pas de clip.

        if (viewInfo == null) return;

        if (mode == Mode.BLUEPRINT) {
            drawBlueprint(canvas, viewInfo);
        } else if (mode == Mode.BLUEPRINT_BOXES) {
            drawBlueprintBoxes(canvas, viewInfo);
        } else if (mode == Mode.DESIGN_WITH_OVERLAYS) {
            drawBlueprint(canvas, viewInfo);
        }

        if (mode == Mode.DESIGN_WITH_OVERLAYS || mode == Mode.DESIGN) {
            if (showMargins) drawMargins(canvas, viewInfo);
            if (showPadding) drawPadding(canvas, viewInfo);
        }

        if (selectedView != null) {
            drawSelection(canvas, selectedView);
        }

        if (activeGuides != null && !activeGuides.isEmpty()) {
            drawAlignmentGuides(canvas);
        }
    }

    /**
     * Dessine les guides d'alignement (lignes dashed rose, comme Android Studio).
     */
    private void drawAlignmentGuides(Canvas canvas) {
        for (int[] guide : activeGuides) {
            int type = guide[0];  // 0 = vertical (ligne verticale), 1 = horizontal
            int value = guide[1]; // position x (vertical) ou y (horizontal)
            if (type == 0) {
                // Ligne verticale sur toute la hauteur
                canvas.drawLine(value, 0, value, getHeight(), guidePaint);
            } else {
                // Ligne horizontale sur toute la largeur
                canvas.drawLine(0, value, getWidth(), value, guidePaint);
            }
        }
    }

    // ---- Dessin Blueprint simple (lignes) ----

    private void drawBlueprint(Canvas canvas, ViewInfoCollector.ViewInfo info) {
        if (info == null || !info.isVisible()) return;

        RectF r = new RectF(info.left, info.top, info.right, info.bottom);
        canvas.drawRect(r, blueprintPaint);

        if (info.width > 40 && info.height > 20) {
            canvas.drawText(info.simpleName, info.left + 4, info.top + 12, textPaint);
        }

        for (ViewInfoCollector.ViewInfo child : info.children) {
            drawBlueprint(canvas, child);
        }
    }

    // ---- Dessin Blueprint BOXES (style bp-box du HTML preview) ----

    private void drawBlueprintBoxes(Canvas canvas, ViewInfoCollector.ViewInfo info) {
        if (info == null || !info.isVisible()) return;

        RectF r = new RectF(info.left, info.top, info.right, info.bottom);

        // Fond semi-transparent
        canvas.drawRect(r, bpBoxFillPaint);
        // Bordure dashed
        canvas.drawRect(r, blueprintPaint);

        // Label : "ClassName @+id/idName"
        if (info.width > 50 && info.height > 16) {
            float density = getResources().getDisplayMetrics().density;
            String classText = info.simpleName;
            String idText = info.idName != null ? " @" + info.idName : "";

            float classW = textPaint.measureText(classText);
            float idW = bpIdTextPaint.measureText(idText);
            float totalW = classW + idW;

            if (totalW < info.width - 8) {
                // Les deux tiennent
                canvas.drawText(classText, info.left + 6 * density, info.top + info.height / 2f + 4 * density, textPaint);
                canvas.drawText(idText, info.left + 6 * density + classW, info.top + info.height / 2f + 4 * density, bpIdTextPaint);
            } else {
                // Seulement la classe
                canvas.drawText(classText, info.left + 6 * density, info.top + info.height / 2f + 4 * density, textPaint);
            }
        }

        for (ViewInfoCollector.ViewInfo child : info.children) {
            drawBlueprintBoxes(canvas, child);
        }
    }

    private void drawMargins(Canvas canvas, ViewInfoCollector.ViewInfo info) {
        if (info == null) return;

        if (info.marginLeft > 0 || info.marginTop > 0
                || info.marginRight > 0 || info.marginBottom > 0) {
            int ml = info.left - info.marginLeft;
            int mt = info.top - info.marginTop;
            int mr = info.right + info.marginRight;
            int mb = info.bottom + info.marginBottom;
            canvas.drawRect(ml, mt, mr, mb, marginPaint);
        }

        for (ViewInfoCollector.ViewInfo child : info.children) {
            drawMargins(canvas, child);
        }
    }

    private void drawPadding(Canvas canvas, ViewInfoCollector.ViewInfo info) {
        if (info == null) return;

        if (info.paddingLeft > 0 || info.paddingTop > 0
                || info.paddingRight > 0 || info.paddingBottom > 0) {
            int pl = info.left + info.paddingLeft;
            int pt = info.top + info.paddingTop;
            int pr = info.right - info.paddingRight;
            int pb = info.bottom - info.paddingBottom;
            canvas.drawRect(pl, pt, pr, pb, paddingPaint);
        }

        for (ViewInfoCollector.ViewInfo child : info.children) {
            drawPadding(canvas, child);
        }
    }

    // ---- Dessin de la sélection (handles + label + tooltip) ----

    private void drawSelection(Canvas canvas, ViewInfoCollector.ViewInfo info) {
        if (info == null) return;

        float density = getResources().getDisplayMetrics().density;

        // Fond de sélection (semi-transparent)
        RectF selRect = new RectF(info.left, info.top, info.right, info.bottom);
        canvas.drawRect(selRect, selectionFillPaint);

        // Bordure de sélection
        canvas.drawRect(selRect, selectionPaint);

        // Label de sélection (tag en haut-gauche avec coin arrondi)
        if (showSelectionLabel && info.width > 20) {
            String label = info.simpleName;
            if (info.idName != null) {
                label += " · @" + info.idName;
            }
            float padH = 7f * density;
            float padV = 3f * density;
            float textW = labelTextPaint.measureText(label);
            float labelW = textW + padH * 2;
            float labelH = labelTextPaint.getTextSize() + padV * 2;
            float labelLeft = info.left - 2 * density;
            float labelTop = info.top - labelH - 4 * density;
            if (labelTop < 0) labelTop = info.top + 2 * density; // bascule en dessous si débordement

            // Forme avec coin encoché (bas-droit)
            notchPath.reset();
            notchPath.moveTo(labelLeft, labelTop);
            notchPath.lineTo(labelLeft + labelW, labelTop);
            notchPath.lineTo(labelLeft + labelW, labelTop + labelH - 6 * density);
            notchPath.lineTo(labelLeft + labelW - 6 * density, labelTop + labelH);
            notchPath.lineTo(labelLeft, labelTop + labelH);
            notchPath.close();
            canvas.drawPath(notchPath, labelBgPaint);

            // Texte
            canvas.drawText(label, labelLeft + padH,
                    labelTop + labelH / 2f + labelTextPaint.getTextSize() / 2f - padV,
                    labelTextPaint);
        }

        // 8 handles de redimensionnement
        if (showHandles) {
            drawHandles(canvas, info);
        }

        // Tooltip de dimensions
        if (showDimensionTooltip) {
            drawDimensionTooltip(canvas, info);
        }
    }

    private void drawHandles(Canvas canvas, ViewInfoCollector.ViewInfo info) {
        float density = getResources().getDisplayMetrics().density;
        float hs = handleSizePx;
        float r = hs / 2f;

        // 4 coins (cercles)
        drawHandleCircle(canvas, info.left, info.top, r);
        drawHandleCircle(canvas, info.right, info.top, r);
        drawHandleCircle(canvas, info.left, info.bottom, r);
        drawHandleCircle(canvas, info.right, info.bottom, r);

        // 4 bords (rectangles arrondis)
        float ew = 14 * density;
        float eh = 10 * density;

        // Top center
        drawHandleRect(canvas, info.left + (info.right - info.left) / 2f,
                info.top, ew, eh, true);
        // Bottom center
        drawHandleRect(canvas, info.left + (info.right - info.left) / 2f,
                info.bottom, ew, eh, true);
        // Left center
        drawHandleRect(canvas, info.left,
                info.top + (info.bottom - info.top) / 2f, eh, ew, false);
        // Right center
        drawHandleRect(canvas, info.right,
                info.top + (info.bottom - info.top) / 2f, eh, ew, false);
    }

    private void drawHandleCircle(Canvas canvas, float cx, float cy, float r) {
        canvas.drawCircle(cx, cy, r, handleFillPaint);
        canvas.drawCircle(cx, cy, r, handleStrokePaint);
    }

    private void drawHandleRect(Canvas canvas, float cx, float cy,
                                float w, float h, boolean horizontal) {
        float left, top, right, bottom;
        if (horizontal) {
            left = cx - w / 2f;
            right = cx + w / 2f;
            top = cy - h / 2f;
            bottom = cy + h / 2f;
        } else {
            // vertical (rotated 90°)
            left = cx - h / 2f;
            right = cx + h / 2f;
            top = cy - w / 2f;
            bottom = cy + w / 2f;
        }
        RectF r = new RectF(left, top, right, bottom);
        float radius = 3f * getResources().getDisplayMetrics().density;
        canvas.drawRoundRect(r, radius, radius, handleFillPaint);
        canvas.drawRoundRect(r, radius, radius, handleStrokePaint);
    }

    private void drawDimensionTooltip(Canvas canvas, ViewInfoCollector.ViewInfo info) {
        float density = getResources().getDisplayMetrics().density;
        String text = info.width + " × " + info.height + " px";
        float padH = 10f * density;
        float padV = 5f * density;
        float textW = tooltipTextPaint.measureText(text);
        float boxW = textW + padH * 2;
        float boxH = tooltipTextPaint.getTextSize() + padV * 2;

        float cx = info.left + (info.right - info.left) / 2f;
        float boxLeft = cx - boxW / 2f;
        float boxTop = info.bottom + 8f * density;

        // Clamp horizontal
        if (boxLeft < 4 * density) boxLeft = 4 * density;
        if (boxLeft + boxW > getWidth() - 4 * density) {
            boxLeft = getWidth() - boxW - 4 * density;
        }

        RectF rect = new RectF(boxLeft, boxTop, boxLeft + boxW, boxTop + boxH);
        float radius = 7f * density;
        canvas.drawRoundRect(rect, radius, radius, tooltipBgPaint);
        canvas.drawRoundRect(rect, radius, radius, tooltipBorderPaint);

        canvas.drawText(text, boxLeft + padH,
                boxTop + boxH / 2f + tooltipTextPaint.getTextSize() / 2f - padV,
                tooltipTextPaint);
    }

    // ---- Touch handling : tap pour sélectionner, drag sur handles, drag-to-move ----

    private static final int HIT_RADIUS_DP = 16;
    private static final int MOVE_THRESHOLD_DP = 4;

    private boolean moving = false;
    private float moveStartX, moveStartY;
    private int moveStartLeft, moveStartTop;
    private OnMoveListener moveListener;

    // ── Valeurs finales du drag (transmises à onHandleDragEnd / onMoveEnd) ──
    // Sans ces champs, on ne transmettrait que info.width/height (immutable
    // pré-drag) — l'édition utilisateur serait silencieusement annulée au
    // prochain rendu.
    private int lastDragW = 0, lastDragH = 0;
    private int lastMoveLeft = 0, lastMoveTop = 0;

    /** Snap à la grille (désactivé par défaut). */
    private boolean snapToGrid = true;
    private int gridSizePx = 8; // 8dp par défaut, converti en px dans init()

    /** Guides d'alignement (comme Android Studio). */
    private boolean showAlignmentGuides = true;
    private int[] alignmentGuides = null; // {type, start, end, axis} — computed during drag
    private Paint guidePaint;
    private java.util.List<int[]> activeGuides = null; // guides actifs pendant le drag

    // ---- Device frame ----
    // Désormais dessiné par {@link DeviceFrameView} (vue sœur placée derrière
    // le root dans previewContainer). L'OverlayView ne dessine QUE les overlays
    // (sélection, handles, guides, marges, padding).
    private boolean showDeviceFrame = false;
    private Paint frameBgPaint;        // fond #15152A
    private Paint frameBorderPaint;    // bordure 1px outline-2
    private Paint frameGlowPaint;      // glow violet 6px
    private float frameCornerDp = 22;
    private float frameGlowDp = 6;
    private float framePaddingDp = 2; // padding intérieur du frame (minimal)
    private boolean showCanvasGrid = true; // grille à l'intérieur du device frame
    private int gridStepDp = 8;        // pas de la grille (8dp)
    private int framePaddingDebugColor = Color.parseColor("#33FF0000"); // rouge semi-transparent pour debug

    // ---- Device profile (modèle d'appareil) ----
    private DeviceProfile deviceProfile = DeviceProfile.PIXEL_4;

    // ---- Zoom (boutons + pinch) ----
    private float zoom = 1.0f;
    private static final float MIN_ZOOM = 0.25f;
    private static final float MAX_ZOOM = 4.0f;
    private ScaleGestureDetector scaleDetector;
    private float panX = 0f, panY = 0f;
    private float touchStartX, touchStartY;
    private float panStartX, panStartY;
    private boolean isPanning = false;

    /**
     * Listener de déplacement d'une vue (drag-to-move).
     */
    public interface OnMoveListener {
        /**
         * Appelé pendant le drag-to-move.
         *
         * @param info    les ViewInfo de la vue déplacée
         * @param newLeft la nouvelle position left en px
         * @param newTop  la nouvelle position top en px
         */
        void onMove(ViewInfoCollector.ViewInfo info, int newLeft, int newTop);

        /**
         * Appelé quand le déplacement se termine.
         *
         * <p><strong>Important :</strong> {@code finalLeft} et {@code finalTop}
         * reflètent la position réellement obtenue à la fin du drag, à écrire
         * dans le XML (en tant que marges). Écrire {@code info.marginLeft/marginTop}
         * (immutable et pré-drag) annulerait silencieusement l'édition au prochain
         * rendu.</p>
         */
        default void onMoveEnd(ViewInfoCollector.ViewInfo info,
                               int finalLeft, int finalTop) {}
    }

    public void setOnMoveListener(OnMoveListener l) {
        this.moveListener = l;
    }

    /**
     * Active/désactive le snap à la grille pendant le drag.
     *
     * @param snap true pour activer le snap (défaut : true)
     */
    public void setSnapToGrid(boolean snap) {
        this.snapToGrid = snap;
    }

    /**
     * Définit la taille de la grille pour le snap.
     *
     * @param gridDp la taille en dp (défaut : 8)
     */
    public void setGridSizeDp(int gridDp) {
        float density = getResources().getDisplayMetrics().density;
        this.gridSizePx = (int) (gridDp * density);
    }

    /**
     * Snap une valeur au multiple le plus proche de gridSizePx.
     */
    private int snapValue(int value) {
        if (!snapToGrid || gridSizePx <= 0) return value;
        return Math.round((float) value / gridSizePx) * gridSizePx;
    }

    /**
     * Active/désactive les guides d'alignement.
     */
    public void setShowAlignmentGuides(boolean show) {
        this.showAlignmentGuides = show;
        invalidate();
    }

    /**
     * Calcule les guides d'alignement pendant le drag-to-move.
     * Compare le left/right/center/top/bottom/center de la vue déplacée
     * avec ceux de toutes les autres vues (frères + parent).
     *
     * @param info    la vue en cours de déplacement
     * @param newLeft la nouvelle position left
     * @param newTop  la nouvelle position top
     * @return les guides à dessiner : tableau de {type, value, ...} ou null
     */
    private java.util.List<int[]> computeAlignmentGuides(
            ViewInfoCollector.ViewInfo info, int newLeft, int newTop) {
        if (!showAlignmentGuides || viewInfo == null || info == null) return null;
        java.util.List<int[]> guides = new java.util.ArrayList<>();
        int newRight = newLeft + info.width;
        int newBottom = newTop + info.height;
        int newCenterX = newLeft + info.width / 2;
        int newCenterY = newTop + info.height / 2;
        float density = getResources().getDisplayMetrics().density;
        int threshold = (int) (3 * density); // 3dp tolerance

        // Parcourir toutes les vues (récursif)
        checkAlignmentWithTree(viewInfo, info, newLeft, newTop, newRight, newBottom,
                newCenterX, newCenterY, threshold, guides);
        return guides;
    }

    private void checkAlignmentWithTree(ViewInfoCollector.ViewInfo current,
                                          ViewInfoCollector.ViewInfo dragging,
                                          int newLeft, int newTop, int newRight, int newBottom,
                                          int newCenterX, int newCenterY, int threshold,
                                          java.util.List<int[]> guides) {
        if (current == null || current == dragging) {
            // Skip, mais recurse children
            if (current != null) {
                for (ViewInfoCollector.ViewInfo child : current.children) {
                    checkAlignmentWithTree(child, dragging, newLeft, newTop,
                            newRight, newBottom, newCenterX, newCenterY, threshold, guides);
                }
            }
            return;
        }

        // Vertical guides (alignement horizontal)
        if (Math.abs(current.left - newLeft) <= threshold) {
            guides.add(new int[]{0, current.left, newTop, current.top}); // left-left
        }
        if (Math.abs(current.right - newRight) <= threshold) {
            guides.add(new int[]{0, current.right, newTop, current.top}); // right-right
        }
        int curCenterX = current.left + current.width / 2;
        if (Math.abs(curCenterX - newCenterX) <= threshold) {
            guides.add(new int[]{0, curCenterX, newTop, current.top}); // center-center
        }

        // Horizontal guides (alignement vertical)
        if (Math.abs(current.top - newTop) <= threshold) {
            guides.add(new int[]{1, current.top, newLeft, current.left}); // top-top
        }
        if (Math.abs(current.bottom - newBottom) <= threshold) {
            guides.add(new int[]{1, current.bottom, newLeft, current.left}); // bottom-bottom
        }
        int curCenterY = current.top + current.height / 2;
        if (Math.abs(curCenterY - newCenterY) <= threshold) {
            guides.add(new int[]{1, curCenterY, newLeft, current.left}); // center-center
        }

        for (ViewInfoCollector.ViewInfo child : current.children) {
            checkAlignmentWithTree(child, dragging, newLeft, newTop,
                    newRight, newBottom, newCenterX, newCenterY, threshold, guides);
        }
    }

    // onInterceptTouchEvent supprimé — l'overlay est un FRÈRE du root, pas son parent.
    // L'overlay reçoit les touch events directement car il est au-dessus du root.

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Pinch-to-zoom en priorité
        scaleDetector.onTouchEvent(event);
        if (scaleDetector.isInProgress()) {
            // Pendant le pinch, on ne gère pas la sélection/drag
            return true;
        }

        float x = event.getX();
        float y = event.getY();
        float density = getResources().getDisplayMetrics().density;
        float hitR = HIT_RADIUS_DP * density;

        // Pas de compensation d'offset — l'overlay est au même repère que le root.

        // Demander au parent (ScrollView) de ne pas intercepter pendant le drag
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            ViewParent p = getParent();
            if (p != null) p.requestDisallowInterceptTouchEvent(true);
        }

        // Pan (quand zoom > 1 et pas de handle/drag actif)
        if (zoom > 1.0f && !dragging && !moving && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            isPanning = true;
            touchStartX = x;
            touchStartY = y;
            panStartX = panX;
            panStartY = panY;
        }
        if (isPanning && event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            panX = panStartX + (x - touchStartX);
            panY = panStartY + (y - touchStartY);
            applyZoom();
            return true;
        }
        if (isPanning && (event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL)) {
            isPanning = false;
            return true;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (selectedView != null && showHandles) {
                    HandleDirection dir = hitTestHandle(selectedView, x, y, hitR);
                    if (dir != null) {
                        dragging = true;
                        dragDir = dir;
                        dragStartX = x;
                        dragStartY = y;
                        dragStartW = selectedView.width;
                        dragStartH = selectedView.height;
                        return true;
                    }
                }
                // Vérifier si on tape à l'intérieur de la vue sélectionnée (drag-to-move)
                if (selectedView != null && isInsideView(selectedView, x, y)) {
                    moving = true;
                    moveStartX = x;
                    moveStartY = y;
                    moveStartLeft = selectedView.left;
                    moveStartTop = selectedView.top;
                    return true;
                }
                // Sinon : tap pour sélectionner
                if (viewInfo != null && selectionListener != null) {
                    ViewInfoCollector.ViewInfo hit = hitTestView(viewInfo, x, y);
                    if (hit != null) {
                        selectedView = hit;
                        invalidate();
                        selectionListener.onSelectionChanged(hit);
                    } else {
                        selectedView = null;
                        invalidate();
                        selectionListener.onSelectionChanged(null);
                    }
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (dragging && selectedView != null && handleDragListener != null) {
                    // ── Coordonnées tactiles en "view px" ──────────────────────────
                    // L'OverlayView n'est plus scalée directement — c'est son parent
                    // (previewContainer) qui l'est. Android fournit donc déjà les
                    // coordonnées tactiles dans l'espace local non-scalé de l'overlay.
                    // Pas besoin de diviser par zoom.
                    float dx = x - dragStartX;
                    float dy = y - dragStartY;
                    int newW = dragStartW;
                    int newH = dragStartH;
                    switch (dragDir) {
                        case TL:
                            newW = dragStartW - (int) dx;
                            newH = dragStartH - (int) dy;
                            break;
                        case TR:
                            newW = dragStartW + (int) dx;
                            newH = dragStartH - (int) dy;
                            break;
                        case BL:
                            newW = dragStartW - (int) dx;
                            newH = dragStartH + (int) dy;
                            break;
                        case BR:
                            newW = dragStartW + (int) dx;
                            newH = dragStartH + (int) dy;
                            break;
                        case LEFT:
                            newW = dragStartW - (int) dx;
                            break;
                        case RIGHT:
                            newW = dragStartW + (int) dx;
                            break;
                        case TOP:
                            newH = dragStartH - (int) dy;
                            break;
                        case BOTTOM:
                            newH = dragStartH + (int) dy;
                            break;
                    }
                    // Clamp aux limites du device frame
                    float pad = framePaddingDp * density;
                    int maxW = (int) (getWidth() - 2 * pad);
                    int maxH = (int) (getHeight() - 2 * pad);
                    newW = Math.max(40, Math.min(maxW, newW));
                    newH = Math.max(36, Math.min(maxH, newH));
                    // Snap à la grille
                    newW = snapValue(newW);
                    newH = snapValue(newH);
                    // Tracker les valeurs finales pour onHandleDragEnd
                    lastDragW = newW;
                    lastDragH = newH;
                    handleDragListener.onHandleDrag(selectedView, dragDir, newW, newH);
                    invalidate();
                    return true;
                }
                // Drag-to-move
                if (moving && selectedView != null && moveListener != null) {
                    // ── Coordonnées tactiles en "view px" ──────────────────────────
                    // Voir commentaire dans le bloc drag ci-dessus — l'overlay n'est
                    // plus scalée directement, c'est son parent.
                    float dx = x - moveStartX;
                    float dy = y - moveStartY;
                    int newLeft = moveStartLeft + (int) dx;
                    int newTop = moveStartTop + (int) dy;
                    // Snap à la grille
                    newLeft = snapValue(newLeft);
                    newTop = snapValue(newTop);
                    // Clamp aux limites du device frame
                    float pad = framePaddingDp * density;
                    int minLeft = (int) pad;
                    int minTop = (int) pad;
                    int maxLeft = (int) (getWidth() - pad - selectedView.width);
                    int maxTop = (int) (getHeight() - pad - selectedView.height);
                    newLeft = Math.max(minLeft, Math.min(maxLeft, newLeft));
                    newTop = Math.max(minTop, Math.min(maxTop, newTop));
                    // Calculer les guides d'alignement
                    activeGuides = computeAlignmentGuides(selectedView, newLeft, newTop);
                    // Tracker les valeurs finales pour onMoveEnd
                    lastMoveLeft = newLeft;
                    lastMoveTop = newTop;
                    moveListener.onMove(selectedView, newLeft, newTop);
                    invalidate();
                    return true;
                }
                return false;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Effacer les guides d'alignement
                activeGuides = null;
                if (dragging) {
                    dragging = false;
                    if (handleDragListener != null && selectedView != null) {
                        // ── Passer les valeurs FINALES (pas info.width/height) ──
                        // Sinon l'édition est silencieusement annulée au prochain rendu.
                        handleDragListener.onHandleDragEnd(selectedView, lastDragW, lastDragH);
                    }
                    invalidate();
                    return true;
                }
                if (moving) {
                    moving = false;
                    if (moveListener != null && selectedView != null) {
                        // ── Passer les valeurs FINALES (pas info.marginLeft/Top) ──
                        moveListener.onMoveEnd(selectedView, lastMoveLeft, lastMoveTop);
                    }
                    invalidate();
                    return true;
                }
                return false;
        }
        return super.onTouchEvent(event);
    }

    /**
     * Vérifie si un point (x, y) est à l'intérieur d'une vue.
     */
    private boolean isInsideView(ViewInfoCollector.ViewInfo info, float x, float y) {
        return x >= info.left && x <= info.right && y >= info.top && y <= info.bottom;
    }

    private HandleDirection hitTestHandle(ViewInfoCollector.ViewInfo info, float x, float y, float r) {
        float l = info.left, t = info.top, rr = info.right, b = info.bottom;
        float cx = l + (rr - l) / 2f;
        float cy = t + (b - t) / 2f;
        if (dist(x, y, l, t) <= r) return HandleDirection.TL;
        if (dist(x, y, rr, t) <= r) return HandleDirection.TR;
        if (dist(x, y, l, b) <= r) return HandleDirection.BL;
        if (dist(x, y, rr, b) <= r) return HandleDirection.BR;
        if (Math.abs(x - cx) <= r && Math.abs(y - t) <= r) return HandleDirection.TOP;
        if (Math.abs(x - cx) <= r && Math.abs(y - b) <= r) return HandleDirection.BOTTOM;
        if (Math.abs(x - l) <= r && Math.abs(y - cy) <= r) return HandleDirection.LEFT;
        if (Math.abs(x - rr) <= r && Math.abs(y - cy) <= r) return HandleDirection.RIGHT;
        return null;
    }

    private float dist(float x1, float y1, float x2, float y2) {
        return (float) Math.hypot(x2 - x1, y2 - y1);
    }

    private ViewInfoCollector.ViewInfo hitTestView(ViewInfoCollector.ViewInfo info, float x, float y) {
        if (info == null || !info.isVisible()) return null;
        if (x < info.left || x > info.right || y < info.top || y > info.bottom) return null;
        // Chercher dans les enfants d'abord (plus profond = prioritaire)
        for (int i = info.children.size() - 1; i >= 0; i--) {
            ViewInfoCollector.ViewInfo childHit = hitTestView(info.children.get(i), x, y);
            if (childHit != null) return childHit;
        }
        return info;
    }

}
