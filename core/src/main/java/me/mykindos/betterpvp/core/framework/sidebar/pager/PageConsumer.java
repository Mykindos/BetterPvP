package me.mykindos.betterpvp.core.framework.sidebar.pager;


import me.mykindos.betterpvp.core.framework.sidebar.Sidebar;

@FunctionalInterface
public interface PageConsumer<R> {

    void accept(int page, int maxPage, Sidebar sidebar);
}
