package jo.layoutlib.inflater.bridge.binding;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper pour adapters, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class AdapterHelper {

    private final List<Object> items = new ArrayList<>();

    public AdapterHelper() {
    }

    public void addItem(Object item) {
        items.add(item);
    }

    public int getItemCount() {
        return items.size();
    }

    public FakeAdapter createFakeAdapter(Context ctx) {
        FakeAdapter adapter = new FakeAdapter(ctx);
        for (Object item : items) {
            adapter.addItem(item);
        }
        return adapter;
    }
}
