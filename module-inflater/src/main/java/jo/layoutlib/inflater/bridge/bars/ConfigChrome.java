package jo.layoutlib.inflater.bridge.bars;

import jo.layoutlib.resources.api.ActionBarCallback;

/**
 * La CONFIGURATION de session du chrome système (PE13, ADR 0137 D1/D2/D6) —
 * TOUT ce que l'hôte dit au moteur, et RIEN d'autre.
 *
 * <p>Miroir du partage de responsabilité du moteur de référence : l'IDE ne
 * dessine pas un pixel de chrome, il ne décrit pas non plus la COMPOSITION
 * (présences des barres, couleurs, hauteurs) — ça, le moteur le DÉRIVE du
 * thème et de la config. L'hôte passe : le drapeau {@code decor} (l'équivalent
 * positif du {@code SessionParams.isForceNoDecor()} de la référence), le
 * TITRE de l'action bar ({@code getAppLabel()} — résolu par l'hôte,
 * littéral), les SURCHARGES utilisateur des barres (le menu Calques de
 * CodeIDE ; la référence n'offre pas ce levier — extension assumée) et les
 * capacités de l'étape 5 (mode de placement, mode de navigation, callback
 * d'action bar) aux DÉFAUTS MAQUETTE.</p>
 *
 * <p><strong>Pas de drapeau NUIT</strong> (ADR 0137 D2) : la nuit est le
 * régime des RÉSOLVEURS ({@code ThemeResolver.isNightMode()}), le chrome la
 * relit — jamais deux sources de vérité. Remplace le stub {@code bars/Config}
 * (drapeaux morts, code sans usager).</p>
 *
 * @author jo@Dev
 * @since 3.5.0
 */
public class ConfigChrome {

    /** Le décor est-il posé autour du contenu ? Sans décor, contenu rendu tel quel. */
    public final boolean decor;

    /** Le titre de l'action bar (label d'activité RÉSOLU par l'hôte), ou {@code null}. */
    public final String appLabel;

    /**
     * SURCHARGE utilisateur de la status bar ({@code null} = dérivation du
     * thème : {@code windowFullscreen} retire la barre).
     */
    public final Boolean surchargeStatusBar;

    /**
     * SURCHARGE utilisateur de la barre de navigation ({@code null} = défaut
     * ON, miroir {@code S.nav} de la maquette — l'interrupteur « Barre de
     * navigation » du menu Calques).
     */
    public final Boolean surchargeNavigationBar;

    /** Le mode de placement du contenu (défaut {@link ModePlacementDecor#PADDE_PAR_DEFAUT}). */
    public final ModePlacementDecor modePlacement;

    /** Le mode de contenu de la barre de navigation (défaut {@link ModeNavigation#TROIS_BOUTONS}). */
    public final ModeNavigation modeNavigation;

    /** La configuration de l'action bar (défaut : action bar maquette, titre seul). */
    public final ActionBarCallback actionBarCallback;

    private ConfigChrome(Builder builder) {
        this.decor = builder.decor;
        this.appLabel = builder.appLabel;
        this.surchargeStatusBar = builder.surchargeStatusBar;
        this.surchargeNavigationBar = builder.surchargeNavigationBar;
        this.modePlacement = builder.modePlacement;
        this.modeNavigation = builder.modeNavigation;
        this.actionBarCallback = builder.actionBarCallback;
    }

    /**
     * Le constructeur fluent de {@link ConfigChrome} (miroir du
     * {@code Layout.Builder} AOSP — la config de session se DÉCIDE côté
     * hôte, champ par champ).
     *
     * @author jo@Dev
     * @since 3.5.0
     */
    public static class Builder {

        private boolean decor = true;
        private String appLabel;
        private Boolean surchargeStatusBar;
        private Boolean surchargeNavigationBar;
        private ModePlacementDecor modePlacement = ModePlacementDecor.PADDE_PAR_DEFAUT;
        private ModeNavigation modeNavigation = ModeNavigation.TROIS_BOUTONS;
        private ActionBarCallback actionBarCallback = ActionBarCallback.parDefaut();

        /** Le décor est-il posé ? (défaut {@code true}). */
        public Builder decor(boolean decor) {
            this.decor = decor;
            return this;
        }

        /** Le titre de l'action bar (label résolu), ou {@code null}. */
        public Builder appLabel(String appLabel) {
            this.appLabel = appLabel;
            return this;
        }

        /** La surcharge de la status bar, ou {@code null} pour dériver du thème. */
        public Builder surchargeStatusBar(Boolean surchargeStatusBar) {
            this.surchargeStatusBar = surchargeStatusBar;
            return this;
        }

        /** La surcharge de la barre de navigation, ou {@code null} pour le défaut ON. */
        public Builder surchargeNavigationBar(Boolean surchargeNavigationBar) {
            this.surchargeNavigationBar = surchargeNavigationBar;
            return this;
        }

        /** Le mode de placement du contenu. */
        public Builder modePlacement(ModePlacementDecor modePlacement) {
            this.modePlacement = modePlacement == null
                    ? ModePlacementDecor.PADDE_PAR_DEFAUT : modePlacement;
            return this;
        }

        /** Le mode de contenu de la barre de navigation. */
        public Builder modeNavigation(ModeNavigation modeNavigation) {
            this.modeNavigation = modeNavigation == null
                    ? ModeNavigation.TROIS_BOUTONS : modeNavigation;
            return this;
        }

        /** La configuration de l'action bar (défaut : titre seul). */
        public Builder actionBarCallback(ActionBarCallback actionBarCallback) {
            this.actionBarCallback = actionBarCallback == null
                    ? ActionBarCallback.parDefaut() : actionBarCallback;
            return this;
        }

        /**
         * @return la config IMMUTABLE
         */
        public ConfigChrome build() {
            return new ConfigChrome(this);
        }
    }
}
