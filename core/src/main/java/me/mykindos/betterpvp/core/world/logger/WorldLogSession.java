package me.mykindos.betterpvp.core.world.logger;

import lombok.Data;
import org.bukkit.block.Block;

import java.util.List;

@Data
public class WorldLogSession {

    public Block block;
    public WorldLogQueryType queryType = WorldLogQueryType.BLOCK;
    public int pages;
    public int currentPage = 1;
    public List<WorldLog> data;

}
