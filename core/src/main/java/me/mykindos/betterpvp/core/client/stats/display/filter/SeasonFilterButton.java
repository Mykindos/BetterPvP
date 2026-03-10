package me.mykindos.betterpvp.core.client.stats.display.filter;

import me.mykindos.betterpvp.core.client.stats.display.IAbstractStatMenu;
import me.mykindos.betterpvp.core.menu.button.filter.FilterButton;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SeasonFilterButton extends FilterButton<IAbstractStatMenu, SeasonContext> {
    /**
     * Create a String filter button, that goes through
     * All values of context, with no default set
     *
     * @param title           the title of this button
     * @param contexts        the list of contexts to use
     * @param numToShow       the number of options to show at once
     * @param displayMaterial the material of the item
     * @param customModelData the custom model data of the item
     */
    public SeasonFilterButton(@NotNull SeasonContext currentContext, List<SeasonContext> contexts) {
        super("Season", contexts, 9, Material.ANVIL, 0);
        this.setSelectedFilter(currentContext);
        setRefresh(this::onChangeSeason);
    }

    public CompletableFuture<Boolean> onChangeSeason() {
        SeasonContext newContext = getSelectedFilter();
        this.getGui().setType(newContext.getStatFilterType());
        this.getGui().setPeriod(newContext.getSeason());
        this.getGui().getRealmFilterButton().onSeasonChange(RealmContext.ALL, IAbstractStatMenu.getRealmContexts(newContext.getSeason(), getGui().getRealmManager()));
        return CompletableFuture.completedFuture(Boolean.TRUE);
    }
}
