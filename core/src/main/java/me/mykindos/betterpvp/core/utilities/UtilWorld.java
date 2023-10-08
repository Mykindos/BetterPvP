package me.mykindos.betterpvp.core.utilities;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Iterator;

public class UtilWorld {

    public static String chunkToPrettyString(Chunk chunk) {
        return "(" + chunk.getX() + ", " + chunk.getZ() + ")";
    }

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
        if (string == null || string.length() == 0) {
            return null;
        }

        String[] split = string.split(", ");
        var world = Bukkit.getWorld(split[0]);

        Location location = new Location(world, Double.parseDouble(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]));

        if (split.length >= 5) {
            location.setYaw(Float.parseFloat(split[4]));
            location.setPitch(Float.parseFloat(split[5]));
        }

        return location;

    }

    public static Chunk closestChunkToPlayer(Collection<Chunk> chunkList, Player player) {
        if (chunkList.isEmpty()) {
            return null;
        }
        Chunk closestChunk = (Chunk) chunkList.stream().findFirst().get();

        int y = (int) player.getY();

        double closestDistance = player.getLocation().distanceSquared(closestChunk.getBlock(8, y, 8).getLocation());

        for (Chunk chunk : chunkList) {
            double distance = player.getLocation().distanceSquared(chunk.getBlock(8, y, 8).getLocation());
            if (closestDistance > distance) {
                closestDistance = distance;
                closestChunk = chunk;
            }
        }
        return closestChunk;
    }

    /**
     * Converts a locations coordinates to a readable string
     *
     * @param location The location
     * @return Returns a string of a locations coordinates
     */
    public static String locationToString(Location location) {
        return locationToString(location, true);
    }

    public static String locationToString(Location location, boolean display) {
        if (display) {
            return "(" + Math.round(location.getX()) + ", " + Math.round(location.getY()) + ", " + Math.round(location.getZ()) + ")";
        }

        return location.getWorld().getName() + ", " + location.getX() + ", " + location.getY() + ", " + location.getZ()
                + ", " + location.getYaw() + ", " + location.getPitch();
    }

    public static Location locMerge(Location a, Location b) {
        a.setX(b.getX());
        a.setY(b.getY());
        a.setZ(b.getZ());
        return a;
    }

}
