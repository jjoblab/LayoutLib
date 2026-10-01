package jo.layoutlib.inflater;

import java.util.HashMap;
import java.util.Map;

/**
 * Registre des mappings entre les noms de tags XML Android et les classes Java
 * qui les implémentent.
 *
 * <p>Le framework Android expose un certain nombre de vues via des tags courts
 * (ex. {@code <TextView>}, {@code <LinearLayout>}). À ces tags courts
 * correspondent des classes {@code android.widget.*} ou {@code android.view.*}.
 * Le mini-layoutlib doit aussi savoir rediriger ces tags vers les classes
 * <em>Design</em> du LayoutEditor (ex. {@code TextViewDesign}) qui ajoutent le
 * stroke et le mode blueprint.</p>
 *
 * <p>Cette classe est volontairement découplée de la réflexion : elle se
 * contente de produire le nom pleinement qualifié de la classe. La résolution
 * effective est faite par {@link ViewFactory}.</p>
 *
 * <h2>Tags supportés</h2>
 * <ul>
 *   <li>Tags courts du framework Android : {@code View}, {@code TextView},
 *       {@code Button}, {@code ImageView}, {@code EditText},
 *       {@code LinearLayout}, {@code FrameLayout}, {@code RelativeLayout},
 *       {@code ScrollView}, {@code HorizontalScrollView},
 *       {@code ConstraintLayout}, {@code RecyclerView}, {@code WebView},
 *       {@code ProgressBar}, {@code CheckBox}, {@code RadioButton},
 *       {@code Switch}, {@code ToggleButton}, {@code SeekBar},
 *       {@code RatingBar}, {@code Spinner}, {@code ListView},
 *       {@code GridView}, {@code TabHost}, {@code ViewStub}.</li>
 *   <li>Tags AndroidX pleinement qualifiés : {@code androidx.cardview.widget.CardView},
 *       {@code androidx.recyclerview.widget.RecyclerView},
 *       {@code androidx.constraintlayout.widget.ConstraintLayout}.</li>
 *   <li>Tags Material Components : {@code com.google.android.material.button.MaterialButton},
 *       {@code com.google.android.material.textfield.TextInputLayout},
 *       {@code com.google.android.material.card.MaterialCardView}.</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 1.0
 */
public final class ViewTagRegistry {

    /** Préfixe des classes Design du LayoutEditor. */
    private static final String DESIGN_PACKAGE = "jo.layoutlib.design.";

    /** Préfixe des classes du framework Android. */
    private static final String WIDGET_PACKAGE = "android.widget.";

    /** Préfixe des classes de vue du framework Android. */
    private static final String VIEW_PACKAGE = "android.view.";

    /** Préfixe AndroidX. */
    private static final String ANDROIDX_PACKAGE = "androidx.";

    /** Préfixe Material Components. */
    private static final String MATERIAL_PACKAGE = "com.google.android.material.";

    /** Map des tags courts → nom pleinement qualifié de la classe Design. */
    private final Map<String, String> shortTagToDesignClass = new HashMap<>();

    /** Map des tags courts → nom pleinement qualifié de la classe Android native. */
    private final Map<String, String> shortTagToNativeClass = new HashMap<>();

    /** Indique si l'on préfère utiliser les classes Design quand elles existent. */
    private final boolean preferDesignClasses;

    /**
     * Construit un nouveau registre avec la stratégie de résolution spécifiée.
     *
     * @param preferDesignClasses si {@code true}, les classes Design sont
     *                            privilégiées quand elles existent ; si
     *                            {@code false}, les classes natives Android
     *                            sont utilisées
     */
    public ViewTagRegistry(boolean preferDesignClasses) {
        this.preferDesignClasses = preferDesignClasses;
        registerDefaults();
    }

    /**
     * Construit un registre par défaut qui privilégie les classes Design.
     * Équivalent à {@code new ViewTagRegistry(true)}.
     */
    public ViewTagRegistry() {
        this(true);
    }

    /**
     * Enregistre les mappings par défaut du framework Android + Design.
     */
    private void registerDefaults() {
        // Vues de base
        register("View", "android.view.View", "widgets.ViewDesign");
        register("ViewGroup", "android.view.ViewGroup", "widgets.ViewDesign");

        // Texte
        register("TextView", "android.widget.TextView", "widgets.TextViewDesign");
        register("Button", "android.widget.Button", "buttons.ButtonDesign");
        register("EditText", "android.widget.EditText", "widgets.TextViewDesign");
        register("TextInputEditText", "android.widget.EditText", "widgets.TextViewDesign");
        register("AutoCompleteTextView", "android.widget.AutoCompleteTextView", "widgets.TextViewDesign");
        register("MultiAutoCompleteTextView", "android.widget.MultiAutoCompleteTextView", "widgets.TextViewDesign");
        register("CheckBox", "android.widget.CheckBox", "buttons.ButtonDesign");
        register("RadioButton", "android.widget.RadioButton", "buttons.ButtonDesign");
        register("CheckedTextView", "android.widget.CheckedTextView", "widgets.TextViewDesign");
        register("Switch", "android.widget.Switch", "buttons.ButtonDesign");
        register("ToggleButton", "android.widget.ToggleButton", "buttons.ButtonDesign");
        register("TextClock", "android.widget.TextClock", "widgets.TextViewDesign");
        register("TextSwitcher", "android.widget.TextSwitcher", "widgets.TextViewDesign");

        // Image et media
        register("ImageView", "android.widget.ImageView", "widgets.ImageViewDesign");
        register("ImageButton", "android.widget.ImageButton", "widgets.ImageViewDesign");
        register("SurfaceView", "android.view.SurfaceView", "widgets.ViewDesign");
        register("TextureView", "android.view.TextureView", "widgets.ViewDesign");

        // Layouts
        register("LinearLayout", "android.widget.LinearLayout", "layouts.LinearLayoutDesign");
        register("FrameLayout", "android.widget.FrameLayout", "layouts.FrameLayoutDesign");
        register("RelativeLayout", "android.widget.RelativeLayout", "layouts.FrameLayoutDesign");
        register("TableLayout", "android.widget.TableLayout", "layouts.LinearLayoutDesign");
        register("TableRow", "android.widget.TableRow", "layouts.LinearLayoutDesign");
        register("GridLayout", "android.widget.GridLayout", "layouts.LinearLayoutDesign");
        register("ScrollView", "android.widget.ScrollView", "layouts.ScrollViewDesign");
        register("HorizontalScrollView", "android.widget.HorizontalScrollView", "layouts.ScrollViewDesign");
        register("NestedScrollView", "android.widget.ScrollView", "layouts.ScrollViewDesign");

        // Conteneurs avancés
        register("ListView", "android.widget.ListView", "layouts.FrameLayoutDesign");
        register("GridView", "android.widget.GridView", "layouts.FrameLayoutDesign");
        register("Spinner", "android.widget.Spinner", "layouts.FrameLayoutDesign");
        register("RecyclerView", "android.support.v7.widget.RecyclerView", "layouts.FrameLayoutDesign");
        register("ViewFlipper", "android.widget.ViewFlipper", "layouts.FrameLayoutDesign");
        register("ViewSwitcher", "android.widget.ViewSwitcher", "layouts.FrameLayoutDesign");
        register("AdapterViewFlipper", "android.widget.AdapterViewFlipper", "layouts.FrameLayoutDesign");
        register("StackView", "android.widget.StackView", "layouts.FrameLayoutDesign");

        // Barres
        register("ProgressBar", "android.widget.ProgressBar", "widgets.ViewDesign");
        register("SeekBar", "android.widget.SeekBar", "widgets.ViewDesign");
        register("RatingBar", "android.widget.RatingBar", "widgets.ViewDesign");
        register("QuickContactBadge", "android.widget.QuickContactBadge", "widgets.ImageViewDesign");

        // Divers
        register("ViewStub", "android.view.ViewStub", "widgets.ViewDesign");
        register("WebView", "android.webkit.WebView", "widgets.ViewDesign");
        register("AnalogClock", "android.widget.AnalogClock", "widgets.ViewDesign");
        register("Chronometer", "android.widget.Chronometer", "widgets.TextViewDesign");
        register("DatePicker", "android.widget.DatePicker", "layouts.FrameLayoutDesign");
        register("TimePicker", "android.widget.TimePicker", "layouts.FrameLayoutDesign");
        register("CalendarView", "android.widget.CalendarView", "layouts.FrameLayoutDesign");
        register("NumberPicker", "android.widget.NumberPicker", "layouts.LinearLayoutDesign");
        register("SearchView", "android.widget.SearchView", "layouts.LinearLayoutDesign");
        register("TabHost", "android.widget.TabHost", "layouts.FrameLayoutDesign");
        register("TabWidget", "android.widget.TabWidget", "layouts.FrameLayoutDesign");
        register("ZoomControls", "android.widget.ZoomControls", "layouts.LinearLayoutDesign");
        register("Space", "android.widget.Space", "widgets.ViewDesign");
        register("ViewAnimator", "android.widget.ViewAnimator", "layouts.FrameLayoutDesign");
        register("GestureOverlayView", "android.gesture.GestureOverlayView", "layouts.FrameLayoutDesign");
        register("ExtractEditText", "android.widget.ExtractEditText", "widgets.TextViewDesign");

        // AppCompat views (AndroidX)
        register("AppCompatTextView", "androidx.appcompat.widget.AppCompatTextView", "widgets.TextViewDesign");
        register("AppCompatButton", "androidx.appcompat.widget.AppCompatButton", "buttons.ButtonDesign");
        register("AppCompatEditText", "androidx.appcompat.widget.AppCompatEditText", "widgets.TextViewDesign");
        register("AppCompatImageView", "androidx.appcompat.widget.AppCompatImageView", "widgets.ImageViewDesign");
        register("AppCompatCheckBox", "androidx.appcompat.widget.AppCompatCheckBox", "buttons.ButtonDesign");
        register("AppCompatRadioButton", "androidx.appcompat.widget.AppCompatRadioButton", "buttons.ButtonDesign");
        register("AppCompatSpinner", "androidx.appcompat.widget.AppCompatSpinner", "layouts.FrameLayoutDesign");
        register("AppCompatSeekBar", "androidx.appcompat.widget.AppCompatSeekBar", "widgets.ViewDesign");
        register("AppCompatRatingBar", "androidx.appcompat.widget.AppCompatRatingBar", "widgets.ViewDesign");
        register("AppCompatAutoCompleteTextView", "androidx.appcompat.widget.AppCompatAutoCompleteTextView", "widgets.TextViewDesign");
        register("AppCompatMultiAutoCompleteTextView", "androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView", "widgets.TextViewDesign");
        register("AppCompatCheckedTextView", "androidx.appcompat.widget.AppCompatCheckedTextView", "widgets.TextViewDesign");
        register("AppCompatImageButton", "androidx.appcompat.widget.AppCompatImageButton", "widgets.ImageViewDesign");
        register("ActionBarContainer", "androidx.appcompat.widget.ActionBarContainer", "layouts.FrameLayoutDesign");
        register("ActionBarContextView", "androidx.appcompat.widget.ActionBarContextView", "layouts.LinearLayoutDesign");
        register("ActionBarOverlayLayout", "androidx.appcompat.widget.ActionBarOverlayLayout", "layouts.FrameLayoutDesign");
        register("ActionBarLayout", "androidx.appcompat.widget.ActionBarLayout", "layouts.FrameLayoutDesign");
        register("ContentFrameLayout", "androidx.appcompat.widget.ContentFrameLayout", "layouts.FrameLayoutDesign");
        register("FitWindowsFrameLayout", "androidx.appcompat.widget.FitWindowsFrameLayout", "layouts.FrameLayoutDesign");
        register("FitWindowsLinearLayout", "androidx.appcompat.widget.FitWindowsLinearLayout", "layouts.LinearLayoutDesign");
        register("FitWindowsViewGroup", "androidx.appcompat.widget.FitWindowsViewGroup", "layouts.FrameLayoutDesign");
        register("ScrollingTabContainerView", "androidx.appcompat.widget.ScrollingTabContainerView", "layouts.LinearLayoutDesign");
        register("Toolbar", "androidx.appcompat.widget.Toolbar", "layouts.FrameLayoutDesign");
        register("VectorEnabledTintTextView", "androidx.appcompat.widget.VectorEnabledTintTextView", "widgets.TextViewDesign");

        // Material Components
        register("MaterialButton", "com.google.android.material.button.MaterialButton", "buttons.ButtonDesign");
        register("MaterialTextView", "com.google.android.material.textview.MaterialTextView", "widgets.TextViewDesign");
        register("MaterialCardView", "com.google.android.material.card.MaterialCardView", "layouts.FrameLayoutDesign");
        register("MaterialCheckBox", "com.google.android.material.checkbox.MaterialCheckBox", "buttons.ButtonDesign");
        register("MaterialRadioButton", "com.google.android.material.radiobutton.MaterialRadioButton", "buttons.ButtonDesign");
        register("MaterialSwitch", "com.google.android.material.materialswitch.MaterialSwitch", "buttons.ButtonDesign");
        register("FloatingActionButton", "com.google.android.material.floatingactionbutton.FloatingActionButton", "widgets.ImageViewDesign");
        register("ExtendedFloatingActionButton", "com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton", "buttons.ButtonDesign");
        register("Chip", "com.google.android.material.chip.Chip", "buttons.ButtonDesign");
        register("ChipGroup", "com.google.android.material.chip.ChipGroup", "layouts.LinearLayoutDesign");
        register("NavigationView", "com.google.android.material.navigation.NavigationView", "layouts.FrameLayoutDesign");
        register("BottomNavigationView", "com.google.android.material.bottomnavigation.BottomNavigationView", "layouts.FrameLayoutDesign");
        register("NavigationRailView", "com.google.android.material.navigationrail.NavigationRailView", "layouts.FrameLayoutDesign");
        register("AppBarLayout", "com.google.android.material.appbar.AppBarLayout", "layouts.LinearLayoutDesign");
        register("CollapsingToolbarLayout", "com.google.android.material.appbar.CollapsingToolbarLayout", "layouts.FrameLayoutDesign");
        register("CoordinatorLayout", "androidx.coordinatorlayout.widget.CoordinatorLayout", "layouts.FrameLayoutDesign");
        register("TextInputLayout", "com.google.android.material.textfield.TextInputLayout", "layouts.LinearLayoutDesign");
        register("TextInputEditText", "com.google.android.material.textfield.TextInputEditText", "widgets.TextViewDesign");
        register("MaterialAutoCompleteTextView", "com.google.android.material.textfield.MaterialAutoCompleteTextView", "widgets.TextViewDesign");
        register("BottomSheetDialog", "com.google.android.material.bottomsheet.BottomSheetDialog", "layouts.FrameLayoutDesign");
        register("BottomSheetDialogFragment", "com.google.android.material.bottomsheet.BottomSheetDialogFragment", "layouts.FrameLayoutDesign");
        register("Snackbar", "com.google.android.material.snackbar.Snackbar", "layouts.LinearLayoutDesign");
        register("SnackbarLayout", "com.google.android.material.snackbar.SnackbarLayout", "layouts.LinearLayoutDesign");
        register("MaterialProgressIndicator", "com.google.android.material.progressindicator.MaterialProgressIndicator", "widgets.ViewDesign");
        register("LinearProgressIndicator", "com.google.android.material.progressindicator.LinearProgressIndicator", "widgets.ViewDesign");
        register("CircularProgressIndicator", "com.google.android.material.progressindicator.CircularProgressIndicator", "widgets.ViewDesign");
        register("Slider", "com.google.android.material.slider.Slider", "widgets.ViewDesign");
        register("RangeSlider", "com.google.android.material.slider.RangeSlider", "widgets.ViewDesign");
        register("MaterialTimePicker", "com.google.android.material.timepicker.MaterialTimePicker", "layouts.FrameLayoutDesign");
        register("MaterialDatePicker", "com.google.android.material.datepicker.MaterialDatePicker", "layouts.FrameLayoutDesign");
        register("BadgeView", "com.google.android.material.badge.BadgeView", "widgets.ViewDesign");
        register("Tooltip", "com.google.android.material.tooltip.Tooltip", "widgets.ViewDesign");

        // AndroidX Layouts
        register("ConstraintLayout", "androidx.constraintlayout.widget.ConstraintLayout", "layouts.FrameLayoutDesign");
        register("ConstraintLayoutHelper", "androidx.constraintlayout.widget.ConstraintLayout", "layouts.FrameLayoutDesign");
        register("Guideline", "androidx.constraintlayout.widget.Guideline", "widgets.ViewDesign");
        register("Barrier", "androidx.constraintlayout.widget.Barrier", "widgets.ViewDesign");
        register("Group", "androidx.constraintlayout.widget.Group", "widgets.ViewDesign");
        register("Layer", "androidx.constraintlayout.widget.Layer", "widgets.ViewDesign");
        register("Chain", "androidx.constraintlayout.widget.Chain", "widgets.ViewDesign");
        register("Flow", "androidx.constraintlayout.helper.widget.Flow", "widgets.ViewDesign");
        register("ConstraintProperties", "androidx.constraintlayout.widget.ConstraintProperties", "widgets.ViewDesign");
        register("MotionLayout", "androidx.constraintlayout.motion.widget.MotionLayout", "layouts.FrameLayoutDesign");
        register("RecyclerView", "androidx.recyclerview.widget.RecyclerView", "layouts.FrameLayoutDesign");
        register("GridLayoutManager", "androidx.recyclerview.widget.GridLayoutManager", "widgets.ViewDesign");
        register("LinearLayoutManager", "androidx.recyclerview.widget.LinearLayoutManager", "widgets.ViewDesign");
        register("StaggeredGridLayoutManager", "androidx.recyclerview.widget.StaggeredGridLayoutManager", "widgets.ViewDesign");
        register("ViewPager", "androidx.viewpager.widget.ViewPager", "layouts.FrameLayoutDesign");
        register("ViewPager2", "androidx.viewpager2.widget.ViewPager2", "layouts.FrameLayoutDesign");
        register("ViewPager3", "androidx.viewpager2.widget.ViewPager2", "layouts.FrameLayoutDesign");
        register("TabLayout", "com.google.android.material.tabs.TabLayout", "layouts.LinearLayoutDesign");
        register("TabItem", "com.google.android.material.tabs.TabItem", "layouts.LinearLayoutDesign");
        register("CardView", "androidx.cardview.widget.CardView", "layouts.FrameLayoutDesign");
        register("GridLayout", "androidx.gridlayout.widget.GridLayout", "layouts.LinearLayoutDesign");
        register("NestedScrollView", "androidx.core.widget.NestedScrollView", "layouts.ScrollViewDesign");
        register("SwipeRefreshLayout", "androidx.swiperefreshlayout.widget.SwipeRefreshLayout", "layouts.FrameLayoutDesign");
        register("DrawerLayout", "androidx.drawerlayout.widget.DrawerLayout", "layouts.FrameLayoutDesign");
        register("CoordinatorLayout_Layout", "androidx.coordinatorlayout.widget.CoordinatorLayout", "layouts.FrameLayoutDesign");
        register("BadgeFrameLayout", "com.google.android.material.badge.BadgeFrameLayout", "layouts.FrameLayoutDesign");
        register("CircularBorderDrawable", "com.google.android.material.circularborder.CircularBorderDrawable", "widgets.ViewDesign");
        register("CircularBorderDrawableLollipop", "com.google.android.material.circularborder.CircularBorderDrawableLollipop", "widgets.ViewDesign");
        register("BottomAppBar", "com.google.android.material.bottomappbar.BottomAppBar", "buttons.ButtonDesign");
        register("ShapeableImageView", "com.google.android.material.imageview.ShapeableImageView", "widgets.ImageViewDesign");
        register("MaterialDivider", "com.google.android.material.divider.MaterialDivider", "widgets.ViewDesign");

        // Android Framework views additionnelles
        register("AbsListView", "android.widget.AbsListView", "layouts.FrameLayoutDesign");
        register("AbsSpinner", "android.widget.AbsSpinner", "layouts.FrameLayoutDesign");
        register("AbsSeekBar", "android.widget.AbsSeekBar", "widgets.ViewDesign");
        register("AdapterView", "android.widget.AdapterView", "layouts.FrameLayoutDesign");
        register("AdapterViewAnimator", "android.widget.AdapterViewAnimator", "layouts.FrameLayoutDesign");
        register("AdapterViewFlipper", "android.widget.AdapterViewFlipper", "layouts.FrameLayoutDesign");
        register("AlphabetIndexer", "android.widget.AlphabetIndexer", "widgets.ViewDesign");
        register("AppSecurityPayload", "android.widget.AppSecurityPayload", "widgets.ViewDesign");
        register("DateTimeView", "android.widget.DateTimeView", "widgets.TextViewDesign");
        register("DialogView", "android.app.Dialog", "layouts.FrameLayoutDesign");
        register("EdgeEffect", "android.widget.EdgeEffect", "widgets.ViewDesign");
        register("FastScroller", "android.widget.FastScroller", "widgets.ViewDesign");
        register("HeaderViewListAdapter", "android.widget.HeaderViewListAdapter", "widgets.ViewDesign");
        register("HeteroExpandableListCursorAdapter", "android.widget.HeteroExpandableListCursorAdapter", "widgets.ViewDesign");
        register("ListPopupWindow", "android.widget.ListPopupWindow", "layouts.FrameLayoutDesign");
        register("Magnifier", "android.widget.Magnifier", "widgets.ViewDesign");
        register("MediaController", "android.widget.MediaController", "layouts.FrameLayoutDesign");
        register("PinnedSectionListView", "android.widget.PinnedSectionListView", "layouts.FrameLayoutDesign");
        register("PopupMenu", "android.widget.PopupMenu", "layouts.FrameLayoutDesign");
        register("PopupWindow", "android.widget.PopupWindow", "layouts.FrameLayoutDesign");
        register("ProgressBar", "android.widget.ProgressBar", "widgets.ViewDesign");
        register("SelectionBoundsShape", "android.widget.SelectionBoundsShape", "widgets.ViewDesign");
        register("ShareActionProvider", "android.widget.ShareActionProvider", "widgets.ViewDesign");
        register("SimpleAdapter", "android.widget.SimpleAdapter", "widgets.ViewDesign");
        register("SimpleCursorAdapter", "android.widget.SimpleCursorAdapter", "widgets.ViewDesign");
        register("SimpleExpandableListAdapter", "android.widget.SimpleExpandableListAdapter", "widgets.ViewDesign");
        register("SlidingDrawer", "android.widget.SlidingDrawer", "layouts.FrameLayoutDesign");
        register("SpellChecker", "android.widget.SpellChecker", "widgets.ViewDesign");
        register("SuggestionsAdapter", "android.widget.SuggestionsAdapter", "widgets.ViewDesign");
        register("Toast", "android.widget.Toast", "layouts.FrameLayoutDesign");
        register("TwoLineListItem", "android.widget.TwoLineListItem", "layouts.LinearLayoutDesign");
        register("VideoView", "android.widget.VideoView", "widgets.ViewDesign");
        register("ViewSwitcher", "android.widget.ViewSwitcher", "layouts.FrameLayoutDesign");
        register("ZoomButton", "android.widget.ZoomButton", "buttons.ButtonDesign");
        register("ExpandableListView", "android.widget.ExpandableListView", "layouts.FrameLayoutDesign");
        register("GadgetOptions", "android.widget.GadgetOptions", "widgets.ViewDesign");
        register("RemoteViews", "android.widget.RemoteViews", "widgets.ViewDesign");
        register("RemoteViewsAdapter", "android.widget.RemoteViewsAdapter", "widgets.ViewDesign");
        register("RemoteViewsService", "android.widget.RemoteViewsService", "widgets.ViewDesign");
        register("SectionIndexer", "android.widget.SectionIndexer", "widgets.ViewDesign");
        register("StockAppWidgetHost", "android.widget.StockAppWidgetHost", "widgets.ViewDesign");
        register("Text EllipsizingTextView", "android.widget.TextEllipsizingTextView", "widgets.TextViewDesign");

        // Tags spéciaux
        register("merge", "android.view.ViewGroup", null);   // Traité séparément
        register("include", null, null);                       // Traité séparément
        register("requestFocus", null, null);                  // Traité séparément
    }

    /**
     * Enregistre un mapping tag → classes.
     *
     * @param tag           nom du tag XML (sans préfixe)
     * @param nativeClass   nom pleinement qualifié de la classe native Android,
     *                      ou {@code null} si non supporté nativement
     * @param designClassSuffix suffixe de la classe Design (ajouté au préfixe
     *                      {@code jo.layoutlib.design.}), ou
     *                      {@code null} si pas de classe Design
     */
    public void register(String tag, String nativeClass, String designClassSuffix) {
        shortTagToNativeClass.put(tag, nativeClass);
        if (designClassSuffix != null) {
            shortTagToDesignClass.put(tag, DESIGN_PACKAGE + designClassSuffix);
        }
    }

    /**
     * Résout un nom de tag vers le nom pleinement qualifié de la classe à
     * instancier.
     *
     * <p>L'algorithme de résolution est le suivant :</p>
     * <ol>
     *   <li>Si le tag contient un point (ex. {@code androidx.recyclerview...}),
     *       on le retourne tel quel : c'est déjà un nom pleinement qualifié.</li>
     *   <li>Sinon, on cherche dans la map des tags courts. Si
     *       {@code preferDesignClasses} est vrai et qu'une classe Design existe,
     *       on la retourne ; sinon on retourne la classe native.</li>
     *   <li>Si le tag n'est pas trouvé, on tente la résolution dans
     *       {@code android.widget.*} puis {@code android.view.*}.</li>
     * </ol>
     *
     * @param tag le nom du tag XML
     * @return le nom pleinement qualifié de la classe, ou {@code null} si le
     *         tag est un tag spécial ({@code include}, {@code merge},
     *         {@code requestFocus}) qui ne correspond pas à une vue
     * @throws InflateException si le tag ne peut pas être résolu du tout
     */
    public String resolveClassName(String tag) {
        if (tag == null || tag.isEmpty()) {
            throw new InflateException("Nom de tag vide ou null");
        }

        // Tag spécial sans vue correspondante
        if ("include".equals(tag) || "merge".equals(tag) || "requestFocus".equals(tag)) {
            return null;
        }

        // Tag pleinement qualifié (contient un point)
        if (tag.indexOf('.') >= 0) {
            return tag;
        }

        // Tag court : on cherche dans les maps
        String designClass = shortTagToDesignClass.get(tag);
        String nativeClass = shortTagToNativeClass.get(tag);

        if (preferDesignClasses && designClass != null) {
            return designClass;
        }
        if (nativeClass != null) {
            return nativeClass;
        }
        if (designClass != null) {
            return designClass;
        }

        // Tentative de résolution par défaut dans android.widget puis android.view
        if (startsWithUppercase(tag)) {
            return WIDGET_PACKAGE + tag;
        }

        throw new InflateException("Tag inconnu et non résolvable : " + tag);
    }

    /**
     * Indique si un tag est un tag spécial géré hors du mécanisme de réflexion.
     *
     * @param tag le nom du tag
     * @return {@code true} si c'est un tag spécial ({@code include}, {@code merge},
     *         {@code requestFocus}, {@code blink})
     */
    public boolean isSpecialTag(String tag) {
        return "include".equals(tag)
                || "merge".equals(tag)
                || "requestFocus".equals(tag)
                || "blink".equals(tag);
    }

    /**
     * Indique si un tag correspond à une classe Design connue.
     *
     * @param tag le nom du tag
     * @return {@code true} si une classe Design existe pour ce tag
     */
    public boolean hasDesignClass(String tag) {
        return shortTagToDesignClass.containsKey(tag);
    }

    /**
     * @return une copie défensive de la map des tags courts → classe Design
     */
    public Map<String, String> getDesignClassMappings() {
        return new HashMap<>(shortTagToDesignClass);
    }

    /**
     * @return une copie défensive de la map des tags courts → classe native
     */
    public Map<String, String> getNativeClassMappings() {
        return new HashMap<>(shortTagToNativeClass);
    }

    /**
     * Vérifie qu'un tag commence par une majuscule (heuristique pour
     * identifier les noms de classes Java).
     *
     * @param tag le tag à tester
     * @return {@code true} si le premier caractère est une majuscule
     */
    private static boolean startsWithUppercase(String tag) {
        if (tag == null || tag.isEmpty()) {
            return false;
        }
        char first = tag.charAt(0);
        return first >= 'A' && first <= 'Z';
    }
}
