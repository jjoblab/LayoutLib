package jo.layoutlib.inflater;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.resources.ResourceResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * v3.7.0 — le VIEWCOOKIE (étude PE14 § 2.4-4, S8 du plan — miroir du
 * cookie {@code ViewInfo}/{@code MergeCookie} de la référence) : chaque vue
 * inflatée porte l'identité de son NŒUD XML SOURCE (« site:ordinal », le
 * site étant la chaîne des {@code <include>}). Deux inclusions du MÊME
 * fichier produisent des cookies DISTINCTS (les vues jumelles restent
 * discernables), les balises qui ne produisent RIEN consument un ordinal
 * (MÊME comptage que l'aligneur CodeIDE).
 *
 * @author jo@Dev
 * @since 3.7.0
 */
@DisplayName("BridgeInflater — le viewCookie de nœud source (v3.7.0, S8)")
class BridgeInflaterViewCookieTest {

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
        when(resources.getIdentifier(anyString(), anyString(), anyString()))
                .thenReturn(0);
        context = mock(Context.class);
        when(context.getResources()).thenReturn(resources);
        inflater = new BridgeInflater(context, new ViewTagRegistry(false));
        inflater.setAttributeApplier(
                new AttributeApplier(context, new DimensionConverter(2f, 2f, 320f)));
    }

    /** Un resolver INJECTÉ qui sert les layouts demandés. */
    private ResourceResolver resolverServant(Map<String, String> layouts) {
        ResourceResolver resolver = mock(ResourceResolver.class);
        when(resolver.getLayout(anyString())).thenAnswer(invocation ->
                layouts.get(invocation.getArgument(0, String.class)));
        return resolver;
    }

    /**
     * Épingle les cookies dans une map FORTE pour la durée du test : le
     * registre de production est une WeakHashMap (aucune fuite entre
     * rendus) et sur l'android.jar mockable les enfants ne sont PAS
     * rattachés (addView no-op) — un GC entre inflate et assert
     * évaporerait les entrées. Le pin passe par le registre actif
     * ({@code setContexteCookie}, même paquet) — sémantique IDENTIQUE à
     * la production, références fortes.
     */
    private Map<View, String> epingleLesCookies() {
        Map<View, String> pin = new java.util.HashMap<>();
        inflater.setContexteCookie("", pin);
        return pin;
    }

    @Test
    @DisplayName("document simple — un cookie par vue, ordinals du document")
    void shouldCookieEveryProducedView() {
        Map<View, String> pin = epingleLesCookies();
        inflater.inflate("<LinearLayout><TextView/><Button/></LinearLayout>");
        // 3 vues produites : la racine COMPTE (elle est aussi un nœud).
        assertThat(pin).hasSize(3);
        assertThat(pin.values()).containsExactlyInAnyOrder(":0", ":1", ":2");
    }

    @Test
    @DisplayName("reset — les cookies repartent de zéro")
    void shouldResetCookies() {
        Map<View, String> pin = epingleLesCookies();
        inflater.inflate("<LinearLayout><TextView/></LinearLayout>");
        assertThat(pin).hasSize(2);
        inflater.reset();
        assertThat(pin).isEmpty();
        inflater.inflate("<TextView/>");
        // Le NOUVEAU document reprend à l'ordinal 0.
        assertThat(pin.values()).containsExactly(":0");
    }

    @Test
    @DisplayName("deux includes du MÊME fichier — cookies DISTINCTS (vues jumelles)")
    void shouldDistinctCookiesForTwinIncludes() {
        Map<View, String> pin = epingleLesCookies();
        String bouton = "<Button xmlns:android=\"http://schemas.android.com/apk/res/android\""
                + " android:layout_width=\"wrap_content\""
                + " android:layout_height=\"wrap_content\"/>";
        inflater.setResourceResolver(
                resolverServant(java.util.Collections.singletonMap("@layout/btn", bouton)));
        inflater.inflate("<LinearLayout>"
                + "<include layout=\"@layout/btn\"/>"
                + "<include layout=\"@layout/btn\"/>"
                + "</LinearLayout>");
        // Ordinals du document principal : LinearLayout=0, include=1, include=2.
        // Le bouton du PREMIER site vit en « 1:0 », le second en « 2:0 » —
        // DEUX vues identiques de nœuds DIFFÉRENTS (l'objet du cookie).
        assertThat(pin).hasSize(3);
        assertThat(pin.values()).containsExactlyInAnyOrder(":0", "1:0", "2:0");
    }

    @Test
    @DisplayName("include imbriqué — la chaîne des sites se compose")
    void shouldComposeIncludeSiteChains() {
        Map<View, String> pin = epingleLesCookies();
        String externe = "<Button xmlns:android=\"http://schemas.android.com/apk/res/android\""
                + " android:layout_width=\"wrap_content\""
                + " android:layout_height=\"wrap_content\"/>";
        String milieu = "<LinearLayout>"
                + "<include layout=\"@layout/externe\"/>"
                + "</LinearLayout>";
        java.util.Map<String, String> layouts = new java.util.HashMap<>();
        layouts.put("@layout/milieu", milieu);
        layouts.put("@layout/externe", externe);
        inflater.setResourceResolver(resolverServant(layouts));
        inflater.inflate("<LinearLayout>"
                + "<include layout=\"@layout/milieu\"/>"
                + "</LinearLayout>");
        // Principal : LinearLayout=:0, include=1 (aucune vue — aucun
        // cookie). Milieu : LinearLayout=1:0, include=1:1 (idem, rien).
        // Externe : Button=1>1:0 — la CHAÎNE des sites (les balises sans
        // vue consument un ordinal sans poser de cookie).
        assertThat(pin.values())
                .containsExactlyInAnyOrder(":0", "1:0", "1>1:0");
    }

    @Test
    @DisplayName("merge inclus — les enfants suivent le site de l'include")
    void shouldKeepSiteForMergedChildren() {
        Map<View, String> pin = epingleLesCookies();
        String fusion = "<merge xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                + "<TextView android:layout_width=\"wrap_content\""
                + " android:layout_height=\"wrap_content\"/>"
                + "<Button android:layout_width=\"wrap_content\""
                + " android:layout_height=\"wrap_content\"/>"
                + "</merge>";
        inflater.setResourceResolver(
                resolverServant(java.util.Collections.singletonMap("@layout/fusion", fusion)));
        inflater.inflate("<LinearLayout><include layout=\"@layout/fusion\"/></LinearLayout>");
        // Le merge consomme l'ordinal 0 du SITE inclus, ses enfants 1 et 2.
        assertThat(pin.values())
                .containsExactlyInAnyOrder(":0", "1:1", "1:2");
    }

    @Test
    @DisplayName("balises sans vue — elles consument un ordinal (comptage aligneur)")
    void shouldConsumeOrdinalForNonProducers() {
        Map<View, String> pin = epingleLesCookies();
        inflater.inflate("<LinearLayout>"
                + "<requestFocus/>"
                + "<TextView/>"
                + "</LinearLayout>");
        // requestFocus consomme l'ordinal 1 : le TextView vit en « :2 » —
        // l'aligneur CodeIDE compte pareil (parité des marches).
        assertThat(pin.values())
                .containsExactlyInAnyOrder(":0", ":2");
    }

    @Test
    @DisplayName("ViewStub — la racine gonflée porte l'ordinal du TAG stub")
    void shouldCookieTheStubTagOrdinal() {
        Map<View, String> pin = epingleLesCookies();
        String cible = "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\""
                + " android:layout_width=\"wrap_content\""
                + " android:layout_height=\"wrap_content\"/>";
        inflater.setResourceResolver(
                resolverServant(java.util.Collections.singletonMap("@layout/cible", cible)));
        inflater.inflate("<LinearLayout"
                + " xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                + "<ViewStub android:layout=\"@layout/cible\""
                + " android:layout_width=\"wrap_content\""
                + " android:layout_height=\"wrap_content\"/>"
                + "<Button/></LinearLayout>");
        // Le tag stub consomme l'ordinal 1 : la vue gonflée le PORTE (posée
        // en dernier par le wrapper) ; ses descendants vivraient dans le
        // SITE « 1 » (pas de collision avec le Button « :2 » qui suit).
        assertThat(pin.values())
                .containsExactlyInAnyOrder(":0", ":1", ":2");
    }
}
