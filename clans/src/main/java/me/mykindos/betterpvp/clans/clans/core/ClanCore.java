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
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.data.CustomDataType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.Display;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
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
    private final @NotNull Clan clan;
    private final @NotNull ClanVault vault;
    private EnderCrystal crystal;
    private TextDisplay healthBar;
    private BukkitTask teleportTask;
    private @Nullable Location position;
    private double health = Double.MAX_VALUE;
    private double maxHealth = Double.MAX_VALUE;

    public ClanCore(@NotNull final Clan clan) {
        this.clan = clan;
        this.vault = new ClanVault(clan);
    }

    public static boolean isCore(final Block block) {
        if (block == null) {
            return false;
        }

        return UtilBlock.getPersistentDataContainer(block).has(ClansNamespacedKeys.CLAN_CORE);
    }

    public boolean isSet() {
        return this.position != null;
    }

    public Location getSafest(final Location location) {
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

    public void teleport(final Player player, final boolean feedback) {
        if (this.position == null) {
            throw new IllegalStateException("Core location is not set");
        }

        Location teleportLocation = this.position.clone().add(0, 0.51, 0);
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

            new SoundEffect(Sound.BLOCK_BEACON_POWER_SELECT, 1.3F, 1f).play(this.position);
            final RespawnAnchor data = (RespawnAnchor) this.position.getBlock().getBlockData();
            data.setCharges(data.getMaximumCharges());
            this.position.getBlock().setBlockData(data);

            if (this.teleportTask != null && !this.teleportTask.isCancelled()) {
                this.teleportTask.cancel();
            }

            final Location old = this.position.clone();
            this.teleportTask = UtilServer.runTaskLater(JavaPlugin.getPlugin(Clans.class), false, () -> {
                if (!old.equals(this.position)) {
                    return;
                }

                data.setCharges(0);
                this.position.getBlock().setBlockData(data);
                this.teleportTask = null;
            }, 40L);
        }).exceptionally(ex -> {
            log.error("Failed to teleport player to core location", ex);
            return null;
        });
    }

    public boolean hasJustTeleported() {
        return this.teleportTask != null && !this.teleportTask.isCancelled();
    }

    public boolean removeBlock() {
        if (this.position != null) {
            final PersistentDataContainer pdc = UtilBlock.getPersistentDataContainer(this.position.getBlock());
            pdc.remove(ClansNamespacedKeys.CLAN_CORE);
            UtilBlock.setPersistentDataContainer(this.position.getBlock(), pdc);

            if (this.position.getBlock().getType().equals(CORE_BLOCK)) {
                this.position.getBlock().setType(Material.AIR);
            }

            return true;
        }
        return false;
    }

    public boolean placeBlock() {
        if (!this.clan.isAdmin() && this.position != null) {
            this.position.getBlock().breakNaturally();
            this.position.getBlock().setType(CORE_BLOCK);
            final PersistentDataContainer pdc = UtilBlock.getPersistentDataContainer(this.position.getBlock());
            pdc.set(ClansNamespacedKeys.CLAN_CORE, PersistentDataType.BOOLEAN, true);
            UtilBlock.setPersistentDataContainer(this.position.getBlock(), pdc);

            if (this.crystal != null && this.crystal.isValid()) {
                this.crystal.teleportAsync(this.position.clone().add(0, 0.5, 0));
            }
            return true;
        }
        return false;
    }

    public void spawnCrystal(final boolean healthbar) {
        if (this.crystal != null && this.crystal.isValid()) {
            return;
        }

        Preconditions.checkNotNull(this.position, "Core location is not set");
        this.crystal = this.position.getWorld().spawn(this.position.clone().add(0, 0.5, 0), EnderCrystal.class, entity -> {
            entity.setShowingBottom(false);
            entity.setGravity(false);
            entity.setVisualFire(false);
            entity.setPersistent(false);
            entity.getPersistentDataContainer().set(ClansNamespacedKeys.CLAN_CORE, PersistentDataType.BOOLEAN, true);
            entity.getPersistentDataContainer().set(ClansNamespacedKeys.CLAN, CustomDataType.UUID, this.clan.getId());
        });

        if (healthbar) {
            this.healthBar = this.crystal.getWorld().spawn(this.crystal.getLocation().add(0, this.crystal.getHeight(), 0), TextDisplay.class, entity -> {
                entity.setInvulnerable(true);
                entity.setGravity(false);
                entity.setVisualFire(false);
                entity.setPersistent(false);
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                entity.setSeeThrough(true);
                entity.setShadowed(false);
                this.crystal.addPassenger(entity);
            });
            this.updateHealthBar();
        } else {
            this.healthBar = null;
        }

        new SoundEffect(Sound.ITEM_TRIDENT_RETURN, 0.4F, 1F).play(this.crystal.getLocation());
        Particle.LARGE_SMOKE.builder()
                .location(this.crystal.getLocation().add(0.0, this.crystal.getHeight() / 2f, 0.0))
                .count(20)
                .extra(0.2)
                .receivers(60)
                .spawn();
    }

    public void updateHealthBar() {
        if (this.healthBar == null) {
            return;
        }

        final double progress = this.health / this.maxHealth;
        this.healthBar.text(ProgressBar.withProgress((float) progress).build());
    }

    public void despawnCrystal() {
        if (this.healthBar != null) {
            if (this.healthBar.isValid()) {
                this.healthBar.remove();
            }
            this.healthBar = null;
        }

        if (this.crystal != null) {
            if (this.crystal.isValid()) {
                new SoundEffect(Sound.ITEM_TRIDENT_RETURN, 1.2F, 0.7F).play(this.crystal.getLocation());
                Particle.CLOUD.builder()
                        .location(this.crystal.getLocation().add(0.0, this.crystal.getHeight() / 2f, 0.0))
                        .count(20)
                        .extra(0.2)
                        .receivers(60)
                        .spawn();

                this.crystal.remove();
            }
            this.crystal = null;
        }
    }

    public boolean isDead() {
        return this.health <= 0;
    }
}
