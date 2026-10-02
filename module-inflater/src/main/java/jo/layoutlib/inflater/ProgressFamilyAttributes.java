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
 * Applier d attributs pour la famille ProgressFamily.
 *
 * <p>Extrait de AttributeApplier (découpage point 8) : le comportement
 * est inchangé — l ordre des vérifications instanceof est identique à
 * l ancienne chaîne du dispatch.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
class ProgressFamilyAttributes implements WidgetAttributes {

    /** Applier parent (helpers de résolution et de parsing). */
    private final AttributeApplier applier;

    ProgressFamilyAttributes(AttributeApplier applier) {
        this.applier = applier;
    }

    @Override
    public void apply(View view, XmlPullParser parser) {
        if (view instanceof ProgressBar) {
            applyProgressBarAttributes((ProgressBar) view, parser);
        }
        if (view instanceof AbsSeekBar) {
            applySeekBarAttributes((AbsSeekBar) view, parser);
        }
        if (view instanceof RatingBar) {
            applyRatingBarAttributes((RatingBar) view, parser);
        }
    }

    void applyProgressBarAttributes(ProgressBar pb, XmlPullParser parser) {
        String max = applier.getAttr(parser, "max");
        if (max != null) { try { pb.setMax(Integer.parseInt(max)); } catch (NumberFormatException e) { applier.logAttrError("max", e); } }

        String progress = applier.getAttr(parser, "progress");
        if (progress != null) { try { pb.setProgress(Integer.parseInt(progress)); } catch (NumberFormatException e) { applier.logAttrError("progress", e); } }

        String secondaryProgress = applier.getAttr(parser, "secondaryProgress");
        if (secondaryProgress != null) { try { pb.setSecondaryProgress(Integer.parseInt(secondaryProgress)); } catch (NumberFormatException e) { applier.logAttrError("secondaryProgress", e); } }

        String progressDrawable = applier.getAttr(parser, "progressDrawable");
        if (progressDrawable != null) {
            Drawable pd = applier.resolveDrawable(progressDrawable);
            if (pd != null) pb.setProgressDrawable(pd);
        }

        String indeterminate = applier.getAttr(parser, "indeterminate");
        if (indeterminate != null) pb.setIndeterminate("true".equals(indeterminate));

        String indeterminateDrawable = applier.getAttr(parser, "indeterminateDrawable");
        if (indeterminateDrawable != null) {
            Drawable id = applier.resolveDrawable(indeterminateDrawable);
            if (id != null) pb.setIndeterminateDrawable(id);
        }

        String progressTint = applier.getAttr(parser, "progressTint");
        if (progressTint != null) { try { pb.setProgressTintList(android.content.res.ColorStateList.valueOf(ColorParser.parse(progressTint))); } catch (RuntimeException e) { applier.logAttrError("progressTint", e); } }

        String min = applier.getAttr(parser, "min");
        // ProgressBar#setMin n existe qu à partir de l API 26 (minSdk = 24).
        if (min != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try { pb.setMin(Integer.parseInt(min)); } catch (NumberFormatException e) { applier.logAttrError("min", e); }
        }
    }

    void applySeekBarAttributes(AbsSeekBar sb, XmlPullParser parser) {
        String thumb = applier.getAttr(parser, "thumb");
        if (thumb != null) {
            Drawable thumbDrawable = applier.resolveDrawable(thumb);
            if (thumbDrawable != null) sb.setThumb(thumbDrawable);
        }

        String splitTrack = applier.getAttr(parser, "splitTrack");
        if (splitTrack != null) sb.setSplitTrack("true".equals(splitTrack));

        String thumbOffset = applier.getAttr(parser, "thumbOffset");
        if (thumbOffset != null) { try { sb.setThumbOffset(applier.parseDim(thumbOffset)); } catch (RuntimeException e) { applier.logAttrError("thumbOffset", e); } }
    }

    void applyRatingBarAttributes(RatingBar rb, XmlPullParser parser) {
        String numStars = applier.getAttr(parser, "numStars");
        if (numStars != null) { try { rb.setNumStars(Integer.parseInt(numStars)); } catch (NumberFormatException e) { applier.logAttrError("numStars", e); } }

        String rating = applier.getAttr(parser, "rating");
        if (rating != null) { try { rb.setRating(Float.parseFloat(rating)); } catch (NumberFormatException e) { applier.logAttrError("rating", e); } }

        String stepSize = applier.getAttr(parser, "stepSize");
        if (stepSize != null) { try { rb.setStepSize(Float.parseFloat(stepSize)); } catch (NumberFormatException e) { applier.logAttrError("stepSize", e); } }

        String isIndicator = applier.getAttr(parser, "isIndicator");
        if (isIndicator != null) rb.setIsIndicator("true".equals(isIndicator));
    }
}
