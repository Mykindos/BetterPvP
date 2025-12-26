package me.mykindos.betterpvp.core.menu.button.filter;

import me.mykindos.betterpvp.core.logging.menu.button.type.IRefreshButton;

import java.util.List;

public interface IContextFilterButton<T extends IFilterContext<T>> extends IRefreshButton {
    void add(T newFilter);

    List<T> getContexts();

    T getSelectedFilter();

    void setSelectedFilter(T selectedFilter);
}
