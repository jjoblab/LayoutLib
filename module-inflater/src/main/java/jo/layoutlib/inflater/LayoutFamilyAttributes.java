package jo.layoutlib.inflater;

import android.content.Context;
import android.os.Build;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsSeekBar;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;
import android.widget.ViewAnimator;
import android.widget.ViewFlipper;
import android.widget.ViewSwitcher;
import android.widget.ZoomButton;

import org.xmlpull.v1.XmlPullParser;

import jo.layoutlib.drawables.DrawableResolver;
import jo.layoutlib.inflater.bridge.util.Debug;
import jo.layoutlib.resources.ColorParser;
import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.resources.ResourceException;
import jo.layoutlib.resources.ResourceResolver;
import jo.layoutlib.themes.ThemeResolver;

/**
 * Applique les attributs XML aux vues via leurs setters natifs.
 *
 * <p>Cette classe est l'équivalent on-device du {@code BridgeTypedArray} de
 * l'AOSP layoutlib. Elle lit tous les attributs {@code android:*} depuis le
 * {@code XmlPullParser} et les applique via les setters natifs des vues.</p>
 *
 * <p>Supporte la résolution de références {@code @color/}, {@code @string/},
 * {@code @dimen/}, {@code @array/} et {@code ?attr/} via un
 * {@link ResourceResolver} connecté, et {@code @drawable/} via un
 * {@link DrawableResolver} connecté (shapes, selectors, vectors) avec
 * repli sur les {@code Resources} natives.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
/**
 * Applier d attributs pour la famille LayoutFamily.
 *
 * <p>Extrait de AttributeApplier (découpage point 8) : le comportement
 * est inchangé — l ordre des vérifications instanceof est identique à
 * l ancienne chaîne du dispatch.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
class LayoutFamilyAttributes implements WidgetAttributes {

    /** Applier parent (helpers de résolution et de parsing). */
    private final AttributeApplier applier;

    LayoutFamilyAttributes(AttributeApplier applier) {
        this.applier = applier;
    }

    @Override
    public void apply(View view, XmlPullParser parser) {
        if (view instanceof LinearLayout) {
            applyLinearLayoutAttributes((LinearLayout) view, parser);
        }
        if (view instanceof RelativeLayout) {
            applyRelativeLayoutAttributes((RelativeLayout) view, parser);
        }
        if (view instanceof FrameLayout) {
            applyFrameLayoutAttributes((FrameLayout) view, parser);
        }
        if (view instanceof GridLayout) {
            applyGridLayoutAttributes((GridLayout) view, parser);
        }
        if (view instanceof TableLayout) {
            applyTableLayoutAttributes((TableLayout) view, parser);
        }
        if (view instanceof TableRow) {
            applyTableRowAttributes((TableRow) view, parser);
        }
        if (view instanceof ScrollView || view instanceof HorizontalScrollView) {
            applyScrollViewAttributes((ScrollView) view, parser);
        }
    }

    void applyLinearLayoutAttributes(LinearLayout ll, XmlPullParser parser) {
        String orientation = applier.getAttr(parser, "orientation");
        if (orientation != null) {
            ll.setOrientation("horizontal".equals(orientation)
                    ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        }

        String gravity = applier.getAttr(parser, "gravity");
        if (gravity != null) ll.setGravity(applier.parseGravity(gravity));

        String weightSum = applier.getAttr(parser, "weightSum");
        if (weightSum != null) { try { ll.setWeightSum(Float.parseFloat(weightSum)); } catch (NumberFormatException e) { applier.logAttrError("weightSum", e); } }

        String baselineAligned = applier.getAttr(parser, "baselineAligned");
        if (baselineAligned != null) ll.setBaselineAligned("true".equals(baselineAligned));

        String measureWithLargestChild = applier.getAttr(parser, "measureWithLargestChild");
        if (measureWithLargestChild != null) ll.setMeasureWithLargestChildEnabled("true".equals(measureWithLargestChild));

        String divider = applier.getAttr(parser, "divider");
        if (divider != null) {
            Drawable div = applier.resolveDrawable(divider);
            if (div != null) ll.setDividerDrawable(div);
        }

        String showDividers = applier.getAttr(parser, "showDividers");
        if (showDividers != null) {
            int dividers = 0;
            if (showDividers.contains("beginning")) dividers |= LinearLayout.SHOW_DIVIDER_BEGINNING;
            if (showDividers.contains("middle")) dividers |= LinearLayout.SHOW_DIVIDER_MIDDLE;
            if (showDividers.contains("end")) dividers |= LinearLayout.SHOW_DIVIDER_END;
            ll.setShowDividers(dividers);
        }

        String dividerPadding = applier.getAttr(parser, "dividerPadding");
        if (dividerPadding != null) ll.setDividerPadding(applier.parseDim(dividerPadding));
    }

    void applyRelativeLayoutAttributes(RelativeLayout rl, XmlPullParser parser) {
        String gravity = applier.getAttr(parser, "gravity");
        if (gravity != null) rl.setGravity(applier.parseGravity(gravity));

        String ignoreGravity = applier.getAttr(parser, "ignoreGravity");
        if (ignoreGravity != null) {
            rl.setIgnoreGravity(applier.resolveViewId(ignoreGravity));
        }
    }

    void applyFrameLayoutAttributes(FrameLayout fl, XmlPullParser parser) {
        String foreground = applier.getAttr(parser, "foreground");
        if (foreground != null) applier.applyForeground(fl, foreground);

        String foregroundGravity = applier.getAttr(parser, "foregroundGravity");
        if (foregroundGravity != null) fl.setForegroundGravity(applier.parseGravity(foregroundGravity));

        String measureAllChildren = applier.getAttr(parser, "measureAllChildren");
        if (measureAllChildren != null) fl.setMeasureAllChildren("true".equals(measureAllChildren));
    }

    void applyGridLayoutAttributes(GridLayout gl, XmlPullParser parser) {
        String orientation = applier.getAttr(parser, "orientation");
        if (orientation != null) gl.setOrientation("horizontal".equals(orientation)
                ? GridLayout.HORIZONTAL : GridLayout.VERTICAL);

        String columnCount = applier.getAttr(parser, "columnCount");
        if (columnCount != null) { try { gl.setColumnCount(Integer.parseInt(columnCount)); } catch (NumberFormatException e) { applier.logAttrError("columnCount", e); } }

        String rowCount = applier.getAttr(parser, "rowCount");
        if (rowCount != null) { try { gl.setRowCount(Integer.parseInt(rowCount)); } catch (NumberFormatException e) { applier.logAttrError("rowCount", e); } }

        String useDefaultMargins = applier.getAttr(parser, "useDefaultMargins");
        if (useDefaultMargins != null) gl.setUseDefaultMargins("true".equals(useDefaultMargins));

        String rowOrderPreserved = applier.getAttr(parser, "rowOrderPreserved");
        if (rowOrderPreserved != null) gl.setRowOrderPreserved("true".equals(rowOrderPreserved));

        String columnOrderPreserved = applier.getAttr(parser, "columnOrderPreserved");
        if (columnOrderPreserved != null) gl.setColumnOrderPreserved("true".equals(columnOrderPreserved));

        String alignmentMode = applier.getAttr(parser, "alignmentMode");
        if (alignmentMode != null) {
            gl.setAlignmentMode("alignBounds".equals(alignmentMode)
                    ? GridLayout.ALIGN_BOUNDS : GridLayout.ALIGN_MARGINS);
        }
    }

    void applyTableLayoutAttributes(TableLayout tl, XmlPullParser parser) {
        String shrinkColumns = applier.getAttr(parser, "shrinkColumns");
        String stretchColumns = applier.getAttr(parser, "stretchColumns");
        String collapseColumns = applier.getAttr(parser, "collapseColumns");

        // Parsing des indices de colonnes — complexe, non géré en prévisualisation

        String collapsed = applier.getAttr(parser, "collapsed");
        // Non géré
    }

    void applyTableRowAttributes(TableRow tr, XmlPullParser parser) {
        // TableRow n'a pas d'attributs spécifiques au-delà de LinearLayout
    }

    void applyScrollViewAttributes(android.widget.ScrollView sv, XmlPullParser parser) {
        String fillViewport = applier.getAttr(parser, "fillViewport");
        if (fillViewport != null) sv.setFillViewport("true".equals(fillViewport));

        String scrollbars = applier.getAttr(parser, "scrollbars");
        // Déjà géré dans View
    }
}
