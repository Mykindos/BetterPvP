package me.mykindos.betterpvp.clans.clans.map.data;

import lombok.Data;
import me.mykindos.betterpvp.core.components.clans.IClan;
import net.minecraft.world.level.material.MaterialColor;
import org.bukkit.block.BlockFace;

import java.util.HashSet;
import java.util.Set;

@Data
public class ChunkData {

    private MaterialColor color;
    private final String world;
    private final int x,z;
    private final IClan clan;
    private final Set<BlockFace> blockFaceSet;

    public ChunkData(String world, MaterialColor color, int x, int z, IClan clan) {
        this.world = world;
        this.color = color;
        this.x = x;
        this.z = z;
        this.clan = clan;
        this.blockFaceSet = new HashSet<>();
    }

}