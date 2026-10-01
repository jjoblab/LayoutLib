package jo.layoutlib.inflater.bridge;

import android.content.Context;
import android.view.View;

/**
 * Vue factice utilisée quand une classe custom ne peut pas être instanciée.
 *
 * <p>Inspirée de {@code com.android.layoutlib.bridge.MockView} de l'AOSP.
 * Quand le mini-layoutlib ne peut pas charger une classe (ClassNotFoundException,
 * NoSuchMethodException, etc.), il crée un {@code MockView} à la place pour
 * préserver la structure arborescente et afficher un placeholder visible.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class MockView extends View {

    /** Nom de la classe qui n'a pas pu être instanciée. */
    private String originalClassName;

    /**
     * Construit un MockView avec un contexte.
     *
     * @param context le contexte Android
     */
    public MockView(Context context) {
        super(context);
        setBackgroundColor(0xFFFFC107);  // ambre pour indiquer un placeholder
    }

    /**
     * Définit le nom de la classe originale.
     *
     * @param name nom de la classe qui n'a pas pu être instanciée
     */
    public void setOriginalClassName(String name) {
        this.originalClassName = name;
    }

    /**
     * @return le nom de la classe originale
     */
    public String getOriginalClassName() {
        return originalClassName;
    }

    @Override
    public String toString() {
        return "MockView{" + originalClassName + "}";
    }
}
