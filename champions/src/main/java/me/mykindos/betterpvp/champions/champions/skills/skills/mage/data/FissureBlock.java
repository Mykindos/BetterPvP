package me.mykindos.betterpvp.champions.champions.skills.skills.mage.data;

import lombok.Data;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

@Data
public class FissureBlock {

    private final Player player;
    private final Block block;
    private final Material materialToSet;

    public List<LivingEntity> getNearbyEntities() {
        return UtilEntity.getNearbyEnemies(player, block.getLocation(), 1.5);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FissureBlock that = (FissureBlock) o;

        return this.player.equals(that.player) && this.block.getLocation().equals(that.block.getLocation());
    }

    @Override
    public int hashCode() {
        int result = player.hashCode();
        result = 31 * result + block.getLocation().hashCode();
        return result;
    }
}
