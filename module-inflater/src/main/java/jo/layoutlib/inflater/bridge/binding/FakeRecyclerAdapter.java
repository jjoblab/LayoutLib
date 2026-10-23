package jo.layoutlib.inflater.bridge.binding;

import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Adapter FACTICE de prévisualisation pour {@code RecyclerView}
 * (v3.6.0 — étude PE14 § 2.4-3, S6 du plan).
 *
 * <p>La référence contourne la classe {@code RecyclerView} — absente de
 * son classpath desktop — par réflexion ({@code RecyclerViewUtil}).
 * Ici la bibliothèque est EMBARQUÉE (v3.4.9) : l'adapter est un vrai
 * {@code RecyclerView.Adapter} typé, et la vraie
 * {@code RecyclerView} fait son travail de recyclage/mesure.</p>
 *
 * <p>{@code RecyclerView} n'est PAS une {@code AdapterView} : elle exige
 * un {@code LayoutManager} — {@code BridgeInflater.brancherApercuListe}
 * en pose un {@code LinearLayoutManager} vertical par défaut si le
 * document n'en déclare pas (miroir du comportement Android Studio, qui
 * prévisualise verticalement toute {@code RecyclerView} nue).</p>
 *
 * @author jo@Dev
 * @since 3.6.0
 */
public class FakeRecyclerAdapter
        extends RecyclerView.Adapter<FakeRecyclerAdapter.Teneur> {

    /** Teneur minimale : une seule vue de rangée. */
    static final class Teneur extends RecyclerView.ViewHolder {

        /** Construit la teneur autour de la vue de rangée. */
        Teneur(View vue) {
            super(vue);
        }
    }

    /** Nombre total d'items (honoré tel quel). */
    private final int nombre;

    /** Fabrique des vues de rangées (partagée avec FakeAdapter). */
    private final FakeAdapter.FournisseurVueItem fournisseur;

    /**
     * Construit l'adapter factice.
     *
     * @param nombre      le nombre d'items (≥ 0)
     * @param fournisseur la fabrique de vues de rangées
     */
    public FakeRecyclerAdapter(int nombre,
                               FakeAdapter.FournisseurVueItem fournisseur) {
        if (nombre < 0) {
            nombre = 0;
        }
        this.nombre = nombre;
        this.fournisseur = fournisseur;
    }

    @Override
    public Teneur onCreateViewHolder(ViewGroup parent, int viewType) {
        return new Teneur(fournisseur.creerVueItem(new AdapterItem(0)));
    }

    @Override
    public void onBindViewHolder(Teneur teneur, int position) {
        // Contenu statique : la rangée gonflée porte déjà son
        // tools:text/preview — rien à lier par position (miroir du
        // FakeAdapter AOSP, qui ne lie rien non plus).
    }

    @Override
    public int getItemCount() {
        return nombre;
    }
}
