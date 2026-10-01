package jo.layoutlib.editor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Remplit une grille 3 colonnes avec les cartes de composants de palette,
 * inspiré du panneau Palette d'Android Studio.
 *
 * <p>Chaque carte affiche une icône vectorielle (matchant le preview HTML)
 * et le nom du composant. Le clic déclenche un callback.</p>
 *
 * @author jo@Dev
 * @since 1.1
 */
public class PaletteAdapter {

    /** Composants disponibles dans la palette (12 entrées) : nom + resId icône. */
    public static final Object[][] PALETTE_COMPONENTS = {
        {"TextView", R.drawable.ic_textview},
        {"Button", R.drawable.ic_button},
        {"EditText", R.drawable.ic_edittext},
        {"ImageView", R.drawable.ic_imageview},
        {"CheckBox", R.drawable.ic_checkbox},
        {"Switch", R.drawable.ic_switch},
        {"RadioButton", R.drawable.ic_radiobutton},
        {"Spinner", R.drawable.ic_spinner},
        {"RecyclerView", R.drawable.ic_recyclerview},
        {"CardView", R.drawable.ic_cardview},
        {"ProgressBar", R.drawable.ic_progressbar},
        {"SeekBar", R.drawable.ic_seekbar},
    };

    /** Noms des composants (pour les snippets XML). */
    public static final String[] PALETTE_NAMES = {
        "TextView", "Button", "EditText", "ImageView",
        "CheckBox", "Switch", "RadioButton", "Spinner",
        "RecyclerView", "CardView", "ProgressBar", "SeekBar",
    };

    /** Snippets XML par composant. */
    public static String createSnippet(String name) {
        switch (name) {
            case "TextView":
                return "    <TextView android:text=\"New Text\" android:textSize=\"14sp\"\n" +
                       "        android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>\n";
            case "Button":
                return "    <Button android:text=\"New Button\"\n" +
                       "        android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>\n";
            case "EditText":
                return "    <EditText android:hint=\"Input\"\n" +
                       "        android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\"/>\n";
            case "ImageView":
                return "    <ImageView android:src=\"@android:drawable/ic_menu_info\"\n" +
                       "        android:layout_width=\"48dp\" android:layout_height=\"48dp\"/>\n";
            case "CheckBox":
                return "    <CheckBox android:text=\"Option\"\n" +
                       "        android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>\n";
            case "Switch":
                return "    <Switch android:text=\"Toggle\"\n" +
                       "        android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>\n";
            case "RadioButton":
                return "    <RadioButton android:text=\"Option\"\n" +
                       "        android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>\n";
            case "Spinner":
                return "    <Spinner android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>\n";
            case "RecyclerView":
                return "    <androidx.recyclerview.widget.RecyclerView\n" +
                       "        android:layout_width=\"match_parent\" android:layout_height=\"match_parent\"/>\n";
            case "CardView":
                return "    <androidx.cardview.widget.CardView\n" +
                       "        android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\"\n" +
                       "        app:cardCornerRadius=\"8dp\"/>\n";
            case "ProgressBar":
                return "    <ProgressBar android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>\n";
            case "SeekBar":
                return "    <SeekBar android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\"/>\n";
            default:
                return "    <View android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>\n";
        }
    }

    public interface OnComponentSelectedListener {
        void onComponentSelected(String name);
    }

    /**
     * Remplit une GridLayout avec les 12 cartes.
     *
     * @param grid     la GridLayout cible
     * @param listener le listener de clic
     */
    public static void populate(GridLayout grid, OnComponentSelectedListener listener) {
        grid.removeAllViews();
        Context ctx = grid.getContext();
        LayoutInflater inflater = LayoutInflater.from(ctx);

        for (Object[] comp : PALETTE_COMPONENTS) {
            String name = (String) comp[0];
            int iconRes = (int) comp[1];

            View card = inflater.inflate(R.layout.item_palette_card, grid, false);
            ImageView iconView = card.findViewById(R.id.palIcon);
            TextView nameView = card.findViewById(R.id.palName);
            iconView.setImageResource(iconRes);
            nameView.setText(name);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
            card.setLayoutParams(lp);

            card.setOnClickListener(v -> {
                if (listener != null) listener.onComponentSelected(name);
            });
            grid.addView(card);
        }
    }
}
