package jo.layoutlib.inflater;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.widget.ExpandableListView;
import android.widget.ListView;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import jo.layoutlib.inflater.bridge.binding.AdapterHelper;
import jo.layoutlib.inflater.bridge.binding.FakeAdapter;
import jo.layoutlib.inflater.bridge.binding.FakeRecyclerAdapter;
import jo.layoutlib.resources.DimensionConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * v3.6.0 — preview de listes (étude PE14 § 2.4-3, S6 du plan) : une
 * ListView/Spinner/GridView (AdapterView) ou RecyclerView du document
 * reçoit un {@link FakeAdapter} construit depuis
 * {@code tools:listitem/itemCount/listheader/listfooter}.
 *
 * <p>Stratégie JVM : {@code brancherApercuListe} (visibilité package)
 * RETOURNE l'adapter posé — les assertions portent sur CE retour, car
 * l'interception Mockito des vues android.jar N'EST PAS FIABLE depuis le
 * bytecode de production (les corps mockables par défaut s'exécutent sans
 * enregistrer l'appel — constaté expérimentalement, {@code getAdapter()}
 * stubbé non-nul lisible comme nul). Les vues d'items sont gonflées par
 * la VRAIE machinerie (inflater enfant) : {@code getView} non nul prouve
 * le chemin complet. RecyclerView (androidx, jar réel) reste vérifiable
 * par ArgumentCaptor.</p>
 *
 * @author jo@Dev
 * @since 3.6.0
 */
@DisplayName("BridgeInflater — preview de listes tools:list* (v3.6.0, S6)")
class BridgeInflaterListPreviewTest {

    private static final String NS_ANDROID =
            "xmlns:android=\"http://schemas.android.com/apk/res/android\"";
    private static final String NS_TOOLS =
            "xmlns:tools=\"http://schemas.android.com/tools\"";

    private Context context;
    private BridgeInflater inflater;

    @BeforeEach
    void setUp() {
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.density = 2f;
        metrics.scaledDensity = 2f;
        metrics.xdpi = 320f;
        Resources resources = mock(Resources.class);
        when(resources.getDisplayMetrics()).thenReturn(metrics);
        when(resources.getIdentifier(any(String.class), any(String.class),
                any(String.class))).thenReturn(0);
        context = mock(Context.class);
        when(context.getResources()).thenReturn(resources);
        inflater = new BridgeInflater(context, new ViewTagRegistry(false));
        inflater.setAttributeApplier(
                new AttributeApplier(context, new DimensionConverter(2f, 2f, 320f)));
    }

    /** Un VRAI parser kxml2 positionné sur le START_TAG du XML donné. */
    private static XmlPullParser parserSur(String xml) throws Exception {
        XmlPullParser parser = new KXmlParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(new StringReader(xml));
        parser.nextTag();
        return parser;
    }

    @Nested
    @DisplayName("AdapterView — FakeAdapter depuis tools:list*")
    class AdapterViewBranch {

        @Test
        @DisplayName("ListView nue — adapter par défaut, remplissage généreux, getView rend")
        void listViewNueAdapteurDefaut() throws Exception {
            XmlPullParser parser = parserSur("<ListView " + NS_ANDROID + "\n"
                    + "    android:layout_width=\"match_parent\"\n"
                    + "    android:layout_height=\"wrap_content\" />");

            Object adapter = inflater.brancherApercuListe(mock(ListView.class), parser);

            assertThat(adapter).isInstanceOf(FakeAdapter.class);
            FakeAdapter fake = (FakeAdapter) adapter;
            assertThat(fake.getCount()).isEqualTo(AdapterHelper.NOMBRE_DEFAUT);
            assertThat(fake.getView(2, null, mock(android.view.ViewGroup.class)))
                    .isNotNull();
        }

        @Test
        @DisplayName("tools:itemCount borne l'adapter")
        void itemCountBorne() throws Exception {
            XmlPullParser parser = parserSur("<ListView " + NS_ANDROID + " " + NS_TOOLS
                    + "\n    android:layout_width=\"match_parent\"\n"
                    + "    android:layout_height=\"wrap_content\"\n"
                    + "    tools:itemCount=\"4\" />");

            Object adapter = inflater.brancherApercuListe(mock(ListView.class), parser);

            assertThat(adapter).isInstanceOf(FakeAdapter.class);
            assertThat(((FakeAdapter) adapter).getCount()).isEqualTo(4);
        }

        @Test
        @DisplayName("tools:itemCount non numérique — défaut, jamais d'exception")
        void itemCountInvalide() throws Exception {
            XmlPullParser parser = parserSur("<ListView " + NS_ANDROID + " " + NS_TOOLS
                    + "\n    tools:itemCount=\"abc\" />");

            Object adapter = inflater.brancherApercuListe(mock(ListView.class), parser);

            assertThat(adapter).isInstanceOf(FakeAdapter.class);
            assertThat(((FakeAdapter) adapter).getCount())
                    .isEqualTo(AdapterHelper.NOMBRE_DEFAUT);
        }

        @Test
        @DisplayName("ExpandableListView — RIEN posé (setAdapter jette)")
        void expandableListeEpargnee() throws Exception {
            XmlPullParser parser = parserSur("<ExpandableListView " + NS_ANDROID
                    + "\n    android:layout_height=\"wrap_content\" />");

            Object adapter = inflater.brancherApercuListe(
                    mock(ExpandableListView.class), parser);

            assertThat(adapter).isNull();
        }

        @Test
        @DisplayName("vue hors listes — retour null silencieux")
        void vueHorsListes() throws Exception {
            XmlPullParser parser = parserSur("<TextView " + NS_ANDROID
                    + "\n    android:layout_height=\"wrap_content\" />");

            Object adapter = inflater.brancherApercuListe(
                    mock(android.widget.TextView.class), parser);

            assertThat(adapter).isNull();
        }

        @Test
        @DisplayName("tools:listitem non résolu — repli items texte, jamais d'échec")
        void listitemIntrouvable() throws Exception {
            XmlPullParser parser = parserSur("<ListView " + NS_ANDROID + " " + NS_TOOLS
                    + "\n    android:layout_height=\"wrap_content\"\n"
                    + "    tools:listitem=\"@layout/introuvable\"\n"
                    + "    tools:itemCount=\"2\" />");

            Object adapter = inflater.brancherApercuListe(mock(ListView.class), parser);

            assertThat(adapter).isInstanceOf(FakeAdapter.class);
            FakeAdapter fake = (FakeAdapter) adapter;
            assertThat(fake.getCount()).isEqualTo(2);
            assertThat(fake.getView(1, null, null)).isNotNull();
        }
    }

    @Nested
    @DisplayName("RecyclerView — FakeRecyclerAdapter + LayoutManager par défaut")
    class RecyclerViewBranch {

        @Test
        @DisplayName("RecyclerView nue — LinearLayoutManager vertical posé")
        void layoutManagerVerticalParDefaut() throws Exception {
            RecyclerView liste = mock(RecyclerView.class);
            when(liste.getLayoutManager()).thenReturn(null);
            when(liste.getAdapter()).thenReturn(null);
            XmlPullParser parser = parserSur(
                    "<androidx.recyclerview.widget.RecyclerView " + NS_ANDROID + "\n"
                    + "    android:layout_width=\"match_parent\"\n"
                    + "    android:layout_height=\"wrap_content\" />");

            Object adapter = inflater.brancherApercuListe(liste, parser);

            assertThat(adapter).isInstanceOf(FakeRecyclerAdapter.class);
            org.mockito.ArgumentCaptor<RecyclerView.LayoutManager> lm =
                    org.mockito.ArgumentCaptor.forClass(
                            RecyclerView.LayoutManager.class);
            verify(liste).setLayoutManager(lm.capture());
            assertThat(lm.getValue()).isInstanceOf(LinearLayoutManager.class);
            assertThat(((LinearLayoutManager) lm.getValue()).getOrientation())
                    .isEqualTo(LinearLayoutManager.VERTICAL);
        }

        @Test
        @DisplayName("android:orientation=horizontal honorée")
        void orientationHorizontale() throws Exception {
            RecyclerView liste = mock(RecyclerView.class);
            when(liste.getLayoutManager()).thenReturn(null);
            when(liste.getAdapter()).thenReturn(null);
            XmlPullParser parser = parserSur(
                    "<androidx.recyclerview.widget.RecyclerView " + NS_ANDROID + "\n"
                    + "    android:layout_width=\"wrap_content\"\n"
                    + "    android:layout_height=\"match_parent\"\n"
                    + "    android:orientation=\"horizontal\" />");

            inflater.brancherApercuListe(liste, parser);

            org.mockito.ArgumentCaptor<RecyclerView.LayoutManager> lm =
                    org.mockito.ArgumentCaptor.forClass(
                            RecyclerView.LayoutManager.class);
            verify(liste).setLayoutManager(lm.capture());
            assertThat(((LinearLayoutManager) lm.getValue()).getOrientation())
                    .isEqualTo(LinearLayoutManager.HORIZONTAL);
        }

        @Test
        @DisplayName("LayoutManager déjà posé — jamais écrasé")
        void layoutManagerDejaPoseNonEcrase() throws Exception {
            RecyclerView liste = mock(RecyclerView.class);
            when(liste.getLayoutManager()).thenReturn(mock(LinearLayoutManager.class));
            when(liste.getAdapter()).thenReturn(null);
            XmlPullParser parser = parserSur(
                    "<androidx.recyclerview.widget.RecyclerView " + NS_ANDROID
                    + "\n    android:layout_height=\"wrap_content\" />");

            inflater.brancherApercuListe(liste, parser);

            verify(liste, org.mockito.Mockito.never()).setLayoutManager(any());
        }

        @Test
        @DisplayName("itemCount borne le FakeRecyclerAdapter")
        void itemCountRecycler() throws Exception {
            RecyclerView liste = mock(RecyclerView.class);
            when(liste.getLayoutManager()).thenReturn(null);
            when(liste.getAdapter()).thenReturn(null);
            XmlPullParser parser = parserSur(
                    "<androidx.recyclerview.widget.RecyclerView " + NS_ANDROID
                    + " " + NS_TOOLS + "\n"
                    + "    android:layout_height=\"wrap_content\"\n"
                    + "    tools:itemCount=\"7\" />");

            Object adapter = inflater.brancherApercuListe(liste, parser);

            assertThat(adapter).isInstanceOf(FakeRecyclerAdapter.class);
            assertThat(((FakeRecyclerAdapter) adapter).getItemCount()).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("Pipeline — le document mixte rend sans exception")
    class PipelineIntegration {

        @Test
        @DisplayName("inflate : listes + contenu + tools:list* survivent au préprocesseur")
        void documentMixteSansException() {
            assertThatCode(() -> inflater.inflate(
                    "<LinearLayout " + NS_ANDROID + " " + NS_TOOLS + "\n"
                    + "    android:layout_width=\"match_parent\"\n"
                    + "    android:layout_height=\"wrap_content\"\n"
                    + "    android:orientation=\"vertical\">\n"
                    + "  <TextView\n"
                    + "      android:layout_width=\"wrap_content\"\n"
                    + "      android:layout_height=\"wrap_content\"\n"
                    + "      tools:text=\"Titre\" />\n"
                    + "  <ListView\n"
                    + "      android:layout_width=\"match_parent\"\n"
                    + "      android:layout_height=\"160dp\"\n"
                    + "      tools:listitem=\"@layout/introuvable\"\n"
                    + "      tools:itemCount=\"3\" />\n"
                    + "</LinearLayout>"))
                    .doesNotThrowAnyException();
        }
    }
}
