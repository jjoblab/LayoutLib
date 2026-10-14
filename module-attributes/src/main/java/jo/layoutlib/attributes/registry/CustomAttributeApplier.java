package jo.layoutlib.attributes.registry;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import jo.layoutlib.attributes.format.ColorFormatter;
import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.resources.ResourceException;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Appliqueur d'attributs custom du namespace {@code app:} (res-auto),
 * piloté par type — équivalent on-device du {@code CustomAttributeDescriptor}
 * de l'AOSP layoutlib.
 *
 * <p>Couverture :</p>
 * <ul>
 *   <li><strong>attributs connus</strong> (Material Components, AndroidX,
 *       CoordinatorLayout, RecyclerView) appliqués via les setters natifs des
 *       vues <em>framework</em> correspondantes — un {@code MaterialButton}
 *       prévisualisé est un {@code Button} framework, un {@code CardView}
 *       un {@code FrameLayout} ;</li>
 *   <li><strong>attributs sans setter natif</strong> ({@code layout_behavior},
 *       {@code fabSize}, {@code strokeWidth}…) : reconnus et neutralisés
 *       silencieusement — l'attribut est <em>consommé</em> mais n'a pas
 *       d'équivalent sur la vue framework (philosophie placeholder) ;</li>
 *   <li><strong>attributs inconnus</strong> (vues custom, DataBinding) :
 *       repli par réflexion sur les setters {@code set<Name>(...)} —
 *       {@code app:heroText="x"} essaie {@code setHeroText(String/int/float/
 *       boolean/CharSequence)}, comme le fait l'AOSP pour les vues custom.</li>
 * </ul>
 *
 * <p>Jamais d'exception : une valeur invalide est journalisée et ignorée.</p>
 *
 * <p>Les attributs {@code app:layout_constraint*} ne passent PAS ici : ils
 * appartiennent au solver Cassowary (ADR 0116 — P2/L9) qui résout les
 * positions après le rendu.</p>
 *
 * @author jo@Dev
 * @since 3.0
 */
public class CustomAttributeApplier {

    /** Tag de journalisation. */
    private static final String TAG = "CustomAttrApplier";

    /** Convertisseur de dimensions (peut être null : repli brut). */
    private final DimensionConverter dimensionConverter;

    /** Formateur de couleurs partagé. */
    private final ColorFormatter colorFormatter = new ColorFormatter();

    /** Cache des setters trouvés par réflexion : classe#setter -> méthode. */
    private final Map<String, Method> setterCache = new HashMap<>();

    /** Setters introuvables (négatif mis en cache). */
    private final Set<String> setterMisses = new HashSet<>();

    /**
     * Construit un appliqueur sans convertisseur de dimensions
     * (les dimensions sont lues en valeur brute).
     */
    public CustomAttributeApplier() {
        this(null);
    }

    /**
     * Construit un appliqueur avec un convertisseur de dimensions.
     *
     * @param dimensionConverter le convertisseur ({@code null} accepté)
     */
    public CustomAttributeApplier(DimensionConverter dimensionConverter) {
        this.dimensionConverter = dimensionConverter;
    }

    /**
     * Applique un attribut sur une vue (compatibilité ancienne signature).
     *
     * @param view la vue cible
     * @param attrName le nom de l attribut (sans préfixe de namespace)
     * @param attrValue la valeur brute
     * @return true si l attribut a été appliqué (ou reconnu sans setter natif)
     */
    public boolean apply(View view, String attrName, String attrValue) {
        return apply(view, attrName, attrValue, null);
    }

    /**
     * Applique un attribut {@code app:} sur une vue.
     *
     * @param view la vue cible
     * @param attrName le nom de l attribut (sans préfixe de namespace)
     * @param attrValue la valeur brute
     * @param context le contexte (peut être {@code null} en JVM)
     * @return true si l attribut a été appliqué (ou reconnu sans setter natif),
     *         false s il est inconnu et sans setter correspondant
     */
    public boolean apply(View view, String attrName, String attrValue,
                         Context context) {
        if (view == null || attrName == null || attrName.isEmpty()
                || attrValue == null || attrValue.isEmpty()) {
            return false;
        }
        switch (attrName) {
            // -------------------------------------------------------------
            // Vue générique (Material + AndroidX posent ces app:*)
            // -------------------------------------------------------------
            case "elevation":
                view.setElevation(parseDim(attrValue));
                return true;
            case "alpha":
                view.setAlpha(parseFloat(attrValue));
                return true;
            case "backgroundTint":
                applyBackgroundTint(view, attrValue);
                return true;
            case "visibility":
                if ("gone".equals(attrValue)) view.setVisibility(View.GONE);
                else if ("invisible".equals(attrValue)) view.setVisibility(View.INVISIBLE);
                else view.setVisibility(View.VISIBLE);
                return true;
            case "enabled":
                view.setEnabled(parseBool(attrValue));
                return true;
            case "clickable":
                view.setClickable(parseBool(attrValue));
                return true;
            case "focusable":
                view.setFocusable(parseBool(attrValue));
                return true;
            case "selected":
                view.setSelected(parseBool(attrValue));
                return true;
            case "tag":
                view.setTag(attrValue);
                return true;

            // -------------------------------------------------------------
            // CardView / MaterialCardView (framework : FrameLayout)
            // -------------------------------------------------------------
            case "cardCornerRadius":
                if (view instanceof FrameLayout
                        && view.getBackground() instanceof GradientDrawable) {
                    ((GradientDrawable) view.getBackground())
                            .setCornerRadius(parseDim(attrValue));
                }
                return true;
            case "cardElevation":
            case "cardMaxElevation":
                view.setElevation(parseDim(attrValue));
                return true;
            case "cardBackgroundColor":
                Integer cardColor = parseColor(attrValue);
                if (cardColor != null) {
                    view.setBackground(new ColorDrawable(cardColor));
                }
                return true;
            case "contentPadding":
                int p = parseDim(attrValue);
                view.setPadding(p, p, p, p);
                return true;
            case "contentPaddingLeft":
                view.setPadding(parseDim(attrValue), view.getPaddingTop(),
                        view.getPaddingRight(), view.getPaddingBottom());
                return true;
            case "contentPaddingTop":
                view.setPadding(view.getPaddingLeft(), parseDim(attrValue),
                        view.getPaddingRight(), view.getPaddingBottom());
                return true;
            case "contentPaddingRight":
                view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                        parseDim(attrValue), view.getPaddingBottom());
                return true;
            case "contentPaddingBottom":
                view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                        view.getPaddingRight(), parseDim(attrValue));
                return true;
            case "cardPreventCornerOverlap":
            case "cardUseCompatPadding":
                // Reconnu : pas d équivalent sur FrameLayout
                return true;

            // -------------------------------------------------------------
            // MaterialButton (framework : Button)
            // -------------------------------------------------------------
            case "cornerRadius":
                if (view instanceof Button
                        && view.getBackground() instanceof GradientDrawable) {
                    ((GradientDrawable) view.getBackground())
                            .setCornerRadius(parseDim(attrValue));
                }
                return true;
            case "strokeColor":
            case "strokeWidth":
            case "rippleColor":
            case "iconGravity":
            case "iconPadding":
            case "iconSize":
            case "iconTint":
            case "iconTintMode":
            case "insetTop":
            case "insetBottom":
                // Reconnu : rendu Material exact hors périmètre de la vue
                // framework Button
                return true;

            // -------------------------------------------------------------
            // FloatingActionButton (framework : ImageView)
            // -------------------------------------------------------------
            case "tint":
                if (view instanceof ImageView) {
                    Integer tintColor = parseColor(attrValue);
                    if (tintColor != null) {
                        ((ImageView) view)
                                .setImageTintList(ColorStateList.valueOf(tintColor));
                    }
                }
                return true;
            case "fabSize":
            case "fabCustomSize":
            case "maxImageSize":
            case "useCompatPadding":
            case "borderWidth":
                // Reconnu : pas de setter natif sur ImageView
                return true;

            // -------------------------------------------------------------
            // CoordinatorLayout / AppBarLayout (comportements de défilement)
            // -------------------------------------------------------------
            case "layout_behavior":
            case "layout_anchor":
            case "layout_anchorGravity":
            case "layout_keyline":
            case "layout_insetEdge":
            case "layout_dodgeInsetEdges":
            case "layout_collapseMode":
            case "layout_collapseParallaxMultiplier":
            case "layout_scrollFlags":
            case "layout_scrollInterpolator":
            case "lifted":
            case "liftOnScroll":
                // Reconnu : appliqué par la bibliothèque hôte à l exécution
                return true;

            // -------------------------------------------------------------
            // RecyclerView (attributs de LayoutManager)
            // -------------------------------------------------------------
            case "layoutManager":
            case "spanCount":
            case "reverseLayout":
            case "stackFromEnd":
                // Reconnu : le LayoutManager est hors périmètre preview
                return true;

            default:
                break;
        }

        // Attribut inconnu : repli réflexion sur les setters de la vue
        // (vues custom, DataBinding — app:heroText -> setHeroText(...))
        return applyByReflection(view, attrName, attrValue);
    }

    /**
     * Applique une teinte d'arrière-plan ({@code app:backgroundTint}).
     *
     * @param view la vue cible
     * @param value la valeur couleur (hex ou nom)
     */
    private void applyBackgroundTint(View view, String value) {
        Integer color = parseColor(value);
        if (color != null) {
            view.setBackgroundTintList(ColorStateList.valueOf(color));
        }
    }

    /**
     * Repli réflexion : cherche un setter {@code set<Name>} à un paramètre
     * dont le type accepte la valeur (String, CharSequence, int, float,
     * boolean, long, double).
     *
     * <p>Le résultat (positif ou négatif) est mis en cache par classe et nom
     * de setter. Aucune exception ne sort de cette méthode.</p>
     *
     * @param view la vue cible
     * @param attrName le nom de l attribut
     * @param attrValue la valeur brute
     * @return true si un setter a été invoqué
     */
    private boolean applyByReflection(View view, String attrName,
                                      String attrValue) {
        Class<?> cls = view.getClass();
        String setterName = "set"
                + Character.toUpperCase(attrName.charAt(0))
                + attrName.substring(1);
        String cacheKey = cls.getName() + "#" + setterName;

        Method cached = setterCache.get(cacheKey);
        if (cached != null) {
            invokeSetter(cached, view, attrValue);
            return true;
        }
        if (setterMisses.contains(cacheKey)) {
            return false;
        }

        for (Method method : cls.getMethods()) {
            if (!method.getName().equals(setterName)
                    || method.getParameterCount() != 1) {
                continue;
            }
            Object converted = convertFor(method.getParameterTypes()[0], attrValue);
            if (converted == null) {
                continue;
            }
            // Vue package-private (cas réel : classe interne d'app) : le
            // setter public doit être rendu invocable avant réflexion
            if (!ensureAccessible(method)) {
                continue;
            }
            setterCache.put(cacheKey, method);
            invokeSetter(method, view, attrValue);
            return true;
        }

        setterMisses.add(cacheKey);
        return false;
    }

    /**
     * Rend un setter invocable par réflexion (vues package-private).
     *
     * @param method le setter candidat
     * @return true si invocable (public ou setAccessible réussi)
     */
    private boolean ensureAccessible(Method method) {
        try {
            method.setAccessible(true);
            return true;
        } catch (RuntimeException e) {
            // SecurityException (JPMS/desktop) et autres : setter ignoré
            Log.d(TAG, "Setter " + method.getName()
                    + " non accessible, ignoré : " + e.getMessage());
            return false;
        }
    }

    /**
     * Invoque un setter déjà validé (valeur reconvertie à l invocation pour
     * rester sans état).
     */
    private void invokeSetter(Method method, View view, String attrValue) {
        try {
            Object converted =
                    convertFor(method.getParameterTypes()[0], attrValue);
            if (converted != null) {
                method.invoke(view, converted);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            Log.d(TAG, "Setter " + method.getName() + " non invocable, ignoré : "
                    + e.getMessage());
        }
    }

    /**
     * Convertit une valeur XML brute vers le type de paramètre du setter.
     *
     * @param paramType le type du paramètre
     * @param value la valeur brute
     * @return la valeur convertie, ou {@code null} si non convertible
     */
    private Object convertFor(Class<?> paramType, String value) {
        if (paramType == String.class || paramType == CharSequence.class) {
            return value;
        }
        if (paramType == int.class || paramType == Integer.class) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                Integer color = parseColor(value);
                return color != null ? color : null;
            }
        }
        if (paramType == float.class || paramType == Float.class) {
            return parseFloat(value);
        }
        if (paramType == boolean.class || paramType == Boolean.class) {
            if ("true".equalsIgnoreCase(value.trim())
                    || "false".equalsIgnoreCase(value.trim())) {
                return Boolean.parseBoolean(value.trim());
            }
            return null;
        }
        if (paramType == long.class || paramType == Long.class) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (paramType == double.class || paramType == Double.class) {
            try {
                return Double.parseDouble(value.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Parse une dimension en pixels via le convertisseur, avec repli brut
     * (valeur numérique dépouillée de son unité).
     *
     * @param value la valeur (ex. {@code "16dp"})
     * @return la dimension en pixels entiers (0 si invalide)
     */
    private int parseDim(String value) {
        if (dimensionConverter != null) {
            try {
                return Math.round(dimensionConverter.toPixels(value));
            } catch (RuntimeException e) {
                // ResourceException et valeurs invalides : repli brut ci-dessous
            }
        }
        try {
            String cleaned = value.replaceAll("[^0-9.\\-]", "").trim();
            return cleaned.isEmpty() ? 0 : Math.round(Float.parseFloat(cleaned));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Parse un flottant (ex. {@code "0.5"}), 0 si invalide.
     */
    private float parseFloat(String value) {
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    /**
     * Parse un booléen (« true »/« false », tolérant à la casse).
     */
    private boolean parseBool(String value) {
        return "true".equalsIgnoreCase(value.trim());
    }

    /**
     * Parse une couleur via le formateur du module.
     *
     * @param value la valeur (hex ou nom)
     * @return la couleur ARGB, ou {@code null} si invalide
     */
    private Integer parseColor(String value) {
        try {
            return colorFormatter.format(value);
        } catch (RuntimeException e) {
            Log.w(TAG, "Couleur '" + value + "' invalide, ignorée : "
                    + e.getMessage());
            return null;
        }
    }
}
