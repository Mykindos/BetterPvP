package me.mykindos.betterpvp.core.block.impl.smelter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.DataHolder;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import me.mykindos.betterpvp.core.block.nexo.NexoBlock;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.metal.casting.CastingMoldRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.smelting.AlloyRegistry;
import me.mykindos.betterpvp.core.recipe.smelting.SmeltingService;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Singleton
public class Smelter extends SmartBlock implements NexoBlock, DataHolder<SmelterData> {

    private final ItemFactory itemFactory;
    private final SmeltingService smeltingService;
    private final AlloyRegistry alloyRegistry;
    private final CastingMoldRecipeRegistry castingMoldRecipeRegistry;
    private final SmelterDataSerializer dataSerializer;

    @Inject
    private Smelter(ItemFactory itemFactory, SmeltingService smeltingService, AlloyRegistry alloyRegistry, CastingMoldRecipeRegistry castingMoldRecipeRegistry) {
        super("smelter", "Smelter");
        this.itemFactory = itemFactory;
        this.smeltingService = smeltingService;
        this.alloyRegistry = alloyRegistry;
        this.castingMoldRecipeRegistry = castingMoldRecipeRegistry;
        this.dataSerializer = new SmelterDataSerializer(itemFactory, smeltingService, alloyRegistry, castingMoldRecipeRegistry);
    }

    @Override
    public Class<SmelterData> getDataType() {
        return SmelterData.class;
    }

    @Override
    public SmartBlockDataSerializer<SmelterData> getDataSerializer() {
        return dataSerializer;
    }

    @Override
    public SmelterData createDefaultData() {
        // 60-second max burn time, above that, fuel stops burning and will wait until less burn time
        // 10,000 millibuckets (10 buckets) max liquid capacity
        final SmelterData smelterData = new SmelterData(smeltingService, itemFactory, castingMoldRecipeRegistry, 60_000L, 10_000);
        smelterData.setBurnTime(10_000L); // Start with 10 seconds of burn time for testing
        smelterData.setTemperature(1000f);
        return smelterData;
    }

    @Override
    public boolean handleClick(@NotNull SmartBlockInstance blockInstance, @NotNull Player player, @NotNull Action action) {
        if (!action.isRightClick()) {
            return false; // Only handle right-click actions
        }

        final SmelterData data = Objects.requireNonNull(blockInstance.getData());
        data.openGui(player, itemFactory);
        return true;
    }

    @Override
    public @NotNull String getId() {
        return "blacksmith_v2_furnace";
    }
}
