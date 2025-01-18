package me.mykindos.betterpvp.core.client.offlinemessages.menu;

import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.impl.ViewDescribableMenu;
import me.mykindos.betterpvp.core.utilities.model.description.Describable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OfflineMessagesMenu extends ViewDescribableMenu {
    public OfflineMessagesMenu(@NotNull String title, @NotNull List<Describable> pool, Windowed previous) {
        super(title, pool, previous);
    }
}
