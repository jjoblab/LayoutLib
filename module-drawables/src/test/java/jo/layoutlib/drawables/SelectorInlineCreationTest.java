package jo.layoutlib.drawables;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.resources.ResourceResolverImpl;
import jo.layoutlib.resources.ResourceTable;
import jo.layoutlib.resources.ResourceQualifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de création effective des {@code <selector>} à items inline par
 * {@link DrawableResolverImpl} (ADR 0116 — L4).
 *
 * <p>Avant L4, seul l'attribut {@code android:drawable} d'un
 * {@code <item>} était lu : un enfant inline ({@code <shape>},
 * {@code <color>}…) était consommé mais jamais capturé, l'item était
 * résolu à {@code null} et sauté. La référence garde la priorité sur
 * l'inline (contrat P0) ; un item non résolvable reste ignoré sans
 * exception.</p>
 *
 * <p>En JVM (mockable android.jar), on vérifie la structure du résultat ;
 * un {@code <vector>} inline ne résout pas nativement (Resources
 * indisponible) et sert donc de « poison pill » pour prouver la priorité
 * de la référence.</p>
 *
 * @author jo@Dev
 * @since 3.1
 */
@DisplayName("DrawableResolverImpl — selectors à items inline (L4)")
class SelectorInlineCreationTest {

    private static final String NS =
            "xmlns:android=\"http://schemas.android.com/apk/res/android\"";

    /** Resolver avec une couleur nommée (pour les items par référence). */
    private static ResourceResolverImpl resolverWithColors() {
        ResourceTable table = new ResourceTable();
        table.putColor("couleur_presse", "#FFFF5722", ResourceQualifier.DEFAULT);
        table.putColor("couleur_repos", "#FFF2EDF7", ResourceQualifier.DEFAULT);
        return new ResourceResolverImpl(table);
    }

    private static DrawableResolverImpl selectorResolver() {
        return new DrawableResolverImpl(resolverWithColors(),
                new DimensionConverter(2f, 2f, 320f));
    }

    @Nested
    @DisplayName("Items inline seuls")
    class PureInlineItems {

        @Test
        @DisplayName("un <color> inline produit un StateListDrawable")
        void createsStateListFromInlineColor() {
            Drawable drawable = selectorResolver().parse(
                    "<selector " + NS + ">\n"
                            + "  <item>\n"
                            + "    <color android:color=\"#FF6750A4\"/>\n"
                            + "  </item>\n"
                            + "</selector>", null);

            assertThat(drawable).isInstanceOf(StateListDrawable.class);
        }

        @Test
        @DisplayName("un <shape> inline produit un StateListDrawable")
        void createsStateListFromInlineShape() {
            Drawable drawable = selectorResolver().parse(
                    "<selector " + NS + ">\n"
                            + "  <item android:state_pressed=\"true\">\n"
                            + "    <shape android:shape=\"oval\">\n"
                            + "      <solid android:color=\"#FFFF5722\"/>\n"
                            + "    </shape>\n"
                            + "  </item>\n"
                            + "  <item>\n"
                            + "    <color android:color=\"#FFF2EDF7\"/>\n"
                            + "  </item>\n"
                            + "</selector>", null);

            assertThat(drawable).isInstanceOf(StateListDrawable.class);
        }

        @Test
        @DisplayName("un <selector> imbriqué inline est résolu récursivement")
        void resolvesNestedInlineSelector() {
            Drawable drawable = selectorResolver().parse(
                    "<selector " + NS + ">\n"
                            + "  <item>\n"
                            + "    <selector>\n"
                            + "      <item android:drawable=\"@color/couleur_repos\"/>\n"
                            + "    </selector>\n"
                            + "  </item>\n"
                            + "</selector>", null);

            assertThat(drawable).isInstanceOf(StateListDrawable.class);
        }
    }

    @Nested
    @DisplayName("Priorité référence > inline (contrat P0)")
    class ReferencePriority {

        @Test
        @DisplayName("la référence résolvable prime sur l'inline non résolvable en JVM")
        void referenceWinsOverInline() {
            // L'item porte une réf @color/ résolvable ET un <vector> inline
            // (non résolvable en JVM). Si l'inline était prioritaire,
            // l'item serait sauté → selector null. Non-null = la réf a gagné.
            Drawable drawable = selectorResolver().parse(
                    "<selector " + NS + ">\n"
                            + "  <item android:drawable=\"@color/couleur_repos\">\n"
                            + "    <vector android:width=\"24dp\" android:height=\"24dp\"\n"
                            + "           android:viewportWidth=\"24\" android:viewportHeight=\"24\">\n"
                            + "      <path android:fillColor=\"#FF000000\"\n"
                            + "            android:pathData=\"M4,4 L20,20\"/>\n"
                            + "    </vector>\n"
                            + "  </item>\n"
                            + "</selector>", null);

            assertThat(drawable).isInstanceOf(StateListDrawable.class);
        }

        @Test
        @DisplayName("référence inconnue → repli sur l'inline")
        void unknownReferenceFallsBackToInline() {
            Drawable drawable = selectorResolver().parse(
                    "<selector " + NS + ">\n"
                            + "  <item android:drawable=\"@drawable/inconnu_au_bataillon\">\n"
                            + "    <color android:color=\"#FF6750A4\"/>\n"
                            + "  </item>\n"
                            + "</selector>", null);

            assertThat(drawable).isInstanceOf(StateListDrawable.class);
        }
    }

    @Nested
    @DisplayName("Tolérance / placeholder")
    class Tolerance {

        @Test
        @DisplayName("un seul item inline vector (non résolvable JVM) → selector null, sans exception")
        void vectorOnlyInlineYieldsNull() {
            Drawable drawable = selectorResolver().parse(
                    "<selector " + NS + ">\n"
                            + "  <item>\n"
                            + "    <vector android:width=\"24dp\" android:height=\"24dp\"\n"
                            + "           android:viewportWidth=\"24\" android:viewportHeight=\"24\"/>\n"
                            + "  </item>\n"
                            + "</selector>", null);

            // Le vector inline ne résout pas en JVM (Resources indisponible) ;
            // l'item est sauté → aucun item résolu → null (placeholder), pas d'exception
            assertThat(drawable).isNull();
        }

        @Test
        @DisplayName("items mixtes : seuls les résolvables comptent")
        void mixedItemsKeepResolvableOnes() {
            Drawable drawable = selectorResolver().parse(
                    "<selector " + NS + ">\n"
                            + "  <item android:state_pressed=\"true\">\n"
                            + "    <vector android:width=\"24dp\" android:height=\"24dp\"/>\n"
                            + "  </item>\n"
                            + "  <item>\n"
                            + "    <color android:color=\"#FFF2EDF7\"/>\n"
                            + "  </item>\n"
                            + "</selector>", null);

            // L'item vector est sauté, l'item color suffit → StateListDrawable
            assertThat(drawable).isInstanceOf(StateListDrawable.class);
        }

        @Test
        @DisplayName("un item sans drawable ni inline est ignoré (aucune exception)")
        void drawablelessItemIsIgnored() {
            Drawable drawable = selectorResolver().parse(
                    "<selector " + NS + ">\n"
                            + "  <item android:state_pressed=\"true\"/>\n"
                            + "  <item android:drawable=\"@color/couleur_repos\"/>\n"
                            + "</selector>", null);

            assertThat(drawable).isInstanceOf(StateListDrawable.class);
        }
    }
}
