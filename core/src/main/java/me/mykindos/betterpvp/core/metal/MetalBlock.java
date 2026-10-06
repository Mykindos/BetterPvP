package me.mykindos.betterpvp.core.metal;

import lombok.Getter;
import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.custom.CustomNoteBlock;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a block of metal in the game.
 */
@Getter
public abstract class MetalBlock extends SmartBlock implements CustomNoteBlock {

    private static final List<Material> PICKAXES = List.of(Material.WOODEN_PICKAXE, Material.STONE_PICKAXE,
            Material.IRON_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE);

    private final @NotNull String blockId;
    private final double hardness;
    private final @NotNull Material sounds;
    private final Material minimumTool;

    /**
     * @param minimumTool the weakest pickaxe that drops the block
     */
    protected MetalBlock(String id, String name, @NotNull String blockId, double hardness, @NotNull Material sounds,
                         Material minimumTool) {
        super(id, name);
        this.blockId = blockId;
        this.hardness = hardness;
        this.sounds = sounds;
        this.minimumTool = minimumTool;
    }

    @Override
    public boolean canHarvest(@NotNull ItemStack held) {
        return PICKAXES.indexOf(held.getType()) >= PICKAXES.indexOf(minimumTool);
    }

}
