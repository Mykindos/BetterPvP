package me.mykindos.betterpvp.core.client.stats.display.filter;

import me.mykindos.betterpvp.core.client.stats.display.IAbstractStatMenu;
import me.mykindos.betterpvp.core.menu.button.filter.FilterButton;
import org.bukkit.Material;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RealmFilterButton extends FilterButton<IAbstractStatMenu, RealmContext> {
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
    public RealmFilterButton(RealmContext currentContext, List<RealmContext> contexts) {
        super("Clan", contexts, 9, Material.IRON_DOOR, 0);
        this.setSelectedFilter(currentContext);
        setRefresh(this::onChangeSeason);
    }

    void onSeasonChange(RealmContext currentContext, Collection<RealmContext> newContexts) {
        this.getContexts().clear();
        this.getContexts().addAll(newContexts);
        this.setSelectedFilter(currentContext);
    }

    public CompletableFuture<Boolean> onChangeSeason() {
        return CompletableFuture.completedFuture(Boolean.TRUE);
    }
}
