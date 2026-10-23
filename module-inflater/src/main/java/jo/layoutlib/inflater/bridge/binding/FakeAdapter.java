package jo.layoutlib.inflater.bridge.binding;

import android.view.View;
import android.view.ViewGroup;

/**
 * Adapter FACTICE de prévisualisation (v3.6.0 — étude PE14 § 2.4-3, S6 du
 * plan ; branché depuis {@code BridgeInflater.brancherApercuListe}).
 *
 * <p>Miroir assumé de l'{@code AdapterBinding} de la référence
 * ({@code impl/binding/AdapterBinding} + {@code FakeAdapter} AOSP) : la
 * référence doit fournir un modèle d'items AU moteur de rendu desktop car
 * rien n'est vivant de son côté ; ici sur appareil, la VRAIE
 * {@code AdapterView} fait son travail — elle demande {@link #getView}
 * pour chaque rangée visible, l'adapter répond en gonflant le layout
 * {@code tools:listitem} (ou l'item texte par défaut).</p>
 *
 * <p>Le recyclage est ignoré (chaque appel gonfle) : une planche de
 * prévisualisation ne défile pas, le coût est borné par le nombre de
 * rangées visibles — même choix que la référence, qui régénère aussi chaque
 * rangée au rendu.</p>
 *
 * @author jo@Dev
 * @since 3.6.0
 */
public class FakeAdapter extends android.widget.BaseAdapter {

    /**
     * Fabrique d'une rangée d'item pour une position donnée.
     *
     * <p>Implémentée par {@code BridgeInflater} (gonflage du layout
     * {@code tools:listitem} via un inflater enfant) ou par
     * {@link AdapterHelper} (item texte par défaut « Item N »).</p>
     */
    public interface FournisseurVueItem {

        /**
         * Crée la vue de la rangée {@code position}.
         *
         * @param item le descripteur de l'item (position + libellé)
         * @return la vue gonflée, jamais {@code null}
         */
        View creerVueItem(AdapterItem item);
    }

    /** Nombre total d'items (honoré tel quel). */
    private final int nombre;

    /** Fabrique des vues d'items. */
    private final FournisseurVueItem fournisseur;

    /**
     * Construit l'adapter factice.
     *
     * @param nombre     le nombre d'items (≥ 0)
     * @param fournisseur la fabrique de vues de rangées
     */
    public FakeAdapter(int nombre, FournisseurVueItem fournisseur) {
        if (nombre < 0) {
            nombre = 0;
        }
        this.nombre = nombre;
        this.fournisseur = fournisseur;
    }

    @Override
    public int getCount() {
        return nombre;
    }

    @Override
    public Object getItem(int position) {
        return new AdapterItem(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return fournisseur.creerVueItem(new AdapterItem(position));
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
