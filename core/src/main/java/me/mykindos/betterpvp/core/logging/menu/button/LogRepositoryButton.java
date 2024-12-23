package me.mykindos.betterpvp.core.logging.menu.button;

import lombok.Setter;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.inventory.item.impl.AbstractItem;
import me.mykindos.betterpvp.core.logging.menu.CachedLogMenu;
import me.mykindos.betterpvp.core.logging.repository.LogRepository;
import me.mykindos.betterpvp.core.menu.PreviousableButton;
import me.mykindos.betterpvp.core.menu.Windowed;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class LogRepositoryButton extends AbstractItem implements PreviousableButton {
    protected final String name;
    private final String key;
    private final String value;
    private final @Nullable String actionFilter;
    private final List<String> contexts;
    private final BPvPPlugin plugin;
    private final LogRepository logRepository;
    @Setter
    private Windowed previous;

    protected LogRepositoryButton(String name, String key, String value, @Nullable String actionFilter, List<String> contexts, BPvPPlugin plugin, LogRepository logRepository, Windowed previous) {
        this.name = name;
        this.key = key;
        this.value = value;
        this.actionFilter = actionFilter;
        this.contexts = contexts;
        this.plugin = plugin;
        this.logRepository = logRepository;
        this.previous = previous;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        new CachedLogMenu(name, key, value, actionFilter, contexts, plugin, logRepository, previous).show(player);

    }
}