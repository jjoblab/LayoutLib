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
 * Applier d attributs pour la famille AdapterFamily.
 *
 * <p>Extrait de AttributeApplier (découpage point 8) : le comportement
 * est inchangé — l ordre des vérifications instanceof est identique à
 * l ancienne chaîne du dispatch.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
class AdapterFamilyAttributes implements WidgetAttributes {

    /** Applier parent (helpers de résolution et de parsing). */
    private final AttributeApplier applier;

    AdapterFamilyAttributes(AttributeApplier applier) {
        this.applier = applier;
    }

    @Override
    public void apply(View view, XmlPullParser parser) {
        if (view instanceof Spinner) {
            applySpinnerAttributes((Spinner) view, parser);
        }
        if (view instanceof AdapterView) {
            applyAdapterViewAttributes((AdapterView<?>) view, parser);
        }
        if (view instanceof ListView) {
            applyListViewAttributes((ListView) view, parser);
        }
        if (view instanceof GridView) {
            applyGridViewAttributes((GridView) view, parser);
        }
    }

    void applySpinnerAttributes(Spinner sp, XmlPullParser parser) {
        String spinnerMode = applier.getAttr(parser, "spinnerMode");
        // Mode dialog vs dropdown — non géré en prévisualisation

        String prompt = applier.getAttr(parser, "prompt");
        if (prompt != null) {
            // Littéral ou @string/ — resolveString gère les deux
            String promptText = applier.resolveString(prompt);
            if (promptText == null && !prompt.startsWith("@")) {
                promptText = prompt;
            }
            if (promptText != null) sp.setPrompt(promptText);
        }
    }

    void applyAdapterViewAttributes(AdapterView<?> av, XmlPullParser parser) {
        String entries = applier.getAttr(parser, "entries");
        if (entries != null) {
            java.util.List<String> items = applier.resolveStringArray(entries);
            if (items != null && !items.isEmpty()) {
                android.widget.ArrayAdapter<String> adapter =
                        new android.widget.ArrayAdapter<>(applier.context,
                                android.R.layout.simple_list_item_1, items);
                if (av instanceof Spinner) {
                    adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item);
                }
                ((AdapterView) av).setAdapter(adapter);
            } else {
                Debug.logWarning("resources",
                        "entries non résolu (" + entries + ") — liste vide");
            }
        }

        String divider = applier.getAttr(parser, "divider");
        if (divider != null && av instanceof ListView) {
            Drawable div = applier.resolveDrawable(divider);
            if (div != null) ((ListView) av).setDivider(div);
        }

        String dividerHeight = applier.getAttr(parser, "dividerHeight");
        if (dividerHeight != null && av instanceof ListView) {
            ((ListView) av).setDividerHeight(applier.parseDim(dividerHeight));
        }
    }

    void applyListViewAttributes(ListView lv, XmlPullParser parser) {
        String choiceMode = applier.getAttr(parser, "choiceMode");
        if (choiceMode != null) {
            switch (choiceMode) {
                case "singleChoice": lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE); break;
                case "multipleChoice": lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE); break;
                case "none": lv.setChoiceMode(ListView.CHOICE_MODE_NONE); break;
            }
        }

        String fastScrollEnabled = applier.getAttr(parser, "fastScrollEnabled");
        if (fastScrollEnabled != null) lv.setFastScrollEnabled("true".equals(fastScrollEnabled));

        String scrollbars = applier.getAttr(parser, "scrollbars");
        // Déjà géré dans View
    }

    void applyGridViewAttributes(GridView gv, XmlPullParser parser) {
        String numColumns = applier.getAttr(parser, "numColumns");
        if (numColumns != null) {
            if ("auto_fit".equals(numColumns)) {
                gv.setNumColumns(GridView.AUTO_FIT);
            } else {
                try { gv.setNumColumns(Integer.parseInt(numColumns)); } catch (NumberFormatException e) { applier.logAttrError("numColumns", e); }
            }
        }

        String columnWidth = applier.getAttr(parser, "columnWidth");
        if (columnWidth != null) gv.setColumnWidth(applier.parseDim(columnWidth));

        String horizontalSpacing = applier.getAttr(parser, "horizontalSpacing");
        if (horizontalSpacing != null) gv.setHorizontalSpacing(applier.parseDim(horizontalSpacing));

        String verticalSpacing = applier.getAttr(parser, "verticalSpacing");
        if (verticalSpacing != null) gv.setVerticalSpacing(applier.parseDim(verticalSpacing));

        String stretchMode = applier.getAttr(parser, "stretchMode");
        if (stretchMode != null) {
            switch (stretchMode) {
                case "none": gv.setStretchMode(GridView.NO_STRETCH); break;
                case "spacingWidth": gv.setStretchMode(GridView.STRETCH_SPACING); break;
                case "columnWidth": gv.setStretchMode(GridView.STRETCH_COLUMN_WIDTH); break;
                case "spacingWidthUniform": gv.setStretchMode(GridView.STRETCH_SPACING_UNIFORM); break;
            }
        }

        String gravity = applier.getAttr(parser, "gravity");
        if (gravity != null) gv.setGravity(applier.parseGravity(gravity));
    }
}
