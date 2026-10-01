package jo.layoutlib.editor;

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
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import jo.layoutlib.inflater.ViewInfoCollector;

/**
 * Construit l'arbre de hiérarchie de vues avec indentation, chevrons
 * expand/collapse, icônes vectorielles par classe de vue et labels monospace.
 * Basé sur mini-layoutlib (pas sur code-editor-lib).
 *
 * @author jo@Dev
 * @since 3.1
 */
public class TreeAdapter {

    public interface OnTreeSelectListener {
        void onTreeSelect(ViewInfoCollector.ViewInfo info);
    }

    private static final Map<String, Integer> ICON_MAP = new HashMap<>();
    static {
        ICON_MAP.put("TextView", R.drawable.ic_textview);
        ICON_MAP.put("Button", R.drawable.ic_button);
        ICON_MAP.put("EditText", R.drawable.ic_edittext);
        ICON_MAP.put("ImageView", R.drawable.ic_imageview);
        ICON_MAP.put("CheckBox", R.drawable.ic_checkbox);
        ICON_MAP.put("Switch", R.drawable.ic_switch);
        ICON_MAP.put("RadioButton", R.drawable.ic_radiobutton);
        ICON_MAP.put("Spinner", R.drawable.ic_spinner);
        ICON_MAP.put("RecyclerView", R.drawable.ic_recyclerview);
        ICON_MAP.put("CardView", R.drawable.ic_cardview);
        ICON_MAP.put("ProgressBar", R.drawable.ic_progressbar);
        ICON_MAP.put("SeekBar", R.drawable.ic_seekbar);
        ICON_MAP.put("LinearLayout", R.drawable.ic_layout_linear);
        ICON_MAP.put("FrameLayout", R.drawable.ic_layout_frame);
        ICON_MAP.put("ScrollView", R.drawable.ic_layout_scroll);
        ICON_MAP.put("ConstraintLayout", R.drawable.ic_layout_constraint);
        ICON_MAP.put("RelativeLayout", R.drawable.ic_layout_constraint);
    }

    private final LinearLayout container;
    private final OnTreeSelectListener listener;
    private final java.util.Set<ViewInfoCollector.ViewInfo> collapsedSet = new java.util.HashSet<>();
    private ViewInfoCollector.ViewInfo rootInfo;
    private ViewInfoCollector.ViewInfo selectedInfo;

    public TreeAdapter(LinearLayout container, OnTreeSelectListener listener) {
        this.container = container;
        this.listener = listener;
    }

    public void setRoot(ViewInfoCollector.ViewInfo root) {
        this.rootInfo = root;
        rebuild();
    }

    public void setSelected(ViewInfoCollector.ViewInfo info) {
        this.selectedInfo = info;
        rebuild();
    }

    public void rebuild() {
        container.removeAllViews();
        if (rootInfo == null) return;
        addNode(rootInfo, 0);
    }

    private void addNode(ViewInfoCollector.ViewInfo info, int depth) {
        if (info == null || !info.isVisible()) return;
        Context ctx = container.getContext();
        float density = ctx.getResources().getDisplayMetrics().density;

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int padH = (int) (8 * density);
        int padV = (int) (7 * density);
        row.setPadding(padH + (int) (depth * 14 * density), padV, padH, padV);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(8 * density);
        bg.setColor(Color.TRANSPARENT);
        row.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(lp);

        // Chevron
        ImageView chevron = new ImageView(ctx);
        int chevSize = (int) (14 * density);
        LinearLayout.LayoutParams chevLp = new LinearLayout.LayoutParams(chevSize, chevSize);
        chevron.setLayoutParams(chevLp);
        chevron.setImageResource(R.drawable.ic_chevron_right);
        chevron.setColorFilter(Color.parseColor("#9B95AD"));
        boolean hasChildren = !info.children.isEmpty();
        boolean isCollapsed = collapsedSet.contains(info);
        if (!hasChildren) {
            chevron.setVisibility(View.INVISIBLE);
        } else {
            chevron.setRotation(isCollapsed ? 0f : 90f);
            chevron.setOnClickListener(v -> {
                if (collapsedSet.contains(info)) collapsedSet.remove(info);
                else collapsedSet.add(info);
                rebuild();
            });
        }
        row.addView(chevron);

        // Icon
        int iconBoxSize = (int) (20 * density);
        FrameLayoutHelper iconBox = new FrameLayoutHelper(ctx, iconBoxSize);
        LinearLayout.LayoutParams iconBoxLp = new LinearLayout.LayoutParams(iconBoxSize, iconBoxSize);
        iconBoxLp.setMargins((int) (7 * density), 0, 0, 0);
        iconBox.setLayoutParams(iconBoxLp);

        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setCornerRadius(5 * density);
        iconBg.setColor(Color.parseColor("#FF28263F"));
        iconBox.setBackground(iconBg);

        ImageView icon = new ImageView(ctx);
        int iconSize = (int) (12 * density);
        android.widget.FrameLayout.LayoutParams iconLp = new android.widget.FrameLayout.LayoutParams(
                iconSize, iconSize, Gravity.CENTER);
        icon.setLayoutParams(iconLp);
        Integer iconRes = ICON_MAP.get(info.simpleName);
        if (iconRes == null) iconRes = R.drawable.ic_layout_frame;
        icon.setImageResource(iconRes);
        iconBox.addView(icon);
        row.addView(iconBox);

        // Name
        TextView name = new TextView(ctx);
        name.setText(info.simpleName);
        name.setTextColor(Color.parseColor("#E7E1EC"));
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        name.setTypeface(Typeface.MONOSPACE);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameLp.setMargins((int) (7 * density), 0, 0, 0);
        name.setLayoutParams(nameLp);
        row.addView(name);

        // Id
        if (info.idName != null) {
            TextView idLabel = new TextView(ctx);
            idLabel.setText("@" + info.idName);
            idLabel.setTextColor(Color.parseColor("#9B95AD"));
            idLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            idLabel.setTypeface(Typeface.MONOSPACE);
            LinearLayout.LayoutParams idLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            idLp.setMargins((int) (4 * density), 0, 0, 0);
            idLabel.setLayoutParams(idLp);
            row.addView(idLabel);
        }

        // Selection highlight
        if (selectedInfo != null && selectedInfo == info) {
            row.setBackgroundColor(Color.parseColor("#267C5CFF"));
            name.setTextColor(Color.parseColor("#FFD0BCFF"));
            icon.setColorFilter(Color.parseColor("#FFD0BCFF"));
        } else {
            icon.setColorFilter(Color.parseColor("#FFC9C3D6"));
        }

        row.setOnClickListener(v -> {
            selectedInfo = info;
            rebuild();
            if (listener != null) listener.onTreeSelect(info);
        });

        container.addView(row);

        if (hasChildren && !isCollapsed) {
            for (ViewInfoCollector.ViewInfo child : info.children) {
                addNode(child, depth + 1);
            }
        }
    }

    /** Helper FrameLayout with explicit size. */
    private static class FrameLayoutHelper extends android.widget.FrameLayout {
        private final int size;
        FrameLayoutHelper(Context ctx, int size) {
            super(ctx);
            this.size = size;
        }
        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            int spec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
            super.onMeasure(spec, spec);
        }
    }
}
