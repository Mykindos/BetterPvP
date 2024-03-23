package me.mykindos.betterpvp.core.utilities;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@CustomLog
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UtilPlayer {

    public static void setWarningEffect(Player player, int warningDelaySeconds) {
        final WorldBorder curBorder = player.getWorld().getWorldBorder();
        final WorldBorder newWorldBorder = Bukkit.getServer().createWorldBorder();
        newWorldBorder.setCenter(curBorder.getCenter());
        newWorldBorder.setSize(curBorder.getSize());
        newWorldBorder.setWarningDistance((int) curBorder.getMaxSize());
        newWorldBorder.setDamageAmount(0);
        newWorldBorder.setDamageBuffer(0);
        newWorldBorder.setWarningTime(warningDelaySeconds);
        player.setWorldBorder(newWorldBorder);
    }

    public static void clearWarningEffect(Player player) {
        player.setWorldBorder(player.getWorld().getWorldBorder());
    }

    public static List<Player> getNearbyEnemies(Player player, Location location, double radius) {
        List<Player> enemies = new ArrayList<>();
        getNearbyPlayers(player, location, radius, EntityProperty.ENEMY).forEach(entry -> enemies.add(entry.get()));
        return enemies;
    }

    public static List<Player> getNearbyAllies(Player player, Location location, double radius) {
        List<Player> friendlies = new ArrayList<>();
        getNearbyPlayers(player, location, radius, EntityProperty.FRIENDLY).forEach(entry -> friendlies.add(entry.get()));
        return friendlies;
    }

    public static List<KeyValue<Player, EntityProperty>> getNearbyPlayers(Player player, double radius) {
        return getNearbyPlayers(player, player.getLocation(), radius, EntityProperty.ALL);
    }

    public static List<KeyValue<Player, EntityProperty>> getNearbyPlayers(Player player, Location location, double radius, EntityProperty entityProperty) {

        List<KeyValue<Player, EntityProperty>> players = new ArrayList<>();
        player.getWorld().getPlayers().stream()
                .filter(worldPlayer -> {
                    if (worldPlayer.equals(player)) return false;
                    if (!worldPlayer.getWorld().getName().equalsIgnoreCase(location.getWorld().getName())) return false;
                    return worldPlayer.getLocation().distance(location) <= radius;
                })
                .forEach(ent -> players.add(new KeyValue<>(ent, entityProperty)));

        FetchNearbyEntityEvent<Player> fetchNearbyEntityEvent = new FetchNearbyEntityEvent<>(player, location, players, entityProperty);
        UtilServer.callEvent(fetchNearbyEntityEvent);

        return fetchNearbyEntityEvent.getEntities();
    }

    public static int getPing(Player player) {
        return player.getPing();
    }

    public static boolean isCreativeOrSpectator(Entity entity) {
        if (entity instanceof Player player) {
            return player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR;
        }
        return false;
    }

    /**
     * Adds a specified amount of health to a player
     *
     * @param player Player to add health to
     * @param mod    Amount of health to add to the player
     */
    public static void health(Player player, double mod) {
        if (player.isDead()) {
            return;
        }
        double health = player.getHealth() + mod;
        if (health < 0.0D) {
            health = 0.0D;
        }
        if (health > UtilPlayer.getMaxHealth(player)) {
            health = UtilPlayer.getMaxHealth(player);
        }
        player.setHealth(health);
    }

    public static double getHealthPercentage(LivingEntity e) {
        return e.getHealth() / e.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
    }

    public static double getMaxHealth(LivingEntity e) {
        return e.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
    }

    @SneakyThrows
    public static void setGlowing(Player player, Entity target, boolean glowing) {
        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, target.getEntityId()); //Set packet's entity id
        WrappedDataWatcher watcher = new WrappedDataWatcher(); //Create data watcher, the Entity Metadata packet requires this
        WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.get(Byte.class); //Found this through google, needed for some stupid reason
        watcher.setEntity(target); //Set the new data watcher's target
        byte entityByte = 0x00;
        if (glowing) {
            entityByte = (byte) (entityByte | 0x40);
        }

        watcher.setObject(0, serializer, entityByte); //Set status to glowing, found on protocol page

        final List<WrappedDataValue> wrappedDataValueList = new ArrayList<>();

        for (final WrappedWatchableObject entry : watcher.getWatchableObjects()) {
            if (entry == null) continue;

            final WrappedDataWatcher.WrappedDataWatcherObject watcherObject = entry.getWatcherObject();
            wrappedDataValueList.add(
                    new WrappedDataValue(
                            watcherObject.getIndex(),
                            watcherObject.getSerializer(),
                            entry.getRawValue()
                    )
            );
        }

        packet.getDataValueCollectionModifier().write(0, wrappedDataValueList);
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
    }

    public static Location getMidpoint(Player player) {
        final Location location = player.getLocation();
        final double height = player.getHeight();
        return location.add(0.0, height / 2, 0.0);
    }

    public static void slowDrainHealth(BPvPPlugin plugin, Player player, double amount, int ticks, boolean canKill) {
        double amountPerTick = amount / ticks;
        for (int i = 0; i < ticks; i++) {
            UtilServer.runTaskLater(plugin, () -> {
                if (player.isDead()) return;
                if (!canKill && player.getHealth() <= 1) return;
                UtilPlayer.health(player, -amountPerTick);
            }, i);
        }
    }
}
