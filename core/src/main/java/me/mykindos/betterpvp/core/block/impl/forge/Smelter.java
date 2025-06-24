package me.mykindos.betterpvp.core.block.impl.forge;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.behavior.StorageBehavior;
import me.mykindos.betterpvp.core.block.nexo.NexoBlock;
import me.mykindos.betterpvp.core.inventory.inventoryaccess.component.AdventureComponentWrapper;
import me.mykindos.betterpvp.core.inventory.window.Window;
import me.mykindos.betterpvp.core.item.ItemFactory;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Singleton
public class Smelter extends SmartBlock implements NexoBlock {

    private final ItemFactory itemFactory;

    @Inject
    private Smelter(ItemFactory itemFactory) {
        super("smelter", "Smelter");
        this.itemFactory = itemFactory;
        setClickBehavior(this::handleClick);
    }

    private void handleClick(@NotNull SmartBlockInstance blockInstance, @NotNull Player player) {
        new GuiSmelter(itemFactory, blockInstance).show(player);
    }

    @Override
    public @NotNull String getId() {
        return "blacksmith_v2_furnace";
    }
}
