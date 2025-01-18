package me.mykindos.betterpvp.progression.profession.mining.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Getter
public class PlayerMinesOreEvent extends ProgressionMiningEvent {
    private final Block minedOreBlock;
    private final ItemStack toolUsed;

    @Setter
    private boolean doubledDrops;
    private boolean smelted;
    private Material smeltedItem;
    private int smeltedAmount;

    public PlayerMinesOreEvent(Player player, Block minedOreBlock, ItemStack toolUsed) {
        super(player);
        this.minedOreBlock = minedOreBlock;
        this.toolUsed = toolUsed;
        this.doubledDrops = false;
        this.smelted = false;
    }

    public void setSmelted(Material smeltedItem, int smeltedAmount){
        this.smelted = true;
        this.smeltedItem = smeltedItem;
        this.smeltedAmount = smeltedAmount;
    }
}
