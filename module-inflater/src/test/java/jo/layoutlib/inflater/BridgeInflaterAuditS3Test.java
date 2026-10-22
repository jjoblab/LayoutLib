package jo.layoutlib.inflater;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.widget.GridLayout;
import android.widget.RelativeLayout;

import jo.layoutlib.inflater.bridge.util.DynamicIdMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.xmlpull.v1.XmlPullParser;

import java.lang.reflect.Method;

/**
 * v3.5.1 — les TROIS bugs ponctuels de l'audit LayoutLib (étude PE14
 * {@code docs/PE14-redimensionnement-layoutlib-conformite-compose-etude.md}
 * § 2.3 « bugs ponctuels à corriger vite », S3 du plan § 6) :
 *
 * <ol>
 *   <li>{@code layout_column} lu depuis {@code layout_rowSpan}
 *       (copier-coller) — tout GridLayout avec {@code layout_column} était
 *       mal placé ;</li>
 *   <li>{@code layout_gravity} d'un enfant de RelativeLayout IGNORÉ —
 *       la valeur tombait toujours en {@code CENTER_IN_PARENT} ;</li>
 *   <li>ids dynamiques RÉGÉNÉRÉS à chaque appel — deux vues référençant le
 *       même {@code @id/x} obtenaient des ids DIFFÉRENTS selon l'ordre de
 *       résolution ({@code layout_below}, contraintes, ancres silencieusement
 *       cassés) : la {@link DynamicIdMap} STABLE nom → id (miroir
 *       {@code util/DynamicIdMap} de la référence) remplace le compteur.</li>
 * </ol>
 *
 * <p>Patron {@code BridgeInflaterDimenReferenceTest} : les LayoutParams des
 * vues ne sont pas observables sous la JVM de test (android.jar mockable)
 * — les lectures d'attributs se vérifient par Mockito, la gravité par le
 * CHAMP des LayoutParams (réel même mockable), les ids par RÉFLEXION sur
 * les résolveurs privés. Chaque test échoue sur le code d'avant le
 * correctif, prouvant la régression.</p>
 *
 * @author jo@Dev
 * @since 3.5.1
 */
@DisplayName("BridgeInflater — les trois bugs de l'audit (v3.5.1, étude § 2.3)")
class BridgeInflaterAuditS3Test {

    /** L'URI du namespace android, telle que le helper la résout. */
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    private Context context;
    private BridgeInflater inflater;

    @BeforeEach
    void setUp() {
        Resources resources = mock(Resources.class);
        when(resources.getIdentifier(anyString(), anyString(), anyString())).thenReturn(0);
        context = mock(Context.class);
        when(context.getResources()).thenReturn(resources);
        when(context.getPackageName()).thenReturn("jo.test");
        inflater = new BridgeInflater(context);
    }

    @Test
    @DisplayName("layout_column se lit depuis SON attribut, plus depuis layout_rowSpan")
    void gridLayoutColumnSeLitDepuisSonAttribut() throws Exception {
        XmlPullParser parser = mock(XmlPullParser.class);
        when(parser.getAttributeValue(ANDROID_NS, "layout_column")).thenReturn("2");
        when(parser.getAttributeValue(ANDROID_NS, "layout_rowSpan")).thenReturn("1");
        Method appliquer = BridgeInflater.class.getDeclaredMethod(
                "applyGridLayoutSpec", GridLayout.LayoutParams.class, XmlPullParser.class);
        appliquer.setAccessible(true);
        appliquer.invoke(inflater, new GridLayout.LayoutParams(), parser);
        // Avant v3.5.1, la colonne était lue depuis layout_rowSpan : le
        // premier verify échouait (l'attribut n'était JAMAIS lu). Le helper
        // résout « android » en URI de namespace (miroir XML).
        verify(parser, times(1)).getAttributeValue(ANDROID_NS, "layout_column");
        verify(parser, times(1)).getAttributeValue(ANDROID_NS, "layout_rowSpan");
    }

    @Test
    @DisplayName("layout_gravity d'un enfant RelativeLayout est traduit en règles parent (plus de CENTER_IN_PARENT imposé)")
    void graviteRelativeTraduiteEnRegles() {
        // Avant v3.5.1, TOUTE gravité retombait en CENTER_IN_PARENT — la
        // valeur lue était ignorée. La translation est PURE : les règles
        // sont des constantes réelles même sous l'android.jar mockable.
        assertThat(BridgeInflater.reglesDeGraviteRelative("end|bottom")).containsExactly(
                RelativeLayout.ALIGN_PARENT_END, RelativeLayout.ALIGN_PARENT_BOTTOM);
        assertThat(BridgeInflater.reglesDeGraviteRelative("center"))
                .containsExactly(RelativeLayout.CENTER_IN_PARENT);
        assertThat(BridgeInflater.reglesDeGraviteRelative("center_vertical"))
                .containsExactly(RelativeLayout.CENTER_VERTICAL);
        assertThat(BridgeInflater.reglesDeGraviteRelative("start"))
                .containsExactly(RelativeLayout.ALIGN_PARENT_START);
        assertThat(BridgeInflater.reglesDeGraviteRelative("top"))
                .containsExactly(RelativeLayout.ALIGN_PARENT_TOP);
        // Gravité absente : AUCUNE règle (jamais de Centre imposé).
        assertThat(BridgeInflater.reglesDeGraviteRelative(null)).isEmpty();
        // Bits FILL sans équivalent : aucune règle silencieuse.
        assertThat(BridgeInflater.reglesDeGraviteRelative("fill")).isEmpty();
        assertThat(BridgeInflater.reglesDeGraviteRelative("end|bottom"))
                .containsExactly(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.ALIGN_PARENT_BOTTOM)
                .doesNotContain(RelativeLayout.CENTER_IN_PARENT);
        // Le poseur de règles ne lève pas sous l'android.jar mockable
        // (addRule no-op) : le chemin complet est AUSSI parcouru.
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(10, 20);
        inflater.applyRelativeLayoutGravity(lp, "end|bottom");
        inflater.applyRelativeLayoutGravity(lp, null);
    }

    @Test
    @DisplayName("ids dynamiques STABLES : même nom → même id, sur les DEUX chemins")
    void idsDynamiquesStablesSurLesDeuxChemins() throws Exception {
        Method reference = BridgeInflater.class.getDeclaredMethod("resolveIdReference", String.class);
        reference.setAccessible(true);
        Method attribut = BridgeInflater.class.getDeclaredMethod("resolveViewId", String.class);
        attribut.setAccessible(true);

        // Le MÊME nom passe par la référence (@id/bas, layout_below…) et
        // par l'attribut (@+id/bas) : UN SEUL id pour les deux — avant
        // v3.5.1 chaque chemin régénérait le sien (View.generateViewId et
        // compteur), cassant ancres et contraintes selon l'ordre.
        int basParReference = (int) reference.invoke(inflater, "@id/bas");
        int basParAttribut = (int) attribut.invoke(inflater, "@+id/bas");
        assertThat(basParReference).isEqualTo(basParAttribut);
        assertThat((int) reference.invoke(inflater, "@id/bas")).isEqualTo(basParReference);

        // Deux noms : deux ids.
        int haut = (int) reference.invoke(inflater, "@id/haut");
        assertThat(haut).isNotEqualTo(basParReference);

        // reset() : la map repart DÉTERMINISTE (compteur à ID_START —
        // nouveau document, mêmes prénoms possibles) : les DEUX chemins
        // continuent de s'accorder, clef du non-déterminisme réparé.
        inflater.reset();
        int basApresReset = (int) reference.invoke(inflater, "@id/bas");
        assertThat(basApresReset).isEqualTo((int) attribut.invoke(inflater, "@+id/bas"));
        assertThat(basApresReset).isNotEqualTo(haut);
    }
}
