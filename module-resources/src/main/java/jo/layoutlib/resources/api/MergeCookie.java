package jo.layoutlib.resources.api;

/**
 * Cookie pour les <merge>, inspiré de l'AOSP.
 * Utilisé pour associer un node XML à une vue pendant l'inflation.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class MergeCookie {

    private final Object cookie;

    public MergeCookie(Object cookie) {
        this.cookie = cookie;
    }

    public Object getCookie() {
        return cookie;
    }

    @Override
    public String toString() {
        return "MergeCookie{" + cookie + "}";
    }
}
