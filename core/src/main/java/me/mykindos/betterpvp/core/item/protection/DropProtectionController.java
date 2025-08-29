package me.mykindos.betterpvp.core.item.protection;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Singleton
public class DropProtectionController implements ReloadHook {

    /**
     * Cache that stores per-player drop protection status. Keys are player UUIDs.
     * The value is a DropProtectionStatus containing the tracked ItemInstance and
     * the remaining drop count before protection expires for that tracked item.
     */
    private Cache<@NotNull UUID, DropProtectionStatus> statuses;

    /** Core plugin instance used to read configuration. */
    private final Core core;

    /** Number of drops required to clear protection for a tracked item. */
    @Getter
    private int requiredDrops;

    /**
     * Constructs the DropProtectionController with the main plugin Core instance.
     * This constructor is injected by the dependency injection framework (Guice).
     *
     * @param core the main plugin/core instance used for configuration and services
     */
    @Inject
    private DropProtectionController(Core core) {
        this.core = core;
    }

    /**
     * Reloads configuration for drop protection and (re)initialises the internal cache.
     * <p>
     * This method reads the following configuration keys from the core plugin:
     * <ul>
     *  <li>items.drop-protection.required-drops (int): how many drops are required to clear protection</li>
     *  <li>items.drop-protection.expiry-seconds (long): how long a protection entry should live in seconds</li>
     * </ul>
     *
     * The cache is configured to expire entries after the configured number of seconds
     * from the time they were written.
     */
    @Override
    public void reload() {
        this.requiredDrops = core.getConfig().getOrSaveObject("items.drop-protection.required-drops", 5, Integer.class);
        double duration = core.getConfig().getOrSaveObject("items.drop-protection.expiry-seconds", 2.0, Double.class);
        this.statuses = Caffeine.newBuilder()
                .expireAfterWrite((long) (duration * 1000), TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * Returns the DropProtectionStatus for a given player UUID, or null if none exists.
     *
     * @param playerId the UUID of the player whose status should be retrieved
     * @return the DropProtectionStatus for the player, or null if not present
     */
    public @Nullable DropProtectionStatus getStatus(UUID playerId) {
        return statuses.getIfPresent(playerId);
    }

    /**
     * Returns a live view of the internal statuses cache as a Map. The returned map
     * is the cache's view and modifications to it may affect the cache.
     *
     * @return a Map of player UUIDs to DropProtectionStatus entries
     */
    public Map<@NotNull UUID, DropProtectionStatus> getStatuses() {
        return statuses.asMap();
    }

    /**
     * Handle a player dropping an item while drop protection is enabled.
     * <p>
     * Behavior:
     * <ul>
     *  <li>If there is an existing DropProtectionStatus for the player and the tracked
     *   ItemStack equals the currently dropped ItemStack, the controller will decrement
     *   the remaining drop count. If the remaining drop count reaches zero or below,
     *   the status is removed and the method returns true to indicate the drop should
     *   be allowed (i.e. protection has been cleared).</li>
     *  <li>If there is no existing status for the player, or the existing tracked item
     *   differs from the newly dropped one, a new DropProtectionStatus is created with
     *   the configured requiredDrops count and stored in the cache. The method returns
     *   false to indicate the drop is still protected/blocked.</li>
     *  </ul>
     * Note: equality of items is determined by ItemStack.equals(...) as used by the
     * stored DropProtectionStatus.
     *
     * @param player the player who dropped the item
     * @param itemInstance the ItemInstance representing the dropped item
     * @return true if the drop should be allowed (protection cleared), false otherwise
     */
    public boolean drop(@NotNull Player player, @NotNull ItemInstance itemInstance) {
        final UUID playerId = player.getUniqueId();
        final ItemStack droppedItem = itemInstance.getItemStack();
        final DropProtectionStatus ifPresent = statuses.getIfPresent(playerId);

        // If it already exists
        if (ifPresent != null) {
            final ItemStack storedItem = ifPresent.getItemInstance().getItemStack();
            if (storedItem.equals(droppedItem)) {
                final int remaining = ifPresent.getRemainingDrops() - 1;
                if (remaining <= 0) {
                    statuses.invalidate(playerId);
                    return true;
                }

                final DropProtectionStatus newStatus = new DropProtectionStatus(itemInstance, remaining);
                statuses.put(playerId, newStatus);
                return false;
            }
        }

        // If it doesn't exist, or the existing one is different
        statuses.put(playerId, new DropProtectionStatus(itemInstance, requiredDrops));
        return false;
    }

}
