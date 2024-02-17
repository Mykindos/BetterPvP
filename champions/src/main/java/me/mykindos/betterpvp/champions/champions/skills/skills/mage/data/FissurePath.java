package me.mykindos.betterpvp.champions.champions.skills.skills.mage.data;

import com.destroystokyo.paper.ParticleBuilder;
import lombok.Data;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

@Data
public class FissurePath {

    private Location startLocation;
    private List<FissureBlock> fissureBlocks = new ArrayList<>();

    public void createPath(FissureCast cast) {
        Player player = cast.getPlayer();
        Location location = player.getLocation().add(0, -0.4, 0);
        startLocation = location.clone();

        Vector direction = player.getLocation().getDirection();
        direction.setY(0);
        direction.normalize();
        direction.multiply(0.1);

        while (UtilMath.offset2d(location, startLocation) < cast.getDistance()) {

            location.add(direction);
            Block block = location.getBlock();

            if (UtilBlock.solid(block.getRelative(BlockFace.UP))) {
                location.add(0, 1, 0);
                block = location.getBlock();

                if (UtilBlock.solid(block.getRelative(BlockFace.UP))) {
                    break;
                }
            } else if (!UtilBlock.solid(block)) {
                location.add(0, -1, 0);
                block = location.getBlock();

                if (!UtilBlock.solid(block)) {
                    break;
                }
            }

            double ratio = UtilMath.offset2d(location, startLocation) / cast.getDistance();

            int scale;
            if (ratio <= 0.25) {
                scale = 1;
            } else if (ratio <= 0.5) {
                scale = 2;
            } else {
                scale = 3;
            }


            for(int i = 0; i < scale; i++) {
                Block targetBlock = location.clone().add(0, 1 + i, 0).getBlock();
                Material targetMaterial = Material.STONE;
                Block belowBlock = location.clone().subtract(0, (scale - 1) - i, 0).getBlock();
                if(!cast.getFissure().isForbiddenBlockType(belowBlock)) {
                    targetMaterial = belowBlock.getType();
                }

                FissureBlock fissureBlock = new FissureBlock(player, targetBlock, targetMaterial);

                if (block.equals(startLocation.getBlock())) continue;
                if (fissureBlocks.contains(fissureBlock)) continue;

                if(i == 0) {
                    targetBlock.getWorld().playEffect(targetBlock.getLocation(), org.bukkit.Effect.STEP_SOUND, block.getType());
                }

                fissureBlocks.add(fissureBlock);
            }



        }


    }
}
