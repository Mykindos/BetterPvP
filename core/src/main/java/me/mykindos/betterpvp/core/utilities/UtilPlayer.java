package me.mykindos.betterpvp.core.utilities;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UtilPlayer {


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
                    return !(worldPlayer.getLocation().distance(location) > radius);
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

    public static boolean isHoldingItem(Player player, Material[] items) {
        return Arrays.stream(items).anyMatch(item -> item == player.getInventory().getItemInMainHand().getType());
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
        if (health > 20.0D) {
            health = 20.0D;
        }
        player.setHealth(health);
    }

    public static double getHealthPercentage(LivingEntity e) {
        return e.getHealth() / e.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * 100;
    }

    public static double getMaxHealth(LivingEntity e) {
        return e.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
    }

    @SneakyThrows
    public static void setGlowing(Player player, Player target, boolean glowing) {
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

        for(final WrappedWatchableObject entry : watcher.getWatchableObjects()) {
            if(entry == null) continue;

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
}
