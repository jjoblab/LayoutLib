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
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * Popup de sélecteur de couleur (color swatch picker), inspiré des
 * éditeurs de couleur d'Android Studio.
 *
 * <p>Affiche une grille de swatches (échantillons de couleur) et un champ
 * hex. Quand l'utilisateur sélectionne une couleur, le listener est notifié.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ColorSwatchPopup popup = new ColorSwatchPopup(context);
 * popup.setInitialColor(currentColor);
 * popup.setOnColorSelectedListener(color -> {
 *     applyColor(color);
 * });
 * popup.showAsDropDown(anchorView);
 * }</pre>
 *
 * @author jo@Dev
 * @since 1.1
 */
public class ColorSwatchPopup extends PopupWindow {

    /** Listener de sélection de couleur. */
    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    /** Palette par défaut (12 couleurs Material 3 + quelques extras). */
    public static final int[] DEFAULT_COLORS = {
        0xFFE7E1EC, 0xFF8D88A6, 0xFF6750A4, 0xFF7C5CFF, 0xFFD0BCFF, 0xFF4AA3FF,
        0xFF6FDC8C, 0xFFFFB74D, 0xFFF4757A, 0xFF23213B, 0xFF0F0F17, 0xFFFFFFFF,
    };

    private OnColorSelectedListener listener;
    private TextView hexLabel;

    public ColorSwatchPopup(Context context) {
        this(context, DEFAULT_COLORS);
    }

    public ColorSwatchPopup(Context context, int[] colors) {
        super(makeContentView(context, colors),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        setFocusable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(null);
        setElevation(20);
    }

    private static View makeContentView(Context context, int[] colors) {
        float density = context.getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (10 * density);
        root.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(12 * density);
        bg.setColor(Color.parseColor("#FF2D2B48"));
        bg.setStroke((int) (1 * density), Color.parseColor("#322F47"));
        root.setBackground(bg);

        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(6);
        grid.setUseDefaultMargins(true);
        int width = (int) (168 * density);
        grid.setLayoutParams(new LinearLayout.LayoutParams(width,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        for (int color : colors) {
            grid.addView(createSwatch(context, color));
        }
        root.addView(grid);

        // Hex label
        TextView hex = new TextView(context);
        hex.setText("#E7E1EC");
        hex.setTextColor(Color.parseColor("#C9C3D6"));
        hex.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        hex.setTypeface(Typeface.MONOSPACE);
        hex.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.setMargins(0, (int) (8 * density), 0, 0);
        hex.setLayoutParams(hlp);
        root.addView(hex);

        // Wire listeners
        for (int i = 0; i < grid.getChildCount(); i++) {
            View child = grid.getChildAt(i);
            child.setOnClickListener(v -> {
                int color = (int) v.getTag();
                hex.setText(String.format("#%06X", color & 0xFFFFFF).toUpperCase());
                hex.setTextColor(color);
            });
        }

        return root;
    }

    private static View createSwatch(Context context, int color) {
        float density = context.getResources().getDisplayMetrics().density;
        int size = (int) (22 * density);

        View sw = new View(context);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(6 * density);
        bg.setColor(color);
        bg.setStroke((int) (1.5f * density), Color.parseColor("#26FFFFFF"));
        sw.setBackground(bg);

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = size;
        lp.height = size;
        sw.setLayoutParams(lp);
        sw.setTag(color);
        return sw;
    }

    public void setInitialColor(int color) {
        // Just update the hex label
        View root = getContentView();
        if (root instanceof LinearLayout) {
            LinearLayout layout = (LinearLayout) root;
            if (layout.getChildCount() >= 2 && layout.getChildAt(1) instanceof TextView) {
                TextView hex = (TextView) layout.getChildAt(1);
                hex.setText(String.format("#%06X", color & 0xFFFFFF).toUpperCase());
                hex.setTextColor(color);
            }
        }
    }

    public void setOnColorSelectedListener(OnColorSelectedListener l) {
        this.listener = l;
        View root = getContentView();
        if (root instanceof LinearLayout) {
            LinearLayout layout = (LinearLayout) root;
            if (layout.getChildAt(0) instanceof GridLayout) {
                GridLayout grid = (GridLayout) layout.getChildAt(0);
                TextView hex = (TextView) layout.getChildAt(1);
                for (int i = 0; i < grid.getChildCount(); i++) {
                    View child = grid.getChildAt(i);
                    child.setOnClickListener(v -> {
                        int color = (int) v.getTag();
                        hex.setText(String.format("#%06X", color & 0xFFFFFF).toUpperCase());
                        hex.setTextColor(color);
                        if (listener != null) listener.onColorSelected(color);
                    });
                }
            }
        }
    }
}
