package me.mykindos.betterpvp.champions.item.scythe;

import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.destroystokyo.paper.ParticleBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.packet.play.clientbound.WrapperPlayServerEntityMetadata;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Represents a soul in the world that can be harvested by players
 */
@SuppressWarnings("UnstableApiUsage")
@RequiredArgsConstructor
@Getter
@Setter
public class Soul {

    private final UUID uniqueId = UUID.randomUUID();
    private final ItemDisplay display;
    private final @NotNull UUID owner;
    private final @NotNull Location location;
    private final long spawnTime;
    private final double count;
    private boolean harvesting;
    private boolean markForRemoval;

    private Collection<Player> getNearbyActive(ScytheOfTheFallenLord scythe) {
        return scythe.getSoulHarvestAbility().getPlayerData().keySet().stream()
                .filter(player -> canSee(player, scythe) && scythe.isHoldingWeapon(player))
                .toList();
    }

    public void play(ScytheOfTheFallenLord scythe) {
        // Play lingering effect on the floor
        new ParticleBuilder(Particle.DUST)
                .extra(0)
                .offset(1, 1, 1)
                .count(5)
                .color(203 + ((int) (Math.random() * 25) - 25), 92, 255)
                .location(getLocation())
                .receivers(getNearbyActive(scythe))
                .spawn();
    }

    public boolean canSee(Player player, ScytheOfTheFallenLord scythe) {
        return !player.getUniqueId().equals(owner)
                && player.getWorld().equals(location.getWorld())
                && player.getLocation().distanceSquared(location) <= scythe.getSoulHarvestAbility().getSoulViewDistanceBlocks() * scythe.getSoulHarvestAbility().getSoulViewDistanceBlocks();
    }

    public void show(Player player, boolean selected, ScytheOfTheFallenLord scythe) {
        if (!canSee(player, scythe)) {
            return;
        }

        final Color color = selected ? Color.GREEN : Color.RED;
        final Vector3f scale = selected ? new Vector3f(1.5f, 1.5f, 1.5f) : new Vector3f(1f, 1f, 1f);
        player.showEntity(JavaPlugin.getPlugin(Champions.class), display);

        final WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata();
        List<WrappedDataValue> items = List.of(
                new WrappedDataValue(22, WrappedDataWatcher.Registry.get(Integer.class), color.asRGB()),
                new WrappedDataValue(12, WrappedDataWatcher.Registry.get(Vector3f.class), scale)
        );
        packet.setId(display.getEntityId());
        packet.setPackedItems(items);
        packet.sendPacket(player);
    }

    public void hide(Player player) {
        UtilPlayer.setGlowing(player, display, false);
        player.hideEntity(JavaPlugin.getPlugin(Champions.class), display);
    }
}