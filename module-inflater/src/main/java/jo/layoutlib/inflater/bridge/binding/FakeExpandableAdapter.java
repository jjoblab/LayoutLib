package jo.layoutlib.inflater.bridge.binding;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter factice pour ExpandableListView, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class FakeExpandableAdapter {

    private final List<String> groups = new ArrayList<>();

    public FakeExpandableAdapter() {
    }

    public void addGroup(String g) {
        groups.add(g);
    }

    public int getGroupCount() {
        return groups.size();
    }

    public int getChildrenCount(int groupPosition) {
        return 3;
    }

    public String getGroup(int groupPosition) {
        if (groupPosition >= 0 && groupPosition < groups.size()) {
            return groups.get(groupPosition);
        }
        return null;
    }
}
