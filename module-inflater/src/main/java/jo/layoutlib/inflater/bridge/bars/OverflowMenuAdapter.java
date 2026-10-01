package jo.layoutlib.inflater.bridge.bars;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter pour menu overflow, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class OverflowMenuAdapter {

    public static class MenuItem {
        private final String title;
        private final int id;
        private boolean enabled = true;

        public MenuItem(String title, int id) {
            this.title = title;
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public int getId() {
            return id;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    private final List<MenuItem> items = new ArrayList<>();

    public void addItem(String title, int id) {
        items.add(new MenuItem(title, id));
    }

    public void removeItem(int id) {
        items.removeIf(item -> item.id == id);
    }

    public List<MenuItem> getItems() {
        return new ArrayList<>(items);
    }

    public int getItemCount() {
        return items.size();
    }

    public MenuItem findItem(int id) {
        for (MenuItem item : items) {
            if (item.id == id) {
                return item;
            }
        }
        return null;
    }

    public void clear() {
        items.clear();
    }
}
