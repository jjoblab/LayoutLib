package jo.layoutlib.resources.configuration;

/**
 * Qualifier de resource Android, inspiré de
 * com.android.ide.common.resources.configuration.ResourceQualifier de l AOSP.
 *
 * <p>Un qualifier correspond à un segment d un nom de dossier de resources,
 * par exemple {@code -night} ou {@code -v31} dans {@code values-night-v31}.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public interface ResourceQualifier {

    /**
     * @return le nom court du qualifier (ex. "night", "v31")
     */
    String getName();

    /**
     * @return le nom long du qualifier (ex. "Mode nuit", "API level 31")
     */
    String getLongName();

    /**
     * @return true si ce qualifier est valide
     */
    boolean isValid();

    /**
     * Vérifie si ce qualifier est compatible avec un autre.
     *
     * @param other l autre qualifier
     * @return true s ils sont compatibles
     */
    boolean isMatchFor(ResourceQualifier other);

    /**
     * @return true si ce qualifier est plus spécifique qu un autre
     */
    boolean isBetterMatchThan(ResourceQualifier other, ResourceQualifier reference);
}
