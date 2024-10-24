package me.mykindos.betterpvp.core.logging.menu.button.type;

public interface IStringFilterButton extends IRefreshButton{
    void add(String newFilter);

    java.util.List<String> getContexts();

    String getSelectedFilter();

    void setSelectedFilter(String selectedFilter);
}
