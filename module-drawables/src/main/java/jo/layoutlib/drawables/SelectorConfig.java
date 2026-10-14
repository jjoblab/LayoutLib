package jo.layoutlib.drawables;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration d'un drawable de type <selector>.
 * Stocke la liste des items d'un StateListDrawable.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class SelectorConfig {

    private final List<SelectorItem> items = new ArrayList<>();

    public SelectorConfig() {
    }

    public void addItem(SelectorItem item) {
        if (item != null) items.add(item);
    }

    public List<SelectorItem> getItems() {
        return new ArrayList<>(items);
    }

    public int getItemCount() {
        return items.size();
    }

    public SelectorItem getDefaultItem() {
        for (SelectorItem item : items) {
            if (!item.hasAnyState()) return item;
        }
        return null;
    }

    public SelectorItem findMatchingItem(boolean pressed, boolean enabled,
                                          boolean focused, boolean checked,
                                          boolean selected) {
        SelectorItem fallback = null;
        for (SelectorItem item : items) {
            if (item.matches(pressed, enabled, focused, checked, selected)) {
                if (item.hasAnyState()) return item;
                if (fallback == null) fallback = item;
            }
        }
        return fallback;
    }

    public static class SelectorItem {
        private String drawableRef;
        private String inlineDrawableXml;
        private Boolean statePressed;
        private Boolean stateEnabled;
        private Boolean stateFocused;
        private Boolean stateChecked;
        private Boolean stateSelected;
        private Boolean stateWindowFocused;
        private Boolean stateCheckable;

        public SelectorItem() {
        }

        public boolean hasAnyState() {
            return statePressed != null || stateEnabled != null
                    || stateFocused != null || stateChecked != null
                    || stateSelected != null || stateWindowFocused != null
                    || stateCheckable != null;
        }

        public boolean matches(boolean pressed, boolean enabled,
                                boolean focused, boolean checked,
                                boolean selected) {
            if (statePressed != null && statePressed != pressed) return false;
            if (stateEnabled != null && stateEnabled != enabled) return false;
            if (stateFocused != null && stateFocused != focused) return false;
            if (stateChecked != null && stateChecked != checked) return false;
            if (stateSelected != null && stateSelected != selected) return false;
            return true;
        }

        public String getDrawableRef() { return drawableRef; }
        public void setDrawableRef(String ref) { this.drawableRef = ref; }

        /**
         * @return le XML inline de l'enfant du {@code <item>} (shape,
         *         vector, color, selector imbriqué…), ou {@code null} si
         *         l'item désigne son drawable uniquement par référence
         * @since 3.1
         */
        public String getInlineDrawableXml() { return inlineDrawableXml; }

        /**
         * Définit le XML inline capturé de l'enfant du {@code <item>}.
         *
         * @param xml le fragment XML autonome, ou {@code null}
         * @since 3.1
         */
        public void setInlineDrawableXml(String xml) { this.inlineDrawableXml = xml; }

        /**
         * Indique si l'item embarque un drawable inline (ADR 0116 — L4).
         *
         * @return {@code true} si un enfant XML a été capturé
         * @since 3.1
         */
        public boolean hasInlineDrawable() { return inlineDrawableXml != null; }
        public Boolean getStatePressed() { return statePressed; }
        public void setStatePressed(Boolean state) { this.statePressed = state; }
        public Boolean getStateEnabled() { return stateEnabled; }
        public void setStateEnabled(Boolean state) { this.stateEnabled = state; }
        public Boolean getStateFocused() { return stateFocused; }
        public void setStateFocused(Boolean state) { this.stateFocused = state; }
        public Boolean getStateChecked() { return stateChecked; }
        public void setStateChecked(Boolean state) { this.stateChecked = state; }
        public Boolean getStateSelected() { return stateSelected; }
        public void setStateSelected(Boolean state) { this.stateSelected = state; }
        public Boolean getStateWindowFocused() { return stateWindowFocused; }
        public void setStateWindowFocused(Boolean state) { this.stateWindowFocused = state; }
        public Boolean getStateCheckable() { return stateCheckable; }
        public void setStateCheckable(Boolean state) { this.stateCheckable = state; }
    }
}
