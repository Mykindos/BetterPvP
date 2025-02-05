package me.mykindos.betterpvp.core.logging.menu.button.type;

import java.util.List;

public interface IStringFilterButton extends IRefreshButton {
    @Override
    default void setRefreshing(boolean isRefreshing) {
        return;
    }
    void add(String newFilter);

    List<String> getContexts();

    String getSelectedFilter();

    void setSelectedFilter(String selectedFilter);
    void setSelected(int selected);

    /**
     * Set whether this button can be used
     * @param isStatic
     */
    void setStatic(boolean isStatic);
}
