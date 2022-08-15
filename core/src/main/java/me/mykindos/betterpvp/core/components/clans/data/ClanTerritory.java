package me.mykindos.betterpvp.core.components.clans.data;

import lombok.Data;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.Chunk;

@Data
public class ClanTerritory {

    private final String chunk;

    public Chunk getWorldChunk(){
        return UtilWorld.stringToChunk(chunk);
    }

}
