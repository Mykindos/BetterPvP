package me.mykindos.betterpvp.core.world.model;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.utilities.model.description.Describable;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * Represents a wrapped world in the server.
 */
@Getter
public final class BPvPWorld implements Describable, Comparable<BPvPWorld> {

    private @NotNull WeakReference<World> world;
    private final @NotNull String name;

    public BPvPWorld(@NotNull World world) {
        this.world = new WeakReference<>(world);
        this.name = world.getName();
    }

    public BPvPWorld(@NotNull File worldFolder) {
        Preconditions.checkArgument(worldFolder.isDirectory(), "The file must be a directory.");
        Preconditions.checkArgument(new File(worldFolder, "level.dat").exists(), "Directory is not a world folder.");
        this.world = new WeakReference<>(Bukkit.getWorld(worldFolder.getName()));
        this.name = worldFolder.getName();
    }

    public BPvPWorld(@NotNull String name) {
        this.world = new WeakReference<>(Bukkit.getWorld(name));
        this.name = name;
    }

    /**
     * @return The world's folder.
     */
    public @NotNull File getWorldFolder() {
        return getWorld() == null ? new File(Bukkit.getWorldContainer(), name) : getWorld().getWorldFolder();
    }

    public void checkLoaded() {
        if (getWorld() == null) {
            this.world = new WeakReference<>(Bukkit.getWorld(name));
        }
    }

    /**
     * @return The world wrapped by this instance. May be null if the world has been unloaded.
     */
    public @Nullable World getWorld() {
        return world.get();
    }

    /**
     * Creates or loads the world.
     */
    public void createWorld() {
        if (isLoaded()) {
            return;
        }
        this.world = new WeakReference<>(Bukkit.createWorld(WorldCreator.name(name)));
    }

    /**
     * Creates or loads the world.
     */
    public void createWorld(final WorldCreator creator) {
        if (isLoaded()) {
            return;
        }
        this.world = new WeakReference<>(creator.createWorld());
    }

    /**
     * @return Whether the world is loaded.
     */
    public boolean isLoaded() {
        return getWorld() != null;
    }

    /**
     * Unloads the world if it is loaded.
     */
    public void unloadWorld() {
        if (isLoaded()) {
            final World handle = Objects.requireNonNull(getWorld());
            final World fallback = Bukkit.getWorlds().get(0);
            handle.getPlayers().forEach(player -> player.teleport(fallback.getSpawnLocation()));
            Bukkit.unloadWorld(handle, false);
            this.world.clear();
        }
    }

    /**
     * @return The world's description.
     */
    @Override
    public Description getDescription() {
        final NamedTextColor loadedColor = isLoaded() ? NamedTextColor.GREEN : NamedTextColor.RED;
        final Description.DescriptionBuilder builder = Description.builder()
                .icon(ItemView.builder().displayName(Component.text(name, loadedColor)).material(Material.PAPER).build())
                .property("Name", Component.text(name, NamedTextColor.WHITE))
                .property("Loaded", Component.text(isLoaded(), loadedColor));

        if (isLoaded()) {
            final World loaded = Objects.requireNonNull(getWorld());
            builder.property("Environment", Component.text(loaded.getEnvironment().name(), NamedTextColor.WHITE));
            builder.property("Seed", Component.text(loaded.getSeed(), NamedTextColor.WHITE));
            builder.property("Chunks Loaded", Component.text(loaded.getLoadedChunks().length, NamedTextColor.WHITE));
            builder.property("Entities Loaded", Component.text(loaded.getEntityCount(), NamedTextColor.WHITE));
            builder.property("Players Loaded", Component.text(loaded.getPlayerCount(), NamedTextColor.WHITE));
            builder.property("Difficulty", Component.text(loaded.getDifficulty().name(), NamedTextColor.WHITE));
            builder.property("View Distance", Component.text(loaded.getViewDistance(), NamedTextColor.WHITE));
            builder.property("PvP", Component.text(loaded.getPVP(), loaded.getPVP() ? NamedTextColor.GREEN : NamedTextColor.RED));
        }

        return builder.build();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BPvPWorld bPvPWorld && name.equals(bPvPWorld.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public int compareTo(@NotNull BPvPWorld o) {
        return name.toLowerCase().compareTo(o.name.toLowerCase());
    }

    @SneakyThrows
    public BPvPWorld duplicate(String name) {
        final File worldFolder = getWorldFolder();
        final File newFolder = new File(Bukkit.getWorldContainer(), name);
        final File sessionLock = new File(worldFolder, "session.lock");
        if (sessionLock.exists()) {
            FileUtils.forceDelete(sessionLock);
        }

        FileUtils.copyDirectory(worldFolder, newFolder);
        final File uidFile = new File(newFolder, "uid.dat");
        if (uidFile.exists()) {
            FileUtils.forceDelete(uidFile);
        }

        return new BPvPWorld(newFolder);
    }
}
