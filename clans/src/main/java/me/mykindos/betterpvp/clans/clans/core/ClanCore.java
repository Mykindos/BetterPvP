package me.mykindos.betterpvp.clans.clans.core;

import com.google.common.base.Preconditions;
import lombok.CustomLog;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.vault.ClanVault;
import me.mykindos.betterpvp.clans.utilities.ClansNamespacedKeys;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.data.CustomDataType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Data
@CustomLog
@EqualsAndHashCode(of = "clan")
public final class ClanCore {

    public static final Material CORE_BLOCK = Material.RESPAWN_ANCHOR;

    public static boolean isCore(Block block) {
        if (block == null) {
            return false;
        }

        return UtilBlock.getPersistentDataContainer(block).has(ClansNamespacedKeys.CLAN_CORE);
    }

    private double health;
    private boolean visible;
    private EnderCrystal crystal;
    private BukkitTask teleportTask;
    private final @NotNull Clan clan;
    private @Nullable Location position;
    private final @NotNull ClanVault vault;
    private int energy;

    public ClanCore(@NotNull Clan clan) {
        this.clan = clan;
        this.vault = new ClanVault(clan);
    }

    public boolean isSet() {
        return position != null;
    }

    public void show(Player player) {
        if (crystal == null) {
            return;
        }

        player.showEntity(JavaPlugin.getPlugin(Clans.class), crystal);
        new SoundEffect(Sound.ITEM_TRIDENT_RETURN, 0.4F, 1F).play(player);
        Particle.SMOKE_LARGE.builder()
                .location(crystal.getLocation().add(0.0, crystal.getHeight() / 2f, 0.0))
                .count(20)
                .extra(0.2)
                .receivers(player)
                .spawn();
    }

    public void hide(Player player) {
        if (crystal == null) {
            return;
        }

        final Location crystalLocation = crystal.getLocation().add(0, crystal.getHeight() / 2f, 0);
        new SoundEffect(Sound.ITEM_TRIDENT_RETURN, 1.2F, 0.7F).play(player);
        Particle.CLOUD.builder()
                .location(crystalLocation)
                .count(20)
                .extra(0.2)
                .receivers(player)
                .spawn();
        player.hideEntity(JavaPlugin.getPlugin(Clans.class), crystal);
    }

    public Location getSafest(Location location) {
        if (location.getBlock().isReplaceable() && location.getBlock().getRelative(BlockFace.UP).isPassable() &&
                location.getBlock().getRelative(BlockFace.UP, 2).isPassable()) {
            return location; // No need to scan if the location is already safe
        }

        final Optional<Block> scanned = UtilBlock.scanCube(location,
                1,
                1,
                1,
                block -> block.isReplaceable() && block.getRelative(BlockFace.UP).isPassable() && block.getRelative(BlockFace.UP, 2).isPassable());

        return scanned.map(Block::getLocation).orElse(location);
    }

    public void teleport(Player player, boolean feedback) {
        if (position == null) {
            throw new IllegalStateException("Core location is not set");
        }

        Location teleportLocation = position.clone().add(0, 0.51, 0);
        if (!teleportLocation.getBlock().isPassable() || !teleportLocation.getBlock().getRelative(BlockFace.UP).isPassable()) {
            final Optional<Block> scanned = UtilBlock.scanCube(teleportLocation,
                    1,
                    1,
                    1,
                    block -> block.isPassable() && block.getRelative(BlockFace.UP).isPassable());

            if (scanned.isPresent()) {
                teleportLocation = scanned.get().getLocation().add(0.5, 0.01, 0.5);
            }
        }

        player.teleportAsync(teleportLocation).thenAccept(success -> {
            if (Boolean.FALSE.equals(success)) {
                UtilMessage.message(player, "Clans", "<red>Failed to teleport to clan home.");
                return;
            }

            if (feedback) {
                UtilMessage.message(player, "Clans", "Teleported to clan home.");
            }

            new SoundEffect(Sound.BLOCK_BEACON_POWER_SELECT, 1.3F, 1f).play(position);
            final RespawnAnchor data = (RespawnAnchor) position.getBlock().getBlockData();
            data.setCharges(data.getMaximumCharges());
            position.getBlock().setBlockData(data);

            if (teleportTask != null && !teleportTask.isCancelled()) {
                teleportTask.cancel();
            }

            final Location old = position.clone();
            teleportTask = UtilServer.runTaskLater(JavaPlugin.getPlugin(Clans.class), false, () -> {
                if (!old.equals(position)) {
                    return;
                }

                data.setCharges(0);
                position.getBlock().setBlockData(data);
                teleportTask = null;
            }, 40L);
        }).exceptionally(ex -> {
            log.error("Failed to teleport player to core location", ex);
            return null;
        });
    }

    public boolean hasJustTeleported() {
        return teleportTask != null && !teleportTask.isCancelled();
    }

    public boolean removeBlock() {
        if (position != null) {
            final PersistentDataContainer pdc = UtilBlock.getPersistentDataContainer(position.getBlock());
            pdc.remove(ClansNamespacedKeys.CLAN_CORE);
            UtilBlock.setPersistentDataContainer(position.getBlock(), pdc);

            if (position.getBlock().getType().equals(CORE_BLOCK)) {
                position.getBlock().setType(Material.AIR);
            }
            return true;
        }
        return false;
    }

    public boolean placeBlock() {
        if (!clan.isAdmin() && position != null) {
            position.getBlock().breakNaturally();
            position.getBlock().setType(CORE_BLOCK);
            final PersistentDataContainer pdc = UtilBlock.getPersistentDataContainer(position.getBlock());
            pdc.set(ClansNamespacedKeys.CLAN_CORE, PersistentDataType.BOOLEAN, true);
            UtilBlock.setPersistentDataContainer(position.getBlock(), pdc);
            return true;
        }
        return false;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;

        if (visible) {
            if (crystal != null) {
                return;
            }

            Preconditions.checkNotNull(position, "Core location is not set");
            crystal = position.getWorld().spawn(position.clone().add(0, 0.5, 0), EnderCrystal.class, entity -> {
                entity.setShowingBottom(false);
                entity.setGravity(false);
                entity.setVisualFire(false);
                entity.setPersistent(false);
                entity.setVisibleByDefault(false);
                entity.getPersistentDataContainer().set(ClansNamespacedKeys.CLAN_CORE, PersistentDataType.BOOLEAN, true);
                entity.getPersistentDataContainer().set(ClansNamespacedKeys.CLAN, CustomDataType.UUID, clan.getId());
            });
        } else if (crystal != null) {
            if (!crystal.isValid()) {
                crystal = null;
                return;
            }

            crystal.remove();
            crystal = null;
        }
    }
}
