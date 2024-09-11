package me.mykindos.betterpvp.core.logging.menu.button;

import me.mykindos.betterpvp.core.inventory.item.Click;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.logging.CachedLog;
import me.mykindos.betterpvp.core.logging.LoggerFactory;
import me.mykindos.betterpvp.core.logging.repository.LogRepository;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class CachedLogButton extends AbstractItem {
    private final CachedLog cachedLog;
    private final LogRepository logRepository;
    private Description description;

    public CachedLogButton(CachedLog cachedLog, LogRepository logRepository, Windowed previous) {
        this.cachedLog = cachedLog;
        this.logRepository = logRepository;
        this.description = LoggerFactory.getInstance().getDescription(cachedLog, logRepository, previous);
    }

    public void setPrevious(Windowed previous) {
        this.description = LoggerFactory.getInstance().getDescription(cachedLog, logRepository, previous);
        this.notifyWindows();
    }

    @Override
    public ItemProvider getItemProvider() {
        return description.getIcon();
    }

    /**
     * A method called if the {@link ItemStack} associated to this {@link Item}
     * has been clicked by a player.
     *
     * @param clickType The {@link ClickType} the {@link Player} performed.
     * @param player    The {@link Player} who clicked on the {@link ItemStack}.
     * @param event     The {@link InventoryClickEvent} associated with this click.
     */
    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {

        if (description.getClickFunction() != null) {
            description.getClickFunction().accept(new Click(event));
        }
    }
}
