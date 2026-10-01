package jo.layoutlib.inflater;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Liste verticale de "bp-box" représentant la hiérarchie de vues en mode Blueprint,
 * inspiré du mode Blueprint d'Android Studio.
 *
 * <p>Chaque vue de la hiérarchie est représentée par un rectangle dashed avec
 * un label monospace affichant la classe + l'id. La profondeur est indiquée
 * par l'indentation horizontale.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * BlueprintListView bp = findViewById(R.id.blueprintList);
 * bp.setViewInfo(rootInfo);
 * bp.setSelectedView(selectedInfo);
 * bp.setOnSelectListener(info -> { ... });
 * }</pre>
 *
 * @author jo@Dev
 * @since 1.1
 */
public class BlueprintListView extends LinearLayout {

    private ViewInfoCollector.ViewInfo rootInfo;
    private ViewInfoCollector.ViewInfo selectedInfo;
    private OnSelectionListener listener;

    private int colorBorder = Color.parseColor("#4AA3FF");
    private int colorBorderSelected = Color.parseColor("#7C5CFF");
    private int colorFill = Color.parseColor("#0F4AA3FF");
    private int colorFillSelected = Color.parseColor("#147C5CFF");
    private int colorText = Color.parseColor("#4AA3FF");
    private int colorTextSelected = Color.parseColor("#D0BCFF");
    private int colorIdText = Color.parseColor("#994AA3FF");

    public interface OnSelectionListener {
        void onSelectionChanged(ViewInfoCollector.ViewInfo info);
    }

    public BlueprintListView(Context context) {
        super(context);
        init();
    }

    public BlueprintListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        float density = getResources().getDisplayMetrics().density;
        setPadding((int) (8 * density), (int) (18 * density),
                (int) (8 * density), (int) (8 * density));
    }

    public void setViewInfo(ViewInfoCollector.ViewInfo info) {
        this.rootInfo = info;
        rebuild();
    }

    public void setSelectedView(ViewInfoCollector.ViewInfo info) {
        this.selectedInfo = info;
        rebuild();
    }

    public void setOnSelectionListener(OnSelectionListener l) {
        this.listener = l;
    }

    public void setColors(int border, int borderSelected, int text, int textSelected) {
        this.colorBorder = border;
        this.colorBorderSelected = borderSelected;
        this.colorText = text;
        this.colorTextSelected = textSelected;
        rebuild();
    }

    private void rebuild() {
        removeAllViews();
        if (rootInfo == null) return;
        addBox(rootInfo, 0);
    }

    private void addBox(ViewInfoCollector.ViewInfo info, int depth) {
        if (info == null || !info.isVisible()) return;

        float density = getResources().getDisplayMetrics().density;
        boolean isSelected = (selectedInfo != null && selectedInfo == info);

        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(depth * (int) (14 * density), (int) (5 * density), 0, 0);
        row.setLayoutParams(rowLp);

        // Box
        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        int ph = (int) (10 * density);
        int pv = (int) (8 * density);
        box.setPadding(ph, pv, ph, pv);
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        box.setLayoutParams(boxLp);

        // Background drawable
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(6 * density);
        bg.setColor(isSelected ? colorFillSelected : colorFill);
        if (isSelected) {
            bg.setStroke((int) (1.5f * density), colorBorderSelected);
        } else {
            bg.setStroke((int) (1.5f * density), colorBorder,
                    6f * density, 4f * density);
        }
        box.setBackground(bg);

        // Class name
        TextView classView = new TextView(getContext());
        classView.setText(info.simpleName);
        classView.setTextColor(isSelected ? colorTextSelected : colorText);
        classView.setTypeface(Typeface.MONOSPACE);
        classView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        box.addView(classView);

        // Id
        if (info.idName != null) {
            TextView idView = new TextView(getContext());
            idView.setText(" @" + info.idName);
            idView.setTextColor(colorIdText);
            idView.setTypeface(Typeface.MONOSPACE);
            idView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            LinearLayout.LayoutParams idLp = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            idLp.setMargins((int) (6 * density), 0, 0, 0);
            idView.setLayoutParams(idLp);
            box.addView(idView);
        }

        // Hint about size
        if (info.width > 0 && info.height > 0) {
            TextView dim = new TextView(getContext());
            dim.setText("  " + info.width + "×" + info.height);
            dim.setTextColor(Color.parseColor("#609B95AD"));
            dim.setTypeface(Typeface.MONOSPACE);
            dim.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            dlp.gravity = Gravity.END;
            dlp.setMargins((int) (6 * density), 0, 0, 0);
            dim.setLayoutParams(dlp);
            box.addView(dim);
        }

        // Tag for identification
        box.setTag(info);

        box.setOnClickListener(v -> {
            selectedInfo = (ViewInfoCollector.ViewInfo) v.getTag();
            rebuild();
            if (listener != null) listener.onSelectionChanged(selectedInfo);
        });

        row.addView(box);
        addView(row);

        // Récursion sur les enfants
        for (ViewInfoCollector.ViewInfo child : info.children) {
            addBox(child, depth + 1);
        }
    }
}
