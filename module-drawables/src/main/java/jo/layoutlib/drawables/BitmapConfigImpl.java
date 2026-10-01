package jo.layoutlib.drawables;

/**
 * Configuration pour drawable de type Bitmap.
 * Wrapper utilisant l API Android native.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class BitmapConfigImpl {

    private String drawableRef;
    private int color = 0;
    private float radius = 0;
    private float width = 0;
    private float height = 0;
    private int insetLeft = 0;
    private int insetTop = 0;
    private int insetRight = 0;
    private int insetBottom = 0;
    private float fromDegrees = 0;
    private float toDegrees = 360;
    private float scaleWidth = 1.0f;
    private float scaleHeight = 1.0f;
    private int orientation = 0;
    private int gravity = 0;

    public BitmapConfigImpl() {
    }

    public String getDrawableRef() { return drawableRef; }
    public void setDrawableRef(String ref) { this.drawableRef = ref; }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }

    public float getRadius() { return radius; }
    public void setRadius(float radius) { this.radius = radius; }

    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }

    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }

    public int getInsetLeft() { return insetLeft; }
    public void setInsetLeft(int v) { this.insetLeft = v; }

    public int getInsetTop() { return insetTop; }
    public void setInsetTop(int v) { this.insetTop = v; }

    public int getInsetRight() { return insetRight; }
    public void setInsetRight(int v) { this.insetRight = v; }

    public int getInsetBottom() { return insetBottom; }
    public void setInsetBottom(int v) { this.insetBottom = v; }
}
