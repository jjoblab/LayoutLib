package jo.layoutlib.editor;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * Popup de sélection (remplace les dropdowns/selects du HTML preview).
 * Affiche une liste verticale d'options dans une PopupWindow stylée Material 3.
 *
 * <p>Style : fond elevated (#2D2B48), bordure outline-2, corner 12dp,
 * chaque option est un TextView monospace 11.5sp avec padding 10dp,
 * option sélectionnée = fond primary-container + texte primary-light.</p>
 *
 * @author jo@Dev
 * @since 3.2
 */
public class SelectPopup extends PopupWindow {

    public interface OnOptionSelectedListener {
        void onOptionSelected(String value);
    }

    private OnOptionSelectedListener listener;

    public SelectPopup(Context context, String[] options, String selectedValue) {
        super(makeContentView(context, options, selectedValue, null),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        setFocusable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(null);
        setElevation(20);
    }

    /**
     * Crée la vue contenu avec les options.
     * Le listener est initialement null, et setOnOptionSelectedListener reconstruit
     * la vue avec le listener wired.
     */
    private static View makeContentView(Context context, String[] options,
                                         String selectedValue,
                                         OnOptionSelectedListener listener) {
        float density = context.getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (6 * density);
        root.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(12 * density);
        bg.setColor(Color.parseColor("#FF2D2B48")); // bg-elevated
        bg.setStroke((int) (1 * density), Color.parseColor("#FF322F47")); // outline-2
        root.setBackground(bg);

        for (String option : options) {
            TextView item = new TextView(context);
            item.setText(option);
            item.setTextColor(Color.parseColor("#E7E1EC")); // on-surface
            item.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            item.setTypeface(Typeface.MONOSPACE);
            int itemPadH = (int) (12 * density);
            int itemPadV = (int) (8 * density);
            item.setPadding(itemPadH, itemPadV, itemPadH, itemPadV);
            item.setSingleLine(true);

            // Background : selected = primary-container, sinon transparent
            GradientDrawable itemBg = new GradientDrawable();
            itemBg.setCornerRadius(8 * density);
            if (option.equals(selectedValue)) {
                itemBg.setColor(Color.parseColor("#FF4F378B")); // primary-container
                item.setTextColor(Color.parseColor("#FFD0BCFF")); // primary-light
                item.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            } else {
                itemBg.setColor(Color.TRANSPARENT);
            }
            item.setBackground(itemBg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = (int) (2 * density);
            item.setLayoutParams(lp);

            item.setClickable(true);
            item.setOnClickListener(v -> {
                // Feedback visuel au clic
                item.setBackgroundColor(Color.parseColor("#267C5CFF"));
                item.postDelayed(() -> {
                    if (listener != null) listener.onOptionSelected(option);
                    // dismiss est appelé par le wrapper via setOnOptionSelectedListener
                }, 100);
            });

            root.addView(item);
        }

        return root;
    }

    public void setOnOptionSelectedListener(OnOptionSelectedListener l) {
        this.listener = l;
        // Reconstruire le contenu avec le listener wired
        // (on ne peut pas juste re-wire les listeners existants car le listener
        // est capturé par la closure dans makeContentView)
        View root = getContentView();
        if (root instanceof LinearLayout) {
            LinearLayout layout = (LinearLayout) root;
            for (int i = 0; i < layout.getChildCount(); i++) {
                View child = layout.getChildAt(i);
                if (child instanceof TextView) {
                    final String option = ((TextView) child).getText().toString();
                    child.setOnClickListener(v -> {
                        // Feedback visuel
                        child.setBackgroundColor(Color.parseColor("#267C5CFF"));
                        child.postDelayed(() -> {
                            if (listener != null) listener.onOptionSelected(option);
                            dismiss();
                        }, 100);
                    });
                }
            }
        }
    }
}
