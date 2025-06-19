package me.mykindos.betterpvp.core.block.impl.forge;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.behavior.StorageBehavior;
import me.mykindos.betterpvp.core.block.impl.workbench.GuiWorkbench;
import me.mykindos.betterpvp.core.block.nexo.NexoBlock;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.blueprint.BlueprintItem;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingManager;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Singleton
public class Forge extends SmartBlock implements NexoBlock {

    private final ItemFactory itemFactory;

    @Inject
    private Forge(ItemFactory itemFactory) {
        super("forge", "Forge");
        this.itemFactory = itemFactory;
        setStorageBehavior(new StorageBehavior(itemFactory));
        setClickBehavior(this::handleClick);
    }

    private void handleClick(@NotNull SmartBlockInstance blockInstance, @NotNull Player player) {
        new GuiForge(itemFactory, blockInstance).show(player);
    }

    @Override
    public @NotNull String getId() {
        return "blacksmith_v2_forge";
    }
}
