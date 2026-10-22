package jo.layoutlib.inflater.bridge.bars;

/**
 * Les insets EXPLICITES du décor, en pixels (PE13, ADR 0137 D3).
 *
 * <p>Le rendu jjoblab est DÉTACHÉ (mesure {@code LayoutEngineImpl}, dessin
 * bitmap — pas de fenêtre réelle) : la machinerie {@code ViewRootImpl} de la
 * référence vivrait sur des invariants absents. Le moteur, PROPRIÉTAIRE des
 * barres, CALCULE donc les insets depuis les hauteurs de ses propres barres
 * et les DISPATCHE au résultat de collecte : en
 * {@link ModePlacementDecor#PADDE_PAR_DEFAUT} le contenu est déjà posé sous
 * les barres (le padding est dans le layout du décor) ; en
 * {@link ModePlacementDecor#SURIMPRESSION} le contenu est mesuré plein
 * cadre et ces insets restent la vérité à consommer en aval. C'est une
 * simplification ASSUMÉE de la référence — le résultat visible est identique
 * et reproductible en test.</p>
 *
 * @author jo@Dev
 * @since 3.5.0
 */
public final class InsetsDecor {

    /** L'inset du HAUT du décor en px (status bar + action bar posées). */
    public final int haut;

    /** L'inset du BAS du décor en px (barre de navigation posée). */
    public final int bas;

    /** L'inset de GAUCHE en px (toujours 0 — barres horizontales). */
    public final int gauche;

    /** L'inset de DROITE en px (toujours 0 — barres horizontales). */
    public final int droite;

    /**
     * Construit les insets du décor.
     *
     * @param haut    l'inset du haut en px
     * @param bas     l'inset du bas en px
     * @param gauche  l'inset de gauche en px
     * @param droite  l'inset de droite en px
     */
    public InsetsDecor(int haut, int bas, int gauche, int droite) {
        this.haut = haut;
        this.bas = bas;
        this.gauche = gauche;
        this.droite = droite;
    }

    /**
     * @return les insets NULS (aucune barre posée)
     */
    public static InsetsDecor aucun() {
        return new InsetsDecor(0, 0, 0, 0);
    }

    @Override
    public String toString() {
        return "InsetsDecor[haut=" + haut + ", bas=" + bas
                + ", gauche=" + gauche + ", droite=" + droite + "]";
    }
}
