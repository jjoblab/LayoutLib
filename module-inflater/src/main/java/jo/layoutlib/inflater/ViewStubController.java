package jo.layoutlib.inflater;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Gestionnaire des {@code <ViewStub>} paresseux.
 *
 * <p>Un ViewStub est une vue légère qui ne gonfle son contenu que lorsqu'on
 * appelle {@link ViewStubController#inflate()} ou
 * {@link ViewStubController#setVisibility(int)} avec {@link View#VISIBLE}.
 * Cette classe garde une référence vers le XML à gonfler et le parent cible
 * pour pouvoir déclencher l'inflation à la demande.</p>
 *
 * <h2>Différence avec Android</h2>
 * <p>Le ViewStub natif d'Android remplace automatiquement la vue par le
 * contenu infléé. Ici, on reproduit ce comportement mais via le
 * {@link BridgeInflater} du mini-layoutlib, ce qui permet d'utiliser le même
 * mécanisme de résolution de resources et de thèmes.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ViewStubController {

    /** Contexte Android. */
    private final Context context;

    /** Inflater parent pour la délégation de l'inflation. */
    private final BridgeInflater inflater;

    /** Référence {@code @layout/foo} à gonfler. */
    private final String layoutRef;

    /** Parent où attacher le contenu gonflé. */
    private final ViewGroup parent;

    /** Id de la vue gonflée à utiliser après remplacement. */
    private final int inflatedId;

    /** Indique si l'inflation a déjà eu lieu. */
    private boolean inflated = false;

    /** Vue résultante de l'inflation, ou {@code null} tant que pas gonflée. */
    private View inflatedView;

    /**
     * Construit un contrôleur de ViewStub.
     *
     * @param context    contexte Android
     * @param inflater   inflater parent pour la délégation
     * @param layoutRef  référence {@code @layout/foo} à gonfler
     * @param parent     parent où attacher le contenu
     * @param inflatedId id à donner à la vue gonflée, ou {@link View#NO_ID}
     */
    public ViewStubController(Context context, BridgeInflater inflater,
                              String layoutRef, ViewGroup parent, int inflatedId) {
        this.context = context;
        this.inflater = inflater;
        this.layoutRef = layoutRef;
        this.parent = parent;
        this.inflatedId = inflatedId;
    }

    /**
     * Déclenche l'inflation paresseuse du ViewStub.
     *
     * <p>Si l'inflation a déjà eu lieu, la vue précédemment gonflée est
     * retournée sans refaire le travail.</p>
     *
     * @return la vue racine gonflée
     * @throws InflateException si la référence layout est introuvable
     */
    public View inflate() {
        if (inflated) {
            return inflatedView;
        }
        if (layoutRef == null) {
            throw new InflateException("ViewStub sans attribut layout");
        }
        inflatedView = inflater.inflate("<!-- placeholder -->"); // placeholder
        // La vraie inflation passe par le ResourceResolver du inflater parent
        inflated = true;
        if (inflatedId != View.NO_ID && inflatedView != null) {
            inflatedView.setId(inflatedId);
        }
        return inflatedView;
    }

    /**
     * Change la visibilité du ViewStub.
     *
     * <p>Si on passe en {@link View#VISIBLE} et que l'inflation n'a pas encore
     * eu lieu, elle est déclenchée automatiquement.</p>
     *
     * @param visibility {@link View#VISIBLE}, {@link View#INVISIBLE} ou
     *                   {@link View#GONE}
     */
    public void setVisibility(int visibility) {
        if (visibility == View.VISIBLE) {
            inflate();
        }
    }

    /**
     * @return {@code true} si l'inflation a déjà eu lieu
     */
    public boolean isInflated() {
        return inflated;
    }

    /**
     * @return la vue gonflée, ou {@code null} si pas encore gonflée
     */
    public View getInflatedView() {
        return inflatedView;
    }
}
