package jo.layoutlib.inflater.bridge.impl;

import jo.layoutlib.inflater.ViewInfoCollector;
import jo.layoutlib.resources.api.ViewType;

/**
 * Les infos d'un nœud SYSTÈME du décor de rendu, miroir du
 * {@code SystemViewInfo} de l'AOSP (PE13, ADR 0137 D5) : une info de vue
 * PORTANT son {@link ViewType} — le marquage qui permet à l'IDE d'exclure
 * le chrome de l'arbre des composants et, plus tard, d'y réintroduire une
 * section « système » interprétable.
 *
 * <p>Ancienne classe (v1.0, focus/visibilité — SÉMANTIQUE DIFFÉRENTE de la
 * référence) : code mort vérifié, réécrite. L'ancien contrat d'« infos
 * système sur une vue » est obsolète : la visibilité vit déjà dans
 * {@link ViewInfoCollector.ViewInfo}, le marquage du décor est le vrai rôle
 * de ce nom chez la référence.</p>
 *
 * @author jo@Dev
 * @since 3.5.0
 */
public class SystemViewInfo extends ViewInfoCollector.ViewInfo {

    /** Le marquage du nœud système (status bar, barre de navigation…). */
    private final ViewType viewType;

    /**
     * Construit une info système par COPIE d'une info collectée, portant
     * son marquage.
     *
     * @param source   l'info collectée (bounds dans l'espace du décor)
     * @param viewType le marquage du nœud
     */
    public SystemViewInfo(ViewInfoCollector.ViewInfo source, ViewType viewType) {
        super(source.className, source.simpleName, source.id, source.idName,
                source.left, source.top, source.right, source.bottom,
                source.width, source.height,
                source.paddingLeft, source.paddingTop, source.paddingRight, source.paddingBottom,
                source.marginLeft, source.marginTop, source.marginRight, source.marginBottom,
                source.visibility, source.depth, source.isViewGroup, source.children);
        this.viewType = viewType == null ? ViewType.UNKNOWN : viewType;
    }

    /**
     * @return le marquage du nœud système
     */
    public ViewType getViewType() {
        return viewType;
    }

    @Override
    public String toString() {
        return super.toString() + " type=" + viewType;
    }
}
