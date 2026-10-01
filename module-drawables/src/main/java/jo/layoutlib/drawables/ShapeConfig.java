package jo.layoutlib.drawables;

/**
 * Configuration d'un drawable de type <shape>.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ShapeConfig {

    private ShapeType shapeType = ShapeType.RECTANGLE;
    private Integer solidColor;
    private Integer gradientStartColor;
    private Integer gradientCenterColor;
    private Integer gradientEndColor;
    private int gradientAngle = 0;
    private GradientType gradientType = GradientType.LINEAR;
    private float gradientRadius = 0f;
    private float cornerRadius = 0f;
    private float topLeftRadius = 0f;
    private float topRightRadius = 0f;
    private float bottomLeftRadius = 0f;
    private float bottomRightRadius = 0f;
    private float strokeWidth = 0f;
    private Integer strokeColor;
    private float strokeDashWidth = 0f;
    private float strokeDashGap = 0f;
    private float paddingLeft = 0f;
    private float paddingTop = 0f;
    private float paddingRight = 0f;
    private float paddingBottom = 0f;

    public ShapeConfig() {
    }

    public ShapeType getShapeType() { return shapeType; }
    public void setShapeType(ShapeType shapeType) { this.shapeType = shapeType; }
    public Integer getSolidColor() { return solidColor; }
    public void setSolidColor(Integer solidColor) { this.solidColor = solidColor; }
    public Integer getGradientStartColor() { return gradientStartColor; }
    public void setGradientStartColor(Integer c) { this.gradientStartColor = c; }
    public Integer getGradientCenterColor() { return gradientCenterColor; }
    public void setGradientCenterColor(Integer c) { this.gradientCenterColor = c; }
    public Integer getGradientEndColor() { return gradientEndColor; }
    public void setGradientEndColor(Integer c) { this.gradientEndColor = c; }
    public int getGradientAngle() { return gradientAngle; }
    public void setGradientAngle(int angle) { this.gradientAngle = angle; }
    public GradientType getGradientType() { return gradientType; }
    public void setGradientType(GradientType t) { this.gradientType = t; }
    public float getGradientRadius() { return gradientRadius; }
    public void setGradientRadius(float r) { this.gradientRadius = r; }
    public float getCornerRadius() { return cornerRadius; }
    public void setCornerRadius(float r) { this.cornerRadius = r; }
    public float getTopLeftRadius() { return topLeftRadius; }
    public void setTopLeftRadius(float r) { this.topLeftRadius = r; }
    public float getTopRightRadius() { return topRightRadius; }
    public void setTopRightRadius(float r) { this.topRightRadius = r; }
    public float getBottomLeftRadius() { return bottomLeftRadius; }
    public void setBottomLeftRadius(float r) { this.bottomLeftRadius = r; }
    public float getBottomRightRadius() { return bottomRightRadius; }
    public void setBottomRightRadius(float r) { this.bottomRightRadius = r; }
    public float getStrokeWidth() { return strokeWidth; }
    public void setStrokeWidth(float w) { this.strokeWidth = w; }
    public Integer getStrokeColor() { return strokeColor; }
    public void setStrokeColor(Integer c) { this.strokeColor = c; }
    public float getStrokeDashWidth() { return strokeDashWidth; }
    public void setStrokeDashWidth(float w) { this.strokeDashWidth = w; }
    public float getStrokeDashGap() { return strokeDashGap; }
    public void setStrokeDashGap(float g) { this.strokeDashGap = g; }
    public float getPaddingLeft() { return paddingLeft; }
    public void setPaddingLeft(float p) { this.paddingLeft = p; }
    public float getPaddingTop() { return paddingTop; }
    public void setPaddingTop(float p) { this.paddingTop = p; }
    public float getPaddingRight() { return paddingRight; }
    public void setPaddingRight(float p) { this.paddingRight = p; }
    public float getPaddingBottom() { return paddingBottom; }
    public void setPaddingBottom(float p) { this.paddingBottom = p; }

    public boolean hasGradient() {
        return gradientStartColor != null || gradientEndColor != null;
    }
    public boolean hasStroke() {
        return strokeWidth > 0 && strokeColor != null;
    }
    public boolean hasCorners() {
        return cornerRadius > 0 || topLeftRadius > 0 || topRightRadius > 0
                || bottomLeftRadius > 0 || bottomRightRadius > 0;
    }
    public boolean hasPadding() {
        return paddingLeft > 0 || paddingTop > 0 || paddingRight > 0 || paddingBottom > 0;
    }
    public float[] getCornerRadii() {
        float tl = topLeftRadius > 0 ? topLeftRadius : cornerRadius;
        float tr = topRightRadius > 0 ? topRightRadius : cornerRadius;
        float br = bottomRightRadius > 0 ? bottomRightRadius : cornerRadius;
        float bl = bottomLeftRadius > 0 ? bottomLeftRadius : cornerRadius;
        return new float[]{tl, tr, br, bl};
    }
}
