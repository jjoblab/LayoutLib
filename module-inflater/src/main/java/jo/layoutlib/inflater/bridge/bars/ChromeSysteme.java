package jo.layoutlib.inflater.bridge.bars;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import jo.layoutlib.resources.api.ActionBarCallback;
import jo.layoutlib.themes.ThemeResolver;

/**
 * Le CHROME SYSTÈME possédé par le MOTEUR de rendu (PE13, ADR 0137) —
 * transport à l'identique du {@code ChromeSysteme} historique de CodeIDE
 * (v0.133.0 → v0.166.5), augmenté de la config de session
 * {@link ConfigChrome}, des insets explicites ({@link InsetsDecor}) et du
 * mode {@link ModePlacementDecor#SURIMPRESSION}.
 *
 * <p><strong>Partage de responsabilité</strong> (miroir du
 * {@code Layout.Builder}/{@code RenderSessionImpl} AOSP) : l'HÔTE ne
 * construit plus AUCUNE vue chrome — il décrit la session (titre résolu,
 * surcharges du menu Calques, capacités étape 5) via {@link ConfigChrome} ;
 * le MOTEUR décide de la COMPOSITION : présences DÉRIVÉES du thème
 * ({@code windowFullscreen} retire la status bar,
 * {@code windowNoTitle}/{@code windowActionBar} retirent l'action bar —
 * correctif C1/ADR 0135 conservé), couleurs résolues
 * ({@code statusBarColor} &gt; {@code colorPrimaryDark} &gt; jetons
 * {@code --s-dark}/{@code --s-pri} de la planche), hauteurs de
 * {@link DimensChrome} (garde-fou G1), régime NUIT relu du résolveur
 * ({@link ThemeResolver#isNightMode()} — jamais un drapeau chrome).</p>
 *
 * <p>Le décor POSÉ PAR DÉFAUT empile status bar / action bar / contenu
 * (poids 1) / barre de navigation — pixel-identique au chrome IDE
 * historique (garde-fou G2). Un thème PLEIN ÉCRAN sans barres rend le
 * CONTENU TEL QUEL (retour anticipé, miroir v0.133.2).</p>
 *
 * @author jo@Dev
 * @since 3.5.0
 */
public class ChromeSysteme {

    /** Fond status bar par défaut — {@code --s-dark} clair de la maquette. */
    private static final int COULEUR_STATUS_BAR_DEFAUT = 0xFF2B2F7A;

    /** Fond status bar par défaut — {@code --s-dark} nuit de la maquette. */
    private static final int COULEUR_STATUS_BAR_NUIT_DEFAUT = 0xFF0C1030;

    /** Fond action bar par défaut — {@code --s-pri} clair de la maquette. */
    private static final int COULEUR_PRIMARY_DEFAUT = 0xFF43559E;

    /** Fond action bar par défaut — {@code --s-pri} nuit de la maquette. */
    private static final int COULEUR_PRIMARY_NUIT_DEFAUT = 0xFFBAC4FF;

    /** Fond de la barre de navigation en clair (maquette {@code --s-sys}). */
    private static final int COULEUR_BARRE_NAVIGATION_JOUR = 0xFF1C1B1F;

    /** Fond de la barre de navigation en nuit (maquette {@code .screen.night --s-sys}). */
    private static final int COULEUR_BARRE_NAVIGATION_NUIT = 0xFF000000;

    private final Context contexte;

    /** Le résolveur de thèmes, ou {@code null} (repli : couleurs/jetons par défaut, jour). */
    private final ThemeResolver themeResolver;

    /** La config de session (titre, surcharges, capacités étape 5). */
    private final ConfigChrome config;

    /** Les insets du DERNIER décor posé (avant envelopper : aucun). */
    private InsetsDecor insetsDernier = InsetsDecor.aucun();

    /**
     * Construit le chrome.
     *
     * @param contexte     le contexte du rendu
     * @param themeResolver le résolveur de thèmes ({@code null} = repli
     *                      couleurs maquette, régime jour)
     * @param config       la config de session
     */
    public ChromeSysteme(Context contexte, ThemeResolver themeResolver, ConfigChrome config) {
        this.contexte = contexte;
        this.themeResolver = themeResolver;
        this.config = config;
    }

    /**
     * Les PRÉSENCES dérivées du thème et des surcharges — PURE (testable
     * JVM), le cœur de la décision D1 : la présence des barres est une
     * donnée du THÈME, la surcharge utilisateur ne sert qu'à forcer.
     *
     * @author jo@Dev
     * @since 3.5.0
     */
    public static final class Presences {

        /** La status bar est-elle posée ? */
        public final boolean avecStatusBar;

        /** L'action bar est-elle posée ? */
        public final boolean avecActionBar;

        /** La barre de navigation est-elle posée ? */
        public final boolean barreNavigation;

        /** Un décor est-il posé (config le demande ET au moins une barre) ? */
        public final boolean poseDecor;

        private Presences(boolean avecStatusBar, boolean avecActionBar,
                           boolean barreNavigation, boolean poseDecor) {
            this.avecStatusBar = avecStatusBar;
            this.avecActionBar = avecActionBar;
            this.barreNavigation = barreNavigation;
            this.poseDecor = poseDecor;
        }
    }

    /**
     * Dérive les présences du thème et de la config — PURE.
     *
     * @param themeResolver le résolveur, ou {@code null}
     * @param config        la config de session
     * @return les présences
     */
    public static Presences presences(ThemeResolver themeResolver, ConfigChrome config) {
        boolean avecActionBar = !attrBool(themeResolver, "windowNoTitle", false)
                && attrBool(themeResolver, "windowActionBar", true);
        boolean avecStatusBar = config.surchargeStatusBar != null
                ? config.surchargeStatusBar
                : !attrBool(themeResolver, "windowFullscreen", false);
        boolean barreNavigation = config.surchargeNavigationBar != null
                ? config.surchargeNavigationBar
                : true;
        boolean poseDecor = config.decor && (avecStatusBar || avecActionBar);
        return new Presences(avecStatusBar, avecActionBar, barreNavigation, poseDecor);
    }

    /**
     * Enveloppe [contenu] dans le décor si la session le demande — sinon le
     * contenu TEL QUEL (config sans décor, ou thème plein écran sans barre).
     *
     * @param contenu la racine inflatée du layout utilisateur
     * @param densite densité du rendu (dp → px — celle du PROFIL)
     * @return le décor posé autour du contenu, ou [contenu] tel quel
     */
    public View envelopper(View contenu, float densite) {
        Presences p = presences(themeResolver, config);
        if (!p.poseDecor) {
            return contenu;
        }
        int statusBarPx = hauteurPx(DimensChrome.STATUS_BAR_HAUTEUR_DP, densite);
        int actionBarPx =
                hauteurPx(DimensChrome.actionBarHauteurDp(themeResolver, densite), densite);
        int barreNavigationPx = hauteurPx(DimensChrome.BARRE_NAVIGATION_HAUTEUR_DP, densite);
        insetsDernier = new InsetsDecor(
                (p.avecStatusBar ? statusBarPx : 0) + (p.avecActionBar ? actionBarPx : 0),
                p.barreNavigation ? barreNavigationPx : 0,
                0,
                0);
        if (config.modePlacement == ModePlacementDecor.SURIMPRESSION) {
            return envelopperSurimpression(contenu, densite, p, statusBarPx, actionBarPx,
                    barreNavigationPx);
        }
        return envelopperPadde(contenu, densite, p, statusBarPx, actionBarPx, barreNavigationPx);
    }

    /**
     * Les insets du DERNIER décor posé (mode PADDE : le contenu est déjà
     * posé sous les barres ; mode SURIMPRESSION : contenu plein cadre, ces
     * insets restent la vérité à consommer en aval).
     *
     * @return les insets en px (aucuns avant {@link #envelopper})
     */
    public InsetsDecor insets() {
        return insetsDernier;
    }

    // ── Assemblage PADDE_PAR_DEFAUT (miroir exact du chrome historique) ───

    private View envelopperPadde(View contenu, float densite, Presences p,
                                 int statusBarPx, int actionBarPx, int barreNavigationPx) {
        LinearLayout conteneur = new LinearLayout(contexte);
        conteneur.setOrientation(LinearLayout.VERTICAL);
        conteneur.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (p.avecStatusBar) {
            StatusBar barre = creerStatusBar();
            barre.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, statusBarPx));
            conteneur.addView(barre);
        }
        if (p.avecActionBar) {
            FrameworkActionBar barre = creerActionBar(actionBarPx);
            conteneur.addView(barre);
        }
        FrameLayout conteneurContenu = conteneurContenu();
        conteneur.addView(conteneurContenu);
        conteneurContenu.addView(contenu);
        if (p.barreNavigation) {
            View barre = creerBarreNavigation();
            barre.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, barreNavigationPx));
            conteneur.addView(barre);
        }
        return conteneur;
    }

    // ── Assemblage SURIMPRESSION (contenu plein cadre, barres par-dessus) ──

    private View envelopperSurimpression(View contenu, float densite, Presences p,
                                         int statusBarPx, int actionBarPx,
                                         int barreNavigationPx) {
        FrameLayout decor = new FrameLayout(contexte);
        decor.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        FrameLayout conteneurContenu = conteneurContenu();
        decor.addView(conteneurContenu);
        conteneurContenu.addView(contenu);
        if (p.avecStatusBar || p.avecActionBar) {
            LinearLayout pileHaute = new LinearLayout(contexte);
            pileHaute.setOrientation(LinearLayout.VERTICAL);
            FrameLayout.LayoutParams paramsPile = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            paramsPile.gravity = Gravity.TOP;
            pileHaute.setLayoutParams(paramsPile);
            if (p.avecStatusBar) {
                StatusBar barre = creerStatusBar();
                barre.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, statusBarPx));
                pileHaute.addView(barre);
            }
            if (p.avecActionBar) {
                FrameworkActionBar barre = creerActionBar(actionBarPx);
                pileHaute.addView(barre);
            }
            decor.addView(pileHaute);
        }
        if (p.barreNavigation) {
            View barre = creerBarreNavigation();
            FrameLayout.LayoutParams paramsBarre = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, barreNavigationPx);
            paramsBarre.gravity = Gravity.BOTTOM;
            barre.setLayoutParams(paramsBarre);
            decor.addView(barre);
        }
        return decor;
    }

    // ── Pièces du décor ──────────────────────────────────────────────────

    /**
     * La status bar — fond thémé ({@code statusBarColor} &gt;
     * {@code colorPrimaryDark} &gt; jeton {@code --s-dark}), contenu maquette
     * lisible par luminance du fond (ou {@code windowLightStatusBar}
     * explicite).
     */
    private StatusBar creerStatusBar() {
        int fond = couleurStatusBar();
        StatusBar barre = new StatusBar(contexte);
        barre.setBackgroundColor(fond);
        barre.setCouleurGlyphes(
                LisibiliteChrome.couleurContenu(fond, windowLightStatusBarExplicite()));
        return barre;
    }

    /**
     * L'action bar — fond thémé ({@code colorPrimary} &gt; jeton
     * {@code --s-pri}), titre + capacités du callback (flèche retour,
     * débordement — défauts maquette).
     */
    private FrameworkActionBar creerActionBar(int hauteurPx) {
        FrameworkActionBar barre = new FrameworkActionBar(
                contexte, couleurPrimary(), config.appLabel, config.actionBarCallback);
        barre.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, hauteurPx));
        return barre;
    }

    /**
     * La barre de navigation (ou la PILULE en mode gestuel) — fond sombre
     * FIXE {@code --s-sys} (clair {@code #1C1B1F}, nuit {@code #000}).
     */
    private View creerBarreNavigation() {
        View barre = config.modeNavigation == ModeNavigation.GESTUELLE
                ? new NavigationHandle(contexte)
                : new NavigationBar(contexte);
        barre.setBackgroundColor(couleurBarreNavigation());
        return barre;
    }

    /**
     * Le conteneur du contenu — porteur du {@code windowBackground} du thème
     * (couleur résolue), repli blanc (miroir v0.133.x).
     */
    private FrameLayout conteneurContenu() {
        FrameLayout conteneur = new FrameLayout(contexte);
        ViewGroup.LayoutParams params;
        if (config.modePlacement == ModePlacementDecor.SURIMPRESSION) {
            params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        } else {
            LinearLayout.LayoutParams linParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0);
            linParams.weight = 1f;
            params = linParams;
        }
        conteneur.setLayoutParams(params);
        Integer fond = null;
        if (themeResolver != null) {
            Object valeur = themeResolver.resolveAttr("?attr/windowBackground");
            if (valeur instanceof Integer) {
                fond = (Integer) valeur;
            }
        }
        conteneur.setBackgroundColor(fond != null ? fond : Color.WHITE);
        return conteneur;
    }

    // ── Résolutions thème ────────────────────────────────────────────────

    /** La nuit du chrome = le RÉGIME DES RÉSOLVEURS (ADR 0137 D2). */
    private boolean nuit() {
        return themeResolver != null && themeResolver.isNightMode();
    }

    /**
     * Couleur de la status bar — {@code statusBarColor} &gt;
     * {@code colorPrimaryDark} &gt; jeton {@code --s-dark} de la maquette.
     */
    private int couleurStatusBar() {
        if (themeResolver != null) {
            Integer couleur = themeResolver.getColorAttr("statusBarColor");
            if (couleur != null) {
                return couleur;
            }
            couleur = themeResolver.getColorAttr("colorPrimaryDark");
            if (couleur != null) {
                return couleur;
            }
        }
        return nuit() ? COULEUR_STATUS_BAR_NUIT_DEFAUT : COULEUR_STATUS_BAR_DEFAUT;
    }

    /** Couleur de l'action bar — {@code colorPrimary} &gt; jeton {@code --s-pri}. */
    private int couleurPrimary() {
        if (themeResolver != null) {
            Integer couleur = themeResolver.getColorAttr("colorPrimary");
            if (couleur != null) {
                return couleur;
            }
        }
        return nuit() ? COULEUR_PRIMARY_NUIT_DEFAUT : COULEUR_PRIMARY_DEFAUT;
    }

    /** Le fond de la barre : {@code --s-sys} de la maquette (sombre, clair et nuit). */
    private int couleurBarreNavigation() {
        return nuit() ? COULEUR_BARRE_NAVIGATION_NUIT : COULEUR_BARRE_NAVIGATION_JOUR;
    }

    /**
     * La valeur EXPLICITE de {@code windowLightStatusBar} du thème, ou
     * {@code null} si non déclarée (la luminance décide alors — étape 5).
     */
    private Boolean windowLightStatusBarExplicite() {
        if (themeResolver == null) {
            return null;
        }
        Object valeur = themeResolver.resolveAttr("?android:attr/windowLightStatusBar");
        if (valeur instanceof String) {
            String texte = (String) valeur;
            if ("true".equalsIgnoreCase(texte)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(texte)) {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    /**
     * Lit un attribut BOOLÉEN de fenêtre du thème par
     * {@code ?android:attr/…} — correctif C1 (ADR 0135) : les attributs
     * fenêtre vivent dans le namespace android, la valeur arrive en chaîne
     * « true »/« false ».
     */
    private static boolean attrBool(ThemeResolver themeResolver, String nom, boolean defaut) {
        if (themeResolver == null) {
            return defaut;
        }
        Object valeur = themeResolver.resolveAttr("?android:attr/" + nom);
        if (valeur instanceof String) {
            return "true".equalsIgnoreCase((String) valeur);
        }
        return defaut;
    }

    /**
     * dp → px par TRONCATURE — transport exact du Kotlin historique
     * {@code (dp * densite).toInt()} (garde-fou G2 : mêmes hauteurs au pixel).
     */
    private static int hauteurPx(int dp, float densite) {
        return (int) (dp * densite);
    }
}
