package me.mykindos.betterpvp.core.block.impl.smelter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.DataHolder;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.metal.casting.CastingMoldRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.smelting.AlloyRegistry;
import me.mykindos.betterpvp.core.recipe.smelting.SmeltingService;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import me.mykindos.betterpvp.core.block.custom.FurnitureBlock;
import me.mykindos.betterpvp.core.block.custom.FurnitureShape;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.joml.Vector3f;

@Singleton
public class Smelter extends SmartBlock implements FurnitureBlock, DataHolder<SmelterData> {

    private static final FurnitureShape SHAPE = FurnitureShape.builder()
            .model(Key.key("betterpvp", "furniture/blacksmith_v2_furnace"))
            .translation(new Vector3f(0.5f, -0.5f, 0.5f))
            .scale(new Vector3f(0.5f, 0.5f, 0.5f))
            .barriers(FurnitureShape.offsets("0..1,0..3,-1..0"))
            .hardness(5)
            .sounds(Material.STONE)
            .build();

    private final ItemFactory itemFactory;
    private final SmeltingService smeltingService;
    private final CastingMoldRecipeRegistry castingMoldRecipeRegistry;
    private final SmelterDataSerializer dataSerializer;

    @Inject
    private Smelter(ItemFactory itemFactory, SmeltingService smeltingService, AlloyRegistry alloyRegistry, CastingMoldRecipeRegistry castingMoldRecipeRegistry) {
        super("smelter", "Smelter");
        this.itemFactory = itemFactory;
        this.smeltingService = smeltingService;
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
        return new SmelterData(smeltingService,
                itemFactory,
                castingMoldRecipeRegistry,
                60_000L,
                10_000);
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
    public @NotNull String getBlockId() {
        return "blacksmith_v2_furnace";
    }

    @Override
    public @NotNull FurnitureShape getShape() {
        return SHAPE;
    }
}
