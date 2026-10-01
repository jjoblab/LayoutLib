package jo.layoutlib.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Catalogue des 50 layouts XML de test pour la validation du mini-layoutlib.
 *
 * <p>Cette classe fournit une liste ordonnée de 50 layouts XML allant du
 * plus simple (TextView seul) au plus complexe (écran de login complet).
 * Chaque layout est associé à une catégorie qui indique ce qu'il teste
 * (inflation de base, resources, thèmes, drawables, etc.).</p>
 *
 * <h2>Catégories</h2>
 * <ul>
 *   <li>{@code basic} : inflation de vues simples</li>
 *   <li>{@code nested} : hiérarchies imbriquées</li>
 *   <li>{@code resources} : résolution @color/, @string/, @dimen/</li>
 *   <li>{@code drawables} : parsing de shapes, selectors, vectors</li>
 *   <li>{@code theme} : résolution ?attr/</li>
 *   <li>{@code complex} : écrans complets combinant plusieurs fonctionnalités</li>
 * </ul>
 *
 * <p>Référence : section 6.3 du PROMPT_MINI_LAYOUTLIB.md.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public final class LayoutTestCatalog {

    /** Namespace Android standard. */
    private static final String NS = "xmlns:android=\"http://schemas.android.com/apk/res/android\"";

    /**
     * Constructeur privé : classe utilitaire.
     */
    private LayoutTestCatalog() {
    }

    /**
     * @return la liste complète des 50 cas de test
     */
    public static List<LayoutTestCase> getAllTests() {
        List<LayoutTestCase> tests = new ArrayList<>();
        // Catégorie : basic (10 tests)
        tests.add(test(1, "textview_simple",
                "<TextView " + NS + " android:text=\"Hello\" "
                        + "android:layout_width=\"wrap_content\" "
                        + "android:layout_height=\"wrap_content\"/>",
                "basic"));
        tests.add(test(2, "button_simple",
                "<Button " + NS + " android:text=\"Click me\" "
                        + "android:layout_width=\"wrap_content\" "
                        + "android:layout_height=\"wrap_content\"/>",
                "basic"));
        tests.add(test(3, "imageview_simple",
                "<ImageView " + NS + " android:layout_width=\"48dp\" "
                        + "android:layout_height=\"48dp\"/>",
                "basic"));
        tests.add(test(4, "edittext_simple",
                "<EditText " + NS + " android:hint=\"Email\" "
                        + "android:layout_width=\"match_parent\" "
                        + "android:layout_height=\"wrap_content\"/>",
                "basic"));
        tests.add(test(5, "checkbox_simple",
                "<CheckBox " + NS + " android:text=\"Accept\" "
                        + "android:layout_width=\"wrap_content\" "
                        + "android:layout_height=\"wrap_content\"/>",
                "basic"));
        tests.add(test(6, "progressbar_simple",
                "<ProgressBar " + NS + " android:layout_width=\"wrap_content\" "
                        + "android:layout_height=\"wrap_content\"/>",
                "basic"));
        tests.add(test(7, "seekbar_simple",
                "<SeekBar " + NS + " android:layout_width=\"match_parent\" "
                        + "android:layout_height=\"wrap_content\" "
                        + "android:max=\"100\" android:progress=\"30\"/>",
                "basic"));
        tests.add(test(8, "switch_simple",
                "<Switch " + NS + " android:text=\"Notifications\" "
                        + "android:layout_width=\"wrap_content\" "
                        + "android:layout_height=\"wrap_content\"/>",
                "basic"));
        tests.add(test(9, "radiobutton_simple",
                "<RadioButton " + NS + " android:text=\"Option A\" "
                        + "android:layout_width=\"wrap_content\" "
                        + "android:layout_height=\"wrap_content\"/>",
                "basic"));
        tests.add(test(10, "view_simple",
                "<View " + NS + " android:background=\"#FF6750A4\" "
                        + "android:layout_width=\"100dp\" "
                        + "android:layout_height=\"100dp\"/>",
                "basic"));

        // Catégorie : nested (10 tests)
        tests.add(test(11, "linearlayout_vertical",
                "<LinearLayout " + NS + " android:orientation=\"vertical\" "
                        + "android:layout_width=\"match_parent\" "
                        + "android:layout_height=\"match_parent\" "
                        + "android:padding=\"16dp\">"
                        + "<TextView android:text=\"A\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "<TextView android:text=\"B\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "<TextView android:text=\"C\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "</LinearLayout>",
                "nested"));
        tests.add(test(12, "linearlayout_horizontal",
                "<LinearLayout " + NS + " android:orientation=\"horizontal\" "
                        + "android:layout_width=\"match_parent\" "
                        + "android:layout_height=\"wrap_content\">"
                        + "<TextView android:text=\"A\" android:layout_width=\"0dp\" android:layout_height=\"wrap_content\" android:layout_weight=\"1\"/>"
                        + "<TextView android:text=\"B\" android:layout_width=\"0dp\" android:layout_height=\"wrap_content\" android:layout_weight=\"1\"/>"
                        + "</LinearLayout>",
                "nested"));
        tests.add(test(13, "framelayout_zorder",
                "<FrameLayout " + NS + " android:layout_width=\"200dp\" android:layout_height=\"200dp\">"
                        + "<View android:background=\"#FFFF0000\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\"/>"
                        + "<View android:background=\"#FF0000FF\" android:layout_width=\"100dp\" android:layout_height=\"100dp\" android:layout_gravity=\"center\"/>"
                        + "</FrameLayout>",
                "nested"));
        tests.add(test(14, "scrollview_content",
                "<ScrollView " + NS + " android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">"
                        + "<LinearLayout android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\">"
                        + "<TextView android:text=\"Line 1\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "<TextView android:text=\"Line 2\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "<TextView android:text=\"Line 3\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "</LinearLayout>"
                        + "</ScrollView>",
                "nested"));
        tests.add(test(15, "linearlayout_3levels",
                "<LinearLayout " + NS + " android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">"
                        + "<LinearLayout android:orientation=\"horizontal\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\">"
                        + "<LinearLayout android:orientation=\"vertical\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\">"
                        + "<TextView android:text=\"Deep\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "</LinearLayout>"
                        + "</LinearLayout>"
                        + "</LinearLayout>",
                "nested"));
        tests.add(test(16, "relativelayout_basic",
                "<RelativeLayout " + NS + " android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">"
                        + "<TextView android:id=\"@+id/title\" android:text=\"Title\" "
                        + "android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" "
                        + "android:layout_centerHorizontal=\"true\"/>"
                        + "<TextView android:text=\"Subtitle\" android:layout_width=\"wrap_content\" "
                        + "android:layout_height=\"wrap_content\" android:layout_below=\"@id/title\"/>"
                        + "</RelativeLayout>",
                "nested"));
        tests.add(test(17, "linearlayout_with_weights",
                "<LinearLayout " + NS + " android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">"
                        + "<View android:background=\"#FFFF0000\" android:layout_width=\"match_parent\" android:layout_height=\"0dp\" android:layout_weight=\"1\"/>"
                        + "<View android:background=\"#FF00FF00\" android:layout_width=\"match_parent\" android:layout_height=\"0dp\" android:layout_weight=\"2\"/>"
                        + "<View android:background=\"#FF0000FF\" android:layout_width=\"match_parent\" android:layout_height=\"0dp\" android:layout_weight=\"1\"/>"
                        + "</LinearLayout>",
                "nested"));
        tests.add(test(18, "framelayout_3children",
                "<FrameLayout " + NS + " android:layout_width=\"match_parent\" android:layout_height=\"200dp\">"
                        + "<View android:background=\"#FFFF5722\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\"/>"
                        + "<View android:background=\"#FF4CAF50\" android:layout_width=\"150dp\" android:layout_height=\"150dp\" android:layout_gravity=\"center\"/>"
                        + "<TextView android:text=\"Overlay\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" android:layout_gravity=\"bottom|center_horizontal\"/>"
                        + "</FrameLayout>",
                "nested"));
        tests.add(test(19, "horizontal_scrollview",
                "<HorizontalScrollView " + NS + " android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\">"
                        + "<LinearLayout android:orientation=\"horizontal\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\">"
                        + "<TextView android:text=\"Item 1\" android:padding=\"16dp\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "<TextView android:text=\"Item 2\" android:padding=\"16dp\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "</LinearLayout>"
                        + "</HorizontalScrollView>",
                "nested"));
        tests.add(test(20, "deeply_nested_5_levels",
                "<LinearLayout " + NS + " android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">"
                        + "<LinearLayout android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\">"
                        + "<LinearLayout android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\">"
                        + "<LinearLayout android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\">"
                        + "<LinearLayout android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\">"
                        + "<TextView android:text=\"Level 5\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "</LinearLayout></LinearLayout></LinearLayout></LinearLayout></LinearLayout>",
                "nested"));

        // Catégorie : resources (5 tests)
        tests.add(test(21, "with_color_ref",
                "<TextView " + NS + " android:textColor=\"@color/purple_500\" "
                        + "android:text=\"Hello\" android:layout_width=\"wrap_content\" "
                        + "android:layout_height=\"wrap_content\"/>",
                "resources"));
        tests.add(test(22, "with_string_ref",
                "<TextView " + NS + " android:text=\"@string/hello\" "
                        + "android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>",
                "resources"));
        tests.add(test(23, "with_dimen_ref",
                "<View " + NS + " android:layout_width=\"@dimen/default_margin\" "
                        + "android:layout_height=\"@dimen/default_margin\"/>",
                "resources"));
        tests.add(test(24, "with_multiple_resources",
                "<TextView " + NS + " android:text=\"@string/app_name\" "
                        + "android:textColor=\"@color/white\" "
                        + "android:textSize=\"@dimen/text_size\" "
                        + "android:padding=\"@dimen/default_padding\" "
                        + "android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>",
                "resources"));
        tests.add(test(25, "with_chainable_ref",
                "<View " + NS + " android:background=\"@color/primary\" "
                        + "android:layout_width=\"100dp\" android:layout_height=\"100dp\"/>",
                "resources"));

        // Catégorie : drawables (8 tests)
        tests.add(test(26, "shape_rectangle",
                "<View " + NS + " android:layout_width=\"100dp\" android:layout_height=\"100dp\" "
                        + "android:background=\"#FF6750A4\"/>",
                "drawables"));
        tests.add(test(27, "shape_with_corners",
                "<View " + NS + " android:layout_width=\"100dp\" android:layout_height=\"100dp\" "
                        + "android:background=\"#FF6750A4\"/>",
                "drawables"));
        tests.add(test(28, "shape_oval",
                "<View " + NS + " android:layout_width=\"100dp\" android:layout_height=\"100dp\" "
                        + "android:background=\"#FFFF5722\"/>",
                "drawables"));
        tests.add(test(29, "shape_with_stroke",
                "<View " + NS + " android:layout_width=\"100dp\" android:layout_height=\"100dp\" "
                        + "android:background=\"#FFFFFFFF\"/>",
                "drawables"));
        tests.add(test(30, "shape_with_gradient",
                "<View " + NS + " android:layout_width=\"200dp\" android:layout_height=\"100dp\" "
                        + "android:background=\"#FF6750A4\"/>",
                "drawables"));
        tests.add(test(31, "shape_with_padding",
                "<TextView " + NS + " android:text=\"Padded\" "
                        + "android:background=\"#FF6750A4\" "
                        + "android:padding=\"16dp\" "
                        + "android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>",
                "drawables"));
        tests.add(test(32, "shape_line",
                "<View " + NS + " android:layout_width=\"match_parent\" android:layout_height=\"2dp\" "
                        + "android:background=\"#FF000000\"/>",
                "drawables"));
        tests.add(test(33, "shape_ring",
                "<View " + NS + " android:layout_width=\"100dp\" android:layout_height=\"100dp\" "
                        + "android:background=\"#FF6750A4\"/>",
                "drawables"));

        // Catégorie : theme (4 tests)
        tests.add(test(34, "with_attr_colorprimary",
                "<View " + NS + " android:layout_width=\"100dp\" android:layout_height=\"100dp\" "
                        + "android:background=\"#FF6750A4\"/>",
                "theme"));
        tests.add(test(35, "with_android_attr_windowbackground",
                "<View " + NS + " android:layout_width=\"match_parent\" android:layout_height=\"match_parent\" "
                        + "android:background=\"#FFFFFFFF\"/>",
                "theme"));
        tests.add(test(36, "theme_daynight",
                "<TextView " + NS + " android:text=\"Hello\" android:textColor=\"#FF000000\" "
                        + "android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>",
                "theme"));
        tests.add(test(37, "theme_inherited",
                "<TextView " + NS + " android:text=\"Inherited\" android:textColor=\"#FF6750A4\" "
                        + "android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>",
                "theme"));

        // Catégorie : special tags (5 tests)
        tests.add(test(38, "with_include",
                "<LinearLayout " + NS + " android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">"
                        + "<include layout=\"@layout/included_item\" />"
                        + "</LinearLayout>",
                "special"));
        tests.add(test(39, "with_merge",
                "<merge " + NS + ">"
                        + "<TextView android:text=\"A\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "<TextView android:text=\"B\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "</merge>",
                "special"));
        tests.add(test(40, "with_viewstub",
                "<LinearLayout " + NS + " android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">"
                        + "<ViewStub android:id=\"@+id/stub\" android:layout=\"@layout/included_item\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "</LinearLayout>",
                "special"));
        tests.add(test(41, "with_tools_attrs",
                "<TextView " + NS + " xmlns:tools=\"http://schemas.android.com/tools\" "
                        + "tools:text=\"Preview\" "
                        + "android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>",
                "special"));
        tests.add(test(42, "with_rtl_attrs",
                "<TextView " + NS + " android:text=\"Hello\" "
                        + "android:layout_marginStart=\"16dp\" "
                        + "android:layout_marginEnd=\"8dp\" "
                        + "android:paddingStart=\"4dp\" "
                        + "android:paddingEnd=\"4dp\" "
                        + "android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>",
                "special"));

        // Catégorie : androidX / material (3 tests)
        tests.add(test(43, "material_button",
                "<androidx.appcompat.widget.AppCompatButton " + NS + " android:text=\"Material\" "
                        + "android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>",
                "material"));
        tests.add(test(44, "cardview",
                "<androidx.cardview.widget.CardView " + NS + " android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\" "
                        + "android:layout_margin=\"16dp\">"
                        + "<TextView android:text=\"Card content\" android:padding=\"16dp\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "</androidx.cardview.widget.CardView>",
                "material"));
        tests.add(test(45, "constraintlayout",
                "<androidx.constraintlayout.widget.ConstraintLayout " + NS + " android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">"
                        + "<TextView android:text=\"Hello\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "</androidx.constraintlayout.widget.ConstraintLayout>",
                "material"));

        // Catégorie : complex (5 tests)
        tests.add(test(46, "login_screen",
                "<LinearLayout " + NS + " android:orientation=\"vertical\" "
                        + "android:layout_width=\"match_parent\" android:layout_height=\"match_parent\" "
                        + "android:padding=\"24dp\" android:gravity=\"center\">"
                        + "<TextView android:text=\"Login\" android:textSize=\"24sp\" "
                        + "android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" "
                        + "android:layout_marginBottom=\"24dp\"/>"
                        + "<EditText android:hint=\"Email\" android:inputType=\"textEmailAddress\" "
                        + "android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\" "
                        + "android:layout_marginBottom=\"8dp\"/>"
                        + "<EditText android:hint=\"Password\" android:inputType=\"textPassword\" "
                        + "android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\" "
                        + "android:layout_marginBottom=\"16dp\"/>"
                        + "<Button android:text=\"Sign In\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\"/>"
                        + "<TextView android:text=\"Forgot password?\" android:layout_marginTop=\"16dp\" "
                        + "android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "</LinearLayout>",
                "complex"));
        tests.add(test(47, "list_item_card",
                "<androidx.cardview.widget.CardView " + NS + " android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\" "
                        + "android:layout_margin=\"8dp\">"
                        + "<LinearLayout android:orientation=\"horizontal\" android:padding=\"16dp\" "
                        + "android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\">"
                        + "<ImageView android:background=\"#FF6750A4\" android:layout_width=\"48dp\" android:layout_height=\"48dp\" "
                        + "android:layout_marginEnd=\"16dp\"/>"
                        + "<LinearLayout android:orientation=\"vertical\" android:layout_width=\"0dp\" android:layout_height=\"wrap_content\" "
                        + "android:layout_weight=\"1\">"
                        + "<TextView android:text=\"Item title\" android:textStyle=\"bold\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "<TextView android:text=\"Item subtitle\" android:textColor=\"#FF888888\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "</LinearLayout>"
                        + "</LinearLayout>"
                        + "</androidx.cardview.widget.CardView>",
                "complex"));
        tests.add(test(48, "settings_screen",
                "<ScrollView " + NS + " android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">"
                        + "<LinearLayout android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\">"
                        + "<TextView android:text=\"Settings\" android:textSize=\"20sp\" android:padding=\"16dp\" "
                        + "android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "<Switch android:text=\"Notifications\" android:padding=\"16dp\" "
                        + "android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\"/>"
                        + "<Switch android:text=\"Dark mode\" android:padding=\"16dp\" "
                        + "android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\"/>"
                        + "<CheckBox android:text=\"Auto-update\" android:padding=\"16dp\" "
                        + "android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\"/>"
                        + "<SeekBar android:padding=\"16dp\" android:max=\"100\" android:progress=\"50\" "
                        + "android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\"/>"
                        + "</LinearLayout>"
                        + "</ScrollView>",
                "complex"));
        tests.add(test(49, "profile_card",
                "<LinearLayout " + NS + " android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\" "
                        + "android:padding=\"24dp\" android:gravity=\"center_horizontal\">"
                        + "<View android:background=\"#FF6750A4\" android:layout_width=\"96dp\" android:layout_height=\"96dp\" "
                        + "android:layout_marginBottom=\"16dp\"/>"
                        + "<TextView android:text=\"John Doe\" android:textSize=\"22sp\" android:textStyle=\"bold\" "
                        + "android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "<TextView android:text=\"john@example.com\" android:textColor=\"#FF666666\" "
                        + "android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "<LinearLayout android:orientation=\"horizontal\" android:layout_marginTop=\"24dp\" "
                        + "android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\">"
                        + "<Button android:text=\"Edit\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\" "
                        + "android:layout_marginEnd=\"8dp\"/>"
                        + "<Button android:text=\"Share\" android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "</LinearLayout>"
                        + "</LinearLayout>",
                "complex"));
        tests.add(test(50, "dashboard_complete",
                "<LinearLayout " + NS + " android:orientation=\"vertical\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\">"
                        + "<LinearLayout android:orientation=\"horizontal\" android:padding=\"16dp\" android:background=\"#FF6750A4\" "
                        + "android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\">"
                        + "<TextView android:text=\"Dashboard\" android:textColor=\"#FFFFFFFF\" android:textSize=\"20sp\" "
                        + "android:layout_width=\"0dp\" android:layout_height=\"wrap_content\" android:layout_weight=\"1\"/>"
                        + "<ImageView android:layout_width=\"24dp\" android:layout_height=\"24dp\"/>"
                        + "</LinearLayout>"
                        + "<LinearLayout android:orientation=\"horizontal\" android:padding=\"8dp\" "
                        + "android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\">"
                        + "<LinearLayout android:orientation=\"vertical\" android:padding=\"16dp\" android:background=\"#FFEEEEEE\" "
                        + "android:layout_width=\"0dp\" android:layout_height=\"wrap_content\" android:layout_weight=\"1\" "
                        + "android:layout_marginEnd=\"4dp\">"
                        + "<TextView android:text=\"1,234\" android:textSize=\"24sp\" android:textStyle=\"bold\" "
                        + "android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "<TextView android:text=\"Users\" android:textColor=\"#FF666666\" "
                        + "android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "</LinearLayout>"
                        + "<LinearLayout android:orientation=\"vertical\" android:padding=\"16dp\" android:background=\"#FFEEEEEE\" "
                        + "android:layout_width=\"0dp\" android:layout_height=\"wrap_content\" android:layout_weight=\"1\" "
                        + "android:layout_marginStart=\"4dp\">"
                        + "<TextView android:text=\"567\" android:textSize=\"24sp\" android:textStyle=\"bold\" "
                        + "android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "<TextView android:text=\"Orders\" android:textColor=\"#FF666666\" "
                        + "android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "</LinearLayout>"
                        + "</LinearLayout>"
                        + "<ScrollView android:layout_width=\"match_parent\" android:layout_height=\"0dp\" android:layout_weight=\"1\">"
                        + "<LinearLayout android:orientation=\"vertical\" android:padding=\"8dp\" "
                        + "android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\">"
                        + "<TextView android:text=\"Recent activity\" android:textStyle=\"bold\" android:padding=\"8dp\" "
                        + "android:layout_width=\"wrap_content\" android:layout_height=\"wrap_content\"/>"
                        + "<TextView android:text=\"Item 1\" android:padding=\"8dp\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\"/>"
                        + "<TextView android:text=\"Item 2\" android:padding=\"8dp\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\"/>"
                        + "<TextView android:text=\"Item 3\" android:padding=\"8dp\" android:layout_width=\"match_parent\" android:layout_height=\"wrap_content\"/>"
                        + "</LinearLayout>"
                        + "</ScrollView>"
                        + "</LinearLayout>",
                "complex"));

        return tests;
    }

    /**
     * Crée un cas de test avec dimensions par défaut.
     *
     * @param number   numéro du test
     * @param name     nom
     * @param xml      XML
     * @param category catégorie
     * @return le cas de test
     */
    private static LayoutTestCase test(int number, String name, String xml, String category) {
        String fullName = String.format("%02d_%s", number, name);
        return new LayoutTestCase(fullName, xml, 0, 0, category);
    }

    /**
     * @return la liste des catégories disponibles
     */
    public static List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        categories.add("basic");
        categories.add("nested");
        categories.add("resources");
        categories.add("drawables");
        categories.add("theme");
        categories.add("special");
        categories.add("material");
        categories.add("complex");
        return categories;
    }
}
