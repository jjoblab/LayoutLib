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
public final class AttributeApplier {

    private static final String NS_ANDROID = "http://schemas.android.com/apk/res/android";
    private static final String NS_APP = "http://schemas.android.com/apk/res-auto";

    final Context context;
    private DimensionConverter dimensionConverter;
    private ResourceResolver resourceResolver;
    private DrawableResolver drawableResolver;
    private ThemeResolver themeResolver;

    /** Applier TextFamilyAttributes. */
    private final TextFamilyAttributes textFamilyAttributes = new TextFamilyAttributes(this);
    /** Applier ProgressFamilyAttributes. */
    private final ProgressFamilyAttributes progressFamilyAttributes = new ProgressFamilyAttributes(this);
    /** Applier AdapterFamilyAttributes. */
    private final AdapterFamilyAttributes adapterFamilyAttributes = new AdapterFamilyAttributes(this);
    /** Applier LayoutFamilyAttributes. */
    private final LayoutFamilyAttributes layoutFamilyAttributes = new LayoutFamilyAttributes(this);
    /** Applier MiscWidgetAttributes. */
    private final MiscWidgetAttributes miscWidgetAttributes = new MiscWidgetAttributes(this);

    public AttributeApplier(Context context, DimensionConverter converter) {
        this(context, converter, null);
    }

    public AttributeApplier(Context context, DimensionConverter converter,
                            ResourceResolver resolver) {
        this.context = context;
        this.dimensionConverter = converter;
        this.resourceResolver = resolver;
    }

    /**
     * Définit le résolveur de ressources utilisé pour les références
     * {@code @color/}, {@code @string/}, {@code @dimen/}…
     *
     * <p>Permet de connecter (ou remplacer) le résolveur <em>après</em> la
     * construction, pour que le câblage ne dépende pas de l ordre des appels
     * des setters du {@link RenderService}.</p>
     *
     * @param resolver le résolveur, ou {@code null} pour revenir aux
     *                 {@code Resources} natives
     */
    public void setResourceResolver(ResourceResolver resolver) {
        this.resourceResolver = resolver;
    }

    /**
     * @return le résolveur de ressources courant (peut être {@code null})
     */
    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    /**
     * Remplace le convertisseur de dimensions.
     *
     * @param converter le nouveau convertisseur (si {@code null}, l ancien
     *                  est conservé)
     */
    public void setDimensionConverter(DimensionConverter converter) {
        if (converter != null) {
            this.dimensionConverter = converter;
        }
    }

    /**
     * @return le convertisseur de dimensions courant
     */
    public DimensionConverter getDimensionConverter() {
        return dimensionConverter;
    }

    /**
     * Définit le résolveur de drawables utilisé pour les références
     * {@code @drawable/} (shapes, selectors, vectors du projet).
     *
     * <p>Sans résolveur (ou si le résolveur échoue), la résolution retombe
     * sur les {@code Resources} natives.</p>
     *
     * @param drawableResolver le résolveur, ou {@code null} pour revenir aux
     *                         {@code Resources} natives uniquement
     */
    public void setDrawableResolver(DrawableResolver drawableResolver) {
        this.drawableResolver = drawableResolver;
    }

    /**
     * @return le résolveur de drawables courant (peut être {@code null})
     */
    public DrawableResolver getDrawableResolver() {
        return drawableResolver;
    }

    /**
     * Définit le résolveur de thèmes utilisé pour les références
     * {@code ?attr/} et {@code ?android:attr/} (module-themes).
     *
     * <p>Sans résolveur (ou si le thème ne définit pas l'attribut), la
     * résolution retombe sur le {@code Resources.Theme} natif.</p>
     *
     * @param themeResolver le résolveur, ou {@code null} pour revenir au
     *                      thème natif uniquement
     */
    public void setThemeResolver(ThemeResolver themeResolver) {
        this.themeResolver = themeResolver;
    }

    /**
     * @return le résolveur de thèmes courant (peut être {@code null})
     */
    public ThemeResolver getThemeResolver() {
        return themeResolver;
    }

    /**
     * Applique tous les attributs à une vue.
     */
    public void applyAttributes(View view, XmlPullParser parser) {
        if (view == null || parser == null) return;

        applyViewAttributes(view, parser);

        // Familles de widgets (ordre identique à l ancienne chaîne instanceof)
        textFamilyAttributes.apply(view, parser);
        progressFamilyAttributes.apply(view, parser);
        adapterFamilyAttributes.apply(view, parser);
        layoutFamilyAttributes.apply(view, parser);
        miscWidgetAttributes.apply(view, parser);

        // Attributs app: (Material Components, AndroidX)
        applyAppAttributes(view, parser);
    }

    // ========================================================================
    // View (commun à toutes les vues)
    // ========================================================================

    private void applyViewAttributes(View view, XmlPullParser parser) {
        // style — appliquer avant les autres attributs pour qu'ils puissent surcharger
        String style = getAttr(parser, "style");
        if (style != null) {
            applyStyle(view, style);
        }

        String id = getAttr(parser, "id");
        if (id != null) view.setId(resolveViewId(id));

        String background = getAttr(parser, "background");
        if (background != null) applyBackground(view, background);

        String visibility = getAttr(parser, "visibility");
        if (visibility != null) {
            switch (visibility) {
                case "gone": view.setVisibility(View.GONE); break;
                case "invisible": view.setVisibility(View.INVISIBLE); break;
                default: view.setVisibility(View.VISIBLE); break;
            }
        }

        String padding = getAttr(parser, "padding");
        String paddingLeft = getAttr(parser, "paddingLeft");
        String paddingTop = getAttr(parser, "paddingTop");
        String paddingRight = getAttr(parser, "paddingRight");
        String paddingBottom = getAttr(parser, "paddingBottom");
        String paddingStart = getAttr(parser, "paddingStart");
        String paddingEnd = getAttr(parser, "paddingEnd");

        int pl = 0, pt = 0, pr = 0, pb = 0;
        if (padding != null) { int p = parseDim(padding); pl = pr = pt = pb = p; }
        if (paddingLeft != null) pl = parseDim(paddingLeft);
        else if (paddingStart != null) pl = parseDim(paddingStart);
        if (paddingTop != null) pt = parseDim(paddingTop);
        if (paddingRight != null) pr = parseDim(paddingRight);
        else if (paddingEnd != null) pr = parseDim(paddingEnd);
        if (paddingBottom != null) pb = parseDim(paddingBottom);
        if (padding != null || paddingLeft != null || paddingTop != null
                || paddingRight != null || paddingBottom != null
                || paddingStart != null || paddingEnd != null) {
            view.setPadding(pl, pt, pr, pb);
        }

        String enabled = getAttr(parser, "enabled");
        if (enabled != null) view.setEnabled("true".equals(enabled));

        String clickable = getAttr(parser, "clickable");
        if (clickable != null) view.setClickable("true".equals(clickable));

        String focusable = getAttr(parser, "focusable");
        if (focusable != null) view.setFocusable("true".equals(focusable));

        String focusableInTouchMode = getAttr(parser, "focusableInTouchMode");
        if (focusableInTouchMode != null) view.setFocusableInTouchMode("true".equals(focusableInTouchMode));

        String alpha = getAttr(parser, "alpha");
        if (alpha != null) { try { view.setAlpha(Float.parseFloat(alpha)); } catch (NumberFormatException e) { logAttrError("alpha", e); } }

        String tag = getAttr(parser, "tag");
        if (tag != null) view.setTag(tag);

        String minWidth = getAttr(parser, "minWidth");
        if (minWidth != null) view.setMinimumWidth(parseDim(minWidth));

        String minHeight = getAttr(parser, "minHeight");
        if (minHeight != null) view.setMinimumHeight(parseDim(minHeight));

        String soundEffectsEnabled = getAttr(parser, "soundEffectsEnabled");
        if (soundEffectsEnabled != null) view.setSoundEffectsEnabled("true".equals(soundEffectsEnabled));

        String hapticFeedbackEnabled = getAttr(parser, "hapticFeedbackEnabled");
        if (hapticFeedbackEnabled != null) view.setHapticFeedbackEnabled("true".equals(hapticFeedbackEnabled));

        String scrollbars = getAttr(parser, "scrollbars");
        if (scrollbars != null) {
            int sb = 0;
            if (scrollbars.contains("horizontal")) sb |= 0x100;
            if (scrollbars.contains("vertical")) sb |= 0x200;
            view.setVerticalScrollBarEnabled((sb & 0x200) != 0);
            view.setHorizontalScrollBarEnabled((sb & 0x100) != 0);
        }

        String fadeScrollbars = getAttr(parser, "fadeScrollbars");
        if (fadeScrollbars != null) view.setScrollbarFadingEnabled("true".equals(fadeScrollbars));

        String keepScreenOn = getAttr(parser, "keepScreenOn");
        if (keepScreenOn != null) view.setKeepScreenOn("true".equals(keepScreenOn));

        String contentDescription = getAttr(parser, "contentDescription");
        if (contentDescription != null) view.setContentDescription(contentDescription);

        String rotation = getAttr(parser, "rotation");
        if (rotation != null) { try { view.setRotation(Float.parseFloat(rotation)); } catch (NumberFormatException e) { logAttrError("rotation", e); } }

        String rotationX = getAttr(parser, "rotationX");
        if (rotationX != null) { try { view.setRotationX(Float.parseFloat(rotationX)); } catch (NumberFormatException e) { logAttrError("rotationX", e); } }

        String rotationY = getAttr(parser, "rotationY");
        if (rotationY != null) { try { view.setRotationY(Float.parseFloat(rotationY)); } catch (NumberFormatException e) { logAttrError("rotationY", e); } }

        String scaleX = getAttr(parser, "scaleX");
        if (scaleX != null) { try { view.setScaleX(Float.parseFloat(scaleX)); } catch (NumberFormatException e) { logAttrError("scaleX", e); } }

        String scaleY = getAttr(parser, "scaleY");
        if (scaleY != null) { try { view.setScaleY(Float.parseFloat(scaleY)); } catch (NumberFormatException e) { logAttrError("scaleY", e); } }

        String translationX = getAttr(parser, "translationX");
        if (translationX != null) view.setTranslationX(parseDim(translationX));

        String translationY = getAttr(parser, "translationY");
        if (translationY != null) view.setTranslationY(parseDim(translationY));

        String translationZ = getAttr(parser, "translationZ");
        if (translationZ != null) view.setTranslationZ(parseDim(translationZ));

        String elevation = getAttr(parser, "elevation");
        if (elevation != null) view.setElevation(parseDim(elevation));

        String backgroundColor = getAttr(parser, "backgroundColor");
        if (backgroundColor != null) {
            try { view.setBackgroundColor(ColorParser.parse(backgroundColor)); } catch (RuntimeException e) { logAttrError("backgroundColor", e); }
        }

        String foreground = getAttr(parser, "foreground");
        if (foreground != null) applyForeground(view, foreground);

        String fitsSystemWindows = getAttr(parser, "fitsSystemWindows");
        if (fitsSystemWindows != null) view.setFitsSystemWindows("true".equals(fitsSystemWindows));

        String selected = getAttr(parser, "selected");
        if (selected != null) view.setSelected("true".equals(selected));

        String longClickable = getAttr(parser, "longClickable");
        if (longClickable != null) view.setLongClickable("true".equals(longClickable));

        String onClick = getAttr(parser, "onClick");
        // onClick nécessite un listener — non géré en prévisualisation
    }

    // ========================================================================
    // TextView (+ Button, EditText, CheckBox, etc.)
    // ========================================================================




    // ========================================================================
    // ImageView (+ ImageButton)
    // ========================================================================

    private void applyImageViewAttributes(ImageView iv, XmlPullParser parser) {
        String src = getAttr(parser, "src");
        if (src != null) applyImageSrc(iv, src);

        String scaleType = getAttr(parser, "scaleType");
        if (scaleType != null) {
            switch (scaleType) {
                case "center": iv.setScaleType(ImageView.ScaleType.CENTER); break;
                case "centerCrop": iv.setScaleType(ImageView.ScaleType.CENTER_CROP); break;
                case "centerInside": iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE); break;
                case "fitCenter": iv.setScaleType(ImageView.ScaleType.FIT_CENTER); break;
                case "fitEnd": iv.setScaleType(ImageView.ScaleType.FIT_END); break;
                case "fitStart": iv.setScaleType(ImageView.ScaleType.FIT_START); break;
                case "fitXY": iv.setScaleType(ImageView.ScaleType.FIT_XY); break;
                case "matrix": iv.setScaleType(ImageView.ScaleType.MATRIX); break;
            }
        }

        String adjustViewBounds = getAttr(parser, "adjustViewBounds");
        if (adjustViewBounds != null) iv.setAdjustViewBounds("true".equals(adjustViewBounds));

        String maxWidth = getAttr(parser, "maxWidth");
        if (maxWidth != null) iv.setMaxWidth(parseDim(maxWidth));

        String maxHeight = getAttr(parser, "maxHeight");
        if (maxHeight != null) iv.setMaxHeight(parseDim(maxHeight));

        String tint = getAttr(parser, "tint");
        if (tint != null) { try { iv.setColorFilter(ColorParser.parse(tint)); } catch (RuntimeException e) { logAttrError("tint", e); } }

        String cropToPadding = getAttr(parser, "cropToPadding");
        if (cropToPadding != null) iv.setCropToPadding("true".equals(cropToPadding));
    }

    // ========================================================================
    // ProgressBar (+ SeekBar, RatingBar)
    // ========================================================================




    // ========================================================================
    // Spinner
    // ========================================================================


    // ========================================================================
    // AdapterView (ListView, GridView, Spinner, etc.)
    // ========================================================================




    // ========================================================================
    // Layouts
    // ========================================================================








    // ========================================================================
    // Views spécialisées
    // ========================================================================









    // ========================================================================
    // Attributs app: (Material Components, AndroidX)
    // ========================================================================

    /**
     * Applique les attributs du namespace app: (res-auto).
     * Ces attributs sont utilisés par Material Components, AndroidX, et les vues custom.
     */
    private void applyAppAttributes(View view, XmlPullParser parser) {
        // Attributs communs app: pour Material Components
        String appElevation = getAppAttr(parser, "elevation");
        if (appElevation != null) {
            view.setElevation(parseDim(appElevation));
        }

        String appBackgroundColor = getAppAttr(parser, "backgroundTint");
        if (appBackgroundColor != null) {
            try { view.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ColorParser.parse(appBackgroundColor))); } catch (RuntimeException e) { logAttrError("backgroundTint", e); }
        }

        // CardView / MaterialCardView
        if (view instanceof android.widget.FrameLayout) {
            String cardCornerRadius = getAppAttr(parser, "cardCornerRadius");
            if (cardCornerRadius != null && view.getBackground() instanceof android.graphics.drawable.GradientDrawable) {
                ((android.graphics.drawable.GradientDrawable) view.getBackground())
                        .setCornerRadius(parseDim(cardCornerRadius));
            }
            String cardElevation = getAppAttr(parser, "cardElevation");
            if (cardElevation != null) {
                view.setElevation(parseDim(cardElevation));
            }
            String cardBackgroundColor = getAppAttr(parser, "cardBackgroundColor");
            if (cardBackgroundColor != null) {
                try { view.setBackground(new ColorDrawable(ColorParser.parse(cardBackgroundColor))); } catch (RuntimeException e) { logAttrError("cardBackgroundColor", e); }
            }
            String contentPadding = getAppAttr(parser, "contentPadding");
            if (contentPadding != null) {
                int p = parseDim(contentPadding);
                view.setPadding(p, p, p, p);
            }
            String contentPaddingLeft = getAppAttr(parser, "contentPaddingLeft");
            String contentPaddingTop = getAppAttr(parser, "contentPaddingTop");
            String contentPaddingRight = getAppAttr(parser, "contentPaddingRight");
            String contentPaddingBottom = getAppAttr(parser, "contentPaddingBottom");
            if (contentPaddingLeft != null || contentPaddingTop != null
                    || contentPaddingRight != null || contentPaddingBottom != null) {
                int pl = contentPaddingLeft != null ? parseDim(contentPaddingLeft) : view.getPaddingLeft();
                int pt = contentPaddingTop != null ? parseDim(contentPaddingTop) : view.getPaddingTop();
                int pr = contentPaddingRight != null ? parseDim(contentPaddingRight) : view.getPaddingRight();
                int pb = contentPaddingBottom != null ? parseDim(contentPaddingBottom) : view.getPaddingBottom();
                view.setPadding(pl, pt, pr, pb);
            }
        }

        // MaterialButton / Button app: attributs
        if (view instanceof android.widget.Button) {
            android.widget.Button btn = (android.widget.Button) view;
            String cornerRadius = getAppAttr(parser, "cornerRadius");
            if (cornerRadius != null) {
                // MaterialButton utilise un GradientDrawable en background
                if (btn.getBackground() instanceof android.graphics.drawable.GradientDrawable) {
                    ((android.graphics.drawable.GradientDrawable) btn.getBackground())
                            .setCornerRadius(parseDim(cornerRadius));
                }
            }
            String icon = getAppAttr(parser, "icon");
            if (icon != null) {
                // MaterialButton setIcon n existe pas sur Button standard :
                // approximation preview — icône en drawable composé gauche.
                Drawable iconDrawable = resolveDrawable(icon);
                if (iconDrawable != null) {
                    btn.setCompoundDrawablesWithIntrinsicBounds(iconDrawable, null, null, null);
                }
            }
            String iconTint = getAppAttr(parser, "iconTint");
            if (iconTint != null) {
                // Pas de setCompoundDrawableTintList sur Button standard
            }
            String strokeColor = getAppAttr(parser, "strokeColor");
            String strokeWidth = getAppAttr(parser, "strokeWidth");
            if (strokeColor != null && strokeWidth != null) {
                if (btn.getBackground() instanceof android.graphics.drawable.GradientDrawable) {
                    try {
                        ((android.graphics.drawable.GradientDrawable) btn.getBackground())
                                .setStroke(parseDim(strokeWidth), ColorParser.parse(strokeColor));
                    } catch (RuntimeException e) { logAttrError("strokeWidth", e); }
                }
            }
            String backgroundTint = getAppAttr(parser, "backgroundTint");
            if (backgroundTint != null) {
                try { btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ColorParser.parse(backgroundTint))); } catch (RuntimeException e) { logAttrError("backgroundTint", e); }
            }
        }

        // FloatingActionButton app: attributs
        if (view instanceof android.widget.ImageView) {
            android.widget.ImageView iv = (android.widget.ImageView) view;
            String fabSize = getAppAttr(parser, "fabSize");
            String fabCustomSize = getAppAttr(parser, "fabCustomSize");
            if (fabCustomSize != null) {
                int size = parseDim(fabCustomSize);
                iv.getLayoutParams().width = size;
                iv.getLayoutParams().height = size;
            }
            String srcCompat = getAppAttr(parser, "srcCompat");
            if (srcCompat != null) {
                applyImageSrc(iv, srcCompat);
            }
            String tintColor = getAppAttr(parser, "tint");
            if (tintColor != null) {
                try { iv.setColorFilter(ColorParser.parse(tintColor)); } catch (RuntimeException e) { logAttrError("tint", e); }
            }
        }

        // TextInputLayout app: attributs
        String hintEnabled = getAppAttr(parser, "hintEnabled");
        String boxStrokeColor = getAppAttr(parser, "boxStrokeColor");
        String boxBackgroundColor = getAppAttr(parser, "boxBackgroundColor");
        String boxCornerRadiusTopStart = getAppAttr(parser, "boxCornerRadiusTopStart");
        // Ces attributs nécessitent TextInputLayout — appliqués si disponible

        // Chip app: attributs
        String chipIcon = getAppAttr(parser, "chipIcon");
        String chipBackgroundColor = getAppAttr(parser, "chipBackgroundColor");
        String chipStrokeColor = getAppAttr(parser, "chipStrokeColor");
        String chipStrokeWidth = getAppAttr(parser, "chipStrokeWidth");
        String chipCornerRadius = getAppAttr(parser, "chipCornerRadius");
        if (chipBackgroundColor != null && view instanceof android.widget.Button) {
            try { view.setBackground(new ColorDrawable(ColorParser.parse(chipBackgroundColor))); } catch (RuntimeException e) { logAttrError("chipCornerRadius", e); }
        }

        // CoordinatorLayout / AppBarLayout app: attributs
        String layout_behavior = getAppAttr(parser, "layout_behavior");
        String layout_scrollFlags = getAppAttr(parser, "layout_scrollFlags");
        String layout_scrollInterpolator = getAppAttr(parser, "layout_scrollInterpolator");
        // Ces attributs nécessitent CoordinatorLayout.LayoutParams — non gérés en prévisualisation

        // ConstraintLayout app: attributs
        String constraint_referenced_ids = getAppAttr(parser, "constraint_referenced_ids");
        String layout_constraintTop_toTopOf = getAppAttr(parser, "layout_constraintTop_toTopOf");
        String layout_constraintTop_toBottomOf = getAppAttr(parser, "layout_constraintTop_toBottomOf");
        String layout_constraintBottom_toTopOf = getAppAttr(parser, "layout_constraintBottom_toTopOf");
        String layout_constraintBottom_toBottomOf = getAppAttr(parser, "layout_constraintBottom_toBottomOf");
        String layout_constraintStart_toStartOf = getAppAttr(parser, "layout_constraintStart_toStartOf");
        String layout_constraintStart_toEndOf = getAppAttr(parser, "layout_constraintStart_toEndOf");
        String layout_constraintEnd_toStartOf = getAppAttr(parser, "layout_constraintEnd_toStartOf");
        String layout_constraintEnd_toEndOf = getAppAttr(parser, "layout_constraintEnd_toEndOf");
        String layout_constraintLeft_toLeftOf = getAppAttr(parser, "layout_constraintLeft_toLeftOf");
        String layout_constraintLeft_toRightOf = getAppAttr(parser, "layout_constraintLeft_toRightOf");
        String layout_constraintRight_toLeftOf = getAppAttr(parser, "layout_constraintRight_toLeftOf");
        String layout_constraintRight_toRightOf = getAppAttr(parser, "layout_constraintRight_toRightOf");
        String layout_constraintHorizontal_bias = getAppAttr(parser, "layout_constraintHorizontal_bias");
        String layout_constraintVertical_bias = getAppAttr(parser, "layout_constraintVertical_bias");
        String layout_constraintWidth_percent = getAppAttr(parser, "layout_constraintWidth_percent");
        String layout_constraintHeight_percent = getAppAttr(parser, "layout_constraintHeight_percent");
        String layout_goneMarginTop = getAppAttr(parser, "layout_goneMarginTop");
        String layout_goneMarginBottom = getAppAttr(parser, "layout_goneMarginBottom");
        String layout_goneMarginStart = getAppAttr(parser, "layout_goneMarginStart");
        String layout_goneMarginEnd = getAppAttr(parser, "layout_goneMarginEnd");
        // ConstraintLayout nécessite son propre LayoutParams + solver Cassowary
        // Non géré en prévisualisation simple

        // TabLayout app: attributs
        String tabIndicatorColor = getAppAttr(parser, "tabIndicatorColor");
        String tabIndicatorHeight = getAppAttr(parser, "tabIndicatorHeight");
        String tabSelectedTextColor = getAppAttr(parser, "tabSelectedTextColor");
        String tabTextColor = getAppAttr(parser, "tabTextColor");
        String tabGravity = getAppAttr(parser, "tabGravity");
        String tabMode = getAppAttr(parser, "tabMode");

        // BottomNavigationView app: attributs
        String itemIconTint = getAppAttr(parser, "itemIconTint");
        String itemTextColor = getAppAttr(parser, "itemTextColor");
        String itemHorizontalTranslationEnabled = getAppAttr(parser, "itemHorizontalTranslationEnabled");
        String labelVisibilityMode = getAppAttr(parser, "labelVisibilityMode");

        // Slider app: attributs
        String app_valueFrom = getAppAttr(parser, "valueFrom");
        String app_valueTo = getAppAttr(parser, "valueTo");
        String app_stepSize = getAppAttr(parser, "stepSize");
        String app_thumbColor = getAppAttr(parser, "thumbColor");
        String app_trackColor = getAppAttr(parser, "trackColor");
        String app_tickVisible = getAppAttr(parser, "tickVisible");
    }

    /**
     * Lit un attribut app:* depuis le parser.
     */
    String getAppAttr(XmlPullParser parser, String name) {
        String value = parser.getAttributeValue(NS_APP, name);
        if (value == null) {
            int count = parser.getAttributeCount();
            for (int i = 0; i < count; i++) {
                String attrName = parser.getAttributeName(i);
                if (name.equals(attrName)) {
                    return parser.getAttributeValue(i);
                }
            }
        }
        return value;
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    String getAttr(XmlPullParser parser, String name) {
        String value = parser.getAttributeValue(NS_ANDROID, name);
        if (value == null) {
            int count = parser.getAttributeCount();
            for (int i = 0; i < count; i++) {
                String attrName = parser.getAttributeName(i);
                if (name.equals(attrName)) {
                    return parser.getAttributeValue(i);
                }
            }
        }
        return value;
    }

    void applyTextColor(TextView tv, String value) {
        try {
            if (value.startsWith("#")) {
                tv.setTextColor(ColorParser.parse(value));
            } else if (value.startsWith("@color/")) {
                Integer color = resolveColor(value);
                if (color != null) tv.setTextColor(color);
            } else if (value.startsWith("?attr/") || value.startsWith("?android:attr/")) {
                Integer color = resolveThemeAttr(value);
                if (color != null) tv.setTextColor(color);
            }
        } catch (RuntimeException e) { logAttrError("tickVisible", e); }
    }

    void applyBackground(View view, String value) {
        if (value == null) return;
        try {
            if (value.startsWith("#")) {
                view.setBackground(new ColorDrawable(ColorParser.parse(value)));
            } else if (value.startsWith("@color/")) {
                Integer color = resolveColor(value);
                if (color != null) view.setBackground(new ColorDrawable(color));
            } else if (value.startsWith("@drawable/")) {
                Drawable d = resolveDrawable(value);
                if (d != null) view.setBackground(d);
            } else if (value.startsWith("?attr/") || value.startsWith("?android:attr/")) {
                Integer color = resolveThemeAttr(value);
                if (color != null) view.setBackground(new ColorDrawable(color));
            }
        } catch (RuntimeException e) { logAttrError("tickVisible", e); }
    }

    void applyImageSrc(ImageView iv, String value) {
        if (value == null) return;
        try {
            if (value.startsWith("@drawable/")) {
                Drawable d = resolveDrawable(value);
                if (d != null) iv.setImageDrawable(d);
            } else if (value.startsWith("@android:drawable/")) {
                String name = value.substring("@android:drawable/".length());
                int id = context.getResources().getIdentifier(name, "drawable", "android");
                if (id != 0) iv.setImageResource(id);
            }
        } catch (RuntimeException e) { logAttrError("tickVisible", e); }
    }

    void applyForeground(View view, String value) {
        try {
            if (value.startsWith("#")) {
                if (view instanceof FrameLayout) {
                    ((FrameLayout) view).setForeground(new ColorDrawable(ColorParser.parse(value)));
                }
            } else if (value.startsWith("@color/")) {
                Integer color = resolveColor(value);
                if (color != null && view instanceof FrameLayout) {
                    ((FrameLayout) view).setForeground(new ColorDrawable(color));
                }
            }
        } catch (RuntimeException e) { logAttrError("tickVisible", e); }
    }

    // ========================================================================
    // Résolution de resources
    // ========================================================================

    /**
     * Applique un style à une vue en résolvant les attributs du style.
     *
     * <p>Le style peut être référencé par :</p>
     * <ul>
     *   <li>{@code @style/MyStyle} — style du projet</li>
     *   <li>{@code @android:style/TextAppearance.Material3.TitleLarge} — style framework</li>
     * </ul>
     *
     * <p>La résolution utilise {@code Resources.getIdentifier()} pour trouver
     * l'id du style, puis {@code obtainStyledAttributes()} pour lire ses
     * attributs et les appliquer à la vue.</p>
     *
     * @param view la vue cible
     * @param styleRef la référence du style
     */
    void applyStyle(View view, String styleRef) {
        if (styleRef == null || styleRef.isEmpty()) return;

        int styleId = 0;
        if (styleRef.startsWith("@style/")) {
            String name = styleRef.substring("@style/".length());
            styleId = context.getResources().getIdentifier(name, "style", context.getPackageName());
            if (styleId == 0) {
                styleId = context.getResources().getIdentifier(name, "style", "android");
            }
        } else if (styleRef.startsWith("@android:style/")) {
            String name = styleRef.substring("@android:style/".length());
            styleId = context.getResources().getIdentifier(name, "style", "android");
        }

        if (styleId == 0) return;

        // Lire les attributs communs depuis le style
        android.content.res.TypedArray a = context.obtainStyledAttributes(styleId,
                new int[]{
                        android.R.attr.background,
                        android.R.attr.padding,
                        android.R.attr.paddingLeft,
                        android.R.attr.paddingTop,
                        android.R.attr.paddingRight,
                        android.R.attr.paddingBottom,
                        android.R.attr.visibility,
                        android.R.attr.enabled,
                        android.R.attr.clickable,
                        android.R.attr.focusable,
                        android.R.attr.minWidth,
                        android.R.attr.minHeight,
                        android.R.attr.elevation,
                });

        try {
            // background
            android.graphics.drawable.Drawable bg = a.getDrawable(0);
            if (bg != null) view.setBackground(bg);

            // padding
            int padding = a.getDimensionPixelSize(1, Integer.MIN_VALUE);
            if (padding != Integer.MIN_VALUE) {
                view.setPadding(padding, padding, padding, padding);
            } else {
                int pl = a.getDimensionPixelSize(2, view.getPaddingLeft());
                int pt = a.getDimensionPixelSize(3, view.getPaddingTop());
                int pr = a.getDimensionPixelSize(4, view.getPaddingRight());
                int pb = a.getDimensionPixelSize(5, view.getPaddingBottom());
                view.setPadding(pl, pt, pr, pb);
            }

            // visibility
            int vis = a.getInt(6, -1);
            if (vis >= 0) {
                switch (vis) {
                    case 0: view.setVisibility(View.VISIBLE); break;
                    case 1: view.setVisibility(View.INVISIBLE); break;
                    case 2: view.setVisibility(View.GONE); break;
                }
            }

            // enabled
            if (!a.getBoolean(7, true)) view.setEnabled(false);

            // clickable
            boolean clickable = a.getBoolean(8, view.isClickable());
            view.setClickable(clickable);

            // focusable
            boolean focusable = a.getBoolean(9, view.isFocusable());
            view.setFocusable(focusable);

            // minWidth / minHeight
            int minWidth = a.getDimensionPixelSize(10, 0);
            if (minWidth > 0) view.setMinimumWidth(minWidth);
            int minHeight = a.getDimensionPixelSize(11, 0);
            if (minHeight > 0) view.setMinimumHeight(minHeight);

            // elevation
            float elevation = a.getDimension(12, 0);
            if (elevation > 0) view.setElevation(elevation);

        } finally {
            a.recycle();
        }

        // Si la vue est un TextView, lire les attributs texte depuis le style
        if (view instanceof TextView) {
            applyTextStyle((TextView) view, styleId);
        }
    }

    /**
     * Applique les attributs de style spécifiques à TextView.
     *
     * @param tv le TextView
     * @param styleId l'id du style
     */
    void applyTextStyle(TextView tv, int styleId) {
        android.content.res.TypedArray a = context.obtainStyledAttributes(styleId,
                new int[]{
                        android.R.attr.text,
                        android.R.attr.textColor,
                        android.R.attr.textSize,
                        android.R.attr.textStyle,
                        android.R.attr.gravity,
                        android.R.attr.hint,
                        android.R.attr.textColorHint,
                        android.R.attr.fontFamily,
                        android.R.attr.maxLines,
                        android.R.attr.singleLine,
                        android.R.attr.ellipsize,
                        android.R.attr.textAllCaps,
                        android.R.attr.drawablePadding,
                        android.R.attr.letterSpacing,
                        android.R.attr.lineSpacingExtra,
                        android.R.attr.shadowColor,
                        android.R.attr.shadowDx,
                        android.R.attr.shadowDy,
                        android.R.attr.shadowRadius,
                });

        try {
            String text = a.getString(0);
            if (text != null) tv.setText(text);

            android.content.res.ColorStateList textColor = a.getColorStateList(1);
            if (textColor != null) tv.setTextColor(textColor);

            float textSize = a.getDimension(2, 0);
            if (textSize > 0) tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

            int textStyle = a.getInt(3, -1);
            if (textStyle >= 0) {
                tv.setTypeface(tv.getTypeface(), textStyle);
            }

            int gravity = a.getInt(4, Integer.MIN_VALUE);
            if (gravity != Integer.MIN_VALUE) tv.setGravity(gravity);

            String hint = a.getString(5);
            if (hint != null) tv.setHint(hint);

            android.content.res.ColorStateList hintColor = a.getColorStateList(6);
            if (hintColor != null) tv.setHintTextColor(hintColor);

            String fontFamily = a.getString(7);
            if (fontFamily != null) {
                tv.setTypeface(Typeface.create(fontFamily, Typeface.NORMAL));
            }

            int maxLines = a.getInt(8, -1);
            if (maxLines > 0) tv.setMaxLines(maxLines);

            boolean singleLine = a.getBoolean(9, false);
            if (singleLine) tv.setSingleLine(true);

            int ellipsize = a.getInt(10, -1);
            if (ellipsize >= 0) {
                switch (ellipsize) {
                    case 1: tv.setEllipsize(android.text.TextUtils.TruncateAt.START); break;
                    case 2: tv.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE); break;
                    case 3: tv.setEllipsize(android.text.TextUtils.TruncateAt.END); break;
                    case 4: tv.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE); break;
                }
            }

            boolean allCaps = a.getBoolean(11, false);
            if (allCaps) tv.setAllCaps(true);

            int drawablePadding = a.getDimensionPixelSize(12, 0);
            if (drawablePadding > 0) tv.setCompoundDrawablePadding(drawablePadding);

            float letterSpacing = a.getFloat(13, Float.MIN_VALUE);
            if (letterSpacing != Float.MIN_VALUE) tv.setLetterSpacing(letterSpacing);

            float lineSpacing = a.getDimension(14, Float.MIN_VALUE);
            if (lineSpacing != Float.MIN_VALUE) tv.setLineSpacing(lineSpacing, 1f);

            int shadowColor = a.getColor(15, 0);
            float shadowRadius = a.getFloat(18, 0);
            if (shadowColor != 0 && shadowRadius > 0) {
                float dx = a.getFloat(16, 0);
                float dy = a.getFloat(17, 0);
                tv.setShadowLayer(shadowRadius, dx, dy, shadowColor);
            }

        } finally {
            a.recycle();
        }
    }

    /**
     * Journalise une erreur d'attribut sans interrompre le rendu.
     *
     * <p>Conformément au contrat du pipeline : une erreur sur un attribut
     * (valeur non numérique, couleur invalide, etc.) est ignorée et
     * journalisée avec son contexte — elle ne doit jamais interrompre le
     * rendu du layout entier.</p>
     *
     * @param attr le nom de l'attribut concerné (approximatif si inconnu)
     * @param e    l'exception attrapée
     */
    static void logAttrError(String attr, RuntimeException e) {
        Debug.logWarning("attributes",
                "Attribut '" + attr + "' ignoré : " + e.getClass().getSimpleName()
                        + (e.getMessage() != null ? " (" + e.getMessage() + ")" : ""));
    }

    /**
     * Résout une référence @color/ en valeur ARGB.
     * Utilise le ResourceResolver si disponible, sinon Resources natives.
     *
     * @param ref la référence (ex. @color/primary)
     * @return la couleur ARGB, ou null
     */
    Integer resolveColor(String ref) {
        if (ref == null) return null;
        // 1. Tenter via ResourceResolver (project resources)
        if (resourceResolver != null) {
            Integer color = resourceResolver.getColor(ref);
            if (color != null) return color;
        }
        // 2. Tenter via Resources natives
        if (ref.startsWith("@color/")) {
            String name = ref.substring("@color/".length());
            int id = context.getResources().getIdentifier(name, "color", context.getPackageName());
            if (id != 0) {
                try { return context.getResources().getColor(id, context.getTheme()); } catch (android.content.res.Resources.NotFoundException e) { return null; }
            }
            // 3. Framework
            id = context.getResources().getIdentifier(name, "color", "android");
            if (id != 0) {
                try { return context.getResources().getColor(id, context.getTheme()); } catch (android.content.res.Resources.NotFoundException e) { return null; }
            }
        } else if (ref.startsWith("@android:color/")) {
            String name = ref.substring("@android:color/".length());
            int id = context.getResources().getIdentifier(name, "color", "android");
            if (id != 0) {
                try { return context.getResources().getColor(id, context.getTheme()); } catch (android.content.res.Resources.NotFoundException e) { return null; }
            }
        }
        return null;
    }

    /**
     * Résout une référence @drawable/ en Drawable.
     *
     * <p>Ordre de résolution :</p>
     * <ol>
     *   <li>{@link DrawableResolver} connecté (resources du projet :
     *       shapes, selectors, vectors, images)</li>
     *   <li>{@code Resources} natives (drawables de l'app hôte et du
     *       framework via {@code getIdentifier})</li>
     * </ol>
     *
     * <p>Un drawable non résolvable retourne {@code null} : l'attribut est
     * ignoré, le rendu continue (jamais d'exception qui casserait tout le
     * rendu).</p>
     *
     * @param ref la référence (ex. @drawable/card_bg)
     * @return le Drawable, ou null
     */
    Drawable resolveDrawable(String ref) {
        if (ref == null || context == null) {
            return null;
        }
        // 1. DrawableResolver du projet
        if (drawableResolver != null) {
            try {
                Drawable d = drawableResolver.resolve(ref, context);
                if (d != null) {
                    return d;
                }
            } catch (RuntimeException e) {
                Debug.logWarning("drawables",
                        "DrawableResolver a échoué pour " + ref + " : " + e);
            }
        }
        // 2. Resources natives
        android.content.res.Resources res = context.getResources();
        if (res == null) {
            return null;
        }
        String name = null;
        String pkg = context.getPackageName();
        if (ref.startsWith("@drawable/")) {
            name = ref.substring("@drawable/".length());
        } else if (ref.startsWith("@android:drawable/")) {
            name = ref.substring("@android:drawable/".length());
            pkg = "android";
        }
        if (name == null) return null;
        try {
            int id = res.getIdentifier(name, "drawable", pkg);
            if (id != 0) {
                return res.getDrawable(id, context.getTheme());
            }
        } catch (RuntimeException e) {
            Debug.logWarning("drawables",
                    "Drawable natif introuvable : " + ref);
        }
        return null;
    }

    /**
     * Résout une référence @array/ en liste de chaînes.
     *
     * <p>Ordre de résolution :</p>
     * <ol>
     *   <li>{@link ResourceResolver} connecté (string-arrays du projet)</li>
     *   <li>{@code Resources} natives via {@code getIdentifier(name, "array", pkg)}</li>
     * </ol>
     *
     * @param ref la référence (ex. @array/planets)
     * @return la liste résolue, ou null si introuvable
     */
    java.util.List<String> resolveStringArray(String ref) {
        if (ref == null || !ref.startsWith("@array/") || context == null) {
            return null;
        }
        String name = ref.substring("@array/".length());
        // 1. ResourceResolver du projet
        if (resourceResolver != null) {
            java.util.List<String> array = resourceResolver.getStringArray(ref);
            if (array != null) {
                return array;
            }
        }
        // 2. Resources natives
        android.content.res.Resources res = context.getResources();
        if (res == null) {
            return null;
        }
        try {
            int id = res.getIdentifier(name, "array", context.getPackageName());
            if (id == 0) {
                id = res.getIdentifier(name, "array", "android");
            }
            if (id != 0) {
                String[] values = res.getStringArray(id);
                if (values != null) {
                    return java.util.Arrays.asList(values);
                }
            }
        } catch (RuntimeException e) {
            Debug.logWarning("resources",
                    "string-array natif introuvable : " + ref);
        }
        return null;
    }

    /**
     * Résout une référence @string/ en chaîne.
     *
     * @param ref la référence
     * @return la chaîne, ou null
     */
    String resolveString(String ref) {
        if (ref == null) return null;
        if (resourceResolver != null) {
            String s = resourceResolver.getString(ref);
            if (s != null) return s;
        }
        if (ref.startsWith("@string/")) {
            String name = ref.substring("@string/".length());
            int id = context.getResources().getIdentifier(name, "string", context.getPackageName());
            if (id != 0) return context.getResources().getString(id);
            id = context.getResources().getIdentifier(name, "string", "android");
            if (id != 0) return context.getResources().getString(id);
        } else if (ref.startsWith("@android:string/")) {
            String name = ref.substring("@android:string/".length());
            int id = context.getResources().getIdentifier(name, "string", "android");
            if (id != 0) return context.getResources().getString(id);
        }
        return null;
    }

    /**
     * Résout une référence @dimen/ en pixels.
     *
     * @param ref la référence
     * @return la valeur en pixels, ou null
     */
    Integer resolveDimension(String ref) {
        if (ref == null) return null;
        if (resourceResolver != null) {
            Float dim = resourceResolver.getDimension(ref);
            if (dim != null) return dim.intValue();
        }
        if (ref.startsWith("@dimen/")) {
            String name = ref.substring("@dimen/".length());
            int id = context.getResources().getIdentifier(name, "dimen", context.getPackageName());
            if (id != 0) return (int) context.getResources().getDimension(id);
            id = context.getResources().getIdentifier(name, "dimen", "android");
            if (id != 0) return (int) context.getResources().getDimension(id);
        }
        return null;
    }

    /**
     * Résout un attribut de thème ?attr/ ou ?android:attr/.
     *
     * <p>Ordre de résolution :</p>
     * <ol>
     *   <li>{@link ThemeResolver} connecté (module-themes : thèmes/styles du
     *       projet parsés depuis themes.xml/styles.xml)</li>
     *   <li>{@code Resources.Theme} natif</li>
     * </ol>
     *
     * <p>Visibilité package : testable unitairement.</p>
     *
     * @param ref la référence
     * @return la couleur ARGB, ou null
     */
    Integer resolveThemeAttr(String ref) {
        if (ref == null) return null;
        // 1. ThemeResolver du projet (themes.xml/styles.xml)
        if (themeResolver != null) {
            try {
                Object value = themeResolver.resolveAttr(ref);
                if (value instanceof Integer) {
                    return (Integer) value;
                }
            } catch (RuntimeException e) {
                Debug.logWarning("themes",
                        "ThemeResolver a échoué pour " + ref + " : " + e);
            }
        }
        // 2. Résoudre via Resources.Theme natif
        String attrName;
        boolean framework;
        if (ref.startsWith("?android:attr/")) {
            attrName = ref.substring("?android:attr/".length());
            framework = true;
        } else if (ref.startsWith("?attr/")) {
            attrName = ref.substring("?attr/".length());
            framework = false;
        } else if (ref.startsWith("?")) {
            attrName = ref.substring(1);
            framework = false;
        } else {
            return null;
        }
        // Résoudre via Resources.Theme natif (contexte absent/minimal → null)
        if (context == null || context.getResources() == null) {
            return null;
        }
        int attrId;
        if (framework) {
            attrId = context.getResources().getIdentifier(attrName, "attr", "android");
        } else {
            attrId = context.getResources().getIdentifier(attrName, "attr", context.getPackageName());
            if (attrId == 0) {
                attrId = context.getResources().getIdentifier(attrName, "attr", "android");
            }
        }
        if (attrId == 0) return null;
        if (context.getTheme() == null) return null;
        android.util.TypedValue value = new android.util.TypedValue();
        if (context.getTheme().resolveAttribute(attrId, value, true)) {
            if (value.type >= android.util.TypedValue.TYPE_FIRST_COLOR_INT
                    && value.type <= android.util.TypedValue.TYPE_LAST_COLOR_INT) {
                return value.data;
            }
            if (value.resourceId != 0) {
                try { return context.getResources().getColor(value.resourceId, context.getTheme()); } catch (android.content.res.Resources.NotFoundException e) { return null; }
            }
        }
        return null;
    }

    int parseGravity(String value) {
        int gravity = 0;
        if (value == null) return gravity;
        String[] parts = value.split("\\|");
        for (String part : parts) {
            switch (part.trim()) {
                case "center": gravity |= Gravity.CENTER; break;
                case "center_horizontal": gravity |= Gravity.CENTER_HORIZONTAL; break;
                case "center_vertical": gravity |= Gravity.CENTER_VERTICAL; break;
                case "left": gravity |= Gravity.LEFT; break;
                case "right": gravity |= Gravity.RIGHT; break;
                case "top": gravity |= Gravity.TOP; break;
                case "bottom": gravity |= Gravity.BOTTOM; break;
                case "start": gravity |= Gravity.START; break;
                case "end": gravity |= Gravity.END; break;
                case "fill": gravity |= Gravity.FILL; break;
                case "fill_horizontal": gravity |= Gravity.FILL_HORIZONTAL; break;
                case "fill_vertical": gravity |= Gravity.FILL_VERTICAL; break;
                case "clip_horizontal": gravity |= Gravity.CLIP_HORIZONTAL; break;
                case "clip_vertical": gravity |= Gravity.CLIP_VERTICAL; break;
            }
        }
        return gravity;
    }

    int parseInputType(String value) {
        int type = InputType.TYPE_CLASS_TEXT;
        String[] parts = value.split("\\|");
        for (String part : parts) {
            switch (part.trim()) {
                case "text": type |= InputType.TYPE_CLASS_TEXT; break;
                case "textEmailAddress": type = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS; break;
                case "textPassword": type = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD; break;
                case "textVisiblePassword": type = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD; break;
                case "textUri": type = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI; break;
                case "textMultiLine": type |= InputType.TYPE_TEXT_FLAG_MULTI_LINE; break;
                case "textNoSuggestions": type |= InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS; break;
                case "textCapCharacters": type |= InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS; break;
                case "textCapWords": type |= InputType.TYPE_TEXT_FLAG_CAP_WORDS; break;
                case "textCapSentences": type |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES; break;
                case "number": type = InputType.TYPE_CLASS_NUMBER; break;
                case "numberDecimal": type = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL; break;
                case "numberSigned": type = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED; break;
                case "numberPassword": type = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD; break;
                case "phone": type = InputType.TYPE_CLASS_PHONE; break;
                case "datetime": type = InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_NORMAL; break;
                case "date": type = InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE; break;
                case "time": type = InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME; break;
            }
        }
        return type;
    }

    int parseDim(String value) {
        if (value == null || value.isEmpty()) return 0;
        if ("match_parent".equals(value) || "fill_parent".equals(value)) return ViewGroup.LayoutParams.MATCH_PARENT;
        if ("wrap_content".equals(value)) return ViewGroup.LayoutParams.WRAP_CONTENT;
        if (dimensionConverter != null) {
            try { return dimensionConverter.toPixelsInt(value); } catch (ResourceException e) { return 0; }
        }
        try { return Integer.parseInt(value.replaceAll("[^0-9-]", "")); } catch (NumberFormatException e) { return 0; }
    }

    int resolveViewId(String idValue) {
        if (idValue == null) return View.NO_ID;
        if (idValue.startsWith("@+id/") || idValue.startsWith("@id/")) {
            String name = idValue.substring(idValue.indexOf('/') + 1);
            int resolved = context.getResources().getIdentifier(name, "id", context.getPackageName());
            if (resolved != 0) return resolved;
            return View.generateViewId();
        }
        if (idValue.startsWith("@android:id/")) {
            String name = idValue.substring("@android:id/".length());
            return context.getResources().getIdentifier(name, "id", "android");
        }
        try { return Integer.parseInt(idValue); } catch (NumberFormatException e) { return View.NO_ID; }
    }
}
