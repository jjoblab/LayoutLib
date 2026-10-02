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
 * Applier d attributs pour la famille MiscWidget.
 *
 * <p>Extrait de AttributeApplier (découpage point 8) : le comportement
 * est inchangé — l ordre des vérifications instanceof est identique à
 * l ancienne chaîne du dispatch.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
class MiscWidgetAttributes implements WidgetAttributes {

    /** Applier parent (helpers de résolution et de parsing). */
    private final AttributeApplier applier;

    MiscWidgetAttributes(AttributeApplier applier) {
        this.applier = applier;
    }

    @Override
    public void apply(View view, XmlPullParser parser) {
        if (view instanceof CalendarView) {
            applyCalendarViewAttributes((CalendarView) view, parser);
        }
        if (view instanceof Chronometer) {
            applyChronometerAttributes((Chronometer) view, parser);
        }
        if (view instanceof TextClock) {
            applyTextClockAttributes((TextClock) view, parser);
        }
        if (view instanceof NumberPicker) {
            applyNumberPickerAttributes((NumberPicker) view, parser);
        }
        if (view instanceof SearchView) {
            applySearchViewAttributes((SearchView) view, parser);
        }
        if (view instanceof ViewAnimator) {
            applyViewAnimatorAttributes((ViewAnimator) view, parser);
        }
        if (view instanceof DatePicker) {
            applyDatePickerAttributes((DatePicker) view, parser);
        }
        if (view instanceof TimePicker) {
            applyTimePickerAttributes((TimePicker) view, parser);
        }
    }

    void applyCalendarViewAttributes(CalendarView cv, XmlPullParser parser) {
        String firstDayOfWeek = applier.getAttr(parser, "firstDayOfWeek");
        if (firstDayOfWeek != null) { try { cv.setFirstDayOfWeek(Integer.parseInt(firstDayOfWeek)); } catch (NumberFormatException e) { applier.logAttrError("firstDayOfWeek", e); } }

        String minDate = applier.getAttr(parser, "minDate");
        // TODO: parse date

        String maxDate = applier.getAttr(parser, "maxDate");
        // TODO: parse date

        String shownWeekCount = applier.getAttr(parser, "shownWeekCount");
        if (shownWeekCount != null) { try { cv.setShownWeekCount(Integer.parseInt(shownWeekCount)); } catch (NumberFormatException e) { applier.logAttrError("shownWeekCount", e); } }
    }

    void applyChronometerAttributes(Chronometer ch, XmlPullParser parser) {
        String format = applier.getAttr(parser, "format");
        if (format != null) ch.setFormat(format);

        String countDown = applier.getAttr(parser, "countDown");
        // countDown nécessite API 29+ — non géré
    }

    void applyTextClockAttributes(TextClock tc, XmlPullParser parser) {
        String format12Hour = applier.getAttr(parser, "format12Hour");
        if (format12Hour != null) tc.setFormat12Hour(format12Hour);

        String format24Hour = applier.getAttr(parser, "format24Hour");
        if (format24Hour != null) tc.setFormat24Hour(format24Hour);

        String timeZone = applier.getAttr(parser, "timeZone");
        if (timeZone != null) tc.setTimeZone(timeZone);
    }

    void applyNumberPickerAttributes(NumberPicker np, XmlPullParser parser) {
        String minValue = applier.getAttr(parser, "minValue");
        if (minValue != null) { try { np.setMinValue(Integer.parseInt(minValue)); } catch (NumberFormatException e) { applier.logAttrError("minValue", e); } }

        String maxValue = applier.getAttr(parser, "maxValue");
        if (maxValue != null) { try { np.setMaxValue(Integer.parseInt(maxValue)); } catch (NumberFormatException e) { applier.logAttrError("maxValue", e); } }

        String value = applier.getAttr(parser, "value");
        if (value != null) { try { np.setValue(Integer.parseInt(value)); } catch (NumberFormatException e) { applier.logAttrError("value", e); } }

        String wrapSelectorWheel = applier.getAttr(parser, "wrapSelectorWheel");
        if (wrapSelectorWheel != null) np.setWrapSelectorWheel("true".equals(wrapSelectorWheel));
    }

    void applySearchViewAttributes(SearchView sv, XmlPullParser parser) {
        String queryHint = applier.getAttr(parser, "queryHint");
        if (queryHint != null) sv.setQueryHint(queryHint);

        String iconified = applier.getAttr(parser, "iconified");
        if (iconified != null) sv.setIconified("true".equals(iconified));

        String iconifiedByDefault = applier.getAttr(parser, "iconifiedByDefault");
        if (iconifiedByDefault != null) sv.setIconifiedByDefault("true".equals(iconifiedByDefault));
    }

    void applyViewAnimatorAttributes(ViewAnimator va, XmlPullParser parser) {
        String displayedChild = applier.getAttr(parser, "displayedChild");
        if (displayedChild != null) { try { va.setDisplayedChild(Integer.parseInt(displayedChild)); } catch (NumberFormatException e) { applier.logAttrError("displayedChild", e); } }

        String animateFirstView = applier.getAttr(parser, "animateFirstView");
        if (animateFirstView != null) va.setAnimateFirstView("true".equals(animateFirstView));
    }

    void applyDatePickerAttributes(DatePicker dp, XmlPullParser parser) {
        String spinnersShown = applier.getAttr(parser, "spinnersShown");
        if (spinnersShown != null) dp.setSpinnersShown("true".equals(spinnersShown));

        String calendarViewShown = applier.getAttr(parser, "calendarViewShown");
        if (calendarViewShown != null) dp.setCalendarViewShown("true".equals(calendarViewShown));

        String firstDayOfWeek = applier.getAttr(parser, "firstDayOfWeek");
        if (firstDayOfWeek != null) { try { dp.setFirstDayOfWeek(Integer.parseInt(firstDayOfWeek)); } catch (NumberFormatException e) { applier.logAttrError("firstDayOfWeek", e); } }
    }

    void applyTimePickerAttributes(TimePicker tp, XmlPullParser parser) {
        String timePickerMode = applier.getAttr(parser, "timePickerMode");
        // Mode spinner vs clock — non géré en prévisualisation

        String hour = applier.getAttr(parser, "hour");
        if (hour != null) { try { tp.setHour(Integer.parseInt(hour)); } catch (NumberFormatException e) { applier.logAttrError("hour", e); } }

        String minute = applier.getAttr(parser, "minute");
        if (minute != null) { try { tp.setMinute(Integer.parseInt(minute)); } catch (NumberFormatException e) { applier.logAttrError("minute", e); } }

        String am_pm = applier.getAttr(parser, "am_pm");
        // Non géré
    }
}
