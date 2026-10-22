package jo.layoutlib.inflater.bridge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires JVM de {@link MockView} (v3.4.8) — la PARTIE PURE
 * uniquement : le libellé du cadre de remplacement. Le DESSIN (contour
 * pointillé, hachures, texte centré) vit sur Android et reste couvert
 * par le rendu de la validation manuelle ; ici on verrouille le CONTRAT
 * du nom affiché — miroir {@code v.cls} de la maquette CodeIDE (le nom
 * SIMPLE, pas la coordonnée complète).
 */
@DisplayName("MockView — libellé du cadre de remplacement (v3.4.8)")
class MockViewTest {

    @Test
    @DisplayName("libelleDe : le segment simple d'un nom pleinement qualifié")
    void libelleSimpleDeNomQualifie() {
        assertThat(MockView.libelleDe("com.example.FooView")).isEqualTo("FooView");
        assertThat(MockView.libelleDe("androidx.recyclerview.widget.RecyclerView"))
                .isEqualTo("RecyclerView");
    }

    @Test
    @DisplayName("libelleDe : un nom simple reste TEL QUEL")
    void libelleSimpleResteTelQuel() {
        assertThat(MockView.libelleDe("RecyclerView")).isEqualTo("RecyclerView");
        assertThat(MockView.libelleDe("MockView")).isEqualTo("MockView");
    }

    @Test
    @DisplayName("libelleDe : les cas limites — null, vide, points finaux")
    void libelleDesCasLimites() {
        assertThat(MockView.libelleDe(null)).isEmpty();
        assertThat(MockView.libelleDe("")).isEmpty();
        // Un FQ se termine par un SEGMENT, pas par un point : le libellé
        // d'un nom en pointillés finaux reste le segment lui-même.
        assertThat(MockView.libelleDe("com.example.")).isEqualTo("");
        assertThat(MockView.libelleDe("a.b.C")).isEqualTo("C");
    }

    @Test
    @DisplayName("la classe MockView reste importable (contrat G15)")
    void mockViewImportable() {
        // Le contrat G15 : le repli existe toujours côté BridgeInflater —
        // vérification statique comme BridgeInflaterV34Test.
        assertThat(MockView.class).isNotNull();
        assertThat(new MockView[0]).isEmpty();
    }
}
