package jo.layoutlib.drawables;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration d'un drawable de type <vector>.
 * Contient les dimensions, viewport, groupes et paths.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class VectorConfig {

    private float width;
    private float height;
    private float viewportWidth;
    private float viewportHeight;
    private Integer tint;
    private String tintMode = "SRC_IN";
    private float alpha = 1.0f;
    private boolean autoMirrored = false;
    private final List<VectorNode> nodes = new ArrayList<>();

    public VectorConfig() {
    }

    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }
    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }
    public float getViewportWidth() { return viewportWidth; }
    public void setViewportWidth(float viewportWidth) { this.viewportWidth = viewportWidth; }
    public float getViewportHeight() { return viewportHeight; }
    public void setViewportHeight(float viewportHeight) { this.viewportHeight = viewportHeight; }
    public Integer getTint() { return tint; }
    public void setTint(Integer tint) { this.tint = tint; }
    public String getTintMode() { return tintMode; }
    public void setTintMode(String tintMode) { this.tintMode = tintMode; }
    public float getAlpha() { return alpha; }
    public void setAlpha(float alpha) { this.alpha = alpha; }
    public boolean isAutoMirrored() { return autoMirrored; }
    public void setAutoMirrored(boolean autoMirrored) { this.autoMirrored = autoMirrored; }

    public void addNode(VectorNode node) {
        if (node != null) nodes.add(node);
    }

    public List<VectorNode> getNodes() {
        return new ArrayList<>(nodes);
    }

    public abstract static class VectorNode {
    }

    public static class GroupNode extends VectorNode {
        private String name;
        private float rotation = 0f;
        private float pivotX = 0f;
        private float pivotY = 0f;
        private float scaleX = 1f;
        private float scaleY = 1f;
        private float translateX = 0f;
        private float translateY = 0f;
        private final List<VectorNode> children = new ArrayList<>();

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public float getRotation() { return rotation; }
        public void setRotation(float rotation) { this.rotation = rotation; }
        public float getPivotX() { return pivotX; }
        public void setPivotX(float pivotX) { this.pivotX = pivotX; }
        public float getPivotY() { return pivotY; }
        public void setPivotY(float pivotY) { this.pivotY = pivotY; }
        public float getScaleX() { return scaleX; }
        public void setScaleX(float scaleX) { this.scaleX = scaleX; }
        public float getScaleY() { return scaleY; }
        public void setScaleY(float scaleY) { this.scaleY = scaleY; }
        public float getTranslateX() { return translateX; }
        public void setTranslateX(float translateX) { this.translateX = translateX; }
        public float getTranslateY() { return translateY; }
        public void setTranslateY(float translateY) { this.translateY = translateY; }
        public void addChild(VectorNode node) { if (node != null) children.add(node); }
        public List<VectorNode> getChildren() { return new ArrayList<>(children); }
    }

    public static class PathNode extends VectorNode {
        private String pathData;
        private Integer fillColor;
        private Integer strokeColor;
        private float strokeWidth = 0f;
        private float fillAlpha = 1.0f;
        private float strokeAlpha = 1.0f;
        private String strokeLineCap = "BUTT";
        private String strokeLineJoin = "MITER";
        private float strokeMiterLimit = 4f;
        private String fillType = "WINDING";

        public String getPathData() { return pathData; }
        public void setPathData(String pathData) { this.pathData = pathData; }
        public Integer getFillColor() { return fillColor; }
        public void setFillColor(Integer fillColor) { this.fillColor = fillColor; }
        public Integer getStrokeColor() { return strokeColor; }
        public void setStrokeColor(Integer strokeColor) { this.strokeColor = strokeColor; }
        public float getStrokeWidth() { return strokeWidth; }
        public void setStrokeWidth(float strokeWidth) { this.strokeWidth = strokeWidth; }
        public float getFillAlpha() { return fillAlpha; }
        public void setFillAlpha(float fillAlpha) { this.fillAlpha = fillAlpha; }
        public float getStrokeAlpha() { return strokeAlpha; }
        public void setStrokeAlpha(float strokeAlpha) { this.strokeAlpha = strokeAlpha; }
        public String getStrokeLineCap() { return strokeLineCap; }
        public void setStrokeLineCap(String strokeLineCap) { this.strokeLineCap = strokeLineCap; }
        public String getStrokeLineJoin() { return strokeLineJoin; }
        public void setStrokeLineJoin(String strokeLineJoin) { this.strokeLineJoin = strokeLineJoin; }
        public float getStrokeMiterLimit() { return strokeMiterLimit; }
        public void setStrokeMiterLimit(float strokeMiterLimit) { this.strokeMiterLimit = strokeMiterLimit; }
        public String getFillType() { return fillType; }
        public void setFillType(String fillType) { this.fillType = fillType; }
    }
}
