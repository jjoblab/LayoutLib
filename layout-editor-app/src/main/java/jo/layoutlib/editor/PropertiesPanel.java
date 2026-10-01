package jo.layoutlib.editor;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import jo.layoutlib.inflater.ColorSwatchPopup;
import jo.layoutlib.inflater.ViewInfoCollector;

/**
 * Panneau de propriétés — identique au preview HTML.
 *
 * <p>5 sections :</p>
 * <ul>
 *   <li><strong>Identity</strong> — id (full)</li>
 *   <li><strong>Text</strong> — text (full), hint (full), textSize, textStyle (select),
 *       textColor (color), hintTextColor (color)</li>
 *   <li><strong>Layout</strong> — layout_width (select), layout_height, marginTop,
 *       marginBottom, paddingH, gravity (select)</li>
 *   <li><strong>Input</strong> — inputType (select, full), imeOptions (select), maxLines</li>
 *   <li><strong>Style</strong> — background (full), backgroundTint (color, full)</li>
 * </ul>
 *
 * <p>3 types de champs :</p>
 * <ul>
 *   <li><strong>Texte</strong> — EditText monospace</li>
 *   <li><strong>Couleur</strong> — EditText + swatch cliquable → ColorSwatchPopup</li>
 *   <li><strong>Select</strong> — EditText non éditable + chevron → SelectPopup (pas de Spinner)</li>
 * </ul>
 *
 * <p>Les selects utilisent un popup (SelectPopup) au lieu d'un dropdown/AlertDialog,
 * comme demandé.</p>
 *
 * @author jo@Dev
 * @since 3.2
 */
public class PropertiesPanel {

    private final Context context;
    private final LinearLayout container;
    private OnPropertyChangeListener propertyListener;

    /** Options pour les champs select (identiques au HTML preview). */
    private static final String[] TEXT_STYLE_OPTIONS = {"normal", "bold", "italic"};
    private static final String[] WIDTH_OPTIONS = {"match_parent", "wrap_content"};
    private static final String[] GRAVITY_OPTIONS = {"start", "center", "end"};
    private static final String[] INPUT_TYPE_OPTIONS = {
        "textEmailAddress", "textPassword", "text", "number"
    };
    private static final String[] IME_OPTIONS = {"actionNext", "actionDone"};

    /** 12 couleurs du color popover (identiques au HTML preview). */
    private static final int[] SWATCH_COLORS = {
        0xFFE7E1EC, 0xFF8D88A6, 0xFF6750A4, 0xFF7C5CFF, 0xFFD0BCFF, 0xFF4AA3FF,
        0xFF6FDC8C, 0xFFFFB74D, 0xFFF4757A, 0xFF23213B, 0xFF0F0F17, 0xFFFFFFFF,
    };

    public interface OnPropertyChangeListener {
        void onPropertyChanged(String key, String value);
    }

    public PropertiesPanel(Context context, LinearLayout container) {
        this.context = context;
        this.container = container;
    }

    public void setOnPropertyChangeListener(OnPropertyChangeListener l) {
        this.propertyListener = l;
    }

    /**
     * Affiche les propriétés d'une vue sélectionnée, avec les valeurs réelles
     * extraites du XML via {@link XmlMutator} et de la {@link ViewInfo}.
     *
     * <p>Pour chaque champ :</p>
     * <ul>
     *   <li>Attributs XML (text, hint, textSize, textColor, etc.) →
     *       {@code XmlMutator.getAttributeById(xml, idName, attrName)}</li>
     *   <li>Dimensions et marges réelles (mesurées) →
     *       {@code ViewInfo.width/height/marginLeft/Top} converties en dp</li>
     *   <li>Si l'attribut est absent du XML, valeur par défaut</li>
     * </ul>
     *
     * @param info les ViewInfo de la vue sélectionnée (peut être null)
     * @param xml  le XML courant de l'éditeur (pour extraire les attributs)
     */
    public void showProperties(ViewInfoCollector.ViewInfo info, String xml) {
        container.removeAllViews();
        if (info == null) {
            TextView empty = new TextView(context);
            empty.setText(R.string.empty_no_selection);
            empty.setTextColor(Color.parseColor("#9B95AD"));
            empty.setTextSize(12);
            empty.setPadding(16, 16, 16, 16);
            container.addView(empty);
            return;
        }

        float density = context.getResources().getDisplayMetrics().density;
        String idName = info.idName;

        // Helper pour extraire un attribut du XML
        // Returns null if not found (caller uses default)
        java.util.function.Function<String, String> getAttr = attrName ->
                XmlMutator.getAttributeById(xml, idName, attrName);

        // Helper pour convertir px → dp
        java.util.function.IntFunction<String> pxToDp = px -> {
            if (px <= 0) return "0dp";
            return Math.round(px / density) + "dp";
        };

        // ════════ Section: Identity ════════
        GridLayout identityGrid = addSection("IDENTITY", 2);
        String idValue = idName != null ? "@+id/" + idName : "";
        addField(identityGrid, "id", idValue, "text", true, null);

        // ════════ Section: Text ════════
        GridLayout textGrid = addSection("TEXT", 2);
        addField(textGrid, "text", getAttr.apply("text"), "text", true, null);
        addField(textGrid, "hint", getAttr.apply("hint"), "text", true, null);
        addField(textGrid, "textSize",
                getAttr.apply("textSize") != null ? getAttr.apply("textSize") : "14sp",
                "text", false, null);
        addField(textGrid, "textStyle",
                getAttr.apply("textStyle") != null ? getAttr.apply("textStyle") : "normal",
                "select", false, TEXT_STYLE_OPTIONS);
        addField(textGrid, "textColor",
                getAttr.apply("textColor") != null ? getAttr.apply("textColor") : "#E7E1EC",
                "color", true, null);
        addField(textGrid, "hintTextColor",
                getAttr.apply("hintTextColor") != null ? getAttr.apply("hintTextColor") : "#8D88A6",
                "color", true, null);

        // ════════ Section: Layout ════════
        GridLayout layoutGrid = addSection("LAYOUT", 2);
        // layout_width : prefer XML value, fallback to measured width in dp
        String widthVal = getAttr.apply("layout_width");
        if (widthVal == null) widthVal = pxToDp.apply(info.width);
        addField(layoutGrid, "layout_width", widthVal, "select", false, WIDTH_OPTIONS);
        String heightVal = getAttr.apply("layout_height");
        if (heightVal == null) heightVal = pxToDp.apply(info.height);
        addField(layoutGrid, "layout_height", heightVal, "text", false, null);
        addField(layoutGrid, "marginTop",
                getAttr.apply("layout_marginTop") != null ? getAttr.apply("layout_marginTop") : pxToDp.apply(info.marginTop),
                "text", false, null);
        addField(layoutGrid, "marginBottom",
                getAttr.apply("layout_marginBottom") != null ? getAttr.apply("layout_marginBottom") : pxToDp.apply(info.marginBottom),
                "text", false, null);
        // paddingH : prefer paddingLeft or paddingStart
        String padH = getAttr.apply("padding");
        if (padH == null) padH = getAttr.apply("paddingLeft");
        if (padH == null) padH = pxToDp.apply(info.paddingLeft);
        addField(layoutGrid, "paddingH", padH, "text", false, null);
        addField(layoutGrid, "gravity",
                getAttr.apply("layout_gravity") != null ? getAttr.apply("layout_gravity") : "start",
                "select", false, GRAVITY_OPTIONS);

        // ════════ Section: Input ════════
        GridLayout inputGrid = addSection("INPUT", 2);
        addField(inputGrid, "inputType",
                getAttr.apply("inputType") != null ? getAttr.apply("inputType") : "text",
                "select", true, INPUT_TYPE_OPTIONS);
        addField(inputGrid, "imeOptions",
                getAttr.apply("imeOptions") != null ? getAttr.apply("imeOptions") : "actionNext",
                "select", false, IME_OPTIONS);
        addField(inputGrid, "maxLines",
                getAttr.apply("maxLines") != null ? getAttr.apply("maxLines") : "1",
                "text", false, null);

        // ════════ Section: Style ════════
        GridLayout styleGrid = addSection("STYLE", 2);
        addField(styleGrid, "background", getAttr.apply("background"), "text", true, null);
        addField(styleGrid, "backgroundTint",
                getAttr.apply("backgroundTint") != null ? getAttr.apply("backgroundTint") : "#23213B",
                "color", true, null);
    }

    /**
     * @deprecated Utiliser {@link #showProperties(ViewInfoCollector.ViewInfo, String)}
     * pour passer le XML et obtenir les valeurs réelles.
     */
    @Deprecated
    public void showProperties(ViewInfoCollector.ViewInfo info) {
        showProperties(info, null);
    }

    /** Affiche la palette de composants. */
    public void showPalette(PaletteAdapter.OnComponentSelectedListener listener) {
        container.removeAllViews();
        TextView title = new TextView(context);
        title.setText(R.string.palette_components);
        title.setTextColor(Color.parseColor("#E7E1EC"));
        title.setTextSize(13);
        title.setPadding(4, 4, 4, 8);
        container.addView(title);

        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(3);
        grid.setUseDefaultMargins(true);
        PaletteAdapter.populate(grid, listener);
        container.addView(grid);
    }

    /** Affiche l'arbre de hiérarchie. */
    public void showTree(ViewInfoCollector.ViewInfo rootInfo,
                         TreeAdapter.OnTreeSelectListener listener) {
        container.removeAllViews();
        if (rootInfo == null) {
            TextView empty = new TextView(context);
            empty.setText(R.string.empty_no_hierarchy);
            empty.setTextColor(Color.parseColor("#9B95AD"));
            empty.setPadding(16, 16, 16, 16);
            container.addView(empty);
            return;
        }
        TreeAdapter adapter = new TreeAdapter(container, listener);
        adapter.setRoot(rootInfo);
    }

    // ============================================================
    // SECTION + FIELD BUILDERS
    // ============================================================

    /**
     * Ajoute une section avec son titre + ligne de séparation.
     *
     * @param title   le titre (sera affiché en uppercase)
     * @param columns le nombre de colonnes de la grille
     * @return la GridLayout créée
     */
    private GridLayout addSection(String title, int columns) {
        float density = context.getResources().getDisplayMetrics().density;

        // Section title avec ligne de séparation (comme .prop-section-title::after)
        LinearLayout titleRow = new LinearLayout(context);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams titleRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleRowLp.setMargins((int) (4 * density), (int) (4 * density), 0, (int) (8 * density));
        titleRow.setLayoutParams(titleRowLp);

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextColor(Color.parseColor("#9B95AD")); // on-surface-3
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setLetterSpacing(0.08f); // .8px approx
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.setMarginEnd((int) (6 * density));
        titleView.setLayoutParams(titleLp);
        titleRow.addView(titleView);

        // Ligne de séparation (flex 1, height 1px, outline-3)
        View divider = new View(context);
        divider.setBackgroundColor(Color.parseColor("#FF252338")); // outline-3
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                0, (int) (1 * density), 1f);
        divider.setLayoutParams(divLp);
        titleRow.addView(divider);

        container.addView(titleRow);

        // Grid
        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(columns);
        grid.setUseDefaultMargins(true);
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gridLp.bottomMargin = (int) (14 * density); // margin-bottom 14px
        grid.setLayoutParams(gridLp);
        container.addView(grid);
        return grid;
    }

    /**
     * Ajoute un champ à la grille.
     *
     * @param grid     la grille cible
     * @param label    le label du champ
     * @param value    la valeur initiale
     * @param type     "text" | "color" | "select"
     * @param full     true si le champ prend toute la largeur (2 colonnes)
     * @param options  les options pour le type "select" (null sinon)
     */
    private void addField(GridLayout grid, String label, String value,
                           String type, boolean full, String[] options) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View field = inflater.inflate(R.layout.item_prop_field, grid, false);

        TextView labelView = field.findViewById(R.id.propLabel);
        labelView.setText(label);

        EditText input = field.findViewById(R.id.propInput);
        // Handle null value : show empty with hint "(empty)" for text fields
        if (value != null) {
            input.setText(value);
        } else {
            input.setText("");
            input.setHint("(empty)");
            input.setTextColor(Color.parseColor("#6F6A87")); // placeholder gris
        }
        input.setTag(label); // pour identifier le champ dans le listener

        View swatch = field.findViewById(R.id.propSwatch);
        ImageView chevron = field.findViewById(R.id.propChevron);

        switch (type) {
            case "color":
                // Swatch visible + padding-left pour l'EditText
                swatch.setVisibility(View.VISIBLE);
                input.setPadding(
                        (int) (36 * context.getResources().getDisplayMetrics().density),
                        input.getPaddingTop(),
                        input.getPaddingEnd(),
                        input.getPaddingBottom());
                // Configurer la couleur du swatch
                try {
                    int color = Color.parseColor(value);
                    setSwatchColor(swatch, color);
                    swatch.setTag(color);
                } catch (Exception e) {
                    // Couleur invalide
                }
                // Clic sur le swatch → ColorSwatchPopup
                swatch.setOnClickListener(v -> openColorPicker(swatch, input));
                break;

            case "select":
                // Chevron visible + EditText non éditable
                chevron.setVisibility(View.VISIBLE);
                input.setInputType(android.text.InputType.TYPE_NULL);
                input.setFocusable(false);
                input.setClickable(true);
                input.setCursorVisible(false);
                // Clic sur l'EditText ou le chevron → SelectPopup
                View.OnClickListener selectClick = v -> openSelectPopup(input, label, options);
                input.setOnClickListener(selectClick);
                chevron.setOnClickListener(selectClick);
                break;

            case "text":
            default:
                // Rien de spécial, juste un EditText
                break;
        }

        // Wire change listener pour les champs texte et couleur
        if (!"select".equals(type)) {
            input.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (propertyListener != null && s != null) {
                        propertyListener.onPropertyChanged(label, s.toString());
                    }
                }
            });
        }

        // LayoutParams : full = 2 colonnes, sinon 1 colonne
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = full ? GridLayout.LayoutParams.MATCH_PARENT : 0;
        lp.height = GridLayout.LayoutParams.WRAP_CONTENT;
        if (full) {
            lp.columnSpec = GridLayout.spec(0, 2);
        } else {
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        }
        field.setLayoutParams(lp);
        grid.addView(field);
    }

    /**
     * Ouvre le ColorSwatchPopup (12 couleurs) pour un champ couleur.
     */
    private void openColorPicker(View swatch, EditText input) {
        ColorSwatchPopup popup = new ColorSwatchPopup(context, SWATCH_COLORS);
        Object tag = swatch.getTag();
        if (tag instanceof Integer) {
            popup.setInitialColor((Integer) tag);
        }
        popup.setOnColorSelectedListener(color -> {
            String hex = String.format("#%06X", color & 0xFFFFFF).toUpperCase();
            input.setText(hex);
            setSwatchColor(swatch, color);
            swatch.setTag(color);
            if (propertyListener != null) {
                propertyListener.onPropertyChanged((String) input.getTag(), hex);
            }
        });
        popup.showAsDropDown(swatch, -150, -180);
    }

    /**
     * Configure la couleur de fond du swatch.
     */
    private void setSwatchColor(View swatch, int color) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(6 * context.getResources().getDisplayMetrics().density);
        bg.setColor(color);
        bg.setStroke((int) (1.5f * context.getResources().getDisplayMetrics().density),
                Color.parseColor("#26FFFFFF"));
        swatch.setBackground(bg);
    }

    /**
     * Ouvre le SelectPopup (liste d'options verticale) pour un champ select.
     */
    private void openSelectPopup(EditText input, String label, String[] options) {
        String currentValue = input.getText().toString();
        SelectPopup popup = new SelectPopup(context, options, currentValue);
        popup.setOnOptionSelectedListener(value -> {
            input.setText(value);
            if (propertyListener != null) {
                propertyListener.onPropertyChanged(label, value);
            }
        });
        popup.showAsDropDown(input, 0, 0);
    }
}
