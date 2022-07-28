package me.mykindos.betterpvp.core.utilities;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Iterator;

public class UtilWorld {

    public static String chunkToFile(Chunk chunk) {
        return chunk.getWorld().getName() + "/ " + chunk.getX() + "/ " + chunk.getZ();
    }

    public static Chunk stringToChunk(String string) {
        try {
            String[] tokens = string.split("/ ");
            World world = Bukkit.getWorld(tokens[0]);
            if (world != null) {
                return world.getChunkAt(Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]));
            }
        } catch (Exception var4) {
            var4.printStackTrace();
        }

        return null;
    }

    public static Location stringToLocation(String string) {
        if (string.length() == 0) {
            return null;
        } else {
            String[] tokens = string.split(",");

            try {
                Iterator var2 = Bukkit.getServer().getWorlds().iterator();

                World cur;
                do {
                    if (!var2.hasNext()) {
                        return null;
                    }

                    cur = (World)var2.next();
                } while(!cur.getName().equalsIgnoreCase(tokens[0]));

                return new Location(cur, Double.parseDouble(tokens[1]), Double.parseDouble(tokens[2]), Double.parseDouble(tokens[3]));
            } catch (Exception var4) {
                return null;
            }
        }
    }

}
