package jo.layoutlib.inflater;

import android.content.Context;
import android.view.View;

import jo.layoutlib.resources.ResourceResolver;

/**
 * Façade publique du mini-layoutlib.
 *
 * <p>Cette classe est le point d'entrée unique pour les applications qui
 * souhaitent inflater des layouts Android en dehors du système
 * {@code LayoutInflater} natif. Elle agrège tous les modules internes et
 * expose une API minimaliste :</p>
 *
 * <pre>{@code
 * MiniLayoutLib layoutLib = new MiniLayoutLib(context);
 * layoutLib.setProjectResources(resFolder);
 * layoutLib.setTheme("Theme.Material3.DayNight");
 * View root = layoutLib.inflate(xml);
 * }</pre>
 *
 * <h2>Migration depuis DesignInflater</h2>
 * <p>La signature est volontairement proche de l'ancien DesignInflater pour
 * faciliter la migration :</p>
 *
 * <pre>{@code
 * // Avant (v3.31.7)
 * DesignInflater inflater = new DesignInflater(this);
 * View view = inflater.inflate(xml);
 *
 * // Après (avec mini-layoutlib)
 * MiniLayoutLib layoutLib = new MiniLayoutLib(this);
 * View view = layoutLib.inflate(xml);
 * }</pre>
 *
 * <p>Voir {@code MIGRATION.md} pour le guide complet.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class MiniLayoutLib {

    /** Contexte Android. */
    private final Context context;

    /** Inflater interne. */
    private final BridgeInflater inflater;

    /** Résolveur de ressources configuré. */
    private ResourceResolver resourceResolver;

    /** Thème courant à appliquer aux vues. */
    private String themeName = "Theme.Material3.DayNight";

    /** Mode strict (lever des exceptions sur attributs inconnus). */
    private boolean strictMode = false;

    /**
     * Construit une instance du mini-layoutlib.
     *
     * @param context contexte Android, ne peut pas être {@code null}
     * @throws IllegalArgumentException si le contexte est {@code null}
     */
    public MiniLayoutLib(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Le contexte ne peut pas être null");
        }
        this.context = context;
        this.inflater = new BridgeInflater(context);
    }

    /**
     * Configure le dossier de resources du projet à utiliser pour résoudre
     * {@code @color/}, {@code @string/}, {@code @dimen/}, {@code @drawable/},
     * {@code @layout/}, etc.
     *
     * @param resFolderAbsolutePath chemin absolu vers le dossier {@code res/}
     * @return cette instance (chaînage)
     */
    public MiniLayoutLib setProjectResources(String resFolderAbsolutePath) {
        // Délègue au module-resources (à brancher quand le module sera prêt)
        // resourceResolver = new ResourceResolver(resFolderAbsolutePath);
        // inflater.setResourceResolver(resourceResolver);
        return this;
    }

    /**
     * Définit le résolveur de resources à utiliser.
     *
     * @param resolver le résolveur
     * @return cette instance (chaînage)
     */
    public MiniLayoutLib setResourceResolver(ResourceResolver resolver) {
        this.resourceResolver = resolver;
        this.inflater.setResourceResolver(resolver);
        return this;
    }

    /**
     * Définit le nom du thème à appliquer.
     *
     * @param themeName nom du thème (ex. {@code Theme.Material3.DayNight})
     * @return cette instance (chaînage)
     */
    public MiniLayoutLib setTheme(String themeName) {
        this.themeName = themeName;
        return this;
    }

    /**
     * Active ou désactive le mode strict.
     *
     * @param strict {@code true} pour lever une exception sur tout attribut
     *               inconnu
     * @return cette instance (chaînage)
     */
    public MiniLayoutLib setStrictMode(boolean strict) {
        this.strictMode = strict;
        this.inflater.setStrictMode(strict);
        return this;
    }

    /**
     * Inflate un layout XML en arbre de vues.
     *
     * @param xml le XML source
     * @return la vue racine
     * @throws InflateException si l'inflation échoue
     */
    public View inflate(String xml) {
        return inflater.inflate(xml);
    }

    /**
     * Recherche une vue par id après inflation.
     *
     * @param id l'id de la vue
     * @return la vue, ou {@code null}
     */
    public View findViewById(int id) {
        return inflater.findViewById(id);
    }

    /**
     * @return le nom du thème courant
     */
    public String getThemeName() {
        return themeName;
    }

    /**
     * @return le résolveur de resources configuré
     */
    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    /**
     * @return l'inflater interne (pour usage avancé)
     */
    public BridgeInflater getInflater() {
        return inflater;
    }

    /**
     * Remet à zéro l'état interne (cache, index des ids).
     */
    public void reset() {
        inflater.reset();
    }
}
