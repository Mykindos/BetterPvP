package me.mykindos.betterpvp.clans.fields.block;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.core.EnergyItem;
import me.mykindos.betterpvp.clans.fields.model.CustomOre;
import me.mykindos.betterpvp.clans.fields.model.FieldsBlock;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Handles gold chunks.
 *
 * Gold chunks give a random amount of gold when picked up.
 */
@BPvPListener
public class EnergyOre extends CustomOre implements Listener {

    @Inject
    @Config(path = "fields.blocks.energy.minEnergy", defaultValue = "25")
    private int minEnergy;

    @Inject
    @Config(path = "fields.blocks.energy.maxEnergy", defaultValue = "50")
    private int maxEnergy;

    @Inject
    public EnergyOre(Clans clans, ClientManager clientManager) {
        super(clans, clientManager);
    }

    @Override
    public String getName() {
        return "Energy Shard";
    }

    @Override
    public boolean matches(Block block) {
        final Material type = block.getType();
        for (EnergyItem energyItem : EnergyItem.values()) {
            if (type.equals(energyItem.getMaterial())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public @NotNull BlockData getType() {
        final List<EnergyItem> items = List.of(EnergyItem.SMALL_CRYSTAL, EnergyItem.LARGE_CRYSTAL, EnergyItem.GIANT_CRYSTAL);
        return items.get(UtilMath.RANDOM.nextInt(items.size())).getMaterial().createBlockData();
    }

    @Override
    public @NotNull BlockData getReplacement() {
        return Material.AIR.createBlockData();
    }

    @Override
    public @NotNull ItemStack @NotNull [] generateDrops(@NotNull FieldsBlock fieldsBlock) {
        final EnergyItem energyItem = EnergyItem.fromType(fieldsBlock.getBlock().getType());
        final int range = maxEnergy - minEnergy;
        // depending on the type, distribute the energy amount between the range of min and max energy
        int amount = switch (energyItem) {
            case SMALL_CRYSTAL -> minEnergy + UtilMath.RANDOM.nextInt(range / 3);
            case LARGE_CRYSTAL -> minEnergy + range / 3 + UtilMath.RANDOM.nextInt(range / 3);
            case GIANT_CRYSTAL -> minEnergy + 2 * range / 3 + UtilMath.RANDOM.nextInt(range / 3);
            default -> 0;
        };
        return new ItemStack[] { EnergyItem.SHARD.generateItem(amount, true) };
    }
}
