package me.mykindos.betterpvp.clans.clans.map.data;

import lombok.Data;
import me.mykindos.betterpvp.core.components.clans.IClan;
import net.minecraft.world.level.material.MapColor;
import org.bukkit.block.BlockFace;

import java.util.HashSet;
import java.util.Set;

@Data
public class ChunkData {

    private MapColor color;
    private final String world;
    private final int x,z;
    private final IClan clan;
    private final Set<BlockFace> blockFaceSet;

    public ChunkData(String world, MapColor color, int x, int z, IClan clan) {
        this.world = world;
        this.color = color;
        this.x = x;
        this.z = z;
        this.clan = clan;
        this.blockFaceSet = new HashSet<>();
    }

}