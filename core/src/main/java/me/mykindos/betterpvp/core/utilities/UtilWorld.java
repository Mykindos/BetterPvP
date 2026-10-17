package me.mykindos.betterpvp.core.utilities;

import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.apache.commons.io.FileUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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

    /**
     * The folder a world is stored in, whether or not it is loaded: {@code dimensions/<namespace>/<key>} under the
     * main level. A world folder from before 26.x sits at {@code <world container>/<name>} until its first load, when
     * Paper imports it here and deletes the old folder.
     */
    public static @NotNull Path getStorageFolder(@NotNull String worldName) {
        final World loaded = Bukkit.getWorld(worldName);
        if (loaded != null) {
            return loaded.getWorldPath();
        }

        final NamespacedKey key = WorldCreator.name(worldName).key();
        return getDimensionsFolder().resolve(key.namespace()).resolve(key.value());
    }

    /**
     * Where a world from before 26.x, or one just copied in that layout, sits until Paper imports it on its first
     * load. Paper deletes this folder once the import is done, files that are not world data included.
     */
    public static @NotNull File getImportFolder(@NotNull String worldName) {
        return new File(Bukkit.getWorldContainer(), worldName);
    }

    /** The world's folder: where it is stored, or its import folder while it has not been loaded since 26.x. */
    public static @NotNull File getWorldFolder(@NotNull String worldName) {
        final File storage = getStorageFolder(worldName).toFile();
        return storage.isDirectory() ? storage : getImportFolder(worldName);
    }

    /** Deletes a world's stored folder and its import folder. The world must be unloaded. */
    public static void deleteWorldFolders(@NotNull String worldName) throws IOException {
        final File storage = getStorageFolder(worldName).toFile();
        if (storage.exists()) {
            FileUtils.forceDelete(storage);
        }
        final File imported = getImportFolder(worldName);
        if (imported.exists()) {
            FileUtils.forceDelete(imported);
        }
    }

    /**
     * Copies an unloaded world under a new name, in whichever layout it is in: a stored world into storage, a world
     * waiting for import into an import folder. The copy loads as its own world.
     */
    public static void copyWorld(@NotNull String sourceName, @NotNull String targetName) throws IOException {
        final File storage = getStorageFolder(sourceName).toFile();
        final File source = storage.isDirectory() ? storage : getImportFolder(sourceName);
        final File target = storage.isDirectory() ? getStorageFolder(targetName).toFile() : getImportFolder(targetName);
        FileUtils.copyDirectory(source, target, file -> !file.getName().equals("session.lock") && !file.getName().equals("uid.dat"));
    }

    private static @NotNull Path getDimensionsFolder() {
        return Bukkit.getServer().getLevelDirectory().resolve("dimensions");
    }

    /**
     * The name of the world a folder holds, from where it sits: under {@code dimensions/<namespace>/} for a stored
     * world, under the world container for one waiting for import.
     */
    public static @NotNull String getWorldName(@NotNull File folder) throws IOException {
        final Path path = folder.getCanonicalFile().toPath();
        final Path dimensions = getDimensionsFolder().toFile().getCanonicalFile().toPath();
        if (path.startsWith(dimensions)) {
            final Path relative = dimensions.relativize(path);
            final String namespace = relative.getName(0).toString();
            final String key = relative.subpath(1, relative.getNameCount()).toString().replace(File.separatorChar, '/');
            return namespace.equals(NamespacedKey.MINECRAFT) ? key : namespace + ":" + key;
        }

        final Path container = Bukkit.getWorldContainer().getCanonicalFile().toPath();
        return container.relativize(path).toString().replace(File.separatorChar, '/');
    }

    /** Every world folder on disk that is not loaded: stored worlds, and worlds still waiting for import. */
    public static Collection<File> getUnloadedWorlds() {
        final List<File> files = new ArrayList<>();
        findStoredWorlds(getDimensionsFolder().toFile(), files);
        findWorlds(Bukkit.getWorldContainer(), files, "");

        return files;
    }

    /** A stored world is a folder under {@code dimensions/} holding a {@code region} folder. */
    private static void findStoredWorlds(File directory, List<File> worlds) {
        final File[] files = directory.listFiles(File::isDirectory);
        if (files == null) return;

        for (File file : files) {
            if (!new File(file, "region").isDirectory()) {
                findStoredWorlds(file, worlds);
                continue;
            }

            try {
                if (Bukkit.getWorld(getWorldName(file)) == null) {
                    worlds.add(file);
                }
            } catch (IOException e) {
                log.warn("Could not resolve the world stored in {}", file, e).submit();
            }
        }
    }

    private static void findWorlds(File directory, List<File> worlds, String path) {
        File[] files = directory.listFiles();
        if (files == null) return;

        final String levelDirectory = Bukkit.getServer().getLevelDirectory().getFileName().toString();
        for (File file : files) {
            // The main level holds every stored world, which findStoredWorlds covers.
            if (!file.isDirectory() || (path.isEmpty() && file.getName().equals(levelDirectory))) {
                continue;
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
