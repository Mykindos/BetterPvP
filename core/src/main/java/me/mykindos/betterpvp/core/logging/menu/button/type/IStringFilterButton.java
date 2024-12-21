package me.mykindos.betterpvp.core.logging.menu.button.type;

import java.util.List;

public interface IStringFilterButton extends IRefreshButton{
    void add(String newFilter);

    List<String> getContexts();

    String getSelectedFilter();

    void setSelectedFilter(String selectedFilter);
}
