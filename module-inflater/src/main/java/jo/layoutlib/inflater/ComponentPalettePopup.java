package jo.layoutlib.inflater;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * Popup de palette de composants Android (TextView, Button, EditText, etc.)
 * inspiré du panneau Palette d'Android Studio.
 *
 * <p>Affiche une grille de cartes (icône vectorielle + nom) qui déclenchent
 * un callback quand cliquées. Utilisé à la fois par le FAB popup et par le
 * panneau palette du bottom sheet.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ComponentPalettePopup popup = new ComponentPalettePopup(context);
 * popup.setOnComponentSelectedListener(name -> {
 *     insertXmlSnippet(name);
 * });
 * popup.showAsDropDown(anchorView);
 * }</pre>
 *
 * @author jo@Dev
 * @since 1.1
 */
public class ComponentPalettePopup extends PopupWindow {

    /** Listener de sélection d'un composant. */
    public interface OnComponentSelectedListener {
        void onComponentSelected(String componentName);
    }

    /** Composants par défaut : {nom, resId icône (0 = pas d'icône)}. */
    private static final Object[][] DEFAULT_COMPONENTS = {
        {"TextView", "T"},
        {"Button", "B"},
        {"EditText", "E"},
        {"ImageView", "I"},
        {"CheckBox", "C"},
        {"Switch", "S"},
    };

    private OnComponentSelectedListener listener;

    public ComponentPalettePopup(Context context) {
        this(context, DEFAULT_COMPONENTS);
    }

    public ComponentPalettePopup(Context context, Object[][] components) {
        super(makeContentView(context, components),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        setFocusable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(null);
        setElevation(20);
        setAnimationStyle(android.R.style.Animation_Dialog);
    }

    private static View makeContentView(Context context, Object[][] components) {
        float density = context.getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (12 * density);
        root.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(18 * density);
        bg.setColor(Color.parseColor("#B3211F35")); // bg-glass 0.72 alpha
        bg.setStroke((int) (1 * density), Color.parseColor("#322F47"));
        root.setBackground(bg);

        // Header
        TextView header = new TextView(context);
        header.setText("QUICK ADD");
        header.setTextColor(Color.parseColor("#9B95AD"));
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.setMargins((int) (6 * density), (int) (4 * density), 0, (int) (10 * density));
        header.setLayoutParams(hlp);
        root.addView(header);

        // Grid 3 colonnes
        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(3);
        grid.setUseDefaultMargins(true);
        int gridWidth = (int) (264 * density);
        grid.setLayoutParams(new LinearLayout.LayoutParams(gridWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        for (Object[] comp : components) {
            grid.addView(createItem(context, (String) comp[0], (String) comp[1]));
        }
        root.addView(grid);
        return root;
    }

    private static View createItem(Context context, String name, String iconGlyph) {
        float density = context.getResources().getDisplayMetrics().density;

        LinearLayout item = new LinearLayout(context);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        int pad = (int) (10 * density);
        item.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(11 * density);
        bg.setColor(Color.parseColor("#08FFFFFF"));
        item.setBackground(bg);

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        item.setLayoutParams(lp);

        // Icon placeholder (texte monospace stylisé)
        TextView iconView = new TextView(context);
        iconView.setText(iconGlyph);
        iconView.setTextColor(Color.parseColor("#D0BCFF"));
        iconView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        iconView.setTypeface(Typeface.MONOSPACE);
        iconView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(
                (int) (32 * density), (int) (32 * density));
        iconView.setLayoutParams(iconLp);
        item.addView(iconView);

        // Name
        TextView nameView = new TextView(context);
        nameView.setText(name);
        nameView.setTextColor(Color.parseColor("#C9C3D6"));
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        nameView.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameLp.setMargins(0, (int) (5 * density), 0, 0);
        nameView.setLayoutParams(nameLp);
        item.addView(nameView);

        item.setTag(name);
        return item;
    }

    public void setOnComponentSelectedListener(OnComponentSelectedListener l) {
        this.listener = l;
        View root = getContentView();
        if (root instanceof LinearLayout) {
            LinearLayout layout = (LinearLayout) root;
            // Le 2e enfant est le GridLayout
            if (layout.getChildCount() >= 2 && layout.getChildAt(1) instanceof GridLayout) {
                GridLayout grid = (GridLayout) layout.getChildAt(1);
                for (int i = 0; i < grid.getChildCount(); i++) {
                    View child = grid.getChildAt(i);
                    child.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onComponentSelected((String) v.getTag());
                        }
                        dismiss();
                    });
                }
            }
        }
    }
}
