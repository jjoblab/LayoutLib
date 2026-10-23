package jo.layoutlib.inflater.bridge.binding;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * Aide au montage des listes d'aperçu (v3.6.0 — étude PE14 § 2.4-3,
 * S6 du plan).
 *
 * <p>Miroir de l'{@code AdapterBinding} de la référence : porte les
 * CONSTANTES de remplissage et l'item TEXTE PAR DÉFAUT utilisé quand la
 * liste ne déclare PAS de {@code tools:listitem} — Android Studio remplit
 * de la même façon une {@code ListView} nue ({@code simple_list_item_1},
 * rangées « Item N »). Sur appareil, le layout framework
 * {@code @android:layout/simple_list_item_1} est gonflé TEL QUEL quand il
 * se résout ; repli sur un {@code TextView} simple sinon (JVM de test,
 * ressources hôte exotiques).</p>
 *
 * @author jo@Dev
 * @since 3.6.0
 */
public final class AdapterHelper {

    /**
     * Nombre d'items par défaut : GENEREUX — la vraie
     * {@code AdapterView} ne demande que les rangées VISIBLES, l'aperçu
     * remplit donc la liste quelle que soit sa taille (la référence doit
     * deviner un compte car rien n'est vivant desktop ; ici le framework
     * fait le calcul). {@code tools:itemCount} le borne explicitement.
     */
    public static final int NOMBRE_DEFAUT = 200;

    /**
     * Item texte par défaut de la référence AOSP, gonflé depuis les
     * resources framework de l'HÔTE.
     */
    private static final String LAYOUT_ITEM_SIMPLE = "simple_list_item_1";

    /** Constructeur privé — classe utilitaire. */
    private AdapterHelper() {
        // Classe utilitaire — pas d'instance
    }

    /**
     * La fabrique d'items texte PAR DÉFAUT : gonfle
     * {@code @android:layout/simple_list_item_1} (vraie rangée framework,
     * thème du contexte de rendu) et pose le libellé « Item N » ; repli
     * sur un {@code TextView} direct si le layout framework ne se résout
     * pas (JVM de test).
     *
     * @param contexte le contexte de rendu (celui du thème projet)
     * @return la fabrique prête pour {@link FakeAdapter}
     */
    public static FakeAdapter.FournisseurVueItem fournisseurTexteParDefaut(
            Context contexte) {
        return item -> {
            View rangee = gonflerItemSimple(contexte);
            if (rangee instanceof TextView) {
                ((TextView) rangee).setText(item.getLibelle());
            }
            return rangee;
        };
    }

    /** Gonfle le layout framework, repli TextView si introuvable. */
    private static View gonflerItemSimple(Context contexte) {
        int id = 0;
        try {
            id = contexte.getResources().getIdentifier(
                    LAYOUT_ITEM_SIMPLE, "layout", "android");
        } catch (RuntimeException e) {
            id = 0;
        }
        if (id != 0) {
            try {
                return LayoutInflater.from(contexte).inflate(id, null);
            } catch (RuntimeException e) {
                // Layout framework illisible — repli TextView ci-dessous
            }
        }
        return new TextView(contexte);
    }
}
