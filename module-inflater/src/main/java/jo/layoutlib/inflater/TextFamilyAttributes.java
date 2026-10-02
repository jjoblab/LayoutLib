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
 * Applier d attributs pour la famille TextFamily.
 *
 * <p>Extrait de AttributeApplier (découpage point 8) : le comportement
 * est inchangé — l ordre des vérifications instanceof est identique à
 * l ancienne chaîne du dispatch.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
class TextFamilyAttributes implements WidgetAttributes {

    /** Applier parent (helpers de résolution et de parsing). */
    private final AttributeApplier applier;

    TextFamilyAttributes(AttributeApplier applier) {
        this.applier = applier;
    }

    @Override
    public void apply(View view, XmlPullParser parser) {
        if (view instanceof CompoundButton) {
            applyCompoundButtonAttributes((CompoundButton) view, parser);
        }
        if (view instanceof TextView) {
            applyTextViewAttributes((TextView) view, parser);
        }
        if (view instanceof EditText) {
            applyEditTextAttributes((EditText) view, parser);
        }
    }

    void applyCompoundButtonAttributes(CompoundButton cb, XmlPullParser parser) {
        String checked = applier.getAttr(parser, "checked");
        if (checked != null) cb.setChecked("true".equals(checked));

        String button = applier.getAttr(parser, "button");
        if (button != null) {
            try { cb.setButtonDrawable(ColorParser.parse(button)); } catch (RuntimeException e) { applier.logAttrError("button", e); }
        }
    }

    void applyTextViewAttributes(TextView tv, XmlPullParser parser) {
        String text = applier.getAttr(parser, "text");
        if (text != null) {
            if (text.startsWith("@string/")) {
                String resolved = applier.resolveString(text);
                tv.setText(resolved != null ? resolved : text);
            } else {
                tv.setText(text);
            }
        }

        String textColor = applier.getAttr(parser, "textColor");
        if (textColor != null) applier.applyTextColor(tv, textColor);

        String textSize = applier.getAttr(parser, "textSize");
        if (textSize != null) { try { tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, applier.parseDim(textSize)); } catch (RuntimeException e) { applier.logAttrError("textSize", e); } }

        String textStyle = applier.getAttr(parser, "textStyle");
        if (textStyle != null) {
            int style = Typeface.NORMAL;
            if (textStyle.contains("bold")) style |= Typeface.BOLD;
            if (textStyle.contains("italic")) style |= Typeface.ITALIC;
            tv.setTypeface(tv.getTypeface(), style);
        }

        String gravity = applier.getAttr(parser, "gravity");
        if (gravity != null) tv.setGravity(applier.parseGravity(gravity));

        String hint = applier.getAttr(parser, "hint");
        if (hint != null) tv.setHint(hint);

        String textColorHint = applier.getAttr(parser, "textColorHint");
        if (textColorHint != null) { try { tv.setHintTextColor(ColorParser.parse(textColorHint)); } catch (RuntimeException e) { applier.logAttrError("textColorHint", e); } }

        String textAlignment = applier.getAttr(parser, "textAlignment");
        if (textAlignment != null) {
            switch (textAlignment) {
                case "center": tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER); break;
                case "textStart": tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START); break;
                case "textEnd": tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END); break;
                case "viewStart": tv.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START); break;
                case "viewEnd": tv.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END); break;
                case "inherit": tv.setTextAlignment(View.TEXT_ALIGNMENT_INHERIT); break;
            }
        }

        String maxLines = applier.getAttr(parser, "maxLines");
        if (maxLines != null) { try { tv.setMaxLines(Integer.parseInt(maxLines)); } catch (NumberFormatException e) { applier.logAttrError("maxLines", e); } }

        String minLines = applier.getAttr(parser, "minLines");
        if (minLines != null) { try { tv.setMinLines(Integer.parseInt(minLines)); } catch (NumberFormatException e) { applier.logAttrError("minLines", e); } }

        String lines = applier.getAttr(parser, "lines");
        if (lines != null) { try { tv.setLines(Integer.parseInt(lines)); } catch (NumberFormatException e) { applier.logAttrError("lines", e); } }

        String maxEms = applier.getAttr(parser, "maxEms");
        if (maxEms != null) { try { tv.setMaxEms(Integer.parseInt(maxEms)); } catch (NumberFormatException e) { applier.logAttrError("maxEms", e); } }

        String minEms = applier.getAttr(parser, "minEms");
        if (minEms != null) { try { tv.setMinEms(Integer.parseInt(minEms)); } catch (NumberFormatException e) { applier.logAttrError("minEms", e); } }

        String ems = applier.getAttr(parser, "ems");
        if (ems != null) { try { tv.setEms(Integer.parseInt(ems)); } catch (NumberFormatException e) { applier.logAttrError("ems", e); } }

        String ellipsize = applier.getAttr(parser, "ellipsize");
        if (ellipsize != null) {
            switch (ellipsize) {
                case "end": tv.setEllipsize(android.text.TextUtils.TruncateAt.END); break;
                case "start": tv.setEllipsize(android.text.TextUtils.TruncateAt.START); break;
                case "middle": tv.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE); break;
                case "marquee": tv.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE); break;
            }
        }

        String singleLine = applier.getAttr(parser, "singleLine");
        if (singleLine != null) tv.setSingleLine("true".equals(singleLine));

        String maxWidth = applier.getAttr(parser, "maxWidth");
        if (maxWidth != null) tv.setMaxWidth(applier.parseDim(maxWidth));

        String minWidth = applier.getAttr(parser, "minWidth");
        if (minWidth != null) tv.setMinWidth(applier.parseDim(minWidth));

        String letterSpacing = applier.getAttr(parser, "letterSpacing");
        if (letterSpacing != null) { try { tv.setLetterSpacing(Float.parseFloat(letterSpacing)); } catch (NumberFormatException e) { applier.logAttrError("letterSpacing", e); } }

        String lineSpacingExtra = applier.getAttr(parser, "lineSpacingExtra");
        if (lineSpacingExtra != null) { try { tv.setLineSpacing(applier.parseDim(lineSpacingExtra), 1f); } catch (RuntimeException e) { applier.logAttrError("lineSpacingExtra", e); } }

        String lineSpacingMultiplier = applier.getAttr(parser, "lineSpacingMultiplier");
        if (lineSpacingMultiplier != null) { try { tv.setLineSpacing(0f, Float.parseFloat(lineSpacingMultiplier)); } catch (NumberFormatException e) { applier.logAttrError("lineSpacingMultiplier", e); } }

        String textAllCaps = applier.getAttr(parser, "textAllCaps");
        if (textAllCaps != null) {
            tv.setAllCaps("true".equals(textAllCaps));
        }

        String drawableLeft = applier.getAttr(parser, "drawableLeft");
        String drawableTop = applier.getAttr(parser, "drawableTop");
        String drawableRight = applier.getAttr(parser, "drawableRight");
        String drawableBottom = applier.getAttr(parser, "drawableBottom");
        if (drawableLeft != null || drawableTop != null || drawableRight != null
                || drawableBottom != null) {
            Drawable dLeft = drawableLeft != null ? applier.resolveDrawable(drawableLeft) : null;
            Drawable dTop = drawableTop != null ? applier.resolveDrawable(drawableTop) : null;
            Drawable dRight = drawableRight != null ? applier.resolveDrawable(drawableRight) : null;
            Drawable dBottom = drawableBottom != null ? applier.resolveDrawable(drawableBottom) : null;
            if (dLeft != null || dTop != null || dRight != null || dBottom != null) {
                tv.setCompoundDrawablesWithIntrinsicBounds(dLeft, dTop, dRight, dBottom);
            } else {
                Debug.logWarning("drawables",
                        "drawableLeft/Top/Right/Bottom non résolus — ignorés");
            }
        }

        String drawablePadding = applier.getAttr(parser, "drawablePadding");
        if (drawablePadding != null) tv.setCompoundDrawablePadding(applier.parseDim(drawablePadding));

        String shadowColor = applier.getAttr(parser, "shadowColor");
        String shadowDx = applier.getAttr(parser, "shadowDx");
        String shadowDy = applier.getAttr(parser, "shadowDy");
        String shadowRadius = applier.getAttr(parser, "shadowRadius");
        if (shadowColor != null && shadowRadius != null) {
            try {
                tv.setShadowLayer(Float.parseFloat(shadowRadius),
                        shadowDx != null ? Float.parseFloat(shadowDx) : 0,
                        shadowDy != null ? Float.parseFloat(shadowDy) : 0,
                        ColorParser.parse(shadowColor));
            } catch (RuntimeException e) { applier.logAttrError("shadowRadius", e); }
        }

        String fontFamily = applier.getAttr(parser, "fontFamily");
        if (fontFamily != null) {
            Typeface tf = Typeface.create(fontFamily, Typeface.NORMAL);
            tv.setTypeface(tf);
        }

        String includeFontPadding = applier.getAttr(parser, "includeFontPadding");
        if (includeFontPadding != null) tv.setIncludeFontPadding("true".equals(includeFontPadding));

        String textIsSelectable = applier.getAttr(parser, "textIsSelectable");
        if (textIsSelectable != null) tv.setTextIsSelectable("true".equals(textIsSelectable));

        String autoLink = applier.getAttr(parser, "autoLink");
        // autoLink nécessite Android text utils — non géré en prévisualisation
    }

    void applyEditTextAttributes(EditText et, XmlPullParser parser) {
        String inputType = applier.getAttr(parser, "inputType");
        if (inputType != null) et.setInputType(applier.parseInputType(inputType));

        String maxLength = applier.getAttr(parser, "maxLength");
        // maxLength nécessite InputFilter — non géré en prévisualisation

        String imeOptions = applier.getAttr(parser, "imeOptions");
        if (imeOptions != null) {
            int opts = 0;
            if (imeOptions.contains("actionDone")) opts |= android.view.inputmethod.EditorInfo.IME_ACTION_DONE;
            else if (imeOptions.contains("actionGo")) opts |= android.view.inputmethod.EditorInfo.IME_ACTION_GO;
            else if (imeOptions.contains("actionNext")) opts |= android.view.inputmethod.EditorInfo.IME_ACTION_NEXT;
            else if (imeOptions.contains("actionSearch")) opts |= android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH;
            else if (imeOptions.contains("actionSend")) opts |= android.view.inputmethod.EditorInfo.IME_ACTION_SEND;
            et.setImeOptions(opts);
        }

        String capitalize = applier.getAttr(parser, "capitalize");
        // Deprecated — non géré

        String digits = applier.getAttr(parser, "digits");
        if (digits != null) et.setKeyListener(android.text.method.DigitsKeyListener.getInstance(digits));
    }
}
