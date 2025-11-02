package me.mykindos.betterpvp.core.menu.button.filter;

import me.mykindos.betterpvp.core.logging.menu.button.type.IRefreshButton;

import java.util.List;

public interface IContextFilterButton<Type extends IFilterContext<Type>> extends IRefreshButton {
    void add(Type newFilter);

    List<Type> getContexts();

    Type getSelectedFilter();

    void setSelectedFilter(Type selectedFilter);
}
