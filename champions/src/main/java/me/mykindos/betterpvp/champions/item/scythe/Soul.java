package me.mykindos.betterpvp.champions.item.scythe;

import com.destroystokyo.paper.ParticleBuilder;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

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
                .map(Bukkit::getPlayer)
                .filter(player -> player != null && player.isOnline())
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

        List<EntityData<?>> items = List.of(
                new EntityData<>(22, EntityDataTypes.INT, color.asRGB()),
                new EntityData<>(12, EntityDataTypes.VECTOR3F, scale)
        );
        final WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(
                display.getEntityId(),
                items
        );
        UtilPlayer.setGlowing(player, display, true);
        PacketEvents.getAPI().getPlayerManager().getUser(player).sendPacket(packet);
    }

    public void hide(Player player) {
        UtilPlayer.setGlowing(player, display, false);
        player.hideEntity(JavaPlugin.getPlugin(Champions.class), display);
    }
}