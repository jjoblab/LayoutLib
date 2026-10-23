package jo.layoutlib.inflater.bridge.binding;

/**
 * Descripteur d'un item d'aperçu (v3.6.0 — étude PE14 § 2.4-3, S6 du plan).
 *
 * <p>La référence ({@code impl/binding/AdapterItem} AOSP) porte un
 * {@code XmlPullParser} complet par item pour varier le contenu des
 * rangées répétées ; ici chaque rangée rend LE MÊME layout
 * {@code tools:listitem} (contenu identique — même choix qu'Android
 * Studio pour un {@code tools:listitem} littéral), le descripteur ne
 * porte donc que la position et son libellé « Item N ».</p>
 *
 * @author jo@Dev
 * @since 3.6.0
 */
public class AdapterItem {

    /** Position de l'item dans la liste (base 0). */
    private final int position;

    /**
     * Construit le descripteur d'un item.
     *
     * @param position la position (base 0)
     */
    public AdapterItem(int position) {
        this.position = position;
    }

    /** @return la position (base 0) */
    public int getPosition() {
        return position;
    }

    /**
     * Le libellé de prévisualisation de la rangée (« Item 1 » pour la
     * position 0 — base 1, convention Android Studio).
     *
     * @return le libellé affichable
     */
    public String getLibelle() {
        return "Item " + (position + 1);
    }
}
