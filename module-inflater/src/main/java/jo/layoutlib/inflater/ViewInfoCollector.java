package jo.layoutlib.inflater;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import jo.layoutlib.inflater.bridge.bars.FrameworkActionBar;
import jo.layoutlib.inflater.bridge.bars.InsetsDecor;
import jo.layoutlib.inflater.bridge.bars.ModePlacementDecor;
import jo.layoutlib.inflater.bridge.bars.NavigationHandle;
import jo.layoutlib.inflater.bridge.bars.NavigationBar;
import jo.layoutlib.inflater.bridge.bars.StatusBar;
import jo.layoutlib.inflater.bridge.impl.SystemViewInfo;
import jo.layoutlib.resources.api.ViewType;

/**
 * Collecte les informations de hiérarchie après rendu, inspiré de
 * {@code com.android.ide.common.rendering.api.ViewInfo} de l'AOSP.
 *
 * <p>Après qu'un layout a été mesuré et positionné, cette classe parcourt
 * l'arbre de vues et collecte pour chaque vue :</p>
 * <ul>
 *   <li>Position (left, top, right, bottom)</li>
 *   <li>Dimensions (width, height)</li>
 *   <li>Classe de la vue</li>
 *   <li>Id (si défini)</li>
 *   <li>Visibilité</li>
 *   <li>Padding et marges</li>
 *   <li>Profondeur dans la hiérarchie</li>
 *   <li>Enfants (récursif)</li>
 * </ul>
 *
 * <p>Ces informations sont utilisées par le mode design/blueprint/overlays
 * pour dessiner les overlays (marges, padding, sélection, wireframe).</p>
 *
 * <p>PE13 (ADR 0137 D5) — la COLLECTE DUALE : quand le moteur possède le
 * décor (chrome système), {@link #collectDecor} produit DEUX arbres d'un
 * coup, miroir du {@code visitAllChildren} de la référence — le CONTENU de
 * l'utilisateur, bounds TRANSLATÉES dans l'espace du décor (= bitmap), et
 * le DÉCOR marqué {@link ViewType} via {@link SystemViewInfo}. L'IDE n'a
 * AUCUN recalage à faire : la séparation contenu/système et la translation
 * bitmap sont des responsabilités du moteur.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ViewInfoCollector {

    /** Tag de journalisation. */
    private static final String TAG = "ViewInfoCollector";

    /**
     * Information sur une vue dans la hiérarchie rendue.
     *
     * @author jo@Dev
     * @since 1.0
     */
    public static class ViewInfo {

        public final String className;
        public final String simpleName;
        public final int id;
        public final String idName;
        public final int left, top, right, bottom;
        public final int width, height;
        public final int paddingLeft, paddingTop, paddingRight, paddingBottom;
        public final int marginLeft, marginTop, marginRight, marginBottom;
        public final int visibility;
        public final int depth;
        public final boolean isViewGroup;
        public final List<ViewInfo> children;

        public ViewInfo(String className, String simpleName, int id, String idName,
                        int left, int top, int right, int bottom,
                        int width, int height,
                        int paddingLeft, int paddingTop, int paddingRight, int paddingBottom,
                        int marginLeft, int marginTop, int marginRight, int marginBottom,
                        int visibility, int depth, boolean isViewGroup,
                        List<ViewInfo> children) {
            this.className = className;
            this.simpleName = simpleName;
            this.id = id;
            this.idName = idName;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.width = width;
            this.height = height;
            this.paddingLeft = paddingLeft;
            this.paddingTop = paddingTop;
            this.paddingRight = paddingRight;
            this.paddingBottom = paddingBottom;
            this.marginLeft = marginLeft;
            this.marginTop = marginTop;
            this.marginRight = marginRight;
            this.marginBottom = marginBottom;
            this.visibility = visibility;
            this.depth = depth;
            this.isViewGroup = isViewGroup;
            this.children = children;
        }

        public boolean isVisible() {
            return visibility == View.VISIBLE;
        }

        public boolean hasId() {
            return id != View.NO_ID;
        }

        @Override
        public String toString() {
            return simpleName + (idName != null ? " @" + idName : "")
                    + " [" + left + "," + top + "," + right + "," + bottom + "]"
                    + " " + width + "x" + height;
        }
    }

    /**
     * Le résultat de la COLLECTE DUALE d'un rendu décoré (PE13, ADR 0137
     * D5) — miroir du couple {@code getViewInfos()}/{@code
     * getSystemViewInfos()} de la référence, enrichi des insets.
     *
     * @author jo@Dev
     * @since 3.5.0
     */
    public static class CollecteDecor {

        /** Le CONTENU de l'utilisateur — bounds ABSOLUES bitmap (décor translaté). */
        public final ViewInfo contenu;

        /** Le DÉCOR marqué (null : aucun chrome posé), miroir des infos système AOSP. */
        public final SystemViewInfo systeme;

        /** Les insets du décor (px) — vérité à consommer en mode SURIMPRESSION. */
        public final InsetsDecor insets;

        /** Le mode de placement du contenu dans le décor. */
        public final ModePlacementDecor modePlacement;

        private CollecteDecor(ViewInfo contenu, SystemViewInfo systeme,
                               InsetsDecor insets, ModePlacementDecor modePlacement) {
            this.contenu = contenu;
            this.systeme = systeme;
            this.insets = insets;
            this.modePlacement = modePlacement;
        }
    }

    /**
     * Collecte la hiérarchie complète d'une vue racine.
     *
     * <p>Les positions renvoyées ({@code left/top/right/bottom}) sont
     * <strong>absolues</strong>, c'est-à-dire relatives à la racine du
     * snapshot — pas au parent direct. C'est ce que consomme l'OverlayView
     * et ce que renvoie le {@code ViewInfo} officiel de l'AOSP. Sans cette
     * convention, les petits-enfants seraient dessinés à l'offset du parent,
     * et la sélection / hit-testing / guides / handles seraient décalés.</p>
     *
     * @param rootView la vue racine (déjà mesurée et positionnée)
     * @return les ViewInfo de la racine
     */
    public static ViewInfo collect(View rootView) {
        return collectInternal(rootView, 0, null, 0, 0);
    }

    /**
     * La COLLECTE DUALE d'un rendu décoré (PE13, ADR 0137 D5).
     *
     * <p>Miroir du {@code visitAllChildren} de la référence : au passage du
     * content root, le collecteur produit DEUX arbres d'un coup — les
     * infos CONTENU avec l'offset du décor appliqué (bounds absolues
     * bitmap), et les infos SYSTÈME du décor marquées {@link ViewType}.
     * Sans décor ({@code decor == racineContenu}), le contenu est collecté
     * tel quel à l'offset nul et l'arbre système est vide — le comportement
     * historique de {@link #collect} est un cas particulier.</p>
     *
     * @param decor         la racine du décor (ou le contenu lui-même sans chrome)
     * @param racineContenu la racine du layout UTILISATEUR dans le décor
     * @param insets        les insets du décor (calculés par le chrome)
     * @param modePlacement le mode de placement du contenu
     * @return la collecte duale (contenu bitmap + décor marqué)
     */
    public static CollecteDecor collectDecor(View decor, View racineContenu,
                                             InsetsDecor insets, ModePlacementDecor modePlacement) {
        InsetsDecor insetsSur = insets == null ? InsetsDecor.aucun() : insets;
        ModePlacementDecor modeSur =
                modePlacement == null ? ModePlacementDecor.PADDE_PAR_DEFAUT : modePlacement;
        if (decor == racineContenu || racineContenu == null) {
            return new CollecteDecor(collectInternal(decor, 0, null, 0, 0),
                    null, insetsSur, modeSur);
        }
        int[] offset = offsetDansDecor(racineContenu);
        ViewInfo contenu = collectInternal(racineContenu, 0, null, offset[0], offset[1]);
        SystemViewInfo systeme = collectSystemNode(decor, racineContenu, 0, null, 0, 0);
        return new CollecteDecor(contenu, systeme, insetsSur, modeSur);
    }

    /**
     * Collecte récursive.
     *
     * @param view     la vue courante
     * @param depth    la profondeur
     * @param parent   le parent (pour lire les marges)
     * @param offsetX  offset X cumulé des ancêtres (pour calculer la position absolue)
     * @param offsetY  offset Y cumulé des ancêtres
     * @return les ViewInfo
     */
    private static ViewInfo collectInternal(View view, int depth, ViewGroup parent,
                                            int offsetX, int offsetY) {
        if (view == null) return null;

        int absLeft   = view.getLeft()   + offsetX;
        int absTop    = view.getTop()    + offsetY;

        // Enfants — on propage l'offset cumulé (absLeft/absTop du parent)
        List<ViewInfo> children = new ArrayList<>();
        boolean isViewGroup = view instanceof ViewGroup;
        if (isViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                ViewInfo child = collectInternal(group.getChildAt(i), depth + 1, group,
                        absLeft, absTop);
                if (child != null) {
                    children.add(child);
                }
            }
        }

        return snapshot(view, depth, parent, offsetX, offsetY, children, isViewGroup);
    }

    /**
     * Le SNAPSHOT d'UN nœud (sans récursion) — bounds absolues au
     * {@code (offsetX, offsetY)} cumulé près, marges lues du parent.
     */
    private static ViewInfo snapshot(View view, int depth, ViewGroup parent,
                                     int offsetX, int offsetY,
                                     List<ViewInfo> children, boolean isViewGroup) {
        String className = view.getClass().getName();
        String simpleName = view.getClass().getSimpleName();
        int id = view.getId();
        String idName = null;
        if (id != View.NO_ID) {
            try {
                idName = view.getContext().getResources().getResourceEntryName(id);
            } catch (RuntimeException e) {
                // Voulu : id de vue non déclaré dans les resources (généré) —
                // pas de nom, la vue reste identifiée par son id numérique
                Log.d(TAG, "getResourceEntryName(" + id + ") indisponible : "
                        + e.getClass().getSimpleName());
            }
        }

        int absLeft   = view.getLeft()   + offsetX;
        int absTop    = view.getTop()    + offsetY;
        int absRight  = view.getRight()  + offsetX;
        int absBottom = view.getBottom() + offsetY;

        // Marges (depuis les LayoutParams du parent)
        int marginLeft = 0, marginTop = 0, marginRight = 0, marginBottom = 0;
        if (parent != null && view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            marginLeft = mlp.leftMargin;
            marginTop = mlp.topMargin;
            marginRight = mlp.rightMargin;
            marginBottom = mlp.bottomMargin;
        }

        return new ViewInfo(
                className, simpleName, id, idName,
                absLeft, absTop, absRight, absBottom,
                view.getWidth(), view.getHeight(),
                view.getPaddingLeft(), view.getPaddingTop(),
                view.getPaddingRight(), view.getPaddingBottom(),
                marginLeft, marginTop, marginRight, marginBottom,
                view.getVisibility(), depth, isViewGroup, children);
    }

    /**
     * Un nœud du DÉCOR (collecte système) — récursion qui SAUTE le contenu
     * utilisateur : le content root reste une FEUILLE de l'arbre système
     * (miroir AOSP : le système voit le content root, jamais son contenu).
     */
    private static SystemViewInfo collectSystemNode(View view, View racineContenu,
                                                    int depth, ViewGroup parent,
                                                    int offsetX, int offsetY) {
        if (view == null || view == racineContenu) return null;
        int absLeft = view.getLeft() + offsetX;
        int absTop = view.getTop() + offsetY;
        List<ViewInfo> children = new ArrayList<>();
        boolean isViewGroup = view instanceof ViewGroup;
        if (isViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View enfant = group.getChildAt(i);
                if (enfant == racineContenu) {
                    continue;
                }
                SystemViewInfo enfantInfo =
                        collectSystemNode(enfant, racineContenu, depth + 1, group,
                                absLeft, absTop);
                if (enfantInfo != null) {
                    children.add(enfantInfo);
                }
            }
        }
        ViewInfo info = snapshot(view, depth, parent, offsetX, offsetY, children, isViewGroup);
        return new SystemViewInfo(info, viewTypeDe(view));
    }

    /**
     * Le MARQUAGE {@link ViewType} d'un nœud du décor — les vues du chrome
     * portent leur type, les conteneurs du décor sont APPLICATION, le
     * reste (défensif) est UNKNOWN.
     */
    private static ViewType viewTypeDe(View view) {
        if (view instanceof StatusBar) return ViewType.STATUS_BAR;
        if (view instanceof NavigationBar) return ViewType.NAVIGATION_BAR;
        if (view instanceof NavigationHandle) return ViewType.NAVIGATION_BAR;
        if (view instanceof FrameworkActionBar) return ViewType.ACTION_BAR;
        if (view instanceof LinearLayout || view instanceof FrameLayout) {
            return ViewType.APPLICATION;
        }
        return ViewType.UNKNOWN;
    }

    /**
     * L'OFFSET de [racineContenu] dans l'arbre RENDU (espace bitmap) — la
     * remontée de la chaîne des parents cumule les positions (left/top) en
     * défaquant le scroll (scrollX/Y), miroir exact de l'ancien recalage IDE
     * {@code decalageContenuDansArbreRendu} (v0.134.2), devenu propriété DU
     * MOTEUR (ADR 0137 D5).
     */
    private static int[] offsetDansDecor(View racineContenu) {
        int gauche = 0;
        int haut = 0;
        android.view.ViewParent parent = racineContenu.getParent();
        while (parent instanceof View) {
            View vue = (View) parent;
            gauche += vue.getLeft() - vue.getScrollX();
            haut += vue.getTop() - vue.getScrollY();
            parent = vue.getParent();
        }
        return new int[]{gauche, haut};
    }

    /**
     * Compte le nombre total de vues dans la hiérarchie.
     *
     * @param info les ViewInfo racine
     * @return le nombre total
     */
    public static int countViews(ViewInfo info) {
        if (info == null) return 0;
        int count = 1;
        for (ViewInfo child : info.children) {
            count += countViews(child);
        }
        return count;
    }

    /**
     * Trouve une vue par id dans la hiérarchie.
     *
     * @param info les ViewInfo racine
     * @param id   l'id à chercher
     * @return les ViewInfo trouvés, ou null
     */
    public static ViewInfo findById(ViewInfo info, int id) {
        if (info == null) return null;
        if (info.id == id) return info;
        for (ViewInfo child : info.children) {
            ViewInfo found = findById(child, id);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * Trouve toutes les vues à une position (x, y).
     *
     * @param info les ViewInfo racine
     * @param x    coordonnée X
     * @param y    coordonnée Y
     * @return la liste des vues à cette position (du plus profond au moins profond)
     */
    public static List<ViewInfo> findAtPoint(ViewInfo info, int x, int y) {
        List<ViewInfo> results = new ArrayList<>();
        findAtPointInternal(info, x, y, results);
        return results;
    }

    private static void findAtPointInternal(ViewInfo info, int x, int y, List<ViewInfo> results) {
        if (info == null || !info.isVisible()) return;
        if (x >= info.left && x <= info.right && y >= info.top && y <= info.bottom) {
            // Chercher d'abord dans les enfants (plus profond = prioritaire)
            for (ViewInfo child : info.children) {
                findAtPointInternal(child, x, y, results);
            }
            results.add(info);
        }
    }
}
