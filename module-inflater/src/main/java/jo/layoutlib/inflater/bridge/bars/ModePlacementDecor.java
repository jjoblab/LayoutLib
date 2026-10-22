package jo.layoutlib.inflater.bridge.bars;

/**
 * Le mode de PLACEMENT du contenu dans le décor (PE13, ADR 0137 D3).
 *
 * <p>Le décor appartient au moteur ; il pose le contenu selon ce mode —
 * l'équivalent déterministe du régime translucide de la référence
 * (android-32 : {@code windowTranslucentStatus}/{@code windowTranslucentNavigation}
 * retirent la règle de placement, le contenu passe SOUS la barre) et du
 * régime edge-to-edge impératif d'Android 15+
 * ({@code setDecorFitsSystemWindows(false)}) qui se branchera sur ce même
 * exposant.</p>
 *
 * @author jo@Dev
 * @since 3.5.0
 */
public enum ModePlacementDecor {

    /**
     * Le content root est posé SOUS la status bar / l'action bar et
     * AU-DESSUS de la barre de navigation (règles {@code BELOW}/{@code ABOVE}
     * de la référence) — comportement nominal, pixel-identique au chrome IDE
     * historique (garde-fou G2).
     */
    PADDE_PAR_DEFAUT,

    /**
     * Le contenu est mesuré PLEIN CADRE, les barres se dessinent
     * PAR-DESSUS (surimpression) et les insets restent EXPOSÉS dans le
     * résultat de collecte — une future surcouche « cutout » ou un document
     * qui se padde lui-même les consommera.
     */
    SURIMPRESSION
}
