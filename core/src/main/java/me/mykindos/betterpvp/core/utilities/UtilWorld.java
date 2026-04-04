package me.mykindos.betterpvp.core.utilities;

import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@CustomLog
public class UtilWorld {

    public static long parseSeed(@NotNull String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException ex) {
            return s.hashCode();
        }
    }

    public static Collection<File> getUnloadedWorlds() {
        final File parent = Bukkit.getWorldContainer();
        final List<File> files = new ArrayList<>();
        findWorlds(parent, files, "");

        return files;
    }

    private static void findWorlds(File directory, List<File> worlds, String path) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (!file.isDirectory()) {
                continue; // Skip non-directories
            }

            // Check if any loaded world has this folder
            boolean alreadyLoaded = false;
            try {
                File canonicalFile = file.getCanonicalFile();
                for (World loadedWorld : Bukkit.getWorlds()) {
                    if (loadedWorld.getWorldFolder().getCanonicalFile().equals(canonicalFile)) {
                        alreadyLoaded = true;
                        break;
                    }
                }
            } catch (IOException e) {
                // Fallback to name check if canonical path fails
                if (Bukkit.getWorld(path + file.getName()) != null) {
                    alreadyLoaded = true;
                }
            }

            if (alreadyLoaded) {
                continue;
            }

            if (new File(file, "level.dat").exists()) {
                worlds.add(file); // Add world folder
            } else {
                findWorlds(file, worlds, path + file.getName() + "/");
            }
        }
    }

    public static String chunkToPrettyString(Chunk chunk) {
        return "(" + chunk.getX() + ", " + chunk.getZ() + ")";
    }

    public static String chunkToFile(Chunk chunk) {
        return chunk.getWorld().getName() + "/ " + chunk.getX() + "/ " + chunk.getZ();
    }

    public static Chunk stringToChunk(String string) {
        return stringToChunk(string, true);
    }

    public static Chunk stringToChunk(String string, boolean generate) {
        try {
            String[] tokens = string.split("/ ");
            World world = Bukkit.getWorld(tokens[0]);
            if (world != null) {
                return world.getChunkAt(Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]), generate);
            }
        } catch (Exception ex) {
            log.error("Error parsing chunk from string: " + string).submit();
        }

        return null;
    }

    public static Location stringToLocation(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }

        string = string.replaceAll("[)(]", "");
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

        Optional<Chunk> chunkOptional = chunkList.stream().findFirst();
        Chunk closestChunk = chunkOptional.get();

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
        return locationToString(location, true, false);
    }

    public static String locationToString(Location location, boolean display) {
        return locationToString(location, display, false);
    }

    public static String locationToString(Location location, boolean display, boolean includeWorld) {
        if (display) {
            if(includeWorld) {
                return "(" + location.getWorld().getName() + ", " + Math.round(location.getX()) + ", " + Math.round(location.getY()) + ", " + Math.round(location.getZ()) + ")";
            } else {
                return "(" + Math.round(location.getX()) + ", " + Math.round(location.getY()) + ", " + Math.round(location.getZ()) + ")";
            }
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
