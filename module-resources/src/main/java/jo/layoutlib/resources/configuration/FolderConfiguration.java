package jo.layoutlib.resources.configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration complète d un dossier de resources, inspiré de
 * com.android.ide.common.resources.configuration.FolderConfiguration de l AOSP.
 *
 * <p>Stocke tous les qualifiers d un dossier : densité, langue, orientation,
 * version API, etc.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class FolderConfiguration {

    /** Tag de journalisation. */
    private static final String TAG = "FolderConfig";

    private DensityQualifier density;
    private LocaleQualifier locale;
    private ScreenSizeQualifier screenSize;
    private ScreenOrientationQualifier screenOrientation;
    private VersionQualifier version;
    private NavigationMethodQualifier navigationMethod;
    private TextInputMethodQualifier textInputMethod;
    private SmallestScreenWidthQualifier smallestScreenWidth;

    /**
     * Parse un nom de dossier et crée la configuration correspondante.
     *
     * @param folderName le nom du dossier (ex. "values-night-v31")
     * @return la configuration parsée
     */
    public static FolderConfiguration createFromFolderName(String folderName) {
        FolderConfiguration config = new FolderConfiguration();
        if (folderName == null || folderName.isEmpty()) {
            return config;
        }
        String[] parts = folderName.split("-");
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part.equals("night") || part.equals("day")) {
                // Pas stocké ici, géré par ResourceQualifier séparé
            } else if (part.equals("land")) {
                config.setScreenOrientation(new ScreenOrientationQualifier(true));
            } else if (part.equals("port")) {
                config.setScreenOrientation(new ScreenOrientationQualifier(false));
            } else if (part.startsWith("v") && part.length() > 1) {
                try {
                    int api = Integer.parseInt(part.substring(1));
                    config.setVersion(new VersionQualifier(api));
                } catch (NumberFormatException ignored) {
                    // Voulu : segment de dossier non numérique (ex. -night) —
                    // pas un niveau d'API, on ignore
                }
            } else {
                // Tenter densité
                DensityQualifier d = DensityQualifier.fromQualifier(part);
                if (d != null) {
                    config.setDensity(d);
                }
            }
        }
        return config;
    }

    /**
     * @return la densité, ou null
     */
    public DensityQualifier getDensity() {
        return density;
    }

    public void setDensity(DensityQualifier density) {
        this.density = density;
    }

    public LocaleQualifier getLocale() {
        return locale;
    }

    public void setLocale(LocaleQualifier locale) {
        this.locale = locale;
    }

    public ScreenSizeQualifier getScreenSize() {
        return screenSize;
    }

    public void setScreenSize(ScreenSizeQualifier screenSize) {
        this.screenSize = screenSize;
    }

    public ScreenOrientationQualifier getScreenOrientation() {
        return screenOrientation;
    }

    public void setScreenOrientation(ScreenOrientationQualifier screenOrientation) {
        this.screenOrientation = screenOrientation;
    }

    public VersionQualifier getVersion() {
        return version;
    }

    public void setVersion(VersionQualifier version) {
        this.version = version;
    }

    public NavigationMethodQualifier getNavigationMethod() {
        return navigationMethod;
    }

    public void setNavigationMethod(NavigationMethodQualifier navigationMethod) {
        this.navigationMethod = navigationMethod;
    }

    public TextInputMethodQualifier getTextInputMethod() {
        return textInputMethod;
    }

    public void setTextInputMethod(TextInputMethodQualifier textInputMethod) {
        this.textInputMethod = textInputMethod;
    }

    public SmallestScreenWidthQualifier getSmallestScreenWidth() {
        return smallestScreenWidth;
    }

    public void setSmallestScreenWidth(SmallestScreenWidthQualifier smallestScreenWidth) {
        this.smallestScreenWidth = smallestScreenWidth;
    }

    /**
     * @return tous les qualifiers non-null
     */
    public List<ResourceQualifier> getAllQualifiers() {
        List<ResourceQualifier> list = new ArrayList<>();
        if (density != null) list.add(density);
        if (locale != null) list.add(locale);
        if (screenSize != null) list.add(screenSize);
        if (screenOrientation != null) list.add(screenOrientation);
        if (version != null) list.add(version);
        if (navigationMethod != null) list.add(navigationMethod);
        if (textInputMethod != null) list.add(textInputMethod);
        if (smallestScreenWidth != null) list.add(smallestScreenWidth);
        return list;
    }

    /**
     * Indique si cette configuration est compatible avec une cible.
     *
     * @param target la configuration cible
     * @return true si compatible
     */
    public boolean isMatchFor(FolderConfiguration target) {
        if (target == null) return false;
        for (ResourceQualifier q : getAllQualifiers()) {
            ResourceQualifier targetQ = findMatchingQualifier(target, q);
            if (targetQ != null && !q.isMatchFor(targetQ)) {
                return false;
            }
        }
        return true;
    }

    private ResourceQualifier findMatchingQualifier(FolderConfiguration other, ResourceQualifier q) {
        if (q instanceof DensityQualifier) return other.density;
        if (q instanceof LocaleQualifier) return other.locale;
        if (q instanceof ScreenSizeQualifier) return other.screenSize;
        if (q instanceof ScreenOrientationQualifier) return other.screenOrientation;
        if (q instanceof VersionQualifier) return other.version;
        if (q instanceof NavigationMethodQualifier) return other.navigationMethod;
        if (q instanceof TextInputMethodQualifier) return other.textInputMethod;
        if (q instanceof SmallestScreenWidthQualifier) return other.smallestScreenWidth;
        return null;
    }

    /**
     * @return le nombre de qualifiers
     */
    public int getQualifierCount() {
        return getAllQualifiers().size();
    }
}
