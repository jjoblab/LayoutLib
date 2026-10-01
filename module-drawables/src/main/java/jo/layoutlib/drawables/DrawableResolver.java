package jo.layoutlib.drawables;

import android.content.Context;
import android.graphics.drawable.Drawable;

/**
 * Résolveur de drawables Android.
 *
 * <p>Cette interface est le contrat que doit implémenter le Module 3
 * (Drawables). Elle transforme une référence {@code @drawable/foo} ou un XML
 * de drawable en un objet {@link Drawable} Android.</p>
 *
 * <h2>Types supportés</h2>
 * <ul>
 *   <li>{@code <shape>} → {@link android.graphics.drawable.GradientDrawable}</li>
 *   <li>{@code <selector>} → {@link android.graphics.drawable.StateListDrawable}</li>
 *   <li>{@code <vector>} → {@link android.graphics.drawable.VectorDrawable}</li>
 *   <li>{@code <layer-list>} → {@link android.graphics.drawable.LayerDrawable}</li>
 *   <li>{@code <ripple>} → {@link android.graphics.drawable.RippleDrawable}</li>
 *   <li>{@code <inset>} → {@link android.graphics.drawable.InsetDrawable}</li>
 *   <li>{@code <bitmap>} → {@link android.graphics.drawable.BitmapDrawable}</li>
 *   <li>{@code <color>} → {@link android.graphics.drawable.ColorDrawable}</li>
 * </ul>
 *
 * <p>Référence layoutlib original :
 * {@code com.android.layoutlib.bridge.impl.ResourceHelper.getDrawable()}.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public interface DrawableResolver {

    /**
     * Résout une référence {@code @drawable/foo} en {@link Drawable}.
     *
     * @param reference la référence (ex. {@code @drawable/my_bg})
     * @param context   le contexte Android
     * @return le drawable, ou {@code null} si introuvable
     */
    Drawable resolve(String reference, Context context);

    /**
     * Parse un XML de drawable directement.
     *
     * @param xml     le XML source
     * @param context le contexte Android
     * @return le drawable créé
     */
    Drawable parse(String xml, Context context);

    /**
     * Vide le cache des drawables résolus.
     */
    void clearCache();
}
