package me.mykindos.betterpvp.core.block.impl.smelter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.DataHolder;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import me.mykindos.betterpvp.core.block.nexo.NexoBlock;
import me.mykindos.betterpvp.core.item.ItemFactory;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Singleton
public class Smelter extends SmartBlock implements NexoBlock, DataHolder<SmelterData> {

    private final ItemFactory itemFactory;
    private final SmelterDataSerializer dataSerializer;

    @Inject
    private Smelter(ItemFactory itemFactory) {
        super("smelter", "Smelter");
        this.itemFactory = itemFactory;
        this.dataSerializer = new SmelterDataSerializer(itemFactory);
        setClickBehavior(this::handleClick);
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
        // 60 seconds max burn time, above that, fuel stops burning and will wait until less burn time
        return new SmelterData(60_000L);
    }

    private void handleClick(@NotNull SmartBlockInstance blockInstance, @NotNull Player player) {
        new GuiSmelter(itemFactory, blockInstance).show(player);
    }

    @Override
    public @NotNull String getId() {
        return "blacksmith_v2_furnace";
    }
}
