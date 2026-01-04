package me.mykindos.betterpvp.core.utilities;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@CustomLog
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UtilPlayer {

    /**
     * Sets a temporary warning effect on the specified player's world border.
     * This modifies the player's local world border settings to customize its warning behavior.
     *
     * @param player the player whose world border is to be modified
     * @param warningDelaySeconds the duration in seconds for the warning time delay
     */
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

    /**
     * Resets the player's world border to match that of the current world.
     *
     * @param player the player whose warning effect is to be cleared
     */
    public static void clearWarningEffect(Player player) {
        player.setWorldBorder(player.getWorld().getWorldBorder());
    }

    /**
     * Retrieves a list of enemy players within a specified radius from a given location.
     * The method filters nearby players based on the EntityProperty.ENEMY criteria.
     *
     * @param player the player on behalf of whom nearby enemies are being identified
     * @param location the location to search for nearby enemies
     * @param radius the radius around the location within which enemies are searched
     * @return a list of players identified as enemies within the specified radius
     */
    public static List<Player> getNearbyEnemies(Player player, Location location, double radius) {
        List<Player> enemies = new ArrayList<>();
        getNearbyPlayers(player, location, radius, EntityProperty.ENEMY).forEach(entry -> enemies.add(entry.get()));
        return enemies;
    }

    /**
     * Retrieves a list of nearby allies within a specified radius from a given location.
     *
     * @param player the player for whom the nearby allies are being determined
     * @param location the location used as the center of the search radius
     * @param radius the radius within which to search for nearby allies
     * @return a list of players identified as friendly within the given radius of the specified location
     */
    public static List<Player> getNearbyAllies(Player player, Location location, double radius) {
        List<Player> friendlies = new ArrayList<>();
        getNearbyPlayers(player, location, radius, EntityProperty.FRIENDLY).forEach(entry -> friendlies.add(entry.get()));
        return friendlies;
    }

    /**
     * Retrieves a list of nearby players within a specified radius around the given player.
     * The result includes each nearby player along with their associated {@link EntityProperty}.
     *
     * @param player the player whose surroundings are being scanned for nearby players
     * @param radius the radius (in blocks) within which to search for nearby players
     * @return a list of {@link KeyValue} objects, where each key is a nearby {@link Player}
     *         and each value is the {@link EntityProperty} associated with that player
     */
    public static List<KeyValue<Player, EntityProperty>> getNearbyPlayers(Player player, double radius) {
        return getNearbyPlayers(player, player.getLocation(), radius, EntityProperty.ALL);
    }

    /**
     * Retrieves a list of nearby players around a specified location within a given radius,
     * filtering based on certain properties and applying additional validations.
     *
     * @param player The player requesting nearby players; this player is excluded from the results.
     * @param location The location from which to search for nearby players.
     * @param radius The radius around the location to search for nearby players.
     * @param entityProperty The desired property filter for nearby players (e.g., FRIENDLY, ENEMY, or ALL).
     * @return A list of KeyValue pairs containing nearby players and their associated entity properties,
     *         filtered according to the specified entityProperty.
     */
    public static List<KeyValue<Player, EntityProperty>> getNearbyPlayers(Player player, Location location, double radius, EntityProperty entityProperty) {

        List<KeyValue<Player, EntityProperty>> players = new ArrayList<>();
        UtilLocation.getNearbyLivingEntities(location, radius).stream()
                .filter(worldPlayer -> {
                    if (worldPlayer.equals(player)) return false;
                    if (!worldPlayer.getWorld().getName().equalsIgnoreCase(location.getWorld().getName())) return false;
                    if (!(worldPlayer instanceof Player target)) return false;
                    return !target.getGameMode().isInvulnerable();
                })
                .forEach(ent -> players.add(new KeyValue<>((Player) ent, entityProperty)));

        FetchNearbyEntityEvent<Player> fetchNearbyEntityEvent = new FetchNearbyEntityEvent<>(player, location, players, entityProperty);
        UtilServer.callEvent(fetchNearbyEntityEvent);
        fetchNearbyEntityEvent.getEntities().removeIf(pair -> {
            if (fetchNearbyEntityEvent.getEntityProperty().equals(EntityProperty.ALL)) {
                return false;
            }
            return !fetchNearbyEntityEvent.getEntityProperty().equals(pair.getValue());
        });
        return fetchNearbyEntityEvent.getEntities();
    }

    /**
     * Retrieves the current network ping of the specified player.
     *
     * @param player the player whose ping is to be retrieved
     * @return the current ping value of the player
     */
    public static int getPing(Player player) {
        return player.getPing();
    }

    /**
     * Checks whether the given entity is a player in the CREATIVE or SPECTATOR game mode.
     *
     * @param entity the entity to check; this is usually an instance of a Player
     * @return true if the entity is a player in the CREATIVE or SPECTATOR game mode, false otherwise
     */
    public static boolean isCreativeOrSpectator(Entity entity) {
        if (entity instanceof Player player) {
            return player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR;
        }
        return false;
    }

    /**
     * Updates the health of the specified player by modifying their current health
     * with the given value. The resulting health is clamped between 0 and the
     * maximum health for the player.
     *
     * @param player the player whose health will be updated
     * @param mod the value to modify the player's current health by; can be
     *            positive or negative
     */
    public static void health(LivingEntity player, double mod) {
        if (player.isDead()) {
            return;
        }
        player.heal(mod, EntityRegainHealthEvent.RegainReason.CUSTOM);
    }

    /**
     * Retrieves the health percentage of a given living entity.
     *
     * @param e the living entity whose health percentage is to be calculated
     * @return the current health of the entity as a fraction of its maximum health
     */
    public static double getHealthPercentage(LivingEntity e) {
        return e.getHealth() / e.getAttribute(Attribute.MAX_HEALTH).getValue();
    }

    /**
     * Retrieves the maximum health attribute value of the specified living entity.
     *
     * @param e the living entity from which to retrieve the maximum health attribute
     * @return the maximum health value of the specified entity
     */
    public static double getMaxHealth(LivingEntity e) {
        return e.getAttribute(Attribute.MAX_HEALTH).getValue();
    }

    /**
     * Sets or removes the glowing effect on a specific target entity for a player.
     *
     * @param player the player to whom the packet should be sent
     * @param target the target entity to apply or remove the glowing effect
     * @param glowing true to apply the glowing effect, false to remove it
     */
    @SneakyThrows
    public static void setGlowing(Player player, Entity target, boolean glowing) {
        byte glowingByte = 0x00;
        if (glowing) {
            glowingByte = (byte) (glowingByte | 0x40);
        }

        // https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata#Entity_Metadata_Format
        EntityData<?> data = new EntityData<>(0, EntityDataTypes.BYTE, glowingByte);
        final WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(
                target.getEntityId(),
                List.of(data)
        );

        PacketEvents.getAPI().getPlayerManager().getUser(player).sendPacket(packet);
    }

    /**
     * Calculates and returns the midpoint of the player's current location,
     * based on their height. The midpoint is determined to be halfway along
     * the player's height from their current location.
     *
     * @param player the player whose midpoint location is being calculated
     * @return the calculated midpoint location of the player
     */
    public static Location getMidpoint(Player player) {
        final Location location = player.getLocation();
        final double height = player.getHeight();
        return location.add(0.0, height / 2, 0.0);
    }

    /**
     * Gradually changes the health of a player over a specified number of ticks.
     * The health is modified incrementally, dividing the total amount by the
     * number of ticks, and applying the change periodically.
     *
     * @param plugin   The instance of the BPvPPlugin handling the task execution.
     * @param player   The Player whose health will be adjusted.
     * @param amount   The total amount of health to add or subtract, distributed across the ticks.
     * @param ticks    The number of ticks over which the health will change.
     * @param canKill  A boolean indicating whether the player's health can drop below 1.
     */
    public static void slowHealth(BPvPPlugin plugin, Player player, double amount, int ticks, boolean canKill) {
        double amountPerTick = amount / ticks;
        for (int i = 0; i < ticks; i++) {
            UtilServer.runTaskLater(plugin, () -> {
                if (player.isDead()) return;
                if (!canKill && player.getHealth() <= 1) return;
                UtilPlayer.health(player, amountPerTick);
            }, i);
        }
    }

    /**
     * Checks if a player is dead. A player is considered dead if their health is less than
     * or equal to 0 or if the player's internal dead status is true.
     *
     * @param player the player whose death status is being checked
     * @return true if the player is dead, false otherwise
     */
    public static boolean isDead(Player player) {
        return player.getHealth() <= 0 || player.isDead();
    }

    /**
     * Sets the offline position for a player by updating their stored player data.
     *
     * @param id the UUID of the player whose offline position is being updated
     * @param location the new location to set as the player's offline position
     */
    public static void setOfflinePosition(UUID id, Location location) {
        CompoundTag compound = UtilNBT.getPlayerData(id).orElseThrow();
        ListTag posList = new ListTag();

        posList.add(DoubleTag.valueOf(location.getX()));
        posList.add(DoubleTag.valueOf(location.getY()));
        posList.add(DoubleTag.valueOf(location.getZ()));

        compound.put("Pos", posList);
        UtilNBT.savePlayerData(id, compound);

    }

    public static boolean isPlayerInCombat(@NotNull ClientManager clientManager, @NotNull Player player) {
        return clientManager.search().online(player).getGamer().isInCombat();
    }
}
