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
/**
 * Contrat commun des appliers d'attributs par famille de widgets
 * (découpage du point 8).
 *
 * <p>Chaque implémentation vérifie elle-même si la vue relève de sa
 * famille (instanceof) et applique les attributs correspondants. Un
 * attribut non résolu est ignoré et journalisé — il n'interrompt jamais
 * le rendu.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
interface WidgetAttributes {

    /**
     * Applique les attributs de la famille si la vue la concerne.
     *
     * @param view   la vue instanciée
     * @param parser le parser positionné sur son START_TAG
     */
    void apply(View view, XmlPullParser parser);
}
