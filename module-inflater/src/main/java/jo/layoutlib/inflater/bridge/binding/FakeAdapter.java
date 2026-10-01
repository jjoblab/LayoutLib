package jo.layoutlib.inflater.bridge.binding;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter factice pour prévisualisation.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class FakeAdapter extends android.widget.BaseAdapter {

    private final android.content.Context ctx;
    private final java.util.List<Object> items = new java.util.ArrayList<>();

    public FakeAdapter(android.content.Context ctx) {
        this.ctx = ctx;
    }

    public void addItem(Object item) { items.add(item); }

    @Override
    public int getCount() { return items.size(); }

    @Override
    public Object getItem(int position) { return items.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
        android.widget.TextView tv = new android.widget.TextView(ctx);
        tv.setText(String.valueOf(getItem(position)));
        tv.setPadding(16, 16, 16, 16);
        return tv;
    }
}
