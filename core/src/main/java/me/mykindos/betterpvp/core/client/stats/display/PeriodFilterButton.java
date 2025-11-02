package me.mykindos.betterpvp.core.client.stats.display;

import me.mykindos.betterpvp.core.client.stats.period.StatPeriod;
import me.mykindos.betterpvp.core.client.stats.period.StatPeriodManager;
import me.mykindos.betterpvp.core.logging.menu.button.StringFilterButton;
import org.bukkit.Material;

import java.util.concurrent.CompletableFuture;

public class PeriodFilterButton extends StringFilterButton<IAbstractStatMenu> {
    public PeriodFilterButton(String currentPeriodKey, StatPeriodManager statPeriodManager) {
        super("Period",
                statPeriodManager.getObjects().values().stream().sorted().map(StatPeriod::getPeriod).toList(),
                9,
                Material.ANVIL,
                0);
        this.setSelectedFilter(currentPeriodKey);
        this.setRefresh(this::onChangePeriod);

    }

    public CompletableFuture<Boolean> onChangePeriod() {
        this.getGui().setPeriodKey(this.getSelectedFilter().getContext());
        return CompletableFuture.completedFuture(Boolean.TRUE);
    }
}
