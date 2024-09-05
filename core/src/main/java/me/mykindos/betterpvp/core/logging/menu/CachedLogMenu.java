package me.mykindos.betterpvp.core.logging.menu;

import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.logging.CachedLog;
import me.mykindos.betterpvp.core.logging.menu.button.CachedLogButton;
import me.mykindos.betterpvp.core.logging.repository.LogRepository;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.impl.ViewCollectionMenu;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CachedLogMenu extends ViewCollectionMenu {
    public CachedLogMenu(@NotNull String title, List<CachedLog> pool, LogRepository logRepository, Windowed previous) {
        super(title, pool.stream().map(cachedLog -> {
            return (Item) new CachedLogButton(cachedLog, logRepository, null);
        }).toList(), previous);
        this.content.forEach(item -> {
            if (item instanceof CachedLogButton button) {
                button.setPrevious(this);
            }
        });
    }
}
