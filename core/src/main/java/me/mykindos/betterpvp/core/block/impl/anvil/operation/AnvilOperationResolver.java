package me.mykindos.betterpvp.core.block.impl.anvil.operation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.anvil.AnvilRecipe;
import me.mykindos.betterpvp.core.anvil.AnvilRecipeRegistry;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.repair.RepairService;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

/**
 * Decides which {@link AnvilOperation}, if any, applies to the current anvil contents.
 * Crafting recipes take precedence; if no recipe matches, a repair is attempted.
 */
@Singleton
public class AnvilOperationResolver {

    private final AnvilRecipeRegistry anvilRecipeRegistry;
    private final ItemFactory itemFactory;
    private final ClientManager clientManager;
    private final RepairService repairService;

    @Inject
    private AnvilOperationResolver(AnvilRecipeRegistry anvilRecipeRegistry,
                                   ItemFactory itemFactory,
                                   ClientManager clientManager,
                                   RepairService repairService) {
        this.anvilRecipeRegistry = anvilRecipeRegistry;
        this.itemFactory = itemFactory;
        this.clientManager = clientManager;
        this.repairService = repairService;
    }

    /**
     * @param stackItems    slot → item stack (for recipe matching)
     * @param instanceItems slot → item instance (for repair resolution)
     * @return the operation the anvil should perform, or empty if none applies.
     */
    public @NotNull Optional<AnvilOperation> resolve(@NotNull Map<Integer, ItemStack> stackItems,
                                                     @NotNull Map<Integer, ItemInstance> instanceItems) {
        final Optional<AnvilRecipe> recipe = anvilRecipeRegistry.findRecipe(stackItems);
        if (recipe.isPresent()) {
            return Optional.of(new CraftOperation(recipe.get(), itemFactory, clientManager));
        }

        return repairService.resolve(instanceItems)
                .map(plan -> new RepairOperation(plan, itemFactory));
    }
}
